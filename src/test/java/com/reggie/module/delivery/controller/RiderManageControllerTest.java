package com.reggie.module.delivery.controller;

import com.reggie.common.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后台骑手管理控制器测试：新增（密码 BCrypt + 手机号查重）、分页、编辑不改密码、
 * 重置密码、删除与跨租户隔离。H2 内存库 + 真实 Redis。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-rider.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RiderManageControllerTest extends com.reggie.controller.BaseControllerTest {

    private static final String PHONE_A = "13900139001";
    private static final String PHONE_B = "13900139002";
    private static final String PHONE_OTHER_TENANT = "13900139003";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM rider WHERE tenant_id IN (999, 888)");
        // 其他租户的骑手（tenant=888），用于跨租户隔离用例
        jdbcTemplate.update("INSERT INTO rider (name, phone, password, status, current_order_count, "
                + "total_order_count, tenant_id, create_time, update_time) "
                + "VALUES ('别店骑手', ?, ?, 1, 0, 0, 888, NOW(), NOW())",
                PHONE_OTHER_TENANT, PasswordUtils.encodePassword("123456"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder employeeSession(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) {
        return req.sessionAttr("employee", 1L).sessionAttr("tenantId", 999L);
    }

    @Test
    @DisplayName("新增骑手成功：密码 BCrypt 入库且响应不含密码")
    void createSuccess() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(post("/api/delivery/rider"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("name","王骑手","phone",PHONE_A,"password","abc123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.password").doesNotExist());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT password, tenant_id, status FROM rider WHERE phone = ?", PHONE_A);
        String stored = String.valueOf(row.get("password"));
        org.assertj.core.api.Assertions.assertThat(stored).isNotEqualTo("abc123");
        org.assertj.core.api.Assertions.assertThat(PasswordUtils.matches("abc123", stored)).isTrue();
        org.assertj.core.api.Assertions.assertThat(row.get("tenant_id")).isEqualTo(999L);
        org.assertj.core.api.Assertions.assertThat(row.get("status")).isEqualTo(0);
    }

    @Test
    @DisplayName("同门店手机号重复时拒绝创建")
    void createDuplicatePhone() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(post("/api/delivery/rider"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("name","王骑手","phone",PHONE_A,"password","abc123"))))
                .andExpect(jsonPath("$.code").value(1));
        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(post("/api/delivery/rider"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("name","王骑手2","phone",PHONE_A,"password","abc123"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该手机号已存在骑手账号"));
    }

    @Test
    @DisplayName("手机号格式或密码长度不合法时拒绝")
    void createInvalidParam() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(post("/api/delivery/rider"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("name","王骑手","phone","12345","password","abc123"))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(post("/api/delivery/rider"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("name","王骑手","phone",PHONE_A,"password","123"))))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("分页查询只返回本租户骑手，支持姓名/手机号/状态过滤")
    void pageQuery() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(post("/api/delivery/rider"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("name","王骑手","phone",PHONE_A,"password","abc123"))))
                .andExpect(jsonPath("$.code").value(1));

        // 无过滤：只看到本租户骑手，别店骑手不可见
        mockMvc.perform(employeeSession(get("/api/delivery/rider/page")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].phone").value(PHONE_A));

        // 手机号模糊
        mockMvc.perform(employeeSession(get("/api/delivery/rider/page").param("phone", "1390013")))
                .andExpect(jsonPath("$.data.total").value(1));
        // 状态过滤：新建骑手离线(0)
        mockMvc.perform(employeeSession(get("/api/delivery/rider/page").param("status", "0")))
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(employeeSession(get("/api/delivery/rider/page").param("status", "1")))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("编辑资料不修改密码与工作状态")
    void editKeepsPasswordAndStatus() throws Exception {
        Long id = createRiderViaApi("王骑手", PHONE_A, "abc123");
        jdbcTemplate.update("UPDATE rider SET status = 1 WHERE id = ?", id);
        String before = String.valueOf(jdbcTemplate.queryForMap(
                "SELECT password FROM rider WHERE id = ?", id).get("password"));

        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(put("/api/delivery/rider"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("id",id,"name","王骑手改名","phone",PHONE_B))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("王骑手改名"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT password, status, phone FROM rider WHERE id = ?", id);
        org.assertj.core.api.Assertions.assertThat(row.get("password")).isEqualTo(before);
        org.assertj.core.api.Assertions.assertThat(row.get("status")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(String.valueOf(row.get("phone"))).isEqualTo(PHONE_B);
    }

    @Test
    @DisplayName("重置密码后新密码可校验通过")
    void resetPassword() throws Exception {
        Long id = createRiderViaApi("王骑手", PHONE_A, "abc123");
        mockMvc.perform(withCsrfToken(mockMvc,
                employeeSession(put("/api/delivery/rider/" + id + "/password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("password","newpass1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
        String stored = String.valueOf(jdbcTemplate.queryForMap(
                "SELECT password FROM rider WHERE id = ?", id).get("password"));
        org.assertj.core.api.Assertions.assertThat(PasswordUtils.matches("newpass1", stored)).isTrue();
        org.assertj.core.api.Assertions.assertThat(PasswordUtils.matches("abc123", stored)).isFalse();
    }

    @Test
    @DisplayName("删除骑手")
    void deleteRider() throws Exception {
        Long id = createRiderViaApi("王骑手", PHONE_A, "abc123");
        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(delete("/api/delivery/rider/" + id))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rider WHERE id = ?", Integer.class, id);
        org.assertj.core.api.Assertions.assertThat(cnt).isZero();
    }

    @Test
    @DisplayName("不能操作其他门店的骑手")
    void crossTenantForbidden() throws Exception {
        Long otherId = jdbcTemplate.queryForObject("SELECT id FROM rider WHERE phone = ?",
                Long.class, PHONE_OTHER_TENANT);
        mockMvc.perform(withCsrfToken(mockMvc,
                employeeSession(put("/api/delivery/rider/" + otherId + "/password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("password","hack123"))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(delete("/api/delivery/rider/" + otherId))))
                .andExpect(jsonPath("$.code").value(0));
    }


    /** 构造 JSON 请求体（避免手写转义） */
    private String body(Object... kv) throws Exception {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(m);
    }

    /** 通过后台接口创建骑手并返回ID */
    private Long createRiderViaApi(String name, String phone, String password) throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, employeeSession(post("/api/delivery/rider"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("name",name,"phone",phone,"password",password))))
                .andExpect(jsonPath("$.code").value(1));
        return jdbcTemplate.queryForObject("SELECT id FROM rider WHERE phone = ?", Long.class, phone);
    }
}
