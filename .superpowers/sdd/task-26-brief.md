# 修复 M2: DishController 日志脱敏

**Files:**
- Modify: `src/main/java/com/reggie/controller\DishController.java`

## 问题描述

DishController 中直接打印 `dishDto.toString()`，可能泄露敏感数据（如价格、成本等）。

## 修复内容

### 1. save() 方法（第51行）

**修改前：**
```java
log.info(dishDto.toString());
```

**修改后：**
```java
log.info("新增菜品：name={}, categoryId={}", dishDto.getName(), dishDto.getCategoryId());
```

### 2. update() 方法（第133行）

**修改前：**
```java
log.info(dishDto.toString());
```

**修改后：**
```java
log.info("修改菜品：id={}, name={}", dishDto.getId(), dishDto.getName());
```

## 验收标准

- [ ] 两处 toString() 日志替换为字段级日志
- [ ] 只打印非敏感信息（名称、ID、分类ID）
- [ ] 不打印价格、成本等敏感字段
- [ ] 编译通过
- [ ] 所有现有测试通过

