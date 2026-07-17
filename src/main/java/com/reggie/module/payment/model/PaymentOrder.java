package com.reggie.module.payment.model;

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
 * 支付订单
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("payment_order")
@Schema(description = "支付订单")
public class PaymentOrder implements Serializable {
    /** 序列化版本UID */
    private static final long serialVersionUID = 1L;

    /** 状态常量：待支付 */
    public static final String STATUS_PENDING = "PENDING";
    /** 状态常量：支付成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";
    /** 状态常量：已退款 */
    public static final String STATUS_REFUND = "REFUND";
    /** 状态常量：支付失败 */
    public static final String STATUS_FAIL = "FAIL";

    @Schema(description = "支付订单ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联订单ID", example = "1")
    private Long orderId;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "内部交易流水号", example = "TX20260709001")
    private String tradeNo;

    @Schema(description = "支付渠道交易号", example = "WX20260709001xxxx")
    private String channelTradeNo;

    @Schema(description = "支付渠道：WECHAT=微信，ALIPAY=支付宝", example = "WECHAT")
    private String channel;

    @Schema(description = "支付金额（元）", example = "88.00")
    private BigDecimal amount;

    @Schema(description = "支付状态：PENDING=待支付，SUCCESS=成功，REFUND=已退款，FAIL=失败", example = "SUCCESS")
    private String status;

    @Schema(description = "支付时间", example = "2026-07-09 12:00:00")
    private LocalDateTime paidTime;

    @Schema(description = "渠道异步通知时间", example = "2026-07-09 12:00:05")
    private LocalDateTime notifyTime;

    @Schema(description = "创建时间", example = "2026-07-09 11:55:00")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:05:00")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
