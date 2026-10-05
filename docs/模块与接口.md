# 模块清单与 API 端点

**范围**: `src/main/java/com/reggie/module/` 下 36 个业务模块、73 个 Controller。

**路径前缀约定**: `/backend/`（早期老接口）· `/front/`（C 端）· `/api/`（混合）· `/admin/`（后台管理）· `/common/`（公共）。部分核心业务 Controller 无统一前缀（如 `/order`、`/dish`、`/employee`），见文末"前缀不一致观察"。

> 根包 `com/reggie/controller/` 已完全清空——员工、订单、菜品、分类等核心业务全部模块化下沉。

---

## 模块总览

| # | 模块 | 一句话职责 | Controller 数 | 主要前缀 |
|---|---|---|---|---|
| 1 | ai | AI 对话、智能点餐、菜品生成、经营分析、AI 供应商管理 | 2 | `/api/ai`, `/admin/ai/provider` |
| 2 | auth | 员工认证与员工管理 | 1 | `/employee` |
| 3 | attendance | 员工考勤打卡与统计 | 1 | `/api/attendance` |
| 4 | address | 用户收货地址簿 | 1 | `/address-book` |
| 5 | cashier | 收银记录与日结结算 | 1 | `/cashier` |
| 6 | category | 菜品/套餐分类 | 1 | `/category` |
| 7 | common | 文件上传下载、餐厅公共信息 | 2 | `/common`, `/restaurant` |
| 8 | cost | 菜品/成本核算与利润预警 | 1 | `/cost` |
| 9 | customer-service | 客服会话、消息、投诉工单 | 1 | `/cs` |
| 10 | dashboard | 经营数据看板总览 | 1 | `/api/dashboard` |
| 11 | delivery | 配送单、配送范围费率、骑手轨迹时效 | 3 | `/api/delivery`, `/delivery/enhanced`, `/delivery/tracking` |
| 12 | dining | 堂食桌台、排队叫号、预约 | 4 | `/api/dining/*` |
| 13 | dish | 菜品 CRUD、口味、规格、评价 | 4 | `/dish`, `/dish/spec`, `/api/dish-evaluation` |
| 14 | export | Excel/PDF 导出 | 1 | `/export` |
| 15 | finance | 提现、对账、利润核算 | 1 | `/finance` |
| 16 | franchise | 加盟商、合同、加盟结算 | 3 | `/api/franchise/*` |
| 17 | inventory | 食材库存、采购、出入库、盘点、补货 | 10 | `/api/inventory/*` |
| 18 | marketing | 满减折扣、营销工具、活动推送 | 3 | `/marketing`, `/marketing/tool` |
| 19 | member | 会员、等级、标签、优惠券、积分、充值 | 8 | `/api/member/*`, `/front/coupon` |
| 20 | notification | 通知模板、消息推送、语音播报 | 2 | `/notification`, `/ux` |
| 21 | order | 订单全流程与平台订单拉取 | 3 | `/order`, `/orders/platform` |
| 22 | payment | 支付、退款、回调、查询 | 1 | `/api/payment` |
| 23 | platform | 第三方平台配置、同步、对账 | 5 | `/admin/platform/*`, `/api/platform/*` |
| 24 | printer | 小票打印、打印机与模板配置 | 3 | `/printer`, `/printer/config` |
| 25 | recommend | 菜品推荐、用户偏好、营销消息 | 1 | `/recommend` |
| 26 | region | 行政区划树 | 1 | `/region` |
| 27 | report | 经营报表与增强报表（预测） | 2 | `/api/report`, `/report/enhanced` |
| 28 | retention | 用户留存与流失挽回 | 1 | `/api/retention` |
| 29 | schedule | 员工排班 | 1 | `/api/schedule` |
| 30 | setmeal | 套餐管理与套餐菜品关联 | 2 | `/setmeal`, `/setmeal-dish` |
| 31 | shopping | C 端购物车 | 1 | `/shopping-cart` |
| 32 | store | 多租户门店管理与门店看板 | 2 | `/store`, `/store/dashboard` |
| 33 | sys | 角色权限、系统配置、操作日志 | 4 | `/sys/*` |
| 34 | tenant | 租户注册 | 1 | `/tenant` |
| 35 | urgency | 异常订单紧急干预催单 | 1 | `/api/urgency` |
| 36 | user | C 端用户认证与管理 | 1 | `/user` |

**核心 Controller 迁移对照**（原根包 → 现模块位置）

| 原根包业务 | 现模块 | Controller | 前缀 |
|---|---|---|---|
| 员工 | `module/auth/` | `EmployeeController` | `/employee` |
| 订单 | `module/order/` | `OrderController` | `/order` |
| 菜品 | `module/dish/` | `DishController` | `/dish` |
| 分类 | `module/category/` | `CategoryController` | `/category` |
| 套餐 | `module/setmeal/` | `SetmealController` | `/setmeal` |
| C 端用户 | `module/user/` | `UserController` | `/user` |

---

## 模块详表

### 1. ai（AI 智能）
- **职责**: 大模型接入、AI 对话、智能点餐、菜品描述生成、经营分析、AI 供应商管理
- **主要 Controller**: `AIChatController` (`/api/ai`)、`AiProviderController` (`/admin/ai/provider`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/ai/chat` | 通用 AI 对话 |
| GET | `/api/ai/chat/stream` | AI 流式对话 (SSE) |
| POST | `/api/ai/order-assistant` | 智能点餐助手 |
| GET | `/api/ai/dish-description` | AI 菜品描述生成 |
| POST | `/api/ai/business-analysis` | AI 经营分析 |
| GET | `/api/ai/conversations` | 对话历史列表 |
| GET | `/api/ai/profile/summary` | 用户画像摘要 |
| GET | `/admin/ai/provider/list` | AI 供应商列表 |
| POST | `/admin/ai/provider/activate/{id}` | 启用供应商 |
| POST | `/admin/ai/provider/test/{id}` | 连通性测试 |

### 2. auth（员工认证）
- **主要 Controller**: `EmployeeController` (`/employee`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/employee/login` | 员工登录（BCrypt + 限流） |
| POST | `/employee/logout` | 登出 |
| POST | `/employee/forgot-password` | 忘记密码 |
| GET | `/employee/page` | 员工分页 |
| GET | `/employee/stats` | 员工统计 |
| PUT | `/employee/status` | 单个启用/停用 |
| PUT | `/employee/batch/status` | 批量状态 |
| PUT | `/employee/password` | 修改密码 |
| GET | `/employee/options` | 下拉选项 |

### 3. attendance（考勤）
- **主要 Controller**: `AttendanceController` (`/api/attendance`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/attendance/clockIn` | 上班打卡 |
| POST | `/api/attendance/clockOut` | 下班打卡 |
| GET | `/api/attendance/today` | 今日考勤 |
| GET | `/api/attendance/calendar/{employeeId}` | 考勤日历 |
| GET | `/api/attendance/week-summary` | 周汇总 |
| GET | `/api/attendance/abnormal` | 异常考勤 |

### 4. address（收货地址）
- **主要 Controller**: `AddressBookController` (`/address-book`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/address-book/list` | 地址列表 |
| GET | `/address-book/{id}` | 详情 |
| POST | `/address-book` | 新增（无参） |
| PUT | `/address-book` | 修改 |
| GET | `/address-book/lastUpdate` | 最近使用地址 |

### 5. cashier（收银）
- **主要 Controller**: `CashierController` (`/cashier`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/cashier/record/list` | 收银记录分页 |
| GET | `/cashier/record/order/{orderId}` | 订单关联记录 |
| POST | `/cashier/record` | 新增收银记录 |
| POST | `/cashier/cash-payment` | 现金收款 |
| POST | `/cashier/settlement/execute` | 执行日结（幂等锁） |
| GET | `/cashier/settlement/date/{date}` | 按日期结算单 |
| GET | `/cashier/statistics` | 收银统计 |
| GET | `/cashier/statistics/payment-type` | 按支付方式统计 |
| GET | `/cashier/statistics/trend` | 趋势 |

### 6. category（分类）
- **主要 Controller**: `CategoryController` (`/category`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/category/page` | 分类分页 |
| GET | `/category/list` | 分类列表 |
| GET | `/category/{id}` | 详情 |
| POST | `/category` | 新增 |
| PUT | `/category` | 修改 |
| DELETE | `/category/{id}` | 删除 |
| GET | `/category/options` | 下拉选项 |
| GET | `/category/stats` | 分类统计 |

### 7. common（公共）
- **主要 Controller**: `CommonController` (`/common`)、`RestaurantController` (`/restaurant`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/common/upload` | 文件上传 |
| GET | `/common/download` | 文件下载 |
| GET | `/restaurant/info` | 餐厅公共信息 |

### 8. cost（成本核算）
- **主要 Controller**: `CostController` (`/cost`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/cost/dish` | 新增菜品成本 |
| POST | `/cost/dish/batch` | 批量 |
| DELETE | `/cost/dish/{id}` | 删除 |
| GET | `/cost/dish/{dishId}` | 单菜品成本详情 |
| GET | `/cost/summary` | 成本汇总 |
| GET | `/cost/trend` | 趋势 |
| GET | `/cost/structure` | 结构 |
| GET | `/cost/dish/ranking` | 排行 |
| GET | `/cost/alert` | 成本预警 |

### 9. customer-service（客服）
- **主要 Controller**: `CustomerServiceController` (`/cs`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/cs/session/create` | 创建会话 |
| GET | `/cs/session/list` | 会话列表 |
| GET | `/cs/session/{id}` | 会话详情 |
| POST | `/cs/session/{id}/assign` | 分配坐席 |
| POST | `/cs/session/{id}/close` | 关闭会话 |
| POST | `/cs/message/send` | 发送消息 |
| GET | `/cs/message/unread/{sessionId}` | 未读消息 |
| POST | `/cs/complaint/create` | 创建投诉 |
| POST | `/cs/complaint/{id}/handle` | 处理投诉 |
| GET | `/cs/statistics` | 客服统计 |
| GET | `/cs/agent/{agentId}/workload` | 坐席工作量 |

### 10. dashboard（数据看板）
- **主要 Controller**: `DashboardController` (`/api/dashboard`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/dashboard/overview` | 经营总览（聚合卡片） |
| GET | `/api/dashboard/trend` | 趋势数据 |
| GET | `/api/dashboard/order-status` | 订单状态分布 |
| GET | `/api/dashboard/hot-dishes` | 热销菜品 |
| GET | `/api/dashboard/all` | 看板全量数据 |
| GET | `/api/dashboard/health` | 服务健康检查 |

### 11. delivery（配送）
- **主要 Controller**: `DeliveryController` (`/api/delivery`)、`DeliveryEnhancedController` (`/delivery/enhanced`)、`DeliveryTrackingController` (`/delivery/tracking`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/delivery/orders/{id}` | 配送单详情 |
| GET | `/api/delivery/orders` | 配送单列表 |
| POST | `/api/delivery/accept` | 接单 |
| PUT | `/api/delivery/status` | 状态流转 |
| POST | `/api/delivery/callback/{platform}` | 平台回调 |
| GET | `/delivery/enhanced/range/list` | 配送范围 |
| POST | `/delivery/enhanced/fee/calculate` | 运费计算 |
| POST | `/delivery/enhanced/fee/auto-calculate` | 自动算费 |
| POST | `/delivery/enhanced/distance/calculate` | 距离计算 |
| GET | `/delivery/tracking/rider/list` | 骑手列表 |
| POST | `/delivery/tracking/location/update` | 骑手位置上报 |
| GET | `/delivery/tracking/time/estimate` | 预计送达 |

### 12. dining（堂食）
- **主要 Controller**: `DiningTableController` (`/api/dining/table`)、`TableAreaController` (`/api/dining/area`)、`QueueController` (`/api/dining/queue`)、`ReservationController` (`/api/dining/reservation`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/dining/table/open` | 开台 |
| POST | `/api/dining/table/openWithOrder` | 带订单开台 |
| POST | `/api/dining/table/transfer` | 转台 |
| GET | `/api/dining/table/qrcode/{id}` | 桌台二维码 |
| GET | `/api/dining/area/list` | 桌区列表 |
| GET | `/api/dining/area/options` | 下拉 |
| POST | `/api/dining/queue/take` | 取号排队 |
| PUT | `/api/dining/queue/call` | 叫号 |
| PUT | `/api/dining/queue/seat` | 入座 |
| PUT | `/api/dining/queue/recall/{id}` | 召回 |
| PUT | `/api/dining/reservation/confirm/{id}` | 确认预约 |
| PUT | `/api/dining/reservation/arrive/{id}` | 到店 |
| PUT | `/api/dining/reservation/cancel/{id}` | 取消 |

### 13. dish（菜品）
- **主要 Controller**: `DishController` (`/dish`)、`DishFlavorController` (`/dish-flavor`)、`DishSpecController` (`/dish/spec`)、`DishEvaluationController` (`/api/dish-evaluation`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/dish` | 新增菜品 |
| PUT | `/dish` | 修改菜品（含口味） |
| POST | `/dish/status/{status}` | 批量起售/停售 |
| PUT | `/dish/stock/{id}` | 更新库存与预警 |
| GET | `/dish/spec/group/list` | 规格组 |
| POST | `/dish/spec/option` | 规格项 |
| POST | `/dish/spec/price/calculate` | 规格价格计算 |
| GET | `/api/dish-evaluation/dish/{dishId}` | 菜品评价 |
| PUT | `/api/dish-evaluation/{id}/reply` | 商家回复评价 |

### 14. export（导出）
- **主要 Controller**: `ExportController` (`/export`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/export/orders/excel` | 订单 Excel |
| GET | `/export/orders/pdf` | 订单 PDF |
| GET | `/export/dishes/excel` | 菜品 Excel |
| GET | `/export/employees/excel` | 员工 Excel |
| GET | `/export/employees/pdf` | 员工 PDF |

### 15. finance（财务）
- **主要 Controller**: `FinanceController` (`/finance`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/finance/withdrawal` | 提现申请 |
| POST | `/finance/withdrawal/{id}/review` | 审核 |
| POST | `/finance/withdrawal/{id}/payment` | 打款 |
| GET | `/finance/withdrawal/list` | 提现列表 |
| POST | `/finance/reconciliation/generate` | 生成对账单（幂等锁） |
| POST | `/finance/reconciliation/{id}/confirm` | 确认对账 |
| POST | `/finance/profit/generate` | 生成利润表（幂等锁） |
| GET | `/finance/profit/trend` | 利润趋势 |
| GET | `/finance/profit/structure` | 利润结构 |
| GET | `/finance/statistics` | 财务统计 |

### 16. franchise（加盟）
- **主要 Controller**: `FranchiseeController` (`/api/franchise/franchisee`)、`FranchiseContractController` (`/api/franchise/contract`)、`FranchiseSettlementController` (`/api/franchise/settlement`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/franchise/franchisee/page` | 加盟商分页 |
| GET | `/api/franchise/franchisee/{id}` | 加盟商详情 |
| GET | `/api/franchise/contract/page` | 合同分页 |
| POST | `/api/franchise/settlement/generate` | 生成结算 |
| PUT | `/api/franchise/settlement/confirm/{id}` | 确认结算 |
| PUT | `/api/franchise/settlement/settle/{id}` | 结算完成 |

### 17. inventory（库存）
- **主要 Controller**: `MaterialController`、`MaterialCategoryController`、`SupplierController`、`PurchaseOrderController`、`PurchaseOrderDetailController`、`StockRecordController`、`StockCheckController`、`ReplenishController`、`DishMaterialController`、`InventoryStatsController`（均 `/api/inventory/...`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/inventory/material/page` | 食材分页 |
| GET | `/api/inventory/material/warning` | 库存预警 |
| GET | `/api/inventory/material/warning-stats` | 预警统计 |
| GET | `/api/inventory/material/replenish-suggest` | 补货建议 |
| POST | `/api/inventory/material/batch-restock` | 批量补货 |
| GET | `/api/inventory/purchase-order/page` | 采购单分页 |
| PUT | `/api/inventory/purchase-order/approve/{id}` | 采购审批 |
| PUT | `/api/inventory/purchase-order/receive/{id}` | 收货 |
| PUT | `/api/inventory/purchase-order/cancel/{id}` | 取消 |
| POST | `/api/inventory/stock-record/stockIn` | 入库 |
| POST | `/api/inventory/stock-record/stockOut` | 出库 |
| PUT | `/api/inventory/stock-check/complete/{id}` | 完成盘点 |
| GET | `/api/inventory/dish-material/listByDish/{dishId}` | 菜品用料 |
| POST | `/api/inventory/dish-material/batchSave` | 批量保存 |
| GET | `/api/inventory/stats/overview` | 库存概览 |
| GET | `/api/inventory/stats/purchase-trend` | 采购趋势 |

### 18. marketing（营销）
- **主要 Controller**: `MarketingController` (`/marketing`)、`MarketingToolController` (`/marketing/tool`)、`MarketingCampaignController` (`/marketing`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/marketing/full-reduction` | 新增满减 |
| POST | `/marketing/calculate/full-reduction` | 满减计算 |
| POST | `/marketing/discount` | 新增折扣 |
| POST | `/marketing/calculate/best` | 最优优惠计算 |
| GET | `/marketing/usage/list` | 使用记录 |
| GET | `/marketing/statistics` | 营销统计 |
| GET | `/marketing/tool/flash-sale/active` | 进行中秒杀 |
| POST | `/marketing/tool/flash-sale/calculate` | 秒杀计算 |
| POST | `/marketing/campaigns` | 创建营销活动 |
| PUT | `/marketing/campaigns/{id}/publish` | 发布 |
| PUT | `/marketing/campaigns/{id}/pause` | 暂停 |
| POST | `/marketing/campaigns/push/{campaignId}/{userId}` | 定向推送 |
| POST | `/marketing/campaigns/batch-push/{campaignId}` | 批量推送 |
| POST | `/marketing/campaigns/auto-dispatch-coupons` | 自动发券 |

### 19. member（会员）
- **主要 Controller**: `MemberController`、`MemberLevelController`、`MemberTagController`、`CouponTemplateController`、`CouponUserController`、`PointsRecordController`、`RechargeRecordController`（均 `/api/member/...`）、`FrontCouponController` (`/front/coupon`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/member/member/page` | 会员分页 |
| GET | `/api/member/member/{id}` | 会员详情 |
| GET | `/api/member/member/by-phone` | 按手机号查 |
| POST | `/api/member/member/recharge` | 会员充值 |
| POST | `/api/member/member/deduct-balance` | 扣余额 |
| GET | `/api/member/level/page` | 会员等级 |
| POST | `/api/member/member/{memberId}/tags` | 打标签 |
| POST | `/api/member/coupon-template/claim` | 领券 |
| POST | `/api/member/coupon-template/batch-issue` | 定向发券 |
| POST | `/api/member/coupon-template/issue-by-condition` | 条件发券 |
| GET | `/api/member/coupon-template/expiring` | 即将到期预警 |
| POST | `/api/member/coupon-template/batch-extend` | 批量延期 |
| GET | `/front/coupon/claim/{templateId}` | C 端领取 |
| GET | `/front/coupon/available` | C 端可领券 |
| GET | `/api/member/points/page` | 积分记录 |
| GET | `/api/member/points/stats` | 积分统计 |

### 20. notification（通知）
- **主要 Controller**: `NotificationController` (`/notification`)、`UserExperienceController` (`/ux`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/notification/template/page` | 模板分页 |
| POST | `/notification/send` | 发送通知 |
| POST | `/notification/batch-send` | 批量发送 |
| POST | `/notification/send-all` | 全员发送 |
| GET | `/notification/record/page` | 发送记录 |
| GET | `/notification/record/stats` | 发送统计 |
| POST | `/notification/device/register` | 推送设备注册 |
| GET | `/notification/biz-types` | 业务类型列表 |
| GET | `/notification/health` | 健康检查 |
| POST | `/ux/notification/order` | UX 订单通知 |
| GET | `/ux/voice/new-order` | 语音播报 |

### 21. order（订单）
- **主要 Controller**: `OrderController` (`/order`)、`OrderDetailController` (`/order-detail`)、`PlatformOrderPullController` (`/orders/platform`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/order/submit` | 提交订单 |
| POST | `/order/eatIn` | 堂食订单 |
| POST | `/order/again` | 再来一单 |
| PUT | `/order/confirm` | 确认订单 |
| PUT | `/order/reject` | 拒单 |
| PUT | `/order/complete` | 完成 |
| PUT | `/order/cancel` | 取消 |
| GET | `/order/page` | 订单分页 |
| GET | `/order/platform/page` | 平台订单分页 |
| GET | `/order/pendingCheckout` | 待结算 |
| GET | `/order/statistics` | 订单统计 |
| GET | `/orders/platform/pull` | 平台拉单 |
| POST | `/orders/platform/pushStatus` | 状态推送 |

### 22. payment（支付）
- **主要 Controller**: `PaymentController` (`/api/payment`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/payment/pay` | 发起支付 |
| POST | `/api/payment/notify/{channel}` | 渠道回调 |
| POST | `/api/payment/refund` | 退款 |
| GET | `/api/payment/query/{tradeNo}` | 按交易号查 |
| GET | `/api/payment/page` | 支付记录分页 |

### 23. platform（第三方平台对接）
- **主要 Controller**: `PlatformConfigController` (`/admin/platform/config`)、`DishPlatformMappingController` (`/admin/platform/mapping`)、`PlatformSyncController` (`/orders/platform`)、`PlatformReconcileController` (`/api/platform/reconcile`)、`PlatformSyncLogController` (`/api/platform/sync-log`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/platform/config/add` | 新增平台配置 |
| POST | `/admin/platform/config/update` | 更新 |
| POST | `/admin/platform/config/toggle` | 启停 |
| GET | `/admin/platform/mapping/page` | 菜品-平台映射分页 |
| POST | `/orders/platform/syncDish` | 同步菜品 |
| POST | `/orders/platform/syncStock` | 同步库存 |
| POST | `/orders/platform/syncBusiness` | 同步营业 |
| POST | `/api/platform/reconcile/execute` | 执行对账 |
| GET | `/api/platform/reconcile/query` | 查询结果 |
| GET | `/api/platform/sync-log/page` | 同步日志 |
| GET | `/api/platform/sync-log/failure-count` | 失败数 |
| GET | `/api/platform/sync-log/abnormal` | 异常日志 |

### 24. printer（打印）
- **主要 Controller**: `PrinterController` (`/printer`)、`PrinterConfigController` (`/printer/config`)、`PrinterLogController` (`/printer/log`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/printer/print/{orderId}` | 打印订单小票 |
| POST | `/printer/test/{id}` | 测试打印 |
| GET | `/printer/status/{id}` | 打印机状态 |
| GET | `/printer/system/list` | 系统打印机 |
| GET | `/printer/config/page` | 打印模板分页 |
| GET | `/printer/config/options` | 模板选项 |
| GET | `/printer/log/page` | 打印日志 |

### 25. recommend（推荐）
- **主要 Controller**: `RecommendController` (`/recommend`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/recommend/dishes` | 菜品推荐 |
| GET | `/recommend/hot` | 热门排行 |
| GET | `/recommend/new-arrivals` | 新品 |
| GET | `/recommend/setmeals` | 套餐推荐 |
| POST | `/recommend/browse` | 记录浏览 |
| GET | `/recommend/browse-history` | 浏览历史 |
| POST | `/recommend/analyze-preference` | 触发偏好分析 |
| GET | `/recommend/preference/distribution` | 偏好分布 |
| GET | `/recommend/messages/unread` | 未读营销消息 |
| PUT | `/recommend/messages/{id}/read` | 标记已读 |
| GET | `/recommend/algo/compare` | 算法效果对比 |

### 26. region（区域）
- **主要 Controller**: `RegionController` (`/region`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/region/tree` | 区域树 |
| GET | `/region/children` | 子级区域 |
| GET | `/region/by-level` | 按层级 |
| GET | `/region/page` | 区域分页 |
| POST | `/region` | 新增区域 |
| DELETE | `/region/{id}` | 删除 |

### 27. report（报表）
- **主要 Controller**: `ReportController` (`/api/report`)、`ReportEnhancedController` (`/report/enhanced`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/report/daily` | 日报 |
| GET | `/api/report/dish-ranking` | 菜品排行 |
| GET | `/api/report/time-slot/heatmap` | 时段热力图 |
| GET | `/api/report/repurchase-rate` | 复购率 |
| GET | `/api/report/repurchase-rate/dish` | 菜品复购 |
| GET | `/api/report/cohort` | 留存队列 |
| GET | `/api/report/export` | 报表导出 |
| GET | `/api/report/export/history` | 导出历史 |
| GET | `/report/enhanced/sales/trend` | 销售趋势 |
| GET | `/report/enhanced/sales/revenue-forecast` | 营收预测 |
| GET | `/report/enhanced/food-cost/report` | 食材成本报表 |

### 28. retention（留存）
- **主要 Controller**: `RetentionController` (`/api/retention`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/retention/overview` | 留存总览 |
| GET | `/api/retention/list` | 留存列表 |
| GET | `/api/retention/ranking` | 排行 |
| GET | `/api/retention/warning` | 流失预警 |
| GET | `/api/retention/recommend` | 挽回推荐策略 |
| POST | `/api/retention/send` | 触达发送 |
| POST | `/api/retention/send-batch` | 批量发送 |

### 29. schedule（排班）
- **主要 Controller**: `ScheduleController` (`/api/schedule`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/schedule/monthly` | 月度排班 |
| GET | `/api/schedule/today` | 今日排班 |
| POST | `/api/schedule/save` | 保存排班 |
| GET | `/api/schedule/employee/{employeeId}` | 员工排班 |

### 30. setmeal（套餐）
- **主要 Controller**: `SetmealController` (`/setmeal`)、`SetmealDishController` (`/setmeal-dish`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/setmeal` | 新增套餐（含关联菜品） |
| PUT | `/setmeal` | 修改套餐 |
| POST | `/setmeal/status/{status}` | 批量起售/停售 |
| GET | `/setmeal/dish/{id}` | 套餐详情含菜品 |
| POST | `/setmeal-dish/batch` | 套餐菜品批量关联 |
| GET | `/setmeal/options` | 套餐下拉选项 |
| GET | `/setmeal/stats` | 套餐统计 |

### 31. shopping（购物车）
- **主要 Controller**: `ShoppingCartController` (`/shopping-cart`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/shopping-cart/add` | 加入购物车 |
| GET | `/shopping-cart/list` | 购物车列表 |
| POST | `/shopping-cart/sub` | 减少数量 |
| DELETE | `/shopping-cart/clean` | 清空购物车 |

### 32. store（门店）
- **主要 Controller**: `StoreController` (`/store`)、`StoreDashboardController` (`/store/dashboard`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/store/create` | 创建门店 |
| POST | `/store/page` | 分页查询 |
| GET | `/store/detail/{tenantId}` | 门店详情 |
| GET | `/store/branches` | 分支门店 |
| POST | `/store/switch/{tenantId}` | 切换门店 |
| PUT | `/store/{tenantId}/status` | 门店状态 |
| PUT | `/store/batch/status` | 批量状态 |
| POST | `/store/sync/dishes` | 同步菜品 |
| POST | `/store/sync/categories` | 同步分类 |
| POST | `/store/sync/setmeals` | 同步套餐 |
| GET | `/store/dashboard/overview` | 门店看板 |
| GET | `/store/dashboard/real-time` | 实时数据 |
| GET | `/store/dashboard/ranking` | 排行 |

### 33. sys（系统管理）
- **主要 Controller**: `RoleController` (`/sys/role`)、`SystemConfigController` (`/sys/config`)、`SysOperationLogController` (`/sys/log`)、`SysNotificationTemplateController` (`/sys/template`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/sys/role/page` | 角色分页 |
| GET | `/sys/role/{id}/permissions` | 角色权限 |
| PUT | `/sys/role/{id}/permissions` | 设置权限 |
| GET | `/sys/role/permissions/tree` | 权限树 |
| GET | `/sys/config/page` | 系统配置分页 |
| GET | `/sys/config/{configKey}` | 按 key 查配置 |
| PUT | `/sys/config/{id}` | 修改配置 |
| PUT | `/sys/config/batch` | 批量修改 |
| GET | `/sys/log/page` | 操作日志 |
| GET | `/sys/log/stats` | 日志统计 |

### 34. tenant（租户）
- **主要 Controller**: `TenantController` (`/tenant`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/tenant/register` | 租户注册 |

### 35. urgency（紧急干预）
- **主要 Controller**: `UrgencyController` (`/api/urgency`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/urgency/overview` | 紧急概览 |
| GET | `/api/urgency/list` | 紧急列表 |
| GET | `/api/urgency/queue` | 紧急队列 |
| POST | `/api/urgency/call/{orderId}` | 催单/紧急呼叫 |
| GET | `/api/urgency/detail/{orderId}` | 订单紧急详情 |
| GET | `/api/urgency/summary` | 汇总统计 |

### 36. user（C 端用户）
- **主要 Controller**: `UserController` (`/user`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/user/login` | 用户登录 |
| POST | `/user/loginout` | 登出 |
| GET | `/user/info` | 用户信息 |
| GET | `/user/page` | 用户分页（后台） |
| PUT | `/user/status` | 用户状态 |
| POST | `/user/sendMsg` | 发送消息 |

---

## 前缀约定观察（技术债）

1. **前缀风格不统一**: `/backend/`（早期）、`/front/`、`/api/`、`/admin/`、以及无前缀（如 `/order`、`/dish`、`/employee`）共存。建议核实 `LoginCheckFilter` 是否覆盖所有无前缀路径。
2. **重复类级前缀**:
   - `/marketing` 同时被 `MarketingController` 与 `MarketingCampaignController` 使用（当前端点不重叠：`/full-reduction/*` vs `/campaigns/*`）
   - `/orders/platform` 同时被 `PlatformOrderPullController` 与 `PlatformSyncController` 使用
3. **C 端与后台混布**: `UserController` 同时承载 C 端登录（`/user/login`）与后台管理（`/user/page`），权限隔离依赖路径判断。
4. **文档语言不一致**: `ReportEnhancedController` 的 `@Operation(summary=...)` 全为英文，其余 Controller 均为中文。
