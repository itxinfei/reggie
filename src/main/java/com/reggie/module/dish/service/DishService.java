package com.reggie.module.dish.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.DishDto;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.dish.model.DishFlavor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 菜品管理服务接口，提供菜品的增删改查、口味管理及库存管理功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface DishService extends IService<Dish> {

    /**
     * 保存菜品及口味（事务保护）
     * <p>内部做分类存在性校验、库存非负校验、库存为0自动停售</p>
     *
     * @param dish 菜品信息
     * @param flavors 口味列表
     */
    void saveDish(Dish dish, List<DishFlavor> flavors);

    /**
     * 根据ID查询菜品信息和对应的口味信息
     *
     * @param id 菜品ID
     * @return 菜品及口味信息
     */
    DishDto getByIdWithFlavor(Long id);

    /**
     * 更新菜品信息，同时更新对应的口味信息
     *
     * @param dishDto 菜品及口味信息
     */
    void updateWithFlavor(DishDto dishDto);

    /**
     * 批量修改菜品起售停售状态
     *
     * @param status 目标状态（0停售 1起售）
     * @param ids 菜品ID列表
     */
    void updateStatus(Integer status, List<Long> ids);

    /**
     * 删除菜品及关联口味（事务保护），删除前校验套餐引用
     * @param ids 菜品ID列表
     */
    void deleteWithFlavorCheck(List<Long> ids);

    // ==================== 库存管理 ====================

    /**
     * 扣减菜品库存
     * @param dishId 菜品ID
     * @param qty 扣减数量
     */
    void deductStock(Long dishId, BigDecimal qty);

    /**
     * 补货菜品库存
     * @param dishId 菜品ID
     * @param qty 补货数量
     */
    void addStock(Long dishId, BigDecimal qty);

    /**
     * 自动售罄处理：库存为0时自动停售，有库存时自动起售
     */
    void autoToggleSoldOut(Long dishId);

    /**
     * 获取菜品统计数据（轻量接口，只查count不拉取全量数据）
     * @return 统计 Map（total/active/inactive/lowStock/soldOut）
     */
    Map<String, Object> getStats();

    /**
     * 更新菜品库存信息
     * @param id 菜品ID
     * @param stockQty 库存数量
     * @param minStock 最低库存预警值
     */
    void updateStock(Long id, BigDecimal stockQty, BigDecimal minStock);
}

