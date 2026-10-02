package com.reggie.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 测试数据清理工具（单库隔离语义，2026-10-02 起）。
 *
 * <p><b>背景</b>：测试与开发共用本地 <code>reggie</code> 单库（application-test.yml）。
 * 库里同时有页面展示的演示数据（租户 <code>1</code>）和自动化测试数据（租户
 * {@link #TEST_TENANT_ID}）。因此<b>禁止全表 DELETE</b>——那会清掉演示数据。
 * 测试只清自己写过的数据，演示数据（尤其租户 1）永不被本类触碰。</p>
 *
 * <p><b>清理规则</b>（cleanTables）：</p>
 * <ul>
 *   <li>表不存在：静默跳过（某些模块测试用不到所有表）；</li>
 *   <li>表有 <code>tenant_id</code> 列：<code>DELETE ... WHERE tenant_id = 999</code>；</li>
 *   <li>表无 <code>tenant_id</code> 列：按 {@link #cleanGlobalTable} 登记的命名空间条件清理
 *       （<code>test:</code> / <code>test_</code> 前缀等）；未登记的表直接 fail-fast 抛异常，
 *       杜绝在单库下误删。</li>
 * </ul>
 *
 * <p>若测试把数据写在了非 999 租户（如模拟 seed 把 TEST_ 角色挂在租户 1），需该测试自行用
 * {@link #cleanByCondition} 显式清理——既有 RoleMapperTest / RoleControllerTest 已是此模式。</p>
 *
 * @since 2026-08-25（2026-10-02 回归单库租户 / 命名空间清理语义）
 */
@Component
public class TestDatabaseCleaner {

    /** 自动化测试专用租户 ID：测试数据默认挂在此租户下（TenantTestExecutionListener 全局注入） */
    public static final Long TEST_TENANT_ID = 999L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 清理指定表中的「测试数据」（单库下绝不全表清；表不存在时静默跳过）。
     *
     * @param tables 表名列表
     */
    public void cleanTables(String... tables) {
        for (String table : tables) {
            if (!tableExists(table)) {
                // 表不存在：静默跳过（某些模块测试不需要所有表）
                continue;
            }
            if (columnExists(table, "tenant_id")) {
                // 业务表：只删测试租户 999，保留租户 1 等演示数据
                jdbcTemplate.update("DELETE FROM " + table + " WHERE tenant_id = ?", TEST_TENANT_ID);
            } else {
                // 无租户列的全局表：按命名空间前缀清理
                cleanGlobalTable(table);
            }
        }
    }

    /**
     * 按自定义条件清理表数据（用于跨租户 / 特殊命名空间的清理，调用方对条件负责）。
     *
     * @param table  表名
     * @param cond   WHERE 条件（不含 WHERE 关键字）
     * @param params 参数
     */
    public void cleanByCondition(String table, String cond, Object... params) {
        String sql = "DELETE FROM " + table + " WHERE " + cond;
        jdbcTemplate.update(sql, params);
    }

    /**
     * 清理「无 tenant_id 列」的全局表。单库下此类表不能全删，必须靠命名空间前缀区分测试数据。
     * role_key / permission_key 为 utf8mb4_unicode_ci（大小写不敏感），小写前缀可同时匹配
     * test_ 与 TEST_。新增此类表时必须在此登记，否则 cleanTables 会 fail-fast。
     */
    private void cleanGlobalTable(String table) {
        switch (table) {
            case "permission":
                // 仅 RoleControllerTest 创建权限，permission_key 统一 "test:" 前缀
                jdbcTemplate.update("DELETE FROM permission WHERE permission_key LIKE 'test:%'");
                break;
            case "role_permission":
                // 关联表自身无租户，按其角色的 test_ 前缀清理
                jdbcTemplate.update("DELETE FROM role_permission WHERE role_id IN "
                        + "(SELECT id FROM role WHERE role_key LIKE 'test\\_%')");
                break;
            case "store_sync_log":
                // 跨店同步表用 source_tenant_id 表达来源
                jdbcTemplate.update("DELETE FROM store_sync_log WHERE source_tenant_id = ?", TEST_TENANT_ID);
                break;
            default:
                throw new IllegalStateException("表 [" + table + "] 无 tenant_id 列，cleanTables 禁止在单库下全表删除。"
                        + "请用 cleanByCondition 显式指定命名空间条件，并在 TestDatabaseCleaner.cleanGlobalTable 登记。");
        }
    }

    /** 判断表是否存在于当前数据库 */
    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return count != null && count.intValue() > 0;
    }

    /** 判断指定表是否存在某列 */
    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        return count != null && count.intValue() > 0;
    }
}
