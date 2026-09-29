package com.reggie.module.order;

import com.reggie.common.BaseContext;
import com.reggie.controller.BaseControllerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 订单取消 / 拒单原因回执测试（P0-5）：
 * 取消原因写入独立字段 cancel_reason，且不得覆盖顾客下单备注 remark。
 *
 * <p>说明：后台定时任务（如 OrderTimeoutTask）会在测试期间并发更新 orders，
 * 与本测试的行级更新存在偶发死锁（DeadlockLoserDataAccessException），
 * 故对写操作做有界重试，避免用例间歇性失败。</p>
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
public class OrderCancelReasonTest extends BaseControllerTest {

    private static final String ORDER_INSERT =
            "INSERT INTO orders (id, number, status, user_id, address_book_id, order_time, amount, delivery_fee, "
            + "remark, phone, address, consignee, dining_type, rider_id, create_time, update_time, "
            + "is_deleted, tenant_id, version) "
            + "VALUES (:id, :number, :status, 9001, 3001, NOW(), 88.00, 5.00, '少放辣', '13900139001', "
            + "'北京市朝阳区三里屯幸福里3栋2单元1503室', '测试用户', 'OUTSIDE', NULL, NOW(), NOW(), 0, 999, 0)";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private NamedParameterJdbcTemplate named;

    @BeforeEach
    void setUp() {
        named = new NamedParameterJdbcTemplate(jdbc);
        BaseContext.setCurrentTenantId(999L);

        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5061 AND 5065");
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
    }

    /**
     * 清理本类数据：surefire 复用 JVM 且连真实 MySQL，遗留订单会污染其他测试类。
     */
    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5061 AND 5065");
        jdbc.update("DELETE FROM address_book WHERE id = 3001");
        jdbc.update("DELETE FROM user WHERE id = 9001");
    }

    private void insertOrder(long id, String number, int status) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("number", number)
                .addValue("status", status);
        named.update(ORDER_INSERT, p);
    }

    private MockHttpSession employeeSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("employee", 1L);
        s.setAttribute("tenantId", 999L);
        return s;
    }

    /**
     * 执行写接口并在失败时有界重试（应对后台定时任务并发导致的偶发死锁）。
     *
     * @param url      接口路径
     * @param id       订单ID
     * @param reason   原因（null 表示不传）
     */
    private void performWithRetry(String url, long id, String reason) throws Exception {
        String lastBody = "";
        for (int attempt = 1; attempt <= 3; attempt++) {
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder =
                    put(url).param("id", String.valueOf(id)).characterEncoding("UTF-8").session(employeeSession());
            if (reason != null) {
                builder = builder.param("reason", reason);
            }
            lastBody = mockMvc.perform(withCsrfToken(mockMvc, builder))
                    .andReturn().getResponse().getContentAsString();
            if (lastBody.contains("\"code\":1")) {
                return;
            }
            Thread.sleep(300L);
        }
        fail("接口连续 3 次失败: url=" + url + ", id=" + id + ", body=" + lastBody);
    }

    private Map<String, Object> orderRow(long id) {
        return jdbc.queryForMap(
                "SELECT status, remark, cancel_reason FROM orders WHERE id = ?", id);
    }

    @Test
    void cancelWritesReasonWithoutOverwritingRemark() throws Exception {
        insertOrder(5061L, "TEST-5061", 2);

        performWithRetry("/order/cancel", 5061L, "备货不足");

        Map<String, Object> row = orderRow(5061L);
        assertEquals(5, ((Number) row.get("status")).intValue());
        // 修改点(P0-5)：取消原因落到独立字段
        assertEquals("备货不足", row.get("cancel_reason"));
        // 修改点(P0-5)：顾客下单备注不被覆盖
        assertEquals("少放辣", row.get("remark"));
    }

    @Test
    void rejectWritesMerchantRejectReason() throws Exception {
        insertOrder(5062L, "TEST-5062", 2);

        performWithRetry("/order/reject", 5062L, null);

        Map<String, Object> row = orderRow(5062L);
        assertEquals(5, ((Number) row.get("status")).intValue());
        assertEquals("商家拒单", row.get("cancel_reason"));
        assertEquals("少放辣", row.get("remark"));
    }

    @Test
    void cancelWithoutReasonKeepsRemarkAndLeavesReasonNull() throws Exception {
        insertOrder(5063L, "TEST-5063", 2);

        performWithRetry("/order/cancel", 5063L, null);

        Map<String, Object> row = orderRow(5063L);
        assertEquals(5, ((Number) row.get("status")).intValue());
        assertEquals("少放辣", row.get("remark"));
        assertEquals(null, row.get("cancel_reason"));
    }
}
