package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 支付 ↔ 售后异常衔接测试（第一步）：发起支付状态校验 + 已支付订单取消自动退款。
 *
 * <p>不使用类级 @Transactional：cancelOrder 注册的 afterCommit 自动退款，必须在主事务
 * 真实提交后才触发；数据由 {@link TestDatabaseCleaner} 按租户 999 清理。</p>
 *
 * <p>支付走 mock（test profile 默认 reggie.payment.mock-mode=true），渠道退款同步成功、零网络。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PaymentRefundFlowIntegrationTest extends BaseControllerTest {

    private static final long USER_ID = 990001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("refund_record", "payment_order", "orders");
        // 清限流计数，避免跨用例 429
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (rateLimitKeys != null && !rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
        BaseContext.setCurrentId(USER_ID);
        BaseContext.setCurrentTenantId(999L);
    }

    /** 构造挂在租户 999 的订单。 */
    private void saveOrder(long id, Integer status, String amount) {
        Orders order = new Orders();
        order.setId(id);
        order.setNumber("N" + id);
        order.setUserId(USER_ID);
        order.setStatus(status);
        order.setAmount(new BigDecimal(amount));
        order.setOrderTime(LocalDateTime.now());
        order.setUserName("测试用户");
        order.setPhone("13800138000");
        order.setAddress("测试地址");
        order.setConsignee("收餐人");
        order.setSource("TAKEOUT");
        order.setTenantId(999L);
        orderService.save(order);
    }

    /** 为订单造一条支付单。 */
    private void savePayment(long orderId, String status, String channel, String amount, String tradeNo) {
        PaymentOrder po = new PaymentOrder();
        po.setOrderId(orderId);
        po.setTenantId(999L);
        po.setTradeNo(tradeNo);
        po.setChannel(channel);
        po.setAmount(new BigDecimal(amount));
        po.setStatus(status);
        if ("SUCCESS".equals(status)) {
            po.setPaidTime(LocalDateTime.now());
        }
        paymentOrderService.save(po);
    }

    private int orderStatus(long orderId) {
        Integer s = jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", Integer.class, orderId);
        return s == null ? -1 : s;
    }

    private String paymentStatus(long orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM payment_order WHERE order_id = ? ORDER BY id DESC LIMIT 1",
                String.class, orderId);
    }

    // ==================== 发起支付：状态校验 ====================

    @Test
    void testPayOrderStatusNotAllowed() throws Exception {
        // 订单已是待接单(2)，不再允许发起支付
        saveOrder(990601L, Orders.STATUS_ORDERED, "38.00");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/pay")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":990601,\"channel\":\"ALIPAY\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("订单状态不允许支付"));
    }

    @Test
    void testPayOrderNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/pay")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":999999,\"channel\":\"ALIPAY\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("订单不存在"));
    }

    @Test
    void testPayAlreadySuccessDuplicate() throws Exception {
        // 订单待付款(1)但已存在 SUCCESS 支付单（矛盾数据）→ createPaymentOrder 拒绝
        saveOrder(990602L, Orders.STATUS_PENDING_PAY, "38.00");
        savePayment(990602L, "SUCCESS", "ALIPAY", "38.00", "TR-DUP-990602");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/pay")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":990602,\"channel\":\"ALIPAY\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该订单已支付成功，请勿重复支付"));
    }

    @Test
    void testPayReusePendingOrder() throws Exception {
        // 已有 PENDING 支付单 → 复用同 tradeNo，不新增
        saveOrder(990603L, Orders.STATUS_PENDING_PAY, "38.00");
        savePayment(990603L, "PENDING", "ALIPAY", "38.00", "TR-PEND-990603");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/pay")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":990603,\"channel\":\"ALIPAY\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.tradeNo").value("TR-PEND-990603"));

        // 该订单仍只有 1 条 PENDING 支付单，tradeNo 不变
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_order WHERE order_id = 990603", Integer.class);
        assertEquals(1, cnt == null ? 0 : cnt.intValue());
        assertEquals("PENDING", paymentStatus(990603L));
    }

    @Test
    void testPayZeroAmountAutoSuccess() throws Exception {
        // 0 元订单：不调渠道，支付单(若有)翻成功，订单 1→2
        saveOrder(990604L, Orders.STATUS_PENDING_PAY, "0.00");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/pay")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":990604,\"channel\":\"ALIPAY\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.rawResponse").value("0元订单自动完成支付"));

        assertEquals(Orders.STATUS_ORDERED, orderStatus(990604L));
    }

    // ==================== 已支付订单取消 → 自动退款 ====================

    @Test
    void testUserCancelPaidTriggersAutoRefund() throws Exception {
        // 待接单(2) + SUCCESS 支付单 → userCancel 主事务提交后 afterCommit 自动全额退款
        saveOrder(990605L, Orders.STATUS_ORDERED, "38.00");
        savePayment(990605L, "SUCCESS", "ALIPAY", "38.00", "TR-PAID-990605");

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userCancel")
                .param("id", "990605")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("订单已取消"));

        // afterCommit 同步执行：订单 2→6、支付单 SUCCESS→REFUND、新增 1 条 SUCCESS 退款记录。
        // 注意：财务型 refund_record 不写 order_id，须按 payment_order_id 关联
        assertEquals(Orders.STATUS_REFUNDED, orderStatus(990605L));
        assertEquals("REFUND", paymentStatus(990605L));
        Integer refundCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record rr WHERE rr.status = 'SUCCESS' "
                        + "AND rr.payment_order_id IN (SELECT id FROM payment_order WHERE order_id = 990605)",
                Integer.class);
        assertEquals(1, refundCnt == null ? 0 : refundCnt.intValue());
        String reason = jdbcTemplate.queryForObject(
                "SELECT rr.reason FROM refund_record rr "
                        + "WHERE rr.payment_order_id IN (SELECT id FROM payment_order WHERE order_id = 990605) "
                        + "ORDER BY rr.id LIMIT 1", String.class);
        assertEquals("用户主动取消", reason);
    }

    @Test
    void testUserCancelUnpaidGoesCancelled() throws Exception {
        // 待付款(1)、无支付单 → 直接 1→5，无退款/支付记录
        saveOrder(990606L, Orders.STATUS_PENDING_PAY, "38.00");

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userCancel")
                .param("id", "990606")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("订单已取消"));

        assertEquals(Orders.STATUS_CANCELLED, orderStatus(990606L));
        Integer poCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_order WHERE order_id = 990606", Integer.class);
        assertEquals(0, poCnt == null ? 0 : poCnt.intValue());
        Integer rrCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE order_id = 990606", Integer.class);
        assertEquals(0, rrCnt == null ? 0 : rrCnt.intValue());
    }

    @Test
    void testUserCancelDeliveringRejectedByController() throws Exception {
        // 配送中(3) 不在 userCancel 允许的 1/2 → 控制器拒绝，订单状态不变
        saveOrder(990607L, Orders.STATUS_DELIVERING, "38.00");

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userCancel")
                .param("id", "990607")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("当前状态不允许取消，如需退款请联系客服"));

        assertEquals(Orders.STATUS_DELIVERING, orderStatus(990607L));
    }

    // ==================== 支付回调到达时订单已取消（TOCTOU） ====================

    @Test
    void testNotifyWhenOrderAlreadyCancelled() throws Exception {
        // 订单待付款(1) + PENDING 支付单；支付回调到达前订单已被（并发）取消置 5
        saveOrder(990608L, Orders.STATUS_PENDING_PAY, "38.00");
        savePayment(990608L, "PENDING", "ALIPAY", "38.00", "TR-CANCEL-990608");
        jdbcTemplate.update("UPDATE orders SET status = 5 WHERE id = 990608");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/notify/ALIPAY")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sign\":\"mock\",\"out_trade_no\":\"TR-CANCEL-990608\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 【缺陷·按当前真实行为断言】支付单 PENDING→SUCCESS（回调）→ afterCommit 自动全额退款 → REFUND，
        // 产生 1 条 SUCCESS 退款记录；但订单 status=5 不在退款联动允许的[2,3,4]，
        // RefundServiceImpl.updateOrderOnFullRefund 落入 else 跳过，订单保持 5
        // （资金已退，语义上订单应为已退款6）。疑似状态机缺陷。
        assertEquals(Orders.STATUS_CANCELLED, orderStatus(990608L));
        assertEquals("REFUND", paymentStatus(990608L));
        Integer refundCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record rr WHERE rr.status = 'SUCCESS' "
                        + "AND rr.payment_order_id IN (SELECT id FROM payment_order WHERE order_id = 990608)",
                Integer.class);
        assertEquals(1, refundCnt == null ? 0 : refundCnt.intValue());
    }
}
