package com.reggie.module.member.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员可用优惠券展示对象（收银台选券场景）
 *
 * @author reggie
 * @since 2026-08-14
 */
@Data
@Schema(description = "会员可用优惠券展示对象")
public class CouponAvailableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户优惠券记录ID")
    private Long id;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "优惠类型：1满减券 2折扣券")
    private Integer type;

    @Schema(description = "使用门槛（满额），0 表示无门槛")
    private BigDecimal conditionAmount;

    @Schema(description = "满减金额（type=1 时有效）")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率（type=2 时有效，如 0.8 表示八折）")
    private BigDecimal discountRate;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "针对当前订单可抵扣金额（前端展示用，由后端计算）")
    private BigDecimal currentDiscount;
}
