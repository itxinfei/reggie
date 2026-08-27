package com.reggie.module.dashboard.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘聚合查询 Mapper 接口
 * <p>
 * 注意：不继承 BaseMapper，因为仪表盘数据来源于多表聚合视图，不映射单张实体表。
 * 所有 SQL 均通过 XML 映射文件定义，租户隔离由参数显式传入 tenantId 过滤。
 * 使用 @InterceptorIgnore(tenantLine = "true") 绕过 MyBatis-Plus 租户拦截器自动注入，
 * 避免拦截器追加 tenant_id 条件与手写条件重复，造成语法冲突或重复 WHERE 子句。
 * </p>
 *
 * @author reggie
 * @since 2026-08-27
 */
@Mapper
public interface DashboardMapper {

    /**
     * 聚合销售概览数据
     * <p>从 orders 表统计全量、今日、昨日、近7天、近30天的营业额</p>
     *
     * @param tenantId 租户ID
     * @param todayStart 今日起始时间（00:00:00）
     * @param todayEnd 今日结束时间（23:59:59）
     * @param weekStart 近7天起始时间
     * @param monthStart 近30天起始时间
     * @return 聚合结果Map（包含totalSales, todaySales, yesterdaySales, weekSales, monthSales）
     */
    @InterceptorIgnore(tenantLine = "true")
    Map<String, Object> getSalesOverview(@Param("tenantId") Long tenantId,
                                         @Param("todayStart") LocalDateTime todayStart,
                                         @Param("todayEnd") LocalDateTime todayEnd,
                                         @Param("weekStart") LocalDateTime weekStart,
                                         @Param("monthStart") LocalDateTime monthStart);

    /**
     * 聚合订单概览数据
     * <p>从 orders 表统计全量订单数、今日订单数、近7天订单数及待处理/已完成/已取消分布</p>
     *
     * @param tenantId 租户ID
     * @param todayStart 今日起始时间
     * @param todayEnd 今日结束时间
     * @param weekStart 近7天起始时间
     * @return 聚合结果Map（包含totalOrders, todayOrders, weekOrders, pendingOrders, completedOrders, cancelOrders）
     */
    @InterceptorIgnore(tenantLine = "true")
    Map<String, Object> getOrderOverview(@Param("tenantId") Long tenantId,
                                         @Param("todayStart") LocalDateTime todayStart,
                                         @Param("todayEnd") LocalDateTime todayEnd,
                                         @Param("weekStart") LocalDateTime weekStart);

    /**
     * 聚合收入趋势数据
     * <p>从 orders 表按日聚合订单数和已完成订单的营业额</p>
     *
     * @param tenantId 租户ID
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 每日聚合结果列表（每行包含 date, revenue, orderCount）
     */
    @InterceptorIgnore(tenantLine = "true")
    List<Map<String, Object>> getRevenueTrend(@Param("tenantId") Long tenantId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * 聚合热销菜品排行
     * <p>通过 order_detail + orders 关联统计菜品销量，再 JOIN dish 获取菜品信息</p>
     *
     * @param tenantId 租户ID
     * @param limit 返回Top N
     * @return 热销菜品列表（每行包含 dishId, dishName, category, salesCount, revenue）
     */
    @InterceptorIgnore(tenantLine = "true")
    List<Map<String, Object>> getTopProducts(@Param("tenantId") Long tenantId,
                                             @Param("limit") Integer limit);

    /**
     * 聚合会员概览数据
     * <p>从 member 表统计会员总数、本月新增、活跃会员数</p>
     *
     * @param tenantId 租户ID
     * @param monthStart 本月起始时间
     * @param monthEnd 本月结束时间
     * @return 聚合结果Map（包含totalMembers, newMembersThisMonth, activeMembers）
     */
    @InterceptorIgnore(tenantLine = "true")
    Map<String, Object> getMemberOverview(@Param("tenantId") Long tenantId,
                                          @Param("monthStart") LocalDateTime monthStart,
                                          @Param("monthEnd") LocalDateTime monthEnd);
}
