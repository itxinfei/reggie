package com.reggie.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 分类新增DTO
 */
@Data
@Schema(description = "分类信息")
public class CategorySaveDTO {

    @Schema(description = "分类类型 1:菜品分类 2:套餐分类", required = true)
    @NotNull(message = "分类类型不能为空")
    private Integer type;

    @Schema(description = "分类名称", required = true)
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Schema(description = "排序序号", required = true)
    @NotNull(message = "排序不能为空")
    private Integer sort;
}
