package com.reggie.module.inventory.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 智能补货服务
 * <p>
 * 面向中小餐厅老板，基于历史出库流水数据，自动计算每种食材的补货建议和紧急度。
 * 核心算法：加权日均消耗（近7天权重1.5）+ 安全库存 + 补货周期 - 当前库存
 * </p>
 *
 * @author reggie
 * @since 2026-08-23
 */
public interface ReplenishService {

    /**
     * 获取智能补货看板数据
     * <p>
     * 汇总返回：
     * - totalSuggestCount: 需补食材总数
     * - urgentCount: 紧急（≤1天）数量
     * - criticalCount: 紧迫（≤3天）数量
     * - warningCount: 关注（≤7天）数量
     * - totalAmount: 补货总金额预估（所有建议的 suggestQty × unitPrice 之和）
     * - top5: 即将断货TOP5（按 estimatedDays 升序取前5）
     * - suggestions: 完整补货建议列表
     * </p>
     *
     * @param tenantId 租户ID
     * @return 看板汇总数据
     */
    Map<String, Object> getReplenishDashboard(Long tenantId);

    /**
     * 获取智能补货建议列表
     * <p>
     * 算法：加权日均消耗（近7天权重1.5）+ 安全库存 + 补货周期 - 当前库存
     * 只返回需要补货的食材（suggestQty > 0）
     * </p>
     *
     * @param tenantId       租户ID
     * @param days           统计天数（默认30天）
     * @param replenishCycle 补货周期天数（默认14天）
     * @return 补货建议列表，按紧急度降序、预计可用天数升序排列
     */
    List<Map<String, Object>> getSmartReplenishSuggest(Long tenantId, int days, int replenishCycle);

    /**
     * 计算加权日均消耗
     * <p>
     * 查询近 N 天该食材的出库记录，按天汇总出库量，然后加权平均。
     * 最近7天 weight=1.5，其余天 weight=1.0
     * 加权消耗 = SUM(每日出库量 × weight) / SUM(weight)
     * 若无历史数据，返回 BigDecimal.ZERO
     * </p>
     *
     * @param materialId 食材ID
     * @param days       统计天数
     * @param tenantId   租户ID
     * @return 加权日均消耗量
     */
    BigDecimal calcWeightedDailyUsage(Long materialId, int days, Long tenantId);

    /**
     * 计算预计可用天数
     * <p>
     * estimatedDays = stockQty / dailyUsage
     * 若 dailyUsage 为0或空，返回 Integer.MAX_VALUE（表示无限期可用）
     * </p>
     *
     * @param stockQty   当前库存量
     * @param dailyUsage 日均消耗量
     * @return 预计可用天数（整数，向下取整）
     */
    int calcEstimatedDays(BigDecimal stockQty, BigDecimal dailyUsage);

    /**
     * 计算紧急度等级
     * <p>
     * 0=充足（>7天）
     * 1=关注（≤7天）
     * 2=紧迫（≤3天）
     * 3=紧急（≤1天）
     * </p>
     *
     * @param estimatedDays 预计可用天数
     * @return 紧急度等级字符串
     */
    String calcUrgency(int estimatedDays);
}