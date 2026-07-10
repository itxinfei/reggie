package com.reggie.dto;

import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
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
public class SetmealDto extends Setmeal {

    /**
     * 套餐菜品关系列表
     */
    private List<SetmealDish> setmealDishes;

    /**
     * 分类名称
     */
    private String categoryName;
}
