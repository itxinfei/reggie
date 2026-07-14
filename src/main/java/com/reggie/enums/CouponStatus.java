package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 优惠券状态枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Getter
public enum CouponStatus {

    /** 未使用 */
    UNUSED("UNUSED", "未使用"),
    /** 已使用 */
    USED("USED", "已使用"),
    /** 已过期 */
    EXPIRED("EXPIRED", "已过期");

    private final String value;
    private final String desc;

    CouponStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
