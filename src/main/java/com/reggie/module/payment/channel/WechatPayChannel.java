package com.reggie.module.payment.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class WechatPayChannel implements PaymentChannel {

    @Override
    public PayResponse createOrder(PayRequest request) {
        log.info("WechatPay createOrder: tradeNo={}, amount={}, subject={}", request.getTradeNo(), request.getAmount(), request.getSubject());
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo("WECHAT_" + UUID.randomUUID().toString().replace("-", ""));
        response.setPayUrl("https://pay.weixin.qq.com/pay/" + response.getChannelTradeNo());
        response.setQrCodeUrl("https://qr.weixin.qq.com/" + response.getChannelTradeNo());
        return response;
    }

    @Override
    public PayResponse queryOrder(String tradeNo) {
        log.info("WechatPay queryOrder: tradeNo={}", tradeNo);
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo("WECHAT_" + tradeNo);
        return response;
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("WechatPay refund: channelTradeNo={}, amount={}, reason={}", request.getChannelTradeNo(), request.getAmount(), request.getReason());
        RefundResponse response = new RefundResponse();
        response.setSuccess(true);
        response.setRefundChannelTradeNo("WECHAT_REFUND_" + UUID.randomUUID().toString().replace("-", ""));
        return response;
    }

    @Override
    public PayResponse handleNotify(Map<String, String> params) {
        log.info("WechatPay handleNotify: params={}", params);
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo(params.get("trade_no"));
        return response;
    }
}
