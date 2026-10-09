package com.reggie.module.member.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 优惠券到期预警统计 VO
 * 按模板聚合即将到期的数量与优惠总额
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "优惠券到期预警统计（按模板）")
public class ExpiringByTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "优惠券模板ID")
    private Long templateId;

    @Schema(description = "优惠券模板名称")
    private String templateName;

    @Schema(description = "优惠券类型")
    private String couponType;

    @Schema(description = "即将到期数量")
    private Integer expiringCount;

    @Schema(description = "已过期数量")
    private Integer expiredCount;

    @Schema(description = "优惠总额（即将到期的优惠金额累加）")
    private BigDecimal expiringDiscountAmount;

    @Schema(description = "优惠总额（已过期的优惠金额累加）")
    private BigDecimal expiredDiscountAmount;
}