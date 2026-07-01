package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PurchaseOrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long purchaseOrderId;
    private Long materialId;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal receivedQty;
    private String remark;
}
