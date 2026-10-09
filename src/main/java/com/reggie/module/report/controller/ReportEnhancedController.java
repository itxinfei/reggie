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

    /**
     * 获取 food cost report。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/food-cost/report")
    @Operation(summary = "查询食材成本报表")
    public R<Map<String, Object>> getFoodCostReport(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getFoodCostReport(startDate, endDate, tenantId);
        return R.success(report);
    }

    /**
     * 获取 food cost trend。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/food-cost/trend")
    @Operation(summary = "查询食材成本趋势")
    public R<Map<String, Object>> getFoodCostTrend(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = reportEnhancedService.getFoodCostTrend(startDate, endDate, tenantId);
        return R.success(trend);
    }

    /**
     * 获取 food cost ranking。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param limit 参数 limit
     * @return 返回结果
     */
    @GetMapping("/food-cost/ranking")
    @Operation(summary = "查询食材成本排名")
    public R<List<Map<String, Object>>> getFoodCostRanking(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate,
            @Parameter(description = "条数上限") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> ranking = reportEnhancedService.getFoodCostRanking(startDate, endDate, limit,
                tenantId);
        return R.success(ranking);
    }

    // ==================== 增强销售报表 ====================

    /**
     * 获取 weekly report。
     * @param year 参数 year
     * @param week 参数 week
     * @return 返回结果
     */
    @GetMapping("/sales/weekly")
    @Operation(summary = "查询周报")
    public R<Map<String, Object>> getWeeklyReport(
                        @Parameter(description = "年份") @RequestParam int year,
            @Parameter(description = "周序号") @RequestParam int week) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getWeeklyReport(year, week, tenantId);
        return R.success(report);
    }

    /**
     * 获取 monthly report。
     * @param year 参数 year
     * @param month 参数 month
     * @return 返回结果
     */
    @GetMapping("/sales/monthly")
    @Operation(summary = "查询月报")
    public R<Map<String, Object>> getMonthlyReport(
                        @Parameter(description = "年份") @RequestParam int year,
            @Parameter(description = "月份") @RequestParam int month) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getMonthlyReport(year, month, tenantId);
        return R.success(report);
    }

    /**
     * 获取 yearly report。
     * @param year 参数 year
     * @return 返回结果
     */
    @GetMapping("/sales/yearly")
    @Operation(summary = "查询年报")
    public R<Map<String, Object>> getYearlyReport(
                        @Parameter(description = "年份") @RequestParam int year) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getYearlyReport(year, tenantId);
        return R.success(report);
    }

    /**
     * 获取 sales comparison。
     * @param period1Start 参数 period1Start
     * @param period1End 参数 period1End
     * @param period2Start 参数 period2Start
     * @param period2End 参数 period2End
     * @return 返回结果
     */
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

    /**
     * 获取 sales trend。
     * @param period 参数 period
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/sales/trend")
    @Operation(summary = "查询销售趋势")
    public R<Map<String, Object>> getSalesTrend(
                        @Parameter(description = "周期类型：day/week/month") @RequestParam(defaultValue =
                                "day") String period,
            @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = reportEnhancedService.getSalesTrend(period, startDate, endDate, tenantId);
        return R.success(trend);
    }

    /**
     * 获取 top selling items。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param type 参数 type
     * @param limit 参数 limit
     * @return 返回结果
     */
    @GetMapping("/sales/top-items")
    @Operation(summary = "查询热销商品排名")
    public R<List<Map<String, Object>>> getTopSellingItems(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate,
            @Parameter(description = "商品类型：dish/setmeal") @RequestParam(defaultValue = "dish") String type,
            @Parameter(description = "条数上限") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> items = reportEnhancedService.getTopSellingItems(startDate, endDate, type, limit,
                tenantId);
        return R.success(items);
    }

    /**
     * 获取 sales by time period。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/sales/time-period")
    @Operation(summary = "查询分时段销售分析")
    public R<Map<String, Object>> getSalesByTimePeriod(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportEnhancedService.getSalesByTimePeriod(startDate, endDate, tenantId);
        return R.success(data);
    }

    /**
     * 获取 customer analysis。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/sales/customer-analysis")
    @Operation(summary = "查询客户分析")
    public R<Map<String, Object>> getCustomerAnalysis(
                        @Parameter(description = "起始日期") @RequestParam String startDate,
            @Parameter(description = "结束日期") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> analysis = reportEnhancedService.getCustomerAnalysis(startDate, endDate, tenantId);
        return R.success(analysis);
    }

    /**
     * 获取 revenue forecast。
     * @param days 参数 days
     * @return 返回结果
     */
    @GetMapping("/sales/revenue-forecast")
    @Operation(summary = "查询营收预测")
    public R<Map<String, Object>> getRevenueForecast(
                        @Parameter(description = "预测天数") @RequestParam(defaultValue = "7") int days) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> forecast = reportEnhancedService.getRevenueForecast(days, tenantId);
        return R.success(forecast);
    }
}
