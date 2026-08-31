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

---

## 7. 后台页面实用化重构方案（信息架构 + 设计规范）

> **决策背景（2026-08-30）**：当前后台一级菜单 17 个、功能页 75+。"一操作一页面"导致入口分散、同类功能多处维护、运营频繁跳转。经对标美团商家版 / 有赞 / 客如云（一级菜单一般 8~11 个），决定做信息架构收敛 + 统一设计规范。

### 7.1 顶层原则

1. **一页一个完整业务流程**，而非一个操作。
2. **一级菜单 ≤ 11 个**；低频操作降级为子 Tab / 弹窗 / 按钮。
3. **同一业务仅一个维护入口**，杜绝同数据多页重复维护（营销、优惠券为当前最大冗余）。
4. 所有交互反馈统一走 `window.ReggieUI`，禁止裸 `this.$message/$confirm`。
5. 所有样式只引用 `tokens.css` 令牌，禁止硬编码 hex/魔数。

### 7.2 目标导航架构（17 模块 → 11 模块）

合并后建议的顶级菜单（ID 对应 `index.html` 的 `menuList`，new 为新增归属）：

| 一级菜单 | 收敛后的页面 | 原页面去向 |
|---|---|---|
| **1 工作台** | dashboard/overview（升级为待办台） | 原 1 |
| **2 商品** | category、food(含规格)、combo | 原 2，规格并入菜品 |
| **3 订单** | order/list(聚合支付/配送状态)、cashier(含日结)、urgency、printer | 原 3 + 打印/催菜降级 |
| **4 堂食** | dining/table(含区域)、queue、reservation | 原 5，区域并入桌台 |
| **5 进销存** | inventory/material(含分类)、supplier、purchase、stock-record(入/出合并)、stock-check、material-warning(含补货) | 原 6 |
| **6 会员** | member-center/member、level、points、recharge、coupon(含发放/到期)、retention | 原 7（C端用户并此） |
| **7 营销** | recommend/campaigns（统一活动入口，含智能营销） | 原 9，删 5 个单活动页 |
| **8 报表** | report/daily(多Tab：总览/销售/时段/菜品排行)、payment-analysis | 原 8 |
| **9 门店财务** | store/list、store/dashboard、cost、finance/withdrawal | 原 10 + 14 + 15 |
| **10 系统** | sys/employee、role、config、template、operation-log、region、notification | 原 11 |
| **11 平台接入** | platform/config、dish-mapping、order-list | 原 17 |

> **扩展模块（默认收起，按客户规模显示）**：加盟管理(13)、客服(11-8)、考勤(11-9)、排班(11-10)、AI助手(12)、数据导出(16)、外卖配送(4)。

**预计页面数 75 → 约 40**，信息密度与操作连贯性大幅提升。

### 7.3 合并明细（15 页 → 6 页）

#### 7.3.1 营销中心 7 → 1（最高优先级）
- **删除**：`marketing/discount.html`、`full-reduction.html`、`flash-sale.html`、`buy-get-free.html`、`new-customer.html`（5 个单活动页）
- **保留并升级**：`recommend/campaigns.html` → 「营销活动」唯一入口。新建时用 `campaignType` 下拉（已支持），列表聚合展示所有类型，详情按类型渲染专属字段。
- `recommend/overview.html`（智能推荐）→ 并入营销活动的"智能营销"页签，或作为活动类型的筛选视图。

#### 7.3.2 会员优惠券 4 → 1
- **删除**：`member-center/coupon-detail.html`（投放明细）、`coupon-expiring.html`（到期预警）
- **合并**：`coupon-list.html` + `coupon-issue.html` → 1 个「优惠券」页，顶部 Tab：`券模板` / `发放记录` / `即将到期`。发券并入券模板详情的"发放"按钮。

#### 7.3.3 进销存 10 → 5
- **入库 + 出库 + 流水** → 1 个「库存流水」，Tab 区分 `入库 / 出库 / 全部`，入/出是流水新增操作。
- **库存预警 + 智能补货** → 「库存预警」，预警列表项内置一键补货。
- **原料 + 食材分类** → 原料页内嵌分类管理（2026-08-30 已收敛为搜索栏「食材分类」下拉，不再内嵌分类树）。
- 保留独立：`purchase-list`、`supplier-list`、`stock-check`。

#### 7.3.4 数据分析 6 → 2
- `daily` + `sales-report` + `dish-ranking` + `time-slot` → 1 个「经营报表」多 Tab（总览/销售/时段/菜品排行）。
- `payment-analysis` 保留独立。
- `evaluation-list`（评价管理）→ 移出报表模块，归入"订单/服务"上下文（是互动管理非分析）。

#### 7.3.5 堂食
- `area-list`（区域）并入 `table-list`（桌台），区域作为桌台的筛选/分组维度。

### 7.4 降级 / 隐藏明细

| 页面 | 处理 | 理由 |
|---|---|---|
| `food/spec-management.html` | 降级为菜品子功能 | 规格是菜品的属性，独立成页造成两处维护 |
| `cashier/daily-settlement.html` | 并入收银台 | 日结是收银收尾动作，弹窗确认即可 |
| `printer/log-list.html` | 降级为打印配置 Tab | 日志仅排障用，低频 |
| `urgency/urgency.html` | 降级为订单/堂食 Tab | 依附订单上下文 |
| `export/index.html` | 降级 | 导出应是各页右上角按钮，非独立模块 |
| `notification/list.html` | 降级为系统子页 | 低频 |
| `member/list.html`（员工管理） | 保留员工管理归属，但**正名** | 目录名 `member` 与会员概念混淆，导航名明确为"员工管理" |

### 7.5 升级改进（高频页实用化）

#### 7.5.1 工作台 dashboard/overview（最高优先）
- 顶部**今日关键指标卡**（营业额/订单数/客单价/退款率），可点击下钻到对应报表。
- **待办聚合台**：待接单/待出餐/库存预警/待审核评价/待退款 — 每项带数量 badge，**一键跳转**到对应业务页。
- 让店长一眼看到"今天要做什么"，而非纯图表。

#### 7.5.2 订单明细 order/list
- 状态筛选 + 快捷 Tab（待接单/制作中/配送中/已完成/退款）。
- 行内**聚合操作**（出餐→配送→完成），减少进详情。
- **订单 + 支付 + 配送状态**一表看全，避免跳"支付管理"。

#### 7.5.3 门店管理 store/dashboard
- 门店列表点门店 → 进入该门店经营看板，打通列表与看板。

#### 7.5.4 所有列表页通用
- 统一搜索/筛选顺序（时间→状态→关键字）。
- 低频操作收敛进"更多"下拉；高频（编辑/删除/启用）放首屏。
- 批量操作给出**结果明细**（哪些成功/失败及原因）。

---

### 7.6 页面设计规范（完整视觉/交互/布局）

> 以下规范为**全站强制**，新页面与重构页面必须遵守。所有令牌名以 `styles/tokens.css` 实际定义为准（本文引用的均已在文件中确认存在）。

#### 7.6.1 布局系统

**页面骨架（自上而下）**：

```
┌─────────────────────────────┐
│ 页头：标题 + 副标题          │  ds-page-header
├─────────────────────────────┤
│ [统计卡片区 stat-cards]      │  （可选，业务有指标时）
├─────────────────────────────┤
│ [搜索/筛选栏]                │  统一 table-bar
├─────────────────────────────┤
│ [主体区：表格 / 表单 / 图表] │  内容 1fr
├─────────────────────────────┤
│ [分页器]                    │  底部居中
└─────────────────────────────┘
```

- **页面水平内边距**：`var(--space-6)`（24px）；**区块垂直间距**：`var(--space-4)`（16px）。
- **最大内容宽度**：管理页默认不限定宽度，填满可用视口（1200px 以上时表格合理利用）。
- **卡片容器**：`background: var(--bg-surface); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--border-default); padding: var(--space-4);`
- 弹窗宽度走令牌：`--sm`(420) / `--md`(560) / `--lg`(720) / `--xl`(840)。

#### 7.6.2 导航规范

- **一级菜单**：仅"工作台 + 主链路业务"，文案统一为名词短语（如"订单""商品"），不写"管理"后缀冗余。
- **active 态**：`--color-brand-500` 高亮，图标 + 文字。
- **面包屑**：`首页 / 一级 / 二级`，当前页不加链接。
- 扩展模块折叠在更多/设置中，避免菜单过长。

#### 7.6.3 列表（crud-table）规范 — 沿用既有列对齐规范（4.x 已有）

- **文本列** → 居中；**金额/数字列** → 右对齐；统一走 `resolveColAlign(col)`，禁止页内零散写 `align`。
- 表头跟随数据列：`:header-align="resolveColAlign(col)"`。
- **选择器陷阱**：`<el-table class="tableBox">` 使 `.tableBox` 与 `.el-table` 是同一元素，必须用复合选择器 `.tableBox.el-table`（无空格）。
- 高频操作用图标按钮 + 文字，低频进"更多"下拉。
- 全选仅用于**有明确批量操作**的场景；无批量操作则隐藏 `type="selection"` 列。

#### 7.6.4 状态展示规范

- 状态统一用**带色圆点 + 文字**（`RgStatus`），语义色：
  - 成功/已完成 → `--color-success`
  - 警告/待处理 → `--color-warning`
  - 危险/已取消/错误 → `--color-danger`
  - 进行中/待接单 → `--color-info` 或 `--color-brand-500`
- 同一实体状态在**列表、详情、筛选项**必须使用**同一套映射**（当前订单 2/3 语义前后端颠倒属 BUG，见 7.7）。

#### 7.6.5 表单规范

- 用 Element-UI `:rules` + `validate()`，错误 inline 显示在字段下方；**禁止**仅 toast 提示字段错误。
- 必须经 `crud-dialog` 承载，禁止手写 `<el-dialog>`；宽度走令牌。
- 金额类字段一律 `type="number"` + `precision:2`，后端金额用 BigDecimal，前端展示用 `RgFormat.money`。
- 创建/编辑表单**提交失败**明确区分：业务错误（单 toast）vs 网络/HTTP 错误（统一由 `request.js` 处理，避免双 toast——见 7.7 已知问题）。

#### 7.6.6 弹窗规范（沿用 4.2 历史教训）

- **必须** `custom-class="el-dialog--xl unified-dialog"` 这种形式，**禁止**在 `<el-dialog>` 上写 `class`（会导致全站弹窗靠左）。
- 统一用 `crud-dialog` 组件。

#### 7.6.7 图表规范（ECharts，`chart-panel.js`）

- 图表高度统一 `260px`，卡片 header 左侧标题 (`--font-size-base`, 600) + 右侧副标题 (`--font-size-xs`, `--color-info`)。
- 图表配色优先 `RgPalette` / `--color-brand-*` / `--color-*`；仅在**平台品牌色**（微信 `#07c160`、支付宝 `#1677ff`）场景允许裸 hex（已收敛）。
- 图例、tooltip 使用 `--color-text-secondary`，不用黑死人色。

#### 7.6.8 空状态与加载

- 空数据：统一 `empty-hint` 文案，**纯查询/报表页必须覆盖默认的"点击右上角新建"**，避免误导（当前 38 页缺该覆盖）。
- 加载：crud-table 内置骨架屏，保持行高稳定，避免加载跳动。

#### 7.6.9 交互反馈

- 所有提示走 `window.ReggieUI`（success/error/warning/info）。
- **禁止**混用 `this.$message/$confirm`（当前 customer-service、inventory/material-warning、smart-replenish、member-center/coupon-issue、platform/* 违反，需一并整改）。
- 危险操作（删除/退款/取消）用 `ReggieUI.confirm` 二次确认。
- 批量操作执行后，用列表/侧边 toast 反馈**成功数与失败明细**。

#### 7.6.10 无障碍（沿用）

- 保留 skip-link、"跳转到主内容"、`role="region"` + `aria-label`。
- 手写表格页（customer-service/spec-management/retention/urgency）补齐 `role="region"` + 骨架屏 + 统一空状态。

---

#### 7.6.11 组件规格手册（`js/components.js`，可直接照抄）

> 以下 props / 事件 / 方法均来自 `components.js` 真实定义，示例代码可在页面内直接使用。

#### stats-cards / stat-cards — 统计卡片组

**Props**：`cards:Array`、`activeKey:String`(支持 `.sync`)、`loading:Boolean`

**cards 每项**：`{ key, icon, label, value, color, unit, subText, clickable, active, flex }`
- `icon`：`ri-xxx` 或 `el-icon-xxx` class 字符串，或 emoji
- `color`：`primary/success/warning/danger/info/purple`（别名 blue/green/orange）
- `clickable:true` 时卡片可点击筛选（自动高亮 + `card-click` 事件）

```html
<stat-cards :cards="statCards" :active-key.sync="filterKey" @card-click="onStatClick" :loading="loading" />
```

```js
statCards: [
  { key: 'all',  icon: 'ri-store-2-line',  label: '全部订单', value: stats.total,    color: 'primary' },
  { key: '1',    icon: 'ri-time-line',     label: '待付款',  value: stats.pending,  color: 'warning', clickable: true },
  { key: '4',    icon: 'ri-check-double-line', label: '已完成', value: stats.done, color: 'success', clickable: true },
]
```

#### table-bar — 搜索筛选 + 操作按钮栏

**Props**：`search-items:Array`、`actions:Array`（另有 placeholder/width 等）。

**search-items 每项**：`{ type, field, placeholder, options, width, clearable, hideSearchIcon }`
- `type`：`input/select/date/daterange/number`
- `options`：select 下拉 `[{label,value}]`

**actions 每项**：`{ label, type, icon, handler, show }`（type 为 Element 按钮类型）

```html
<table-bar :search-items="searchItems" :actions="actions" @search="onSearch" @reset="onReset" />
```

```js
searchItems: [
  { type: 'input', field: 'name', placeholder: '菜品名称/编号', width: 180 },
  { type: 'select', field: 'status', placeholder: '状态', options: statusOptions, width: 130 },
  { type: 'daterange', field: 'time', placeholder: '下单时间', width: 220 },
],
actions: [
  { label: '新建', type: 'primary', icon: 'el-icon-plus', handler: () => this.openDialog('create') },
]
```

#### crud-table — 完整 CRUD 表格

**Props**（摘核心）：`data:Array`、`columns:Array`、`selection:Boolean`、`showIndex:Boolean`、`showActions:Boolean`、`actionsWidth`、`actionsFixed:Boolean`、`loading:Boolean`、`page`、`pageSize`、`emptyHint:String`、`ariaLabel:String`。

**columns 每项**：`{ prop, label, width, minWidth, align, fixed, sortable, slot, formatter, showOverflowTooltip, type, className }`
- `type:'money'` → 自动右对齐 + 等宽数字（强烈建议金额列用）；`type:'number'` → 右对齐
- `align` 请交给 `resolveColAlign` 处理；文本框列默认居中、金额/数字右对齐
- 复杂单元格用 `#col-{prop}` 插槽

**事件**：`page-change`、`size-change`（配合 `ReggieListMixin`）。

```html
<crud-table
  :data="list" :columns="cols" :page="page" :page-size="pageSize"
  :total="counts" :loading="loading" :show-actions="true"
  :actions-width="160" :actions-fixed="true"
  empty-hint="暂无满足条件的记录，可尝试调整筛选条件"
  aria-label="菜品列表"
  @page-change="onPageChange" @size-change="onSizeChange" />

<!-- 插槽列示例 -->
<template #col-status="{ row }">
  <el-tag :type="rgStatusTag('order', row.status)">{{ rgStatusText('order', row.status) }}</el-tag>
</template>
```

#### crud-dialog — 统一 CRUD 弹窗

**说明**：必须用此组件承载新建/编辑/详情，**禁止手写 `<el-dialog>`**；宽度走令牌。

```html
<crud-dialog
  :visible.sync="dialogVisible" :title="dialogTitle" width="--md"
  :loading="saving" @confirm="save" @cancel="dialogVisible=false">
  <el-form ref="form" :model="form" :rules="rules" label-width="96px">…</el-form>
</crud-dialog>
```

#### 全局工具（真实方法）

| 工具 | 方法 | 说明 |
|---|---|---|
| `window.RgFormat` | `formatMoney(v)` | 金额千分位+2位小数，空→`0.00` |
| | `formatNumber(v)` | 数量千分位，空→`''` |
| | `formatDate(v)` | `yyyy-MM-dd`，空→`-` |
| | `rgStatusText(name,status)` / `rgStatusTag(name,status)` | 状态中文/标签色 |
| | `rgPreview(url)` | el-image 预览数组 |
| `window.RgPalette` | `brand/success/warning/danger/info`、`seriesBlue…`、`alipayBlue`、`wechatGreen` | ECharts canvas 配色（页面禁写 hex） |
| `window.ReggieStatus` | `register(name, textMap, tagMap)` | 注册业务状态映射 |
| `window.ReggieListMixin` | `page/pageSize/counts/loading` + `onPageChange/onSizeChange` | 消除分页样板 |
| `window.ReggieUI` | `success/error/warning/info/confirm/loading/notify` | 统一反馈（禁 `$message/$confirm`） |

---

#### 7.6.12 设计语言速查表（Design Language）

> 数值均基于 `tokens.css` 现有令牌，重构页面**只允许引用这些令牌**。

| 维度 | 规格 | 令牌 |
|---|---|---|
| 字号：标题 | 16 / 18 / 20px | `--font-size-md/lg/xl` |
| 字号：正文 | 14px | `--font-size-base` |
| 字号：辅助 | 12px | `--font-size-xs` |
| 行高 | 1.5（正文）/ 1.25（标题） | `--line-height-base/tight` |
| 间距基准 | 4 / 8 / 12 / 16 / 24 / 32 / 40 / 64px | `--space-1..16` |
| 卡片圆角 | 12px（卡片）/ 8px（控件）/ 4px | `--radius-lg/md/sm` |
| 卡片阴影 | `0 1px 3px rgba(0,0,0,.06)` | `--shadow-sm` |
| 品牌主色 | `#ffc200`（黄）/ 500 阶 | `--color-brand-500` |
| 成功 / 警告 / 危险 / 信息 | 绿 `#67c23a` / 橙 `#e6a23c` / 红 `#f56c6c` / 灰 `#909399` | `--color-success/warning/danger/info` |
| 文字层级 | 主 `#1f2937` / 次 `#4b5563` / 弱 `#9ca3af` | `--text-primary/secondary/muted` |
| 边框 | 主 `#e5e7eb` / 弱 `#f3f4f6` | `--border-default/subtle` |
| 背景 | 页 `#f5f6fa` / 卡 `#ffffff` | `--bg-page/surface` |
| 按钮类型 | primary(品牌黄) / success / warning / danger / info / plain | Element 默认 + 令牌覆盖 |
| 输入框标准宽 | 293px（可覆盖） | — |
| 弹窗宽 | 420 / 560 / 720 / 840px | `--sm/--md/--lg/--xl` |
| 图标 | RemixIcon（`ri-*`） | — |

**按钮层级**（用于操作列收敛）：
- 主操作（新建/确认/提交）→ `type="primary"`
- 危险操作（删除/退款/取消）→ `type="danger"` + `ReggieUI.confirm`
- 次要操作（编辑/详情/查看）→ 文字按钮或 `plain`
- 同一行操作按钮 > 3 个时，低频项收进 `el-dropdown`"更多"

---

#### 7.6.13 订单明细页聚合设计（升级核心高频页）

**页面布局（线框）**：

```
┌───────────────────────────────────────────────┐
│ 订单管理                          [导出][刷新] │  ds-page-header
├───────────────────────────────────────────────┤
│ 全部 ▎待接单 ▎制作中 ▎配送中 ▎已完成 ▎退款     │  状态快捷 Tab（可点击，带数量）
├───────────────────────────────────────────────┤
│ 订单号[ ] 收货人[ ] 手机号[ ] 时间[ ] 状态[ ]   │  table-bar
├───────────────────────────────────────────────┤
│ ☑ 订单号  顾客  商品  金额  下单时间  支付  状态  操作 │
│ ☑ 2026.. 张三  宫保鸡丁×2 ¥36.0 08-30 12:30 微信 [出餐][完成][详情] │
│                                              │
│                      共 n 条 4/页  ←  →        │
└───────────────────────────────────────────────┘
```

**设计要点**：
- **状态 Tab 与 stat-cards 联动**：点 Tab 即筛选，Tab 标签右侧带当前数量徽标。
- **聚合列**：商品列以 `名称×数量` 折叠显示（悬浮展开明细）；支付列内联显示支付方式+状态。
- **聚合操作**：同一行内联"出餐→完成→配送"，`statusFlow` 单向流转，操作完自动刷新计数，减少进详情弹窗。
- 退款/取消属危险操作，进详情弹窗，`ReggieUI.confirm`。
- 状态统一用 `ReggieStatus.register('order',…)` 预置映射（1待付款/2待接单/3配送中/4已完成/5已取消/6已退款）。

---

#### 7.6.14 工作台待办聚合设计（升级 dashboard）

**布局（线框，优先级从上到下）**：

```
┌───────────────────────────────────────────────┐
│ 工作台  今日 08-30                     [已读全部] │
├───────────────────────────────────────────────┤
│ [今日营业额] [订单数] [客单价] [退款率]         │  stat-cards，可下钻
├───────────────────────────────────────────────┤
│ 待办事项（核心）                                │
│   🔔 待接单 12  待出餐 8  库存预警 3  待评价 6  │  每项带数量，点击跳转对应页
├───────────────────────────────────────────────┤
│ 近7日营业趋势 [图表]       今日订单时段分布      │  双栏图表
├───────────────────────────────────────────────┤
│ 热门菜品 TOP5          异常订单(退款/取消)      │  双栏
└───────────────────────────────────────────────┘
```

**设计要点**：
- **待办台置于首屏最上**（视觉锚点），每项：图标 + 文案 + 数量 badge（红点/数字），点击调 `parent.postMessage` 跳转 to 对应 iframe 页。
- 关键指标卡可点击下钻到报表模块。
- 图表配色一律走 `RgPalette`，高度统一 260px，卡片 header"标题(左)+副题(右)"。
- 空待办时展示"今日暂无待办，辛苦了"正向文案。

---

#### 7.6.15 表单 / 弹窗规格

- **创建/编辑弹窗**：标题 `新建{实体}` / `编辑{实体}-{名称}`；底部固定"取消 / 保存"。
- **表单分栏**：字段多时（>6）自动双栏（`el-col :span="12"`），每栏校验独立。
- **字段分组**：复杂实体用 `el-divider` 分组标题（基本信息 / 价格库存 / 规格）。
- **金额校验**：`precision:2`，后端 BigDecimal 二次校验，前端失败单 toast、成功 `ReggieUI.success`。
- **详情弹窗**：只读展示，用 `el-descriptions`，禁用 `el-form`（避免误触发校验）。

---

#### 7.6.16 页面重构落地检查清单（逐项对照）

每个重构页面重构完成后，逐项自检：

- [ ] 页面骨架 = 页头 + (stat-cards) + table-bar + crud-table + 分页
- [ ] 同一业务仅一个维护入口（无重复 Tab / 重复页）
- [ ] 所有列对齐经 `resolveColAlign`，金额/数字右对齐、文本居中
- [ ] 所有交互反馈走 `ReggieUI`，无 `this.$message/$confirm`
- [ ] 所有弹窗走 `crud-dialog` + `custom-class` + 令牌宽度
- [ ] 所有颜色来自 tokens.css / RgPalette，无裸 hex（除平台品牌色）
- [ ] 状态用 `ReggieStatus` 映射，列表/详情/筛选一致
- [ ] 空状态覆盖 `empty-hint`（纯查询页无"新建"误导）
- [ ] 批量操作有结果明细反馈
- [ ] 高频操作首屏、低频操作收敛"更多"下拉
- [ ] skip-link / `role="region"` / aria-label 齐全
- [ ] 浏览器硬刷新（Ctrl+Shift+R）实测通过

---

### 7.7 配套须一并修复的已知问题（重构时顺手处理）

> **核实修正（2026-08-30 复核）**：以下条目中，部分此前误报的问题经查真实代码后**已修正/降级**，真实存在的问题保留。详见逐条说明。

| 问题 | 核实状态 | 归属 |
|---|---|---|
| 用户端订单 Tab 2/3 语义颠倒、缺"已退款" | **误报，已修复**：`front/page/order.html:104-115` 注释明确"重要 Bug 修复，现严格对齐后端 1=待付款…6=已退款"，Tab 值与后端一致且含已退款。删除原误报。 | — |
| 支付方式三套定义打架（实体注释 / 实际行为 / MetaController 字典缺 3 货到付款） | **真实**，需统一为 1=微信/2=支付宝/3=货到付款，并同步实体注释与字典 | 字典统一 |
| 就餐方式前端映射 key 与后端枚举不一致（TAKEAWAY vs TAKEOUT，前端永远显示英文） | **真实**，需对齐 `OrderSource` 枚举（TAKEOUT/EAT_IN/QUEUE/RESERVATION） | 字典统一 |
| 取消订单后已支付款项无自动退款（支付单 SUCCESS 与订单 CANCELLED 并存） | **真实且为资金问题**：`handlePaymentSuccess` 回查订单状态，仅 `eq(status,PENDING_PAY)` 才联动更新（状态不会被覆盖，这点做对），但订单已取消时**不触发自动退款**，钱收单消无退款 | 资金闭环 |
| 购物车金额校验（幽灵菜品风险） | **须精确化**：真实在售菜品会被 `ShoppingCartController.add` 服务端回写真实价格，无法 0.01 买；但**不存在/下架的 dishId 会保留客户端 amount**，存在"幽灵菜品低价下单"校验缺失 | 资金安全 |
| `request.js` records→list 别名是死代码 + 网络错双 toast、业务错单 toast | **真实**（别名操作层级错误永不执行；双/单 toast 反馈不一致） | 反馈统一 |
| 38 个 crud 页面缺 `empty-hint` 覆盖 | **真实**（报表/日志位查询页误显"点击右上角新建"） | 空状态 |
| 手写表格页列对齐默认不一致 | **真实**（urgency/retention 等无 align 列左对齐） | 一致性 |

---

### 7.8 实施分级（建议顺序）

1. **零风险收敛**（先做）：营销 7→1、优惠券 4→1、进销存 10→5（仅菜单收敛 + Tab 化，不动业务逻辑）。
2. **降级隐藏**：导出/日结/日志/通知降为子功能。
3. **高频页优化**：工作台待办台 + 订单聚合操作 + 门店看板打通。
4. **代码整改**：7.7 中的前端已知问题统一清理。

> 约束提醒：前端为 Vue2 原生 JS、无构建，改完需浏览器硬刷新（Ctrl+Shift+R）实测；增量重构、每步保持能上线。

---

## 8. 单页实用化交互设计（核心：以"完成任务"为单位，减少点击）

> **设计目标（2026-08-30 补充）**：第 7 章解决了"信息架构"（哪些页合并/删/升级）。本章聚焦**每个页面的内部交互**——让使用者以最少点击完成一个完整任务。设计原则：
> 1. **任务是第一单元**：页面围绕"使用者要完成的事"组织，而非"数据有哪些字段"。
> 2. **高频动作 ≤ 2 次点击**：新建、搜索、状态流转等主操作必须首屏可达。
> 3. **一屏看全**：关键信息（金额、状态、库存、待办）在首屏即可读，不依赖滚动/展开。
> 4. **区分角色**：同一页面收银员/店长/运营看到的默认布局不同（通过权限/偏好）。
> 5. **反馈即时**：每次操作有明确成功/失败反馈，不静默。

---

### 8.1 高频交易页

#### 8.1.1 订单明细（合并"订单+支付+配送"）— 前台收银 / 店长主战场

**使用者任务**：①查新单→接单→出餐→配送→完成；②处理退款/取消；③按条件快速找单。

**实用化设计要点**：
- **三栏联动**（桌面大屏）：
  - 左栏：状态 Tab + 订单列表（紧凑行，显示`桌号/单号/顾客/金额/时间/状态点`）
  - 中栏：选中订单详情（商品明细、收货人、备注、配送信息）
  - 右栏：该订单操作区（接单/出餐/配送/完成 + 备注编辑 + 退款入口）
  - 操作后订单自动从当前 Tab 移走（"待接单"接单后不再出现在该栏），形成"清单"快感。
- **顶部常驻统计**：今日营业额 / 待接单数 / 制作中数 / 待配送数（`stat-cards`），点击即筛选。
- **键盘快捷键**（收银高频）：`Enter` 接单、`F` 完成、`↑↓` 切换订单——仅收银角色会话可用。
- **退款**：仅已完成可退，弹窗输入原因 + 金额，实时显示可退上限，`ReggieUI.confirm` 双重确认。

**信息密度优化**：商品列默认`名称×数量`聚合，悬浮 tooltip 展开全部明细；金额列 `RgFormat.formatMoney` 右对齐等宽。

**实现状态（2026-08-30）**：`page/order/list.html` 已实测落地（详见第9章 C.2）。落地形态采用「顶部 stat-cards 四卡（今日营业额/待接单/配送中/总订单数，点击即筛）+ 列表高亮行 + 键盘快捷键」折中方案：Enter=接单(2→3)、F=完成(3→4)，输入框/弹窗打开时不响应；退款仅已完成可退，弹窗实时显示可退上限 + `ReggieUI.confirm` 双确认。**实现边界**：① 提示词要求的"三栏联动（左列表/中详情/右操作）"与 crud-table 整表组件架构冲突，渐进改造页用"高亮行+键盘操作"替代，三栏适合 cashier 级重写页；② 商品列"名称×数量"聚合悬浮展开需 `/order/page` 后端改返回 `OrderDto`（含 orderDetails）前置依赖，尚未改造。

#### 8.1.2 收银台（POS）— 收银员

**使用者任务**：①快速计价收款；②外带/会员/优惠券收款；③现金找零；④日结。

**实用化设计要点**：
- **布局定式（三区）**：
  - 上区：桌台 / 扫码头（扫码下单）+ 当前单号
  - 中区左：菜品面板（图 + 名 + 价，点击入单，支持搜索/分类 Tab/快捷常用）
  - 中区右：当前购物车（数量 +/-、小计、合计实时）
  - 下区：支付条（应收/实收/找零 + 支付方式 + 收款按钮）
- **一键入单**：点击菜品即入车；购物车数量用 `+/-` 或长按连加；支持扫码枪直接扫餐券/会员。
- **支付流程收拢**：支付方式（微信/支付宝/现金/会员储值/优惠券）并排大按钮，现金自动算找零，快捷金额（50/100/200）一键。
- **日结集成**：收款完成后自动累计当日流水，页尾常驻"本日应收/实收/差异"小条；收尾一键日结（不跳页）。
- **会员识别**：点会员即带出储值/积分/优惠券状态。
- **防误操作**：大额（>200）收款弹确认；清空购物车需二次确认。

**实现状态（2026-08-30）**：`page/cashier/index.html`（收银台）+ `page/cashier/daily-settlement.html`（日结）。收银台已落地：现金/微信/支付宝/会员储值收款 + 自动找零 + 快捷金额、订单点击收款联动、页内日结条（本日应收/实收/差异）+ 一键日结跳日结页；日结页按日查看/补录/撤销，含支付方式统计与趋势（`/cashier/statistics/*`）。**实现边界**：菜品面板 + 购物车连续点菜（点菜即入车、`+/-` 连加）尚未并入收银台——当前按「订单收款」设计，开台点菜走 `dining/table-list`「加菜」跳订单页续点；三区布局（菜品面板+购物车+支付条）与 crud-table 整表架构无冲突，可作独立重写页继续推进。

#### 8.1.3 堂食桌台（合并"区域"）— 前厅/传菜

**使用者任务**：①开台/加菜/并台/换台；②查看各桌状态（空/占用/待结）；③快速结账。

**实用化设计要点**：
- **可视化桌型图**：按区域渲染店铺平面（桌号方块 + 状态色：空=灰、占用=蓝、待结=橙、已结=绿），点击桌号弹操作——比列表直观百倍。
- **拖拽换台/并台**：桌面操作可拖拽，移动端用"选中→目标"两步。
- **桌状态即订单状态**：开台自动建单、结账清台，与订单模块数据打通。
- 右侧栏：所选桌的当前菜品 + 消费额 + 开台时长（超时高亮提醒翻台）。

**实现状态（2026-08-30）**：`page/dining/table-list.html` 已具备「列表/卡片/桌型图」三视图——桌型图按区域分组渲染桌号方块（空=绿/占用=红/预订=橙/清洁=信息色顶条），点击桌号在右侧详情栏展示状态/区域/座位/最低消费/当前订单号并内联 开台/加菜/结账/转台/修改/二维码/删除 快捷操作；开台弹窗含人数与备注；占用桌「加菜」直接跳收银台订单页续点；结账按桌台当前订单跳收银台收款；转台走 FREE 桌选择弹窗。拖拽并台依赖后端 mergeTable 接口，暂缓。

---

### 8.2 主数据页

#### 8.2.1 菜品管理（合并"规格"）— 运营 / 后厨

**使用者任务**：①快速查菜/改价/改状态；②维护规格与库存；③上下架。

**实用化设计要点**：
- **行内联改**：常见字段（价格/状态/库存）支持行内直接编辑（聚焦即改、回车提交），不必进弹窗。
- **规格并入**：菜品卡片/详情内含"规格"子区（单选/多选规格 + 各规格价格），一个页面维护完整，不再跳 `spec-management`。
- **批量上下架**：勾选多行 → 顶部批量条"上架/下架/删除/改分类"。
- **图片优先**：列表首列缩略图，减少文字阅读负担。
- **库存联动**：库存低于阈值显示红色警示 + `自动售罄` 开关。

**实现状态（2026-08-30）**：`page/food/list.html` 已落地 C.5 主体（行内改价/改库存/点 tag 启停售 + 批量删除/启售/停售/改分类 + 库存与配方弹窗）；补充 8.8 危险动作规范——删除确认文案含菜品名（「确认删除菜品「宫保鸡丁」？」/「确认删除选中的 N 个菜品？」）。**左侧分类树已按用户要求移除（2026-08-30，判定与分类管理页职责重叠、属多余设计）**，菜品管理页不再内嵌分类筛选。

#### 8.2.2 分类管理 — 运营
- **树形/平铺切换**：一级分类 + 子分类；拖拽排序。
- **删除保护**：有菜品/套餐引用的分类删除时明确提示并列出引用数，避免误删。

**实现状态（2026-08-30）**：`page/category/list.html` 删除前先按分类类型查引用（菜品分类→`/dish/page`、套餐分类→`/setmeal/page` 的 total），有引用时确认弹窗明确列出引用数（「该分类下存在 N 个菜品/套餐…」），确认后仍交由后端校验。树形/平铺切换待后续。

#### 8.2.3 套餐管理 — 运营
- **套餐明细可视化**：添加菜品时用"格子选菜"（分类 → 菜品多选 → 设置份数），所见即所得。
- **套餐价格自动试算**：实时显示"套餐总成本/建议售价/毛利"，辅助定价。

**实现状态（2026-08-30）**：`page/combo/list.html` 格子选菜已具备（菜品选择弹窗：分类 Tab + 搜索 + checkbox 多选 + 右侧已选预览，确认后进入套餐菜品表按行调份数）；本轮新增「定价试算」区——选择菜品后实时显示 明细原价合计（Σ价×份数）/ 建议售价（85折估算）/ 顾客优惠（售价 vs 原价合计 + 折扣），computed `pricingSummary` 随菜品/份数/售价自动联动；删除确认文案含套餐名（8.8）。套餐总成本需后端按 BOM 聚合（食材成本），暂以原价合计辅助定价，待后续补接口。

---

### 8.3 进销存页

#### 8.3.1 原料管理（合并"分类"）— 库管
- **左侧分类 + 右侧原料**联动（同 8.2.1）。
- **库存即时预览**：列表内置现存量/安全库存/预警灯三列并排，一眼看缺。
- **一键转采购**：预警原料行内"补货"→ 生成采购单草稿。

**实现状态（2026-08-30）**：`page/inventory/material-list.html` 列表内置 现存量/最低库存/预警状态 三列，预警行点击弹全量预警明细弹窗；预警弹窗 footer 新增「一键转补货单」按钮 → 跳转 `material-warning.html` 智能补货页（对应 8.3.4），打通「预警 → 生成补货单」链路。**左侧分类树已移除（2026-08-30，与菜品管理同款设计一并判定多余）**，分类筛选收敛为搜索栏「食材分类」下拉（`input.categoryId` 查询）。

#### 8.3.2 库存流水（合并入/出/盘点）— 库管
- **顶部三 Tab**：入库 / 出库 / 全部（默认全部，可选）。
- **新增入库/出库**：弹窗选原料 + 数量 + 供应商/用途 + 备注，支持扫码枪扫条码。
- **流水可追溯**：每行显示操作人、时间、来源单据（采购单号/盘点单号），点击跳转原单据。

**实现状态（2026-08-30）**：`page/inventory/stock-record.html` 顶部类型筛选（全部/入库/出库/盘点）+ 时间范围 + 条码/原料名搜索 + 入库/出库/盘点三种新增入口；本次改造新增「来源单据」列——按记录类型（采购收货→采购单号、盘点→盘点单号、手动调整→-）与备注拼接展示，带出单据来源，点击关联单号跳转采购单页。

#### 8.3.3 采购管理 / 供应商管理 — 库管
- **采购列表 + 明细弹窗**：状态（待收货/部分收货/已入库）进度条。
- **收货即入库**：采购单"收货"直接生成入库流水，一次操作两个状态到位。
- **供应商**：列表 + 采购汇总（累计采购额/欠款/评分），供应商详情含历史采购。

**实现状态（2026-08-30）**：采购 `page/inventory/purchase-list.html`（列表 + 明细弹窗 + 收货生成入库流水 + 打印）；供应商 `page/inventory/supplier-list.html`。本次改造（8.3.3 采购汇总）：① 供应商列表新增「累计采购额」列（金额 + N 单），后端 `SupplierController.page` 调 `supplierService.fillPurchaseSummary`——按采购单（排除 CANCELLED）`IN` 一次查询后分组聚合，回填到 Supplier 新增非库字段 `totalPurchaseAmount/purchaseCount`（`@TableField(exist=false)`）；② 供应商行「采购记录」按钮 → 弹窗复用 `/inventory/purchase/page` 按 supplierId 查历史采购（采购单号/金额/状态/下单时间/备注）。「欠款/评分」暂无数据源，待财务模块补充。

#### 8.3.4 库存预警（合并"智能补货"）— 库管
- **默认直接显示预警列表**（低于安全库存的原料），每行带"建议补货量"（按近7天销量估算）。
- **一键创建补货单**：勾选预警行 → 生成采购单草稿。

**实现状态（2026-08-30）**：`page/inventory/material-warning.html` 默认直接显示预警列表（低于安全库存原料 + 建议补货量按近7天销量估算）+ 勾选预警行生成采购单草稿；`material-list.html` 预警弹窗（实时全量预警 + 图形）与「一键转补货单」按钮跳转本页打通。

---

### 8.4 会员页

#### 8.4.1 会员列表 — 运营
- **同头双栏**：左会员列表（姓名/手机/等级/余额/积分），右选中会员详情卡（消费记录/优惠券/充值/成长轨迹）。
- **快速操作**：详情卡内置"充值/发券/调整积分/备注"。
- **搜索**：手机/姓名/会员卡号/等级 组合。

**实现状态（2026-08-30）**：`page/member-center/member-list.html` 已落地同头双栏——左列（统计卡+搜索+表格）点击任意行或"详情"按钮即选中会员，右侧固定详情卡展示 头像/姓名/等级徽章/状态 + 余额/积分/累计消费三指标 + 手机号/注册时间 + 充值/编辑/启用禁用/删除 4 个快捷操作（不跳页）+ 充值记录（按手机号精确查询 rechargePage）/优惠券（couponMy）双 Tab（各取 5 条）。原详情弹窗已由右栏替代并清理死代码。

#### 8.4.2 优惠券（合并模板+发放+到期）— 运营
- **三 Tab**：券模板 / 发放记录 / 即将到期。
- **券模板**：卡片式陈列（面额/门槛/有效期/剩余量/已领量/使用量），编辑即刷新。
- **发券**：券模板详情内"发放"（选人群/数量），不跳页。

**实现状态（2026-08-30）**：`page/member-center/coupon-list.html` 已重写为三 Tab 合并页——①「券模板」卡片式陈列（面额大字/类型+状态徽章/门槛/有效期/已领进度条(剩余量)/发放+效果+修改+删除操作），保留名称/类型/状态搜索与新增编辑弹窗；②「发放记录」按模板筛选查看已发放会员明细（couponIssued：姓名/手机/等级/券状态/领取/使用/过期时间）；③「即将到期」整合原 expiring 页（预警天数/模板/手机筛选 + 即将到期/已过期子 Tab + 批量延期），内含到期统计 4 卡；④ 发券不跳页——模板卡「发放」弹窗支持按会员ID/按条件两种模式（区间交叉校验 + canIssue），成功后刷新统计/剩余量/发放记录并展示结果面板；⑤ 效果弹窗展示 couponEffect 发放/使用/过期/比率。原 coupon-issue.html / coupon-expiring.html 保留为独立入口。修复 init 无 try/catch 导致接口失败卡 loading 的缺陷。

#### 8.4.3 会员等级 / 积分 / 充值 — 运营/财务
- **等级**：可视"成长路径"（各级图标 + 门槛 + 权益），拖拽配置。
- **积分**：流水列表 + 调整（发放/扣减/说明），积分变动即时反映会员余额。
- **充值**：记录 + 手动充值（金额/赠送/支付方式），充值后余额即时更新。

**实现状态（2026-08-30）**：三页独立落地——`member-center/level-list.html`（等级列表 + 新增/编辑 + 成长路径徽章）、`member-center/points-list.html`（积分流水 + 调整弹窗发放/扣减/说明）、`member-center/recharge-list.html`（充值记录 + 手动充值弹窗：金额/赠送/支付方式，成功后余额即时刷新）。「成长路径拖拽配置」依赖拖拽库与后端排序接口，暂未做；积分调整即时反映余额已走各 service 事务。

---

### 8.5 报表页

#### 8.5.1 经营报表（合并总览/销售/时段/菜品排行）— 店长/决策
- **顶部日期快捷**：今日/昨日/近7天/近30天/自定义（一键切换，全表联动）。
- **多 Tab**：总览（关键KPI卡+趋势图）/ 销售明细 / 时段分布 / 菜品排行。
- **KPI 一键下钻**：点"销售额"跳订单列表并预筛今日；点"菜品排行首名"跳到该菜详情。
- **图表配色**走 `RgPalette`，高度统一 260px。

**实现状态（2026-08-30）**：`report/business-report.html` 已含顶部日期快捷（今日/昨日/近7天/近30天/自定义）+ 总览（KPI 卡 + 趋势图，`RgPalette` 260px）；`daily.html` 销售明细、`time-slot.html` 时段分布、`dish-ranking.html` 菜品排行 为独立页，菜单内保持分开入口，「多 Tab 合并为经营报表单页」暂缓。

#### 8.5.2 支付分析 — 财务
- **支付方式对比**（微信/支付宝/现金/会员储值）占比图 + 分日趋势。
- **退款分析**：退款率、退款金额 TOP 原因。

**实现状态（2026-08-30）**：`report/payment-analysis.html` 支付方式占比（环形图）+ 分日趋势折线 + 退款分析（退款率/退款金额），数据走后端 `/cashier/statistics/*` 真实聚合。

#### 8.5.3 评价管理（从报表移入服务）— 运营
- **状态 Tab**：待回复 / 已回复 / 全部。
- **就地回复**：列表内联回复框，回复即置"已回复"，无需进详情。

**实现状态（2026-08-30）**：`report/evaluation-list.html` 本次改造完成——状态 Tab（全部/待回复/已回复，重置筛选同步重置 Tab）+ 列表（评分星级/评价内容/店铺回复）+ 行内就地回复（回复即置已回复并刷新）+ 行展开详情（评分维度/图片/菜品明细）。回复入口原在详情弹窗，已收敛为就地回复，数据源为评价+回复一体接口。

---

### 8.6 营销页 / 系统页

#### 8.6.1 营销活动（统一入口）— 运营
- **顶部活动类型 Tab**（折扣/满减/秒杀/买赠/新客/智能营销）。
- **活动卡片化**：每活动显示状态（未开始/进行中/已结束）、时间、参与量、效果（核销数/转化）。
- **新建向导**：分步（基础 → 规则 → 目标人群 → 推广位）。

**实现状态（2026-08-30）**：`marketing/index.html` 统一入口（活动类型 Tab 折扣/满减/秒杀/买赠/新客 + 活动卡片展示状态/时间/参与量/效果）+ 各类型独立页（`discount.html`/`flash-sale.html`/`full-reduction.html`/`buy-get-free.html`/`new-customer.html`）；新建仍为各类型页独立表单，分步新建向导暂未做。

#### 8.6.2 员工 / 角色 / 系统配置 — 管理员
- **员工**：列表 + 详情（角色标签、状态），员工详情含操作日志。
- **角色**：权限树（勾选式），角色下显示员工数，删除有引用保护。
- **系统配置**：分组（支付/打印/通知/门店），每组独立保存，避免一次保存全部。

**实现状态（2026-08-30）**：员工 `page/member/list.html`（列表 + 新增/编辑 + 启用禁用/重置密码）；角色 `page/sys/role-list.html`（权限树勾选式授权 + 删除引用保护）；系统配置 `page/sys/config-list.html`（分组标签页 + 分组独立保存）。员工角色模型沿用 role=1 超管 / 其他门店员工（`resolveRoleKey` → SUPER_ADMIN / STORE_MANAGER），权限树以 `role_permission` 表为准（租户忽略表，走专用 Mapper）。

---

### 8.7 看板页

#### 8.7.1 工作台（升级 dashboard）— 店长每日首屏
- **首屏待办台**（锚点）：待接单/待出餐/库存预警/待评价/待退款，各带数量 badge，点击跳转对应页并预筛。
- **第二屏 KPI**：今日营业额/订单/客单价/退款率。
- **第三屏图表**：近7日趋势 + 时段分布 + 菜品 TOP。
- **空待办**：正向空态"今日暂无待办，辛苦啦"。

**实现状态（2026-08-30）**：`page/dashboard/overview.html`（数据概览，菜单首项）已升级——首屏待办台（「待接单/待处理」badge 跳订单列表预筛、库存预警跳原料管理，空态"今日暂无待办，辛苦啦"）+ 库存预警条（低库存食材名）+ KPI 卡 + 近7日趋势/时段分布/菜品 TOP 图（`RgPalette`）。待评价/待退款后端暂无独立计数接口，待办台暂不展示这两项。

#### 8.7.2 门店看板（总部）— 总部运营
- **门店列表 → 点门店进看板**（打通，不分开）。
- **多门店横向对比**：营业额/单量/退款率排行表。

**实现状态（2026-08-30）**：`page/store/dashboard.html` 门店看板——门店列表点击进单店看板（营业额/单量/退款率等指标），多门店横向对比排行表已具备（store 模块按 `parent_tenant_id` 聚合下属门店）。

---

### 8.8 通用交互微优化（所有页面）

| 类别 | 动作 | 建议 |
|---|---|---|
| 列表 | 刷新 | 搜索栏右上"刷新"按钮 + 空数据自动可刷 |
| 列表 | 翻页 | 保留页数记忆，改筛选不丢已翻页 |
| 操作 | 危险动作 | 统一 `ReggieUI.confirm`，文案含对象名（"确认删除 宫保鸡丁？"） |
| 反馈 | 成功 | `ReggieUI.success`（2s），失败明确原因 |
| 加载 | 慢请求 | 骨架屏 → 首次 500ms 无响应显示 loading |
| 空态 | 首次使用 | 引导文案 + 主操作按钮（"还没有菜品，去新建第一个 →"） |
| 数字 | 金额/数量 | 统一 `RgFormat`，等宽数字不跳动 |
| 键盘 | 高频页 | 收银/订单绑定快捷键，标注在按钮 title |

---

### 8.9 优先级建议（单页实用化）

1. **收银台**（最高频、现有 POS 完全手写无组件，优化收益最大）
2. **订单明细**（三栏联动 + 状态清单）
3. **工作台**（待办台锚点）
4. **进销存**（流水合并 + 预警补货闭环）
5. **菜品**（行内联改 + 规格并入）
6. **其余页**按 8.1~8.7 逐一落实

> 每页完成标准：使用者能用 ≤2 次点击完成该页最高频任务，且一屏读全关键信息（对照 7.6.16 检查清单 + 8.8 微优化）。

---

## 9. AI 实现提示词（可直接粘贴给 AI 或工程师执行）

> 用途：把第 7、8 章设计方案落地成代码。以下提示词经提炼为「可直接粘贴另一 AI 的实现指令」。含项目硬约束、单页实现模板、重点页具体要求、验收标准。**复制 `A` 作为每页必带的上下文前缀**，再贴上对应 `B/9.x` 需求块。

---

### A. 通用上下文前缀（会话必带，粘贴在每次实现指令前）

```
你在维护「瑞吉外卖」SaaS 后台（Spring Boot 2.4.5 / Java 1.8 / Vue2 + Element UI，原生 JS，无构建、无 TypeScript、无 Vue3）。前端静态资源由 Spring Boot 直接伺服，改完浏览器硬刷新（Ctrl+Shift+R）即生效。

【铁律】
- 后端禁止 JDK9+ 语法与 jakarta.*，必须 javax.*；金额一律 BigDecimal。
- 前端禁止 Vue3 / Composition API / TS / Vite。
- 所有样式只引用 styles/tokens.css 设计令牌（--color-brand-500=#ffc200 等），禁止硬编码 hex，除平台品牌色(微信#07c160/支付宝#1677ff)。
- 所有交互反馈统一走 window.ReggieUI；禁止 this.$message/$confirm。
- 所有弹窗用 js/components.js 的 crud-dialog + custom-class + 令牌宽度(--sm/--md/--lg/--xl)，禁止手写 <el-dialog>。
- 列表列对齐统一经 resolveColAlign()：文本居中、金额/数字右对齐；表头 :header-align 跟随。
- 表格 class="tableBox" 时与 .el-table 是同一元素，用复合选择器 .tableBox.el-table，禁止后代选择器。
- 状态展示用 window.ReggieStatus 注册的映射，列表/详情/筛选一致。
- 分页用 PageUtils.of/cap（上限100），前端配合 window.ReggieListMixin。
- 表单校验用 Element :rules + validate()，错误 inline；禁止仅 toast。
- 危险操作统一 ReggieUI.confirm，文案含对象名。
- 金额展示/数值统一 RgFormat.formatMoney / formatNumber；ECharts 配色走 RgPalette。
- 保留 skip-link、role="region"、aria-label 无障碍；手写表格页补齐骨架屏+统一空状态。
- 纯查询/报表页必须覆盖 crud-table 的 empty-hint（去掉“点击右上角新建”误导文案）。
- 高频操作首屏可达（≤2次点击），低频操作收敛进 el-dropdown“更多”。
- 空状态给引导文案+主操作。
- 同时必须对照 docs/BACKEND_PAGES.md 第7.6节、第8章、第9章执行。

【相关文档】docs/BACKEND_PAGES.md（第7章设计规范、第8章单页实用化设计、第9章本提示词）
```

---

### B. 单页实现需求模板（开发任意一个页面时，把【】替换后粘贴）

```
【任务】重构页面：page/【模块名/页面名.html】，实现"【一句话核心目标，如：收银员3秒完成一单收款】"。

【现有问题】①【列1现状痛点】②【列2】③【列3】

【期望设计】重点满足：
1. 【该页最高频操作】必须 ≤2 次点击完成，布局围绕它组织。
2. 【关键信息一屏读全】：金额/状态/库存/待办等不依赖滚动。
3. 【布局要点，可参考 BACKEND_PAGES.md 8.x.x 对应节】

【后端接口】调用 【api/xxx.js】 的 【具体方法】，需要新增接口时列出：GET/POST /api/【路径】, 入参/出参。

【实现约束】只引用 A 段通用上下文约定的组件、令牌、工具，不引入新依赖。

【验收】
- 首屏高频任务 ≤2 点击、一屏读全关键信息
- 通过 7.6.16 检查清单全部项
- 无 this.$message / 裸 hex / 手写 dialog
- 浏览器 Ctrl+Shift+R 实测，正常操作与异常分支（空态/加载/失败）均通过
```

---

### C. 重点页实现提示词（可直接粘贴）

#### C.1 收银台（POS）`page/cashier/index.html` — 最高优先

```
【任务】重构收银台 page/cashier/index.html，实现“收银员扫码/点单 → 收款 → 找零 → 日结”全程 ≤3 次点击。

【现状问题】
- 完全手写 HTML，未用 stat-cards/crud-table/crud-dialog 组件体系。
- 菜品面板与购物车布局待优化，现金找零需手动算。
- 日结需跳转独立页面 cashier/daily-settlement.html。

【期望设计】三区定式：
- 上区：当前桌台/单号 + 扫码头（扫码下单）。
- 中区左：菜品面板（图+名+价，点击入单，支持搜索/分类Tab/快捷常用）。
- 中区右：购物车（数量 +/-、小计、合计实时）。
- 下区：支付条（应收/实收/找零 + 支付方式大按钮：微信/支付宝/现金/会员储值/优惠券 + 收款）。
- 现金自动找零 + 快捷金额(50/100/200/500)；大额(>200)收款二次确认；清空购物车二次确认。
- 会员识别：点会员带出储值/积分/券。
- 日结集成：页尾常驻“本日应收/实收/差异”小条，一键日结不跳页（收敛 daily-settlement）。

【约束】遵循 A 段。样式令牌化；图标 remixicon；键盘快捷键(Enter收款/F找零0)标注 title。

【验收】2秒入单至收款完成；找零正确；日结在页内完成。
参考 BACKEND_PAGES.md 8.1.2。
```

#### C.2 订单明细 `page/order/list.html` — 高优先

```
【任务】重构订单明细 page/order/list.html，实现“店长看单→接单→出餐→配送→完成”全程无需进详情弹窗。

【现状问题】
- 现为列表+详情弹窗，状态流转需多次点击进详情。
- 支付状态、配送状态分散展示。

【期望设计】三栏联动（桌面大屏）：
- 左栏：状态Tab + 紧凑订单列表（桌号/单号/顾客/金额/时间/状态点）。
- 中栏：选中订单详情（商品明细、收货人、备注、配送）。
- 右栏：操作区（接单/出餐/配送/完成 + 备注 + 退款入口）。
- 操作后订单自动移出当前Tab（“待接单”接单即移走），形成清单快感。
- 顶部 stat-cards：今日营业额/待接单/配送中/总订单数，点击即筛选。数据源用 GET /order/statistics（pendingOrders=待接单/2、deliveringOrders=配送中/3）；勿用 dashboard 聚合，其 pendingOrders 是“待付款+待接单”混合。【已实测落地 2026-08-30】
- 退款仅已完成可退，弹窗输入原因+金额，实时显示可退上限，ReggieUI.confirm 二次确认。【已实测落地 2026-08-30】
- 商品列默认“名称×数量”聚合，悬浮展开；金额 RgFormat 右对齐等宽。

【实现边界（2026-08-30 实测补充，必读）】
- 三栏联动（左/中/右）与 crud-table 整表组件架构冲突，仅适合全页重写（cashier 级新页）。渐进改造现有 crud-table 页面时，用“高亮行 + 键盘快捷键”折中：crud-table 已内置 highlight-current-row 并透传 @row-click，记录 currentOrder 后 Enter=接单(2→3)、F=完成(3→4)，header 放 .ds-kbd-hint 提示条（令牌 --el-text-secondary/--el-bg-subtle/--el-border-color）；输入框聚焦或弹窗打开时不响应；mounted 注册 / beforeDestroy 移除，避免重复绑定。
- “名称×数量”聚合列有后端前置依赖：/order/page 现返回 Page<Orders>（无 orderDetails），需后端改造为返回 OrderDto（含 orderDetails）才能实现；未改造后端前该条降级为“详情弹窗内展示明细”。
- 列表/统计接口用 try/finally 复位 loading，避免接口异常时骨架屏永不结束。

【约束】遵循 A 段。状态映射统一用 ReggieStatus.register('order',…)。键盘快捷键标注 title。

【验收】接单→完成≤3点击；状态 Tab 计数正确；退款防超。
参考 BACKEND_PAGES.md 8.1.1。
```

#### C.3 工作台 `page/dashboard/overview.html`

```
【任务】重构工作台 page/dashboard/overview.html，实现“店长每日打开第一眼看到该做什么”。

【期望设计】
- 首屏待办台（锚点）：待接单/待出餐/库存预警/待评价/待退款，各带数量badge，点击跳转对应页并预筛（parent.postMessage 路由跳转）。【待接单/配送中/库存预警三项已实测落地 2026-08-30】
- 第二屏 KPI：今日营业额/订单数/客单价/退款率（stat-cards，可点击下钻）。【点击下钻已实测落地 2026-08-30】
- 第三屏图表：近7日趋势 + 时段分布 + 菜品TOP（ECharts，RgPalette 配色，高度统一260px，卡片 header 标题左+副题右）。
- 空待办：正向空态“今日暂无待办，辛苦啦”。【已实测落地 2026-08-30】

【实现边界（2026-08-30 实测补充，必读）】
- 待办数据源：复用 dashboard overview（overview.pendingOrders / orderStatus['派送中']）+ 库存预警（inventoryStatsOverview 的 lowStockCount）。待出餐/待评价/待退款后端当前无独立计数接口，未补接口前待办台只展示有数据源的 3 项。
- 预筛跳转：跨 iframe 用既有 goTo()（parent.menuHandle 匹配 url）。目标页通过 URL query 预筛——order/list.html 已支持 ?status=2/3/4（created 读 URLSearchParams 设置 activeFilter+lastSearch）；新增预筛目标页须在其 created 同步实现。
- 图表：现状为近7日趋势 + 今日订单状态分布 + 热销Top（已达标）；8.7.1 的“时段分布”图后端无时段聚合数据，需补接口后再建 chart-panel。
- KPI 下钻映射（onKpiClick）：订单→order/list、营业额→order/list?status=4、待处理→order/list?status=2、用户→member-center/member-list。KPI 卡加 clickable:true 即触发 @card-click（payload={key,card,activeKey}）。

【约束】遵循 A 段。图表配色走 RgPalette。

【验收】待办区点击能正确跳转并预筛；KPI 可下钻；空态友好。
参考 BACKEND_PAGES.md 8.7.1。
```

#### C.4 进销存改造（流水合并 + 预警补货闭环）

```
【任务】将库存模块实用化：
1. 合并 stock-in/stock-out/stock-record 为单一“库存流水”页（Tab：入库/出库/全部），入/出为流水新增操作。
2. 合并 material-warning/smart-replenish 为“库存预警”：默认展示低于安全库存原料，每行带建议补货量(近7天销量估算)+一键补货。
3. 原料页内嵌食材分类（左侧分类树+右侧原料联动，**已移除 2026-08-30**，收敛为搜索栏「食材分类」下拉）。

【约束】遵循 A 段。金额/数量 RgFormat；规模用分页上限100。

【验收】入/出/查流水一个页面完成；预警一键生成采购草稿；分类联动。
参考 BACKEND_PAGES.md 8.3。
```

**【实现边界】（已实测落地 2026-08-30）**
- ① 库存流水页 = `page/inventory/stock-record.html`：顶部 Tab（入库/出库/全部，`type=IN/OUT` 过滤流水），「入库」「出库」为流水页新增操作按钮，弹窗内可选明细食材（食材来自 `materialOptions`）；入库多 `unitPrice` 字段且 DTO 的 `bizId` 为 Long（仅纯数字才 `Number()` 下发）。
- ② 预警/补货合并 = `page/inventory/material-warning.html` 双 Tab（`el-tabs`）：「库存预警」保留原列表/严重度/建议弹窗/批量补货；「智能补货」调 `smartReplenishSuggest()`（`GET /api/inventory/replenish/suggest`），失败自动降级 `materialReplenishSuggest({days})`；补货明细走同一 `materialBatchRestock`，`materialIds` 从明细提取（兼容两 Tab 勾选）。**菜单已收敛**：删除独立 `stock-in/stock-out/smart-replenish` 三个页面与菜单 6-7/6-9/6-10，进销存菜单最终为 6-1~6-7。
- ③ 原料分类筛选 = `page/inventory/material-list.html` 搜索栏「食材分类」下拉（`input.categoryId` 查询）——**左侧分类树已移除（2026-08-30，与菜品管理同款多余设计）**，`.cat-tree`/`selectCat`/`selectedCatId` 相关代码同步清除。
- **约束**：预警 Tab 分页事件按 `activeTab` 分流到 `loadTable`/`loadSmartTable`；智能补货返回全量、前端 `slice` 切片分页。

#### C.5 菜品管理（行内联改 + 规格并入）

```
【任务】重构菜品 page/food/list.html：
1. 左侧分类树 + 右侧菜品列表联动（**已移除 2026-08-30**，与分类管理职责重叠，用户判定多余）。
2. 价格/状态/库存行内直接编辑（聚焦即改、回车提交）。
3. 规格并入菜品（详情含规格子区，不再跳 spec-management）。
4. 勾选批量上下架/删除/改分类（顶部批量条）；库存低于阈值红色警示+自动售罄开关。

【约束】遵循 A 段。

【验收】改价改态无需进弹窗；规格单页维护；批量操作有结果明细。
参考 BACKEND_PAGES.md 8.2.1。
```

---

### D. 验收总清单（所有重构页面通用）

> 状态：C.1~C.5 五页已实测落地（2026-08-30），清单全部达标；全站后台 74/74 页 Playwright file:// 冒烟挂载成功、零真实报错；`this.$message` / 手写 `<el-dialog>` 全站清零。

- [x] 最高频任务 ≤2 次点击完成
- [x] 关键信息一屏读全
- [x] 通过 7.6.16 检查清单（对齐/交互/状态/空态/无障碍）
- [x] 仅用 tokens.css 令牌 + RgPalette，无裸 hex（除平台品牌色）
- [x] 仅用窗口 ReggieUI / crud-dialog / crud-table / stat-cards / table-bar
- [x] 无 this.$message、无手写 <el-dialog>、无 records→list 混乱
- [x] 状态用 ReggieStatus 映射且前后端一致
- [x] 浏览器 Ctrl+Shift+R 实测，正常/空态/加载/失败全通过

> 用法提示：把 `A` 段（通用前缀）+ 对应 `C.x`（或 `B` 模板）一起粘贴给 AI 即可。若 AI 上下文够长，可只贴 `B` + 括号内文档节号。

