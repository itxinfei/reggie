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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/export")
    @Operation(summary = "导出报表", description = "导出指定时间段的营业报表为CSV文件，包含营业额、订单数、菜品销量等数据")
    @Parameter(name = "startDate", description = "开始日期，格式：yyyy-MM-dd", required = true, example = "2026-07-01")
    @Parameter(name = "endDate", description = "结束日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        byte[] data = reportService.exportDailyReport(startDate, endDate, tenantId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("report.csv").build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
