package com.reggie.module.payment.channel;

import com.reggie.common.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 支付渠道工厂
 * 根据渠道类型返回对应的支付适配器实例
 *
 * @author reggie
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
}
