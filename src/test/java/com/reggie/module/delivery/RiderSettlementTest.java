package com.reggie.module.delivery;

import com.reggie.ReggieApplication;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.controller.BaseControllerTest;
import com.reggie.module.delivery.model.RiderAccount;
import com.reggie.module.delivery.model.RiderWithdrawal;
import com.reggie.module.delivery.service.RiderSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 骑手结算与提现测试：幂等入账、提现冻结、余额不足、审核通过/驳回、防双扣，
 * 并通过 HTTP 覆盖骑手端余额/提现与管理端审核端点。
 *
 * @author reggie
 * @since 2026-09-28
 */
@SpringBootTest(classes = ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-delivery.sql", "classpath:schema-rider.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RiderSettlementTest extends BaseControllerTest {

    private static final long TENANT = 999L;
    private static final long RIDER = 2001L;

    @Autowired
    private RiderSettlementService settlementService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        // 租户上下文必须设置：MyBatis-Plus 租户插件会从 BaseContext 注入 tenant_id，
        // 与业务表显式 tenant_id 保持一致，避免重复列/不一致。
        BaseContext.setCurrentTenantId(TENANT);
        jdbc.update("INSERT IGNORE INTO tenant (id, name, phone, address, contact, status, create_time, update_time) "
                + "VALUES (999, '自动化测试租户', '13800138099', '测试', '联系人', 1, NOW(), NOW())");
        jdbc.update("DELETE FROM rider WHERE id = 2001");
        jdbc.update("INSERT INTO rider (id, name, phone, status, current_order_count, total_order_count, "
                + "tenant_id, create_time, update_time) VALUES (2001, '张骑手', '13800000001', 1, 0, 0, 999, NOW(), NOW())");
    }

    private void fund(BigDecimal fee) {
        settlementService.settle(90001L, RIDER, TENANT, fee);
    }

    @Test
    void settle_isIdempotent() {
        fund(new BigDecimal("8.00"));
        RiderAccount a1 = settlementService.getAccount(RIDER, TENANT);
        assertEquals(0, new BigDecimal("8.00").compareTo(a1.getWithdrawableBalance()));
        assertEquals(0, new BigDecimal("8.00").compareTo(a1.getTotalIncome()));

        // 同一订单重复送达：余额不应被重复累加
        fund(new BigDecimal("8.00"));
        RiderAccount a2 = settlementService.getAccount(RIDER, TENANT);
        assertEquals(0, new BigDecimal("8.00").compareTo(a2.getWithdrawableBalance()));
    }

    @Test
    void applyWithdraw_freezesBalance() {
        fund(new BigDecimal("10.00"));
        RiderWithdrawal w = settlementService.applyWithdraw(RIDER, TENANT, new BigDecimal("6.00"));
        assertEquals("PENDING", w.getStatus());

        RiderAccount a = settlementService.getAccount(RIDER, TENANT);
        assertEquals(0, new BigDecimal("4.00").compareTo(a.getWithdrawableBalance()));
        assertEquals(0, new BigDecimal("6.00").compareTo(a.getFrozenBalance()));
    }

    @Test
    void applyWithdraw_insufficient_fails() {
        fund(new BigDecimal("5.00"));
        CustomException ex = assertThrows(CustomException.class,
                () -> settlementService.applyWithdraw(RIDER, TENANT, new BigDecimal("100.00")));
        assertTrue(ex.getMessage().contains("余额不足"));
    }

    @Test
    void review_approve_clearsFrozen() {
        fund(new BigDecimal("10.00"));
        RiderWithdrawal w = settlementService.applyWithdraw(RIDER, TENANT, new BigDecimal("6.00"));
        RiderWithdrawal reviewed = settlementService.review(w.getId(), true, 1L, "店长", "同意");
        assertEquals("APPROVED", reviewed.getStatus());

        RiderAccount a = settlementService.getAccount(RIDER, TENANT);
        assertEquals(0, new BigDecimal("4.00").compareTo(a.getWithdrawableBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(a.getFrozenBalance()));
        assertEquals(0, new BigDecimal("6.00").compareTo(a.getTotalWithdrawn()));
    }

    @Test
    void review_reject_returnsFrozen() {
        fund(new BigDecimal("10.00"));
        RiderWithdrawal w = settlementService.applyWithdraw(RIDER, TENANT, new BigDecimal("6.00"));
        RiderWithdrawal reviewed = settlementService.review(w.getId(), false, 1L, "店长", "信息不符");
        assertEquals("REJECTED", reviewed.getStatus());

        RiderAccount a = settlementService.getAccount(RIDER, TENANT);
        assertEquals(0, new BigDecimal("10.00").compareTo(a.getWithdrawableBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(a.getFrozenBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(a.getTotalWithdrawn()));
    }

    @Test
    void review_twice_fails() {
        fund(new BigDecimal("10.00"));
        RiderWithdrawal w = settlementService.applyWithdraw(RIDER, TENANT, new BigDecimal("6.00"));
        settlementService.review(w.getId(), true, 1L, "店长", "同意");
        // 已审批状态再次审核应被拒（CAS 防双扣）
        assertThrows(CustomException.class,
                () -> settlementService.review(w.getId(), false, 1L, "店长", "反悔"));
    }

    @Test
    void http_riderBalanceAndWithdraw() throws Exception {
        fund(new BigDecimal("10.00"));

        mockMvc.perform(get("/api/rider/settlement/balance").session(riderSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.withdrawableBalance").value(10.00));

        MvcResult r = mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/settlement/withdraw")
                        .session(riderSession()).param("amount", "4.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        // 提现记录可见
        mockMvc.perform(get("/api/rider/settlement/withdrawals").session(riderSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void http_adminReviewApprove() throws Exception {
        fund(new BigDecimal("10.00"));
        MvcResult r =         mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/settlement/withdraw")
                        .session(riderSession()).param("amount", "4.00")))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body).path("data").path("id").asLong();

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/delivery/rider-withdrawal/" + id + "/review")
                        .session(employeeSession()).param("approve", "true").param("remark", "同意")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    private MockHttpSession riderSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("rider", RIDER);
        s.setAttribute("tenantId", TENANT);
        return s;
    }

    private MockHttpSession employeeSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("employee", 1L);
        s.setAttribute("tenantId", TENANT);
        return s;
    }
}
