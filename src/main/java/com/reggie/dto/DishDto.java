package com.reggie.dto;

import com.reggie.module.dish.model.Dish;
import com.reggie.module.dish.model.DishFlavor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品数据传输对象
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜品数据传输对象（含口味信息）")
public class DishDto extends Dish {

    @Schema(description = "菜品口味列表")
    private List<DishFlavor> flavors = new ArrayList<>();

    @Schema(description = "分类名称", example = "热销榜")
    private String categoryName;

    @Schema(description = "份数（购物车使用）", example = "1")
    private Integer copies;
}

