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

    @Test
    void tableQRContentPrefersExplicitSiteUrl() {
        // 桌台二维码内容：显式传入的站点地址优先——手机扫码可达；
        // 回退 server-url（localhost/内网占位 IP）会导致扫码打不开自助点餐页
        QRCodeUtil util = new QRCodeUtil();
        ReflectionTestUtils.setField(util, "serverUrl", "http://localhost:8080");

        String explicit = util.buildTableQRContent(7L, "http://192.168.1.10:8080");
        assertTrue(explicit.equals("http://192.168.1.10:8080/front/page/qrcode-order.html?tableId=7"),
                "显式 siteUrl 应优先生效，实际 " + explicit);

        String fallback = util.buildTableQRContent(7L, "");
        assertTrue(fallback.equals("http://localhost:8080/front/page/qrcode-order.html?tableId=7"),
                "空 siteUrl 应回退 server-url，实际 " + fallback);
    }
}
