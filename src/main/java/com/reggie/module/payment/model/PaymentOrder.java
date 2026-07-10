package com.reggie.module.payment.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
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

    /** 主键ID */
    private Long id;
    /** 订单ID */
    private Long orderId;
    /** 租户ID */
    private Long tenantId;
    /** 交易流水号 */
    private String tradeNo;
    /** 渠道交易号 */
    private String channelTradeNo;
    /** 支付渠道 */
    private String channel;
    /** 支付金额 */
    private BigDecimal amount;
    /** 支付状态 */
    private String status;
    /** 支付时间 */
    private LocalDateTime paidTime;
    /** 通知时间 */
    private LocalDateTime notifyTime;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 更新时间 */
    private LocalDateTime updatedTime;
}
