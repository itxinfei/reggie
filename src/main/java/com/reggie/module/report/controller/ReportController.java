package com.reggie.module.report.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 经营报表控制器
 * 提供日报、菜品排行、时段分析、支付分析、报表导出等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/api/report")
@Slf4j
@Tag(name = "经营报表")
@Validated
@RequireEmployee
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 获取指定日期的经营日报表
     * @param date 日期，格式：yyyy-MM-dd
     * @return 日报表数据（营业额、订单数、客单价等）
     */
    @GetMapping("/daily")
    @Operation(summary = "日报表", description = "获取指定日期的经营日报表数据，包含营业额、订单数、客单价等核心指标")
    public R<Map<String, Object>> dailyReport(@Parameter(description = "日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08") @RequestParam String date) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportService.getDailyReport(date, tenantId);
        return R.success(data);
    }

    /**
     * 获取指定时间段的菜品销售排行
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate 结束日期，格式：yyyy-MM-dd
     * @param limit 返回前N条排行
     * @return 菜品排行列表
     */
    @GetMapping("/dish-ranking")
    @Operation(summary = "菜品排行", description = "获取指定时间段的菜品销售排行，支持限制返回数量")
    public R<List<Map<String, Object>>> dishRanking(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true, example = "2026-07-01") @RequestParam String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08") @RequestParam String endDate,
            @Parameter(description = "返回前N条排行", required = false, example = "10") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> data = reportService.getDishRanking(startDate, endDate, limit, tenantId);
        return R.success(data);
    }

    /**
     * 获取指定时间段的营业时段分析
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate 结束日期，格式：yyyy-MM-dd
     * @return 时段分析列表
     */
    @GetMapping("/time-slot")
    @Operation(summary = "时段分析", description = "获取指定时间段的营业时段分析，识别高峰时段")
    public R<List<Map<String, Object>>> timeSlotAnalysis(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true, example = "2026-07-01") @RequestParam String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> data = reportService.getTimeSlotAnalysis(startDate, endDate, tenantId);
        return R.success(data);
    }

    /**
     * 获取指定时间段的支付方式统计
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate 结束日期，格式：yyyy-MM-dd
     * @return 支付方式统计数据
     */
    @GetMapping("/payment")
    @Operation(summary = "支付方式分析", description = "获取指定时间段的支付方式统计，分析微信/支付宝等支付渠道占比")
    public R<Map<String, Object>> paymentAnalysis(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true, example = "2026-07-01") @RequestParam String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08") @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportService.getPaymentAnalysis(startDate, endDate, tenantId);
        return R.success(data);
    }

    // ======================== 增强分析接口 ========================

    /**
     * 菜品分类销售占比
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 各菜品分类销售数量和占比
     */
    @GetMapping("/category-sales")
    @Operation(summary = "菜品分类销售占比", description = "获取各菜品分类的销售数量和占比，数据来源：order_detail + dish + category 联表统计")
    public R<List<Map<String, Object>>> categorySales(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true) @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> data = reportService.getCategorySales(startDate, endDate, tenantId);
        return R.success(data);
    }

    /**
     * 获取指定菜品的每日销量趋势
     * @param names 菜品名称列表（逗号分隔）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 各菜品每日销量趋势数据
     */
    @GetMapping("/dish-trend")
    @Operation(summary = "菜品销量趋势", description = "获取指定菜品在日期范围内的每日销量趋势，数据来源：order_detail + orders 真实统计")
    public R<Map<String, Object>> dishTrend(@Parameter(description = "菜品名称列表，逗号分隔", required = true) @RequestParam String names,
                                              @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true) @RequestParam String startDate,
                                              @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true) @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        String[] nameArr = names.split(",");
        List<String> dishNameList = new ArrayList<>();
        for (String n : nameArr) {
            String trimmed = n.trim();
            if (!trimmed.isEmpty()) dishNameList.add(trimmed);
        }
        Map<String, Object> data = reportService.getDishTrend(dishNameList, startDate, endDate, tenantId);
        return R.success(data);
    }

    /**
     * 获取各支付渠道每日金额趋势
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 各支付渠道每日金额趋势
     */
    @GetMapping("/payment/trend")
    @Operation(summary = "支付金额趋势", description = "获取各支付渠道每日金额趋势，数据来源：orders 表真实统计")
    public R<Map<String, Object>> paymentTrend(@Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true) @RequestParam String startDate,
                                                @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true) @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportService.getPaymentTrend(startDate, endDate, tenantId);
        return R.success(data);
    }

    /**
     * 获取工作日与时段的客流量热力图数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 工作日与时段的客流量热力图数据
     */
    @GetMapping("/time-slot/heatmap")
    @Operation(summary = "时段热力图", description = "获取工作日×时段的客流量热力图数据，数据来源：orders 表真实统计")
    public R<Map<String, Object>> timeSlotHeatmap(@Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true) @RequestParam String startDate,
                                                    @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true) @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportService.getTimeSlotHeatmap(startDate, endDate, tenantId);
        return R.success(data);
    }

    /**
     * 获取复购率统计趋势
     * @param period 统计周期（day/week/month/year）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 复购率趋势数据
     */
    @GetMapping("/repurchase-rate")
    @Operation(summary = "复购率统计", description = "获取指定时间范围内的复购率趋势，支持按日/周/月/年分组")
    public R<Map<String, Object>> repurchaseRate(
            @Parameter(description = "统计周期（day/week/month/year）", required = false, example = "day")
            @RequestParam(defaultValue = "day") String period,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true) @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportService.getRepurchaseRate(period, startDate, endDate, tenantId);
        return R.success(data);
    }

    /**
     * 获取各菜品的复购率排行
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param limit 返回前N条排行
     * @return 菜品复购率排行
     */
    @GetMapping("/repurchase-rate/dish")
    @Operation(summary = "菜品复购率排行", description = "获取各菜品的复购率排行，数据来源：order_detail + orders 联表统计")
    public R<Map<String, Object>> repurchaseRateByDish(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true) @RequestParam String endDate,
            @Parameter(description = "返回前N条排行", required = false, example = "10") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportService.getRepurchaseRateByDish(startDate, endDate, limit, tenantId);
        return R.success(data);
    }

    /**
     * 同期群分析（Cohort Analysis）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 同期群分析数据
     */
    @GetMapping("/cohort")
    @Operation(summary = "同期群分析", description = "按首次消费月份分组，分析各用户群的复购率表现")
    public R<Map<String, Object>> cohortAnalysis(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true) @RequestParam String endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = reportService.getCohortAnalysis(startDate, endDate, tenantId);
        return R.success(data);
    }

    /**
     * 导出营业报表
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate 结束日期，格式：yyyy-MM-dd
     * @param format 导出格式：excel 或 pdf
     * @return 报表文件流
     */
    @GetMapping("/export")
    @Operation(summary = "导出报表", description = "导出指定时间段的营业报表，支持Excel和PDF两种格式")
    public ResponseEntity<?> exportReport(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true, example = "2026-07-01") @RequestParam String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true, example = "2026-07-08") @RequestParam String endDate,
            @Parameter(description = "导出格式：excel 或 pdf", required = false, example = "excel") @RequestParam(defaultValue = "excel") String format) {

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
            // 修改点：filename* UTF-8 百分号编码，修复中文文件名乱码
            String safeName = "营业报表_" + startDate + "_" + endDate + "." + ext;
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"report_" + startDate + "_" + endDate + "." + ext + "\"; filename*=UTF-8''"
                            + URLEncoder.encode(safeName, "UTF-8").replace("+", "%20"));
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("报表导出失败: startDate={}, endDate={}, format={}", startDate, endDate, format, e);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(
                    R.error("报表导出失败，请稍后重试"), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取经营报表导出历史记录
     * @return 导出历史列表
     */
    @GetMapping("/export/history")
    @Operation(summary = "导出历史", description = "获取经营报表导出的历史记录列表，包含导出时间、文件名、文件大小等信息")
    public R<List<Map<String, Object>>> getExportHistory() {
        List<Map<String, Object>> history = reportService.getExportHistory();
        return R.success(history);
    }

    /**
     * 清除经营报表导出历史记录
     * @return 操作结果
     */
    @DeleteMapping("/export/history")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "清除导出历史", description = "清除所有经营报表导出历史记录")
    public R<String> clearExportHistory() {
        reportService.clearExportHistory();
        return R.success("导出历史记录已清除");
    }
}


