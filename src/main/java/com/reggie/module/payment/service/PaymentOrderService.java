package com.reggie.module.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.payment.model.PaymentOrder;
import java.math.BigDecimal;

/**
 * <p>
 * 支付订单服务接口
 * </p>
 * <p>提供支付订单创建、支付成功/失败回调处理等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface PaymentOrderService extends IService<PaymentOrder> {

    /**
     * 创建支付订单
     *
     * @param orderId 关联业务订单ID
     * @param channel 支付渠道（如微信、支付宝）
     * @param amount  支付金额
     * @return 支付订单
     */
    PaymentOrder createPaymentOrder(Long orderId, String channel, BigDecimal amount);

    /**
     * 创建通用业务支付单（会员充值等非订单业务）。
     *
     * @param bizType 业务类型（如 {@link PaymentOrder#BIZ_RECHARGE}）
     * @param bizId   业务单ID（RECHARGE 时为充值记录ID）
     * @param channel 支付渠道（WECHAT/ALIPAY）
     * @param amount  支付金额
     * @return 支付订单
     */
    PaymentOrder createPaymentOrderForBiz(String bizType, Long bizId, String channel, BigDecimal amount);

    /**
     * 处理支付成功回调
     *
     * @param tradeNo       内部交易号
     * @param channelTradeNo 第三方平台交易号
     */
    void handlePaymentSuccess(String tradeNo, String channelTradeNo);

    /**
     * 处理支付失败回调
     *
     * @param tradeNo  内部交易号
     * @param errorMsg 错误信息
     */
    void handlePaymentFail(String tradeNo, String errorMsg);

    /**
     * 支付回调专用：按交易号查询支付订单（忽略租户拦截，调用方需自行处理租户上下文）。
     *
     * @param tradeNo 交易号
     * @return 支付订单
     */
    PaymentOrder selectByTradeNoIgnoreTenant(String tradeNo);

    /**
     * 0 元订单自动完成支付（优惠券/折扣把实付压到 0，无需调用渠道）。
     * <p>收敛自 PaymentController.pay 的 0 元分支：原实现直接双表 lambdaUpdate 无事务，支付单与订单
     * 可能部分更新（支付单已 SUCCESS 而订单仍待付款）。此处以单一事务原子更新：PENDING 支付单（若有）翻
     * SUCCESS + 待付款订单 1→2。归属/状态校验仍由调用方完成。</p>
     *
     * @param orderId 业务订单ID
     */
    void completeZeroAmountPayment(Long orderId);
}
