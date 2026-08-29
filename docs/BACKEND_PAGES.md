# 管理后台页面清单

**范围**: `src/main/resources/backend/` 下管理后台（Vue2 + Element-UI，原生 JS，无构建）。

---

## 1. 目录结构

```
src/main/resources/backend/
├── index.html                # 主入口 + 侧边栏菜单 + 路由
├── favicon.ico
├── images/                   # 静态图片
├── page/                     # 页面 HTML（33 个模块，见第 2 节）
├── api/                      # API 封装（35 个 .js 文件，按模块拆分）
├── js/                       # 公共 JS（组件、请求、校验、性能等）
│   ├── index.js              # 入口
│   ├── request.js            # axios 实例 + CSRF 注入
│   ├── components.js         # 全局组件（crud-dialog、stat-cards 等）
│   ├── common.js             # 通用工具
│   ├── validate.js           # 表单校验规则
│   ├── chart-panel.js        # ECharts 封装
│   ├── performance-utils.js  # 性能工具
│   ├── accessibility-utils.js
│   └── emoji-to-icon.js
├── styles/                   # 样式（21 个 CSS 文件）
│   ├── tokens.css            # ★ 设计令牌（全站唯一事实来源）
│   ├── design-system.css     # 设计系统
│   ├── unified-components.css
│   ├── consistency.css
│   ├── components.css        # 组件样式
│   ├── common-base.css       # 基础公共
│   ├── common-el.css         # Element-UI 定制
│   ├── common.css            # 通用样式
│   ├── common-fixes.css
│   ├── form-fixes.css
│   ├── components-stats-card.css
│   ├── chart-panel.css
│   ├── page.css
│   ├── main.css
│   ├── index.css
│   ├── login.css
│   ├── ai-chat.css
│   ├── ai-provider.css
│   └── fonts/                # 图标字体
└── plugins/                  # 第三方插件
```

---

## 2. 页面清单（33 个模块，60+ HTML 页面）

| 模块 | 页面文件 | 用途 |
|---|---|---|
| **login** | login.html | 员工登录页 |
| **dashboard** | overview.html | 经营总览看板 |
| **food** | list.html, spec-management.html | 菜品列表 + 规格管理 |
| **category** | list.html | 分类管理 |
| **combo** | list.html | 套餐管理 |
| **order** | list.html | 订单管理 |
| **payment** | order-list.html | 支付订单列表 |
| **delivery** | order-list.html, range-management.html | 配送单 + 配送范围/费率 |
| **dining** | table-list.html, area-list.html, queue-list.html, reservation-list.html, qrcode-order.html | 堂食桌台/桌区/排队/预约/二维码点餐 |
| **export** | index.html | Excel/PDF 导出 |
| **cashier** | index.html, daily-settlement.html | 收银台 + 日结 |
| **finance** | withdrawal.html | 提现管理 |
| **cost** | overview.html, dish-cost.html | 成本总览 + 菜品成本 |
| **inventory** | material-list.html, category-list.html, supplier-list.html, purchase-list.html, stock-check.html, stock-out.html, stock-record.html, material-warning.html, smart-replenish.html | 库存：物料/分类/供应商/采购/盘点/出库/流水/预警/智能补货 |
| **member** | list.html | 会员列表（后台） |
| **member-center** | member-list.html, level-list.html, coupon-list.html, coupon-detail.html, coupon-issue.html, coupon-expiring.html, points-list.html, recharge-list.html | 会员中心（会员/等级/优惠券/积分/充值） |
| **marketing** | full-reduction.html, discount.html, buy-get-free.html, flash-sale.html, new-customer.html | 满减/折扣/买赠/秒杀/新客 |
| **recommend** | overview.html, campaigns.html | 推荐总览 + 营销消息 |
| **retention** | retention.html | 用户留存与流失挽回 |
| **customer-service** | list.html | 客服会话/消息/投诉 |
| **notification** | list.html | 通知模板与发送记录 |
| **printer** | config-list.html, log-list.html | 打印机配置 + 打印日志 |
| **attendance** | attendance.html | 员工考勤 |
| **schedule** | schedule.html | 员工排班 |
| **urgency** | urgency.html | 紧急订单干预 |
| **region** | list.html | 行政区划 |
| **franchise** | franchisee-list.html, contract-list.html, settlement-list.html | 加盟商/合同/结算 |
| **store** | list.html, dashboard.html | 门店管理 + 门店看板 |
| **platform** | config.html, dish-mapping.html, order-list.html | 平台配置/菜品映射/平台订单 |
| **report** | daily.html, dish-ranking.html, evaluation-list.html, food-cost.html, payment-analysis.html, sales-report.html, time-slot.html | 日/菜品排行/评价/食材成本/支付分析/销售报表/时段分析 |
| **sys** | role-list.html, config-list.html, operation-log.html, template-list.html | 角色/系统配置/操作日志/通知模板 |
| **user** | list.html | C 端用户管理 |
| **ai** | assistant.html, provider-config.html | AI 助手 + AI 供应商配置 |
| **_templates** | list-page.html | 通用列表页模板（骨架） |

---

## 3. API 封装清单（35 个 .js）

`src/main/resources/backend/api/` 下每个模块一个 JS 文件，遵循"页面 → 模块 API → 后端接口"分层：

| JS 文件 | 对应模块 |
|---|---|
| login.js | 登录 |
| food.js, dish-spec.js | 菜品 + 规格 |
| category.js | 分类 |
| combo.js | 套餐 |
| order.js | 订单 |
| user.js | C 端用户 |
| payment.js | 支付 |
| delivery.js, delivery-enhanced.js | 配送 |
| dining.js | 堂食 |
| export.js | 导出（含 CSRF 注入拦截器） |
| cashier.js | 收银 |
| finance.js | 财务 |
| cost.js | 成本 |
| inventory.js | 库存 |
| member.js, member-center.js | 会员/会员中心 |
| marketing.js | 营销 |
| recommend.js | 推荐 |
| retention.js | 留存 |
| customer-service.js | 客服 |
| notification.js | 通知 |
| printer.js | 打印 |
| attendance.js | 考勤 |
| schedule.js（如存在，未列出） | 排班 |
| urgency.js | 紧急干预 |
| region.js | 区域 |
| franchise.js | 加盟 |
| store.js | 门店 |
| platform.js | 平台 |
| report.js, report-enhanced.js | 报表 + 增强报表 |
| sys.js | 系统管理 |
| dashboard.js | 数据看板 |
| evaluation.js | 评价 |
| ai.js | AI |

---

## 4. 前端关键规范（强制）

### 4.1 设计令牌（`tokens.css` 是唯一事实来源）

**核心令牌**:

| 类别 | 令牌 | 值 |
|---|---|---|
| 品牌主色 | `--color-brand-500` | `#ffc200` |
| 页面背景 | `--bg-page` | `#f5f6fa` |
| 卡片背景 | `--bg-surface` | `#ffffff` |
| 一级文字 | `--text-primary` | `#1f2937` |
| 正文 | `--text-secondary` | `#4b5563` |
| 辅助文字 | `--text-muted` | `#9ca3af` |
| 边框 | `--border-default` | `#e5e7eb` |
| 间距基准 | `--space-4` | `16px` |
| 模块间距 | `--space-6` | `24px` |
| 圆角卡片 | `--radius-md` | `8px` |
| 圆角弹窗 | `--radius-lg` | `12px` |
| 字号正文 | `--font-size-base` | `14px` |
| 阴影卡片 | `--shadow-sm` | `0 1px 3px rgba(0,0,0,0.06)` |
| 过渡 | `--transition-base` | `0.2s cubic-bezier(0.4,0,0.2,1)` |
| 弹窗宽度别名 | `--sm` / `--md` / `--lg` / `--xl` | 420 / 560 / 720 / 840 px |
| 输入框标准宽度 | — | 293 px |

### 4.2 Element-UI 弹窗规范（**系统性历史 Bug 教训**）

> 曾因 `<el-dialog>` 上误用 `class` 导致全站 18 处弹窗靠左不居中。

- **弹窗类样式必须走 `custom-class`**，**禁止**在 `<el-dialog>` 上写 `class` 或 `v-bind:class`。
  - 原因：`<el-dialog>` 根元素是遮罩层 `.el-dialog__wrapper`；Vue 会把 `class` 合并到该遮罩层，而非内部弹窗盒子 `.el-dialog`。`width` / `unified-dialog` 套到 `position:fixed; left:0` 的遮罩上会导致弹窗靠左。
  - `custom-class` 是 Element-UI 官方 prop，专门把 class 打到内部盒子。
  - ✅ 正确：`custom-class="el-dialog--xl unified-dialog"`
  - ❌ 错误：`class="el-dialog--xl unified-dialog"`
- **统一使用 `js/components.js` 的 `crud-dialog` 组件**，禁止在页面里手写 `<el-dialog>`（需要特殊布局时经批准才可用，且仍须遵守上条）。
- **宽度必须使用设计令牌**（`--sm`/`--md`/`--lg`/`--xl`），禁止魔数。

### 4.3 表单校验规范

- 使用 Element-UI 内置 `:rules` + `validate()`（错误 inline 显示在字段下方）。
- **禁止**仅靠 `this.$message` toast 提示字段错误。
- 禁止遗留无作用的死 class（如 `demo-form-inline`）。

### 4.4 "能上线"定义

- **前端修改后必须浏览器实测**（`Ctrl+Shift+R` 硬刷新清 CSS 缓存）。
- 未实测不得视为完成。

---

## 5. 前端关键公共组件（`js/components.js`）

- `crud-dialog`：统一 CRUD 弹窗（含尺寸令牌 `--sm`/`--md`/`--lg`/`--xl`）
- `stat-cards`：统计卡片区
- 其他：见 `js/index.js` 与 `js/components.js` 具体实现

---

## 6. 与后端的接口约定

- 管理后台主入口：`index.html`
- 路由前缀约定：
  - `/backend/` — 老接口（保留）
  - `/api/` — 新接口（大部分模块使用）
  - `/admin/` — 后台管理专属（如平台配置、AI 供应商）
  - `/common/` — 公共接口（文件上传、餐厅信息）
- 请求拦截器（`js/request.js`）：
  - 自动注入 CSRF Token（含 export.js 独立拦截器）
  - 统一处理 `NOTLOGIN` 响应跳转登录页
  - 统一错误处理（CustomException 422 → 提示业务消息）
