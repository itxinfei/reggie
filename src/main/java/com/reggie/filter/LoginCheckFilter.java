package com.reggie.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.AuthConstants;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.delivery.model.RiderRememberToken;
import com.reggie.module.delivery.service.RiderRememberTokenService;
import com.reggie.module.store.service.StoreService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * <p>
 * 登录校验过滤器，拦截所有请求检查用户登录状态
 * </p>
 * <p>
 * 支持两种登录态：员工登录（session 中存 employee）和用户登录（session 中存 user）。
 * 配置了排除路径列表，对不需要登录即可访问的路径直接放行。
 * 登录成功后，将员工ID、租户ID、角色标识存入 ThreadLocal（BaseContext）和 request 属性，
 * 供后续业务层和 AOP 权限拦截器使用。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*", asyncSupported = true)
@Slf4j
@Order(3) // 在 CsrfFilter(@Order(1)) 之后，保证先过 CSRF 再过登录校验
public class LoginCheckFilter implements Filter{
    /** 路径匹配器，支持通配符 */
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** JSON序列化工具 */
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();

    /** 不需要处理的请求路径（引用 AuthConstants，保持单一来源） */
    private static final String[] EXCLUDE_URLS = AuthConstants.LOGIN_EXCLUDE_URLS;

    /**
     * 处理 do filter。
     * @param servletRequest 参数 servletRequest
     * @param servletResponse 参数 servletResponse
     * @param filterChain 参数 filterChain
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            //1、获取本次请求的URI
            String requestURI = request.getRequestURI();

            log.debug("拦截到请求：{}", requestURI);

            //2、判断本次请求是否需要处理（使用唯一的 EXCLUDE_URLS 常量）
            //3、如果不需要处理，则直接放行（仍尽力恢复已有登录态上下文，保证租户隔离）
            if (check(EXCLUDE_URLS, requestURI)) {
                log.debug("本次请求{}不需要处理", requestURI);
                restoreExcludeContext(request);
                filterChain.doFilter(request, response);
                return;
            }

            //4-1、判断登录状态，如果已登录，则直接放行
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("employee") != null) {
                if (applyEmployeeContext(session, request, response)) {
                    filterChain.doFilter(request, response);
                }
                return;
            }

            //4-2、判断登录状态，如果已登录，则直接放行
            if (session != null && session.getAttribute("user") != null) {
                if (applyUserContext(session, response)) {
                    filterChain.doFilter(request, response);
                }
                return;
            }

            //4-3、判断骑手会话，如果已登录，则直接放行
            if (session != null && session.getAttribute("rider") != null) {
                if (applyRiderContext(session, request, response)) {
                    filterChain.doFilter(request, response);
                }
                return;
            }

            //4-4、骑手会话已过期时，凭「记住登录」cookie 自动恢复会话（cookie 30天有效）
            HttpSession autoSession = autoLoginByRememberCookie(request);
            if (autoSession != null) {
                if (applyRiderContext(autoSession, request, response)) {
                    filterChain.doFilter(request, response);
                }
                return;
            }

            log.info("用户未登录");
            //5、如果未登录则返回未登录结果：HTTP 401 + JSON 体 NOTLOGIN
            //   （此前仅返回 200 + NOTLOGIN，前端按 body 判断；补 401 让标准客户端/网关/监控
            //    能识别未授权，且与 CommonController.download 的 SC_UNAUTHORIZED 行为一致）
            writeNotLogin(response);
        } finally {
            BaseContext.remove();
        }
    }

    /**
     * 放行分支：EXCLUDE_URLS 中的公开接口虽不需登录，但按以下优先级恢复租户上下文，
     * 保证数据按租户隔离（R-21-A 公开端点租户解析）：
     * <ol>
     *   <li>会话已有登录态（employee/user + tenantId）→ 按会话恢复；</li>
     *   <li>匿名 → 请求参数解析（?tenantId= 或 ?storeId=，storeId 经门店档案反查租户）；</li>
     *   <li>无参数 → 配置 reggie.public-tenant-id（单租户自托管默认租户兜底）。</li>
     * </ol>
     * 三级均不可得时保持原状（不设上下文，由租户插件 fail-closed 兜底），零破坏升级。
     *
     * @param request 请求
     */
    private void restoreExcludeContext(HttpServletRequest request) {
        HttpSession excludeSession = request.getSession(false);
        Long excludeTenantId = excludeSession == null ? null : (Long) excludeSession.getAttribute("tenantId");
        if (excludeTenantId == null) {
            excludeTenantId = resolvePublicTenantId(request);
            if (excludeTenantId == null) {
                return;
            }
            BaseContext.setCurrentTenantId(excludeTenantId);
            return;
        }
        BaseContext.setCurrentTenantId(excludeTenantId);
        Object empId = excludeSession.getAttribute("employee");
        Object userId = excludeSession.getAttribute("user");
        if (empId != null) {
            BaseContext.setCurrentId((Long) empId);
        } else if (userId != null) {
            BaseContext.setCurrentId((Long) userId);
        }
    }

    /**
     * 公开端点租户解析（R-21-A 三级回落的后两级，仅匿名公开请求会走到这里）。
     * <ol>
     *   <li>请求参数 tenantId（显式租户）；</li>
     *   <li>请求参数 storeId → store_info 主键反查所属租户（模式同堂食"桌台反查门店"），
     *       无会话查库须走 {@code @InterceptorIgnore} 跨租户方法；</li>
     *   <li>配置 reggie.public-tenant-id（默认公开租户；未配置=不启用）。</li>
     * </ol>
     *
     * @param request 请求
     * @return 租户 ID；无法解析时 null（调用方保持无上下文现状）
     */
    private Long resolvePublicTenantId(HttpServletRequest request) {
        WebApplicationContext context = WebApplicationContextUtils
                .getWebApplicationContext(request.getServletContext());
        if (context == null) {
            return null;
        }
        // 修改点(2026-09-30 代码审查)：?tenantId= 裸参数此前被无条件信任写入租户上下文，
        // 任何人可遍历任意租户（含已禁用）的公开目录数据。现要求该租户真实存在且 status=1
        // （口径同 TenantService.listActiveTenants）；校验不过则忽略本级回落，继续走 storeId 反查。
        Long paramTenantId = parseLongParam(request.getParameter("tenantId"));
        if (paramTenantId != null) {
            if (isPublicTenantUsable(context, paramTenantId)) {
                return paramTenantId;
            }
            log.warn("公开请求 tenantId 参数非法或租户未启用，已忽略该参数: tenantId={}", paramTenantId);
        }
        String storeIdParam = request.getParameter("storeId");
        if (storeIdParam != null && !storeIdParam.isEmpty()) {
            Long storeId = parseLongParam(storeIdParam);
            if (storeId != null) {
                try {
                    Long tenantId = context.getBean(StoreService.class).findTenantIdByStoreId(storeId);
                    if (tenantId != null) {
                        return tenantId;
                    }
                } catch (Exception e) {
                    log.warn("公开请求 storeId 反查租户失败: storeId={}", storeIdParam, e);
                }
            }
        }
        try {
            String defaultTenantId = context.getEnvironment().getProperty("reggie.public-tenant-id");
            if (defaultTenantId != null && !defaultTenantId.trim().isEmpty()) {
                return Long.valueOf(defaultTenantId.trim());
            }
        } catch (NumberFormatException e) {
            log.warn("reggie.public-tenant-id 配置格式错误: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 校验匿名公开请求携带的 tenantId 是否可用：租户必须真实存在且处于启用态（status=1）。
     * <p>{@code tenant} 表无 tenant_id 且已在 {@code MybatisPlusConfig.IGNORE_TABLES}，
     * 故此查询不会被租户插件注入条件；查不到即视为不可用（fail-closed）。</p>
     *
     * @param context  Spring 上下文
     * @param tenantId 待校验租户 ID
     * @return 可用返回 true
     */
    private boolean isPublicTenantUsable(WebApplicationContext context, Long tenantId) {
        try {
            Tenant tenant = context.getBean(TenantService.class).getById(tenantId);
            return tenant != null && Integer.valueOf(1).equals(tenant.getStatus());
        } catch (Exception e) {
            log.warn("公开请求 tenantId 校验异常，按不可用处理: tenantId={}", tenantId, e);
            return false;
        }
    }

    /**
     * 解析 Long 型参数值，非法格式返回 null 并告警（公开参数不可信，失败即放弃该级回落）。
     *
     * @param value 参数原始值
     * @return Long 值，或 null
     */
    private Long parseLongParam(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("公开请求租户解析参数格式错误: {}", value);
            return null;
        }
    }

    /**
     * 应用员工登录上下文。
     *
     * @param session 会话
     * @param request 请求
     * @param response 响应
     * @return true 表示登录态有效已放行；false 表示登录态不完整（已写出 401）
     * @throws IOException 写响应失败
     */
    private boolean applyEmployeeContext(HttpSession session, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.debug("员工已登录，用户id为：{}", session.getAttribute("employee"));
        Long empId = (Long) session.getAttribute("employee");
        Long tenantId = (Long) session.getAttribute("tenantId");
        // 兼容测试环境：session 中无 tenantId 时从 BaseContext 获取
        if (tenantId == null) {
            tenantId = BaseContext.getCurrentTenantId();
        }
        String roleKey = (String) session.getAttribute("roleKey");
        // 必须同时有员工ID和租户ID才算登录有效
        if (tenantId == null) {
            log.warn("员工登录态不完整，tenantId为null");
            writeNotLogin(response);
            return false;
        }
        BaseContext.setCurrentId(empId);
        BaseContext.setCurrentTenantId(tenantId);

        // 将角色标识和员工ID存入request属性，供AOP权限拦截器使用
        request.setAttribute("employeeId", empId);
        request.setAttribute("roleKey", roleKey);
        return true;
    }

    /**
     * 应用用户登录上下文。
     *
     * @param session 会话
     * @param response 响应
     * @return true 表示登录态有效已放行；false 表示登录态不完整（已写出 401）
     * @throws IOException 写响应失败
     */
    private boolean applyUserContext(HttpSession session, HttpServletResponse response) throws IOException {
        log.debug("用户已登录，用户id为：{}", session.getAttribute("user"));
        Long userId = (Long) session.getAttribute("user");
        Long tenantId = (Long) session.getAttribute("tenantId");
        // 必须同时有用户ID和租户ID才算登录有效
        if (tenantId == null) {
            log.warn("用户登录态不完整，tenantId为null");
            writeNotLogin(response);
            return false;
        }
        BaseContext.setCurrentId(userId);
        BaseContext.setCurrentTenantId(tenantId);
        return true;
    }

    /**
     * 应用骑手登录上下文。
     *
     * @param session 会话
     * @param request 请求
     * @param response 响应
     * @return true 表示登录态有效已放行；false 表示登录态不完整（已写出 401）
     * @throws IOException 写响应失败
     */
    private boolean applyRiderContext(HttpSession session, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.debug("骑手已登录，骑手id为：{}", session.getAttribute("rider"));
        Long riderId = (Long) session.getAttribute("rider");
        Long tenantId = (Long) session.getAttribute("tenantId");
        // 必须同时有骑手ID和租户ID才算登录有效
        if (tenantId == null) {
            log.warn("骑手登录态不完整，tenantId为null");
            writeNotLogin(response);
            return false;
        }
        BaseContext.setCurrentId(riderId);
        BaseContext.setCurrentTenantId(tenantId);

        // 将骑手ID存入request属性，供 RiderGuardAspect 鉴权使用
        request.setAttribute("riderId", riderId);
        return true;
    }

    /**
     * 凭「记住登录」cookie 自动恢复骑手会话。
     * <p>{@code @WebFilter} 不由 Spring 管理，通过 WebApplicationContextUtils 取
     * {@code RiderRememberTokenService}；令牌不存在/已过期或容器中无该 bean 时返回 null。</p>
     *
     * @param request 请求
     * @return 写入 rider / tenantId 的新会话；无法自动登录时 null
     */
    private HttpSession autoLoginByRememberCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String tokenValue = null;
        for (Cookie cookie : cookies) {
            if (RiderRememberTokenService.COOKIE_NAME.equals(cookie.getName())) {
                tokenValue = cookie.getValue();
                break;
            }
        }
        if (tokenValue == null || tokenValue.isEmpty()) {
            return null;
        }
        try {
            WebApplicationContext context = WebApplicationContextUtils
                    .getWebApplicationContext(request.getServletContext());
            if (context == null) {
                return null;
            }
            RiderRememberTokenService rememberTokenService =
                    context.getBean(RiderRememberTokenService.class);
            RiderRememberToken rememberToken = rememberTokenService.validate(tokenValue);
            if (rememberToken == null) {
                return null;
            }
            HttpSession autoSession = request.getSession(true);
            autoSession.setAttribute("rider", rememberToken.getRiderId());
            autoSession.setAttribute("tenantId", rememberToken.getTenantId());
            log.info("骑手凭记住登录cookie自动登录：riderId={}", rememberToken.getRiderId());
            return autoSession;
        } catch (Exception e) {
            log.warn("记住登录自动登录失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 写出未登录响应：HTTP 401 + JSON 体 NOTLOGIN。
     *
     * @param response 响应
     * @throws IOException 写响应失败
     */
    private void writeNotLogin(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(R.error("NOTLOGIN")));
    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls,String requestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match){
                return true;
            }
        }
        return false;
    }
}


