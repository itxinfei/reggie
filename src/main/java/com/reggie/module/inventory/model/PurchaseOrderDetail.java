package com.reggie.module.inventory.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.math.BigDecimal;
import java.io.Serializable;

/**
 * 采购订单明细实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class PurchaseOrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 采购订单ID
     */
    private Long purchaseOrderId;

    /**
     * 物料ID
     */
    private Long materialId;

    /**
     * 数量
     */
    private BigDecimal qty;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 收货数量
     */
    private BigDecimal receivedQty;

    /**
     * 备注
     */
    private String remark;
}
