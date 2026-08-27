package com.reggie.module.retention.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 留存趋势 VO
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
public class RetentionTrendVO {

    /** 日期 */
    private LocalDate date;

    /** 新增会员数 */
    private Integer newMembers;

    /** 留存会员数 */
    private Integer retainedMembers;

    /** 流失会员数 */
    private Integer churnedMembers;

    /** 留存率（百分比） */
    private Double retentionRate;
}