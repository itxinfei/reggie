package com.reggie.module.payment.channel;

import java.util.Map;

/**
 * <p>
 * 表单（APIv2）回调能力接口。承载支付结果异步通知的解析与验签，
 * 与 {@link PaymentChannel} 的收款/退款主流程能力分离。
 * </p>
 * <p>
 * 存量 mock 渠道（{@link WechatPayChannel}、{@link AlipayChannel}）实现本接口，
 * 沿用其原有验签逻辑；真实渠道（APIv3）的回调改由独立的通知解析器处理。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
public interface MapNotifyCapable {

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
