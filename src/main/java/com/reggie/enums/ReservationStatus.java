package com.reggie.enums;

import lombok.Getter;

/**
 * 预订状态枚举
 */
@Getter
public enum ReservationStatus {

    PENDING("PENDING", "待确认"),
    CONFIRMED("CONFIRMED", "已确认"),
    ARRIVED("ARRIVED", "已到店"),
    CANCELLED("CANCELLED", "已取消");

    private final String value;
    private final String desc;

    ReservationStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
