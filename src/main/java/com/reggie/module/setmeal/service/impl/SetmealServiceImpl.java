package com.reggie.module.setmeal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.common.RedisCacheUtil;
import com.reggie.dto.SetmealDto;
import com.reggie.module.category.model.Category;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.enums.DishStatus;
import com.reggie.module.setmeal.mapper.SetmealMapper;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.setmeal.service.SetmealDishService;
import com.reggie.module.setmeal.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐服务实现类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    /** 套餐菜品关联服务 */
    @Autowired
    private SetmealDishService setmealDishService;

    /** 分类服务 */
    @Autowired
    private CategoryService categoryService;

    /** Redis缓存工具 */
    @Autowired
    private RedisCacheUtil redisCacheUtil;

    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     * 缓存删除在事务提交后执行，避免事务回滚导致缓存与数据库不一致
     *
     * @param setmealDto 套餐DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithDish(SetmealDto setmealDto) {
        // 校验分类是否存在
        if (setmealDto.getCategoryId() != null) {
            Category category = categoryService.getById(setmealDto.getCategoryId());
            if (category == null) {
                throw new CustomException("套餐分类不存在，请先创建分类");
            }
        }

        //保存套餐的基本信息，操作setmeal，执行insert操作
        this.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes.forEach(item -> item.setSetmealId(setmealDto.getId()));
            //保存套餐和菜品的关联信息，操作setmeal_dish,执行insert操作
            setmealDishService.saveBatch(setmealDishes);
        }

        // 事务提交后清除缓存（避免事务回滚导致缓存与数据库不一致）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisCacheUtil.doubleDeleteAllEntries("setmeal");
            }
        });
    }

    /**
     * 根据id查询套餐及关联菜品
     * 使用sync=true防止缓存击穿
     *
     * @param id 套餐ID
     * @return 套餐DTO
     */
    @Override
    @Cacheable(value = "setmeal", key = "#id", sync = true)
    public SetmealDto getByIdWithDish(Long id) {
        Setmeal setmeal = this.getById(id);
        if (setmeal == null) {
            throw new CustomException("套餐不存在");
        }
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
        List<SetmealDish> dishes = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(dishes);
        return setmealDto;
    }

    /**
     * 更新套餐
     * 缓存删除在事务提交后执行
     *
     * @param setmealDto 套餐DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithDish(SetmealDto setmealDto) {
        // 校验套餐是否存在
        Setmeal existing = this.getById(setmealDto.getId());
        if (existing == null) {
            throw new CustomException("套餐不存在");
        }

        this.updateById(setmealDto);

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        List<SetmealDish> dishes = setmealDto.getSetmealDishes();
        if (dishes != null && !dishes.isEmpty()) {
            dishes.forEach(item -> item.setSetmealId(setmealDto.getId()));
            setmealDishService.saveBatch(dishes);
        }

        // 事务提交后清除缓存
        Long setmealId = setmealDto.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisCacheUtil.doubleDelete("setmeal", setmealId);
            }
        });
    }

    /**
     * 删除套餐及关联菜品
     * 逐条校验状态：售卖中的套餐拒绝删除，停售的套餐允许删除
     *
     * @param ids 套餐ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWithDish(List<Long> ids) {
        // 逐条校验：查询传入ID中处于售卖状态的套餐
        LambdaQueryWrapper<Setmeal> enabledQuery = new LambdaQueryWrapper<>();
        enabledQuery.in(Setmeal::getId, ids)
                    .eq(Setmeal::getStatus, DishStatus.ENABLED.getValue());
        List<Setmeal> enabledSetmeals = this.list(enabledQuery);

        if (!enabledSetmeals.isEmpty()) {
            List<String> enabledNames = enabledSetmeals.stream()
                .map(Setmeal::getName)
                .collect(Collectors.toList());
            throw new CustomException("以下套餐正在售卖中，无法删除：" + String.join("、", enabledNames));
        }

        // 所有传入ID的套餐均为停售状态，允许删除
        this.removeByIds(ids);

        // delete from setmeal_dish where setmeal_id in (...)
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(lambdaQueryWrapper);

        // 事务提交后清除缓存
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (ids != null) {
                    ids.forEach(id -> redisCacheUtil.doubleDelete("setmeal", id));
                }
            }
        });
    }

    /**
     * 批量更新套餐状态
     * 缓存删除在事务提交后执行
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer status, List<Long> ids) {
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Setmeal::getId, ids);
        updateWrapper.set(Setmeal::getStatus, status);
        this.update(updateWrapper);

        // 事务提交后清除缓存
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (ids != null) {
                    ids.forEach(id -> redisCacheUtil.doubleDelete("setmeal", id));
                }
            }
        });
    }
}



