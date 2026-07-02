# 管理后台前端页面实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 7 个业务模块开发 23 个管理后台前端页面，追加 API 封装层和菜单入口。

**Architecture:** 延续现有 Vue.js 2 + ElementUI CDN + iframe 模式，每个页面独立 HTML 文件，通过 `el-table` + `el-dialog` 弹框实现 CRUD，报表页面引入 ECharts CDN 渲染图表。

**Tech Stack:** Vue.js 2 (CDN) + ElementUI (CDN) + axios + ECharts 5 (CDN, 仅报表)

## 全局约束

- 完全复用现有 `request.js`（`$axios` 实例）、`common.css`、`page.css`
- API 封装在 `api/*.js` 中，使用 `$axios({url, method, params/data})` 模式
- 每个页面独立 HTML 文件，通过 iframe 在 index.html 中加载
- 列表页使用 `el-table` + `el-pagination`（布局：total, sizes, prev, pager, next, jumper）
- 新增/编辑使用 `el-dialog` 内联弹框，不跳转独立页面
- 删除操作必须经过 `$confirm()` 二次确认
- 成功操作后 `$message.success()` + 刷新列表，失败显示 `$message.error(res.msg)`
- 空数据显示 `empty-text="暂无数据"`
- 日期格式统一 `YYYY-MM-DD HH:mm:ss`

---

## 文件结构总览

### 新增 API 文件 (7 个)
```
backend/api/
├── printer.js         # 打印管理
├── payment.js         # 支付管理
├── delivery.js        # 外卖平台
├── dining.js          # 堂食管理
├── inventory.js       # 进销存
├── member-center.js   # 会员营销
└── report.js          # 经营报表
```

### 新增页面文件 (23 个)
```
backend/page/
├── printer/
│   ├── config-list.html
│   └── log-list.html
├── payment/
│   └── order-list.html
├── delivery/
│   └── order-list.html
├── dining/
│   ├── area-list.html
│   ├── table-list.html
│   ├── queue-list.html
│   └── reservation-list.html
├── inventory/
│   ├── category-list.html
│   ├── supplier-list.html
│   ├── material-list.html
│   ├── purchase-list.html
│   ├── stock-check.html
│   └── stock-record.html
├── member-center/
│   ├── member-list.html
│   ├── level-list.html
│   ├── coupon-list.html
│   ├── points-list.html
│   └── recharge-list.html
└── report/
    ├── daily.html
    ├── dish-ranking.html
    ├── time-slot.html
    └── payment-analysis.html
```

### 修改文件
```
backend/index.html                   # 追加 7 个菜单项
```

### 新增后端文件
```
module/printer/controller/PrinterLogController.java   # 打印日志分页接口
```

---

## 参考模板：标准 CRUD 页面结构

所有列表页遵循以下 HTML 骨架：

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Document</title>
  <link rel="stylesheet" href="../../plugins/element-ui/index.css" />
  <link rel="stylesheet" href="../../styles/common.css" />
  <link rel="stylesheet" href="../../styles/page.css" />
</head>
<body>
  <div class="dashboard-container" id="app">
    <div class="container">
      <!-- 搜索栏 -->
      <div class="tableBar">
        <el-input v-model="input" placeholder="搜索..." style="width:250px" clearable @keyup.enter.native="handleQuery">
          <i slot="prefix" class="el-input__icon el-icon-search" style="cursor:pointer" @click="handleQuery"></i>
        </el-input>
        <el-button type="primary" @click="openDialog('add')">+ 新增</el-button>
      </div>
      <!-- 表格 -->
      <el-table :data="tableData" stripe class="tableBox">
        <el-table-column prop="name" label="名称"></el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="small" class="blueBug" @click="openDialog('edit', scope.row)">编辑</el-button>
            <el-button type="text" size="small" class="delBut non" @click="deleteHandle(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页 -->
      <el-pagination class="pageList" :page-sizes="[10,20,30,40]" :page-size="pageSize" layout="total, sizes, prev, pager, next, jumper" :total="counts" :current-page.sync="page" @size-change="pageSize=val;init()" @current-change="page=val;init()"></el-pagination>
    </div>
    <!-- 弹框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" :before-close="()=>dialogVisible=false">
      <el-form ref="form" :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </span>
    </el-dialog>
  </div>
  <script src="../../plugins/vue/vue.js"></script>
  <script src="../../plugins/element-ui/index.js"></script>
  <script src="../../plugins/axios/axios.min.js"></script>
  <script src="../../js/request.js"></script>
  <script src="../../api/xxx.js"></script>
  <script>
    new Vue({
      el: '#app',
      data() {
        return {
          input: '', counts: 0, page: 1, pageSize: 10, tableData: [],
          dialogVisible: false, dialogTitle: '', form: {}
        }
      },
      created() { this.init() },
      methods: {
        async init() {
          await listApi({ page: this.page, pageSize: this.pageSize }).then(res => {
            if (String(res.code) === '1') { this.tableData = res.data.records || []; this.counts = res.data.total }
          }).catch(err => this.$message.error('请求出错了：' + err))
        },
        handleQuery() { this.page = 1; this.init() },
        openDialog(type, row) {
          this.dialogVisible = true; this.dialogTitle = type === 'add' ? '新增' : '修改'
          this.form = type === 'add' ? {} : { ...row }
        },
        submitForm() {
          const api = this.form.id ? updateApi : addApi
          api(this.form).then(res => {
            if (res.code === 1) { this.$message.success('操作成功！'); this.dialogVisible = false; this.init() }
            else { this.$message.error(res.msg || '操作失败') }
          }).catch(err => this.$message.error('请求出错了：' + err))
        },
        deleteHandle(id) {
          this.$confirm('确认删除?', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }).then(() => {
            deleteApi(id).then(res => {
              if (res.code === 1) { this.$message.success('删除成功！'); this.init() }
              else { this.$message.error(res.msg || '操作失败') }
            }).catch(err => this.$message.error('请求出错了：' + err))
          })
        }
      }
    })
  </script>
</body>
</html>
```

**上述模板是后续所有 CRUD 页面的基础。** 每个页面的差异仅在于：表格列定义、搜索字段、弹框表单字段、引用的 API 文件。

---

## Task 1: API 封装层

创建 7 个 API JS 文件，每个文件封装对应模块的所有后端接口调用。

### printer.js

```javascript
const printerConfigPage = (params) => $axios({ url: '/printer/config/page', method: 'get', params })
const printerConfigList = (params) => $axios({ url: '/printer/config/list', method: 'get', params })
const addPrinterConfig = (params) => $axios({ url: '/printer/config', method: 'post', data: params })
const updatePrinterConfig = (params) => $axios({ url: '/printer/config', method: 'put', data: params })
const deletePrinterConfig = (id) => $axios({ url: `/printer/config/${id}`, method: 'delete' })
const getPrinterConfig = (id) => $axios({ url: `/printer/config/${id}`, method: 'get' })
const printerTest = (id) => $axios({ url: `/printer/test/${id}`, method: 'post' })
const printerStatus = (id) => $axios({ url: `/printer/status/${id}`, method: 'get' })
const printerPrint = (orderId, type) => $axios({ url: `/printer/print/${orderId}`, method: 'post', params: { type } })
const printerLogPage = (params) => $axios({ url: '/printer/log/page', method: 'get', params })
```

### payment.js

```javascript
const paymentPage = (params) => $axios({ url: '/api/payment/page', method: 'get', params })
const paymentCreate = (params) => $axios({ url: '/api/payment/pay', method: 'post', data: params })
const paymentRefund = (params) => $axios({ url: '/api/payment/refund', method: 'post', data: params })
const paymentQuery = (tradeNo) => $axios({ url: `/api/payment/query/${tradeNo}`, method: 'get' })
```

### delivery.js

```javascript
const deliveryOrderPage = (params) => $axios({ url: '/api/delivery/orders', method: 'get', params })
const deliveryAccept = (params) => $axios({ url: '/api/delivery/accept', method: 'post', data: params })
const deliverySyncMenu = () => $axios({ url: '/api/delivery/sync/menu', method: 'post' })
const deliverySyncStock = () => $axios({ url: '/api/delivery/sync/stock', method: 'post' })
```

### dining.js

```javascript
const areaPage = (params) => $axios({ url: '/api/dining/area/page', method: 'get', params })
const areaList = () => $axios({ url: '/api/dining/area/list', method: 'get' })
const addArea = (params) => $axios({ url: '/api/dining/area', method: 'post', data: params })
const updateArea = (params) => $axios({ url: '/api/dining/area', method: 'put', data: params })
const deleteArea = (id) => $axios({ url: `/api/dining/area/${id}`, method: 'delete' })
const getArea = (id) => $axios({ url: `/api/dining/area/${id}`, method: 'get' })

const tablePage = (params) => $axios({ url: '/api/dining/table/page', method: 'get', params })
const addTable = (params) => $axios({ url: '/api/dining/table', method: 'post', data: params })
const updateTable = (params) => $axios({ url: '/api/dining/table', method: 'put', data: params })
const deleteTable = (id) => $axios({ url: `/api/dining/table/${id}`, method: 'delete' })
const getTable = (id) => $axios({ url: `/api/dining/table/${id}`, method: 'get' })
const updateTableStatus = (params) => $axios({ url: '/api/dining/table/status', method: 'put', data: params })
const tableQrcode = (id) => $axios({ url: `/api/dining/table/qrcode/${id}`, method: 'get' })

const queuePage = (params) => $axios({ url: '/api/dining/queue/page', method: 'get', params })
const queueTake = (params) => $axios({ url: '/api/dining/queue/take', method: 'post', data: params })
const queueCall = () => $axios({ url: '/api/dining/queue/call', method: 'put' })
const queueCancel = (id) => $axios({ url: `/api/dining/queue/cancel/${id}`, method: 'put' })

const reservationPage = (params) => $axios({ url: '/api/dining/reservation/page', method: 'get', params })
const addReservation = (params) => $axios({ url: '/api/dining/reservation', method: 'post', data: params })
const confirmReservation = (id) => $axios({ url: `/api/dining/reservation/confirm/${id}`, method: 'put' })
const cancelReservation = (id) => $axios({ url: `/api/dining/reservation/cancel/${id}`, method: 'put' })
const arriveReservation = (id) => $axios({ url: `/api/dining/reservation/arrive/${id}`, method: 'put' })
```

### inventory.js

```javascript
// 食材分类
const matCategoryPage = (params) => $axios({ url: '/api/inventory/material-category/page', method: 'get', params })
const matCategoryList = () => $axios({ url: '/api/inventory/material-category/list', method: 'get' })
const addMatCategory = (params) => $axios({ url: '/api/inventory/material-category', method: 'post', data: params })
const updateMatCategory = (params) => $axios({ url: '/api/inventory/material-category', method: 'put', data: params })
const deleteMatCategory = (id) => $axios({ url: `/api/inventory/material-category/${id}`, method: 'delete' })

// 供应商
const supplierPage = (params) => $axios({ url: '/api/inventory/supplier/page', method: 'get', params })
const supplierList = () => $axios({ url: '/api/inventory/supplier/list', method: 'get' })
const addSupplier = (params) => $axios({ url: '/api/inventory/supplier', method: 'post', data: params })
const updateSupplier = (params) => $axios({ url: '/api/inventory/supplier', method: 'put', data: params })
const deleteSupplier = (id) => $axios({ url: `/api/inventory/supplier/${id}`, method: 'delete' })

// 食材
const materialPage = (params) => $axios({ url: '/api/inventory/material/page', method: 'get', params })
const materialList = () => $axios({ url: '/api/inventory/material/list', method: 'get' })
const addMaterial = (params) => $axios({ url: '/api/inventory/material', method: 'post', data: params })
const updateMaterial = (params) => $axios({ url: '/api/inventory/material', method: 'put', data: params })
const deleteMaterial = (id) => $axios({ url: `/api/inventory/material/${id}`, method: 'delete' })
const materialWarning = () => $axios({ url: '/api/inventory/material/warning', method: 'get' })

// 采购单
const purchasePage = (params) => $axios({ url: '/api/inventory/purchase-order/page', method: 'get', params })
const addPurchase = (params) => $axios({ url: '/api/inventory/purchase-order', method: 'post', data: params })
const getPurchase = (id) => $axios({ url: `/api/inventory/purchase-order/${id}`, method: 'get' })
const addPurchaseDetail = (params) => $axios({ url: '/api/inventory/purchase-order/addDetail', method: 'post', data: params })
const receivePurchase = (id) => $axios({ url: `/api/inventory/purchase-order/receive/${id}`, method: 'put' })
const cancelPurchase = (id) => $axios({ url: `/api/inventory/purchase-order/cancel/${id}`, method: 'put' })
const purchaseDetailList = (orderId) => $axios({ url: `/api/inventory/purchase-order-detail/list/${orderId}`, method: 'get' })

// 库存记录
const stockRecordPage = (params) => $axios({ url: '/api/inventory/stock-record/page', method: 'get', params })
const stockIn = (params) => $axios({ url: '/api/inventory/stock-record/stockIn', method: 'post', data: params })
const stockOut = (params) => $axios({ url: '/api/inventory/stock-record/stockOut', method: 'post', data: params })

// 盘点
const stockCheckPage = (params) => $axios({ url: '/api/inventory/stock-check/page', method: 'get', params })
const addStockCheck = (params) => $axios({ url: '/api/inventory/stock-check', method: 'post', data: params })
const completeStockCheck = (id) => $axios({ url: `/api/inventory/stock-check/complete/${id}`, method: 'put' })
```

### member-center.js

```javascript
const memberPage = (params) => $axios({ url: '/api/member/member/page', method: 'get', params })
const addMember = (params) => $axios({ url: '/api/member/member', method: 'post', data: params })
const updateMember = (params) => $axios({ url: '/api/member/member', method: 'put', data: params })
const getMember = (id) => $axios({ url: `/api/member/member/${id}`, method: 'get' })
const memberRecharge = (params) => $axios({ url: '/api/member/member/recharge', method: 'post', data: params })
const memberDeductBalance = (params) => $axios({ url: '/api/member/member/deduct-balance', method: 'post', data: params })

const levelPage = (params) => $axios({ url: '/api/member/level/page', method: 'get', params })
const addLevel = (params) => $axios({ url: '/api/member/level', method: 'post', data: params })
const updateLevel = (params) => $axios({ url: '/api/member/level', method: 'put', data: params })
const deleteLevel = (id) => $axios({ url: `/api/member/level/${id}`, method: 'delete' })
const getLevel = (id) => $axios({ url: `/api/member/level/${id}`, method: 'get' })

const rechargePage = (params) => $axios({ url: '/api/member/recharge/page', method: 'get', params })
const pointsPage = (params) => $axios({ url: '/api/member/points/page', method: 'get', params })

const couponTemplatePage = (params) => $axios({ url: '/api/member/coupon-template/page', method: 'get', params })
const addCouponTemplate = (params) => $axios({ url: '/api/member/coupon-template', method: 'post', data: params })
const updateCouponTemplate = (params) => $axios({ url: '/api/member/coupon-template', method: 'put', data: params })
const deleteCouponTemplate = (id) => $axios({ url: `/api/member/coupon-template/${id}`, method: 'delete' })
const getCouponTemplate = (id) => $axios({ url: `/api/member/coupon-template/${id}`, method: 'get' })

const couponUserPage = (params) => $axios({ url: '/api/member/coupon-user/page', method: 'get', params })
const couponMy = (memberId) => $axios({ url: `/api/member/coupon-user/my/${memberId}`, method: 'get' })
```

### report.js

```javascript
const reportDaily = (params) => $axios({ url: '/api/report/daily', method: 'get', params })
const reportDishRanking = (params) => $axios({ url: '/api/report/dish-ranking', method: 'get', params })
const reportTimeSlot = (params) => $axios({ url: '/api/report/time-slot', method: 'get', params })
const reportPayment = (params) => $axios({ url: '/api/report/payment', method: 'get', params })
const reportExport = (params) => $axios({ url: '/api/report/export', method: 'get', params, responseType: 'blob' })
```

- [ ] **Step 1: 创建 `backend/api/printer.js`**
- [ ] **Step 2: 创建 `backend/api/payment.js`**
- [ ] **Step 3: 创建 `backend/api/delivery.js`**
- [ ] **Step 4: 创建 `backend/api/dining.js`**
- [ ] **Step 5: 创建 `backend/api/inventory.js`**
- [ ] **Step 6: 创建 `backend/api/member-center.js`**
- [ ] **Step 7: 创建 `backend/api/report.js`**
- [ ] **Step 8: 提交**

```bash
git add src/main/resources/backend/api/printer.js src/main/resources/backend/api/payment.js src/main/resources/backend/api/delivery.js src/main/resources/backend/api/dining.js src/main/resources/backend/api/inventory.js src/main/resources/backend/api/member-center.js src/main/resources/backend/api/report.js
git commit -m "feat(frontend): add API layer for 7 business modules"
```

---

## Task 2: 打印管理页面 + PrinterLogController

### PrinterLogController.java

```java
package com.reggie.module.printer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/printer/log")
@Slf4j
public class PrinterLogController {

    @Autowired
    private PrinterLogService printerLogService;

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long orderId) {
        Page<PrinterLog> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PrinterLog> qw = new LambdaQueryWrapper<>();
        qw.eq(orderId != null, PrinterLog::getOrderId, orderId);
        qw.orderByDesc(PrinterLog::getCreatedTime);
        printerLogService.page(pageInfo, qw);
        return R.success(pageInfo);
    }
}
```

### config-list.html 关键差异

- 表格列：name, brand, type, ipAddress, port, paperSize, printType, status(启用/停用标签)
- 表单字段：name, type(下拉:USB/TCP/CLOUD/BLUETOOTH), brand, deviceId, ipAddress, port, paperSize(下拉:58mm/80mm), printType(多选框), sort, status
- 操作列额外按钮：测试连接（调用 `printerTest(id)`）
- 引用的 API 文件：`../../api/printer.js`

### log-list.html 关键差异

- 纯展示页，无新增/编辑/删除操作
- 表格列：orderId, printType, printerId(显示名称), status(成功/失败标签), errorMsg, createdTime
- 搜索：orderId
- 引用的 API 文件：`../../api/printer.js`

- [ ] **Step 1: 创建 `PrinterLogController.java`**
- [ ] **Step 2: 创建 `printer/config-list.html`**
- [ ] **Step 3: 创建 `printer/log-list.html`**
- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/reggie/module/printer/controller/PrinterLogController.java src/main/resources/backend/page/printer/config-list.html src/main/resources/backend/page/printer/log-list.html
git commit -m "feat(frontend): add printer management pages and PrinterLogController"
```

---

## Task 3: 支付管理页面

### order-list.html 关键差异

- 表格列：id(tradeNo), orderId, channel(支付宝/微信标签), amount(￥前缀), status(待支付/成功/失败/已退款标签), paidTime
- 搜索：orderId, channel(下拉), status(下拉), date range (el-date-picker type=datetimerange)
- 操作：查看详情(弹框)、退款(仅status=SUCCESS时显示，二次确认弹框含退款金额和原因)
- 引用的 API 文件：`../../api/payment.js`

- [ ] **Step 1: 创建 `payment/order-list.html`**
- [ ] **Step 2: 提交**

```bash
git add src/main/resources/backend/page/payment/order-list.html
git commit -m "feat(frontend): add payment order list page"
```

---

## Task 4: 外卖平台页面

### order-list.html 关键差异

- 表格列：platformOrderId, platform(美团/饿了么/抖音标签), dishSummary, amount, userName, address, status, orderTime
- 搜索：platform(下拉), status, date range
- 顶部按钮：同步菜品、同步库存（调用 deliverySyncMenu / deliverySyncStock）
- 操作：手动接单（调用 deliveryAccept）
- 引用的 API 文件：`../../api/delivery.js`

- [ ] **Step 1: 创建 `delivery/order-list.html`**
- [ ] **Step 2: 提交**

```bash
git add src/main/resources/backend/page/delivery/order-list.html
git commit -m "feat(frontend): add delivery platform order list page"
```

---

## Task 5: 堂食管理页面

### area-list.html

- 表格列：name, sort
- 弹框：name, sort
- API 引用：`../../api/dining.js`（areaPage, addArea, updateArea, deleteArea）

### table-list.html

- 表格列：name, areaId(显示区域名), seatCount, status(绿色空闲/红色占用/橙色预留标签), minAmount(￥), sort
- 弹框：name, areaId(下拉加载 areaList), seatCount, minAmount, status, sort
- 操作：编辑、状态切换(点击标签弹出确认)、生成二维码(调用 tableQrcode(id) 下载)
- API 引用：`../../api/dining.js`（tablePage, addTable, updateTable, deleteTable, updateTableStatus, tableQrcode, areaList）

### queue-list.html

- 表格列：queueNo, phone, seatCount, status(等待/已叫号/已入座/已取消标签), createdTime
- 搜索：phone, status(下拉)
- 操作：叫号(queueCall)、取消(queueCancel)
- 弹框：取号弹框(phone, seatCount) 调用 queueTake
- API 引用：`../../api/dining.js`（queuePage, queueTake, queueCall, queueCancel）

### reservation-list.html

- 表格列：customerName, phone, tableId(显示桌名), reservedTime, seatCount, status(待确认/已确认/已到店/已取消标签), remark
- 搜索：customerName, phone, status, date range
- 操作：确认(confirmReservation)、到店(arriveReservation)、取消(cancelReservation)
- API 引用：`../../api/dining.js`（reservationPage, confirmReservation, cancelReservation, arriveReservation）

- [ ] **Step 1: 创建 `dining/area-list.html`**
- [ ] **Step 2: 创建 `dining/table-list.html`**
- [ ] **Step 3: 创建 `dining/queue-list.html`**
- [ ] **Step 4: 创建 `dining/reservation-list.html`**
- [ ] **Step 5: 提交**

```bash
git add src/main/resources/backend/page/dining/
git commit -m "feat(frontend): add dining management pages (area, table, queue, reservation)"
```

---

## Task 6: 进销存页面

### category-list.html

- 表格列：name, sort
- 弹框：name, sort
- API 引用：`../../api/inventory.js`（matCategoryPage, addMatCategory, updateMatCategory, deleteMatCategory）

### supplier-list.html

- 表格列：name, contact, phone, address, status
- 搜索：name, contact
- 弹框：name, contact, phone, address, status
- API 引用：`../../api/inventory.js`（supplierPage, addSupplier, updateSupplier, deleteSupplier）

### material-list.html

- 表格列：name, categoryId(分类名), unit, stockQty(红色高亮 if stockQty <= minStock), minStock, unitPrice(￥), supplierId(供应商名), barcode, status
- 搜索：name, categoryId(下拉加载 matCategoryList)
- 弹框表单：name, categoryId(下拉), unit, stockQty, minStock, unitPrice, supplierId(下拉加载 supplierList), barcode, status
- 顶部按钮：库存预警(弹框调用 materialWarning 显示低库存清单)
- API 引用：`../../api/inventory.js`（materialPage, addMaterial, updateMaterial, deleteMaterial, materialWarning, matCategoryList, supplierList）

### purchase-list.html

- 表格列：id(采购单号), supplierId(供应商名), totalAmount, status(待收货/已收货/已取消标签), createdTime
- 搜索：supplierId(下拉), status(下拉)
- 展开行：el-table 的 expand 行，加载 purchaseDetailList(orderId) 显示明细表格( materialId(食材名), quantity, unitPrice, subtotal )
- 操作：收货(receivePurchase)、取消(cancelPurchase)
- 弹框：新增采购单(supplierId + 明细行)
- API 引用：`../../api/inventory.js`（purchasePage, getPurchase, addPurchase, addPurchaseDetail, receivePurchase, cancelPurchase, purchaseDetailList, supplierList）

### stock-check.html

- 表格列：id(盘点单号), itemCount, profitLoss(盈亏金额，负值红色), status(进行中/已完成标签), createdTime
- 搜索：status, date range
- 展开行：el-table 的 expand 行显示盘点明细( materialId, bookQty, actualQty, diff )
- 操作：完成盘点(completeStockCheck)
- 弹框：新增盘点(materialId + actualQty)
- API 引用：`../../api/inventory.js`（stockCheckPage, addStockCheck, completeStockCheck, materialList）

### stock-record.html

- 表格列：materialId(食材名), type(入库蓝/出库红/盘点灰标签), quantity, unitPrice, totalAmount, remark, createdTime
- 搜索：materialId(下拉), type(下拉), date range
- 只读操作：无新增/编辑/删除
- API 引用：`../../api/inventory.js`（stockRecordPage, materialList）

- [ ] **Step 1: 创建 `inventory/category-list.html`**
- [ ] **Step 2: 创建 `inventory/supplier-list.html`**
- [ ] **Step 3: 创建 `inventory/material-list.html`**
- [ ] **Step 4: 创建 `inventory/purchase-list.html`**
- [ ] **Step 5: 创建 `inventory/stock-check.html`**
- [ ] **Step 6: 创建 `inventory/stock-record.html`**
- [ ] **Step 7: 提交**

```bash
git add src/main/resources/backend/page/inventory/
git commit -m "feat(frontend): add inventory management pages"
```

---

## Task 7: 会员营销页面

### member-list.html

- 表格列：name, phone, levelName, points, balance(￥), totalConsumption(￥), createdTime
- 搜索：name, phone, levelId(下拉加载 levelPage)
- 操作：查看详情(弹框)、充值(弹框: amount + giftAmount, 调用 memberRecharge)
- API 引用：`../../api/member-center.js`（memberPage, getMember, memberRecharge, memberDeductBalance, levelPage）

### level-list.html

- 表格列：name, requiredPoints, discountRate(显示为百分比), sort
- 弹框：name, requiredPoints, discountRate, sort
- API 引用：`../../api/member-center.js`（levelPage, addLevel, updateLevel, deleteLevel）

### coupon-list.html

- 表格列：name, type(满减/折扣/新客标签), conditionAmount, discountAmount, totalCount, remainCount, validDays, status
- 搜索：name, type(下拉), status
- 弹框：name, type(下拉), conditionAmount, discountAmount, totalCount, validDays, status
- API 引用：`../../api/member-center.js`（couponTemplatePage, addCouponTemplate, updateCouponTemplate, deleteCouponTemplate）

### points-list.html

- 只读列表
- 搜索：phone(会员手机号)
- 表格列：memberName, phone, type(获取/消费/过期标签), points, balance, createdTime
- API 引用：`../../api/member-center.js`（pointsPage）

### recharge-list.html

- 只读列表
- 搜索：phone(会员手机号)
- 表格列：memberName, phone, amount(￥), giftAmount(￥), createdTime
- API 引用：`../../api/member-center.js`（rechargePage）

- [ ] **Step 1: 创建 `member-center/member-list.html`**
- [ ] **Step 2: 创建 `member-center/level-list.html`**
- [ ] **Step 3: 创建 `member-center/coupon-list.html`**
- [ ] **Step 4: 创建 `member-center/points-list.html`**
- [ ] **Step 5: 创建 `member-center/recharge-list.html`**
- [ ] **Step 6: 提交**

```bash
git add src/main/resources/backend/page/member-center/
git commit -m "feat(frontend): add member marketing pages"
```

---

## Task 8: 经营报表页面（含 ECharts）

所有 4 个报表页面引入 ECharts CDN：
```html
<script src="https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js"></script>
```

在 `mounted()` 中 init 图表，`beforeDestroy()` 中 dispose：

```javascript
mounted() {
  this.$nextTick(() => {
    this.chart = echarts.init(document.getElementById('chart'))
    window.addEventListener('resize', () => this.chart.resize())
    this.loadChart()
  })
},
beforeDestroy() {
  window.removeEventListener('resize', () => this.chart.resize())
  if (this.chart) this.chart.dispose()
}
```

### daily.html

- 日期选择：`el-date-picker` type="date"，默认当天
- 指标卡片：4 个 `el-card` 行内显示营业额、订单数、客单价、翻台率
- 趋势图：ECharts 折线图，X 轴日期，Y 轴金额，显示近 7 天趋势
- API 引用：`../../api/report.js`（reportDaily）

图表 option 示例：
```javascript
const option = {
  xAxis: { type: 'category', data: dates },
  yAxis: { type: 'value' },
  series: [{ type: 'line', data: amounts, smooth: true, areaStyle: {} }],
  tooltip: { trigger: 'axis' }
}
```

### dish-ranking.html

- 日期范围：`el-date-picker` type="daterange"
- 柱状图：ECharts，X 轴菜品名，Y 轴销量，Top 10
- 排行表格：rank, name, salesCount, amount(￥)
- 导出按钮：调用 reportExport 下载 CSV
- API 引用：`../../api/report.js`（reportDishRanking, reportExport）

### time-slot.html

- 日期选择：`el-date-picker` type="date"
- 饼图：ECharts，显示早/中/晚/夜各时段订单占比
- 数据表格：timeSlot, orderCount, percentage, amount

### payment-analysis.html

- 日期范围：`el-date-picker` type="daterange"
- 饼图：ECharts，显示支付宝/微信/其他占比
- 数据表格：channel, transactionCount, amount, percentage

- [ ] **Step 1: 创建 `report/daily.html`**
- [ ] **Step 2: 创建 `report/dish-ranking.html`**
- [ ] **Step 3: 创建 `report/time-slot.html`**
- [ ] **Step 4: 创建 `report/payment-analysis.html`**
- [ ] **Step 5: 提交**

```bash
git add src/main/resources/backend/page/report/
git commit -m "feat(frontend): add business report pages with ECharts"
```

---

## Task 9: 菜单入口

修改 `backend/index.html`，在 `menuList` 数组末尾追加 7 个菜单项：

```javascript
{ id: '7', name: '打印管理', url: 'page/printer/config-list.html', icon: 'icon-printer' },
{ id: '8', name: '支付管理', url: 'page/payment/order-list.html', icon: 'icon-payment' },
{ id: '9', name: '外卖平台', url: 'page/delivery/order-list.html', icon: 'icon-delivery' },
{ id: '10', name: '堂食管理', url: 'page/dining/table-list.html', icon: 'icon-dining' },
{ id: '11', name: '进销存管理', url: 'page/inventory/material-list.html', icon: 'icon-inventory' },
{ id: '12', name: '会员营销', url: 'page/member-center/member-list.html', icon: 'icon-member-center' },
{ id: '13', name: '经营报表', url: 'page/report/daily.html', icon: 'icon-report' },
```

注意：菜单 ID 从 7 开始（已有 2-6 对应员工/分类/菜品/套餐/订单，ID 1 预留）。图标类名需要在 `iconfont.css` 中存在或新增对应的 CSS 类。

- [ ] **Step 1: 修改 `backend/index.html`，追加菜单项**
- [ ] **Step 2: 提交**

```bash
git add src/main/resources/backend/index.html
git commit -m "feat(frontend): add menu entries for 7 business modules"
```

---

## 执行顺序

```
Task 1 (API 层)           → 基础，必须先做
Task 2 (Printer 页面)      → 可独立
Task 3 (Payment 页面)      → 可独立
Task 4 (Delivery 页面)     → 可独立
Task 5 (Dining 页面)       → 可独立
Task 6 (Inventory 页面)    → 可独立
Task 7 (Member 页面)       → 可独立
Task 8 (Report 页面)       → 可独立
Task 9 (菜单入口)           → 最后，依赖所有页面存在
```

Task 2-8 之间无依赖，可并行执行。
