package com.reggie.test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试库自动重建器（"一次性沙盒"语义，解决两个历史痛点）：
 * <ol>
 *   <li><b>双库双迁移</b>：过去给实体加列后要手动在 reggie / reggie_test 各执行一次 ALTER，
 *       漏掉一边就出现 {@code Unknown column} 全量失败（2026-09-30 notice 列实际发生）。
 *       现在结构指纹（所有 schema*.sql 内容的 MD5）变化时自动 DROP+CREATE+重建全量表结构，
 *       reggie_test 永远不需要手动迁移；结构没变则直接复用（只花一次 SELECT）。</li>
 *   <li><b>数据被误删的担忧</b>：测试代码含大量 {@code DELETE FROM ...} 清库动作，
 *       这就是测试必须与开发库 reggie 隔离的原因——隔离后 reggie 里的业务数据永远不被测试触碰。
 *       本类只允许重建名字以 {@code _test}/{@code -test} 结尾的库，配错库会被拒绝而不是清库。</li>
 * </ol>
 *
 * <p>触发时机：Spring 测试上下文刷新前（见 {@link TestDbProvisionCustomizerFactory}），
 * 每个 JVM 最多执行一次重建；结构一致时快速跳过。纯 JDBC，不依赖 Spring 上下文。</p>
 *
 * <p>重建 = 应用 schema.sql + 全部 schema-*.sql（仅结构）+ reggie-test-baseline.sql
 * （开发库 reggie 的全量数据底座，无建库语句/存储过程）。启动期种子器已于 2026-09-30
 * 删除；本类不生成任何假数据，数据来源只有基线转储与各测试类 @Sql 种子。</p>
 *
 * @author reggie
 * @since 2026-09-30
 */
public final class TestDbProvisioner {

    /** 只重建名字以 _test / -test 结尾的库——防止测试配置误指业务库被整库 DROP */
    private static final Pattern TEST_DB_SUFFIX = Pattern.compile(".*(_test|-test)$");

    /** 不参与重建的脚本：陈旧参考转储（含 DROP TABLE，仅 DbAuditProbe 结构对照用）与类内数据种子 */
    private static final Set<String> EXCLUDED_FILES = new HashSet<String>(Arrays.asList(
            "schema-mysql.sql", "schema-test-orders.sql"));

    private static final String MARKER_TABLE = "_test_db_provision";

    private TestDbProvisioner() {
    }

    /**
     * 检查并按需重建测试库。失败直接抛异常阻断测试（fail-fast 优于带病跑在漂移结构上）。
     */
    public static void provision() {
        try {
            File testClassesDir = new File(TestDbProvisioner.class.getResource("/").toURI());
            List<File> schemaFiles = collectSchemaFiles(testClassesDir);
            File baseline = new File(testClassesDir, "reggie-test-baseline.sql");
            String fingerprint = fingerprint(schemaFiles, baseline);

            String ymlText = new String(Files.readAllBytes(
                    new File(testClassesDir, "application-test.yml").toPath()), StandardCharsets.UTF_8);
            String url = extractJdbcUrl(ymlText);
            String username = extractYamlValue(ymlText, "username");
            String password = extractYamlValue(ymlText, "password");

            // 解析 jdbc:mysql://host:port/db?query —— 注意 query 里可能有斜杠
            // （如 serverTimezone=Asia/Shanghai），必须先截断 query 再找路径斜杠
            int question = url.indexOf('?');
            String query = question >= 0 ? url.substring(question) : "";
            String pathPart = question >= 0 ? url.substring(0, question) : url;
            int slash = pathPart.indexOf('/', "jdbc:mysql://".length());
            if (slash < 0) {
                throw new IllegalStateException("JDBC URL 缺少库名路径段: " + url);
            }
            String baseUrl = pathPart.substring(0, slash);
            String dbName = pathPart.substring(slash + 1);

            if (!TEST_DB_SUFFIX.matcher(dbName).matches()) {
                throw new IllegalStateException("测试库名必须以 _test/-test 结尾才允许自动重建，当前: "
                        + dbName + "（防止误清业务库，请检查 application-test.yml）");
            }

            Class.forName("com.mysql.cj.jdbc.Driver");

            if (fingerprint.equals(readMarker(baseUrl, query, dbName, username, password))) {
                System.out.println("[TestDbProvision] reggie_test 结构未变，跳过重建");
                return;
            }

            System.out.println("[TestDbProvision] 结构指纹变化，重建测试库 " + dbName + " ...");
            try (Connection c = DriverManager.getConnection(baseUrl, username, password);
                 Statement st = c.createStatement()) {
                st.execute("DROP DATABASE IF EXISTS " + dbName);
                st.execute("CREATE DATABASE " + dbName
                        + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
            try (Connection c = DriverManager.getConnection(
                    baseUrl + "/" + dbName + query, username, password)) {
                int count = 0;
                for (File f : schemaFiles) {
                    for (String sql : splitStatements(
                            new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8))) {
                        try (Statement st = c.createStatement()) {
                            st.execute(sql);
                        }
                        count++;
                    }
                }
                // 基线数据（reggie 全库转储，无建库语句/存储过程）：恢复与开发库一致的
                // 数据底座，覆盖"旧 reggie_test 由数据转储初始化"的历史语义。
                // 内含 DROP TABLE IF EXISTS + INSERT，对共享表以开发库为准；仅测试库有的表不受影响。
                if (baseline.exists()) {
                    for (String sql : splitStatements(
                            new String(Files.readAllBytes(baseline.toPath()), StandardCharsets.UTF_8))) {
                        try (Statement st = c.createStatement()) {
                            st.execute(sql);
                        }
                        count++;
                    }
                }
                try (Statement st = c.createStatement()) {
                    st.execute("DROP TABLE IF EXISTS " + MARKER_TABLE);
                    st.execute("CREATE TABLE " + MARKER_TABLE
                            + " (version varchar(64) NOT NULL, PRIMARY KEY (version))");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO " + MARKER_TABLE + " (version) VALUES (?)")) {
                    ps.setString(1, fingerprint);
                    ps.executeUpdate();
                }
                System.out.println("[TestDbProvision] 重建完成: " + schemaFiles.size()
                        + " 个结构文件 + " + (baseline.exists() ? "基线数据 " : "")
                        + "共 " + count + " 条 SQL。测试库为一次性沙盒，业务数据请放 reggie");
            }
        } catch (Exception e) {
            throw new IllegalStateException("测试库自动重建失败: " + e.getMessage(), e);
        }
    }

    /** schema.sql + schema-*.sql（排除陈旧转储与数据种子），按文件名排序保证幂等顺序 */
    private static List<File> collectSchemaFiles(File dir) {
        File[] all = dir.listFiles();
        if (all == null) {
            throw new IllegalStateException("测试类路径目录不可读: " + dir);
        }
        List<File> files = new ArrayList<File>();
        for (File f : all) {
            String name = f.getName();
            if (name.startsWith("schema") && name.endsWith(".sql") && !EXCLUDED_FILES.contains(name)) {
                files.add(f);
            }
        }
        Collections.sort(files);
        // schema.sql 是核心表的权威定义，必须最先建表：多个模块 schema 文件里存在
        // 同名表的旧版定义（如 orders 缺 pickup_code），若先应用会顶掉权威结构
        File core = null;
        for (File f : files) {
            if (f.getName().equals("schema.sql")) {
                core = f;
                break;
            }
        }
        if (core != null) {
            files.remove(core);
            files.add(0, core);
        }
        if (files.isEmpty()) {
            throw new IllegalStateException("未找到任何 schema*.sql，目录: " + dir);
        }
        return files;
    }

    /** 结构指纹：schema 应用顺序 + 各文件内容 + 基线数据文件（若存在）的 MD5，任一变化即命中重建 */
    private static String fingerprint(List<File> files, File baseline) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        for (File f : files) {
            md.update(f.getName().getBytes(StandardCharsets.UTF_8));
            md.update((byte) 1);
            // 文件内容必须入指纹：仅哈希文件名会漏检 schema 内容编辑（如加列），导致漂移不重建
            md.update(Files.readAllBytes(f.toPath()));
            md.update((byte) 2);
        }
        if (baseline.exists()) {
            md.update(baseline.getName().getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(Files.readAllBytes(baseline.toPath()));
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String readMarker(String baseUrl, String query, String db,
            String username, String password) {
        try (Connection c = DriverManager.getConnection(baseUrl + "/" + db + query, username, password);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM " + MARKER_TABLE)) {
            return rs.next() ? rs.getString(1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 先按行剔除 -- 注释，再按分号切分。切分须感知单引号字符串
     * （基线数据的 INSERT 值可能含分号/引号/反斜杠转义），否则会把字符串里的
     * 分号误判为语句边界。
     */
    private static List<String> splitStatements(String sql) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        List<String> statements = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        String text = cleaned.toString();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                current.append(c);
                if (c == '\\') {
                    if (i + 1 < text.length()) {
                        current.append(text.charAt(i + 1));
                        i++;
                    }
                } else if (c == '\'') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                        current.append('\'');
                        i++;
                    } else {
                        inString = false;
                    }
                }
            } else if (c == '\'') {
                inString = true;
                current.append(c);
            } else if (c == ';') {
                String t = current.toString().trim();
                if (!t.isEmpty()) {
                    statements.add(t);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }

    private static String extractJdbcUrl(String ymlText) {
        for (String line : ymlText.split("\r?\n")) {
            int idx = line.indexOf("jdbc:mysql://");
            if (idx >= 0) {
                return line.substring(idx).trim();
            }
        }
        throw new IllegalStateException("application-test.yml 中未找到 jdbc:mysql URL");
    }

    private static String extractYamlValue(String ymlText, String key) {
        Matcher m = Pattern.compile("(?m)^\\s*" + key + ":\\s*(\\S+)\\s*$").matcher(ymlText);
        if (m.find()) {
            return m.group(1);
        }
        throw new IllegalStateException("application-test.yml 中未找到 " + key);
    }
}
