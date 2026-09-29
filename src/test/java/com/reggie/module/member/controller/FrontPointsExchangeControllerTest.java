package com.reggie.module.member.controller;

import com.reggie.common.BaseContext;
import com.reggie.controller.BaseControllerTest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端积分兑换优惠券接口测试（P1-5）：POST /front/coupon/exchange/{templateId}。
 * 校验兑换成功后积分扣减、OUT 流水、coupon_user 落库；积分不足 / 券不支持积分兑换返回 422。
 *
 * @author reggie
 * @since 2026-09-29
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-member.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FrontPointsExchangeControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(9001L);
        BaseContext.setCurrentTenantId(999L);
    }

    private void seed(long memberPoints, Integer pointsPrice) {
        jdbc.update("INSERT INTO member (id, tenant_id, user_id, points, balance, status, version) "
                + "VALUES (7001, 999, 9001, ?, 0, 1, 0)", memberPoints);
        jdbc.update("INSERT INTO coupon_template (id, tenant_id, name, type, total_count, remain_count, "
                + "valid_days, points_price, status) VALUES (8001, 999, '积分兑换券', 'FULL_REDUCTION', "
                + "100, 50, 30, ?, 1)", pointsPrice);
    }

    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("user", 9001L);
        s.setAttribute("tenantId", 999L);
        return s;
    }

    private long memberPoints() {
        Long p = jdbc.queryForObject("SELECT points FROM member WHERE id = 7001", Long.class);
        return p == null ? -1L : p;
    }

    private int couponCount() {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM coupon_user WHERE member_id = 7001 "
                + "AND template_id = 8001", Integer.class);
        return c == null ? 0 : c;
    }

    private int outRecordCount() {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM points_record WHERE member_id = 7001 "
                + "AND type = 'OUT' AND biz_type = 'POINTS_EXCHANGE'", Integer.class);
        return c == null ? 0 : c;
    }

    @Test
    void exchangeSuccess() throws Exception {
        seed(200L, 100);

        mockMvc.perform(withCsrfToken(mockMvc, post("/front/coupon/exchange/8001")).session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(100L, memberPoints());
        assertEquals(1, couponCount());
        assertEquals(1, outRecordCount());
    }

    @Test
    void exchangeNotEnoughPoints() throws Exception {
        seed(50L, 100);

        mockMvc.perform(withCsrfToken(mockMvc, post("/front/coupon/exchange/8001")).session(session()))
                .andExpect(status().isUnprocessableEntity());

        assertEquals(50L, memberPoints());
        assertEquals(0, couponCount());
        assertEquals(0, outRecordCount());
    }

    @Test
    void exchangeNotPointsCoupon() throws Exception {
        // JdbcTemplate 对 NULL 参数需要类型
        seed(200L, null);

        mockMvc.perform(withCsrfToken(mockMvc, post("/front/coupon/exchange/8001")).session(session()))
                .andExpect(status().isUnprocessableEntity());

        assertEquals(0, couponCount());
    }
}
