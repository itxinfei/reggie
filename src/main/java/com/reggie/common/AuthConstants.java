package com.reggie.common;

/**
 * <p>
 * 认证相关路径常量（域3 改造）。
 * </p>
 *
 * <p>
 * 背景：CsrfFilter 和 LoginCheckFilter 各自维护了 EXCLUDE_URLS 数组，
 * 存在多份重复路径，一旦新增公开接口容易漏改。本类作为唯一来源，
 * 将"CSRF 排除"和"登录排除"两份路径清单集中管理。
 * </p>
 *
 * <p>
 * 注意：两份列表语义不同，不应合并：
 * <ul>
 *     <li>CSRF 排除列表 — 仅需排除匿名接口（登录/注册/文档），已登录用户的 POST 仍需校验</li>
 *     <li>登录排除列表 — 需排除所有公开可访问的接口（含菜品浏览、推荐等匿名 GET）</li>
 * </ul>
 * </p>
 *
 * @author AI
 * @since 2026-08-22
 */
public final class AuthConstants {

    private AuthConstants() {
        throw new AssertionError();
    }

    /**
     * 不需要 CSRF 校验的路径（仅匿名接口 + 公开资源）
     * 使用 Ant 通配符
     */
    public static final String[] CSRF_EXCLUDE_URLS = new String[]{
        "/employee/login",
        "/employee/logout",
        "/employee/forgot-password",
        "/user/sendMsg",
        "/user/login",
        "/user/loginout",
        "/tenant/register",
        // 注意：/api/ai/** 已从 CSRF 排除列表中移除（2026-08-23 安全加固）
        // AI 模块的写操作接口（/api/ai/chat, /api/ai/session/* 等）需要 CSRF 防护，
        // 仅保留 /api/ai/health 在 LOGIN_EXCLUDE 中作为匿名健康检查
        "/swagger-ui",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/actuator",
        "/actuator/**",
        "/doc.html",
        "/webjars/**"
    };

    /**
     * 不需要登录校验的路径（公开接口 + 静态资源 + 文档）
     * 使用 Ant 通配符
     */
    public static final String[] LOGIN_EXCLUDE_URLS = new String[]{
        // 登录/登出/忘记密码接口（匿名访问）
        "/employee/login",
        "/employee/logout",
        "/employee/forgot-password",
        "/user/sendMsg",
        "/user/login",
        "/user/loginout",
        "/tenant/register",
        // 公开的商家信息接口（首页匿名访问）
        "/restaurant/info",
        "/restaurant/status",
        // AI模块健康检查（匿名访问）
        "/api/ai/health",
        // 公开菜品/套餐接口（C端菜单浏览）
        "/category/list",
        "/category/options",
        "/dish/list",
        "/dish/options",
        "/setmeal/list",
        "/setmeal/options",
        // 推荐模块公开接口
        "/recommend/dishes",
        "/recommend/hot",
        "/recommend/new-arrivals",
        "/recommend/setmeals",
        // 静态资源目录（图片、上传文件）
        "/images/**",
        "/uploads/**",
        // 前端静态资源（后台管理系统和用户端）
        "/backend/**",
        "/front/**",
        // API文档相关路径
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-ui/",
        "/v3/api-docs/**",
        "/v3/api-docs",
        "/swagger-resources/**",
        "/webjars/**",
        "/doc.html",
        // Spring Boot Actuator 监控端点（仅允许健康检查和基本信息）
        "/actuator/health",
        "/actuator/info",
        // 公共资源接口（文件上传预览）
        "/common/download",
        "/common/download/**"
    };
}