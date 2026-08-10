package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.DishSpecGroup;
import com.reggie.entity.DishSpecOption;
import com.reggie.entity.DishSpecRelation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 菜品规格服务接口
 *
 * @author reggie
 * @since 2026-08-11
 */
public interface DishSpecService extends IService<DishSpecGroup> {

    // ==================== 规格组管理 ====================

    /**
     * 获取规格组列表
     *
     * @param tenantId 租户ID
     * @return 规格组列表
     */
    List<DishSpecGroup> getSpecGroups(Long tenantId);

    /**
     * 根据ID获取规格组
     *
     * @param id 规格组ID
     * @return 规格组
     */
    DishSpecGroup getSpecGroupById(Long id);

    /**
     * 保存或更新规格组
     *
     * @param group 规格组
     * @return 是否成功
     */
    boolean saveOrUpdateSpecGroup(DishSpecGroup group);

    /**
     * 删除规格组
     *
     * @param id 规格组ID
     * @return 是否成功
     */
    boolean deleteSpecGroup(Long id);

    // ==================== 规格选项管理 ====================

    /**
     * 获取规格选项列表
     *
     * @param groupId  规格组ID
     * @param tenantId 租户ID
     * @return 规格选项列表
     */
    List<DishSpecOption> getSpecOptions(Long groupId, Long tenantId);

    /**
     * 保存或更新规格选项
     *
     * @param option 规格选项
     * @return 是否成功
     */
    boolean saveOrUpdateSpecOption(DishSpecOption option);

    /**
     * 删除规格选项
     *
     * @param id 规格选项ID
     * @return 是否成功
     */
    boolean deleteSpecOption(Long id);

    /**
     * 批量保存规格选项
     *
     * @param options 规格选项列表
     * @return 是否成功
     */
    boolean batchSaveSpecOptions(List<DishSpecOption> options);

    // ==================== 菜品规格关联 ====================

    /**
     * 获取菜品关联的规格组
     *
     * @param dishId   菜品ID
     * @param tenantId 租户ID
     * @return 规格组列表（含选项）
     */
    List<Map<String, Object>> getDishSpecGroups(Long dishId, Long tenantId);

    /**
     * 设置菜品规格关联
     *
     * @param dishId   菜品ID
     * @param groupIds 规格组ID列表
     * @param tenantId 租户ID
     * @return 是否成功
     */
    boolean setDishSpecGroups(Long dishId, List<Long> groupIds, Long tenantId);

    /**
     * 删除菜品规格关联
     *
     * @param dishId 菜品ID
     * @return 是否成功
     */
    boolean deleteDishSpecRelations(Long dishId);

    // ==================== 规格价格计算 ====================

    /**
     * 计算菜品规格价格
     *
     * @param dishId      菜品ID
     * @param basePrice   基础价格
     * @param optionIds   选择的规格选项ID列表
     * @return 最终价格
     */
    BigDecimal calculateSpecPrice(Long dishId, BigDecimal basePrice, List<Long> optionIds);

    /**
     * 获取菜品规格详情（用于点餐页面）
     *
     * @param dishId   菜品ID
     * @param tenantId 租户ID
     * @return 规格详情
     */
    Map<String, Object> getDishSpecDetail(Long dishId, Long tenantId);

    // ==================== 统计分析 ====================

    /**
     * 获取规格统计
     *
     * @param tenantId 租户ID
     * @return 统计数据
     */
    Map<String, Object> getSpecStatistics(Long tenantId);
}
