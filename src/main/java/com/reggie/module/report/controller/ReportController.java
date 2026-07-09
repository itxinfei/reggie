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
