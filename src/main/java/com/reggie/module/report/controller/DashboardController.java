package com.reggie.module.report.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.time.LocalDateTime;

/**
 * 数据概览控制器
 * 提供首页Dashboard所需的全部聚合数据，含订单KPI、趋势图、订单状态分布、热销菜品、系统状态
 * 修改点：新增 /api/dashboard/all 端点，聚合所有Dashboard数据返回前端
 *
 * @author reggie
 * @since 2026-07-11
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "数据概览", description = "首页Dashboard数据看板API")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取Dashboard全部聚合数据
     * GET /api/dashboard/all
     * 包含：overview KPI卡片 + 近7日趋势 + 今日订单状态分布 + 热销菜品TopN + 系统健康 + 库存预警摘要
     */
    @GetMapping("/all")
    @Operation(summary = "首页总览", description = "获取数据概览全部聚合数据：4项KPI+近7日趋势+订单状态分布+热销菜品+系统健康+库存预警")
    @Parameter(name = "hotDishLimit", description = "热销菜品返回数量上限", required = false, example = "10")
    public R<Map<String, Object>> all(@RequestParam(defaultValue = "10") int hotDishLimit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = new LinkedHashMap<>();

        try {
            // 概览KPI
            data.put("overview", dashboardService.getOverview(tenantId));
        } catch (Exception e) {
            log.warn("Dashboard概览数据获取失败", e);
            data.put("overview", Collections.emptyMap());
        }

        try {
            // 近7日趋势
            data.put("trend", dashboardService.getTrend(tenantId));
        } catch (Exception e) {
            log.warn("Dashboard趋势数据获取失败", e);
            data.put("trend", Collections.emptyList());
        }

        try {
            // 今日订单状态分布
            data.put("orderStatus", dashboardService.getOrderStatusDistribution(tenantId));
        } catch (Exception e) {
            log.warn("Dashboard订单状态获取失败", e);
            data.put("orderStatus", Collections.emptyMap());
        }

        try {
            // 热销菜品TopN
            data.put("hotDishes", dashboardService.getHotDishes(tenantId, hotDishLimit));
        } catch (Exception e) {
            log.warn("Dashboard热销菜品获取失败", e);
            data.put("hotDishes", Collections.emptyList());
        }

        try {
            // 系统健康状态
            data.put("health", dashboardService.getSystemHealth());
        } catch (Exception e) {
            log.warn("Dashboard系统健康获取失败", e);
            data.put("health", Collections.emptyMap());
        }

        data.put("serverTime", LocalDateTime.now().toString());
        return R.success(data);
    }
}
