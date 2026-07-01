package com.reggie.enums;

import lombok.Getter;

/**
 * 积分变动类型枚举
 */
@Getter
public enum PointsRecordType {

    IN("IN", "增加"),
    OUT("OUT", "扣除");

    private final String value;
    private final String desc;

    PointsRecordType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
