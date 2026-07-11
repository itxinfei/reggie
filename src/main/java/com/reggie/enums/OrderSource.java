package com.reggie.enums;

import lombok.Getter;

/**
 * 订单来源枚举
 */
@Getter
public enum OrderSource {

    /** 外卖配送 */
    TAKEOUT("TAKEOUT", "外卖配送"),
    /** 堂食扫码 */
    EAT_IN("EAT_IN", "堂食扫码"),
    /** 排队取号 */
    QUEUE("QUEUE", "排队"),
    /** 预订到店 */
    RESERVATION("RESERVATION", "预订");

    private final String value;
    private final String desc;

    OrderSource(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
