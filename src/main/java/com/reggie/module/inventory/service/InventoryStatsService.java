package com.reggie.module.inventory.service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 进销存统计服务接口
 * <p>域4 改造：从 InventoryStatsController 下沉，Controller 不再直接操作 Mapper</p>
 * <p>封装进销存总览、采购趋势、库存趋势等聚合查询逻辑</p>
 *
 * @author reggie
 * @since 2026-08-22
 */
public interface InventoryStatsService {

    /**
     * 进销存总览统计
     * <p>封装食材统计、库存预警、今日/本月采购、今日出入库等聚合查询</p>
     *
     * @return 总览数据，包含 totalMaterials/activeMaterials/lowStockCount/totalInventoryValue
     *         totalCategories/totalSuppliers/todayPOCount/todayPOAmount 等字段
     */
    Map<String, Object> overview();

    /**
     * 近30天采购趋势
     * <p>返回每日采购金额和采购单数</p>
     *
     * @return 30天的每日趋势，每项含 date/amount/count
     */
    List<Map<String, Object>> purchaseTrend();

    /**
     * 近30天出入库趋势
     * <p>返回每日入库/出库数量</p>
     *
     * @return 30天的每日趋势，每项含 date/inCount/outCount
     */
    List<Map<String, Object>> stockTrend();
}