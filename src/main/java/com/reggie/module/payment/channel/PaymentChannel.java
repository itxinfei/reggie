package com.reggie.module.payment.channel;

import java.util.Map;

/**
 * <p>
 * 支付渠道接口（策略模式），定义与第三方支付平台的交互规范。
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
    /**
     * 处理支付回调通知
     *
     * @param params 回调参数
     * @return 支付响应
     */
    PayResponse handleNotify(Map<String, String> params);

    /**
     * 校验支付回调通知签名（防回调伪造）。
     * <p>
     * 回调接口为外部无登录态请求，必须先校验签名再处理业务，禁止直接信任回调参数。
     * 生产环境必须使用渠道官方 SDK 的签名校验（如支付宝 {@code AlipaySignature.rsaCheckV1}、
     * 微信 {@code WxPayUtil.verifyNotifySign}）配合平台公钥/密钥，严禁返回恒真。
     * </p>
     *
     * @param params 回调参数
     * @return true=签名校验通过；false=校验失败，调用方应拒绝处理
     */
    boolean verifyNotifySign(Map<String, String> params);
}