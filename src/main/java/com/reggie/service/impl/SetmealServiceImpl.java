package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.common.RedisCacheUtil;
import com.reggie.dto.SetmealDto;
import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
import com.reggie.enums.DishStatus;
import com.reggie.mapper.SetmealMapper;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
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
     * 先校验状态，再删除数据和缓存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWithDish(List<Long> ids) {
        //select count(*) from setmeal where id in (1,2,3) and status = 1
        //查询套餐状态，确定是否可用删除
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus, DishStatus.ENABLED.getValue());

        int count = this.count(queryWrapper);
        if(count > 0){
            //如果不能删除，抛出一个业务异常
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        //如果可以删除，先删除套餐表中的数据---setmeal
        this.removeByIds(ids);

        //delete from setmeal_dish where setmeal_id in (1,2,3)
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        //删除关系表中的数据----setmeal_dish
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
