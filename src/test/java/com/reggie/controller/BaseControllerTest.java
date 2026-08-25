package com.reggie.controller;

import com.reggie.common.CsrfTokenUtil;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Controller 测试基类：提供 CSRF Token 辅助方法
 *
 * CsrfFilter 对 POST/PUT/DELETE/PATCH 请求要求携带有效的 CSRF Token。
 * 测试类继承此基类后，在 POST/PUT/DELETE 请求中调用 {@link #withCsrfToken(MockMvc, MockHttpServletRequestBuilder)}
 * 即可自动获取 Session 中的 Token 并添加到请求头，绕过 CSRF 校验。
 *
 * 使用示例：
 * <pre>
 * mockMvc.perform(withCsrfToken(mockMvc, post("/address-book")
 *     .sessionAttr("user", 1L)
 *     .contentType(MediaType.APPLICATION_JSON)
 *     .content(json)))
 *     .andExpect(status().isOk());
 * </pre>
 *
 * @since 2026-08-25
 */
public abstract class BaseControllerTest {

    /** Session 中 CSRF Token 的 key，与 CsrfFilter.CSRF_TOKEN_KEY 保持一致 */
    protected static final String CSRF_TOKEN_SESSION_KEY = "csrfToken";

    /** 请求头中 CSRF Token 的 key，与 CsrfFilter.CSRF_HEADER_NAME 保持一致 */
    protected static final String CSRF_HEADER_NAME = "X-CSRF-Token";

    /**
     * 为 MockMvc 请求添加有效的 CSRF Token。
     *
     * 工作原理：
     * 1. 用 CsrfTokenUtil.generateToken() 生成合法 Token
     * 2. 通过 MockHttpServletRequestBuilder 的 sessionAttr 将其存入 Session
     * 3. 同时添加到 X-CSRF-Token 请求头
     *
     * CsrfFilter 验证逻辑：从 Session 读取 sessionToken，从请求头读取 requestToken，
     * 两者一致且未过期则通过。
     *
     * @param mockMvc  MockMvc 实例（此参数仅为占位，保持调用一致性）
     * @param request  MockHttpServletRequestBuilder
     * @return 添加了 CSRF Token 的 MockHttpServletRequestBuilder
     */
    @SuppressWarnings("unused")
    protected static MockHttpServletRequestBuilder withCsrfToken(
            MockMvc mockMvc, MockHttpServletRequestBuilder request) {
        String token = CsrfTokenUtil.generateToken();
        return request
                .sessionAttr(CSRF_TOKEN_SESSION_KEY, token)
                .header(CSRF_HEADER_NAME, token);
    }

    /**
     * 便捷方法：仅生成一个有效的 CSRF Token（供需要手动设置的测试使用）。
     */
    @SuppressWarnings("unused")
    protected static String generateCsrfToken() {
        return CsrfTokenUtil.generateToken();
    }
}