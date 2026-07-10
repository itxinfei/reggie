package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存盘点实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class StockCheck implements Serializable {
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
     * 盘点单号
     */
    private String checkNo;

    /**
     * 状态
     */
    private String status;

    /**
     * 总差异金额
     */
    private BigDecimal totalDiffAmount;

    /**
     * 操作员
     */
    private String operator;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
