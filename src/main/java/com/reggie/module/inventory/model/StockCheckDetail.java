package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存盘点明细实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class StockCheckDetail implements Serializable {
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
     * 盘点ID
     */
    private Long checkId;

    /**
     * 物料ID
     */
    private Long materialId;

    /**
     * 账面数量
     */
    private BigDecimal bookQty;

    /**
     * 实际数量
     */
    private BigDecimal actualQty;

    /**
     * 差异数量
     */
    private BigDecimal diffQty;

    /**
     * 备注
     */
    private String remark;
}
