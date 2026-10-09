package com.reggie.module.payment.channel;

import lombok.Data;
import java.math.BigDecimal;

/**
 * <p>
 * 退款请求参数封装类。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Data
public class RefundRequest {
    /** 渠道交易号 */
    private String channelTradeNo;
    /** 退款金额 */
    private BigDecimal amount;
    /** 退款原因 */
    private String reason;
    /**
     * 商户退款单号（渠道幂等键）。
     * <p>
     * 生产渠道（微信 out_request_no / 支付宝 out_request_no）以此做退款幂等去重：
     * 同一商户退款单号重复请求只退款一次，防止"本地 DB 落库失败后重试/并发退款"造成双重扣款。
     * 本地退款流水号 {@code RF+yyyyMMddHHmmss+UUID8} 天然唯一，可直接复用为渠道 out_request_no。
     * </p>
     */
    private String outRequestNo;
}
