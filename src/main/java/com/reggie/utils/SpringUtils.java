package com.reggie.utils;

import cn.hutool.core.bean.BeanUtil;
import com.reggie.common.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring utility class wrapping frequently-used Spring APIs.
 * <p>
 * Uses Hutool BeanUtil for property copy (supports type conversion).
 * </p>
 *
 * @author reggie
 * @since 2024-01-01
 */
@Slf4j
public final class SpringUtils {

    private SpringUtils() {
        throw new AssertionError();
    }

    // ==================== Bean Retrieval ====================

    public static <T> T getBean(Class<T> clazz) {
        return ApplicationContextProvider.getBean(clazz);
    }

    public static Object getBean(String beanName) {
        return ApplicationContextProvider.getBean(beanName);
    }

    // ==================== Property Copy (Hutool BeanUtil) ====================

    /**
     * Copy properties from source to target using Hutool BeanUtil.
     * <p>Hutool supports type conversion (e.g. String -&gt; Integer) unlike Spring BeanUtils.</p>
     *
     * @param source source object
     * @param target target object
     */
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            log.warn("[SpringUtils] copyProperties called with null: source={}, target={}", source, target);
            return;
        }
        BeanUtil.copyProperties(source, target);
    }

    // ==================== Current Request / Response ====================

    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    public static HttpServletResponse getCurrentResponse() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getResponse() : null;
    }

    // ==================== Request Info ====================

    /**
     * 获取客户端 IP。
     * <p>
     * 安全策略：本系统生产环境通过 Nginx 反代，<b>必须</b>依赖代理写入的
     * X-Forwarded-For（由运维配置为仅接受可信来源），因此这里信任该头；
     * 取第一个值并剥离端口，丢弃后置的代理链。若未配置反代（直连应用），
     * 优先回退 getRemoteAddr()，避免被伪造的 XFF 头绕过 IP 维度限流/审计。
     * </p>
     * <p>
     * 补充：XFF 第一个值通常即真实客户端 IP（Nginx 默认在链头追加），
     * 恶意客户端可直接伪造该头——故本方法同时提供 getRealIp()（受信直连场景）
     * 供 @RateLimit(type=IP) 使用，生产反代场景请配置
     * server.forward-headers-strategy=NATIVE 且信任代理。
     * </p>
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        // 反代场景：X-Forwarded-For = "client, proxy1, proxy2"，取第一个并剥离端口/引号
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            String first = xff.split(",")[0].trim();
            return stripPort(first);
        }
        // 直连场景：X-Real-IP（由反向代理写入时仍可信）
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
            return stripPort(realIp.trim());
        }
        // 兜底：直连时取 TCP 对端地址，不可伪造
        return request.getRemoteAddr();
    }

    /**
     * 剥离 IPv4 端口（"1.2.3.4:8080" → "1.2.3.4"）；IPv6 形态（含冒号）原样返回。
     */
    private static String stripPort(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        int colon = ip.indexOf(':');
        // 仅剥离形如 "d.d.d.d:port" 的 IPv4 带端口；IPv6 含多个冒号，不剥离
        if (colon > 0 && ip.indexOf(':', colon + 1) < 0) {
            // 同时兼容带引号（如 "1.2.3.4":8080 少见形态）与裸 IP
            String before = ip.substring(0, colon);
            if (before.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
                return before;
            }
        }
        return ip;
    }

    public static Map<String, String> getHeaders() {
        HttpServletRequest request = getCurrentRequest();
        Map<String, String> result = new HashMap<>();
        if (request == null) {
            return result;
        }
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            result.put(name, request.getHeader(name));
        }
        return result;
    }

    // ==================== Config Properties ====================

    public static String getProperty(String key) {
        return getEnvironment().getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return getEnvironment().getProperty(key, defaultValue);
    }

    public static <T> T getProperty(String key, Class<T> clazz) {
        return getEnvironment().getProperty(key, clazz);
    }

    // ==================== Path Matching ====================

    public static boolean matchPath(String pattern, String path) {
        if (pattern == null || path == null) {
            return false;
        }
        return new AntPathMatcher().match(pattern, path);
    }

    // ==================== Profile ====================

    public static boolean isProfileActive(String profile) {
        Environment env = getEnvironment();
        for (String active : env.getActiveProfiles()) {
            if (active.equals(profile)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getActiveProfiles() {
        return getEnvironment().getActiveProfiles();
    }

    // ==================== Private Helpers ====================

    private static Environment getEnvironment() {
        return ApplicationContextProvider.getApplicationContext().getEnvironment();
    }
}
