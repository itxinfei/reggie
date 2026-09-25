package com.reggie.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试数据库清理工具（非破坏版）
 *
 * 关键约束：测试与开发共用本地 MySQL（reggie），<b>严禁全表删除</b>。
 * 清理规则：
 * <ul>
 *   <li>含 {@code tenant_id} 列的表：只删测试租户 {@link #TEST_TENANT_ID}（999）的数据；</li>
 *   <li>无 {@code tenant_id} 列的表：必须在 {@link #NO_TENANT_WHERE} 显式登记安全条件，
 *       未登记直接抛 {@link IllegalArgumentException}（fail-loud，防止再次裸删）；</li>
 *   <li>表不存在时静默跳过（保持旧语义）。</li>
 * </ul>
 *
 * 使用方式：
 * <pre>
 * &#64;Autowired
 * private TestDatabaseCleaner cleaner;
 *
 * &#64;BeforeEach
 * void setUp() {
 *     cleaner.cleanTables("employee", "user", "address_book", "orders");
 *     // ... 插入 tenant_id=999 的测试数据
 * }
 * </pre>
 *
 * @since 2026-08-25
 */
@Component
public class TestDatabaseCleaner {

    /** 自动化测试专用租户 ID，测试数据全部挂在此租户下，与开发数据（tenant=1）隔离 */
    public static final Long TEST_TENANT_ID = 999L;

    /**
     * 无 tenant_id 列的表的安全清理条件，key=表名，value=WHERE 片段（不含 WHERE 关键字）。
     * 新增无租户表的清理时，必须在此登记。
     */
    private static final Map<String, String> NO_TENANT_WHERE = new HashMap<String, String>();
    static {
        // shopping_cart / dish_evaluation 在 MP 租户白名单内（表无 tenant_id），按专用测试用户隔离
        NO_TENANT_WHERE.put("shopping_cart", "user_id = 990001");
        NO_TENANT_WHERE.put("dish_evaluation", "user_id = 990001");
        // 权限相关为全局表，测试数据统一带前缀
        NO_TENANT_WHERE.put("permission", "permission_key LIKE 'test:_%'");
        NO_TENANT_WHERE.put("role_permission",
                "role_id IN (SELECT id FROM role WHERE tenant_id = 999 OR role_key LIKE 'TEST\\_%')");
        // tenant 表只允许删测试租户行，绝不删主租户
        NO_TENANT_WHERE.put("tenant", "id = 999");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 清理指定表的<b>测试数据</b>（tenant=999 或登记的安全条件），绝不全表删除。
     *
     * @param tables 表名列表
     */
    public void cleanTables(String... tables) {
        for (String table : tables) {
            if (!tableExists(table)) {
                // 表不存在：静默跳过（某些模块测试不需要所有表）
                continue;
            }
            String sql;
            if (hasTenantColumn(table)) {
                sql = "DELETE FROM " + table + " WHERE tenant_id = " + TEST_TENANT_ID;
            } else {
                String where = NO_TENANT_WHERE.get(table);
                if (where == null) {
                    throw new IllegalArgumentException(
                            "无 tenant_id 表未登记安全清理策略: " + table
                                    + "（请在 TestDatabaseCleaner.NO_TENANT_WHERE 中登记）");
                }
                sql = "DELETE FROM " + table + " WHERE " + where;
            }
            // 表已确认存在，其余错误（如 SQL 语法）如实抛出，不再吞掉
            jdbcTemplate.update(sql);
        }
    }

    /**
     * 按自定义条件清理表数据（调用方自行保证条件安全，禁止写成全表删）。
     *
     * @param table  表名
     * @param cond   WHERE 条件（不含 WHERE 关键字）
     * @param params 参数
     */
    public void cleanByCondition(String table, String cond, Object... params) {
        String sql = "DELETE FROM " + table + " WHERE " + cond;
        jdbcTemplate.update(sql, params);
    }

    /** 判断表是否存在于当前数据库 */
    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return count != null && count.intValue() > 0;
    }

    /** 判断表是否含 tenant_id 列 */
    private boolean hasTenantColumn(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = 'tenant_id'",
                Integer.class, table);
        return count != null && count.intValue() > 0;
    }
}
