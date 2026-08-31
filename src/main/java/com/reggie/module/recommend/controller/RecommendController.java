package com.reggie.module.recommend.controller;
import com.reggie.common.utils.PageUtils;

import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.user.model.User;
import com.reggie.module.recommend.dto.RecordBrowseDTO;
import com.reggie.module.recommend.model.BrowseHistory;
import com.reggie.module.marketing.model.MarketingCampaign;
import com.reggie.module.recommend.model.RecommendationFeedback;
import com.reggie.module.recommend.service.BrowseHistoryService;
import com.reggie.module.marketing.service.MarketingCampaignService;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import com.reggie.module.recommend.service.RecommendService;
import com.reggie.module.recommend.service.analytics.RecommendationAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 智能推荐控制器
 * 提供菜品推荐、偏好分析、浏览记录等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/recommend")
@Tag(name = "智能推荐", description = "菜品推荐、偏好分析、浏览记录等接口")
public class RecommendController {

    /** 默认推荐数量 */
    private static final int DEFAULT_LIMIT = 10;

    @Autowired
    private RecommendService recommendService;
    @Autowired
    private RecommendationAnalyticsService analyticsService;
    @Autowired
    private BrowseHistoryService browseHistoryService;
    @Autowired
    private MarketingCampaignService marketingCampaignService;
    @Autowired
    private PreferenceAnalysisService preferenceAnalysisService;

    /**
     * 获取个性化菜品推荐
     * @param limit 推荐数量
     * @param session HTTP会话
     * @return 推荐菜品列表（未登录时返回热门菜品）
     */
    @GetMapping("/dishes")
    @Operation(summary = "菜品推荐", description = "获取个性化菜品推荐，未登录时返回热门菜品")
    public R<List<Map<String, Object>>> recommendDishes(
            @Parameter(description = "L i m i t")
            @Parameter(description = "推荐数量", example = "10") @RequestParam(defaultValue = "10") int limit,
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
     * 获取热门菜品排行
     * @param limit 返回数量
     * @return 热门菜品排行列表
     */
    @GetMapping("/hot")
    @Operation(summary = "热门排行", description = "获取热门菜品排行")
    public R<List<Map<String, Object>>> hotRank(
            @Parameter(description = "L i m i t")
            @Parameter(description = "返回数量", example = "10") @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> result = recommendService.hotRankRecommend(null, limit);
        return R.success(result);
    }

    /**
     * 获取推荐引擎真实统计数据
     * @return 覆盖率、点击率、转化率等指标
     */
    @GetMapping("/stats")
    @RequireEmployee
    @Operation(summary = "推荐统计数据", description = "获取推荐引擎真实统计数据：覆盖率、点击率、转化率等指标")
    public R<Map<String, Object>> getStats() {
        Map<String, Object> stats = analyticsService.calculateStats();
        return R.success(stats);
    }

    /**
     * 获取新品尝鲜推荐列表
     * @param limit 推荐数量
     * @param session HTTP会话
     * @return 新品推荐列表
     */
    @GetMapping("/new-arrivals")
    @Operation(summary = "新品推荐", description = "获取新品尝鲜推荐列表")
    public R<List<Map<String, Object>>> newArrivals(
            @Parameter(description = "L i m i t")
            @Parameter(description = "推荐数量", example = "6") @RequestParam(defaultValue = "6") int limit,
            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.success(recommendService.hotRankRecommend(null, limit));
        }
        List<Map<String, Object>> result = recommendService.recommendNewArrivals(userId, limit);
        return R.success(result);
    }

    /**
     * 获取套餐推荐列表
     * @param limit 推荐数量
     * @param session HTTP会话
     * @return 套餐推荐列表
     */
    @GetMapping("/setmeals")
    @Operation(summary = "套餐推荐", description = "获取套餐推荐列表")
    public R<List<Map<String, Object>>> recommendSetmeals(
            @Parameter(description = "L i m i t")
            @Parameter(description = "推荐数量", example = "6") @RequestParam(defaultValue = "6") int limit,
            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.success(recommendService.hotRankRecommend(null, limit));
        }
        List<Map<String, Object>> result = recommendService.recommendSetmeals(userId, limit);
        return R.success(result);
    }

    /**
     * 记录用户对推荐结果的反馈
     * @param feedback 反馈信息
     * @param session HTTP会话
     * @return 操作结果
     */
    @PostMapping("/feedback")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "记录推荐反馈", description = "用户对推荐结果进行反馈（喜欢/不喜欢）")
    public R<String> recordFeedback(
            @Parameter(description = "反馈信息", required = true) @RequestBody RecommendationFeedback feedback,
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
     * @param session HTTP会话
     * @return 操作结果
     */
    @PostMapping("/refresh-cache")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "刷新推荐缓存", description = "用户操作触发推荐缓存刷新，如下拉刷新")
    public R<String> refreshCache(HttpSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            recommendService.refreshCache(userId);
        }
        return R.success("推荐缓存已刷新");
    }

    /**
     * 记录用户浏览行为
     * @param body 浏览信息（targetType/targetId/targetName/duration/actionType）
     * @param session HTTP会话
     * @return 操作结果
     */
    @PostMapping("/browse")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "记录浏览行为", description = "记录用户浏览菜品的行为，用于推荐算法")
    public R<String> recordBrowse(
            @Parameter(description = "浏览信息（targetType/targetId/targetName/duration/actionType）", required = true) @Valid @RequestBody RecordBrowseDTO dto,
            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.error("请先登录");
        }

        Integer targetType = dto.getTargetType();
        Long targetId = dto.getTargetId();
        String targetName = dto.getTargetName();
        Integer duration = dto.getDuration();
        Integer actionType = dto.getActionType() != null ? dto.getActionType() : BrowseHistory.ACTION_VIEW;

        browseHistoryService.recordBrowse(userId, targetType, targetId,
                targetName, duration, actionType);
        return R.success("浏览记录已保存");
    }

    /**
     * 获取用户浏览历史
     * @param limit 返回数量
     * @param session HTTP会话
     * @return 浏览历史列表
     */
    @GetMapping("/browse-history")
    @Operation(summary = "浏览历史", description = "获取用户浏览历史列表")
    public R<List<BrowseHistory>> browseHistory(
            @Parameter(description = "L i m i t")
            @Parameter(description = "返回数量", example = "20") @RequestParam(defaultValue = "20") int limit,
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
     * @param session HTTP会话
     * @return 营销活动列表
     */
    @GetMapping("/campaigns")
    @Operation(summary = "营销活动匹配", description = "获取匹配当前用户的营销活动列表")
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
     * @param session HTTP会话
     * @return 未读营销消息列表
     */
    @GetMapping("/messages/unread")
    @Operation(summary = "未读营销消息", description = "获取当前用户的未读营销消息列表")
    public R<List<Map<String, Object>>> unreadMessages(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.success(java.util.Collections.emptyList());
        }
        List<Map<String, Object>> messages = marketingCampaignService.getUnreadMessages(userId);
        return R.success(messages);
    }

    /**
     * 将指定营销消息标记为已读
     * @param id 消息ID
     * @return 操作结果
     */
    @PutMapping("/messages/{id}/read")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "标记消息已读", description = "将指定营销消息标记为已读")
    public R<String> markMessageRead(
            @Parameter(description = "消息ID", required = true) @PathVariable Long id) {
        marketingCampaignService.markMessageRead(id);
        return R.success("已标记");
    }

    /**
     * 获取当前用户的所有营销消息列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param session HTTP会话
     * @return 分页消息列表
     */
    @GetMapping("/messages")
    @Operation(summary = "用户消息列表", description = "获取当前用户的所有营销消息列表（分页）")
    public R<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Map<String, Object>>> getUserMessages(
            @Parameter(description = "P a g e")
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.error("请先登录");
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Map<String, Object>> result =
                marketingCampaignService.getMessages(userId, page, PageUtils.cap(pageSize));
        return R.success(result);
    }

    /**
     * 获取未读消息数量（用于首页铃铛角标）
     * @param session HTTP会话
     * @return 未读消息数量
     */
    @GetMapping("/messages/unread-count")
    @Operation(summary = "未读消息数量", description = "获取未读消息数量，用于首页铃铛角标显示")
    public R<Integer> unreadCount(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return R.success(0);
        }
        int count = marketingCampaignService.getUnreadCount(userId);
        return R.success(count);
    }

    /**
     * 触发用户口味偏好分析
     * @param session HTTP会话
     * @return 操作结果
     */
    @PostMapping("/analyze-preference")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "触发偏好分析", description = "触发用户口味偏好分析，通常在用户订单完成后调用")
    public R<String> analyzePreference(HttpSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            preferenceAnalysisService.analyzeUserPreferences(userId);
        }
        return R.success("偏好分析已触发");
    }

    // ======================== 修改点：概览页增强接口（全部改为真实DB统计） ========================

    /**
     * 获取推荐反馈分布统计（概览页反馈图表）
     * @param days 统计天数
     * @return 反馈分布统计
     */
    @GetMapping("/feedback/stats")
    @RequireEmployee
    @Operation(summary = "反馈统计", description = "获取推荐反馈分布统计，用于概览页反馈图表")
    public R<Map<String, Integer>> feedbackStats(
            @Parameter(description = "D a y s")
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "7") int days) {
        Map<String, Integer> stats = analyticsService.getFeedbackStats(days);
        return R.success(stats);
    }

    /**
     * 获取用户口味偏好分布（概览页偏好饼图）
     * @return 口味偏好分布列表
     */
    @GetMapping("/preference/distribution")
    @RequireEmployee
    @Operation(summary = "口味偏好分布", description = "获取用户口味偏好分布，用于概览页偏好饼图")
    public R<List<Map<String, Object>>> preferenceDistribution() {
        List<Map<String, Object>> list = analyticsService.getPreferenceDistribution();
        return R.success(list);
    }

    /**
     * 获取推荐算法效果对比（概览页对比柱状图）
     * @return 算法效果对比数据
     */
    @GetMapping("/algo/compare")
    @RequireEmployee
    @Operation(summary = "算法效果对比", description = "获取推荐算法效果对比数据，用于概览页对比柱状图")
    public R<Map<String, Object>> algoCompare() {
        Map<String, Object> result = analyticsService.getAlgoCompare();
        return R.success(result);
    }

    /**
     * 获取浏览行为趋势（概览页趋势图）
     * @param days 统计天数
     * @return 浏览行为趋势数据
     */
    @GetMapping("/browse/trend")
    @RequireEmployee
    @Operation(summary = "浏览趋势", description = "获取浏览行为趋势数据，用于概览页趋势图")
    public R<Map<String, Object>> browseTrend(
            @Parameter(description = "D a y s")
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = analyticsService.getBrowseTrend(days);
        return R.success(result);
    }

    /**
     * 从Session获取当前登录用户ID
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



