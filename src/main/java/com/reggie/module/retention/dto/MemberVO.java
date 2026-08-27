package com.reggie.module.retention.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 会员详情 VO
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
public class MemberVO {

    /** 会员ID */
    private Long id;

    /** 会员姓名 */
    private String memberName;

    /** 手机号 */
    private String phone;

    /** 等级: GOLD / SILVER / NORMAL */
    private String level;

    /** 积分 */
    private Integer points;

    /** 最后下单日期 */
    private LocalDate lastOrderDate;

    /** 总下单次数 */
    private Integer totalOrders;

    /** 总消费金额 */
    private BigDecimal totalSpent;

    /** 状态: ACTIVE / DORMANT / CHURNED */
    private String status;

    /** 标签 */
    private String tag;
}