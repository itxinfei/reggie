package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
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
 * @author AI
 * @since 2026-08-22
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceIdFilter extends OncePerRequestFilter {

    /** 请求头名称 */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** MDC key */
    private static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
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
}