package com.reggie.module.payment.channel;

import lombok.Data;

/**
 * 退款响应结果
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class RefundResponse {
    /** 是否成功 */
    private boolean success;
    /** 退款渠道交易号 */
    private String refundChannelTradeNo;
    /** 错误信息 */
    private String errorMsg;
}
