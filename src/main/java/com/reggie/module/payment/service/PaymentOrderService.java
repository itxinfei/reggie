package com.reggie.module.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.payment.model.PaymentOrder;
import java.math.BigDecimal;

public interface PaymentOrderService extends IService<PaymentOrder> {
    PaymentOrder createPaymentOrder(Long orderId, String channel, BigDecimal amount);
    void handlePaymentSuccess(String tradeNo, String channelTradeNo);
    void handlePaymentFail(String tradeNo, String errorMsg);
}
