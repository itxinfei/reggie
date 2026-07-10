package com.reggie.enums;

import lombok.Getter;

/**
 * 餐桌状态枚举
 */
@Getter
public enum DiningTableStatus {

    /** 空闲 */
    EMPTY("EMPTY", "空闲"),
    /** 已占用 */
    OCCUPIED("OCCUPIED", "已占用"),
    /** 已预订 */
    RESERVED("RESERVED", "已预订");

    private final String value;
    private final String desc;

    DiningTableStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
