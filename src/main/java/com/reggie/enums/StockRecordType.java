package com.reggie.enums;

import lombok.Getter;

/**
 * 库存变动类型枚举
 */
@Getter
public enum StockRecordType {

    IN("IN", "入库"),
    OUT("OUT", "出库"),
    CHECK("CHECK", "盘点调整");

    private final String value;
    private final String desc;

    StockRecordType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
