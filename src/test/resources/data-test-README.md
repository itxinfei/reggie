# 测试数据说明文档

## 📋 概述

本文档描述了 `data-test.sql` 文件中的测试数据结构和数量。

**生成时间**: 2026-07-03
**数据总量**: 超过 400 条测试记录
**覆盖模块**: 34 张数据表

---

## 📊 数据统计

| 序号 | 表名 | 数据量 | 说明 |
|------|------|--------|------|
| 1 | tenant | 10 条 | 租户/店铺信息 |
| 2 | employee | 15 条 | 员工信息 |
| 3 | category | 20 条 | 菜品分类(10) + 套餐分类(10) |
| 4 | dish | 20 条 | 菜品信息 |
| 5 | dish_flavor | 30 条 | 菜品口味配置 |
| 6 | setmeal | 15 条 | 套餐信息 |
| 7 | setmeal_dish | 50 条 | 套餐菜品关联 |
| 8 | user | 20 条 | 用户信息 |
| 9 | address_book | 15 条 | 地址簿 |
| 10 | member_level | 5 条 | 会员等级 |
| 11 | member | 15 条 | 会员信息 |
| 12 | points_record | 20 条 | 积分记录 |
| 13 | recharge_record | 10 条 | 充值记录 |
| 14 | coupon_template | 10 条 | 优惠券模板 |
| 15 | coupon_user | 15 条 | 用户优惠券 |
| 16 | dining_area | 8 条 | 就餐区域 |
| 17 | dining_table | 20 条 | 餐桌信息 |
| 18 | dining_queue | 10 条 | 排队信息 |
| 19 | dining_reservation | 12 条 | 预订信息 |
| 20 | supplier | 12 条 | 供应商信息 |
| 21 | material_category | 10 条 | 物料分类 |
| 22 | material | 25 条 | 物料信息 |
| 23 | stock_record | 20 条 | 库存记录 |
| 24 | stock_check | 10 条 | 库存盘点 |
| 25 | stock_check_detail | 30 条 | 库存盘点明细 |
| 26 | purchase_order | 10 条 | 采购单 |
| 27 | purchase_order_detail | 40 条 | 采购单明细 |
| 28 | orders | 15 条 | 订单信息 |
| 29 | order_detail | 45 条 | 订单明细 |
| 30 | shopping_cart | 10 条 | 购物车 |
| 31 | printer_config | 10 条 | 打印机配置 |
| 32 | printer_log | 15 条 | 打印日志 |
| 33 | payment_order | 15 条 | 支付订单 |
| 34 | refund_record | 5 条 | 退款记录 |
| 35 | delivery_order | 10 条 | 配送订单 |

**总计**: 35 张表，402 条数据记录

---

## 🎯 数据特点

### ✅ 覆盖全面
- ✅ 员工管理模块
- ✅ 菜品/套餐管理模块
- ✅ 订单管理模块（堂食、外卖、自提）
- ✅ 会员营销模块（会员、积分、优惠券、充值）
- ✅ 库存管理模块（物料、盘点、采购）
- ✅  dining 管理模块（区域、餐桌、排队、预订）
- ✅ 支付模块（支付、退款）
- ✅ 打印模块（配置、日志）

### ✅ 数据真实
- 使用真实的菜品名称（红烧肉、宫保鸡丁等）
- 使用真实的人名和联系方式
- 使用真实的地址信息
- 符合业务逻辑的数据关联

### ✅ 场景丰富
- 不同订单状态：待处理、已完成、已取消
- 不同支付状态：待支付、已支付、已退款
- 不同库存状态：充足、不足、盘点中
- 不同餐桌状态：空闲、占用、预订
- 排队状态：等待中、叫号中、已入座、已取消

---

## 🚀 使用方法

### 方法 1: 命令行导入 (推荐)

```bash
# MySQL 命令行导入
mysql -u root -p reggie_db < src/test/resources/data-test.sql

# 或使用完整路径
mysql -u root -p your_database < "D:\MyCode\reggie\src\test\resources\data-test.sql"
```

### 方法 2: 使用 Navicat / phpMyAdmin

1. 打开数据库管理工具
2. 连接 MySQL 数据库
3. 选择 `reggie_db` 数据库
4. 点击"导入" → 选择 `data-test.sql` 文件
5. 执行导入

### 方法 3: IDEA Database 工具

1. 打开 IDEA 右侧 Database 面板
2. 连接 MySQL 数据库
3. 右键点击数据库 → "Run SQL Script"
4. 选择 `data-test.sql` 文件
5. 执行

---

## 📝 注意事项

### ⚠️ 执行前准备

1. **确保数据库已创建**
   ```sql
   CREATE DATABASE reggie_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **确保表结构已创建**
   ```bash
   # 先执行 schema.sql 创建表结构
   mysql -u root -p reggie_db < src/test/resources/schema.sql
   ```

3. **按顺序执行**
   - 先执行: `schema.sql` (创建表)
   - 再执行: `data-test.sql` (插入数据)

### ⚠️ 数据依赖关系

部分表存在外键依赖，已按依赖顺序排列：

```
tenant → employee
tenant → category → dish → dish_flavor
tenant → category → setmeal → setmeal_dish
tenant → user → address_book
tenant → member → (points_record, recharge_record, coupon_user)
tenant → dining_area → dining_table
tenant → dining_table → dining_reservation
tenant → supplier → material → (stock_record, stock_check_detail)
stock_check → stock_check_detail
purchase_order → purchase_order_detail
orders → order_detail
payment_order → refund_record
```

### ⚠️ 自定义修改

如需修改数据，可以：

1. **修改租户信息**
   - 所有数据的 `tenant_id` 默认为 `1`
   - 修改时需要同步修改相关数据

2. **修改密码**
   - 员工密码使用 MD5 加密
   - 默认密码: `123456` (MD5: `e10adc3949ba59abbe56e057f20f883e`)

3. **修改时间**
   - 使用 `NOW()` 函数生成当前时间
   - 部分数据使用 `DATE_SUB()` 生成历史时间

---

## 🧪 测试场景

### 场景 1: 员工管理测试
- 15 名员工数据
- 包含不同门店 (tenant_id: 1, 2, 3)
- 包含不同性别、不同状态

### 场景 2: 菜品管理测试
- 20 个菜品，10 个分类
- 30 个口味配置
- 15 个套餐，50 个套餐菜品关联

### 场景 3: 订单管理测试
- 15 个订单，45 个订单明细
- 3 种订单类型：DELIVERY（外卖）、DINING（堂食）、TAKEOUT（自提）
- 4 种订单状态：PENDING、COMPLETED、CANCELLED

### 场景 4: 会员营销测试
- 15 个会员，5 个等级
- 20 条积分记录
- 10 条充值记录
- 10 个优惠券模板，15 张用户优惠券

### 场景 5: 库存管理测试
- 25 种物料，10 个分类，12 个供应商
- 20 条库存记录
- 10 个盘点单，30 条盘点明细
- 10 个采购单，40 条采购明细

### 场景 6: 就餐管理测试
- 8 个就餐区域
- 20 张餐桌
- 10 个排队号
- 12 个预订

### 场景 7: 支付配送测试
- 15 个支付订单
- 5 条退款记录
- 10 个配送订单
- 15 条打印日志

---

## 🔍 验证数据

执行以下 SQL 验证数据是否正确导入：

```sql
-- 查看各表数据量
SELECT '员工表' AS name, COUNT(*) AS count FROM employee
UNION ALL SELECT '菜品表', COUNT(*) FROM dish
UNION ALL SELECT '订单表', COUNT(*) FROM orders
UNION ALL SELECT '会员表', COUNT(*) FROM member;

-- 查看最近订单
SELECT o.id, o.number, u.name, o.amount, o.status
FROM orders o
LEFT JOIN user u ON o.user_id = u.id
ORDER BY o.id DESC
LIMIT 10;

-- 查看热销菜品
SELECT d.name, COUNT(od.id) AS sales_count
FROM order_detail od
LEFT JOIN dish d ON od.dish_id = d.id
GROUP BY od.dish_id
ORDER BY sales_count DESC
LIMIT 10;
```

---

## 📞 技术支持

如有问题，请检查：

1. ✅ MySQL 版本 >= 5.7
2. ✅ 数据库字符集为 `utf8mb4`
3. ✅ 已执行 `schema.sql` 创建表结构
4. ✅ 数据库用户有足够权限

---

**祝测试顺利！** 🎉
