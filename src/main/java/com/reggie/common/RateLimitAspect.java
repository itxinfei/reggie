package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * 基于 Redis 滑动窗口算法实现接口限流
 *
 * @author itxinfei
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean enabled;

    @Autowired
    public RateLimitAspect(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.enabled = redisTemplate != null;
        if (enabled) {
            log.info("API限流已启用（Redis模式）");
        } else {
            log.warn("⚠️ API限流未启用（Redis不可用），自动降级");
        }
    }

    /**
     * 环绕通知：处理限流逻辑
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        // 如果限流未启用或Redis不可用，直接放行
        if (!enabled || !rateLimit.enabled()) {
            return point.proceed();
        }

        // 构建限流 key
        String limitKey = buildLimitKey(point, rateLimit);

        try {
            // 增加计数器
            Long count = redisTemplate.opsForValue().increment(limitKey);

            // 首次设置过期时间（1秒）
            if (count != null && count == 1) {
                redisTemplate.expire(limitKey, 1, TimeUnit.SECONDS);
            }

            // 判断是否超过限流阈值
            if (count != null && count > rateLimit.maxRequestsPerSecond()) {
                log.warn("⚠️ 接口限流触发 - 请求数：{}/{}，Key：{}",
                    count, rateLimit.maxRequestsPerSecond(), limitKey);
                throw new RuntimeException("请求过于频繁，请稍后重试");
            }

            // 放行
            return point.proceed();
        } catch (RuntimeException e) {
            // 限流异常直接抛出
            throw e;
        } catch (Exception e) {
            // Redis 异常降级
            log.error("限流检查异常：{}", e.getMessage());
            return point.proceed();
        }
    }

    /**
     * 构建限流 key
     */
    private String buildLimitKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder(rateLimit.keyPrefix());

        // 添加方法全限定名
        String className = point.getTarget().getClass().getName();
        String methodName = point.getSignature().getName();
        keyBuilder.append(className).append(".").append(methodName).append(":");

        // 根据限流类型添加标识
        switch (rateLimit.type()) {
            case IP:
                String ip = getClientIp();
                keyBuilder.append(ip);
                break;
            case USER:
                // TODO: 从 Session/Token 中获取用户 ID
                keyBuilder.append("anonymous");
                break;
            case GLOBAL:
                keyBuilder.append("global");
                break;
        }

        return keyBuilder.toString();
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return "unknown";
    }

    /**
     * 检查 Redis 是否可用
     */
    private boolean isRedisAvailable() {
        try {
            redisTemplate.opsForValue().get("health_check");
            return true;
        } catch (Exception e) {
            log.warn("Redis 连接异常：{}", e.getMessage());
            return false;
        }
    }
}
