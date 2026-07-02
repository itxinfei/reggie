package com.reggie.filter;

import com.reggie.common.CsrfTokenUtil;
import lombok.extern.slf4j.Slf4j;

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
import java.util.HashMap;
import java.util.Map;

/**
 * CSRF 防护过滤器
 * 轻量级实现，不依赖 Spring Security
 *
 * 防护策略：
 * 1. GET 请求：生成或刷新 CSRF Token，存入 Session 和响应头
 * 2. POST/PUT/DELETE 请求：验证 CSRF Token
 *
 * @author itxinfei
 */
@Slf4j
public class CsrfFilter implements Filter {

    /**
     * CSRF Token Session 属性名
     */
    public static final String CSRF_TOKEN_SESSION_ATTR = "CSRF_TOKEN";

    /**
     * CSRF Token 请求参数名
     */
    public static final String CSRF_TOKEN_PARAM = "_csrf";

    /**
     * CSRF Token 响应头名
     */
    public static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";

    /**
     * Token 有效期（1小时）
     */
    private static final long TOKEN_MAX_AGE = 3600 * 1000;

    /**
     * 排除路径（不需要 CSRF 防护）
     */
    private static final String[] EXCLUDED_PATHS = {
        "/actuator/",
        "/backend/",
        "/front/",
        "/common/upload",
        "/common/download",
        "/csrf/",
        "/user/sendMsg",
        "/user/login",
        "/employee/login"
    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("CSRF 防护过滤器初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 在测试环境下跳过 CSRF 验证
        if (isTestEnvironment()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        // 检查是否为排除路径
        if (isExcludedPath(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // GET 请求：生成或刷新 Token
        if ("GET".equalsIgnoreCase(httpRequest.getMethod())) {
            handleGetRequest(httpRequest, httpResponse);
            chain.doFilter(request, response);
            return;
        }

        // POST/PUT/DELETE 请求：验证 Token
        if ("POST".equalsIgnoreCase(httpRequest.getMethod())
            || "PUT".equalsIgnoreCase(httpRequest.getMethod())
            || "DELETE".equalsIgnoreCase(httpRequest.getMethod())) {

            if (!validateToken(httpRequest)) {
                log.warn("CSRF Token 验证失败 - URI：{}, Method：{}", uri, httpRequest.getMethod());
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF Token 验证失败");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.info("CSRF 防护过滤器销毁");
    }

    /**
     * 处理 GET 请求：生成或刷新 Token
     */
    private void handleGetRequest(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();

        // 获取或生成 Token
        String token = (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
        if (token == null || !CsrfTokenUtil.isTokenNotExpired(token, TOKEN_MAX_AGE)) {
            token = CsrfTokenUtil.generateToken();
            session.setAttribute(CSRF_TOKEN_SESSION_ATTR, token);
            log.debug("生成新的 CSRF Token");
        }

        // 设置响应头
        response.setHeader(CSRF_TOKEN_HEADER, token);

        // 设置 Cookie（供前端读取）
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie("XSRF-TOKEN", token);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setSecure(request.isSecure());
        cookie.setMaxAge((int) (TOKEN_MAX_AGE / 1000));
        response.addCookie(cookie);
    }

    /**
     * 验证 CSRF Token
     */
    private boolean validateToken(HttpServletRequest request) {
        // 1. 从请求参数获取 Token
        String token = request.getParameter(CSRF_TOKEN_PARAM);

        // 2. 从请求头获取 Token
        if (token == null) {
            token = request.getHeader(CSRF_TOKEN_HEADER);
        }

        // 3. 从 Session 获取预期 Token
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.warn("CSRF 验证失败：Session 不存在");
            return false;
        }

        String expectedToken = (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
        if (expectedToken == null) {
            log.warn("CSRF 验证失败：Session 中无 Token");
            return false;
        }

        // 4. 验证 Token
        boolean valid = CsrfTokenUtil.validateToken(token, expectedToken);
        if (!valid) {
            log.warn("CSRF Token 不匹配");
        }

        return valid;
    }

    /**
     * 检查是否为排除路径
     */
    private boolean isExcludedPath(String uri) {
        for (String excludedPath : EXCLUDED_PATHS) {
            if (uri.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前请求的 CSRF Token
     *
     * @param request HTTP 请求
     * @return CSRF Token，不存在则返回 null
     */
    public static String getToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
    }

    /**
     * 生成新的 CSRF Token
     *
     * @param request HTTP 请求
     * @return 新的 CSRF Token
     */
    public static String regenerateToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(true);
        String token = CsrfTokenUtil.generateToken();
        session.setAttribute(CSRF_TOKEN_SESSION_ATTR, token);
        return token;
    }

    /**
     * 检查是否为测试环境
     *
     * @return true=测试环境
     */
    private boolean isTestEnvironment() {
        // 检查 system property
        String activeProfiles = System.getProperty("spring.profiles.active", "");
        if (activeProfiles.contains("test")) {
            return true;
        }

        // 检查 environment variable
        String envProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
        if (envProfiles != null && envProfiles.contains("test")) {
            return true;
        }

        return false;
    }
}
