package com.reggie.module.payment.channel;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.reggie.module.payment.channel.real.RealAlipayChannel;
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.reggie.utils.QRCodeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * {@link RealAlipayChannel} 单元测试：用 Mockito 模拟支付宝客户端，
 * 验证 WAP 跳转、NATIVE 预下单二维码、查询状态映射、退款 code=10000 判定（不启动 Spring、不发网络）。
 *
 * @author reggie
 * @since 2026-09-21
 */
class RealAlipayChannelTest {

    private AlipayClient alipayClient;
    private QRCodeUtil qrCodeUtil;
    private RealAlipayChannel channel;

    @BeforeEach
    void setUp() {
        alipayClient = mock(AlipayClient.class);
        qrCodeUtil = mock(QRCodeUtil.class);
        doReturn("data:image/png;base64,BBB").when(qrCodeUtil).generateDataUri(anyString());

        PaymentChannelConfig config = new PaymentChannelConfig();
        config.setAliAppId("2021000000000000");
        config.setPayNotifyUrl("https://example.com/api/payment/notify/ALIPAY");

        channel = new RealAlipayChannel(alipayClient, config, qrCodeUtil);
    }

    @Test
    void wapPayReturnsCashierUrl() throws Exception {
        AlipayTradeWapPayResponse response = new AlipayTradeWapPayResponse();
        response.setBody("https://openapi.alipay.com/gateway.do?alipay_cashier=xxx");
        doReturn(response).when(alipayClient).pageExecute(any(), anyString());

        PayRequest request = new PayRequest();
        request.setTradeNo("T2001");
        request.setAmount(new BigDecimal("88.50"));

        PayResponse result = channel.createOrder(request);
        assertTrue(result.isSuccess());
        assertEquals("H5", result.getPayType());
        assertEquals("https://openapi.alipay.com/gateway.do?alipay_cashier=xxx", result.getPayUrl());
    }

    @Test
    void precreateReturnsQrCode() throws Exception {
        AlipayTradePrecreateResponse response = new AlipayTradePrecreateResponse();
        response.setCode("10000");
        response.setQrCode("https://qr.alipay.com/bax-abcd1234");
        doReturn(response).when(alipayClient).execute(any(AlipayTradePrecreateRequest.class));

        PayRequest request = new PayRequest();
        request.setTradeNo("T2002");
        request.setPayType("NATIVE");
        request.setAmount(new BigDecimal("0.01"));

        PayResponse result = channel.createOrder(request);
        assertTrue(result.isSuccess());
        assertEquals("NATIVE", result.getPayType());
        assertEquals("https://qr.alipay.com/bax-abcd1234", result.getPayUrl());
        assertEquals("data:image/png;base64,BBB", result.getQrCodeUrl());
    }

    @Test
    void queryTradeSuccessIsSuccess() throws Exception {
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setCode("10000");
        response.setTradeStatus("TRADE_SUCCESS");
        response.setTradeNo("2026-alipay-trade-no");
        doReturn(response).when(alipayClient).execute(any(AlipayTradeQueryRequest.class));

        PayResponse result = channel.queryOrder("T2001");
        assertTrue(result.isSuccess());
        assertEquals("2026-alipay-trade-no", result.getChannelTradeNo());
    }

    @Test
    void queryWaitingPayIsNotSuccess() throws Exception {
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setCode("10000");
        response.setTradeStatus("WAIT_BUYER_PAY");
        doReturn(response).when(alipayClient).execute(any(AlipayTradeQueryRequest.class));

        assertFalse(channel.queryOrder("T2001").isSuccess());
    }

    @Test
    void refundCode10000IsSuccess() throws Exception {
        AlipayTradeRefundResponse response = new AlipayTradeRefundResponse();
        response.setCode("10000");
        doReturn(response).when(alipayClient).execute(any(AlipayTradeRefundRequest.class));

        RefundRequest request = new RefundRequest();
        request.setChannelTradeNo("2026-alipay-trade-no");
        request.setAmount(new BigDecimal("0.01"));
        request.setOutRequestNo("RF20260921003");

        RefundResponse result = channel.refund(request);
        assertTrue(result.isSuccess());
    }

    @Test
    void refundBusinessErrorIsNotSuccess() throws Exception {
        AlipayTradeRefundResponse response = new AlipayTradeRefundResponse();
        response.setCode("40004");
        response.setSubCode("ACQ.TRADE_NOT_EXIST");
        doReturn(response).when(alipayClient).execute(any(AlipayTradeRefundRequest.class));

        RefundRequest request = new RefundRequest();
        request.setChannelTradeNo("2026-alipay-trade-no");
        request.setAmount(new BigDecimal("0.01"));
        request.setOutRequestNo("RF20260921004");

        RefundResponse result = channel.refund(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMsg().contains("ACQ.TRADE_NOT_EXIST"));
    }
}
