package com.reggie.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * AI菜品描述生成请求DTO
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class DishDescriptionRequest {

    @Schema(description = "菜品名称", required = true, example = "宫保鸡丁")
    @NotBlank(message = "菜品名称不能为空")
    @Size(min = 1, max = 100, message = "菜品名称长度不能超过100字符")
    private String dishName;

    @Schema(description = "分类名称", example = "热菜")
    @Size(max = 50, message = "分类名称长度不能超过50字符")
    private String categoryName;

    @Schema(description = "食材列表", example = "鸡肉,花生米,干辣椒")
    @Size(max = 500, message = "食材描述长度不能超过500字符")
    private String ingredients;
}
