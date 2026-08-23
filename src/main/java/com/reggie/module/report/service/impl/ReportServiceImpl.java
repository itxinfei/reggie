package com.reggie.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.category.model.Category;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.export.util.ExportUtil;
import com.reggie.module.report.service.ReportService;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 经营报表服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ReportServiceImpl implements ReportService {

    /** 时段数量 */
    private static final int SLOT_COUNT = 5;
    /** 时段名称 */
    private static final String[] SLOT_NAMES = {"早市(6-10)", "午市(10-14)", "下午茶(14-17)", "晚市(17-21)", "夜市(21-6)"};

    /** 导出历史记录存储，应用级别共享（导出操作已做租户隔离），线程安全 */
    private final List<Map<String, Object>> exportHistory =
            Collections.synchronizedList(new ArrayList<>());

    /** 订单服务 */
    @Autowired
    private OrderService orderService;

    /** 订单明细服务 */
    @Autowired
    private OrderDetailService orderDetailService;

    /** 菜品服务 */
    @Autowired
    private DishService dishService;

    /** 分类服务 */
    @Autowired
    private CategoryService categoryService;

    @Override
    public Map<String, Object> getDailyReport(String date, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate reportDate = LocalDate.parse(date);
        LocalDateTime start = reportDate.atStartOfDay();
        LocalDateTime end = reportDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.between(Orders::getOrderTime, start, end);
        if (tenantId != null) {
            orderQw.eq(Orders::getTenantId, tenantId);
        }
        List<Orders> orders = orderService.list(orderQw);

        int totalOrders = 0;
        int completedOrders = 0;
        int cancelledOrders = 0;
        // 营业额只统计已完成订单，避免将未支付/已取消订单计入
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Orders o : orders) {
            totalOrders++;
            if (o.getStatus() != null && o.getStatus() == Orders.STATUS_COMPLETED) {
                completedOrders++;
                totalAmount = totalAmount.add(o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO);
            }
            if (o.getStatus() != null && o.getStatus() == Orders.STATUS_CANCELLED) cancelledOrders++;
        }

        BigDecimal avgAmount = completedOrders > 0
                ? totalAmount.divide(BigDecimal.valueOf(completedOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        result.put("totalOrders", totalOrders);
        result.put("totalAmount", totalAmount);
        result.put("completedOrders", completedOrders);
        result.put("cancelledOrders", cancelledOrders);
        result.put("avgAmount", avgAmount);
        return result;
    }

    @Override
    public List<Map<String, Object>> getDishRanking(String startDate, String endDate, int limit, Long tenantId) {
        List<Map<String, Object>> ranking = new ArrayList<>();

        // 租户隔离
        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atTime(LocalTime.MAX));
            orderQw.select(Orders::getId);
            List<Orders> orders = orderService.list(orderQw);
            if (orders.isEmpty()) return ranking;

            List<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());
            LambdaQueryWrapper<OrderDetail> detailQw = new LambdaQueryWrapper<>();
            detailQw.in(OrderDetail::getOrderId, orderIds);
            List<OrderDetail> details = orderDetailService.list(detailQw);

            Map<String, Integer> dishCount = new LinkedHashMap<>();
            for (OrderDetail d : details) {
                if (d.getName() != null) {
                    dishCount.merge(d.getName(), d.getNumber() != null ? d.getNumber() : 0, Integer::sum);
                }
            }

            dishCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .forEach(e -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", e.getKey());
                        item.put("count", e.getValue());
                        ranking.add(item);
                    });
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }

        return ranking;
    }

    @Override
    public List<Map<String, Object>> getTimeSlotAnalysis(String startDate, String endDate, Long tenantId) {
        List<Map<String, Object>> slots = new ArrayList<>();

        // 租户隔离
        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
            qw.between(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atTime(LocalTime.MAX));
            List<Orders> orders = orderService.list(qw);

            int[] counts = new int[SLOT_COUNT];
            BigDecimal[] amounts = new BigDecimal[SLOT_COUNT];
            for (int i = 0; i < SLOT_COUNT; i++) amounts[i] = BigDecimal.ZERO;

            for (Orders o : orders) {
                if (o.getOrderTime() == null) continue;
                int hour = o.getOrderTime().getHour();
                int idx;
                if (hour >= 6 && hour < 10) idx = 0;
                else if (hour >= 10 && hour < 14) idx = 1;
                else if (hour >= 14 && hour < 17) idx = 2;
                else if (hour >= 17 && hour < 21) idx = 3;
                else idx = 4;
                counts[idx]++;
                amounts[idx] = amounts[idx].add(o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO);
            }

            for (int i = 0; i < SLOT_COUNT; i++) {
                Map<String, Object> slot = new HashMap<>();
                slot.put("name", SLOT_NAMES[i]);
                slot.put("count", counts[i]);
                slot.put("amount", amounts[i]);
                slots.add(slot);
            }
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
        return slots;
    }

    @Override
    public Map<String, Object> getPaymentAnalysis(String startDate, String endDate, Long tenantId) {
        // 租户隔离
        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
            qw.between(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atTime(LocalTime.MAX));
            List<Orders> orders = orderService.list(qw);

            int wechatCount = 0, alipayCount = 0, balanceCount = 0, otherCount = 0;
            BigDecimal wechatAmount = BigDecimal.ZERO, alipayAmount = BigDecimal.ZERO;

            for (Orders o : orders) {
                BigDecimal amt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
                if (o.getPayMethod() != null) {
                    switch (o.getPayMethod()) {
                        case 1: wechatCount++; wechatAmount = wechatAmount.add(amt); break;
                        case 2: alipayCount++; alipayAmount = alipayAmount.add(amt); break;
                        case 3: balanceCount++; break;
                        default: otherCount++; break;
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> wechat = new HashMap<>();
            wechat.put("count", wechatCount);
            wechat.put("amount", wechatAmount);
            result.put("wechat", wechat);
            Map<String, Object> alipay = new HashMap<>();
            alipay.put("count", alipayCount);
            alipay.put("amount", alipayAmount);
            result.put("alipay", alipay);
            Map<String, Object> balance = new HashMap<>();
            balance.put("count", balanceCount);
            result.put("balance", balance);
            Map<String, Object> other = new HashMap<>();
            other.put("count", otherCount);
            result.put("other", other);
            return result;
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
    }

    @Override
    public byte[] exportDailyReport(String startDate, String endDate, Long tenantId, String format) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        boolean isExcel = !"pdf".equalsIgnoreCase(format);

        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            // 收集日报数据
            List<Map<String, Object>> dailyRows = new ArrayList<>();
            BigDecimal totalRevenue = BigDecimal.ZERO;
            int totalOrders = 0, totalCompleted = 0, totalCancelled = 0;

            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                Map<String, Object> report = getDailyReport(date.toString(), tenantId);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date", date.toString());
                row.put("totalOrders", report.get("totalOrders") != null
                        ? Integer.parseInt(report.get("totalOrders").toString()) : 0);
                row.put("totalAmount", report.get("totalAmount") != null ? report.get("totalAmount").toString() : "0");
                row.put("completedOrders", report.get("completedOrders") != null
                        ? Integer.parseInt(report.get("completedOrders").toString()) : 0);
                row.put("cancelledOrders", report.get("cancelledOrders") != null
                        ? Integer.parseInt(report.get("cancelledOrders").toString()) : 0);
                dailyRows.add(row);

                totalOrders += (Integer) row.get("totalOrders");
                totalCompleted += (Integer) row.get("completedOrders");
                totalCancelled += (Integer) row.get("cancelledOrders");
                totalRevenue = totalRevenue.add(new BigDecimal(row.get("totalAmount").toString()));
            }

            byte[] result;
            String fileName;

            if (isExcel) {
                // 生成 Excel
                LinkedHashMap<String, String> columns = new LinkedHashMap<>();
                columns.put("date", "日期");
                columns.put("totalOrders", "订单数");
                columns.put("totalAmount", "总金额(元)");
                columns.put("completedOrders", "已完成");
                columns.put("cancelledOrders", "已取消");

                List<Map<String, Object>> excelRows = new ArrayList<>();
                for (Map<String, Object> row : dailyRows) {
                    Map<String, Object> excelRow = new LinkedHashMap<>();
                    excelRow.put("date", row.get("date"));
                    excelRow.put("totalOrders", row.get("totalOrders"));
                    excelRow.put("totalAmount", row.get("totalAmount"));
                    excelRow.put("completedOrders", row.get("completedOrders"));
                    excelRow.put("cancelledOrders", row.get("cancelledOrders"));
                    excelRows.add(excelRow);
                }

                result = ExportUtil.generateExcelBytes(columns, excelRows);
                fileName = "report_" + startDate + "_" + endDate + ".xlsx";
            } else {
                // 生成 PDF
                LinkedHashMap<String, String> columns = new LinkedHashMap<>();
                columns.put("date", "日期");
                columns.put("totalOrders", "订单数");
                columns.put("totalAmount", "总金额");
                columns.put("completedOrders", "已完成");
                columns.put("cancelledOrders", "已取消");

                Map<String, String> summary = new LinkedHashMap<>();
                summary.put("日期范围", startDate + " ~ " + endDate);
                summary.put("总订单数", String.valueOf(totalOrders));
                summary.put("总营业额", "¥" + totalRevenue.toPlainString());
                summary.put("已完成", String.valueOf(totalCompleted));
                summary.put("已取消", String.valueOf(totalCancelled));

                result = ExportUtil.generatePdfBytes(
                        "瑞吉外卖 - 营业日报表",
                        columns, dailyRows, summary
                );
                fileName = "report_" + startDate + "_" + endDate + ".pdf";
            }

            // 导出成功后记录历史
            addExportRecord(
                    startDate + " ~ " + endDate,
                    isExcel ? "excel" : "pdf",
                    fileName,
                    result.length,
                    "success"
            );

            return result;
        } catch (Exception e) {
            log.error("导出日报失败: format={}", format, e);
            addExportRecord(
                    startDate + " ~ " + endDate,
                    isExcel ? "excel" : "pdf",
                    "report_" + startDate + "_" + endDate + "." + (isExcel ? "xlsx" : "pdf"),
                    0,
                    "failed"
            );
            throw new RuntimeException("经营报表导出失败: " + e.getMessage(), e);
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
    }

    @Override
    public void addExportRecord(String dateRange, String format, String fileName, long fileSize, String status) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", UUID.randomUUID().toString().replace("-", ""));
        record.put("exportTime", LocalDateTime.now().toString().replace("T", " "));
        record.put("dateRange", dateRange);
        record.put("format", format);
        record.put("fileName", fileName);
        record.put("fileSize", fileSize);
        record.put("status", status);
        // 记录当前租户ID，查询/清除时按租户隔离，避免跨租户泄露
        record.put("tenantId", BaseContext.getCurrentTenantId());
        exportHistory.add(record);
        // 容量上限：保留最近100条记录，防止内存溢出
        synchronized (exportHistory) {
            while (exportHistory.size() > 100) {
                exportHistory.remove(0);
            }
        }
        log.info("记录导出历史: dateRange={}, format={}, fileName={}, fileSize={}bytes, status={}, tenantId={}",
                dateRange, format, fileName, fileSize, status, BaseContext.getCurrentTenantId());
    }

    @Override
    public List<Map<String, Object>> getExportHistory() {
        Long currentTenantId = BaseContext.getCurrentTenantId();
        synchronized (exportHistory) {
            // 仅返回当前租户的导出记录，fail-closed：无租户上下文时返回空列表
            if (currentTenantId == null) {
                return new ArrayList<>();
            }
            return exportHistory.stream()
                    .filter(r -> currentTenantId.equals(r.get("tenantId")))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void clearExportHistory() {
        Long currentTenantId = BaseContext.getCurrentTenantId();
        synchronized (exportHistory) {
            // 仅清除当前租户的导出记录，fail-closed：无租户上下文时不执行清除
            if (currentTenantId == null) {
                log.warn("清除导出历史记录失败：当前租户上下文为空，拒绝操作");
                return;
            }
            int before = exportHistory.size();
            exportHistory.removeIf(r -> currentTenantId.equals(r.get("tenantId")));
            int removed = before - exportHistory.size();
            log.info("清除导出历史记录，共清除 {} 条（租户 {}）", removed, currentTenantId);
        }
    }

    @Override
    public Map<String, Object> getRepurchaseRate(String period, String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Double> rates = new ArrayList<>();

        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // 查询日期范围内已完成的订单（仅 status=4，已取消订单不计入复购率）
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime, start.atStartOfDay(), end.atTime(LocalTime.MAX));
            orderQw.in(Orders::getStatus, Orders.STATUS_COMPLETED);
            orderQw.select(Orders::getId, Orders::getUserId, Orders::getOrderTime);
            List<Orders> orders = orderService.list(orderQw);

            // 按时间窗口分组：(windowKey -> (userId -> count))
            Map<String, Map<Long, Integer>> windowUserCountMap = new LinkedHashMap<>();
            Map<String, Map<Long, Long>> windowUserFirstOrderMap = new LinkedHashMap<>();

            for (Orders o : orders) {
                if (o.getUserId() == null || o.getOrderTime() == null) continue;
                String windowKey = getWindowKey(o.getOrderTime(), period);
                windowUserCountMap
                        .computeIfAbsent(windowKey, k -> new HashMap<>())
                        .merge(o.getUserId(), 1, Integer::sum);
            }

            // 生成完整的时间窗口序列
            List<String> allWindows = generateWindowSequence(start, end, period);
            int totalUsers = 0;
            int repurchaseUsers = 0;

            for (String w : allWindows) {
                dates.add(w);
                Map<Long, Integer> userCountMap = windowUserCountMap.getOrDefault(w, new HashMap<>());
                int windowTotal = userCountMap.size();
                int windowRepurchase = (int) userCountMap.values().stream().filter(c -> c >= 2).count();
                double rate = windowTotal > 0 ? (double) windowRepurchase / windowTotal * 100.0 : 0.0;
                rates.add(Math.round(rate * 100.0) / 100.0);
                totalUsers += windowTotal;
                repurchaseUsers += windowRepurchase;
            }

            double totalRate = totalUsers > 0 ? (double) repurchaseUsers / totalUsers * 100.0 : 0.0;
            result.put("dates", dates);
            result.put("rates", rates);
            result.put("totalRate", Math.round(totalRate * 100.0) / 100.0);
            result.put("totalUsers", totalUsers);
            result.put("repurchaseUsers", repurchaseUsers);

        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
        return result;
    }

    private String getWindowKey(LocalDateTime dateTime, String period) {
        switch (period) {
            case "week":
                return dateTime.getYear() + "-W" + String.format("%02d", dateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case "month":
                return dateTime.getYear() + "-" + String.format("%02d", dateTime.getMonthValue());
            case "year":
                return String.valueOf(dateTime.getYear());
            case "day":
            default:
                return dateTime.toLocalDate().toString();
        }
    }

    private List<String> generateWindowSequence(LocalDate start, LocalDate end, String period) {
        List<String> windows = new ArrayList<>();
        if (period.equals("week")) {
            // 按周生成
            LocalDate current = start;
            while (!current.isAfter(end)) {
                String key = current.getYear() + "-W" + String.format("%02d", current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                if (windows.isEmpty() || !windows.get(windows.size() - 1).equals(key)) {
                    windows.add(key);
                }
                current = current.plusWeeks(1);
            }
        } else if (period.equals("month")) {
            LocalDate current = start.withDayOfMonth(1);
            while (!current.isAfter(end)) {
                windows.add(current.getYear() + "-" + String.format("%02d", current.getMonthValue()));
                current = current.plusMonths(1);
            }
        } else if (period.equals("year")) {
            int startYear = start.getYear();
            int endYear = end.getYear();
            for (int y = startYear; y <= endYear; y++) {
                windows.add(String.valueOf(y));
            }
        } else {
            // day
            LocalDate current = start;
            while (!current.isAfter(end)) {
                windows.add(current.toString());
                current = current.plusDays(1);
            }
        }
        return windows;
    }

    @Override
    public List<Map<String, Object>> getCategorySales(String startDate, String endDate, Long tenantId) {
        List<Map<String, Object>> result = new ArrayList<>();
        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            // 1. 查询日期范围内的订单ID
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime,
                    LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atTime(LocalTime.MAX));
            orderQw.select(Orders::getId);
            List<Orders> orders = orderService.list(orderQw);
            if (orders.isEmpty()) return result;

            List<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());

            // 2. 查询订单详情，获取(dishId, number)
            LambdaQueryWrapper<OrderDetail> detailQw = new LambdaQueryWrapper<>();
            detailQw.in(OrderDetail::getOrderId, orderIds);
            detailQw.isNotNull(OrderDetail::getDishId);
            List<OrderDetail> details = orderDetailService.list(detailQw);
            if (details.isEmpty()) return result;

            // 3. 收集涉及的菜品ID
            Set<Long> dishIds = details.stream()
                    .map(OrderDetail::getDishId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            if (dishIds.isEmpty()) return result;

            // 4. 查询菜品，获取 dishId -> categoryId 映射
            LambdaQueryWrapper<Dish> dishQw = new LambdaQueryWrapper<>();
            dishQw.in(Dish::getId, dishIds);
            dishQw.select(Dish::getId, Dish::getCategoryId);
            List<Dish> dishes = dishService.list(dishQw);
            Map<Long, Long> dishCategoryMap = dishes.stream()
                    .collect(Collectors.toMap(Dish::getId, Dish::getCategoryId, (a, b) -> a));

            // 5. 查询分类，获取 categoryId -> categoryName 映射（只查菜品分类 type=1）
            Set<Long> categoryIds = new HashSet<>(dishCategoryMap.values());
            LambdaQueryWrapper<Category> catQw = new LambdaQueryWrapper<>();
            catQw.in(Category::getId, categoryIds);
            catQw.eq(Category::getType, 1);
            catQw.select(Category::getId, Category::getName);
            List<Category> categories = categoryService.list(catQw);
            Map<Long, String> categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

            // 6. 按分类名称聚合销量
            Map<String, Integer> catSalesMap = new LinkedHashMap<>();
            for (OrderDetail detail : details) {
                Long dishId = detail.getDishId();
                Long categoryId = dishCategoryMap.get(dishId);
                if (categoryId == null) continue;
                String catName = categoryNameMap.get(categoryId);
                if (catName == null) catName = "其他";
                int qty = detail.getNumber() != null ? detail.getNumber() : 0;
                catSalesMap.merge(catName, qty, Integer::sum);
            }

            // 7. 转换为结果列表，按销量降序排列
            catSalesMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", e.getKey());
                        item.put("count", e.getValue());
                        result.add(item);
                    });

        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
        return result;
    }

    @Override
    public Map<String, Object> getDishTrend(List<String> dishNames, String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Map<String, Object>> series = new ArrayList<>();

        if (dishNames == null || dishNames.isEmpty()) {
            result.put("dates", dates);
            result.put("series", series);
            return result;
        }

        // 初始化各菜品的 series 结构
        Map<String, List<Integer>> dishDataMap = new LinkedHashMap<>();
        for (String name : dishNames) {
            dishDataMap.put(name.trim(), new ArrayList<>());
        }

        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // 生成日期序列
            List<String> dateKeys = new ArrayList<>();
            LocalDate d = start;
            while (!d.isAfter(end)) {
                dateKeys.add(d.toString().substring(5)); // MM-DD 格式
                d = d.plusDays(1);
            }

            // 一次性查询整个日期范围内的订单ID
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime, start.atStartOfDay(), end.atTime(LocalTime.MAX));
            orderQw.select(Orders::getId);
            List<Orders> allOrders = orderService.list(orderQw);

            // 一次性查询整个日期范围内的订单详情
            List<Long> allOrderIds;
            if (!allOrders.isEmpty()) {
                allOrderIds = allOrders.stream().map(Orders::getId).collect(Collectors.toList());
                LambdaQueryWrapper<OrderDetail> detailQw = new LambdaQueryWrapper<>();
                detailQw.in(OrderDetail::getOrderId, allOrderIds);
                detailQw.select(OrderDetail::getName, OrderDetail::getNumber);
                List<OrderDetail> allDetails = orderDetailService.list(detailQw);

                // 构建 orderId -> Order 的日期映射，用于确定每笔订单所属日期
                Map<Long, String> orderIdToDateKey = new HashMap<>();
                for (Orders order : allOrders) {
                    if (order.getOrderTime() != null) {
                        String dayKey = order.getOrderTime().toLocalDate().toString().substring(5);
                        orderIdToDateKey.put(order.getId(), dayKey);
                    }
                }

                // 按 dateKey -> dishName 聚合销量: (dateKey, dishName) -> totalNumber
                Map<String, Map<String, Integer>> dateDishCountMap = new HashMap<>();
                for (OrderDetail detail : allDetails) {
                    if (detail.getName() == null) continue;
                    Long orderId = detail.getOrderId();
                    String dayKey = orderIdToDateKey.get(orderId);
                    if (dayKey == null) continue;
                    int qty = detail.getNumber() != null ? detail.getNumber() : 0;
                    dateDishCountMap
                            .computeIfAbsent(dayKey, k -> new HashMap<>())
                            .merge(detail.getName(), qty, Integer::sum);
                }

                // 填充各菜品每天的销量
                for (String dateKey : dateKeys) {
                    Map<String, Integer> dayCountMap = dateDishCountMap.get(dateKey);
                    if (dayCountMap == null) {
                        for (String name : dishNames) {
                            dishDataMap.get(name.trim()).add(0);
                        }
                    } else {
                        for (String name : dishNames) {
                            String key = name.trim();
                            dishDataMap.get(key).add(dayCountMap.getOrDefault(key, 0));
                        }
                    }
                }
            } else {
                // 整个范围无订单，所有日期所有菜品均为0
                for (String dateKey : dateKeys) {
                    for (String name : dishNames) {
                        dishDataMap.get(name.trim()).add(0);
                    }
                }
            }

            dates.addAll(dateKeys);

            // 构建 series 列表
            for (String name : dishNames) {
                Map<String, Object> s = new HashMap<>();
                s.put("name", name.trim());
                s.put("data", dishDataMap.get(name.trim()));
                series.add(s);
            }

        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }

        result.put("dates", dates);
        result.put("series", series);
        return result;
    }

    @Override
    public Map<String, Object> getPaymentTrend(String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Double> wechatList = new ArrayList<>();
        List<Double> alipayList = new ArrayList<>();
        List<Double> balanceList = new ArrayList<>();

        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            // 查询日期范围内的所有订单
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime,
                    LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atTime(LocalTime.MAX));
            orderQw.select(Orders::getOrderTime, Orders::getPayMethod, Orders::getAmount);
            List<Orders> allOrders = orderService.list(orderQw);

            // 按日期分组：dateStr -> {1: amount, 2: amount, 3: amount}
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // 初始化日期 -> (payMethod -> totalAmount) 映射
            Map<String, Map<Integer, BigDecimal>> dailyMap = new LinkedHashMap<>();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                String dateKey = d.toString().substring(5);
                Map<Integer, BigDecimal> payMap = new HashMap<>();
                payMap.put(1, BigDecimal.ZERO);
                payMap.put(2, BigDecimal.ZERO);
                payMap.put(3, BigDecimal.ZERO);
                dailyMap.put(dateKey, payMap);
            }

            // 累加金额
            for (Orders o : allOrders) {
                if (o.getOrderTime() == null) continue;
                String dateKey = o.getOrderTime().toLocalDate().toString().substring(5);
                Map<Integer, BigDecimal> payMap = dailyMap.get(dateKey);
                if (payMap == null) continue;
                int payMethod = o.getPayMethod() != null ? o.getPayMethod() : 0;
                BigDecimal amt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
                if (payMethod >= 1 && payMethod <= 3) {
                    payMap.put(payMethod, payMap.get(payMethod).add(amt));
                }
            }

            // 构建返回数据
            for (Map.Entry<String, Map<Integer, BigDecimal>> entry : dailyMap.entrySet()) {
                dates.add(entry.getKey());
                Map<Integer, BigDecimal> payMap = entry.getValue();
                wechatList.add(payMap.get(1).setScale(2, RoundingMode.HALF_UP).doubleValue());
                alipayList.add(payMap.get(2).setScale(2, RoundingMode.HALF_UP).doubleValue());
                balanceList.add(payMap.get(3).setScale(2, RoundingMode.HALF_UP).doubleValue());
            }

        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }

        result.put("dates", dates);
        result.put("wechat", wechatList);
        result.put("alipay", alipayList);
        result.put("balance", balanceList);
        return result;
    }

    @Override
    public Map<String, Object> getTimeSlotHeatmap(String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> heatData = new ArrayList<>();

        // 初始化 7天 × 5时段 的计数矩阵
        int[][] counts = new int[7][SLOT_COUNT];
        int maxVal = 0;

        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            // 查询日期范围内的所有订单
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime,
                    LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atTime(LocalTime.MAX));
            orderQw.select(Orders::getOrderTime);
            List<Orders> orders = orderService.list(orderQw);

            for (Orders o : orders) {
                if (o.getOrderTime() == null) continue;
                // dayIdx: 周一=0 ... 周日=6
                java.time.DayOfWeek dow = o.getOrderTime().getDayOfWeek();
                int dayIdx = dow.getValue() - 1; // DayOfWeek: MON=1...SUN=7 -> 0...6

                // slotIdx: 按小时划分到5个时段
                int hour = o.getOrderTime().getHour();
                int slotIdx;
                if (hour >= 6 && hour < 10) slotIdx = 0;
                else if (hour >= 10 && hour < 14) slotIdx = 1;
                else if (hour >= 14 && hour < 17) slotIdx = 2;
                else if (hour >= 17 && hour < 21) slotIdx = 3;
                else slotIdx = 4;

                counts[dayIdx][slotIdx]++;
            }

            // 构建结果
            for (int dayIdx = 0; dayIdx < 7; dayIdx++) {
                for (int slotIdx = 0; slotIdx < SLOT_COUNT; slotIdx++) {
                    int value = counts[dayIdx][slotIdx];
                    if (value > maxVal) maxVal = value;
                    Map<String, Object> cell = new HashMap<>();
                    cell.put("dayIdx", dayIdx);
                    cell.put("slotIdx", slotIdx);
                    cell.put("value", value);
                    heatData.add(cell);
                }
            }

        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }

        result.put("data", heatData);
        result.put("maxVal", maxVal);
        return result;
    }

    @Override
    public Map<String, Object> getRepurchaseRateByDish(String startDate, String endDate, int limit, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> ranking = new ArrayList<>();

        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            // 1. 查询日期范围内已完成的订单
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atTime(LocalTime.MAX));
            orderQw.in(Orders::getStatus, Orders.STATUS_COMPLETED);
            orderQw.select(Orders::getId);
            List<Orders> orders = orderService.list(orderQw);
            if (orders.isEmpty()) {
                result.put("ranking", ranking);
                result.put("totalDishes", 0);
                return result;
            }

            List<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());

            // 2. 查询订单详情，关联菜品
            LambdaQueryWrapper<OrderDetail> detailQw = new LambdaQueryWrapper<>();
            detailQw.in(OrderDetail::getOrderId, orderIds);
            detailQw.isNotNull(OrderDetail::getDishId);
            detailQw.select(OrderDetail::getDishId, OrderDetail::getOrderId);
            List<OrderDetail> details = orderDetailService.list(detailQw);
            if (details.isEmpty()) {
                result.put("ranking", ranking);
                result.put("totalDishes", 0);
                return result;
            }

            // 3. 按 (dishId, userId) 分组统计购买次数
            // 先通过 orderId 反查 userId
            Map<Long, Long> orderIdUserIdMap = orders.stream()
                    .collect(Collectors.toMap(Orders::getId, Orders::getUserId, (a, b) -> a));

            Map<Long, Map<Long, Integer>> dishUserCountMap = new HashMap<>();
            for (OrderDetail d : details) {
                Long userId = orderIdUserIdMap.get(d.getOrderId());
                if (userId == null) continue;
                dishUserCountMap
                        .computeIfAbsent(d.getDishId(), k -> new HashMap<>())
                        .merge(userId, 1, Integer::sum);
            }

            // 4. 收集涉及的菜品ID并查询名称
            Set<Long> dishIds = dishUserCountMap.keySet();
            LambdaQueryWrapper<Dish> dishQw = new LambdaQueryWrapper<>();
            dishQw.in(Dish::getId, dishIds);
            dishQw.select(Dish::getId, Dish::getName);
            List<Dish> dishes = dishService.list(dishQw);
            Map<Long, String> dishNameMap = dishes.stream()
                    .collect(Collectors.toMap(Dish::getId, Dish::getName, (a, b) -> a));

            // 5. 计算每个菜品的复购率
            for (Map.Entry<Long, Map<Long, Integer>> entry : dishUserCountMap.entrySet()) {
                Long dishId = entry.getKey();
                Map<Long, Integer> userCountMap = entry.getValue();
                int totalUsers = userCountMap.size();
                long repurchaseUsers = userCountMap.values().stream().filter(c -> c >= 2).count();
                double rate = totalUsers > 0 ? (double) repurchaseUsers / totalUsers * 100.0 : 0.0;

                Map<String, Object> item = new HashMap<>();
                item.put("dishId", dishId);
                item.put("dishName", dishNameMap.getOrDefault(dishId, "未知菜品"));
                item.put("totalUsers", totalUsers);
                item.put("repurchaseUsers", (int) repurchaseUsers);
                item.put("rate", Math.round(rate * 100.0) / 100.0);
                ranking.add(item);
            }

            // 6. 按复购率降序排列
            ranking.sort((a, b) -> Double.compare((Double) b.get("rate"), (Double) a.get("rate")));
            if (ranking.size() > limit) {
                ranking = ranking.subList(0, limit);
            }

            result.put("ranking", ranking);
            result.put("totalDishes", ranking.size());

        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
        return result;
    }

    @Override
    public Map<String, Object> getCohortAnalysis(String startDate, String endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> cohorts = new ArrayList<>();

        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            // 1. 查询日期范围内已完成订单的用户消费记录
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atTime(LocalTime.MAX));
            orderQw.in(Orders::getStatus, Orders.STATUS_COMPLETED);
            orderQw.select(Orders::getUserId, Orders::getOrderTime);
            List<Orders> orders = orderService.list(orderQw);
            if (orders.isEmpty()) {
                result.put("cohorts", cohorts);
                result.put("totalCohorts", 0);
                return result;
            }

            // 2. 按用户分组，找到每个用户的首次消费日期
            Map<Long, LocalDateTime> userFirstOrderMap = new HashMap<>();
            for (Orders o : orders) {
                if (o.getUserId() == null || o.getOrderTime() == null) continue;
                userFirstOrderMap.merge(o.getUserId(), o.getOrderTime(), (old, newVal) -> old.isBefore(newVal) ? old : newVal);
            }

            // 3. 按首次消费月份分组
            Map<String, Set<Long>> cohortUserMap = new LinkedHashMap<>();
            for (Map.Entry<Long, LocalDateTime> entry : userFirstOrderMap.entrySet()) {
                String cohortKey = entry.getValue().getYear() + "-" + String.format("%02d", entry.getValue().getMonthValue());
                cohortUserMap.computeIfAbsent(cohortKey, k -> new HashSet<>()).add(entry.getKey());
            }

            // 4. 构建 userId -> 订单列表的映射，用于计算复购
            Map<Long, List<Orders>> userOrdersMap = new HashMap<>();
            for (Orders o : orders) {
                if (o.getUserId() == null) continue;
                userOrdersMap.computeIfAbsent(o.getUserId(), k -> new ArrayList<>()).add(o);
            }

            // 5. 计算每个 cohort 的复购率
            for (Map.Entry<String, Set<Long>> entry : cohortUserMap.entrySet()) {
                String cohortDate = entry.getKey();
                Set<Long> users = entry.getValue();
                int total = users.size();
                long repurchase = users.stream()
                        .filter(u -> userOrdersMap.getOrDefault(u, Collections.emptyList()).size() >= 2)
                        .count();
                double rate = total > 0 ? (double) repurchase / total * 100.0 : 0.0;

                Map<String, Object> cohort = new HashMap<>();
                cohort.put("cohortDate", cohortDate);
                cohort.put("users", total);
                cohort.put("repurchaseUsers", (int) repurchase);
                cohort.put("repurchaseRate", Math.round(rate * 100.0) / 100.0);
                cohorts.add(cohort);
            }

            result.put("cohorts", cohorts);
            result.put("totalCohorts", cohorts.size());

        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
        return result;
    }
}






