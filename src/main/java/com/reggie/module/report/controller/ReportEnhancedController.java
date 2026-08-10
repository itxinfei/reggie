package com.reggie.module.report.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.report.service.ReportEnhancedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Enhanced Report Controller
 *
 * @author reggie
 * @since 2026-08-11
 */
@Slf4j
@RestController
@RequestMapping("/report/enhanced")
@Tag(name = "Enhanced Report Management")
public class ReportEnhancedController {

    @Autowired
    private ReportEnhancedService reportEnhancedService;

    // ==================== Food Cost Report ====================

    @GetMapping("/food-cost/report")
    @Operation(summary = "Get food cost report")
    public R<Map<String, Object>> getFoodCostReport(
            @Parameter(description = "S t a r t D a t e")
            @Parameter(description = "Start date") @RequestParam String startDate,
            @Parameter(description = "End date") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getFoodCostReport(startDate, endDate, tenantId);
        return R.success(report);
    }

    @GetMapping("/food-cost/trend")
    @Operation(summary = "Get food cost trend")
    public R<Map<String, Object>> getFoodCostTrend(
            @Parameter(description = "S t a r t D a t e")
            @Parameter(description = "Start date") @RequestParam String startDate,
            @Parameter(description = "End date") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = reportEnhancedService.getFoodCostTrend(startDate, endDate, tenantId);
        return R.success(trend);
    }

    @GetMapping("/food-cost/category")
    @Operation(summary = "Get food cost by category")
    public R<List<Map<String, Object>>> getFoodCostByCategory(
            @Parameter(description = "S t a r t D a t e")
            @Parameter(description = "Start date") @RequestParam String startDate,
            @Parameter(description = "End date") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> data = reportEnhancedService.getFoodCostByCategory(startDate, endDate, tenantId);
        return R.success(data);
    }

    @GetMapping("/food-cost/ranking")
    @Operation(summary = "Get food cost ranking")
    public R<List<Map<String, Object>>> getFoodCostRanking(
            @Parameter(description = "S t a r t D a t e")
            @Parameter(description = "Start date") @RequestParam String startDate,
            @Parameter(description = "End date") @RequestParam String endDate,
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> ranking = reportEnhancedService.getFoodCostRanking(startDate, endDate, limit, tenantId);
        return R.success(ranking);
    }

    // ==================== Enhanced Sales Report ====================

    @GetMapping("/sales/weekly")
    @Operation(summary = "Get weekly sales report")
    public R<Map<String, Object>> getWeeklyReport(
            @Parameter(description = "Y e a r")
            @Parameter(description = "Year") @RequestParam int year,
            @Parameter(description = "Week number") @RequestParam int week) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getWeeklyReport(year, week, tenantId);
        return R.success(report);
    }

    @GetMapping("/sales/monthly")
    @Operation(summary = "Get monthly sales report")
    public R<Map<String, Object>> getMonthlyReport(
            @Parameter(description = "Y e a r")
            @Parameter(description = "Year") @RequestParam int year,
            @Parameter(description = "Month") @RequestParam int month) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getMonthlyReport(year, month, tenantId);
        return R.success(report);
    }

    @GetMapping("/sales/yearly")
    @Operation(summary = "Get yearly sales report")
    public R<Map<String, Object>> getYearlyReport(
            @Parameter(description = "Y e a r")
            @Parameter(description = "Year") @RequestParam int year) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportEnhancedService.getYearlyReport(year, tenantId);
        return R.success(report);
    }

    @GetMapping("/sales/comparison")
    @Operation(summary = "Get sales comparison")
    public R<Map<String, Object>> getSalesComparison(
            @Parameter(description = "P e r i o d1 S t a r t")
            @Parameter(description = "Period 1 start") @RequestParam String period1Start,
            @Parameter(description = "Period 1 end") @RequestParam String period1End,
            @Parameter(description = "Period 2 start") @RequestParam String period2Start,
            @Parameter(description = "Period 2 end") @RequestParam String period2End) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> comparison = reportEnhancedService.getSalesComparison(
                period1Start, period1End, period2Start, period2End, tenantId);
        return R.success(comparison);
    }

    @GetMapping("/sales/trend")
    @Operation(summary = "Get sales trend")
    public R<Map<String, Object>> getSalesTrend(
            @Parameter(description = "P e r i o d")
            @Parameter(description = "Period type: day, week, month") @RequestParam(defaultValue = "day") String period,
            @Parameter(description = "Start date") @RequestParam String startDate,
            @Parameter(description = "End date") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = reportEnhancedService.getSalesTrend(period, startDate, endDate, tenantId);
        return R.success(trend);
    }

    @GetMapping("/sales/top-items")
    @Operation(summary = "Get top selling items")
    public R<List<Map<String, Object>>> getTopSellingItems(
            @Parameter(description = "S t a r t D a t e")
            @Parameter(description = "Start date") @RequestParam String startDate,
            @Parameter(description = "End date") @RequestParam String endDate,
            @Parameter(description = "Item type: dish, setmeal") @RequestParam(defaultValue = "dish") String type,
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> items = reportEnhancedService.getTopSellingItems(startDate, endDate, type, limit, tenantId);
        return R.success(items);
    }

    @GetMapping("/sales/time-period")
    @Operation(summary = "Get sales by time period")
    public R<Map<String, Object>> getSalesByTimePeriod(
            @Parameter(description = "S t a r t D a t e")
            @Parameter(description = "Start date") @RequestParam String startDate,
            @Parameter(description = "End date") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportEnhancedService.getSalesByTimePeriod(startDate, endDate, tenantId);
        return R.success(data);
    }

    @GetMapping("/sales/customer-analysis")
    @Operation(summary = "Get customer analysis")
    public R<Map<String, Object>> getCustomerAnalysis(
            @Parameter(description = "S t a r t D a t e")
            @Parameter(description = "Start date") @RequestParam String startDate,
            @Parameter(description = "End date") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> analysis = reportEnhancedService.getCustomerAnalysis(startDate, endDate, tenantId);
        return R.success(analysis);
    }

    @GetMapping("/sales/revenue-forecast")
    @Operation(summary = "Get revenue forecast")
    public R<Map<String, Object>> getRevenueForecast(
            @Parameter(description = "D a y s")
            @Parameter(description = "Forecast days") @RequestParam(defaultValue = "7") int days) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> forecast = reportEnhancedService.getRevenueForecast(days, tenantId);
        return R.success(forecast);
    }
}

