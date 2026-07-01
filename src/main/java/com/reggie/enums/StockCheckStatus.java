package com.reggie.enums;

import lombok.Getter;

/**
 * 盘点单状态枚举
 */
@Getter
public enum StockCheckStatus {

    DRAFT("DRAFT", "草稿"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    DONE("DONE", "已完成");

    private final String value;
    private final String desc;

    StockCheckStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
