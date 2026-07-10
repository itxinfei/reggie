package com.reggie.module.report.service;

import java.util.List;
import java.util.Map;

/**
 * 经营报表服务接口
 * 提供日报、菜品排行、时段分析、支付分析及报表导出等功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface ReportService {

    /**
     * 获取指定日期的经营日报
     *
     * @param date     日期（yyyy-MM-dd）
     * @param tenantId 租户ID
     * @return 日报数据
     */
    Map<String, Object> getDailyReport(String date, Long tenantId);

    /**
     * 获取菜品销量排行
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param limit     排行数量上限
     * @param tenantId  租户ID
     * @return 菜品排行列表
     */
    List<Map<String, Object>> getDishRanking(String startDate, String endDate, int limit, Long tenantId);

    /**
     * 获取时段客流分析
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 时段分析数据
     */
    List<Map<String, Object>> getTimeSlotAnalysis(String startDate, String endDate, Long tenantId);

    /**
     * 获取支付方式分析
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 支付分析数据
     */
    Map<String, Object> getPaymentAnalysis(String startDate, String endDate, Long tenantId);

    /**
     * 导出经营报表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @param format    导出格式（如excel、pdf）
     * @return 报表文件字节数组
     */
    byte[] exportDailyReport(String startDate, String endDate, Long tenantId, String format);

    // ======================== 增强分析接口（真实数据库查询）========================

    /**
     * 菜品分类销售占比（日报-分类饼图）
     * 从 order_detail + dish + category 三表联查，按分类聚合销量
     *
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @param tenantId  租户ID
     * @return [{name: "热菜", count: 150}, ...]
     */
    List<Map<String, Object>> getCategorySales(String startDate, String endDate, Long tenantId);

    /**
     * Top3菜品每日销量趋势（菜品排行-趋势折线图）
     * 从 order_detail + orders 按日期聚合指定菜品的销量
     *
     * @param dishNames 菜品名称列表
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @param tenantId  租户ID
     * @return {dates: [...], series: [{name, data: [...]}]}
     */
    Map<String, Object> getDishTrend(List<String> dishNames, String startDate, String endDate, Long tenantId);

    /**
     * 每日支付金额趋势（支付分析-趋势折线图）
     * 从 orders 按日期+支付方式聚合金额
     *
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @param tenantId  租户ID
     * @return {dates: [...], wechat: [...], alipay: [...], balance: [...]}
     */
    Map<String, Object> getPaymentTrend(String startDate, String endDate, Long tenantId);

    /**
     * 工作日×时段客流量热力图（时段分析-热力图）
     * 从 orders 按星期+时段统计订单数
     *
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @param tenantId  租户ID
     * @return {data: [{dayIdx, slotIdx, value}], maxVal: int}
     */
    Map<String, Object> getTimeSlotHeatmap(String startDate, String endDate, Long tenantId);

    /**
     * 添加导出记录
     *
     * @param dateRange  日期范围，如 "2026-07-01 ~ 2026-07-08"
     * @param format     导出格式，如 "excel"
     * @param fileName   文件名，如 "report_2026-07-01_2026-07-08.xlsx"
     * @param fileSize   文件大小（字节）
     * @param status     导出状态，如 "success"、"failed"
     */
    void addExportRecord(String dateRange, String format, String fileName, long fileSize, String status);

    /**
     * 获取导出历史记录
     *
     * @return 导出记录列表，按导出时间倒序排列
     */
    List<Map<String, Object>> getExportHistory();

    /**
     * 清除所有导出历史记录
     */
    void clearExportHistory();

    /**
     * 获取复购率统计（按日/周/月/年）
     *
     * @param period   统计周期：day | week | month | year
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @param tenantId  租户ID
     * @return {dates: [...], rates: [...], totalRate: float, totalUsers: int, repurchaseUsers: int}
     */
    Map<String, Object> getRepurchaseRate(String period, String startDate, String endDate, Long tenantId);

    /**
     * 获取各菜品复购率排行
     *
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @param limit     返回条数
     * @param tenantId  租户ID
     * @return [{dishId, dishName, totalUsers, repurchaseUsers, rate}, ...]
     */
    Map<String, Object> getRepurchaseRateByDish(String startDate, String endDate, int limit, Long tenantId);

    /**
     * 同期群分析（Cohort Analysis）
     *
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @param tenantId  租户ID
     * @return {cohorts: [{cohortDate, users, repurchaseRate}], totalCohorts: int}
     */
    Map<String, Object> getCohortAnalysis(String startDate, String endDate, Long tenantId);
}
