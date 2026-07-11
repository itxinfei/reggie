# 代码质量与性能优化设计

## 背景

安全加固专项已完成，现在优化代码质量、性能和用户体验。

## 优化范围（不含功能完善）

### Phase 1: 代码质量优化（1天）

#### 1.1 提取状态码常量
**问题：** 状态码 0/1 硬编码在多处出现
**方案：** 创建 `DishStatus`、`OrderStatus`、`UserStatus` 枚举类
**收益：** 提升可读性，防止拼写错误

#### 1.2 提取公共方法
**问题：** stream().map() 转换逻辑重复7次
**方案：** 创建 `CollectionUtils` 工具类，提取 `toList()`、`mapToId()` 等通用方法
**收益：** 减少重复代码，统一转换逻辑

#### 1.3 简化大型文件
**问题：** DishController 201行，职责混杂
**方案：** 提取私有方法（如 `saveWithFlavor()`、`updateWithFlavor()` 已存在，继续优化）
**收益：** 提升可读性

### Phase 2: 性能优化（1天）

#### 2.1 引入 Redis 缓存
**问题：** 分类列表、套餐详情、菜品列表每次查询数据库
**方案：** 使用 Spring Cache + Redis
- `@Cacheable("categories")` - 分类列表
- `@Cacheable("setmeal")` - 套餐详情
- `@Cacheable("dishes")` - 菜品列表（按分类）
- `@CacheEvict` - 增删改时清除缓存
**收益：** 响应时间从 50-200ms 降至 <10ms

#### 2.2 添加数据库索引
**问题：** 多租户查询无索引优化
**方案：** 添加联合索引
```sql
CREATE INDEX idx_tenant_id ON employee(tenant_id);
CREATE INDEX idx_category_id ON dish(category_id);
CREATE INDEX idx_tenant_category ON dish(tenant_id, category_id);
```
**收益：** 查询性能提升 30-50%

### Phase 3: 用户体验优化（0.5天）

#### 3.1 统一 API 返回格式
**问题：** 部分接口返回格式不统一
**方案：** 在 R.java 中添加 `timestamp` 和 `requestId` 字段
**收益：** 便于前端调试和日志追踪

#### 3.2 补充 API 文档
**问题：** 部分接口缺少文档说明
**方案：** 为所有 Controller 方法添加 Swagger/OpenAPI 注解
**收益：** 提升开发效率

## 技术决策

### 缓存策略
- **使用 Spring Cache 抽象层**，不直接依赖 Redis API
- **TTL 设置：** 分类列表 1小时，套餐详情 30分钟，菜品列表 15分钟
- **缓存失效策略：** 写操作时清除相关缓存（Cache Aside Pattern）

### 状态码设计
- **使用枚举类**而非常量接口，类型更安全
- **实现 `Serializable`**，支持序列化
- **提供 `getValue()` 方法**，兼容数据库存储

## 验收标准

### Phase 1
- [ ] 状态码全部替换为枚举
- [ ] 无硬编码的 0/1 状态码
- [ ] 公共方法提取到工具类
- [ ] 全部测试通过

### Phase 2
- [ ] 分类列表接口响应时间 < 10ms（缓存命中）
- [ ] 套餐详情接口响应时间 < 10ms（缓存命中）
- [ ] 数据库索引添加成功
- [ ] 全部测试通过

### Phase 3
- [ ] R.java 包含 timestamp 和 requestId
- [ ] 所有 Controller 方法有 API 文档注解
- [ ] 全部测试通过

## 向后兼容

- 状态码枚举与数据库 0/1 兼容（通过 `getValue()`）
- 缓存开启/关闭通过配置开关控制（`spring.cache.enabled`）
- API 返回格式扩展字段，不破坏现有结构
