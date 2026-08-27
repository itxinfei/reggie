package com.reggie.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 销售概览数据传输对象
 * <p>统计全量、今日、昨日、近7天、近30天的销售额及增长趋势</p>
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "销售概览")
public class SalesOverviewVO {

    @Schema(description = "总销售额", example = "125800.00")
    private BigDecimal totalSales;

    @Schema(description = "今日销售额", example = "3680.00")
    private BigDecimal todaySales;

    @Schema(description = "昨日销售额", example = "3520.00")
    private BigDecimal yesterdaySales;

    @Schema(description = "近7天销售额", example = "24500.00")
    private BigDecimal weekSales;

    @Schema(description = "近30天销售额", example = "98200.00")
    private BigDecimal monthSales;

    @Schema(description = "销售额增长率（百分比），同比昨日", example = "4.5")
    private BigDecimal salesGrowth;
}
