# 后台前端审查报告 · 数据分析 / 营销中心 / 门店管理（2026-08-13）

逐文件审查了三大模块的弹窗、接线、组件可用性、HTML/CSS 规范，共覆盖 20+ 页面、含 16 个 `<crud-dialog>`。

## 一、审查范围
- **数据分析**：`report/*`（含 evaluation-list 评价管理）、`dashboard/overview`、`cost/overview`、`recommend/overview`、`cashier/daily-settlement`（日结/收银）
- **营销中心**：`marketing/discount`、`marketing/full-reduction`、`recommend/campaigns`、`member-center/coupon-list`、`member-center/level-list`、`member-center/member-list`
- **门店管理**：`store/list`、`store/dashboard`、`region/list`

## 二、已修复的真实缺陷（按严重程度）

### 🔴 P0 · `report/evaluation-list.html` 整页排版错乱
- 第 74 行：`class="<div class="eval-app-wrap""`（复制粘贴残留），`class` 属性值被截断为字面 `<div class="`，导致 `.eval-app-wrap` 的所有样式（grid 布局、筛选栏、卡片、表格容器）全部失效，整页从设计稿退化成无样式裸排。
- 修复：`class="eval-app-wrap"`，恢复全部布局。

### 🔴 P0 · `cashier/daily-settlement.html` 日结详情弹窗渲染失败
- 详情弹窗使用了 `<el-descriptions>` / `<el-descriptions-item>`。
- **根因**：本项目 Element UI 构建为 2.4.x（描述列表组件 2.9.0 才引入）。经核实 `plugins/element-ui/index.js`（单行压缩包，`grep -c "Descriptions" = 0`）**未注册该组件**。`store/list.html` 的注释"替代未注册的 el-descriptions"印证此坑。因此该弹窗实际渲染为空/破损。
- 同弹窗还有**无用的"确定"按钮**（无 `@submit`、无 `show-submit=false`）。
- 修复：① 改用标准 CSS 网格定义列表（新增 `.settle-detail` / `.dt-item` 样式，复用语义令牌，保持 2 列 + 备注/结账时间整行）；② 弹窗加 `:show-submit="false" cancel-text="关 闭"`，去掉死按钮。功能、字段、配色（退款红/毛利绿）完全保留。

### 🟠 P1 · `store/list.html` 新建/编辑弹窗出现重复底栏
- 表单弹窗同时有 crud-dialog 默认底栏（确定/取消）+ 体内 `.footer-actions` 又一对（取消/保存修改）→ 弹窗底部出现 **4 个按钮**，重复且易混淆。
- 修复：删除体内重复底栏，仅保留 crud-dialog 默认底栏（确定 → `submitForm`）。detail 弹窗的 body 按钮（编辑/切换/同步）**保留**，因其 `show-submit=false` 且为必要操作。

### 🟡 P2 · `recommend/campaigns.html` 重复注释
- 新建/编辑、推送、统计三个对话框的注释各写了两遍，无功能影响。
- 修复：清理为单份注释。

## 三、审查结论：其余页面接线均正确
- 所有保存型弹窗均已 `@submit` 接对应方法：`marketing/discount`、`marketing/full-reduction`、`member-center/coupon-list`、`member-center/level-list`、`member-center/member-list`(充值)、`region/list`、`store/list`、`recommend/campaigns`(表单/推送)、`cost/dish-cost` 等。
- 所有展示型弹窗均已 `:show-submit="false"` 或带自定义 footer：评价详情/图片预览、门店详情/同步、活动统计、会员详情、日结详情。
- 颜色规范：内联 `style` 均走 `var(--...)` 令牌；ECharts 图表硬编码 hex 为数据系列色（标准用法，未动）。

## 四、经验沉淀（跨会话必读）
- **本项目 Element UI 无 `el-descriptions`**。详情展示类页面一律用自绘 CSS 网格（参照 `store/list.html` 的 `.detail-grid` 或本次 `cashier` 的 `.settle-detail`），新增页面勿用 `<el-descriptions>`。
- 审查未知组件是否可用，先 `grep -c "ComponentName" plugins/element-ui/index.js` 确认。

## 五、改动文件清单
| 文件 | 改动 |
|---|---|
| `report/evaluation-list.html` | 修复第 74 行畸形 `class` |
| `cashier/daily-settlement.html` | 详情弹窗去 `el-descriptions`→CSS 网格 + 加 `show-submit=false` |
| `store/list.html` | 删除新建/编辑弹窗体内重复底栏 |
| `recommend/campaigns.html` | 清理三处重复注释 |

无编译步骤，刷新即生效。建议本地起服务后重点回归：**菜品评价页排版、日结"查看详情"字段是否完整、门店新建/编辑弹窗按钮是否只剩一套**。
