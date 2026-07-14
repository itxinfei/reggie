package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.DishDto;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;

import java.math.BigDecimal;
import java.util.List;

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
     * 新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish、dish_flavor
     *
     * @param dishDto 菜品及口味信息
     */
    public void saveWithFlavor(DishDto dishDto);

    /**
     * 根据ID查询菜品信息和对应的口味信息
     *
     * @param id 菜品ID
     * @return 菜品及口味信息
     */
    public DishDto getByIdWithFlavor(Long id);

    /**
     * 更新菜品信息，同时更新对应的口味信息
     *
     * @param dishDto 菜品及口味信息
     */
    public void updateWithFlavor(DishDto dishDto);

    /**
     * 批量修改菜品起售停售状态
     *
     * @param status 目标状态（0停售 1起售）
     * @param ids 菜品ID列表
     */
    public void updateStatus(Integer status, List<Long> ids);

    /**
     * 保存菜品及口味（事务保护）
     *
     * @param dish 菜品信息
     * @param flavors 口味列表
     */
    public void saveDish(Dish dish, List<DishFlavor> flavors);

    /**
     * 删除菜品及关联口味（事务保护），删除前校验套餐引用
     * @param ids 菜品ID列表
     */
    public void deleteWithFlavorCheck(List<Long> ids);

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
}
