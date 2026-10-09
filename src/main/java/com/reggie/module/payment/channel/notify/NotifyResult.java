package com.reggie.module.payment.channel.notify;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调解析结果（验签 + 解密/解析之后）。
 *
 * @author reggie
 * @since 2026-09-21
 */
@Data
public class NotifyResult {

    /** 验签与业务状态是否成功（成功才触发支付成功回流） */
    private boolean success;

    /** 商户支付单号 out_trade_no */
    private String tradeNo;

    /** 渠道交易流水号（微信 transaction_id / 支付宝 trade_no） */
    private String channelTradeNo;

    /** 回调金额，统一换算为「元」 */
    private BigDecimal amount;

    /** 失败原因 */
    private String errorMsg;
}
