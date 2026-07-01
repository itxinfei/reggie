package com.reggie.enums;

import lombok.Getter;

/**
 * 优惠券状态枚举
 */
@Getter
public enum CouponStatus {

    UNUSED("UNUSED", "未使用"),
    USED("USED", "已使用"),
    EXPIRED("EXPIRED", "已过期");

    private final String value;
    private final String desc;

    CouponStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
