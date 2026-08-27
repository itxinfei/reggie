package com.reggie.module.retention.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 流失预警 VO
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
public class ChurnWarningVO {

    /** 会员ID */
    private Long id;

    /** 会员姓名 */
    private String memberName;

    /** 手机号 */
    private String phone;

    /** 等级 */
    private String level;

    /** 最后下单日期 */
    private LocalDate lastOrderDate;

    /** 距今天数 */
    private Integer daysSinceLastOrder;

    /** 风险评分（0-100） */
    private Integer riskScore;

    /** 风险等级: LOW / MEDIUM / HIGH */
    private String riskLevel;
}