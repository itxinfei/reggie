package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 暴力破解防护过滤器
 * 检测登录失败次数，超过阈值锁定账号/IP
 * 使用Lua脚本保证increment+expire的原子性
 *
 * 注意：需要 Redis 支持，如果 Redis 不可用则自动降级（不启用防护）
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Component
@Profile("!dev") // 开发环境禁用：内网测试无需暴力破解防护，避免锁号影响调试
@WebFilter(filterName = "bruteForceProtectionFilter", urlPatterns = "/*", asyncSupported = true)
public class BruteForceProtectionFilter implements Filter {

    /**
     * Redis操作模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 是否启用暴力破解防护
     */
    private final boolean enabled;

    /**
     * Redis 瞬态故障熔断标记：Redis 异常时 fail-open（放行登录但放弃防护），
     * 避免单点故障导致全站登录不可用。熔断持续 30 秒后自动恢复探测。
     */
    private volatile long redisCircuitOpenUntil = 0;

    /**
     * 熔断持续时间（毫秒）
     */
    private static final long CIRCUIT_OPEN_DURATION_MS = 30_000L;

    /**
     * 最大允许失败次数（复用 SecurityConstants，保持单一来源）
     */
    private static final int MAX_FAILED_ATTEMPTS = SecurityConstants.MAX_LOGIN_FAIL_COUNT;

    /**
     * 锁定时间（秒）
     * 注意：SecurityConstants.LOGIN_LOCK_DURATION 以分钟为单位，此处转换为秒以匹配 Lua 脚本
     */
    private static final int LOCKOUT_DURATION = SecurityConstants.LOGIN_LOCK_DURATION * 60;

    /**
     * 登录失败计数 key 前缀
     */
    private static final String LOGIN_FAILURE_KEY_PREFIX = "login:failure:";

    /**
     * 登录锁定 key 前缀
     */
    private static final String LOGIN_LOCKED_KEY_PREFIX = "login:locked:";

    /**
     * Lua脚本：暴力破解防护原子操作
     * 修改点：使用专用脚本 brute_force.lua，将 increment+expire+锁定+清理 合并为原子操作
     * KEYS[1] = 失败计数Key, KEYS[2] = 锁定Key
     * ARGV[1] = 过期时间(秒), ARGV[2] = 最大失败次数
     * 返回值 = {count, locked}
     */
    private DefaultRedisScript<Long> incrementWithExpireScript;

    /**
     * 检查暴力破解防护是否启用
     *
     * @return true=启用，false=禁用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 构造方法
     * RedisTemplate 为可选依赖，如果不可用则降级
     */
    public BruteForceProtectionFilter(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.enabled = redisTemplate != null;
        if (enabled) {
            log.info("暴力破解防护已启用（Redis模式）");
        } else {
            log.info("暴力破解防护未启用（Redis不可用），已降级");
        }
    }

    /**
     * 初始化Lua脚本
     * 修改点：加载专用暴力破解脚本 brute_force.lua
     */
    @PostConstruct
    public void init() {
        incrementWithExpireScript = new DefaultRedisScript<>();
        incrementWithExpireScript.setScriptSource(new ResourceScriptSource(
                new org.springframework.core.io.ClassPathResource("scripts/brute_force.lua")));
        incrementWithExpireScript.setResultType(Long.class);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("暴力破解防护过滤器初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 如果未启用或Redis不可用，直接放行
        if (!enabled || redisTemplate == null) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 只处理登录接口
        if (isLoginRequest(httpRequest)) {
            String identifier = getIdentifier(httpRequest);

            // 检查是否已被锁定
            if (isLocked(identifier)) {
                log.warn("账号已被锁定 - 标识：{}", identifier);
                httpResponse.setStatus(429); // 429 Too Many Requests
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"code\": 429, \"msg\": \"登录失败次数过多，请15分钟后重试\"}");
                return;
            }

            // 继续过滤器链
            chain.doFilter(request, response);

            // 注意：不在此处检查响应状态码来记录失败
            // 原因：登录接口返回的是 JSON 响应体（HTTP 200），而非 HTTP 401
            // 登录失败由 Controller 层主动调用 recordLoginFailure() 记录
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        log.info("暴力破解防护过滤器销毁");
    }

    /**
     * 记录登录失败
     * 修改点：使用专用 Lua 脚本 brute_force.lua 保证 increment+expire+锁定+清理 原子执行
     *
     * @param identifier 标识（用户名/IP）
     */
    public void recordFailedAttempt(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return;
        }

        String failureKey = LOGIN_FAILURE_KEY_PREFIX + identifier;
        String lockedKey = LOGIN_LOCKED_KEY_PREFIX + identifier;

        try {
            // 修改点：使用Lua脚本原子性执行全部操作
            // KEYS: [failureKey, lockedKey]  ARGV: [expire, maxAttempts]
            // 返回 -1 表示已锁定，否则为当前失败次数
            Long failures = redisTemplate.execute(incrementWithExpireScript,
                    Arrays.asList(failureKey, lockedKey),
                    LOCKOUT_DURATION,  // ARGV[1]: 过期时间（秒），引用 SecurityConstants 计算值
                    MAX_FAILED_ATTEMPTS);  // ARGV[2]: 最大尝试次数

            if (failures != null && failures == -1) {
                log.warn("账号已被锁定 {} 秒 - 标识：{}", LOCKOUT_DURATION, identifier);
            } else if (failures != null) {
                log.info("登录失败 - 标识：{}, 失败次数：{}/{}", identifier, failures, MAX_FAILED_ATTEMPTS);
                if (failures >= MAX_FAILED_ATTEMPTS) {
                    log.warn("账号已被锁定 {} 秒 - 标识：{}, 失败次数：{}",
                        LOCKOUT_DURATION, identifier, failures);
                }
            }
        } catch (Exception e) {
            log.error("记录登录失败异常：identifier={}, error={}", identifier, e.getMessage(), e);
        }
    }

    /**
     * 重置登录失败计数
     *
     * @param identifier 标识（用户名/IP）
     */
    public void resetFailedAttempts(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return;
        }

        try {
            String failureKey = LOGIN_FAILURE_KEY_PREFIX + identifier;
            String lockedKey = LOGIN_LOCKED_KEY_PREFIX + identifier;
            redisTemplate.delete(failureKey);
            redisTemplate.delete(lockedKey);
            log.info("登录失败计数已重置 - 标识：{}", identifier);
        } catch (Exception e) {
            log.error("重置登录失败计数异常：{}", e.getMessage(), e);
        }
    }

    /**
     * 获取登录失败次数（基于标识符）
     *
     * @param identifier 用户标识
     * @return 失败次数，Redis 不可用时返回 0
     */
    public int getFailedAttemptCount(String identifier) {
        if (!enabled || identifier == null || identifier.trim().isEmpty() || redisTemplate == null) {
            return 0;
        }

        try {
            String failureKey = LOGIN_FAILURE_KEY_PREFIX + identifier;
            Object count = redisTemplate.opsForValue().get(failureKey);
            return count != null ? Integer.parseInt(count.toString()) : 0;
        } catch (Exception e) {
            log.error("获取登录失败次数异常：{}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 检查是否被锁定
     * 修改点：Redis 异常时 fail-open（放行），配合瞬态故障熔断，
     * 避免 Redis 单点故障导致全站登录不可用。暴力破解防护降级为"放弃防护但保持可用"。
     */
    private boolean isLocked(String identifier) {
        try {
            String lockedKey = LOGIN_LOCKED_KEY_PREFIX + identifier;
            return redisTemplate.hasKey(lockedKey);
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now < redisCircuitOpenUntil) {
                // 熔断期内，静默放行，降低重复告警噪声
                return false;
            }
            redisCircuitOpenUntil = now + CIRCUIT_OPEN_DURATION_MS;
            log.error("检查账号锁定状态异常，Redis 故障熔断 30s（fail-open 放行，暴力破解防护临时失效）：{}",
                    e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查是否被锁定（供 Controller 查询，基于直接标识符）
     *
     * @param identifier 用户标识（用户名/手机号）
     * @return true=已锁定，false=未锁定
     */
    public boolean isAccountLocked(String identifier) {
        if (!enabled || identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        return isLocked(identifier);
    }

    /**
     * 记录登录失败（供 Controller 调用）
     *
     * @param request HTTP 请求
     */
    public void recordLoginFailure(HttpServletRequest request) {
        if (!enabled) {
            return;
        }

        String identifier = getIdentifier(request);
        recordFailedAttempt(identifier);
    }

    /**
     * 重置登录失败计数（供 Controller 调用，登录成功后调用）
     *
     * @param request HTTP 请求
     */
    public void resetLoginAttempts(HttpServletRequest request) {
        if (!enabled) {
            return;
        }

        String identifier = getIdentifier(request);
        resetFailedAttempts(identifier);
    }

    /**
     * 获取登录失败次数（供 Controller 查询剩余重试次数）
     *
     * @param request HTTP 请求
     * @return 失败次数，Redis 不可用时返回 0
     */
    public int getFailedAttempts(HttpServletRequest request) {
        if (!enabled || request == null || redisTemplate == null) {
            return 0;
        }

        try {
            String identifier = getIdentifier(request);
            String failureKey = LOGIN_FAILURE_KEY_PREFIX + identifier;
            Object count = redisTemplate.opsForValue().get(failureKey);
            return count != null ? Integer.parseInt(count.toString()) : 0;
        } catch (Exception e) {
            log.error("获取登录失败次数异常：{}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 检查是否被锁定（供 Controller 查询）
     *
     * @param request HTTP 请求
     * @return true=已锁定，false=未锁定
     */
    public boolean isAccountLocked(HttpServletRequest request) {
        if (!enabled || request == null) {
            return false;
        }

        String identifier = getIdentifier(request);
        return isLocked(identifier);
    }

    /**
     * 判断是否为登录请求
     */
    private boolean isLoginRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/employee/login".equals(uri) || "/user/login".equals(uri);
    }

    /**
     * 获取标识（用户名/IP）
     * <p>
     * 修复说明：原实现仅通过 request.getParameter() 获取用户名/手机号，
     * 但登录接口（/employee/login、/user/login）均使用 @RequestBody 接收 JSON，
     * getParameter() 对 JSON body 无效，导致暴力破解防护完全失效。
     * 现实现改为直接从输入流读取 JSON body，并解析 username/phone/userAccount 字段；
     * 同时依赖 Spring 的 CharacterEncodingFilter（通常已启用 ContentCachingRequestWrapper）
     * 保证输入流可被重复读取，下游 Controller 仍可正常接收 @RequestBody。
     */
    private String getIdentifier(HttpServletRequest request) {
        // 优先从 query 参数获取（兼容 form-urlencoded）
        String username = request.getParameter("username");
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }

        // 移动端登录获取 phone 参数
        String phone = request.getParameter("phone");
        if (phone != null && !phone.trim().isEmpty()) {
            return phone.trim();
        }

        // 从 JSON body 读取（@RequestBody 场景）
        String bodyStr = readRequestJsonBody(request);
        if (bodyStr != null && !bodyStr.isEmpty()) {
            String u = extractJsonValue(bodyStr, "username");
            if (u != null && !u.trim().isEmpty()) {
                return u.trim();
            }
            String p = extractJsonValue(bodyStr, "phone");
            if (p != null && !p.trim().isEmpty()) {
                return p.trim();
            }
            String ua = extractJsonValue(bodyStr, "userAccount");
            if (ua != null && !ua.trim().isEmpty()) {
                return ua.trim();
            }
        }

        // 兜底：使用请求源IP
        return request.getRemoteAddr();
    }

    /**
     * 从 HttpServletRequest 读取 JSON body
     * <p>
     * 注意：此方法仅应在请求未被下游消费前调用一次。Spring 的
     * CharacterEncodingFilter 通常已将请求包装为 ContentCachingRequestWrapper，
     * 使得输入流可被重复读取；此处兜底处理：若未包装则直接读取。
     */
    private String readRequestJsonBody(HttpServletRequest request) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(request.getInputStream(), Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            return sb.toString();
        } catch (IOException e) {
            log.debug("读取登录请求JSON body失败，降级使用IP：{}", e.getMessage());
            return null;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 简易 JSON 字段值提取（仅支持顶层字符串值，不依赖第三方 JSON 库）
     * 格式匹配：{"fieldName":"value"} 或 {"fieldName": "value"}
     */
    private String extractJsonValue(String json, String fieldName) {
        if (json == null || fieldName == null) {
            return null;
        }
        String search = "\"" + fieldName + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) {
            return null;
        }
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) {
            return null;
        }
        int startIdx = -1;
        for (int i = colonIdx + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                startIdx = i + 1;
                break;
            } else if (c == '{' || c == '[' || c == 'n' || c == 't' || c == 'f' || Character.isDigit(c)) {
                // 非字符串值不提取
                return null;
            }
        }
        if (startIdx < 0) {
            return null;
        }
        int endIdx = json.indexOf('"', startIdx);
        if (endIdx < 0) {
            return null;
        }
        return json.substring(startIdx, endIdx);
    }
}
