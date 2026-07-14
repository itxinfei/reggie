package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 库存变动类型枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Getter
public enum StockRecordType {

    /** 入库 */
    IN("IN", "入库"),
    /** 出库 */
    OUT("OUT", "出库"),
    /** 盘点调整 */
    CHECK("CHECK", "盘点调整");

    private final String value;
    private final String desc;

    StockRecordType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
