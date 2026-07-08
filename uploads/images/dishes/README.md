# 菜品图片使用说明

## 📸 图片说明

本系统的菜品图片目前使用 **Unsplash 免费图库**的真实菜品照片。

### 现有菜品清单

| 文件名 | 菜品名称 | 状态 | 大小 |
|--------|---------|------|------|
| hongshaorou.jpg | 红烧肉 | ✅ 真实图片 | ~559 KB |
| gongbaojiding.jpg | 宫保鸡丁 | ✅ 真实图片 | ~559 KB |
| fanqijidantang.jpg | 番茄鸡蛋汤 | ✅ 真实图片 | ~559 KB |
| yuxiangrous.jpg | 鱼香肉丝 | ⬇️ 需下载 | - |
| laziji.jpg | 辣子鸡 | ⬇️ 需下载 | - |
| tangculiji.jpg | 糖醋里脊 | ⬇️ 需下载 | - |
| qingzhengluyu.jpg | 清蒸鲈鱼 | ⬇️ 需下载 | - |
| liangbanghuanggua.jpg | 凉拌黄瓜 | ⬇️ 需下载 | - |
| paohuanggua.jpg | 拍黄瓜 | ⬇️ 需下载 | - |
| mapotoufu.jpg | 麻婆豆腐 | ⬇️ 需下载 | - |
| suorongxilanhua.jpg | 蒜蓉西兰花 | ⬇️ 需下载 | - |
| fanqiejidan.jpg | 番茄鸡蛋 | ⬇️ 需下载 | - |
| xihuniurougeng.jpg | 西湖牛肉羹 | ⬇️ 需下载 | - |
| yangzhouchaofan.jpg | 扬州炒饭 | ⬇️ 需下载 | - |
| niuroumian.jpg | 牛肉面 | ⬇️ 需下载 | - |
| xiaolongbao.jpg | 小笼包 | ⬇️ 需下载 | - |
| shutiao.jpg | 薯条 | ⬇️ 需下载 | - |
| jimihua.jpg | 鸡米花 | ⬇️ 需下载 | - |
| laocuhuasheng.jpg | 老醋花生 | ⬇️ 需下载 | - |

**总计：** 19 道菜品

---

## 🔄 图片更新机制

### 自动更新逻辑

系统启动时会自动检查图片：

1. **有效图片存在**（> 1KB）→ ✅ 跳过，保留
2. **无效图片**（< 1KB 或损坏）→ 🗑️ 自动删除
3. **缺失图片** → ⬇️ 自动下载或生成

### 启动示例

```
场景1：首次启动（2张已有，17张缺失）
🎨 开始生成/下载测试菜品图片（已有 2 张，需要 19 张，真实图片：是）...
⬇️ 正在下载 鱼香肉丝 的图片...
✅ 鱼香肉丝 下载成功（559 KB）
...
✅ 测试图片处理完成，成功 19 张（新下载/生成 17 张）

场景2：二次启动（19张全部有效）
✅ 已有 19 张有效图片（期望 19 张），跳过生成
```

---

## ⚙️ 配置说明

### application.yml

```yaml
reggie:
  image:
    # 是否下载真实网络图片
    # true  = 从 Unsplash 下载真实菜品图片（推荐开发环境）
    # false = 仅使用本地生成（推荐离线/内网环境）
    download-real-images: true

    # 下载超时时间（毫秒）
    download-timeout: 10000

    # 下载重试次数（暂未启用，预留）
    download-retry: 2
```

### 使用场景

| 环境 | 配置建议 | 原因 |
|------|----------|------|
| 本地开发 | `download-real-images: true` | 图片质量高，效果好 |
| CI/CD 环境 | `download-real-images: false` | 避免网络依赖，加快构建 |
| Docker 容器 | `download-real-images: false` | 容器可能无法访问外网 |
| 内网部署 | `download-real-images: false` | 无法访问 Unsplash |

---

## 📂 图片管理

### 手动替换图片

如果需要替换为自定义菜品图片：

1. 准备图片（建议尺寸：400x300 像素，JPG 格式）
2. 命名为对应的菜品名，如 `hongshaorou.jpg`
3. 放入目录：`uploads/images/dishes/`
4. 重启应用，系统会自动识别并跳过生成

### 图片命名规则

```
{菜品英文名}.jpg

例如：
- 红烧肉  → hongshaorou.jpg
- 宫保鸡丁 → gongbaojiding.jpg
- 鱼香肉丝 → yuxiangrous.jpg
```

### 支持的格式

- **格式：** JPG / JPEG / PNG
- **推荐尺寸：** 400 x 300 像素
- **推荐大小：** < 500 KB（Web 优化）

---

## 🔧 添加新菜品

### 步骤1：修改 TestImageGenerator.java

在 `DISH_IMAGE_URLS` 中添加图片 URL：

```java
DISH_IMAGE_URLS.put("xinpin",
    "https://images.unsplash.com/photo-xxxxx?w=400&h=300&fit=crop");
```

### 步骤2：添加中文名

在 `DISH_NAMES` 中添加名称：

```java
DISH_NAMES.put("xinpin", "新品名");
```

### 步骤3：添加颜色（可选，用于降级生成）

在 `DISH_COLORS` 中添加颜色：

```java
DISH_COLORS.put("xinpin", new Color(150, 100, 80));
```

### 步骤4：重启应用

系统会自动检测并下载新菜品图片。

---

## ⚠️ 注意事项

### 网络依赖

- **首次启动**需要访问 Unsplash（外网）
- 如果网络不通，会自动降级到本地生成
- 本地生成的图片会标注 "本地生成" 水印

### 版权说明

- 使用 [Unsplash](https://unsplash.com/) 免费图库
- 遵循 [Unsplash 使用条款](https://unsplash.com/terms)
- **建议**：生产环境替换为自己的授权菜品照片

### 性能建议

- 当前图片约 400-600 KB（网络下载）
- 建议生产环境使用压缩后的图片（< 200 KB）
- 可配合 CDN 加速图片访问

### 离线部署

如果部署环境无法访问外网：

1. 配置 `download-real-images: false`
2. 提前准备图片并放入 `uploads/images/dishes/`
3. 或者使用本地图片生成方案

---

## 🐛 常见问题

### Q1: 启动时下载图片很慢？

**A:** 调整超时时间或关闭网络图片下载：
```yaml
reggie:
  image:
    download-real-images: false
```

### Q2: 图片显示不完整或损坏？

**A:** 系统会自动检测并删除无效图片，重启后会重新下载/生成。

### Q3: 如何完全禁用图片自动生成？

**A:** 在 `TestImageGenerator` 类上添加 `@Profile("dev")` 注解，仅在开发环境启用。

### Q4: 如何添加自己的菜品图片？

**A:** 准备图片后放入 `uploads/images/dishes/` 目录，重启应用即可。

---

## 📞 技术支持

如有问题，请查看：
- [Unsplash 免费图库](https://unsplash.com/)
- [项目文档](../README.md)
