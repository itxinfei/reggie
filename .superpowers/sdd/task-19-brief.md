# Task 2: 批量替换状态码魔法值

**Files:**
- Modify: `src/main/java/com/reggie/controller/DishController.java`
- Modify: `src/main/java/com/reggie/controller/EmployeeController.java`
- Modify: `src/main/java/com/reggie/controller/UserController.java`
- Modify: `src/main/java/com/reggie/service/impl/DishServiceImpl.java`
- Modify: `src/main/java/com/reggie/service/impl/SetmealServiceImpl.java`

## 任务描述

使用 Task 1 创建的状态码枚举，替换所有硬编码的 0/1 状态码。

## 扫描与替换规则

### 1. DishController.java

```java
// 第148行：修改前
queryWrapper.eq(Dish::getStatus,1);

// 修改后
queryWrapper.eq(Dish::getStatus, DishStatus.ENABLED.getValue());

// 第164行：修改前
queryWrapper.eq(Dish::getStatus,1);

// 修改后
queryWrapper.eq(Dish::getStatus, DishStatus.ENABLED.getValue());
```

### 2. EmployeeController.java

```java
// 第68行：修改前
if (emp.getStatus() == 0) {
    return R.error("账号已禁用");
}

// 修改后
if (emp.getStatus() != null && emp.getStatus() == UserStatus.DISABLED.getValue()) {
    return R.error("账号已禁用");
}
```

### 3. UserController.java

```java
// 第87行：修改前
user.setStatus(1);

// 修改后
user.setStatus(UserStatus.ENABLED.getValue());
```

### 4. DishServiceImpl.java

查找所有 `setStatus(0)` 和 `setStatus(1)`，替换为：
```java
// 修改前
dish.setStatus(0);
dish.setStatus(1);

// 修改后
dish.setStatus(DishStatus.DISABLED.getValue());
dish.setStatus(DishStatus.ENABLED.getValue());
```

### 5. SetmealServiceImpl.java

```java
// 第88行：修改前
queryWrapper.eq(Setmeal::getStatus, 1);

// 修改后
queryWrapper.eq(Setmeal::getStatus, DishStatus.ENABLED.getValue());
```

查找所有 `setStatus(0)` 和 `setStatus(1)`，同样替换。

## 添加 import

在每个修改的文件中添加：
```java
import com.reggie.enums.DishStatus;
import com.reggie.enums.UserStatus;
```

## 验收标准

- [ ] 所有硬编码的状态码 0/1 已替换为枚举
- [ ] 添加必要的 import
- [ ] 编译通过
- [ ] 所有现有测试通过（mvn test -DfailIfNoTests=false）
- [ ] 可以运行 grep 验证：`grep -rn "getStatus() == 0\|getStatus() == 1" src/main/java/ | grep -v "Test.java" | grep -v "enum" | wc -l` 应为 0

