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
    BigDecimal calculateDiscount(Long campaignId, BigDecimal orderAmount, List<Long> dishIds, Long userId,
            Long tenantId);

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

    // ==================== C 端满减（生效活动 / 凑单试算 / 核销） ====================

    /**
     * 查询当前生效满减活动的全部启用档位（不含金额，供首页凑单进度条本地计算差额）
     *
     * @param tenantId 租户ID
     * @return 档位列表，每项含 campaignId/ruleId/ruleName/discountType/minAmount/discountValue/maxDiscountAmount
     */
    List<Map<String, Object>> getActiveFullReductionTiers(Long tenantId);

    /**
     * 权威满减试算（结算页展示与下单计费同源，永不漂移）。
     *
     * @param goodsAmount 商品金额（不含配送费）
     * @param userId      用户ID
     * @param tenantId    租户ID
     * @return 试算结果：available/discount/命中档/下一档门槛/差额/进度/全部档位
     */
    Map<String, Object> evaluateFullReduction(BigDecimal goodsAmount, Long userId, Long tenantId);

    /**
     * 记录满减核销（订单落库成功后写入 campaign_usage_record，支撑每人限次与对账）。
     *
     * @param campaignId   活动ID
     * @param ruleId       规则ID
     * @param orderId      订单ID
     * @param orderNumber  订单号
     * @param userId       用户ID
     * @param goodsAmount  商品金额
     * @param discount     满减优惠金额
     * @param actualAmount 满减后商品金额
     * @param tenantId     租户ID
     */
    void recordFullReductionUsage(Long campaignId, Long ruleId, Long orderId, String orderNumber, Long userId,
            BigDecimal goodsAmount, BigDecimal discount, BigDecimal actualAmount, Long tenantId);

    // ==================== 使用记录 ====================

    /**
     * 获取使用记录列表
     *
     * @param campaignId 活动ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param tenantId   租户ID
     * @return 使用记录列表
     */
    List<CampaignUsageRecord> getUsageRecords(Long campaignId, LocalDateTime startDate, LocalDateTime endDate,
            Long tenantId);

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
