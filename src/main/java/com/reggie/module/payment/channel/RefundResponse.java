package com.reggie.module.payment.channel;

import lombok.Data;

/**
 * <p>
 * 退款响应结果封装类。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Data
public class RefundResponse {
    /** 是否成功（同步即确定终态：支付宝恒为此模式；微信状态 SUCCESS） */
    private boolean success;
    /**
     * 是否处于处理中（已受理但未定终态）。
     * 微信退款可能同步返回 PROCESSING：此时仅登记 processing 退款记录、不做任何联动，
     * 最终结果以退款异步回调为准。支付宝无此状态，恒为 false。
     */
    private boolean processing;
    /** 退款渠道交易号 */
    private String refundChannelTradeNo;
    /** 错误信息 */
    private String errorMsg;
}
