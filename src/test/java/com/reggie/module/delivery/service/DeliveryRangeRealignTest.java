package com.reggie.module.delivery.service;

import com.reggie.common.BaseContext;
import com.reggie.module.delivery.service.DeliveryEnhancedService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 配送范围圆心重配测试（圆心围绕门店）：
 * 圆形规则圆心对齐到该租户主门店（id 最小门店）坐标；多边形规则与无门店租户不处理。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DeliveryRangeRealignTest {

    private static final long TENANT = 999L;
    private static final long OTHER_TENANT = 888L;

    @Autowired
    private DeliveryEnhancedService deliveryEnhancedService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM delivery_range_rule WHERE tenant_id IN (?, ?)", TENANT, OTHER_TENANT);
        jdbcTemplate.update("DELETE FROM store_info WHERE tenant_id IN (?, ?)", TENANT, OTHER_TENANT);
    }

    @AfterEach
    void tearDown() {
        BaseContext.remove();
    }

    @Test
    void realignCircleRulesToMainStore() {
        // 主门店（id 最小）坐标 116.51,39.92；分店坐标不同，不应被采用
        jdbcTemplate.update("INSERT INTO store_info (tenant_id, longitude, latitude, pause_order, is_deleted) "
                + "VALUES (?, 116.5100000, 39.9200000, 0, 0)", TENANT);
        jdbcTemplate.update("INSERT INTO store_info (tenant_id, longitude, latitude, pause_order, is_deleted) "
                + "VALUES (?, 116.6000000, 39.8000000, 0, 0)", TENANT);
        // 两条圆心错误的圆形规则 + 一条多边形规则（无单一圆心，应跳过）
        jdbcTemplate.update("INSERT INTO delivery_range_rule (rule_name, range_type, "
                + "center_longitude, center_latitude, tenant_id) VALUES ('r1', 1, 100.1, 20.2, ?)", TENANT);
        jdbcTemplate.update("INSERT INTO delivery_range_rule (rule_name, range_type, "
                + "center_longitude, center_latitude, tenant_id) VALUES ('r2', 1, 100.3, 20.4, ?)", TENANT);
        jdbcTemplate.update("INSERT INTO delivery_range_rule (rule_name, range_type, "
                + "polygon_points, tenant_id) VALUES ('p', 2, '[[1,2],[3,4]]', ?)", TENANT);

        BaseContext.setCurrentTenantId(TENANT);
        int updated = deliveryEnhancedService.realignCircleCenters(TENANT);

        assertEquals(2, updated);
        List<Map<String, Object>> circles = jdbcTemplate.queryForList(
                "SELECT center_longitude AS lng, center_latitude AS lat "
                        + "FROM delivery_range_rule WHERE range_type = 1 AND tenant_id = ?", TENANT);
        assertEquals(2, circles.size());
        for (Map<String, Object> row : circles) {
            assertEquals(116.51, ((Number) row.get("lng")).doubleValue(), 0.0000001);
            assertEquals(39.92, ((Number) row.get("lat")).doubleValue(), 0.0000001);
        }
        // 多边形规则：圆心保持 NULL，polygon_points 不变
        Map<String, Object> polygon = jdbcTemplate.queryForMap(
                "SELECT center_longitude AS lng, polygon_points AS pts "
                        + "FROM delivery_range_rule WHERE range_type = 2 AND tenant_id = ?", TENANT);
        assertNull(polygon.get("lng"));
        assertEquals("[[1,2],[3,4]]", String.valueOf(polygon.get("pts")));
    }

    @Test
    void realignSkipsTenantWithoutStore() {
        // 有圆形规则但无门店 → 无法围绕门店重配，返回 0 且圆心不变
        jdbcTemplate.update("INSERT INTO delivery_range_rule (rule_name, range_type, "
                + "center_longitude, center_latitude, tenant_id) VALUES ('r', 1, 100.1, 20.2, ?)", OTHER_TENANT);

        BaseContext.setCurrentTenantId(OTHER_TENANT);
        int updated = deliveryEnhancedService.realignCircleCenters(OTHER_TENANT);

        assertEquals(0, updated);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT center_longitude AS lng FROM delivery_range_rule WHERE tenant_id = ?", OTHER_TENANT);
        assertEquals(100.1, ((Number) row.get("lng")).doubleValue(), 0.0000001);
    }
}
