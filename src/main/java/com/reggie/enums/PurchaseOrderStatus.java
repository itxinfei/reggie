package com.reggie.enums;

import lombok.Getter;

/**
 * 采购单状态枚举
 */
@Getter
public enum PurchaseOrderStatus {

    DRAFT("DRAFT", "草稿"),
    ORDERED("ORDERED", "已下单"),
    PARTIAL("PARTIAL", "部分收货"),
    RECEIVED("RECEIVED", "已收货"),
    CANCELLED("CANCELLED", "已取消");

    private final String value;
    private final String desc;

    PurchaseOrderStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
