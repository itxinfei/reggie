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
    CHECK("CHECK", "盘点调整"),
    /** 订单销售扣减 */
    SALE_ORDER("SALE_ORDER", "订单销售"),
    /** 退款/取消回补 */
    REFUND_ORDER("REFUND_ORDER", "退款回补");

    private final String value;
    private final String desc;

    StockRecordType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
