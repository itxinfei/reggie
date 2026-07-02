# Task 5: 添加数据库索引

**Files:**
- Modify: `src/test/resources/schema.sql`

## 任务描述

为多租户查询和常用查询条件添加数据库索引，提升查询性能。

## 具体要求

在 `src/test/resources/schema.sql` 的**末尾**添加以下 SQL：

```sql
-- ========================================
-- 多租户查询索引
-- ========================================

-- 员工表：tenant_id 索引
CREATE INDEX idx_employee_tenant ON employee(tenant_id);

-- 菜品表：tenant_id + category_id 联合索引
CREATE INDEX idx_dish_tenant_category ON dish(tenant_id, category_id);

-- 套餐表：tenant_id + category_id 联合索引
CREATE INDEX idx_setmeal_tenant_category ON setmeal(tenant_id, category_id);

-- 订单表：user_id + order_time 联合索引
CREATE INDEX idx_order_user ON orders(user_id, order_time);

-- 地址表：user_id 索引
CREATE INDEX idx_address_user ON address_book(user_id);

-- 购物车表：user_id 索引
CREATE INDEX idx_cart_user ON shopping_cart(user_id);
```

## 验证

1. **查看现有 schema.sql** 确认表结构
2. **添加索引 SQL**
3. **启动应用验证**（不需要重新创建表，只需验证语法）

## 验收标准

- [ ] schema.sql 添加所有索引 SQL
- [ ] 索引命名规范：idx_表名_字段名
- [ ] 多租户字段优先建索引
- [ ] 应用正常启动

## 注意事项

- **不会影响现有数据**，CREATE INDEX 是 DDL 操作
- **如果索引已存在**，MySQL 会报错，需要在测试环境先验证
- **H2 数据库**：CREATE INDEX 语法与 MySQL 兼容

