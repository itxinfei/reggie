package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户持有优惠券
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("coupon_user")
@Schema(description = "用户持有优惠券")
public class CouponUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户优惠券ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @Schema(description = "关联优惠券模板ID", example = "1")
    private Long templateId;

    @Schema(description = "优惠券码", example = "CPN20260709001")
    private String code;

    @Schema(description = "状态：unused=未使用，used=已使用，expired=已过期", example = "unused")
    private String status;

    @Schema(description = "使用时间", example = "2026-07-10 12:00:00")
    private LocalDateTime usedTime;

    @Schema(description = "使用订单ID", example = "1")
    private Long orderId;

    @Schema(description = "过期时间", example = "2026-08-09 23:59:59")
    private LocalDateTime expireTime;

    @Schema(description = "领取时间", example = "2026-07-09 10:00:00")
    private LocalDateTime createdTime;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
