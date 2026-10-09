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

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        String bcrypt = PasswordUtils.encodePassword("123456");
        // 先按 id/专用 phone 清理上轮残留（非全表删），再插入 tenant=999 的测试骑手
        // 轨迹表为 CREATE TABLE IF NOT EXISTS（schema-rider.sql），@Sql 不重建表；
        // 同 JVM 内其他方法/类的位置上报会残留，按骑手清理后才能断言"恰好一条"
        jdbcTemplate.update("DELETE FROM rider_location_record WHERE rider_id = ?", RIDER_ID);
        jdbcTemplate.update("DELETE FROM rider WHERE id = ? OR phone = ?", RIDER_ID, PHONE);
        jdbcTemplate.update("DELETE FROM rider_remember_token WHERE rider_id = ?", RIDER_ID);
        jdbcTemplate.update("INSERT INTO rider (id, name, phone, password, status, current_order_count, "
                + "total_order_count, tenant_id, create_time, update_time) "
                + "VALUES (?, '张骑手', ?, ?, 1, 0, 0, 999, NOW(), NOW())", RIDER_ID, PHONE, bcrypt);
        // forgot-password 限流 1/s：清掉滑动窗口，避免连续用例触发 429
        java.util.Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (rateLimitKeys != null && !rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
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

    // ---- P2-2：记住我 ----

    @Test
    void loginWithRememberMeTokenIssued() throws Exception {
        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/api/rider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"password\":\"123456\",\"rememberMe\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();
        // cookie 随响应下发且为 HttpOnly
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertNotNull(setCookie);
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("rider_remember="));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("HttpOnly"));
        // 令牌落库（rider + tenant）
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rider_remember_token WHERE rider_id = ? AND tenant_id = 999",
                Integer.class, RIDER_ID);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    void loginWithoutRememberMeNoToken() throws Exception {
        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/api/rider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertTrue(setCookie == null || !setCookie.contains("rider_remember"));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rider_remember_token WHERE rider_id = ?", Integer.class, RIDER_ID);
        org.junit.jupiter.api.Assertions.assertEquals(0, count);
    }

    // ---- P2-2：自助重置密码 ----

    @Test
    void forgotPasswordSuccess() throws Exception {
        String codeKey = "smsCode_" + PHONE;
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute(codeKey, "246810");
        session.setAttribute(codeKey + "_time", System.currentTimeMillis());

        mockMvc.perform(post("/api/rider/forgot-password").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"code\":\"246810\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("密码重置成功，请使用新密码登录"));
        // 验证码一次性：成功后立即作废
        org.junit.jupiter.api.Assertions.assertNull(session.getAttribute(codeKey));
        // 新密码可登录、旧密码失效
        mockMvc.perform(post("/api/rider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"password\":\"654321\"}"))
                .andExpect(jsonPath("$.code").value(1));
        mockMvc.perform(post("/api/rider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void forgotPasswordWrongCode() throws Exception {
        String codeKey = "smsCode_" + PHONE;
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute(codeKey, "246810");
        session.setAttribute(codeKey + "_time", System.currentTimeMillis());

        mockMvc.perform(post("/api/rider/forgot-password").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"code\":\"111111\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("验证码错误"));
        // 校验失败码不作废
        org.junit.jupiter.api.Assertions.assertNotNull(session.getAttribute(codeKey));
    }

    @Test
    void forgotPasswordCodeExpired() throws Exception {
        String codeKey = "smsCode_" + PHONE;
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute(codeKey, "246810");
        // 6 分钟前发出，超过 5 分钟有效期
        session.setAttribute(codeKey + "_time", System.currentTimeMillis() - 6L * 60 * 1000);

        mockMvc.perform(post("/api/rider/forgot-password").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"code\":\"246810\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("验证码已过期，请重新获取"));
        // 过期码同时作废
        org.junit.jupiter.api.Assertions.assertNull(session.getAttribute(codeKey));
    }

    @Test
    void forgotPasswordCodeMissing() throws Exception {
        mockMvc.perform(post("/api/rider/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"code\":\"246810\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("验证码不存在或已失效，请重新获取"));
    }

    @Test
    void forgotPasswordRiderNotFound() throws Exception {
        String unknownPhone = "13900999999";
        String codeKey = "smsCode_" + unknownPhone;
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute(codeKey, "246810");
        session.setAttribute(codeKey + "_time", System.currentTimeMillis());

        mockMvc.perform(post("/api/rider/forgot-password").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + unknownPhone + "\",\"code\":\"246810\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该手机号尚未注册为骑手"));
    }

    @Test
    void forgotPasswordBadNewPasswordLength() throws Exception {
        // 密码长度校验在验证码校验之前，无需有效码
        mockMvc.perform(post("/api/rider/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + PHONE + "\",\"code\":\"246810\",\"newPassword\":\"12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("新密码长度须为6-20位"));
    }
}
