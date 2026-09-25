package com.reggie.config;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 全表种子数据初始化（反射驱动）。
 *
 * <p>启动时对 MyBatis-Plus 管理的每张表计数（有 tenant_id 列按 tenant=1，否则全表），
 * 少于 {@link #MIN_ROWS} 行就用 JdbcTemplate 补齐。只插入、缺多少补多少，绝不覆盖已有数据。</p>
 *
 * <p>本库无真实外键约束，因此只需要让每列满足 NOT NULL / 唯一约束：
 * 通过 information_schema 读取列的可空性、默认值、类型，按类型给合法值，
 * 字符串值带表序号+行号后缀保证唯一。偶发插入失败的表收集后告警，不阻断启动，
 * 最终由测试侧健康检查硬验证。</p>
 *
 * @author reggie
 * @since 2026-09-25
 */
@Slf4j
@Component
@Order(30)
public class TableSeeder implements ApplicationRunner {

    /** 每张表期望的最少行数 */
    private static final int MIN_ROWS = 10;

    /** 非自增主键使用的测试 ID 基段（避开 1~ 业务段与 990xxx 登录测试段） */
    private static final long STATIC_ID_BASE = 900000000L;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /** 全局自增序号，用于生成跨表唯一的字符串与静态 ID */
    private long seq = 0;

    @Override
    public void run(ApplicationArguments args) {
        List<TableInfo> tables = TableInfoHelper.getTableInfos();
        List<String> failures = new ArrayList<String>();
        int seededTables = 0;

        int tableIndex = 0;
        for (TableInfo info : tables) {
            tableIndex++;
            String table = info.getTableName();
            try {
                List<Col> cols = loadColumns(table);
                boolean tenantScoped = hasColumn(cols, "tenant_id");
                long count = countRows(table, tenantScoped);
                if (count >= MIN_ROWS) {
                    continue;
                }

                int need = MIN_ROWS - (int) count;
                String prefix = "t" + tableIndex;
                String pk = getPrimaryKey(table);
                boolean inserted = false;
                for (int r = 0; r < need; r++) {
                    if (insertRow(table, cols, tenantScoped, prefix, r, pk)) {
                        inserted = true;
                    }
                }
                if (inserted) {
                    seededTables++;
                } else {
                    failures.add(table + "（0 行插入成功）");
                }
            } catch (Exception e) {
                failures.add(table + "（" + e.getMessage() + "）");
            }
        }

        if (seededTables > 0) {
            log.info("全表种子数据：为 {} 张表补齐了数据（目标每表 {} 行）", seededTables, MIN_ROWS);
        }
        if (!failures.isEmpty()) {
            log.warn("全表种子：{} 张表未能补齐，不影响启动: {}", failures.size(), failures);
        }
    }

    /** 插入一行；返回是否成功 */
    private boolean insertRow(String table, List<Col> cols, boolean tenantScoped,
                              String prefix, int rowIndex, String pk) {
        List<String> names = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        for (Col col : cols) {
            // 自增主键交给数据库生成
            if (col.extra != null && col.extra.toLowerCase().contains("auto_increment")) {
                continue;
            }
            // 非自增主键：给测试段静态 ID
            if (pk != null && col.name.equalsIgnoreCase(pk)) {
                names.add(col.name);
                values.add(STATIC_ID_BASE + (++seq));
                continue;
            }
            // 有默认值且非必须的列：跳过，用数据库默认
            if (col.nullable || col.hasDefault) {
                // 租户列若可空但多租户表需要值，统一补 tenant=1
                if (col.name.equals("tenant_id") && tenantScoped) {
                    names.add(col.name);
                    values.add(1L);
                }
                continue;
            }
            names.add(col.name);
            values.add(buildValue(col, prefix, rowIndex));
        }

        if (names.isEmpty()) {
            // 表仅有自增主键、其余均可空/有默认：插一条全默认行
            try {
                jdbcTemplate.update("INSERT INTO " + table + " () VALUES ()");
                return true;
            } catch (Exception e) {
                log.debug("种子空行插入失败 {}: {}", table, e.getMessage());
                return false;
            }
        }
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sql.append(", ");
                marks.append(", ");
            }
            sql.append(names.get(i));
            marks.append("?");
        }
        sql.append(") VALUES (").append(marks).append(")");

        try {
            jdbcTemplate.update(sql.toString(), values.toArray());
            return true;
        } catch (Exception e) {
            log.debug("种子插入失败 {}: {}", table, e.getMessage());
            return false;
        }
    }

    /** 按列类型 / 列名构造一个合法且尽量唯一的值 */
    private Object buildValue(Col col, String prefix, int rowIndex) {
        String dataType = col.dataType == null ? "" : col.dataType.toLowerCase();

        // 租户列
        if (col.name.equals("tenant_id")) {
            return 1L;
        }
        // 手机号：生成 11 位合法且唯一号码（139 + 8 位补零）
        if (col.name.toLowerCase().contains("phone") || col.name.toLowerCase().contains("mobile")) {
            seq++;
            long n = (Math.abs((long) prefix.hashCode()) + rowIndex * 131L + seq) % 100000000L;
            return String.format("139%08d", n);
        }

        switch (dataType) {
            case "bigint":
                // 统一给唯一递增值：本库无外键，既可满足 NOT NULL，
                // 也避免 order_id / (employee_id,role_id) 等带唯一约束的关联列只插得进一条
                return ++seq;
            case "tinyint":
                return col.columnType != null && col.columnType.contains("tinyint(1)") ? 1 : 1;
            case "int":
            case "integer":
            case "smallint":
            case "mediumint":
                return 1;
            case "decimal":
            case "numeric":
            case "double":
            case "float":
                return new java.math.BigDecimal("10.00");
            case "datetime":
            case "timestamp":
                return new java.sql.Timestamp(System.currentTimeMillis());
            case "date":
                return new java.sql.Date(System.currentTimeMillis());
            case "time":
                return "12:00:00";
            case "json":
                return "{}";
            default:
                return uniqueText(col, prefix, rowIndex);
        }
    }

    /** 字符串列：带前缀与序号保证唯一，并按列长度截断 */
    private String uniqueText(Col col, String prefix, int rowIndex) {
        String base;
        String lower = col.name.toLowerCase();
        if (lower.equals("username") || lower.equals("account")) {
            base = prefix + "_u" + rowIndex;
        } else if (lower.contains("code") || lower.endsWith("_no") || lower.equals("number")) {
            base = prefix.toUpperCase() + String.format("%03d", rowIndex);
        } else if (lower.contains("name")) {
            base = "测试" + prefix + "_" + rowIndex;
        } else {
            base = "TXT_" + prefix + "_" + rowIndex;
        }
        if (col.length != null && col.length > 0 && base.length() > col.length) {
            base = base.substring(0, col.length);
        }
        return base;
    }

    private long countRows(String table, boolean tenantScoped) {
        String sql = "SELECT COUNT(1) FROM " + table
                + (tenantScoped ? " WHERE tenant_id = 1" : "");
        Long c = jdbcTemplate.queryForObject(sql, Long.class);
        return c == null ? 0L : c;
    }

    private String getPrimaryKey(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT column_name FROM information_schema.key_column_usage "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND constraint_name = 'PRIMARY' LIMIT 1",
                String.class, table);
    }

    private boolean hasColumn(List<Col> cols, String name) {
        for (Col c : cols) {
            if (c.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** 读取表的列元数据 */
    private List<Col> loadColumns(String table) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, column_type, is_nullable, column_default, extra, "
                        + "character_maximum_length FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position",
                table);
        List<Col> cols = new ArrayList<Col>();
        for (Map<String, Object> row : rows) {
            Col c = new Col();
            // MySQL 列名大小写不敏感，统一归一化为小写，避免与实体映射对不上
            c.name = String.valueOf(row.get("column_name")).toLowerCase();
            c.dataType = row.get("data_type") == null ? null : String.valueOf(row.get("data_type"));
            c.columnType = row.get("column_type") == null ? null : String.valueOf(row.get("column_type"));
            c.nullable = "YES".equalsIgnoreCase(String.valueOf(row.get("is_nullable")));
            c.hasDefault = row.get("column_default") != null;
            c.extra = row.get("extra") == null ? null : String.valueOf(row.get("extra"));
            Object len = row.get("character_maximum_length");
            c.length = len == null ? null : ((Number) len).intValue();
            cols.add(c);
        }
        return cols;
    }

    /** 列元数据内部结构 */
    private static class Col {
        String name;
        String dataType;
        String columnType;
        boolean nullable;
        boolean hasDefault;
        String extra;
        Integer length;
    }
}
