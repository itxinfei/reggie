package com.reggie.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * 基于 Redis 的滑动窗口算法实现接口限流
 *
 * @author reggie
 * @since 2026-07-09
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
     * 限流窗口时间（秒）
     * 决定滑动窗口的大小，控制 Redis key 的过期时间
     */
    int time() default 1;

    /**
     * 是否启用限流
     */
    boolean enabled() default true;
}
