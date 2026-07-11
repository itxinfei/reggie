# TestImageGenerator 图片生成器改进说明

## 改进概述

### 之前的问题
1. ❌ 生成的是简单的色块模拟图（331字节），不是真实图片
2. ❌ 无法区分有效图片和无效图片
3. ❌ 网络环境下无法获取真实菜品照片

### 现在的方案
1. ✅ **优先下载真实网络图片**（Unsplash 免费图库）
2. ✅ **智能图片验证**（大小检查，自动清理假图片）
3. ✅ **降级机制**（网络失败时本地生成，带水印标识）
4. ✅ **可配置开关**（`reggie.image.download-real-images`）

---

## 核心特性

### 1. 真实图片下载
- 使用 **Unsplash 免费图库**的真实菜品照片
- 自动验证图片有效性（大小 > 1KB）
- 支持超时和重试机制

### 2. 图片有效性验证
```java
// 最小有效图片大小：1KB
private static final int MIN_VALID_IMAGE_SIZE = 1024;

// 自动清理小于 1KB 的假图片
private void cleanInvalidImages(File dir) { ... }
```

### 3. 智能跳过逻辑
```java
// 只跳过已存在的【有效】图片
if (imageFile.exists() && isValidImage(imageFile)) {
    log.info("✅ 已有 {} 张有效图片（期望 {} 张），跳过生成", validImageCount, expectedCount);
    return;
}
```

### 4. 降级方案
- 网络下载失败 → 本地生成带水印的图片
- 本地生成图片添加 "本地生成" 标识
- 避免程序因网络问题无法启动

---

## 配置说明

### application.yml

```yaml
# 测试图片生成配置
reggie:
  image:
    # 是否下载真实网络图片（true=下载，false=仅本地生成）
    download-real-images: true
    # 网络图片下载超时时间（毫秒）
    download-timeout: 10000
```

### 配置场景

| 场景 | 配置值 | 说明 |
|------|--------|------|
| 开发环境（有网络） | `download-real-images: true` | 下载真实图片（默认） |
| 离线/内网环境 | `download-real-images: false` | 仅使用本地生成 |
| 慢速网络 | `download-timeout: 20000` | 增加超时时间 |

---

## 启动日志示例

### 场景1：首次启动（下载真实图片）
```
2026-07-08 12:15:00.123 [main] INFO  TestImageGenerator - 🎨 开始生成/下载测试菜品图片（已有 0 张，需要 19 张，真实图片：是）...
2026-07-08 12:15:01.456 [main] INFO  TestImageGenerator - ⬇️ 正在下载 红烧肉 的图片...
2026-07-08 12:15:02.123 [main] INFO  TestImageGenerator - ✅ 红烧肉 下载成功（559 KB）
2026-07-08 12:15:02.456 [main] INFO  TestImageGenerator - ⬇️ 正在下载 宫保鸡丁 的图片...
...
2026-07-08 12:15:30.123 [main] INFO  TestImageGenerator - ✅ 测试图片处理完成，成功 19 张（新下载/生成 19 张）
```

### 场景2：二次启动（跳过已存在）
```
2026-07-08 12:16:00.123 [main] INFO  TestImageGenerator - ✅ 已有 19 张有效图片（期望 19 张），跳过生成
```

### 场景3：检测到假图片（自动清理）
```
2026-07-08 12:17:00.123 [main] WARN  TestImageGenerator - ⚠️ 检测到无效图片（可能损坏或不完整），正在清理...
2026-07-08 12:17:00.456 [main] INFO  TestImageGenerator - 🗑️ 共删除 5 张无效图片
2026-07-08 12:17:00.789 [main] INFO  TestImageGenerator - 🎨 开始生成/下载测试菜品图片（已有 14 张，需要 19 张，真实图片：是）...
```

### 场景4：网络失败（降级生成）
```
2026-07-08 12:18:00.123 [main] WARN  TestImageGenerator - ⚠️ 红烧肉 下载失败，降级到本地生成
2026-07-08 12:18:00.456 [main] INFO  TestImageGenerator - ✅ 红烧肉 本地生成完成（8 KB）
```

---

## 真实菜品列表

目前已配置 19 道真实菜品：

### 荤菜
- 红烧肉 (hongshaorou)
- 宫保鸡丁 (gongbaojiding)
- 鱼香肉丝 (yuxiangrous)
- 辣子鸡 (laziji)
- 糖醋里脊 (tangculiji)
- 清蒸鲈鱼 (qingzhengluyu)

### 素菜
- 凉拌黄瓜 (liangbanghuanggua)
- 拍黄瓜 (paohuanggua)
- 麻婆豆腐 (mapotoufu)
- 蒜蓉西兰花 (suorongxilanhua)
- 番茄鸡蛋 (fanqiejidan)

### 汤类
- 西湖牛肉羹 (xihuniurougeng)
- 番茄鸡蛋汤 (fanqijidantang)

### 主食
- 扬州炒饭 (yangzhouchaofan)
- 牛肉面 (niuroumian)
- 小笼包 (xiaolongbao)

### 小吃
- 薯条 (shutiao)
- 鸡米花 (jimihua)
- 老醋花生 (laocuhuasheng)

---

## 添加新菜品

### 1. 添加图片 URL
```java
DISH_IMAGE_URLS.put("xinpin", "https://images.unsplash.com/photo-xxx?w=400&h=300&fit=crop");
```

### 2. 添加中文名
```java
DISH_NAMES.put("xinpin", "新品名");
```

### 3. 添加颜色（降级用）
```java
DISH_COLORS.put("xinpin", new Color(100, 100, 100));
```

---

## 图片存储位置

```
uploads/
└── images/
    └── dishes/
        ├── hongshaorou.jpg      (559 KB - 真实图片)
        ├── gongbaojiding.jpg    (559 KB - 真实图片)
        ├── yuxiangrous.jpg      (8 KB - 本地生成)
        └── ...
```

---

## 注意事项

### ⚠️ 网络依赖
- 首次启动会从 Unsplash 下载图片（需要外网访问）
- 如果网络不可用，自动降级到本地生成（带水印）

### ⚠️ 离线部署
如果部署环境无法访问外网，请配置：
```yaml
reggie:
  image:
    download-real-images: false
```

### ⚠️ 图片版权
- 使用 **Unsplash 免费图库**的图片
- 遵循 Unsplash 使用条款
- 建议生产环境替换为自己的真实菜品照片

### ⚠️ 图片大小
- 下载的图片约 400-600 KB
- 本地生成的图片约 8-10 KB
- 建议生产环境使用自己的压缩图片

---

## 性能优化

### 图片缓存
- 有效图片会被缓存，启动时自动跳过
- 只在图片缺失或损坏时重新下载/生成

### 并发控制
- 串行下载，避免并发过多
- 单张图片下载失败不影响其他图片

### 资源释放
- 使用 try-finally 确保资源正确关闭
- 不完整的文件会自动删除
