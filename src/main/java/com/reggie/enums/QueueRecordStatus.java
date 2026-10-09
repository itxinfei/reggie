package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 排队状态枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Getter
public enum QueueRecordStatus {

    /** 等待中 */
    WAITING("WAITING", "等待中"),
    /** 已叫号 */
    CALLED("CALLED", "已叫号"),
    /** 已入座 */
    SEATED("SEATED", "已入座"),
    /** 已取消 */
    CANCELLED("CANCELLED", "已取消");
    private final String value;
    private final String desc;

    QueueRecordStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
