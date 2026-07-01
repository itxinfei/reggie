package com.reggie.module.payment.channel;

import java.util.Map;

public interface PaymentChannel {
    PayResponse createOrder(PayRequest request);
    PayResponse queryOrder(String tradeNo);
    RefundResponse refund(RefundRequest request);
    PayResponse handleNotify(Map<String, String> params);
}
