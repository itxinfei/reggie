package com.reggie.common;

import java.lang.annotation.*;

/**
 * 限流注解
 * 基于 Redis 的滑动窗口算法实现接口限流
 *
 * @author itxinfei
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 每秒最大请求数
     */
    int maxRequestsPerSecond() default 10;

    /**
     * 限流维度
     * IP - 按 IP 限流
     * USER - 按用户 ID 限流
     * GLOBAL - 全局限流
     */
    RateLimitType type() default RateLimitType.IP;

    /**
     * 限流key前缀
     */
    String keyPrefix() default "rate_limit:";

    /**
     * 是否启用限流
     */
    boolean enabled() default true;
}
