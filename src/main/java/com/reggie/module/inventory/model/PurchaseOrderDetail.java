package com.reggie.module.inventory.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.math.BigDecimal;
import java.io.Serializable;

@Data
public class PurchaseOrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long purchaseOrderId;
    private Long materialId;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal receivedQty;
    private String remark;
}
