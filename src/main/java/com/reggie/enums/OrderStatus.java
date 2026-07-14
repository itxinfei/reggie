package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 订单状态枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Getter
public enum OrderStatus {

    /**
     * 待付款
     */
    PENDING_PAYMENT(1, "待付款"),

    /**
     * 待接单
     */
    TO_BE_CONFIRMED(2, "待接单"),

    /**
     * 已接单
     */
    CONFIRMED(3, "已接单"),

    /**
     * 派送中
     */
    DELIVERED(4, "派送中"),

    /**
     * 已完成
     */
    COMPLETED(5, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(6, "已取消");

    private final int value;
    private final String desc;

    OrderStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
