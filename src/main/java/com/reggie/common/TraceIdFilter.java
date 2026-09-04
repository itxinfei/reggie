package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import javax.servlet.FilterChain;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 过滤器，为每个 HTTP 请求生成/传播唯一的追踪 ID，
 * 注入 MDC 供 SLF4J 日志使用，同步写入 BaseContext 供业务层查询。
 *
 * 工作原理：
 * 1. 优先从请求头 X-Trace-Id 读取客户端传入的 traceId（支持网关/链路透传）
 * 2. 若请求头为空，生成 UUID 作为本请求 traceId
 * 3. 设置到 MDC("traceId") + 响应头 X-Trace-Id（返回给调用方）+ BaseContext
 * 4. finally 中清理 MDC 与 BaseContext，防止线程池内存泄漏
 *
 * 执行顺序（@Order 越小越靠前）：
 *  1. TraceIdFilter            (HIGHEST_PRECEDENCE + 1) — 注入 traceId
 *  2. CsrfFilter               (@Order(1))              — CSRF 校验
 *  3. LoginCheckFilter         (无 @Order，字母序)      — 登录态校验
 *
 * 修复记录：此前该类只有 @Order 没有 @WebFilter，未被 ServletComponentScan
 * 扫描注册，是死代码，MDC traceId 与响应头 X-Trace-Id 完全不生效。
 * 现补 @WebFilter 使全链路 traceId 生效（可观测性）。
 *
 * @author AI
 * @since 2026-08-22
 */
@Slf4j
@WebFilter(filterName = "traceIdFilter", urlPatterns = "/*", asyncSupported = true)
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceIdFilter implements Filter {

    /** 请求头名称 */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** MDC key */
    private static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain filterChain) throws IOException, ServletException {
        doFilterInternal((HttpServletRequest) request, (HttpServletResponse) response, filterChain);
    }

    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        try {
            MDC.put(MDC_TRACE_ID_KEY, traceId);
            BaseContext.setCurrentTraceId(traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);

            log.debug("TraceIdFilter 注入 traceId: {}", traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
            BaseContext.removeTraceId();
        }
    }

    @Override
    public void destroy() {
        // no-op
    }
}