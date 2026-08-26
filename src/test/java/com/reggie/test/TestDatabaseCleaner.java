package com.reggie.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 测试数据库清理工具
 *
 * 每个测试方法的 {@code @BeforeEach} 中调用 {@link #cleanTables(String...)} 清理旧数据，
 * 避免固定 ID 的主键冲突和数据残留。
 *
 * 使用方式：
 * <pre>
 * {@code
 * @Autowired
 * private TestDatabaseCleaner cleaner;
 *
 * @BeforeEach
 * void setUp() {
 *     cleaner.cleanTables("employee", "user", "address_book", "orders");
 *     // ... 插入测试数据
 * }
 * }
 * </pre>
 *
 * @since 2026-08-25
 */
@Component
public class TestDatabaseCleaner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 清理指定表的所有数据（执行 DELETE FROM table）。
     *
     * @param tables 表名列表
     */
    public void cleanTables(String... tables) {
        Arrays.stream(tables)
                .forEach(table -> {
                    try {
                        jdbcTemplate.update("DELETE FROM " + table);
                    } catch (Exception e) {
                        // 表不存在时忽略（某些模块测试不需要清理所有表）
                    }
                });
    }

    /**
     * 按条件清理表数据。
     *
     * @param table  表名
     * @param cond   WHERE 条件（不含 WHERE 关键字）
     * @param params 参数
     */
    public void cleanByCondition(String table, String cond, Object... params) {
        String sql = "DELETE FROM " + table + " WHERE " + cond;
        jdbcTemplate.update(sql, params);
    }
}