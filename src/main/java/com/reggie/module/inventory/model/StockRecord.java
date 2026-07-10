package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存记录实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class StockRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 物料ID
     */
    private Long materialId;

    /**
     * 类型
     */
    private String type;

    /**
     * 数量
     */
    private BigDecimal qty;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作员
     */
    private String operator;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
