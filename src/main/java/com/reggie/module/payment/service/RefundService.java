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

    /**
     * 员工按支付单手动退款（支持部分退款）——退款编排的唯一实现，收敛自 {@code PaymentController.refund}。
     * <p>编排：租户/状态/金额校验 → 离线通道走本地记账；在线通道走「Redis 锁 → 渠道 HTTP（事务外）→
     * REQUIRED 编程式事务落库（行锁二次校验 + 新建退款记录 + 全额联动）」。渠道调用不被事务包裹。</p>
     *
     * @param tenantId       当前会话租户（null 视为越权，fail-closed）
     * @param paymentOrderId 支付单ID
     * @param refundAmount   本次退款金额（须 &lt;= 支付金额、&lt;= 剩余可退额）
     * @param reason         退款原因
     * @return 成功文案（终态成功 / 处理中 / 线下记账成功），供 Controller 包装为 {@code R.success}
     * @throws com.reggie.common.CustomException 校验失败、渠道拒绝/异常、本地落库失败，供 Controller 捕获转 {@code R.error}
     */
    String refundByPaymentOrder(Long tenantId, Long paymentOrderId, BigDecimal refundAmount, String reason);

    /**
     * 售后单审核通过后触发渠道退款——退款编排的唯一实现，收敛自 {@code PaymentController.executeUserRefund}。
     * <p>复用既有售后单退款号（不新建同号财务记录）：「Redis 锁 → 渠道 HTTP（事务外）→ REQUIRED 编程式事务落库
     *（售后单本身 processing→SUCCESS + 全额联动支付单 REFUND + 权益/库存回退）」。</p>
     *
     * @param tenantId 当前会话租户
     * @param refundId 售后记录ID（状态须为 PROCESSING）
     * @return 成功文案（终态成功 / 处理中 / 已退款幂等提示 / 渠道已处理请核对），供 Controller 包装为 {@code R.success}
     * @throws com.reggie.common.CustomException 记录不存在、越权、状态不符、无有效支付单、渠道拒绝/异常
     */
    String executeUserRefundByRecord(Long tenantId, Long refundId);
}
