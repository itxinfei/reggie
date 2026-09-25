package com.reggie.test;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.reggie.config.CoreDataSeeder;
import com.reggie.config.TableSeeder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据库结构与种子数据健康检查。
 *
 * <p>启动期种子器（CoreDataSeeder / TableSeeder）已先执行，本测试为只读对账：</p>
 * <ol>
 *   <li>实体表 vs 物理表存在对账；</li>
 *   <li>实体字段 vs 物理列对账；</li>
 *   <li>每张物理表至少 10 行（有 tenant_id 按 tenant=1，否则全表）；</li>
 *   <li>关键唯一索引缺失仅告警，不失败。</li>
 * </ol>
 * <p>表/列/计数问题全部收集后一次性失败，便于一次暴露全部漂移。</p>
 *
 * @author reggie
 * @since 2026-09-25
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseSchemaHealthCheckTest {

    private static final int MIN_ROWS = 10;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CoreDataSeeder coreDataSeeder;

    @Autowired
    private TableSeeder tableSeeder;

    /**
     * @SpringBootTest 不会执行 ApplicationRunner，这里显式触发种子器（幂等），
     * 保证全表 ≥10 后再做对账。
     */
    @BeforeAll
    void seed() {
        coreDataSeeder.run(null);
        tableSeeder.run(null);
    }

    @Test
    void databaseShouldMatchEntitiesAndHaveEnoughRows() {
        List<String> problems = new ArrayList<String>();

        Set<String> physicalTables = loadPhysicalTables();

        for (TableInfo info : TableInfoHelper.getTableInfos()) {
            // MySQL 库内表名/列名可能大小写混用，统一归一化为小写对账
            String table = info.getTableName().toLowerCase();

            // 1. 表存在对账
            if (!physicalTables.contains(table)) {
                problems.add("缺失物理表: " + table);
                continue;
            }

            // 2. 字段→列对账
            Set<String> columns = loadColumns(table);
            for (TableFieldInfo field : info.getFieldList()) {
                if (!columns.contains(field.getColumn().toLowerCase())) {
                    problems.add(table + " 缺失列: " + field.getColumn());
                }
            }

            // 3. 种子计数
            boolean tenantScoped = columns.contains("tenant_id");
            long count = countRows(table, tenantScoped);
            if (count < MIN_ROWS) {
                problems.add(table + " 行数不足: " + count + " < " + MIN_ROWS
                        + (tenantScoped ? "（按 tenant_id=1）" : "（全表）"));
            }
        }

        // 4. 关键唯一索引（仅告警）
        warnUniqueIndex("employee", "username");
        warnUniqueIndex("permission", "permission_key");
        warnUniqueIndex("orders", "platform_order_id");

        assertTrue(problems.isEmpty(),
                "数据库健康检查发现 " + problems.size() + " 个问题:\n - "
                        + String.join("\n - ", problems));
    }

    private void warnUniqueIndex(String table, String column) {
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = ? "
                            + "AND column_name = ? AND non_unique = 0",
                    Integer.class, table, column);
            if (n == null || n == 0) {
                System.out.println("[健康检查-告警] " + table + "." + column + " 上未发现唯一索引");
            }
        } catch (Exception ignored) {
            // 告警项不影响结论
        }
    }

    private long countRows(String table, boolean tenantScoped) {
        String sql = "SELECT COUNT(1) FROM " + table
                + (tenantScoped ? " WHERE tenant_id = 1" : "");
        Long c = jdbcTemplate.queryForObject(sql, Long.class);
        return c == null ? 0L : c;
    }

    private Set<String> loadColumns(String table) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ?", table);
        Set<String> columns = new HashSet<String>();
        for (Map<String, Object> row : rows) {
            columns.add(String.valueOf(row.get("column_name")).toLowerCase());
        }
        return columns;
    }

    private Set<String> loadPhysicalTables() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE()");
        Set<String> tables = new HashSet<String>();
        for (Map<String, Object> row : rows) {
            tables.add(String.valueOf(row.get("table_name")).toLowerCase());
        }
        return tables;
    }
}
