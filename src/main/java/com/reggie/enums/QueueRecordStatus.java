package com.reggie.enums;

import lombok.Getter;

/**
 * 排队状态枚举
 */
@Getter
public enum QueueRecordStatus {

    WAITING("WAITING", "等待中"),
    CALLED("CALLED", "已叫号"),
    CANCELLED("CANCELLED", "已取消");

    private final String value;
    private final String desc;

    QueueRecordStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
