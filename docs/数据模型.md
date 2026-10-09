# 数据模型

**范围**: `src/main/java/com/reggie/module/*/model/` 下所有带 `@TableName` 的持久化实体（共 117 个），以及 4 个带 `@Version` 乐观锁的核心资金/流转实体。

根包 `src/main/java/com/reggie/entity/` 已清空——所有实体已按模块迁移至 `module/*/model/`。

---

## 1. 全局字段约定（所有实体高度统一）

| 约定 | 值 | 说明 |
|---|---|---|
| 多租户字段 | `Long tenantId` | 几乎全部实体带此字段，`@TableField(fill = FieldFill.INSERT)` |
| 创建时间 | `createTime` 或 `createdTime` | `FieldFill.INSERT` |
| 更新时间 | `updateTime` | `FieldFill.INSERT_UPDATE` |
| 创建人 | `createUser` 或 `createdUser` | `FieldFill.INSERT` |
| 修改人 | `updateUser` | `FieldFill.INSERT_UPDATE` |
| 逻辑删除 | `Integer isDeleted` | `@TableLogic(value="0", delval="1")` |
| 主键类型 | `Long id` | `IdType.AUTO`（多数）或 `IdType.ASSIGN_ID`（雪花 ID） |
| 乐观锁 | `@Version Integer version` | 仅 4 个核心实体（见第 3 节） |

> **命名双风格**: 早期核心表用 `createTime/createdUser`，后期模块统一用 `createdTime/createdUser`。`MyMetaObjectHandler` 兼容两套。

---

## 2. 核心业务实体（精读确认字段）

### Orders（订单主表）
- **表名**: `orders`
- **主键**: `@TableId(ASSIGN_ID)` 雪花 ID
- **乐观锁**: `@Version`
- **关键字段**:
  | 字段 | 类型 | 说明 |
  |---|---|---|
  | number | String | 订单号 |
  | status | Integer | 1待付款→6已退款（6 状态） |
  | userId | Long | 下单用户 |
  | addressBookId | Long | 收货地址 |
  | payMethod | Integer | 1现金/2微信/3支付宝/4银行卡/5储值 |
  | amount | BigDecimal | 实收金额 |
  | source | String | `@TableField("dining_type")` TAKEOUT/EAT_IN/QUEUE/RESERVATION |
  | platformOrderId | String | 平台订单号（唯一键去重） |
  | stockRefunded | Integer | 库存是否回退 |

### OrderDetail（订单明细）
- **表名**: `order_detail`
- **主键**: `@TableId(AUTO)`
- **关键字段**: id, orderId, dishId, setmealId, dishFlavor, number, amount, tenantId

### Dish（菜品）
- **表名**: `dish`
- **主键**: `@TableId(AUTO)`
- **关键字段**: categoryId, price, code, image, status, sort, stockQty, minStock

### Category（分类）
- **表名**: `category`
- **关键字段**: type(1 菜品 / 2 套餐), name, sort

### Setmeal（套餐）
- **表名**: `setmeal`
- **关键字段**: categoryId, name, price, status, code, image

### User（C 端用户）
- **表名**: `user`（无 @TableName，按类名映射）
- **关键字段**: name, phone, sex, idNumber, avatar, status
- **特殊**: `UserMapper` 的查询方法用 `@InterceptorIgnore(tenantLine="true")` 绕过租户拦截（C 端查询）

### Employee（员工）
- **表名**: `employee`
- **关键字段**: username, name, password, passwordType, phone, status, role
- **特殊**: password/passwordType 加 `@JsonIgnore`（防 JSON 泄露，不影响 MP 列映射）

### ShoppingCart（购物车）
- **表名**: `shopping_cart`
- **关键字段**: userId, dishId, setmealId, dishFlavor, number, amount, image

### PaymentOrder（支付订单）
- **表名**: `payment_order`
- **主键**: `@TableId(AUTO)`
- **乐观锁**: `@Version`
- **关键字段**: orderId, tradeNo, channelTradeNo, channel, amount, status(PENDING/SUCCESS/REFUND/FAIL), paidTime

### RefundRecord（退款记录）
- **表名**: `refund_record`
- **主键**: `@TableId(ASSIGN_ID)`
- **乐观锁**: `@Version`
- **关键字段**: paymentOrderId, refundNo, amount, reason, status

### DeliveryOrder（配送订单）
- **表名**: `delivery_order`
- **主键**: `@TableId(ASSIGN_ID)`
- **乐观锁**: `@Version`
- **关键字段**: platformOrderId, platform, dishSummary, amount, phone, address, status
- **特殊**: `createdUser` 通过 `@TableField("created_user")` 显式映射 snake_case 列名

### Member（会员）
- **表名**: `member`
- **关键字段**: userId, levelId, name, phone, points, balance, totalConsumption, status
- **扩展**: `@TableField(exist=false) levelName` 关联查询填充

### Material（物料）
- **表名**: `material`
- **关键字段**: categoryId, name, unit, stockQty, minStock, unitPrice, supplierId, barcode, status
- **扩展**: `categoryName`、`supplierName` 关联查询填充

### OperationLog（操作审计日志）
- **表名**: `operation_log`
- **关键字段**: operatorId, operatorName, operatorIp, module, operationType, tableName, bizId, oldValue/newValue(JSON), requestUrl, requestMethod, requestParams, duration, isSuccess
- **特殊**: `OperationLogMapper` 用 `@InterceptorIgnore(tenantLine="true")`（fail-closed 下避免无租户上下文返空）

### Tenant（租户根表）
- **表名**: `tenant`
- **关键字段**: name, phone, address, status, passwordType
- **特殊**: **无 tenantId 字段**（本身即租户主体）

### StoreInfo（门店扩展信息）
- **表名**: `store_info`
- **关键字段**: tenantId（关联 tenant）, storeCode, storeType(1总店/2直营/3加盟), parentTenantId, deliveryRadius, minDeliveryAmount, longitude, latitude
- **业务含义**: 在 Tenant 基础上补充门店运营信息，实现总部-分店隔离

### Region（行政区划）
- **表名**: `region`
- **关键字段**: name, code, parentId（自关联）, level(1省/2市/3区/4街道), sort
- **特殊**: `@TableField(exist=false) children` 树形；**无 tenantId**（全局共享表）

### ReconciliationStatement（财务对账单）
- **表名**: `reconciliation_statement`
- **关键字段**: statementNo, statementDate, platform, systemAmount, platformAmount, differenceAmount, refundAmount, netAmount, status(0未对账/1已对账/2差异), reconcileUserId

---

## 3. `@Version` 乐观锁覆盖范围

**仅 4 个核心实体（P3-8 修复项）**:

| 实体 | 表名 | 用途 |
|---|---|---|
| Orders | `orders` | 订单状态流转 |
| PaymentOrder | `payment_order` | 支付状态 |
| RefundRecord | `refund_record` | 退款状态 |
| DeliveryOrder | `delivery_order` | 配送状态 |

**风险点**（未加乐观锁但涉及金额/状态变更）: `Member.balance`、`DailySettlement`、`ReconciliationStatement`、`CostRecord`、`PurchaseOrder`。

---

## 4. `IdType.ASSIGN_ID` 雪花 ID 实体

以下实体使用雪花 ID（跨实例唯一，无需数据库自增）:

- `Orders` · `RefundRecord` · `DeliveryOrder`
- `PurchaseOrderDetail`
- `AIConversation` · `AIMessageRecord` · `AiProviderConfig` · `UserProfile`

---

## 5. `@InterceptorIgnore(tenantLine="true")` 绕过租户拦截

**Mapper 层**（非实体）在特定场景下绕过租户拦截器：

| Mapper | 场景 | 说明 |
|---|---|---|
| `UserMapper` | C 端用户查询 | 无租户上下文 |
| `OperationLogMapper` | 审计日志 | fail-closed 下避免无租户返空 |
| `DashboardMapper` | 看板聚合（5 处） | 跨表聚合 |
| `RecommendationFeedbackMapper` | 推荐反馈 | 无租户上下文 |
| `RecommendationCacheMapper` | 推荐缓存 | 无租户上下文 |
| `BrowseHistoryMapper` | 浏览历史 | 无租户上下文 |
| `DeliveryOrderMapper` | 平台配送回传 | 平台侧无租户上下文 |

> **注意**: 绕过拦截器后，SQL 中必须**手动添加** `tenantId = #{tenantId}` 条件，否则构成越权风险。

---

## 6. 实体总览（117 个持久化实体，按模块）

| 模块 | 实体类 → 表名 → 职责 |
|---|---|
| **auth** | Employee → `employee` → 后台员工账号 |
| **user** | User → `user` → C 端用户 |
| **tenant** | Tenant → `tenant` → 租户主体（根表） |
| **store** | StoreInfo → `store_info` → 门店扩展；StoreConfig → `store_config` → 门店配置；StoreEmployeePermission → `store_employee_permission` → 门店员工权限；StoreDailySummary → `store_daily_summary` → 门店日汇总；StoreSyncLog → `store_sync_log` → 门店同步日志 |
| **category** | Category → `category` → 菜品/套餐分类 |
| **dish** | Dish → `dish` → 菜品；DishSpecGroup → `dish_spec_group` → 规格组；DishSpecOption → `dish_spec_option` → 规格选项；DishSpecRelation → `dish_spec_relation` → 规格关联；DishFlavor → `dish_flavor` → 口味；DishEvaluation → `dish_evaluation` → 菜品评价 |
| **setmeal** | Setmeal → `setmeal` → 套餐；SetmealDish → `setmeal_dish` → 套餐菜品关联 |
| **order** | Orders → `orders` → 订单主表；OrderDetail → `order_detail` → 订单明细 |
| **shopping** | ShoppingCart → `shopping_cart` → 购物车 |
| **address** | AddressBook → `address_book` → 收货地址簿 |
| **payment** | PaymentOrder → `payment_order` → 支付订单；RefundRecord → `refund_record` → 退款记录 |
| **delivery** | DeliveryOrder → `delivery_order` → 配送单；Rider → `rider` → 骑手；RiderLocationRecord → `rider_location_record` → 骑手定位；DeliveryTimeRecord → `delivery_time_record` → 配送时长；DeliveryRangeRule → `delivery_range_rule` → 配送范围规则；DeliveryFeeStep → `delivery_fee_step` → 配送费阶梯 |
| **member** | Member → `member` → 会员；MemberLevel → `member_level` → 会员等级；MemberTag → `member_tag` → 会员标签；CouponTemplate → `coupon_template` → 优惠券模板；CouponUser → `coupon_user` → 用户优惠券；PointsRecord → `points_record` → 积分记录；RechargeRecord → `recharge_record` → 充值记录 |
| **inventory** | Material → `material` → 物料；MaterialCategory → `material_category` → 物料分类；Supplier → `supplier` → 供应商；DishMaterial → `dish_material` → 菜品物料 BOM；PurchaseOrder → `purchase_order` → 采购单；PurchaseOrderDetail → `purchase_order_detail` → 采购明细；StockCheck → `stock_check` → 盘点单；StockCheckDetail → `stock_check_detail` → 盘点明细；StockRecord → `stock_record` → 出入库流水 |
| **cost** | DishCost → `dish_cost` → 菜品成本；LaborCost → `labor_cost` → 人工成本；OtherCost → `other_cost` → 其他成本；CostRecord → `cost_record` → 成本记录 |
| **finance** | ReconciliationStatement → `reconciliation_statement` → 对账单；ProfitAnalysis → `profit_analysis` → 利润分析；WithdrawalApplication → `withdrawal_application` → 提现申请 |
| **cashier** | CashierRecord → `cashier_record` → 收银记录；DailySettlement → `daily_settlement` → 日结单 |
| **marketing** | MarketingCampaign → `marketing_campaign` → 营销活动；CampaignUsageRecord → `campaign_usage_record` → 活动使用记录；DiscountRule → `discount_rule` → 折扣规则；FullReductionRule → `full_reduction_rule` → 满减规则；BuyGetFree → `buy_get_free` → 买赠规则；FlashSale → `flash_sale` → 秒杀；NewCustomerDiscount → `new_customer_discount` → 新客折扣；MarketingMessage → `marketing_message` → 营销消息 |
| **dining** | DiningTable → `dining_table` → 堂食桌台；TableArea → `dining_area` → 桌台区域；QueueRecord → `dining_queue` → 排队记录；Reservation → `dining_reservation` → 预订 |
| **attendance** | Attendance → `attendance` → 员工考勤 |
| **schedule** | WorkSchedule → `work_schedule` → 排班 |
| **urgency** | UrgencyRecord → `urgency_record` → 催单记录 |
| **franchise** | Franchisee → `franchisee` → 加盟商；FranchiseContract → `franchise_contract` → 加盟合同；FranchiseSettlement → `franchise_settlement` → 加盟结算 |
| **platform** | PlatformConfig → `platform_config` → 平台配置；DishPlatformMapping → `dish_platform_mapping` → 菜品平台映射；PlatformSyncLog → `platform_sync_log` → 平台同步日志；PlatformReconcileTask → `platform_reconcile_task` → 平台对账任务 |
| **recommend** | BrowseHistory → `user_browse_history` → 浏览历史；UserPreferenceTag → `user_preference_tag` → 用户偏好标签；RecommendationCache → `recommendation_cache` → 推荐缓存；RecommendationFeedback → `recommendation_feedback` → 推荐反馈 |
| **ai** | AIConversation → `ai_conversation` → AI 会话；AIMessageRecord → `ai_message` → AI 消息；UserProfile → `ai_user_profile` → AI 用户画像；AiProviderConfig → `ai_provider_config` → AI 供应商配置 |
| **notification** | NotificationRecord → `notification_record` → 通知记录；NotificationTemplate → `notification_template` → 通知模板；UserDevice → `user_device` → 用户设备 |
| **customer-service** | CsSession → `cs_session` → 客服会话；CsMessage → `cs_message` → 客服消息；Complaint → `complaint` → 投诉 |
| **printer** | PrinterLog → `printer_log` → 打印日志；PrinterConfig → `printer_config` → 打印机配置 |
| **sys** | SystemConfig → `system_config` → 系统配置；Role → `role` → 角色；Permission → `permission` → 权限；RolePermission → `role_permission` → 角色权限关联；OperationLog → `operation_log` → 操作审计日志 |
| **region** | Region → `region` → 行政区划（全局共享） |

---

## 7. 非持久化瞬态模型（无 `@TableName`，易误判）

以下类**不**参与数据库映射，纯 Java 数据结构：

- `UrgencyOrder`、`RetentionMember`、`StoreSearchDTO`
- 各类 `*DTO` / `*VO`（如 `CouponAvailableDTO`、`IssuedMemberVO`、`ExpiringCouponVO`）
- AI 的 `AIChatRequest` / `AIChatResponse` / `AIMessage`
- printer 的 `PrintJob` / `PrintLine` / `PrinterStatus`
- notification 的 `PushMessage`
- delivery 的 `PlatformEnum`

---

## 8. 关键观察

1. **多租户例外表**: `tenant`（根表）、`region`（全局区划）**无 tenantId 字段**。
2. **snake_case 显式映射**: `DeliveryOrder.createdUser`、`DishMaterial` 等用 `@TableField(value="created_user")` 显式指定列名，需注意与其他实体的默认映射差异。
3. **`@TableField(exist=false)` 关联填充字段**: Orders（tableName/customerCount）、Material（categoryName/supplierName）、Member（levelName）、Region（children 树形）、StoreInfo 等。
4. **逻辑删除不一致**: 核心业务表普遍带 `@TableLogic isDeleted`，但部分财务/报表类表（`ReconciliationStatement`、`ProfitAnalysis`、`SystemConfig`）无逻辑删除。
5. **乐观锁覆盖偏窄**: 仅 4 个实体有 `@Version`，其他涉及金额变更的实体（Member.balance、DailySettlement、CostRecord）无保护，存在并发更新风险。
