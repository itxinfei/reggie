package com.reggie.module.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 菜品食材批量保存DTO
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(name = "DishMaterialBatchDTO", description = "菜品食材批量保存DTO")
public class DishMaterialBatchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "菜品ID不能为空")
    @Schema(description = "菜品ID")
    private Long dishId;

    @Valid
    @Schema(description = "食材明细列表")
    private List<DishMaterialSaveDTO> items;
}
