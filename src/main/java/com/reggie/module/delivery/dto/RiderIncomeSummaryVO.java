package com.reggie.module.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 骑手收入汇总视图对象。
 * <p>
 * 由 {@code RiderIncomeService} 基于现有 {@code orders.delivery_fee} 与
 * {@code delivery_time_record} 按时间窗口（今日/本周/本月）聚合得到，仅做展示，不涉及记账。
 * </p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Data
@Schema(description = "骑手收入汇总")
public class RiderIncomeSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配送费收入（元）", example = "128.50")
    private BigDecimal income;

    @Schema(description = "完成单量", example = "32")
    private Integer orderCount;

    @Schema(description = "配送里程（公里）", example = "86.40")
    private BigDecimal mileage;

    @Schema(description = "准时率（百分比，0-100）", example = "92.50")
    private BigDecimal onTimeRate;

    @Schema(description = "平均送达时长（分钟）", example = "24.5")
    private BigDecimal avgMinutes;
}
