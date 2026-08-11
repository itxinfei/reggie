package com.reggie.module.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.marketing.mapper.FullReductionRuleMapper;
import com.reggie.module.marketing.model.FullReductionRule;
import com.reggie.module.marketing.model.DiscountRule;
import com.reggie.module.marketing.model.CampaignUsageRecord;
import com.reggie.module.marketing.mapper.DiscountRuleMapper;
import com.reggie.module.marketing.mapper.CampaignUsageRecordMapper;
import com.reggie.module.marketing.service.MarketingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 营销活动服务实现
 *
 * @author reggie
 * @since 2026-08-11
 */
@Slf4j
/**
 * Marketing service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class MarketingServiceImpl extends ServiceImpl<FullReductionRuleMapper, FullReductionRule> implements MarketingService {

    @Autowired
    private FullReductionRuleMapper fullReductionRuleMapper;

    @Autowired
    private DiscountRuleMapper discountRuleMapper;

    @Autowired
    private CampaignUsageRecordMapper usageRecordMapper;

    // ==================== 满减规则管理 ====================

    @Override
    public List<FullReductionRule> getFullReductionRules(Long campaignId, Long tenantId) {
        LambdaQueryWrapper<FullReductionRule> qw = new LambdaQueryWrapper<>();
        if (campaignId != null) {
            qw.eq(FullReductionRule::getCampaignId, campaignId);
        }
        if (tenantId != null) {
            qw.eq(FullReductionRule::getTenantId, tenantId);
        }
        qw.eq(FullReductionRule::getStatus, 1);
        qw.orderByAsc(FullReductionRule::getSortOrder);
        return fullReductionRuleMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateFullReductionRule(FullReductionRule rule) {
        if (rule.getId() == null) {
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            return fullReductionRuleMapper.insert(rule) > 0;
        } else {
            rule.setUpdateTime(LocalDateTime.now());
            return fullReductionRuleMapper.updateById(rule) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFullReductionRule(Long id) {
        return fullReductionRuleMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveFullReductionRules(List<FullReductionRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        for (FullReductionRule rule : rules) {
            saveOrUpdateFullReductionRule(rule);
        }
        return true;
    }

    // ==================== 折扣规则管理 ====================

    @Override
    public List<DiscountRule> getDiscountRules(Long campaignId, Long tenantId) {
        LambdaQueryWrapper<DiscountRule> qw = new LambdaQueryWrapper<>();
        if (campaignId != null) {
            qw.eq(DiscountRule::getCampaignId, campaignId);
        }
        if (tenantId != null) {
            qw.eq(DiscountRule::getTenantId, tenantId);
        }
        qw.eq(DiscountRule::getStatus, 1);
        qw.orderByAsc(DiscountRule::getSortOrder);
        return discountRuleMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateDiscountRule(DiscountRule rule) {
        if (rule.getId() == null) {
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            return discountRuleMapper.insert(rule) > 0;
        } else {
            rule.setUpdateTime(LocalDateTime.now());
            return discountRuleMapper.updateById(rule) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDiscountRule(Long id) {
        return discountRuleMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveDiscountRules(List<DiscountRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        for (DiscountRule rule : rules) {
            saveOrUpdateDiscountRule(rule);
        }
        return true;
    }

    // ==================== 营销计算 ====================

    @Override
    public BigDecimal calculateFullReduction(Long campaignId, BigDecimal orderAmount, Long userId, Long tenantId) {
        // 1. 获取活动的满减规则
        List<FullReductionRule> rules = getFullReductionRules(campaignId, tenantId);
        if (rules.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 2. 找到满足条件的最优规则
        FullReductionRule bestRule = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (FullReductionRule rule : rules) {
            // 检查是否满足最低消费
            if (orderAmount.compareTo(rule.getMinAmount()) >= 0) {
                // 检查用户使用次数
                if (rule.getPerUserLimit() != null && rule.getPerUserLimit() > 0) {
                    int usageCount = getUserUsageCount(campaignId, rule.getId(), userId, tenantId);
                    if (usageCount >= rule.getPerUserLimit()) {
                        continue;
                    }
                }

                BigDecimal discount = BigDecimal.ZERO;
                if (rule.getDiscountType() == FullReductionRule.TYPE_REDUCE_AMOUNT) {
                    // 减固定金额
                    discount = rule.getDiscountValue();
                } else if (rule.getDiscountType() == FullReductionRule.TYPE_DISCOUNT) {
                    // 打折
                    discount = orderAmount.multiply(BigDecimal.ONE.subtract(rule.getDiscountValue()));
                    if (rule.getMaxDiscountAmount() != null && discount.compareTo(rule.getMaxDiscountAmount()) > 0) {
                        discount = rule.getMaxDiscountAmount();
                    }
                }

                // 选择优惠最大的规则
                if (discount.compareTo(bestDiscount) > 0) {
                    bestDiscount = discount;
                    bestRule = rule;
                }
            }
        }

        return bestDiscount;
    }

    @Override
    public BigDecimal calculateDiscount(Long campaignId, BigDecimal orderAmount, List<Long> dishIds, Long userId, Long tenantId) {
        // 1. 获取活动的折扣规则
        List<DiscountRule> rules = getDiscountRules(campaignId, tenantId);
        if (rules.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 2. 找到满足条件的最优规则
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (DiscountRule rule : rules) {
            // 检查最低消费
            if (rule.getMinConsumption() != null && orderAmount.compareTo(rule.getMinConsumption()) < 0) {
                continue;
            }

            // 检查用户使用次数
            if (rule.getPerUserLimit() != null && rule.getPerUserLimit() > 0) {
                int usageCount = getUserUsageCount(campaignId, rule.getId(), userId, tenantId);
                if (usageCount >= rule.getPerUserLimit()) {
                    continue;
                }
            }

            BigDecimal discount = BigDecimal.ZERO;

            if (rule.getScope() == DiscountRule.SCOPE_ALL) {
                // 全场折扣
                discount = orderAmount.multiply(BigDecimal.ONE.subtract(rule.getDiscountRate()));
            } else if (rule.getScope() == DiscountRule.SCOPE_DISH && dishIds != null) {
                // 指定菜品折扣（简化处理，实际需要查询菜品价格）
                discount = orderAmount.multiply(BigDecimal.ONE.subtract(rule.getDiscountRate()));
            }

            // 限制最大优惠金额
            if (rule.getMaxDiscountAmount() != null && discount.compareTo(rule.getMaxDiscountAmount()) > 0) {
                discount = rule.getMaxDiscountAmount();
            }

            totalDiscount = totalDiscount.add(discount);
        }

        return totalDiscount;
    }

    @Override
    public Map<String, Object> calculateBestDiscount(BigDecimal orderAmount, List<Long> dishIds, Long userId, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查询所有进行中的营销活动
        LambdaQueryWrapper<FullReductionRule> frQw = new LambdaQueryWrapper<>();
        frQw.eq(FullReductionRule::getTenantId, tenantId);
        frQw.eq(FullReductionRule::getStatus, 1);
        List<FullReductionRule> allFrRules = fullReductionRuleMapper.selectList(frQw);

        LambdaQueryWrapper<DiscountRule> drQw = new LambdaQueryWrapper<>();
        drQw.eq(DiscountRule::getTenantId, tenantId);
        drQw.eq(DiscountRule::getStatus, 1);
        List<DiscountRule> allDrRules = discountRuleMapper.selectList(drQw);

        // 计算满减优惠
        BigDecimal frDiscount = BigDecimal.ZERO;
        FullReductionRule bestFrRule = null;
        for (FullReductionRule rule : allFrRules) {
            if (orderAmount.compareTo(rule.getMinAmount()) >= 0) {
                // 检查每人限用次数
                if (rule.getPerUserLimit() != null && rule.getPerUserLimit() > 0) {
                    int usageCount = getUserUsageCount(rule.getCampaignId(), rule.getId(), userId, tenantId);
                    if (usageCount >= rule.getPerUserLimit()) {
                        continue;
                    }
                }
                BigDecimal discount = BigDecimal.ZERO;
                if (rule.getDiscountType() == FullReductionRule.TYPE_REDUCE_AMOUNT) {
                    discount = rule.getDiscountValue();
                } else if (rule.getDiscountType() == FullReductionRule.TYPE_DISCOUNT) {
                    discount = orderAmount.multiply(BigDecimal.ONE.subtract(rule.getDiscountValue()));
                    if (rule.getMaxDiscountAmount() != null && discount.compareTo(rule.getMaxDiscountAmount()) > 0) {
                        discount = rule.getMaxDiscountAmount();
                    }
                }
                if (discount.compareTo(frDiscount) > 0) {
                    frDiscount = discount;
                    bestFrRule = rule;
                }
            }
        }

        // 计算折扣优惠
        BigDecimal drDiscount = BigDecimal.ZERO;
        DiscountRule bestDrRule = null;
        for (DiscountRule rule : allDrRules) {
            if (rule.getMinConsumption() != null && orderAmount.compareTo(rule.getMinConsumption()) < 0) {
                continue;
            }
            BigDecimal discount = orderAmount.multiply(BigDecimal.ONE.subtract(rule.getDiscountRate()));
            if (rule.getMaxDiscountAmount() != null && discount.compareTo(rule.getMaxDiscountAmount()) > 0) {
                discount = rule.getMaxDiscountAmount();
            }
            if (discount.compareTo(drDiscount) > 0) {
                drDiscount = discount;
                bestDrRule = rule;
            }
        }

        // 选择最优优惠
        if (frDiscount.compareTo(drDiscount) >= 0) {
            result.put("type", "full_reduction");
            result.put("discount", frDiscount);
            result.put("rule", bestFrRule);
            result.put("actualAmount", orderAmount.subtract(frDiscount));
        } else {
            result.put("type", "discount");
            result.put("discount", drDiscount);
            result.put("rule", bestDrRule);
            result.put("actualAmount", orderAmount.subtract(drDiscount));
        }

        result.put("orderAmount", orderAmount);
        return result;
    }

    // ==================== 使用记录 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveUsageRecord(CampaignUsageRecord record) {
        record.setCreateTime(LocalDateTime.now());
        return usageRecordMapper.insert(record) > 0;
    }

    @Override
    public List<CampaignUsageRecord> getUsageRecords(Long campaignId, LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        LambdaQueryWrapper<CampaignUsageRecord> qw = new LambdaQueryWrapper<>();
        if (campaignId != null) {
            qw.eq(CampaignUsageRecord::getCampaignId, campaignId);
        }
        if (startDate != null) {
            qw.ge(CampaignUsageRecord::getUseTime, startDate);
        }
        if (endDate != null) {
            qw.le(CampaignUsageRecord::getUseTime, endDate);
        }
        if (tenantId != null) {
            qw.eq(CampaignUsageRecord::getTenantId, tenantId);
        }
        qw.orderByDesc(CampaignUsageRecord::getUseTime);
        return usageRecordMapper.selectList(qw);
    }

    @Override
    public int getUserUsageCount(Long campaignId, Long ruleId, Long userId, Long tenantId) {
        LambdaQueryWrapper<CampaignUsageRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(CampaignUsageRecord::getCampaignId, campaignId);
        qw.eq(CampaignUsageRecord::getRuleId, ruleId);
        qw.eq(CampaignUsageRecord::getUserId, userId);
        if (tenantId != null) {
            qw.eq(CampaignUsageRecord::getTenantId, tenantId);
        }
        return (int) usageRecordMapper.selectCount(qw);
    }

    // ==================== 统计分析 ====================

    @Override
    public Map<String, Object> getMarketingStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查询使用记录
        List<CampaignUsageRecord> records = getUsageRecords(null, startDate, endDate, tenantId);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        int totalUsageCount = records.size();
        Set<Long> uniqueUsers = new HashSet<>();

        for (CampaignUsageRecord record : records) {
            totalDiscount = totalDiscount.add(record.getDiscountAmount() != null ? record.getDiscountAmount() : BigDecimal.ZERO);
            totalOrderAmount = totalOrderAmount.add(record.getOrderAmount() != null ? record.getOrderAmount() : BigDecimal.ZERO);
            if (record.getUserId() != null) {
                uniqueUsers.add(record.getUserId());
            }
        }

        result.put("totalDiscount", totalDiscount);
        result.put("totalOrderAmount", totalOrderAmount);
        result.put("totalUsageCount", totalUsageCount);
        result.put("uniqueUsers", uniqueUsers.size());
        result.put("avgDiscount", totalUsageCount > 0 ? totalDiscount.divide(BigDecimal.valueOf(totalUsageCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return result;
    }

    @Override
    public Map<String, Object> getFullReductionEffect(Long campaignId, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<CampaignUsageRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(CampaignUsageRecord::getCampaignId, campaignId);
        qw.eq(CampaignUsageRecord::getRuleType, 1); // 满减
        if (tenantId != null) {
            qw.eq(CampaignUsageRecord::getTenantId, tenantId);
        }
        List<CampaignUsageRecord> records = usageRecordMapper.selectList(qw);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        for (CampaignUsageRecord record : records) {
            totalDiscount = totalDiscount.add(record.getDiscountAmount() != null ? record.getDiscountAmount() : BigDecimal.ZERO);
            totalOrderAmount = totalOrderAmount.add(record.getOrderAmount() != null ? record.getOrderAmount() : BigDecimal.ZERO);
        }

        result.put("usageCount", records.size());
        result.put("totalDiscount", totalDiscount);
        result.put("totalOrderAmount", totalOrderAmount);
        result.put("avgOrderAmount", records.size() > 0 ? totalOrderAmount.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return result;
    }

    @Override
    public Map<String, Object> getDiscountEffect(Long campaignId, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<CampaignUsageRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(CampaignUsageRecord::getCampaignId, campaignId);
        qw.eq(CampaignUsageRecord::getRuleType, 2); // 折扣
        if (tenantId != null) {
            qw.eq(CampaignUsageRecord::getTenantId, tenantId);
        }
        List<CampaignUsageRecord> records = usageRecordMapper.selectList(qw);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        for (CampaignUsageRecord record : records) {
            totalDiscount = totalDiscount.add(record.getDiscountAmount() != null ? record.getDiscountAmount() : BigDecimal.ZERO);
            totalOrderAmount = totalOrderAmount.add(record.getOrderAmount() != null ? record.getOrderAmount() : BigDecimal.ZERO);
        }

        result.put("usageCount", records.size());
        result.put("totalDiscount", totalDiscount);
        result.put("totalOrderAmount", totalOrderAmount);
        result.put("avgDiscountRate", totalOrderAmount.compareTo(BigDecimal.ZERO) > 0 ?
                totalDiscount.divide(totalOrderAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : BigDecimal.ZERO);

        return result;
    }

    @Override
    public Map<String, Object> getMarketingTrend(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> discounts = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        // 按天统计
        LocalDateTime current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current.toLocalDate().toString());

            LambdaQueryWrapper<CampaignUsageRecord> qw = new LambdaQueryWrapper<>();
            qw.ge(CampaignUsageRecord::getUseTime, current.toLocalDate().atStartOfDay());
            qw.le(CampaignUsageRecord::getUseTime, current.toLocalDate().atTime(23, 59, 59));
            if (tenantId != null) {
                qw.eq(CampaignUsageRecord::getTenantId, tenantId);
            }
            List<CampaignUsageRecord> records = usageRecordMapper.selectList(qw);

            BigDecimal dayDiscount = BigDecimal.ZERO;
            for (CampaignUsageRecord record : records) {
                dayDiscount = dayDiscount.add(record.getDiscountAmount() != null ? record.getDiscountAmount() : BigDecimal.ZERO);
            }

            discounts.add(dayDiscount);
            counts.add(records.size());

            current = current.plusDays(1);
        }

        result.put("dates", dates);
        result.put("discounts", discounts);
        result.put("counts", counts);

        return result;
    }

    @Override
    public List<Map<String, Object>> getTopActivities(int limit, Long tenantId) {
        // 统计每个活动的使用次数
        LambdaQueryWrapper<CampaignUsageRecord> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(CampaignUsageRecord::getTenantId, tenantId);
        }
        List<CampaignUsageRecord> allRecords = usageRecordMapper.selectList(qw);

        Map<Long, Integer> campaignCountMap = new HashMap<>();
        Map<Long, BigDecimal> campaignDiscountMap = new HashMap<>();

        for (CampaignUsageRecord record : allRecords) {
            Long campaignId = record.getCampaignId();
            campaignCountMap.merge(campaignId, 1, Integer::sum);
            campaignDiscountMap.merge(campaignId,
                    record.getDiscountAmount() != null ? record.getDiscountAmount() : BigDecimal.ZERO,
                    BigDecimal::add);
        }

        // 排序并取前N个
        List<Map<String, Object>> result = new ArrayList<>();
        campaignCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .forEach(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("campaignId", entry.getKey());
                    item.put("usageCount", entry.getValue());
                    item.put("totalDiscount", campaignDiscountMap.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                    result.add(item);
                });

        return result;
    }
}

