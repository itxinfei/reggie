package com.reggie.module.recommend.service.analytics.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.recommend.mapper.BrowseHistoryMapper;
import com.reggie.module.recommend.mapper.RecommendationCacheMapper;
import com.reggie.module.recommend.mapper.RecommendationFeedbackMapper;
import com.reggie.module.recommend.mapper.UserPreferenceMapper;
import com.reggie.module.recommend.model.RecommendationCache;
import com.reggie.module.recommend.model.RecommendationFeedback;
import com.reggie.module.recommend.service.analytics.RecommendationAnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 推荐引擎统计分析服务实现
 * </p>
 * <p>从 RecommendServiceImpl 拆分出的概览页统计逻辑（域4 结构优化）</p>
 *
 * @author 心飞为你飞
 * @since 2026-08-22
 */
@Slf4j
@Service
public class RecommendationAnalyticsServiceImpl implements RecommendationAnalyticsService {

    /** 概览统计窗口天数（近7天） */
    private static final int STATS_WINDOW_DAYS = 7;
    /** 算法对比统计窗口天数（近30天） */
    private static final int ALGO_COMPARE_DAYS = 30;

    @Autowired
    private RecommendationFeedbackMapper feedbackMapper;
    @Autowired
    private UserPreferenceMapper userPreferenceMapper;
    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;
    @Autowired
    private RecommendationCacheMapper cacheMapper;
    @Autowired
    private OrderService orderService;

    @Override
    public Map<String, Object> calculateStats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
            if (tenantId != null) {
                orderWrapper.eq(Orders::getTenantId, tenantId);
            }
            int totalOrderUsers = (int) orderService.count(orderWrapper);

            LambdaQueryWrapper<RecommendationCache> cacheWrapper = new LambdaQueryWrapper<>();
            cacheWrapper.gt(RecommendationCache::getExpireTime, LocalDateTime.now());
            if (tenantId != null) {
                cacheWrapper.eq(RecommendationCache::getTenantId, tenantId);
            }
            int cachedUsers = (int) cacheMapper.selectCount(cacheWrapper);
            int hybridRate = totalOrderUsers > 0 ? (int) (cachedUsers * 100.0 / totalOrderUsers) : 0;

            LambdaQueryWrapper<RecommendationFeedback> feedbackWrapper = new LambdaQueryWrapper<>();
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(STATS_WINDOW_DAYS);
            feedbackWrapper.ge(RecommendationFeedback::getCreateTime, sevenDaysAgo);
            if (tenantId != null) {
                feedbackWrapper.eq(RecommendationFeedback::getTenantId, tenantId);
            }
            List<RecommendationFeedback> feedbacks = feedbackMapper.selectList(feedbackWrapper);

            long totalFeedback = feedbacks.size();
            long clickCount = feedbacks.stream()
                    .filter(f -> f.getFeedbackType() == RecommendationFeedback.FEEDBACK_CLICK)
                    .count();
            long orderCount = feedbacks.stream()
                    .filter(f -> f.getFeedbackType() == RecommendationFeedback.FEEDBACK_ORDER)
                    .count();

            int clickRate = totalFeedback > 0 ? (int) (clickCount * 100.0 / totalFeedback) : 0;
            int conversionRate = totalFeedback > 0 ? (int) (orderCount * 100.0 / totalFeedback) : 0;

            double recommendGMV = 0;
            if (orderCount > 0) {
                Set<Long> feedbackUserIds = feedbacks.stream()
                        .filter(f -> f.getFeedbackType() == RecommendationFeedback.FEEDBACK_ORDER)
                        .map(RecommendationFeedback::getUserId)
                        .collect(Collectors.toSet());
                LambdaQueryWrapper<Orders> gmvWrapper = new LambdaQueryWrapper<>();
                gmvWrapper.in(Orders::getUserId, feedbackUserIds)
                        .ge(Orders::getCreateTime, sevenDaysAgo)
                        .eq(Orders::getStatus, Orders.STATUS_COMPLETED);
                if (tenantId != null) {
                    gmvWrapper.eq(Orders::getTenantId, tenantId);
                }
                List<Orders> orders = orderService.list(gmvWrapper);
                recommendGMV = orders.stream()
                        .mapToDouble(o -> o.getAmount() != null ? o.getAmount().doubleValue() : 0)
                        .sum();
            }

            stats.put("hybridRate", hybridRate);
            stats.put("clickRate", clickRate);
            stats.put("conversionRate", conversionRate);
            stats.put("recommendGMV", Math.round(recommendGMV));
            stats.put("cachedUsers", cachedUsers);
            stats.put("totalOrderUsers", totalOrderUsers);
        } catch (Exception e) {
            log.warn("[推荐引擎] 统计数据计算异常", e);
            stats.put("hybridRate", 0);
            stats.put("clickRate", 0);
            stats.put("conversionRate", 0);
            stats.put("recommendGMV", 0);
        }

        return stats;
    }

    @Override
    public Map<String, Integer> getFeedbackStats(int days) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("click", 0);
        stats.put("favorite", 0);
        stats.put("cart", 0);
        stats.put("order", 0);
        stats.put("unlike", 0);

        try {
            String startTime = LocalDateTime.now().minusDays(days).toString();
            Long tenantId = BaseContext.getCurrentTenantId();
            List<Map<String, Object>> rows = feedbackMapper.countByTypeSince(startTime, tenantId);

            for (Map<String, Object> row : rows) {
                Integer type = (Integer) row.get("feedback_type");
                Long cnt = ((Number) row.get("cnt")).longValue();
                switch (type) {
                    case RecommendationFeedback.FEEDBACK_CLICK:
                        stats.put("click", cnt.intValue());
                        break;
                    case RecommendationFeedback.FEEDBACK_FAVORITE:
                        stats.put("favorite", cnt.intValue());
                        break;
                    case RecommendationFeedback.FEEDBACK_ADD_CART:
                        stats.put("cart", cnt.intValue());
                        break;
                    case RecommendationFeedback.FEEDBACK_ORDER:
                        stats.put("order", cnt.intValue());
                        break;
                    case RecommendationFeedback.FEEDBACK_NOT_INTERESTED:
                        stats.put("unlike", cnt.intValue());
                        break;
                }
            }
            log.debug("[推荐引擎] 反馈分布统计: days={}, stats={}", days, stats);
        } catch (Exception e) {
            log.warn("[推荐引擎] 反馈分布统计异常", e);
        }
        return stats;
    }

    @Override
    public List<Map<String, Object>> getPreferenceDistribution() {
        try {
            List<Map<String, Object>> result = userPreferenceMapper.countTasteDistribution();
            if (result != null && !result.isEmpty()) {
                log.debug("[推荐引擎] 口味偏好分布: count={}", result.size());
                return result;
            }
        } catch (Exception e) {
            log.warn("[推荐引擎] 偏好分布查询异常", e);
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAlgoCompare() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algos", Arrays.asList("协同过滤", "内容推荐", "热门排行", "混合推荐"));
        result.put("ctRates", Arrays.asList(0.0, 0.0, 0.0, 0.0));
        result.put("cvRates", Arrays.asList(0.0, 0.0, 0.0, 0.0));

        try {
            String startTime = LocalDateTime.now().minusDays(ALGO_COMPARE_DAYS).toString();
            Long tenantId = BaseContext.getCurrentTenantId();
            List<Map<String, Object>> rows = feedbackMapper.countByAlgorithmSince(startTime, tenantId);

            Map<String, Integer> algoIndex = new HashMap<>();
            algoIndex.put("CF", 0);
            algoIndex.put("CONTENT", 1);
            algoIndex.put("HOT", 2);
            algoIndex.put("HYBRID", 3);

            double[] ctRates = new double[]{0.0, 0.0, 0.0, 0.0};
            double[] cvRates = new double[]{0.0, 0.0, 0.0, 0.0};

            for (Map<String, Object> row : rows) {
                String algo = (String) row.get("algo_name");
                Long clickCnt = ((Number) row.get("click_cnt")).longValue();
                Long orderCnt = ((Number) row.get("order_cnt")).longValue();
                Long totalCnt = ((Number) row.get("total_cnt")).longValue();

                Integer idx = algoIndex.getOrDefault(algo, -1);
                if (idx >= 0 && totalCnt > 0) {
                    ctRates[idx] = Math.round(clickCnt * 1000.0 / totalCnt) / 10.0;
                    cvRates[idx] = Math.round(orderCnt * 1000.0 / totalCnt) / 10.0;
                }
            }

            List<Double> ctList = new ArrayList<>();
            List<Double> cvList = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                ctList.add(ctRates[i]);
                cvList.add(cvRates[i]);
            }
            result.put("ctRates", ctList);
            result.put("cvRates", cvList);

            log.debug("[推荐引擎] 算法效果对比: ct={}, cv={}", ctList, cvList);
        } catch (Exception e) {
            log.warn("[推荐引擎] 算法对比查询异常", e);
        }
        return result;
    }

    @Override
    public Map<String, Object> getBrowseTrend(int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> browseCount = new ArrayList<>();
        List<Integer> cartCount = new ArrayList<>();

        try {
            String startTime = LocalDateTime.now().minusDays(days).toString();
            List<Map<String, Object>> rows = browseHistoryMapper.countDailyTrend(startTime, BaseContext.getCurrentTenantId());

            Map<String, Map<String, Object>> dateMap = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String date = row.get("date").toString();
                dateMap.put(date, row);
            }

            for (int i = days - 1; i >= 0; i--) {
                java.time.LocalDate d = java.time.LocalDate.now().minusDays(i);
                String dateKey = d.toString();
                String label = d.toString().substring(5);
                dates.add(label);

                Map<String, Object> row = dateMap.get(dateKey);
                if (row != null) {
                    browseCount.add(((Number) row.get("browse_count")).intValue());
                    cartCount.add(((Number) row.get("cart_count")).intValue());
                } else {
                    browseCount.add(0);
                    cartCount.add(0);
                }
            }

            log.debug("[推荐引擎] 浏览趋势: dates size={}, browse={}, cart={}", dates.size(), browseCount, cartCount);
        } catch (Exception e) {
            log.warn("[推荐引擎] 浏览趋势查询异常", e);
            for (int i = days - 1; i >= 0; i--) {
                java.time.LocalDate d = java.time.LocalDate.now().minusDays(i);
                dates.add(d.toString().substring(5));
                browseCount.add(0);
                cartCount.add(0);
            }
        }
        result.put("dates", dates);
        result.put("browseCount", browseCount);
        result.put("cartCount", cartCount);
        return result;
    }
}