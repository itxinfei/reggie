package com.reggie.module.recommend.service.analytics;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 推荐引擎统计分析服务接口
 * </p>
 * <p>从 RecommendServiceImpl 拆分出的独立职责：推荐效果、反馈分布、算法对比、浏览趋势等概览页统计</p>
 * <p>域4 结构优化：将 1117 行的 RecommendServiceImpl 拆分为"核心推荐算法"和"概览统计"两个内聚单元</p>
 *
 * @author 心飞为你飞
 * @since 2026-08-22
 */
public interface RecommendationAnalyticsService {

    /**
     * 计算推荐引擎真实统计数据
     * 从数据库实时计算：覆盖率、点击率、转化率、推荐贡献GMV等
     *
     * @return 统计数据Map
     */
    Map<String, Object> calculateStats();

    /**
     * 获取推荐反馈分布统计（从 recommendation_feedback 表真实查询）
     *
     * @param days 统计最近N天
     * @return key: click/favorite/cart/order/unlike, value: 计数
     */
    Map<String, Integer> getFeedbackStats(int days);

    /**
     * 获取用户口味偏好分布（从 user_preference_tag 表真实查询）
     *
     * @return 每项包含 name(口味名) 和 value(用户数)
     */
    List<Map<String, Object>> getPreferenceDistribution();

    /**
     * 获取推荐算法效果对比（从 recommendation_feedback + recommendation_cache 真实计算）
     *
     * @return 包含 algos/ctRates/cvRates 的Map
     */
    Map<String, Object> getAlgoCompare();

    /**
     * 获取浏览行为趋势（从 user_browse_history 表真实统计每日浏览/加购数）
     *
     * @param days 统计最近N天
     * @return 包含 dates/browseCount/cartCount 的Map
     */
    Map<String, Object> getBrowseTrend(int days);
}