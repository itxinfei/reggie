package com.reggie.dto.dish;

import com.reggie.entity.DishFlavor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 菜品新增/修改DTO
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Data
@Schema(description = "菜品信息")
public class DishSaveDTO {

    @Schema(description = "菜品名称", required = true)
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 64, message = "菜品名称不能超过64个字符")
    private String name;

    @Schema(description = "分类ID", required = true)
    @NotNull(message = "菜品分类不能为空")
    private Long categoryId;

    @Schema(description = "菜品价格", required = true, example = "38.00")
    @NotNull(message = "菜品价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "菜品价格必须大于0")
    private BigDecimal price;

    @Schema(description = "商品码", example = "D001")
    @Size(max = 64, message = "商品码不能超过64个字符")
    private String code;

    @Schema(description = "图片路径", required = true)
    @NotBlank(message = "菜品图片不能为空")
    private String image;

    @Schema(description = "描述信息", example = "美味可口")
    @Size(max = 400, message = "描述信息不能超过400个字符")
    private String description;

    @Schema(description = "状态 0:停售 1:起售", required = true)
    @NotNull(message = "菜品状态不能为空")
    private Integer status;

    @Schema(description = "口味列表")
    private List<DishFlavor> flavors;
}


