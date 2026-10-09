package com.reggie.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.reggie.common.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 二维码工具类，用于生成桌台点餐二维码等场景。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Component
public class QRCodeUtil {

    /**
     * 文件上传路径，用于保存生成的二维码图片
     */
    @Value("${reggie.path:}")
    private String uploadPath;

    /**
     * 服务器地址（用于生成二维码链接），配置在 application.yml
     * 格式：http://IP:端口 或 http://域名
     */
    @Value("${reggie.server-url:http://localhost:8080}")
    private String serverUrl;

    /**
     * 二维码尺寸（像素）
     */
    private static final int QR_CODE_SIZE = 300;

    /**
     * 二维码容错级别（H级可容忍30%遮挡）
     */
    private static final ErrorCorrectionLevel ERROR_CORRECTION_LEVEL = ErrorCorrectionLevel.H;

    /** 海报中二维码尺寸（像素）：高清，放大打印不发虚 */
    private static final int POSTER_QR_SIZE = 520;

    /** 海报画布宽/高（像素，按 A4 竖版比例设计，浏览器打印等比缩放） */
    private static final int POSTER_WIDTH = 800;
    private static final int POSTER_HEIGHT = 1040;

    /** 海报用品牌金 / 深色文字 / 辅助灰 */
    private static final Color BRAND_COLOR = new Color(0xff, 0xc2, 0x00);
    private static final Color DARK_COLOR = new Color(0x1f, 0x29, 0x37);
    private static final Color GRAY_COLOR = new Color(0x6b, 0x72, 0x80);

    /**
     * 海报中文字体。Windows/Windows Server 自带微软雅黑；
     * 若部署到无该字体的 Linux，AWT 会回退逻辑字体（可能中文显示为方框，需按服务器字库调整）。
     */
    private static final String POSTER_FONT = "Microsoft YaHei";

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
            String content = buildTableQRContent(tableId, serverUrl);

            // 2. 生成二维码图片
            BufferedImage qrImage = generateQRCode(content);

            // 3. 添加Logo（可选）
            // qrImage = addLogo(qrImage);

            // 4. 转换为Base64
            return bufferedImageToBase64(qrImage, "png");
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("生成桌台二维码失败: tableId={}, tableName={}", tableId, tableName, e);
            throw new CustomException("生成二维码失败，请稍后重试");
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

            // 如果未配置上传路径，返回Base64 DataURL
            return "data:image/png;base64," + base64;
        } catch (IOException e) {
            log.error("保存二维码失败: tableId={}", tableId, e);
            throw new CustomException("保存二维码失败，请稍后重试");
        }
    }

    /**
     * 将任意文本（如微信 NATIVE 的 code_url、支付宝扫码的 qr_code）渲染为 PNG 二维码 Data URI。
     *
     * @param content 二维码内容
     * @return data:image/png;base64,xxx；内容为空或渲染失败时返回 null（调用方可降级展示原始链接）
     */
    public String generateDataUri(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        try {
            BufferedImage image = generateQRCode(content);
            return "data:image/png;base64," + bufferedImageToBase64(image, "png");
        } catch (Exception e) {
            log.warn("渲染二维码 Data URI 失败 content={}, err={}", content, e.getMessage());
            return null;
        }
    }

    /**
     * 构建桌台点餐二维码链接。
     * <p>站点地址由调用方显式传入（为空时回退配置的 server-url），
     * 根治原先写死 localhost / 固定 IP 导致手机扫码无法访问的问题。</p>
     *
     * @param tableId 桌台ID
     * @param siteUrl 站点地址，如 http://192.168.1.10:8080
     * @return 完整点餐链接
     */
    public String buildTableQRContent(Long tableId, String siteUrl) {
        String base = (siteUrl == null || siteUrl.trim().isEmpty()) ? serverUrl : siteUrl;
        return base + "/front/page/qrcode-order.html?tableId=" + tableId;
    }

    /**
     * 生成高清桌贴海报：品牌金顶条 + 高清二维码 + 门店名 + 大号桌号 + 引导语 + 底条。
     *
     * @param tableId   桌台ID
     * @param tableName 桌台名称（如 A01）
     * @param storeName 门店名称
     * @param siteUrl   站点地址（二维码链接用）
     * @return Base64 编码的 PNG（不含 data:image/png;base64, 前缀）
     */
    public String generateTablePoster(Long tableId, String tableName, String storeName, String siteUrl) {
        try {
            String content = buildTableQRContent(tableId, siteUrl);
            BufferedImage qr = generateQRCode(content, POSTER_QR_SIZE);

            BufferedImage poster = new BufferedImage(POSTER_WIDTH, POSTER_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = poster.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 白底
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, POSTER_WIDTH, POSTER_HEIGHT);

            // 顶条 + 标题
            g.setColor(BRAND_COLOR);
            g.fillRect(0, 0, POSTER_WIDTH, 120);
            drawCentered(g, "扫码点餐", new Font(POSTER_FONT, Font.BOLD, 58), DARK_COLOR, POSTER_WIDTH, 84);

            // 高清二维码
            g.drawImage(qr, (POSTER_WIDTH - POSTER_QR_SIZE) / 2, 150, null);

            // 门店名 + 大号桌号 + 引导语
            drawCentered(g, storeName == null ? "" : storeName,
                    new Font(POSTER_FONT, Font.BOLD, 42), DARK_COLOR, POSTER_WIDTH, 735);
            String tableText = (tableName == null || tableName.trim().isEmpty())
                    ? "#" + tableId : tableName;
            drawCentered(g, tableText,
                    new Font(POSTER_FONT, Font.BOLD, 96), DARK_COLOR, POSTER_WIDTH, 862);
            drawCentered(g, "微信 / 支付宝 扫一扫 · 无需下载 App",
                    new Font(POSTER_FONT, Font.PLAIN, 30), GRAY_COLOR, POSTER_WIDTH, 960);

            // 底条
            g.setColor(BRAND_COLOR);
            g.fillRect(0, POSTER_HEIGHT - 24, POSTER_WIDTH, 24);
            g.dispose();

            return bufferedImageToBase64(poster, "png");
        } catch (Exception e) {
            // 宽异常兜底：统一转为业务异常
            log.error("生成桌贴海报失败: tableId={}, tableName={}", tableId, tableName, e);
            throw new CustomException("生成桌贴海报失败，请稍后重试");
        }
    }

    /**
     * 在画布上水平居中绘制一行文字。
     */
    private void drawCentered(Graphics2D g, String text, Font font, Color color, int width, int baselineY) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics(font);
        g.drawString(text, (width - fm.stringWidth(text)) / 2, baselineY);
    }

    /**
     * 生成二维码图片（默认尺寸）。
     */
    private BufferedImage generateQRCode(String content) throws WriterException {
        return generateQRCode(content, QR_CODE_SIZE);
    }

    /**
     * 按指定尺寸生成二维码图片（高清海报使用大尺寸）。
     */
    private BufferedImage generateQRCode(String content, int size) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION_LEVEL);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2); // 边距

        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

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
            throw new CustomException("图片转换失败，请稍后重试");
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
            throw new CustomException("图片转换失败，请稍后重试");
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.warn("添加Logo失败，返回原图", e);
            return qrImage;
        }
    }
}
