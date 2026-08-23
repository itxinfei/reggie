package com.reggie.module.inventory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存预警响应 VO
 * 包装 Material 核心字段 + 预警阈值比例 + 严重度分级
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "库存预警响应")
public class WarningMaterialVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 严重度枚举值：
     * CRITICAL - 库存低于预警阈值的30%（极度危险）
     * WARNING  - 库存低于预警阈值的30%~80%
     * LOW      - 库存低于预警阈值的80%~100%
     */
    public static final String SEVERITY_CRITICAL = "CRITICAL";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String SEVERITY_LOW = "LOW";

    @Schema(description = "食材ID")
    private Long id;

    @Schema(description = "食材名称")
    private String name;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "当前库存数量")
    private BigDecimal stockQty;

    @Schema(description = "最低库存预警阈值")
    private BigDecimal minStock;

    @Schema(description = "单价（元）")
    private BigDecimal unitPrice;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "预警阈值比例，取值 0~1，越小越危险")
    private BigDecimal stockRatio;

    @Schema(description = "预警阈值比例（百分比，0~100）")
    private BigDecimal warningRatio;

    @Schema(description = "严重度：CRITICAL/WARNING/LOW")
    private String severity;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}