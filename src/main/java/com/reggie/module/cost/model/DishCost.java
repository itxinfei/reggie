package com.reggie.module.cost.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品成本实体
 *
 * @author reggie
 * @since 2026-08-10
 */
@Data
@TableName("dish_cost")
@Schema(description = "菜品成本")
public class DishCost implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "菜品ID")
    private Long dishId;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "食材成本")
    private BigDecimal materialCost;

    @Schema(description = "人工成本")
    private BigDecimal laborCost;

    @Schema(description = "其他成本")
    private BigDecimal otherCost;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "售价")
    private BigDecimal salePrice;

    @Schema(description = "毛利率")
    private BigDecimal profitRate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private Long createUser;

    @Schema(description = "更新人")
    private Long updateUser;
}
