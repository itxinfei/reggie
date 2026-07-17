package com.reggie.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
public class LoginCheckFilter implements Filter{
    /** 路径匹配器，支持通配符 */
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** JSON序列化工具 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 不需要处理的请求路径（唯一来源，避免重复维护） */
    private static final String[] EXCLUDE_URLS = new String[]{
        "/employee/login",
        "/employee/logout",
        // 修改点：放行忘记密码接口（匿名访问，无需登录）
        "/employee/forgot-password",
        "/backend/**",
        "/front/**",
        "/common/**",
        "/user/sendMsg",
        "/user/login",
        "/tenant/register",
        // 修改点：放行公开的商家信息接口（首页匿名访问）
        "/restaurant/info",
        "/restaurant/status",
        // 放行AI模块的公开API（健康检查允许匿名访问）
        "/api/ai/health",
        // 修改点：放行静态资源目录，避免图片/上传文件被拦截导致死循环请求
        "/images/**",
        "/uploads/**",
        // 放行C端菜单浏览（允许未登录用户查看菜品和套餐列表）
        "/category/list",
        "/category/options",
        "/dish/list",
        "/dish/options",
        "/setmeal/list",
        "/setmeal/options",
        // 放行推荐模块公开接口（未登录返回热门菜品）
        "/recommend/dishes",
        "/recommend/hot",
        "/recommend/new-arrivals",
        "/recommend/setmeals",
        // 放行API文档相关路径
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-ui/",
        "/v3/api-docs/**",
        "/v3/api-docs",
        "/swagger-resources/**",
        "/webjars/**",
        "/doc.html"
    };

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
                filterChain.doFilter(request, response);
                return;
            }

            //4-1、判断登录状态，如果已登录，则直接放行
            if (request.getSession().getAttribute("employee") != null) {
                log.debug("员工已登录，用户id为：{}", request.getSession().getAttribute("employee"));

                Long empId = (Long) request.getSession().getAttribute("employee");
                tenantId = (Long) request.getSession().getAttribute("tenantId");
                // 兼容测试环境：session 中无 tenantId 时从 BaseContext 获取
                if (tenantId == null) {
                    tenantId = BaseContext.getCurrentTenantId();
                }
                String roleKey = (String) request.getSession().getAttribute("roleKey");
                // 必须同时有员工ID和租户ID才算登录有效
                if (tenantId == null) {
                    log.warn("员工登录态不完整，tenantId为null");
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
            if (request.getSession().getAttribute("user") != null) {
                log.debug("用户已登录，用户id为：{}", request.getSession().getAttribute("user"));

                Long userId = (Long) request.getSession().getAttribute("user");
                tenantId = (Long) request.getSession().getAttribute("tenantId");
                // 必须同时有用户ID和租户ID才算登录有效
                if (tenantId == null) {
                    log.warn("用户登录态不完整，tenantId为null");
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
            //5、如果未登录则返回未登录结果，通过输出流方式向客户端页面响应数据
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
