package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundRecordService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 售后审核 → 执行退款链路测试：{@code /api/payment/refund/user/audit}、{@code /execute}。
 *
 * <p>审核/执行均为员工操作（@RequireEmployee，由 EmployeeGuardAspect 切面校验，
 * MockMvc 下 AOP 生效，故带 employee 会话）。用户申请用 Service 直调以拿到售后记录 ID。</p>
 *
 * <p>【已确认缺陷】execute 在当前 MySQL（refund_record.uk_refund_no）下：渠道退款已成功，
 * 但本地用售后单同号再插财务记录撞唯一键，售后单卡 processing、订单停 4、支付单仍 SUCCESS，
 * 仅留一条 [对账待办] 痕迹。见 testExecuteProcessingHitsDefect。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RefundAuditExecuteControllerTest extends BaseControllerTest {

    private static final long USER_ID = 990001L;
    private static final long EMP_ID = 1L;
    private static final long ORDER_ID = 990701L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("refund_record", "payment_order", "orders");
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (rateLimitKeys != null && !rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
        BaseContext.setCurrentTenantId(999L);
    }

    /** 造已完成订单(4) + SUCCESS 支付单（申请售后的前置）。 */
    private void seedCompletedOrder() {
        Orders order = new Orders();
        order.setId(ORDER_ID);
        order.setNumber("N" + ORDER_ID);
        order.setUserId(USER_ID);
        order.setStatus(Orders.STATUS_COMPLETED);
        order.setAmount(new BigDecimal("38.00"));
        order.setOrderTime(LocalDateTime.now());
        order.setUserName("测试用户");
        order.setPhone("13800138000");
        order.setSource("TAKEOUT");
        order.setTenantId(999L);
        orderService.save(order);

        PaymentOrder po = new PaymentOrder();
        po.setOrderId(ORDER_ID);
        po.setTenantId(999L);
        po.setTradeNo("TR-990701");
        po.setChannelTradeNo("CHT-990701");
        po.setChannel("ALIPAY");
        po.setAmount(new BigDecimal("38.00"));
        po.setStatus("SUCCESS");
        po.setPaidTime(LocalDateTime.now());
        paymentOrderService.save(po);
    }

    /** 扮用户提交售后，返回 PENDING 售后记录。 */
    private RefundRecord applyAsUser() {
        BaseContext.setCurrentId(USER_ID);
        return refundRecordService.applyUserRefund(ORDER_ID, "售后原因");
    }

    /** 切换为员工身份（审核/执行的 BaseContext）。 */
    private void asEmployee() {
        BaseContext.setCurrentId(EMP_ID);
    }

    private String refundStatus(long refundId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM refund_record WHERE id = ?", String.class, refundId);
    }

    // ==================== 审核 ====================

    @Test
    void testAuditApprove() throws Exception {
        seedCompletedOrder();
        long refundId = applyAsUser().getId();
        asEmployee();

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/refund/user/audit")
                .param("refundId", String.valueOf(refundId))
                .param("approve", "true")
                .sessionAttr("employee", EMP_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("审核通过"));

        // pending → processing
        assertEquals("processing", refundStatus(refundId));
    }

    @Test
    void testAuditRejectWithoutReason() throws Exception {
        seedCompletedOrder();
        long refundId = applyAsUser().getId();
        asEmployee();

        // 拒绝但不填原因 → Controller catch 后 HTTP 200/code=0
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/refund/user/audit")
                .param("refundId", String.valueOf(refundId))
                .param("approve", "false")
                .sessionAttr("employee", EMP_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("拒绝时必须填写拒绝原因"));

        // 状态仍是 pending（未改动）
        assertEquals("pending", refundStatus(refundId));
    }

    @Test
    void testAuditRejectWithReason() throws Exception {
        seedCompletedOrder();
        long refundId = applyAsUser().getId();
        asEmployee();

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/refund/user/audit")
                .param("refundId", String.valueOf(refundId))
                .param("approve", "false")
                .param("rejectReason", "超时送达")
                .sessionAttr("employee", EMP_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("已拒绝"));

        assertEquals("rejected", refundStatus(refundId));
        String rejectReason = jdbcTemplate.queryForObject(
                "SELECT reject_reason FROM refund_record WHERE id = ?", String.class, refundId);
        assertEquals("超时送达", rejectReason);
    }

    @Test
    void testReapplyAfterRejected() throws Exception {
        seedCompletedOrder();
        long firstId = applyAsUser().getId();
        asEmployee();
        // 第一次被拒
        refundRecordService.auditUserRefund(firstId, false, "证据不足");
        assertEquals("rejected", refundStatus(firstId));

        // 拒绝后用户可重新申请：新 PENDING 记录，旧 rejected 保留
        RefundRecord second = applyAsUser();
        assertEquals("pending", refundStatus(second.getId()));
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE payment_order_id IN "
                        + "(SELECT id FROM payment_order WHERE order_id = ?)", Integer.class, ORDER_ID);
        // 旧 rejected + 新 pending = 2
        assertEquals(2, total == null ? 0 : total.intValue());
    }

    @Test
    void testAuditDuplicate() throws Exception {
        seedCompletedOrder();
        long refundId = applyAsUser().getId();
        asEmployee();
        // 已审核通过
        refundRecordService.auditUserRefund(refundId, true, null);

        // 再次审核 → 已处理，不可重复
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/refund/user/audit")
                .param("refundId", String.valueOf(refundId))
                .param("approve", "true")
                .sessionAttr("employee", EMP_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该售后记录已处理，不可重复审核"));
    }

    @Test
    void testAuditRecordNotFound() throws Exception {
        asEmployee();
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/refund/user/audit")
                .param("refundId", "999999")
                .param("approve", "true")
                .sessionAttr("employee", EMP_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("售后记录不存在"));
    }

    // ==================== 执行退款 ====================

    @Test
    void testExecuteProcessingHitsDefect() throws Exception {
        seedCompletedOrder();
        long refundId = applyAsUser().getId();
        asEmployee();
        // 审核通过 → processing
        refundRecordService.auditUserRefund(refundId, true, null);

        // execute：渠道退款（mock）已成功，但本地落库撞 uk_refund_no → 告警性成功返回
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/refund/user/execute")
                .param("refundId", String.valueOf(refundId))
                .sessionAttr("employee", EMP_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("退款已提交（渠道已处理），请核对退款记录"));

        // 【缺陷后果】售后单仍 processing、订单仍 4、支付单仍 SUCCESS，
        // 且不存在任何 SUCCESS 退款财务记录——渠道已退款、本地零落账，资金账实不符，需人工对账
        assertEquals("processing", refundStatus(refundId));
        Integer orderStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", Integer.class, ORDER_ID);
        assertEquals(4, orderStatus == null ? -1 : orderStatus.intValue());
        String poStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM payment_order WHERE order_id = ?", String.class, ORDER_ID);
        assertEquals("SUCCESS", poStatus);
        Integer successFinancing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record rr WHERE rr.status = 'SUCCESS' AND rr.payment_order_id IN "
                        + "(SELECT id FROM payment_order WHERE order_id = ?)", Integer.class, ORDER_ID);
        assertEquals(0, successFinancing == null ? 0 : successFinancing.intValue());
    }

    @Test
    void testExecuteAlreadySuccessIdempotent() throws Exception {
        seedCompletedOrder();
        long refundId = applyAsUser().getId();
        asEmployee();
        // 直接把售后单置 SUCCESS（模拟已退款）
        jdbcTemplate.update("UPDATE refund_record SET status = 'SUCCESS' WHERE id = ?", refundId);

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/refund/user/execute")
                .param("refundId", String.valueOf(refundId))
                .sessionAttr("employee", EMP_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("该售后单已退款成功，请勿重复操作"));
    }

    @Test
    void testExecutePendingNotSupported() throws Exception {
        seedCompletedOrder();
        long refundId = applyAsUser().getId();
        asEmployee();
        // 未审核（仍 pending）直接 execute → 拒绝
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/refund/user/execute")
                .param("refundId", String.valueOf(refundId))
                .sessionAttr("employee", EMP_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该售后单当前状态不支持退款执行（需先审核通过）"));
    }

    @Test
    void testExecuteRecordNotFound() throws Exception {
        asEmployee();
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/refund/user/execute")
                .param("refundId", "999999")
                .sessionAttr("employee", EMP_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("售后记录不存在"));
    }
}
