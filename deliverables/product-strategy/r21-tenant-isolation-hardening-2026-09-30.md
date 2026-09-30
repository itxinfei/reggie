# Reggie · R-21 租户隔离健壮性补全 · 实施规格书

**日期**：2026-09-30
**类型**：功能实施规格书（承接路线图 R-21 / M1，P0 工程健康地基）
**作者**：析客（需求分析师 / 产品战略团队）
**关联需求**：R-14（多租户合同差异，M3–M4，本规格是其硬前置）、R-09（Flyway 版本化）、R-15（全渠道四通）
**定位基线**：Reggie 是给餐饮商家"自己部署、自己用"的开源 / 可二开系统；多租户隔离本身是刚需（租用 / 租赁使隔离成为必需），但**隔离改造必须先解决"公开端点的租户解析"前置问题**，否则翻 fail-fast 会直接炸掉顾客端未登录浏览。

---

## 📌 TL;DR（执行摘要）

- **核心命题**：当前 `MybatisPlusConfig.getTenantId()` 在租户上下文为空时返回 `-1L`（**fail-closed 但静默返回空集**），既不报错也不泄漏，但掩盖了"无上下文却查业务表"的真实路径；同时 `shopping_cart` / `dish_evaluation` 两张表虽**已带 `tenant_id` 列却仍留在白名单**，未受插件隔离。R-21 要把隔离从"静默兜底"升级为"显式 fail-fast + 真正隔离"，并在**翻 fail-fast 之前**先解决公开端点的租户解析。
- **关键修正（相对 R-14 规格）**：R-14 规格假定这两张表"无 `tenant_id` 列需补列"，但实测 `src/test/resources/schema.sql:236`（`shopping_cart`）、`:432`（`dish_evaluation`）**都已有 `tenant_id bigint NULL DEFAULT NULL`**。因此 R-21 实际只需 **回填（从 `user_id` → `user.tenant_id`）+ 移出白名单**，不需 ALTER 加列（生产库若漂移需先 `SHOW CREATE TABLE` 核对）。
- **安全护栏（不可逾越）**：`DishController.java:144-146` 的公开菜品查询 `if (tenantId != null) .eq(Dish::getTenantId, tenantId)`——**匿名顾客端请求 `tenantId==null` 时手动过滤被跳过，完全依赖插件 `-1L` 空集合兜底**。若直接把 `getTenantId()` 改成抛异常，未登录浏览菜单会直接 500。**必须先落地"公开端点租户解析"（见第三节决策 A），fail-fast 才能安全翻面。**
- **复用优先**：`DbAuditProbe`（test 底座，只读 JDBC 体检，检测 tenant_id 分布 / 幽灵租户）可提升为生产级 `@Scheduled` 隔离审计任务；跨租户 / 无上下文查询已有成熟范式（`@InterceptorIgnore(tenantLine="true")`，几十处）。
- **投入与位置**：R-21 合计 **3–4 人周**（与路线图一致），落 M1（2026Q4，P0 先行）；公开端点租户解析若需新建 `TenantResolver` 可能 +1 人周（见待确认）。

---

## 🎯 核心结论卡片

| 项目 | 内容 |
|------|------|
| 推荐方案 | **分阶段**：① 先落地公开端点租户解析（顾客端传 `storeId`/`tenantId` 或单租户默认）→ ② 新增 `TenantContextMissingException` 并把 `getTenantId()` 翻为 fail-fast 抛异常（仅作用于非白名单表，白名单表不受影响）→ ③ 回填 `shopping_cart`/`dish_evaluation` 的 `tenant_id` 并移出白名单 → ④ 提升 `DbAuditProbe` 为生产级 `@Scheduled` 隔离审计 |
| 优先级 | **P0（工程健康地基）**，M1 先行；是 R-14 的硬前置，但 fail-fast 必须排在公开端点租户解析之后 |
| 预期影响 | 消除"无上下文静默空集合"的隐性 bug；`shopping_cart`/`dish_evaluation` 真正按租户隔离；隔离审计可观测、可告警 |
| 资源需求 | **3–4 人周**（后端架构师 + 资深后端）；若新建 `TenantResolver` +1 人周 |
| 风险等级 | **中高**。fail-fast 误伤公开顾客端是头号风险（已定位 `DishController`）；两表回填脏数据；39 模块回归 |

---

## 一、当前状态（代码事实，带文件行号）

> 以下事实由主理人探查 + 复核，均为只读引用，未改动任何源码。

| 事实 | 证据 | 对 R-21 的含义 |
|---|---|---|
| `getTenantId()` 上下文为空返回 `new LongValue(-1L)`（fail-closed 但静默空集） | `MybatisPlusConfig.java:70-78` | 无上下文查业务表 → `WHERE tenant_id=-1` → 空结果；不报错、不泄漏，但掩盖了"无上下文路径" |
| `ignoreTable()` 仅白名单判断；非白名单 + 空上下文仅 `log.warn`（仍返回空集） | `MybatisPlusConfig.java:94-108`（:101-106、`TenantAspect.java:80-90` 同样仅 WARN） | 当前"兜底"是告警而非拦截；fail-fast 需把告警升级为抛异常 |
| `DishController.java:144-146` 公开菜品查询 `if (tenantId != null) .eq(Dish::getTenantId,...)` | `DishController.java:144-146`、`:311-313`、`:373-375` | **匿名顾客端 `tenantId==null` 时手动过滤被跳过**，依赖插件 `-1L` 兜底；翻 fail-fast 前必须先解决公开端点租户解析 |
| `LoginCheckFilter.restoreExcludeContext` 匿名公开请求不设置租户上下文 | `LoginCheckFilter.java:132-146`（:135-137 "匿名请求无会话则跳过"） | 公开 EXCLUDE_URLS 端点默认无租户上下文；fail-fast 会在此类路径抛异常 |
| `shopping_cart` / `dish_evaluation` **已带 `tenant_id` 列**（但白名单中、未回填） | `schema.sql:236`、`schema.sql:432`（`tenant_id bigint NULL DEFAULT NULL`） | R-21 只需回填 + 移出白名单，**不需 ALTER 加列**（生产库先 `SHOW CREATE TABLE` 核对漂移） |
| 调度任务 / 回调 / 报表均显式 `setCurrentTenantId(...)` 后再查 | `OrderTimeoutTask`、`ReportServiceImpl`（数十处 `setCurrentTenantId(tenant.getId())`） | 这些路径已有上下文，fail-fast 不会误伤；但需在回归中确认无遗漏 |
| 跨租户 / 无上下文查询已有成熟逃逸范式 `@InterceptorIgnore(tenantLine="true")` | `UserMapper`、`PaymentOrderMapper`、`DashboardMapper`、`OrderMapper`（总部聚合）等几十处 | 真正的跨租户查询应继续用此范式，不应被 fail-fast 误伤 |
| `DbAuditProbe` 已是只读 JDBC 体检探针，检测 tenant_id 分布 / 幽灵租户 | `src/test/java/.../test/DbAuditProbe.java:52`（:181-243 行数/租户分布/孤儿） | 可提升为生产级 `@Scheduled` 隔离审计任务（见第四节 D） |
| `tenant` 表无租户级 `ai_provider_config` / `ai_prompt_template` 已确认系统级全局表（见 R-14 决策⑤） | `MybatisPlusConfig.java:36`、`AiProviderConfigServiceImpl` | **保留白名单、不补列、不回填**；本规格不涉及 |
| 测试耦合真实 MySQL+Redis（742 用例），无 CI | `AGENTS.md`、内存 `@DirtiesContext` | R-21 改动必须在该环境下回归；离线仅能 `mvn -o compile` 验证编译 |

---

## 二、产品目标（2 个正交目标）

| # | 目标 | 对齐定位 | 成功信号 |
|---|------|----------|----------|
| G1 | **隔离失败显式化（fail-fast）**：任何"非白名单表 + 无租户上下文"的查询必须抛异常而非静默空集 | 自托管 + 多租户（防事故） | 缺上下文访问隔离表必抛 `TenantContextMissingException`；公开端点能正确解析租户 |
| G2 | **两张漏网表真正隔离**：`shopping_cart` / `dish_evaluation` 回填 `tenant_id` 后受插件隔离 | 自托管 + 多租户（租户间购物车/评价不串） | 两表 `tenant_id IS NULL` 孤儿行 = 0；以两不同租户上下文查询结果集不相交 |

---

## 三、关键设计决策（须先决，再翻 fail-fast）

### 决策 A · 公开端点租户解析（头号前置，必须先行）

**问题**：顾客端（C 端 Vue2+Vant）未登录浏览菜单 / 菜品 / 套餐时，请求经 `LoginCheckFilter` 的 EXCLUDE_URLS 放行且**不设租户上下文**（`restoreExcludeContext` 仅在已有会话时恢复）。`DishController` 等公开查询在 `tenantId==null` 时跳过手动过滤，依赖插件 `-1L` 兜底。

**若不解决直接翻 fail-fast**：未登录顾客端浏览 → `getTenantId()` 抛异常 → 500。

**推荐方案（供产品/架构确认）**：
- **主推**：顾客端在公开浏览端点携带 `storeId`（或 `tenantId`）参数；新增轻量 `TenantResolver`（在 `LoginCheckFilter` 或独立 `HandlerInterceptor` 中），对 EXCLUDE_URLS 公开请求从参数解析租户并注入 `BaseContext.setCurrentTenantId(...)`，使插件始终有租户。
- **单租户自托管兜底**：配置 `reggie.default-tenant-id`，当请求无显式租户参数时回落到默认租户（覆盖"一个实例 = 一家店"的最常见自托管形态）。
- **拒绝方案**：靠 session 强制顾客端登录才能浏览——违背"未登录也能看菜单"的产品预期，不可取。

> ✅ **现成正面范本（2026-09-30 调研补充）**：扫码堂食已实现完整的"参数→解析租户→公开查询"链路——`DiningTablePublicController.getPublicMenu`（`/api/dining/table/public/{tableId}/menu`，`:60-68`，注释明确"租户由桌台反查确定…匿名可访问"），经 `DiningTableService.getPublicMenu(tableId)` 反查桌台所属门店后查询该店分类与在售菜品。**R-21-A 的 `TenantResolver` 直接沿用此模式**（把 `tableId` 换成 `storeId`/`tenantId`），不需要发明新机制。

**关联产品缺口（同源问题，建议与 R-21-A 同窗补齐）——店铺名 C 端可配性差**：
- 店名唯一来源是 `tenant` 表（`RestaurantController.java:83-93`），但更新仅 `PUT /tenant` **超管**接口（`TenantController.java:156-160` `@RequiresAdmin`），后台**无商家自助"店铺设置"页**（backend 前端搜不到店铺设置/店铺资料入口）→ 普通商家无从改名。
- 即使改了 tenant 名，C 端仍有大量写死"瑞吉外卖"：所有页面 `<title>`（`front/index.html:9`、`page/*.html` 20+ 处）、匿名时店名回落写死值（`RestaurantController.java:84`，`tenantId=null` 查不到 tenant）、logo 为静态品牌资源（`:94-95`"暂无数据字段，沿用品牌静态资源"）、公告文案写死（`:127`）。
- 建议补齐：① 商家端"店铺设置"页（店名/公告/营业时间/品牌色，权限下放店长，非超管）；② C 端 `<title>`/logo/notice 改读 `tenant`/`store_info` 动态数据，去硬编码。

> ⚠️ **此决策为产品/架构层面，须用户确认后实施**（决定顾客端如何选店、单租户默认值语义）。本规格其余内容均以此决策落地为前提。

### 决策 B · fail-fast 作用域

`getTenantId()` 抛异常**仅对"非白名单表"生效**（MyBatis-Plus 先 `ignoreTable` 再取租户值，白名单表不进 `getTenantId`）。白名单表（tenant/permission/role_permission/region/rider_remember_token/store_sync_log/ai_provider_config/ai_prompt_template/employee/待定）不受影响。因此 fail-fast **不会误伤全局共享表**。

### 决策 C · 跨租户查询继续用 `@InterceptorIgnore`

总部聚合 / 调度遍历租户 / 支付回调等既有跨租户路径已用 `@InterceptorIgnore(tenantLine="true")`，保留该范式；**不引入"超级租户哨兵绕过插件"**（避免全局关隔离的安全漂移，与 R-14 §6.4 一致）。

---

## 四、实施拆解（4 个子项）

| 子编号 | 子方向 | 优先级 | 验收标准（摘要） | 估算人周 |
|---|---|---|---|---|
| **R-21-A** | 公开端点租户解析（决策 A 落地） | P0 | 顾客端公开浏览端点能从 `storeId`/`tenantId` 参数或默认租户注入上下文；`LoginCheckFilter` 匿名路径不再"无上下文" | 1（若新建 Resolver，+1） |
| **R-21-B** | fail-fast 异常 + `getTenantId()` 翻面 | P0 | 新增 `TenantContextMissingException`；`getTenantId()` 空上下文抛异常；`ignoreTable()` 移除冗余 WARN；白名单表不受影响 | 0.5 |
| **R-21-C** | 两表回填 + 移出白名单 | P0 | `shopping_cart`/`dish_evaluation` 由 `user_id→user.tenant_id` 回填（幂等/可回滚）；移出 `IGNORE_TABLES`；全量查询设上下文或用 `@InterceptorIgnore` | 1–1.5 |
| **R-21-D** | 隔离审计定时任务（提升 `DbAuditProbe`） | P1 | 生产级 `@Scheduled` 扫描"应隔离表 `tenant_id IS NULL` 孤儿行" + 白名单表无 `tenant_id` 列校验；失败时告警 | 0.5–1 |
| **合计** | — | — | — | **3–4（含 A 可能 +1）** |

> 说明：R-21-B 翻 fail-fast 必须排在 R-21-A 之后；R-21-C 必须排在"生产库已回填且审计通过"之后，避免历史 NULL 行在移出白名单后"消失"（隔离后 `tenant_id IS NULL` 行对所有租户不可见 = 数据不可见风险）。

---

## 五、数据模型与迁移

### 5.1 列现状（已存在，不需 ALTER）
- `shopping_cart.tenant_id`（`schema.sql:236`，`bigint NULL DEFAULT NULL`）
- `dish_evaluation.tenant_id`（`schema.sql:432`，`bigint NULL DEFAULT NULL`）

> 生产库可能漂移（`.gitignore` 忽略 `db/migration/*.sql`）。实施第一步：`SHOW CREATE TABLE shopping_cart` / `dish_evaluation` 核对；若缺列则补 `ALTER TABLE ... ADD COLUMN tenant_id bigint NULL`（仅当生产库缺失时）。

### 5.2 回填脚本（幂等、可回滚、带校验）
```sql
-- 前置：确认 user 表带 tenant_id（见待确认①）
UPDATE shopping_cart sc
   SET sc.tenant_id = (SELECT u.tenant_id FROM user u WHERE u.id = sc.user_id)
 WHERE sc.tenant_id IS NULL AND EXISTS (SELECT 1 FROM user u WHERE u.id = sc.user_id);

UPDATE dish_evaluation de
   SET de.tenant_id = (SELECT u.tenant_id FROM user u WHERE u.id = de.user_id)
 WHERE de.tenant_id IS NULL AND EXISTS (SELECT 1 FROM user u WHERE u.id = de.user_id);
```
- **校验**：回填后 `SELECT COUNT(1) FROM shopping_cart WHERE tenant_id IS NULL` 应趋零（仅 user 已删除的孤儿行可保留并单独清理）。
- **回滚**：回填前全表 `tenant_id` 先不依赖隔离，回滚即 `UPDATE ... SET tenant_id = NULL WHERE <回填标记>`（建议回填前加临时列或备份表）。
- **Flyway**：按 R-09 引入后对齐 `Vx__*.sql`；实施期先用现有 `db/YYYYMMDD_*.sql` 命名。

### 5.3 白名单收敛（`MybatisPlusConfig.IGNORE_TABLES`）
- **移出**：`shopping_cart`、`dish_evaluation`（回填完成后）。
- **保留**：`tenant`、`permission`、`role_permission`、`region`、`rider_remember_token`、`store_sync_log`、`ai_provider_config`、`ai_prompt_template`、`employee`（待定复核）。
- **新增须隔离**：本期无新建表。

---

## 六、fail-fast 改造（`MybatisPlusConfig`）

### 6.1 新增异常类
```java
package com.reggie.common.exception;
// 继承现有 RuntimeException 体系（与项目异常风格一致）
public class TenantContextMissingException extends RuntimeException {
    public TenantContextMissingException(String msg) { super(msg); }
}
```

### 6.2 `getTenantId()` 翻面
```java
@Override
public Expression getTenantId() {
    Long tenantId = BaseContext.getCurrentTenantId();
    if (tenantId == null) {
        // fail-fast：非白名单表 + 无租户上下文 = 非法访问，必须显式报错而非静默空集
        throw new TenantContextMissingException(
            "租户上下文为空，无法安全隔离；请检查登录态/公开端点租户解析/TenantResolver");
    }
    return new LongValue(tenantId);
}
```
> 关键点：仅非白名单表进入本方法（MP 先 `ignoreTable` 再取租户值），故白名单表、跨租户 `@InterceptorIgnore` 查询均不受影响。

### 6.3 `ignoreTable()` 收敛
- 移除原 `:101-106` 的冗余 WARN（该路径在 fail-fast 下不可达）；保留白名单判断本身（白名单表仍需跳过隔离）。

### 6.4 `TenantAspect` 同步
- `TenantAspect.java:80-90` 当前仅 WARN；可升级为：对**非豁免**的 service 方法，若 `tenantId==null` 直接抛（与 fail-fast 一致），但须排除已确认的无上下文合法路径（跨租户 Mapper、公开端点已由 Resolver 注入）。**建议 R-21-B 阶段保持 WARN，待 R-21-A 全量铺开后再收紧为抛异常**，降低误伤面。

---

## 七、隔离审计定时任务（R-21-D，提升 `DbAuditProbe`）

- **复用提升**：将 `DbAuditProbe`（只读 JDBC 体检）的核心扫描逻辑提升为生产级 `@Scheduled` 任务（注意现有 26 个 `@Scheduled` 共用 3 线程池，R-07 治理前须独立分组，避免长任务互阻塞）。
- **审计项**：
  1. 所有"应隔离表"是否存在 `tenant_id IS NULL` 孤儿行（回填后应为 0）——复用 `DbAuditProbe.countGhostTenants` 思路。
  2. 校验白名单表确实无 `tenant_id` 列（防误加导致 `Unknown column`）。
  3. 周期性抽样：以两个不同租户上下文查询同一隔离表，断言结果集不相交。
- **产出**：审计日志表 `tenant_isolation_audit_log` + 失败即告警（对接现有告警通道）。

---

## 八、验收标准（Given-When-Then）

### R-21-A 公开端点租户解析
- **AC-A1**：Given 顾客端未登录访问公开菜品列表并携带 `storeId=X`，When `TenantResolver` 解析，Then `BaseContext.getCurrentTenantId()` = X 且返回该店菜品。
- **AC-A2**：Given 单租户自托管部署且请求无显式租户参数，When 访问公开端点，Then 回落到 `reggie.default-tenant-id`，不抛 `TenantContextMissingException`。

### R-21-B fail-fast
- **AC-B1**：Given 非白名单表查询且租户上下文为空（且非公开端点已解析），When 执行查询，Then 抛 `TenantContextMissingException` 而非返回空集。
- **AC-B2**：Given 白名单表（如 `tenant`/`permission`）查询，When 上下文为空，Then 正常返回（不受 fail-fast 影响）。

### R-21-C 两表隔离
- **AC-C1**：Given 两表 `tenant_id` 回填完成并移出白名单，When 运行隔离审计，Then `tenant_id IS NULL` 孤儿行数为 0。
- **AC-C2**：Given 两个不同租户上下文查询同一购物车表，When 取结果，Then 结果集不相交。

### R-21-D 审计
- **AC-D1**：Given 审计定时任务运行，When 发现某应隔离表存在 `tenant_id IS NULL` 孤儿，Then 写入 `tenant_isolation_audit_log` 并触发告警。

---

## 九、Non-goals（本期明确不做什么）

- ❌ **不在 R-21 做多租户合同差异 / 分账**（那是 R-14，M3–M4；R-21 只做隔离健壮性）。
- ❌ **不引入"超级租户哨兵"绕过插件**（避免全局关隔离的安全漂移；跨租户统一用 `@InterceptorIgnore`）。
- ❌ **不改动 `ai_provider_config` / `ai_prompt_template` 白名单**（R-14 决策⑤已确认系统级全局表，保留）。
- ❌ **不替换 MySQL/Redis 技术栈**（仅加列 + 配置 + 异常，不换引擎）。

---

## 十、待确认 / 风险

1. **公开端点租户解析方案（决策 A）**：顾客端如何选店（`storeId`/`tenantId` 参数 vs 主机头 vs 单租户默认）——**产品/架构须确认后才能翻 fail-fast**，是 R-21 的头号前置。
2. **`user` 表是否带 `tenant_id`**：回填 `shopping_cart`/`dish_evaluation` 依赖 `user_id→user.tenant_id` 关联；须先 `SHOW CREATE TABLE user` 确认（若 user 无 tenant_id，回填方案改为从 `shopping_cart` 现有 `store_id`/订单侧解析）。
3. **生产库列漂移**：`schema.sql` 显示两表已有 `tenant_id`，但生产库 `.gitignore` 忽略迁移脚本，须先核对生产真实结构再决定是否需要 ALTER 加列。
4. **`employee` / `ai_prompt_template` 白名单复核**：`employee` 当前由 Controller 手动过滤；`ai_prompt_template` 与 `ai_provider_config` 同源（注释"系统级…无 tenant_id"），倾向保留白名单，待显式确认（R-14 决策⑤同口径）。
5. **验证门槛**：742 测试强耦合真实 MySQL+Redis，本机无法全跑；R-21 合并前必须以真实中间件跑通全量回归 + 隔离专项用例，离线仅能验证编译。

---

## 十一、行动清单 + 数据来源索引

### 行动清单
| # | 行动 | 负责方 | 时间窗 |
|---|------|--------|--------|
| 1 | 决策 A 落地：确定公开端点租户解析方案（顾客端选店参数 / 单租户默认） | 产品 + 后端架构师 | M1 前（须先决） |
| 2 | R-21-A：实现 `TenantResolver` 注入公开端点租户上下文 | 后端架构师 + 资深后端 | M1（2026Q4） |
| 3 | R-21-B：新增 `TenantContextMissingException` + `getTenantId()` fail-fast + 收敛 `ignoreTable`/`TenantAspect` WARN | 资深后端 | M1（A 之后） |
| 4 | R-21-C：核对生产列结构 → 两表回填（幂等/可回滚）→ 移出白名单 | 资深后端 + DBA | M1（B 之后） |
| 5 | R-21-D：提升 `DbAuditProbe` 为 `@Scheduled` 隔离审计（独立线程池分组） | 资深后端 | M1–M2 |
| 6 | 真实 MySQL+Redis 全量回归 + 隔离专项用例（AC-A1/AC-B1/AC-C1/AC-D1） | QA + 后端 | M1 末 |

### 数据来源 & 成员产出索引
- **主理人（产品总监）**：R-21 与 R-14 边界复核、fail-fast 前置（公开端点租户解析）定位、公开端点风险（`DishController`/`LoginCheckFilter`）探查。
- **数析（数据分析师）**：代码事实（fail-closed `-1L`、`schema.sql` 两表已有 `tenant_id` 列、`DbAuditProbe` 审计底座、`@InterceptorIgnore` 逃逸范式、742 测试强耦合）。
- **析客（需求分析师 / 本文档作者）**：本《R-21 实施规格书》——目标 / 当前状态 / 设计决策 / 实施拆解 / 数据模型 / fail-fast 改造 / 审计 / 验收标准 / Non-goals / 待确认 / 行动清单。

---

> 本规格由产品战略团队 AI 协作生成。关键前置决策（公开端点租户解析方案、user 表 tenant_id 确认）须经产品负责人与架构师审定后实施；fail-fast 严禁在公开端点租户解析落地前翻面。
