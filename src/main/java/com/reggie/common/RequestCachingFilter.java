package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 请求体缓存包装过滤器
 * <p>
 * 目的：将每个请求包装为 {@link RequestCachingWrapper}（基于
 * {@link ContentCachingRequestWrapper} 扩展），使 {@code HttpServletRequest}
 * 的输入流（getInputStream / getReader）可被多次重复读取，每次调用都返回
 * 一个基于缓存字节全新构造的 InputStream。
 * <p>
 * 背景：{@link BruteForceProtectionFilter} 需要从登录请求的 JSON body 中提取
 * username/phone/userAccount 以定位攻击来源，它会消费一次输入流；若不在此处
 * 缓存包装，下游 Controller 的 {@code @RequestBody} 反序列化将读到已关闭的流，
 * 抛出 {@code HttpMessageNotReadableException: Stream closed}。
 * <p>
 * <b>关键点</b>：Spring 原生的 {@link ContentCachingRequestWrapper} 只把字节
 * 缓存进 {@code getContentAsByteArray()}，但 {@code getInputStream()} 返回的是
 * <i>同一个</i> InputStream 实例（字段单例缓存），第二次调用拿到已耗尽的流，
 * 因此 <i>不能</i> 单独靠它修复本问题。本过滤器在此之上再套一层
 * {@link RequestCachingWrapper}，在 getInputStream/getReader 中每次基于缓存字节
 * 构造全新的 InputStream/Reader，使下游每个消费者都能拿到完整 body。
 * <p>
 * 排序：使用 {@code Ordered.HIGHEST_PRECEDENCE} 确保本过滤器在所有会读取请求体的
 * 过滤器（BruteForceProtectionFilter 等）之前执行，先完成包装再流转。
 * <p>
 * 内存：通过 ContentCachingRequestWrapper(HttpServletRequest, int) 两参数构造器
 * 设置缓存上限（256KB），避免超大文件上传导致请求体全量驻留堆内存。
 *
 * @author reggie
 * @since 2026-08-22
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCachingFilter implements Filter {

    /**
     * 请求体最大缓存字节数：256KB。
     * 登录/管理端 JSON 请求体量极小（远小于 1KB），256KB 足以覆盖全部正常场景，
     * 同时对超大文件上传起到内存保护，避免请求体全量驻留堆。
     */
    private static final int MAX_CACHE_SIZE = 256 * 1024;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("请求体缓存包装过滤器初始化，最大缓存 {} KB", MAX_CACHE_SIZE / 1024);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 第一层：ContentCachingRequestWrapper，读取请求体时把字节写入缓存
        // （使用两参数构造器传入缓存上限，超出上限时仅缓存部分字节，避免超大文件撑爆堆）
        ContentCachingRequestWrapper cachingWrapper =
                new ContentCachingRequestWrapper(httpRequest, MAX_CACHE_SIZE);

        // 第二层：RequestCachingWrapper，让每次 getInputStream/getReader 都基于缓存
        // 字节构造全新的 InputStream/Reader，从而可被多个消费者重复读取
        RequestCachingWrapper repeatableWrapper = new RequestCachingWrapper(cachingWrapper);

        chain.doFilter(repeatableWrapper, response);
    }

    @Override
    public void destroy() {
        log.info("请求体缓存包装过滤器销毁");
    }

    // =========================================================================
    // 内部类：使 getInputStream/getReader 每次返回基于缓存的新流
    // =========================================================================

    /**
     * 基于 {@link ContentCachingRequestWrapper} 包装，提供"每次调用 getInputStream
     * / getReader 都返回基于已缓存字节的 InputStream/Reader"的能力。
     * <p>
     * 生命周期：
     * <ol>
     *     <li>首次调用 getInputStream 触发底层 ContentCachingRequestWrapper
     *         完成一次完整读取，字节写入其内部缓存；</li>
     *     <li>后续任何消费者（BruteForceProtectionFilter、@RequestBody 等）调用
     *         getInputStream/getReader 时，均基于该缓存字节构造全新的
     *         ByteArrayInputStream/InputStreamReader；</li>
     *     <li>若缓存尚未填充（例如某消费者绕过了流直接访问参数），先触发一次读取
     *         使缓存填充，再返回基于缓存的新流。</li>
     * </ol>
     */
    private static class RequestCachingWrapper extends ContentCachingRequestWrapper {

        RequestCachingWrapper(ContentCachingRequestWrapper delegate) {
            super(delegate);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ensureCachePopulated();
            return createRepeatableInputStream();
        }

        @Override
        public java.io.BufferedReader getReader() throws IOException {
            ensureCachePopulated();
            String enc = getCharacterEncoding();
            byte[] cached = getContentAsByteArray();
            InputStream is = new ByteArrayInputStream(cached != null ? cached : new byte[0]);
            if (enc != null && !enc.isEmpty()) {
                return new java.io.BufferedReader(new java.io.InputStreamReader(is, enc));
            }
            return new java.io.BufferedReader(new java.io.InputStreamReader(is));
        }

        /**
         * 确保底层缓存已填充：若缓存为空，通过读取底层 getInputStream() 触发填充。
         */
        private void ensureCachePopulated() throws IOException {
            byte[] cached = getContentAsByteArray();
            if (cached != null && cached.length > 0) {
                return;
            }
            // 触发一次完整读取，让字节写入 ContentCachingRequestWrapper 的缓存
            ServletInputStream is = super.getInputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                // 消费到 EOF，触发缓存填充
            }
        }

        /**
         * 基于已填充的缓存字节构造全新的 InputStream，每次调用都是独立流，
         * 可被不同消费者重复读取。
         */
        private ServletInputStream createRepeatableInputStream() {
            byte[] cached = getContentAsByteArray();
            byte[] data = cached != null ? cached : new byte[0];
            final ByteArrayInputStream bais = new ByteArrayInputStream(data);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // 同步 InputStream，无需异步 ReadListener
                }

                @Override
                public int read() throws IOException {
                    return bais.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    return bais.read(b, off, len);
                }
            };
        }
    }
}