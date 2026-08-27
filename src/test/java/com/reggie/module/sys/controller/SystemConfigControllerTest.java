package com.reggie.module.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.sys.model.SystemConfig;
import com.reggie.module.sys.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.reggie.controller.BaseControllerTest;
import com.reggie.test.TestDatabaseCleaner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SystemConfigControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    private static final String ADMIN_BYPASS_ATTRIBUTE = "SUPER_ADMIN";

    private Long testConfigId;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("role", "permission", "role_permission", "system_config");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        // 创建测试配置
        systemConfigService.addTenantConfig("order.auto_cancel_minutes", "30");
        SystemConfig config = systemConfigService.getOne(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, "order.auto_cancel_minutes"));
        testConfigId = config.getId();
    }

    // ==================== 列表 ====================

    @Test
    void testList() throws Exception {
        mockMvc.perform(get("/sys/config/list")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].configKey").value("order.auto_cancel_minutes"));
    }

    @Test
    void testListEmpty() throws Exception {
        systemConfigService.deleteTenantConfig(testConfigId);

        mockMvc.perform(get("/sys/config/list")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testRootList() throws Exception {
        mockMvc.perform(get("/sys/config")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].configKey").value("order.auto_cancel_minutes"));
    }

    // ==================== 分页 ====================

    @Test
    void testPage() throws Exception {
        mockMvc.perform(get("/sys/config/page")
                .param("page", "1")
                .param("pageSize", "10")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testPageByConfigKey() throws Exception {
        mockMvc.perform(get("/sys/config/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("configKey", "auto_cancel")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testPageNotFound() throws Exception {
        mockMvc.perform(get("/sys/config/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("configKey", "不存在的配置")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ==================== 获取单个配置 ====================

    @Test
    void testGetConfig() throws Exception {
        mockMvc.perform(get("/sys/config/order.auto_cancel_minutes")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("30"));
    }

    @Test
    void testGetConfigNotFound() throws Exception {
        mockMvc.perform(get("/sys/config/nonexist.key")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ==================== 新增 ====================

    @Test
    void testAdd() throws Exception {
        String json = "{\"configKey\":\"delivery_fee\",\"configValue\":\"5.00\"}";

        mockMvc.perform(withCsrfToken(mockMvc, post("/sys/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("配置创建成功"));

        String value = systemConfigService.getConfig("delivery_fee");
        assert value != null && value.equals("5.00");
    }

    @Test
    void testAddDuplicateKey() throws Exception {
        // configKey="order.auto_cancel_minutes" 已存在于 setUp，service 抛 CustomException → 422
        String json = "{\"configKey\":\"order.auto_cancel_minutes\",\"configValue\":\"60\"}";

        mockMvc.perform(withCsrfToken(mockMvc, post("/sys/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testAddWithoutConfigKey() throws Exception {
        // 缺少 @NotBlank 字段 configKey，@Valid 校验 → 400
        String json = "{\"configValue\":\"test\"}";

        mockMvc.perform(withCsrfToken(mockMvc, post("/sys/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ==================== 更新 ====================

    @Test
    void testUpdate() throws Exception {
        String json = "{\"id\":" + testConfigId + ",\"configKey\":\"order.auto_cancel_minutes\",\"configValue\":\"60\"}";

        mockMvc.perform(withCsrfToken(mockMvc, put("/sys/config/" + testConfigId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("配置更新成功"));

        String value = systemConfigService.getConfig("order.auto_cancel_minutes");
        assert value != null && value.equals("60");
    }

    @Test
    void testUpdateNotFound() throws Exception {
        // ID 9999 不存在，service 抛 CustomException → 422
        // JSON 需包含 id 以通过 @Valid 校验（@NotNull），service 再按 ID 查不到
        String json = "{\"id\":9999,\"configKey\":\"order.auto_cancel_minutes\",\"configValue\":\"60\"}";

        mockMvc.perform(withCsrfToken(mockMvc, put("/sys/config/9999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ==================== 批量更新 ====================

    @Test
    void testBatchUpdate() throws Exception {
        String json = "["
                + "{\"id\":" + testConfigId + ",\"configKey\":\"order.auto_cancel_minutes\",\"configValue\":\"90\"}"
                + "]";

        mockMvc.perform(withCsrfToken(mockMvc, put("/sys/config/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("配置批量更新成功"));

        String value = systemConfigService.getConfig("order.auto_cancel_minutes");
        assert value != null && value.equals("90");
    }

    @Test
    void testRootBatchUpdate() throws Exception {
        String json = "["
                + "{\"configKey\":\"order.auto_cancel_minutes\",\"configValue\":\"120\"}"
                + "]";

        mockMvc.perform(withCsrfToken(mockMvc, put("/sys/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("配置批量更新成功"));
    }

    // ==================== 删除 ====================

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, delete("/sys/config/" + testConfigId)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("配置删除成功"));

        // 物理删除后直接查不到
        SystemConfig deleted = systemConfigService.getById(testConfigId);
        assert deleted == null;
    }

    @Test
    void testDeleteNotFound() throws Exception {
        // ID 9999 不存在，service 抛 CustomException → 422
        mockMvc.perform(withCsrfToken(mockMvc, delete("/sys/config/9999")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0));
    }
}
