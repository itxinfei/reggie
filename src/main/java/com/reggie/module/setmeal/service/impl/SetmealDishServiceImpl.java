package com.reggie.module.setmeal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.module.setmeal.mapper.SetmealDishMapper;
import com.reggie.module.setmeal.service.SetmealDishService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 套餐菜品关联服务实现类
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
public class SetmealDishServiceImpl extends ServiceImpl<SetmealDishMapper, SetmealDish> implements SetmealDishService {

    @Override
    public List<SetmealDish> listBySetmealId(Long setmealId) {
        return this.list(new LambdaQueryWrapper<SetmealDish>()
                .eq(SetmealDish::getSetmealId, setmealId)
                .eq(SetmealDish::getTenantId, BaseContext.getCurrentTenantId())
                .orderByAsc(SetmealDish::getSort));
    }
}

