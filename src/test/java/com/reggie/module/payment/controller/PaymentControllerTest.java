package com.reggie.module.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CsrfTokenUtil;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.dto.PayRequestDTO;
import com.reggie.dto.RefundRequestDTO;
import com.reggie.enums.RefundStatus;
import com.reggie.module.payment.config.PaymentConfigProperties;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.mapper.RefundRecordMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.RefundRecordService;
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
import java.time.LocalDateTime;
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
    private RefundRecordService refundRecordService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("refund_record", "payment_order");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(999L);
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
    @DisplayName("4b. 退款回调 - mock 模式直接回渠道成功 ACK，不产生副作用")
    void testRefundNotify_mockMode_ackSuccess() throws Exception {
        // mock 模式不存在真实退款回调：端点应直接回微信原生成功 ACK，且不触渠道/不联动
        mockMvc.perform(withCsrfToken(post("/api/payment/refund-notify/WECHAT")
                        .contentType("application/json")
                        .content("{}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L))
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
                        .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("支付订单不存在"));
    }

    // ==================== C 端用户查询（/user/query/{tradeNo}）====================

    @Test
    @DisplayName("16a. 用户查询支付状态 - 本人订单成功返回精简字段")
    void testUserQuery_owner_success() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(200L, "WECHAT", new BigDecimal("99.99"));

        mockMvc.perform(get("/api/payment/user/query/{tradeNo}", po.getTradeNo())
                        .sessionAttr("user", 1L)
                        .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.orderId").value(200))
                .andExpect(jsonPath("$.data.channel").value("WECHAT"))
                .andExpect(jsonPath("$.data.mockMode").value(true));
    }

    @Test
    @DisplayName("16b. 用户查询支付状态 - 非本人订单拒绝")
    void testUserQuery_not_owner_rejected() throws Exception {
        PaymentOrder po = paymentOrderService.createPaymentOrder(200L, "WECHAT", new BigDecimal("99.99"));

        // MockMvc 不执行 @WebFilter（非 Spring Bean），登录上下文按用户态显式设置：
        // schema 中订单 200 的 user_id=1，以用户 2 的身份查询应被归属校验拒绝
        BaseContext.setCurrentId(2L);
        mockMvc.perform(get("/api/payment/user/query/{tradeNo}", po.getTradeNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("支付订单不存在"));
    }

    @Test
    @DisplayName("16c. 用户查询支付状态 - tradeNo 不存在返回错误")
    void testUserQuery_trade_no_not_found() throws Exception {
        mockMvc.perform(get("/api/payment/user/query/{tradeNo}", "NON_EXISTENT")
                        .sessionAttr("user", 1L)
                        .sessionAttr("tenantId", 999L))
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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
                        .sessionAttr("tenantId", 999L)
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

    @Test
    @DisplayName("21. 退款分析 - 不传日期为累计口径，传日期按 createdTime 区间过滤")
    void testRefundStats_dateRange() throws Exception {
        // 3 笔成功退款，分布在 9-01 / 9-10 / 9-20，金额递增便于断言
        insertTestRefund(901L, "2026-09-01T10:00:00", "100.00", "口味问题");
        insertTestRefund(902L, "2026-09-10T10:00:00", "200.00", "配送超时");
        insertTestRefund(903L, "2026-09-20T10:00:00", "300.00", "不想要了");

        // 累计口径：3 笔、成功退款总额 600
        mockMvc.perform(get("/api/payment/refund/stats")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.successCount").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(600.00));

        // 区间 9-05 ~ 9-15：仅命中 9-10 的 200
        mockMvc.perform(get("/api/payment/refund/stats")
                        .param("startDate", "2026-09-05")
                        .param("endDate", "2026-09-15")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(200.00));

        // 单日 9-01：命中 100（验证同一天起止边界包含）
        mockMvc.perform(get("/api/payment/refund/stats")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-01")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(100.00));
    }

    @Test
    @DisplayName("22. sumRefundBetween - 按创建时间区间聚合成功退款金额与笔数")
    void testSumRefundBetween() {
        insertTestRefund(911L, "2026-09-01T10:00:00", "100.00", "原因一");
        insertTestRefund(912L, "2026-09-10T10:00:00", "200.00", "原因二");

        // 全区间：2 笔合计 300
        Map<String, Object> all = refundRecordService.sumRefundBetween(999L,
                LocalDateTime.parse("2026-09-01T00:00:00"),
                LocalDateTime.parse("2026-09-30T23:59:59"));
        assertThat(((BigDecimal) all.get("amount")).compareTo(new BigDecimal("300.00"))).isEqualTo(0);
        assertThat(all.get("count")).isEqualTo(2);

        // 子区间 9-05~9-15：仅命中 200
        Map<String, Object> part = refundRecordService.sumRefundBetween(999L,
                LocalDateTime.parse("2026-09-05T00:00:00"),
                LocalDateTime.parse("2026-09-15T23:59:59"));
        assertThat(((BigDecimal) part.get("amount")).compareTo(new BigDecimal("200.00"))).isEqualTo(0);
        assertThat(part.get("count")).isEqualTo(1);

        // 不匹配区间：0 笔
        Map<String, Object> none = refundRecordService.sumRefundBetween(999L,
                LocalDateTime.parse("2026-10-01T00:00:00"),
                LocalDateTime.parse("2026-10-31T23:59:59"));
        assertThat(((BigDecimal) none.get("amount")).compareTo(BigDecimal.ZERO)).isEqualTo(0);
        assertThat(none.get("count")).isEqualTo(0);
    }

    // ==================== Helper Methods ====================

    // 插入一笔成功退款（手动指定 createdTime，绕过自动填充的当前时间）
    private void insertTestRefund(Long id, String createdTimeIso, String amount, String reason) {
        RefundRecord r = new RefundRecord();
        r.setId(id);
        r.setTenantId(999L);
        r.setRefundNo("RF-TEST-" + id);
        r.setPaymentOrderId(80000L + id);
        r.setOrderId(90000L + id);
        r.setAmount(new BigDecimal(amount));
        r.setReason(reason);
        r.setStatus(RefundStatus.SUCCESS.getCode());
        r.setCreatedTime(LocalDateTime.parse(createdTimeIso));
        refundRecordMapper.insert(r);
    }

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