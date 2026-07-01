package com.reggie.module.payment.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AlipayChannel implements PaymentChannel {

    private static final String TRADE_NO_PREFIX = "ALIPAY_";
    private static final String REFUND_PREFIX = "ALIPAY_REFUND_";
    private static final String PAY_URL_PREFIX = "https://pay.alipay.com/pay/";
    private static final String QR_CODE_URL_PREFIX = "https://qr.alipay.com/";

    @Override
    public PayResponse createOrder(PayRequest request) {
        log.info("Alipay createOrder: tradeNo={}, amount={}, subject={}", request.getTradeNo(), request.getAmount(), request.getSubject());
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo(TRADE_NO_PREFIX + UUID.randomUUID().toString().replace("-", ""));
        response.setPayUrl(PAY_URL_PREFIX + response.getChannelTradeNo());
        response.setQrCodeUrl(QR_CODE_URL_PREFIX + response.getChannelTradeNo());
        return response;
    }

    @Override
    public PayResponse queryOrder(String tradeNo) {
        log.info("Alipay queryOrder: tradeNo={}", tradeNo);
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo(TRADE_NO_PREFIX + tradeNo);
        return response;
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("Alipay refund: channelTradeNo={}, amount={}, reason={}", request.getChannelTradeNo(), request.getAmount(), request.getReason());
        RefundResponse response = new RefundResponse();
        response.setSuccess(true);
        response.setRefundChannelTradeNo(REFUND_PREFIX + UUID.randomUUID().toString().replace("-", ""));
        return response;
    }

    @Override
    public PayResponse handleNotify(Map<String, String> params) {
        log.info("Alipay handleNotify: params={}", params);
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo(params.get("trade_no"));
        return response;
    }
}
