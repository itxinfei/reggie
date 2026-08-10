package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.DeliveryOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 配送订单 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface DeliveryOrderMapper extends BaseMapper<DeliveryOrder> {

    /**
     * 跨租户按平台+平台订单号查询配送订单（供外部平台回调用，回调无登录态无法依赖租户上下文）。
     * 使用 @InterceptorIgnore 跳过租户拦截器，手动在 SQL 中不添加 tenant_id 过滤，
     * 因为 platform_order_id + platform 组合在全局唯一，可精确定位订单。
     *
     * @param platform        平台标识
     * @param platformOrderId 平台订单号
     * @return 配送订单（含 tenantId 供后续操作使用），不存在返回 null
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM delivery_order WHERE platform = #{platform} AND platform_order_id = #{platformOrderId} AND is_deleted = 0 LIMIT 1")
    DeliveryOrder selectByPlatformOrderCrossTenant(@Param("platform") String platform,
                                                    @Param("platformOrderId") String platformOrderId);

    /**
     * 按状态聚合统计配送订单数量与金额（替代全量加载内存计算，防止 OOM）。
     * 使用 @InterceptorIgnore 跳过租户拦截器，手动在 SQL 中添加 tenant_id 过滤，
     * 因为回调/统计场景可能无租户上下文或需显式指定租户。
     *
     * @param tenantId  租户ID（必填）
     * @param platform  平台筛选（可选，为 null 不过滤）
     * @param startDate 开始时间（可选，格式 yyyy-MM-dd 或 yyyy-MM-ddTHH:mm:ss）
     * @param endDate   结束时间（可选）
     * @return 每种状态的聚合结果列表，元素包含 status、cnt、total
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" +
            "SELECT status, COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS total " +
            "FROM delivery_order " +
            "WHERE tenant_id = #{tenantId} AND is_deleted = 0 " +
            "<if test='platform != null and platform != \"\"'> AND platform = #{platform} </if>" +
            "<if test='startDate != null and startDate != \"\"'> AND order_time &gt;= #{startDate} </if>" +
            "<if test='endDate != null and endDate != \"\"'> AND order_time &lt;= #{endDate} </if>" +
            "GROUP BY status" +
            "</script>")
    List<Map<String, Object>> selectStatsByStatus(@Param("tenantId") Long tenantId,
                                                    @Param("platform") String platform,
                                                    @Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);
}
