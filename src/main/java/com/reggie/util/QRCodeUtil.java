package com.reggie.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码工具类
 * 用于生成桌台二维码、菜品二维码等
 */
@Slf4j
@Component
public class QRCodeUtil {

    @Value("${reggie.path:}")
    private String uploadPath;

    /**
     * 二维码尺寸（像素）
     */
    private static final int QR_CODE_SIZE = 300;

    /**
     * 二维码容错级别（H级可容忍30%遮挡）
     */
    private static final ErrorCorrectionLevel ERROR_CORRECTION_LEVEL = ErrorCorrectionLevel.H;

    /**
     * 生成桌台点餐二维码
     *
     * @param tableId   桌台ID
     * @param tableName 桌台名称
     * @return Base64编码的PNG图片数据（不包含data:image/png;base64,前缀）
     */
    public String generateTableQRCode(Long tableId, String tableName) {
        try {
            // 1. 构建二维码内容（H5点餐链接）
            String content = buildTableQRContent(tableId, tableName);

            // 2. 生成二维码图片
            BufferedImage qrImage = generateQRCode(content);

            // 3. 添加Logo（可选）
            // qrImage = addLogo(qrImage);

            // 4. 转换为Base64
            return bufferedImageToBase64(qrImage, "png");
        } catch (Exception e) {
            log.error("生成桌台二维码失败: tableId={}, tableName={}", tableId, tableName, e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    /**
     * 生成二维码并保存到文件
     *
     * @param tableId   桌台ID
     * @param tableName 桌台名称
     * @return 二维码文件的访问路径
     */
    public String generateAndSaveTableQRCode(Long tableId, String tableName) {
        try {
            // 生成Base64图片
            String base64 = generateTableQRCode(tableId, tableName);

            // 保存到本地（如果配置了上传路径）
            if (uploadPath != null && !uploadPath.isEmpty()) {
                String relativePath = "qrcode/table_" + tableId + ".png";
                File outputFile = new File(uploadPath + relativePath);
                File dir = outputFile.getParentFile();

                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // Base64转图片并保存
                BufferedImage image = base64ToBufferedImage(base64);
                ImageIO.write(image, "png", outputFile);

                log.info("二维码已保存: {}", relativePath);
                return "/common/download?name=" + relativePath;
            }

            // 如果未配置上传路径，返回Base64 DataURL
            return "data:image/png;base64," + base64;
        } catch (IOException e) {
            log.error("保存二维码失败: tableId={}", tableId, e);
            throw new RuntimeException("保存二维码失败", e);
        }
    }

    /**
     * 构建二维码内容
     * 格式：https://your-domain.com/h5/order?tableId={桌台ID}
     */
    private String buildTableQRContent(Long tableId, String tableName) {
        // 注意：这里的域名需要根据实际部署环境修改
        // 可以通过配置文件动态读取
        String domain = System.getProperty("qr.domain", "https://your-domain.com");
        return domain + "/h5/order?tableId=" + tableId;
    }

    /**
     * 生成二维码图片
     */
    private BufferedImage generateQRCode(String content) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION_LEVEL);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2); // 边距

        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

        MatrixToImageConfig config = new MatrixToImageConfig();
        return MatrixToImageWriter.toBufferedImage(bitMatrix, config);
    }

    /**
     * BufferedImage转Base64
     */
    private String bufferedImageToBase64(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, format, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            log.error("图片转Base64失败", e);
            throw new RuntimeException("图片转Base64失败", e);
        }
    }

    /**
     * Base64转BufferedImage
     */
    private BufferedImage base64ToBufferedImage(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (IOException e) {
            log.error("Base64转图片失败", e);
            throw new RuntimeException("Base64转图片失败", e);
        }
    }

    /**
     * （可选）给二维码添加Logo
     */
    private BufferedImage addLogo(BufferedImage qrImage) {
        try {
            int qrWidth = qrImage.getWidth();
            int qrHeight = qrImage.getHeight();

            // Logo尺寸为二维码的1/5
            int logoSize = Math.min(qrWidth, qrHeight) / 5;

            // 创建Logo图片（这里使用纯色代替，实际可以加载图片）
            BufferedImage logo = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = logo.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, logoSize, logoSize);
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.BOLD, logoSize / 3));
            FontMetrics fm = g.getFontMetrics();
            String text = "瑞吉";
            int logoX = (logoSize - fm.stringWidth(text)) / 2;
            int logoY = (logoSize - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, logoX, logoY);
            g.dispose();

            // 将Logo绘制到二维码中心
            Graphics2D g2d = qrImage.createGraphics();
            int x = (qrWidth - logoSize) / 2;
            int y = (qrHeight - logoSize) / 2;
            g2d.drawImage(logo, x, y, null);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawRect(x, y, logoSize, logoSize);
            g2d.dispose();

            return qrImage;
        } catch (Exception e) {
            log.warn("添加Logo失败，返回原图", e);
            return qrImage;
        }
    }
}
