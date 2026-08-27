package com.reggie.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单概览数据传输对象
 * <p>统计全量、今日、近7天订单数及待处理、已完成、已取消订单分布</p>
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单概览")
public class OrderOverviewVO {

    @Schema(description = "总订单数", example = "12580")
    private Long totalOrders;

    @Schema(description = "今日订单数", example = "126")
    private Long todayOrders;

    @Schema(description = "近7天订单数", example = "856")
    private Long weekOrders;

    @Schema(description = "待处理订单数（待付款+待接单）", example = "18")
    private Long pendingOrders;

    @Schema(description = "已完成订单数", example = "98")
    private Long completedOrders;

    @Schema(description = "已取消订单数", example = "10")
    private Long cancelOrders;
}
