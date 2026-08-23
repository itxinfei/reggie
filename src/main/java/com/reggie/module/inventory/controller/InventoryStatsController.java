package com.reggie.module.inventory.controller;

import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.inventory.service.InventoryStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 进销存统计控制器
 * 提供进销存模块的数据统计、趋势分析、库存预警摘要等聚合接口
 * <p>域4 改造：所有聚合查询下沉到 InventoryStatsService，Controller 不再直接操作 Mapper</p>
 *
 * @author reggie
 * @since 2026-07-11
 */
@RequireEmployee
@RestController
@RequestMapping("/api/inventory/stats")
@Tag(name = "进销存统计", description = "进销存模块数据统计API")
public class InventoryStatsController {

    @Autowired
    private InventoryStatsService inventoryStatsService;

    /**
     * 进销存总览统计
     * GET /api/inventory/stats/overview
     */
    @GetMapping("/overview")
    @Operation(summary = "进销存总览", description = "获取进销存模块的核心统计数据：食材/分类/供应商数量、库存预警、今日采购、今日出入库")
    public R<Map<String, Object>> overview() {
        return R.success(inventoryStatsService.overview());
    }

    /**
     * 近30天采购趋势
     * GET /api/inventory/stats/purchase-trend
     */
    @GetMapping("/purchase-trend")
    @Operation(summary = "采购趋势", description = "获取近30天每日采购金额趋势")
    public R<List<Map<String, Object>>> purchaseTrend() {
        return R.success(inventoryStatsService.purchaseTrend());
    }

    /**
     * 近30天出入库趋势
     * GET /api/inventory/stats/stock-trend
     */
    @GetMapping("/stock-trend")
    @Operation(summary = "库存趋势", description = "获取近30天每日入库/出库数量趋势")
    public R<List<Map<String, Object>>> stockTrend() {
        return R.success(inventoryStatsService.stockTrend());
    }
}