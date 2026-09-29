package com.reggie.config;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.reggie.common.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 全表种子数据初始化（反射驱动）。
 *
 * <p>启动时对 MyBatis-Plus 管理的每张表计数（有 tenant_id 列按开发租户 1，否则全表），
 * 少于 {@link #MIN_ROWS} 行就补齐。只插入、缺多少补多少，绝不覆盖已有数据。</p>
 *
 * <p>取值三级优先：① 采样同表同列的既有取值 —— 外键、枚举型字符串、金额区间因此天然
 * 与真实数据一致；② 按列名关键字给语义值（中文店名/人名/金额/折扣率/经纬度…）；
 * ③ 按类型兜底，并用 information_schema 的 precision/scale 收敛数值，
 * 避免 decimal(5,4) 塞 10.00 这类溢出导致整表插不进行。</p>
 *
 * <p>本库无真实外键约束，只需满足 NOT NULL 与唯一约束：唯一列靠 STATISTICS 识别并追加
 * 序号尾。偶发插入失败的表连同原因汇总告警，不阻断启动，最终由测试侧健康检查硬验证。</p>
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

    /** 演示数据统一挂在开发租户上，绝不复用采样到的其它租户 */
    private static final long DEV_TENANT = 1L;

    /** 种子行的时间基准：往前 90 天起步再逐行铺开，避免整表 create_time 挤在同一秒 */
    private static final LocalDateTime BASE_TIME = LocalDateTime.now().minusDays(90);

    /** 上一版生成器的占位特征（测试t9_0 / TXT_t9_0 / t9_u0），采样时一并过滤掉 */
    private static final Pattern PLACEHOLDER = Pattern.compile(
            "^(测试)?t\\d+_|^txt_t\\d+_|^t\\d+_u\\d+$|^[a-z]{3,4}_t\\d+_", Pattern.CASE_INSENSITIVE);

    /** 采样时跳过的列：这些列要么必须唯一，要么按语义现造更真实 */
    private static final Set<String> NO_SAMPLE = new HashSet<String>();

    static {
        NO_SAMPLE.add("tenant_id");
        NO_SAMPLE.add("phone");
        NO_SAMPLE.add("mobile");
        NO_SAMPLE.add("tel");
    }

    /** 名称池：按表名关键字匹配业务族，族内取值就是一批像样的演示名称 */
    private static final class Pool {
        final String[] tables;
        final String[] values;

        Pool(String tables, String... values) {
            this.tables = tables.split(" ");
            this.values = values;
        }
    }

    private static final Pool[] NAME_POOLS = {
            new Pool("store shop", "瑞吉望京店", "瑞吉中关村店", "瑞吉国贸店", "瑞吉西单店", "瑞吉五道口店",
                    "瑞吉回龙观店", "瑞吉亦庄店", "瑞吉双井店", "瑞吉亚运村店", "瑞吉丽泽店"),
            new Pool("dish food", "宫保鸡丁", "鱼香肉丝", "麻婆豆腐", "红烧狮子头", "酸汤肥牛",
                    "干锅花菜", "手撕包菜", "蒜蓉粉丝虾", "京味小酥肉", "口水鸡"),
            new Pool("setmeal combo package", "双人精选套餐", "家庭分享餐", "商务简餐", "轻食沙拉餐",
                    "麻辣香锅套餐", "元气早餐组合", "下午茶套餐", "深夜食堂套餐"),
            new Pool("category", "热菜", "凉菜", "主食", "汤羹", "饮品", "小食", "甜点", "招牌推荐"),
            new Pool("coupon campaign activity promotion marketing", "周末满减酬宾", "新客首单立减",
                    "老客回馈券", "周三会员日五折", "下单返券活动", "下午茶专享券", "雨天暖心补贴"),
            new Pool("printer terminal", "前台小票打印机", "后厨监控打印机", "标签打印机", "自助机打印终端"),
            new Pool("delivery range area", "三公里基础范围", "五公里扩展范围", "核心商务区", "高校聚集区"),
    };

    private static final String[] SURNAMES = {"张", "王", "李", "赵", "刘", "陈", "杨", "黄", "周", "吴", "徐", "孙"};
    private static final String[] GIVEN_NAMES = {"伟", "芳", "磊", "敏", "静", "强", "丽", "涛", "建", "娟", "晨", "雨"};
    private static final String[] TITLES = {"周末特惠通知", "会员日活动说明", "配送范围调整公告", "菜单更新说明", "门店歇业通知",
            "新品上市介绍", "服务流程规范", "月度经营小结", "客户反馈处理记录", "系统升级安排"};
    private static final String[] CONTENTS = {"用于演示环境的常规记录，可随时调整。", "系统自动补齐的示例数据。",
            "按业务流程录入的一条典型记录。", "运营日常维护产生的记录。", "供联调与走查使用的样例内容。"};
    private static final String[] ADDRESSES = {"北京市朝阳区望京街道广顺北大街33号院", "北京市海淀区中关村大街27号",
            "北京市东城区建国门内大街5号", "北京市西城区金融大街28号", "北京市丰台区南三环西路5号",
            "北京市通州区新华大街16号", "北京市石景山区鲁谷路12号", "北京市昌平区回龙观东大街18号"};
    private static final String[] STATUS_WORDS = {"ENABLED", "DISABLED", "PENDING", "SUCCESS", "PROCESSING", "CLOSED"};
    private static final String[] TYPE_WORDS = {"DEFAULT", "NORMAL", "SPECIAL", "TEMP", "CUSTOM", "BASIC"};
    private static final String[] CHANNEL_WORDS = {"WECHAT", "ALIPAY", "UNIONPAY", "CASH", "BANKCARD", "OTHER"};
    private static final String[] GENERIC_ADJ = {"常规", "默认", "补充", "备用", "扩展", "标准", "增值", "临时", "长期", "专项"};
    private static final String[] GENERIC_NOUN = {"项目", "配置", "条目", "记录", "方案", "服务", "规则", "参数", "选项", "通道"};
    private static final String[] IMAGE_PATHS = {"images/demo/dish-01.jpg", "images/demo/store-02.jpg",
            "images/demo/banner-03.jpg", "images/demo/avatar-04.png", "images/demo/logo-05.png"};

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");

    /** 关联列名（去掉 _id 后）-> 父表名，仅收录规则推不出来的不规则映射 */
    private static final String[][] IRREGULAR_PARENT = {
            {"order", "orders"},
            {"campaign", "marketing_campaign"},
            {"employee", "employee"},
            {"sender", "employee"},
            {"auditor", "employee"},
            {"operator", "employee"},
            {"creator", "employee"},
    };

    @Resource
    private JdbcTemplate jdbcTemplate;

    /** 全局自增序号，用于生成跨表唯一的编号与静态 ID */
    private long seq = 0;

    /** 采样缓存：表.列 -> 该列既有真实取值（已剔除占位值） */
    private final Map<String, List<Object>> sampleCache = new HashMap<String, List<Object>>();

    /** 唯一索引列缓存：表 -> 参与唯一索引的列名集合 */
    private final Map<String, Set<String>> uniqueCache = new HashMap<String, Set<String>>();

    /** 父表主键缓存：表 -> 可引用的 id 列表 */
    private final Map<String, List<Long>> pkCache = new HashMap<String, List<Long>>();

    @Override
    public void run(ApplicationArguments args) {
        List<TableInfo> tables = TableInfoHelper.getTableInfos();
        List<String> failures = new ArrayList<String>();
        int seededTables = 0;

        for (TableInfo info : tables) {
            String table = info.getTableName();
            try {
                List<Col> cols = loadColumns(table);
                boolean tenantScoped = hasColumn(cols, "tenant_id");
                long count = countRows(table, tenantScoped);
                if (count >= MIN_ROWS) {
                    continue;
                }

                int need = MIN_ROWS - (int) count;
                String pk = getPrimaryKey(table);
                Set<String> unique = uniqueColumns(table);
                String lastError = null;
                boolean inserted = false;
                for (int r = 0; r < need; r++) {
                    String error = insertRow(table, cols, tenantScoped, r, pk, unique);
                    if (error == null) {
                        inserted = true;
                    } else {
                        lastError = error;
                    }
                }
                if (inserted) {
                    seededTables++;
                } else {
                    failures.add(table + "（0 行插入成功，末次原因：" + lastError + "）");
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

    /** 插入一行；成功返回 null，失败返回数据库给出的原因 */
    private String insertRow(String table, List<Col> cols, boolean tenantScoped,
                             int rowIndex, String pk, Set<String> unique) {
        List<String> names = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        for (Col col : cols) {
            // 自增主键交给数据库生成
            if (col.isAutoIncrement()) {
                continue;
            }
            // 非自增主键：给测试段静态 ID
            if (pk != null && col.name.equalsIgnoreCase(pk)) {
                names.add(col.name);
                values.add(STATIC_ID_BASE + (++seq));
                continue;
            }
            boolean mustBeUnique = unique.contains(col.name);
            // 有默认值且非必须的列：跳过，用数据库默认
            if (col.nullable || col.hasDefault) {
                if (col.name.equals("tenant_id") && tenantScoped) {
                    names.add(col.name);
                    values.add(DEV_TENANT);
                } else if (worthFilling(col, mustBeUnique)) {
                    // 可空的金额与业务标签也补上样例值，避免演示页面出现成片空白
                    names.add(col.name);
                    values.add(buildValue(table, col, rowIndex, false));
                }
                continue;
            }
            names.add(col.name);
            values.add(buildValue(table, col, rowIndex, mustBeUnique));
        }

        if (names.isEmpty()) {
            // 表仅有自增主键、其余均可空/有默认：插一条全默认行
            try {
                jdbcTemplate.update("INSERT INTO " + table + " () VALUES ()");
                return null;
            } catch (Exception e) {
                return reason(e);
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
            return null;
        } catch (Exception e) {
            log.debug("种子插入失败 {}: {}", table, e.getMessage());
            return reason(e);
        }
    }

    /** 只留异常首行，避免整段 SQL 回显糊满日志 */
    private String reason(Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        int newline = msg.indexOf('\n');
        return newline > 0 ? msg.substring(0, newline) : msg;
    }

    /** 可空列里值得补样例值的那些：金额与业务标签留白会让演示页面成片空白；图片、备注、扩展位保持 NULL 更自然 */
    private boolean worthFilling(Col col, boolean mustBeUnique) {
        if (mustBeUnique || isPhoneColumn(col.name)) {
            return false;
        }
        if (col.isDecimal()) {
            return true;
        }
        return col.isText() && contains(col.name, "name", "title", "code", "_no", "contact", "address", "desc", "remark");
    }

    /** 三级取值：采样既有值 -> 语义值 -> 类型兜底 */
    private Object buildValue(String table, Col col, int rowIndex, boolean mustBeUnique) {
        if (col.name.equals("tenant_id")) {
            return DEV_TENANT;
        }
        if (isPhoneColumn(col.name)) {
            return phone(rowIndex);
        }
        Object sampled = sample(table, col, rowIndex, mustBeUnique);
        if (sampled != null) {
            return sampled;
        }

        String type = col.dataType == null ? "" : col.dataType.toLowerCase(Locale.ROOT);
        switch (type) {
            case "bigint":
            case "int":
            case "integer":
            case "smallint":
            case "mediumint":
            case "tinyint":
                return integerFor(table, col, rowIndex);
            case "decimal":
            case "numeric":
            case "double":
            case "float":
                return clampDecimal(decimalFor(col, rowIndex), col);
            case "datetime":
            case "timestamp":
                return Timestamp.valueOf(timeFor(col.name, rowIndex));
            case "date":
                return java.sql.Date.valueOf(timeFor(col.name, rowIndex).toLocalDate());
            case "time":
                return String.format("%02d:%02d:00", 9 + rowIndex % 12, (rowIndex * 17) % 60);
            case "json":
                return "{}";
            default:
                return textFor(table, col, rowIndex, mustBeUnique);
        }
    }

    /** 数值列：标记位/排序/数量/关联 id 各有语义，其余给小区间递增值 */
    private Object integerFor(String table, Col col, int rowIndex) {
        String n = col.name;
        if (n.contains("parent")) {
            return 0L;
        }
        if (n.contains("deleted")) {
            return 0;
        }
        if (n.equals("version")) {
            return 0;
        }
        if (n.endsWith("status") || n.equals("status") || n.contains("enabled") || n.endsWith("flag")) {
            return 1;
        }
        if (n.contains("sort") || n.contains("order_num") || n.equals("seq") || n.contains("rank")) {
            return rowIndex + 1;
        }
        if (n.endsWith("_id") && !n.endsWith("tenant_id")) {
            // 关联列优先取真实存在的父行 id（哪怕该列参与唯一索引，按行号轮换也能取到不同父行）
            Long parent = parentKey(table, n, rowIndex);
            if (parent != null) {
                return parent;
            }
        }
        if (n.contains("count") || n.contains("quantity") || n.contains("stock") || n.endsWith("_num")) {
            return 5 + rowIndex * 2;
        }
        if (n.contains("days") || n.contains("duration") || n.contains("minute") || n.contains("hour")) {
            return 7 + rowIndex * 3;
        }
        if (n.contains("type") || n.equals("scope") || n.equals("level") || n.equals("source")) {
            return 1 + rowIndex % 4;
        }
        return 1 + rowIndex % 3;
    }

    /** 数值列的语义估值：比例给小数、金额给几十块、经纬度给北京坐标 */
    private BigDecimal decimalFor(Col col, int rowIndex) {
        String n = col.name;
        int i = rowIndex;
        if (contains(n, "rate", "ratio", "percent")) {
            return new BigDecimal("0." + String.format("%02d", 55 + (i * 4) % 40));
        }
        if (contains(n, "longitude", "lng")) {
            return new BigDecimal("116.30").add(new BigDecimal("0.0" + (3 + i)));
        }
        if (contains(n, "latitude", "lat")) {
            return new BigDecimal("39.90").add(new BigDecimal("0.0" + (2 + i)));
        }
        if (contains(n, "radius", "distance", "weight")) {
            return new BigDecimal("1.50").add(new BigDecimal("0.50").multiply(new BigDecimal(i)));
        }
        if (contains(n, "fee", "freight", "postage", "shipping")) {
            return new BigDecimal("2.00").add(new BigDecimal("0.50").multiply(new BigDecimal(i)));
        }
        if (contains(n, "price", "amount", "money", "total", "cost", "pay", "sum", "balance")) {
            return new BigDecimal("18.00").add(new BigDecimal("7.00").multiply(new BigDecimal(i)));
        }
        if (contains(n, "count", "quantity", "stock")) {
            return new BigDecimal("20.00").add(new BigDecimal(i));
        }
        return new BigDecimal("10.00").add(new BigDecimal(i));
    }

    /** 用列定义精度收敛数值：decimal(5,4) 只能放一位整数，超出就取模回落 */
    private BigDecimal clampDecimal(BigDecimal v, Col col) {
        if (v == null) {
            return new BigDecimal("1.00");
        }
        if (col.precision == null || col.scale == null || col.precision <= 0 || col.scale < 0) {
            return v;
        }
        BigDecimal out = v.setScale(col.scale, RoundingMode.HALF_UP);
        int intDigits = col.precision - col.scale;
        if (intDigits <= 0) {
            intDigits = 1;
            if (out.abs().compareTo(BigDecimal.ONE) >= 0) {
                out = out.remainder(BigDecimal.ONE).setScale(col.scale, RoundingMode.HALF_UP);
            }
        }
        BigDecimal limit = BigDecimal.TEN.pow(intDigits);
        if (out.abs().compareTo(limit) >= 0) {
            out = out.remainder(limit).setScale(col.scale, RoundingMode.HALF_UP);
        }
        if (out.compareTo(BigDecimal.ZERO) == 0) {
            out = BigDecimal.ONE.setScale(col.scale, RoundingMode.HALF_UP);
        }
        return out;
    }

    /** 字符串列：按列名与表名挑一个像样的取值 */
    private String textFor(String table, Col col, int rowIndex, boolean mustBeUnique) {
        String n = col.name;
        int i = rowIndex;
        String value;

        if (n.contains("password") && !n.contains("type")) {
            value = demoPassword();
        } else if (n.equals("password_type")) {
            value = "BCRYPT";
        } else if (n.equals("token") || n.contains("access_token") || n.contains("secret")) {
            value = fit(tableAbbr(table) + Long.toHexString(System.nanoTime() + i), col.length);
        } else if (n.equals("sex") || n.equals("gender")) {
            value = i % 2 == 0 ? "1" : "2";
        } else if (n.equals("email")) {
            value = "demo" + (i + 1) + "@reggie.example.com";
        } else if (n.contains("phone") || n.contains("mobile")) {
            value = phone(i);
        } else if (n.contains("name")) {
            value = nameFor(table, i);
        } else if (n.contains("title")) {
            value = TITLES[i % TITLES.length];
        } else if (n.equals("contact") || n.contains("consignee") || n.contains("receiver")) {
            value = personName(i);
        } else if (n.contains("address")) {
            value = ADDRESSES[i % ADDRESSES.length];
        } else if (n.equals("city")) {
            value = "北京市";
        } else if (contains(n, "remark", "desc", "content", "comment", "reason", "msg", "message", "text")) {
            value = CONTENTS[i % CONTENTS.length];
        } else if (contains(n, "url", "path", "image", "icon", "avatar", "file", "photo")) {
            value = IMAGE_PATHS[i % IMAGE_PATHS.length];
        } else if (contains(n, "status", "state")) {
            value = STATUS_WORDS[i % STATUS_WORDS.length];
        } else if (n.contains("channel")) {
            value = CHANNEL_WORDS[i % CHANNEL_WORDS.length];
        } else if (contains(n, "type", "platform", "scene", "category", "mode", "source", "role", "level", "direction")) {
            value = TYPE_WORDS[i % TYPE_WORDS.length];
        } else if (contains(n, "key") || n.equals("code") || n.endsWith("_no") || n.equals("no") || n.contains("number")) {
            value = serialCode(table, i);
        } else if (contains(n, "ids", "list", "tags", "params", "options")) {
            value = "[]";
        } else {
            // floorMod：table.hashCode() 可能为负，普通 % 会产生负索引导致 AIOOBE
            value = GENERIC_ADJ[i % GENERIC_ADJ.length]
                    + GENERIC_NOUN[Math.floorMod(i / GENERIC_ADJ.length + table.hashCode(), GENERIC_NOUN.length)];
        }
        value = fit(value, col.length);
        if (mustBeUnique) {
            value = withTail(value, i, col.length);
        }
        return value;
    }

    private volatile String demoPassword;

    /** 演示口令统一按 123456 现算 bcrypt（仅在密码列既无采样值又必填时用到，算一次即缓存） */
    private String demoPassword() {
        if (demoPassword == null) {
            demoPassword = PasswordUtils.encodePassword("123456");
        }
        return demoPassword;
    }

    /** 中文名称：按表名归属业务族挑选；池子绕完第二圈时补轮次号，避免出现整片同名行 */
    private String nameFor(String table, int rowIndex) {
        for (Pool pool : NAME_POOLS) {
            for (String key : pool.tables) {
                if (table.contains(key)) {
                    int size = pool.values.length;
                    return rowIndex < size
                            ? pool.values[rowIndex]
                            : pool.values[rowIndex % size] + "-" + (rowIndex / size + 1);
                }
            }
        }
        if (contains(table, "employee", "user", "member", "customer", "rider", "courier", "contact", "staff")) {
            return personName(rowIndex);
        }
        return GENERIC_ADJ[rowIndex % GENERIC_ADJ.length] + GENERIC_NOUN[(rowIndex / 3) % GENERIC_NOUN.length];
    }

    private String personName(int rowIndex) {
        return SURNAMES[rowIndex % SURNAMES.length] + GIVEN_NAMES[(rowIndex / 2 + 1) % GIVEN_NAMES.length];
    }

    /** 业务编号：表缩写 + 行时间日期 + 递增序号，天然跨表唯一 */
    private String serialCode(String table, int rowIndex) {
        seq++;
        String day = timeFor("create_time", rowIndex).format(DAY);
        return String.format("%s%s%04d", tableAbbr(table), day, (int) (seq % 10000));
    }

    /** 表名缩写（取各段首字母，最多 4 位大写） */
    private String tableAbbr(String table) {
        StringBuilder sb = new StringBuilder();
        for (String part : table.split("_")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        String abbr = sb.toString();
        return abbr.length() > 4 ? abbr.substring(0, 4) : abbr;
    }

    /** 11 位手机号：号段轮换 + 全局序号，保证不与既有号码重复 */
    private String phone(int rowIndex) {
        seq++;
        String[] segments = {"138", "139", "136", "135", "188", "177", "150", "166"};
        return String.format("%s%08d", segments[rowIndex % segments.length], seq % 100000000L);
    }

    private boolean isPhoneColumn(String name) {
        return name.equals("phone") || name.equals("mobile") || name.equals("tel")
                || name.equals("contact_phone") || name.equals("receiver_phone") || name.equals("phone_number");
    }

    private static boolean contains(String name, String... keys) {
        for (String key : keys) {
            if (name.contains(key)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ 采样

    /** 取同表同列的既有取值；唯一列复用时会追加序号尾 */
    private Object sample(String table, Col col, int rowIndex, boolean mustBeUnique) {
        if (NO_SAMPLE.contains(col.name) || col.isDateTime() || col.isBlob()) {
            return null;
        }
        if (mustBeUnique && !col.isText()) {
            // 唯一数值的列（如单列唯一业务 id）无法复用，交给类型兜底
            return null;
        }
        List<Object> pool = samples(table, col.name);
        if (pool.isEmpty()) {
            return null;
        }
        if (col.isText() && contains(col.name, "name", "title") && rowIndex >= pool.size()) {
            // 名称类列的可取值已被用尽，再采样就是同名复制；改走语义池给出不同名称
            return null;
        }
        Object raw = pool.get(Math.floorMod(rowIndex * 31 + col.name.hashCode(), pool.size()));
        if (mustBeUnique && raw instanceof String) {
            return withTail(fit((String) raw, col.length), rowIndex, col.length);
        }
        return raw;
    }

    /** 从表内最近若干行采样该列的非空、非占位取值 */
    private List<Object> samples(String table, String column) {
        String key = table + "." + column;
        List<Object> cached = sampleCache.get(key);
        if (cached != null) {
            return cached;
        }
        List<Object> out = new ArrayList<Object>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT DISTINCT `" + column + "` AS v FROM (SELECT `" + column + "` FROM `" + table
                            + "` ORDER BY id DESC LIMIT 200) recent WHERE `" + column + "` IS NOT NULL LIMIT 20");
            for (Map<String, Object> row : rows) {
                Object v = row.get("v");
                if (v == null || (v instanceof String && PLACEHOLDER.matcher((String) v).matches())) {
                    continue;
                }
                if (v instanceof String && ((String) v).trim().isEmpty()) {
                    continue;
                }
                out.add(v);
            }
        } catch (Exception e) {
            log.debug("列采样失败 {}.{}: {}", table, column, e.getMessage());
        }
        sampleCache.put(key, out);
        return out;
    }

    /** 关联列：从父表取一个真实存在的 id */
    private Long parentKey(String table, String column, int rowIndex) {
        String hint = column.substring(0, column.length() - 3);
        String parent = resolveParent(hint);
        if (parent == null) {
            return null;
        }
        List<Long> ids = primaryKeys(parent);
        if (ids.isEmpty()) {
            return null;
        }
        return ids.get(Math.floorMod(rowIndex * 7 + hint.hashCode(), ids.size()));
    }

    private String resolveParent(String hint) {
        List<String> candidates = new ArrayList<String>(Arrays.asList(hint, hint + "s", hint + "es"));
        for (String[] pair : IRREGULAR_PARENT) {
            if (pair[0].equals(hint)) {
                candidates.add(pair[1]);
            }
        }
        for (String candidate : candidates) {
            if (tableExists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private List<Long> primaryKeys(String table) {
        List<Long> cached = pkCache.get(table);
        if (cached != null) {
            return cached;
        }
        List<Long> out = new ArrayList<Long>();
        try {
            out = jdbcTemplate.queryForList("SELECT id FROM `" + table + "` ORDER BY id DESC LIMIT 20", Long.class);
        } catch (Exception e) {
            log.debug("父表主键采样失败 {}: {}", table, e.getMessage());
        }
        pkCache.put(table, out);
        return out;
    }

    private boolean tableExists(String table) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return n != null && n > 0;
    }

    // ------------------------------------------------------------------ 元数据

    /** 行时间：按行号在 90 天窗口内铺开 */
    private LocalDateTime rowTime(int rowIndex) {
        return BASE_TIME.plusDays((rowIndex * 5 + 11) % 80).plusHours(9 + rowIndex % 10).plusMinutes((rowIndex * 13) % 60);
    }

    private LocalDateTime timeFor(String column, int rowIndex) {
        if (contains(column, "expire", "valid", "deadline")) {
            return LocalDateTime.now().plusDays(90L + rowIndex * 15L);
        }
        LocalDateTime created = rowTime(rowIndex);
        if (column.contains("update")) {
            return cap(created.plusDays(3L + rowIndex));
        }
        if (contains(column, "pay", "finish", "complete", "cancel", "accept", "arrive", "deliver", "end")) {
            return cap(created.plusHours(2L + rowIndex));
        }
        return created;
    }

    private LocalDateTime cap(LocalDateTime t) {
        LocalDateTime now = LocalDateTime.now();
        return t.isAfter(now) ? now.minusMinutes(30L) : t;
    }

    /** 参与唯一索引的列（含复合唯一索引的每一列） */
    private Set<String> uniqueColumns(String table) {
        Set<String> cached = uniqueCache.get(table);
        if (cached != null) {
            return cached;
        }
        Set<String> out = new HashSet<String>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT DISTINCT column_name FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = ? AND non_unique = 0 "
                            + "AND index_name <> 'PRIMARY'", table);
            for (Map<String, Object> row : rows) {
                out.add(String.valueOf(row.get("column_name")).toLowerCase(Locale.ROOT));
            }
        } catch (Exception e) {
            log.debug("唯一索引读取失败 {}: {}", table, e.getMessage());
        }
        uniqueCache.put(table, out);
        return out;
    }

    private long countRows(String table, boolean tenantScoped) {
        String sql = "SELECT COUNT(1) FROM " + table
                + (tenantScoped ? " WHERE tenant_id = " + DEV_TENANT : "");
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
                        + "character_maximum_length, numeric_precision, numeric_scale FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position",
                table);
        List<Col> cols = new ArrayList<Col>();
        for (Map<String, Object> row : rows) {
            Col c = new Col();
            // MySQL 列名大小写不敏感，统一归一化为小写，避免与实体映射对不上
            c.name = String.valueOf(row.get("column_name")).toLowerCase(Locale.ROOT);
            c.dataType = row.get("data_type") == null ? null : String.valueOf(row.get("data_type")).toLowerCase(Locale.ROOT);
            c.columnType = row.get("column_type") == null ? null : String.valueOf(row.get("column_type"));
            c.nullable = "YES".equalsIgnoreCase(String.valueOf(row.get("is_nullable")));
            c.hasDefault = row.get("column_default") != null;
            c.extra = row.get("extra") == null ? null : String.valueOf(row.get("extra"));
            Object len = row.get("character_maximum_length");
            c.length = len == null ? null : ((Number) len).intValue();
            Object precision = row.get("numeric_precision");
            c.precision = precision == null ? null : ((Number) precision).intValue();
            Object scale = row.get("numeric_scale");
            c.scale = scale == null ? null : ((Number) scale).intValue();
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
        Integer precision;
        Integer scale;

        boolean isAutoIncrement() {
            return extra != null && extra.toLowerCase(Locale.ROOT).contains("auto_increment");
        }

        boolean isDecimal() {
            return "decimal".equals(dataType) || "numeric".equals(dataType)
                    || "double".equals(dataType) || "float".equals(dataType);
        }

        boolean isDateTime() {
            return "datetime".equals(dataType) || "timestamp".equals(dataType) || "date".equals(dataType);
        }

        boolean isBlob() {
            return dataType != null && dataType.contains("blob");
        }

        boolean isText() {
            return dataType != null && (dataType.contains("char") || dataType.contains("text"));
        }
    }

    // ------------------------------------------------------------------ 字符串工具

    private static String fit(String value, Integer maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength == null || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /** 唯一列复用取值时追加两位序号；超长时优先保住尾号 */
    private static String withTail(String base, int rowIndex, Integer maxLength) {
        String tail = String.format("%02d", (rowIndex + 1) % 100);
        if (maxLength != null && maxLength > 0 && base.length() + tail.length() > maxLength) {
            int keep = Math.max(maxLength - tail.length(), 1);
            base = base.length() > keep ? base.substring(0, keep) : base;
        }
        return base + tail;
    }
}
