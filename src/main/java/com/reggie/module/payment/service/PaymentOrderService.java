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
}
