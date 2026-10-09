package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 采购单状态枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Getter
public enum PurchaseOrderStatus {

    /** 草稿 */
    DRAFT("DRAFT", "草稿"),
    /** 已下单 */
    ORDERED("ORDERED", "已下单"),
    /** 部分收货 */
    PARTIAL("PARTIAL", "部分收货"),
    /** 已收货 */
    RECEIVED("RECEIVED", "已收货"),
    /** 已取消 */
    CANCELLED("CANCELLED", "已取消");

    private final String value;
    private final String desc;

    PurchaseOrderStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
