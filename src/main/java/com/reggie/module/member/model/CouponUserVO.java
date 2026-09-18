package com.reggie.module.member.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员「我的优惠券」展示对象。
 * <p>
 * 在用户领券记录 {@link CouponUser} 基础上，关联模板 {@link CouponTemplate}
 * 补充名称/类型/面额等展示字段，避免 C 端直接渲染裸 CouponUser 导致金额恒为 0、名称缺失。
 *
 * @author reggie
 * @since 2026-09-18
 */
@Data
@Schema(description = "我的优惠券展示对象")
public class CouponUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户优惠券记录ID")
    private Long id;

    @Schema(description = "优惠券模板ID")
    private Long templateId;

    @Schema(description = "券码")
    private String code;

    @Schema(description = "状态：unused 未使用 / used 已使用 / expired 已过期")
    private String status;

    @Schema(description = "使用时间")
    private LocalDateTime usedTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "领取时间")
    private LocalDateTime createdTime;

    @Schema(description = "优惠券名称（来自模板）")
    private String name;

    @Schema(description = "类型：FULL_REDUCTION 满减券 / DISCOUNT 折扣券 / NEW_MEMBER 新客券（来自模板）")
    private String type;

    @Schema(description = "使用门槛（满额），0 或 null 表示无门槛（来自模板）")
    private BigDecimal conditionAmount;

    @Schema(description = "满减金额（满减券，来自模板）")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率，如 0.85 表示 8.5 折（折扣券，来自模板）")
    private BigDecimal discountRate;
}
