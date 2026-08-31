package com.reggie.module.store.controller;

import com.reggie.common.R;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.module.store.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
