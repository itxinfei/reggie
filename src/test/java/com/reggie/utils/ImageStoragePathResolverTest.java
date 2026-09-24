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
