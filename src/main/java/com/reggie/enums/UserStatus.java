package com.reggie.enums;

import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
public enum UserStatus {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 正常
     */
    ENABLED(1, "正常");

    private final int value;
    private final String desc;

    UserStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
