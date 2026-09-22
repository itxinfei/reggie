package com.reggie.module.payment.channel;

/**
 * <p>
 * 支付渠道接口（策略模式），定义与第三方支付平台收款/退款主流程的交互规范。
 * </p>
 * <p>
 * 异步通知的解析与验签已拆分到 {@link MapNotifyCapable}（存量表单/mock 渠道）
 * 或独立通知解析器（真实 APIv3 渠道），本接口只聚焦下单、查询、退款。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
public interface PaymentChannel {
    /**
     * 创建支付订单
     *
     * @param request 支付请求参数
     * @return 支付响应
     */
    PayResponse createOrder(PayRequest request);
    /**
     * 查询订单状态
     *
     * @param tradeNo 交易号
     * @return 支付响应
     */
    PayResponse queryOrder(String tradeNo);
    /**
     * 退款
     *
     * @param request 退款请求参数
     * @return 退款响应
     */
    RefundResponse refund(RefundRequest request);
}
