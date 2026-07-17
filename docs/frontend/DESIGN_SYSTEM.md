# 瑞吉外卖管理端 · 设计系统规范（Design System）

> 适用范围：后台管理端全部页面（`src/main/resources/backend/page/*`）
> 技术约束：iframe 架构 + Vue 2 + Element UI，**无构建工具**（原生 `<link>`/`<script>`）
> 配套文档：[CODE_STANDARDS.md](./CODE_STANDARDS.md)

---

## 1. 设计令牌（Design Tokens）—— 唯一事实来源

所有颜色 / 间距 / 字号 / 圆角 / 阴影 / 层级**禁止硬编码**，一律引用
`styles/tokens.css` 中的 CSS 变量。任何页面或组件不得再写死色值（如 `#ffc200`、
`#f5f6fa`）或魔法间距（`20px`）。

引入方式（子页面必须按顺序加载，不可省略）：

```html
<link rel="stylesheet" href="../../styles/common.css" />
<link rel="stylesheet" href="../../styles/page.css" />
<link rel="stylesheet" href="../../styles/components.css" />
<link rel="stylesheet" href="../../styles/tokens.css" />        <!-- 设计令牌 -->
<link rel="stylesheet" href="../../styles/design-system.css" />  <!-- 设计系统基础层 -->
```

### 1.1 品牌主色（金色，贯穿全站）

| Token | 值 | 用途 |
| --- | --- | --- |
| `--color-brand-500` | `#ffc200` | **主色**：主按钮、激活态、强调文字、图标 |
| `--color-brand-600` | `#e6ae00` | 主色按下态 / 聚焦环 |
| `--color-brand-700` | `#c99200` | 文字按钮（`.btn-view`/`.ds-link`）加深可读色 |
| `--color-brand-50/100` | 浅金 | 文字按钮 hover 背景 |

> ⚠️ 品牌金底色上**必须使用深色文字** `--text-on-brand`（#1f2937），
> 以满足 WCAG AA 对比度（金色 + 白字对比度仅 ~1.7，不合格）。

### 1.2 中性灰阶 / 语义色 / 文本 / 边框 / 背景

见 `tokens.css`：`--color-gray-25…900`、`--color-success/warning/danger/info`
（含 `-light`/`-dark` 变体）、`--text-*`、`--border-*`、`--bg-*`。

### 1.3 间距（4px 基准栅格）

`--space-1`(4) → `--space-16`(64)。**布局间距一律用 token**，
禁止出现 `margin:20px` 这类字面量（特殊微调除外，须加注释）。

### 1.4 圆角 / 字号 / 字重 / 行高 / 阴影 / 层级 / 动效

均提供 token（`--radius-*`、`--font-size-*`、`--font-weight-*`、
`--line-height-*`、`--shadow-*`、`--z-*`、`--transition-*`）。

---

## 2. 组件视觉规范

### 2.1 页面表面（容器卡片）

`.dashboard-container .container` 已统一为：白底 + 1px 浅边框 +
`--radius-lg`(12px) + `--shadow-sm`。**不要再手写页面白底容器**。

### 2.2 页头（Page Header）

每页顶部统一使用：

```html
<header class="ds-page-header">
  <div class="ds-page-header__titles">
    <h1 class="ds-page-header__title" id="page-title">页面名称</h1>
    <p class="ds-page-header__subtitle">一句话说明</p>
  </div>
  <div class="ds-page-header__actions"><!-- 页级主操作 --></div>
</header>
```

### 2.3 面包屑（Breadcrumb）

```html
<nav class="ds-breadcrumb" aria-label="面包屑导航">
  <span class="ds-breadcrumb__item">首页</span>
  <span class="ds-breadcrumb__sep" aria-hidden="true">/</span>
  <span class="ds-breadcrumb__item ds-breadcrumb__item--current" aria-current="page">当前页</span>
</nav>
```

### 2.4 统计卡片 / 表格 / 弹窗

- 统计卡片：`<stat-cards>`（色值由 `color` 字段驱动，已映射到品牌金等 token）
- 表格：`<crud-table>`（禁止手写 `<el-table>`/`<el-pagination>`）
- 弹窗：`<crud-dialog size="sm|md|lg|xl">`（禁止手写 `<el-dialog>`）

### 2.5 按钮与文字链接

- 主操作按钮：Element `type="primary"` → 已由 `design-system.css` 统一为金色底 +
  深色字（见 §1.1）。
- 表格内文字按钮：用 `.btn-view`（金）/`.btn-delete`（红）或 `.ds-link`/`.ds-link--danger`。
- 语义按钮（success/warning/danger）：沿用 Element 默认并映射到 token。

---

## 3. 无障碍（a11y，WCAG 2.1 AA）

| 要求 | 落地 |
| --- | --- |
| 键盘可操作 | 全局 `:focus-visible` 聚焦环；`crud-dialog` 支持 ESC 关闭 |
| 跳转主内容 | 每页顶部 `<a class="skip-link" href="#main-content">` |
| 地标 | 主内容包 `<main role="main" aria-labelledby="page-title">` |
| 标题层级 | 每页唯一 `<h1>`（`.ds-page-header__title`） |
| 可点击卡片 | `<stat-cards>` 可点击项带 `role="button"`/`tabindex=0`/`aria-pressed`/`aria-label`，支持 Enter/Space |
| 搜索区 | `<table-bar>` 搜索区 `role="search"` |
| 表格区 | `<crud-table>` 容器 `role="region"` + 可透传 `aria-label` |
| 弹窗 | Element `el-dialog` 原生 `role="dialog"` + `aria-labelledby`（标题） |
| 动效偏好 | `@media (prefers-reduced-motion: reduce)` 全局停用动画 |
| 对比度 | 金色底用深色字；文字按钮用 `--color-brand-700` |

---

## 4. 响应式规范

断点（`tokens.css` 注释文档化，媒体查询用实际像素值）：

| 断点 | 像素 | 行为 |
| --- | --- | --- |
| 手机 | ≤480px | 弹窗近全屏（96%），隐藏折叠按钮 |
| 平板 | ≤768px | 弹窗 92%；`.ds-page-header` 纵向堆叠；`.ds-hide-sm` 隐藏 / `.ds-show-sm` 显示 |
| 桌面 | 默认 | 标准布局 |
| 大屏 | ≥1600px | 侧边栏加宽至 220px |

工具类：`.ds-hide-sm` / `.ds-show-sm`（响应式显隐）；
布局原语：`.ds-stack` / `.ds-cluster` / `.ds-gap-*`（用 token 控制间距）。

---

## 5. 禁止事项（红线）

1. ❌ 在页面/组件里硬编码色值、间距、字号（必须引用 token）。
2. ❌ 手写 `<el-table>`/`<el-pagination>`/`<el-dialog>` 重复实现通用组件。
3. ❌ 偏离品牌主色（金色），擅自使用 Element 默认蓝 `#409eff` 作为强调色。
4. ❌ 页面缺少 `main` 地标 / `h1` / 面包屑（新页面必须按模板 `_templates/list-page.html`）。
