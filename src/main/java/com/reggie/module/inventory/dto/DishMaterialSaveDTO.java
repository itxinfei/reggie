package com.reggie.module.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品食材关联保存DTO
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(name = "DishMaterialSaveDTO", description = "菜品食材关联保存DTO")
public class DishMaterialSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "菜品ID不能为空")
    @Schema(description = "菜品ID")
    private Long dishId;

    @NotNull(message = "食材ID不能为空")
    @Schema(description = "食材ID")
    private Long materialId;

    @NotNull(message = "用量不能为空")
    @Positive(message = "用量必须大于0")
    @Schema(description = "单份菜品消耗食材数量")
    private BigDecimal usageQty;

    @Schema(description = "排序")
    private Integer sort;
}
