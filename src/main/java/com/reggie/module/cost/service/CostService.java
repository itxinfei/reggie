package com.reggie.module.cost.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.DishCost;
import com.reggie.entity.CostRecord;
import com.reggie.entity.LaborCost;
import com.reggie.entity.OtherCost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 成本核算服务接口
 *
 * @author reggie
 * @since 2026-08-10
 */
public interface CostService extends IService<DishCost> {

    // ==================== 菜品成本管理 ====================

    /**
     * 获取菜品成本列表
     *
     * @param tenantId 租户ID
     * @return 菜品成本列表
     */
    List<DishCost> getDishCostList(Long tenantId);

    /**
     * 根据菜品ID获取成本
     *
     * @param dishId   菜品ID
     * @param tenantId 租户ID
     * @return 菜品成本
     */
    DishCost getDishCostByDishId(Long dishId, Long tenantId);

    /**
     * 保存或更新菜品成本
     *
     * @param dishCost 菜品成本
     * @return 是否成功
     */
    boolean saveOrUpdateDishCost(DishCost dishCost);

    /**
     * 删除菜品成本
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean deleteDishCost(Long id);

    /**
     * 批量更新菜品成本
     *
     * @param dishCosts 菜品成本列表
     * @return 是否成功
     */
    boolean batchUpdateDishCost(List<DishCost> dishCosts);

    // ==================== 成本记录管理 ====================

    /**
     * 获取成本记录列表
     *
     * @param costType 成本类型
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 成本记录列表
     */
    List<CostRecord> getCostRecordList(Integer costType, LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * 保存成本记录
     *
     * @param costRecord 成本记录
     * @return 是否成功
     */
    boolean saveCostRecord(CostRecord costRecord);

    /**
     * 删除成本记录
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean deleteCostRecord(Long id);

    // ==================== 人工成本管理 ====================

    /**
     * 获取人工成本列表
     *
     * @param costMonth 成本月份
     * @param tenantId  租户ID
     * @return 人工成本列表
     */
    List<LaborCost> getLaborCostList(LocalDate costMonth, Long tenantId);

    /**
     * 保存或更新人工成本
     *
     * @param laborCost 人工成本
     * @return 是否成功
     */
    boolean saveOrUpdateLaborCost(LaborCost laborCost);

    /**
     * 删除人工成本
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean deleteLaborCost(Long id);

    /**
     * 批量保存人工成本
     *
     * @param laborCosts 人工成本列表
     * @return 是否成功
     */
    boolean batchSaveLaborCost(List<LaborCost> laborCosts);

    // ==================== 其他成本管理 ====================

    /**
     * 获取其他成本列表
     *
     * @param costType  成本类型
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 其他成本列表
     */
    List<OtherCost> getOtherCostList(Integer costType, LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * 保存或更新其他成本
     *
     * @param otherCost 其他成本
     * @return 是否成功
     */
    boolean saveOrUpdateOtherCost(OtherCost otherCost);

    /**
     * 删除其他成本
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean deleteOtherCost(Long id);

    // ==================== 成本统计分析 ====================

    /**
     * 获取成本汇总统计
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 成本汇总
     */
    Map<String, Object> getCostSummary(LocalDate startDate, LocalDate endDate, Long tenantId);

    /**
     * 获取成本趋势分析
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 成本趋势
     */
    Map<String, Object> getCostTrend(LocalDate startDate, LocalDate endDate, Long tenantId);

    /**
     * 获取成本结构分析
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 成本结构
     */
    Map<String, Object> getCostStructure(LocalDate startDate, LocalDate endDate, Long tenantId);

    /**
     * 获取菜品成本排行
     *
     * @param limit    排行数量
     * @param tenantId 租户ID
     * @return 菜品成本排行
     */
    List<Map<String, Object>> getDishCostRanking(int limit, Long tenantId);

    /**
     * 计算菜品毛利率
     *
     * @param dishId   菜品ID
     * @param tenantId 租户ID
     * @return 毛利率
     */
    BigDecimal calculateProfitRate(Long dishId, Long tenantId);

    /**
     * 获取成本预警列表
     *
     * @param threshold 预警阈值
     * @param tenantId  租户ID
     * @return 成本预警列表
     */
    List<Map<String, Object>> getCostAlert(BigDecimal threshold, Long tenantId);
}
