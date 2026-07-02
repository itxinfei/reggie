package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * API 性能监控切面
 * 自动记录所有 Controller 接口的响应时间
 * 慢接口（>1秒）自动告警
 *
 * @author itxinfei
 */
@Slf4j
@Aspect
@Component
public class ApiPerformanceMonitorAspect {

    /**
     * 慢接口阈值（毫秒）
     */
    private static final long SLOW_THRESHOLD_MS = 1000;

    /**
     * 环绕通知：监控所有 Controller 方法
     */
    @Around("execution(* com.reggie.controller..*(..))")
    public Object monitorApiPerformance(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            // 执行目标方法
            Object result = point.proceed();

            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;

            // 记录性能日志
            logPerformance(point, executionTime);

            return result;
        } catch (Throwable throwable) {
            // 异常情况也记录执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("接口异常执行时间：{} ms, 方法：{}, 异常：{}",
                executionTime,
                getMethodSignature(point),
                throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * 记录性能日志
     */
    private void logPerformance(ProceedingJoinPoint point, long executionTime) {
        String methodSignature = getMethodSignature(point);
        String requestUri = getRequestUri();

        if (executionTime > SLOW_THRESHOLD_MS) {
            // 慢接口告警
            log.warn("⚠️ 慢接口告警 - 执行时间：{} ms, URI：{}, 方法：{}",
                executionTime, requestUri, methodSignature);
        } else {
            // 正常性能日志（DEBUG 级别）
            log.debug("✅ 接口性能正常 - 执行时间：{} ms, URI：{}, 方法：{}",
                executionTime, requestUri, methodSignature);
        }
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
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRequestURI();
        }
        return "N/A";
    }
}
