package com.reggie.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.AuthConstants;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
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

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            Long tenantId = null;

            //1、获取本次请求的URI
            String requestURI = request.getRequestURI();

            log.debug("拦截到请求：{}", requestURI);

            //2、判断本次请求是否需要处理（使用唯一的 EXCLUDE_URLS 常量）
            boolean check = check(EXCLUDE_URLS, requestURI);

            //3、如果不需要处理，则直接放行
            if (check) {
                log.debug("本次请求{}不需要处理", requestURI);
                // EXCLUDE_URLS 中的公开接口（如 C 端浏览、推荐、后台公共数据）虽不需登录，
                // 但若当前会话已有登录态（employee/user + tenantId），仍恢复上下文，
                // 保证数据按租户隔离；匿名请求无会话则跳过（由租户插件 fail-open 处理）。
                HttpSession excludeSession = request.getSession(false);
                if (excludeSession != null) {
                    Long excludeTenantId = (Long) excludeSession.getAttribute("tenantId");
                    if (excludeTenantId != null) {
                        BaseContext.setCurrentTenantId(excludeTenantId);
                        Object empId = excludeSession.getAttribute("employee");
                        Object userId = excludeSession.getAttribute("user");
                        if (empId != null) {
                            BaseContext.setCurrentId((Long) empId);
                        } else if (userId != null) {
                            BaseContext.setCurrentId((Long) userId);
                        }
                    }
                }
                filterChain.doFilter(request, response);
                return;
            }

            //4-1、判断登录状态，如果已登录，则直接放行
            HttpSession session = request.getSession(false);

            if (session != null && session.getAttribute("employee") != null) {
                log.debug("员工已登录，用户id为：{}", session.getAttribute("employee"));

                Long empId = (Long) session.getAttribute("employee");
                tenantId = (Long) session.getAttribute("tenantId");
                // 兼容测试环境：session 中无 tenantId 时从 BaseContext 获取
                if (tenantId == null) {
                    tenantId = BaseContext.getCurrentTenantId();
                }
                String roleKey = (String) session.getAttribute("roleKey");
                // 必须同时有员工ID和租户ID才算登录有效
                if (tenantId == null) {
                    log.warn("员工登录态不完整，tenantId为null");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(OBJECT_MAPPER.writeValueAsString(R.error("NOTLOGIN")));
                    return;
                }
                BaseContext.setCurrentId(empId);
                BaseContext.setCurrentTenantId(tenantId);

                // 将角色标识和员工ID存入request属性，供AOP权限拦截器使用
                request.setAttribute("employeeId", empId);
                request.setAttribute("roleKey", roleKey);

                filterChain.doFilter(request, response);
                return;
            }

            //4-2、判断登录状态，如果已登录，则直接放行
            if (session != null && session.getAttribute("user") != null) {
                log.debug("用户已登录，用户id为：{}", session.getAttribute("user"));

                Long userId = (Long) session.getAttribute("user");
                tenantId = (Long) session.getAttribute("tenantId");
                // 必须同时有用户ID和租户ID才算登录有效
                if (tenantId == null) {
                    log.warn("用户登录态不完整，tenantId为null");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(OBJECT_MAPPER.writeValueAsString(R.error("NOTLOGIN")));
                    return;
                }
                BaseContext.setCurrentId(userId);
                BaseContext.setCurrentTenantId(tenantId);

                filterChain.doFilter(request, response);
                return;
            }

            log.info("用户未登录");
            //5、如果未登录则返回未登录结果：HTTP 401 + JSON 体 NOTLOGIN
            //   （此前仅返回 200 + NOTLOGIN，前端按 body 判断；补 401 让标准客户端/网关/监控
            //    能识别未授权，且与 CommonController.download 的 SC_UNAUTHORIZED 行为一致）
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(R.error("NOTLOGIN")));
        } finally {
            BaseContext.remove();
        }
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


