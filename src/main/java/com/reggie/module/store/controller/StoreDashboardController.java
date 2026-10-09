package com.reggie.module.store.controller;

import com.reggie.common.R;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.module.store.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 总部控制台Dashboard控制器
 * 提供跨门店经营数据汇总、排行等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RequiresAdmin
@RestController
@RequestMapping("/store/dashboard")
@Tag(name = "总部控制台", description = "跨门店经营数据汇总与排行接口")
public class StoreDashboardController {

    @Autowired
    private StoreService storeService;

    /**
     * 获取总部控制台聚合数据
     * @return 跨门店经营数据汇总（门店总数、今日订单/营收、新增用户、门店排行）
     */
    @GetMapping("/overview")
    @Operation(summary = "总部控制台总览", description = "获取跨门店经营数据汇总：门店总数、今日订单/营收、新增用户、门店排行")
    public R<Map<String, Object>> overview() {
        Map<String, Object> dashboard = storeService.getAggregatedDashboard();
        return R.success(dashboard);
    }

    /**
     * 获取门店排行（按今日订单量）
     * @return 门店排行列表
     */
    @GetMapping("/ranking")
    @Operation(summary = "门店排行", description = "按今日订单量获取门店排行列表")
    public R<Map<String, Object>> ranking() {
        Map<String, Object> data = storeService.getAggregatedDashboard();
        @SuppressWarnings("unchecked") // Map.get返回Object类型，需要类型转换
        Object ranking = data.get("storeRanking");
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("storeRanking", ranking);
        return R.success(result);
    }

    /**
     * 多店近 N 天营收趋势对比（多折线图）
     * @param days 天数，默认 7
     * @return 趋势数据：{ dates, stores: [ { tenantId, name, amounts }, ... ] }
     */
    @GetMapping("/trend")
    @Operation(summary = "多店营收趋势", description = "近 N 天各门店每日营收趋势对比，用于多折线图展示")
    public R<Map<String, Object>> multiStoreTrend(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> data = storeService.getMultiStoreTrend(days);
        return R.success(data);
    }

    /**
     * 多品类销售对比（各店各品类的销售占比）
     * @param startDate 起始日期（yyyy-MM-dd，可选）
     * @param endDate   结束日期（yyyy-MM-dd，可选）
     * @return 品类对比数据列表
     */
    @GetMapping("/category-comparison")
    @Operation(summary = "品类销售对比", description = "各门店各品类的销售金额占比，支持日期范围筛选")
    public R<List<Map<String, Object>>> categoryComparison(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<Map<String, Object>> data = storeService.getCategoryComparison(startDate, endDate);
        return R.success(data);
    }

    /**
     * 门店排行详情（近 N 天，营收/订单数/客单价三维）
     * @param days 天数，默认 7
     * @return 排行列表
     */
    @GetMapping("/ranking-detail")
    @Operation(summary = "门店排行详情", description = "近 N 天门店营收/订单数/客单价三维排行")
    public R<List<Map<String, Object>>> rankingDetail(
            @RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> data = storeService.getRankingDetail(days);
        return R.success(data);
    }
}
