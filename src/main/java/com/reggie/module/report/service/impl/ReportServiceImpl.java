package com.reggie.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.module.export.util.ExportUtil;
import com.reggie.module.report.service.ReportService;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final int SLOT_COUNT = 5;
    private static final String[] SLOT_NAMES = {"早市(6-10)", "午市(10-14)", "下午茶(14-17)", "晚市(17-21)", "夜市(21-6)"};

    /** 导出历史记录存储，应用级别共享（导出操作已做租户隔离），线程安全 */
    private final List<Map<String, Object>> exportHistory =
            Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Override
    public Map<String, Object> getDailyReport(String date, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate reportDate = LocalDate.parse(date);
        LocalDateTime start = reportDate.atStartOfDay();
        LocalDateTime end = reportDate.atTime(LocalTime.MAX);

        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime, start, end);
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
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
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
            // 修改点：不再返回空数组导致前端下载空文件，而是抛出RuntimeException
            // ReportController的GlobalExceptionHandler会捕获并返回JSON错误给前端
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
        exportHistory.add(record);
        log.info("记录导出历史: dateRange={}, format={}, fileName={}, fileSize={}bytes, status={}",
                dateRange, format, fileName, fileSize, status);
    }

    @Override
    public List<Map<String, Object>> getExportHistory() {
        synchronized (exportHistory) {
            return new ArrayList<>(exportHistory);
        }
    }

    @Override
    public void clearExportHistory() {
        synchronized (exportHistory) {
            int size = exportHistory.size();
            exportHistory.clear();
            log.info("清除导出历史记录，共清除 {} 条", size);
        }
    }
}
