package com.reggie.module.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.order.model.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /**
     * 按状态聚合指定时间区间内的订单数量与金额
     * 租户过滤由 TenantLineInnerInterceptor 自动注入，无需手动拼接 tenant_id
     */
    @Select("SELECT status, COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS amt "
            + "FROM orders WHERE 1=1 AND order_time BETWEEN #{start} AND #{end} "
            + "GROUP BY status")
    List<Map<String, Object>> statOrderByStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按日聚合指定时间区间内的订单数量与已完成订单金额（用于趋势图）
     */
    @Select("SELECT DATE(order_time) AS day, COUNT(*) AS cnt, "
            + "COALESCE(SUM(CASE WHEN status = #{completed} THEN amount ELSE 0 END), 0) AS amt "
            + "FROM orders WHERE 1=1 AND order_time BETWEEN #{start} AND #{end} "
            + "GROUP BY DATE(order_time)")
    List<Map<String, Object>> statOrderByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                             @Param("completed") int completed);

    /**
     * 聚合指定时间区间内热销菜品（按名称汇总销售数量，降序）
     * order_detail 表在租户忽略列表中，通过 order_id 关联已注入租户的 orders 子查询实现隔离
     */
    @Select("SELECT od.name AS name, COALESCE(SUM(od.number), 0) AS cnt "
            + "FROM order_detail od WHERE 1=1 "
            + "AND od.order_id IN (SELECT id FROM orders WHERE 1=1 AND order_time BETWEEN #{start} AND #{end}) "
            + "GROUP BY od.name ORDER BY cnt DESC")
    List<Map<String, Object>> statHotDishes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按门店(tenant_id)聚合指定时间区间内的订单数与已完成订单金额（总部控制台用）
     * <p>仅在超管视图（tenantId 上下文为空）下跨门店返回全部分组；用于替代逐店 N+1 查询</p>
     *
     * @param start     起始时间（含）
     * @param end       结束时间（不含）
     * @param completed 已完成状态值
     * @return 每个 tenantId 的 totalOrders/todayAmount
     */
    @Select("SELECT tenant_id AS tenantId, COUNT(*) AS totalOrders, "
            + "COALESCE(SUM(CASE WHEN status = #{completed} THEN amount ELSE 0 END), 0) AS todayAmount "
            + "FROM orders WHERE create_time >= #{start} AND create_time < #{end} "
            + "GROUP BY tenant_id")
    List<Map<String, Object>> statTodayByTenant(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("completed") int completed);

    /**
     * 聚合指定租户、状态、起始时间之后的订单金额总和（替代全量加载内存求和）
     * 使用 @InterceptorIgnore 避免租户拦截器重复追加 tenant_id 条件，由参数自行过滤
     * 注意：原生注解 SQL 不会自动应用 @TableLogic 逻辑删除过滤，需手动添加 is_deleted = 0。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COALESCE(SUM(amount), 0) FROM orders "
            + "WHERE tenant_id = #{tenantId} AND status = #{status} AND order_time >= #{startTime} AND is_deleted = 0")
    BigDecimal sumAmount(@Param("tenantId") Long tenantId,
                         @Param("status") int status,
                         @Param("startTime") LocalDateTime startTime);

    /**
     * 聚合指定门店租户、状态、时间区间内的订单数与金额（加盟分账结算用）
     * 总部租户聚合加盟门店数据属于跨租户场景，必须 @InterceptorIgnore 绕开租户拦截，
     * 由 tenantId 参数显式过滤，否则拦截器会追加总部 tenant_id 条件导致查不到加盟店订单。
     * 注意：原生注解 SQL 不会自动应用 @TableLogic 逻辑删除过滤，需手动添加 is_deleted = 0。
     *
     * @param tenantId 门店租户ID
     * @param status 订单状态（已完成）
     * @param startTime 起始时间（含）
     * @param endTime 结束时间（含）
     * @return 聚合结果：cnt=订单数，amt=金额总和
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS amt FROM orders "
            + "WHERE tenant_id = #{tenantId} AND status = #{status} "
            + "AND order_time >= #{startTime} AND order_time <= #{endTime} AND is_deleted = 0")
    Map<String, Object> sumOrdersByTenantAndPeriod(@Param("tenantId") Long tenantId,
                                                   @Param("status") int status,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);
}
