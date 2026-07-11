package com.reggie.enums;

import lombok.Getter;

/**
 * 餐桌状态枚举
 */
@Getter
public enum DiningTableStatus {

    /** 空闲 */
    FREE("FREE", "空闲"),
    /** 已占用 */
    OCCUPIED("OCCUPIED", "占用"),
    /** 已预订 */
    RESERVED("RESERVED", "预留"),
    /** 清洁中 */
    CLEANING("CLEANING", "清洁中");
    private final String value;
    private final String desc;

    DiningTableStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
