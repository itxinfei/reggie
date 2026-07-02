# 第二轮修复 #1: CommonController 日志遗漏

**Files:**
- Modify: `src/main/java/com/reggie/controller\CommonController.java`

## 问题描述

upload() 方法第60行仍有 `file.toString()` 日志，之前修复时遗漏。

## 修复内容

### 第60行

**修改前：**
```java
log.info(file.toString());
```

**修改后：**
```java
log.info("文件上传：originalFilename={}, size={}", originalFilename, file.getSize());
```

## 验收标准

- [ ] 替换 file.toString() 为字段级日志
- [ ] 打印原始文件名和文件大小
- [ ] 编译通过
- [ ] 所有现有测试通过
