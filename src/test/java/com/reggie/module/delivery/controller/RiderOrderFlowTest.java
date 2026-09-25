package com.reggie.module.delivery.controller;

import com.reggie.common.BaseContext;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 骑手订单全流程测试：店长派单→骑手接单/取餐/送达、抢单大厅抢单、
 * 并发双抢 CAS 只 1 成功、非归属骑手被拒、离线骑手不可派/抢。
 * H2 内存库（含全量主表）+ 骑手两表 + 真实 Redis。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-delivery.sql", "classpath:schema-rider.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RiderOrderFlowTest extends com.reggie.controller.BaseControllerTest {

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

        // 先按固定主键清理上轮残留（只删测试专用 id，非全表删），保证可重复执行
        jdbc.update("DELETE FROM delivery_time_record WHERE order_id BETWEEN 5001 AND 5010");
        jdbc.update("DELETE FROM orders WHERE id BETWEEN 5001 AND 5010");
        jdbc.update("DELETE FROM rider_location_record WHERE tenant_id = 999");
        jdbc.update("DELETE FROM rider WHERE id IN (2001, 2002, 2003)");
        jdbc.update("DELETE FROM address_book WHERE id = 3001");
        jdbc.update("DELETE FROM user WHERE id = 9001");
        jdbc.update("DELETE FROM store_info WHERE id = 4001");

        jdbc.update("INSERT IGNORE INTO tenant (id, name, phone, address, contact, status, create_time, update_time) "
                + "VALUES (999, '自动化测试租户', '13800138099', '测试', '联系人', 1, NOW(), NOW())");
        jdbc.update("INSERT INTO store_info (id, tenant_id, store_code, contact_phone, longitude, latitude, "
                + "delivery_radius, is_delivery_enabled, create_time, update_time, is_deleted) "
                + "VALUES (4001, 999, 'BJ001', '13800138001', 116.466042, 39.911042, 3000, 1, NOW(), NOW(), 0)");
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

    private MockHttpSession userSession(long userId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("user", userId);
        s.setAttribute("tenantId", 999L);
        return s;
    }

    private Map<String, Object> riderRow(long id) {
        return jdbc.queryForMap("SELECT status, current_order_count, total_order_count FROM rider WHERE id = ?", id);
    }

    @Test
    void fullDispatchFlow() throws Exception {
        insertOrder(5001L, "TEST-5001", 2, null);

        // 店长派单
        mockMvc.perform(withCsrfToken(mockMvc, post("/order/dispatch")
                .session(employeeSession())
                .contentType("application/json")
                .content("{\"orderId\":5001,\"riderId\":2001}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("派单成功"));

        // 骑手待接单列表见任务
        mockMvc.perform(get("/api/rider/tasks/mine").param("scope", "todo").session(riderSession(RIDER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].id").value(5001));

        // 确认接单
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5001/accept")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("已接单"));
        Map<String, Object> afterAccept = riderRow(RIDER_A);
        assertEquals(2, ((Number) afterAccept.get("status")).intValue());
        assertEquals(1, ((Number) afterAccept.get("current_order_count")).intValue());

        // 确认取餐：主状态仍 3，记录取餐时间
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5001/pickup")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("已取餐"));
        assertEquals(3, jdbc.queryForObject("SELECT status FROM orders WHERE id=5001", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM delivery_time_record WHERE order_id=5001 AND pickup_time IS NOT NULL",
                Integer.class));

        // 确认送达：订单完成，骑手负载归零、累计 +1
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5001/deliver")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("已送达"));
        assertEquals(4, jdbc.queryForObject("SELECT status FROM orders WHERE id=5001", Integer.class));
        Map<String, Object> afterDeliver = riderRow(RIDER_A);
        assertEquals(1, ((Number) afterDeliver.get("status")).intValue());
        assertEquals(0, ((Number) afterDeliver.get("current_order_count")).intValue());
        assertEquals(1, ((Number) afterDeliver.get("total_order_count")).intValue());

        // 历史任务可见
        mockMvc.perform(get("/api/rider/tasks/mine").param("scope", "history").session(riderSession(RIDER_A)))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].number").value("TEST-5001"));
    }

    @Test
    void grabFromHall() throws Exception {
        insertOrder(5002L, "TEST-5002", 2, null);

        // 大厅有单
        mockMvc.perform(get("/api/rider/tasks/hall").session(riderSession(RIDER_A)))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].id").value(5002));

        // 抢单成功，订单直接进入配送中
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5002/grab")
                .session(riderSession(RIDER_A))))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("抢单成功"));
        assertEquals(3, jdbc.queryForObject("SELECT status FROM orders WHERE id=5002", Integer.class));
        assertEquals(RIDER_A, jdbc.queryForObject("SELECT rider_id FROM orders WHERE id=5002", Long.class));
        assertEquals(1, ((Number) riderRow(RIDER_A).get("current_order_count")).intValue());

        // 大厅清空
        mockMvc.perform(get("/api/rider/tasks/hall").session(riderSession(RIDER_A)))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void concurrentGrabOnlyOneSucceeds() throws Exception {
        insertOrder(5003L, "TEST-5003", 2, null);

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        long[] riders = {RIDER_A, RIDER_B};

        for (int i = 0; i < threads; i++) {
            final long riderId = riders[i];
            futures.add(pool.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    MvcResult mr = mockMvc.perform(withCsrfToken(mockMvc,
                            post("/api/rider/tasks/5003/grab")
                            .session(riderSession(riderId)))).andReturn();
                    return mr.getResponse().getContentAsString().contains("\"code\":1") ? 1 : 0;
                }
            }));
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        int success = 0;
        for (Future<Integer> f : futures) {
            success += f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, success, "并发双抢必须恰有 1 人成功");
        assertEquals(3, jdbc.queryForObject("SELECT status FROM orders WHERE id=5003", Integer.class));
        Long winner = jdbc.queryForObject("SELECT rider_id FROM orders WHERE id=5003", Long.class);
        assertTrue(RIDER_A == winner || RIDER_B == winner);
    }

    @Test
    void nonAssigneeRejected() throws Exception {
        // 已派给 A 的单，B 不能接单
        insertOrder(5004L, "TEST-5004", 2, RIDER_A);
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5004/accept")
                .session(riderSession(RIDER_B))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该订单未指派给你"));

        // A 配送中的单，B 不能取餐/送达
        insertOrder(5005L, "TEST-5005", 3, RIDER_A);
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5005/pickup")
                .session(riderSession(RIDER_B))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该订单不属于你"));
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5005/deliver")
                .session(riderSession(RIDER_B))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该订单不属于你"));
    }

    @Test
    void customerViewsLocalOrderTracking() throws Exception {
        // 自有骑手配送中订单（已接单、已取餐）
        insertOrder(5008L, "TEST-5008", 3, RIDER_A);
        // 骑手当前位置（接近收货地址：116.447219,39.937610）
        jdbc.update("UPDATE rider SET current_longitude=116.450000, current_latitude=39.930000, "
                + "last_location_time=NOW() WHERE id=?", RIDER_A);
        // 配送时效记录：接单 + 取餐时间齐全
        jdbc.update("INSERT INTO delivery_time_record (order_id, order_number, rider_id, rider_name, order_time, "
                + "accept_time, pickup_time, status, tenant_id, create_time, update_time) "
                + "VALUES (5008, 'TEST-5008', 2001, '张骑手', NOW(), NOW(), NOW(), 3, 999, NOW(), NOW())");
        // 订单明细（dishSummary 数据源）
        jdbc.update("INSERT INTO order_detail (id, order_id, name, dish_id, number, amount, "
                + "tenant_id, create_time, update_time) "
                + "VALUES (8001, 5008, '鱼香肉丝', 7001, 2, 20.00, 1, NOW(), NOW())");

        try {
            // MockMvc 不经过 LoginCheckFilter（@WebFilter 未进入 MockMvc 链），
            // 手动注入顾客身份，模拟 filter 对 BaseContext 的设置
            BaseContext.setCurrentId(9001L);

            // 下单顾客查询：结构与平台单同构
            mockMvc.perform(get("/api/delivery/tracking/5008").session(userSession(9001)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1))
                    .andExpect(jsonPath("$.data.status").value("DELIVERING"))
                    .andExpect(jsonPath("$.data.riderName").value("张骑手"))
                    .andExpect(jsonPath("$.data.riderPhone").value("13800000001"))
                    .andExpect(jsonPath("$.data.riderLocation.longitude").value(116.45))
                    .andExpect(jsonPath("$.data.riderLocation.latitude").value(39.93))
                    // 骑手与收货点距离约 0.88km
                    .andExpect(jsonPath("$.data.distance").isNumber())
                    .andExpect(jsonPath("$.data.dishSummary").value("鱼香肉丝 等2件商品"))
                    // 无 record.estimatedMinutes 时按距离估算
                    .andExpect(jsonPath("$.data.estimatedMinutes").isNumber());

            // IDOR：其他用户查询被拒
            BaseContext.setCurrentId(9002L);
            mockMvc.perform(get("/api/delivery/tracking/5008").session(userSession(9002)))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.msg").value("无权查看该订单"));

            // 不存在的订单
            BaseContext.setCurrentId(9001L);
            mockMvc.perform(get("/api/delivery/tracking/9999").session(userSession(9001)))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.msg").value("配送订单不存在"));

            // 非数字入参
            mockMvc.perform(get("/api/delivery/tracking/ABC123").session(userSession(9001)))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.msg").value("配送订单不存在"));
        } finally {
            BaseContext.remove();
        }
    }

    @Test
    void customerViewsPendingLocalOrderTracking() throws Exception {
        // 待接单、未指派骑手：PENDING，无骑手卡片但可看时间线
        insertOrder(5009L, "TEST-5009", 2, null);
        try {
            // MockMvc 不经过 LoginCheckFilter，手动注入顾客身份
            BaseContext.setCurrentId(9001L);
            mockMvc.perform(get("/api/delivery/tracking/5009").session(userSession(9001)))
                    .andExpect(jsonPath("$.code").value(1))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.riderName").doesNotExist())
                    .andExpect(jsonPath("$.data.distance").doesNotExist());
        } finally {
            BaseContext.remove();
        }
    }

    @Test
    void trackingRejectedWithoutUserSession() throws Exception {
        // 未登录（无会话、BaseContext 无身份）：归属校验拦截，code=0
        insertOrder(5009L, "TEST-5009", 2, null);
        mockMvc.perform(get("/api/delivery/tracking/5009"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void offlineRiderCannotBeDispatchedOrGrab() throws Exception {
        // 店长派单给离线骑手
        insertOrder(5006L, "TEST-5006", 2, null);
        mockMvc.perform(withCsrfToken(mockMvc, post("/order/dispatch")
                .session(employeeSession())
                .contentType("application/json")
                .content("{\"orderId\":5006,\"riderId\":2003}")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("骑手当前离线，无法派单"));
        assertEquals(null, jdbc.queryForObject("SELECT rider_id FROM orders WHERE id=5006", Long.class));

        // 离线骑手抢单
        insertOrder(5007L, "TEST-5007", 2, null);
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/tasks/5007/grab")
                .session(riderSession(RIDER_C))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("请先上线后再抢单"));
    }
}
