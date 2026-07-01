package com.reggie.module.payment.channel;

import com.reggie.common.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentChannelFactory {

    @Autowired
    private AlipayChannel alipayChannel;

    @Autowired
    private WechatPayChannel wechatPayChannel;

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
