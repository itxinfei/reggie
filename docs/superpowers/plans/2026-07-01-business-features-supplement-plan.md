# 瑞吉外卖商业功能补充 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为瑞吉外卖补充打印小票、聚合支付、外卖平台对接、堂食管理、进销存、会员营销、经营报表 7 个商业功能模块。

**Architecture:** 每个模块独立 package（`module/*`），通过适配器模式解耦硬件/支付渠道/外卖平台，不破坏现有代码。

**Tech Stack:** Java 1.8 + Spring Boot 2.4.5 + MyBatis Plus 3.4.2 + MySQL 8.0 / H2

**数据库迁移脚本:** `docs/migrations/2026-07-01-module-*.sql`

## Global Constraints

- Java 1.8 语法，无 lambda 外的 Java 8+ 特性
- Spring Boot 2.4.5
- MyBatis Plus 3.4.2
- 所有新表包含 `tenant_id` 支持多租户
- 新模块独立 package `com.reggie.module.*`
- 不修改现有 entity/controller/service 代码
- 测试使用 H2 内存数据库
- 代码无 TODO/FIXME 注释
- 所有 API 返回统一 R 格式

---

## Phase 1: 打印小票模块

### Task 1: Printer 配置管理

**Files:**
- Create: `src/main/java/com/reggie/module/printer/model/PrinterConfig.java`
- Create: `src/main/java/com/reggie/module/printer/model/PrinterStatus.java`
- Create: `src/main/java/com/reggie/module/printer/mapper/PrinterConfigMapper.java`
- Create: `src/main/java/com/reggie/module/printer/service/PrinterConfigService.java`
- Create: `src/main/java/com/reggie/module/printer/service/impl/PrinterConfigServiceImpl.java`
- Create: `src/main/java/com/reggie/module/printer/controller/PrinterConfigController.java`
- Test: `src/test/java/com/reggie/module/printer/PrinterConfigControllerTest.java`

**Interfaces:**
- Produces: `PrinterConfig` entity, CRUD API `POST/GET/PUT/DELETE /printer/config`

- [ ] **Step 1: 创建实体类 PrinterConfig**

```java
package com.reggie.module.printer.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PrinterConfig {
    private Long id;
    private Long tenantId;
    private Long storeId;
    private String name;
    private String type;          // USB/TCP/CLOUD/BLUETOOTH
    private String brand;
    private String deviceId;
    private String ipAddress;
    private Integer port;
    private String paperSize;     // 58mm/80mm
    private String printType;     // BILL/KITCHEN/DELIVERY
    private Integer status;
    private Integer sort;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
```

- [ ] **Step 2: 创建 Mapper**

```java
package com.reggie.module.printer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.printer.model.PrinterConfig;

public interface PrinterConfigMapper extends BaseMapper<PrinterConfig> {
}
```

- [ ] **Step 3: 创建 Service 接口 + 实现**

```java
public interface PrinterConfigService extends IService<PrinterConfig> {}
public class PrinterConfigServiceImpl extends ServiceImpl<PrinterConfigMapper, PrinterConfig> implements PrinterConfigService {}
```

- [ ] **Step 4: 创建 Controller**

```java
@RestController
@RequestMapping("/printer/config")
public class PrinterConfigController {
    @GetMapping    public R<Page> page(int page, int pageSize);
    @PostMapping   public R<String> save(@RequestBody PrinterConfig config);
    @PutMapping    public R<String> update(@RequestBody PrinterConfig config);
    @DeleteMapping public R<String> delete(Long id);
    @GetMapping("/{id}") public R<PrinterConfig> get(@PathVariable Long id);
}
```

- [ ] **Step 5: 编写测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
public class PrinterConfigControllerTest {
    @Test public void testAddConfig();
    @Test public void testListConfigs();
    @Test public void testDeleteConfig();
}
```

- [ ] **Step 6: 验证测试全部通过**

Run: `mvn test -Dtest=PrinterConfigControllerTest -DfailIfNoTests=false`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/reggie/module/printer/model/PrinterConfig.java \
        src/main/java/com/reggie/module/printer/mapper/PrinterConfigMapper.java \
        src/main/java/com/reggie/module/printer/service/PrinterConfigService.java \
        src/main/java/com/reggie/module/printer/service/impl/PrinterConfigServiceImpl.java \
        src/main/java/com/reggie/module/printer/controller/PrinterConfigController.java \
        src/test/java/com/reggie/module/printer/PrinterConfigControllerTest.java \
        docs/migrations/2026-07-01-module-printer.sql
git commit -m "feat(printer): add printer config CRUD"
```

---

### Task 2: 打印模板引擎 + 打印服务实现

**Files:**
- Create: `src/main/java/com/reggie/module/printer/model/PrintJob.java`
- Create: `src/main/java/com/reggie/module/printer/model/PrintLine.java`
- Create: `src/main/java/com/reggie/module/printer/core/PrinterTemplate.java`
- Create: `src/main/java/com/reggie/module/printer/core/PrinterDeviceManager.java`
- Create: `src/main/java/com/reggie/module/printer/adapter/PrinterAdapter.java`
- Create: `src/main/java/com/reggie/module/printer/adapter/GprinterAdapter.java`
- Create: `src/main/java/com/reggie/module/printer/adapter/XprinterAdapter.java`
- Modify: (Task 1 PrinterConfigService 追加)
- Test: `src/test/java/com/reggie/module/printer/PrinterServiceTest.java`

**Interfaces:**
- Consumes: `PrinterConfig`, `Order`
- Produces: `PrinterService.printOrder(orderId)` → PrintJob

- [ ] **Step 1: 定义 PrintLine / PrintJob 模型**

```java
public class PrintLine {
    private String text;
    private int fontSize;      // 0=正常 1=倍宽 2=倍高 3=倍宽倍高
    private boolean bold;
    private Align align;       // LEFT/CENTER/RIGHT
    private LineType type;     // TEXT/DIVIDER/QR/BARCODE/TABLE
}

public class PrintJob {
    private Long orderId;
    private String printType;    // BILL/KITCHEN/DELIVERY
    private List<PrintLine> lines;
}
```

- [ ] **Step 2: 实现 PrinterTemplate（按订单组装打印内容）**

- BILL 类型: 店名+订单号+日期+菜品明细+金额+二维码
- KITCHEN 类型: 桌号+菜品+数量+备注，按档口拆分
- DELIVERY 类型: 平台+订单号+菜品+配送地址+预计时间

- [ ] **Step 3: 实现 PrinterAdapter 接口和 GprinterAdapter**

```java
public interface PrinterAdapter {
    boolean print(PrintJob job, PrinterConfig config);
    PrinterStatus queryStatus(PrinterConfig config);
    boolean testConnection(PrinterConfig config);
}
```

- TCP/IP 模式: Socket 连接发送 ESC/POS 指令
- USB 模式: javax.print 服务

- [ ] **Step 4: 实现 PrinterService.printOrder()**

```java
public void printOrder(Long orderId, String printType) {
    // 1. 查询订单
    // 2. 查找对应 printType 的已启用打印机
    // 3. PrinterTemplate 组装 PrintJob
    // 4. 匹配 Adapter → 发送
    // 5. 记录 PrinterLog
}
```

- [ ] **Step 5: PrinterController 追加打印接口**

```
POST /printer/print/{orderId}?type=KITCHEN
POST /printer/test
GET  /printer/status/{id}
```

- [ ] **Step 6: 编写测试**

```java
@Test public void testPrintTemplateBill();
@Test public void testPrintTemplateKitchen();
```

- [ ] **Step 7: 提交**

```bash
git commit -m "feat(printer): add print template and print service"
```

---

## Phase 2: 聚合支付模块

### Task 3: 支付订单管理

**Files:**
- Create: `src/main/java/com/reggie/module/payment/model/PayChannel.java`
- Create: `src/main/java/com/reggie/module/payment/model/TradeStatus.java`
- Create: `src/main/java/com/reggie/module/payment/model/PaymentOrder.java`
- Create: `src/main/java/com/reggie/module/payment/model/RefundRecord.java`
- Create: `src/main/java/com/reggie/module/payment/mapper/PaymentOrderMapper.java`
- Create: `src/main/java/com/reggie/module/payment/mapper/RefundRecordMapper.java`
- Create: `src/main/java/com/reggie/module/payment/service/PaymentOrderService.java`
- Create: `src/main/java/com/reggie/module/payment/service/impl/PaymentOrderServiceImpl.java`
- Create: `src/main/java/com/reggie/module/payment/channel/PaymentChannel.java`
- Create: `src/main/java/com/reggie/module/payment/channel/AlipayChannel.java`
- Create: `src/main/java/com/reggie/module/payment/channel/WechatPayChannel.java`
- Create: `src/main/java/com/reggie/module/payment/controller/PaymentController.java`
- Test: `src/test/java/com/reggie/module/payment/PaymentControllerTest.java`
- DB: `docs/migrations/2026-07-01-module-payment.sql`

- [ ] **Step 1: 创建所有模型类和枚举**
- [ ] **Step 2: 创建 Mapper 和 Service**
- [ ] **Step 3: 实现支付通道接口和支付宝/微信通道**

```java
public interface PaymentChannel {
    PayResponse createOrder(PayRequest request);
    PayResponse queryOrder(String tradeNo);
    RefundResponse refund(RefundRequest request);
    PayResponse handleNotify(Map<String, String> params);
}
```

- [ ] **Step 4: Controller CRUD + 支付/退款/回调 API**
- [ ] **Step 5: 测试**
- [ ] **Step 6: 提交**

```bash
git commit -m "feat(payment): add payment module with Alipay/WeChat channels"
```

---

### Task 4: 堂食管理模块

**Files:**
- Create: module/dining/ 下：Table/TableArea/QueueRecord/Reservation 实体 + Mapper + Service + Controller
- DB: `docs/migrations/2026-07-01-module-dining.sql`

**需要创建的类（~15个文件）：**
```
dining/model/Table.java
dining/model/TableArea.java
dining/model/QueueRecord.java
dining/model/Reservation.java
dining/model/TableStatus.java
dining/mapper/TableMapper.java
dining/mapper/TableAreaMapper.java
dining/mapper/QueueMapper.java
dining/mapper/ReservationMapper.java
dining/service/TableService.java
dining/service/QueueService.java
dining/service/ReservationService.java
dining/service/impl/TableServiceImpl.java
dining/service/impl/QueueServiceImpl.java
dining/service/impl/ReservationServiceImpl.java
dining/controller/TableController.java
dining/controller/QueueController.java
dining/controller/ReservationController.java
```

- [ ] **Step 1: 创建所有实体和枚举**
- [ ] **Step 2: Mapper + Service**
- [ ] **Step 3: Controller API**

```
/api/dining/table        CRUD
/api/dining/area         CRUD
/api/dining/queue        取号/叫号/取消
/api/dining/reservation  预订/确认/取消
/api/dining/scan/{tableId}  生成桌码点餐链接
```

- [ ] **Step 4: 扫码点餐流程（复用现有 OrderController 扩展）**

扩展 `OrderController.submit()` 增加 `tableId` 参数，提交后自动调用 PrinterService 打印后厨单。

- [ ] **Step 5: 测试 + 提交**

---

### Task 5: 外卖平台对接

**Files:**
- Create: module/delivery/ 下
- No new DB tables (复用现有 Orders)

**核心类：**
```
delivery/model/PlatformEnum.java
delivery/model/DeliveryStatus.java
delivery/platform/DeliveryPlatform.java
delivery/platform/MeituanAdapter.java
delivery/platform/ElemeAdapter.java
delivery/service/DeliveryService.java
delivery/service/impl/DeliveryServiceImpl.java
delivery/controller/DeliveryController.java
delivery/callback/DeliveryCallbackController.java
```

- [ ] **Step 1: 定义平台适配器接口**

```java
public interface DeliveryPlatform {
    boolean acceptOrder(String orderData);        // 自动接单
    boolean syncMenu(List<Dish> dishes);          // 同步菜品
    boolean updateStatus(String orderId, String status);
    boolean syncStock(Map<Long, Integer> stock);   // 同步库存
}
```

- [ ] **Step 2: 实现美团/饿了么适配器（占位实现，真实对接需 API Key）**
- [ ] **Step 3: 回调处理（平台推单到本系统）**
- [ ] **Step 4: 提交**

---

## Phase 3: 供应链、会员、报表

### Task 6: 进销存/供应链模块

**Files:**
- Create: module/inventory/ 下全部（~25 个文件）
- DB: `docs/migrations/2026-07-01-module-inventory.sql`

**包含：** Material, MaterialCategory, Supplier, StockRecord, StockCheck, PurchaseOrder, PurchaseOrderDetail

- [ ] **Step 1: 实体 + Mapper + Service（CRUD）**
- [ ] **Step 2: 核心业务逻辑**

```java
// 菜品销售自动扣减库存
public void deductStock(Map<Dish, Integer> dishes) {
    // 根据菜品配方扣减对应食材
}

// 库存预警
public List<Material> checkWarning() {
    // 查询 stock_qty < min_stock 的食材
}
```

- [ ] **Step 3: Controller API**
- [ ] **Step 4: 测试 + 提交**

### Task 7: 会员营销模块

**Files:**
- Create: module/member/ 下全部（~20 个文件）
- DB: `docs/migrations/2026-07-01-module-member.sql`

- [ ] **Step 1: 会员 CRUD + 等级体系**
- [ ] **Step 2: 积分系统（消费积分/签到积分/积分抵现）**
- [ ] **Step 3: 储值功能（充值送/余额消费）**
- [ ] **Step 4: 优惠券（模板+发放+核销+过期）**
- [ ] **Step 5: 测试 + 提交**

### Task 8: 经营报表模块

- [ ] **Step 1: 营业日报（每日定时汇总）**
- [ ] **Step 2: 菜品排行、时段分析**
- [ ] **Step 3: 会员分析、支付分析**
- [ ] **Step 4: Excel 导出**
- [ ] **Step 5: 测试 + 提交**

---

## 执行顺序总结

```
Phase 1 (独立模块):
  Task 1 → Task 2  (printer)
  Task 7            (member - 独立, 可并行)

Phase 2 (依赖 printer):
  Task 3  (payment)
  Task 4  (dining)
  Task 5  (delivery)

Phase 3 (依赖前序):
  Task 6  (inventory)
  Task 8  (report)
```
