package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.delivery.model.DeliveryRangeRule;
import com.reggie.module.delivery.model.DeliveryFeeStep;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 配送增强服务接口
 *
 * @author reggie
 * @since 2026-08-11
 */
public interface DeliveryEnhancedService extends IService<DeliveryRangeRule> {

    // ==================== 配送范围管理 ====================

    /**
     * 获取配送范围规则列表
     *
     * @param tenantId 租户ID
     * @return 配送范围规则列表
     */
    List<DeliveryRangeRule> getRangeRules(Long tenantId);

    /**
     * 根据ID获取配送范围规则
     *
     * @param id 规则ID
     * @return 配送范围规则
     */
    DeliveryRangeRule getRangeRuleById(Long id);

    /**
     * 保存或更新配送范围规则
     *
     * @param rule 配送范围规则
     * @return 是否成功
     */
    boolean saveOrUpdateRangeRule(DeliveryRangeRule rule);

    /**
     * 删除配送范围规则
     *
     * @param id 规则ID
     * @return 是否成功
     */
    boolean deleteRangeRule(Long id);

    // ==================== 配送费阶梯管理 ====================

    /**
     * 获取配送费阶梯规则列表
     *
     * @param ruleId   规则ID
     * @param tenantId 租户ID
     * @return 配送费阶梯规则列表
     */
    List<DeliveryFeeStep> getFeeSteps(Long ruleId, Long tenantId);

    /**
     * 保存或更新配送费阶梯规则
     *
     * @param step 配送费阶梯规则
     * @return 是否成功
     */
    boolean saveOrUpdateFeeStep(DeliveryFeeStep step);

    /**
     * 删除配送费阶梯规则
     *
     * @param id 规则ID
     * @return 是否成功
     */
    boolean deleteFeeStep(Long id);

    /**
     * 批量保存配送费阶梯规则
     *
     * @param steps 配送费阶梯规则列表
     * @return 是否成功
     */
    boolean batchSaveFeeSteps(List<DeliveryFeeStep> steps);

    // ==================== 配送范围校验 ====================

    /**
     * 校验地址是否在配送范围内
     *
     * @param ruleId    规则ID
     * @param longitude 经度
     * @param latitude  纬度
     * @return 是否在范围内
     */
    boolean isInRange(Long ruleId, BigDecimal longitude, BigDecimal latitude);

    /**
     * 校验地址是否在任意配送范围内
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param tenantId  租户ID
     * @return 匹配的规则ID，null表示不在范围内
     */
    Long findMatchingRule(BigDecimal longitude, BigDecimal latitude, Long tenantId);

    // ==================== 配送费计算 ====================

    /**
     * 计算配送费
     *
     * @param ruleId    规则ID
     * @param distance  距离（米）
     * @param orderAmount 订单金额
     * @return 配送费
     */
    BigDecimal calculateDeliveryFee(Long ruleId, BigDecimal distance, BigDecimal orderAmount);

    /**
     * 计算配送费（自动匹配规则）
     *
     * @param longitude   经度
     * @param latitude    纬度
     * @param distance    距离（米）
     * @param orderAmount 订单金额
     * @param tenantId    租户ID
     * @return 配送费信息
     */
    Map<String, Object> calculateFee(BigDecimal longitude, BigDecimal latitude, BigDecimal distance, 
                                     BigDecimal orderAmount, Long tenantId);

    /**
     * 计算两点间距离（米）
     *
     * @param lon1 经度1
     * @param lat1 纬度1
     * @param lon2 经度2
     * @param lat2 纬度2
     * @return 距离（米）
     */
    BigDecimal calculateDistance(BigDecimal lon1, BigDecimal lat1, BigDecimal lon2, BigDecimal lat2);

    // ==================== 统计分析 ====================

    /**
     * 获取配送统计
     *
     * @param tenantId 租户ID
     * @return 配送统计
     */
    Map<String, Object> getDeliveryStatistics(Long tenantId);

    /**
     * 获取配送范围覆盖分析
     *
     * @param tenantId 租户ID
     * @return 覆盖分析
     */
    Map<String, Object> getRangeCoverage(Long tenantId);
}
