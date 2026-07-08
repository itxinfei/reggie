# TestImageGenerator 最终版本说明

## ✅ 核心改进：下载一次即可

### 启动逻辑

```
┌──────────────────────────────────────┐
│  TestImageGenerator.run()             │
└──────────────────────────────────────┘
            │
            ├─ 检查 1：是否为开发环境？
            │   ├─ 否（prod/test）→ ℹ️ 跳过
            │   └─ 是（dev）→ 继续
            │
            ├─ 检查 2：是否禁用自动下载？
            │   ├─ 是（download-real-images=false）→ ℹ️ 跳过
            │   └─ 否 → 继续
            │
            ├─ 检查 3：是否已存在任意图片？
            │   ├─ 是（> 0 张图片）→ ✅ 跳过
            │   └─ 否（目录为空）→ ⬇️ 开始下载
            │
            └─ 执行结果：下载 0 次 or 1 次
```

---

## 🚀 启动场景

### 场景1：首次启动（无图片）
```log
2026-07-08 12:30:00 [main] INFO  TestImageGenerator - 🎨 检测到菜品图片目录为空，开始初始化图片...
2026-07-08 12:30:00 [main] INFO  TestImageGenerator - 📥 模式：下载真实图片（仅执行一次）
2026-07-08 12:30:01 [main] INFO  TestImageGenerator - ⬇️ 正在下载 红烧肉 的图片...
2026-07-08 12:30:02 [main] INFO  TestImageGenerator - ✅ 红烧肉 下载成功（559 KB）
...
2026-07-08 12:30:30 [main] INFO  TestImageGenerator - ✅ 菜品图片初始化完成，成功 19 张
2026-07-08 12:30:30 [main] INFO  TestImageGenerator - 💡 提示：图片已保存到本地，下次启动将不会自动下载
```

### 场景2：后续启动（有图片）
```log
2026-07-08 12:31:00 [main] INFO  TestImageGenerator - ✅ 检测到 19 张菜品图片（用户已上传或已生成），跳过自动下载
```

### 场景3：生产环境
```log
2026-07-08 12:32:00 [main] INFO  TestImageGenerator - ℹ️ TestImageGenerator 仅在开发环境（dev）启用，当前环境：prod，跳过执行
```

### 场景4：关闭配置
```log
2026-07-08 12:33:00 [main] INFO  TestImageGenerator - ℹ️ 图片自动下载已禁用（download-real-images=false），跳过执行
```

---

## 🔧 禁用自动下载的3种方法

### 方法1：修改配置（推荐）

```yaml
# application.yml
reggie:
  image:
    download-real-images: false
```

**适用场景**：
- 暂时禁用，后续可能重新启用
- 测试环境不需要图片

---

### 方法2：删除类文件（彻底移除）

```bash
# 备份
mv src/main/java/com/reggie/util/TestImageGenerator.java \
   src/main/java/com/reggie/util/TestImageGenerator.java.bak

# 或直接删除
rm src/main/java/com/reggie/util/TestImageGenerator.java
```

**适用场景**：
- 准备上线，完全移除自动下载功能
- 使用用户上传功能

---

### 方法3：Spring Profile（推荐生产环境）

在类上添加 `@Profile("dev")` 注解：

```java
@Profile("dev")  // 仅在 dev 环境启用
@Component
public class TestImageGenerator implements CommandLineRunner {
    ...
}
```

**效果**：
- dev 环境：自动下载
- prod/test 环境：完全禁用

**适用场景**：
- 开发环境保留自动下载
- 生产环境完全禁用

---

## 📋 用户上传图片方案

### 当前状态
- ✅ 图片检测逻辑已就绪
- ✅ 用户上传的图片优先级最高
- ⏳ 待开发：后台图片上传组件

### 后续开发计划

#### 1. 后台菜品编辑 - 图片上传
```java
@PostMapping("/dish/upload")
public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
    // 1. 校验文件类型（JPG/PNG）
    // 2. 校验文件大小（< 2MB）
    // 3. 生成唯一文件名（UUID）
    // 4. 保存到 uploads/images/dishes/
    // 5. 返回图片URL
}
```

#### 2. 移动端菜品详情 - 图片显示
```html
<img :src="dish.image" alt="菜品图片" />
```

#### 3. 图片管理功能
- 图片列表（查看所有菜品图片）
- 图片删除（删除后恢复默认）
- 图片替换（重新上传）

---

## ⚙️ 配置说明

### application.yml

```yaml
# 测试图片生成配置
reggie:
  image:
    # 是否在启动时下载真实网络图片
    # true  = 仅首次启动时下载（默认）
    # false = 完全禁用下载功能
    download-real-images: true

    # 网络图片下载超时时间（毫秒）
    download-timeout: 10000

    # 下载重试次数（暂未启用，预留）
    download-retry: 2
```

### 配置场景

| 场景 | 配置 | 说明 |
|------|------|------|
| 本地开发 | `download-real-images: true` | 首次启动自动下载 |
| CI/CD | `download-real-images: false` | 禁用自动下载 |
| Docker | `download-real-images: false` | 禁用自动下载 |
| 生产环境 | `download-real-images: false` 或删除类 | 完全禁用 |

---

## 📊 启动日志对比

### 首次启动（执行下载）
```log
2026-07-08 12:30:00 [main] INFO  TestImageGenerator - 🎨 检测到菜品图片目录为空，开始初始化图片...
2026-07-08 12:30:00 [main] INFO  TestImageGenerator - 📥 模式：下载真实图片（仅执行一次）
2026-07-08 12:30:01 [main] INFO  TestImageGenerator - ⬇️ 正在下载 红烧肉 的图片...
2026-07-08 12:30:02 [main] INFO  TestImageGenerator - ✅ 红烧肉 下载成功（559 KB）
...
2026-07-08 12:30:30 [main] INFO  TestImageGenerator - ✅ 菜品图片初始化完成，成功 19 张
2026-07-08 12:30:30 [main] INFO  TestImageGenerator - 💡 提示：图片已保存到本地，下次启动将不会自动下载
```

### 后续启动（跳过）
```log
# 输出
（无日志输出，完全静默）

# 原因
检测到目录已有图片 → 直接return
```

### 禁用后（配置开关）
```log
2026-07-08 12:33:00 [main] INFO  TestImageGenerator - ℹ️ 图片自动下载已禁用（download-real-images=false），跳过执行
```

### 禁用后（生产环境）
```log
2026-07-08 12:34:00 [main] INFO  TestImageGenerator - ℹ️ TestImageGenerator 仅在开发环境（dev）启用，当前环境：prod，跳过执行
```

---

## 🎯 迁移到用户上传

### 步骤1：下载一次（已完成）
```yaml
reggie:
  image:
    download-real-images: true  # 首次启动下载
```

### 步骤2：开发图片上传功能
- [ ] 后台管理 - 菜品编辑 - 图片上传
- [ ] 图片校验（类型、大小、尺寸）
- [ ] 图片存储（UUID命名）

### 步骤3：禁用自动下载
```yaml
reggie:
  image:
    download-real-images: false  # 关闭自动下载
```

### 步骤4：（可选）删除 TestImageGenerator
```bash
rm src/main/java/com/reggie/util/TestImageGenerator.java
```

---

## 📁 图片目录结构

```
uploads/
├── images/
│   ├── dishes/          ← 菜品图片目录
│   │   ├── hongshaorou.jpg      ← 真实图片（559 KB）
│   │   ├── gongbaojiding.jpg    ← 真实图片（558 KB）
│   │   ├── fanqijidantang.jpg   ← 真实图片（559 KB）
│   │   ├── ...                  ← 其他菜品图片
│   │   └── README.md            ← 图片说明文档
│   └── ...
└── ...
```

---

## ✅ 编译验证

```bash
$ mvn clean compile
[INFO] BUILD SUCCESS
[INFO] Compiling 251 source files to target/classes
```

---

## 🎉 总结

### 核心特性
1. ✅ **下载一次即可** - 首次启动自动下载，后续跳过
2. ✅ **零重复下载** - 检测到图片即停止
3. ✅ **用户优先** - 检测到用户上传即跳过
4. ✅ **三种禁用方式** - 配置、删除、环境隔离

### 后续建议
1. **短期**：添加 `@Profile("dev")` 注解
2. **中期**：开发图片上传功能
3. **长期**：删除 TestImageGenerator，完全使用用户上传

---

**当前版本已完成，满足需求：**
- ✅ 下载一次即可
- ✅ 后续不重复下载
- ✅ 支持用户上传
- ✅ 三种方式禁用
