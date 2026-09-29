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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 取餐码核销测试（P0-6）：派单/抢单生成取餐码；取餐必须核销一致；
 * 错误码被拒；历史无码订单放行（避免存量数据被卡住）。
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
public class RiderPickupCodeTest extends BaseControllerTest {

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
        jdbc.update("DELETE FROM delivery_time_record WHERE order_id BETWEEN 5071 AND 5075");
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5071 AND 5075");
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
     * 清理本类数据：surefire 复用 JVM 且连真实 MySQL，遗留订单会污染其他测试类。
     */
    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM rider_message WHERE rider_id IN (2001, 2002)");
        jdbc.update("DELETE FROM delivery_time_record WHERE order_id BETWEEN 5071 AND 5075");
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5071 AND 5075");
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

    /** 派单并返回生成的取餐码 */
    private String dispatchAndGetCode(long orderId, long riderId) throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/order/dispatch")
                .session(employeeSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + orderId + ",\"riderId\":" + riderId + "}")))
                .andExpect(jsonPath("$.code").value(1));
        String code = jdbc.queryForObject(
                "SELECT pickup_code FROM orders WHERE id = ?", String.class, orderId);
        assertNotNull(code, "派单后应生成取餐码");
        assertTrue(code.matches("\\d{6}"), "取餐码应为 6 位数字，实际=" + code);
        return code;
    }

    @Test
    void dispatchGeneratesPickupCode() throws Exception {
        insertOrder(5071L, "TEST-5071", 2, null);
        dispatchAndGetCode(5071L, RIDER_A);
    }

    @Test
    void grabGeneratesPickupCode() throws Exception {
        insertOrder(5072L, "TEST-5072", 2, null);
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5072/grab")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1));
        String code = jdbc.queryForObject(
                "SELECT pickup_code FROM orders WHERE id = 5072", String.class);
        assertNotNull(code, "抢单后应生成取餐码");
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void pickupRejectedWithWrongCode() throws Exception {
        insertOrder(5073L, "TEST-5073", 2, null);
        String code = dispatchAndGetCode(5073L, RIDER_A);
        // 接单 → 进入配送中
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5073/accept")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1));

        String wrong = "000000".equals(code) ? "111111" : "000000";
        // 错误码
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5073/pickup")
                .param("pickupCode", wrong)
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("取餐码不正确，请向店员索取正确的取餐码"));
        // 不传码
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5073/pickup")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("取餐码不正确，请向店员索取正确的取餐码"));

        // 未核销成功则不记录取餐时间
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM delivery_time_record WHERE order_id=5073 AND pickup_time IS NOT NULL",
                Integer.class));
    }

    @Test
    void pickupSucceedsWithCorrectCode() throws Exception {
        insertOrder(5074L, "TEST-5074", 2, null);
        String code = dispatchAndGetCode(5074L, RIDER_A);
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5074/accept")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5074/pickup")
                .param("pickupCode", code)
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("已取餐"));

        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM delivery_time_record WHERE order_id=5074 AND pickup_time IS NOT NULL",
                Integer.class));
    }

    @Test
    void legacyOrderWithoutCodeCanStillPickup() throws Exception {
        // 历史订单：已指派骑手且配送中，但无取餐码（存量数据）→ 放行，避免被卡住
        insertOrder(5075L, "TEST-5075", 3, RIDER_A);
        jdbc.update("UPDATE orders SET pickup_code = NULL WHERE id = 5075");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5075/pickup")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("已取餐"));
    }
}
