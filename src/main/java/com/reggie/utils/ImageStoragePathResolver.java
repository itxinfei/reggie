package com.reggie.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 运行时图片存储路径解析器（上传落盘 / 权限前缀判定 / 存量迁移映射的单一真源）。
 * </p>
 * <p>
 * 目录结构：uploads/{public|private}/{source}/{bizType}/{yyyyMM}/{fileName}。
 * 首段 public 免登录静态直出，private 按第二段细分 employee/user 会话要求。
 * 详见 docs/superpowers/specs/2026-09-24-image-storage-unification-design.md。
 * </p>
 *
 * @author reggie
 * @since 2026-09-24
 */
public final class ImageStoragePathResolver {

    private static final DateTimeFormatter YYYY_MM = DateTimeFormatter.ofPattern("yyyyMM");

    private static final Map<String, String> BIZ_DIRS = new HashMap<String, String>();
    private static final Map<String, String> BIZ_VISIBILITY = new HashMap<String, String>();
    private static final Set<String> PRIVATE_BIZ_DIRS = new HashSet<String>(Arrays.asList(
            "purchase", "stockcheck", "stockrecord", "supplier", "tenant"));

    static {
        BIZ_DIRS.put("dish", "dishes");
        BIZ_DIRS.put("setmeal", "setmeal");
        BIZ_DIRS.put("evaluation", "evaluation");
        BIZ_DIRS.put("purchase", "purchase");
        BIZ_DIRS.put("stockcheck", "stockcheck");
        BIZ_DIRS.put("stockrecord", "stockrecord");
        BIZ_DIRS.put("supplier", "supplier");
        BIZ_DIRS.put("tenant", "tenant");
        BIZ_DIRS.put("avatar", "avatar");
        BIZ_DIRS.put("chat", "chat");

        BIZ_VISIBILITY.put("dish", "public");
        BIZ_VISIBILITY.put("setmeal", "public");
        BIZ_VISIBILITY.put("evaluation", "public");
        BIZ_VISIBILITY.put("purchase", "private");
        BIZ_VISIBILITY.put("stockcheck", "private");
        BIZ_VISIBILITY.put("stockrecord", "private");
        BIZ_VISIBILITY.put("supplier", "private");
        BIZ_VISIBILITY.put("tenant", "private");
        BIZ_VISIBILITY.put("avatar", "private");
        BIZ_VISIBILITY.put("chat", "private");
    }

    private ImageStoragePathResolver() {
        throw new AssertionError();
    }

    /** 上传根目录：reggie.path 优先，否则 {user.dir}/uploads/（target/classes 回退项目根）。 */
    public static String resolveRoot(String configPath) {
        if (configPath != null && !configPath.isEmpty()) {
            return configPath.endsWith(File.separator) ? configPath : configPath + File.separator;
        }
        String userDir = System.getProperty("user.dir");
        if (userDir.contains("target") && userDir.endsWith("classes")) {
            userDir = new File(userDir).getParentFile().getParent();
        }
        return new File(userDir, "uploads").getAbsolutePath() + File.separator;
    }

    /** 当前年月 yyyyMM。 */
    public static String currentYyyyMm() {
        return LocalDate.now().format(YYYY_MM);
    }

    /**
     * 上传落盘相对路径。
     *
     * @param bizType     业务类型（可空，空/未知按 dish）
     * @param sessionRole 会话来源：admin（employee 会话）/ user（顾客会话）
     * @param fileName    调用方已生成的 UUID 文件名（含扩展名）
     */
    public static String resolveUploadPath(String bizType, String sessionRole, String fileName) {
        String key = bizType == null ? "" : bizType.toLowerCase();
        String dir = BIZ_DIRS.containsKey(key) ? BIZ_DIRS.get(key) : BIZ_DIRS.get("dish");
        String visibility = BIZ_VISIBILITY.containsKey(key) ? BIZ_VISIBILITY.get(key) : "public";
        String source = "user".equals(sessionRole) ? "user" : "admin";
        return visibility + "/" + source + "/" + dir + "/" + currentYyyyMm() + "/" + fileName;
    }

    public static boolean isPublicPath(String relativePath) {
        return relativePath != null && relativePath.startsWith("public/");
    }

    public static boolean isAdminPrivatePath(String relativePath) {
        return relativePath != null && relativePath.startsWith("private/admin/");
    }

    public static boolean isUserPrivatePath(String relativePath) {
        return relativePath != null && relativePath.startsWith("private/user/");
    }

    /** fallbackBizDir 是否公开业务（迁移 fallback 拼 visibility 用）。 */
    public static boolean isPublicBiz(String bizDir) {
        return "dishes".equals(bizDir) || "setmeal".equals(bizDir)
                || "evaluation".equals(bizDir) || "qr".equals(bizDir)
                || "campaign".equals(bizDir);
    }

    /**
     * 存量相对路径 → 新相对路径映射。
     *
     * @param oldPath        旧相对路径（如 images/dishes/a.jpg）
     * @param sourceHint     来源提示 admin/user
     * @param fallbackYyyyMm 文件不存在时的月份回退
     * @param uploadsRoot    上传根（定位物理文件取 mtime）
     * @return 新相对路径；已迁移/外链/不可映射返回 null
     */
    public static String migratePath(String oldPath, String sourceHint, String fallbackYyyyMm, Path uploadsRoot) {
        if (oldPath == null || oldPath.trim().isEmpty()) {
            return null;
        }
        String p = oldPath.trim().replace('\\', '/');
        if (p.startsWith("public/") || p.startsWith("private/")) {
            return null;
        }
        if (p.startsWith("http://") || p.startsWith("https://") || p.startsWith("/")) {
            return null;
        }
        if (!p.startsWith("images/")) {
            return null;
        }
        String rest = p.substring("images/".length());
        if (rest.startsWith("ai/")) {
            return migrateAi(rest.substring("ai/".length()), sourceHint);
        }
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash == rest.length() - 1) {
            return null;
        }
        String dir = rest.substring(0, slash);
        String fileName = rest.substring(slash + 1);
        if (fileName.indexOf('/') >= 0) {
            return null;
        }
        String month = monthFor(uploadsRoot, p, fallbackYyyyMm);
        if ("avatar".equals(dir)) {
            String source = "user".equals(sourceHint) ? "user" : "admin";
            return "private/" + source + "/avatar/" + month + "/" + fileName;
        }
        if ("dishes".equals(dir)) {
            String source = "user".equals(sourceHint) ? "user" : "admin";
            String biz = "user".equals(sourceHint) ? "evaluation" : "dishes";
            return "public/" + source + "/" + biz + "/" + month + "/" + fileName;
        }
        if (PRIVATE_BIZ_DIRS.contains(dir)) {
            return "private/admin/" + dir + "/" + month + "/" + fileName;
        }
        return null;
    }

    /** ai：images/ai/{tenant}/{uuid}[/{file}] → private/{source}/ai/{tenant}/{file}。 */
    private static String migrateAi(String restAfterAi, String sourceHint) {
        String[] segs = restAfterAi.split("/");
        if (segs.length < 2 || segs.length > 3) {
            return null;
        }
        String tenantDir = segs[0];
        String fileName = segs.length == 2 ? segs[1] : segs[2];
        String source = "user".equals(sourceHint) ? "user" : "admin";
        return "private/" + source + "/ai/" + tenantDir + "/" + fileName;
    }

    /** yyyyMM：优先旧文件 mtime，缺失回退 fallback。 */
    private static String monthFor(Path uploadsRoot, String oldRelative, String fallbackYyyyMm) {
        try {
            Path candidate = uploadsRoot.resolve(oldRelative);
            if (Files.exists(candidate)) {
                FileTime time = Files.getLastModifiedTime(candidate);
                return Instant.ofEpochMilli(time.toMillis())
                        .atZone(ZoneId.systemDefault()).toLocalDate().format(YYYY_MM);
            }
        } catch (Exception ignore) {
            // mtime 不可得 → 回退
        }
        return fallbackYyyyMm;
    }
}
