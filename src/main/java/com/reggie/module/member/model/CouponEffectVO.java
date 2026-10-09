package com.reggie.module.member.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 优惠券模板投放效果 VO
 * 用于展示某模板的发放总数、使用分布、使用率、活跃率等聚合指标
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "优惠券模板投放效果")
public class CouponEffectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "优惠券类型")
    private String type;

    @Schema(description = "发放总数（模板 totalCount）")
    private Integer totalCount;

    @Schema(description = "剩余可领数量")
    private Integer remainCount;

    @Schema(description = "已发放数（totalCount - remainCount）")
    private Integer issuedCount;

    @Schema(description = "已使用数")
    private Integer usedCount;

    @Schema(description = "未使用数")
    private Integer unusedCount;

    @Schema(description = "已过期数")
    private Integer expiredCount;

    @Schema(description = "发放率 = 已发放数 / 发放总数")
    private String issueRate;

    @Schema(description = "使用率 = 已使用数 / 已发放数")
    private String usageRate;

    @Schema(description = "活跃率 = 已使用数 / 发放总数")
    private String activeRate;

    @Schema(description = "优惠金额（元）")
    private BigDecimal discountAmount;

    @Schema(description = "满减条件金额（元）")
    private BigDecimal conditionAmount;
}