package com.reggie.module.payment.channel.notify;

/**
 * 真实渠道的回调解析能力（与 mock 的表单回调 {@code MapNotifyCapable} 对应）。
 * <p>由微信 APIv3 / 支付宝真实渠道实现：用各自 SDK 完成验签、解密/解析。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
public interface RealNotifyCapable {

    /** 解析并验签支付回调。 */
    NotifyResult parsePayment(NotifyRequest request);

    /** 解析并验签退款回调。 */
    RefundNotifyResult parseRefund(NotifyRequest request);
}
