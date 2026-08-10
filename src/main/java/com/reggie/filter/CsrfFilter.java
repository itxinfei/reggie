package com.reggie.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * CSRF防护过滤器
 * 对POST/PUT/DELETE请求验证CSRF Token，防止跨站请求伪造攻击
 *
 * 工作原理：
 * 1. 登录成功后，后端生成CSRF Token存入Session，并通过响应头返回给前端
 * 2. 前端保存Token到Cookie/SessionStorage，后续POST/PUT/DELETE请求携带在X-CSRF-Token头部
 * 3. 后端验证请求头中的Token与Session中的Token是否一致
 *
 * @author reggie
 * @since 2026-07-23
 */
@Slf4j
@WebFilter(filterName = "csrfFilter", urlPatterns = "/*", asyncSupported = true)
@Order(1) // 在LoginCheckFilter之前执行
public class CsrfFilter implements Filter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 路径匹配器，支持通配符（与 LoginCheckFilter 保持一致） */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** CSRF Token Session Key */
    private static final String CSRF_TOKEN_KEY = "csrfToken";

    /** 响应头名称 */
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";

    /** 不需要CSRF校验的路径（使用 Ant 通配符，避免 startsWith 匹配过宽） */
    private static final String[] EXCLUDE_URLS = new String[]{
        "/employee/login",
        "/employee/logout",
        "/employee/forgot-password",
        "/user/sendMsg",
        "/user/login",
        "/user/loginout",
        "/tenant/register",
        "/api/ai/**",  // AI模块有自己的验证机制
        "/swagger-ui",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/actuator",
        "/actuator/**",
        "/doc.html",
        "/webjars/**"
    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("CSRF防护过滤器初始化完成");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        // GET等安全方法：生成并返回CSRF Token（如果Session中有用户）
        if (!"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method)) {
            // 在doFilter之前设置CSRF Token响应头，避免响应提交后无法设置
            setCsrfTokenHeader(request, response);
            filterChain.doFilter(request, response);
            return;
        }

        // 检查是否为排除路径（登录等接口不需要CSRF校验）
        if (isExcludedPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取Session中的CSRF Token
        HttpSession session = request.getSession(false);
        if (session == null) {
            // 未登录用户，放行（由LoginCheckFilter处理）
            filterChain.doFilter(request, response);
            return;
        }

        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_KEY);

        // 从请求头获取CSRF Token
        String requestToken = request.getHeader(CSRF_HEADER_NAME);
        if (requestToken == null || requestToken.isEmpty()) {
            // 尝试从请求参数获取
            requestToken = request.getParameter("_csrf");
        }

        // 验证CSRF Token
        if (sessionToken == null || requestToken == null || !sessionToken.equals(requestToken)) {
            log.warn("CSRF验证失败 - URI: {}, sessionToken: {}, requestToken: {}",
                requestURI,
                sessionToken != null ? "exists" : "null",
                requestToken != null ? "exists" : "null");

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                R.error("CSRF验证失败，请刷新页面后重试")));
            return;
        }

        // 验证通过，继续处理
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.info("CSRF防护过滤器销毁");
    }

    /**
     * 检查是否为排除路径（使用 AntPathMatcher 精确匹配，避免 startsWith 匹配过宽）
     */
    private boolean isExcludedPath(String requestURI) {
        for (String excludeUrl : EXCLUDE_URLS) {
            if (PATH_MATCHER.match(excludeUrl, requestURI)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在响应头中设置CSRF Token（仅对已登录用户）
     */
    private void setCsrfTokenHeader(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        // 只有已登录用户才生成CSRF Token
        if (session.getAttribute("employee") == null && session.getAttribute("user") == null) {
            return;
        }

        String token = (String) session.getAttribute(CSRF_TOKEN_KEY);
        if (token == null) {
            // 生成新的CSRF Token
            token = generateCsrfToken();
            session.setAttribute(CSRF_TOKEN_KEY, token);
            log.debug("为用户生成新的CSRF Token");
        }

        // 设置响应头
        response.setHeader(CSRF_HEADER_NAME, token);
    }

    /**
     * 生成CSRF Token
     */
    private String generateCsrfToken() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 获取当前用户的CSRF Token（供Controller调用）
     */
    public static String getCsrfToken(HttpSession session) {
        if (session == null) {
            return null;
        }
        String token = (String) session.getAttribute(CSRF_TOKEN_KEY);
        if (token == null) {
            java.security.SecureRandom random = new java.security.SecureRandom();
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CSRF_TOKEN_KEY, token);
        }
        return token;
    }
}