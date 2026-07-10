package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物料实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class Material implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 正常状态
     */
    public static final int STATUS_NORMAL = 1;

    /**
     * 禁用状态
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 物料名称
     */
    private String name;

    /**
     * 单位
     */
    private String unit;

    /**
     * 库存数量
     */
    private BigDecimal stockQty;

    /**
     * 最小库存
     */
    private BigDecimal minStock;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 条形码
     */
    private String barcode;

    /**
     * 状态（1-正常，0-禁用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
