package com.reggie.module.payment.channel;

import lombok.Data;

/**
 * <p>
 * 支付响应结果封装类。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Data
public class PayResponse {
    /** 是否成功 */
    private boolean success;
    /** 渠道交易号 */
    private String channelTradeNo;
    /** 支付URL */
    private String payUrl;
    /** 二维码URL */
    private String qrCodeUrl;
    /** 原始响应 */
    private String rawResponse;
    /** 错误信息 */
    private String errorMsg;
}
