package com.reggie.module.recommend.service;

import com.reggie.module.recommend.model.RecommendationFeedback;
import java.util.List;
import java.util.Map;

/**
 * 智能推荐服务接口
 * 基于协同过滤、内容推荐、热门排行等多算法融合的菜品推荐引擎
 * 修改点：新增概览页真实统计方法，替换原先Math.random()假数据
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface RecommendService {

    /**
     * 为用户生成个性化菜品推荐
     * 算法逻辑：优先使用混合推荐(Hybrid)，回退到热门排行(HotRank)
     *
     * @param userId 用户ID
     * @param limit  推荐数量上限
     * @return 推荐菜品列表，每个Map包含 id, name, image, price, score 等字段
     */
    List<Map<String, Object>> recommendDishes(Long userId, int limit);

    /**
     * 为用户推荐套餐
     *
     * @param userId 用户ID
     * @param limit  推荐数量上限
     * @return 推荐套餐列表
     */
    List<Map<String, Object>> recommendSetmeals(Long userId, int limit);

    /**
     * 新品尝鲜推荐 - 推荐用户未浏览/未购买的新上架菜品
     *
     * @param userId 用户ID
     * @param limit  推荐数量上限
     * @return 新品列表
     */
    List<Map<String, Object>> recommendNewArrivals(Long userId, int limit);

    /**
     * 基于协同过滤推荐 - 找到相似用户，推荐他们的高频菜品
     *
     * @param userId 用户ID
     * @param limit  推荐数量上限
     * @return 推荐菜品列表
     */
    List<Map<String, Object>> collaborativeFiltering(Long userId, int limit);

    /**
     * 基于内容推荐 - 根据用户偏好标签匹配菜品
     *
     * @param userId 用户ID
     * @param limit  推荐数量上限
     * @return 推荐菜品列表
     */
    List<Map<String, Object>> contentBasedRecommend(Long userId, int limit);

    /**
     * 热门排行推荐 - 门店销量最高的菜品
     *
     * @param tenantId 门店ID
     * @param limit    推荐数量上限
     * @return 热门菜品列表
     */
    List<Map<String, Object>> hotRankRecommend(Long tenantId, int limit);

    /**
     * 记录推荐反馈，用于优化推荐算法
     *
     * @param feedback 反馈信息
     */
    void recordFeedback(RecommendationFeedback feedback);

    /**
     * 刷新推荐缓存，清除过期数据并触发重算
     *
     * @param userId 用户ID
     */
    void refreshCache(Long userId);

    /**
     * 计算推荐引擎真实统计数据
     * 从数据库实时计算：覆盖率、点击率、转化率、推荐贡献GMV等
     *
     * @return 统计数据Map
     */
    Map<String, Object> calculateStats();

    // ==================== 修改点：概览页真实统计方法 ====================

    /**
     * 获取推荐反馈分布统计（从recommendation_feedback表真实查询）
     * 替代原先的Math.random()假数据
     *
     * @param days 统计最近N天
     * @return key: click/favorite/cart/order/unlike, value: 计数
     */
    Map<String, Integer> getFeedbackStats(int days);

    /**
     * 获取用户口味偏好分布（从user_preference_tag表真实查询）
     * 替代原先的硬编码口味列表 + Math.random()
     *
     * @return 每项包含 name(口味名) 和 value(用户数)
     */
    List<Map<String, Object>> getPreferenceDistribution();

    /**
     * 获取推荐算法效果对比（从recommendation_feedback + recommendation_cache真实计算）
     * 替代原先的Math.random()假数据
     *
     * @return 包含 algos/ctRates/cvRates 的Map
     */
    Map<String, Object> getAlgoCompare();

    /**
     * 获取浏览行为趋势（从user_browse_history表真实统计每日浏览/加购数）
     * 替代原先的Math.random()假数据
     *
     * @param days 统计最近N天
     * @return 包含 dates/browseCount/cartCount 的Map
     */
    Map<String, Object> getBrowseTrend(int days);
}
