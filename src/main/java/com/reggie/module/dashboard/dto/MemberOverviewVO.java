package com.reggie.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 会员概览数据传输对象
 * <p>统计会员总数、本月新增、活跃会员及增长率</p>
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会员概览")
public class MemberOverviewVO {

    @Schema(description = "会员总数", example = "12580")
    private Long totalMembers;

    @Schema(description = "本月新增会员数", example = "268")
    private Long newMembersThisMonth;

    @Schema(description = "活跃会员数（近30天有订单）", example = "8640")
    private Long activeMembers;

    @Schema(description = "会员增长率（百分比），同比上月", example = "12.5")
    private BigDecimal memberGrowth;
}
