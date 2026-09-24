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
