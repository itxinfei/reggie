package com.reggie.module.payment.channel;

import com.reggie.module.payment.channel.real.WechatV3PayChannel;
import com.reggie.module.payment.channel.sdk.WechatSdkClients;
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import com.reggie.utils.QRCodeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link WechatV3PayChannel} 单元测试：用 Mockito 模拟微信 SDK 客户端，
 * 验证 NATIVE 下单二维码、查询状态映射、退款反查原单总额与状态映射（不启动 Spring、不发真实网络）。
 *
 * @author reggie
 * @since 2026-09-21
 */
class WechatV3PayChannelTest {

    private NativePayService nativePayService;
    private WechatSdkClients clients;
    private WechatV3PayChannel channel;

    @BeforeEach
    void setUp() {
        nativePayService = mock(NativePayService.class);
        clients = mock(WechatSdkClients.class);
        when(clients.getNativePayService()).thenReturn(nativePayService);

        QRCodeUtil qrCodeUtil = mock(QRCodeUtil.class);
        when(qrCodeUtil.generateDataUri(anyString())).thenReturn("data:image/png;base64,AAA");

        PaymentChannelConfig config = new PaymentChannelConfig();
        config.setWxAppId("wx123456");
        config.setWxMchId("1900000000");
        config.setPayNotifyUrl("https://example.com/api/payment/notify/WECHAT");
        config.setRefundNotifyUrl("https://example.com/api/payment/refund-notify/WECHAT");

        channel = new WechatV3PayChannel(clients, config, qrCodeUtil);
    }

    @Test
    void nativeOrderReturnsCodeUrlAndQrDataUri() {
        PrepayResponse prepayResponse = new PrepayResponse();
        prepayResponse.setCodeUrl("weixin://wxpay/bizpayurl?pr=abc123");
        when(nativePayService.prepay(any())).thenReturn(prepayResponse);

        PayRequest request = new PayRequest();
        request.setTradeNo("T1001");
        request.setSubject("瑞吉外卖-订单T1001");
        request.setAmount(new BigDecimal("0.01"));

        PayResponse response = channel.createOrder(request);
        assertTrue(response.isSuccess());
        assertEquals("NATIVE", response.getPayType());
        assertEquals("weixin://wxpay/bizpayurl?pr=abc123", response.getPayUrl());
        assertEquals("data:image/png;base64,AAA", response.getQrCodeUrl());
    }

    @Test
    void querySuccessMapsTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTradeState(Transaction.TradeStateEnum.SUCCESS);
        transaction.setTransactionId("4200-wx-transaction-id");
        when(nativePayService.queryOrderByOutTradeNo(any())).thenReturn(transaction);

        PayResponse response = channel.queryOrder("T1001");
        assertTrue(response.isSuccess());
        assertEquals("4200-wx-transaction-id", response.getChannelTradeNo());
    }

    @Test
    void queryNotPayIsNotSuccess() {
        Transaction transaction = new Transaction();
        transaction.setTradeState(Transaction.TradeStateEnum.NOTPAY);
        when(nativePayService.queryOrderByOutTradeNo(any())).thenReturn(transaction);

        PayResponse response = channel.queryOrder("T1001");
        assertFalse(response.isSuccess());
    }

    @Test
    void refundSuccessUsesQueriedOriginalTotal() {
        // 反查原单总额：1.00 元 = 100 分
        Transaction paidTransaction = new Transaction();
        TransactionAmount transactionAmount = new TransactionAmount();
        transactionAmount.setTotal(100);
        paidTransaction.setAmount(transactionAmount);
        when(nativePayService.queryOrderById(any())).thenReturn(paidTransaction);

        RefundService refundService = mock(RefundService.class);
        when(clients.getRefundService()).thenReturn(refundService);
        Refund refund = new Refund();
        refund.setRefundId("5000-wx-refund-id");
        refund.setStatus(Status.SUCCESS);
        when(refundService.create(any())).thenReturn(refund);

        RefundRequest request = new RefundRequest();
        request.setChannelTradeNo("4200-wx-transaction-id");
        request.setAmount(new BigDecimal("0.01"));
        request.setOutRequestNo("RF20260921001");

        RefundResponse response = channel.refund(request);
        assertTrue(response.isSuccess());
        assertEquals("5000-wx-refund-id", response.getRefundChannelTradeNo());
    }

    @Test
    void refundWithoutTransactionIdIsRejected() {
        RefundRequest request = new RefundRequest();
        request.setAmount(new BigDecimal("0.01"));
        request.setOutRequestNo("RF20260921002");

        RefundResponse response = channel.refund(request);
        assertFalse(response.isSuccess());
    }
}
