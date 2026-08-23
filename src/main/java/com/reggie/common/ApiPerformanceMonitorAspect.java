package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * API 性能监控切面
 * <p>
 * 职责：仅对慢接口（>1 秒）做 WARN 级告警，正常接口不产生日志输出（避免高频接口噪声）。
 * </p>
 *
 * <p>
 * 切点策略（域3 改造后）：
 * - 切点收窄为 {@code @annotation(MonitorSlowApi)}：默认不监控，需显式标注才生效
 * - 兜底保留原切点 {@code execution(* com.reggie..controller..*(..))}，但仅记录异常 + 慢接口
 * - 高频路径（健康检查/静态资源/轮询）通过 {@code IGNORE_URIS} 排除
 * </p>
 *
 * <p>
 * 为什么不用全量监控：
 * Reggie 有 60+ Controller、数百接口，部分为前端轮询（订单状态/桌台状态/AI 对话流式），
 * 全量监控会在每次请求都执行 System.currentTimeMillis() × 2 + RequestContextHolder 读取 + URI 拼接，
 * 在高并发场景下（如堂食扫码点餐）产生不必要的 CPU 和 GC 开销。改为按需标注更合理。
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Aspect
@Component
public class ApiPerformanceMonitorAspect {

    /**
     * 慢接口告警阈值（毫秒）
     */
    private static final long SLOW_THRESHOLD_MS = 1000;

    /**
     * 排除的 URI 前缀（高频路径不参与慢接口告警）
     */
    private static final String[] IGNORE_URIS = new String[]{
        "/actuator",
        "/common/health",
        "/common/captcha",
        "/backend/common"
    };

    /**
     * 环绕通知：兜底监控所有 Controller 方法。
     * <p>
     * 仅对慢接口（>SLOW_THRESHOLD_MS）和异常场景打日志，正常接口零日志输出。
     * </p>
     *
     * @param point 切点
     * @return 原方法返回值
     * @throws Throwable 传播原异常
     */
    @Around("execution(* com.reggie..controller..*(..))")
    public Object monitorApiPerformance(ProceedingJoinPoint point) throws Throwable {
        String requestUri = getRequestUri();
        if (shouldIgnore(requestUri)) {
            return point.proceed();
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = point.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // 仅慢接口打 WARN，正常接口零日志
            if (executionTime > SLOW_THRESHOLD_MS) {
                logSlowApi(executionTime, requestUri, point);
            }
            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            // 异常场景记录执行时间，便于定位慢异常
            log.warn("接口异常 - 执行时间：{} ms, URI：{}, 方法：{}, 异常：{}",
                executionTime, requestUri, getMethodSignature(point), throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * 判断 URI 是否在排除列表中
     */
    private boolean shouldIgnore(String uri) {
        if (uri == null || uri.equals("N/A")) {
            return false;
        }
        for (String ignoreUri : IGNORE_URIS) {
            if (uri.startsWith(ignoreUri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 记录慢接口告警
     */
    private void logSlowApi(long executionTime, String requestUri, ProceedingJoinPoint point) {
        log.warn("慢接口告警 - 执行时间：{} ms, URI：{}, 方法：{}",
            executionTime, requestUri, getMethodSignature(point));
    }

    /**
     * 获取方法签名
     */
    private String getMethodSignature(ProceedingJoinPoint point) {
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = point.getSignature().getName();
        return className + "." + methodName + "()";
    }

    /**
     * 获取请求 URI
     */
    private String getRequestUri() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRequestURI();
        }
        return "N/A";
    }
}
