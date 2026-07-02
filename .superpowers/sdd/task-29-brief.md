# 修复 M5: @Transactional 统一配置 rollbackFor

**Files:**
- Modify: `src/main/java/com/reggie/service/impl/OrderServiceImpl.java`
- Modify: `src/main/java/com/reggie/service/impl/SetmealServiceImpl.java`
- Modify: `src/main/java/com/reggie/service/impl/DishServiceImpl.java`

## 问题描述

所有 @Transactional 注解缺少 rollbackFor 配置，默认只回滚 RuntimeException，不回滚受检异常。

## 修复方案

将所有 @Transactional 注解改为：
```java
@Transactional(rollbackFor = Exception.class)
```

## 需要修改的文件和位置

### 1. OrderServiceImpl.java（2处）

第46行：
```java
@Transactional(rollbackFor = Exception.class)
public void submit(Orders orders) {
```

第193行：
```java
@Transactional(rollbackFor = Exception.class)
public void updateStatus(...) {
```

### 2. SetmealServiceImpl.java（4处）

第36行、70行、87行、112行 - 所有 @Transactional 注解

### 3. DishServiceImpl.java（3处）

第35行、91行、115行 - 所有 @Transactional 注解

**总计：9处 @Transactional 注解需要修改**

## 验收标准

- [ ] 所有 @Transactional 添加 rollbackFor = Exception.class
- [ ] 编译通过
- [ ] 所有现有测试通过
