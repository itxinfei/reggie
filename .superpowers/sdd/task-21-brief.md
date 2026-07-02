# Task 4: 添加 Redis 缓存支持

**Files:**
- Modify: `pom.xml` (添加 Redis 依赖)
- Create: `src/main/java/com/reggie/config/CacheConfig.java` (可选)
- Modify: `src/main/java/com/reggie/service/impl/CategoryServiceImpl.java`
- Modify: `src/main/java/com/reggie/service/impl/SetmealServiceImpl.java`
- Modify: `src/main/java/com/reggie/service/impl/DishServiceImpl.java`
- Modify: `src/main/resources/application.yml` (添加缓存配置)

## 任务描述

为高频访问接口添加 Redis 缓存，提升响应速度。

## 前置要求

**检查 pom.xml 是否已有 Redis 依赖：**
- 如果没有，添加 `spring-boot-starter-data-redis` 依赖
- 如果有，直接使用

## 具体要求

### 1. 添加 Redis 依赖（如果需要）

在 pom.xml 中添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. 配置 application.yml

在 `application.yml` 的 `spring:` 节点下添加：

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
      cache-null-values: false
```

在 `application-dev.yml` 中添加（如果测试环境需要）：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

### 3. 添加缓存注解

**CategoryServiceImpl.java:**
在 `list()` 方法上添加：
```java
@Cacheable(value = "categories", key = "#categoryType")
public List<Category> list(Integer categoryType) {
    // ... 原有逻辑
}
```

**SetmealServiceImpl.java:**
在 `getByIdWithDish()` 方法上添加：
```java
@Cacheable(value = "setmeal", key = "#id")
public SetmealDto getByIdWithDish(Long id) {
    // ... 原有逻辑
}
```

**DishServiceImpl.java:**
在 `listByCategoryId()` 方法上添加：
```java
@Cacheable(value = "dishes", key = "#categoryId")
public List<Dish> listByCategoryId(Long categoryId) {
    // ... 原有逻辑
}
```

### 4. 添加缓存失效注解（重要）

在增删改方法上添加 `@CacheEvict` 或 `@CachePut`：

**CategoryServiceImpl:**
```java
@CacheEvict(value = "categories", allEntries = true)
@Transactional
public void save(Category category) {
    // ... 原有逻辑
}

@CacheEvict(value = "categories", allEntries = true)
@Transactional
public void update(Category category) {
    // ... 原有逻辑
}
```

**SetmealServiceImpl:**
```java
@CacheEvict(value = "setmeal", key = "#setmeal.id")
@Transactional
public void updateWithDish(SetmealDto setmealDto) {
    // ... 原有逻辑
}
```

**DishServiceImpl:**
```java
@CacheEvict(value = "dishes", allEntries = true)
@Transactional
public void saveWithFlavor(DishDto dishDto) {
    // ... 原有逻辑
}

@CacheEvict(value = "dishes", allEntries = true)
@Transactional
public void updateWithFlavor(DishDto dishDto) {
    // ... 原有逻辑
}
```

## 测试注意事项

- **如果没有 Redis 环境**，测试会自动跳过缓存相关逻辑
- **测试重点：** 确保缓存注解不影响原有逻辑
- **运行测试：** `mvn test -DfailIfNoTests=false` 必须通过

## 验收标准

- [ ] pom.xml 添加 Redis 依赖（如果需要）
- [ ] application.yml 添加缓存配置
- [ ] CategoryServiceImpl.list() 添加 @Cacheable
- [ ] SetmealServiceImpl.getByIdWithDish() 添加 @Cacheable
- [ ] DishServiceImpl.listByCategoryId() 添加 @Cacheable
- [ ] 增删改方法添加 @CacheEvict
- [ ] 编译通过
- [ ] 所有测试通过

