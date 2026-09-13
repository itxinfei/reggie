package com.reggie.module.setmeal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.common.BaseContext;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.setmeal.model.SetmealDish;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.setmeal.mapper.SetmealDishMapper;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.setmeal.service.SetmealDishService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 套餐菜品关联服务实现类
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SetmealDishServiceImpl extends ServiceImpl<SetmealDishMapper, SetmealDish> implements SetmealDishService {

    /**
     * 查询列表 by setmeal id。
     * @param setmealId 参数 setmealId
     * @return 返回结果
     */
    @Override
    public List<SetmealDish> listBySetmealId(Long setmealId) {
        return this.list(new LambdaQueryWrapper<SetmealDish>()
                .eq(SetmealDish::getSetmealId, setmealId)
                .eq(SetmealDish::getTenantId, BaseContext.getCurrentTenantId())
                .orderByAsc(SetmealDish::getSort));
    }
}

