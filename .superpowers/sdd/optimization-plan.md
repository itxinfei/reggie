# 优化完善专项计划

## 背景

安全加固专项已完成，系统安全性提升。现在需要进行代码质量、性能、用户体验和功能完善四个维度的优化。

## 问题扫描结果

### 1. 代码质量问题

#### 1.1 魔法值问题
**严重性：** MEDIUM  
**位置：**
- `DishController.java:148` - `queryWrapper.eq(Dish::getStatus,1)`
- `DishController.java:164` - `queryWrapper.eq(Dish::getStatus,1)`
- `EmployeeController.java:68` - `if (emp.getStatus() == 0)`
- `TenantController.java:38` - `employee.setStatus(1)`
- `UserController.java:87` - `user.setStatus(1)`
- `SetmealServiceImpl.java:88` - `queryWrapper.eq(Setmeal::getStatus, 1)`
- `DishServiceImpl.java` - `setStatus(0/1)`
- `SetmealServiceImpl.java` - `setStatus(0/1)`

**问题：** 状态码 0/1 硬编码，可读性差，易出错。

#### 1.2 重复代码
**严重性：** MEDIUM  
**位置：**
- `DishServiceImpl.java:41` 和 `87` - 相同的 stream().map() 模式
- `OrderServiceImpl.java:74` - stream().map() 转换
- `OrderServiceImpl.java:140` - stream().map() 提取ID
- `OrderServiceImpl.java:145` - stream().map() 转换DTO
- `SetmealServiceImpl.java:39` - stream().map() 转换
- `SetmealServiceImpl.java:75` - stream().map() 转换

**问题：** 相同的转换逻辑在多处重复，违反 DRY 原则。

#### 1.3 大型文件
**严重性：** LOW  
**文件：**
- `DishController.java` - 201行
- `OrderServiceImpl.java` - 200行
- `DishServiceImpl.java` - 103行
- `SetmealServiceImpl.java` - 114行

**问题：** 文件过大，职责不够聚焦。

### 2. 性能问题

#### 2.1 无缓存
**严重性：** MEDIUM  
**影响接口：**
- `GET /category/list` - 分类列表（高频访问）
- `GET /setmeal/dish/{id}` - 套餐详情（高频访问）
- `GET /dish/list` - 菜品列表（可缓存）

**问题：** 每次请求都查询数据库，响应时间 50-200ms。

#### 2.2 N+1 查询风险
**严重性：** LOW  
**位置：**
- `OrderServiceImpl` 中查询订单列表后，再查询每个订单的详情
- `SetmealServiceImpl` 中查询套餐后，再查询套餐内的菜品

**问题：** 虽然已使用 MyBatis Plus 的关联查询，但仍有优化空间。

### 3. 用户体验问题

#### 3.1 API 返回格式不统一
**严重性：** LOW  
**问题：**
- 分页接口返回格式不一致
- 部分接口返回 `{code: 1, msg: "success", data: {...}}`
- 部分接口直接返回 `data`
- 缺少统一的响应元数据（timestamp、requestId）

#### 3.2 错误消息技术化
**严重性：** LOW  
**问题：**
- "SQLIntegrityConstraintViolationException" 等技术术语直接暴露给用户
- 已在 Task 9 中部分修复，但仍有改进空间

### 4. 功能完善问题

#### 4.1 订单管理缺失
**严重性：** HIGH  
**缺失功能：**
- 订单取消（用户主动取消）
- 订单超时自动取消（30分钟未支付）
- 订单备注

#### 4.2 库存管理缺失
**严重性：** MEDIUM  
**缺失功能：**
- 库存数量管理
- 超卖防护
- 库存预警

## 优化优先级

### Phase 1: 代码质量优化（1天）
- [ ] 提取状态码常量（DishStatus、OrderStatus、UserStatus）
- [ ] 提取公共方法（stream 转换工具）
- [ ] 简化大型文件（提取私有方法）

### Phase 2: 性能优化（1天）
- [ ] 引入 Redis 缓存（分类列表、套餐详情）
- [ ] 优化批量操作事务
- [ ] 添加数据库索引

### Phase 3: 用户体验优化（0.5天）
- [ ] 统一 API 返回格式
- [ ] 补充 API 文档
- [ ] 优化错误消息

### Phase 4: 功能完善（2天）
- [ ] 订单取消功能
- [ ] 订单超时自动取消
- [ ] 库存管理基础

**总预估：4.5天**

## 建议执行顺序

1. **代码质量优化**（降低技术债务）
2. **性能优化**（提升用户体验）
3. **用户体验优化**（改善开发体验）
4. **功能完善**（增加业务价值）

---

**你的想法？这个优化计划合理吗？要不要调整优先级或者增加/删减某些优化项？**
