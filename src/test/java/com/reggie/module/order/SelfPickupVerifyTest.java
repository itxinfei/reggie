package com.reggie.module.order;

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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 店员核销自提订单测试（P1-2）：
 * 校验顾客取餐码，通过则订单完成；码错误 / 非自提 / 状态不对一律拒绝且状态不变。
 *
 * <p>Orders.source 映射 dining_type 列（{@code @TableField("dining_type")}），
 * 故自提订单 dining_type='SELF_PICKUP'。</p>
 *
 * @author reggie
 * @since 2026-09-29
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-delivery.sql", "classpath:schema-rider.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SelfPickupVerifyTest extends BaseControllerTest {

    private static final String ORDER_INSERT =
            "INSERT INTO orders (id, number, status, user_id, order_time, amount, delivery_fee, "
            + "phone, consignee, dining_type, pickup_code, create_time, update_time, "
            + "is_deleted, tenant_id, version) "
            + "VALUES (:id, :number, :status, 9001, NOW(), 66.00, 0.00, '13900139001', '测试用户', "
            + ":diningType, :pickupCode, NOW(), NOW(), 0, 999, 0)";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private NamedParameterJdbcTemplate named;

    @BeforeEach
    void setUp() {
        named = new NamedParameterJdbcTemplate(jdbc);
        BaseContext.setCurrentTenantId(999L);

        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5071 AND 5076");
        jdbc.update("DELETE FROM user WHERE id = 9001");

        jdbc.update("INSERT IGNORE INTO tenant (id, name, phone, address, contact, status, create_time, update_time) "
                + "VALUES (999, '自动化测试租户', '13800138099', '测试', '联系人', 1, NOW(), NOW())");
        jdbc.update("INSERT INTO user (id, name, phone, status, create_time, update_time, tenant_id) "
                + "VALUES (9001, '测试用户', '13900000001', 1, NOW(), NOW(), 999)");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5071 AND 5076");
        jdbc.update("DELETE FROM user WHERE id = 9001");
    }

    private void insertOrder(long id, int status, String diningType, String pickupCode) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("number", "TEST-" + id)
                .addValue("status", status)
                .addValue("diningType", diningType)
                .addValue("pickupCode", pickupCode);
        named.update(ORDER_INSERT, p);
    }

    private MockHttpSession employeeSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("employee", 1L);
        s.setAttribute("tenantId", 999L);
        return s;
    }

    private MockHttpSession customerSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("user", 9001L);
        s.setAttribute("tenantId", 999L);
        return s;
    }

    private String body(long id, String pickupCode) {
        return "{\"id\":" + id + ",\"pickupCode\":\"" + pickupCode + "\"}";
    }

    private int orderStatus(long id) {
        Integer st = jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", Integer.class, id);
        return st == null ? -1 : st;
    }

    @Test
    void verifyWithCorrectCodeCompletesOrder() throws Exception {
        insertOrder(5071L, 3, "SELF_PICKUP", "8888");

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/selfPickup/verify"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5071L, "8888"))
                        .session(employeeSession()))
                .andExpect(status().isOk())
                .andReturn();

        org.junit.jupiter.api.Assertions.assertEquals(4, orderStatus(5071L));
    }

    @Test
    void wrongCodeRejectedAndStatusUnchanged() throws Exception {
        insertOrder(5072L, 3, "SELF_PICKUP", "8888");

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/selfPickup/verify"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5072L, "0000"))
                        .session(employeeSession()))
                .andExpect(status().isUnprocessableEntity());

        org.junit.jupiter.api.Assertions.assertEquals(3, orderStatus(5072L));
    }

    @Test
    void nonSelfPickupOrderRejected() throws Exception {
        insertOrder(5073L, 3, "TAKEOUT", "8888");

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/selfPickup/verify"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5073L, "8888"))
                        .session(employeeSession()))
                .andExpect(status().isUnprocessableEntity());

        org.junit.jupiter.api.Assertions.assertEquals(3, orderStatus(5073L));
    }

    @Test
    void orderNotInDeliveringStatusRejected() throws Exception {
        insertOrder(5074L, 2, "SELF_PICKUP", "8888");

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/selfPickup/verify"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5074L, "8888"))
                        .session(employeeSession()))
                .andExpect(status().isUnprocessableEntity());

        org.junit.jupiter.api.Assertions.assertEquals(2, orderStatus(5074L));
    }

    @Test
    void blankCodeReturnsBadRequest() throws Exception {
        insertOrder(5075L, 3, "SELF_PICKUP", "8888");

        mockMvc.perform(withCsrfToken(mockMvc, put("/order/selfPickup/verify"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5075L, ""))
                        .session(employeeSession()))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(3, orderStatus(5075L));
    }

    @Test
    void customerSessionRejected() throws Exception {
        insertOrder(5076L, 3, "SELF_PICKUP", "8888");

        String resp = mockMvc.perform(withCsrfToken(mockMvc, put("/order/selfPickup/verify"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5076L, "8888"))
                        .session(customerSession()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(resp.contains("\"code\":0"));
        org.junit.jupiter.api.Assertions.assertEquals(3, orderStatus(5076L));
    }
}
