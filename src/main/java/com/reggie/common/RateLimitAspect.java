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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
     * Redis 瞬态故障熔断标记：Redis 异常时降级到本地内存计数（见 localWindow），
     * 而不是静默放行——避免 Redis 故障时限流彻底失效（短信验证码可被爆破、资金接口可被高频打）。
     */
    private volatile long redisCircuitOpenUntil = 0;

    /**
     * 熔断持续时间（毫秒）
     */
    private static final long CIRCUIT_OPEN_DURATION_MS = 30_000L;

    /**
     * 本地内存降级窗口（fail-safe，而非 fail-open）：
     * key = 限流Key，值 = 窗口内的累计请求数。
     * Redis 故障时用 ConcurrentHashMap 兜底计数，保证限流在 Redis 恢复前依然生效；
     * 本地窗口按注解 time 秒滑动，30s 熔断结束后自动回到 Redis。
     * 注：本地降级是进程内近似计数（多实例部署时各实例独立），仅作兜底，
     * 精度不及 Redis 滑动窗口，但绝不允许"Redis 挂了直接放行"。
     */
    private final ConcurrentHashMap<String, LocalWindow> localWindow = new ConcurrentHashMap<>();

    /**
     * 本地内存窗口：进入窗口时记 count=1 并记入窗时间，随后按 limit 上限/时间窗判定。
     */
    static class LocalWindow {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStartMs;
    }

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

        // 限流检查：try 块仅包裹 Redis 调用，业务方法 point.proceed() 在下方独立调用。
        // 历史 Bug（P0）：若把 point.proceed() 放进 try，业务异常会被 catch(Exception)
        // 误判为 Redis 故障，导致业务方法被再次执行（收银入账/下单扣库存等非幂等操作
        // 会重复执行），且误开全局 30s 熔断窗口。修复后业务异常原样上抛。
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
        } catch (RateLimitExceededException e) {
            // 限流命中：直接向上抛出，由 GlobalExceptionHandler 返回 429
            throw e;
        } catch (Exception e) {
            // Redis 异常：降级到本地内存计数（fail-safe 而非 fail-open）。
            // 首次异常打 error 便于定位；随后 30s 熔断期走本地窗口，仍限制频率，
            // 30s 后自动恢复 Redis 探测。业务方法在 try 块外仅执行一次。
            long now = System.currentTimeMillis();
            if (now >= redisCircuitOpenUntil) {
                redisCircuitOpenUntil = now + CIRCUIT_OPEN_DURATION_MS;
                log.error("限流检查异常（Redis连接问题），降级本地内存限流30s：{}",
                        e.getMessage(), e);
            }
            // 本地窗口计数判定：窗口按 rateLimit.time() 秒滑动，窗口起始时间取首次计数时刻，
            // 超过 time 秒则重置窗口；否则累计。命中即抛 429，绝不静默放行。
            long windowMs = (long) rateLimit.time() * 1000L;
            long ts = System.currentTimeMillis();
            LocalWindow lw = localWindow.computeIfAbsent(limitKey, k -> {
                LocalWindow w = new LocalWindow();
                w.windowStartMs = ts;
                return w;
            });
            int localCount = lw.count.incrementAndGet();
            if (ts - lw.windowStartMs > windowMs) {
                // 窗口过期：重置为当前窗口计数 1
                lw.windowStartMs = ts;
                lw.count.set(1);
                localCount = 1;
            }
            if (localCount > rateLimit.maxRequestsPerSecond()) {
                log.warn("本地降级限流触发 - 请求数：{}/{}，Key：{}",
                        localCount, rateLimit.maxRequestsPerSecond(), limitKey);
                throw new RateLimitExceededException("请求过于频繁，请稍后重试");
            }
        }

        // 放行：执行业务方法。放在 try 块外，业务异常原样上抛，绝不因限流检查而二次执行。
        return point.proceed();
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
