package com.reggie.module.delivery.controller;

import com.reggie.common.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 骑手认证控制器测试：登录（成功/密码错/不存在/空值）、会话校验、登出。
 * H2 内存库 + 真实 Redis；登录前 selectByPhone 关闭租户过滤，无需建租户上下文。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-rider.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RiderAuthControllerTest extends com.reggie.controller.BaseControllerTest {

    private static final long RIDER_ID = 1001L;
    private static final String PHONE = "13900100001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        String bcrypt = PasswordUtils.encodePassword("123456");
        // 先按 id/专用 phone 清理上轮残留（非全表删），再插入 tenant=999 的测试骑手
        jdbcTemplate.update("DELETE FROM rider WHERE id = ? OR phone = ?", RIDER_ID, PHONE);
        jdbcTemplate.update("INSERT INTO rider (id, name, phone, password, status, current_order_count, "
                + "total_order_count, tenant_id, create_time, update_time) "
                + "VALUES (?, '张骑手', ?, ?, 1, 0, 0, 999, NOW(), NOW())", RIDER_ID, PHONE, bcrypt);
    }

    @Test
    void loginSuccess() throws Exception {
        mockMvc.perform(post("/api/rider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(RIDER_ID))
                .andExpect(jsonPath("$.data.name").value("张骑手"))
                // 密码任何情况下都不得出现在响应中
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void loginWrongPassword() throws Exception {
        mockMvc.perform(post("/api/rider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"password\":\"badpwd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("手机号或密码错误"));
    }

    @Test
    void loginPhoneNotFound() throws Exception {
        // 不存在账号与密码错误返回相同提示，避免账号枚举
        mockMvc.perform(post("/api/rider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13900000099\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("手机号或密码错误"));
    }

    @Test
    void loginBlankFields() throws Exception {
        mockMvc.perform(post("/api/rider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("手机号和密码不能为空"));
    }

    @Test
    void meWithoutLogin() throws Exception {
        // 无骑手会话：过滤器/守卫拦截，返回 code=0
        mockMvc.perform(get("/api/rider/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void meWithSession() throws Exception {
        mockMvc.perform(get("/api/rider/me")
                .sessionAttr("rider", RIDER_ID)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(RIDER_ID))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void logoutThenMeRejected() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/logout")
                .sessionAttr("rider", RIDER_ID)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("退出成功"));
    }

    @Test
    void onlineOfflineSwitch() throws Exception {
        // 下线
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/offline")
                .sessionAttr("rider", RIDER_ID)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("已下线"));
        // 再上线
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/online")
                .sessionAttr("rider", RIDER_ID)
                .sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("已上线"));
    }

    @Test
    void reportLocationSuccess() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/location")
                .sessionAttr("rider", RIDER_ID)
                .sessionAttr("tenantId", 999L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longitude\":116.397428,\"latitude\":39.90923,\"speed\":32.5,\"heading\":180}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("位置已更新"));

        // rider 当前位置与上报时间已落库
        java.util.Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT current_longitude, current_latitude, last_location_time FROM rider WHERE id = ?", RIDER_ID);
        org.junit.jupiter.api.Assertions.assertEquals(116.397428,
                ((Number) row.get("current_longitude")).doubleValue(), 0.000001);
        org.junit.jupiter.api.Assertions.assertEquals(39.90923,
                ((Number) row.get("current_latitude")).doubleValue(), 0.000001);
        org.junit.jupiter.api.Assertions.assertNotNull(row.get("last_location_time"));
        // 轨迹记录插入一条
        Integer recordCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rider_location_record WHERE rider_id = ?", Integer.class, RIDER_ID);
        org.junit.jupiter.api.Assertions.assertEquals(1, recordCount);
    }

    @Test
    void reportLocationMissingCoordinates() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/location")
                .sessionAttr("rider", RIDER_ID)
                .sessionAttr("tenantId", 999L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longitude\":116.397428}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("经纬度不能为空"));
    }

    @Test
    void reportLocationOutOfRange() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/location")
                .sessionAttr("rider", RIDER_ID)
                .sessionAttr("tenantId", 999L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longitude\":200,\"latitude\":39.9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("经纬度格式不正确"));
    }

    @Test
    void reportLocationWithoutLogin() throws Exception {
        // 无骑手会话：守卫拦截，code=0
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/rider/location"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longitude\":116.397428,\"latitude\":39.90923}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
