package com.reggie.module.retention.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员等级统计 VO
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
public class MemberLevelStatsVO {

    /** 等级 */
    private String level;

    /** 会员数量 */
    private Integer count;

    /** 占比（百分比） */
    private Double ratio;

    /** 平均消费金额 */
    private BigDecimal avgSpent;
}