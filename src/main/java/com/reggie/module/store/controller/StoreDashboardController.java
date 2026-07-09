package com.reggie.module.store.controller;

import com.reggie.common.R;
import com.reggie.module.store.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 总部控制台Dashboard Controller
 * 提供跨门店经营数据汇总、排行等API
 *
 * @author Reggie Team
 */
@Slf4j
@RestController
@RequestMapping("/store/dashboard")
public class StoreDashboardController {

    @Autowired
    private StoreService storeService;

    /**
     * 获取总部控制台聚合数据
     * GET /store/dashboard/overview
     *
     * 返回：
     * - totalStores: 门店总数
     * - todayTotalOrders: 今日总订单
     * - todayTotalAmount: 今日总营收
     * - todayNewUsers: 今日新增用户
     * - avgOrderAmount: 平均客单价
     * - storeRanking: 门店排行列表
     */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> dashboard = storeService.getAggregatedDashboard();
        return R.success(dashboard);
    }

    /**
     * 获取今日各门店实时订单数
     * GET /store/dashboard/real-time
     */
    @GetMapping("/real-time")
    public R<Map<String, Object>> realTime() {
        // 实时数据通过StoreService获取
        Map<String, Object> data = storeService.getAggregatedDashboard();
        return R.success(data);
    }

    /**
     * 获取门店排行（按今日订单量）
     * GET /store/dashboard/ranking
     */
    @GetMapping("/ranking")
    public R<Map<String, Object>> ranking() {
        Map<String, Object> data = storeService.getAggregatedDashboard();
        @SuppressWarnings("unchecked")
        Object ranking = data.get("storeRanking");
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("storeRanking", ranking);
        return R.success(result);
    }
}
