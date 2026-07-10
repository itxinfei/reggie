package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.RedisCacheUtil;
import com.reggie.dto.DishDto;
import com.reggie.entity.Dish;
import com.reggie.common.CustomException;
import com.reggie.entity.DishFlavor;
import com.reggie.entity.SetmealDish;
import com.reggie.mapper.DishMapper;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import com.reggie.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品服务实现类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper,Dish> implements DishService {

    /** 菜品口味服务 */
    @Autowired
    private DishFlavorService dishFlavorService;

    /** 套餐菜品关联服务 */
    @Autowired
    private SetmealDishService setmealDishService;

    /** Redis缓存工具 */
    @Autowired
    private RedisCacheUtil redisCacheUtil;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithFlavor(DishDto dishDto) {
        redisCacheUtil.doubleDeleteAllEntries("dishes");

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
     * 保存菜品及口味信息
     *
     * @param dish 菜品实体
     * @param flavors 口味列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDish(Dish dish, List<DishFlavor> flavors) {
        redisCacheUtil.doubleDeleteAllEntries("dishes");

        // 保存菜品基本信息
        this.save(dish);

        // 保存菜品口味（事务保护）
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dish.getId()));
            dishFlavorService.saveBatch(flavors);
        }
    }

    /**
     * 更新菜品及口味信息
     *
     * @param dishDto 菜品DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithFlavor(DishDto dishDto) {
        redisCacheUtil.doubleDeleteAllEntries("dishes");

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

    /**
     * 批量更新菜品状态
     *
     * @param status 目标状态
     * @param ids 菜品ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer status, List<Long> ids) {
        redisCacheUtil.doubleDeleteAllEntries("dishes");

        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(ids != null, Dish::getId, ids);
        updateWrapper.set(Dish::getStatus, status);
        this.update(updateWrapper);
    }

    /**
     * 删除菜品及关联口味（事务保护），删除前校验套餐引用
     * @param ids 菜品ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithFlavorCheck(List<Long> ids) {
        redisCacheUtil.doubleDeleteAllEntries("dishes");
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

    // ==================== 库存管理 ====================

    /**
     * 扣减菜品库存
     *
     * @param dishId 菜品ID
     * @param qty 扣减数量
     */
    @Override
    public void deductStock(Long dishId, java.math.BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }
        redisCacheUtil.doubleDeleteAllEntries("dishes");

        Dish dish = this.getById(dishId);
        if (dish == null) return;

        BigDecimal currentStock = dish.getStockQty() != null ? dish.getStockQty() : BigDecimal.ZERO;
        BigDecimal newStock = currentStock.subtract(qty);
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("菜品「" + dish.getName() + "」库存不足，当前库存：" + currentStock);
        }
        dish.setStockQty(newStock);
        // 库存为0时自动停售
        if (newStock.compareTo(BigDecimal.ZERO) == 0) {
            dish.setStatus(0);
            log.info("[库存] 菜品「{}」已售罄，自动停售", dish.getName());
        }
        this.updateById(dish);
        log.info("[库存扣减] 菜品{} 扣减{}份，剩余库存{}", dish.getName(), qty, newStock);
    }

    /**
     * 增加菜品库存
     *
     * @param dishId 菜品ID
     * @param qty 增加数量
     */
    @Override
    public void addStock(Long dishId, java.math.BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }
        redisCacheUtil.doubleDeleteAllEntries("dishes");

        Dish dish = this.getById(dishId);
        if (dish == null) return;

        BigDecimal currentStock = dish.getStockQty() != null ? dish.getStockQty() : BigDecimal.ZERO;
        BigDecimal newStock = currentStock.add(qty);
        dish.setStockQty(newStock);
        // 补货后如果之前是停售状态且库存大于0，自动恢复起售
        if (dish.getStatus() != null && dish.getStatus() == 0 && newStock.compareTo(BigDecimal.ZERO) > 0) {
            // 检查是否有最低库存设置
            if (dish.getMinStock() != null && newStock.compareTo(dish.getMinStock()) >= 0) {
                dish.setStatus(1);
                log.info("[库存] 菜品「{}」补货至{}，恢复起售", dish.getName(), newStock);
            }
        }
        this.updateById(dish);
        log.info("[库存补货] 菜品{} 补货{}份，当前库存{}", dish.getName(), qty, newStock);
    }

    /**
     * 自动切换菜品售罄状态
     *
     * @param dishId 菜品ID
     */
    @Override
    public void autoToggleSoldOut(Long dishId) {
        if (dishId == null) return;
        redisCacheUtil.doubleDeleteAllEntries("dishes");

        Dish dish = this.getById(dishId);
        if (dish == null) return;

        BigDecimal stock = dish.getStockQty() != null ? dish.getStockQty() : BigDecimal.ZERO;
        Integer newStatus = stock.compareTo(BigDecimal.ZERO) <= 0 ? 0 : 1;
        if (!newStatus.equals(dish.getStatus())) {
            dish.setStatus(newStatus);
            this.updateById(dish);
            log.info("[库存] 菜品「{}」状态自动切换为：{}", dish.getName(), newStatus == 1 ? "起售" : "停售");
        }
    }
}
