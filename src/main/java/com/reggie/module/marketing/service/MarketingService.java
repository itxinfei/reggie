package com.reggie.module.marketing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.marketing.model.FullReductionRule;
import com.reggie.module.marketing.model.DiscountRule;
import com.reggie.module.marketing.model.CampaignUsageRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 营销活动服务接口
 *
 * @author reggie
 * @since 2026-08-11
 */
public interface MarketingService extends IService<FullReductionRule> {

    // ==================== 满减规则管理 ====================

    /**
     * 获取满减规则列表
     *
     * @param campaignId 活动ID
     * @param tenantId   租户ID
     * @return 满减规则列表
     */
    List<FullReductionRule> getFullReductionRules(Long campaignId, Long tenantId);

    /**
     * 保存或更新满减规则
     *
     * @param rule 满减规则
     * @return 是否成功
     */
    boolean saveOrUpdateFullReductionRule(FullReductionRule rule);

    /**
     * 删除满减规则
     *
     * @param id 规则ID
     * @return 是否成功
     */
    boolean deleteFullReductionRule(Long id);

    /**
     * 批量保存满减规则
     *
     * @param rules 规则列表
     * @return 是否成功
     */
    boolean batchSaveFullReductionRules(List<FullReductionRule> rules);

    // ==================== 折扣规则管理 ====================

    /**
     * 获取折扣规则列表
     *
     * @param campaignId 活动ID
     * @param tenantId   租户ID
     * @return 折扣规则列表
     */
    List<DiscountRule> getDiscountRules(Long campaignId, Long tenantId);

    /**
     * 保存或更新折扣规则
     *
     * @param rule 折扣规则
     * @return 是否成功
     */
    boolean saveOrUpdateDiscountRule(DiscountRule rule);

    /**
     * 删除折扣规则
     *
     * @param id 规则ID
     * @return 是否成功
     */
    boolean deleteDiscountRule(Long id);

    /**
     * 批量保存折扣规则
     *
     * @param rules 规则列表
     * @return 是否成功
     */
    boolean batchSaveDiscountRules(List<DiscountRule> rules);

    // ==================== 营销计算 ====================

    /**
     * 计算满减优惠
     *
     * @param campaignId 活动ID
     * @param orderAmount 订单金额
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @return 优惠金额
     */
    BigDecimal calculateFullReduction(Long campaignId, BigDecimal orderAmount, Long userId, Long tenantId);

    /**
     * 计算折扣优惠
     *
     * @param campaignId 活动ID
     * @param orderAmount 订单金额
     * @param dishIds    菜品ID列表
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @return 优惠金额
     */
    BigDecimal calculateDiscount(Long campaignId, BigDecimal orderAmount, List<Long> dishIds, Long userId, Long tenantId);

    /**
     * 计算最优优惠
     *
     * @param orderAmount 订单金额
     * @param dishIds    菜品ID列表
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @return 最优优惠信息
     */
    Map<String, Object> calculateBestDiscount(BigDecimal orderAmount, List<Long> dishIds, Long userId, Long tenantId);

    // ==================== 使用记录 ====================

    /**
     * 保存使用记录
     *
     * @param record 使用记录
     * @return 是否成功
     */
    boolean saveUsageRecord(CampaignUsageRecord record);

    /**
     * 获取使用记录列表
     *
     * @param campaignId 活动ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param tenantId   租户ID
     * @return 使用记录列表
     */
    List<CampaignUsageRecord> getUsageRecords(Long campaignId, LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * 获取用户使用次数
     *
     * @param campaignId 活动ID
     * @param ruleId     规则ID
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @return 使用次数
     */
    int getUserUsageCount(Long campaignId, Long ruleId, Long userId, Long tenantId);

    // ==================== 统计分析 ====================

    /**
     * 获取营销活动统计
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 统计数据
     */
    Map<String, Object> getMarketingStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * 获取满减活动效果统计
     *
     * @param campaignId 活动ID
     * @param tenantId   租户ID
     * @return 效果统计
     */
    Map<String, Object> getFullReductionEffect(Long campaignId, Long tenantId);

    /**
     * 获取折扣活动效果统计
     *
     * @param campaignId 活动ID
     * @param tenantId   租户ID
     * @return 效果统计
     */
    Map<String, Object> getDiscountEffect(Long campaignId, Long tenantId);

    /**
     * 获取营销趋势分析
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 趋势数据
     */
    Map<String, Object> getMarketingTrend(LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * 获取热门活动排行
     *
     * @param limit    排行数量
     * @param tenantId 租户ID
     * @return 热门活动列表
     */
    List<Map<String, Object>> getTopActivities(int limit, Long tenantId);
}
