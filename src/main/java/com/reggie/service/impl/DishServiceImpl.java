package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.dto.DishDto;
import com.reggie.entity.Dish;
import com.reggie.common.CustomException;
import com.reggie.entity.DishFlavor;
import com.reggie.entity.SetmealDish;
import com.reggie.enums.DishStatus;
import com.reggie.mapper.DishMapper;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import com.reggie.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper,Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dishes", allEntries = true)
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表dish
        this.save(dishDto);

        Long dishId = dishDto.getId();//菜品id

        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(item -> item.setDishId(dishId));
            //保存菜品口味数据到菜品口味表dish_flavor
            dishFlavorService.saveBatch(flavors);
        }
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息，从dish表查询
        Dish dish = this.getById(id);
        if (dish == null) {
            throw new CustomException("菜品不存在");
        }

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);

        //查询当前菜品对应的口味信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     * 根据分类id查询菜品列表
     * @param categoryId
     * @return
     */
    @Override
    @Cacheable(value = "dishes", key = "#categoryId")
    public List<Dish> listByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(categoryId != null, Dish::getCategoryId, categoryId);
        queryWrapper.eq(Dish::getStatus, DishStatus.ENABLED.getValue());
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        return this.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dishes", allEntries = true)
    public void saveDish(Dish dish, List<DishFlavor> flavors) {
        // 保存菜品基本信息
        this.save(dish);

        // 保存菜品口味（事务保护）
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dish.getId()));
            dishFlavorService.saveBatch(flavors);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dishes", allEntries = true)
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息
        this.updateById(dishDto);

        //清理当前菜品对应口味数据---dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());

        dishFlavorService.remove(queryWrapper);

        //添加当前提交过来的口味数据---dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();

        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(item -> item.setDishId(dishDto.getId()));
            dishFlavorService.saveBatch(flavors);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer status, List<Long> ids) {
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(ids != null, Dish::getId, ids);
        updateWrapper.set(Dish::getStatus, status);
        this.update(updateWrapper);
    }

    /**
     * 删除菜品及关联口味（事务保护），删除前校验套餐引用
     * 修改点：原Controller中for循环逐条删除无事务保护，
     *         改为Service层统一管理，添加@Transactional和套餐引用校验
     * @param ids 菜品ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dishes", allEntries = true)
    public void deleteWithFlavorCheck(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomException("请选择要删除的菜品");
        }

        // 1. 检查是否有菜品被套餐引用
        LambdaQueryWrapper<SetmealDish> setmealDishWrapper = new LambdaQueryWrapper<>();
        setmealDishWrapper.in(SetmealDish::getDishId, ids);
        long refCount = setmealDishService.count(setmealDishWrapper);
        if (refCount > 0) {
            // 找出被引用的菜品名称
            List<Dish> dishes = this.listByIds(ids);
            List<String> referencedNames = dishes.stream()
                .filter(d -> {
                    LambdaQueryWrapper<SetmealDish> checkWrapper = new LambdaQueryWrapper<>();
                    checkWrapper.eq(SetmealDish::getDishId, d.getId());
                    return setmealDishService.count(checkWrapper) > 0;
                })
                .map(Dish::getName)
                .collect(Collectors.toList());
            throw new CustomException("以下菜品正在被套餐引用，无法删除：" + String.join("、", referencedNames));
        }

        // 2. 批量删除关联的口味数据
        LambdaQueryWrapper<DishFlavor> flavorWrapper = new LambdaQueryWrapper<>();
        flavorWrapper.in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(flavorWrapper);

        // 3. 批量删除菜品
        this.removeByIds(ids);

        log.info("批量删除菜品完成：ids={}", ids);
    }
}
