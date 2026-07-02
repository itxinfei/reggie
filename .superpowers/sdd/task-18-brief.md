# Task 1: 创建状态码枚举类

**Files:**
- Create: `src/main/java/com/reggie/enums/DishStatus.java`
- Create: `src/main/java/com/reggie/enums/OrderStatus.java`
- Create: `src/main/java/com/reggie/enums/UserStatus.java`
- Test: `src/test/java/com/reggie/enums/StatusEnumTest.java`

## 任务描述

创建三个状态码枚举类，替代硬编码的 0/1 状态码。

## 具体要求

### 1. DishStatus.java

```java
package com.reggie.enums;

import lombok.Getter;

/**
 * 菜品状态枚举
 */
@Getter
public enum DishStatus {

    /**
     * 停售
     */
    DISABLED(0, "停售"),

    /**
     * 起售
     */
    ENABLED(1, "起售");

    private final int value;
    private final String desc;

    DishStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
```

### 2. OrderStatus.java

```java
package com.reggie.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatus {

    /**
     * 待付款
     */
    PENDING_PAYMENT(1, "待付款"),

    /**
     * 待接单
     */
    TO_BE_CONFIRMED(2, "待接单"),

    /**
     * 已接单
     */
    CONFIRMED(3, "已接单"),

    /**
     * 派送中
     */
    DELIVERED(4, "派送中"),

    /**
     * 已完成
     */
    COMPLETED(5, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(6, "已取消");

    private final int value;
    private final String desc;

    OrderStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
```

### 3. UserStatus.java

```java
package com.reggie.enums;

import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
public enum UserStatus {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 正常
     */
    ENABLED(1, "正常");

    private final int value;
    private final String desc;

    UserStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
```

### 4. StatusEnumTest.java

```java
package com.reggie.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatusEnumTest {

    @Test
    void testDishStatus() {
        assertEquals(0, DishStatus.DISABLED.getValue());
        assertEquals(1, DishStatus.ENABLED.getValue());
    }

    @Test
    void testOrderStatus() {
        assertEquals(1, OrderStatus.PENDING_PAYMENT.getValue());
        assertEquals(6, OrderStatus.CANCELLED.getValue());
    }

    @Test
    void testUserStatus() {
        assertEquals(0, UserStatus.DISABLED.getValue());
        assertEquals(1, UserStatus.ENABLED.getValue());
    }
}
```

## 验收标准

- [ ] 3个枚举类创建成功
- [ ] StatusEnumTest.java 所有测试通过（Tests run: 3, Failures: 0）
- [ ] 使用 @Getter 注解（Lombok）

