package com.reggie.module.finance.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Profit Analysis Entity
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("profit_analysis")
@Schema(description = "Profit Analysis")
public class ProfitAnalysis implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Analysis Date")
    private LocalDate analysisDate;

    @Schema(description = "Total Revenue")
    private BigDecimal totalRevenue;

    @Schema(description = "Food Cost")
    private BigDecimal foodCost;

    @Schema(description = "Labor Cost")
    private BigDecimal laborCost;

    @Schema(description = "Other Cost")
    private BigDecimal otherCost;

    @Schema(description = "Total Cost")
    private BigDecimal totalCost;

    @Schema(description = "Gross Profit")
    private BigDecimal grossProfit;

    @Schema(description = "Gross Profit Rate")
    private BigDecimal grossProfitRate;

    @Schema(description = "Operating Expense")
    private BigDecimal operatingExpense;

    @Schema(description = "Net Profit")
    private BigDecimal netProfit;

    @Schema(description = "Net Profit Rate")
    private BigDecimal netProfitRate;

    @Schema(description = "Order Count")
    private Integer orderCount;

    @Schema(description = "Customer Count")
    private Integer customerCount;

    @Schema(description = "Average Order Value")
    private BigDecimal averageOrderValue;

    @Schema(description = "Tenant ID")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;

    @Schema(description = "Update Time")
    private LocalDateTime updateTime;
}
