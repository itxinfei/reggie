# 瑞吉外卖商业功能补充设计

**日期：** 2026-07-01
**版本：** v1.0
**状态：** 设计稿

---

## 1. 概述

瑞吉外卖现有项目实现了外卖小程序的基础 CRUD 功能（菜品浏览、购物车、下单），但与市场主流餐饮管理系统（客如云、美团收银、哗啦啦等）相比，缺少硬件对接、支付、堂食、供应链、会员营销等核心商业功能。

本文档设计 7 个补充模块，覆盖餐饮门店完整业务闭环。

### 1.1 设计原则

- **模块化插入** — 每个模块独立 package，不破坏现有代码结构
- **接口抽象** — 硬件、支付渠道、外卖平台均通过适配器模式解耦
- **渐进式实现** — 模块间无强依赖，可按优先级独立开发
- **多租户兼容** — 所有新表延续 tenant_id 行级隔离

---

## 2. 整体架构

```
src/main/java/com/reggie/
├── (已有) common/ controller/ entity/ service/ filter/ ...
│
├── module/
│   ├── printer/          # 打印小票
│   ├── payment/          # 聚合支付
│   ├── delivery/         # 外卖平台对接
│   ├── dining/           # 堂食管理
│   ├── inventory/        # 进销存
│   ├── member/           # 会员营销
│   └── report/           # 经营报表
```

每个模块内部遵循统一分层：`controller/` → `service/` → `repository/` → `model/`。

---

## 3. 模块一：打印小票 (Printer)

### 3.1 业务场景

| 场景 | 触发 | 内容 | 打印机 |
|------|------|------|--------|
| 前台结账 | 支付成功 | 结账小票（顾客联+商户联） | 80mm 前台 |
| 后厨出品 | 订单提交 | 菜品+桌号+备注（按档口拆分） | 58mm 后厨 |
| 外卖接单 | 外卖订单接入 | 外卖单+配送信息 | 58mm 外卖 |
| 催菜 | 服务员操作 | 催菜提醒 | 后厨 |
| 预结单 | 顾客要求 | 消费明细 | 前台 |

### 3.2 架构

```
printer/
├── PrinterService.java              # 打印服务接口
├── PrinterServiceImpl.java          # 实现
├── PrinterDeviceManager.java        # 设备管理器
├── PrinterTemplate.java             # 模板引擎
├── model/
│   ├── PrintJob.java                # 打印任务
│   ├── PrintLine.java               # 打印行（文本/表格/分割线/二维码）
│   ├── PrinterConfig.java           # 配置
│   └── PrinterStatus.java           # 状态
├── adapter/
│   ├── PrinterAdapter.java          # 适配器接口
│   ├── GprinterAdapter.java         # 佳博
│   ├── XprinterAdapter.java         # 芯烨
│   └── SunmiAdapter.java            # 商米
├── controller/
│   └── PrinterController.java
└── repository/
    └── PrinterConfigRepository.java
```

### 3.3 数据库

```sql
CREATE TABLE printer_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    store_id BIGINT,
    name VARCHAR(50),
    type VARCHAR(20),             -- USB/TCP/CLOUD
    brand VARCHAR(20),
    device_id VARCHAR(100),
    ip_address VARCHAR(15),
    port INT,
    paper_size VARCHAR(10),       -- 58mm/80mm
    print_type VARCHAR(20),       -- BILL/KITCHEN/DELIVERY
    status INT DEFAULT 1,
    created_time DATETIME,
    updated_time DATETIME
);

CREATE TABLE printer_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT,
    print_type VARCHAR(20),
    printer_id BIGINT,
    content TEXT,
    status INT,
    error_msg VARCHAR(255),
    created_time DATETIME
);
```

### 3.4 API

```
POST   /printer/config          — 新增配置
PUT    /printer/config/{id}     — 修改
DELETE /printer/config/{id}     — 删除
GET    /printer/config          — 列表
POST   /printer/test            — 测试打印
POST   /printer/print/{orderId} — 手动打印
GET    /printer/status/{id}     — 状态查询
```

---

## 4. 模块二：聚合支付 (Payment)

### 4.1 业务场景

扫码支付（主扫/被扫）、小程序支付、聚合码、退款、对账。

### 4.2 架构

```
payment/
├── PaymentService.java
├── PaymentServiceImpl.java
├── RefundService.java
├── SettlementService.java          # 结算对账
├── model/
│   ├── PayRequest.java / PayResponse.java
│   ├── RefundRequest.java / RefundResponse.java
│   ├── PayChannel.java             # 枚举
│   └── TradeStatus.java            # 枚举
├── channel/
│   ├── PaymentChannel.java         # 支付通道接口
│   ├── AlipayChannel.java
│   ├── WechatPayChannel.java
│   └── UnionPayChannel.java
├── controller/
│   └── PaymentController.java
└── repository/
    └── PaymentOrderRepository.java
```

### 4.3 数据库

```sql
CREATE TABLE payment_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    tenant_id BIGINT,
    trade_no VARCHAR(64),
    channel_trade_no VARCHAR(128),
    channel VARCHAR(20),
    amount DECIMAL(10,2),
    status VARCHAR(20),              -- PENDING/SUCCESS/FAIL/REFUND
    paid_time DATETIME,
    notify_time DATETIME,
    created_time DATETIME,
    updated_time DATETIME
);

CREATE TABLE refund_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_order_id BIGINT,
    refund_no VARCHAR(64),
    amount DECIMAL(10,2),
    reason VARCHAR(255),
    status VARCHAR(20),
    created_time DATETIME
);
```

### 4.4 API

```
POST   /payment/create           — 发起支付
POST   /payment/notify/{channel} — 异步回调
POST   /payment/refund           — 退款
GET    /payment/query/{orderId}  — 查询
GET    /payment/settlement       — 对账
```

---

## 5. 模块三：外卖平台对接 (Delivery)

### 5.1 业务场景

对接美团外卖、饿了么、抖音团购，实现自动接单、菜品同步、状态回传、库存同步、退款处理、对账。

### 5.2 架构

```
delivery/
├── DeliveryService.java
├── DeliveryServiceImpl.java
├── DeliveryOrderSync.java           # 订单自动同步
├── DeliveryMenuSync.java            # 菜品同步
├── model/
│   ├── DeliveryOrder.java
│   ├── DeliveryShop.java
│   ├── DeliveryStatus.java
│   └── PlatformEnum.java
├── platform/
│   ├── DeliveryPlatform.java        # 平台适配器接口
│   ├── MeituanAdapter.java
│   ├── ElemeAdapter.java
│   └── DouyinAdapter.java
├── controller/
│   └── DeliveryController.java
└── callback/
    └── DeliveryCallbackController.java
```

### 5.3 核心能力

| 功能 | 说明 |
|------|------|
| 自动接单 | 平台新订单自动接入并打印 |
| 菜品同步 | 系统菜品上架到平台 |
| 状态同步 | 接单/制作中/出餐/配送 |
| 库存同步 | 售罄自动同步到平台 |
| 退款处理 | 平台退款申请处理 |
| 对账 | 平台结算单核对 |

### 5.4 API

```
POST   /delivery/sync/menu      — 同步菜品到平台
POST   /delivery/status/update  — 更新订单状态
GET    /delivery/orders         — 平台订单列表
GET    /delivery/settlement     — 结算对账
```

---

## 6. 模块四：堂食管理 (Dining)

### 6.1 业务场景

排队取号、桌台管理（开台/并台/换台）、扫码点餐、服务员手持点餐、预订、催菜加菜。

### 6.2 架构

```
dining/
├── QueueService.java
├── TableService.java
├── ScanOrderService.java
├── ReservationService.java
├── model/
│   ├── Table.java / TableArea.java
│   ├── QueueRecord.java
│   ├── Reservation.java
│   └── TableStatus.java
├── controller/
│   ├── TableController.java
│   ├── QueueController.java
│   └── ReservationController.java
└── repository/
```

### 6.3 数据库

```sql
CREATE TABLE dining_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT,
    area_id BIGINT,
    name VARCHAR(20),
    seat_count INT,
    status VARCHAR(20),           -- FREE/OCCUPIED/RESERVED
    min_amount DECIMAL(10,2),
    qr_code_url VARCHAR(255),
    sort INT,
    created_time DATETIME
);

CREATE TABLE dining_area (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT,
    name VARCHAR(50),
    sort INT
);

CREATE TABLE dining_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT,
    queue_no VARCHAR(10),
    phone VARCHAR(20),
    seat_count INT,
    status VARCHAR(20),
    created_time DATETIME
);

CREATE TABLE dining_reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT,
    table_id BIGINT,
    customer_name VARCHAR(50),
    phone VARCHAR(20),
    reserved_time DATETIME,
    seat_count INT,
    status VARCHAR(20),
    remark VARCHAR(255),
    created_time DATETIME
);
```

### 6.4 扫码点餐流程

```
顾客入座扫桌码 → 进入点餐 → 加购 → 提交
    → 订单关联桌台 → 后厨打印 → 出餐上菜
    → 加菜/催菜 → 结账 → 桌台状态恢复
```

---

## 7. 模块五：进销存/供应链 (Inventory)

### 7.1 业务场景

食材管理、入库/出库、库存盘点、库存预警（低库存/临期）、供应商管理、采购单。

### 7.2 架构

```
inventory/
├── InventoryService.java
├── StockService.java
├── PurchaseService.java
├── SupplierService.java
├── WarningService.java
├── model/
│   ├── Material.java / MaterialCategory.java
│   ├── StockRecord.java / StockCheck.java
│   ├── PurchaseOrder.java
│   └── Supplier.java
├── controller/
│   ├── MaterialController.java
│   ├── StockController.java
│   ├── PurchaseController.java
│   └── SupplierController.java
└── repository/
```

### 7.3 数据库核心表

```sql
CREATE TABLE material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT,
    category_id BIGINT,
    name VARCHAR(100),
    unit VARCHAR(10),
    stock_qty DECIMAL(10,2),
    min_stock DECIMAL(10,2),
    unit_price DECIMAL(10,2),
    supplier_id BIGINT,
    barcode VARCHAR(50),
    status INT DEFAULT 1,
    created_time DATETIME
);

CREATE TABLE stock_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT,
    material_id BIGINT,
    type VARCHAR(10),            -- IN/OUT/CHECK
    qty DECIMAL(10,2),
    unit_price DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    biz_id BIGINT,
    remark VARCHAR(255),
    operator VARCHAR(50),
    created_time DATETIME
);
```

### 7.4 闭环流程

```
采购入库 → 库存增加 → 菜品销售自动扣减
    → 低于预警 → 生成采购建议 → 发起采购
    → 定期盘点 → 盈亏调整
```

---

## 8. 模块六：会员营销 (Member)

### 8.1 业务场景

会员注册、等级体系（普通→钻石）、积分系统、储值、优惠券（满减/折扣/新客/生日）、营销活动（返券/拼团）。

### 8.2 架构

```
member/
├── MemberService.java
├── MemberLevelService.java
├── PointsService.java
├── RechargeService.java
├── CouponService.java
├── MarketingService.java
├── model/
│   ├── Member.java / MemberLevel.java
│   ├── PointsRecord.java / RechargeRecord.java
│   ├── Coupon.java / CouponTemplate.java
│   └── MarketingActivity.java
├── controller/
│   ├── MemberController.java
│   ├── CouponController.java
│   └── MarketingController.java
└── strategy/
    ├── PointsRule.java
    └── DiscountRule.java
```

### 8.3 数据库核心表

```sql
CREATE TABLE member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT,
    user_id BIGINT,
    level_id BIGINT,
    name VARCHAR(50),
    phone VARCHAR(20),
    points BIGINT DEFAULT 0,
    balance DECIMAL(10,2) DEFAULT 0,
    total_consumption DECIMAL(10,2),
    created_time DATETIME
);

CREATE TABLE coupon_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT,
    name VARCHAR(100),
    type VARCHAR(20),             -- FULL_REDUCE/DISCOUNT/NEW_USER
    condition_amount DECIMAL(10,2),
    discount_amount DECIMAL(10,2),
    total_count INT,
    remain_count INT,
    valid_days INT,
    status INT DEFAULT 1,
    created_time DATETIME
);
```

---

## 9. 模块七：经营报表 (Report)

### 9.1 业务场景

| 报表 | 内容 |
|------|------|
| 营业日报 | 营业额、订单数、客单价、翻台率 |
| 菜品排行 | 销量/营收 Top N |
| 时段分析 | 各时段分布、高峰识别 |
| 支付分析 | 各渠道占比 |
| 会员分析 | 新增/活跃/消费占比 |
| 利润分析 | 收入-成本=毛利 |

### 9.2 架构

```
report/
├── ReportService.java
├── DailyReportService.java
├── DishReportService.java
├── MemberReportService.java
├── FinanceReportService.java
├── controller/
│   └── ReportController.java
└── exporter/
    ├── ExcelExporter.java
    └── PdfExporter.java
```

### 9.3 实现方案

- **实时查询** — MySQL 聚合查询
- **定时日报** — `@Scheduled` 每日凌晨汇总
- **缓存** — Redis 缓存热点报表
- **导出** — Apache POI 导出 Excel

---

## 10. 模块依赖关系

```
printer ──→ payment ──→ delivery
   │                     │
   └─────────┬───────────┘
             ↓
          dining
             │
    ┌────────┼────────┐
    ↓        ↓        ↓
 inventory  member  report
```

- **Phase 1**（无外部依赖）：printer, member
- **Phase 2**（依赖 printer）：payment, delivery, dining
- **Phase 3**（依赖前序）：inventory, report

---

## 11. 技术选型

| 模块 | 关键依赖 | 说明 |
|------|---------|------|
| printer | javax.print / Apache Commons Net | USB/串口/TCP 打印 |
| payment | 支付宝 SDK / 微信支付 SDK | 官方 SDK 对接 |
| delivery | HttpClient / OkHttp | REST API 对接平台 |
| dining | 同现有（Spring MVC） | 无新增依赖 |
| inventory | MyBatis Plus（已有） | 无新增依赖 |
| member | MyBatis Plus（已有） | 无新增依赖 |
| report | Apache POI / ECharts | Excel 导出 / 前端图表 |

---

## 12. 向后兼容

- 所有新表使用独立的表前缀或 module_ 命名空间
- 不修改已有 entity/controller/service 的任何代码
- 新模块通过 application.yml 开关控制是否启用
- 前端页面可通过独立路由访问
