package com.reggie.module.delivery;

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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 骑手异常工单与转单改派测试（P0-3）：
 * 异常上报与校验、骑手/管理端查询、管理端处理（解决/关闭/改派）、
 * 骑手转单、可转单骑手列表，以及改派对订单归属与双方在途单量的影响。
 *
 * @author reggie
 * @since 2026-09-28
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-delivery.sql", "classpath:schema-rider.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RiderExceptionTest extends BaseControllerTest {

    private static final long RIDER_A = 2001L;
    private static final long RIDER_B = 2002L;
    private static final long RIDER_C = 2003L;

    private static final String ORDER_INSERT =
            "INSERT INTO orders (id, number, status, user_id, address_book_id, order_time, amount, delivery_fee, "
            + "remark, phone, address, consignee, dining_type, rider_id, create_time, update_time, "
            + "is_deleted, tenant_id, version) "
            + "VALUES (:id, :number, :status, 9001, 3001, NOW(), 127.00, 8.00, '测试备注', '13900139001', "
            + "'北京市朝阳区三里屯幸福里3栋2单元1503室', '测试用户', 'OUTSIDE', :rider, NOW(), NOW(), 0, 999, 0)";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private NamedParameterJdbcTemplate named;

    @BeforeEach
    void setUp() {
        named = new NamedParameterJdbcTemplate(jdbc);
        BaseContext.setCurrentTenantId(999L);

        jdbc.update("DELETE FROM rider_message WHERE rider_id IN (2001, 2002, 2003)");
        jdbc.update("DELETE FROM rider_exception_order WHERE order_id BETWEEN 5021 AND 5031");
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5021 AND 5031");
        jdbc.update("DELETE FROM rider WHERE id IN (2001, 2002, 2003)");
        jdbc.update("DELETE FROM address_book WHERE id = 3001");
        jdbc.update("DELETE FROM user WHERE id = 9001");

        jdbc.update("INSERT IGNORE INTO tenant (id, name, phone, address, contact, status, create_time, update_time) "
                + "VALUES (999, '自动化测试租户', '13800138099', '测试', '联系人', 1, NOW(), NOW())");
        jdbc.update("INSERT INTO user (id, name, phone, status, create_time, update_time, tenant_id) "
                + "VALUES (9001, '测试用户', '13900000001', 1, NOW(), NOW(), 999)");
        jdbc.update("INSERT INTO address_book (id, user_id, consignee, phone, detail, longitude, latitude, "
                + "is_default, create_time, update_time, create_user, update_user, is_deleted, tenant_id) "
                + "VALUES (3001, 9001, '测试用户', '13900000001', '北京市朝阳区三里屯幸福里3栋2单元1503室', "
                + "116.447219, 39.937610, 1, NOW(), NOW(), 9001, 9001, 0, 999)");
        insertRider(RIDER_A, "张骑手", 1);
        insertRider(RIDER_B, "王骑手", 1);
        insertRider(RIDER_C, "李骑手", 0);
    }

    /**
     * 清理本类数据：surefire 复用 JVM，遗留订单会污染其他骑手测试类的列表断言。
     */
    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM rider_message WHERE rider_id IN (2001, 2002, 2003)");
        jdbc.update("DELETE FROM rider_exception_order WHERE order_id BETWEEN 5021 AND 5031");
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5021 AND 5031");
        jdbc.update("DELETE FROM rider WHERE id IN (2001, 2002, 2003)");
        jdbc.update("DELETE FROM address_book WHERE id = 3001");
        jdbc.update("DELETE FROM user WHERE id = 9001");
    }

    private void insertRider(long id, String name, int status) {
        jdbc.update("INSERT INTO rider (id, name, phone, status, current_order_count, total_order_count, "
                + "tenant_id, create_time, update_time) VALUES (?, ?, ?, ?, 0, 0, 999, NOW(), NOW())",
                id, name, "1380000000" + (id - 2000), status);
    }

    private void insertOrder(long id, String number, int status, Long riderId) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("number", number)
                .addValue("status", status)
                .addValue("rider", riderId, java.sql.Types.BIGINT);
        named.update(ORDER_INSERT, p);
    }

    private MockHttpSession employeeSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("employee", 1L);
        s.setAttribute("tenantId", 999L);
        return s;
    }

    private MockHttpSession riderSession(long riderId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("rider", riderId);
        s.setAttribute("tenantId", 999L);
        return s;
    }

    @Test
    void riderSubmitAndListException() throws Exception {
        insertOrder(5021L, "TEST-5021", 3, RIDER_A);

        // 上报异常
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider-exception")
                .session(riderSession(RIDER_A))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":5021,\"exceptionType\":1,\"description\":\"联系不上顾客\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.orderNumber").value("TEST-5021"))
                .andExpect(jsonPath("$.data.exceptionType").value(1));

        // 骑手可见自己的工单
        mockMvc.perform(get("/api/rider-exception/mine").session(riderSession(RIDER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].description").value("联系不上顾客"));

        // 管理端可见并可按状态筛选
        mockMvc.perform(get("/api/rider-exception/admin/page")
                        .param("status", "0").session(employeeSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1));
        mockMvc.perform(get("/api/rider-exception/admin/count").session(employeeSession()))
                .andExpect(jsonPath("$.data['0']").value(1));
    }

    @Test
    void submitValidation() throws Exception {
        // 非本人订单
        insertOrder(5022L, "TEST-5022", 3, RIDER_B);
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider-exception")
                .session(riderSession(RIDER_A))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":5022,\"exceptionType\":1,\"description\":\"x\"}")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该订单不属于你，无法上报异常"));

        // 订单已完成不可上报
        insertOrder(5023L, "TEST-5023", 4, RIDER_A);
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider-exception")
                .session(riderSession(RIDER_A))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":5023,\"exceptionType\":1,\"description\":\"x\"}")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("订单当前状态不可上报异常"));

        // 非法类型
        insertOrder(5024L, "TEST-5024", 3, RIDER_A);
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider-exception")
                .session(riderSession(RIDER_A))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":5024,\"exceptionType\":9,\"description\":\"x\"}")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("异常的异常类型"));

        // 空描述
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider-exception")
                .session(riderSession(RIDER_A))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":5024,\"exceptionType\":2,\"description\":\"  \"}")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("请填写异常描述"));
    }

    @Test
    void adminHandleResolveAndRejectDuplicate() throws Exception {
        insertOrder(5025L, "TEST-5025", 3, RIDER_A);
        submitException(5025L, RIDER_A, 3, "地址有误");
        long id = latestExceptionId(5025L);

        mockMvc.perform(withCsrfToken(mockMvc, put("/api/rider-exception/" + id + "/handle")
                .session(employeeSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"RESOLVE\",\"handleNote\":\"已电话引导\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.handleNote").value("已电话引导"));

        // 重复处理被拒
        mockMvc.perform(withCsrfToken(mockMvc, put("/api/rider-exception/" + id + "/handle")
                .session(employeeSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"CLOSE\"}")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("工单已处理，请勿重复操作"));
    }

    @Test
    void adminReassignMovesOrderAndLoads() throws Exception {
        insertOrder(5026L, "TEST-5026", 3, RIDER_A);
        // 模拟 A 已接单：在途 1、忙碌
        jdbc.update("UPDATE rider SET current_order_count = 1, status = 2 WHERE id = ?", RIDER_A);
        submitException(5026L, RIDER_A, 1, "送错地址");
        long id = latestExceptionId(5026L);

        mockMvc.perform(withCsrfToken(mockMvc, put("/api/rider-exception/" + id + "/handle")
                .session(employeeSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"REASSIGN\",\"newRiderId\":" + RIDER_B + ",\"handleNote\":\"改派王骑手\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.status").value(2));

        // 订单归属转移
        assertEquals(RIDER_B, jdbc.queryForObject(
                "SELECT rider_id FROM orders WHERE id = 5026", Long.class));
        // A 释放在途（不增累计单量），归零回在线
        Map<String, Object> a = riderRow(RIDER_A);
        assertEquals(0, ((Number) a.get("current_order_count")).intValue());
        assertEquals(0, ((Number) a.get("total_order_count")).intValue());
        assertEquals(1, ((Number) a.get("status")).intValue());
        // B 在途 +1、忙碌
        Map<String, Object> b = riderRow(RIDER_B);
        assertEquals(1, ((Number) b.get("current_order_count")).intValue());
        assertEquals(2, ((Number) b.get("status")).intValue());
    }

    @Test
    void riderTransferOwnOrder() throws Exception {
        insertOrder(5027L, "TEST-5027", 3, RIDER_A);
        jdbc.update("UPDATE rider SET current_order_count = 1, status = 2 WHERE id = ?", RIDER_A);

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5027/transfer")
                .param("newRiderId", String.valueOf(RIDER_B))
                .session(riderSession(RIDER_A))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("已转单"));

        assertEquals(RIDER_B, jdbc.queryForObject(
                "SELECT rider_id FROM orders WHERE id = 5027", Long.class));
        assertEquals(0, ((Number) riderRow(RIDER_A).get("current_order_count")).intValue());
        assertEquals(1, ((Number) riderRow(RIDER_B).get("current_order_count")).intValue());
    }

    @Test
    void transferValidation() throws Exception {
        insertOrder(5028L, "TEST-5028", 3, RIDER_A);

        // 转给离线骑手被拒
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5028/transfer")
                .param("newRiderId", String.valueOf(RIDER_C))
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("目标骑手当前离线，无法改派"));

        // 非归属骑手转单被拒（订单属于 A，B 来转）
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5028/transfer")
                .param("newRiderId", String.valueOf(RIDER_B))
                .session(riderSession(RIDER_B))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("订单已不属于原骑手，改派失败"));

        // 转给自己被拒
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5028/transfer")
                .param("newRiderId", String.valueOf(RIDER_A))
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("目标骑手与原骑手相同，无需改派"));
    }

    @Test
    void availableRidersExcludesSelfAndOffline() throws Exception {
        mockMvc.perform(get("/api/rider-exception/available-riders").session(riderSession(RIDER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(RIDER_B));
    }

    // ---- 辅助 ----

    private void submitException(long orderId, long riderId, int type, String desc) throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider-exception")
                .session(riderSession(riderId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + orderId + ",\"exceptionType\":" + type
                        + ",\"description\":\"" + desc + "\"}")))
                .andExpect(jsonPath("$.code").value(1));
    }

    private long latestExceptionId(long orderId) {
        Long id = jdbc.queryForObject(
                "SELECT MAX(id) FROM rider_exception_order WHERE order_id = ?", Long.class, orderId);
        return id == null ? 0L : id;
    }

    private Map<String, Object> riderRow(long id) {
        return jdbc.queryForMap(
                "SELECT status, current_order_count, total_order_count FROM rider WHERE id = ?", id);
    }
}
