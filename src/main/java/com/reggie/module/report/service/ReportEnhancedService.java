package com.reggie.module.report.service;

import java.util.List;
import java.util.Map;

/**
 * Enhanced Report Service Interface
 *
 * @author reggie
 * @since 2026-08-11
 */
public interface ReportEnhancedService {

    // ==================== Food Cost Report ====================

    /**
     * Get food cost report
     *
     * @param startDate Start date (yyyy-MM-dd)
     * @param endDate   End date (yyyy-MM-dd)
     * @param tenantId  Tenant ID
     * @return Food cost report data
     */
    Map<String, Object> getFoodCostReport(String startDate, String endDate, Long tenantId);

    /**
     * Get food cost trend
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Food cost trend data
     */
    Map<String, Object> getFoodCostTrend(String startDate, String endDate, Long tenantId);

    /**
     * Get food cost by category
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Food cost by category
     */
    List<Map<String, Object>> getFoodCostByCategory(String startDate, String endDate, Long tenantId);

    /**
     * Get food cost ranking
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param limit     Ranking limit
     * @param tenantId  Tenant ID
     * @return Food cost ranking
     */
    List<Map<String, Object>> getFoodCostRanking(String startDate, String endDate, int limit, Long tenantId);

    // ==================== Enhanced Sales Report ====================

    /**
     * Get weekly sales report
     *
     * @param year     Year
     * @param week     Week number
     * @param tenantId Tenant ID
     * @return Weekly sales report
     */
    Map<String, Object> getWeeklyReport(int year, int week, Long tenantId);

    /**
     * Get monthly sales report
     *
     * @param year     Year
     * @param month    Month
     * @param tenantId Tenant ID
     * @return Monthly sales report
     */
    Map<String, Object> getMonthlyReport(int year, int month, Long tenantId);

    /**
     * Get yearly sales report
     *
     * @param year     Year
     * @param tenantId Tenant ID
     * @return Yearly sales report
     */
    Map<String, Object> getYearlyReport(int year, Long tenantId);

    /**
     * Get sales comparison
     *
     * @param period1Start Period 1 start date
     * @param period1End   Period 1 end date
     * @param period2Start Period 2 start date
     * @param period2End   Period 2 end date
     * @param tenantId     Tenant ID
     * @return Sales comparison data
     */
    Map<String, Object> getSalesComparison(String period1Start, String period1End,
                                           String period2Start, String period2End, Long tenantId);

    /**
     * Get sales trend by period
     *
     * @param period    Period type: day, week, month
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Sales trend data
     */
    Map<String, Object> getSalesTrend(String period, String startDate, String endDate, Long tenantId);

    /**
     * Get top selling items
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param type      Item type: dish, setmeal
     * @param limit     Limit
     * @param tenantId  Tenant ID
     * @return Top selling items
     */
    List<Map<String, Object>> getTopSellingItems(String startDate, String endDate, String type, int limit, Long tenantId);

    /**
     * Get sales by time period
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Sales by time period
     */
    Map<String, Object> getSalesByTimePeriod(String startDate, String endDate, Long tenantId);

    /**
     * Get customer analysis
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Customer analysis data
     */
    Map<String, Object> getCustomerAnalysis(String startDate, String endDate, Long tenantId);

    /**
     * Get revenue forecast
     *
     * @param days     Forecast days
     * @param tenantId Tenant ID
     * @return Revenue forecast
     */
    Map<String, Object> getRevenueForecast(int days, Long tenantId);
}
