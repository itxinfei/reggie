package com.reggie.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试图片生成器
 * 启动时自动生成测试菜品图片（仅在开发环境）
 */
@Slf4j
@Component
public class TestImageGenerator implements CommandLineRunner {

    @Value("${reggie.path:}")
    private String configPath;

    private String basePath;

    // 菜品名称和对应颜色
    private static final Map<String, Color> DISH_COLORS = new HashMap<>();

    static {
        // 荤菜 - 红色系
        DISH_COLORS.put("hongshaorou", new Color(180, 60, 60));      // 红烧肉 - 深红
        DISH_COLORS.put("gongbaojiding", new Color(200, 80, 50));    // 宫保鸡丁 - 橙红
        DISH_COLORS.put("yuxiangrous", new Color(160, 100, 70));     // 鱼香肉丝 - 棕色
        DISH_COLORS.put("laziji", new Color(200, 40, 40));           // 辣子鸡 - 红色
        DISH_COLORS.put("tangculiji", new Color(180, 100, 50));      // 糖醋里脊 - 橙色
        DISH_COLORS.put("qingzhengluyu", new Color(220, 200, 180));  // 清蒸鲈鱼 - 浅色

        // 素菜 - 绿色系
        DISH_COLORS.put("liangbanghuanggua", new Color(100, 160, 80));  // 凉拌黄瓜 - 绿色
        DISH_COLORS.put("paohuanggua", new Color(120, 180, 100));      // 拍黄瓜 - 浅绿
        DISH_COLORS.put("mapotoufu", new Color(180, 100, 60));         // 麻婆豆腐 - 橙棕
        DISH_COLORS.put("suorongxilanhua", new Color(60, 140, 60));    // 蒜蓉西兰花 - 深绿
        DISH_COLORS.put("fanqiejidan", new Color(200, 120, 60));       // 番茄鸡蛋 - 橙色

        // 汤类 - 黄色系
        DISH_COLORS.put("xihuniurougeng", new Color(220, 200, 150));   // 西湖牛肉羹 - 浅黄
        DISH_COLORS.put("fanqijidantang", new Color(200, 150, 80));    // 番茄鸡蛋汤 - 橙黄

        // 主食 - 棕色系
        DISH_COLORS.put("yangzhouchaofan", new Color(220, 200, 120));  // 扬州炒饭 - 米黄
        DISH_COLORS.put("niuroumian", new Color(180, 140, 80));        // 牛肉面 - 棕色
        DISH_COLORS.put("xiaolongbao", new Color(240, 230, 200));      // 小笼包 - 白色

        // 小吃 - 紫色系
        DISH_COLORS.put("shutiao", new Color(220, 180, 80));           // 薯条 - 金黄
        DISH_COLORS.put("jimihua", new Color(200, 160, 80));           // 鸡米花 - 金黄
        DISH_COLORS.put("laocuhuasheng", new Color(140, 100, 60));     // 老醋花生 - 棕色
    }

    // 菜品中文名
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

    @Override
    public void run(String... args) {
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

        // 如果目录已存在且有图片，跳过生成
        if (dir.exists() && dir.list() != null) {
            long imageCount = 0;
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".jpg") || f.getName().endsWith(".png")) {
                    imageCount++;
                }
            }
            if (imageCount > 0) {
                log.info("📸 已有 {} 张图片，跳过生成", imageCount);
                return;
            }
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }

        log.info("🎨 开始生成测试菜品图片...");

        int count = 0;
        for (Map.Entry<String, Color> entry : DISH_COLORS.entrySet()) {
            String filename = entry.getKey() + ".jpg";
            Color color = entry.getValue();
            String dishName = DISH_NAMES.getOrDefault(entry.getKey(), entry.getKey());

            File imageFile = new File(dishesDir + filename);
            if (!imageFile.exists()) {
                generateFoodImage(imageFile, color, dishName, filename);
                count++;
            }
        }

        log.info("✅ 测试图片生成完成，共 {} 张", count);
    }

    /**
     * 生成美食风格图片
     */
    private void generateFoodImage(File file, Color baseColor, String dishName, String filename) {
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
        } catch (IOException e) {
            log.error("生成图片失败: {}", filename, e);
        }
    }
}
