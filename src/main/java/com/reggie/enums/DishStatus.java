package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 菜品状态枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Getter
public enum DishStatus {

    /**
     * 停售
     */
    DISABLED(0, "停售"),

    /**
     * 起售
     */
    ENABLED(1, "起售");

    private final int value;
    private final String desc;

    DishStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
