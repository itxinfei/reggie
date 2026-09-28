package com.reggie.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据库只读体检探针（诊断工具，非回归用例）。
 *
 * <p>类名刻意不含 Test/Tests/TestCase，surefire 默认 include 匹配不到，
 * 常规 {@code mvn test} 不会执行；需要时显式触发：</p>
 * <pre>mvn test -o -Dtest=DbAuditProbe</pre>
 *
 * <p><b>刻意不起 Spring 上下文</b>：dev profile 的上下文会与正在运行的开发服务器抢同一份
 * Redis 消息流/定时任务，产生写库副作用。这里只用 DriverManager 直连，读
 * {@code application-<profile>.yml} 的 datasource 配置，全程 SELECT。</p>
 *
 * <p>目标库由 {@code -Dreggie.audit.profile} 选择，默认 {@code dev}（= 演示库 {@code reggie}，
 * 页面上真正展示的数据）；{@code test} = {@code reggie_test}（自动化测试库）。</p>
 *
 * <p>输出五段：① role 表唯一键残留证据 ② 表级行数与租户分布 ③ 数据真实性问题清单
 * ④ 外键孤儿 ⑤ 摘要。真实性体检只统计业务租户 1 的数据。
 * 报告以 UTF-8 写入 {@code target/db-audit-<profile>.txt}
 * （Maven 控制台是 GBK，中文会乱码，看报告请读该文件）。</p>
 *
 * @author reggie
 * @since 2026-09-27
 */
public class DbAuditProbe {

    private static final int MIN_ROWS = 10;
    private static final long DEV_TENANT = 1L;
    private static final long TEST_TENANT = 999L;

    private static final Pattern SAFE_IDENT = Pattern.compile("^[A-Za-z0-9_]+$");

    /** 单个单元格导出宽度：过长文本（富文本/JSON）截断，避免报告不可读 */
    private static final int CELL_MAX = 60;
    private static final int DUMP_LIMIT = 20;
    /** 时间打散只针对演示业务表；行政区划这类大字典整片同日导入是事实，打散反而失真 */
    private static final long SPREAD_MAX_ROWS = 500L;

    private Jdbc jdbcTemplate;
    private PrintWriter report;
    private String profile;

    @BeforeEach
    void open() throws Exception {
        profile = System.getProperty("reggie.audit.profile", "dev");
        String[] ds = readDataSource(profile);
        jdbcTemplate = Jdbc.open(ds[0], ds[1], ds[2]);
        File file = new File("target/db-audit-" + profile + ".txt");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("无法创建报告目录: " + parent.getAbsolutePath());
        }
        report = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
    }

    @AfterEach
    void close() {
        if (jdbcTemplate != null) {
            jdbcTemplate.close();
        }
        if (report != null) {
            report.flush();
            report.close();
        }
    }

    /** 从 application-{profile}.yml 取 spring.datasource（.druid 优先）的 url/username/password */
    @SuppressWarnings("unchecked")
    private static String[] readDataSource(String profile) throws Exception {
        File file = new File("src/test/resources/application-" + profile + ".yml");
        if (!file.isFile()) {
            file = new File("src/main/resources/application-" + profile + ".yml");
        }
        if (!file.isFile()) {
            throw new IllegalStateException("找不到 profile 配置文件: " + profile);
        }
        Map<String, Object> root;
        Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
        try {
            root = new Yaml().load(reader);
        } finally {
            reader.close();
        }
        Map<String, Object> spring = (Map<String, Object>) root.get("spring");
        Map<String, Object> datasource = spring == null ? null : (Map<String, Object>) spring.get("datasource");
        if (datasource == null) {
            throw new IllegalStateException(file + " 中无 spring.datasource 配置");
        }
        Map<String, Object> druid = (Map<String, Object>) datasource.get("druid");
        Map<String, Object> node = druid != null ? druid : datasource;
        String url = text(node.get("url"));
        String username = text(node.get("username"));
        String password = text(node.get("password"));
        if (url == null || url.contains("${")) {
            throw new IllegalStateException("无法解析 datasource.url: " + url);
        }
        return new String[]{url, username, password == null ? "" : password};
    }

    private static String text(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /** 同时输出到控制台与 UTF-8 报告文件 */
    private void say(String line) {
        System.out.println(line);
        if (report != null) {
            report.println(line);
        }
    }

    @Test
    void audit() {
        List<Table> tables = loadTables();
        say("[AUDIT] ==================== 数据库只读体检开始 ====================");
        say("[AUDIT] profile=" + profile + " 数据库=" + database() + " 物理表数=" + tables.size());

        auditRoleResidue();
        auditRowCounts(tables);
        List<String> smells = auditRealism(tables);
        auditOrphans(tables);
        emitRepairSql(tables);
        emitNormalizeDdl();
        dumpDdl();
        dumpSchema();
        auditDump();

        say("[AUDIT] -------------------- ⑤ 摘要 --------------------");
        say("[AUDIT] 真实性问题列数=" + smells.size());
        say("[AUDIT] ==================== 体检结束 ====================");
    }

    // ---------------------------------------------------------- ① role 残留

    private void auditRoleResidue() {
        say("[AUDIT] -------------------- ① role 唯一键残留 --------------------");
        if (!tableExists("role")) {
            say("[AUDIT] role 表不存在");
            return;
        }
        printRows("role 全部行",
                "SELECT id, tenant_id, role_key, role_name, status, is_deleted, create_time FROM role ORDER BY role_key, id");
        printRows("idx_role_key 索引定义",
                "SELECT index_name, column_name, non_unique FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'role'");
        printRows("employee_role 挂载情况",
                "SELECT er.role_id, r.role_key, r.tenant_id, COUNT(er.employee_id) AS bound_emp "
                        + "FROM employee_role er JOIN role r ON r.id = er.role_id "
                        + "GROUP BY er.role_id, r.role_key, r.tenant_id ORDER BY r.role_key");
    }

    // ---------------------------------------------------------- ② 行数与租户分布

    private void auditRowCounts(List<Table> tables) {
        say("[AUDIT] -------------------- ② 表行数 / 租户分布 --------------------");
        for (Table t : tables) {
            long total = queryForLong("SELECT COUNT(1) FROM " + t.sql);
            StringBuilder line = new StringBuilder();
            line.append("[AUDIT] 表 ").append(pad(t.name, 30)).append(" 总计=").append(total);
            if (t.hasTenant) {
                line.append(" 租户1=").append(countTenant(t, DEV_TENANT))
                        .append(" 租户999=").append(countTenant(t, TEST_TENANT))
                        .append(" 其他租户=").append(total - countTenant(t, DEV_TENANT) - countTenant(t, TEST_TENANT));
            }
            if (t.hasTenant && countTenant(t, DEV_TENANT) < MIN_ROWS) {
                line.append("  <<< 业务租户不足 ").append(MIN_ROWS).append(" 行");
            }
            long ghost = countGhostTenants(t);
            if (ghost > 0) {
                line.append("  <<< 幽灵租户（tenant_id 在 tenant 表不存在）").append(ghost).append(" 行");
            }
            say(line.toString());
            if (t.hasTenant) {
                printTenantSpread(t);
                reportGhostTenants(t);
            }
        }
    }

    private void printTenantSpread(Table t) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT tenant_id, COUNT(1) AS c FROM " + t.sql + " GROUP BY tenant_id ORDER BY c DESC LIMIT 12");
        StringBuilder sb = new StringBuilder("[AUDIT]   └─ " + t.name + " 租户分布: ");
        for (Map<String, Object> row : rows) {
            sb.append(row.get("tenant_id")).append("=").append(row.get("c")).append("  ");
        }
        say(sb.toString());
    }

    private long countTenant(Table t, long tenantId) {
        return queryForLong("SELECT COUNT(1) FROM " + t.sql + " WHERE tenant_id = " + tenantId);
    }

    /** tenant_id 非空且在 tenant 表里查无此租户的行（孤儿租户数据，页面永远看不到） */
    private long countGhostTenants(Table t) {
        if (!t.hasTenant || !tableExists("tenant")) {
            return 0L;
        }
        return queryForLong("SELECT COUNT(1) FROM " + t.sql + " ch WHERE ch.tenant_id IS NOT NULL "
                + "AND NOT EXISTS (SELECT 1 FROM `tenant` p WHERE p.id = ch.tenant_id)");
    }

    private void reportGhostTenants(Table t) {
        if (countGhostTenants(t) <= 0) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ch.tenant_id, COUNT(1) AS c FROM " + t.sql + " ch WHERE ch.tenant_id IS NOT NULL "
                        + "AND NOT EXISTS (SELECT 1 FROM `tenant` p WHERE p.id = ch.tenant_id) "
                        + "GROUP BY ch.tenant_id ORDER BY c DESC LIMIT 8");
        StringBuilder sb = new StringBuilder("[AUDIT]   └─ " + t.name + " 幽灵租户明细: ");
        for (Map<String, Object> row : rows) {
            sb.append(row.get("tenant_id")).append("=").append(row.get("c")).append("  ");
        }
        say(sb.toString());
    }

    // ---------------------------------------------------------- ③ 真实性体检

    private List<String> auditRealism(List<Table> tables) {
        say("[AUDIT] -------------------- ③ 数据真实性问题 --------------------");
        List<String> smells = new ArrayList<String>();
        for (Table t : tables) {
            for (Column c : t.columns) {
                String finding = checkColumn(t, c);
                if (finding != null) {
                    smells.add(t.name + "." + c.name);
                    say("[AUDIT] ! " + finding);
                }
            }
            checkTimestampSpread(t);
        }
        return smells;
    }

    private String checkColumn(Table t, Column c) {
        if (isPhone(c)) {
            return checkPhone(t, c);
        }
        if (isNameish(c)) {
            return checkPlaceholder(t, c);
        }
        if (isMoney(c)) {
            return checkMoney(t, c);
        }
        return null;
    }

    private String checkPhone(Table t, Column c) {
        Map<String, Object> r = jdbcTemplate.queryForMap(
                "SELECT COUNT(1) AS total, COUNT(DISTINCT " + c.sql + ") AS dis, "
                        + "SUM(" + c.sql + " IS NULL OR " + c.sql + " = '') AS blank, "
                        + "SUM(" + c.sql + " REGEXP '^[0-9]{11}$') AS ok11 "
                        + "FROM " + t.sql + scopeAnd(t, c.sql + " IS NOT NULL"));
        long total = asLong(r.get("total"));
        if (total == 0) {
            return null;
        }
        long distinct = asLong(r.get("dis"));
        long ok11 = asLong(r.get("ok11"));
        List<String> issues = new ArrayList<String>();
        if (distinct < total) {
            issues.add("重复 " + (total - distinct) + "/" + total);
        }
        if (ok11 < total) {
            issues.add("非11位数字 " + (total - ok11) + "/" + total);
        }
        issues.addAll(checkPlaceholderPatterns(t, c, total).values());
        if (issues.isEmpty()) {
            return null;
        }
        return t.name + "." + c.name + " [手机号] " + join(issues, "; ");
    }

    private String checkPlaceholder(Table t, Column c) {
        Map<String, Object> totalRow = jdbcTemplate.queryForMap(
                "SELECT COUNT(1) AS total, SUM(" + c.sql + " IS NULL OR " + c.sql + " = '') AS blank FROM "
                        + t.sql + scope(t));
        long total = asLong(totalRow.get("total"));
        if (total == 0) {
            return null;
        }
        List<String> issues = new ArrayList<String>();
        long blank = asLong(totalRow.get("blank"));
        if (blank > 0) {
            issues.add("空值 " + blank + "/" + total);
        }
        issues.addAll(checkPlaceholderPatterns(t, c, total).values());
        if (issues.isEmpty()) {
            return null;
        }
        return t.name + "." + c.name + " [文本] " + join(issues, "; ");
    }

    /** 命中 测试/test/xxx/demo/待填/123 等占位符模式的行数 */
    private Map<String, String> checkPlaceholderPatterns(Table t, Column c, long total) {
        Map<String, String> hits = new LinkedHashMap<String, String>();
        String[][] patterns = {
                {"含'测试'", "测试"},
                {"含'test'", "test"},
                {"含'demo'", "demo"},
                {"含'xxx'", "xxx"},
                {"含'待填'", "待填"},
                {"含'123'", "123"},
        };
        for (String[] p : patterns) {
            long n = queryForLong("SELECT COUNT(1) FROM " + t.sql
                    + scopeAnd(t, "LOWER(" + c.sql + ") LIKE '%" + p[1].toLowerCase() + "%'"));
            if (n > 0) {
                hits.put(p[0], p[0] + " " + n + "/" + total);
            }
        }
        return hits;
    }

    private String checkMoney(Table t, Column c) {
        Map<String, Object> r = jdbcTemplate.queryForMap(
                "SELECT COUNT(1) AS total, SUM(" + c.sql + " IS NULL) AS nulls, "
                        + "SUM(" + c.sql + " = 0) AS zeros, SUM(" + c.sql + " < 0) AS negs, "
                        + "MAX(" + c.sql + ") AS mx FROM " + t.sql + scope(t));
        long total = asLong(r.get("total"));
        if (total == 0) {
            return null;
        }
        List<String> issues = new ArrayList<String>();
        long zeros = asLong(r.get("zeros"));
        long negs = asLong(r.get("negs"));
        long nulls = asLong(r.get("nulls"));
        if (zeros > 0) {
            issues.add("零值 " + zeros + "/" + total);
        }
        if (negs > 0) {
            issues.add("负值 " + negs + "/" + total);
        }
        if (nulls == total) {
            issues.add("全为 NULL");
        }
        if (issues.isEmpty()) {
            return null;
        }
        return t.name + "." + c.name + " [金额 " + r.get("mx") + "] " + join(issues, "; ");
    }

    private void checkTimestampSpread(Table t) {
        for (Column ct : t.columns) {
            if (!isSeedTimestamp(ct.name) || !ct.isDateTimeLike()) {
                continue;
            }
            reportTimeConcentration(t, ct);
        }
    }

    private void reportTimeConcentration(Table t, Column ct) {
        Map<String, Object> r = jdbcTemplate.queryForMap(
                "SELECT COUNT(1) AS total, COUNT(DISTINCT " + ct.sql + ") AS dis, "
                        + "COUNT(DISTINCT DATE(" + ct.sql + ")) AS days, "
                        + "MIN(" + ct.sql + ") AS mn, MAX(" + ct.sql + ") AS mx FROM " + t.sql + scope(t));
        long total = asLong(r.get("total"));
        if (total < MIN_ROWS || total > SPREAD_MAX_ROWS) {
            // 大字典（行政区划一次性导入）整片同日是事实，不算失真
            return;
        }
        long distinctDates = asLong(r.get("days"));
        long distinctTs = asLong(r.get("dis"));
        if (distinctDates <= 1 || distinctTs == 1) {
            say("[AUDIT] ! " + t.name + "." + ct.name + " [时间集中] 行数=" + total
                    + " 不同日期=" + distinctDates + " 不同时刻=" + distinctTs
                    + " 区间=" + r.get("mn") + " ~ " + r.get("mx"));
        }
    }

    // ---------------------------------------------------------- ④ 外键孤儿

    /** 命名约定到物理表的额外映射（列名 → 父表名），resolveParent 猜不到时兜底 */
    private static final Map<String, String> FK_ALIAS = new LinkedHashMap<String, String>();
    static {
        FK_ALIAS.put("create_user", "employee");
        FK_ALIAS.put("update_user", "employee");
        FK_ALIAS.put("auditor_id", "employee");
        FK_ALIAS.put("operator_id", "employee");
        FK_ALIAS.put("rider_id", "employee");
        FK_ALIAS.put("sender_id", "employee");
    }

    private void auditOrphans(List<Table> tables) {
        say("[AUDIT] -------------------- ④ 外键孤儿（父行不存在） --------------------");
        int found = 0;
        for (Table t : tables) {
            for (Column c : t.columns) {
                Table parent = resolveParent(t, c, tables);
                if (parent == null) {
                    continue;
                }
                long orphans = queryForLong("SELECT COUNT(1) FROM " + t.sql + " ch WHERE ch." + c.sql
                        + " IS NOT NULL AND ch." + c.sql + " <> 0"
                        + scopedLive(t, "ch")
                        + " AND NOT EXISTS (SELECT 1 FROM " + parent.sql + " p WHERE p.id = ch." + c.sql + ")");
                if (orphans > 0) {
                    long total = queryForLong("SELECT COUNT(1) FROM " + t.sql + scope(t));
                    found++;
                    say("[AUDIT] ! " + t.name + "." + c.name + " → " + parent.name + ".id 孤儿 "
                            + orphans + "/" + total);
                }
            }
        }
        say("[AUDIT] 孤儿外键列数=" + found + "（未列出的即全部指向存在的父行）");
    }

    /**
     * 按命名约定推断父表：{@code dish_id → dish / dishes}，兜底走 {@link #FK_ALIAS}。
     * 父表必须存在且有 {@code id} 列，否则返回 null 跳过（宁漏不误报）。
     */
    private Table resolveParent(Table child, Column c, List<Table> tables) {
        String hint = null;
        if (c.name.endsWith("_id") && !c.name.equals("tenant_id") && !c.name.equals("id")) {
            hint = c.name.substring(0, c.name.length() - 3);
        } else if (c.name.endsWith("_user")) {
            hint = FK_ALIAS.get(c.name);
        } else {
            hint = FK_ALIAS.get(c.name);
        }
        if (hint == null) {
            return null;
        }
        String[] candidates = {hint, hint + "s", hint + "es"};
        for (String candidate : candidates) {
            for (Table t : tables) {
                if (t.name.equals(candidate) && t.find("id") != null && !t.name.equals(child.name)) {
                    return t;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------- ⑦ 修复 SQL 生成

    /** 旧种子器的占位值特征：测试t109_0 / TXT_t95_0 / t9_u0，全库扫一遍并给出删除语句 */
    private static final String JUNK_REGEXP =
            "'^(测试)?t[0-9]+(_[0-9]+)+$|^TXT_t[0-9]+(_[0-9]+)+$|^t[0-9]+_u[0-9]+$'";

    /**
     * 把扫描到的脏数据翻译成可执行 SQL，供人工复核后到演示库执行（本探针始终只读）。
     * 用 {@code -Dreggie.audit.sql=1} 开启。
     */
    private void emitRepairSql(List<Table> tables) {
        if (!"1".equals(System.getProperty("reggie.audit.sql", ""))) {
            return;
        }
        say("[AUDIT] ------------- ⑦ 修复 SQL（扫描生成，需人工确认后执行） -------------");

        say("-- ⑦-1 旧种子器留下的占位行");
        int junkTables = 0;
        for (Table t : tables) {
            StringBuilder detail = new StringBuilder();
            List<String> statements = new ArrayList<String>();
            for (Column c : t.columns) {
                if (!c.isText()) {
                    continue;
                }
                long n = queryForLong("SELECT COUNT(1) FROM " + t.sql + " WHERE " + c.sql + " REGEXP " + JUNK_REGEXP);
                if (n == 0) {
                    continue;
                }
                detail.append(' ').append(c.name).append('=').append(n);
                statements.add("DELETE FROM " + t.sql + " WHERE " + c.sql + " REGEXP " + JUNK_REGEXP + ";");
            }
            if (statements.isEmpty()) {
                continue;
            }
            junkTables++;
            say("-- " + t.name + " 占位行:" + detail);
            for (String sql : statements) {
                say(sql);
            }
        }
        say(junkTables == 0 ? "-- （未发现占位行）" : "-- 命中 " + junkTables + " 张表");

        say("-- ⑦-2 测试租户 999 落在演示库的残留");
        for (Table t : tables) {
            if (!t.hasTenant) {
                continue;
            }
            long n = queryForLong("SELECT COUNT(1) FROM " + t.sql + " WHERE tenant_id = " + TEST_TENANT);
            if (n > 0) {
                say("DELETE FROM " + t.sql + " WHERE tenant_id = " + TEST_TENANT + ";  -- " + n + " 行");
            }
        }

        say("-- ⑦-3 幽灵租户（tenant_id 在 tenant 表不存在）");
        boolean hasBranchTenants = tableExists("tenant")
                && queryForLong("SELECT COUNT(1) FROM `tenant` WHERE id BETWEEN 1000 AND 1008") > 0;
        for (Table t : tables) {
            if (!t.hasTenant) {
                continue;
            }
            long n = queryForLong("SELECT COUNT(1) FROM " + t.sql + " ch WHERE ch.tenant_id BETWEEN 2 AND 10"
                    + " AND NOT EXISTS (SELECT 1 FROM `tenant` p WHERE p.id = ch.tenant_id)");
            if (n == 0) {
                continue;
            }
            if (hasBranchTenants) {
                // 分店租户实际挂在 1000~1008，把 2~10 的编号平移过去即可全部对上
                say("UPDATE " + t.sql + " SET tenant_id = tenant_id + 998 WHERE tenant_id BETWEEN 2 AND 10;  -- "
                        + n + " 行");
            } else {
                say("DELETE FROM " + t.sql + " WHERE tenant_id BETWEEN 2 AND 10;  -- " + n + " 行（无分店租户可平移）");
            }
        }

        say("-- ⑦-4 测试租户主档");
        if (tableExists("tenant")) {
            say("DELETE FROM `tenant` WHERE id = " + TEST_TENANT + " OR name LIKE '%测试租户%';");
        }

        say("-- ⑦-5 角色与权限关联孤儿");
        say("DELETE rp FROM `role_permission` rp LEFT JOIN `permission` p ON p.id = rp.permission_id WHERE p.id IS NULL;");
        say("DELETE rp FROM `role_permission` rp LEFT JOIN `role` r ON r.id = rp.role_id WHERE r.id IS NULL;");
        if (tableExists("role")) {
            say("DELETE FROM `role` WHERE role_key LIKE 'TEST\\_%' OR role_key REGEXP " + JUNK_REGEXP + ";");
        }

        emitOrphanSql(tables);

        say("-- ⑦-7 录入时间整片相同的列：按主键打散，演示列表不再全挤在同一天");
        for (Table t : tables) {
            if (t.find("id") == null) {
                continue;
            }
            for (Column c : t.columns) {
                if (!c.isDateTimeLike() || !isSeedTimestamp(c.name)) {
                    continue;
                }
                long rows = queryForLong("SELECT COUNT(1) FROM " + t.sql + scope(t));
                if (rows < 5 || rows > SPREAD_MAX_ROWS) {
                    continue;
                }
                long distinctDays = queryForLong("SELECT COUNT(DISTINCT DATE(" + c.sql + ")) FROM " + t.sql + scope(t));
                if (distinctDays > 1) {
                    continue;
                }
                // 两列用同一个主键偏移量；update_time 兜底不早于建行列，避免时间倒挂。
                // 只能对 id 取模或用 DIV：雪花 ID 已是 BIGINT 上限附近，任何 id*N 都会 1690 溢出
                String shift = c.sql
                        + " - INTERVAL (`id` % 45) DAY"
                        + " - INTERVAL (`id` DIV 45 % 24) HOUR"
                        + " - INTERVAL (`id` DIV 1080 % 60) MINUTE";
                Column born = birthColumn(t);
                if (born != null && !isBirthColumn(c.name)) {
                    shift = "GREATEST(" + shift + ", " + born.sql + ")";
                }
                say("-- " + t.name + "." + c.name + " 全部 " + rows + " 行同一天");
                say("UPDATE " + t.sql + " SET " + c.sql + " = " + shift + " WHERE " + c.sql + " IS NOT NULL;");
            }
        }
    }

    /** 种子/演示数据里最常见的"批量导入时间戳"，业务时间列（pay_time 等）不参与打散 */
    private static final List<String> SEED_TIMESTAMPS = Arrays.asList(
            "create_time", "created_time", "update_time", "updated_time");

    private static boolean isSeedTimestamp(String column) {
        return SEED_TIMESTAMPS.contains(column);
    }

    /** 建行列的两种命名（老表 create_time，部分模块表用 created_time） */
    private static boolean isBirthColumn(String column) {
        return "create_time".equals(column) || "created_time".equals(column);
    }

    /** 表的建行列，用于给更新时间做倒挂兜底；没有则返回 null */
    private static Column birthColumn(Table t) {
        Column born = t.find("create_time");
        return born != null ? born : t.find("created_time");
    }

    /** ⑦-6 外键孤儿：*_user 直接改指真实员工，子行孤儿留给人工判断 */
    private void emitOrphanSql(List<Table> tables) {
        say("-- ⑦-6 其它外键孤儿（*_user 直接改指真实员工，子行孤儿留给人工判断）");
        long fallbackEmployee = tableExists("employee")
                ? queryForLong("SELECT COALESCE(MIN(id), 1) FROM `employee` WHERE tenant_id = " + DEV_TENANT) : 1L;
        for (Table t : tables) {
            for (Column c : t.columns) {
                Table parent = resolveParent(t, c, tables);
                if (parent == null) {
                    continue;
                }
                long orphans = queryForLong("SELECT COUNT(1) FROM " + t.sql + " ch WHERE ch." + c.sql
                        + " IS NOT NULL AND ch." + c.sql + " <> 0"
                        + scopedLive(t, "ch")
                        + " AND NOT EXISTS (SELECT 1 FROM " + parent.sql + " p WHERE p.id = ch." + c.sql + ")");
                if (orphans == 0) {
                    continue;
                }
                if (c.name.endsWith("_user")) {
                    say("UPDATE " + t.sql + " SET " + c.sql + " = " + fallbackEmployee + " WHERE " + c.sql
                            + " IS NOT NULL AND " + c.sql + " <> 0 AND NOT EXISTS (SELECT 1 FROM " + parent.sql
                            + " p WHERE p.id = " + t.sql + "." + c.sql + ");  -- " + orphans + " 行");
                } else {
                    say("-- 待确认: " + t.name + "." + c.name + " → " + parent.name + ".id 有 " + orphans
                            + " 行孤儿，可删除子行或改指现有父行");
                }
            }
        }
    }

    // ---------------------------------------------------------- 结构导出

    /**
     * 导出机器可读的表结构（TAB 分隔：表 / 列 / 类型 / 可空 / 默认值 / extra / 键 / 注释），
     * 用于和 Java 实体、Mapper XML 里真正用到的列做比对：{@code -Dreggie.audit.schema=1}。
     */
    private void dumpSchema() {
        if (!"1".equals(System.getProperty("reggie.audit.schema", ""))) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT table_name AS tb, column_name AS co, column_type AS ty, is_nullable AS nu, "
                        + "column_default AS df, extra AS ex, column_key AS ky, column_comment AS cm "
                        + "FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "ORDER BY table_name, ordinal_position");
        File file = new File("target/db-schema-" + profile + ".txt");
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            for (Map<String, Object> r : rows) {
                out.println(tsv(r.get("tb"), r.get("co"), r.get("ty"), r.get("nu"),
                        r.get("df"), r.get("ex"), r.get("ky"), r.get("cm")));
            }
        } catch (IOException e) {
            say("[AUDIT] ! 表结构导出失败: " + e.getMessage());
            return;
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        say("[AUDIT] 表结构已导出 " + rows.size() + " 列 -> " + file.getPath());
        dumpIndexes();
    }

    /** 索引进 {@code target/db-index-<profile>.txt}：唯一键决定种子器能不能复用取值 */
    private void dumpIndexes() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT table_name AS tb, index_name AS ix, non_unique AS nu, seq_in_index AS sq, "
                        + "column_name AS co FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() ORDER BY table_name, index_name, seq_in_index");
        File file = new File("target/db-index-" + profile + ".txt");
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            for (Map<String, Object> r : rows) {
                out.println(tsv(r.get("tb"), r.get("ix"), "0".equals(String.valueOf(r.get("nu"))) ? "UNIQUE" : "INDEX",
                        r.get("sq"), r.get("co")));
            }
        } catch (IOException e) {
            say("[AUDIT] ! 索引导出失败: " + e.getMessage());
            return;
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        say("[AUDIT] 索引已导出 " + rows.size() + " 条 -> " + file.getPath());
    }

    private static String tsv(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('\t');
            }
            sb.append(parts[i] == null ? "" : String.valueOf(parts[i]).replace('\t', ' '));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------- 大小写规范化 DDL

    /**
     * 生成"列名统一小写"的 ALTER 脚本：{@code -Dreggie.audit.normalize=1} 时写
     * {@code target/db-normalize-<profile>.sql}。
     *
     * <p>MySQL 的列名本身不区分大小写（区分的是表名），但混用会让手写 SQL、
     * {@code selectMaps} 的 Map key、以及迁移到区分大小写的引擎时踩坑，
     * 因此按 information_schema 原样重建列定义（类型/可空/默认值/extra/注释），只改名字大小写。</p>
     */
    private void emitNormalizeDdl() {
        if (!"1".equals(System.getProperty("reggie.audit.normalize", ""))) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT table_name AS tb, column_name AS co, column_type AS ty, is_nullable AS nu, "
                        + "column_default AS df, extra AS ex, column_comment AS cm, "
                        + "character_set_name AS cs, collation_name AS cl, "
                        + "generation_expression AS gen, ordinal_position AS pos "
                        + "FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "ORDER BY table_name, ordinal_position");
        Map<String, List<String>> byTable = new LinkedHashMap<String, List<String>>();
        List<String> skipped = new ArrayList<String>();
        for (Map<String, Object> r : rows) {
            String table = String.valueOf(r.get("tb"));
            String column = String.valueOf(r.get("co"));
            if (column.equals(column.toLowerCase())) {
                continue;
            }
            if (notBlank(r.get("gen"))) {
                skipped.add(table + "." + column + "（生成列，不能 CHANGE）");
                continue;
            }
            byTable.computeIfAbsent(table, k -> new ArrayList<String>())
                    .add("ALTER TABLE " + quote(table) + " CHANGE COLUMN " + quote(column) + " "
                            + quote(column.toLowerCase()) + " " + rebuildDefinition(r));
        }
        File file = new File("target/db-normalize-" + profile + ".sql");
        PrintWriter out = null;
        int total = 0;
        try {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            out.println("-- 列名统一小写（基准库：" + database() + "，生成时间见 git；执行前务必备份）");
            out.println("-- 只改标识符大小写，类型/可空/默认值/注释按 information_schema 原样重建");
            out.println("-- 生产 Linux 表名区分大小写，列名虽不区分，但混用会在 selectMaps / 跨引擎迁移时出问题");
            out.println();
            for (Map.Entry<String, List<String>> e : byTable.entrySet()) {
                out.println("-- " + e.getKey() + "（" + e.getValue().size() + " 列）");
                for (String sql : e.getValue()) {
                    out.println(sql + ";");
                    total++;
                }
                out.println();
            }
            if (!skipped.isEmpty()) {
                out.println("-- 跳过：");
                for (String s : skipped) {
                    out.println("--   " + s);
                }
            }
        } catch (IOException e) {
            say("[AUDIT] ! 规范化脚本生成失败: " + e.getMessage());
            return;
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        say("[AUDIT] 列名小写化脚本：" + byTable.size() + " 张表 / " + total + " 列 -> " + file.getPath()
                + (skipped.isEmpty() ? "" : "，跳过 " + skipped.size() + " 个生成列"));
    }

    /** 按 information_schema 还原一列的完整定义（不含名字） */
    private static String rebuildDefinition(Map<String, Object> r) {
        StringBuilder sb = new StringBuilder(String.valueOf(r.get("ty")));
        // 列级字符集与表级不同时必须带上，否则 CHANGE 会把它悄悄改回表默认
        if (notBlank(r.get("cs"))) {
            sb.append(" CHARACTER SET ").append(r.get("cs"));
        }
        if (notBlank(r.get("cl"))) {
            sb.append(" COLLATE ").append(r.get("cl"));
        }
        boolean nullable = "YES".equalsIgnoreCase(String.valueOf(r.get("nu")));
        sb.append(nullable ? " NULL" : " NOT NULL");
        Object def = r.get("df");
        String extra = r.get("ex") == null ? "" : String.valueOf(r.get("ex"));
        if (extra.contains("DEFAULT_GENERATED")) {
            sb.append(" DEFAULT (").append(def).append(')');
        } else if (def != null) {
            sb.append(" DEFAULT ").append(quoteDefault(String.valueOf(def), String.valueOf(r.get("ty"))));
        } else if (nullable) {
            sb.append(" DEFAULT NULL");
        }
        for (String part : extra.split("\\s+")) {
            // DEFAULT_GENERATED 只是"默认值是表达式"的标记，已按 DEFAULT (...) 输出过
            if (!part.isEmpty() && !part.equalsIgnoreCase("DEFAULT_GENERATED")) {
                sb.append(' ').append(part.toUpperCase(Locale.ROOT));
            }
        }
        String comment = r.get("cm") == null ? "" : String.valueOf(r.get("cm"));
        if (!comment.isEmpty()) {
            sb.append(" COMMENT '").append(comment.replace("'", "''")).append('\'');
        }
        return sb.toString();
    }

    /** 数值类默认值裸写，字符串/时间类加引号 */
    private static String quoteDefault(String value, String columnType) {
        String base = columnType.replaceAll("\\(.*\\)", "").toLowerCase(Locale.ROOT);
        boolean plain = NUMERIC_TYPES.contains(base) || "yes".equals(value) || "no".equals(value);
        return plain ? value : "'" + value.replace("'", "''") + "'";
    }

    private static final List<String> NUMERIC_TYPES = Arrays.asList(
            "tinyint", "smallint", "mediumint", "int", "bigint", "decimal", "numeric", "float", "double", "real", "bit");

    private static boolean notBlank(Object o) {
        return o != null && !String.valueOf(o).isEmpty();
    }

    /** 只读导出全库 SHOW CREATE TABLE，作为「库的真实结构」基准：{@code -Dreggie.audit.ddl=1} */
    private void dumpDdl() {
        if (!"1".equals(System.getProperty("reggie.audit.ddl", ""))) {
            return;
        }
        File file = new File("target/db-ddl-" + profile + ".sql");
        PrintWriter out = null;
        int n = 0;
        try {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            out.println("-- 由 DbAuditProbe 只读导出（SHOW CREATE TABLE），库：" + database());
            out.println("-- 这是「库现在的真实结构」，与 src/test/resources/schema-mysql.sql 可能有漂移");
            out.println();
            for (String table : tableNames()) {
                Map<String, Object> row = jdbcTemplate.queryForList("SHOW CREATE TABLE " + quote(table)).get(0);
                String ddl = null;
                for (Map.Entry<String, Object> e : row.entrySet()) {
                    String v = e.getValue() == null ? "" : String.valueOf(e.getValue());
                    if (v.regionMatches(true, 0, "CREATE TABLE", 0, 12)) {
                        ddl = v;
                        break;
                    }
                }
                if (ddl == null) {
                    say("[AUDIT] ! " + table + " 的 SHOW CREATE TABLE 结果里没找到建表语句，列名=" + row.keySet());
                    continue;
                }
                out.println(ddl);
                out.println(";");
                out.println();
                n++;
            }
        } catch (Exception e) {
            say("[AUDIT] ! DDL 导出失败: " + e.getMessage());
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        say("[AUDIT] 建表语句已导出 " + n + " 张表 -> " + file.getPath());
    }

    private List<String> tableNames() {
        List<String> names = new ArrayList<String>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT table_name AS tn FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() ORDER BY table_name")) {
            names.add(String.valueOf(row.get("tn")));
        }
        return names;
    }

    // ---------------------------------------------------------- ⑥ 指定表全量导出

    /**
     * 聚合指标看不出"这行数据像不像真的"，需要读原文时用这里导出前若干行：
     * {@code -Dreggie.audit.dump=permission,tenant,store}。默认不导出。
     * 无 tenant_id 列的全局表（permission/role_permission 等）自动退化为全表前 N 行。
     */
    private void auditDump() {
        String spec = System.getProperty("reggie.audit.dump", "");
        if (spec.trim().isEmpty()) {
            return;
        }
        say("[AUDIT] -------------------- ⑥ 指定表内容导出 --------------------");
        for (String table : spec.split(",")) {
            String name = table.trim().toLowerCase();
            if (name.isEmpty() || !SAFE_IDENT.matcher(name).matches() || !tableExists(name)) {
                say("[AUDIT]   # 跳过（不存在或表名非法）: " + table);
                continue;
            }
            String where = hasTenantColumn(name) ? " WHERE tenant_id = " + DEV_TENANT : "";
            printRows(name + " 样本 " + DUMP_LIMIT + " 行" + (where.isEmpty() ? "（全表）" : "（业务租户）"),
                    "SELECT * FROM " + quote(name) + where + " LIMIT " + DUMP_LIMIT);
        }
    }

    private boolean hasTenantColumn(String table) {
        return queryForLong("SELECT COUNT(1) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = '" + table + "' "
                + "AND COLUMN_NAME = 'tenant_id'") > 0;
    }

    // ---------------------------------------------------------- 辅助

    private boolean isPhone(Column c) {
        return c.isText() && (c.name.matches(".*(_phone|_mobile|phone|mobile|tel)$") || c.name.equals("phone"));
    }

    private boolean isNameish(Column c) {
        return c.isText() && !c.name.equals("tenant_id")
                && (c.name.matches(".*(_name|name|title|nickname|remark|comment)$"));
    }

    private boolean isMoney(Column c) {
        return c.isDecimal() && !c.name.matches(".*(count|num|sort|status|type|version|ratio|rate|discount|percent).*");
    }

    private List<Table> loadTables() {
        List<Table> tables = new ArrayList<Table>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT table_name AS tn FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() ORDER BY table_name")) {
            String name = String.valueOf(row.get("tn"));
            if (!SAFE_IDENT.matcher(name).matches()) {
                continue;
            }
            tables.add(new Table(name, loadColumns(name)));
        }
        return tables;
    }

    private List<Column> loadColumns(String table) {
        List<Column> columns = new ArrayList<Column>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME AS cn, DATA_TYPE AS dt FROM information_schema.COLUMNS "
                        + "WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ORDINAL_POSITION", table)) {
            columns.add(new Column(String.valueOf(row.get("cn")).toLowerCase(),
                    String.valueOf(row.get("dt")).toLowerCase()));
        }
        return columns;
    }

    private void printRows(String title, String sql) {
        say("[AUDIT]   # " + title);
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows.isEmpty()) {
                say("[AUDIT]     (无行)");
                return;
            }
            for (Map<String, Object> row : rows) {
                say("      " + formatRow(row));
            }
        } catch (Exception e) {
            say("[AUDIT]     查询失败: " + e.getMessage());
        }
    }

    private String formatRow(Map<String, Object> row) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append('=');
            Object v = entry.getValue();
            if (v == null) {
                sb.append("NULL");
                continue;
            }
            String s = String.valueOf(v);
            sb.append(s.length() > CELL_MAX ? s.substring(0, CELL_MAX) + "…" : s);
        }
        return sb.toString();
    }

    private boolean tableExists(String table) {
        return queryForLong("SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() "
                + "AND table_name = '" + table + "'") > 0;
    }

    private String database() {
        try {
            return String.valueOf(jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));
        } catch (Exception e) {
            return "?";
        }
    }

    private long queryForLong(String sql) {
        Long v = jdbcTemplate.queryForObject(sql, Long.class);
        return v == null ? 0L : v.longValue();
    }

    /** 真实性体检只看开发演示租户（tenant_id=1）且未软删的数据，测试租户 999 的脏数据不算问题 */
    private String scope(Table t) {
        return " " + where(scopeCond(t, null));
    }

    private String scopeAnd(Table t, String cond) {
        return " " + where(scopeCond(t, cond));
    }

    private static String scopeCond(Table t, String extra) {
        StringBuilder sb = new StringBuilder();
        if (t.hasTenant) {
            sb.append("tenant_id = ").append(DEV_TENANT);
        }
        if (t.hasSoftDelete) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append("is_deleted = 0");
        }
        if (extra != null) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append(extra);
        }
        return sb.toString();
    }

    private static String where(String cond) {
        return cond.isEmpty() ? "" : " WHERE " + cond;
    }

    /** 带表别名时的同款过滤，前面自带 AND，接在已有 WHERE 之后 */
    private static String scopedLive(Table t, String alias) {
        StringBuilder sb = new StringBuilder();
        if (t.hasTenant) {
            sb.append(" AND ").append(alias).append(".tenant_id = ").append(DEV_TENANT);
        }
        if (t.hasSoftDelete) {
            sb.append(" AND ").append(alias).append(".is_deleted = 0");
        }
        return sb.toString();
    }

    /** 标识符已由 SAFE_IDENT 白名单校验，反引号仅为规避 MySQL 保留字表名/列名 */
    private static String quote(String ident) {
        return "`" + ident + "`";
    }

    private long asLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String pad(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    /** 极简只读 JDBC 包装，方法名对齐 Spring JdbcTemplate，方便后续替换 */
    private static final class Jdbc implements AutoCloseable {
        private final Connection conn;

        private Jdbc(Connection conn) {
            this.conn = conn;
        }

        static Jdbc open(String url, String user, String password) throws SQLException {
            Connection c = DriverManager.getConnection(url, user, password);
            c.setReadOnly(true);
            return new Jdbc(c);
        }

        List<Map<String, Object>> queryForList(String sql) {
            return queryForList(sql, new Object[0]);
        }

        List<Map<String, Object>> queryForList(String sql, Object... args) {
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = conn.prepareStatement(sql);
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
                rs = ps.executeQuery();
                List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<String, Object>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            } catch (SQLException e) {
                throw new IllegalStateException("查询失败: " + sql, e);
            } finally {
                closeQuietly(rs, ps);
            }
        }

        Map<String, Object> queryForMap(String sql) {
            List<Map<String, Object>> rows = queryForList(sql);
            if (rows.isEmpty()) {
                throw new IllegalStateException("聚合查询无结果: " + sql);
            }
            return rows.get(0);
        }

        @SuppressWarnings("unchecked")
        <T> T queryForObject(String sql, Class<T> requiredType) {
            List<Map<String, Object>> rows = queryForList(sql);
            if (rows.isEmpty()) {
                return null;
            }
            Object v = rows.get(0).values().iterator().next();
            if (v == null) {
                return null;
            }
            if (requiredType == Long.class) {
                return (T) Long.valueOf(v instanceof Number ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v)));
            }
            if (requiredType == Integer.class) {
                return (T) Integer.valueOf(v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(String.valueOf(v)));
            }
            return requiredType.cast(String.valueOf(v));
        }

        private void closeQuietly(ResultSet rs, PreparedStatement ps) {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {
                // 关闭失败不影响结论
            }
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ignored) {
                // 同上
            }
        }

        @Override
        public void close() {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // 同上
            }
        }
    }

    private static final class Table {
        private final String name;
        private final String sql;
        private final List<Column> columns;
        private final boolean hasTenant;
        /** 有逻辑删除列时，软删掉的行不参与体检（页面本来就不展示它们） */
        private final boolean hasSoftDelete;

        Table(String name, List<Column> columns) {
            this.name = name;
            this.sql = quote(name);
            this.columns = columns;
            this.hasTenant = find("tenant_id") != null;
            this.hasSoftDelete = find("is_deleted") != null;
        }

        Column find(String column) {
            for (Column c : columns) {
                if (c.name.equals(column)) {
                    return c;
                }
            }
            return null;
        }
    }

    private static final class Column {
        private final String name;
        private final String sql;
        private final String type;

        Column(String name, String type) {
            this.name = name;
            this.sql = quote(name);
            this.type = type;
        }

        boolean isText() {
            return Arrays.asList("varchar", "char", "text", "mediumtext", "longtext").contains(type);
        }

        boolean isDecimal() {
            return Arrays.asList("decimal", "numeric").contains(type);
        }

        boolean isDateTimeLike() {
            return Arrays.asList("datetime", "timestamp", "date").contains(type);
        }
    }
}
