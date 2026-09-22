package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 租户平台管理接口测试（分页 / 详情 / 编辑 / 启停用 / 超管权限）。
 * MockMvc 中 LoginCheckFilter 不生效，超管身份通过 requestAttr("roleKey") + sessionAttr("employee") 模拟。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TenantManageControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantService tenantService;

    // 本类各用例专属租户（不清理 tenant 表，避免影响依赖租户的其他测试）
    private Long tenantId;
    private String uniqueName;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        uniqueName = "平台管理测试租户-" + System.currentTimeMillis();
        Tenant tenant = new Tenant();
        tenant.setName(uniqueName);
        tenant.setPhone("13800138000");
        tenant.setAddress("测试地址");
        tenant.setStatus(1);
        tenantService.save(tenant);
        tenantId = tenant.getId();
    }

    @Test
    void testPageAsAdmin() throws Exception {
        // 超管分页，按唯一名称过滤，应能查到本用例租户
        mockMvc.perform(get("/tenant/page")
                .requestAttr("roleKey", "SUPER_ADMIN")
                .sessionAttr("employee", 1L)
                .param("name", uniqueName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value(uniqueName));
    }

    @Test
    void testPageRejectedWithoutAdmin() throws Exception {
        // 不提供 roleKey（普通/未识别身份），AdminGuard 应 fail-closed 拒绝
        mockMvc.perform(get("/tenant/page")
                .sessionAttr("employee", 1L)
                .param("name", uniqueName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testGetInfoAsAdmin() throws Exception {
        mockMvc.perform(get("/tenant/" + tenantId)
                .requestAttr("roleKey", "SUPER_ADMIN")
                .sessionAttr("employee", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(tenantId));
    }

    @Test
    void testUpdateAsAdmin() throws Exception {
        String body = "{\"id\":" + tenantId + ",\"contact\":\"李经理\",\"packageName\":\"标准版\","
                + "\"expireTime\":\"2027-01-01 00:00:00\"}";
        mockMvc.perform(withCsrfToken(mockMvc, put("/tenant")
                .requestAttr("roleKey", "SUPER_ADMIN")
                .sessionAttr("employee", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Tenant updated = tenantService.getById(tenantId);
        org.junit.jupiter.api.Assertions.assertEquals("李经理", updated.getContact());
        org.junit.jupiter.api.Assertions.assertEquals("标准版", updated.getPackageName());
    }

    @Test
    void testDisableOtherTenantAsAdmin() throws Exception {
        // 禁用的是本用例租户（非当前登录租户 1L），允许
        mockMvc.perform(withCsrfToken(mockMvc, put("/tenant/status")
                .requestAttr("roleKey", "SUPER_ADMIN")
                .sessionAttr("employee", 1L)
                .param("id", String.valueOf(tenantId))
                .param("status", "0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(0), tenantService.getById(tenantId).getStatus());
    }

    @Test
    void testCannotDisableCurrentTenant() throws Exception {
        // 尝试禁用当前登录账号所属租户（1L），应被拒绝
        mockMvc.perform(withCsrfToken(mockMvc, put("/tenant/status")
                .requestAttr("roleKey", "SUPER_ADMIN")
                .sessionAttr("employee", 1L)
                .param("id", "1")
                .param("status", "0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
