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

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx != -1 ? ip.substring(0, idx).trim() : ip;
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
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
