package com.reggie.module.recommend.service;

import com.reggie.module.recommend.model.RecommendationFeedback;
import java.util.List;
import java.util.Map;

/**
 * 智能推荐服务
 * 基于协同过滤、内容推荐、热门排行等多算法融合的菜品推荐引擎
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
     * 修改点：计算推荐引擎真实统计数据
     * 从数据库实时计算：覆盖率、点击率、转化率、推荐贡献GMV等
     *
     * @return 统计数据Map
     */
    Map<String, Object> calculateStats();
}
