package com.reggie.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 收入趋势数据传输对象
 * <p>按日统计营业额、订单数和客单价</p>
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "收入趋势")
public class RevenueTrendVO {

    @Schema(description = "日期（格式：yyyy-MM-dd）", example = "2026-08-27")
    private String date;

    @Schema(description = "日营业额", example = "3680.00")
    private BigDecimal revenue;

    @Schema(description = "日订单数", example = "126")
    private Integer orderCount;

    @Schema(description = "客单价", example = "29.21")
    private BigDecimal avgOrderValue;
}
