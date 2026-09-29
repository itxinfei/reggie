package com.reggie.module.delivery;

import com.reggie.ReggieApplication;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.controller.BaseControllerTest;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.model.RiderEvaluation;
import com.reggie.module.delivery.service.RiderEvaluationService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 骑手评价测试：提交/幂等/校验、评分统计、商家回复，以及顾客/骑手/管理端 HTTP 端点。
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
public class RiderEvaluationTest extends BaseControllerTest {

    private static final long TENANT = 999L;
    private static final long USER = 9001L;
    private static final long RIDER = 2001L;

    @Autowired
    private RiderEvaluationService evaluationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(TENANT);
        jdbc.update("INSERT IGNORE INTO tenant (id, name, phone, address, contact, status, create_time, update_time) "
                + "VALUES (999, '自动化测试租户', '13800138099', '测试', '联系人', 1, NOW(), NOW())");
        jdbc.update("DELETE FROM rider WHERE id = 2001");
        jdbc.update("INSERT INTO rider (id, name, phone, status, current_order_count, total_order_count, "
                + "rating, tenant_id, create_time, update_time) "
                + "VALUES (2001, '张骑手', '13800000001', 1, 0, 0, 5.0, 999, NOW(), NOW())");
        jdbc.update("DELETE FROM user WHERE id = 9001");
        jdbc.update("INSERT INTO user (id, name, phone, status, create_time, update_time, tenant_id) "
                + "VALUES (9001, '测试用户', '13900000001', 1, NOW(), NOW(), 999)");
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 6001 AND 6010");
        // 已完成、由骑手 2001 配送、归属用户 9001 的订单
        jdbc.update("INSERT INTO orders (id, number, status, user_id, rider_id, user_name, amount, "
                + "delivery_fee, address, consignee, dining_type, create_time, update_time, is_deleted, tenant_id, version) "
                + "VALUES (6001, 'EV-6001', 4, 9001, 2001, '测试用户', 127.00, 8.00, '地址', '收件人', 'OUTSIDE', "
                + "NOW(), NOW(), 0, 999, 0)");
        jdbc.update("INSERT INTO orders (id, number, status, user_id, rider_id, user_name, amount, "
                + "delivery_fee, address, consignee, dining_type, create_time, update_time, is_deleted, tenant_id, version) "
                + "VALUES (6002, 'EV-6002', 3, 9001, 2001, '测试用户', 127.00, 8.00, '地址', '收件人', 'OUTSIDE', "
                + "NOW(), NOW(), 0, 999, 0)");
    }

    private RiderEvaluation buildEval(long orderId, long riderId, int star) {
        RiderEvaluation e = new RiderEvaluation();
        e.setOrderId(orderId);
        e.setRiderId(riderId);
        e.setStarRating(star);
        e.setContent("准时送达");
        e.setTags("[\"准时送达\"]");
        e.setAnonymous(0);
        return e;
    }

    @Test
    void submit_success_updatesRiderRating() {
        BaseContext.setCurrentId(USER);
        RiderEvaluation saved = evaluationService.submit(buildEval(6001L, RIDER, 5));
        assertNotNull(saved.getId());
        assertEquals(Integer.valueOf(1), saved.getStatus());

        // 已评价查询
        RiderEvaluation existing = evaluationService.getByOrderAndRider(TENANT, 6001L, RIDER);
        assertNotNull(existing);

        // 骑手评分统计：平均分 5.0，数量 1
        Map<String, Object> stats = evaluationService.getRiderStats(TENANT, RIDER);
        assertEquals(0, new BigDecimal("5.0").compareTo(new BigDecimal(stats.get("avg").toString())));
        assertEquals(1L, Long.parseLong(stats.get("cnt").toString()));
    }

    @Test
    void submit_duplicate_rejected() {
        BaseContext.setCurrentId(USER);
        evaluationService.submit(buildEval(6001L, RIDER, 5));
        assertThrows(CustomException.class, () -> evaluationService.submit(buildEval(6001L, RIDER, 4)));
    }

    @Test
    void submit_orderNotCompleted_rejected() {
        BaseContext.setCurrentId(USER);
        // 6002 状态为 3（配送中），不可评价
        assertThrows(CustomException.class, () -> evaluationService.submit(buildEval(6002L, RIDER, 5)));
    }

    @Test
    void submit_notOwnOrder_rejected() {
        BaseContext.setCurrentId(8888L);
        assertThrows(CustomException.class, () -> evaluationService.submit(buildEval(6001L, RIDER, 5)));
    }

    @Test
    void reply_setsContent() {
        BaseContext.setCurrentId(USER);
        RiderEvaluation saved = evaluationService.submit(buildEval(6001L, RIDER, 4));
        boolean ok = evaluationService.reply(saved.getId(), "感谢你的评价", 1L, TENANT);
        assertTrue(ok);
        RiderEvaluation after = evaluationService.getByOrderAndRider(TENANT, 6001L, RIDER);
        assertEquals("感谢你的评价", after.getReplyContent());
    }

    @Test
    void http_customerSubmit_riderReceived_adminReply() throws Exception {
        // 顾客提交（@RequireUser 切面从会话恢复上下文，MockMvc 下兜底生效）
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider-evaluation")
                        .session(userSession())
                        .contentType("application/json")
                        .content("{\"orderId\":6001,\"riderId\":2001,\"starRating\":5,"
                                + "\"content\":\"非常好\",\"tags\":\"[\\\"准时送达\\\"]\",\"anonymous\":0}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 骑手收到的评价（@RequireRider）
        mockMvc.perform(get("/api/rider-evaluation/received").session(riderSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1));

        // 骑手评分统计（公开端点，需租户上下文；MockMvc 下手动设置）
        BaseContext.setCurrentTenantId(TENANT);
        mockMvc.perform(get("/api/rider-evaluation/rider/2001/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.cnt").value(1));

        // 管理端回复（需先取评价 id，@RequireEmployee）
        MvcResult r = mockMvc.perform(get("/api/rider-evaluation/admin/page?page=1&pageSize=10")
                        .session(employeeSession()))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andReturn();
        long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(r.getResponse().getContentAsString()).path("data").path("records")
                .get(0).path("id").asLong();

        mockMvc.perform(withCsrfToken(mockMvc, put("/api/rider-evaluation/" + id + "/reply")
                        .session(employeeSession())
                        .contentType("application/json")
                        .content("{\"replyContent\":\"感谢支持\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    private MockHttpSession userSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("user", USER);
        s.setAttribute("tenantId", TENANT);
        return s;
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
