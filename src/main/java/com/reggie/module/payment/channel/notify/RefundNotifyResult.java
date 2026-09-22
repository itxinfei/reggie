package com.reggie.module.payment.channel.notify;

import lombok.Data;

/**
 * 退款回调解析结果。
 *
 * @author reggie
 * @since 2026-09-21
 */
@Data
public class RefundNotifyResult {

    /** 验签/解析是否成功 */
    private boolean success;

    /** 商户退款单号 out_refund_no（对应 RefundRecord.refundNo） */
    private String outRefundNo;

    /** 渠道退款流水号（微信 refund_id） */
    private String refundId;

    /** 退款状态：SUCCESS / CLOSED / ABNORMAL / PROCESSING */
    private String status;

    /** 失败原因 */
    private String errorMsg;
}
