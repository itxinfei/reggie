# Task 12: CommonController 文件上传校验

**Files:**
- Modify: `src/main/java/com/reggie/controller\CommonController.java`

## 任务描述

为文件上传接口添加文件类型和大小校验。

## 具体要求

### 1. 在 upload() 方法中添加校验

在文件处理逻辑之前添加：

```java
@PostMapping("/upload")
public R<String> upload(MultipartFile file) {
    // 1. 校验文件是否为空
    if (file.isEmpty()) {
        return R.error("上传文件不能为空");
    }

    // 2. 校验文件类型（仅允许图片格式）
    String originalFilename = file.getOriginalFilename();
    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    if (!Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension)) {
        return R.error("文件类型不支持，仅支持jpg、jpeg、png、gif格式");
    }

    // 3. 校验文件大小（5MB）
    if (file.getSize() > 5 * 1024 * 1024) {
        return R.error("文件大小不能超过5MB");
    }

    // ... 原有逻辑保持不变
}
```

### 2. 添加 import

```java
import java.util.Arrays;
import java.util.List;
```

或直接在代码中使用 `List.of("jpg", "jpeg", "png", "gif")`。

## 验收标准

- [ ] upload() 方法添加文件非空校验
- [ ] upload() 方法添加文件类型校验（jpg/jpeg/png/gif）
- [ ] upload() 方法添加文件大小校验（5MB）
- [ ] 编译通过
- [ ] 所有现有测试通过

