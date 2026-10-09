package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 预订状态枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Getter
public enum ReservationStatus {

    /** 待确认 */
    PENDING("PENDING", "待确认"),
    /** 已确认 */
    CONFIRMED("CONFIRMED", "已确认"),
    /** 已到店 */
    ARRIVED("ARRIVED", "已到店"),
    /** 已取消 */
    CANCELLED("CANCELLED", "已取消");

    private final String value;
    private final String desc;

    ReservationStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
