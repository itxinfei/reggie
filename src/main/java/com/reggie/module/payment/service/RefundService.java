package com.reggie.module.payment.service;

import java.math.BigDecimal;

/**
 * 退款服务：提供"按业务订单全额退款"能力。
 * <p>
 * 供订单取消/拒单等流程自动调用（资金闭环：已支付订单取消时必须退款）。
 * 区别于 {@code PaymentController.refund}（员工手动按支付单退款），本服务按业务订单查找
 * 已成功支付的支付单并全额退款，无 SUCCESS 支付单时直接返回 false（未支付无需退款）。
 * </p>
 *
 * @author reggie
 * @since 2026-08-30
 */
public interface RefundService {

    /**
     * 按业务订单全额退款（余额退）。
     * <p>
     * 幂等安全：已存在全额退款记录或支付单非 SUCCESS 时直接返回 false，不会重复退款。
     * 渠道调用在事务外执行（外部 HTTP 不应被事务包裹），本地落库由内部事务保证。
     * </p>
     *
     * @param orderId 业务订单ID
     * @param reason  退款原因（会落库到退款记录）
     * @return true=退款已成功发起并完成本地记账；false=无已支付支付单或退款失败（调用方应记录告警）
     */
    boolean refundByOrder(Long orderId, String reason);

    /**
     * 判断是否为可走渠道 API 的在线支付通道（微信/支付宝）。
     * 现金/银行卡/储值/货到付款等线下通道返回 false（须走本地手动退款记账）。
     *
     * @param channel 渠道
     * @return 是否在线通道
     */
    boolean isOnlineChannel(String channel);

    /**
     * 按支付单ID执行线下支付本地手动退款（现金/银行卡/储值/货到付款）。
     * <p>由人工完成实际退款，系统仅做本地记账闭环：标记支付单 REFUND + 订单 REFUNDED + 回退会员权益。
     * 供 {@code PaymentController.refund}（员工手动退款）等路径复用，避免 getChannel 抛“不支持的支付通道”。</p>
     *
     * @return true=记账成功；false=支付单不存在/非 SUCCESS/记账失败
     */
    boolean refundOfflineByPaymentOrderId(Long paymentOrderId, BigDecimal amount, String reason);
}
