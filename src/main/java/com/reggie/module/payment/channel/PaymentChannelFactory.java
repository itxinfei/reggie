package com.reggie.module.payment.channel;

import com.reggie.common.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 支付渠道工厂，根据渠道类型返回对应的支付适配器实例。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Component
public class PaymentChannelFactory {

    /** 支付宝支付渠道 */
    @Autowired
    private AlipayChannel alipayChannel;

    /** 微信支付渠道 */
    @Autowired
    private WechatPayChannel wechatPayChannel;

    /**
     * 根据渠道类型获取对应的支付适配器
     *
     * @param channel 渠道类型（ALIPAY/WECHAT）
     * @return 支付适配器实例
     * @throws CustomException 当渠道类型不支持时抛出
     */
    public PaymentChannel getChannel(String channel) {
        if (channel == null) {
            throw new CustomException("支付通道不能为空");
        }
        switch (channel.toUpperCase()) {
            case "ALIPAY":
                return alipayChannel;
            case "WECHAT":
                return wechatPayChannel;
            default:
                throw new CustomException("不支持的支付通道: " + channel);
        }
    }

    /**
     * 根据渠道类型获取对应的支付适配器（回调场景专用）
     * <p>
     * 与 {@link #getChannel(String)} 的区别：未知/空渠道返回 null 而非抛异常。
     * 回调场景下若抛异常会被全局异常处理器转为 HTTP 500，支付平台会持续重试；
     * 返回 null 后由调用方返回 200 + 明确失败码，主动停止平台重试。
     * </p>
     *
     * @param channel 渠道类型（ALIPAY/WECHAT）
     * @return 支付适配器实例，未知/空渠道返回 null
     */
    public PaymentChannel getChannelNullable(String channel) {
        if (channel == null) {
            return null;
        }
        switch (channel.toUpperCase()) {
            case "ALIPAY":
                return alipayChannel;
            case "WECHAT":
                return wechatPayChannel;
            default:
                return null;
        }
    }
}
