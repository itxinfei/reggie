package com.reggie.module.payment.controller;

import com.reggie.common.BaseContext;
import com.reggie.controller.BaseControllerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 会员充值在线支付发起接口测试（P1-4）：POST /api/payment/recharge/pay。
 * 覆盖：成功发起（生成 bizType=RECHARGE 支付单）、非本人/不存在统一文案、
 * 状态非 PENDING 拒绝、参数缺失 400、未登录 401。
 *
 * @author reggie
 * @since 2026-09-29
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "reggie.payment.mock-mode=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema-payment-controller.sql", "classpath:schema-member.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RechargePaymentControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // 清理本方法限流计数：7 个用例命中同一 rate_limit key（同 controller.pay + user 9001），
        // 复用 Spring 上下文时方法间隔可能 <1s，跨用例累计触发 429，故每用例前清空
        java.util.Set<String> limitKeys = redisTemplate.keys(
                "rate_limit:com.reggie.module.payment.controller.RechargePaymentController.pay:*");
        if (limitKeys != null && !limitKeys.isEmpty()) {
            redisTemplate.delete(limitKeys);
        }
        BaseContext.setCurrentId(9001L);
        BaseContext.setCurrentTenantId(999L);
        jdbc.update("INSERT INTO member (id, tenant_id, user_id, points, balance, status, version) "
                + "VALUES (7001, 999, 9001, 0, 0, 1, 0)");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM payment_order WHERE biz_type = 'RECHARGE'");
        jdbc.update("DELETE FROM recharge_record WHERE id BETWEEN 6101 AND 6105");
        jdbc.update("DELETE FROM member WHERE id = 7001");
    }

    private void insertRecharge(long id, String no, Long ownerUserId, String status) {
        jdbc.update("INSERT INTO recharge_record (id, tenant_id, member_id, user_id, recharge_no, status, "
                        + "amount, payment_method, created_time) VALUES (?, 999, 7001, ?, ?, ?, 100.00, "
                        + "'WECHAT', NOW())",
                id, ownerUserId, no, status);
    }

    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("user", 9001L);
        s.setAttribute("tenantId", 999L);
        return s;
    }

    private String body(String rechargeNo, String channel) {
        return "{\"rechargeNo\":\"" + rechargeNo + "\",\"channel\":\"" + channel + "\"}";
    }

    @Test
    void paySuccess() throws Exception {
        insertRecharge(6101L, "RC-PAY-1", 9001L, "PENDING");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/recharge/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RC-PAY-1", "WECHAT"))
                        .session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.tradeNo").isNotEmpty())
                .andExpect(jsonPath("$.data.mockMode").value(true));

        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM payment_order WHERE biz_type = 'RECHARGE' "
                + "AND order_id = 6101 AND status = 'PENDING'", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), c);
    }

    @Test
    void payNotOwner() throws Exception {
        insertRecharge(6102L, "RC-PAY-2", 9999L, "PENDING");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/recharge/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RC-PAY-2", "WECHAT"))
                        .session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void payMissing() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/recharge/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RC-NONE", "WECHAT"))
                        .session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void payNotPending() throws Exception {
        insertRecharge(6103L, "RC-PAY-3", 9001L, "SUCCESS");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/recharge/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RC-PAY-3", "WECHAT"))
                        .session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void payBlankRechargeNo() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/recharge/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "WECHAT"))
                        .session(session()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payBlankChannel() throws Exception {
        insertRecharge(6104L, "RC-PAY-4", 9001L, "PENDING");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/recharge/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RC-PAY-4", ""))
                        .session(session()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payWithoutLoginContext() throws Exception {
        // MockMvc 已在测试侧移除 LoginCheckFilter（见 MockMvcFilterExclusionAutoConfiguration），
        // 这里清空 ThreadLocal 登录态，验证 controller 自身的未登录纵深防御：200 + code=0 + 请先登录。
        // filter 层的 401 拦截由 LoginCheckFilter 独立单元测试覆盖。
        BaseContext.setCurrentId(null);

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/recharge/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RC-PAY-1", "WECHAT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("请先登录"));
    }
}
