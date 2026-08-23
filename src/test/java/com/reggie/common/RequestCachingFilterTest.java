package com.reggie.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RequestCachingFilter 单元测试
 * <p>
 * 验证核心机制：经本过滤器包装后，请求输入流可被多次重复读取，
 * 从而避免 BruteForceProtectionFilter 消费流后下游 {@code @RequestBody}
 * 反序列化抛出 {@code Stream closed}。
 * <p>
 * 说明：真实 Bug 依赖 Redis 启用暴力破解过滤器，而 test profile 无 Redis、
 * 过滤器自动降级，故无法用集成测试复现；此处用 Spring 提供的 Mock 对象
 * 直接验证"包装使流可重复读"这一修复机制本身。
 * <p>
 * Spring 5.3.x 的 {@link ContentCachingRequestWrapper} 行为要点：
 * <ul>
 *     <li>第一次 {@code getInputStream()} 返回包装原始流的流，读一次即消费；</li>
 *     <li>读取过程中内容被 {@code ContentCatcher} 同步拷贝进内部缓存；</li>
 *     <li>第二次起 {@code getInputStream()} 基于已填充的缓存构造 InputStream，
 *         因此需要第一次读流<b>读完整个 body</b>，缓存才完整，第二次才能拿到完整内容。</li>
 * </ul>
 * 本测试据此设计：首次调用读取整个 body（模拟 BruteForceProtectionFilter 的行为），
 * 随后再读，应拿到完整内容——这正是修复的目标。
 *
 * @author itxinfei
 */
class RequestCachingFilterTest {

    private RequestCachingFilter filter = new RequestCachingFilter();

    @Test
    void testInitAndDestroy() throws ServletException {
        assertDoesNotThrow(() -> filter.init(new MockFilterConfig()));
        assertDoesNotThrow(filter::destroy);
    }

    /**
     * 核心验证：包装后的请求输入流可被重复读取。
     * 首次读完整 body，第二次读仍拿到完整 JSON，证明 ContentCachingRequestWrapper
     * 的缓存机制生效，修复了 "BruteForceProtectionFilter 消费流 → @RequestBody 读到 Stream closed"。
     */
    @Test
    void testInputStreamCanBeanReadTwice() throws IOException, ServletException {
        filter.init(new MockFilterConfig());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        String json = "{\"username\":\"admin\",\"password\":\"123456\"}";
        request.setContent(json.getBytes("UTF-8"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest req,
                                 javax.servlet.ServletResponse res) throws IOException, ServletException {
                // 收到的 req 是 ContentCachingRequestWrapper（若过滤器未包装，此处将抛出 ClassCastException）
                ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) req;

                // 第一次完整读流（模拟 BruteForceProtectionFilter 读取整个 JSON body）
                byte[] body1 = readAll(wrapper.getInputStream());
                String firstRead = new String(body1, "UTF-8");
                assertEquals(json, firstRead, "第一次读取应得到完整 JSON body");

                // 第二次完整读流（模拟 Controller 的 @RequestBody 反序列化）
                byte[] body2 = readAll(wrapper.getInputStream());
                String secondRead = new String(body2, "UTF-8");
                assertEquals(json, secondRead,
                        "第二次读取仍应得到完整 JSON body，证明流可被重复读取");

                // 额外：通过 getContentAsByteArray() 拿到的缓存也应一致
                byte[] cached = wrapper.getContentAsByteArray();
                assertArrayEquals(body1, cached,
                        "ContentCachingRequestWrapper 内部缓存应与第一次读取内容一致");
            }
        };

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    /**
     * getReader 与 getInputStream 均可在包装后重复使用
     */
    @Test
    void testReaderCanBeanReused() throws IOException, ServletException {
        filter.init(new MockFilterConfig());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        String body = "phone=13800138000";
        request.setContent(body.getBytes("UTF-8"));
        request.setContentType("application/x-www-form-urlencoded");

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest req,
                                 javax.servlet.ServletResponse res) throws IOException, ServletException {
                ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) req;
                // 先通过 reader 完整读一次
                java.io.BufferedReader reader = wrapper.getReader();
                char[] buf = new char[1024];
                int total = 0;
                int n;
                while ((n = reader.read(buf)) != -1) {
                    total += n;
                }
                assertTrue(total > 0, "reader 应读到内容");

                // 再通过 getInputStream 完整读一次
                byte[] body2 = readAll(wrapper.getInputStream());
                assertEquals(body, new String(body2, "UTF-8"),
                        "通过 getInputStream 第二次读仍应拿到完整 body");
            }
        };

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    /**
     * 超大请求体触发缓存上限时，过滤器不应崩溃，请求仍应被正常处理
     */
    @Test
    void testLargeBodyDoesNotCrash() throws IOException, ServletException {
        filter.init(new MockFilterConfig());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            sb.append("x");
        }
        request.setContent(sb.toString().getBytes("UTF-8"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest req,
                                 javax.servlet.ServletResponse res) throws IOException, ServletException {
                ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) req;
                assertDoesNotThrow(() -> {
                    byte[] body = readAll(wrapper.getInputStream());
                    assertNotNull(body);
                    assertTrue(body.length > 0, "应读到请求体内容");
                });
            }
        };

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    // ---------- 工具 ----------

    private byte[] readAll(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }
}