# 瑞吉外卖管理端 · 前端代码规范（Code Standards）

> 配套文档：[DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md)
> 适用：后台管理端（`src/main/resources/backend/`）

---

## 1. 目录结构（清晰分层）

```
backend/
├─ page/                 # 页面（按业务模块分子目录）
│  ├─ _templates/        # 标准模板（list-page.html 为高复用骨架，**新页面必须基于它**）
│  ├─ food/  order/  ... # 各业务页
├─ js/
│  ├─ components.js      # 通用组件库：stat-cards / table-bar / crud-table / crud-dialog
│  └─ request.js         # Axios 封装
├─ api/                  # 接口定义（按模块拆分 .js）
├─ styles/
│  ├─ tokens.css         # 【设计令牌唯一来源】颜色/间距/字号/圆角/阴影/层级
│  ├─ design-system.css  # 【设计系统基础层】a11y/布局原语/卡片/面包屑/品牌按钮
│  ├─ index.css          # 主框架（@import tokens.css）
│  ├─ page.css           # 页面级布局（dialog 宽度/列宽/按钮规范）
│  ├─ components.css     # 通用组件补充样式
│  └─ components-stats-card.css # 统计卡片主题
└─ index.html            # 主框架壳（侧边栏+顶栏+iframe）
```

**原则**：
- 通用能力（表格/弹窗/搜索栏/统计卡）**只实现一次**，放在 `js/components.js` + 对应样式。
- 新业务页**不重复造轮子**：继承 `_templates/list-page.html`，复用 4 大组件。
- 样式分三层：令牌层（`tokens.css`）→ 设计系统层（`design-system.css`）→ 业务/页面层（`page.css`/`components.css`）。

---

## 2. 命名规范

| 层 | 约定 | 示例 |
| --- | --- | --- |
| CSS 类 | **BEM / kebab-case**，新类加 `ds-` 前缀 | `.ds-page-header__title`、`.ds-link--danger` |
| JS 变量/函数 | **camelCase** | `searchValues`、`onCardClick` |
| JS 常量 | **UPPER_SNAKE_CASE** | `IGNORE_TABLES` |
| Vue 组件 | **kebab-case**（全局注册） | `<crud-table>`、`<stat-cards>` |
| 文件 | **kebab-case** | `list.html`、`food.js` |
| 设计令牌 | `--类别-语义-阶调` | `--color-brand-500`、`--space-4` |

---

## 3. 组件化开发契约

通用组件（`stat-cards` / `table-bar` / `crud-table` / `crud-dialog`）：

- **配置驱动**：通过 `props`（如 `:cards`、`:search-items`、`:columns`、`:visible`）声明能力，
  通过 `slots`（如 `#col-xxx`、`#actions`、`#footer`）开放定制点。
- **事件规范**：交互以 `@search`/`@action`/`@card-click`/`@submit`/`@page-change` 等语义事件上抛，
  业务页只写**业务回调**，不碰 DOM 细节。
- **双向绑定**：开关类状态用 `.sync`（如 `:visible.sync`、`:active-key.sync`）。
- **a11y 内建**：组件自身保证 `role`/`aria`/键盘可达（见 DESIGN_SYSTEM §3），
  业务页无需再为通用组件补无障碍属性。
- **禁止**：业务页内手写等价实现；给通用组件加破坏性内联样式覆盖。

---

## 4. 代码复用机制

- **Mixin**：列表页分页逻辑复用 `window.ReggieListMixin`（提供 `page/pageSize/counts/loading` + 翻页处理），
  业务页 `mixins:[window.ReggieListMixin]` 并实现 `fetchData()`。
- **状态枚举中心**：`window.ReggieStatus.register('模块', textMap, tagMap)`，消除各页重复 `statusMap`。
- **全局工具**：`formatMoney` / `formatDate` / `rgStatusText` / `rgStatusTag` / `rgPreview`（components.js 注入）。
- **设计令牌复用**：所有视觉常量来自 `tokens.css`，改一处全局生效。

---

## 5. 无障碍（a11y）自查清单

新页面 / 组件提交前必须逐项确认：

- [ ] 含 `<a class="skip-link">` 跳转到 `#main-content`
- [ ] 主内容用 `<main role="main" aria-labelledby="page-title">` 包裹
- [ ] 唯一 `<h1>`（`.ds-page-header__title`）
- [ ] 含面包屑 `<nav aria-label="面包屑导航">`
- [ ] 所有可交互元素可纯键盘操作且有可见聚焦环
- [ ] 图标按钮带 `aria-label` 或 `title`
- [ ] 图片有 `alt` / 装饰图 `aria-hidden`
- [ ] 弹窗标题可被屏幕阅读器读出（`crud-dialog` 已内建）
- [ ] 颜色不作为唯一信息载体（状态同时有文字/图标）

---

## 6. 提交与变更记录

- 分支：`main ← test ← dev ← feature/*`
- 提交信息：`feat:`/`fix:`/`docs:`/`style:`/`refactor:`/`test:`
- **变更记录**：每次修改/新增前端代码，在根 `CHANGELOG.md` 追加一行：
  `YYYY-MM-DD | 模块 | 更新内容 | 开发者：XXX`
- **禁止提交**：`.log`、`node_modules`、`target`、临时文件。

---

## 7. 代码评审（Review）清单

- [ ] 是否引入了新的硬编码色值/间距？（应改用 token）
- [ ] 是否重复实现了通用组件能力？（应复用 4 大组件）
- [ ] 是否偏离品牌金主色？
- [ ] a11y 自查清单是否全过？
- [ ] 是否基于 `_templates/list-page.html` 开发？（新列表页）
- [ ] 是否更新了 `CHANGELOG.md`？
