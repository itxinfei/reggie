package com.reggie.enums;

import lombok.Getter;

/**
 * 外卖配送状态枚举
 */
@Getter
public enum DeliveryOrderStatus {

    PENDING("PENDING", "待接单"),
    ACCEPTED("ACCEPTED", "已接单"),
    PICKING("PICKING", "取餐中"),
    DELIVERING("DELIVERING", "配送中"),
    DELIVERED("DELIVERED", "已送达"),
    CANCELLED("CANCELLED", "已取消");

    private final String value;
    private final String desc;

    DeliveryOrderStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
