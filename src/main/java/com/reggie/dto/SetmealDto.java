package com.reggie.dto;

import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.setmeal.model.SetmealDish;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 套餐数据传输对象
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "套餐数据传输对象（含菜品列表）")
public class SetmealDto extends Setmeal {

    @Schema(description = "套餐菜品关系列表")
    private List<SetmealDish> setmealDishes;

    @Schema(description = "分类名称", example = "超值套餐")
    private String categoryName;
}

