# 修复 M1: CommonController 异常处理

**Files:**
- Modify: `src/main/java/com/reggie/controller/CommonController.java`

## 问题描述

CommonController 中使用 `e.printStackTrace()` 而不是日志框架，导致异常信息无法集中管理。

## 修复内容

### 1. upload() 方法（第77行）

**修改前：**
```java
} catch (IOException e) {
    e.printStackTrace();
}
```

**修改后：**
```java
} catch (IOException e) {
    log.error("文件上传失败", e);
}
```

### 2. download() 方法（第111行）

**修改前：**
```java
} catch (Exception e) {
    e.printStackTrace();
}
```

**修改后：**
```java
} catch (Exception e) {
    log.error("文件下载失败", e);
}
```

## 验收标准

- [ ] 两处 e.printStackTrace() 都替换为 log.error()
- [ ] 添加了清晰的错误描述信息
- [ ] 保留了异常堆栈（通过 log.error 的第二个参数）
- [ ] 编译通过
- [ ] 所有现有测试通过

