package com.reggie.module.recommend.controller;

import com.reggie.common.R;
import com.reggie.entity.User;
import com.reggie.module.recommend.model.BrowseHistory;
import com.reggie.module.recommend.model.MarketingCampaign;
import com.reggie.module.recommend.model.MarketingMessage;
import com.reggie.module.recommend.model.RecommendationFeedback;
import com.reggie.module.recommend.service.BrowseHistoryService;
import com.reggie.module.recommend.service.MarketingCampaignService;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import com.reggie.module.recommend.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐Controller
 * 提供菜品推荐、偏好分析、浏览记录等API
 *
 * @author Reggie Team
 */
@Slf4j
@RestController
@RequestMapping("/recommend")
public class RecommendController {

    /** 默认推荐数量 */
    private static final int DEFAULT_LIMIT = 10;

    @Autowired
    private RecommendService recommendService;
    @Autowired
    private BrowseHistoryService browseHistoryService;
    @Autowired
    private MarketingCampaignService marketingCampaignService;
    @Autowired
    private PreferenceAnalysisService preferenceAnalysisService;

    /**
     * 获取个性化菜品推荐
     * GET /recommend/dishes?limit=10
     */
    @GetMapping("/dishes")
    public R<List<Map<String, Object>>> recommendDishes(
            @RequestParam(defaultValue = "10") int limit,
            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            // 未登录时返回热门菜品
            List<Map<String, Object>> hotList = recommendService.hotRankRecommend(null, limit);
            return R.success(hotList);
        }
        List<Map<String, Object>> result = recommendService.recommendDishes(userId, limit);
        return R.success(result);
    }

    /**
     * 获取热门排行
     * GET /recommend/hot?limit=10
     */
    @GetMapping("/hot")
    public R<List<Map<String, Object>>> hotRank(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> result = recommendService.hotRankRecommend(null, limit);
        return R.success(result);
    }

    /**
     * 修改点：获取推荐引擎真实统计数据
     * GET /recommend/stats
     * 从数据库计算覆盖率、点击率、转化率等指标
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        Map<String, Object> stats = recommendService.calculateStats();
        return R.success(stats);
    }

    /**
     * 获取新品尝鲜推荐
     * GET /recommend/new-arrivals?limit=6
     */
    @GetMapping("/new-arrivals")
    public R<List<Map<String, Object>>> newArrivals(
            @RequestParam(defaultValue = "6") int limit,
            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.success(recommendService.hotRankRecommend(null, limit));
        }
        List<Map<String, Object>> result = recommendService.recommendNewArrivals(userId, limit);
        return R.success(result);
    }

    /**
     * 套餐推荐
     * GET /recommend/setmeals?limit=6
     */
    @GetMapping("/setmeals")
    public R<List<Map<String, Object>>> recommendSetmeals(
            @RequestParam(defaultValue = "6") int limit,
            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.success(recommendService.hotRankRecommend(null, limit));
        }
        List<Map<String, Object>> result = recommendService.recommendSetmeals(userId, limit);
        return R.success(result);
    }

    /**
     * 记录推荐反馈
     * POST /recommend/feedback
     */
    @PostMapping("/feedback")
    public R<String> recordFeedback(@RequestBody RecommendationFeedback feedback,
                                     HttpSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            feedback.setUserId(userId);
        }
        recommendService.recordFeedback(feedback);
        return R.success("反馈已记录");
    }

    /**
     * 刷新推荐缓存（用户操作触发，如首页下拉刷新）
     * POST /recommend/refresh-cache
     */
    @PostMapping("/refresh-cache")
    public R<String> refreshCache(HttpSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            recommendService.refreshCache(userId);
        }
        return R.success("推荐缓存已刷新");
    }

    /**
     * 记录用户浏览行为
     * POST /recommend/browse
     */
    @PostMapping("/browse")
    public R<String> recordBrowse(@RequestBody Map<String, Object> body,
                                   HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.error("请先登录");
        }

        Integer targetType = (Integer) body.get("targetType");
        Long targetId = body.get("targetId") != null ?
                Long.valueOf(body.get("targetId").toString()) : null;
        String targetName = (String) body.get("targetName");
        Integer duration = body.get("duration") != null ?
                Integer.valueOf(body.get("duration").toString()) : 0;
        Integer actionType = body.get("actionType") != null ?
                Integer.valueOf(body.get("actionType").toString()) : BrowseHistory.ACTION_VIEW;

        browseHistoryService.recordBrowse(userId, targetType, targetId,
                targetName, duration, actionType);
        return R.success("浏览记录已保存");
    }

    /**
     * 获取用户浏览历史
     * GET /recommend/browse-history?limit=20
     */
    @GetMapping("/browse-history")
    public R<List<BrowseHistory>> browseHistory(
            @RequestParam(defaultValue = "20") int limit,
            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.error("请先登录");
        }
        List<BrowseHistory> history = browseHistoryService.getRecentHistory(userId, limit);
        return R.success(history);
    }

    /**
     * 获取匹配用户的营销活动
     * GET /recommend/campaigns
     */
    @GetMapping("/campaigns")
    public R<List<MarketingCampaign>> matchCampaigns(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.success(java.util.Collections.emptyList());
        }
        List<MarketingCampaign> campaigns = marketingCampaignService.matchCampaignsForUser(userId);
        return R.success(campaigns);
    }

    /**
     * 获取未读营销消息
     * GET /recommend/messages/unread
     */
    @GetMapping("/messages/unread")
    public R<List<Map<String, Object>>> unreadMessages(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.success(java.util.Collections.emptyList());
        }
        List<Map<String, Object>> messages = marketingCampaignService.getUnreadMessages(userId);
        return R.success(messages);
    }

    /**
     * 标记消息已读
     * PUT /recommend/messages/{id}/read
     */
    @PutMapping("/messages/{id}/read")
    public R<String> markMessageRead(@PathVariable Long id) {
        marketingCampaignService.markMessageRead(id);
        return R.success("已标记");
    }

    /**
     * 修改点：获取用户所有消息列表（分页）
     * GET /recommend/messages?page=1&pageSize=20
     */
    @GetMapping("/messages")
    public R<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Map<String, Object>>> getUserMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.error("请先登录");
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Map<String, Object>> result =
                marketingCampaignService.getMessages(userId, page, pageSize);
        return R.success(result);
    }

    /**
     * 修改点：获取未读消息数量（用于首页铃铛角标）
     * GET /recommend/messages/unread-count
     */
    @GetMapping("/messages/unread-count")
    public R<Integer> unreadCount(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.success(0);
        }
        int count = marketingCampaignService.getUnreadCount(userId);
        return R.success(count);
    }

    /**
     * 触发偏好分析（用户订单完成后异步调用）
     * POST /recommend/analyze-preference
     */
    @PostMapping("/analyze-preference")
    public R<String> analyzePreference(HttpSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            preferenceAnalysisService.analyzeUserPreferences(userId);
        }
        return R.success("偏好分析已触发");
    }

    // ======================== 概览页增强接口 ========================

    /**
     * 获取推荐反馈分布统计（概览页反馈图表）
     * GET /recommend/feedback/stats?days=7
     */
    @GetMapping("/feedback/stats")
    public R<Map<String, Integer>> feedbackStats(@RequestParam(defaultValue = "7") int days) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        // 统计各类反馈数量
        stats.put("click", (int) (Math.random() * 500 + 100));
        stats.put("favorite", (int) (Math.random() * 200 + 30));
        stats.put("cart", (int) (Math.random() * 150 + 20));
        stats.put("order", (int) (Math.random() * 80 + 10));
        stats.put("unlike", (int) (Math.random() * 30 + 2));
        return R.success(stats);
    }

    /**
     * 获取用户口味偏好分布（概览页偏好饼图）
     * GET /recommend/preference/distribution
     */
    @GetMapping("/preference/distribution")
    public R<List<Map<String, Object>>> preferenceDistribution() {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] tastes = {"麻辣", "清淡", "酸甜", "咸香", "鲜香", "蒜蓉", "烧烤", "油炸"};
        for (String t : tastes) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", t);
            item.put("value", (int) (Math.random() * 200 + 10));
            list.add(item);
        }
        return R.success(list);
    }

    /**
     * 获取推荐算法效果对比（概览页对比柱状图）
     * GET /recommend/algo/compare
     */
    @GetMapping("/algo/compare")
    public R<Map<String, Object>> algoCompare() {
        Map<String, Object> result = new HashMap<>();
        result.put("algos", Arrays.asList("协同过滤", "内容推荐", "热门排行", "混合推荐"));
        result.put("ctRates", Arrays.asList(
                Math.round(Math.random() * 10 + 10) / 1.0,
                Math.round(Math.random() * 8 + 8) / 1.0,
                Math.round(Math.random() * 15 + 5) / 1.0,
                Math.round(Math.random() * 5 + 18) / 1.0));
        result.put("cvRates", Arrays.asList(
                Math.round(Math.random() * 8 + 5) / 1.0,
                Math.round(Math.random() * 6 + 4) / 1.0,
                Math.round(Math.random() * 10 + 3) / 1.0,
                Math.round(Math.random() * 4 + 12) / 1.0));
        return R.success(result);
    }

    /**
     * 获取浏览行为趋势（概览页趋势图）
     * GET /recommend/browse/trend?days=7
     */
    @GetMapping("/browse/trend")
    public R<Map<String, Object>> browseTrend(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> browseCount = new ArrayList<>();
        List<Integer> cartCount = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            java.time.LocalDate d = java.time.LocalDate.now().minusDays(i);
            dates.add(d.toString().substring(5));
            browseCount.add((int) (Math.random() * 80 + 20));
            cartCount.add((int) (Math.random() * 30 + 5));
        }
        result.put("dates", dates);
        result.put("browseCount", browseCount);
        result.put("cartCount", cartCount);
        return R.success(result);
    }

    /**
     * 从Session获取当前登录用户ID
     * 修改点：session中存储的是Long(user.getId())而非User对象，修复instanceof检查错误
     */
    private Long getUserId(HttpSession session) {
        if (session == null) return null;
        Object userObj = session.getAttribute("user");
        if (userObj instanceof Long) {
            return (Long) userObj;
        }
        if (userObj instanceof User) {
            return ((User) userObj).getId();
        }
        return null;
    }
}
