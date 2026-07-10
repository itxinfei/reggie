package com.reggie.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 限流类型枚举
 *
 * @author reggie
 * @since 2026-07-09
 */
@Getter
@AllArgsConstructor
public enum RateLimitType {

    /**
     * 按 IP 限流
     */
    IP("按 IP 限流"),

    /**
     * 按用户 ID 限流
     */
    USER("按用户 ID 限流"),

    /**
     * 全局限流
     */
    GLOBAL("全局限流");

    /**
     * 限流类型描述
     */
    private final String description;
}
