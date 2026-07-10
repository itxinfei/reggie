package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户持有优惠券实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class CouponUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    /** 租户ID */
    private Long tenantId;

    /** 持有优惠券的会员ID */
    private Long memberId;
    /** 关联优惠券模板ID */
    private Long templateId;
    /** 优惠券码 */
    private String code;
    /** 状态（unused未使用 used已使用 expired已过期） */
    private String status;
    /** 使用时间 */
    private LocalDateTime usedTime;
    /** 使用的订单ID */
    private Long orderId;
    /** 过期时间 */
    private LocalDateTime expireTime;
    /** 创建时间 */
    private LocalDateTime createdTime;
}
