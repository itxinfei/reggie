package com.reggie.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试图片生成器（仅开发环境启用）
 * 启动时自动下载/生成测试菜品图片
 *
 * <p>重要提示：
 * <ul>
 *   <li>仅在开发环境（dev）自动运行</li>
 *   <li>首次启动自动下载真实图片</li>
 *   <li>后续启动检测到图片后直接跳过</li>
 *   <li>生产环境（prod）完全禁用</li>
 *   <li>用户上传图片后建议移除此类</li>
 * </ul>
 *
 * <p>配置项（application.yml）：
 * <ul>
 *   <li>reggie.image.download-real-images: 是否下载真实图片（默认 true）</li>
 *   <li>reggie.image.download-timeout: 下载超时时间（默认 10000ms）</li>
 * </ul>
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Component
public class TestImageGenerator implements CommandLineRunner {

    /**
     * 配置文件中的上传路径
     */
    @Value("${reggie.path:}")
    private String configPath;

    /**
     * 是否下载真实图片（默认true）
     */
    @Value("${reggie.image.download-real-images:true}")
    private boolean downloadRealImages;

    /**
     * 下载超时时间（毫秒，默认10000ms）
     */
    @Value("${reggie.image.download-timeout:10000}")
    private int downloadTimeout;

    /**
     * Spring环境上下文，用于判断当前激活的环境配置
     */
    private final Environment environment;

    // 构造函数注入 Environment
    public TestImageGenerator(Environment environment) {
        this.environment = environment;
    }

    /**
     * 基础路径，用于存储生成的图片文件
     */
    private String basePath;

    /**
     * 菜品名称和对应颜色（降级方案）
     */
    private static final Map<String, Color> DISH_COLORS = new HashMap<>();

    /**
     * 真实图片URL（使用免费图库）
     */
    private static final Map<String, String> DISH_IMAGE_URLS = new HashMap<>();

    static {
        // 荤菜 - 使用 Unsplash 免费图库的真实菜品图片
        DISH_IMAGE_URLS.put("hongshaorou",
            "https://images.unsplash.com/photo-1623689046286-01561a1518d7?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("gongbaojiding",
            "https://images.unsplash.com/photo-1603073809655-f3662c8d02a7?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("yuxiangrous",
            "https://images.unsplash.com/photo-1603073809655-f3662c8d02a7?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("laziji",
            "https://images.unsplash.com/photo-1584576667856-f5121f48f6d9?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("tangculiji",
            "https://images.unsplash.com/photo-1603073809655-f3662c8d02a7?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("qingzhengluyu",
            "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=400&h=300&fit=crop");

        // 素菜
        DISH_IMAGE_URLS.put("liangbanghuanggua",
            "https://images.unsplash.com/photo-1604908176997-1257f3ca5ae0?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("paohuanggua",
            "https://images.unsplash.com/photo-1604908176997-1257f3ca5ae0?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("mapotoufu",
            "https://images.unsplash.com/photo-1582452932304-60d2bd8e8a8a?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("suorongxilanhua",
            "https://images.unsplash.com/photo-1459411621453-7debff8f3ebf?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("fanqiejidan",
            "https://images.unsplash.com/photo-1484723090599-4447d4d3c8a7?w=400&h=300&fit=crop");

        // 汤类
        DISH_IMAGE_URLS.put("xihuniurougeng",
            "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("fanqijidantang",
            "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400&h=300&fit=crop");

        // 主食
        DISH_IMAGE_URLS.put("yangzhouchaofan",
            "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("niuroumian",
            "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("xiaolongbao",
            "https://images.unsplash.com/photo-1496116218417-1a781b1c416c?w=400&h=300&fit=crop");

        // 小吃
        DISH_IMAGE_URLS.put("shutiao",
            "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("jimihua",
            "https://images.unsplash.com/photo-1626073292877-27bc4c79665e?w=400&h=300&fit=crop");
        DISH_IMAGE_URLS.put("laocuhuasheng",
            "https://images.unsplash.com/photo-1606757389575-b20e2fa31f5b?w=400&h=300&fit=crop");

        // 降级颜色方案（网络不可用时使用）
        DISH_COLORS.put("hongshaorou", new Color(180, 60, 60));
        DISH_COLORS.put("gongbaojiding", new Color(200, 80, 50));
        DISH_COLORS.put("yuxiangrous", new Color(160, 100, 70));
        DISH_COLORS.put("laziji", new Color(200, 40, 40));
        DISH_COLORS.put("tangculiji", new Color(180, 100, 50));
        DISH_COLORS.put("qingzhengluyu", new Color(220, 200, 180));
        DISH_COLORS.put("liangbanghuanggua", new Color(100, 160, 80));
        DISH_COLORS.put("paohuanggua", new Color(120, 180, 100));
        DISH_COLORS.put("mapotoufu", new Color(180, 100, 60));
        DISH_COLORS.put("suorongxilanhua", new Color(60, 140, 60));
        DISH_COLORS.put("fanqiejidan", new Color(200, 120, 60));
        DISH_COLORS.put("xihuniurougeng", new Color(220, 200, 150));
        DISH_COLORS.put("fanqijidantang", new Color(200, 150, 80));
        DISH_COLORS.put("yangzhouchaofan", new Color(220, 200, 120));
        DISH_COLORS.put("niuroumian", new Color(180, 140, 80));
        DISH_COLORS.put("xiaolongbao", new Color(240, 230, 200));
        DISH_COLORS.put("shutiao", new Color(220, 180, 80));
        DISH_COLORS.put("jimihua", new Color(200, 160, 80));
        DISH_COLORS.put("laocuhuasheng", new Color(140, 100, 60));
    }

    /**
     * 菜品中文名映射
     */
    private static final Map<String, String> DISH_NAMES = new HashMap<>();

    static {
        DISH_NAMES.put("hongshaorou", "红烧肉");
        DISH_NAMES.put("gongbaojiding", "宫保鸡丁");
        DISH_NAMES.put("yuxiangrous", "鱼香肉丝");
        DISH_NAMES.put("laziji", "辣子鸡");
        DISH_NAMES.put("tangculiji", "糖醋里脊");
        DISH_NAMES.put("qingzhengluyu", "清蒸鲈鱼");
        DISH_NAMES.put("liangbanghuanggua", "凉拌黄瓜");
        DISH_NAMES.put("paohuanggua", "拍黄瓜");
        DISH_NAMES.put("mapotoufu", "麻婆豆腐");
        DISH_NAMES.put("suorongxilanhua", "蒜蓉西兰花");
        DISH_NAMES.put("fanqiejidan", "番茄鸡蛋");
        DISH_NAMES.put("xihuniurougeng", "西湖牛肉羹");
        DISH_NAMES.put("fanqijidantang", "番茄鸡蛋汤");
        DISH_NAMES.put("yangzhouchaofan", "扬州炒饭");
        DISH_NAMES.put("niuroumian", "牛肉面");
        DISH_NAMES.put("xiaolongbao", "小笼包");
        DISH_NAMES.put("shutiao", "薯条");
        DISH_NAMES.put("jimihua", "鸡米花");
        DISH_NAMES.put("laocuhuasheng", "老醋花生");
    }

    /**
     * 最小有效图片大小（字节）
     * 小于此大小的图片认为是无效图片
     */
    private static final int MIN_VALID_IMAGE_SIZE = 1024; // 1KB

    /**
     * 应用启动时执行，自动下载或生成测试菜品图片
     *
     * @param args 命令行参数
     * @throws Exception 启动异常
     */
    @Override
    public void run(String... args) throws Exception {
        // 检查是否为开发环境
        boolean isDevEnvironment = false;
        if (environment != null) {
            isDevEnvironment = java.util.Arrays.asList(environment.getActiveProfiles())
                .contains("dev");
        }

        // 非开发环境直接跳过
        if (!isDevEnvironment) {
            log.info("ℹ️ TestImageGenerator 仅在开发环境（dev）启用，当前环境：{}，跳过执行",
                isDevEnvironment ? "dev" : "other");
            return;
        }

        // 检查是否禁用了图片下载
        if (!downloadRealImages) {
            log.info("ℹ️ 图片自动下载已禁用（download-real-images=false），跳过执行");
            return;
        }

        if (configPath != null && !configPath.isEmpty()) {
            basePath = configPath;
        } else {
            String userDir = System.getProperty("user.dir");
            if (userDir.contains("target") && userDir.endsWith("classes")) {
                userDir = new File(userDir).getParentFile().getParent();
            }
            basePath = new File(userDir, "uploads").getAbsolutePath() + File.separator;
        }

        String dishesDir = basePath + "images" + File.separator + "dishes" + File.separator;
        File dir = new File(dishesDir);

        // 如果目录存在且有任意图片文件，直接跳过（用户可能已上传自定义图片）
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                long imageCount = java.util.Arrays.stream(files)
                    .filter(f -> f.isFile() && (f.getName().endsWith(".jpg") || f.getName().endsWith(".png")))
                    .count();
                if (imageCount > 0) {
                    log.info("✅ 检测到 {} 张菜品图片（用户已上传或已生成），跳过自动下载", imageCount);
                    return;
                }
            }
        }

        // 目录不存在或无图片，需要初始化
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                log.error("❌ 无法创建目录: {}", dishesDir);
                return;
            }
        }

        log.info("🎨 检测到菜品图片目录为空，开始初始化图片...");
        log.info("📥 模式：{}", downloadRealImages ? "下载真实图片（仅执行一次）" : "本地生成");

        int count = 0;
        int successCount = 0;
        for (Map.Entry<String, String> entry : DISH_IMAGE_URLS.entrySet()) {
            String filename = entry.getKey() + ".jpg";
            File imageFile = new File(dishesDir + filename);

            // 如果图片已存在，跳过（防止重复下载）
            if (imageFile.exists()) {
                log.debug("✓ {} 已存在，跳过", filename);
                successCount++;
                continue;
            }

            String dishName = DISH_NAMES.getOrDefault(entry.getKey(), entry.getKey());

            // 优先下载真实图片
            if (downloadRealImages) {
                if (downloadImage(entry.getValue(), imageFile, dishName)) {
                    successCount++;
                    count++;
                    continue;
                } else {
                    log.warn("⚠️ {} 下载失败，降级到本地生成", dishName);
                }
            }

            // 本地生成（降级方案或配置关闭网络下载）
            generateFoodImage(imageFile, DISH_COLORS.get(entry.getKey()), dishName, filename);
            successCount++;
            count++;
        }

        log.info("✅ 菜品图片初始化完成，成功 {} 张（本次新生成 {} 张）", successCount, count);
        log.info("💡 提示：图片已保存到本地，下次启动将不会自动下载");
    }

    /**
     * 统计有效图片数量
     */
    private int countValidImages(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }

        int count = 0;
        for (File f : files) {
            if (f.isFile() && (f.getName().endsWith(".jpg") || f.getName().endsWith(".png"))) {
                if (isValidImage(f)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 检查图片是否有效（大小足够）
     */
    private boolean isValidImage(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        // 检查文件大小
        return file.length() >= MIN_VALID_IMAGE_SIZE;
    }

    /**
     * 清理无效图片
     */
    private void cleanInvalidImages(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        int deleted = 0;
        for (File f : files) {
            if (f.isFile() && !isValidImage(f)) {
                if (f.delete()) {
                    log.debug("🗑️ 删除无效图片: {}", f.getName());
                    deleted++;
                }
            }
        }
        if (deleted > 0) {
            log.info("🗑️ 共删除 {} 张无效图片", deleted);
        }
    }

    /**
     * 从网络下载图片
     *
     * @return true=下载成功，false=下载失败
     */
    private boolean downloadImage(String imageUrl, File targetFile, String dishName) {
        HttpURLConnection conn = null;
        InputStream input = null;
        FileOutputStream output = null;

        try {
            log.info("⬇️ 正在下载 {} 的图片...", dishName);

            URL url = new URL(imageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000); // 5秒连接超时
            conn.setReadTimeout(downloadTimeout);   // 可配置读取超时
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("❌ {} 下载失败，HTTP 状态码: {}", dishName, responseCode);
                return false;
            }

            // 检查内容类型
            String contentType = conn.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("❌ {} 下载的内容不是图片: {}", dishName, contentType);
                return false;
            }

            // 下载图片数据
            input = conn.getInputStream();
            output = new FileOutputStream(targetFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytes = 0;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            output.flush();

            // 验证下载的图片大小
            if (totalBytes < MIN_VALID_IMAGE_SIZE) {
                log.warn("❌ {} 下载的图片太小（{} bytes），可能无效", dishName, totalBytes);
                targetFile.delete();
                return false;
            }

            log.info("✅ {} 下载成功（{} KB）", dishName, totalBytes / 1024);
            return true;

        } catch (IOException e) {
            log.warn("❌ {} 下载异常: {}", dishName, e.getMessage());
            // 清理不完整的文件
            if (targetFile.exists()) {
                targetFile.delete();
            }
            return false;
        } finally {
            // 关闭资源
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                log.debug("关闭资源失败: {}", e.getMessage());
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 生成美食风格图片（降级方案）
     */
    private void generateFoodImage(File file, Color baseColor, String dishName, String filename) {
        if (baseColor == null) {
            baseColor = new Color(150, 150, 150); // 默认灰色
        }

        int width = 400;
        int height = 300;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 背景渐变
        GradientPaint gradient = new GradientPaint(
            0, 0, baseColor.brighter(),
            0, height, baseColor.darker()
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // 装饰圆形（模拟食物）
        g2d.setColor(baseColor.brighter().brighter());
        g2d.fillOval(width / 2 - 80, height / 2 - 60, 160, 120);

        // 盘子
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(width / 2 - 90, height / 2 - 50, 180, 100);

        // 食物主体
        g2d.setColor(baseColor);
        g2d.fillOval(width / 2 - 70, height / 2 - 40, 140, 80);

        // 添加文字说明（降级图片标识）
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setFont(new Font("Microsoft YaHei", Font.ITALIC, 12));
        g2d.drawString("本地生成", 10, 20);

        // 菜品名称
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(dishName);
        g2d.drawString(dishName, (width - textWidth) / 2, height - 40);

        // 底部阴影条
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillRect(0, height - 30, width, 30);

        g2d.dispose();

        try {
            ImageIO.write(image, "jpg", file);
            log.info("✅ {} 本地生成完成（{} KB）", dishName, file.length() / 1024);
        } catch (IOException e) {
            log.error("生成图片失败: {}", filename, e);
        }
    }
}
