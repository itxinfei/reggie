package com.reggie.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * HTTP 安全响应头过滤器
 *
 * 在每次响应中添加安全相关的 HTTP 头，防御 XSS、点击劫持、MIME 类型嗅探等常见 Web 攻击。
 * - Content-Security-Policy: 内容安全策略，限制页面可加载资源的来源（防御 XSS/注入）
 * - X-Content-Type-Options: nosniff，禁止浏览器对响应 MIME 类型进行猜测（防御 MIME 混淆攻击）
 * - X-Frame-Options: SAMEORIGIN，仅允许同源页面嵌入 iframe（防御跨站点点击劫持）
 * - X-XSS-Protection: 1; mode=block，启用浏览器内置 XSS 过滤器（兼容旧版浏览器）
 * - Strict-Transport-Security: 强制浏览器仅通过 HTTPS 访问（仅在生产环境启用）
 * - Referrer-Policy: no-referrer-when-downgrade，控制 Referer 头泄露敏感信息
 *
 * 关于 frame-ancestors / X-Frame-Options：
 * 本项目管理后台与用户端均为 SPA，父页面通过同源 iframe 加载 /backend/page/* 等页面，
 * 因此必须允许同源框架嵌入（SAMEORIGIN / 'self'）。若使用 DENY / 'none' 会封死整个后台页面，
 * 表现为页面打不开并报错 "Framing ... violates frame-ancestors 'none'"。
 * 采用 SAMEORIGIN 仍可阻断任意外部站点以 iframe 嵌入本系统的点击劫持攻击。
 *
 * 执行顺序：@Order(0) 确保在 CsrfFilter(@Order(1)) 之前执行，
 * 使得安全头在所有响应中都能被设置（包括被 CsrfFilter 拒绝的请求）。
 *
 * @author reggie
 * @since 2026-08-27
 */
@Slf4j
@WebFilter(filterName = "securityHeaderFilter", urlPatterns = "/*", asyncSupported = true)
@Order(0) // 最先执行，确保安全头始终被设置
public class SecurityHeaderFilter extends OncePerRequestFilter {

    // ==================== CSP 配置 ====================

    /**
     * Content-Security-Policy 策略值
     *
     * 策略说明：
     * - default-src 'self': 默认只允许同源资源
     * - script-src 'self' 'unsafe-inline' 'unsafe-eval': 允许内联脚本（管理后台使用 Vue2 原生 JS 需要）
     * - style-src 'self' 'unsafe-inline': 允许内联样式（Element-UI 等组件库需要）
     * - img-src 'self' data: https:：允许内联 base64 图片、本地图片及 HTTPS 外部图片
     * - font-src 'self': 仅允许同源字体
     * - connect-src 'self': 仅允许同源 AJAX 请求
     * - frame-ancestors 'self': 仅允许同源页面以 iframe 嵌入（SPA 后台需要，同时阻断跨站点点击劫持）
     *
     * 注意：项目使用 Vue2 + Element-UI（原生 JS，无编译），内联脚本不可避免，
     * 因此保留了 'unsafe-inline' / 'unsafe-eval'。若未来迁移至构建型前端可收紧策略。
     */
    private static final String CSP_VALUE =
            "default-src 'self'; "
                    + "script-src 'self' 'unsafe-inline' 'unsafe-eval'; "
                    + "style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data: https:; "
                    + "font-src 'self'; "
                    + "connect-src 'self'; "
                    + "frame-ancestors 'self'; "
                    + "base-uri 'self'; "
                    + "form-action 'self'";

    // ==================== HSTS 配置 ====================

    /** HSTS max-age: 1 年（秒） */
    private static final long HSTS_MAX_AGE_SECONDS = 31536000L;

    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        log.info("HTTP安全响应头过滤器初始化完成");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 设置所有安全头
        response.setHeader("Content-Security-Policy", CSP_VALUE);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "no-referrer-when-downgrade");

        // HSTS: 仅在非开发环境启用（生产环境强制 HTTPS）
        // getEnvironment() 返回 Spring Environment 对象，读取当前 active profile
        org.springframework.core.env.Environment env = getEnvironment();
        if (env != null) {
            List<String> profiles = Arrays.asList(env.getActiveProfiles());
            if (!profiles.contains("dev")) {
                String hsts = "max-age=" + HSTS_MAX_AGE_SECONDS + "; includeSubDomains";
                response.setHeader("Strict-Transport-Security", hsts);
            }
        }

        filterChain.doFilter(request, response);
    }
}