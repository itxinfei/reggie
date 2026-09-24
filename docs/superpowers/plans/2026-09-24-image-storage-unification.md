# 运行时图片存储与访问统一 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将运行时上传图片统一到 `uploads/{public|private}/{source}/{bizType}/{yyyyMM}/` 分层存储，公私分流鉴权（公开静态直出），三端前端收敛到共享 `imgPath()`，并提供存量文件+数据库的幂等迁移工具。

**Architecture:** 后端以 `ImageStoragePathResolver`（纯逻辑，路径映射单一来源）支撑上传落盘、download 分流、迁移工具三处；`WebMvcConfig` 仅映射 `/uploads/public/**` 静态直出，`AuthConstants` 白名单同步收窄；前端共享单文件 `/shared/js/img-path.js` 由三个入口文件（backend `components.js`、front `common.js`、rider `util.js`）在文件头同步注入，各页面零散拼接全部收敛为调用 `imgPath()`。Spec 见 `docs/superpowers/specs/2026-09-24-image-storage-unification-design.md`。

**Tech Stack:** Java 8 / Spring Boot 2.4.5 / MyBatis-Plus / JUnit 5 + MockMvc（连真实 MySQL+Redis 测试库）；前端 Vue 2 无构建，静态 JS，node 跑无依赖断言脚本。

## Global Constraints

- **JDK 1.8 硬约束**：禁 `var`、`String.isBlank()`、`List.of()`、`Map.of()`、text blocks、`jakarta.*`；用 `javax.*`、`Arrays.asList()`、Hutool/Commons-Lang3。
- **禁止升级 Spring Boot 3.x**。
- 无 lint/format 门禁；**验证铁律 = `mvn test` 全绿（414 用例，需本地 MySQL localhost:3306/reggie + Redis localhost:6379）**；触碰 JDK API 兼容时跑 `mvn verify`（animal-sniffer）。
- 前端无编译：Vue 2 + 原生 JS，禁 Vue 3/TS/Vite；颜色走 `styles/tokens.css` 设计令牌，禁硬编码 hex。
- `application*.yml`、`db/migration/*.sql` 被 .gitignore 忽略，不在版本库。
- 新增/修改表结构须同步 `src/test/resources/schema*.sql`（本计划不新增表）。
- REST 响应用 `com.reggie.common.R<T>`；Mapper 用 `@Mapper` 注解，无 `@MapperScan`。
- git 提交信息用中文 conventional 风格（`feat:`/`fix:`/`test:`/`refactor:`），只提交本任务文件。
- **classpath 页面静态图（`backend/images/*`、`front/images/*`）不在范围，禁止改动。**
- 存量迁移「先拷贝文件、校验字节数、后 UPDATE」；旧文件在观察期（约一周）内不删除。
- 测试 profile 连真实 MySQL+Redis，跑测试前确认本地二者已启动。

---

### Task 1: 共享 imgPath 前端脚本与三端入口注入

**Files:**
- Create: `src/main/resources/shared/js/img-path.js`
- Create: `src/main/resources/shared/js/img-path.test.js`
- Modify: `src/main/resources/backend/js/components.js`（文件最顶部插入）
- Modify: `src/main/resources/front/js/common.js:1-6`（删除本地 imgPath 定义改注入；mixin 改延迟包装）
- Modify: `src/main/resources/rider/js/util.js`（文件最顶部插入）

**Interfaces:**
- Consumes: 无（前端基础件）
- Produces: 全局 `imgPath(path: string): string`、`imgPathPlaceholder(): string`（window 挂载）；Task 9/10 所有收敛调用消费 `imgPath`。

- [ ] **Step 1: 写共享脚本的行为测试（先失败）**

`src/main/resources/shared/js/img-path.test.js`（无框架，node 直跑）：

```js
// img-path 行为断言（node src/main/resources/shared/js/img-path.test.js）
var assert = require('assert');
var fs = require('fs');
var path = require('path');
var vm = require('vm');

var src = fs.readFileSync(path.join(__dirname, 'img-path.js'), 'utf8');
var sandbox = {};
vm.createContext(sandbox);
vm.runInContext(src, sandbox);
var imgPath = sandbox.imgPath;

// 1. 空值 → 占位图（node 无 location，回退 front 默认）
assert.strictEqual(imgPath(''), '/front/images/noImg.png');
assert.strictEqual(imgPath(null), '/front/images/noImg.png');
assert.strictEqual(imgPath(undefined), '/front/images/noImg.png');

// 2. 外链原样返回
assert.strictEqual(imgPath('https://cdn.example.com/a.jpg'), 'https://cdn.example.com/a.jpg');
assert.strictEqual(imgPath('http://cdn.example.com/a.jpg'), 'http://cdn.example.com/a.jpg');

// 3. 已带 /common/download 前缀不重复拼接
assert.strictEqual(
  imgPath('/common/download?name=images/dishes/x.jpg'),
  '/common/download?name=images/dishes/x.jpg'
);

// 4. 站内绝对路径原样返回
assert.strictEqual(imgPath('/front/images/logo.png'), '/front/images/logo.png');

// 5. public 前缀 → 静态直出
assert.strictEqual(
  imgPath('public/admin/dishes/202609/abc.jpg'),
  '/uploads/public/admin/dishes/202609/abc.jpg'
);

// 6. private 与旧相对路径 → encode 后走 download
assert.strictEqual(
  imgPath('private/admin/purchase/202609/abc.jpg'),
  '/common/download?name=' + encodeURIComponent('private/admin/purchase/202609/abc.jpg')
);
assert.strictEqual(
  imgPath('images/dishes/中 文.jpg'),
  '/common/download?name=' + encodeURIComponent('images/dishes/中 文.jpg')
);

console.log('img-path.test.js: all passed');
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node src/main/resources/shared/js/img-path.test.js`
Expected: FAIL（`img-path.js` ENOENT）

- [ ] **Step 3: 实现共享脚本**

`src/main/resources/shared/js/img-path.js`：

```js
// 三端共享图片路径工具（单一真源）。
// 权限语义：public/ 前缀免登录静态直出；private/ 与旧相对路径走 /common/download 鉴权。
// 三入口（backend/js/components.js、front/js/common.js、rider/js/util.js）文件头同步注入本文件。
(function (global) {
  if (typeof global.imgPath === 'function') return; // 幂等，防重复注入

  function imgPathPlaceholder() {
    var p = (typeof location !== 'undefined' && location.pathname) ? location.pathname : '';
    if (p.indexOf('/backend/') === 0) return '/backend/images/noImg.png';
    if (p.indexOf('/rider/') === 0) return '/front/images/noImg.png'; // rider 暂无自有占位图，复用 front
    return '/front/images/noImg.png';
  }

  function imgPath(path) {
    if (!path) return imgPathPlaceholder();
    if (/^https?:\/\//i.test(path)) return path;
    if (path.indexOf('/common/download') === 0) return path;
    if (path.charAt(0) === '/') return path;
    if (path.indexOf('public/') === 0) return '/uploads/' + path;
    return '/common/download?name=' + encodeURIComponent(path);
  }

  global.imgPath = imgPath;
  global.imgPathPlaceholder = imgPathPlaceholder;
})(typeof window !== 'undefined' ? window : this);
```

- [ ] **Step 4: 运行测试确认通过**

Run: `node src/main/resources/shared/js/img-path.test.js`
Expected: `img-path.test.js: all passed`

- [ ] **Step 5: 三端入口注入**

`src/main/resources/backend/js/components.js` **第 1 行之前**插入：

```js
// 三端共享 imgPath（单一真源，幂等注入）：必须先于本文件所有 Vue 组件定义
document.write('<script src="/shared/js/img-path.js?v=20260924"><\/script>');
```

`src/main/resources/rider/js/util.js` **第 1 行之前**插入同样两行。

`src/main/resources/front/js/common.js`：删除第 1-6 行本地 `imgPath` 定义，替换为：

```js
// 三端共享 imgPath（单一真源，幂等注入）：必须先于下方 mixin 装配
document.write('<script src="/shared/js/img-path.js?v=20260924"><\/script>');
```

并将 `installReggieVueHelpers` 内 `imgPath: imgPath,` 改为延迟包装（common.js 执行时注入脚本尚未运行，直接引用值会 ReferenceError；包装在渲染时才解析）：

```js
        imgPath: function (p) { return imgPath(p); },
```

保留 `cssVar: cssVar,` 等其余成员与文件尾 `installReggieVueHelpers();` 调用不动。

- [ ] **Step 6: 验证**

Run: `node -e "var s=require('fs').readFileSync('src/main/resources/shared/js/img-path.js','utf8'); eval(s); if(imgPath('public/a.jpg')!=='/uploads/public/a.jpg') process.exit(1); console.log('ok')"`
Expected: `ok`

人工核对：三入口文件第一行均为 document.write 注入；`rg -n "function imgPath" src/main/resources/front/js/common.js` 为 0 命中。

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/shared/js/img-path.js src/main/resources/shared/js/img-path.test.js src/main/resources/backend/js/components.js src/main/resources/front/js/common.js src/main/resources/rider/js/util.js
git commit -m "feat(frontend): 三端共享 imgPath 单一真源与入口同步注入"
```

---

### Task 2: ImageStoragePathResolver 存储路径解析器（TDD）

**Files:**
- Create: `src/main/java/com/reggie/utils/ImageStoragePathResolver.java`
- Test: `src/test/java/com/reggie/utils/ImageStoragePathResolverTest.java`

**Interfaces:**
- Consumes: 配置 `reggie.path`（由调用方注入字符串）
- Produces（Task 3/4/5/8 依赖，签名必须一致）:
  - `static String resolveRoot(String configPath)` → 根目录绝对路径（以 `File.separator` 结尾；configPath 空则 `{user.dir}/uploads/`，target/classes 回退项目根）
  - `static String currentYyyyMm()` → 如 `202609`
  - `static String resolveUploadPath(String bizType, String sessionRole, String fileName)` → 如 `public/admin/dishes/202609/uuid.jpg`；sessionRole ∈ `admin|user`；未知 bizType 按 dish；avatar visibility 恒 private、source 按 session
  - `static boolean isPublicPath/isAdminPrivatePath/isUserPrivatePath(String)`
  - `static boolean isPublicBiz(String bizDir)` → dishes/setmeal/evaluation/qr/campaign 为 true（Task 8 迁移 fallback 用）
  - `static String migratePath(String oldPath, String sourceHint, String fallbackYyyyMm, java.nio.file.Path uploadsRoot)` → 不可映射返回 null

- [ ] **Step 1: 写失败测试**

`src/test/java/com/reggie/utils/ImageStoragePathResolverTest.java`：

```java
package com.reggie.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ImageStoragePathResolver} 单元测试：上传落盘映射、权限前缀判定、存量路径迁移映射。
 */
class ImageStoragePathResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveRootUsesConfigWhenPresent() {
        String root = ImageStoragePathResolver.resolveRoot(tempDir.toString() + File.separator);
        assertTrue(root.startsWith(tempDir.toString()));
    }

    @Test
    void resolveRootFallsBackToUploadsDir() {
        String root = ImageStoragePathResolver.resolveRoot("");
        assertTrue(root.endsWith("uploads" + File.separator) || root.endsWith("uploads/"));
    }

    @Test
    void resolveUploadPathMapsPublicDish() {
        String p = ImageStoragePathResolver.resolveUploadPath("dish", "admin", "a.jpg");
        assertTrue(p.startsWith("public/admin/dishes/" + ImageStoragePathResolver.currentYyyyMm() + "/"));
        assertTrue(p.endsWith("a.jpg"));
    }

    @Test
    void resolveUploadPathUnknownBizFallsBackToDish() {
        String p = ImageStoragePathResolver.resolveUploadPath("whatever", "admin", "a.jpg");
        assertTrue(p.startsWith("public/admin/dishes/"));
    }

    @Test
    void resolveUploadPathAvatarFollowsSessionRole() {
        assertTrue(ImageStoragePathResolver.resolveUploadPath("avatar", "admin", "a.jpg")
                .startsWith("private/admin/avatar/"));
        assertTrue(ImageStoragePathResolver.resolveUploadPath("avatar", "user", "a.jpg")
                .startsWith("private/user/avatar/"));
    }

    @Test
    void resolveUploadPathVisibilityByBiz() {
        assertTrue(ImageStoragePathResolver.resolveUploadPath("purchase", "admin", "a.jpg")
                .startsWith("private/admin/purchase/"));
        assertTrue(ImageStoragePathResolver.resolveUploadPath("evaluation", "user", "b.jpg")
                .startsWith("public/user/evaluation/"));
        assertTrue(ImageStoragePathResolver.resolveUploadPath("setmeal", "admin", "c.jpg")
                .startsWith("public/admin/setmeal/"));
        assertTrue(ImageStoragePathResolver.resolveUploadPath("chat", "user", "d.jpg")
                .startsWith("private/user/chat/"));
    }

    @Test
    void prefixPredicates() {
        assertTrue(ImageStoragePathResolver.isPublicPath("public/admin/dishes/202609/a.jpg"));
        assertFalse(ImageStoragePathResolver.isPublicPath("private/admin/avatar/x.jpg"));
        assertTrue(ImageStoragePathResolver.isAdminPrivatePath("private/admin/purchase/202609/a.jpg"));
        assertFalse(ImageStoragePathResolver.isAdminPrivatePath("private/user/avatar/x.jpg"));
        assertTrue(ImageStoragePathResolver.isUserPrivatePath("private/user/chat/202609/a.jpg"));
    }

    @Test
    void publicBizHelper() {
        assertTrue(ImageStoragePathResolver.isPublicBiz("dishes"));
        assertTrue(ImageStoragePathResolver.isPublicBiz("evaluation"));
        assertFalse(ImageStoragePathResolver.isPublicBiz("purchase"));
        assertFalse(ImageStoragePathResolver.isPublicBiz("avatar"));
    }

    @Test
    void migrateSkipsAlreadyMigratedAndExternalUrls() {
        assertNull(ImageStoragePathResolver.migratePath("public/admin/dishes/202609/a.jpg", "admin", "202609", tempDir));
        assertNull(ImageStoragePathResolver.migratePath("private/user/avatar/202609/a.jpg", "user", "202609", tempDir));
        assertNull(ImageStoragePathResolver.migratePath("https://cdn.example.com/a.jpg", "admin", "202609", tempDir));
    }

    @Test
    void migrateDishesByFileMtime() throws Exception {
        Path old = tempDir.resolve("images/dishes/a.jpg");
        Files.createDirectories(old.getParent());
        Files.write(old, new byte[]{1});
        Files.setLastModifiedTime(old, FileTime.fromMillis(
                java.time.LocalDate.of(2026, 7, 15).atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli()));
        String migrated = ImageStoragePathResolver.migratePath(
                "images/dishes/a.jpg", "admin", "202609", tempDir);
        assertEquals("public/admin/dishes/202607/a.jpg", migrated);
    }

    @Test
    void migrateFallsBackToMonthWhenFileMissing() {
        String migrated = ImageStoragePathResolver.migratePath(
                "images/dishes/gone.jpg", "admin", "202603", tempDir);
        assertEquals("public/admin/dishes/202603/gone.jpg", migrated);
    }

    @Test
    void migratePrivateBizDirs() {
        assertEquals("private/admin/purchase/202609/a.jpg",
                ImageStoragePathResolver.migratePath("images/purchase/a.jpg", "admin", "202609", tempDir));
        assertEquals("private/admin/supplier/202609/b.jpg",
                ImageStoragePathResolver.migratePath("images/supplier/b.jpg", "admin", "202609", tempDir));
    }

    @Test
    void migrateAvatarUsesSourceHint() {
        assertEquals("private/admin/avatar/202609/a.jpg",
                ImageStoragePathResolver.migratePath("images/avatar/a.jpg", "admin", "202609", tempDir));
        assertEquals("private/user/avatar/202609/a.jpg",
                ImageStoragePathResolver.migratePath("images/avatar/a.jpg", "user", "202609", tempDir));
    }

    @Test
    void migrateAiKeepsTenantActorStructure() {
        assertEquals("private/user/ai/7/uuid.jpg",
                ImageStoragePathResolver.migratePath("images/ai/7/CUSTOMER/uuid.jpg", "user", "202609", tempDir));
        assertEquals("private/admin/ai/7/uuid.jpg",
                ImageStoragePathResolver.migratePath("images/ai/7/EMPLOYEE/uuid.jpg", "admin", "202609", tempDir));
        assertEquals("private/admin/ai/7/uuid.jpg",
                ImageStoragePathResolver.migratePath("images/ai/7/uuid.jpg", "admin", "202609", tempDir));
    }

    @Test
    void migrateUnknownStructureReturnsNull() {
        assertNull(ImageStoragePathResolver.migratePath("mystery/unknown/a.jpg", "admin", "202609", tempDir));
        assertNull(ImageStoragePathResolver.migratePath("images/unknown/nested/deep.jpg", "admin", "202609", tempDir));
    }

    @Test
    void migrateEvaluationOldPathGoesPublicUser() {
        assertEquals("public/user/evaluation/202609/e.jpg",
                ImageStoragePathResolver.migratePath("images/dishes/e.jpg", "user", "202609", tempDir));
    }

    @Test
    void migrateBareFileNameReturnsNull() {
        assertNull(ImageStoragePathResolver.migratePath("test.jpg", "admin", "202609", tempDir));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=ImageStoragePathResolverTest`
Expected: FAIL（编译错误：找不到类）

- [ ] **Step 3: 实现解析器**

`src/main/java/com/reggie/utils/ImageStoragePathResolver.java`：

```java
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
            return migrateAi(rest, sourceHint);
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
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=ImageStoragePathResolverTest`
Expected: PASS（15 用例全绿）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/reggie/utils/ImageStoragePathResolver.java src/test/java/com/reggie/utils/ImageStoragePathResolverTest.java
git commit -m "feat(utils): 图片存储路径解析器（上传/权限前缀/迁移映射单一真源）"
```

---

### Task 3: 上传接口按 resolver 落盘（TDD）

**Files:**
- Modify: `src/main/java/com/reggie/module/common/controller/CommonController.java`（BIZ_DIR_MAP/init/upload 三段）
- Test: `src/test/java/com/reggie/controller/CommonControllerTest.java`

**Interfaces:**
- Consumes: Task 2 `resolveRoot/resolveUploadPath`
- Produces: `POST /common/upload` 返回含 `public/` 或 `private/` 前缀相对路径；Task 4 download、Task 9/10 前端回显以此为契约。

- [ ] **Step 1: 修改既有断言并新增用例（先写失败）**

`CommonControllerTest.java`：

1. `testUploadSuccess` 断言改为：

```java
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.startsWith("public/admin/dishes/")));
```

2. `testUploadPngFile` 断言同样改为 `startsWith("public/admin/dishes/")`。

3. `testDownloadExistingFile` 内 `responseContent.contains("images")` 改为 `responseContent.contains("public/admin/dishes/")`，注释改为「上传响应应包含 public/admin/dishes/ 路径」。

4. 新增两个用例：

```java
    @Test
    void testUploadAvatarFollowsSessionRole() throws Exception {
        byte[] jpgBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00
        };
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, jpgBytes);

        // 顾客会话 + bizType=avatar → private/user/avatar/
        mockMvc.perform(multipart("/common/upload")
                .file(file)
                .param("bizType", "avatar")
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.startsWith("private/user/avatar/")));
    }

    @Test
    void testUploadPurchaseGoesPrivateAdmin() throws Exception {
        byte[] jpgBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00
        };
        MockMultipartFile file = new MockMultipartFile(
                "file", "voucher.jpg", MediaType.IMAGE_JPEG_VALUE, jpgBytes);

        mockMvc.perform(multipart("/common/upload")
                .file(file)
                .param("bizType", "purchase")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.startsWith("private/admin/purchase/")));
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=CommonControllerTest`
Expected: FAIL（实现仍返回 `images/dishes/...`）

- [ ] **Step 3: 改造 CommonController**

`CommonController.java`：

1. 删除 `BIZ_DIR_MAP` 静态块与 `DEFAULT_SUB_DIR`（原 50-65 行），`init()` 改为：

```java
    @Value("${reggie.path:}")
    private String configPath;

    private String basePath;

    /**
     * 初始化上传根目录：委托 ImageStoragePathResolver（reggie.path 优先，否则工作目录 uploads）
     */
    @PostConstruct
    public void init() {
        basePath = com.reggie.utils.ImageStoragePathResolver.resolveRoot(configPath);
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        log.info("文件上传目录初始化完成: {}", basePath);
    }
```

2. `upload` 中原「BIZ_DIR_MAP 取 subDir + mkdir」段（原 175-185 行）替换为：

```java
        // 来源段只信 session（employee→admin，user→user），防前端伪造 bizType 越权落 public/admin
        String sessionRole;
        if (request.getSession().getAttribute("employee") != null) {
            sessionRole = "admin";
        } else if (request.getSession().getAttribute("user") != null) {
            sessionRole = "user";
        } else {
            return R.error("NOTLOGIN");
        }
        String relativePath = com.reggie.utils.ImageStoragePathResolver
                .resolveUploadPath(bizType, sessionRole, fileName);

        log.info("文件上传: originalFilename={}, size={} bytes, path={}",
                originalFilename, file.getSize(), basePath + relativePath);
        File dir = new File(basePath + relativePath).getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
```

`file.transferTo(new File(basePath + relativePath));` 与 `return R.success(relativePath);` 不动。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=CommonControllerTest`
Expected: PASS（含新增 avatar/purchase 用例）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/reggie/module/common/controller/CommonController.java src/test/java/com/reggie/controller/CommonControllerTest.java
git commit -m "feat(upload): 上传按 public/private/来源/业务/月份分层落盘"
```

---

### Task 4: download 接口按路径前段分流鉴权（TDD）

**Files:**
- Modify: `src/main/java/com/reggie/module/common/controller/CommonController.java`（download 方法与新增 authorizeDownload）
- Test: `src/test/java/com/reggie/controller/CommonControllerTest.java`

**Interfaces:**
- Consumes: Task 2 前缀判定、Task 3 路径契约
- Produces: download 对 `public/**` 免登录、`private/admin/**` 仅 employee、`private/user/**` 仅 user、旧路径 employee/user 任一可读；拒绝时 HTTP 401 + `{"code":0,"msg":"NOTLOGIN"}`（与现状一致）。

- [ ] **Step 1: 写失败测试——CommonControllerTest 追加**

```java
    @Test
    void testDownloadPublicPathWithoutLogin() throws Exception {
        // public 前缀免登录；文件不存在也回 200 占位 SVG（鉴权先于存在性）
        mockMvc.perform(get("/common/download")
                .param("name", "public/admin/dishes/202609/not-exist.jpg"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentTypeCompatibleWith("image/svg+xml"));
    }

    @Test
    void testDownloadPrivateAdminWithoutEmployeeRejected() throws Exception {
        mockMvc.perform(get("/common/download")
                .param("name", "private/admin/purchase/202609/x.jpg")
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("NOTLOGIN"));
    }

    @Test
    void testDownloadPrivateUserWithoutUserRejected() throws Exception {
        mockMvc.perform(get("/common/download")
                .param("name", "private/user/avatar/202609/x.jpg")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("NOTLOGIN"));
    }

    @Test
    void testDownloadPrivateAdminWithEmployeeAllowed() throws Exception {
        mockMvc.perform(get("/common/download")
                .param("name", "private/admin/purchase/202609/not-exist.jpg")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentTypeCompatibleWith("image/svg+xml"));
    }

    @Test
    void testDownloadLegacyPathStillRequiresAnyLogin() throws Exception {
        // 旧相对路径（迁移窗口期兼容）：未登录 401，user 登录可读
        mockMvc.perform(get("/common/download")
                .param("name", "images/dishes/legacy.jpg"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/common/download")
                .param("name", "images/dishes/legacy.jpg")
                .sessionAttr("user", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk());
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=CommonControllerTest`
Expected: FAIL（`testDownloadPublicPathWithoutLogin` 现状 401）

- [ ] **Step 3: 实现分流**

`CommonController.java`：

1. `upload` 的 `checkLogin` 调用保留不动。
2. `download` 方法开头原登录态校验块（原 260-271 行）整段删除，改为在 canonical 路径校验之后、存在性判断之前插入分流（download 方法整体改为如下核心流程；`applyContentType/streamFile/sendPlaceholderImage` 不动）：

```java
    public void download(String name, HttpServletResponse response, HttpServletRequest request) {
        String filePath = null;
        try {
            String decodedName = java.net.URLDecoder.decode(name, java.nio.charset.StandardCharsets.UTF_8.name());
            String normalizedPath = decodedName.replace("\\", "/");
            File baseDir = new File(basePath).getCanonicalFile();
            File targetFile = new File(baseDir, normalizedPath).getCanonicalFile();

            if (!targetFile.equals(baseDir)
                    && !targetFile.getPath().startsWith(baseDir.getPath() + File.separator)) {
                log.warn("路径穿越攻击被拦截: name={}, resolved={}", name, targetFile.getPath());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "非法路径访问");
                return;
            }

            // 按路径前段分流鉴权（public 免登录；private 细分角色；旧路径任一登录可读）
            if (authorizeDownload(normalizedPath, request) == false) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":0,\"msg\":\"NOTLOGIN\"}");
                return;
            }

            filePath = targetFile.getAbsolutePath();
            if (!targetFile.exists()) {
                log.warn("文件不存在，返回占位图: {}", filePath);
                sendPlaceholderImage(response);
                return;
            }
            applyContentType(response, decodedName);
            streamFile(targetFile, response);
        } catch (Exception e) {
            log.error("文件下载失败: {}", filePath, e);
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            } catch (IOException ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    /**
     * download 鉴权分流：public 放行；private/admin 要求 employee；private/user 要求 user；
     * 旧相对路径（无 public|private 前缀）兼容 employee 或 user 任一登录。
     *
     * @return true 放行；false 拒绝（调用方写 401 NOTLOGIN）
     */
    private boolean authorizeDownload(String normalizedPath, HttpServletRequest request) {
        boolean hasEmployee = request.getSession().getAttribute("employee") != null;
        boolean hasUser = request.getSession().getAttribute("user") != null;
        if (com.reggie.utils.ImageStoragePathResolver.isPublicPath(normalizedPath)) {
            return true;
        }
        if (com.reggie.utils.ImageStoragePathResolver.isAdminPrivatePath(normalizedPath)) {
            return hasEmployee;
        }
        if (com.reggie.utils.ImageStoragePathResolver.isUserPrivatePath(normalizedPath)) {
            return hasUser;
        }
        return hasEmployee || hasUser;
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=CommonControllerTest`
Expected: PASS（全部上传+下载用例）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/reggie/module/common/controller/CommonController.java src/test/java/com/reggie/controller/CommonControllerTest.java
git commit -m "feat(download): 按 public/private/来源前段分流鉴权并保留旧路径兼容"
```

---

### Task 5: 静态映射与登录白名单收窄

**Files:**
- Modify: `src/main/java/com/reggie/config/WebMvcConfig.java`
- Modify: `src/main/java/com/reggie/common/AuthConstants.java:101-103`
- Test: `src/test/java/com/reggie/config/PublicUploadsStaticMappingTest.java`（Create）

**Interfaces:**
- Consumes: Task 2 `resolveRoot`；Task 3/4 目录契约
- Produces: `GET /uploads/public/**` 匿名可读 `{root}/public/**`；`/uploads/private/**` 404；白名单 `/uploads/public/**`；**`GET /shared/**` 匿名可读 classpath `shared/`（Task 1 三端注入的 img-path.js 依赖此映射+白名单，否则 404/401）**；`/common/download` 已在 LOGIN_EXCLUDE_URLS，不动。

> **plan 补丁（Task 1 评审发现）**：原计划遗漏 `/shared/**` 静态映射与白名单，导致 Task 1 注入的 `/shared/js/img-path.js` 线上不可达。本任务 Step 1/3 一并修复。

- [ ] **Step 1: 写失败测试**

`src/test/java/com/reggie/config/PublicUploadsStaticMappingTest.java`：

```java
package com.reggie.config;

import com.reggie.utils.ImageStoragePathResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /uploads/public/** 静态映射：公开图匿名可读，private 不进静态映射。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PublicUploadsStaticMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicUploadIsAnonymousReadable() throws Exception {
        String root = ImageStoragePathResolver.resolveRoot(null);
        Path dir = new File(root, "public/admin/dishes").toPath();
        Files.createDirectories(dir);
        Path probe = dir.resolve("static-mapping-probe.txt");
        Files.write(probe, "hello-public".getBytes("UTF-8"));
        assertTrue(Files.exists(probe));

        mockMvc.perform(get("/uploads/public/admin/dishes/static-mapping-probe.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello-public"));

        Files.deleteIfExists(probe);
    }

    @Test
    void privateUploadNotMapped() throws Exception {
        mockMvc.perform(get("/uploads/private/admin/purchase/202609/whatever.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sharedScriptAnonymousReadable() throws Exception {
        mockMvc.perform(get("/shared/js/img-path.js"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=PublicUploadsStaticMappingTest`
Expected: FAIL（`publicUploadIsAnonymousReadable` 404；`sharedScriptAnonymousReadable` 401 或 404）

- [ ] **Step 3: 实现静态映射与白名单收窄**

`WebMvcConfig.java`：新增 import 与字段：

```java
import org.springframework.beans.factory.annotation.Value;
```

```java
    /** 上传根目录（与 CommonController 同源：reggie.path 优先） */
    @Value("${reggie.path:}")
    private String configPath;
```

`addResourceHandlers` 整体替换为：

```java
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始进行静态资源映射...");
        // 根路径入口引导页：展示管理后台与用户端两个入口
        registry.addResourceHandler("/").addResourceLocations("classpath:/");
        registry.addResourceHandler("/backend/**").addResourceLocations("classpath:/backend/");
        registry.addResourceHandler("/front/**").addResourceLocations("classpath:/front/");
        // 骑手端 H5（独立目录）
        registry.addResourceHandler("/rider/**").addResourceLocations("classpath:/rider/");
        // 三端共享 JS（img-path.js 等，Task 1 注入依赖）
        registry.addResourceHandler("/shared/**").addResourceLocations("classpath:/shared/");
        // 运行时公开图：仅 public 段静态直出；private 永不映射（走 /common/download 鉴权）
        String uploadRoot = com.reggie.utils.ImageStoragePathResolver.resolveRoot(configPath);
        registry.addResourceHandler("/uploads/public/**")
                .addResourceLocations("file:" + uploadRoot + "public/");
    }
```

`AuthConstants.java` LOGIN_EXCLUDE_URLS 中，将：

```java
        // 静态资源目录（图片、上传文件）
        "/images/**",
        "/uploads/**",
```

替换为：

```java
        // 静态资源目录：仅公开上传目录 + 三端共享 JS（private 图经 /common/download 鉴权，不匿名放行）
        "/images/**",
        "/uploads/public/**",
        "/shared/**",
```

`/common/download`、`/common/download/**` 两行保持不动。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=PublicUploadsStaticMappingTest`
Expected: PASS（3 用例）

- [ ] **Step 5: 回归 download 用例**

Run: `mvn test -Dtest=CommonControllerTest,PublicUploadsStaticMappingTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/reggie/config/WebMvcConfig.java src/main/java/com/reggie/common/AuthConstants.java src/test/java/com/reggie/config/PublicUploadsStaticMappingTest.java
git commit -m "feat(config): uploads/public 静态映射与登录白名单收窄"
```

---

### Task 6: AI 附件落盘路径对齐（TDD）

**Files:**
- Modify: `src/main/java/com/reggie/module/ai/service/impl/AiAttachmentServiceImpl.java`（init 与 storagePath 段）
- Test: `src/test/java/com/reggie/module/ai/service/impl/AiAttachmentServiceImplTest.java`

**Interfaces:**
- Consumes: Task 2 `resolveRoot`
- Produces: `ai_attachment.storage_path` 新格式 `private/{admin|user}/ai/{tenantId}/{uuid}.{ext}`（actorType `CUSTOMER`→user，其余→admin；**无 yyyyMM**）。

- [ ] **Step 1: 修改测试断言——先写失败**

先列断言：`rg -n "images/ai" src/test/java/com/reggie/module/ai/service/impl/AiAttachmentServiceImplTest.java`

替换规则：
- `startsWith("images/ai/7/CUSTOMER/")` → `startsWith("private/user/ai/7/")`
- `startsWith("images/ai/{t}/EMPLOYEE/")`（或非 CUSTOMER）→ `startsWith("private/admin/ai/{t}/")`
- `startsWith("images/ai/{t}/")` → 按对应 actorType 改为 `private/{admin|user}/ai/{t}/`

已知至少 `jpegMagicUploadSucceeds`（行 75）：`assertTrue(saved.getStoragePath().startsWith("images/ai/7/CUSTOMER/"));` → `assertTrue(saved.getStoragePath().startsWith("private/user/ai/7/"));`

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=AiAttachmentServiceImplTest`
Expected: FAIL（实现仍产出 `images/ai/...`）

- [ ] **Step 3: 实现路径改造**

`AiAttachmentServiceImpl.java`：

1. `init()` 整体替换：

```java
    @PostConstruct
    public void init() {
        basePath = com.reggie.utils.ImageStoragePathResolver.resolveRoot(configPath);
        File dir = new File(basePath);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("AI附件目录创建失败: {}", basePath);
        }
    }
```

2. 落盘路径（原 124-126 行）替换：

```java
        String tenantDir = tenantId != null ? tenantId.toString() : "0";
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        // CUSTOMER→user，其余（EMPLOYEE等）→admin；无 yyyyMM（tenantId 已分桶）
        String source = "CUSTOMER".equalsIgnoreCase(actorType) ? "user" : "admin";
        String storagePath = "private/" + source + "/ai/" + tenantDir + "/" + fileName;
```

其余 `Files.write`、`record.setStoragePath(storagePath)`、VO 输出不动。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=AiAttachmentServiceImplTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/reggie/module/ai/service/impl/AiAttachmentServiceImpl.java src/test/java/com/reggie/module/ai/service/impl/AiAttachmentServiceImplTest.java
git commit -m "feat(ai): AI附件落盘对齐 private/{source}/ai/{tenant} 结构"
```

---

### Task 7: QRCodeUtil 与 TestImageGenerator 落盘对齐

**Files:**
- Modify: `src/main/java/com/reggie/utils/QRCodeUtil.java`（generateAndSaveTableQRCode 保存段）
- Modify: `src/main/java/com/reggie/utils/TestImageGenerator.java:264-275`
- Test: `src/test/java/com/reggie/utils/QRCodeUtilSavePathTest.java`（Create）

**Interfaces:**
- Consumes: Task 2 `resolveRoot/currentYyyyMm`
- Produces: 二维码落 `public/admin/qr/{yyyyMM}/table_{id}.png`，`generateAndSaveTableQRCode` 返回 `/uploads/public/admin/qr/{yyyyMM}/table_{id}.png`；测试图目录 `{root}/public/admin/dishes/`。

- [ ] **Step 1: 写失败测试**

`src/test/java/com/reggie/utils/QRCodeUtilSavePathTest.java`：

```java
package com.reggie.utils;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QRCodeUtil 落盘路径：public/admin/qr/{yyyyMM}/，返回值直出 /uploads/public/ 前缀。
 */
class QRCodeUtilSavePathTest {

    @Test
    void savePathUsesPublicQrDir() throws Exception {
        QRCodeUtil util = new QRCodeUtil();
        File tmp = new File(System.getProperty("java.io.tmpdir"), "rg-qr-test");
        tmp.mkdirs();
        ReflectionTestUtils.setField(util, "uploadPath", tmp.getAbsolutePath() + File.separator);
        ReflectionTestUtils.setField(util, "serverUrl", "http://localhost:8080");

        String url = util.generateAndSaveTableQRCode(99L, "T99");

        String expectedPrefix = "/uploads/public/admin/qr/" + ImageStoragePathResolver.currentYyyyMm()
                + "/table_99.png";
        assertTrue(url.equals(expectedPrefix), "期望 " + expectedPrefix + "，实际 " + url);
        File saved = new File(tmp, "public/admin/qr/" + ImageStoragePathResolver.currentYyyyMm()
                + "/table_99.png");
        assertTrue(saved.exists(), "二维码应落盘到 public/admin/qr/" + ImageStoragePathResolver.currentYyyyMm());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=QRCodeUtilSavePathTest`
Expected: FAIL（仍生成 `qrcode/table_99.png`、返回 `/common/download?...`）

- [ ] **Step 3: 实现两处对齐**

`QRCodeUtil.java` `generateAndSaveTableQRCode` 保存段（原 124-140 行）替换：

```java
            if (uploadPath != null && !uploadPath.isEmpty()) {
                String root = uploadPath.endsWith(File.separator)
                        ? uploadPath : uploadPath + File.separator;
                String relativePath = "public/admin/qr/"
                        + ImageStoragePathResolver.currentYyyyMm()
                        + "/table_" + tableId + ".png";
                File outputFile = new File(root, relativePath);
                File dir = outputFile.getParentFile();

                if (!dir.exists()) {
                    dir.mkdirs();
                }

                BufferedImage image = base64ToBufferedImage(base64);
                ImageIO.write(image, "png", outputFile);

                log.info("二维码已保存: {}", relativePath);
                // 公开图：静态直出，免 download 鉴权
                return "/uploads/" + relativePath;
            }
```

`TestImageGenerator.java` `resolveDishesDir()` 返回段（原 274 行）替换：

```java
        return basePath + "public" + File.separator + "admin" + File.separator
                + "dishes" + File.separator;
```

（`basePath` 根解析三行不动。）

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=QRCodeUtilSavePathTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/reggie/utils/QRCodeUtil.java src/main/java/com/reggie/utils/TestImageGenerator.java src/test/java/com/reggie/utils/QRCodeUtilSavePathTest.java
git commit -m "feat(utils): 二维码与测试图生成落盘对齐 public 目录"
```

---

### Task 8: 存量迁移工具 ImageMigrationTool（TDD）

**Files:**
- Create: `src/main/java/com/reggie/utils/ImageMigrationTool.java`
- Test: `src/test/java/com/reggie/utils/ImageMigrationToolTest.java`
- 无新增表，不改 schema

**Interfaces:**
- Consumes: Task 2 `migratePath/resolveRoot/isPublicBiz`；Spring `JdbcTemplate`
- Produces:
  - 开关 `reggie.image.migration` = `off`（默认）| `dry-run` | `apply`
  - `static List<String[]> planRow(String table, String column, String format, String sourceHint, String rawValue, String fallbackBizDir, String fallbackMonth, Path uploadsRoot)` → `[[oldPath, newPath], ...]`，newPath=null 表示不可迁
  - Runner：dry-run 只统计；apply 先拷文件校验字节再 UPDATE；幂等跳过已前缀值；行内含不可迁项整行跳过

- [ ] **Step 1: 写失败测试（纯逻辑：值解析 + 行计划）**

`src/test/java/com/reggie/utils/ImageMigrationToolTest.java`：

```java
package com.reggie.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ImageMigrationTool} 行级迁移计划：单值/CSV/JSON 值解析与路径映射。
 */
class ImageMigrationToolTest {

    @TempDir
    Path tempDir;

    @Test
    void planSingleValue() {
        List<String[]> plan = ImageMigrationTool.planRow(
                "dish", "image", "SINGLE", "admin",
                "images/dishes/a.jpg", "dishes", "202609", tempDir);
        assertEquals(1, plan.size());
        assertEquals("images/dishes/a.jpg", plan.get(0)[0]);
        assertTrue(plan.get(0)[1].startsWith("public/admin/dishes/"));
    }

    @Test
    void planEmptyAndNullValue() {
        assertTrue(ImageMigrationTool.planRow("dish", "image", "SINGLE", "admin",
                null, "dishes", "202609", tempDir).isEmpty());
        assertTrue(ImageMigrationTool.planRow("dish", "image", "SINGLE", "admin",
                "", "dishes", "202609", tempDir).isEmpty());
    }

    @Test
    void planCsvValueSplitsAndFiltersBlank() {
        List<String[]> plan = ImageMigrationTool.planRow(
                "purchase_order", "voucher_images", "CSV", "admin",
                "images/purchase/a.jpg, images/purchase/b.jpg ,", "purchase", "202609", tempDir);
        assertEquals(2, plan.size());
        assertEquals("private/admin/purchase/202609/a.jpg", plan.get(0)[1]);
        assertEquals("private/admin/purchase/202609/b.jpg", plan.get(1)[1]);
    }

    @Test
    void planJsonValueArray() {
        List<String[]> plan = ImageMigrationTool.planRow(
                "dish_evaluation", "images", "JSON", "user",
                "[\"images/dishes/e1.jpg\",\"images/dishes/e2.jpg\"]", "evaluation", "202609", tempDir);
        assertEquals(2, plan.size());
        assertTrue(plan.get(0)[1].startsWith("public/user/evaluation/"));
        assertTrue(plan.get(1)[1].startsWith("public/user/evaluation/"));
    }

    @Test
    void planJsonValueMixedUnmappableIsNull() {
        List<String[]> plan = ImageMigrationTool.planRow(
                "dish_evaluation", "images", "JSON", "user",
                "[\"images/dishes/e1.jpg\",\"https://cdn.example.com/x.jpg\",\"odd.png\"]",
                "evaluation", "202609", tempDir);
        assertEquals(3, plan.size());
        assertTrue(plan.get(0)[1].startsWith("public/user/evaluation/"));
        assertNull(plan.get(1)[1]); // 外链不可迁
        assertNull(plan.get(2)[1]); // JSON 内裸名不 fallback
    }

    @Test
    void planSkipsAlreadyMigrated() {
        List<String[]> plan = ImageMigrationTool.planRow(
                "dish", "image", "SINGLE", "admin",
                "public/admin/dishes/202609/a.jpg", "dishes", "202609", tempDir);
        assertTrue(plan.isEmpty());
    }

    @Test
    void planAiStoragePath() {
        List<String[]> plan = ImageMigrationTool.planRow(
                "ai_attachment", "storage_path", "SINGLE", "user",
                "images/ai/7/CUSTOMER/uuid.jpg", "ai", "202609", tempDir);
        assertEquals(1, plan.size());
        assertEquals("private/user/ai/7/uuid.jpg", plan.get(0)[1]);
    }

    @Test
    void planSingleBareFileNameUsesFallback() {
        // SINGLE 格式裸文件名：fallback 到 public/admin/{fallbackBizDir}/{month}/
        List<String[]> plan = ImageMigrationTool.planRow(
                "dish", "image", "SINGLE", "admin",
                "test.jpg", "dishes", "202609", tempDir);
        assertEquals(1, plan.size());
        assertEquals("test.jpg", plan.get(0)[0]);
        assertEquals("public/admin/dishes/202609/test.jpg", plan.get(0)[1]);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=ImageMigrationToolTest`
Expected: FAIL（编译错误：类不存在）

- [ ] **Step 3: 实现迁移工具**

`src/main/java/com/reggie/utils/ImageMigrationTool.java`：

```java
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
                rows = jdbcTemplate.queryForList(
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
```

注意：`planRow` 中 SINGLE 裸名 fallback 的 avatar 场景：`fallbackBizDir=avatar`、`isPublicBiz("avatar")=false` → `private/{source}/avatar/...`，与来源语义一致。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=ImageMigrationToolTest,ImageStoragePathResolverTest`
Expected: PASS（8 + 15 用例）

- [ ] **Step 5: 全量单测回归（本任务相关）**

Run: `mvn test -Dtest=ImageMigrationToolTest,ImageStoragePathResolverTest,CommonControllerTest`
Expected: PASS

dry-run 冒烟（本地 MySQL 就绪时执行；应用进入运行态打完迁移统计日志后 Ctrl+C 终止；环境缺失则以单测为准并在提交信息外标注「dry-run 待环境就绪补跑」）：

Run: `mvn spring-boot:run "-Dspring-boot.run.arguments=--reggie.image.migration=dry-run --server.port=0"`
Expected: 日志出现 `图片迁移dry-run完成: planned=..., unmappable=..., ...`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/reggie/utils/ImageMigrationTool.java src/test/java/com/reggie/utils/ImageMigrationToolTest.java
git commit -m "feat(utils): 存量图片迁移工具（dry-run/apply、幂等、行级原子）"
```

---

### Task 9: backend 前端收敛到 imgPath

**Files:**
- Modify: `src/main/resources/backend/js/components.js`（约 1412/1461/1504 行三处调用；头部注入已在 Task 1）
- Modify: `src/main/resources/backend/index.html`（imgUrl 方法）
- Modify: `src/main/resources/backend/api/food.js`（删除未使用的 commonDownload）
- Modify: `src/main/resources/backend/page/combo/list.html`（getImage、imageUrl 回显）
- Modify: `src/main/resources/backend/page/food/list.html`（getImage、两处 imageUrl）
- Modify: `src/main/resources/backend/page/employee/list.html`（imgUrl）
- Modify: `src/main/resources/backend/page/tenant/list.html`（imgUrl）
- Modify: `src/main/resources/backend/page/inventory/purchase-list.html`
- Modify: `src/main/resources/backend/page/inventory/stock-check.html`
- Modify: `src/main/resources/backend/page/inventory/stock-record.html`
- Modify: `src/main/resources/backend/page/inventory/supplier-list.html`
- Modify: `src/main/resources/backend/page/report/evaluation-list.html`（getImageUrl）

**Interfaces:**
- Consumes: Task 1 全局 `imgPath`（经 components.js 头部注入；上述页面均引 components.js）
- Produces: backend 业务代码不再出现 `'/common/download?name='` 字面拼接；包装方法名保留（`imgUrl`/`getImage`/`getImageUrl`/`singleUrl`），内部改调 `imgPath`。

- [ ] **Step 1: 逐文件替换（机械收敛）**

`components.js` 三处：

- `return rel ? ('/common/download?name=' + rel) : ''` → `return rel ? imgPath(rel) : ''`
- `list.push({ name: seg[seg.length - 1], url: '/common/download?name=' + rel, relative: rel, status: 'success' })` → `list.push({ name: seg[seg.length - 1], url: imgPath(rel), relative: rel, status: 'success' })`
- `file.url = '/common/download?name=' + response.data` → `file.url = imgPath(response.data)`

`index.html` imgUrl：

```js
          imgUrl(name) {
            return imgPath(name)
          },
```

`api/food.js`：删除「// 文件down预览」注释与整个 `commonDownload` 函数（约 66-76 行；全仓无调用方，`rg -n commonDownload src/main/resources` 仅定义处命中）。

`page/combo/list.html` getImage 替换：

```js
    getImage(image) {
      // 外链/防重复/编码分支统一收敛到共享 imgPath（行为见 shared/js/img-path.js）
      return imgPath(image)
    },
```

同文件 `this.comboForm.imageUrl = \`/common/download?name=${response.data}\`` → `this.comboForm.imageUrl = imgPath(response.data)`

`page/food/list.html` 三处：

- `getImage: function(image) { return '/common/download?name=' + image },` → `getImage: function(image) { return imgPath(image) },`
- 行 638 内 `imageUrl: '/common/download?name=' + data.image` → `imageUrl: imgPath(data.image)`
- 行 758 → `this.dishForm.imageUrl = imgPath(response.data); this.dishForm.image = response.data`

`page/employee/list.html` imgUrl：

```js
          imgUrl(name) {
            return imgPath(name)
          },
```

`page/tenant/list.html` imgUrl：同上（缩进以原文件为准，仅替换函数体返回语句为 `return imgPath(name)`）。

四个 inventory 页面（purchase-list / stock-check / stock-record / supplier-list）：把

`return '/common/download?name=' + x` → `return imgPath(x)`

（各文件变量名可能是 `x`，以原行为准，只替换拼接表达式。）

`page/report/evaluation-list.html` getImageUrl：

```js
          getImageUrl: function(name) {
            // 外链与编码分支已由共享 imgPath 覆盖
            return imgPath(name)
          },
```

- [ ] **Step 2: 残留拼接扫描**

Run: `rg -n "/common/download\?name=" src/main/resources/backend`
Expected: 业务代码 0 命中（若仅剩注释可保留）

- [ ] **Step 3: 注入与调用核对**

Run: `rg -n "imgPath" src/main/resources/backend/js/components.js`
Expected: ≥4 命中（注入注释 1 + 调用 3）；第一行为 document.write 注入。

- [ ] **Step 4: 回归共享脚本测试**

Run: `node src/main/resources/shared/js/img-path.test.js`
Expected: `img-path.test.js: all passed`

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/backend
git commit -m "refactor(backend): 图片 URL 拼接全量收敛到共享 imgPath"
```

---

### Task 10: front 前端收敛到共享 imgPath

**Files:**
- Modify: `src/main/resources/front/page/order.html`（删除局部 imgPath）
- Modify: `src/main/resources/front/page/user.html`（删除局部 imgPath）
- Modify: `src/main/resources/front/page/add-order.html`（删除局部 imgPath）
- Modify: `src/main/resources/front/index.html`（删除局部 imgPath）
- Modify: 其余 `rg` 命中的裸拼接点（如有）

**Interfaces:**
- Consumes: Task 1 共享 `imgPath` + front `common.js` mixin 延迟包装
- Produces: front 各页 `imgPath` 一律解析到共享实现（页面 methods 不再覆盖 mixin）；`imgError` onerror 兜底**全部保留**。

- [ ] **Step 1: 搜索 front 全部局部定义与拼接**

Run: `rg -n "imgPath\(path\)|'/common/download\?name='|function imgPath" src/main/resources/front`
Expected 处理表：

| 命中 | 处理 |
|---|---|
| `js/common.js` 的 `function imgPath` | Task 1 已删——应 0 命中，否则补删 |
| `page/order.html` 局部 `imgPath(path){...}` | 连同上方注释整段删除 |
| `page/user.html` 局部 `imgPath: function(path){...}` | 连同注释删除，保留紧随 `imgError` |
| `page/add-order.html` 局部 `imgPath(path){...}` | 连同注释删除，保留 `imgError` |
| `index.html` 局部 `imgPath(path){...}` | 连同注释删除，保留 `imgError` |
| 其余 `'/common/download?name='` 裸拼 | 改调 `imgPath(...)` |

- [ ] **Step 2: 执行删除（邻接上下文验收标准）**

`page/order.html` 删除后应为：

```js
                goHome(){
                    window.requestAnimationFrame(()=>{
                        window.location.href = '/front/index.html'
                    })
                },
                // 修改点(2026-09-22)：底部 Tab 切换已统一由 cend-shell 组件处理，页面不再重复定义
                initData(){
```

`page/user.html` 删除后应为：

```js
                toOrderPageWithStatus: function(status){
                    window.requestAnimationFrame(function(){ window.location.href = '/front/page/order.html?status=' + status })
                },
                // 修改点：图片加载失败兜底占位图
                imgError: function(e){
```

`page/add-order.html` 删除后应为：

```js
                },
                // 修改点：图片加载失败兜底
                imgError(e){
                    e.target.src = './../images/noImg.png'
                },
```

`index.html` 删除后应为：

```js
            },
            // 修改点：图片加载失败兜底，替代el-image的error slot
            imgError(e){
```

- [ ] **Step 3: 确认 mixin 装配**

Run: `rg -n "installReggieVueHelpers|imgPath: function|function imgPath" src/main/resources/front/js/common.js`
Expected: `installReggieVueHelpers` 存在、成员为 `imgPath: function (p) { return imgPath(p); }`、**无** `function imgPath(path)` 定义。

- [ ] **Step 4: 残留扫描**

Run: `rg -n "imgPath\(path\)|'/common/download\?name='" src/main/resources/front`
Expected: 0 业务命中

- [ ] **Step 5: 回归共享脚本测试**

Run: `node src/main/resources/shared/js/img-path.test.js`
Expected: `img-path.test.js: all passed`

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/front
git commit -m "refactor(front): 删除页面局部 imgPath 统一走共享实现"
```

---

### Task 11: rider 端接入核验

**Files:**
- Modify: `src/main/resources/rider/js/util.js`（注入已在 Task 1——本任务核验）
- 4 个 rider 页面无图片引用，预期零行为变化

**Interfaces:**
- Consumes: Task 1 rider 注入
- Produces: rider 页面运行时 `window.imgPath` 可用（预置就绪）。

- [ ] **Step 1: 注入核验**

Run: `rg -n "img-path.js" src/main/resources/rider/js/util.js`
Expected: 命中 document.write 注入行

- [ ] **Step 2: 确认无存量图片拼接**

Run: `rg -n "common/download|imgPath" src/main/resources/rider`
Expected: 仅 util.js 注入行命中，页面无业务拼接

- [ ] **Step 3: Commit（若 Step 1/2 发现遗漏则修复后提交；全通过则无改动跳过提交）**

```bash
git add src/main/resources/rider
git commit -m "feat(rider): 预置共享 imgPath 接入核验"
```

（无文件变更时跳过此 commit。）

---

### Task 12: 预置图互联网下载补齐

**Files:**
- Create: `uploads/public/system/defaults/default-avatar.png`（产物不进 git）
- Create: `uploads/public/system/defaults/default-dish.jpg`（产物不进 git）
- Create: `uploads/public/system/brand/login-bg.jpg`（产物不进 git）
- Create: `docs/image-sources.md`（来源与授权记录，进 git）

**Interfaces:**
- Consumes: Task 2 目录约定；`uploads/` 应被 .gitignore 忽略
- Produces: 运行时可引用的 `public/system/...` 预置图实体 + 权威来源清单文档。

- [ ] **Step 1: 确认 ignore 归属**

Run: `git check-ignore -v uploads/public/system/defaults/default-avatar.png`
Expected: 命中 .gitignore 规则。若未命中：在 `.gitignore` 追加 `uploads/` 一行并单独随本任务提交；**禁止**提交图片二进制进 git。

- [ ] **Step 2: 互联网搜索可商用图源**

用 websearch 依次检索并选定直链（Unsplash/Pexels License 免费商用）：

- Query: `site:unsplash.com plain gray default avatar silhouette`
- Query: `site:unsplash.com chinese food dish top view free stock photo`
- Query: `site:pexels.com restaurant interior warm background`

选图标准：无可见水印/人脸特写（avatar 用剪影或图形）、构图适合裁剪、License 允许商用与修改。每个用途选定 1 张后记录**原始页面 URL 与图片直链**。

- [ ] **Step 3: 下载落盘**

PowerShell（直链替换为 Step 2 实际选定值）：

```powershell
New-Item -ItemType Directory -Force -Path uploads/public/system/defaults, uploads/public/system/brand | Out-Null
Invoke-WebRequest -Uri "<AVATAR_DIRECT_URL>" -OutFile uploads/public/system/defaults/default-avatar.png
Invoke-WebRequest -Uri "<DISH_DIRECT_URL>" -OutFile uploads/public/system/defaults/default-dish.jpg
Invoke-WebRequest -Uri "<BRAND_DIRECT_URL>" -OutFile uploads/public/system/brand/login-bg.jpg
Get-ChildItem uploads/public/system -Recurse -File | Select-Object FullName, Length
```

Expected: 三个文件存在且 Size > 10KB；扩展名与实际格式一致（`Format-Hex` 前 4 字节核对 PNG `89 50 4E 47` / JPEG `FF D8 FF`）。

- [ ] **Step 4: 写来源清单**

`docs/image-sources.md`：

```markdown
# 运行时预置图来源清单

| 用途 | 存放路径（相对 uploads/） | 来源页面 | 直链 | License |
|---|---|---|---|---|
| 默认头像 | public/system/defaults/default-avatar.png | <Step2 页面URL> | <直链> | Unsplash/Pexels License |
| 默认菜品图 | public/system/defaults/default-dish.jpg | <页面URL> | <直链> | 同上 |
| 登录宣传图 | public/system/brand/login-bg.jpg | <页面URL> | <直链> | 同上 |

下载日期：YYYY-MM-DD。图片实体在 uploads/（不进 git），本文件为唯一权威记录。
```

（`<...>` 处填 Step 2/3 实际值，不得留占位符提交。）

- [ ] **Step 5: Commit（仅文档与可能的 .gitignore）**

```bash
git add docs/image-sources.md .gitignore
git commit -m "docs: 运行时预置图来源与授权清单"
```

---

### Task 13: 全量回归与人工验收

**Files:** 无新增；验证性任务

**Interfaces:**
- Consumes: Task 1-12 全部产物
- Produces: 全绿测试报告 + 人工验收勾选记录

- [ ] **Step 1: 全量单元/集成测试**

Run: `mvn test`
Expected: `BUILD SUCCESS`，Tests run 全部通过（基线 414 用例 + 本计划新增，无 Failures/Errors）。本地 MySQL(3306/reggie)+Redis(6379) 必须已启动。

- [ ] **Step 2: JDK8 兼容校验**

Run: `mvn verify`
Expected: `BUILD SUCCESS`（animal-sniffer + enforcer 通过；须 JDK 8 环境，JDK 9+ 会在 enforcer 失败）

- [ ] **Step 3: 残留扫描**

Run: `rg -n "'/common/download\\?name='" src/main/resources`
Expected: 0 业务命中（shared/js/img-path.js 中的 `'/common/download?name=' + encodeURIComponent` 为唯一合法实现点）

Run: `rg -n "\"/uploads/\\*\\*\"" src/main/java`
Expected: 0 命中（白名单已收窄为 `/uploads/public/**`）

- [ ] **Step 4: 人工验收清单（本地 `mvn spring-boot:run` + 浏览器）**

| # | 场景 | 预期 |
|---|---|---|
| 1 | 匿名打开 `backend/index.html` 登录页 logo/noImg | classpath 静态图正常（未被本计划破坏） |
| 2 | 员工登录后台上传菜品图（food 页） | 数据库 `dish.image` = `public/admin/dishes/{yyyyMM}/{uuid}.jpg`，文件落对应目录，回显正常 |
| 3 | 刷新页面看该菜品图 | 图片 URL 为 `/uploads/public/admin/dishes/...`（静态直出，Network 无 download 请求） |
| 4 | 退出登录直接访问该 `/uploads/public/...` URL | 200 可见 |
| 5 | 顾客登录上传头像（user 页） | 落 `private/user/avatar/{yyyyMM}/`，回显走 `/common/download?...` |
| 6 | 匿名访问第 5 步的 download URL | 401 NOTLOGIN |
| 7 | 员工访问顾客私有图 download URL | 401（private/user 段要求 user 会话） |
| 8 | 套餐页上传图（combo） | bizType=setmeal 生效，落 `public/admin/setmeal/` |
| 9 | 评价页上传图（front my-evaluations） | 落 `public/user/evaluation/` |
| 10 | 历史未迁移旧路径图片（`images/...`）登录态浏览 | 仍可显示（旧路径兼容分支） |
| 11 | 空图片字段页面（菜品无图） | 后台显示 `/backend/images/noImg.png`，C端显示 `/front/images/noImg.png` |
| 12 | C端匿名浏览菜单菜品图 | 正常显示（public 直出，不再因未登录裂图） |
| 13 | 桌台二维码生成（qrcode-center 或 dining） | 返回 URL 为 `/uploads/public/admin/qr/{yyyyMM}/table_{id}.png` 且可匿名打开 |
| 14 | dry-run → apply 迁移（本地有存量数据时） | dry-run 统计合理；apply 后文件在新目录、DB 值带新前缀、旧文件仍在、页面仍正常 |

逐项勾选，任何一项失败回到对应 Task 修复后重跑 Step 1。

- [ ] **Step 5: 迁移执行（生产/长期数据环境，按序）**

1. 部署后先 `--reggie.image.migration=dry-run`，人工审阅 planned/unmappable/failures 统计与明细日志。
2. 无异常后 `--reggie.image.migration=apply`，核对 `copied==updated` 且 failures=0（或逐条人工裁定）。
3. 观察期约一周（双路径共存均可读）→ 人工备份后删除 `uploads/images/` 旧目录。
4. 收尾清理（独立小提交）：`AuthConstants` 删除 `"/images/**"` 白名单行；`CommonController.authorizeDownload` 删除旧路径兼容分支（else 分支改 `return false`），并把 `testDownloadLegacyPathStillRequiresAnyLogin` 改为断言 401、同步更新 spec §4.2/§4.4 的「迁移完成后移除」备注已执行。

- [ ] **Step 6: 最终 Commit（若有收尾变更）**

```bash
git add -A src/main/java src/test/java
git commit -m "chore: 迁移观察期结束收尾（移除旧路径兼容与 images 白名单）"
```

（仅 Step 5.4 实际执行时提交；Step 1-4 全过且未到观察期结束则本步跳过。）

---

## 计划自审记录（writing-plans Self-Review）

1. **Spec 覆盖**：§3 存储结构→Task 2；§4.1 上传→Task 3；§4.2 download→Task 4；§4.3/§4.4 映射与白名单→Task 5；§4.5/§4.6 AI/二维码/测试图→Task 6/7；§5 三端 imgPath→Task 1/9/10/11；§6 迁移→Task 8 + Task 13 Step 5；§7 预置图→Task 12；§8 测试→各任务 TDD + Task 13。无缺口。
2. **占位符扫描**：Task 12 的 `<AVATAR_DIRECT_URL>` 为「执行时从搜索结果填入」的运行时输入（Step 4 明确禁留占位提交），非 TBD；其余无 TODO/TBD。
3. **类型一致性**：`planRow` 8 参签名在 Task 8 测试与实现一致；`resolveUploadPath/migratePath/isPublicBiz` 在 Task 2 定义与 Task 3/5/8 调用一致；`imgPath` 单源定义与 Task 9/10 调用一致。
4. **发现的 spec 修正**：`/common/download` 实际**已在** `LOGIN_EXCLUDE_URLS`（AuthConstants:126-127），Task 5 只收窄 `/uploads/**`，与 spec §4.4 修复后表述一致。

