package com.reggie.module.dashboard.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据概览仪表盘控制器
 * <p>
 * 提供后台管理首页所需的全部实时统计数据。
 * 数据通过Redis缓存加速，详见 DashboardServiceImpl 中的缓存策略说明。
 *
 * <p>Redis应用场景：
 * <ul>
 *   <li><b>String缓存</b>：7日趋势数据（dashboard:trend:{tenantId}）</li>
 *   <li><b>Hash缓存</b>：今日概览KPI（dashboard:overview:{tenantId}）、订单状态分布（dashboard:order-status:{tenantId}）</li>
 *   <li><b>ZSet排行</b>：热销菜品实时排行（dashboard:hot-dishes:{tenantId}）</li>
 *   <li><b>健康检查</b>：Redis连通性检测（dashboard:health-check）</li>
 * <ul>
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/api/dashboard")
@Slf4j
@Tag(name = "数据概览仪表盘", description = "后台管理首页实时统计数据接口，支持Redis缓存加速")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取今日核心KPI概览数据
     * <p>包含：总订单数、营业额、待处理订单、完成率、活跃用户等
     * <p>Redis: Hash key=dashboard:overview:{tenantId}, TTL=5min
     */
    @GetMapping("/overview")
    @Operation(summary = "今日概览", description = "获取今日核心运营指标数据（总订单数、营业额、完成率等）")
    public R<Map<String, Object>> overview() {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[Dashboard] 获取今日概览 tenantId={}", tenantId);

        Map<String, Object> data = dashboardService.getOverview(tenantId);

        // 附加Redis状态标识
        data.put("redisEnabled", dashboardService.isRedisAvailable());
        data.put("serverTime", System.currentTimeMillis());
        return R.success(data);
    }

    /**
     * 获取最近7天趋势数据
     * <p>Redis: String key=dashboard:trend:{tenantId}, TTL=30min
     */
    @GetMapping("/trend")
    @Operation(summary = "7日趋势", description = "获取最近7天订单数和营业额趋势")
    public R<List<Map<String, Object>>> trend() {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[Dashboard] 获取趋势数据 tenantId={}", tenantId);
        List<Map<String, Object>> data = dashboardService.getTrend(tenantId);
        return R.success(data);
    }

    /**
     * 获取当前订单状态分布
     * <p>Redis: Hash key=dashboard:order-status:{tenantId}, TTL=2min
     */
    @GetMapping("/order-status")
    @Operation(summary = "订单状态分布", description = "获取今日各状态订单数量分布（待接单、已接单、已完成等）")
    public R<Map<String, Object>> orderStatus() {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[Dashboard] 获取订单状态分布 tenantId={}", tenantId);
        Map<String, Object> data = dashboardService.getOrderStatusDistribution(tenantId);

        data.put("redisEnabled", dashboardService.isRedisAvailable());
        return R.success(data);
    }

    /**
     * 获取热销菜品排名
     * <p>Redis: ZSet key=dashboard:hot-dishes:{tenantId}, TTL=15min
     * <p>使用ZSet的reverseRangeWithScores获取Top N
     */
    @GetMapping("/hot-dishes")
    @Operation(summary = "热销菜品", description = "获取今日热销菜品排行Top N")
    public R<List<Map<String, Object>>> hotDishes(
            @Parameter(name = "limit", description = "返回的热销菜品数量", required = true, example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[Dashboard] 获取热销菜品 tenantId={} limit={}", tenantId, limit);
        List<Map<String, Object>> data = dashboardService.getHotDishes(tenantId, limit);
        return R.success(data);
    }

    /**
     * 获取系统健康状态
     * <p>检查Redis连接、数据库状态、JVM信息等
     */
    @GetMapping("/health")
    @Operation(summary = "系统健康", description = "获取各组件健康状态（Redis、数据库、JVM等）")
    public R<Map<String, Object>> health() {
        log.info("[Dashboard] 获取系统健康状态");
        try {
            Map<String, Object> data = dashboardService.getSystemHealth();
            return R.success(data);
        } catch (Exception e) {
            log.error("[Dashboard] 系统健康状态获取异常: {}", e.getMessage(), e);
            return R.success(healthErrorPlaceholder(e));
        }
    }

    /**
     * 获取汇总面板数据（一次性获取所有仪表盘数据）
     * <p>前端首页调用此接口可减少请求次数
     */
    @GetMapping("/all")
    @Operation(summary = "汇总数据", description = "一次性获取仪表盘所有数据（概览、趋势、状态分布、热销菜品、健康状态），减少前端请求次数")
    public R<Map<String, Object>> all(
            @Parameter(name = "hotDishLimit", description = "热销菜品返回数量", required = true, example = "10")
            @RequestParam(defaultValue = "10") int hotDishLimit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[Dashboard] 获取汇总数据 tenantId={}", tenantId);

        Map<String, Object> result = new HashMap<>();
        try {
            result.put("overview", dashboardService.getOverview(tenantId));
        } catch (Exception e) {
            log.error("[Dashboard] 概览数据获取异常: {}", e.getMessage(), e);
            result.put("overview", errorPlaceholder("概览", e));
        }
        try {
            result.put("trend", dashboardService.getTrend(tenantId));
        } catch (Exception e) {
            log.error("[Dashboard] 趋势数据获取异常: {}", e.getMessage(), e);
            result.put("trend", new ArrayList<>());
        }
        try {
            result.put("orderStatus", dashboardService.getOrderStatusDistribution(tenantId));
        } catch (Exception e) {
            log.error("[Dashboard] 订单状态分布获取异常: {}", e.getMessage(), e);
            result.put("orderStatus", errorPlaceholder("订单状态", e));
        }
        try {
            result.put("hotDishes", dashboardService.getHotDishes(tenantId, hotDishLimit));
        } catch (Exception e) {
            log.error("[Dashboard] 热销菜品获取异常: {}", e.getMessage(), e);
            result.put("hotDishes", new ArrayList<>());
        }
        try {
            result.put("health", dashboardService.getSystemHealth());
        } catch (Exception e) {
            log.error("[Dashboard] 系统健康获取异常: {}", e.getMessage(), e);
            result.put("health", healthErrorPlaceholder(e));
        }
        result.put("serverTime", System.currentTimeMillis());
        return R.success(result);
    }

    private Map<String, Object> errorPlaceholder(String moduleName, Exception e) {
        Map<String, Object> map = new HashMap<>();
        map.put("totalOrders", 0);
        map.put("totalRevenue", 0);
        map.put("errorMsg", moduleName + "异常: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        map.put("cacheSource", "Error");
        return map;
    }

    private Map<String, Object> healthErrorPlaceholder(Exception e) {
        Map<String, Object> map = new HashMap<>();
        map.put("redisAvailable", false);
        map.put("redisInfo", "异常");
        map.put("dbAvailable", false);
        map.put("dbInfo", "异常: " + (e.getMessage() != null ? e.getMessage() : "未知"));
        map.put("javaVersion", System.getProperty("java.version"));
        map.put("osName", System.getProperty("os.name"));
        map.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        return map;
    }
}

