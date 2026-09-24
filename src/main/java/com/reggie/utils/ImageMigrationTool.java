package com.reggie.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 存量图片一次性迁移工具：uploads/images/** → uploads/{public|private}/**。
 * </p>
 * <p>
 * 开关 {@code reggie.image.migration}：off（默认，不执行）/ dry-run（只打印计划）/
 * apply（先拷贝校验字节、后 UPDATE）。幂等：已带 public|private 前缀的值跳过；
 * 行内含不可迁路径则整行跳过；旧文件保留不删（观察期后人工清理）。
 * </p>
 *
 * @author reggie
 * @since 2026-09-24
 */
@Component
public class ImageMigrationTool implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ImageMigrationTool.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 迁移字段配置：table|column|format|sourceHint|fallbackBizDir；format ∈ SINGLE|CSV|JSON */
    private static final String[][] FIELD_CONFIG = {
            {"dish", "image", "SINGLE", "admin", "dishes"},
            {"setmeal", "image", "SINGLE", "admin", "dishes"},
            {"order_detail", "image", "SINGLE", "admin", "dishes"},
            {"shopping_cart", "image", "SINGLE", "admin", "dishes"},
            {"group_buy_campaign", "image", "SINGLE", "admin", "dishes"},
            {"dish_evaluation", "images", "JSON", "user", "evaluation"},
            {"employee", "avatar", "SINGLE", "admin", "avatar"},
            {"user", "avatar", "SINGLE", "user", "avatar"},
            {"rider", "avatar", "SINGLE", "admin", "avatar"},
            {"tenant", "logo", "SINGLE", "admin", "tenant"},
            {"tenant", "license_image", "SINGLE", "admin", "tenant"},
            {"purchase_order", "voucher_images", "CSV", "admin", "purchase"},
            {"stock_check", "voucher_images", "CSV", "admin", "stockcheck"},
            {"stock_record", "voucher_images", "CSV", "admin", "stockrecord"},
            {"supplier", "license_images", "CSV", "admin", "supplier"},
            {"ai_attachment", "storage_path", "SINGLE", "user", "ai"}
    };

    @Value("${reggie.image.migration:off}")
    private String mode;

    @Value("${reggie.path:}")
    private String configPath;

    private final JdbcTemplate jdbcTemplate;

    public ImageMigrationTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private String basePath;

    @PostConstruct
    public void init() {
        basePath = ImageStoragePathResolver.resolveRoot(configPath);
    }

    @Override
    public void run(String... args) {
        if (mode == null || "off".equalsIgnoreCase(mode)) {
            return;
        }
        boolean apply = "apply".equalsIgnoreCase(mode);
        if (!apply && !"dry-run".equalsIgnoreCase(mode)) {
            log.warn("未知 reggie.image.migration 值: {}（支持 off/dry-run/apply），跳过", mode);
            return;
        }
        execute(apply);
    }

    /** 执行迁移：dry-run 只统计，apply 拷文件+UPDATE。 */
    void execute(boolean apply) {
        Path uploadsRoot = Paths.get(basePath);
        String fallbackMonth = ImageStoragePathResolver.currentYyyyMm();
        int planned = 0;
        int unmappable = 0;
        int copied = 0;
        int updated = 0;
        List<String> failures = new ArrayList<String>();

        for (String[] cfg : FIELD_CONFIG) {
            String table = cfg[0];
            String column = cfg[1];
            String format = cfg[2];
            String sourceHint = cfg[3];
            String fallbackBizDir = cfg[4];

            List<Object[]> rows;
            try {
                rows = jdbcTemplate.query(
                        "SELECT id, `" + column + "` AS val FROM `" + table
                                + "` WHERE `" + column + "` IS NOT NULL AND `" + column + "` <> ''",
                        new Object[]{},
                        (rs, i) -> new Object[]{rs.getLong("id"), rs.getString("val")});
            } catch (Exception e) {
                log.warn("读取表字段失败，跳过: {}.{} - {}", table, column, e.getMessage());
                continue;
            }

            for (Object[] row : rows) {
                long id = ((Number) row[0]).longValue();
                String raw = (String) row[1];
                List<String[]> plan = planRow(table, column, format, sourceHint, raw,
                        fallbackBizDir, fallbackMonth, uploadsRoot);
                if (plan.isEmpty()) {
                    continue;
                }
                boolean hasUnmappable = false;
                List<String> oldList = new ArrayList<String>();
                List<String> newList = new ArrayList<String>();
                for (String[] pair : plan) {
                    planned++;
                    if (pair[1] == null) {
                        unmappable++;
                        hasUnmappable = true;
                        continue;
                    }
                    oldList.add(pair[0]);
                    newList.add(pair[1]);
                }
                if (hasUnmappable) {
                    failures.add(table + "." + column + "#" + id + " 行内含不可迁路径，整行跳过");
                    continue;
                }
                if (newList.isEmpty()) {
                    continue;
                }
                if (!apply) {
                    continue;
                }
                // 1) 拷贝并校验字节，任一失败放弃本行更新（行级原子）
                boolean rowOk = true;
                for (int i = 0; i < oldList.size(); i++) {
                    String oldRel = oldList.get(i);
                    String newRel = newList.get(i);
                    Path src = uploadsRoot.resolve(oldRel);
                    Path dst = uploadsRoot.resolve(newRel);
                    try {
                        if (!Files.exists(src)) {
                            rowOk = false;
                            failures.add(table + "." + column + "#" + id + " 源文件缺失: " + oldRel);
                            break;
                        }
                        Files.createDirectories(dst.getParent());
                        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                        if (Files.size(src) != Files.size(dst)) {
                            rowOk = false;
                            failures.add(table + "." + column + "#" + id + " 字节校验失败: " + oldRel);
                            break;
                        }
                        copied++;
                    } catch (Exception e) {
                        rowOk = false;
                        failures.add(table + "." + column + "#" + id + " 拷贝失败: " + oldRel + " - " + e.getMessage());
                        break;
                    }
                }
                if (!rowOk) {
                    continue;
                }
                // 2) 按原格式回拼并 UPDATE
                try {
                    String newVal = rejoin(format, newList);
                    jdbcTemplate.update("UPDATE `" + table + "` SET `" + column + "` = ? WHERE id = ?",
                            newVal, id);
                    updated++;
                } catch (Exception e) {
                    failures.add(table + "." + column + "#" + id + " UPDATE失败: " + e.getMessage());
                }
            }
        }

        log.info("图片迁移{}完成: planned={}, unmappable={}, copied={}, updated={}, failures={}",
                apply ? "apply" : "dry-run", planned, unmappable, copied, updated, failures.size());
        for (String f : failures) {
            log.warn("迁移失败明细: {}", f);
        }
        if (unmappable > 0) {
            log.warn("共 {} 个路径不可自动映射（外链/JSON内裸名/未知结构），见上或人工核查", unmappable);
        }
    }

    /**
     * 单元格值 → [[oldPath, newPath], ...] 计划（newPath=null 表示不可迁）。
     * 已迁移项整值返回空表；外链占行记 null；CSV/JSON 内裸名不 fallback；SINGLE 裸名走 fallbackBizDir。
     */
    public static List<String[]> planRow(String table, String column, String format,
            String sourceHint, String rawValue, String fallbackBizDir,
            String fallbackMonth, Path uploadsRoot) {
        List<String[]> out = new ArrayList<String[]>();
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return out;
        }
        List<String> items = splitValue(format, rawValue.trim());
        for (String item : items) {
            String p = item.trim();
            if (p.isEmpty()) {
                continue;
            }
            if (p.startsWith("http://") || p.startsWith("https://")) {
                out.add(new String[]{p, null}); // 外链：占行计 unmappable，不迁
                continue;
            }
            if (p.startsWith("public/") || p.startsWith("private/")) {
                continue; // 已迁移
            }
            String migrated = ImageStoragePathResolver.migratePath(p, sourceHint, fallbackMonth, uploadsRoot);
            if (migrated == null) {
                boolean bare = p.indexOf('/') < 0;
                if (bare && "SINGLE".equals(format)) {
                    String visibility = ImageStoragePathResolver.isPublicBiz(fallbackBizDir)
                            ? "public" : "private";
                    String source = "user".equals(sourceHint) ? "user" : "admin";
                    migrated = visibility + "/" + source + "/" + fallbackBizDir + "/" + fallbackMonth + "/" + p;
                    out.add(new String[]{p, migrated});
                } else {
                    out.add(new String[]{p, null});
                }
                continue;
            }
            out.add(new String[]{p, migrated});
        }
        return out;
    }

    /** 按格式切分单元格值。JSON 非数组/解析失败按单值处理。 */
    static List<String> splitValue(String format, String trimmed) {
        List<String> items = new ArrayList<String>();
        if ("CSV".equals(format)) {
            for (String s : trimmed.split(",")) {
                items.add(s);
            }
        } else if ("JSON".equals(format)) {
            try {
                JsonNode node = MAPPER.readTree(trimmed);
                if (node.isArray()) {
                    for (JsonNode n : node) {
                        items.add(n.asText());
                    }
                } else {
                    items.add(trimmed);
                }
            } catch (Exception e) {
                items.add(trimmed);
            }
        } else {
            items.add(trimmed);
        }
        return items;
    }

    /** 新路径列表 → 按原格式回拼（CSV 逗号、JSON 数组、SINGLE 单值）。 */
    static String rejoin(String format, List<String> values) {
        if ("JSON".equals(format)) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('"').append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            }
            sb.append(']');
            return sb.toString();
        }
        if ("CSV".equals(format)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(values.get(i));
            }
            return sb.toString();
        }
        return values.isEmpty() ? "" : values.get(0);
    }
}
