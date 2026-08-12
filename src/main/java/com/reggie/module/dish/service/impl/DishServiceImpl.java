package com.reggie.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.RedisCacheUtil;
import com.reggie.dto.DishDto;
import com.reggie.module.category.model.Category;
import com.reggie.module.dish.model.Dish;
import com.reggie.common.CustomException;
import com.reggie.module.dish.model.DishFlavor;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.module.dish.mapper.DishMapper;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.dish.service.DishFlavorService;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.setmeal.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** 分类服务 */
    @Autowired
    private CategoryService categoryService;

    /**
     * 根据ID查询菜品信息及关联的口味列表
     *
     * @param id 菜品ID
     * @return 菜品及口味信息DTO
     * @throws CustomException 菜品不存在时抛出
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
        // 校验分类是否存在
        if (dish.getCategoryId() != null) {
            Category category = categoryService.getById(dish.getCategoryId());
            if (category == null) {
                throw new CustomException("菜品分类不存在，请先创建分类");
            }
        }

        // 校验库存非负
        if (dish.getStockQty() != null && dish.getStockQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("库存数量不能小于0");
        }
        if (dish.getMinStock() != null && dish.getMinStock().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("最低库存不能小于0");
        }

        // 如果有库存且为0，自动设为停售
        if (dish.getStockQty() != null && dish.getStockQty().compareTo(BigDecimal.ZERO) <= 0) {
            dish.setStatus(0);
        }

        // 保存菜品基本信息
        this.save(dish);

        // 保存菜品口味（事务保护）
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dish.getId()));
            dishFlavorService.saveBatch(flavors);
        }
    }

    /**
     * 更新菜品及口味信息（事务保护）
     * 先删除旧口味，再插入新口味
     *
     * @param dishDto 菜品及口味DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithFlavor(DishDto dishDto) {
        // 校验菜品是否存在
        Dish existing = this.getById(dishDto.getId());
        if (existing == null) {
            throw new CustomException("菜品不存在");
        }

        // 校验库存非负
        if (dishDto.getStockQty() != null && dishDto.getStockQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("库存数量不能小于0");
        }
        if (dishDto.getMinStock() != null && dishDto.getMinStock().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("最低库存不能小于0");
        }

        //更新dish表基本信息
        this.updateById(dishDto);

        //修改后检查是否需要自动切换售罄状态
        autoToggleSoldOut(dishDto.getId());

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
        if (ids == null || ids.isEmpty()) {
            throw new CustomException("请选择要删除的菜品");
        }

        // 1. 一次性查出所有被套餐引用的菜品ID（避免N+1）
        List<Long> referencedIds = setmealDishService.list(
            new LambdaQueryWrapper<SetmealDish>()
                .in(SetmealDish::getDishId, ids)
                .select(SetmealDish::getDishId)
        ).stream().map(SetmealDish::getDishId).collect(Collectors.toList());

        if (!referencedIds.isEmpty()) {
            List<Dish> dishes = this.listByIds(referencedIds);
            List<String> referencedNames = dishes.stream()
                .map(Dish::getName)
                .collect(Collectors.toList());
            throw new CustomException("以下菜品正在被套餐引用，无法删除：" + String.join("、", referencedNames));
        }

        // 2. 清理套餐关联残留
        LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
        sdWrapper.in(SetmealDish::getDishId, ids);
        setmealDishService.remove(sdWrapper);

        // 3. 批量删除关联的口味数据
        LambdaQueryWrapper<DishFlavor> flavorWrapper = new LambdaQueryWrapper<>();
        flavorWrapper.in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(flavorWrapper);

        // 4. 批量删除菜品
        this.removeByIds(ids);

        log.info("批量删除菜品完成：ids={}", ids);
    }

    // ==================== 库存管理 ====================

    /**
     * 扣减菜品库存（使用乐观锁防止超卖）
     *
     * @param dishId 菜品ID
     * @param qty 扣减数量
     */
    @Override
    public void deductStock(Long dishId, java.math.BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }

        // 使用乐观锁原子扣减：WHERE stock_qty >= ? 防止超卖
        int affected = getBaseMapper().deductStock(dishId, qty);
        boolean success = affected > 0;
        if (!success) {
            // 查询当前库存用于错误提示
            Dish dish = this.getById(dishId);
            BigDecimal currentStock = dish != null && dish.getStockQty() != null
                    ? dish.getStockQty() : java.math.BigDecimal.ZERO;
            throw new CustomException("菜品库存不足，当前库存：" + currentStock + "，需要扣减：" + qty);
        }

        // 扣减成功后，检查是否需要自动停售
        autoToggleSoldOut(dishId);
        log.info("[库存扣减] 菜品ID={} 扣减{}份，乐观锁原子更新", dishId, qty);
    }

    /**
     * 增加菜品库存（原子操作）
     *
     * @param dishId 菜品ID
     * @param qty 增加数量
     */
    @Override
    public void addStock(Long dishId, java.math.BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }

        // 修复 read-modify-write 竞态：调用 DishMapper 原子方法 addStock
        // SQL: UPDATE dish SET stock_qty = IFNULL(stock_qty,0) + #{qty} WHERE id = #{id}
        int affected = getBaseMapper().addStock(dishId, qty);
        if (affected <= 0) {
            return; // 菜品不存在
        }

        // 补货后若处于停售状态且库存已达标，独立恢复起售（状态机维护，与库存原子更新解耦）
        Dish dish = this.getById(dishId);
        if (dish == null) return;
        BigDecimal newStock = dish.getStockQty() != null ? dish.getStockQty() : BigDecimal.ZERO;
        if (dish.getStatus() != null && dish.getStatus() == 0
                && dish.getMinStock() != null && newStock.compareTo(dish.getMinStock()) >= 0) {
            LambdaUpdateWrapper<Dish> statusWrapper = new LambdaUpdateWrapper<>();
            statusWrapper.eq(Dish::getId, dishId).set(Dish::getStatus, 1);
            this.update(statusWrapper);
            log.info("[库存] 菜品「{}」补货至{}，恢复起售", dish.getName(), newStock);
        }
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

    /**
     * 获取菜品统计数据（轻量接口，仅COUNT查询，不拉取全量数据）
     *
     * @return 统计 Map（total/active/inactive/lowStock/soldOut）
     */
    @Override
    public Map<String, Object> getStats() {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();

        long total = countDishes(tenantId, null, null);

        // 已起售
        long active = countDishes(tenantId, 1, null);

        // 已停售
        long inactive = countDishes(tenantId, 0, null);

        // 低库存（stockQty <= minStock 且 stockQty > 0）
        long lowStock = countDishes(tenantId, null, "lowStock");

        // 已售罄（stockQty = 0）
        long soldOut = countDishes(tenantId, null, "soldOut");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("activeDishes", active);
        stats.put("inactiveDishes", inactive);
        stats.put("lowStock", lowStock);
        stats.put("soldOut", soldOut);
        return stats;
    }

    /**
     * 根据条件统计菜品数量
     */
    private long countDishes(Long tenantId, Integer status, String stockFilter) {
        LambdaQueryWrapper<Dish> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(Dish::getTenantId, tenantId);
        }
        if (status != null) {
            qw.eq(Dish::getStatus, status);
        }
        if ("lowStock".equals(stockFilter)) {
            // stockQty <= minStock AND stockQty > 0
            qw.apply("stock_qty <= min_stock AND stock_qty > 0");
        } else if ("soldOut".equals(stockFilter)) {
            qw.eq(Dish::getStockQty, BigDecimal.ZERO);
        }
        return this.count(qw);
    }

    /**
     * 更新菜品库存信息
     *
     * @param id       菜品ID
     * @param stockQty 库存数量
     * @param minStock 最低库存预警值
     */
    @Override
    public void updateStock(Long id, java.math.BigDecimal stockQty, java.math.BigDecimal minStock) {
        // 校验库存非负
        if (stockQty != null && stockQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("库存数量不能小于0");
        }
        if (minStock != null && minStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("最低库存不能小于0");
        }

        Dish dish = new Dish();
        dish.setId(id);
        dish.setStockQty(stockQty);
        dish.setMinStock(minStock);
        this.updateById(dish);
        log.info("[库存] 更新菜品库存：id={}, stockQty={}, minStock={}", id, stockQty, minStock);

        // 库存更新后检查是否需要切换售罄状态
        autoToggleSoldOut(id);
    }
}




