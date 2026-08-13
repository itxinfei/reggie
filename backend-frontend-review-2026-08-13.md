# 后台管理系统前端全面审查与优化分析

> 审查对象：`src/main/resources/backend/`（约 64 个 HTML 页面 + 20+ 个样式文件 + `js/components.js` 组件库）
> 审查方式：直接阅读实际代码（HTML 结构 / CSS 级联 / 组件库 / 配置文件），逐文件核实，不依赖旧报告
> 审查日期：2026-08-13
> 审查依据：项目既定规范（`AGENTS.md` 前端规范、"禁止渐变发光"硬规范、`tokens.css` 设计令牌、列对齐"尽量都居中"要求）

---

## 一、执行摘要

读完实际代码后，结论是：**这套后台在"工程架构"上已经相当成熟**（组件化 `crud-table`/`table-bar`/`stat-cards`/`crud-dialog`、设计令牌、一致性层、a11y 基础设施都到位，旧审计里"操作列按钮截断""列宽写死"等问题已在 `components.js` 内修复），但**在"视觉一致性"和"弹窗交互"上仍明显拉胯，整体观感不佳、操作体验差的根因集中在三处**：

1. **样式体系仍存在"两套规范打架"**：一份新规范（`design-system.css`/`tokens.css`：圆角 ≤12px、禁止发光）与一份更激进的增强层（`unified-components.css`：圆角 16px、玻璃拟态 `backdrop-filter`、按钮悬停发光）同时生效，且后者在多处把前者的硬规范推翻。结果是**全站圆角 12/16 混用、卡片带玻璃模糊光晕、表格中文表头字距异常**。
2. **弹窗（crud-dialog 迁移）接线不完整**：组件库升级为 `crud-dialog` 是逐步逐页做的，部分页面（菜品新增/修改弹窗、套餐"添加菜品"弹窗）**只换了外壳、忘了把"确定"按钮接到提交逻辑**，导致这些弹窗的保存/确认动作失效。
3. **页面两极分化**：以 `food/list.html`、`category/list.html` 为代表的"现代化页面"已全面组件化、令牌化；以 `combo/add.html` 及大量使用裸 `el-dialog` 的页面为代表的"遗留页面"仍堆内联 `<style>`、硬编码色值/宽度、甚至用橙色强调色顶替金色品牌色。

下面按"严重度 + 你最关心的弹窗"展开，**每条均给出文件路径与行号**。

---

## 二、已确认的核心问题

### 🔴 P0 — 功能性 / 交互缺陷（直接不可用，最优先修）

#### P0-1. 菜品"新增/修改"弹窗的保存动作未接线（疑似回归）
- **位置**：`page/food/list.html:103-109`（`<crud-dialog>` 菜品弹窗）与 `:463`（`submitDishForm` 方法）。
- **现象**：该弹窗**既没有 `@submit`，也没有 `#footer` 插槽**，只能依赖 `crud-dialog` 的默认页脚（"确定/取消"按钮，点击"确定"仅 `emit('submit')`）。但父页面没有任何 `submitDishForm` 的调用点（方法定义了却从不被触发），顶栏 `ds-page-header__actions` 也是空的。
- **后果**：用户填完表单点"确定"→ 事件无人接收 → 弹窗要么不动、要么只能"取消"关闭 → **菜品无法保存**。
- **对照**：`page/order/list.html:183` 正确写了 `@submit="submitRefund"`，`page/category/list.html:121-132` 用 `#footer` 插槽显式接了 `submitForm()`。说明这是**迁移到 `crud-dialog` 时个别页面漏接**，非全局问题。

#### P0-2. 套餐"添加菜品"弹窗的确认动作未接线（疑似回归）
- **位置**：`page/combo/add.html:267-348`（`<crud-dialog title="添加菜品">`）与 `:523`（`addTableList()` 方法）。
- **现象**：弹窗里是双栏菜品选择器 + 勾选列表，但**没有"添加/确定"按钮去调用 `addTableList()`**（原 `<AddDish>` 组件已被注释掉，`:277`）。`crud-dialog` 默认页脚的"确定"同样 `emit('submit')` 无人接收。
- **后果**：用户勾选菜品后**无法把所选菜品提交进套餐**，弹窗等于摆设。
- **旁证**：该页 `<style>`（`:12-172`）还保留大量遗留硬编码（见 P3-3），基本没纳入现代化改造，属典型"两极分化"页面。

> 修复方式很简单：在这两个弹窗补 `@submit="submitDishForm()"` / `@submit="addTableList()"`，或补 `#footer` 插槽。建议**全站排查一次 `crud-dialog` 的"确定"是否都被 `@submit` 接住**（grep `@submit` vs `<crud-dialog` 出现次数即可快速定位）。

---

### 🟠 P1 — 视觉规范冲突（违反项目既定"硬规范"）

项目明文硬规范（见 `tokens.css:405-406` 与 `AGENTS.md`）：**圆角仅 4/8/12px、禁止渐变/发光、颜色一律引令牌**。但以下规则正在内部打架：

#### P1-1. 圆角超规范：16px（`--radius-xl`）广泛存在
- `tokens.css:90` 定义了 `--radius-xl: 16px`（本就超出"≤12px"硬规范）。
- `design-system.css:498` 与 `unified-components.css:685` 给 `.el-dialog` 用 `--radius-xl` → **所有弹窗 16px 圆角**。
- `unified-components.css` 还给 `.dashboard-container .container`(`:529`)、`.toolbar`(`:540`)、`.stats-card`(`:573`)、`.chart-card`(`:643`)、`.table-card`(`:671`) 一律 16px。
- 与此同时 `design-system.css` 的 `.el-table/.el-card/.ds-card` 用 `--radius-lg`(12px)。
- **后果**：全站卡片/弹窗圆角 **12px 与 16px 混用**，无统一视觉节奏。建议把对话框与容器统一收敛到 12px（`--radius-lg`），并删除 `--radius-xl` 或回归令牌。

#### P1-2. 玻璃拟态发光：违反"禁止渐变发光"
- `unified-components.css:534`：`.dashboard-container .container { backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.8); }` —— 典型玻璃光晕。
- `design-system.css:780`：`.el-loading-mask { backdrop-filter: blur(4px); }` —— 加载遮罩也发光。
- 由于 `consistency.css` 最后加载但**并未重置 `backdrop-filter`**，这个模糊光晕**实际仍然生效**（全站内容卡片都带一层毛玻璃）。
- **后果**：与"禁止发光"硬规范直接冲突，且毛玻璃在浅灰底（`--bg-page:#f5f6fa`）上几乎看不出层次，反而显得"脏"。建议直接删除这两处 `backdrop-filter`。

#### P1-3. 表格中文表头字距异常（全站 30+ 列表页）
- `design-system.css:450` 与 `unified-components.css:791`：`.el-table th { text-transform: uppercase; letter-spacing: 0.5px; }`。
- **`text-transform:uppercase` 对中文无效但无伤**；**`letter-spacing:0.5px` 却对中文生效** → 所有表头变成"菜 品 名 称""操 作 时 间"的松散字距，观感廉价、扫读吃力。
- **这是全站级缺陷**（所有用 `crud-table`/手写 `el-table` 的页面都中招）。建议表头移除 `letter-spacing`，`text-transform` 仅对非中文场景保留或干脆删除。

#### P1-4. 按钮中文文字字距异常（全局）
- `design-system.css:401`：`.el-button { letter-spacing: 0.5px; }` → "确 定""取 消""新 建"等按钮文字被人为拉开字距，同样显廉价。建议移除。

#### P1-5. 直角遗留未根治（旧根因仍在）
- `common-fixes.css`(`:87,152,157,165,173`) 与 `common-el.css`(`:652,707,715,723,10168` 等) 仍含 **`border-radius:0`** 与游离 `font-size:16px`。它们经 `main.css:26`(`common.css`) 仍被引入，处于级联中段，多数被后加载的 `design-system/unified/consistency` 覆盖，但**凡是没被后续规则覆盖的遗留选择器，依然渲染成直角**。
- 这正是旧审计"多套风格并存"根因的残留。**彻底方案是弃用 `common-fixes.css`/`common-el.css` 中的冲突声明**（或将其直角规则改为 `@deprecated` 注释后全量移除），而不是靠后文件一个个覆盖。

---

### 🟡 P2 — 弹窗设计缺陷（你重点要求的维度）

下面按"尺寸 / 层级 / 交互逻辑 / 信息密度"四个子维度系统梳理。

#### 2.1 尺寸（Size）
- **缺乏统一尺寸体系**：`crud-dialog` 仅有 `sm(420)/md(560)/lg(720)/xl(840)` 四档，但大量页面仍用**裸 `el-dialog` 并手填 width**：`30%`/`50%`/`600px`/`800px` 等取值散落（裸 `el-dialog` 出现在 `combo/list`、`store/list`、`payment/order-list`、`delivery/range-management`、`marketing/*`、`cost/*`、`cashier/*` 等数十处，grep `el-dialog` 出现 ≥2 次的页面就有 30+）。
- **对话框与内容不匹配**：
  - `combo/add.html` 的"添加菜品"弹窗未传 `size` → 默认 `md=560px`，但内容是**双栏**（左 60% 分类+菜品勾选，右 40% 已选列表），560px 下两栏极度拥挤、勾选项文字换行。
  - `food/list.html` 菜品弹窗用 `size="xl"(840px)` 塞入口味编辑器（`:151-175` 嵌套增删标签交互），宽度够但**纵向很高**，在矮视口下风险见 2.2。

#### 2.2 层级（z-index / 嵌套 / 裁切）
- **iframe 内弹窗的纵向裁切风险**：`index.html` 把 iframe 高度固定为 `window.innerHeight - 56`，弹窗 `append-to-body` 挂在 iframe 内部 `<body>`。Element 弹窗默认**垂直居中 + 高度随内容增长**；当弹窗（如菜品 xl 弹窗、订单详情弹窗）高度接近或超过 iframe 可用高度时，**顶部会被裁切且无法滚动到达**（弹窗容器自身不滚动，只有 `__body` 在设了 `max-height` 时才滚）。
  - 建议：`crud-dialog` 给 `.el-dialog` 加 `max-height: calc(100vh - 120px)` 且 `__body` `overflow:auto`，或直接用抽屉（`el-drawer`）承载超高表单。
- **重阴影**：`design-system.css:500` / `unified-components.css:687` 弹窗用 `box-shadow: --shadow-xl`(`0 20px 48px`)，配合 16px 圆角，在浅背景上显得"浮夸"，与"克制企业风"诉求相悖。建议降到 `--shadow-lg` 或 `--shadow-md`。

#### 2.3 交互逻辑（Interaction）
- **点遮罩关闭不一致，存在数据丢失风险**：`crud-dialog` 默认 `closeOnClickModal:false`（安全），但**裸 `el-dialog` 不显式声明时继承 Element 默认 `true`** → 点遮罩即关闭、未保存内容直接丢。多数遗留页的裸弹窗未设 `:close-on-click-modal="false"`。建议全站统一为 false（或在 `main.css` 用全局覆盖兜底）。
- **ESC / 点 X 关闭无"未保存确认"**：`crud-dialog` `:843` `close-on-press-escape="true"` 且无 `before-close` 守卫。用户在表单里改了一半按 ESC 或点 X，**没有任何"放弃修改？"提示** → 误触丢数据。建议 `crud-dialog` 增加 `before-close` 守卫（脏检查 → `MessageBox.confirm`）。
- **确认动作接线遗漏**：见 P0-1 / P0-2，这是当前最痛的交互缺陷。

#### 2.4 信息密度（Info Density）
- **一个弹窗塞太多**：菜品弹窗（xl）把"基础信息 + 图片上传 + 描述 + 动态口味标签编辑器"全堆在一个模态里，口味编辑还嵌套增删标签的微交互，认知负荷高。建议把"口味做法"拆为独立步骤/子区域，或默认折叠。
- **字段宽度 / label-width 不统一**：
  - `food/list.html` 菜品弹窗 `label-width="100px"`；`category/list.html` 用 `80px`；`combo/add.html` 用 `90px` → **同一系统三套标签宽度**。
  - 魔法数字：菜品弹窗内口味标签输入框 `style="width:80px/140px"`（`:160,167`），分类弹窗用 `style="width:293px"`（`:107,116`）——既硬编码、又彼此不一致。应抽出 `--form-control-width` 令牌（如 280px）统一。
- **语义色/强调色错位**：`combo/add.html` 的数字输入框用**橙色**强调（`--orange-50/--orange-100`），而全站品牌是金色 `#ffc200` → 同一表单里出现金+橙双强调色，视觉噪音。
- **空状态提示误导**（影响列表页信息准确性）：`components.js:526-535` `crud-table` 的 `empty-hint` 默认文案是"试试调整筛选条件，或点击右上角'新建'添加一条记录"。但**报表/日志/纯查询类列表页（如 `report/*`、`sys/operation-log`、`payment/order-list`、`delivery/order-list` 等）根本没有"新建"按钮**，却未覆盖 `empty-hint` → 给用户错误引导。建议：无新建能力的页面显式传 `empty-hint` 覆盖，或在组件里按是否配置 `add` action 自动判定。

---

### 🟢 P3 — 布局结构与一致性

#### P3-1. 页面两极分化（"观感不佳"的宏观根因）
- **现代化页面**（已组件化/令牌化）：`food/list.html`、`category/list.html`、`order/list.html`、`dashboard/overview.html` 等 —— 结构清晰、用 `crud-table`/`stat-cards`、颜色走令牌。
- **遗留页面**（未改造）：`combo/add.html`（整段内联 `<style>` + 硬编码）、`store/list.html`、`marketing/*`、`cost/*`、`cashier/*`、以及所有用裸 `el-dialog` 的页面 —— 圆角/字号/颜色各写各的。
- **后果**：用户在页面间切换时"像换了套系统"，这是"整体观感不佳"最直接的体感来源。建议以 `food/list.html` 为样板，逐页对齐（重点先处理高频访问页）。

#### P3-2. "全局居中"对齐与长文本扫读的矛盾
- `components.js:702-706` `resolveColAlign` 默认**所有文本列居中**、仅金额/数字右对齐（符合你"尽量都居中"的要求）。
- **可用性提醒**：居中对"短标识列"（状态/排序/分类）友好，但对"长文本列"（菜品名称、地址、描述、备注）会**破坏从左向右的扫读动线**，反而降低可读性。
- 建议：保留你的"默认居中"偏好，但**对长文本列由页面显式 `align:'left'` 覆盖**（一致性层的 `.is-left` 已支持左对齐 `!important`）。这是"规范统一"与"可读性"的平衡点。

#### P3-3. 内联样式与硬编码残留
- `combo/add.html:12-172` 一处 `<style>` 内含：硬编码 `width:777px/130px/110px/60%`、游离 `border-radius:3px/4px/6px`、`box-shadow:0px 1px 4px 3px rgba(0,0,0,0.03)`（发光）、以及 `--orange-*`/`--gray-333`/`--el-bg-white` 等非令牌/遗留色。
- 旧审计统计全站 51 个页约 300+ 处内联样式，虽经一致性层部分收敛，但**遗留页面（如 combo/add）仍未清理**。
- 建议：把 `combo/add.html` 这类页面的 `<style>` 抽到 `unified-components.css` 或页面级 css，颜色/圆角/宽度全部令牌化。

#### P3-4. 金额列绕过组件格式化
- `food/list.html:250` 售价列写了 `slot:true` + 自定义 `￥{{ formatMoney(...) }}`，绕过了 `crud-table` 的 `type:'money'`（组件本可自动右对齐+千分位+等宽）。虽功能正确，但**与"金额列统一走 `type:'money'`"的约定相悖**，且易在别处出现"有的金额有千分位、有的没有"的漂移。建议逐步去掉自定义 `slot`，改 `type:'money'`（组件已支持 `tabular-nums` 等宽）。

---

## 三、根因总结（一句话）

> **后台"架构现代化了，但视觉规范没有收敛到底"**：一套更激进的 `unified-components.css` 在多处推翻了 `tokens.css`/`design-system.css` 的硬规范（16px 圆角、玻璃发光、中文字距），`crud-dialog` 组件迁移逐页进行且**部分页面漏接确认逻辑**，叠加"现代化页面 vs 遗留页面"两极分化，共同造成"每页看起来都不一样、弹窗点不动/点不准"的体感。

---

## 四、优化建议（分阶段、可落地）

### 阶段 1：止血（1 天内，零业务逻辑风险，最优先）
1. **修 P0 两个弹窗**：`food/list.html` 菜品弹窗补 `@submit="submitDishForm()"`；`combo/add.html` "添加菜品"弹窗补 `@submit="addTableList()"`（或补 `#footer`）。
2. **全站排查 `crud-dialog` 接线**：grep `<crud-dialog` 与 `@submit`，凡有弹窗无 `@submit`/无 `#footer` 且需要提交的，一律补上。
3. **修 P1-3/P1-4 字距**：在 `design-system.css` 删除 `.el-table th` 与 `.el-button` 的 `letter-spacing:0.5px`（及无意义的 `text-transform:uppercase`）。

### 阶段 2：去冲突（2–3 天，治本）
4. **圆角收敛**：全站圆角统一到 4/8/12px；弹窗与容器改用 `--radius-lg`(12px)，删除 `--radius-xl` 或仅作内部别名。
5. **去发光**：删除 `unified-components.css:534`（容器 `backdrop-filter`）、`design-system.css:780`（遮罩 `backdrop-filter`）、按钮/卡片悬停 `box-shadow` 降到 `--shadow-md`。
6. **弃用直角遗留**：审计 `common-fixes.css`/`common-el.css` 的 `border-radius:0`，把仍生效的直角选择器迁移/删除，使现代化规范成为唯一事实来源。
7. **弹窗安全兜底**：`crud-dialog` 增加 `before-close` 脏检查守卫；全站裸 `el-dialog` 统一 `:close-on-click-modal="false"`（或在 `main.css` 全局兜底）。

### 阶段 3：一致性（按页推进）
8. **以 `food/list.html` 为样板**，把 `combo/add.html`、`store/list.html`、`marketing/*`、`cost/*`、`cashier/*` 等遗留页的内联 `<style>` 抽出、令牌化、统一 `label-width`(建议 100px) 与字段宽度。
9. **统一弹窗尺寸体系**：优先用 `crud-dialog` 四档，禁止裸 `el-dialog` 手填 `width`；超高表单改抽屉。
10. **列对齐**：长文本列显式 `align:'left'`，短标识列居中（保留你的"尽量居中"偏好）。
11. **金额列**：去自定义 `slot`，统一 `type:'money'`。
12. **空状态**：无"新建"的列表页显式覆盖 `empty-hint`。

### 验证方式
- 阶段 1/2 改动影响全站，**先对核心页（food/category/order/combo/store/dashboard）截图建立"样式基线"**，改完逐页比对圆角/字距/发光是否归一。
- 起本地服务（`mvn spring-boot:run`）或 `file://` 打开列表页，验证：弹窗"确定"可保存、表头字距正常、卡片无玻璃模糊、圆角统一 12px。

---

## 五、立即可执行的快速修复清单（Top 10）

| # | 问题 | 文件:行 | 动作 |
|---|------|---------|------|
| 1 | 菜品弹窗保存未接线 | `food/list.html:103-109` | 加 `@submit="submitDishForm()"` |
| 2 | 套餐添加菜品未接线 | `combo/add.html:267-348` | 加 `@submit="addTableList()"` |
| 3 | 表头中文字距异常 | `design-system.css:450` | 删 `letter-spacing:0.5px` |
| 4 | 按钮中文字距异常 | `design-system.css:401` | 删 `letter-spacing:0.5px` |
| 5 | 容器玻璃发光 | `unified-components.css:534` | 删 `backdrop-filter: blur(10px)` |
| 6 | 遮罩发光 | `design-system.css:780` | 删 `backdrop-filter: blur(4px)` |
| 7 | 弹窗圆角超规范 | `design-system.css:498` / `unified-components.css:685` | 改 `--radius-xl` → `--radius-lg` |
| 8 | 弹窗无未保存守卫 | `components.js:759-872` | 加 `before-close` 脏检查 |
| 9 | 裸弹窗点遮罩丢数据 | 各裸 `el-dialog` 页 | 统一 `:close-on-click-modal="false"` |
| 10 | 空状态误导提示 | `components.js:526-535` | 无新建页覆盖 `empty-hint` |

---

> 备注：以上结论均基于 2026-08-13 当前代码逐文件核实。P0 两项为静态代码推断（未实际运行），建议按阶段 1 修复后本地起服务实测一次确认"确定"可保存。其余 P1/P2/P3 为已确认的样式/交互规范冲突，可直接按清单修。
