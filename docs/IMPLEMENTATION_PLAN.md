# 瑞吉外卖系统 — 实施计划（v2 修订版）

> 基于 BUSINESS_GAP_ANALYSIS.md v2 + 代码交叉验证制定
> 创建日期：2026-08-10
> 修订原则：不重复定义已有代码，不随意删除/修改现有文件

---

## 已有功能勘误（重要）

在制定计划前，交叉验证发现以下功能**已实现或部分实现**，不应重复开发：

| 功能 | 实际状态 | 证据 |
|---|---|---|
| 储值卡充值赠送 | **已实现** | `recharge_record.gift_amount` 字段已有数据（500+50、1000+100）；`RechargeDTO.giftAmount` 已接收；`MemberController.recharge()` 已处理 |
| 会员等级自动升级 | **部分实现** | `MemberServiceImpl.addPoints()` 内部已调用 `memberLevelService.findLevelByPoints()` 自动升级；但 `OrderServiceImpl.completeOrder()` **未调用** `addPoints()`，导致订单完成后不自动加分/升级 |
| 积分兑换记录 | **部分实现** | `points_record` 表已有 `EXCHANGE` 类型数据；但无 `points_mall_item` 表和管理界面 |
| 订单备注 | **已实现** | `Orders.remark` 字段存在（`@Schema(description="备注", example="少放辣")`）；缺的是 `OrderDetail` 逐菜品备注 |
| 部分退款 | **已实现** | 累计退款金额校验 + 渠道退款调用 + `RefundRecord` 追踪 |

---

## 总览

| 阶段 | 名称 | 工时 | 优先级 | 核心产出 |
|---|---|---|---|---|
| 1 | 成本闭环 + 基础收银 | 4-5 周 | P0 | BOM 配方、食材成本、毛利率、现金收款、日结交班 |
| 2 | 厨房效率 | 2-3 周 | P0 | KDS 大屏、出单分单、超时预警、逐菜品备注 |
| 3 | 营销引擎 | 3-4 周 | P0 | 优惠券规则、等级自动升降接单、积分商城、充值活动模板 |
| 4 | 外卖对接 | 4-6 周 | P1 | 美团/饿了么真实接单、菜单同步、对账 |
| 5 | 堂食体验 | 3-4 周 | P1 | 扫码点餐、排队增强、预约增强、桌台呼叫 |
| 6 | 多店铺 + 数据 | 4-6 周 | P2 | 连锁管理、深度报表、员工排班 |
| 7 | 体验增强 | 3-4 周 | P2 | 菜品多图/规格/标签、用户端、运营工具 |

---

## 工程规范（贯穿所有阶段）

1. **不重复定义**：新增代码前先搜索是否已有等效实现，复用而非重写
2. **不随意修改**：只修改任务清单中明确标注的文件，不顺手重构无关代码
3. **不删除文件**：所有变更通过新增文件或 ALTER TABLE 实现，不删除现有表/类/方法
4. **多租户隔离**：所有新增表必须包含 `tenant_id` 字段
5. **JDK 8 约束**：禁止 `var`、`List.of()`、`String.isBlank()` 等 JDK 9+ 语法
6. **统一响应**：所有新增接口返回 `R<T>`
7. **MyBatis-Plus 规范**：新增实体继承 `ServiceImpl`，使用 `LambdaQueryWrapper`

---

## 阶段 1：成本闭环 + 基础收银（4-5 周）

### 1.1 菜品-食材 BOM 配方

> 目标：建立"一道菜需要哪些食材、多少量"的数据关联

**数据库变更**（仅新增表，不修改现有表）：
```sql
CREATE TABLE `dish_ingredient` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `material_id` bigint NOT NULL COMMENT '食材ID',
  `quantity` decimal(10,3) NOT NULL COMMENT '用量',
  `unit` varchar(10) NOT NULL DEFAULT 'g' COMMENT '单位(g/kg/ml/个)',
  `is_main` tinyint NOT NULL DEFAULT 0 COMMENT '是否主料',
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint NOT NULL,
  `update_user` bigint NOT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_material` (`dish_id`, `material_id`),
  KEY `idx_dish` (`dish_id`),
  KEY `idx_material` (`material_id`)
) ENGINE=InnoDB COMMENT='菜品-食材配方表';
```

**后端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `entity/DishIngredient.java` | 新建实体 |
| `mapper/DishIngredientMapper.java` | 新建 Mapper |
| `service/DishIngredientService.java` | 新建接口：saveBatch, updateBatch, deleteByDishId, getByDishId |
| `service/impl/DishIngredientServiceImpl.java` | 新建实现 |
| `controller/DishIngredientController.java` | 新建 CRUD + 按菜品查询配方 |

**后端修改文件**（仅追加方法，不改动现有逻辑）：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `service/DishService.java` | 追加 2 个方法签名 | `getCostPrice(Long dishId)`, `getProfitRate(Long dishId)` |
| `service/impl/DishServiceImpl.java` | 追加 2 个方法实现 | 遍历 BOM 查食材单价，累加计算成本价；毛利率 = (售价-成本)/售价 |

**前端修改文件**（仅追加 Tab，不改动现有页面结构）：

| 文件 | 修改内容 |
|---|---|
| `backend/page/food/add.html` | 在现有编辑表单中追加"配方"Tab |
| `backend/api/food.js` | 追加 BOM 相关接口封装 |

**验收标准**：
- [ ] 可以为每道菜添加配方（食材+用量）
- [ ] 保存后可以查看每道菜的成本价
- [ ] 成本价 = 所有食材单价 × 用量之和

---

### 1.2 下单自动扣食材库存

> 目标：下单时根据 BOM 自动扣减食材库存，取消/退款时回退

**后端修改文件**（仅在现有方法中追加调用，不改动现有逻辑）：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `service/impl/OrderServiceImpl.java` | 在 `submit()` 的 try 块中，`deductStockForOrder()` 之后追加一行调用 | `this.deductIngredientStock(shoppingCarts);` |
| `service/impl/OrderServiceImpl.java` | 新增 private 方法 | `deductIngredientStock(shoppingCarts)`：遍历购物车→查 BOM→按比例扣减食材库存 |
| `service/impl/OrderServiceImpl.java` | 新增 private 方法 | `restoreIngredientStock(orderId)`：取消/退款时遍历订单明细→查 BOM→回退食材库存 |
| `service/impl/OrderServiceImpl.java` | 修改 `cancelOrder()` | 在回退菜品库存后追加调用 `restoreIngredientStock(orderId)` |
| `service/impl/OrderServiceImpl.java` | 修改 `submitEatInOrder()` | 在 `deductStockForOrderDetails()` 之后追加调用 `this.deductIngredientStockDetails(orderDetails);` |
| `service/impl/OrderServiceImpl.java` | 新增 private 方法 | `deductIngredientStockDetails(orderDetails)`：堂食下单的食材扣减 |
| `service/MaterialService.java` | 追加方法签名 | `deductStock(Long materialId, BigDecimal qty)` |
| `service/impl/MaterialServiceImpl.java` | 追加方法实现 | 原子扣减食材库存，库存不足时记录 warning 但不阻断 |
| `service/MaterialService.java` | 追加方法签名 | `addStock(Long materialId, BigDecimal qty)` |
| `service/impl/MaterialServiceImpl.java` | 追加方法实现 | 回退食材库存 |
| `service/impl/DishServiceImpl.java` | 修改 `autoToggleSoldOut()` | 增加：检查关联食材库存是否充足，食材不足时也停售 |

**关键逻辑**（伪代码，实际实现时参考现有 `deductStockForOrder` 的写法）：
```java
private void deductIngredientStock(List<ShoppingCart> shoppingCarts) {
    for (ShoppingCart item : shoppingCarts) {
        List<DishIngredient> ingredients = dishIngredientService.getByDishId(item.getDishId());
        for (DishIngredient ing : ingredients) {
            BigDecimal deductQty = ing.getQuantity().multiply(new BigDecimal(item.getNumber()));
            materialService.deductStock(ing.getMaterialId(), deductQty);
        }
    }
}
```

**验收标准**：
- [ ] 下单后食材库存自动减少
- [ ] 取消订单后食材库存自动回退
- [ ] 食材库存不足时关联菜品自动停售

---

### 1.3 成本核算与毛利率

> 目标：让经营者知道每道菜赚多少钱

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `module/report/service/ReportService.java` | 追加方法签名 | `getCostReport(String startDate, String endDate, Long tenantId)` |
| `module/report/service/impl/ReportServiceImpl.java` | 追加方法实现 | 遍历日期范围内已完成订单→查 BOM→累加食材成本→计算毛利率 |
| `service/impl/DashboardServiceImpl.java` | 修改 `getOverview()` | 追加 `avgProfitRate` 字段 |
| `module/export/controller/ExportController.java` | 追加接口 | `exportCostReportExcel()` / `exportCostReportPdf()` |

**验收标准**：
- [ ] Dashboard 首页显示今日毛利率
- [ ] 报表页可以查看每个菜品的毛利率排行
- [ ] 可以导出菜品成本报表

---

### 1.4 现金收款

> 目标：堂食场景支持现金结账

**后端修改文件**（仅修改注释和校验逻辑，不改动现有字段结构）：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `entity/Orders.java` | 修改 `payMethod` 字段的 `@Schema` 注释 | `1=微信 2=支付宝 3=现金` |
| `dto/EatInOrderRequest.java` | 修改 `payMethod` 字段的 `@Schema` 注释 | 同上 |
| `module/report/service/impl/ReportServiceImpl.java` | 修改 `getPaymentAnalysis()` | 增加 `pay_method=3` 的现金统计分支 |

**前端修改文件**：

| 文件 | 修改内容 |
|---|---|
| `backend/page/dining/` 相关页面 | 支付方式选择增加"现金"选项 |

**验收标准**：
- [ ] 堂食下单可以选择"现金支付"
- [ ] 报表中可以看到现金/微信/支付宝的占比

---

### 1.5 日结/交班

> 目标：收银员交接班时打印账目汇总

**数据库变更**（仅新增表）：
```sql
CREATE TABLE `daily_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `settle_date` date NOT NULL COMMENT '结算日期',
  `employee_id` bigint NOT NULL COMMENT '操作员工',
  `cash_total` decimal(12,2) NOT NULL DEFAULT 0 COMMENT '现金总额',
  `wechat_total` decimal(12,2) NOT NULL DEFAULT 0 COMMENT '微信总额',
  `alipay_total` decimal(12,2) NOT NULL DEFAULT 0 COMMENT '支付宝总额',
  `stored_total` decimal(12,2) NOT NULL DEFAULT 0 COMMENT '储值卡总额',
  `order_count` int NOT NULL DEFAULT 0 COMMENT '订单数',
  `refund_total` decimal(12,2) NOT NULL DEFAULT 0 COMMENT '退款总额',
  `actual_total` decimal(12,2) NOT NULL DEFAULT 0 COMMENT '实收总额',
  `remark` varchar(200) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_date` (`tenant_id`, `settle_date`)
) ENGINE=InnoDB COMMENT='日结报表';
```

**后端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `entity/DailySettlement.java` | 新建实体 |
| `mapper/DailySettlementMapper.java` | 新建 Mapper |
| `service/SettlementService.java` | 新建接口：generateSettlement, printSettlement, getHistory |
| `service/impl/SettlementServiceImpl.java` | 新建实现 |
| `controller/SettlementController.java` | 新建：日结生成、打印、历史查询 |

**前端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `backend/page/settlement/daily.html` | 新建：今日汇总 + 交班打印按钮 |
| `backend/page/settlement/history.html` | 新建：历史日结记录列表 |
| `backend/index.html` | 追加菜单项（不改动现有菜单结构，仅追加） |

**验收标准**：
- [ ] 点击"日结"按钮，自动汇总今日各支付方式的金额
- [ ] 可以打印日结小票
- [ ] 可以查看历史日结记录

---

## 阶段 2：厨房效率（2-3 周）

### 2.1 KDS 厨房显示系统

**数据库变更**（仅新增表）：
```sql
CREATE TABLE `kitchen_station` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `name` varchar(50) NOT NULL COMMENT '档口名称',
  `printer_config_id` bigint DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` int NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='厨房档口';

CREATE TABLE `kitchen_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `station_id` bigint NOT NULL,
  `dish_name` varchar(100) NOT NULL,
  `dish_quantity` int NOT NULL,
  `remark` varchar(200) DEFAULT NULL COMMENT '逐菜品备注',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/COOKING/DONE/CANCELLED',
  `print_time` datetime DEFAULT NULL,
  `cook_start_time` datetime DEFAULT NULL,
  `cook_end_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_station_status` (`station_id`, `status`)
) ENGINE=InnoDB COMMENT='厨房生产单';
```

**后端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `entity/KitchenStation.java` | 新建实体 |
| `entity/KitchenOrder.java` | 新建实体 |
| `mapper/KitchenStationMapper.java` | 新建 Mapper |
| `mapper/KitchenOrderMapper.java` | 新建 Mapper |
| `service/KitchenService.java` | 新建接口 |
| `service/impl/KitchenServiceImpl.java` | 新建实现 |
| `controller/KitchenController.java` | 新建 KDS 查询、状态更新接口 |
| `config/WebSocketConfig.java` | 新建 WebSocket 配置（如不存在） |

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `service/impl/OrderServiceImpl.java` | 在 `submit()` 中 `printerService.printOrder()` 之后追加 | 调用 `kitchenService.createKitchenOrders(orderId)` 创建 kitchen_order 记录 |
| `service/impl/OrderServiceImpl.java` | 在 `submitEatInOrder()` 中同理追加 | 堂食下单也创建 kitchen_order |

**前端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `backend/page/kitchen/kds.html` | 新建 KDS 大屏 |
| `backend/page/kitchen/station-list.html` | 新建档口管理 |

**验收标准**：
- [ ] 下单后 KDS 大屏实时显示新订单
- [ ] 按档口分栏显示
- [ ] 厨师点击"完成"后订单状态更新
- [ ] 超时未出餐的订单标红预警

---

### 2.2 出单/生产单

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `service/impl/KitchenServiceImpl.java` | 追加方法 | `printProductionOrder(orderId)`：按档口分单，生成 kitchen_order 记录，调用 printerService 打印 |

**验收标准**：
- [ ] 生产单按档口分单打印
- [ ] 生产单包含菜品名称、数量、逐菜品备注

---

### 2.3 出餐确认 + 超时预警

**后端新增/修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `service/impl/KitchenServiceImpl.java` | 追加方法 | `markDone(kitchenOrderId)`：更新状态 + 记录 cook_end_time |
| `module/schedule/task/KitchenTimeoutTask.java` | 新建定时任务 | 每分钟扫描 COOKING 超过阈值的订单，发送预警通知 |

**验收标准**：
- [ ] 超过 15 分钟未出餐的订单在 KDS 大屏标红
- [ ] 超时订单自动发送通知给店长

---

### 2.4 逐菜品备注

**数据库变更**（仅 ALTER TABLE 追加字段，不改动现有字段）：
```sql
ALTER TABLE `order_detail` ADD COLUMN `remark` varchar(200) DEFAULT NULL COMMENT '逐菜品备注';
```

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `entity/OrderDetail.java` | 追加字段 | `private String remark;` + `@Schema` 注解 |
| `service/impl/OrderServiceImpl.java` | 修改 `submitEatInOrder()` | 保存 `detail.getRemark()` 到 order_detail |
| `service/impl/KitchenServiceImpl.java` | 修改打印逻辑 | 生产单显示 `kitchenOrder.getRemark()` |

**前端修改文件**：

| 文件 | 修改内容 |
|---|---|
| `backend/page/food/add.html` | 选择菜品后追加备注输入框 |
| `front/page/add-order.html` | 菜品旁追加备注图标 |

**验收标准**：
- [ ] 下单时可以为每道菜输入备注
- [ ] 生产单上显示每道菜的备注
- [ ] KDS 大屏显示备注

---

## 阶段 3：营销引擎（3-4 周）

### 3.1 优惠券使用规则引擎

**数据库变更**（仅 ALTER TABLE 追加字段）：
```sql
ALTER TABLE `coupon_template`
  ADD COLUMN `applicable_type` varchar(20) DEFAULT 'ALL' COMMENT 'ALL/CATEGORY/DISH',
  ADD COLUMN `applicable_ids` text DEFAULT NULL COMMENT '适用ID列表(JSON)',
  ADD COLUMN `min_amount` decimal(10,2) DEFAULT NULL COMMENT '最低消费',
  ADD COLUMN `max_per_user` int DEFAULT 1 COMMENT '每人限领',
  ADD COLUMN `usage_start_time` datetime DEFAULT NULL,
  ADD COLUMN `usage_end_time` datetime DEFAULT NULL,
  ADD COLUMN `stackable` tinyint DEFAULT 0 COMMENT '是否可叠加';
```

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `module/member/model/CouponTemplate.java` | 追加字段 | 对应新增的 7 个字段 |
| `module/member/service/CouponTemplateService.java` | 追加方法 | `validateCoupon(couponUserId, orderAmount, orderItems)` |
| `module/member/service/impl/CouponTemplateServiceImpl.java` | 追加实现 | 校验适用范围、最低消费、有效期 |
| `service/impl/OrderServiceImpl.java` | 修改 `submit()` | 在计算总金额后追加：查询可用优惠券→自动匹配最优券→核销→抵扣 |
| `module/member/controller/CouponTemplateController.java` | 修改 `save()`/`update()` | 增加规则字段的接收和保存 |

**前端修改文件**：

| 文件 | 修改内容 |
|---|---|
| `backend/page/member-center/` 优惠券编辑页 | 追加适用范围、最低消费、每人限领等规则配置项 |
| `front/page/add-order.html` | 追加可用优惠券列表，自动推荐最优券 |

**验收标准**：
- [ ] 可以创建"满 100 减 20"的优惠券
- [ ] 可以限制"仅限堂食菜品可用"
- [ ] 下单时自动匹配最优优惠券

---

### 3.2 会员订单积分 + 等级自动升级

> 注意：`addPoints()` 内部已有等级升级逻辑，只需在订单完成时调用即可

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `service/impl/OrderServiceImpl.java` | 修改 `completeOrder()` | 在订单状态改为完成后追加：`memberService.addPoints(userId, points, "ORDER", orderId)` |
| `module/member/service/MemberService.java` | 追加方法 | `addPointsIfOrderComplete(Long orderId)` — 封装积分计算规则（消费金额×倍率） |

**前端无需改动**（等级自动升降逻辑已在 `MemberServiceImpl.addPoints()` 中实现）。

**验收标准**：
- [ ] 订单完成后自动获得积分
- [ ] 积分累计达到阈值后自动升级等级
- [ ] 升级后自动发送通知

---

### 3.3 充值活动模板管理

> 注意：充值赠送功能已在 `RechargeRecordService.recharge(giftAmount)` 中实现，
> 此处只需新增活动模板管理界面，让运营可以配置"充 X 送 Y"规则

**数据库变更**（仅新增表）：
```sql
CREATE TABLE `recharge_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `recharge_amount` decimal(10,2) NOT NULL,
  `gift_amount` decimal(10,2) NOT NULL DEFAULT 0,
  `gift_points` int NOT NULL DEFAULT 0,
  `status` int NOT NULL DEFAULT 1,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='充值赠送活动';
```

**后端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `entity/RechargeActivity.java` | 新建实体 |
| `mapper/RechargeActivityMapper.java` | 新建 Mapper |
| `service/RechargeActivityService.java` | 新建接口 |
| `service/impl/RechargeActivityServiceImpl.java` | 新建实现 |
| `controller/RechargeActivityController.java` | 新建 CRUD 接口 |

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `module/member/controller/MemberController.java` | 修改 `recharge()` | 追加：查询匹配的充值活动→自动填充 giftAmount（如未传入） |

**前端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `backend/page/member-center/recharge-activity.html` | 新建：充值活动配置页面 |

**验收标准**：
- [ ] 可以配置"充 100 送 20"活动
- [ ] 充值时自动匹配活动并赠送
- [ ] 充值记录中显示赠送金额

---

### 3.4 积分商城

**数据库变更**（仅新增表）：
```sql
CREATE TABLE `points_mall_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `image` varchar(200) DEFAULT NULL,
  `points_cost` int NOT NULL,
  `stock` int NOT NULL DEFAULT 0,
  `type` varchar(20) NOT NULL COMMENT 'DISH/COUPON/GIFT',
  `target_id` bigint DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='积分商城商品';
```

**后端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `entity/PointsMallItem.java` | 新建实体 |
| `mapper/PointsMallItemMapper.java` | 新建 Mapper |
| `service/PointsMallService.java` | 新建接口：listItems, exchange |
| `service/impl/PointsMallServiceImpl.java` | 新建实现 |
| `controller/PointsMallController.java` | 新建 CRUD + 兑换接口 |

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `module/member/controller/MemberController.java` | 追加接口 | `/api/member/points/mall` 积分商城列表 |

**前端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `backend/page/member-center/points-mall.html` | 新建：商品列表+兑换 |
| `backend/page/member-center/points-item-list.html` | 新建：商品管理 |

**验收标准**：
- [ ] 可以上架积分商品
- [ ] 会员可以用积分兑换商品
- [ ] 兑换后积分自动扣减（复用已有的 `points_record` EXCHANGE 类型）

---

## 阶段 4：外卖对接（4-6 周）

### 4.1 美团对接

**后端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `module/delivery/platform/meituan/MeituanClient.java` | 新建：美团 API 客户端（签名、token、HTTP 调用） |
| `module/delivery/platform/meituan/MeituanOrderDTO.java` | 新建：美团订单数据结构 |
| `module/delivery/platform/meituan/MeituanMenuDTO.java` | 新建：美团菜单数据结构 |

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `module/delivery/platform/MeituanAdapter.java` | 实现接口方法 | `fetchOrders()`, `acceptOrder()`, `updateStatus()`, `syncMenu()`, `syncStock()`, `verifyCallback()` |
| `module/delivery/service/impl/DeliveryServiceImpl.java` | 实现 `syncMenu()`/`syncStock()` | 调用 MeituanAdapter 实际 API |
| `controller/DeliveryController.java` | 追加 webhook 接口 | `/api/delivery/callback/meituan` |
| `module/schedule/task/` | 新建定时任务 | 每 30 秒拉取美团新订单 |

**前端修改文件**：

| 文件 | 修改内容 |
|---|---|
| `backend/page/delivery/` | 增加美团配置页面（商户ID、API密钥） |

**验收标准**：
- [ ] 美团平台下单后 30 秒内本地接单
- [ ] 本地菜品变更后自动同步到美团
- [ ] 配送状态实时更新

### 4.2 饿了么对接

同美团结构，使用 `ElemeAdapter` 实现。

### 4.3 对账

**后端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `service/ReconciliationService.java` | 新建接口 |
| `service/impl/ReconciliationServiceImpl.java` | 新建实现 |
| `module/schedule/task/DailyReconcileTask.java` | 新建：每日凌晨自动对账 |

**前端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `backend/page/delivery/reconciliation.html` | 新建：对账结果、差异明细 |

---

## 阶段 5：堂食体验（3-4 周）

### 5.1 扫码点餐

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `module/dining/controller/DiningTableController.java` | 修改 `qrcode()` | 生成含 tableId 的点餐 URL |

**前端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `front/page/scan-order.html` | 新建：扫码后点餐页 |
| `backend/page/dining/table-manage.html` | 追加：查看每桌已点菜品 |

### 5.2 排队增强

**后端修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `module/dining/service/impl/QueueServiceImpl.java` | 追加方法 | `estimateWaitTime()`：基于历史翻台率计算 |
| `controller/QueueController.java` | 追加接口 | `remoteTakeNumber()`：手机端远程取号 |
| `module/notification/service/impl/NotificationServiceImpl.java` | 追加调用 | 叫号时自动发送短信/推送 |

### 5.3 预约增强

**数据库变更**：
```sql
CREATE TABLE `reservation_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `date` date NOT NULL,
  `time_slot` varchar(20) NOT NULL COMMENT 'LUNCH/DINNER',
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `max_tables` int NOT NULL DEFAULT 0,
  `booked_count` int NOT NULL DEFAULT 0,
  `deposit_amount` decimal(10,2) DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_date_slot` (`tenant_id`, `date`, `time_slot`)
) ENGINE=InnoDB COMMENT='预约时段';
```

**后端新增/修改文件**：

| 文件 | 说明 |
|---|---|
| `entity/ReservationSlot.java` | 新建实体 |
| `mapper/ReservationSlotMapper.java` | 新建 Mapper |
| `service/ReservationSlotService.java` | 新建接口 |
| `service/impl/ReservationSlotServiceImpl.java` | 新建实现 |
| `module/dining/service/impl/ReservationServiceImpl.java` | 修改 `create()` | 按时段查询可预约数 |
| `module/schedule/task/ReservationReminderTask.java` | 新建 | 提前 1 小时发送预约提醒 |

### 5.4 桌台呼叫

**数据库变更**：
```sql
CREATE TABLE `table_call` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `table_id` bigint NOT NULL,
  `call_type` varchar(20) NOT NULL COMMENT 'WATER/BILL/OTHER',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `response_time` datetime DEFAULT NULL,
  `response_employee_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_table_status` (`table_id`, `status`)
) ENGINE=InnoDB COMMENT='桌台呼叫记录';
```

**后端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `entity/TableCall.java` | 新建实体 |
| `mapper/TableCallMapper.java` | 新建 Mapper |
| `service/TableCallService.java` | 新建接口 |
| `service/impl/TableCallServiceImpl.java` | 新建实现 |
| `controller/TableCallController.java` | 新建：呼叫、响应、查询待处理 |

**前端新增文件**：

| 文件路径 | 说明 |
|---|---|
| `front/page/dining-call.html` | 新建：手机端"呼叫服务"按钮 |
| `backend/page/dining/call-list.html` | 新建：服务员端待响应呼叫列表 |

---

## 阶段 6：多店铺 + 数据（4-6 周）

### 6.1 多店铺管理

| 任务 | 修改文件 | 说明 |
|---|---|---|
| 菜单同步 | `module/store/service/impl/StoreSyncServiceImpl.java` | 追加菜单同步逻辑 |
| 营业时间 | `module/store/model/StoreInfo.java` | 追加营业时间字段 |
| 跨店调拨 | 新建 `controller/StoreTransferController.java` | 跨店库存调拨接口 |
| 聚合报表 | `module/store/controller/StoreDashboardController.java` | 追加总部聚合视图 |
| 员工排班 | 新建 `entity/EmployeeSchedule.java` 等 | 排班 CRUD + 可视化 |

### 6.2 报表深度

| 任务 | 修改文件 | 说明 |
|---|---|---|
| 食材成本报表 | `module/report/service/impl/ReportServiceImpl.java` | 追加方法 |
| 人效分析 | 同上 | 追加方法 |
| 同比/环比 | 同上 | 追加方法 |
| 平台抽成 | 同上 | 追加方法 |
| 营业额预测 | 同上 | 追加方法 |
| 实时大屏 | 新建 `controller/LiveDashboardController.java` | WebSocket 刷新 |

---

## 阶段 7：体验增强（3-4 周）

### 7.1 菜品增强

| 任务 | 新增/修改文件 | 说明 |
|---|---|---|
| 多图 | 新建 `dish_image` 表 + Entity/Mapper/Service | 菜品详情页轮播 |
| 标签 | 新建 `dish_tag` 表 + Entity/Mapper/Service | 标签筛选 |
| 制作时间 | 修改 `entity/Dish.java` 追加 `cookTime` 字段 | ALTER TABLE |
| 规格 | 新建 `dish_spec` 表 + Entity/Mapper/Service | 大/中/小份 |
| 起售数量 | 修改 `entity/Dish.java` 追加 `minOrderQty` 字段 | ALTER TABLE |

### 7.2 用户端增强

| 任务 | 新增/修改文件 | 说明 |
|---|---|---|
| 收藏 | 新建 `user_favorite` 表 + Service | 菜品/店铺收藏 |
| 评价带图 | 修改 `entity/DishEvaluation.java` 追加 `images` 字段 | ALTER TABLE |
| 优惠券中心 | 前端新增页面 | 可用/已用/过期分类 |
| 会员中心 | 前端新增页面 | 余额/积分/等级/消费记录 |

### 7.3 运营工具

| 任务 | 说明 |
|---|---|
| 语音播报 | 修改 `module/printer/service/impl/PrinterServiceImpl.java` 追加 TTS 播报 |
| 打印模板 | 新建模板管理页面 |
| 秒杀 | 新建 `flash_sale` 表 + Service |
| 每日特价 | 修改 `entity/Dish.java` 追加 `isDailySpecial` + 定时上下架 |
| 签到 | 新建 `sign_in_record` 表 + Service |
| 生日营销 | 新建定时任务：扫描当日生日会员→自动发券 |

---

## 依赖关系图

```
阶段 1（成本闭环）
  ├── 1.1 BOM 配方 ──→ 1.2 扣食材库存 ──→ 1.3 成本核算
  ├── 1.4 现金收款（独立）
  └── 1.5 日结交班（独立，依赖 1.4）

阶段 2（厨房效率）
  ├── 2.1 KDS ──→ 2.2 出单分单 ──→ 2.3 超时预警
  └── 2.4 逐菜品备注（独立，可与 2.1 并行）

阶段 3（营销引擎）
  ├── 3.1 优惠券规则（独立）
  ├── 3.2 订单积分+等级升级（独立）
  ├── 3.3 充值活动模板（独立）
  └── 3.4 积分商城（独立）

阶段 4（外卖对接）
  └── 依赖阶段 1.2（食材库存联动）

阶段 5（堂食体验）
  └── 依赖阶段 2（KDS 出单）

阶段 6（多店铺+数据）
  └── 依赖阶段 1（成本数据）

阶段 7（体验增强）
  └── 独立，可随时插入
```

---

## 风险与注意事项

1. **BOM 配方数据录入**：需为现有 30 个菜品逐一配置配方，建议预留 1-2 周给运营团队
2. **美团/饿了么 API 密钥**：审批周期约 1-2 周，建议阶段 4 开始前提前申请
3. **KDS 硬件**：需采购平板或电视作为厨房终端
4. **打印适配**：生产单打印需与现有打印机模块适配，测试约 1 周
5. **Java 8 约束**：所有新增代码必须遵守 JDK 1.8 语法限制
6. **多租户隔离**：所有新增表必须包含 `tenant_id` 字段
7. **现有数据兼容**：ALTER TABLE 追加字段时设置 DEFAULT 值，确保现有数据不受影响

---

## 附录 A：代码规范修复（前置任务）

> 在开始业务开发前，先修复代码规范性问题，为后续开发建立统一标准。
> 原则：只新增文件和追加方法，不删除/重命名现有文件。

### A.1 统一分页响应封装

**问题**：全项目分页响应格式不统一，有的返回 `R<Page<T>>`，有的返回 `R<Map<String, Object>>` 手动拼装。

**新增文件**：

| 文件路径 | 说明 |
|---|---|
| `common/PageResult.java` | 统一分页响应 DTO：records, total, size, current |

**使用方式**：后续所有分页接口统一返回 `R<PageResult<T>>`，不改动现有接口（保持兼容），新接口直接使用。

### A.2 统一选项 DTO

**问题**：`Map<String, List<String>>` 用于下拉选项，无类型安全。

**新增文件**：

| 文件路径 | 说明 |
|---|---|
| `common/OptionDTO.java` | 统一选项 DTO：value, label |

### A.3 提取库存扣减公共方法

**问题**：`OrderServiceImpl` 中 `deductStockForOrder()` 和 `deductStockForOrderDetails()` 逻辑重复，`refundStockForOrder()` 和 `refundStockForOrderDetails()` 也重复。

**修改文件**：

| 文件 | 修改内容 | 说明 |
|---|---|---|
| `service/impl/OrderServiceImpl.java` | 新增 `deductStockForItems(List<Long> dishIds, List<BigDecimal> quantities)` | 抽取公共扣减逻辑 |
| `service/impl/OrderServiceImpl.java` | 新增 `refundStockForItems(List<Long> dishIds, List<BigDecimal> quantities)` | 抽取公共回退逻辑 |
| `service/impl/OrderServiceImpl.java` | 修改 `deductStockForOrder()` | 改为调用公共方法 |
| `service/impl/OrderServiceImpl.java` | 修改 `deductStockForOrderDetails()` | 改为调用公共方法 |
| `service/impl/OrderServiceImpl.java` | 修改 `refundStockForOrder()` | 改为调用公共方法 |
| `service/impl/OrderServiceImpl.java` | 修改 `refundStockForOrderDetails()` | 改为调用公共方法 |

### A.4 提取批量名称回填公共方法

**问题**：`fillSupplierName()`、`fillMaterialName()`、`fillLevelName()` 等在多个 Service 中重复。

**新增文件**：

| 文件路径 | 说明 |
|---|---|
| `common/BatchFillHelper.java` | 通用批量回填工具：`fillNameByIds(list, idGetter, nameGetter, nameSetter)` |

**修改文件**（仅替换调用，不改动方法签名）：

| 文件 | 修改内容 |
|---|---|
| `module/inventory/service/impl/PurchaseOrderServiceImpl.java` | `fillSupplierName()` / `fillMaterialName()` 改为调用 BatchFillHelper |
| `module/inventory/service/impl/StockRecordServiceImpl.java` | `fillMaterialName()` 改为调用 BatchFillHelper |
| `module/member/service/impl/MemberServiceImpl.java` | `fillLevelName()` 改为调用 BatchFillHelper |

---

## 附录 B：架构迁移路径（渐进式）

> 原则：不一次性大重构，在每个阶段开发中顺带迁移。
> 所有迁移只移动文件+更新 import，不改动业务逻辑。

### B.1 现状

项目混用两种架构模式：

| 模式 | 位置 | 包含模块 |
|---|---|---|
| 传统三层（扁平） | `com.reggie.controller/` `entity/` `mapper/` `service/` `dto/` `enums/` | 订单、菜品、用户、员工、地址、购物车、分类、评价、仪表盘 |
| 模块化（DDD 风格） | `com.reggie.module.*` | AI、配送、堂食、库存、会员、通知、支付、打印、推荐、报表、店铺、系统、导出、调度 |

**主要不一致**：

1. 实体层：顶层用 `entity/`（复数命名 Orders），模块用 `model/`（单数命名 DiningTable），sys 模块用 `entity/`
2. DTO 层：顶层 `dto/` 混用 `Dto`/`DTO`/`Request` 三种后缀，模块的 DTO 散落在 `model/` 里
3. 枚举层：16 个在顶层 `enums/`，`PlatformEnum` 在 delivery 模块 `model/`
4. API 路径：有的 `/api/xxx`，有的 `/xxx`
5. 跨模块依赖：顶层 `OrderServiceImpl` 依赖 printer/dining 模块，payment 模块依赖顶层 `OrderService`

### B.2 迁移目标

```
com.reggie
├── common/                          ← 保留：工具类、注解、切面、事件
├── config/                          ← 保留：Spring 配置
├── filter/                          ← 保留：Servlet 过滤器
├── enums/                           ← 保留：所有枚举统一在此
│
├── module/
│   ├── order/                       ← 从顶层迁移
│   │   ├── controller/              ← OrderController, OrderDetailController
│   │   ├── model/                   ← Orders, OrderDetail
│   │   ├── mapper/                  ← OrderMapper, OrderDetailMapper
│   │   ├── dto/                     ← OrderDto, EatInOrderRequest, OrderAgainDTO 等
│   │   └── service/                 ← OrderService, OrderDetailService
│   ├── dish/                        ← 从顶层迁移
│   │   ├── controller/              ← DishController, CategoryController, SetmealController 等
│   │   ├── model/                   ← Dish, Category, Setmeal, SetmealDish, DishFlavor, DishEvaluation
│   │   ├── mapper/
│   │   ├── dto/                     ← DishDto, SetmealDto, DishSaveDTO 等
│   │   └── service/
│   ├── user/                        ← 从顶层迁移
│   │   ├── controller/              ← UserController, EmployeeController, AddressBookController
│   │   ├── model/                   ← User, Employee, AddressBook, Tenant
│   │   ├── mapper/
│   │   ├── dto/                     ← UserLoginDTO, EmployeeLoginDTO 等
│   │   └── service/
│   ├── cart/                        ← 从顶层迁移
│   │   ├── controller/              ← ShoppingCartController
│   │   ├── model/                   ← ShoppingCart
│   │   ├── mapper/
│   │   └── service/
│   ├── dashboard/                   ← 从顶层迁移
│   │   ├── controller/              ← DashboardController
│   │   └── service/                 ← DashboardService
│   ├── evaluation/                  ← 从顶层迁移
│   │   ├── controller/              ← DishEvaluationController
│   │   └── service/                 ← DishEvaluationService
│   │
│   ├── ai/                          ← 保持不变
│   ├── delivery/                    ← 保持不变
│   ├── dining/                      ← 保持不变
│   ├── export/                      ← 保持不变
│   ├── inventory/                   ← 保持不变
│   ├── member/                      ← 保持不变
│   ├── notification/                ← 保持不变
│   ├── payment/                     ← 保持不变
│   ├── printer/                     ← 保持不变
│   ├── recommend/                   ← 保持不变
│   ├── report/                      ← 保持不变
│   ├── schedule/                    ← 保持不变
│   ├── store/                       ← 保持不变
│   └── sys/                         ← model/ 改名 entity/ → model/（与其他模块统一）
```

### B.3 迁移规则

1. **只移动文件 + 更新 import**，不改动任何业务逻辑
2. **移动后保留旧路径的空包或 @Deprecated 类**作为过渡（可选，视情况决定）
3. **枚举统一放 `enums/`**，不带 `Enum` 后缀（`PlatformEnum` → `Platform`）
4. **DTO 后缀统一为 `DTO`**（`DishDto` → `DishDTO`，`SetmealDto` → `SetmealDTO`，`OrderDto` → `OrderDTO`）
5. **API 路径统一加 `/api/` 前缀**（`/dish` → `/api/dish`，`/setmeal` → `/api/setmeal`）
6. **每个模块自包含**：controller/model/mapper/dto/service 全在模块内，不跨层依赖

### B.4 分批迁移计划

| 批次 | 迁移内容 | 时机 | 预估工时 |
|---|---|---|---|
| 第 1 批 | sys 模块 `entity/` → `model/`（统一命名） | 阶段 1 开发时 | 0.5 天 |
| 第 2 批 | 订单模块迁移到 `module/order/` | 阶段 1 开发时（改动 OrderServiceImpl 时顺带） | 1 天 |
| 第 3 批 | 菜品模块迁移到 `module/dish/` | 阶段 1 开发时（改动 DishServiceImpl 时顺带） | 1 天 |
| 第 4 批 | 用户模块迁移到 `module/user/` | 阶段 2 开发时 | 1 天 |
| 第 5 批 | 购物车/仪表盘/评价迁移 | 阶段 3 开发时 | 1 天 |
| 第 6 批 | DTO 后缀统一（Dto → DTO） | 阶段 3 开发时 | 0.5 天 |
| 第 7 批 | API 路径统一加 `/api/` 前缀 | 阶段 5 开发时（前端联调时） | 1 天 |
| 第 8 批 | 枚举统一（PlatformEnum → Platform） | 阶段 4 开发时 | 0.5 天 |
| **合计** | — | — | **6.5 天** |

### B.5 风险控制

1. **每批迁移后必须编译通过**：`mvn compile` 验证
2. **每批迁移后必须跑测试**：`mvn test` 验证
3. **import 更新用 IDE 重构**：不用手动搜索替换，避免遗漏
4. **前端路径不变**：后端 API 路径变更时，同步修改前端 `api/*.js` 中的 URL
5. **Swagger 文档同步**：路径变更后 `@Tag` 和 `@Operation` 注解同步更新
