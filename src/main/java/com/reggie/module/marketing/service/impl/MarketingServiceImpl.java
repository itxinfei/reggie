package com.reggie.module.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.marketing.mapper.FullReductionRuleMapper;
import com.reggie.module.marketing.model.FullReductionRule;
import com.reggie.module.marketing.model.DiscountRule;
import com.reggie.module.marketing.model.CampaignUsageRecord;
import com.reggie.module.marketing.model.MarketingCampaign;
import com.reggie.module.marketing.mapper.DiscountRuleMapper;
import com.reggie.module.marketing.mapper.CampaignUsageRecordMapper;
import com.reggie.module.marketing.mapper.MarketingCampaignMapper;
import com.reggie.module.marketing.service.MarketingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 营销活动服务实现
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class MarketingServiceImpl extends ServiceImpl<FullReductionRuleMapper, FullReductionRule> implements
        MarketingService {

    @Autowired
    private FullReductionRuleMapper fullReductionRuleMapper;

    @Autowired
    private DiscountRuleMapper discountRuleMapper;

    @Autowired
    private CampaignUsageRecordMapper usageRecordMapper;

    /** 营销活动主表 Mapper（精确查询生效中的满减活动） */
    @Autowired
    private MarketingCampaignMapper campaignMapper;

    // ==================== 满减规则管理 ====================

    /**
     * 获取 full reduction rules。
     * @param campaignId 参数 campaignId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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

    /**
     * 保存 or update full reduction rule。
     * @param rule 参数 rule
     * @return 返回结果
     */
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

    /**
     * 删除 full reduction rule。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFullReductionRule(Long id) {
        return fullReductionRuleMapper.deleteById(id) > 0;
    }

    /**
     * 批量处理 save full reduction rules。
     * @param rules 参数 rules
     * @return 返回结果
     */
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

    /**
     * 获取 discount rules。
     * @param campaignId 参数 campaignId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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

    /**
     * 保存 or update discount rule。
     * @param rule 参数 rule
     * @return 返回结果
     */
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

    /**
     * 删除 discount rule。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDiscountRule(Long id) {
        return discountRuleMapper.deleteById(id) > 0;
    }

    /**
     * 批量处理 save discount rules。
     * @param rules 参数 rules
     * @return 返回结果
     */
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

    /**
     * 计算 full reduction。
     * @param campaignId 参数 campaignId
     * @param orderAmount 参数 orderAmount
     * @param userId 参数 userId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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
            if (rule.getMinAmount() == null || orderAmount.compareTo(rule.getMinAmount()) >= 0) {
                // 检查用户使用次数（等价重构：合并为单个条件，避免嵌套）
                if (rule.getPerUserLimit() != null && rule.getPerUserLimit() > 0
                        && getUserUsageCount(campaignId, rule.getId(), userId, tenantId) >= rule.getPerUserLimit()) {
                    continue;
                }

                BigDecimal discount = BigDecimal.ZERO;
                if (rule.getDiscountType() == FullReductionRule.TYPE_REDUCE_AMOUNT) {
                    // 减固定金额
                    discount = rule.getDiscountValue() != null ? rule.getDiscountValue() : BigDecimal.ZERO;
                } else if (rule.getDiscountType() == FullReductionRule.TYPE_DISCOUNT) {
                    // 打折
                    BigDecimal discountValue = rule.getDiscountValue() != null ? rule.getDiscountValue() : BigDecimal
                            .ZERO;
                    discount = orderAmount.multiply(BigDecimal.ONE.subtract(discountValue));
                    BigDecimal maxDiscount = rule.getMaxDiscountAmount();
                    discount = maxDiscount == null ? discount : discount.min(maxDiscount);
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

    /**
     * 计算 discount。
     * @param campaignId 参数 campaignId
     * @param orderAmount 参数 orderAmount
     * @param dishIds 参数 dishIds
     * @param userId 参数 userId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public BigDecimal calculateDiscount(Long campaignId, BigDecimal orderAmount, List<Long> dishIds, Long userId,
            Long tenantId) {
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
                BigDecimal discountRate = rule.getDiscountRate() != null ? rule.getDiscountRate() : BigDecimal.ZERO;
                discount = orderAmount.multiply(BigDecimal.ONE.subtract(discountRate));
            } else if (rule.getScope() == DiscountRule.SCOPE_DISH && dishIds != null) {
                // 指定菜品折扣（简化处理，实际需要查询菜品价格）
                BigDecimal discountRate = rule.getDiscountRate() != null ? rule.getDiscountRate() : BigDecimal.ZERO;
                discount = orderAmount.multiply(BigDecimal.ONE.subtract(discountRate));
            }

            // 限制最大优惠金额
            if (rule.getMaxDiscountAmount() != null && discount.compareTo(rule.getMaxDiscountAmount()) > 0) {
                discount = rule.getMaxDiscountAmount();
            }

            totalDiscount = totalDiscount.add(discount);
        }

        return totalDiscount;
    }

    /**
     * 计算 best discount。
     * @param orderAmount 参数 orderAmount
     * @param dishIds 参数 dishIds
     * @param userId 参数 userId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> calculateBestDiscount(BigDecimal orderAmount, List<Long> dishIds, Long userId,
            Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查询所有进行中的营销活动（等价抽取）
        List<FullReductionRule> allFrRules = loadActiveFrRules(tenantId);
        List<DiscountRule> allDrRules = loadActiveDrRules(tenantId);

        // 一次性批量查询当前用户在该租户下所有满减规则的使用记录，内存统计，避免 N+1（等价抽取）
        Map<String, Integer> usageCountMap = loadUsageCountMap(userId, tenantId);

        // 计算满减优惠
        BigDecimal frDiscount = BigDecimal.ZERO;
        FullReductionRule bestFrRule = null;
        for (FullReductionRule rule : allFrRules) {
            if (rule.getMinAmount() == null || orderAmount.compareTo(rule.getMinAmount()) >= 0) {
                // 检查每人限用次数（从内存 Map 中获取，避免逐条数据库查询）
                String usageKey = rule.getCampaignId() + "_" + rule.getId();
                if (rule.getPerUserLimit() != null && rule.getPerUserLimit() > 0
                        && usageCountMap.getOrDefault(usageKey, 0) >= rule.getPerUserLimit()) {
                    continue;
                }
                BigDecimal discount = BigDecimal.ZERO;
                if (rule.getDiscountType() == FullReductionRule.TYPE_REDUCE_AMOUNT) {
                    discount = rule.getDiscountValue() != null ? rule.getDiscountValue() : BigDecimal.ZERO;
                } else if (rule.getDiscountType() == FullReductionRule.TYPE_DISCOUNT) {
                    BigDecimal discountValue = rule.getDiscountValue() != null ? rule.getDiscountValue() : BigDecimal
                            .ZERO;
                    discount = orderAmount.multiply(BigDecimal.ONE.subtract(discountValue));
                    BigDecimal maxDiscount = rule.getMaxDiscountAmount();
                    discount = maxDiscount == null ? discount : discount.min(maxDiscount);
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
            BigDecimal discountRate = rule.getDiscountRate() != null ? rule.getDiscountRate() : BigDecimal.ZERO;
            BigDecimal discount = orderAmount.multiply(BigDecimal.ONE.subtract(discountRate));
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

    // ==================== C 端满减（生效活动 / 凑单试算 / 核销） ====================

    /**
     * 查询当前生效满减活动（类型=满减、状态=进行中、当前时间在活动区间）下的全部启用档位。
     * <p>与 {@link #loadActiveFrRules} 的区别：本方法校验活动本身状态/时间/类型，可用于计费。</p>
     */
    private List<FullReductionRule> loadActiveFullReductionRules(Long tenantId) {
        List<FullReductionRule> all = new ArrayList<>();
        if (tenantId == null) {
            return all;
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<MarketingCampaign> campaignQw = new LambdaQueryWrapper<>();
        campaignQw.eq(MarketingCampaign::getTenantId, tenantId)
                .eq(MarketingCampaign::getCampaignType, MarketingCampaign.TYPE_FULL_REDUCTION)
                .eq(MarketingCampaign::getStatus, MarketingCampaign.STATUS_ACTIVE)
                .le(MarketingCampaign::getStartTime, now)
                .ge(MarketingCampaign::getEndTime, now);
        List<MarketingCampaign> campaigns = campaignMapper.selectList(campaignQw);
        for (MarketingCampaign campaign : campaigns) {
            all.addAll(getFullReductionRules(campaign.getId(), tenantId));
        }
        return all;
    }

    /**
     * 计算单个档位在给定商品金额下的优惠金额。
     * <p>减固定金额=discountValue；打折=金额×(1-折扣率)，受 maxDiscountAmount 封顶；赠品不计金额优惠。</p>
     */
    private BigDecimal ruleDiscount(FullReductionRule rule, BigDecimal goodsAmount) {
        Integer type = rule.getDiscountType();
        if (type != null && type == FullReductionRule.TYPE_REDUCE_AMOUNT) {
            return rule.getDiscountValue() != null ? rule.getDiscountValue() : BigDecimal.ZERO;
        }
        if (type != null && type == FullReductionRule.TYPE_DISCOUNT) {
            BigDecimal rate = rule.getDiscountValue() != null ? rule.getDiscountValue() : BigDecimal.ZERO;
            BigDecimal discount = goodsAmount.multiply(BigDecimal.ONE.subtract(rate));
            return rule.getMaxDiscountAmount() != null ? discount.min(rule.getMaxDiscountAmount()) : discount;
        }
        return BigDecimal.ZERO;
    }

    @Override
    public List<Map<String, Object>> getActiveFullReductionTiers(Long tenantId) {
        List<FullReductionRule> rules = loadActiveFullReductionRules(tenantId);
        List<Map<String, Object>> tiers = new ArrayList<>();
        for (FullReductionRule rule : rules) {
            Map<String, Object> tier = new HashMap<>();
            tier.put("campaignId", rule.getCampaignId());
            tier.put("ruleId", rule.getId());
            tier.put("ruleName", rule.getRuleName());
            tier.put("discountType", rule.getDiscountType());
            tier.put("minAmount", rule.getMinAmount());
            tier.put("discountValue", rule.getDiscountValue());
            tier.put("maxDiscountAmount", rule.getMaxDiscountAmount());
            tiers.add(tier);
        }
        return tiers;
    }

    @Override
    public Map<String, Object> evaluateFullReduction(BigDecimal goodsAmount, Long userId, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        BigDecimal amount = goodsAmount != null ? goodsAmount : BigDecimal.ZERO;
        result.put("goodsAmount", amount.setScale(2, RoundingMode.HALF_UP));

        List<FullReductionRule> rules = loadActiveFullReductionRules(tenantId);
        if (rules.isEmpty()) {
            result.put("available", false);
            result.put("hit", false);
            result.put("discount", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.put("gap", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.put("progress", 0);
            result.put("tiers", new ArrayList<>());
            return result;
        }
        result.put("available", true);

        // 一次性查当前用户各档核销计数（内存过滤 perUserLimit，避免 N+1）
        Map<String, Integer> usageMap = userId != null
                ? loadUsageCountMap(userId, tenantId) : new HashMap<>();

        List<Map<String, Object>> tierViews = new ArrayList<>();
        BigDecimal bestDiscount = BigDecimal.ZERO;
        FullReductionRule hitRule = null;
        FullReductionRule nextRule = null;

        for (FullReductionRule rule : rules) {
            BigDecimal min = rule.getMinAmount() != null ? rule.getMinAmount() : BigDecimal.ZERO;
            boolean reached = amount.compareTo(min) >= 0;
            boolean limited = false;
            if (reached && rule.getPerUserLimit() != null && rule.getPerUserLimit() > 0) {
                Integer used = usageMap.get(rule.getCampaignId() + "_" + rule.getId());
                if (used != null && used >= rule.getPerUserLimit()) {
                    limited = true;
                }
            }
            BigDecimal discount = reached && !limited ? ruleDiscount(rule, amount) : BigDecimal.ZERO;

            Map<String, Object> view = new HashMap<>();
            view.put("ruleId", rule.getId());
            view.put("ruleName", rule.getRuleName());
            view.put("discountType", rule.getDiscountType());
            view.put("minAmount", min);
            view.put("reached", reached);
            view.put("limited", limited);
            view.put("previewDiscount", discount.setScale(2, RoundingMode.HALF_UP));
            tierViews.add(view);

            // 命中：已达成且未超每人限次，取优惠最大的一档
            if (reached && !limited && discount.compareTo(bestDiscount) > 0) {
                bestDiscount = discount;
                hitRule = rule;
            }
            // 下一档：门槛高于当前金额的最近一档
            if (!reached && (nextRule == null || min.compareTo(nextRule.getMinAmount()) < 0)) {
                nextRule = rule;
            }
        }

        result.put("tiers", tierViews);
        result.put("discount", bestDiscount.setScale(2, RoundingMode.HALF_UP));
        if (hitRule != null) {
            result.put("hit", true);
            result.put("campaignId", hitRule.getCampaignId());
            result.put("ruleId", hitRule.getId());
            result.put("ruleName", hitRule.getRuleName());
            result.put("discountType", hitRule.getDiscountType());
            result.put("currentMinAmount", hitRule.getMinAmount());
        } else {
            result.put("hit", false);
        }

        if (nextRule != null) {
            BigDecimal gap = nextRule.getMinAmount().subtract(amount);
            if (gap.compareTo(BigDecimal.ZERO) < 0) {
                gap = BigDecimal.ZERO;
            }
            result.put("nextMinAmount", nextRule.getMinAmount());
            result.put("nextRuleName", nextRule.getRuleName());
            result.put("nextDiscountType", nextRule.getDiscountType());
            // 达成下一档门槛时可享优惠（按门槛金额试算，供凑单文案；类型3赠品为0）
            result.put("nextDiscount", ruleDiscount(nextRule, nextRule.getMinAmount())
                    .setScale(2, RoundingMode.HALF_UP));
            result.put("gap", gap.setScale(2, RoundingMode.HALF_UP));
            // 进度 = 当前金额 / 下一档门槛 ×100（0~99，达成即100）
            BigDecimal progress = amount.multiply(BigDecimal.valueOf(100))
                    .divide(nextRule.getMinAmount(), 0, RoundingMode.DOWN);
            if (progress.compareTo(BigDecimal.valueOf(99)) > 0) {
                progress = BigDecimal.valueOf(99);
            }
            result.put("progress", progress.intValue());
        } else {
            // 无更高门槛档：已达最高档
            result.put("nextMinAmount", null);
            result.put("gap", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.put("progress", 100);
        }
        return result;
    }

    @Override
    public void recordFullReductionUsage(Long campaignId, Long ruleId, Long orderId, String orderNumber, Long userId,
            BigDecimal goodsAmount, BigDecimal discount, BigDecimal actualAmount, Long tenantId) {
        CampaignUsageRecord record = new CampaignUsageRecord();
        record.setCampaignId(campaignId);
        record.setRuleId(ruleId);
        record.setRuleType(1);
        record.setOrderId(orderId);
        record.setOrderNumber(orderNumber);
        record.setUserId(userId);
        record.setOrderAmount(goodsAmount);
        record.setDiscountAmount(discount);
        record.setActualAmount(actualAmount);
        record.setUseTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setTenantId(tenantId);
        usageRecordMapper.insert(record);
    }

    /**
     * 加载租户下进行中的满减规则（等价抽取）。
     */
    private List<FullReductionRule> loadActiveFrRules(Long tenantId) {
        LambdaQueryWrapper<FullReductionRule> frQw = new LambdaQueryWrapper<>();
        frQw.eq(FullReductionRule::getTenantId, tenantId);
        frQw.eq(FullReductionRule::getStatus, 1);
        return fullReductionRuleMapper.selectList(frQw);
    }

    /**
     * 加载租户下进行中的折扣规则（等价抽取）。
     */
    private List<DiscountRule> loadActiveDrRules(Long tenantId) {
        LambdaQueryWrapper<DiscountRule> drQw = new LambdaQueryWrapper<>();
        drQw.eq(DiscountRule::getTenantId, tenantId);
        drQw.eq(DiscountRule::getStatus, 1);
        return discountRuleMapper.selectList(drQw);
    }

    /**
     * 一次性加载用户满减规则使用次数映射 (campaignId_ruleId -> 次数)，避免 N+1（等价抽取）。
     */
    private Map<String, Integer> loadUsageCountMap(Long userId, Long tenantId) {
        List<CampaignUsageRecord> allUsageRecords;
        if (userId != null && tenantId != null) {
            LambdaQueryWrapper<CampaignUsageRecord> usageQw = new LambdaQueryWrapper<>();
            usageQw.eq(CampaignUsageRecord::getUserId, userId);
            usageQw.eq(CampaignUsageRecord::getTenantId, tenantId);
            usageQw.eq(CampaignUsageRecord::getRuleType, 1); // 只查满减类型的记录
            allUsageRecords = usageRecordMapper.selectList(usageQw);
        } else {
            allUsageRecords = new ArrayList<>();
        }
        Map<String, Integer> usageCountMap = new HashMap<>();
        for (CampaignUsageRecord record : allUsageRecords) {
            String key = record.getCampaignId() + "_" + record.getRuleId();
            usageCountMap.put(key, usageCountMap.getOrDefault(key, 0) + 1);
        }
        return usageCountMap;
    }

    // ==================== 使用记录 ====================

    /**
     * 获取 usage records。
     * @param campaignId 参数 campaignId
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<CampaignUsageRecord> getUsageRecords(Long campaignId, LocalDateTime startDate, LocalDateTime endDate,
            Long tenantId) {
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

    /**
     * 获取 user usage count。
     * @param campaignId 参数 campaignId
     * @param ruleId 参数 ruleId
     * @param userId 参数 userId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public int getUserUsageCount(Long campaignId, Long ruleId, Long userId, Long tenantId) {
        LambdaQueryWrapper<CampaignUsageRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(CampaignUsageRecord::getCampaignId, campaignId);
        qw.eq(CampaignUsageRecord::getRuleId, ruleId);
        qw.eq(CampaignUsageRecord::getUserId, userId);
        if (tenantId != null) {
            qw.eq(CampaignUsageRecord::getTenantId, tenantId);
        }
        return usageRecordMapper.selectCount(qw).intValue();
    }

    // ==================== 统计分析 ====================

    /**
     * 获取 marketing statistics。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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
            totalDiscount = totalDiscount.add(record.getDiscountAmount() != null ? record
                    .getDiscountAmount() : BigDecimal.ZERO);
            totalOrderAmount = totalOrderAmount.add(record.getOrderAmount() != null ? record
                    .getOrderAmount() : BigDecimal.ZERO);
            if (record.getUserId() != null) {
                uniqueUsers.add(record.getUserId());
            }
        }

        result.put("totalDiscount", totalDiscount);
        result.put("totalOrderAmount", totalOrderAmount);
        result.put("totalUsageCount", totalUsageCount);
        result.put("uniqueUsers", uniqueUsers.size());
        result.put("avgDiscount", totalUsageCount > 0 ? totalDiscount.divide(BigDecimal.valueOf(totalUsageCount), 2,
                RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return result;
    }

    /**
     * 获取 full reduction effect。
     * @param campaignId 参数 campaignId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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
            totalDiscount = totalDiscount.add(record.getDiscountAmount() != null ? record
                    .getDiscountAmount() : BigDecimal.ZERO);
            totalOrderAmount = totalOrderAmount.add(record.getOrderAmount() != null ? record
                    .getOrderAmount() : BigDecimal.ZERO);
        }

        result.put("usageCount", records.size());
        result.put("totalDiscount", totalDiscount);
        result.put("totalOrderAmount", totalOrderAmount);
        result.put("avgOrderAmount", records.size() > 0 ? totalOrderAmount.divide(BigDecimal.valueOf(records.size()), 2,
                RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return result;
    }

    /**
     * 获取 discount effect。
     * @param campaignId 参数 campaignId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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
            totalDiscount = totalDiscount.add(record.getDiscountAmount() != null ? record
                    .getDiscountAmount() : BigDecimal.ZERO);
            totalOrderAmount = totalOrderAmount.add(record.getOrderAmount() != null ? record
                    .getOrderAmount() : BigDecimal.ZERO);
        }

        result.put("usageCount", records.size());
        result.put("totalDiscount", totalDiscount);
        result.put("totalOrderAmount", totalOrderAmount);
        result.put("avgDiscountRate", totalOrderAmount.compareTo(BigDecimal.ZERO) > 0 ?
                totalDiscount.divide(totalOrderAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")) : BigDecimal.ZERO);

        return result;
    }

    /**
     * 获取 marketing trend。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getMarketingTrend(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> discounts = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        // 一次性查询整个时间范围内的所有使用记录，然后在内存中按日期分组统计
        // 避免每天的 while 循环中都执行一次数据库查询（N+1 问题）
        LambdaQueryWrapper<CampaignUsageRecord> qw = new LambdaQueryWrapper<>();
        qw.ge(CampaignUsageRecord::getUseTime, startDate);
        qw.le(CampaignUsageRecord::getUseTime, endDate);
        if (tenantId != null) {
            qw.eq(CampaignUsageRecord::getTenantId, tenantId);
        }
        List<CampaignUsageRecord> allRecords = usageRecordMapper.selectList(qw);

        // 按日期字符串分组统计折扣金额和使用次数
        Map<String, List<CampaignUsageRecord>> recordsByDay = new HashMap<>();
        for (CampaignUsageRecord record : allRecords) {
            LocalDateTime useTime = record.getUseTime();
            if (useTime != null) {
                String dayStr = useTime.toLocalDate().toString();
                List<CampaignUsageRecord> dayList = recordsByDay.get(dayStr);
                if (dayList == null) {
                    dayList = new ArrayList<>();
                    recordsByDay.put(dayStr, dayList);
                }
                dayList.add(record);
            }
        }

        // 遍历日期范围，从内存分组中取出每天的统计数据
        LocalDateTime current = startDate;
        while (!current.isAfter(endDate)) {
            String dayStr = current.toLocalDate().toString();
            dates.add(dayStr);

            List<CampaignUsageRecord> dayRecords = recordsByDay.get(dayStr);
            if (dayRecords == null) {
                discounts.add(BigDecimal.ZERO);
                counts.add(0);
            } else {
                BigDecimal dayDiscount = BigDecimal.ZERO;
                for (CampaignUsageRecord record : dayRecords) {
                    dayDiscount = dayDiscount.add(record.getDiscountAmount() != null ? record
                            .getDiscountAmount() : BigDecimal.ZERO);
                }
                discounts.add(dayDiscount);
                counts.add(dayRecords.size());
            }

            current = current.plusDays(1);
        }

        result.put("dates", dates);
        result.put("discounts", discounts);
        result.put("counts", counts);

        return result;
    }

    /**
     * 获取 top activities。
     * @param limit 参数 limit
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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
