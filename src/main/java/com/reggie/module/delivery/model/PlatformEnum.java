package com.reggie.module.delivery.model;

public enum PlatformEnum {
    MEITUAN("美团"),
    ELEME("饿了么"),
    DOUYIN("抖音");

    private final String displayName;

    PlatformEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
