package com.reggie.module.member.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券发放会员明细 VO
 * 用于展示某模板已发放会员的列表及其用券状态
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "优惠券发放会员明细")
public class IssuedMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员姓名")
    private String memberName;

    @Schema(description = "会员手机")
    private String memberPhone;

    @Schema(description = "会员等级名称")
    private String levelName;

    @Schema(description = "用户优惠券ID")
    private Long couponUserId;

    @Schema(description = "优惠券状态：unused/used/expired")
    private String couponStatus;

    @Schema(description = "领取时间")
    private LocalDateTime createdTime;

    @Schema(description = "使用时间")
    private LocalDateTime usedTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}