# 代码质量与性能优化实施计划

**Goal:** 优化代码质量、性能、用户体验（不含功能完善）

**Tech Stack:** Spring Boot 2.4.5 + Spring Cache + Redis + Lombok

---

## Phase 1: 代码质量优化（3个任务）

### Task 1: 创建状态码枚举类

**创建 3 个枚举：**
- `DishStatus` (DISABLED=0, ENABLED=1)
- `OrderStatus` (PENDING_PAYMENT=1, ..., CANCELLED=6)
- `UserStatus` (DISABLED=0, ENABLED=1)

**验收：**
- [ ] 测试通过
- [ ] 提交：`feat: add status enums`

### Task 2: 批量替换状态码魔法值

**文件：** DishController, EmployeeController, UserController, DishServiceImpl, SetmealServiceImpl

**替换示例：**
```java
// 修改前
queryWrapper.eq(Dish::getStatus,1)

// 修改后
queryWrapper.eq(Dish::getStatus, DishStatus.ENABLED.getValue())
```

**验收：**
- [ ] 无硬编码 0/1 状态码
- [ ] 测试通过

### Task 3: 创建集合工具类

**创建 `CollectionUtils`：**
- `toList()` - 类型安全转换
- `mapToIds()` - 提取ID列表
- `mapTo()` - 通用映射

**验收：**
- [ ] 工具类创建完成
- [ ] 测试通过

---

## Phase 2: 性能优化（2个任务）

### Task 4: 添加 Redis 缓存支持

**依赖：** 检查 pom.xml 是否有 spring-boot-starter-data-redis

**缓存配置：**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
      cache-null-values: false
```

**添加 @Cacheable：**
- `CategoryServiceImpl.list()` → `@Cacheable("categories")`
- `SetmealServiceImpl.getByIdWithDish()` → `@Cacheable("setmeal")`
- `DishServiceImpl.listByCategoryId()` → `@Cacheable("dishes")`

**添加 @CacheEvict：**
- 增删改操作时清除相关缓存

**验收：**
- [ ] 缓存配置添加成功
- [ ] 热点接口添加缓存注解
- [ ] 测试通过（配置 redis.host=localhost，若无Redis则跳过集成测试）

### Task 5: 添加数据库索引

**SQL 脚本：**
```sql
-- src/main/resources/schema.sql 末尾添加

-- 多租户查询索引
CREATE INDEX idx_employee_tenant ON employee(tenant_id);
CREATE INDEX idx_dish_tenant_category ON dish(tenant_id, category_id);
CREATE INDEX idx_setmeal_tenant_category ON setmeal(tenant_id, category_id);
CREATE INDEX idx_order_user ON orders(user_id, order_time);
```

**验收：**
- [ ] 索引添加成功
- [ ] 不影响现有数据

---

## Phase 3: 用户体验优化（2个任务）

### Task 6: 统一 API 返回格式

**修改 `R.java`：**
```java
@Data
public class R<T> {
    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;  // 新增
    private String requestId;  // 新增
    
    public static <T> R<T> success(T object) {
        R<T> r = new R<>();
        r.data = object;
        r.code = 1;
        r.timestamp = System.currentTimeMillis();
        return r;
    }
    
    public static <T> R<T> error(String msg) {
        R r = new R<>();
        r.msg = msg;
        r.code = 0;
        r.timestamp = System.currentTimeMillis();
        return r;
    }
}
```

**验收：**
- [ ] timestamp 字段添加
- [ ] requestId 字段添加
- [ ] 测试通过

### Task 7: 补充 API 文档注解

**为所有 Controller 方法添加：**
```java
@Tag(name = "菜品管理", description = "菜品CRUD接口")
@Operation(summary = "新增菜品", description = "保存菜品基本信息及口味")
@Parameter(name = "dishDto", description = "菜品DTO", required = true)
@ApiResponse(responseCode = "200", description = "新增成功")
@ApiResponse(responseCode = "400", description = "参数错误")
```

**验收：**
- [ ] 所有 Controller 有 @Tag
- [ ] 所有方法有 @Operation
- [ ] 测试通过

---

## 验收标准

### 总体
- [ ] 全部测试通过（Tests run: XX, Failures: 0）
- [ ] 代码覆盖率 > 80%
- [ ] 无破坏性变更

### Phase 1
- [ ] 状态码全部使用枚举
- [ ] 公共方法提取完成

### Phase 2
- [ ] 缓存命中率 > 50%（分类、套餐接口）
- [ ] 响应时间 < 10ms（缓存命中）

### Phase 3
- [ ] API 返回格式统一
- [ ] 接口文档完整

---

## 提交规划

```
feat: add status enums
refactor: replace magic status numbers with enums
feat: add CollectionUtils for collection conversion
feat: add Redis cache support for categories/setmeals/dishes
chore: add database indexes for multi-tenant queries
feat: enhance R with timestamp and requestId
docs: add Swagger annotations to all controllers
```
