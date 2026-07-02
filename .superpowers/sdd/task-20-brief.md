# Task 3: 创建集合工具类

**Files:**
- Create: `src/main/java/com/reggie/utils/optimization/CollectionUtils.java`
- Test: `src/test/java/com/reggie/utils/optimization/CollectionUtilsTest.java`

## 任务描述

创建集合转换工具类，提取重复的 stream 转换逻辑。

## 具体要求

### CollectionUtils.java

```java
package com.reggie.utils.optimization;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 集合转换工具类
 */
public class CollectionUtils {

    /**
     * 类型安全转换（去除警告）
     * @param list 原始列表
     * @param <T> 元素类型
     * @return 转换后的列表
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(List<?> list) {
        return (List<T>) list;
    }

    /**
     * 提取对象列表中的ID列表
     * @param list 对象列表
     * @param getIdFunction 获取ID的函数
     * @param <T> 对象类型
     * @param <ID> ID类型
     * @return ID列表
     */
    public static <T, ID> List<ID> mapToIds(List<T> list, Function<T, ID> getIdFunction) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .map(getIdFunction)
                .collect(Collectors.toList());
    }

    /**
     * 通用映射转换
     * @param list 原始列表
     * @param mapper 映射函数
     * @param <T> 源类型
     * @param <R> 目标类型
     * @return 转换后的列表
     */
    public static <T, R> List<R> mapTo(List<T> list, Function<T, R> mapper) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
}
```

### CollectionUtilsTest.java

```java
package com.reggie.utils.optimization;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CollectionUtilsTest {

    static class TestObj {
        private Long id;
        private String name;

        TestObj(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    @Test
    void testToList() {
        List<String> result = CollectionUtils.toList(Arrays.asList("a", "b", "c"));
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
    }

    @Test
    void testMapToIds() {
        List<TestObj> list = Arrays.asList(new TestObj(1L, "A"), new TestObj(2L, "B"));
        List<Long> ids = CollectionUtils.mapToIds(list, TestObj::getId);
        assertEquals(2, ids.size());
        assertEquals(1L, ids.get(0));
        assertEquals(2L, ids.get(1));
    }

    @Test
    void testMapTo() {
        List<TestObj> list = Arrays.asList(new TestObj(1L, "A"), new TestObj(2L, "B"));
        List<String> names = CollectionUtils.mapTo(list, TestObj::getName);
        assertEquals(2, names.size());
        assertEquals("A", names.get(0));
        assertEquals("B", names.get(1));
    }

    @Test
    void testEmptyList() {
        assertTrue(CollectionUtils.mapToIds(List.of(), TestObj::getId).isEmpty());
        assertTrue(CollectionUtils.mapTo(List.of(), obj -> obj.toString()).isEmpty());
    }
}
```

## 验收标准

- [ ] CollectionUtils.java 创建成功
- [ ] 包含 toList()、mapToIds()、mapTo() 三个方法
- [ ] CollectionUtilsTest.java 所有测试通过（Tests run: 4, Failures: 0）
- [ ] 空列表处理正确（返回 emptyList 而非 null）

