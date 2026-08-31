package com.reggie.enums;

import java.util.Arrays;

/**
 * <p>
 * 订单状态枚举（与 {@link com.reggie.entity.Orders} 常量对齐）
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
public enum OrderStatus {

    /**
     * 待付款
     */
    PENDING_PAYMENT(1, "待付款"),

    /**
     * 待接单/处理中
     */
    ORDERED(2, "待接单"),

    /**
     * 派送中/已接单
     */
    DELIVERING(3, "配送中"),

    /**
     * 已完成
     */
    COMPLETED(4, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(5, "已取消"),

    /**
     * 已退款
     */
    REFUNDED(6, "已退款"),

    /**
     * 已分账
     */
    SPLIT(7, "已分账");

    private final int value;
    private final String desc;

    OrderStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 获取状态值
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取状态描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 根据数值反向获取枚举
     */
    public static OrderStatus fromCode(int value) {
        return Arrays.stream(values())
                .filter(s -> s.value == value)
                .findFirst()
                .orElse(null);
    }
}
