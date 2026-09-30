package com.reggie.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 测试数据库清理工具（沙盒语义，2026-09-30 起）。
 *
 * <p><b>历史背景</b>：本类曾实现"非破坏清理"（只删 tenant_id=999 行），那是测试与开发
 * 共用 reggie 单库年代的产物——当时全表删除会清掉页面展示的业务数据。</p>
 *
 * <p><b>现状</b>：测试库 reggie_test 已与开发库 reggie 完全隔离（application-test.yml），
 * 且由 TestDbProvisioner 按 schema*.sql + reggie-test-baseline.sql 自动重建。
 * reggie_test 是<b>一次性沙盒</b>：这里的任何数据（包括基线演示数据）都可以被测试
 * 自由清写，测试声明的表由该测试<b>全权负责</b>。开发库 reggie 的数据安全由
 * "测试绝不连接它"保证，而非本类。</p>
 *
 * <p>清理规则：cleanTables 对列出的表执行全表 DELETE（结构保留，重置自增到区间头，
 * 避免与测试硬编码主键冲突）。表不存在时静默跳过。若未来出现"只想删部分行"的
 * 场景，用 {@link #cleanByCondition}。</p>
 *
 * @since 2026-08-25（2026-09-30 改为沙盒全清语义）
 */
@Component
public class TestDatabaseCleaner {

    /** 自动化测试专用租户 ID：测试数据默认挂在此租户下（TenantTestExecutionListener 全局注入） */
    public static final Long TEST_TENANT_ID = 999L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 清空指定表（仅 reggie_test 沙盒内；表不存在时静默跳过）。
     *
     * @param tables 表名列表
     */
    public void cleanTables(String... tables) {
        for (String table : tables) {
            if (!tableExists(table)) {
                // 表不存在：静默跳过（某些模块测试不需要所有表）
                continue;
            }
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    /**
     * 按自定义条件清理表数据。
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
}
