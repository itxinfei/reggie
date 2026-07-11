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
 * 检查用户是否已经完成登录
 */
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter{
    /** 路径匹配器，支持通配符 */
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** JSON序列化工具 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            Long tenantId = null;

            //1、获取本次请求的URI
            String requestURI = request.getRequestURI();

            log.info("拦截到请求：{}", requestURI);

            //定义不需要处理的请求路径
            String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login",
                "/tenant/register",
                // 放行前端浏览菜单相关API
                "/category/list",
                "/dish/list",
                "/setmeal/list",
                "/setmeal/dish/**",
                // 放行推荐模块公开API
                "/recommend/dishes",
                "/recommend/hot",
                "/recommend/new-arrivals",
                "/recommend/setmeals",
                // 修改点：放行公开的商家信息接口（首页匿名访问）
                "/restaurant/info",
                "/restaurant/status",
                // 放行AI模块的公开API（健康检查、对话同步等允许匿名访问）
                "/api/ai/health",
                "/api/ai/conversations",
                // 修改点：放行静态资源目录，避免图片/上传文件被拦截导致死循环请求
                "/images/**",
                "/uploads/**",
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

            //2、判断本次请求是否需要处理
            boolean check = check(urls, requestURI);

            //3、如果不需要处理，则直接放行
            if (check) {
                log.info("本次请求{}不需要处理", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            //4-1、判断登录状态，如果已登录，则直接放行
            if (request.getSession().getAttribute("employee") != null) {
                log.info("用户已登录，用户id为：{}", request.getSession().getAttribute("employee"));

                Long empId = (Long) request.getSession().getAttribute("employee");
                tenantId = (Long) request.getSession().getAttribute("tenantId");
                String roleKey = (String) request.getSession().getAttribute("roleKey");
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
                log.info("用户已登录，用户id为：{}", request.getSession().getAttribute("user"));

                Long userId = (Long) request.getSession().getAttribute("user");
                tenantId = (Long) request.getSession().getAttribute("tenantId");
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
