package com.reggie.dto;

import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
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
public class DishDto extends Dish {

    /**
     * 菜品对应的口味数据
     */
    private List<DishFlavor> flavors = new ArrayList<>();

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 份数
     */
    private Integer copies;
}
