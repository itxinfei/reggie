package com.reggie.module.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CsrfTokenUtil;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.dto.PayRequestDTO;
import com.reggie.dto.RefundRequestDTO;
import com.reggie.module.payment.config.PaymentConfigProperties;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.mapper.RefundRecordMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PaymentController 测试 — 聚合支付
 *
 * 测试策略：
 * - schema-payment-controller.sql 在每个测试方法前插入 3 笔业务订单
 * - @TestPropertySource(reggie.payment.mock-mode=true) 开启 mock 验签，回调签名校验恒真
 * - @Transactional 每个测试方法回滚，天然隔离
 * - sessionAttr 注入 employee/tenantId 触发 LoginCheckFilter
 * - POST 写操作通过 withCsrfToken 注入有效 CSRF token
 *
 * 端点覆盖：
 * 1. POST /api/payment/pay — 创建支付单
 * 2. POST /api/payment/notify/{channel} — 支付回调
 * 3. POST /api/payment/refund — 退款
 * 4. GET /api/payment/query/{tradeNo} — 查询支付单
 * 5. GET /api/payment/page — 分页查询
 *
 * @author reggie
 * @since 2026-08-28
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "reggie.payment.mock-mode=true")
@Sql(scripts = "classpath:schema-payment-controller.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class PaymentControllerTest {

    private static final String CSRF_TOKEN_SESSION_KEY = "csrfToken";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private PaymentConfigProperties paymentConfig;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("refund_record", "payment_order");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    // ==================== 创建支付单 ====================

    @Test
    @DisplayName("1. 创建支付单 - ALIPAY 渠道成功")
    void testPay_alipay_success() throws Exception {
        PayRequestDTO dto = new PayRequestDTO();
        dto.setOrderId(200L);
        dto.setChannel("ALIPAY");
        dto.setAmount(new BigDecimal("99.99"));

        mockMvc.perform(withCsrfToken(post("/api/payment/pay")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.success").value(true));

        List<PaymentOrder> list = paymentOrderService.list();
        assertThat(list).hasSize(1);
        PaymentOrder po = list.get(0);
        assertThat(po.getOrderId()).isEqualTo(200L);
        assertThat(po.getChannel()).isEqualTo("ALIPAY");
        assertThat(po.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(po.getStatus()).isEqualTo(PaymentOrder.STATUS_PENDING);
        assertThat(po.getTradeNo()).isNotNull();
    }

    @Test
    @DisplayName("2. 创建支付单 - WECHAT 渠道成功")
    void testPay_wechat_success() throws Exception {
        PayRequestDTO dto = new PayRequestDTO();
        dto.setOrderId(201L);
        dto.setChannel("WECHAT");
        dto.setAmount(new BigDecimal("50.00"));

        mockMvc.perform(withCsrfToken(post("/api/payment/pay")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<PaymentOrder> list = paymentOrderService.list();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getChannel()).isEqualTo("WECHAT");
    }

    @Test
    @DisplayName("3. 创建支付单 - 订单不存在返回错误")
    void testPay_order_not_found() throws Exception {
        PayRequestDTO dto = new PayRequestDTO();
        dto.setOrderId(9999L);
        dto.setChannel("ALIPAY");
        dto.setAmount(new BigDecimal("1.00"));

        mockMvc.perform(withCsrfToken(post("/api/payment/pay")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("订单不存在"));
    }

    // ==================== 支付回调 ====================

    @Test
    @DisplayName("4. 支付回调 - WECHAT 渠道成功")
    void testNotify_wechat_success() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(200L, "WECHAT", new BigDecimal("99.99"));
        String tradeNo = po.getTradeNo();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", tradeNo);
        params.put("transaction_id", "WECHAT_TXN_" + System.currentTimeMillis());
        params.put("total_fee", "9999"); // 分
        params.put("mch_id", "1900000109");
        params.put("sign", "test_sign");

        mockMvc.perform(withCsrfToken(post("/api/payment/notify/WECHAT")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(params))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("回调处理成功"));

        PaymentOrder updated = paymentOrderService.lambdaQuery()
                .eq(PaymentOrder::getTradeNo, tradeNo).one();
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(PaymentOrder.STATUS_SUCCESS);
        assertThat(updated.getChannelTradeNo()).isNotNull();
        assertThat(updated.getPaidTime()).isNotNull();
    }

    @Test
    @DisplayName("5. 支付回调 - ALIPAY 渠道成功")
    void testNotify_alipay_success() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(201L, "ALIPAY", new BigDecimal("50.00"));
        String tradeNo = po.getTradeNo();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", tradeNo);
        params.put("trade_no", "ALIPAY_TXN_" + System.currentTimeMillis());
        params.put("total_amount", "50.00");
        params.put("sign", "test_sign");

        mockMvc.perform(withCsrfToken(post("/api/payment/notify/ALIPAY")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(params))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("回调处理成功"));

        PaymentOrder updated = paymentOrderService.lambdaQuery()
                .eq(PaymentOrder::getTradeNo, tradeNo).one();
        assertThat(updated.getStatus()).isEqualTo(PaymentOrder.STATUS_SUCCESS);
    }

    @Test
    @DisplayName("6. 支付回调 - 缺少 out_trade_no 参数返回错误")
    void testNotify_missing_out_trade_no() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("transaction_id", "TXN_001");
        params.put("sign", "test_sign");

        mockMvc.perform(withCsrfToken(post("/api/payment/notify/WECHAT")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(params))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("回调缺少 out_trade_no 参数"));
    }

    @Test
    @DisplayName("7. 支付回调 - tradeNo 不存在返回错误")
    void testNotify_trade_no_not_found() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", "NOT_EXIST_999");
        params.put("transaction_id", "TXN_002");
        params.put("sign", "test_sign");

        mockMvc.perform(withCsrfToken(post("/api/payment/notify/WECHAT")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(params))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("回调交易号不存在"));
    }

    @Test
    @DisplayName("8. 支付回调 - 渠道不匹配返回错误")
    void testNotify_channel_mismatch() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(201L, "ALIPAY", new BigDecimal("50.00"));
        String tradeNo = po.getTradeNo();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", tradeNo);
        params.put("transaction_id", "WECHAT_TXN_003");
        params.put("sign", "test_sign");

        mockMvc.perform(withCsrfToken(post("/api/payment/notify/WECHAT")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(params))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("支付渠道不匹配"));
    }

    @Test
    @DisplayName("9. 支付回调 - 金额不一致返回错误")
    void testNotify_amount_mismatch() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(201L, "WECHAT", new BigDecimal("50.00"));
        String tradeNo = po.getTradeNo();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", tradeNo);
        params.put("transaction_id", "WECHAT_TXN_004");
        params.put("total_fee", "9999"); // 99.99元 != 50元
        params.put("sign", "test_sign");

        mockMvc.perform(withCsrfToken(post("/api/payment/notify/WECHAT")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(params))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("支付金额不一致"));
    }

    @Test
    @DisplayName("10. 支付回调 - 金额格式非法返回错误")
    void testNotify_amount_format_invalid() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(201L, "WECHAT", new BigDecimal("50.00"));
        String tradeNo = po.getTradeNo();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", tradeNo);
        params.put("transaction_id", "WECHAT_TXN_005");
        params.put("total_fee", "invalid_amount");
        params.put("sign", "test_sign");

        mockMvc.perform(withCsrfToken(post("/api/payment/notify/WECHAT")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(params))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("支付金额格式非法"));
    }

    // ==================== 退款 ====================

    @Test
    @DisplayName("11. 退款 - 全额退款成功")
    void testRefund_full_success() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(200L, "ALIPAY", new BigDecimal("99.99"));
        paymentOrderService.handlePaymentSuccess(po.getTradeNo(), "ALIPAY_CHANNEL_" + po.getTradeNo());

        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setPaymentOrderId(po.getId());
        dto.setAmount(new BigDecimal("99.99"));
        dto.setReason("顾客取消");

        mockMvc.perform(withCsrfToken(post("/api/payment/refund")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<RefundRecord> refunds = refundRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RefundRecord>()
                        .eq("payment_order_id", po.getId()));
        assertThat(refunds).hasSize(1);
        assertThat(refunds.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("12. 退款 - 支付单不存在返回错误")
    void testRefund_payment_order_not_found() throws Exception {
        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setPaymentOrderId(99999L);
        dto.setAmount(new BigDecimal("10.00"));
        dto.setReason("测试");

        mockMvc.perform(withCsrfToken(post("/api/payment/refund")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("支付订单不存在"));
    }

    @Test
    @DisplayName("13. 退款 - 支付状态非成功不允许退款")
    void testRefund_payment_pending_not_allowed() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(201L, "WECHAT", new BigDecimal("50.00"));

        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setPaymentOrderId(po.getId());
        dto.setAmount(new BigDecimal("50.00"));
        dto.setReason("顾客取消");

        mockMvc.perform(withCsrfToken(post("/api/payment/refund")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("14. 退款 - 退款金额超过支付金额返回错误")
    void testRefund_amount_exceeds_payment() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(202L, "ALIPAY", new BigDecimal("200.00"));
        paymentOrderService.handlePaymentSuccess(po.getTradeNo(), "ALIPAY_CHANNEL_" + po.getTradeNo());

        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setPaymentOrderId(po.getId());
        dto.setAmount(new BigDecimal("201.00"));
        dto.setReason("测试超额退款");

        mockMvc.perform(withCsrfToken(post("/api/payment/refund")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("15. 查询支付单 - 成功")
    void testQuery_trade_no_success() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(200L, "ALIPAY", new BigDecimal("99.99"));
        String tradeNo = po.getTradeNo();

        mockMvc.perform(get("/api/payment/query/{tradeNo}", tradeNo)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.tradeNo").value(tradeNo))
                .andExpect(jsonPath("$.data.channel").value("ALIPAY"))
                .andExpect(jsonPath("$.data.orderId").value(200));
    }

    @Test
    @DisplayName("16. 查询支付单 - 不存在返回错误")
    void testQuery_trade_no_not_found() throws Exception {
        mockMvc.perform(get("/api/payment/query/{tradeNo}", "NON_EXISTENT")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("支付订单不存在"));
    }

    // ==================== 分页查询 ====================

    @Test
    @DisplayName("17. 分页查询 - 成功并支持渠道筛选")
    void testPage_success_with_channel_filter() throws Exception {
        paymentOrderService.createPaymentOrder(200L, "ALIPAY", new BigDecimal("99.99"));
        paymentOrderService.createPaymentOrder(201L, "WECHAT", new BigDecimal("50.00"));
        paymentOrderService.createPaymentOrder(202L, "ALIPAY", new BigDecimal("200.00"));

        mockMvc.perform(get("/api/payment/page")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("channel", "ALIPAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    @DisplayName("18. 分页查询 - 状态筛选")
    void testPage_with_status_filter() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(200L, "ALIPAY", new BigDecimal("99.99"));
        paymentOrderService.handlePaymentSuccess(po.getTradeNo(), "CH_" + po.getTradeNo());
        paymentOrderService.createPaymentOrder(201L, "WECHAT", new BigDecimal("50.00"));

        mockMvc.perform(get("/api/payment/page")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("19. 分页查询 - 空结果")
    void testPage_empty() throws Exception {
        mockMvc.perform(get("/api/payment/page")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("20. mock-mode 确认 - 验签跳过")
    void testMockMode_enabled() {
        assertThat(paymentConfig.isMockMode()).isTrue();
    }

    // ==================== Helper Methods ====================

    private MockHttpServletRequestBuilder withCsrfToken(MockHttpServletRequestBuilder request) {
        String token = CsrfTokenUtil.generateToken();
        return request
                .sessionAttr(CSRF_TOKEN_SESSION_KEY, token)
                .header(CSRF_HEADER_NAME, token);
    }

    private String toJson(Object obj) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }
}