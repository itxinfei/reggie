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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("recharge_record")
@Schema(description = "会员充值记录")
public class RechargeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：待门店确认到账（顾客已在线发起，等待门店收款确认） */
    public static final String STATUS_PENDING = "PENDING";
    /** 状态：已入账（余额已到账） */
    public static final String STATUS_SUCCESS = "SUCCESS";
    /** 状态：已取消（超时未支付/顾客或门店取消） */
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Schema(description = "充值记录ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @Schema(description = "归属用户ID（C端自助充值场景冗余，便于按用户查询）", example = "1")
    private Long userId;

    @Schema(description = "充值单号（业务唯一，对账单号）", example = "RC20260921120000AB12CD34")
    private String rechargeNo;

    @Schema(description = "状态：PENDING=待门店确认到账，SUCCESS=已入账，CANCELLED=已取消", example = "PENDING")
    private String status;

    @Schema(description = "充值金额（元）", example = "200.00")
    private BigDecimal amount;

    @Schema(description = "赠送金额（元）", example = "20.00")
    private BigDecimal giftAmount;

    @Schema(description = "支付/意向渠道：WECHAT=微信，ALIPAY=支付宝，CASH=现金（预留在线支付渠道）", example = "WECHAT")
    private String paymentMethod;

    @Schema(description = "渠道交易号（预留：未来在线支付成功后回填第三方流水号）")
    private String tradeNo;

    @Schema(description = "确认到账操作员工ID（门店确认模式）", example = "1")
    private Long confirmEmployeeId;

    @Schema(description = "确认到账时间")
    private LocalDateTime confirmTime;

    @Schema(description = "充值时间", example = "2026-07-09 10:00:00")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "是否删除：0=未删除，1=已删除", example = "0")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
