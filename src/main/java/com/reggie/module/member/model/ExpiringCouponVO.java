package com.reggie.module.member.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券到期预警明细 VO
 * 用于展示即将到期/已过期优惠券的会员与模板信息
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "优惠券到期预警明细")
public class ExpiringCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户优惠券ID")
    private Long couponUserId;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员姓名")
    private String memberName;

    @Schema(description = "会员手机")
    private String memberPhone;

    @Schema(description = "优惠券模板ID")
    private Long templateId;

    @Schema(description = "优惠券模板名称")
    private String templateName;

    @Schema(description = "优惠券类型：FULL_REDUCTION/DISCOUNT/NEW_MEMBER")
    private String couponType;

    @Schema(description = "优惠券码")
    private String couponCode;

    @Schema(description = "优惠券状态：unused/used/expired")
    private String couponStatus;

    @Schema(description = "优惠金额（满减/新客）")
    private BigDecimal discountAmount;

    @Schema(description = "条件金额（满减满X元）")
    private BigDecimal conditionAmount;

    @Schema(description = "折扣率（折扣券，如0.85表示8.5折）")
    private BigDecimal discountRate;

    @Schema(description = "领取时间")
    private LocalDateTime createdTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}