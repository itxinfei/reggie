package com.reggie.module.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.sys.entity.Permission;
import com.reggie.module.sys.entity.Role;
import com.reggie.module.sys.mapper.PermissionMapper;
import com.reggie.module.sys.service.PermissionService;
import com.reggie.module.sys.service.RoleService;
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

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class RoleControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private TestDatabaseCleaner cleaner;

    private static final String ADMIN_BYPASS_ATTRIBUTE = "SUPER_ADMIN";

    private Long testRoleId;
    private Long testPermId;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("role", "permission", "role_permission", "system_config");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        // 创建测试权限
        Permission perm = new Permission();
        perm.setId(1L);
        perm.setPermissionName("菜品管理");
        perm.setPermissionKey("dish:view");
        perm.setPermissionType(Permission.TYPE_MENU);
        perm.setParentId(0L);
        perm.setStatus(1);
        perm.setCreateTime(java.time.LocalDateTime.now());
        perm.setUpdateTime(java.time.LocalDateTime.now());
        permissionMapper.insert(perm);
        testPermId = 1L;

        // 创建测试角色
        roleService.addTenantRole("店长", "manager", "店铺管理员", 10, 1);
        Role role = roleService.getOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, "manager"));
        testRoleId = role.getId();
    }

    // ==================== 分页查询 ====================

    @Test
    void testPage() throws Exception {
        mockMvc.perform(get("/sys/role/page")
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
                .andExpect(jsonPath("$.data.records[0].roleName").value("店长"));
    }

    @Test
    void testPageByRoleName() throws Exception {
        mockMvc.perform(get("/sys/role/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("roleName", "店长")
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
    void testPageEmpty() throws Exception {
        mockMvc.perform(get("/sys/role/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("roleName", "不存在的角色")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ==================== 统计 ====================

    @Test
    void testStats() throws Exception {
        mockMvc.perform(get("/sys/role/stats")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.enabled").value(1))
                .andExpect(jsonPath("$.data.disabled").value(0));
    }

    @Test
    void testStatsWithDisabled() throws Exception {
        roleService.addTenantRole("厨师", "chef", "厨房厨师", 5, 0);

        mockMvc.perform(get("/sys/role/stats")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.enabled").value(1))
                .andExpect(jsonPath("$.data.disabled").value(1));
    }

    // ==================== 列表 ====================

    @Test
    void testList() throws Exception {
        mockMvc.perform(get("/sys/role/list")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].roleName").value("店长"));
    }

    @Test
    void testListEmpty() throws Exception {
        // 逻辑删除后 @TableLogic 的 list 查不到，但需确保物理删除以验证 list 端点
        roleService.deleteTenantRole(testRoleId);
        // 再用物理删除兜底（确保 list 端点查不到）
        roleService.removeById(testRoleId);

        mockMvc.perform(get("/sys/role/list")
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

    // ==================== 新增 ====================

    @Test
    void testAdd() throws Exception {
        String json = "{\"roleName\":\"收银员\",\"roleKey\":\"cashier\",\"description\":\"前台收银\",\"sort\":5,\"status\":1}";

        mockMvc.perform(withCsrfToken(mockMvc, post("/sys/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("角色创建成功"));

        Role saved = roleService.getOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, "cashier"));
        assert saved != null;
        assert saved.getRoleName().equals("收银员");
        assert saved.getTenantId().equals(1L);
    }

    @Test
    void testAddDuplicateRoleKey() throws Exception {
        // roleKey="manager" 已存在于 setUp，service 抛 CustomException → 422
        String json = "{\"roleName\":\"重复角色\",\"roleKey\":\"manager\",\"description\":\"重复的key\"}";

        mockMvc.perform(withCsrfToken(mockMvc, post("/sys/role")
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
    void testAddWithoutRoleName() throws Exception {
        // 缺少 @NotBlank 字段 roleName，@Valid 校验 → 400
        String json = "{\"roleKey\":\"noName\"}";

        mockMvc.perform(withCsrfToken(mockMvc, post("/sys/role")
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
        String json = "{\"id\":" + testRoleId + ",\"roleName\":\"超级店长\",\"roleKey\":\"manager\",\"description\":\"升级后的店长\",\"sort\":20,\"status\":1}";

        mockMvc.perform(withCsrfToken(mockMvc, put("/sys/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("角色更新成功"));

        Role updated = roleService.getById(testRoleId);
        assert updated != null;
        assert updated.getRoleName().equals("超级店长");
        assert updated.getSort().equals(20);
    }

    @Test
    void testUpdateNotFound() throws Exception {
        // ID 9999 不存在，service 抛 CustomException → 422
        String json = "{\"id\":9999,\"roleName\":\"不存在\",\"roleKey\":\"nonexist\"}";

        mockMvc.perform(withCsrfToken(mockMvc, put("/sys/role")
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

    // ==================== 删除 ====================

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, delete("/sys/role/" + testRoleId)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("角色删除成功"));

        // 验证角色已被逻辑删除（@TableLogic 自动过滤，getById 查不到）
        Role deleted = roleService.getById(testRoleId);
        assert deleted == null;
    }

    @Test
    void testDeleteNotFound() throws Exception {
        // ID 9999 不存在，service 抛 CustomException → 422
        mockMvc.perform(withCsrfToken(mockMvc, delete("/sys/role/9999")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ==================== 权限相关 ====================

    @Test
    void testGetPermissions() throws Exception {
        List<Long> permIds = new ArrayList<>();
        permIds.add(testPermId);
        roleService.assignPermissions(testRoleId, permIds);

        mockMvc.perform(get("/sys/role/" + testRoleId + "/permissions")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value(testPermId));
    }

    @Test
    void testGetPermissionsEmpty() throws Exception {
        mockMvc.perform(get("/sys/role/" + testRoleId + "/permissions")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testAssignPermissions() throws Exception {
        List<Long> permIds = new ArrayList<>();
        permIds.add(testPermId);
        String body = "{\"permissionIds\":[" + testPermId + "]}";

        mockMvc.perform(withCsrfToken(mockMvc, put("/sys/role/" + testRoleId + "/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("权限分配成功"));

        List<Long> assigned = roleService.getPermissionIds(testRoleId);
        assert assigned != null && assigned.contains(testPermId);
    }

    @Test
    void testPermissionTree() throws Exception {
        mockMvc.perform(get("/sys/role/permissions/tree")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].permissionName").value("菜品管理"));
    }

    @Test
    void testOptions() throws Exception {
        mockMvc.perform(get("/sys/role/options")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.names").isArray())
                .andExpect(jsonPath("$.data.names[0]").value("店长"));
    }

    @Test
    void testOptionsWithMultipleRoles() throws Exception {
        roleService.addTenantRole("厨师", "chef", "厨房", 5, 1);

        String response = mockMvc.perform(get("/sys/role/options")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.names").isArray())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode dataNode =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("data");
        int namesSize = dataNode.has("names") ? dataNode.get("names").size() : 0;
        org.junit.jupiter.api.Assertions.assertEquals(2, namesSize);
    }
}