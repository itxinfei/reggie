package com.reggie.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.dish.model.DishFlavor;
import com.reggie.module.dish.mapper.DishFlavorMapper;
import com.reggie.module.dish.service.DishFlavorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜品口味服务实现类
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DishFlavorServiceImpl extends ServiceImpl<DishFlavorMapper, DishFlavor> implements DishFlavorService {

    @Override
    public List<DishFlavor> listByDishId(Long dishId) {
        return this.list(new LambdaQueryWrapper<DishFlavor>()
                .eq(DishFlavor::getDishId, dishId)
                .eq(DishFlavor::getTenantId, BaseContext.getCurrentTenantId())
                .orderByAsc(DishFlavor::getId));
    }
}


