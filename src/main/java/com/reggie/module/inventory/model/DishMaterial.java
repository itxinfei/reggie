package com.reggie.module.inventory.model;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 菜品食材关联（BOM）
 * 记录菜品与食材的配方关系，用于成本核算和库存联动出库。
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@TableName("dish_material")
@Schema(name = "DishMaterial", description = "菜品食材关联")
public class DishMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Long tenantId;

    @Schema(description = "菜品ID")
    @TableField("dish_id")
    private Long dishId;

    @Schema(description = "食材ID")
    @TableField("material_id")
    private Long materialId;

    @Schema(description = "单份菜品消耗食材数量")
    @TableField("usage_qty")
    private BigDecimal usageQty;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private Date updateTime;

    @TableField(value = "create_user", fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createUser;

    @TableField(value = "update_user", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人ID")
    private Long updateUser;

    @TableLogic
    @Schema(description = "逻辑删除")
    @TableField("is_deleted")
    private Integer isDeleted;
}
