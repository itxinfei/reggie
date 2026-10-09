package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 外卖配送状态枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Getter
public enum DeliveryOrderStatus {

    /** 待接单 */
    PENDING("PENDING", "待接单"),
    /** 已接单 */
    ACCEPTED("ACCEPTED", "已接单"),
    /** 取餐中 */
    PICKING("PICKING", "取餐中"),
    /** 配送中 */
    DELIVERING("DELIVERING", "配送中"),
    /** 已送达 */
    DELIVERED("DELIVERED", "已送达"),
    /** 已取消 */
    CANCELLED("CANCELLED", "已取消");

    private final String value;
    private final String desc;

    DeliveryOrderStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
