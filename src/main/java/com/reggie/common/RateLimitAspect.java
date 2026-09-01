package com.reggie.common;

import com.reggie.common.BaseContext;
import com.reggie.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * 限流切面
 * 基于 Redis 滑动窗口算法实现接口限流
 * 使用Lua脚本保证increment+expire的原子性
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    /**
     * Redis操作模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 是否启用限流
     */
    private final boolean enabled;

    /**
     * Redis 瞬态故障熔断标记：Redis 异常时降级放行（限流失效），
     * 避免每次请求都重复打印 error 日志。熔断持续 30s 后自动恢复探测。
     */
    private volatile long redisCircuitOpenUntil = 0;

    /**
     * 熔断持续时间（毫秒）
     */
    private static final long CIRCUIT_OPEN_DURATION_MS = 30_000L;

    /**
     * 匿名用户标识
     */
    private static final String ANONYMOUS_USER = "anonymous";

    /**
     * Lua脚本：原子性的increment+expire操作
     * KEYS[1] = 限流Key
     * ARGV[1] = 过期时间（秒）
     * 返回值 = increment后的计数值
     */
    private DefaultRedisScript<Long> rateLimitScript;

    /**
     * 构造方法，注入RedisTemplate
     *
     * @param redisTemplate Redis操作模板，可选依赖
     */
    @Autowired
    public RateLimitAspect(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.enabled = redisTemplate != null;
        if (enabled) {
            log.info("API限流已启用（Redis模式）");
        } else {
            log.info("API限流未启用（Redis不可用），已降级");
        }
    }

    /**
     * 初始化Lua脚本
     */
    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(
                new org.springframework.core.io.ClassPathResource("scripts/rate_limit.lua")));
        rateLimitScript.setResultType(Long.class);
    }

    /**
     * 检查限流是否启用
     *
     * @return true=启用，false=禁用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 限流异常，用于区分 Redis 连接异常
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) { super(message); }
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
            // 使用Lua脚本原子性执行increment+expire
            Long count = redisTemplate.execute(rateLimitScript,
                    Collections.singletonList(limitKey),
                    rateLimit.time());

            // 判断是否超过限流阈值
            if (count != null && count > rateLimit.maxRequestsPerSecond()) {
                log.warn("接口限流触发 - 请求数：{}/{}，Key：{}",
                    count, rateLimit.maxRequestsPerSecond(), limitKey);
                throw new RateLimitExceededException("请求过于频繁，请稍后重试");
            }

            // 放行
            return point.proceed();
        } catch (RateLimitExceededException e) {
            // 限流命中：直接向上抛出，由 GlobalExceptionHandler 返回 429
            throw e;
        } catch (Exception e) {
            // Redis 异常降级：瞬态故障熔断（fail-open），
            // 30s 内静默放行降低噪声，之后恢复探测；异常首次发生时打 error 便于定位。
            long now = System.currentTimeMillis();
            if (now < redisCircuitOpenUntil) {
                return point.proceed();
            }
            redisCircuitOpenUntil = now + CIRCUIT_OPEN_DURATION_MS;
            log.error("限流检查异常（Redis连接问题），已降级放行并熔断30s（限流临时失效）：{}",
                    e.getMessage(), e);
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
                // 从BaseContext获取真实用户ID
                Long userId = BaseContext.getCurrentId();
                keyBuilder.append(userId != null ? userId : ANONYMOUS_USER);
                break;
            case GLOBAL:
                keyBuilder.append("global");
                break;
            default:
                keyBuilder.append("unknown");
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
            String ip = SpringUtils.getClientIp(request);
            return (ip != null && !ip.isEmpty()) ? ip : "unknown";
        }
        return "unknown";
    }
}
