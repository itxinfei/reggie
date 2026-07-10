package com.reggie.service;

import java.util.List;
import java.util.Map;

/**
 * 数据概览仪表盘服务接口，聚合系统核心指标，通过Redis缓存降低数据库查询压力
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface DashboardService {

    /**
     * 获取今日核心KPI数据
     * Redis缓存Key: dashboard:overview:{tenantId}
     * 缓存TTL: 5分钟（高频访问，需较高实时性）
     * 数据流向：前端请求 → Redis(命中直接返回) → MySQL(未命中则查询并回填Redis)
     *
     * @return 包含今日订单数、营业额、活跃用户、完成率等指标的Map
     */
    Map<String, Object> getOverview(Long tenantId);

    /**
     * 获取最近7天趋势数据
     * Redis缓存Key: dashboard:trend:{tenantId}
     * 缓存TTL: 30分钟（历史数据不频繁变动）
     *
     * @return 每日的订单数和营业额列表
     */
    List<Map<String, Object>> getTrend(Long tenantId);

    /**
     * 获取当前订单状态分布
     * Redis缓存Key: dashboard:order-status:{tenantId}
     * 缓存TTL: 2分钟（订单状态变动较频繁）
     *
     * @return 各状态订单数量分布
     */
    Map<String, Object> getOrderStatusDistribution(Long tenantId);

    /**
     * 获取热门菜品排行（基于Redis ZSet实时排序）
     * Redis数据结构: ZSet - dashboard:hot-dishes:{tenantId}
     * 缓存TTL: 15分钟
     *
     * @param limit 返回Top N
     * @return 菜品名称和销量列表
     */
    List<Map<String, Object>> getHotDishes(Long tenantId, int limit);

    /**
     * 获取系统健康状态
     * 检查Redis连接状态、数据库状态等
     *
     * @return 系统各组件的健康状态
     */
    Map<String, Object> getSystemHealth();

    /**
     * 检查Redis是否可用
     *
     * @return true表示Redis连接正常
     */
    boolean isRedisAvailable();

    /**
     * 清除指定租户的 Dashboard 概览和订单状态分布缓存
     * <p>当订单状态发生变更（创建/状态流转/取消/退款）时调用，确保前端看到的数据实时准确
     *
     * @param tenantId 租户ID
     */
    void clearOverviewCache(Long tenantId);
}
