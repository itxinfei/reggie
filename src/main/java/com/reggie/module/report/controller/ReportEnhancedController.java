package com.reggie.module.report.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.report.service.ReportEnhancedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 增强报表控制器
 *
 * @author reggie
 * @since 2026-08-11
 */
@RestController
@RequestMapping("/report/enhanced")
@Tag(name = "增强报表管理")
@RequireEmployee
public class ReportEnhancedController {

    @Autowired
    private ReportEnhancedService reportEnhancedService;

    // ==================== 食材成本报表 ====================

    @GetMapping("/food-cost/report")
    @Operation(summary = "查询食材成本报表")
    public R<Map<String, Object>> getFoodCostReport(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getFoodCostReport(startDate, endDate, tenantId);
        return R.success(report);
    }

    @GetMapping("/food-cost/trend")
    @Operation(summary = "查询食材成本趋势")
    public R<Map<String, Object>> getFoodCostTrend(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = reportEnhancedService.getFoodCostTrend(startDate, endDate, tenantId);
        return R.success(trend);
    }

    @GetMapping("/food-cost/ranking")
    @Operation(summary = "查询食材成本排名")
    public R<List<Map<String, Object>>> getFoodCostRanking(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate,
            @Parameter(description = "条数上限") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> ranking = reportEnhancedService.getFoodCostRanking(startDate, endDate, limit, tenantId);
        return R.success(ranking);
    }

    // ==================== 增强销售报表 ====================

    @GetMapping("/sales/weekly")
    @Operation(summary = "查询周报")
    public R<Map<String, Object>> getWeeklyReport(
                        @Parameter(description = "年份") @RequestParam int year,
            @Parameter(description = "周序号") @RequestParam int week) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getWeeklyReport(year, week, tenantId);
        return R.success(report);
    }

    @GetMapping("/sales/monthly")
    @Operation(summary = "查询月报")
    public R<Map<String, Object>> getMonthlyReport(
                        @Parameter(description = "年份") @RequestParam int year,
            @Parameter(description = "月份") @RequestParam int month) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getMonthlyReport(year, month, tenantId);
        return R.success(report);
    }

    @GetMapping("/sales/yearly")
    @Operation(summary = "查询年报")
    public R<Map<String, Object>> getYearlyReport(
                        @Parameter(description = "年份") @RequestParam int year) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getYearlyReport(year, tenantId);
        return R.success(report);
    }

    @GetMapping("/sales/comparison")
    @Operation(summary = "查询销售对比分析")
    public R<Map<String, Object>> getSalesComparison(
                        @Parameter(description = "第一期间起始") @RequestParam String period1Start,
            @Parameter(description = "第一期间结束") @RequestParam String period1End,
            @Parameter(description = "第二期间起始") @RequestParam String period2Start,
            @Parameter(description = "第二期间结束") @RequestParam String period2End) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> comparison = reportEnhancedService.getSalesComparison(
                period1Start, period1End, period2Start, period2End, tenantId);
        return R.success(comparison);
    }

    @GetMapping("/sales/trend")
    @Operation(summary = "查询销售趋势")
    public R<Map<String, Object>> getSalesTrend(
                        @Parameter(description = "周期类型：day/week/month") @RequestParam(defaultValue = "day") String period,
            @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = reportEnhancedService.getSalesTrend(period, startDate, endDate, tenantId);
        return R.success(trend);
    }

    @GetMapping("/sales/top-items")
    @Operation(summary = "查询热销商品排名")
    public R<List<Map<String, Object>>> getTopSellingItems(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate,
            @Parameter(description = "商品类型：dish/setmeal") @RequestParam(defaultValue = "dish") String type,
            @Parameter(description = "条数上限") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> items = reportEnhancedService.getTopSellingItems(startDate, endDate, type, limit, tenantId);
        return R.success(items);
    }

    @GetMapping("/sales/time-period")
    @Operation(summary = "查询分时段销售分析")
    public R<Map<String, Object>> getSalesByTimePeriod(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportEnhancedService.getSalesByTimePeriod(startDate, endDate, tenantId);
        return R.success(data);
    }

    @GetMapping("/sales/customer-analysis")
    @Operation(summary = "查询客户分析")
    public R<Map<String, Object>> getCustomerAnalysis(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> analysis = reportEnhancedService.getCustomerAnalysis(startDate, endDate, tenantId);
        return R.success(analysis);
    }

    @GetMapping("/sales/revenue-forecast")
    @Operation(summary = "查询营收预测")
    public R<Map<String, Object>> getRevenueForecast(
                        @Parameter(description = "预测天数") @RequestParam(defaultValue = "7") int days) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> forecast = reportEnhancedService.getRevenueForecast(days, tenantId);
        return R.success(forecast);
    }
}
