package com.reggie.module.sys.mapper;

import com.reggie.module.sys.model.Role;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 角色租户兜底查询回归测试。
 *
 * <p>对应修复（2026-08-30）：role 表 seed 角色仅挂在 tenant_id=1（role_key=SUPER_ADMIN/STORE_MANAGER），
 * 而 role 表是全局共享角色表（idx_role_key 全局唯一索引）。
 * 原 SQL 用精确 tenant_id 匹配，导致非 tenant1 门店员工 DB 权限加载（PermissionAspect.loadPermissionsFromDb）
 * 查不到角色被拒（RBAC 失效）。修复后按「租户私有 → 公共(tenant_id IS NULL) → 全局 seed」三级兜底。
 *
 * @author SoftwareArchitect
 * @since 2026-08-30
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RoleMapperTest {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        // 幂等清理：仅清 TEST_ 前缀角色，绝不碰真实 role_key（role_key 为全局唯一索引）。
        // 必须先清 role_permission（其子查询依赖 role 行仍存在），再清 role 本身。
        cleaner.cleanByCondition("role_permission",
                "role_id IN (SELECT id FROM role WHERE role_key LIKE ?)", "TEST\\_%");
        cleaner.cleanByCondition("role", "role_key LIKE ?", "TEST\\_%");
        // 模拟 seed：全局角色挂在 tenant_id=1（idx_role_key 全局唯一，各租户共享）
        insertRole("超级管理员", "TEST_SUPER_ADMIN", 1L, 1);
        insertRole("店长", "TEST_STORE_MANAGER", 1L, 1);
        // 公共角色：tenant_id IS NULL
        insertRole("公共收银员", "TEST_CASHIER", null, 1);
        // 租户2私有角色
        insertRole("门店厨师", "TEST_CHEF", 2L, 1);
    }

    private void insertRole(String name, String key, Long tenantId, Integer status) {
        jdbcTemplate.update("INSERT INTO role (tenant_id, role_name, role_key, description, sort, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, ?, ?, ?, 0, ?, NOW(), NOW(), 0)",
                tenantId, name, key, "测试角色", status);
    }

    /** 租户1 查 seed 角色：精确命中自家角色 */
    @Test
    void findSeedRoleByTenant1() {
        Role role = roleMapper.findByRoleKeyAndTenantId(1L, "TEST_SUPER_ADMIN");
        assertNotNull(role);
        assertEquals("TEST_SUPER_ADMIN", role.getRoleKey());
        assertEquals(1L, role.getTenantId());
    }

    /** 核心修复：非 tenant1 门店员工查全局 seed 角色（原 SQL 返回 null → RBAC 失效） */
    @Test
    void findSeedRoleByNonTenant1_fallsBackToGlobalSeed() {
        Role role = roleMapper.findByRoleKeyAndTenantId(2L, "TEST_STORE_MANAGER");
        assertNotNull(role);
        assertEquals("TEST_STORE_MANAGER", role.getRoleKey());
        // 兜底命中挂载在租户1的全局 seed 角色
        assertEquals(1L, role.getTenantId());
    }

    /** 公共角色（tenant_id IS NULL）对任意租户可见 */
    @Test
    void findPublicRoleByAnyTenant() {
        Role role = roleMapper.findByRoleKeyAndTenantId(2L, "TEST_CASHIER");
        assertNotNull(role);
        assertEquals("TEST_CASHIER", role.getRoleKey());
        assertNull(role.getTenantId());
    }

    /** 租户私有角色优先命中 */
    @Test
    void findPrivateRoleOfTenant() {
        Role role = roleMapper.findByRoleKeyAndTenantId(2L, "TEST_CHEF");
        assertNotNull(role);
        assertEquals("TEST_CHEF", role.getRoleKey());
        assertEquals(2L, role.getTenantId());
    }

    /** 不存在的角色返回 null（安全降级，不放行） */
    @Test
    void findNonExistentRole_returnsNull() {
        Role role = roleMapper.findByRoleKeyAndTenantId(2L, "TEST_NO_SUCH_ROLE");
        assertNull(role);
    }
}
