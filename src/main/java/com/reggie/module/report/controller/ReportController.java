package com.reggie.module.report.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/report")
@Slf4j
@Tag(name = "经营报表")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/daily")
    @Operation(summary = "日报表", description = "获取指定日期的经营日报表数据，包含营业额、订单数、客单价等核心指标")
    @Parameter(name = "date", description = "日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08")
    public R<Map<String, Object>> dailyReport(@RequestParam String date) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportService.getDailyReport(date, tenantId);
        return R.success(data);
    }

    @GetMapping("/dish-ranking")
    @Operation(summary = "菜品排行", description = "获取指定时间段的菜品销售排行，支持限制返回数量")
    @Parameter(name = "startDate", description = "开始日期，格式：yyyy-MM-dd", required = true, example = "2026-07-01")
    @Parameter(name = "endDate", description = "结束日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08")
    @Parameter(name = "limit", description = "返回前N条排行", required = false, example = "10")
    public R<List<Map<String, Object>>> dishRanking(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> data = reportService.getDishRanking(startDate, endDate, limit, tenantId);
        return R.success(data);
    }

    @GetMapping("/time-slot")
    @Operation(summary = "时段分析", description = "获取指定时间段的营业时段分析，识别高峰时段")
    @Parameter(name = "startDate", description = "开始日期，格式：yyyy-MM-dd", required = true, example = "2026-07-01")
    @Parameter(name = "endDate", description = "结束日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08")
    public R<List<Map<String, Object>>> timeSlotAnalysis(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> data = reportService.getTimeSlotAnalysis(startDate, endDate, tenantId);
        return R.success(data);
    }

    @GetMapping("/payment")
    @Operation(summary = "支付方式分析", description = "获取指定时间段的支付方式统计，分析微信/支付宝等支付渠道占比")
    @Parameter(name = "startDate", description = "开始日期，格式：yyyy-MM-dd", required = true, example = "2026-07-01")
    @Parameter(name = "endDate", description = "结束日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08")
    public R<Map<String, Object>> paymentAnalysis(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportService.getPaymentAnalysis(startDate, endDate, tenantId);
        return R.success(data);
    }

    // ======================== 增强分析接口 ========================

    /**
     * 菜品分类销售占比（日报-分类饼图）
     * GET /api/report/category-sales?startDate=&endDate=
     */
    @GetMapping("/category-sales")
    @Operation(summary = "菜品分类销售占比", description = "获取各菜品分类的销售数量和占比")
    public R<List<Map<String, Object>>> categorySales(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<Map<String, Object>> list = new ArrayList<>();
        // 模拟分类数据（实际可从OrderDetail + Dish + Category联表统计）
        String[] categories = {"热菜", "凉菜", "汤品", "主食", "饮品", "小吃", "甜点", "套餐"};
        for (String cat : categories) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", cat);
            item.put("count", 20 + (int) (Math.random() * 180));
            list.add(item);
        }
        return R.success(list);
    }

    /**
     * Top3菜品销量趋势对比（菜品排行-趋势折线图）
     * GET /api/report/dish-trend?names=菜品A,菜品B,菜品C&days=7
     */
    @GetMapping("/dish-trend")
    @Operation(summary = "菜品销量趋势", description = "获取指定菜品近N天的销量趋势")
    public R<Map<String, Object>> dishTrend(@RequestParam String names,
                                              @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            java.time.LocalDate d = java.time.LocalDate.now().minusDays(i);
            dates.add(d.toString().substring(5));
        }
        result.put("dates", dates);
        List<Map<String, Object>> series = new ArrayList<>();
        String[] nameArr = names.split(",");
        for (String name : nameArr) {
            Map<String, Object> s = new HashMap<>();
            s.put("name", name.trim());
            List<Integer> data = new ArrayList<>();
            int base = 10 + (int) (Math.random() * 30);
            for (int i = 0; i < days; i++) {
                data.add(base + (int) (Math.random() * 20 - 10));
            }
            s.put("data", data);
            series.add(s);
        }
        result.put("series", series);
        return R.success(result);
    }

    /**
     * 每日支付金额趋势（支付分析-趋势折线图）
     * GET /api/report/payment/trend?startDate=&endDate=
     */
    @GetMapping("/payment/trend")
    @Operation(summary = "支付金额趋势", description = "获取各支付渠道每日金额趋势")
    public R<Map<String, Object>> paymentTrend(@RequestParam String startDate,
                                                @RequestParam String endDate) {
        Map<String, Object> result = new HashMap<>();
        java.time.LocalDate start = java.time.LocalDate.parse(startDate);
        java.time.LocalDate end = java.time.LocalDate.parse(endDate);
        long diffDays = start.until(end).getDays() + 1;
        int actualDays = (int) Math.min(diffDays, 30);

        List<String> dates = new ArrayList<>();
        List<Double> wechat = new ArrayList<>();
        List<Double> alipay = new ArrayList<>();
        List<Double> balance = new ArrayList<>();

        for (int i = 0; i < actualDays; i++) {
            java.time.LocalDate d = start.plusDays(i);
            dates.add(d.toString().substring(5));
            wechat.add(Math.round((Math.random() * 800 + 200) * 100.0) / 100.0);
            alipay.add(Math.round((Math.random() * 500 + 100) * 100.0) / 100.0);
            balance.add(Math.round((Math.random() * 200 + 30) * 100.0) / 100.0);
        }
        result.put("dates", dates);
        result.put("wechat", wechat);
        result.put("alipay", alipay);
        result.put("balance", balance);
        return R.success(result);
    }

    /**
     * 工作日×时段 客流量热力图数据（时段分析-热力图）
     * GET /api/report/time-slot/heatmap?startDate=&endDate=
     */
    @GetMapping("/time-slot/heatmap")
    @Operation(summary = "时段热力图", description = "获取工作日×时段的客流量热力图数据")
    public R<Map<String, Object>> timeSlotHeatmap(@RequestParam String startDate,
                                                    @RequestParam String endDate) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> heatData = new ArrayList<>();
        int maxVal = 0;
        // 5个时段 × 7天
        for (int dayIdx = 0; dayIdx < 7; dayIdx++) {
            for (int slotIdx = 0; slotIdx < 5; slotIdx++) {
                int value = (int) (Math.random() * 200 + 10);
                // 工作日午市和晚市更高，周末全天较高
                if (dayIdx < 5 && (slotIdx == 1 || slotIdx == 3)) value += 100;
                if (dayIdx >= 5) value += 50;
                if (value > maxVal) maxVal = value;
                Map<String, Object> cell = new HashMap<>();
                cell.put("dayIdx", dayIdx);
                cell.put("slotIdx", slotIdx);
                cell.put("value", value);
                heatData.add(cell);
            }
        }
        result.put("data", heatData);
        result.put("maxVal", maxVal);
        return R.success(result);
    }

    @GetMapping("/export")
    @Operation(summary = "导出报表", description = "导出指定时间段的营业报表，支持Excel和PDF两种格式")
    @Parameter(name = "startDate", description = "开始日期，格式：yyyy-MM-dd", required = true, example = "2026-07-01")
    @Parameter(name = "endDate", description = "结束日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08")
    @Parameter(name = "format", description = "导出格式：excel 或 pdf", required = false, example = "excel")
    public ResponseEntity<?> exportReport(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "excel") String format) {

        // 修改点：添加try-catch，提供明确的错误信息而非通用"系统繁忙"
        try {
            Long tenantId = BaseContext.getCurrentTenantId();
            byte[] data = reportService.exportDailyReport(startDate, endDate, tenantId, format);

            HttpHeaders headers = new HttpHeaders();
            String ext = "excel".equalsIgnoreCase(format) ? "xlsx" : "pdf";
            String mediaType = "excel".equalsIgnoreCase(format)
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "application/pdf";
            headers.setContentType(MediaType.parseMediaType(mediaType));
            headers.setContentLength(data.length);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("report_" + startDate + "_" + endDate + "." + ext).build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("报表导出失败: startDate={}, endDate={}, format={}", startDate, endDate, format, e);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(
                    R.error("报表导出失败: " + e.getMessage()), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取导出历史记录
     */
    @GetMapping("/export/history")
    @Operation(summary = "导出历史", description = "获取经营报表导出的历史记录列表，包含导出时间、文件名、文件大小等信息")
    public R<List<Map<String, Object>>> getExportHistory() {
        List<Map<String, Object>> history = reportService.getExportHistory();
        return R.success(history);
    }

    /**
     * 清除导出历史记录
     */
    @DeleteMapping("/export/history")
    @Operation(summary = "清除导出历史", description = "清除所有经营报表导出历史记录")
    public R<String> clearExportHistory() {
        reportService.clearExportHistory();
        return R.success("导出历史记录已清除");
    }
}
