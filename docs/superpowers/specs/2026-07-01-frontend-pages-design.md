# 瑞吉外卖管理后台前端页面设计

**日期：** 2026-07-01
**版本：** v1.0
**状态：** 设计稿

---

## 1. 概述

为瑞吉外卖 7 个新业务模块（打印小票、聚合支付、外卖平台对接、堂食管理、进销存、会员营销、经营报表）开发管理后台前端页面。

### 1.1 技术架构

- 延续现有前端模式：**Vue.js 2 + ElementUI CDN**，每个页面独立 HTML 文件
- 页面通过 iframe 加载在 `backend/index.html` 中
- API 请求封装在独立的 `api/*.js` 文件中
- 报表模块引入 ECharts CDN 渲染图表

### 1.2 设计原则

- **继承现有模式** — 完全复用现有 page.css/common.css 样式和 request.js 拦截器
- **弹框内联** — 新增/编辑统一使用 `el-dialog` 弹框，不跳转独立页面
- **操作直观** — 所有操作提供 loading、成功/错误提示、二次确认弹框
- **搜索即用** — 列表页顶部预留搜索条件，回车或点击查询触发
- **分页统一** — 所有列表页使用 `el-pagination`，布局统一 `total, sizes, prev, pager, next, jumper`

---

## 2. 整体文件结构

```
src/main/resources/backend/
├── api/
│   ├── (已有) login.js member.js category.js food.js combo.js order.js
│   ├── printer.js
│   ├── payment.js
│   ├── delivery.js
│   ├── dining.js
│   ├── inventory.js
│   ├── member-center.js
│   └── report.js
├── page/
│   ├── (已有) member/ category/ food/ combo/ order/ login/ demo/
│   ├── printer/
│   │   ├── config-list.html
│   │   └── log-list.html
│   ├── payment/
│   │   └── order-list.html
│   ├── delivery/
│   │   └── order-list.html
│   ├── dining/
│   │   ├── area-list.html
│   │   ├── table-list.html
│   │   ├── queue-list.html
│   │   └── reservation-list.html
│   ├── inventory/
│   │   ├── category-list.html
│   │   ├── supplier-list.html
│   │   ├── material-list.html
│   │   ├── purchase-list.html
│   │   ├── stock-check.html
│   │   └── stock-record.html
│   ├── member-center/
│   │   ├── member-list.html
│   │   ├── level-list.html
│   │   ├── coupon-list.html
│   │   ├── points-list.html
│   │   └── recharge-list.html
│   └── report/
│       ├── daily.html
│       ├── dish-ranking.html
│       ├── time-slot.html
│       └── payment-analysis.html
└── index.html (修改：追加菜单项)
```

---

## 3. 菜单结构

在 `backend/index.html` 的 `menuList` 中新增 7 个一级菜单：

| 菜单名称 | ID | 默认页面 | 图标 |
|---------|-----|---------|------|
| 打印管理 | 7 | page/printer/config-list.html | icon-printer |
| 支付管理 | 8 | page/payment/order-list.html | icon-payment |
| 外卖平台 | 9 | page/delivery/order-list.html | icon-delivery |
| 堂食管理 | 10 | page/dining/table-list.html | icon-dining |
| 进销存管理 | 11 | page/inventory/material-list.html | icon-inventory |
| 会员营销 | 12 | page/member-center/member-list.html | icon-member-center |
| 经营报表 | 13 | page/report/daily.html | icon-report |

---

## 4. 模块设计

### 4.1 打印管理 (Printer)

**后端 API 前缀：** `/printer/config`（配置）、`/printer`（打印操作）

#### 4.1.1 打印机配置列表 (`config-list.html`)

| 元素 | 说明 |
|------|------|
| 搜索 | 名称模糊搜索 |
| 表格列 | 名称、品牌、类型、IP地址、端口、纸张规格、打印类型、状态(启用/停用)、排序 |
| 操作 | 编辑(弹框)、删除(确认)、测试连接(调用`/printer/test/{id}`) |
| 新增 | `el-dialog` 弹框：名称、类型(下拉：USB/TCP/CLOUD/BLUETOOTH)、品牌(下拉：佳博/芯烨/商米)、deviceId、IP地址、端口、纸张规格(下拉：58mm/80mm)、打印类型(多选：BILL/KITCHEN/DELIVERY)、排序、状态 |

#### 4.1.2 打印日志列表 (`log-list.html`)

| 元素 | 说明 |
|------|------|
| 搜索 | 订单号搜索 |
| 表格列 | 订单号、打印类型、打印机名称、状态(成功/失败)、错误信息、打印时间 |
| 操作 | 无（只读日志） |

> **注意：** 需要新增 `PrinterLogController`（`GET /printer/log/page`）提供分页查询接口。

---

### 4.2 支付管理 (Payment)

**后端 API 前缀：** `/api/payment`

#### 4.2.1 支付订单列表 (`order-list.html`)

| 元素 | 说明 |
|------|------|
| 搜索 | 订单号、支付渠道(下拉)、状态(下拉)、时间范围 |
| 表格列 | 支付单号、关联订单号、渠道(支付宝/微信)、金额、状态(待支付/成功/失败/已退款)、支付时间 |
| 操作 | 查看详情(弹框)、退款(状态为成功时显示，二次确认) |
| 退款弹框 | 输入退款金额、退款原因 |

---

### 4.3 外卖平台 (Delivery)

**后端 API 前缀：** `/api/delivery`

#### 4.3.1 平台订单列表 (`order-list.html`)

| 元素 | 说明 |
|------|------|
| 搜索 | 平台类型(下拉：美团/饿了么/抖音)、状态、时间范围 |
| 表格列 | 平台订单号、平台类型、菜品摘要、金额、用户、地址、状态、下单时间 |
| 操作 | 查看详情(弹框)、手动接单 |
| 顶部操作 | 同步菜品按钮(`POST /api/delivery/sync/menu`)、同步库存按钮(`POST /api/delivery/sync/stock`) |

---

### 4.4 堂食管理 (Dining)

#### 4.4.1 区域管理 (`area-list.html`)

**API：** `/api/dining/area`
样式类似现有分类管理，表格列：区域名称、排序；新增/编辑弹框。

#### 4.4.2 桌台管理 (`table-list.html`)

**API：** `/api/dining/table`

| 元素 | 说明 |
|------|------|
| 搜索 | 桌台名称、区域(下拉)、状态(下拉：空闲/占用/预留) |
| 表格列 | 桌台名称、所属区域、座位数、状态(带颜色标签)、最低消费、排序 |
| 操作 | 编辑(弹框)、状态切换(点击标签切换)、生成二维码(调用`/api/dining/table/qrcode/{id}`) |
| 状态标签配色 | 空闲-绿色、占用-红色、预留-橙色 |

#### 4.4.3 排队管理 (`queue-list.html`)

**API：** `/api/dining/queue`

| 元素 | 说明 |
|------|------|
| 搜索 | 手机号、状态(下拉) |
| 表格列 | 排队号、手机号、人数、状态(等待/已叫号/已入座/已取消)、创建时间 |
| 操作 | 叫号、取消(二次确认) |

#### 4.4.4 预订管理 (`reservation-list.html`)

**API：** `/api/dining/reservation`

| 元素 | 说明 |
|------|------|
| 搜索 | 顾客姓名、手机号、状态、预订日期 |
| 表格列 | 顾客姓名、手机号、桌台、预订时间、人数、状态(待确认/已确认/已到店/已取消)、备注 |
| 操作 | 确认(待确认→已确认)、到店(已确认→已到店)、取消 |

---

### 4.5 进销存管理 (Inventory)

#### 4.5.1 食材分类 (`category-list.html`)

**API：** `/api/inventory/material-category`
表格列：分类名称、排序；新增/编辑弹框。复用区域管理类似风格。

#### 4.5.2 供应商管理 (`supplier-list.html`)

**API：** `/api/inventory/supplier`

| 元素 | 说明 |
|------|------|
| 搜索 | 名称、联系人 |
| 表格列 | 供应商名称、联系人、联系电话、地址、状态 |
| 操作 | 编辑(弹框)、删除 |

#### 4.5.3 食材管理 (`material-list.html`)

**API：** `/api/inventory/material`

| 元素 | 说明 |
|------|------|
| 搜索 | 名称、分类(下拉)、库存预警 |
| 表格列 | 名称、分类、单位、当前库存、最低库存预警线、单价、供应商、条码、状态 |
| 视觉提示 | 库存 <= 最低库存时，行背景标红 |
| 操作 | 编辑(弹框)、删除 |
| 顶部按钮 | 库存预警列表(跳转到预警页面或弹框) |

#### 4.5.4 采购订单 (`purchase-list.html`)

**API：** `/api/inventory/purchase-order`

| 元素 | 说明 |
|------|------|
| 搜索 | 采购单号、供应商(下拉)、状态(下拉：待收货/已收货/已取消) |
| 表格列 | 采购单号、供应商、采购日期、总金额、状态、创建人、创建时间 |
| 展开行 | 点击展开显示采购明细（食材名、数量、单价、小计） |
| 操作 | 收货（待收货→已收货）、取消（待收货→已取消） |

#### 4.5.5 库存盘点 (`stock-check.html`)

**API：** `/api/inventory/stock-check`

| 元素 | 说明 |
|------|------|
| 搜索 | 盘点单号、状态(下拉)、日期范围 |
| 表格列 | 盘点单号、盘点食材数、盈亏金额、状态(进行中/已完成)、盘点人、盘点时间 |
| 操作 | 完成盘点(进行中→已完成)、查看明细(展开行显示食材名、账面数量、实际数量、盈亏) |
| 新增 | 弹框选择食材、填入实际数量，系统自动计算盈亏 |

#### 4.5.6 库存记录 (`stock-record.html`)

**API：** `/api/inventory/stock-record`

| 元素 | 说明 |
|------|------|
| 搜索 | 食材名称、类型(下拉：全部/入库/出库/盘点)、时间范围 |
| 表格列 | 食材名称、类型(带颜色标签：入库-蓝/出库-红/盘点-灰)、数量、单价、总金额、操作人、创建时间 |
| 操作 | 无（只读流水） |

---

### 4.6 会员营销 (Member)

#### 4.6.1 会员列表 (`member-list.html`)

**API：** `/api/member/member`

| 元素 | 说明 |
|------|------|
| 搜索 | 姓名、手机号、等级(下拉) |
| 表格列 | 姓名、手机号、等级、积分、余额、累计消费、注册时间 |
| 操作 | 查看详情(弹框)、充值(弹框输入金额+赠送金额)、扣减余额 |

#### 4.6.2 会员等级 (`level-list.html`)

**API：** `/api/member/level`
表格列：等级名称、所需积分、折扣率(%)、排序；新增/编辑弹框。

#### 4.6.3 优惠券模板 (`coupon-list.html`)

**API：** `/api/member/coupon-template`

| 元素 | 说明 |
|------|------|
| 搜索 | 名称、类型(下拉)、状态 |
| 表格列 | 名称、类型(满减/折扣/新客)、条件金额、优惠金额/折扣率、总数量、剩余数量、有效期天数、状态 |
| 操作 | 编辑(弹框)、删除 |

#### 4.6.4 积分记录 (`points-list.html`)

**API：** `/api/member/points`
只读列表，搜索会员手机号，表格列：会员姓名、手机号、类型(获取/消费/过期)、分值、余额、创建时间。

#### 4.6.5 充值记录 (`recharge-list.html`)

**API：** `/api/member/recharge`
只读列表，搜索会员手机号，表格列：会员姓名、手机号、充值金额、赠送金额、操作人、创建时间。

---

### 4.7 经营报表 (Report)

**API 前缀：** `/api/report`
所有报表页面引入 ECharts CDN（`https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js`）。

#### 4.7.1 营业日报 (`daily.html`)

| 区域 | 内容 |
|------|------|
| 日期选择 | `el-date-picker` 选择日期 |
| 指标卡片 | 营业额、订单数、客单价、翻台率 4 个卡片 |
| 趋势图 | ECharts 折线图展示近 7 天营业趋势 |

#### 4.7.2 菜品排行 (`dish-ranking.html`)

| 区域 | 内容 |
|------|------|
| 日期范围 | 查询日期区间 |
| 排行表格 | 排名、菜品名、销量、营收金额 |
| 柱状图 | ECharts 柱状图展示 Top 10 |
| 导出 | CSV 导出按钮（`/api/report/export`） |

#### 4.7.3 时段分析 (`time-slot.html`)

| 区域 | 内容 |
|------|------|
| 日期选择 | 选择日期 |
| 饼图 | ECharts 饼图展示早/中/晚/夜各时段订单占比 |
| 数据表格 | 时段、订单数、占比、营收 |

#### 4.7.4 支付分析 (`payment-analysis.html`)

| 区域 | 内容 |
|------|------|
| 日期选择 | 查询日期区间 |
| 饼图 | ECharts 饼图展示支付宝/微信/其他渠道占比 |
| 数据表格 | 渠道、交易笔数、金额、占比 |

---

## 5. 交互规范

### 5.1 通用操作流程

```
用户操作 → loading 状态 → 请求成功/失败
  ├─ 成功：$message.success() + 刷新列表
  └─ 失败：$message.error(res.msg || '操作失败')
```

### 5.2 删除操作

所有删除必须经过 `$confirm()` 二次确认。

### 5.3 状态变更

- 单个状态切换：点击行内按钮或标签，弹出确认后操作
- 批量操作：勾选后点击顶部按钮

### 5.4 表单校验

- 必填项：名称、手机号、金额等字段添加 `required` 校验
- 金额：只能输入正数
- 手机号：11 位数字格式校验

### 5.5 表格空状态

无数据时显示 ElementUI 默认空状态插槽 `empty-text="暂无数据"`。

### 5.6 日期时间格式

统一使用 `YYYY-MM-DD HH:mm:ss` 格式。

---

## 6. 技术要点

### 6.1 图表 (ECharts)

报表页面在 `mounted()` 中初始化 ECharts 实例，窗口 resize 时自适应：

```javascript
mounted() {
  this.$nextTick(() => {
    this.chart = echarts.init(document.getElementById('chart'))
    window.addEventListener('resize', this.chart.resize)
  })
},
beforeDestroy() {
  window.removeEventListener('resize', this.chart.resize)
  this.chart.dispose()
}
```

### 6.2 图片预览

在 `api/report.js` 中添加导出函数：

```javascript
const exportReport = (params) => {
  return $axios({
    url: '/api/report/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
```

下载通过创建 Blob URL 触发下载。

### 6.3 二维码

桌台二维码使用 `QRCode.js` CDN 库生成，或在弹框中显示后端返回的二维码图片 URL。

---

## 7. 向后兼容

- 不修改已有 page/ 目录下的任何文件
- 不修改已有 api/ 目录下的任何文件
- 仅修改 `backend/index.html` 追加菜单配置
- 所有新页面路径以新目录名隔离

---

## 8. 页面清单汇总

| 模块 | 页面 | 文件 |
|------|------|------|
| 打印管理 | 配置列表 | printer/config-list.html |
| 打印管理 | 打印日志 | printer/log-list.html |
| 支付管理 | 支付订单 | payment/order-list.html |
| 外卖平台 | 平台订单 | delivery/order-list.html |
| 堂食管理 | 区域管理 | dining/area-list.html |
| 堂食管理 | 桌台管理 | dining/table-list.html |
| 堂食管理 | 排队管理 | dining/queue-list.html |
| 堂食管理 | 预订管理 | dining/reservation-list.html |
| 进销存 | 食材分类 | inventory/category-list.html |
| 进销存 | 供应商管理 | inventory/supplier-list.html |
| 进销存 | 食材管理 | inventory/material-list.html |
| 进销存 | 采购订单 | inventory/purchase-list.html |
| 进销存 | 库存盘点 | inventory/stock-check.html |
| 进销存 | 库存记录 | inventory/stock-record.html |
| 会员营销 | 会员列表 | member-center/member-list.html |
| 会员营销 | 会员等级 | member-center/level-list.html |
| 会员营销 | 优惠券模板 | member-center/coupon-list.html |
| 会员营销 | 积分记录 | member-center/points-list.html |
| 会员营销 | 充值记录 | member-center/recharge-list.html |
| 经营报表 | 营业日报 | report/daily.html |
| 经营报表 | 菜品排行 | report/dish-ranking.html |
| 经营报表 | 时段分析 | report/time-slot.html |
| 经营报表 | 支付分析 | report/payment-analysis.html |
| API | 打印 | api/printer.js |
| API | 支付 | api/payment.js |
| API | 外卖 | api/delivery.js |
| API | 堂食 | api/dining.js |
| API | 进销存 | api/inventory.js |
| API | 会员 | api/member-center.js |
| API | 报表 | api/report.js |
| 配置 | 菜单入口 | index.html (修改) |

**总计：23 个 HTML 页面 + 7 个 API JS 文件 + 1 个 index.html 修改 + 1 个后端 Controller 新增**
