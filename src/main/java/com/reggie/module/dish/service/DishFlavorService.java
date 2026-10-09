package com.reggie.module.dish.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dish.model.DishFlavor;

import java.util.List;

/**
 * <p>
 * 菜品口味服务接口，提供菜品口味数据的增删改查功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface DishFlavorService extends IService<DishFlavor> {

    /**
     * 根据菜品ID查询口味列表
     *
     * @param dishId 菜品ID
     * @return 口味列表
     */
    List<DishFlavor> listByDishId(Long dishId);
}

