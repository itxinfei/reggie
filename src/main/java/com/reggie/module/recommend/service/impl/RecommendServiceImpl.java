package com.reggie.module.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.dish.model.DishFlavor;
import com.reggie.module.category.model.Category;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.setmeal.service.SetmealService;
import com.reggie.module.recommend.mapper.*;
import com.reggie.module.recommend.model.*;
import com.reggie.module.recommend.service.RecommendService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.dish.model.DishFlavor;
import com.reggie.module.category.model.Category;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.setmeal.service.SetmealService;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.category.model.Category;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
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
/**
 * Recommend service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    /** 推荐缓存过期时间(小时) */
    private static final int CACHE_EXPIRE_HOURS = 6;
    /** 协同过滤最小用户数 */
    private static final int CF_MIN_USERS = 5;
    /** 推荐结果多样性因子（越高越多随机性） */
    private static final double DIVERSITY_FACTOR = 0.2;
    /** 异步任务线程池（Spring 管理，应用关闭时优雅停机；替代原静态 FixedThreadPool） */
    @Resource(name = "recommendExecutor")
    private ThreadPoolTaskExecutor ASYNC_EXECUTOR;

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
    private PreferenceAnalysisServiceImpl preferenceAnalysisService;

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

    /**
     * 应用启动时清理过期缓存
     */
    @PostConstruct
    public void init() {
        try {
            int deleted = cacheMapper.deleteExpired();
            log.info("[推荐引擎] 初始化完成，清理过期缓存 {} 条", deleted);
        } catch (Exception e) {
            log.warn("[推荐引擎] 初始化跳过（可能是测试环境缺少数据表）: {}", e.getMessage());
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
                log.debug("[推荐引擎] 命中缓存 userId={}, algorithm={}", userId, cache.getAlgorithm());
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
        ASYNC_EXECUTOR.submit(() -> saveToCache(userId, RecommendationCache.TYPE_DISH, finalResult, finalAlgorithm));

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
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, Dish::getTenantId, tenantId)
               .eq(Dish::getStatus, 1)
               .ge(Dish::getCreateTime, thirtyDaysAgo)
               .orderByDesc(Dish::getCreateTime)
               .last("LIMIT " + limit * 2);

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

        // 5. 基于内容匹配评分
        Map<Dish, Double> scoredDishes = new HashMap<>();
        for (Dish dish : candidateDishes) {
            if (purchasedIds.contains(dish.getId())) {
                continue;
            }
            double score = calculateContentScore(dish, tastePrefs, categoryPrefs, tags);
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

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
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
            ASYNC_EXECUTOR.submit(() -> preferenceAnalysisService.analyzeUserPreferences(feedback.getUserId()));
        }
    }

    @Override
    public void refreshCache(Long userId) {
        // 删除用户所有缓存，触发下次请求时重新计算
        LambdaQueryWrapper<RecommendationCache> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendationCache::getUserId, userId);
        cacheMapper.delete(wrapper);
        // 异步重新分析偏好
        ASYNC_EXECUTOR.submit(() -> preferenceAnalysisService.analyzeUserPreferences(userId));
        log.info("[推荐引擎] 刷新用户{}的推荐缓存完成", userId);
    }

    @Override
    public Map<String, Object> calculateStats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            // 1. 总用户数（有订单记录的用户）
            LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
            if (tenantId != null) {
                orderWrapper.eq(Orders::getTenantId, tenantId);
            }
            int totalOrderUsers = (int) orderService.count(orderWrapper);

            // 2. 有推荐缓存的用户数 → 覆盖率
            LambdaQueryWrapper<RecommendationCache> cacheWrapper = new LambdaQueryWrapper<>();
            cacheWrapper.gt(RecommendationCache::getExpireTime, LocalDateTime.now());
            if (tenantId != null) {
                cacheWrapper.eq(RecommendationCache::getTenantId, tenantId);
            }
            int cachedUsers = (int) cacheMapper.selectCount(cacheWrapper);
            int hybridRate = totalOrderUsers > 0 ? (int) (cachedUsers * 100.0 / totalOrderUsers) : 0;

            // 3. 推荐反馈统计 → 点击率(click) / 转化率(order)
            LambdaQueryWrapper<RecommendationFeedback> feedbackWrapper =
                    new LambdaQueryWrapper<>();
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
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

            // 4. 推荐贡献GMV：近7天通过推荐反馈产生的订单金额
            double recommendGMV = 0;
            if (orderCount > 0) {
                Set<Long> feedbackUserIds = feedbacks.stream()
                        .filter(f -> f.getFeedbackType() == RecommendationFeedback.FEEDBACK_ORDER)
                        .map(RecommendationFeedback::getUserId)
                        .collect(Collectors.toSet());
                LambdaQueryWrapper<Orders> gmvWrapper = new LambdaQueryWrapper<>();
                gmvWrapper.in(Orders::getUserId, feedbackUserIds)
                        .ge(Orders::getCreateTime, sevenDaysAgo)
                        .eq(Orders::getStatus, 4); // 已完成订单
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
            log.warn("[推荐引擎] 统计数据计算异常: {}", e.getMessage());
            stats.put("hybridRate", 0);
            stats.put("clickRate", 0);
            stats.put("conversionRate", 0);
            stats.put("recommendGMV", 0);
        }

        return stats;
    }

    // ==================== 修改点：概览页真实统计方法（替换Math.random()假数据） ====================

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
                    default:
                        break;
                }
            }
            log.debug("[推荐引擎] 反馈分布统计: days={}, stats={}", days, stats);
        } catch (Exception e) {
            log.warn("[推荐引擎] 反馈分布统计异常: {}", e.getMessage());
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
            log.warn("[推荐引擎] 偏好分布查询异常: {}", e.getMessage());
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
            String startTime = LocalDateTime.now().minusDays(30).toString();
            Long tenantId = BaseContext.getCurrentTenantId();
            List<Map<String, Object>> rows = feedbackMapper.countByAlgorithmSince(startTime, tenantId);

            // 算法名 -> 索引映射
            Map<String, Integer> algoIndex = new HashMap<>();
            algoIndex.put("CF", 0);
            algoIndex.put("CONTENT", 1);
            algoIndex.put("HOT", 2);
            algoIndex.put("HYBRID", 3);

            double[] ctRates = new double[]{0.0, 0.0, 0.0, 0.0};
            double[] cvRates = new double[]{0.0, 0.0, 0.0, 0.0};

            for (Map<String, Object> row : rows) {
                String algo = (String) row.get("algorithm");
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
            log.warn("[推荐引擎] 算法对比查询异常: {}", e.getMessage());
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

            // 构建日期索引Map，方便按日期查找
            Map<String, Map<String, Object>> dateMap = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String date = row.get("date").toString();
                dateMap.put(date, row);
            }

            // 按日期顺序填充，缺失日期补0
            for (int i = days - 1; i >= 0; i--) {
                java.time.LocalDate d = java.time.LocalDate.now().minusDays(i);
                String dateKey = d.toString();
                String label = d.toString().substring(5); // MM-DD 格式
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
            log.warn("[推荐引擎] 浏览趋势查询异常: {}", e.getMessage());
            // 异常时返回空日期+0值
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

    // ==================== 私有方法：核心算法逻辑 ====================

    /**
     * 混合推荐：协同过滤 + 内容推荐 + 热门排行加权融合
     * 权重分配：CF:50%, Content:30%, Hot:20%
     */
    private List<Map<String, Object>> hybridRecommend(Long userId, Long tenantId, int limit) {
        // 并发执行三种推荐算法
        Future<List<Map<String, Object>>> cfFuture = ASYNC_EXECUTOR.submit(
                () -> collaborativeFiltering(userId, limit * 2));
        Future<List<Map<String, Object>>> contentFuture = ASYNC_EXECUTOR.submit(
                () -> contentBasedRecommend(userId, limit * 2));
        Future<List<Map<String, Object>>> hotFuture = ASYNC_EXECUTOR.submit(
                () -> hotRankRecommend(tenantId, limit));

        try {
            List<Map<String, Object>> cfResult = cfFuture.get(3, TimeUnit.SECONDS);
            List<Map<String, Object>> contentResult = contentFuture.get(3, TimeUnit.SECONDS);
            List<Map<String, Object>> hotResult = hotFuture.get(3, TimeUnit.SECONDS);

            // 加权融合
            Map<Long, Double> fusionScore = new HashMap<>();
            Map<Long, Map<String, Object>> dishInfoMap = new LinkedHashMap<>();

            // CF推荐 权重0.5
            mergeRecommendResult(cfResult, fusionScore, dishInfoMap, 0.5, "CF");
            // 内容推荐 权重0.3
            mergeRecommendResult(contentResult, fusionScore, dishInfoMap, 0.3, "Content");
            // 热门排行 权重0.2 + 多样性因子
            mergeRecommendResult(hotResult, fusionScore, dishInfoMap, 0.2 + DIVERSITY_FACTOR, "Hot");

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
            log.warn("[推荐引擎] 混合推荐异常，回退热门排行: {}", e.getMessage());
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
                                          List<UserPreferenceTag> allTags) {
        double score = 0.0;

        // 品类匹配
        if (dish.getCategoryId() != null) {
            Category category = categoryService.getById(dish.getCategoryId());
            if (category != null && categoryPrefs.contains(category.getName())) {
                score += 0.5;
            }
        }

        // 口味匹配（通过菜品描述/名称模糊匹配）
        if (dish.getName() != null && dish.getDescription() != null) {
            String text = dish.getName() + " " + dish.getDescription();
            for (String taste : tastePrefs) {
                if (text.contains(taste)) {
                    score += 0.3;
                }
            }
        }

        // 价格匹配
        for (UserPreferenceTag tag : allTags) {
            if (tag.getTagType() == UserPreferenceTag.TAG_TYPE_PRICE) {
                boolean priceMatch = isPriceInRange(dish.getPrice(), tag.getTagName());
                if (priceMatch) {
                    score += 0.2 * tag.getTagValue().doubleValue();
                }
            }
        }

        return Math.min(score, 1.0);
    }

    /**
     * 判断菜品价格是否在偏好区间内
     */
    private boolean isPriceInRange(BigDecimal price, String rangeTag) {
        if (price == null || rangeTag == null) return false;
        double p = price.doubleValue();
        switch (rangeTag) {
            case "经济型": return p < 20;
            case "实惠型": return p >= 20 && p < 40;
            case "中档": return p >= 40 && p < 80;
            case "高端": return p >= 80;
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
                .filter(e -> e.getValue() >= 3) // 至少3个共同菜品
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
     */
    @Transactional
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
            cache.setAlgorithm(algorithm);
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
            log.warn("[推荐引擎] 缓存保存失败: {}", e.getMessage());
        }
    }

    /**
     * 计算推荐置信度
     */
    private double computeConfidence(List<Map<String, Object>> result, String algorithm) {
        if (result == null || result.isEmpty()) return 0.0;
        switch (algorithm) {
            case RecommendationCache.ALGO_HYBRID: return 0.85;
            case RecommendationCache.ALGO_CF: return 0.70;
            case RecommendationCache.ALGO_CONTENT: return 0.65;
            case RecommendationCache.ALGO_HOT: return 0.40;
            default: return 0.50;
        }
    }

    /**
     * 解析缓存的菜品ID JSON
     */
    private List<Long> parseDishIds(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("[推荐引擎] JSON解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 构建菜品结果列表
     */
    private List<Map<String, Object>> buildDishResultList(List<Long> dishIds, int limit) {
        return dishIds.stream()
                .limit(limit)
                .map(id -> {
                    Dish dish = dishService.getById(id);
                    return dish != null ? dishToMap(dish) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建套餐结果列表
     */
    private List<Map<String, Object>> buildSetmealResultList(List<Long> ids, int limit) {
        return ids.stream()
                .limit(limit)
                .map(id -> {
                    Setmeal setmeal = setmealService.getById(id);
                    if (setmeal == null) return null;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", setmeal.getId());
                    map.put("name", setmeal.getName());
                    map.put("image", setmeal.getImage());
                    map.put("price", setmeal.getPrice());
                    map.put("description", setmeal.getDescription());
                    map.put("type", "setmeal");
                    return map;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
               .last("LIMIT " + limit);
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
               .last("LIMIT " + limit);
        return setmealService.list(wrapper).stream()
                .map(Setmeal::getId)
                .collect(Collectors.toList());
    }
}






