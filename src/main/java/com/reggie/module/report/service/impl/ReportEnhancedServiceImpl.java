package com.reggie.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.cost.service.CostService;
import com.reggie.module.finance.service.FinanceService;
import com.reggie.module.report.service.ReportEnhancedService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.dish.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.util.*;
import com.reggie.module.category.model.Category;
import java.util.stream.Collectors;

/**
 * Enhanced Report Service Implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Slf4j
/**
 * ReportEnhanced service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ReportEnhancedServiceImpl implements ReportEnhancedService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private CostService costService;
    // ==================== Food Cost Report ====================

    @Override
    public Map<String, Object> getFoodCostReport(String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        // Get cost summary from cost service
        Map<String, Object> costSummary = costService.getCostSummary(start, end, tenantId);
        BigDecimal totalMaterialCost = (BigDecimal) costSummary.getOrDefault("materialCost", BigDecimal.ZERO);

        // Get revenue for the period
        BigDecimal totalRevenue = getRevenueForPeriod(start, end, tenantId);

        // Calculate food cost rate
        BigDecimal foodCostRate = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            foodCostRate = totalMaterialCost.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        result.put("totalMaterialCost", totalMaterialCost);
        result.put("totalRevenue", totalRevenue);
        result.put("foodCostRate", foodCostRate);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        return result;
    }

    @Override
    public Map<String, Object> getFoodCostTrend(String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<BigDecimal> rates = new ArrayList<>();

        LocalDate current = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        while (!current.isAfter(end)) {
            dates.add(current.toString());

            Map<String, Object> costSummary = costService.getCostSummary(current, current, tenantId);
            BigDecimal dayCost = (BigDecimal) costSummary.getOrDefault("materialCost", BigDecimal.ZERO);
            BigDecimal dayRevenue = getRevenueForPeriod(current, current, tenantId);

            costs.add(dayCost);
            revenues.add(dayRevenue);

            BigDecimal dayRate = dayRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                    dayCost.divide(dayRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                    BigDecimal.ZERO;
            rates.add(dayRate);

            current = current.plusDays(1);
        }

        result.put("dates", dates);
        result.put("costs", costs);
        result.put("revenues", revenues);
        result.put("rates", rates);

        return result;
    }

    @Override
    public List<Map<String, Object>> getFoodCostByCategory(String startDate, String endDate, Long tenantId) {
        List<Map<String, Object>> result = new ArrayList<>();

        // This is a simplified implementation
        // In real scenario, you would join with dish and category tables
        Map<String, Object> costSummary = costService.getCostSummary(
                LocalDate.parse(startDate), LocalDate.parse(endDate), tenantId);

        Map<String, Object> item = new HashMap<>();
        item.put("categoryName", "All Categories");
        item.put("materialCost", costSummary.getOrDefault("materialCost", BigDecimal.ZERO));
        item.put("laborCost", costSummary.getOrDefault("laborCost", BigDecimal.ZERO));
        item.put("otherCost", costSummary.getOrDefault("otherCost", BigDecimal.ZERO));
        item.put("totalCost", costSummary.getOrDefault("totalCost", BigDecimal.ZERO));
        result.add(item);

        return result;
    }

    @Override
    public List<Map<String, Object>> getFoodCostRanking(String startDate, String endDate, int limit, Long tenantId) {
        // Use dish cost ranking from cost service
        return costService.getDishCostRanking(limit, tenantId);
    }

    // ==================== Enhanced Sales Report ====================

    @Override
    public Map<String, Object> getWeeklyReport(int year, int week, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // Calculate week start and end dates
        LocalDate weekStart = LocalDate.ofYearDay(year, 1)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        // Get daily data for the week
        List<Map<String, Object>> dailyData = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalOrders = 0;

        LocalDate current = weekStart;
        while (!current.isAfter(weekEnd)) {
            Map<String, Object> dayData = getDaySummary(current, tenantId);
            dailyData.add(dayData);

            totalRevenue = totalRevenue.add((BigDecimal) dayData.getOrDefault("revenue", BigDecimal.ZERO));
            totalOrders += (int) dayData.getOrDefault("orderCount", 0);

            current = current.plusDays(1);
        }

        result.put("year", year);
        result.put("week", week);
        result.put("weekStart", weekStart.toString());
        result.put("weekEnd", weekEnd.toString());
        result.put("dailyData", dailyData);
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", totalOrders);
        result.put("avgDailyRevenue", dailyData.size() > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(dailyData.size()), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);

        return result;
    }

    @Override
    public Map<String, Object> getMonthlyReport(int year, int month, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        // Get weekly data for the month
        List<Map<String, Object>> weeklyData = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalOrders = 0;

        LocalDate current = monthStart;
        while (!current.isAfter(monthEnd)) {
            LocalDate weekEnd = current.plusDays(6);
            if (weekEnd.isAfter(monthEnd)) {
                weekEnd = monthEnd;
            }

            Map<String, Object> weekData = getPeriodSummary(current, weekEnd, tenantId);
            weeklyData.add(weekData);

            totalRevenue = totalRevenue.add((BigDecimal) weekData.getOrDefault("revenue", BigDecimal.ZERO));
            totalOrders += (int) weekData.getOrDefault("orderCount", 0);

            current = weekEnd.plusDays(1);
        }

        result.put("year", year);
        result.put("month", month);
        result.put("monthStart", monthStart.toString());
        result.put("monthEnd", monthEnd.toString());
        result.put("weeklyData", weeklyData);
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", totalOrders);
        result.put("avgWeeklyRevenue", weeklyData.size() > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(weeklyData.size()), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);

        return result;
    }

    @Override
    public Map<String, Object> getYearlyReport(int year, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        // Get monthly data for the year
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalOrders = 0;

        for (int month = 1; month <= 12; month++) {
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

            Map<String, Object> monthData = getPeriodSummary(monthStart, monthEnd, tenantId);
            monthData.put("month", month);
            monthlyData.add(monthData);

            totalRevenue = totalRevenue.add((BigDecimal) monthData.getOrDefault("revenue", BigDecimal.ZERO));
            totalOrders += (int) monthData.getOrDefault("orderCount", 0);
        }

        result.put("year", year);
        result.put("yearStart", yearStart.toString());
        result.put("yearEnd", yearEnd.toString());
        result.put("monthlyData", monthlyData);
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", totalOrders);
        result.put("avgMonthlyRevenue", totalRevenue.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));

        return result;
    }

    @Override
    public Map<String, Object> getSalesComparison(String period1Start, String period1End,
                                                   String period2Start, String period2End, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> period1 = getPeriodSummary(
                LocalDate.parse(period1Start), LocalDate.parse(period1End), tenantId);
        Map<String, Object> period2 = getPeriodSummary(
                LocalDate.parse(period2Start), LocalDate.parse(period2End), tenantId);

        BigDecimal revenue1 = (BigDecimal) period1.getOrDefault("revenue", BigDecimal.ZERO);
        BigDecimal revenue2 = (BigDecimal) period2.getOrDefault("revenue", BigDecimal.ZERO);
        int orders1 = (int) period1.getOrDefault("orderCount", 0);
        int orders2 = (int) period2.getOrDefault("orderCount", 0);

        // Calculate growth rates
        BigDecimal revenueGrowth = BigDecimal.ZERO;
        if (revenue1.compareTo(BigDecimal.ZERO) > 0) {
            revenueGrowth = revenue2.subtract(revenue1)
                    .divide(revenue1, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        BigDecimal orderGrowth = BigDecimal.ZERO;
        if (orders1 > 0) {
            orderGrowth = new BigDecimal(orders2 - orders1)
                    .divide(new BigDecimal(orders1), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        result.put("period1", period1);
        result.put("period2", period2);
        result.put("revenueGrowth", revenueGrowth);
        result.put("orderGrowth", orderGrowth);

        return result;
    }

    @Override
    public Map<String, Object> getSalesTrend(String period, String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        if ("day".equals(period)) {
            LocalDate current = start;
            while (!current.isAfter(end)) {
                labels.add(current.toString());
                Map<String, Object> dayData = getDaySummary(current, tenantId);
                revenues.add((BigDecimal) dayData.getOrDefault("revenue", BigDecimal.ZERO));
                orders.add((int) dayData.getOrDefault("orderCount", 0));
                current = current.plusDays(1);
            }
        } else if ("week".equals(period)) {
            LocalDate current = start;
            while (!current.isAfter(end)) {
                LocalDate weekEnd = current.plusDays(6);
                if (weekEnd.isAfter(end)) {
                    weekEnd = end;
                }
                labels.add(current.toString() + " ~ " + weekEnd.toString());
                Map<String, Object> weekData = getPeriodSummary(current, weekEnd, tenantId);
                revenues.add((BigDecimal) weekData.getOrDefault("revenue", BigDecimal.ZERO));
                orders.add((int) weekData.getOrDefault("orderCount", 0));
                current = weekEnd.plusDays(1);
            }
        } else if ("month".equals(period)) {
            LocalDate current = start.withDayOfMonth(1);
            while (!current.isAfter(end)) {
                LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());
                labels.add(current.getYear() + "-" + current.getMonthValue());
                Map<String, Object> monthData = getPeriodSummary(current, monthEnd, tenantId);
                revenues.add((BigDecimal) monthData.getOrDefault("revenue", BigDecimal.ZERO));
                orders.add((int) monthData.getOrDefault("orderCount", 0));
                current = monthEnd.plusDays(1);
            }
        }

        result.put("labels", labels);
        result.put("revenues", revenues);
        result.put("orders", orders);

        return result;
    }

    @Override
    public List<Map<String, Object>> getTopSellingItems(String startDate, String endDate, String type, int limit, Long tenantId) {
        List<Map<String, Object>> result = new ArrayList<>();

        // Query order details
        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.ge(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay());
        orderQw.le(Orders::getOrderTime, LocalDate.parse(endDate).atTime(LocalTime.MAX));
        orderQw.eq(Orders::getStatus, Orders.STATUS_COMPLETED);
        if (tenantId != null) {
            orderQw.eq(Orders::getTenantId, tenantId);
        }
        orderQw.select(Orders::getId);
        List<Orders> orders = orderService.list(orderQw);

        if (orders.isEmpty()) {
            return result;
        }

        List<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());

        LambdaQueryWrapper<OrderDetail> detailQw = new LambdaQueryWrapper<>();
        detailQw.in(OrderDetail::getOrderId, orderIds);
        if ("dish".equals(type)) {
            detailQw.isNotNull(OrderDetail::getDishId);
        } else if ("setmeal".equals(type)) {
            detailQw.isNotNull(OrderDetail::getSetmealId);
        }
        List<OrderDetail> details = orderDetailService.list(detailQw);

        // Aggregate by item
        Map<Long, Map<String, Object>> itemMap = new HashMap<>();
        for (OrderDetail detail : details) {
            Long itemId = "dish".equals(type) ? detail.getDishId() : detail.getSetmealId();
            if (itemId == null) continue;

            itemMap.computeIfAbsent(itemId, k -> {
                Map<String, Object> item = new HashMap<>();
                item.put("itemId", k);
                item.put("name", detail.getName());
                item.put("quantity", 0);
                item.put("revenue", BigDecimal.ZERO);
                return item;
            });

            Map<String, Object> item = itemMap.get(itemId);
            item.put("quantity", (int) item.get("quantity") + detail.getNumber());
            item.put("revenue", ((BigDecimal) item.get("revenue")).add(
                    detail.getAmount().multiply(new BigDecimal(detail.getNumber()))));
        }

        // Sort by quantity and limit
        result = itemMap.values().stream()
                .sorted((a, b) -> Integer.compare((int) b.get("quantity"), (int) a.get("quantity")))
                .limit(limit)
                .collect(Collectors.toList());

        return result;
    }

    @Override
    public Map<String, Object> getSalesByTimePeriod(String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        String[] timePeriods = {
                "Morning (6-10)", "Lunch (10-14)", "Afternoon (14-17)",
                "Dinner (17-21)", "Night (21-24)"
        };
        int[][] timeRanges = {
                {6, 10}, {10, 14}, {14, 17}, {17, 21}, {21, 24}
        };

        List<Map<String, Object>> periodData = new ArrayList<>();

        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.ge(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay());
        orderQw.le(Orders::getOrderTime, LocalDate.parse(endDate).atTime(LocalTime.MAX));
        orderQw.eq(Orders::getStatus, Orders.STATUS_COMPLETED);
        if (tenantId != null) {
            orderQw.eq(Orders::getTenantId, tenantId);
        }
        List<Orders> orders = orderService.list(orderQw);

        for (int i = 0; i < timePeriods.length; i++) {
            int startHour = timeRanges[i][0];
            int endHour = timeRanges[i][1];

            BigDecimal periodRevenue = BigDecimal.ZERO;
            int periodOrders = 0;

            for (Orders order : orders) {
                if (order.getOrderTime() != null) {
                    int hour = order.getOrderTime().getHour();
                    if (hour >= startHour && hour < endHour) {
                        periodRevenue = periodRevenue.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
                        periodOrders++;
                    }
                }
            }

            Map<String, Object> period = new HashMap<>();
            period.put("period", timePeriods[i]);
            period.put("revenue", periodRevenue);
            period.put("orderCount", periodOrders);
            periodData.add(period);
        }

        result.put("periodData", periodData);

        return result;
    }

    @Override
    public Map<String, Object> getCustomerAnalysis(String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.ge(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay());
        orderQw.le(Orders::getOrderTime, LocalDate.parse(endDate).atTime(LocalTime.MAX));
        orderQw.eq(Orders::getStatus, Orders.STATUS_COMPLETED);
        if (tenantId != null) {
            orderQw.eq(Orders::getTenantId, tenantId);
        }
        orderQw.select(Orders::getUserId, Orders::getAmount);
        List<Orders> orders = orderService.list(orderQw);

        // Analyze customers
        Map<Long, Integer> customerOrderCount = new HashMap<>();
        Map<Long, BigDecimal> customerTotalSpent = new HashMap<>();

        for (Orders order : orders) {
            if (order.getUserId() != null) {
                customerOrderCount.merge(order.getUserId(), 1, Integer::sum);
                customerTotalSpent.merge(order.getUserId(),
                        order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO,
                        BigDecimal::add);
            }
        }

        int totalCustomers = customerOrderCount.size();
        long newCustomers = customerOrderCount.values().stream().filter(count -> count == 1).count();
        long returningCustomers = totalCustomers - newCustomers;

        BigDecimal avgOrdersPerCustomer = totalCustomers > 0 ?
                new BigDecimal(orders.size()).divide(new BigDecimal(totalCustomers), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        BigDecimal avgSpentPerCustomer = BigDecimal.ZERO;
        if (totalCustomers > 0) {
            BigDecimal totalSpent = customerTotalSpent.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgSpentPerCustomer = totalSpent.divide(new BigDecimal(totalCustomers), 2, RoundingMode.HALF_UP);
        }

        result.put("totalCustomers", totalCustomers);
        result.put("newCustomers", newCustomers);
        result.put("returningCustomers", returningCustomers);
        result.put("returningRate", totalCustomers > 0 ?
                new BigDecimal(returningCustomers).divide(new BigDecimal(totalCustomers), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")) : BigDecimal.ZERO);
        result.put("avgOrdersPerCustomer", avgOrdersPerCustomer);
        result.put("avgSpentPerCustomer", avgSpentPerCustomer);

        return result;
    }

    @Override
    public Map<String, Object> getRevenueForecast(int days, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // Get recent 30 days data for forecast
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(29);

        List<BigDecimal> recentRevenues = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            BigDecimal dayRevenue = getRevenueForPeriod(current, current, tenantId);
            recentRevenues.add(dayRevenue);
            current = current.plusDays(1);
        }

        // Simple moving average forecast
        BigDecimal avgRevenue = BigDecimal.ZERO;
        if (!recentRevenues.isEmpty()) {
            BigDecimal sum = recentRevenues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avgRevenue = sum.divide(new BigDecimal(recentRevenues.size()), 2, RoundingMode.HALF_UP);
        }

        // Generate forecast
        List<Map<String, Object>> forecast = new ArrayList<>();
        LocalDate forecastStart = LocalDate.now();
        BigDecimal totalForecast = BigDecimal.ZERO;

        for (int i = 0; i < days; i++) {
            LocalDate forecastDate = forecastStart.plusDays(i);
            Map<String, Object> dayForecast = new HashMap<>();
            dayForecast.put("date", forecastDate.toString());
            dayForecast.put("forecastRevenue", avgRevenue);
            forecast.add(dayForecast);
            totalForecast = totalForecast.add(avgRevenue);
        }

        result.put("forecastDays", days);
        result.put("avgDailyRevenue", avgRevenue);
        result.put("totalForecast", totalForecast);
        result.put("forecast", forecast);

        return result;
    }

    // ==================== Private Helper Methods ====================

    private BigDecimal getRevenueForPeriod(LocalDate start, LocalDate end, Long tenantId) {
        LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
        qw.ge(Orders::getOrderTime, start.atStartOfDay());
        qw.le(Orders::getOrderTime, end.atTime(LocalTime.MAX));
        qw.eq(Orders::getStatus, Orders.STATUS_COMPLETED);
        if (tenantId != null) {
            qw.eq(Orders::getTenantId, tenantId);
        }
        qw.select(Orders::getAmount);
        List<Orders> orders = orderService.list(qw);

        return orders.stream()
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> getDaySummary(LocalDate date, Long tenantId) {
        Map<String, Object> summary = new HashMap<>();

        LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
        qw.ge(Orders::getOrderTime, date.atStartOfDay());
        qw.le(Orders::getOrderTime, date.atTime(LocalTime.MAX));
        if (tenantId != null) {
            qw.eq(Orders::getTenantId, tenantId);
        }
        List<Orders> orders = orderService.list(qw);

        BigDecimal revenue = BigDecimal.ZERO;
        int orderCount = 0;
        int completedCount = 0;
        int cancelledCount = 0;

        for (Orders order : orders) {
            orderCount++;
            if (order.getStatus() == Orders.STATUS_COMPLETED) {
                completedCount++;
                revenue = revenue.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
            } else if (order.getStatus() == Orders.STATUS_CANCELLED) {
                cancelledCount++;
            }
        }

        summary.put("date", date.toString());
        summary.put("revenue", revenue);
        summary.put("orderCount", orderCount);
        summary.put("completedCount", completedCount);
        summary.put("cancelledCount", cancelledCount);
        summary.put("avgOrderValue", completedCount > 0 ?
                revenue.divide(new BigDecimal(completedCount), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);

        return summary;
    }

    private Map<String, Object> getPeriodSummary(LocalDate start, LocalDate end, Long tenantId) {
        Map<String, Object> summary = new HashMap<>();

        LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
        qw.ge(Orders::getOrderTime, start.atStartOfDay());
        qw.le(Orders::getOrderTime, end.atTime(LocalTime.MAX));
        if (tenantId != null) {
            qw.eq(Orders::getTenantId, tenantId);
        }
        List<Orders> orders = orderService.list(qw);

        BigDecimal revenue = BigDecimal.ZERO;
        int orderCount = 0;
        int completedCount = 0;

        for (Orders order : orders) {
            orderCount++;
            if (order.getStatus() == Orders.STATUS_COMPLETED) {
                completedCount++;
                revenue = revenue.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
            }
        }

        summary.put("startDate", start.toString());
        summary.put("endDate", end.toString());
        summary.put("revenue", revenue);
        summary.put("orderCount", orderCount);
        summary.put("completedCount", completedCount);

        return summary;
    }
}






