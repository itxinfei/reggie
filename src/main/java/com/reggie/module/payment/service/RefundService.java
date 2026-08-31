package com.reggie.module.payment.service;

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
}
