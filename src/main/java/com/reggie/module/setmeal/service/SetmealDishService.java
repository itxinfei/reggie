package com.reggie.module.setmeal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.setmeal.model.SetmealDish;

import java.util.List;

/**
 * <p>
 * 套餐菜品关联服务接口，提供套餐与菜品关联关系的管理功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface SetmealDishService extends IService<SetmealDish> {

    /**
     * 根据套餐ID查询关联的菜品列表
     *
     * @param setmealId 套餐ID
     * @return 关联菜品列表
     */
    List<SetmealDish> listBySetmealId(Long setmealId);
}

