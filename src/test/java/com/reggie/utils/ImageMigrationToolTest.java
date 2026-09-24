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
