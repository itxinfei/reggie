package com.reggie.module.delivery.model;

/**
 * 配送平台枚举
 *
 * @author reggie
 * @since 2026-07-09
 */
public enum PlatformEnum {
    /** 美团 */
    MEITUAN("美团"),
    /** 饿了么 */
    ELEME("饿了么"),
    /** 抖音 */
    DOUYIN("抖音"),
    /** 达达 */
    DADA("达达"),
    /** 蜂鸟 */
    FENGNIAO("蜂鸟"),
    /** 顺丰 */
    SHUNFENG("顺丰");

    /** 平台显示名称 */
    private final String displayName;

    PlatformEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
