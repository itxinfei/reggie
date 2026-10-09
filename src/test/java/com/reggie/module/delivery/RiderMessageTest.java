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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 骑手消息中心测试（P0-4）：派单/异常处理挂钩写入消息、骑手收件箱、未读数、
 * 标记已读与全部已读、归属校验、管理端公告广播。
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
public class RiderMessageTest extends BaseControllerTest {

    private static final long RIDER_A = 2001L;
    private static final long RIDER_B = 2002L;

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

        jdbc.update("DELETE FROM rider_message WHERE rider_id IN (2001, 2002)");
        jdbc.update("DELETE FROM rider_exception_order WHERE order_id BETWEEN 5041 AND 5051");
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5041 AND 5051");
        jdbc.update("DELETE FROM rider WHERE id IN (2001, 2002)");
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
    }

    /**
     * 清理本类数据：surefire 复用 JVM，遗留的待接单订单会污染其他骑手测试类的列表断言。
     */
    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM rider_message WHERE rider_id IN (2001, 2002)");
        jdbc.update("DELETE FROM rider_exception_order WHERE order_id BETWEEN 5041 AND 5051");
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5041 AND 5051");
        jdbc.update("DELETE FROM rider WHERE id IN (2001, 2002)");
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
    void dispatchCreatesMessageAndCanBeRead() throws Exception {
        insertOrder(5041L, "TEST-5041", 2, null);

        // 店长派单 → 应写入一条派单提醒
        mockMvc.perform(withCsrfToken(mockMvc, post("/order/dispatch")
                .session(employeeSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":5041,\"riderId\":2001}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM rider_message WHERE rider_id = 2001 AND type = 1", Integer.class));

        // 骑手可见、未读 1
        mockMvc.perform(get("/api/rider-message/mine").session(riderSession(RIDER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].type").value(1))
                .andExpect(jsonPath("$.data.records[0].isRead").value(0))
                .andExpect(jsonPath("$.data.records[0].bizId").value(5041));

        mockMvc.perform(get("/api/rider-message/unread-count").session(riderSession(RIDER_A)))
                .andExpect(jsonPath("$.data.count").value(1));

        // 标记已读 → 未读归零，且幂等
        long id = latestMessageId(RIDER_A);
        mockMvc.perform(withCsrfToken(mockMvc, put("/api/rider-message/" + id + "/read")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.isRead").value(1));
        mockMvc.perform(get("/api/rider-message/unread-count").session(riderSession(RIDER_A)))
                .andExpect(jsonPath("$.data.count").value(0));
        mockMvc.perform(withCsrfToken(mockMvc, put("/api/rider-message/" + id + "/read")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.isRead").value(1));
    }

    @Test
    void markReadRejectsOtherRider() throws Exception {
        insertOrder(5042L, "TEST-5042", 2, null);
        mockMvc.perform(withCsrfToken(mockMvc, post("/order/dispatch")
                .session(employeeSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":5042,\"riderId\":2001}")))
                .andExpect(jsonPath("$.code").value(1));

        long id = latestMessageId(RIDER_A);
        mockMvc.perform(withCsrfToken(mockMvc, put("/api/rider-message/" + id + "/read")
                .session(riderSession(RIDER_B))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("无权操作他人的消息"));
    }

    @Test
    void exceptionHandleNotifiesReporter() throws Exception {
        insertOrder(5043L, "TEST-5043", 3, RIDER_A);
        // 骑手上报异常
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider-exception")
                .session(riderSession(RIDER_A))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":5043,\"exceptionType\":1,\"description\":\"联系不上顾客\"}")))
                .andExpect(jsonPath("$.code").value(1));
        long exId = jdbc.queryForObject(
                "SELECT MAX(id) FROM rider_exception_order WHERE order_id = 5043", Long.class);

        // 后台处理 → 应写入一条异常结果消息
        mockMvc.perform(withCsrfToken(mockMvc, put("/api/rider-exception/" + exId + "/handle")
                .session(employeeSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"RESOLVE\",\"handleNote\":\"已电话引导\"}")))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/api/rider-message/mine").session(riderSession(RIDER_A)))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].type").value(3))
                .andExpect(jsonPath("$.data.records[0].content").value(
                        org.hamcrest.Matchers.containsString("已电话引导")));
    }

    @Test
    void broadcastReachesAllRidersAndMarkAllRead() throws Exception {
        // 广播范围 = 当前租户下全部骑手（含 schema 种子数据，动态取值避免硬编码）
        int riderCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rider WHERE tenant_id = 999", Integer.class);
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider-message/broadcast")
                .session(employeeSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"雨天注意安全\",\"content\":\"今日有雨，请减速慢行\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.count").value(riderCount));

        // 每位骑手各收到一条公告
        mockMvc.perform(get("/api/rider-message/mine").session(riderSession(RIDER_A)))
                .andExpect(jsonPath("$.data.records[0].type").value(4))
                .andExpect(jsonPath("$.data.records[0].title").value("雨天注意安全"));
        mockMvc.perform(get("/api/rider-message/unread-count").session(riderSession(RIDER_B)))
                .andExpect(jsonPath("$.data.count").value(1));

        // 全部已读
        mockMvc.perform(withCsrfToken(mockMvc, put("/api/rider-message/read-all")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.count").value(1));
        mockMvc.perform(get("/api/rider-message/unread-count").session(riderSession(RIDER_A)))
                .andExpect(jsonPath("$.data.count").value(0));
        // 不影响其他骑手
        mockMvc.perform(get("/api/rider-message/unread-count").session(riderSession(RIDER_B)))
                .andExpect(jsonPath("$.data.count").value(1));
    }

    private long latestMessageId(long riderId) {
        Long id = jdbc.queryForObject(
                "SELECT MAX(id) FROM rider_message WHERE rider_id = ?", Long.class, riderId);
        return id == null ? 0L : id;
    }
}
