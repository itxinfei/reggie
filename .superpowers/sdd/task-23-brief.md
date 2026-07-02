# Task 6: 统一 API 返回格式

**Files:**
- Modify: `src/main/java/com/reggie/common/R.java`

## 任务描述

在 R.java 中添加 timestamp 和 requestId 字段，统一 API 返回格式。

## 具体要求

### 1. 修改 R.java

在 `R.java` 中添加两个新字段：

```java
package com.reggie.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通用返回结果，服务端响应的数据最终都会封装成此对象
 * @param <T>
 */
@Data
public class R<T> {

    private Integer code; //编码：1成功，0和其它数字为失败

    private String msg; //错误信息

    private T data; //数据

    private Map map = new HashMap(); //动态数据

    private Long timestamp; //时间戳（新增）

    private String requestId; //请求ID（新增）

    public static <T> R<T> success(T object) {
        R<T> r = new R<T>();
        r.data = object;
        r.code = 1;
        r.timestamp = System.currentTimeMillis();
        r.requestId = UUID.randomUUID().toString();
        return r;
    }

    public static <T> R<T> error(String msg) {
        R r = new R();
        r.msg = msg;
        r.code = 0;
        r.timestamp = System.currentTimeMillis();
        r.requestId = UUID.randomUUID().toString();
        return r;
    }

    public R<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }

}
```

### 2. 添加 import

```java
import java.util.UUID;
```

### 3. 创建测试

```java
// 在 R.java 同级目录创建 RTest.java
package com.reggie.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RTest {

    @Test
    void testSuccessHasTimestampAndRequestId() {
        R<String> r = R.success("test");
        assertNotNull(r.getTimestamp());
        assertNotNull(r.getRequestId());
        assertTrue(r.getTimestamp() > 0);
        assertFalse(r.getRequestId().isEmpty());
    }

    @Test
    void testErrorHasTimestampAndRequestId() {
        R<String> r = R.error("error");
        assertNotNull(r.getTimestamp());
        assertNotNull(r.getRequestId());
        assertTrue(r.getTimestamp() > 0);
        assertFalse(r.getRequestId().isEmpty());
    }

    @Test
    void testRequestIdIsUnique() {
        R<String> r1 = R.success("test1");
        R<String> r2 = R.success("test2");
        assertNotEquals(r1.getRequestId(), r2.getRequestId());
    }
}
```

## 验收标准

- [ ] R.java 添加 timestamp 字段
- [ ] R.java 添加 requestId 字段
- [ ] success() 和 error() 方法自动填充这两个字段
- [ ] requestId 使用 UUID 保证唯一性
- [ ] RTest.java 所有测试通过（Tests run: 3, Failures: 0）
- [ ] 编译通过
- [ ] 所有现有测试通过

