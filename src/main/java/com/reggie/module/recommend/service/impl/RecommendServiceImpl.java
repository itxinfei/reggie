package com.reggie.module.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.category.model.Category;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.recommend.mapper.BrowseHistoryMapper;
import com.reggie.module.recommend.mapper.RecommendationCacheMapper;
import com.reggie.module.recommend.mapper.RecommendationFeedbackMapper;
import com.reggie.module.recommend.mapper.UserPreferenceMapper;
import com.reggie.module.recommend.model.BrowseHistory;
import com.reggie.module.recommend.model.RecommendationCache;
import com.reggie.module.recommend.model.RecommendationFeedback;
import com.reggie.module.recommend.model.UserPreferenceTag;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import com.reggie.module.recommend.service.RecommendService;
import com.reggie.module.recommend.service.analytics.RecommendationAnalyticsService;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.setmeal.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智能推荐引擎实现
 *
 * 核心算法：
 * 1. Hybrid混合推荐（协同过滤 + 内容推荐 + 热门排行加权融合）
 * 2. 协同过滤：找到相似订单行为的用户，推荐他们的热购菜品
 * 3. 内容推荐：根据用户偏好标签（口味/品类/价格）匹配菜品
 * 4. 热门排行：基于门店近期销量排序
 * 5. 冷启动：新用户使用热门排行，有数据后逐步切换到个性化推荐
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class RecommendServiceImpl implements RecommendService {

    /** 推荐缓存过期时间(小时) */
    private static final int CACHE_EXPIRE_HOURS = 6;
    /** 协同过滤最小用户数 */
    private static final int CF_MIN_USERS = 5;
    /** 推荐结果多样性因子（越高越多随机性） */
    private static final double DIVERSITY_FACTOR = 0.2;
    /** 混合推荐权重：协同过滤（阿里规范：魔法值需定义语义化常量） */
    private static final double WEIGHT_CF = 0.5;
    /** 混合推荐权重：内容推荐 */
    private static final double WEIGHT_CONTENT = 0.3;
    /** 混合推荐权重：热门排行 */
    private static final double WEIGHT_HOT = 0.2;
    /** 混合推荐异步任务超时时间（秒） */
    private static final int HYBRID_TIMEOUT_SECONDS = 3;
    /** 相似用户至少共同购买菜品数 */
    private static final int MIN_SHARED_DISHES = 3;
    /** 内容匹配评分：品类命中 */
    private static final double SCORE_CATEGORY_MATCH = 0.5;
    /** 内容匹配评分：口味命中 */
    private static final double SCORE_TASTE_MATCH = 0.3;
    /** 内容匹配评分：价格区间命中 */
    private static final double SCORE_PRICE_MATCH = 0.2;
    /** 内容匹配评分总分上限 */
    private static final double SCORE_MAX_TOTAL = 1.0;
    /** 价格档位分界：经济型 / 实惠型 / 中档 / 高端 */
    private static final double PRICE_ECONOMY_MAX = 20;
    private static final double PRICE_AFFORDABLE_MAX = 40;
    private static final double PRICE_MID_RANGE_MAX = 80;
    /** 新品推荐：最近 N 天上架的菜品视为新品 */
    private static final long ALGO_COMPARE_DAYS = 30;
    /** 推荐缓存置信度：各算法基础置信度 */
    private static final double CONFIDENCE_HYBRID = 0.85;
    private static final double CONFIDENCE_CF = 0.70;
    private static final double CONFIDENCE_CONTENT = 0.65;
    private static final double CONFIDENCE_HOT = 0.40;
    private static final double CONFIDENCE_DEFAULT = 0.50;
    private static final double CONFIDENCE_EMPTY = 0.0;
    /** 异步任务线程池（Spring 管理，应用关闭时优雅停机；替代原静态 FixedThreadPool） */
    @Resource(name = "recommendExecutor")
    private ThreadPoolTaskExecutor asyncExecutor;

    /** 用户偏好标签Mapper */
    @Autowired
    private UserPreferenceMapper userPreferenceMapper;
    /** 浏览历史Mapper */
    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;
    /** 推荐缓存Mapper */
    @Autowired
    private RecommendationCacheMapper cacheMapper;
    /** 推荐反馈Mapper */
    @Autowired
    private RecommendationFeedbackMapper feedbackMapper;
    /** 用户偏好分析服务 */
    @Autowired
    private PreferenceAnalysisService preferenceAnalysisService;

    // 核心业务依赖：
    /** 订单服务 */
    @Autowired
    private OrderService orderService;
    /** 订单明细服务 */
    @Autowired
    private OrderDetailService orderDetailService;
    /** 菜品服务 */
    @Autowired
    private DishService dishService;
    /** 套餐服务 */
    @Autowired
    private SetmealService setmealService;
    /** 分类服务 */
    @Autowired
    private CategoryService categoryService;

    /** JSON序列化工具 */
    private final ObjectMapper objectMapper = ObjectMapperHolder.getDefault();

    /** 概览页统计分析服务（从1117行超类拆分出的独立职责单元） */
    @Autowired
    private RecommendationAnalyticsService analyticsService;

    /**
     * 应用启动时清理过期缓存
     */
    @PostConstruct
    public void init() {
        try {
            int deleted = cacheMapper.deleteExpired();
            log.info("[推荐引擎] 初始化完成，清理过期缓存 {} 条", deleted);
        } catch (Exception e) {
            log.warn("[推荐引擎] 初始化跳过（可能是测试环境缺少数据表）", e);
        }
    }

    // ==================== 公开推荐接口 ====================

    @Override
    public List<Map<String, Object>> recommendDishes(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return Collections.emptyList();
        }
        Long tenantId = BaseContext.getCurrentTenantId();

        // 1. 尝试从缓存获取
        RecommendationCache cache = cacheMapper.findValidCache(userId, RecommendationCache.TYPE_DISH, tenantId);
        if (cache != null) {
            List<Long> cachedIds = parseDishIds(cache.getDishIds());
            if (!cachedIds.isEmpty()) {
                log.debug("[推荐引擎] 命中缓存 userId={}, algoName={}", userId, cache.getAlgoName());
                return buildDishResultList(cachedIds, limit);
            }
        }

        // 2. 执行推荐算法
        List<Map<String, Object>> result;
        String algorithm;

        int preferenceCount = userPreferenceMapper.countByUserId(userId);
        if (preferenceCount > 0) {
            // 有偏好数据：使用混合推荐
            result = hybridRecommend(userId, tenantId, limit);
            algorithm = RecommendationCache.ALGO_HYBRID;
        } else {
            // 冷启动：使用热门排行
            result = hotRankRecommend(tenantId, limit);
            algorithm = RecommendationCache.ALGO_HOT;
        }

        // 3. 异步缓存推荐结果
        final String finalAlgorithm = algorithm;
        final List<Map<String, Object>> finalResult = result;
        asyncExecutor.submit(() -> saveToCache(userId, RecommendationCache.TYPE_DISH, finalResult, finalAlgorithm));

        return result;
    }

    @Override
    public List<Map<String, Object>> recommendSetmeals(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return Collections.emptyList();
        }
        Long tenantId = BaseContext.getCurrentTenantId();

        // 检查缓存
        RecommendationCache cache = cacheMapper.findValidCache(userId, RecommendationCache.TYPE_SETMEAL, tenantId);
        if (cache != null) {
            List<Long> cachedIds = parseDishIds(cache.getDishIds());
            if (!cachedIds.isEmpty()) {
                return buildSetmealResultList(cachedIds, limit);
            }
        }

        // 套餐推荐逻辑：根据用户浏览/购买最多的菜品分类推荐同类别套餐
        List<Map<String, Object>> topCategories = browseHistoryMapper.findTopViewedDishes(userId, 3);
        List<Long> setmealIds;
        if (!topCategories.isEmpty()) {
            // 基于用户偏好推荐套餐：按分类数向上取整分配配额，避免整数除法截断导致数量不足
            setmealIds = new ArrayList<>();
            int perCategory = (int) Math.ceil((double) limit / topCategories.size());
            for (Map<String, Object> cat : topCategories) {
                List<Long> ids = findSetmealsByCategory((Long) cat.get("target_id"), perCategory);
                setmealIds.addAll(ids);
            }
        } else {
            // 基于热门套餐推荐
            setmealIds = findHotSetmeals(tenantId, limit);
        }
        return buildSetmealResultList(setmealIds, limit);
    }

    @Override
    public List<Map<String, Object>> recommendNewArrivals(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return Collections.emptyList();
        }
        Long tenantId = BaseContext.getCurrentTenantId();

        // 查询最近30天上架的菜品
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(ALGO_COMPARE_DAYS);
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, Dish::getTenantId, tenantId)
               .eq(Dish::getStatus, 1)
               .ge(Dish::getCreateTime, thirtyDaysAgo)
               .orderByDesc(Dish::getCreateTime)
               .last("LIMIT " + sanitizeLimit(limit * 2));

        List<Dish> newDishes = dishService.list(wrapper);

        // 过滤掉用户已经浏览过的
        List<BrowseHistory> recentHistory = browseHistoryMapper.findRecentByUserId(userId, 100);
        Set<Long> viewedIds = recentHistory.stream()
                .filter(h -> h.getTargetType() == BrowseHistory.TARGET_TYPE_DISH)
                .map(BrowseHistory::getTargetId)
                .collect(Collectors.toSet());

        return newDishes.stream()
                .filter(d -> !viewedIds.contains(d.getId()))
                .limit(limit)
                .map(this::dishToMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> collaborativeFiltering(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return Collections.emptyList();
        }
        Long tenantId = BaseContext.getCurrentTenantId();

        // 1. 获取当前用户订购过的菜品ID集合
        Set<Long> userDishIds = getUserPurchasedDishIds(userId);
        if (userDishIds.isEmpty()) {
            return hotRankRecommend(tenantId, limit);
        }

        // 2. 找到购买过相同菜品的相似用户（协作用户）
        List<Long> similarUsers = findSimilarUsers(userId, userDishIds, CF_MIN_USERS);
        if (similarUsers.isEmpty()) {
            return hotRankRecommend(tenantId, limit);
        }

        // 3. 统计协作用户的高频菜品（当前用户未购买的）
        Map<Long, Long> dishScore = new HashMap<>();
        for (Long simUserId : similarUsers) {
            Set<Long> simDishIds = getUserPurchasedDishIds(simUserId);
            for (Long dishId : simDishIds) {
                if (!userDishIds.contains(dishId)) {
                    dishScore.merge(dishId, 1L, Long::sum);
                }
            }
        }

        // 4. 按评分排序并返回TOP N
        return dishScore.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    Dish dish = dishService.getById(e.getKey());
                    if (dish == null) return null;
                    Map<String, Object> map = dishToMap(dish);
                    map.put("cfScore", e.getValue());
                    return map;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> contentBasedRecommend(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return Collections.emptyList();
        }
        Long tenantId = BaseContext.getCurrentTenantId();

        // 1. 获取用户偏好标签
        List<UserPreferenceTag> tags = userPreferenceMapper.findByUserId(userId);
        if (tags.isEmpty()) {
            return hotRankRecommend(tenantId, limit);
        }

        // 2. 提取口味偏好和品类偏好
        List<String> tastePrefs = tags.stream()
                .filter(t -> t.getTagType() == UserPreferenceTag.TAG_TYPE_TASTE)
                .map(UserPreferenceTag::getTagName)
                .collect(Collectors.toList());

        List<String> categoryPrefs = tags.stream()
                .filter(t -> t.getTagType() == UserPreferenceTag.TAG_TYPE_CATEGORY)
                .map(UserPreferenceTag::getTagName)
                .collect(Collectors.toList());

        // 3. 获取用户已购买过的菜品ID（排除）
        Set<Long> purchasedIds = getUserPurchasedDishIds(userId);

        // 4. 查询候选菜品
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, Dish::getTenantId, tenantId)
               .eq(Dish::getStatus, 1);
        List<Dish> candidateDishes = dishService.list(wrapper);
        if (candidateDishes.isEmpty()) {
            return Collections.emptyList();
        }

        // 预加载所有品类（消除逐个 getById 的 N+1 查询）
        Set<Long> categoryIds = new HashSet<>();
        for (Dish dish : candidateDishes) {
            if (dish.getCategoryId() != null) {
                categoryIds.add(dish.getCategoryId());
            }
        }
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryService.listByIds(categoryIds);
            if (categories != null) {
                for (Category c : categories) {
                    categoryNameMap.put(c.getId(), c.getName());
                }
            }
        }

        // 5. 基于内容匹配评分
        Map<Dish, Double> scoredDishes = new HashMap<>();
        for (Dish dish : candidateDishes) {
            if (purchasedIds.contains(dish.getId())) {
                continue;
            }
            double score = calculateContentScore(dish, tastePrefs, categoryPrefs, tags, categoryNameMap);
            if (score > 0) {
                scoredDishes.put(dish, score);
            }
        }

        // 6. 按评分排序，加入多样性因子
        return scoredDishes.entrySet().stream()
                .sorted(Map.Entry.<Dish, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    Map<String, Object> map = dishToMap(e.getKey());
                    map.put("contentScore", e.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> hotRankRecommend(Long tenantId, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(ALGO_COMPARE_DAYS);
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.ge(Orders::getCreateTime, thirtyDaysAgo);
        if (tenantId != null) {
            orderWrapper.eq(Orders::getTenantId, tenantId);
        }
        orderWrapper.select(Orders::getId);
        List<Orders> recentOrders = orderService.list(orderWrapper);
        if (recentOrders.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询所有订单明细，消除逐单 N+1
        List<Long> orderIds = recentOrders.stream().map(Orders::getId).collect(Collectors.toList());
        Map<Long, Long> dishOrderCount = new HashMap<>();
        LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(OrderDetail::getOrderId, orderIds);
        detailWrapper.isNotNull(OrderDetail::getDishId);
        detailWrapper.select(OrderDetail::getDishId, OrderDetail::getOrderId);
        List<OrderDetail> details = orderDetailService.list(detailWrapper);
        for (OrderDetail detail : details) {
            if (detail.getDishId() != null) {
                dishOrderCount.merge(detail.getDishId(), 1L, Long::sum);
            }
        }

        // 按销量排序，批量查询菜品消除 N+1
        List<Map.Entry<Long, Long>> topEntries = dishOrderCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit * 2)
                .collect(Collectors.toList());

        List<Long> candidateDishIds = topEntries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        Map<Long, Dish> dishMap = new HashMap<>();
        if (!candidateDishIds.isEmpty()) {
            LambdaQueryWrapper<Dish> dishQw = new LambdaQueryWrapper<>();
            dishQw.in(Dish::getId, candidateDishIds);
            dishQw.eq(Dish::getStatus, 1);
            if (tenantId != null) {
                dishQw.eq(Dish::getTenantId, tenantId);
            }
            dishService.list(dishQw).forEach(d -> dishMap.put(d.getId(), d));
        }

        return topEntries.stream()
                .map(e -> {
                    Dish dish = dishMap.get(e.getKey());
                    if (dish == null) {
                        return null;
                    }
                    Map<String, Object> map = dishToMap(dish);
                    map.put("orderCount", e.getValue());
                    map.put("hotScore", e.getValue());
                    return map;
                })
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void recordFeedback(RecommendationFeedback feedback) {
        if (feedback == null || feedback.getUserId() == null) {
            return;
        }
        feedbackMapper.insert(feedback);
        log.debug("[推荐引擎] 记录反馈 userId={}, dishId={}, type={}",
                feedback.getUserId(), feedback.getDishId(), feedback.getFeedbackType());

        // 正向反馈时更新偏好权重
        if (feedback.getFeedbackType() == RecommendationFeedback.FEEDBACK_ORDER ||
            feedback.getFeedbackType() == RecommendationFeedback.FEEDBACK_ADD_CART) {
            asyncExecutor.submit(() -> preferenceAnalysisService.analyzeUserPreferences(feedback.getUserId()));
        }
    }

    @Override
    public void refreshCache(Long userId) {
        // 删除用户所有缓存，触发下次请求时重新计算
        LambdaQueryWrapper<RecommendationCache> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendationCache::getUserId, userId);
        cacheMapper.delete(wrapper);
        // 异步重新分析偏好
        asyncExecutor.submit(() -> preferenceAnalysisService.analyzeUserPreferences(userId));
        log.info("[推荐引擎] 刷新用户{}的推荐缓存完成", userId);
    }

    // 域4 结构优化：概览统计方法已拆分至 RecommendationAnalyticsService
    @Override
    public Map<String, Object> calculateStats() {
        return analyticsService.calculateStats();
    }

    @Override
    public Map<String, Integer> getFeedbackStats(int days) {
        return analyticsService.getFeedbackStats(days);
    }

    @Override
    public List<Map<String, Object>> getPreferenceDistribution() {
        return analyticsService.getPreferenceDistribution();
    }

    @Override
    public Map<String, Object> getAlgoCompare() {
        return analyticsService.getAlgoCompare();
    }

    @Override
    public Map<String, Object> getBrowseTrend(int days) {
        return analyticsService.getBrowseTrend(days);
    }

    // ==================== 私有方法：核心算法逻辑 ====================

    /**
     * 混合推荐：协同过滤 + 内容推荐 + 热门排行加权融合
     * 权重分配：CF:50%, Content:30%, Hot:20%
     */
    private List<Map<String, Object>> hybridRecommend(Long userId, Long tenantId, int limit) {
        // 并发执行三种推荐算法
        Future<List<Map<String, Object>>> cfFuture = asyncExecutor.submit(
                () -> collaborativeFiltering(userId, limit * 2));
        Future<List<Map<String, Object>>> contentFuture = asyncExecutor.submit(
                () -> contentBasedRecommend(userId, limit * 2));
        Future<List<Map<String, Object>>> hotFuture = asyncExecutor.submit(
                () -> hotRankRecommend(tenantId, limit));

        try {
            List<Map<String, Object>> cfResult = cfFuture.get(HYBRID_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<Map<String, Object>> contentResult = contentFuture.get(HYBRID_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<Map<String, Object>> hotResult = hotFuture.get(HYBRID_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // 加权融合
            Map<Long, Double> fusionScore = new HashMap<>();
            Map<Long, Map<String, Object>> dishInfoMap = new LinkedHashMap<>();

            // CF推荐 权重0.5
            mergeRecommendResult(cfResult, fusionScore, dishInfoMap, WEIGHT_CF, "CF");
            // 内容推荐 权重0.3
            mergeRecommendResult(contentResult, fusionScore, dishInfoMap, WEIGHT_CONTENT, "Content");
            // 热门排行 权重0.2 + 多样性因子
            mergeRecommendResult(hotResult, fusionScore, dishInfoMap, WEIGHT_HOT + DIVERSITY_FACTOR, "Hot");

            // 按融合分数排序
            return fusionScore.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(limit)
                    .map(e -> {
                        Map<String, Object> dish = dishInfoMap.get(e.getKey());
                        if (dish != null) {
                            dish.put("fusionScore", Math.round(e.getValue() * 100.0) / 100.0);
                        }
                        return dish;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("[推荐引擎] 混合推荐异常，回退热门排行", e);
            return hotRankRecommend(tenantId, limit);
        }
    }

    /**
     * 合并推荐结果到融合评分表
     */
    private void mergeRecommendResult(List<Map<String, Object>> results,
                                       Map<Long, Double> fusionScore,
                                       Map<Long, Map<String, Object>> dishInfoMap,
                                       double weight, String source) {
        int rank = 0;
        for (Map<String, Object> item : results) {
            Long dishId = (Long) item.get("id");
            if (dishId == null) continue;
            rank++;
            // 排名越靠前分数越高，加入位置衰减
            double positionalScore = weight * (1.0 / Math.sqrt(rank + 1));
            fusionScore.merge(dishId, positionalScore, Double::sum);
            dishInfoMap.putIfAbsent(dishId, item);
            @SuppressWarnings("unchecked") // Map.computeIfAbsent返回值类型安全
            Set<String> sources = (Set<String>) item.computeIfAbsent("recommendSources",
                    k -> new LinkedHashSet<String>());
            sources.add(source);
        }
    }

    /**
     * 计算内容匹配评分
     */
    private double calculateContentScore(Dish dish, List<String> tastePrefs,
                                          List<String> categoryPrefs,
                                          List<UserPreferenceTag> allTags,
                                          Map<Long, String> categoryNameMap) {
        double score = 0.0;

        // 品类匹配（使用预加载的品类Map，避免N+1查询）
        if (dish.getCategoryId() != null && categoryNameMap != null) {
            String categoryName = categoryNameMap.get(dish.getCategoryId());
            if (categoryName != null && categoryPrefs.contains(categoryName)) {
                score += SCORE_CATEGORY_MATCH;
            }
        }

        // 口味匹配（通过菜品描述/名称模糊匹配）
        if (dish.getName() != null && dish.getDescription() != null) {
            String text = dish.getName() + " " + dish.getDescription();
            for (String taste : tastePrefs) {
                if (text.contains(taste)) {
                    score += SCORE_TASTE_MATCH;
                }
            }
        }

        // 价格匹配
        for (UserPreferenceTag tag : allTags) {
            if (tag.getTagType() == UserPreferenceTag.TAG_TYPE_PRICE) {
                boolean priceMatch = isPriceInRange(dish.getPrice(), tag.getTagName());
                if (priceMatch) {
                    score += SCORE_PRICE_MATCH * tag.getTagValue().doubleValue();
                }
            }
        }

        return Math.min(score, SCORE_MAX_TOTAL);
    }

    /**
     * 判断菜品价格是否在偏好区间内
     */
    private boolean isPriceInRange(BigDecimal price, String rangeTag) {
        if (price == null || rangeTag == null) return false;
        double p = price.doubleValue();
        switch (rangeTag) {
            case "经济型": return p < PRICE_ECONOMY_MAX;
            case "实惠型": return p >= PRICE_ECONOMY_MAX && p < PRICE_AFFORDABLE_MAX;
            case "中档": return p >= PRICE_AFFORDABLE_MAX && p < PRICE_MID_RANGE_MAX;
            case "高端": return p >= PRICE_MID_RANGE_MAX;
            default: return false;
        }
    }

    /**
     * 查找相似用户（购买过相同菜品的其他用户）
     */
    private List<Long> findSimilarUsers(Long userId, Set<Long> userDishIds, int minUsers) {
        if (userDishIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查询购买了相同菜品的订单明细，消除逐菜品 N+1
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrderDetail::getDishId, userDishIds);
        wrapper.select(OrderDetail::getOrderId);
        List<OrderDetail> details = orderDetailService.list(wrapper);
        if (details.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询相关订单获取 userId，消除逐单 getById N+1
        Set<Long> orderIds = details.stream().map(OrderDetail::getOrderId).collect(Collectors.toSet());
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(Orders::getId, orderIds);
        orderWrapper.select(Orders::getId, Orders::getUserId);
        List<Orders> orders = orderService.list(orderWrapper);
        Map<Long, Long> orderUserMap = new HashMap<>();
        for (Orders order : orders) {
            if (order.getUserId() != null) {
                orderUserMap.put(order.getId(), order.getUserId());
            }
        }

        Map<Long, Integer> userSimilarity = new HashMap<>();
        for (OrderDetail detail : details) {
            Long orderUserId = orderUserMap.get(detail.getOrderId());
            if (orderUserId != null && !orderUserId.equals(userId)) {
                userSimilarity.merge(orderUserId, 1, Integer::sum);
            }
        }

        // 返回相似度最高的用户
        return userSimilarity.entrySet().stream()
                .filter(e -> e.getValue() >= MIN_SHARED_DISHES) // 至少3个共同菜品
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(minUsers * 2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户购买过的菜品ID集合
     */
    private Set<Long> getUserPurchasedDishIds(Long userId) {
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Orders::getUserId, userId);
        orderWrapper.select(Orders::getId);
        List<Orders> userOrders = orderService.list(orderWrapper);
        if (userOrders.isEmpty()) {
            return Collections.emptySet();
        }

        // 批量查询所有订单明细，消除逐单 N+1
        List<Long> orderIds = userOrders.stream().map(Orders::getId).collect(Collectors.toList());
        LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(OrderDetail::getOrderId, orderIds);
        detailWrapper.isNotNull(OrderDetail::getDishId);
        detailWrapper.select(OrderDetail::getDishId);
        List<OrderDetail> details = orderDetailService.list(detailWrapper);

        Set<Long> dishIds = new HashSet<>();
        for (OrderDetail detail : details) {
            if (detail.getDishId() != null) {
                dishIds.add(detail.getDishId());
            }
        }
        return dishIds;
    }

    // ==================== 缓存与辅助方法 ====================

    /**
     * 保存推荐结果到缓存
     * <p>
     * 注意：此方法由异步线程池（asyncExecutor）调用，且内部已捕获全部异常做降级处理，
     * 因此 {@code @Transactional} 在此不适用——事务注解对异步线程不生效，且吞异常会阻止回滚。
     * 缓存写入属尽力而为（best-effort）操作：失败仅记日志，下次请求会重新计算并覆盖，无需事务保护。
     */
    void saveToCache(Long userId, Integer type, List<Map<String, Object>> result, String algorithm) {
        try {
            // 删除旧缓存
            LambdaQueryWrapper<RecommendationCache> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RecommendationCache::getUserId, userId)
                   .eq(RecommendationCache::getRecommendType, type);
            cacheMapper.delete(wrapper);

            // 计算置信度
            double score = computeConfidence(result, algorithm);

            // 构建缓存记录
            RecommendationCache cache = new RecommendationCache();
            cache.setUserId(userId);
            cache.setRecommendType(type);
            cache.setAlgoName(algorithm);
            cache.setScore(BigDecimal.valueOf(score));
            cache.setExpireTime(LocalDateTime.now().plusHours(CACHE_EXPIRE_HOURS));

            // 序列化菜品ID列表
            List<Long> dishIdList = result.stream()
                    .map(m -> (Long) m.get("id"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            cache.setDishIds(objectMapper.writeValueAsString(dishIdList));

            cacheMapper.insert(cache);
        } catch (Exception e) {
            log.warn("[推荐引擎] 缓存保存失败", e);
        }
    }

    /**
     * 计算推荐置信度
     * 置信度作为缓存的 score 字段，用于多算法结果比较
     */
    private double computeConfidence(List<Map<String, Object>> result, String algorithm) {
        if (result == null || result.isEmpty()) return CONFIDENCE_EMPTY;
        switch (algorithm) {
            case RecommendationCache.ALGO_HYBRID: return CONFIDENCE_HYBRID;
            case RecommendationCache.ALGO_CF: return CONFIDENCE_CF;
            case RecommendationCache.ALGO_CONTENT: return CONFIDENCE_CONTENT;
            case RecommendationCache.ALGO_HOT: return CONFIDENCE_HOT;
            default: return CONFIDENCE_DEFAULT;
        }
    }

    /**
     * 解析缓存的菜品ID JSON
     */
    private List<Long> parseDishIds(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("[推荐引擎] JSON解析失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建菜品结果列表（批量加载，避免N+1查询）
     */
    private List<Map<String, Object>> buildDishResultList(List<Long> dishIds, int limit) {
        if (dishIds == null || dishIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> limitedIds = dishIds.size() <= limit
                ? dishIds
                : new ArrayList<>(dishIds.subList(0, limit));
        // 批量查询，避免逐个 getById 导致的 N+1
        List<Dish> dishes = dishService.listByIds(limitedIds);
        if (dishes == null || dishes.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.Map<Long, Dish> dishMap = new java.util.LinkedHashMap<>();
        for (Dish d : dishes) {
            dishMap.put(d.getId(), d);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : limitedIds) {
            Dish dish = dishMap.get(id);
            if (dish != null) {
                result.add(dishToMap(dish));
            }
        }
        return result;
    }

    /**
     * 构建套餐结果列表（批量加载，避免N+1查询）
     */
    private List<Map<String, Object>> buildSetmealResultList(List<Long> ids, int limit) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> limitedIds = ids.size() <= limit
                ? ids
                : new ArrayList<>(ids.subList(0, limit));
        // 批量查询，避免逐个 getById 导致的 N+1
        List<Setmeal> setmeals = setmealService.listByIds(limitedIds);
        if (setmeals == null || setmeals.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.Map<Long, Setmeal> setmealMap = new java.util.LinkedHashMap<>();
        for (Setmeal s : setmeals) {
            setmealMap.put(s.getId(), s);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : limitedIds) {
            Setmeal setmeal = setmealMap.get(id);
            if (setmeal != null) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", setmeal.getId());
                map.put("name", setmeal.getName());
                map.put("image", setmeal.getImage());
                map.put("price", setmeal.getPrice());
                map.put("description", setmeal.getDescription());
                map.put("type", "setmeal");
                result.add(map);
            }
        }
        return result;
    }

    /**
     * 菜品实体转Map
     */
    private Map<String, Object> dishToMap(Dish dish) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", dish.getId());
        map.put("name", dish.getName());
        map.put("image", dish.getImage());
        map.put("price", dish.getPrice());
        map.put("description", dish.getDescription());
        map.put("categoryId", dish.getCategoryId());
        map.put("status", dish.getStatus());
        return map;
    }

    /**
     * 根据分类ID查找套餐
     */
    private List<Long> findSetmealsByCategory(Long targetId, int limit) {
        if (targetId == null) return Collections.emptyList();
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Setmeal::getCategoryId, targetId)
               .eq(Setmeal::getStatus, 1)
               .orderByDesc(Setmeal::getUpdateTime)
               .last("LIMIT " + sanitizeLimit(limit));
        return setmealService.list(wrapper).stream()
                .map(Setmeal::getId)
                .collect(Collectors.toList());
    }

    /**
     * 查找热门套餐
     */
    private List<Long> findHotSetmeals(Long tenantId, int limit) {
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, Setmeal::getTenantId, tenantId)
               .eq(Setmeal::getStatus, 1)
               .orderByDesc(Setmeal::getUpdateTime)
               .last("LIMIT " + sanitizeLimit(limit));
        return setmealService.list(wrapper).stream()
                .map(Setmeal::getId)
                .collect(Collectors.toList());
    }

    /**
     * 校验 LIMIT 参数，防止负值/过大值导致查询异常
     * 修复说明：原实现 .last("LIMIT " + limit) 中 limit 为 int 类型，
     * 虽不存在字符串注入，但负值/超大值（如 limit * 2 溢出）会生成非法或性能恶化的 SQL。
     */
    private static int sanitizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}





