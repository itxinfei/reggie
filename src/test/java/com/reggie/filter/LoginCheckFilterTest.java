package com.reggie.filter;

import com.reggie.common.BaseContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * LoginCheckFilter 单元测试（不启动 Spring 容器）。
 *
 * <p>MockMvc 全链路已在测试侧移除 LoginCheckFilter（见 MockMvcFilterExclusionAutoConfiguration），
 * 故过滤器自身"未登录→401 / 白名单放行 / 会话登录态校验"的行为在此独立覆盖。
 * 使用 spring-test 的 MockHttpServletRequest/Response + Mockito 模拟过滤器链。</p>
 *
 * @author reggie
 * @since 2026-10-02
 */
class LoginCheckFilterTest {

    private final LoginCheckFilter filter = new LoginCheckFilter();

    @AfterEach
    void tearDown() {
        BaseContext.remove();
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setRequestURI(uri);
        return req;
    }

    @Test
    @DisplayName("受保护路径无会话：401 NOTLOGIN，且不放行")
    void protectedPath_withoutSession_returns401() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request("GET", "/employee/page"), resp, chain);

        assertEquals(401, resp.getStatus());
        assertTrue(resp.getContentAsString().contains("NOTLOGIN"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("登录白名单端点匿名访问：放行")
    void loginEndpoint_anonymous_passesThrough() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request("POST", "/user/login"), resp, chain);

        verify(chain).doFilter(any(), any());
        assertEquals(200, resp.getStatus());
    }

    @Test
    @DisplayName("员工会话（含租户）：放行并写入员工身份 request 属性")
    void employeeSession_passesThrough() throws Exception {
        MockHttpServletRequest req = request("GET", "/employee/me");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("employee", 990001L);
        session.setAttribute("tenantId", 999L);
        req.setSession(session);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        assertEquals(990001L, req.getAttribute("employeeId"));
    }

    @Test
    @DisplayName("用户会话（含租户）：放行")
    void userSession_passesThrough() throws Exception {
        MockHttpServletRequest req = request("GET", "/order/userPage");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", 994001L);
        session.setAttribute("tenantId", 999L);
        req.setSession(session);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("骑手会话（含租户）：放行并写入骑手身份 request 属性")
    void riderSession_passesThrough() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/rider/orders");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("rider", 8001L);
        session.setAttribute("tenantId", 999L);
        req.setSession(session);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        assertEquals(8001L, req.getAttribute("riderId"));
    }

    @Test
    @DisplayName("用户会话缺租户ID：登录态不完整，401 不放行")
    void userSessionWithoutTenantId_returns401() throws Exception {
        MockHttpServletRequest req = request("GET", "/order/userPage");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", 994001L);
        req.setSession(session);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertEquals(401, resp.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}
