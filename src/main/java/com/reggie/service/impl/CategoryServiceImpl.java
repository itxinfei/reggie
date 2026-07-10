package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.common.RedisCacheUtil;
import com.reggie.entity.Category;
import com.reggie.entity.Dish;
import com.reggie.entity.Setmeal;
import com.reggie.mapper.CategoryMapper;
import com.reggie.service.CategoryService;
import com.reggie.service.DishService;
import com.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 分类服务实现类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService{

    /** 菜品服务 */
    @Autowired
    private DishService dishService;

    /** 套餐服务 */
    @Autowired
    private SetmealService setmealService;

    /** Redis缓存工具 */
    @Autowired
    private RedisCacheUtil redisCacheUtil;

    /**
     * 根据id删除分类，删除之前需要进行判断
     * @param id
     */
    @Override
    public void remove(Long id) {
        redisCacheUtil.doubleDeleteAllEntries("categories");
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据分类id进行查询
        dishLambdaQueryWrapper.eq(Dish::getCategoryId,id);
        int count1 = dishService.count(dishLambdaQueryWrapper);

        //查询当前分类是否关联了菜品，如果已经关联，抛出一个业务异常
        if(count1 > 0){
            //已经关联菜品，抛出一个业务异常
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }

        //查询当前分类是否关联了套餐，如果已经关联，抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据分类id进行查询
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId,id);
        int count2 = setmealService.count(setmealLambdaQueryWrapper);
        if(count2 > 0){
            //已经关联套餐，抛出一个业务异常
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }

        //正常删除分类
        super.removeById(id);
    }

    /**
     * 新增分类并清除缓存
     *
     * @param entity 分类实体
     * @return 是否成功
     */
    @Override
    public boolean save(Category entity) {
        redisCacheUtil.doubleDeleteAllEntries("categories");
        return super.save(entity);
    }

    /**
     * 更新分类并清除缓存
     *
     * @param entity 分类实体
     * @return 是否成功
     */
    @Override
    public boolean updateById(Category entity) {
        redisCacheUtil.doubleDeleteAllEntries("categories");
        return super.updateById(entity);
    }
}
