package com.reggie.module.payment;

import com.reggie.common.BaseContext;
import com.reggie.module.payment.channel.*;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = "classpath:schema-payment.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PaymentServiceTest {

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private PaymentChannelFactory paymentChannelFactory;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testCreatePaymentOrder() {
        PaymentOrder po = paymentOrderService.createPaymentOrder(100L, "ALIPAY", new BigDecimal("99.99"));
        assertNotNull(po.getId());
        assertNotNull(po.getTradeNo());
        assertEquals(100L, po.getOrderId().longValue());
        assertEquals("ALIPAY", po.getChannel());
        assertEquals(0, po.getAmount().compareTo(new BigDecimal("99.99")));
        assertEquals("PENDING", po.getStatus());
        assertEquals(1L, po.getTenantId().longValue());
    }

    @Test
    void testPayOrder() {
        PaymentOrder po = paymentOrderService.createPaymentOrder(101L, "WECHAT", new BigDecimal("50.00"));
        assertNotNull(po.getTradeNo());

        PaymentChannel channel = paymentChannelFactory.getChannel("WECHAT");
        PayRequest request = new PayRequest();
        request.setTradeNo(po.getTradeNo());
        request.setAmount(new BigDecimal("50.00"));
        request.setSubject("瑞吉外卖-订单101");
        PayResponse payResponse = channel.createOrder(request);
        assertTrue(payResponse.isSuccess());
        assertNotNull(payResponse.getChannelTradeNo());

        paymentOrderService.handlePaymentSuccess(po.getTradeNo(), payResponse.getChannelTradeNo());
        PaymentOrder updated = paymentOrderService.lambdaQuery()
            .eq(PaymentOrder::getTradeNo, po.getTradeNo()).one();
        assertEquals("SUCCESS", updated.getStatus());
        assertNotNull(updated.getPaidTime());
    }

    @Test
    void testRefund() {
        PaymentOrder po = paymentOrderService.createPaymentOrder(102L, "ALIPAY", new BigDecimal("200.00"));
        PaymentChannel channel = paymentChannelFactory.getChannel("ALIPAY");
        PayRequest request = new PayRequest();
        request.setTradeNo(po.getTradeNo());
        request.setAmount(new BigDecimal("200.00"));
        request.setSubject("瑞吉外卖-订单102");
        PayResponse payResponse = channel.createOrder(request);
        paymentOrderService.handlePaymentSuccess(po.getTradeNo(), payResponse.getChannelTradeNo());

        PaymentOrder paid = paymentOrderService.lambdaQuery()
            .eq(PaymentOrder::getTradeNo, po.getTradeNo()).one();
        assertEquals("SUCCESS", paid.getStatus());

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setChannelTradeNo(paid.getChannelTradeNo());
        refundRequest.setAmount(new BigDecimal("200.00"));
        refundRequest.setReason("测试退款");
        RefundResponse refundResponse = channel.refund(refundRequest);
        assertTrue(refundResponse.isSuccess());
        assertNotNull(refundResponse.getRefundChannelTradeNo());

        RefundRecord record = new RefundRecord();
        record.setPaymentOrderId(paid.getId());
        record.setRefundNo("RF" + System.currentTimeMillis());
        record.setAmount(new BigDecimal("200.00"));
        record.setReason("测试退款");
        record.setStatus("SUCCESS");
        refundRecordService.save(record);

        List<RefundRecord> records = refundRecordService.list();
        assertEquals(1, records.size());
        assertEquals(paid.getId(), records.get(0).getPaymentOrderId());
    }

    @Test
    void testAlipayChannelCreate() {
        PaymentChannel alipay = paymentChannelFactory.getChannel("ALIPAY");
        PayRequest request = new PayRequest();
        request.setTradeNo("TEST20260701000001");
        request.setAmount(new BigDecimal("88.00"));
        request.setSubject("测试商品");

        PayResponse response = alipay.createOrder(request);
        assertTrue(response.isSuccess());
        assertNotNull(response.getChannelTradeNo());
        assertTrue(response.getChannelTradeNo().startsWith("ALIPAY_"));
        assertNotNull(response.getPayUrl());
        assertNotNull(response.getQrCodeUrl());
    }
}

