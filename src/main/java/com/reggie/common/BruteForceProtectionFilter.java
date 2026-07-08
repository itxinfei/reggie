package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 暴力破解防护过滤器
 * 检测登录失败次数，超过阈值锁定账号/IP
 *
 * 注意：需要 Redis 支持，如果 Redis 不可用则自动降级（不启用防护）
 *
 * @author itxinfei
 */
@Slf4j
@Component
public class BruteForceProtectionFilter implements Filter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean enabled;

    /**
     * 最大允许失败次数
     */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * 锁定时间（秒）
     */
    private static final int LOCKOUT_DURATION = 900; // 15分钟，与SecurityConstants.LOGIN_LOCK_DURATION保持一致

    /**
     * 登录失败计数 key 前缀
     */
    private static final String LOGIN_FAILURE_KEY_PREFIX = "login:failure:";

    /**
     * 登录锁定 key 前缀
     */
    private static final String LOGIN_LOCKED_KEY_PREFIX = "login:locked:";

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
            // 增加失败计数
            Long failures = redisTemplate.opsForValue().increment(failureKey);

            if (failures != null && failures == 1) {
                // 首次失败，设置过期时间（15分钟）
                redisTemplate.expire(failureKey, 900, TimeUnit.SECONDS);
            }

            log.info("登录失败 - 标识：{}, 失败次数：{}/{}", identifier, failures, MAX_FAILED_ATTEMPTS);

            // 检查是否需要锁定
            if (failures != null && failures >= MAX_FAILED_ATTEMPTS) {
                redisTemplate.opsForValue().set(lockedKey, "locked", LOCKOUT_DURATION, TimeUnit.SECONDS);
                redisTemplate.delete(failureKey);
                log.warn("账号已被锁定 {} 秒 - 标识：{}, 失败次数：{}",
                    LOCKOUT_DURATION, identifier, failures);
            }
        } catch (Exception e) {
            log.error("记录登录失败异常：{}", e.getMessage());
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
            log.error("重置登录失败计数异常：{}", e.getMessage());
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
            log.error("获取登录失败次数异常：{}", e.getMessage());
            return 0;
        }
    }

    /**
     * 检查是否被锁定
     */
    private boolean isLocked(String identifier) {
        try {
            String lockedKey = LOGIN_LOCKED_KEY_PREFIX + identifier;
            return redisTemplate.hasKey(lockedKey);
        } catch (Exception e) {
            log.error("检查账号锁定状态异常：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 记录登录失败（供 Controller 调用，基于 HttpSession）
     *
     * @param session HTTP 会话
     */
    public void recordLoginFailure(HttpSession session) {
        if (!enabled || session == null) {
            return;
        }
        // 从 session 中提取用户标识
        Object userId = session.getAttribute("user");
        Object username = session.getAttribute("username");
        String identifier = userId != null ? userId.toString() : (username != null ? username.toString() : null);
        if (identifier != null) {
            recordFailedAttempt(identifier);
        }
    }

    /**
     * 重置登录失败计数（供 Controller 调用，登录成功后调用）
     *
     * @param session HTTP 会话
     */
    public void resetLoginAttempts(HttpSession session) {
        if (!enabled || session == null) {
            return;
        }
        // 从 session 中提取用户标识
        Object userId = session.getAttribute("user");
        Object username = session.getAttribute("username");
        String identifier = userId != null ? userId.toString() : (username != null ? username.toString() : null);
        if (identifier != null) {
            resetFailedAttempts(identifier);
        }
    }

    /**
     * 获取登录失败次数（供 Controller 查询剩余重试次数，基于 HttpSession）
     *
     * @param session HTTP 会话
     * @return 失败次数，Redis 不可用时返回 0
     */
    public int getFailedAttempts(HttpSession session) {
        if (!enabled || session == null) {
            return 0;
        }
        // 从 session 中提取用户标识
        Object userId = session.getAttribute("user");
        Object username = session.getAttribute("username");
        String identifier = userId != null ? userId.toString() : (username != null ? username.toString() : null);
        if (identifier == null) {
            return 0;
        }
        return getFailedAttemptCount(identifier);
    }

    /**
     * 检查是否被锁定（供 Controller 查询，基于 HttpSession）
     *
     * @param session HTTP 会话
     * @return true=已锁定，false=未锁定
     */
    public boolean isAccountLocked(HttpSession session) {
        if (!enabled || session == null) {
            return false;
        }
        // 尝试从 session 中获取用户标识
        Object userId = session.getAttribute("user");
        Object username = session.getAttribute("username");

        if (userId != null) {
            return isLocked(userId.toString());
        }
        if (username != null) {
            return isLocked(username.toString());
        }
        return false;
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
            log.error("获取登录失败次数异常：{}", e.getMessage());
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
        return uri.contains("/employee/login") || uri.contains("/user/login");
    }

    /**
     * 获取标识（用户名/IP）
     */
    private String getIdentifier(HttpServletRequest request) {
        // 优先使用用户名
        String username = request.getParameter("username");
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }

        // 移动端登录获取 phone 参数
        String phone = request.getParameter("phone");
        if (phone != null && !phone.trim().isEmpty()) {
            return phone;
        }

        // 否则使用 IP
        return request.getRemoteAddr();
    }
}
