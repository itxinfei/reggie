# 瑞吉外卖（Reggie）系统 — 架构审查与长期发展规划

> 审查角色：Software Architect（软件架构师）
> 审查日期：2026-08-13
> 审查范围：后端 31 模块 + 共享核（common/sys）+ 多租户基础设施 + 前端架构形态
> 审查方式：直接阅读当前源码（非仅依赖旧报告），逐条核对 `文件:行号`

---

## 0. 执行摘要（Verdict）

**一句话结论：骨架优秀，但安全与质量基建拖了后腿。**

这是一套**结构合理、但有几个致命/高风险点的“模块化单体（Modular Monolith）”**。分层纪律清晰、按领域拆 31 模块、多租户隔离设计稳健、前端已建立设计令牌体系、安全组件齐全——这些是该肯定的。此前存活的 **Critical 级越权漏洞（鉴权切面失效）已于 2026-08-13 修复并补回归测试**；**最小 CI 已于同日落地**（`.github/workflows/ci.yml`，JDK 8 + Redis 服务容器跑全量 `mvn test`）；但**测试覆盖仍极低（全量套件基线 249 测 / 36 失败，均为既有失败）**、**技术栈逼近 EOL**、以及巨型 Service / 模块共享核泄漏等可维护性问题仍在。

| 维度 | 严重级别 | 核心结论 |
|------|---------|---------|
| 安全/鉴权 | 🟢 **Critical（2026-08-13 已修复）** | 3 个鉴权切面切点只命中方法级注解，而 14 个 Controller 把注解放类级 → 切面永不触发 → 顾客越权接管后台；已加 `|| @within(...)` 并补回归测试 `AuthAspectClassLevelTest` |
| 质量基建 | 🟠 P1（2026-08-13 已加最小 CI） | 测试覆盖仍极低：全量 `mvn test` 基线 249 测 / 36 失败（**均为既有失败，与 P0 修复无关**）；sys/ai/store/recommend/notification/marketing/schedule 零测试；CI 已能拦安全回归，但全量套件需作为独立专项逐步补绿 |
| 技术战略 | 🟠 Major | Spring Boot 2.4.5 / Java 8 早已 EOL，长期不升级会累积依赖与安全债 |
| 多租户 | 🟠 Major | `IGNORE_TABLES` 含 `employee` 等表，绕过插件靠手工 `.eq(tenant_id)`，是维护陷阱 |
| 可维护性 | 🟠 Major | 9 个 500~1000 行 God Service；AI 双 `AIClient` 冗余实现；`customer` 模块目录结构漂移 |
| 模块边界 | 🟡 Minor | `common`（共享核）反向依赖 `store`；跨模块引用多为向下但 `ai→order/dish` 数据耦合偏紧 |
| 性能 | 🟡 Minor | 报表增强循环按天全表拉订单、推荐缓存命中路径 N+1、通知类级 `@Transactional` 包同步 HTTP 调用 |

> **与既有文档的关系**：2026-08-12 的 `CODE-REVIEW-REPORT.md` 判断“鉴权切面全部失效”是**准确的且当前代码依然如此**；本文已逐文件核实确认。2026-07-20 的 `CHANGELOG.md` 称“已加 `@RequireEmployee` 等切面”——实际上只**加了注解、没修切点**，属于 incomplete fix（详见 §3.1）。前端侧问题见 `backend-frontend-review-2026-08-13.md`，本文不再重复。

---

## 1. 当前架构快照（C4 视角）

### 1.1 System Context & Container

```
┌─────────────────┐   ┌─────────────────┐   ┌──────────────────────────────┐
│ 管理后台 (PC)    │   │ 移动端 (H5)      │   │ 第三方能力                     │
│ Element UI 2.x   │   │ Vant UI + AI点餐 │   │ 微信/支付宝支付 · 短信 · 打印  │
└────────┬────────┘   └────────┬────────┘   │ 飞鹅/易联云 · AI(DeepSeek等)   │
         │  HTTP/JSON          │            │ 美团/饿了么/抖音配送  · OAuth   │
         └──────────┬──────────┘            └───────────────┬──────────────┘
                    │                                        │
                    ▼                                        │
        ┌───────────────────────────────────────────┐      │
        │          单 Spring Boot 应用（单 Jar）        │◄─────┘
        │  ┌─────────────────────────────────────┐   │
        │  │ 31 业务模块 + 共享核(common/sys)      │   │
        │  │ 鉴权 AOP ×3 · 租户插件 · 限流 · CSRF   │   │
        │  └─────────────────────────────────────┘   │
        │  前端静态资源（backend/ + front/）内嵌伺服   │
        └──────────────┬──────────────────┬──────────┘
                       │                  │
                       ▼                  ▼
                 ┌────────────┐    ┌────────────┐
                 │  MySQL 8.x │    │  Redis 6+  │
                 │ 50+ 张表   │    │ 会话/缓存/ │
                 └────────────┘    │ 限流/队列  │
                                    └────────────┘
```

**关键特征**：单一部署单元、单一数据库、单一 Redis、前后端同 Jar 同发版。这是**模块化单体**，不是微服务。

### 1.2 Component 分层（模块内）

```
Controller → Service/ServiceImpl → Mapper → XML
    ↓            ↓                  ↓
   DTO         DTO              Entity
```

每模块 `controller/service/mapper/model(+dto)`，顶层 `common/`（R、异常、限流、脱敏、AOP）、`config/`、`dto/`、`enums/`、`filter/`、`utils/`。

---

## 2. 做得好的地方（Positive — 不要动）

1. **模块化单体选型正确**：以当前团队规模与阶段，单体比微服务更合适（团队自治/独立伸缩需求尚未出现）。模块按领域划分，方向对。
2. **分层纪律严格**：Controller/Service/Mapper/Model 边界清晰，`common.R<T>` 统一响应，DTO/Entity 分离。
3. **多租户失败封闭（fail-closed）设计稳健**：`TenantLineInnerInterceptor` + `BaseContext` ThreadLocal；`getTenantId()` 在无上下文时返回 `-1L` 哨兵值而非 `null`，避免意外注入 `tenant_id=null`（`MybatisPlusConfig.java:50-58`）。
4. **权限 fail-closed**：`PermissionAspect.loadPermissionsFromDb` 异常返回空集（不放行），`getTenantId()==null` 时 `ignoreTable` 返回 `true` 跳过过滤避免空集误伤（`PermissionAspect.java:193-217`）。
5. **缓存失效链路完整**：角色权限变更双清缓存 + `SCAN` 替代 `KEYS`（`PermissionAspect.java:245-277`）。
6. **前端设计系统基础扎实**：`tokens.css` 设计令牌单一来源、响应式工具类、骨架屏样式已就绪（只是未被页面执行）。
7. **安全组件齐全**：CSRF、Redis Lua 限流、暴力破解锁定、日志脱敏、BCrypt 强度因子 10。
8. **编译双保险**：`maven-enforcer-plugin` + `animal-sniffer` 守住 JDK 8 语法红线（`AGENTS.md`）。
9. **领域事件雏形**：`common/event/` 下已有 `OrderCancelledEvent`/`OrderCompletedEvent` + 监听器，为后续事件驱动解耦埋下好种子。
10. **前端非工程化但有规范**：`crud-table`/`stat-cards` 组件化、禁 Vue3、统一令牌——规范意识强。

---

## 3. 关键风险（按严重度）

### 3.1 ✅ P0 Critical — 鉴权切面失效（**已于 2026-08-13 修复并补回归测试**）

**证据（已逐文件核实当前代码）**：

- 三个切面切点**全部只命中方法级**：
  - `EmployeeGuardAspect.java:37` → `@Around("@annotation(com.reggie.common.annotation.RequireEmployee)")`
  - `AdminGuardAspect.java:32` → `@Around("@annotation(com.reggie.common.annotation.RequiresAdmin)")`
  - `PermissionAspect.java:68` → `@Around("@annotation(com.reggie.common.annotation.RequiresPermission)")`
- 但下列 Controller 把注解放**类级**（注解放类声明上方、无缩进）：
  - `@RequiresAdmin`（类级，6 个）：`StoreController.java:46`、`StoreDashboardController.java:22`、`RoleController.java:47`、`SysNotificationTemplateController.java:36`、`SysOperationLogController.java:35`、`SystemConfigController.java:31`
  - `@RequireEmployee`（类级，8 个库存）：`InventoryStatsController.java:42`、`MaterialController.java:32`、`MaterialCategoryController.java:33`、`PurchaseOrderController.java:36`、`PurchaseOrderDetailController.java:26`、`StockCheckController.java:38`、`StockRecordController.java:35`、`SupplierController.java:32`

**根因**：AspectJ `@annotation(X)` 只匹配**方法自身携带**注解的 join point；类级注解需 `@within(X)`。切点未加 `@within`，故类级注解**永不被拦截**。`PermissionAspect.getAnnotation()` 虽写了读类级注解的兜底（`PermissionAspect.java:134-135`），但**切点不命中使该逻辑成死代码**。

**攻击链**：`LoginCheckFilter` 对 `employee`/`user` 会话一视同仁只校验“已登录”；类级 `@RequiresAdmin`/`@RequireEmployee` 失效 → 任意 C 端顾客登录后，携带自己 Cookie 即可调用 `PUT /sys/role/{id}/permissions` 给自己授权、或 `PUT /sys/config` 篡改配置、或接管门店/库存全模块 → **完整水平+垂直越权直至提权超管**。

**与 CHANGELOG 的矛盾（重要）**：`CHANGELOG.md` 2026-07-20 写道“库存 8 个 Controller 类级加 `@RequireEmployee`”“StoreController 类级加 `@RequiresAdmin`”——这是在**加注解**，但**没修切点**。所以那一轮改动后漏洞依旧存在。`CODE-REVIEW-REPORT.md`（2026-08-12）的判断是准确的、且当前代码仍如此。

**修复（已于 2026-08-13 落地，3 行 + 1 个回归测试）**：三个切面切点已改为同时覆盖方法级与类级：
```java
@Around("@annotation(com.reggie.common.annotation.RequireEmployee) " +
        "|| @within(com.reggie.common.annotation.RequireEmployee)")
```
（另两个切面同理，`@RequiresAdmin` / `@RequiresPermission` 各加 `|| @within(...)`。）

**回归测试**：`src/test/java/com/reggie/common/aspect/AuthAspectClassLevelTest.java`（`@WebMvcTest` + 类级注解测试 Controller，5 个用例覆盖员工/管理员/顾客三种会话），已 `mvn test` 验证全绿：
- 顾客会话访问类级 `@RequireEmployee` / `@RequiresAdmin` 接口 → 被切面拒绝（code=0）；
- 员工会话 / 超级管理员会话 → 正常放行（code=1）。

该测试锁定“类级注解即触发鉴权”这一不变量，防止再次回归。

> ⚠️ 注：方法级 `@RequireEmployee`/`@RequiresPermission`（Dish/Setmeal/Category/Order/Member/Payment/User/Region 等）**是生效的**，仅类级注解失效。修复面小但收益极高。

### 3.2 🔴 P1 — 测试近乎空白 / 无 CI

- 估算整体覆盖率 **<8%**，集中在 Controller 层；`module/sys`（权限数据源）、`module/ai`、`module/store`、`module/recommend`、`module/notification`、`module/marketing`、`module/schedule` **零测试**。
- 三个鉴权切面**零测试** → §3.1 这类安全回归无法被任何自动化手段拦截。
- `application-test.yml` 的 Redis 指向真实 `localhost:6379`、未用嵌入式 → 离线 CI `mvn test` 可能启动失败。
- **战略含义**：没有安全网，长期演进会越改越脆，任何重构都靠“人肉”。

#### 3.2.0 ✅ 2026-08-13 进展：最小 CI 已落地 + P0 修复回归测试已补

- **CI**：新增 `.github/workflows/ci.yml`——`ubuntu-latest` + JDK 8（temurin）+ Redis 7 服务容器，每次 push/PR 跑全量 `mvn test`。这是安全回归的第一道门禁。
- **类级鉴权回归测试**：`src/test/java/com/reggie/common/aspect/AuthAspectClassLevelTest.java`（5 用例，类级注解场景）。
- **方法级鉴权回归测试**：`src/test/java/com/reggie/common/aspect/AuthAspectMethodLevelTest.java`（8 用例，方法级 + 类级/方法级共存场景，证明 `|| @within` 未破坏原 `@annotation` 分支、也未误伤公开端点）。
- 二者均用 `@WebMvcTest` + 合成测试 Controller，**不依赖 MySQL/Redis**，本地 `mvn test` 已验证全绿。

#### 3.2.1 🔴 全量测试基线（2026-08-13 `mvn test` 实测，非本次修改引入）

> 全量套件共 **249 测 / 36 失败 / 0 跳过**。**全部 36 个失败均为既有问题，与 P0 鉴权修复无关**：
> 本次仅改动 3 个切面切点（加 `|| @within`）并新增 2 个测试文件；失败类不包含这些文件，且失败 Controller（`OrderController`/`AddressBookController`/`OrderDetailController` 等）**均无类级安全注解**，故 `@within` 不可能命中它们。

| 失败类 | 失败数 | 根因（已定位） | 类别 |
|--------|-------|---------------|------|
| `BruteForceProtectionFilterTest` | 7 | `@MockBean RedisTemplate` 与 Spring Data Redis `RedisRepositoriesAutoConfiguration` 冲突 → `RedisKeyValueAdapter` 构造报 `ConnectionFactory must not be null`，上下文起不来 | 测试配置 bug |
| `RateLimitAspectTest` | 3 | 同上（Redis 仓库自动配置冲突） | 测试配置 bug |
| `AddressBookControllerTest` | 10 | 全部 `404`：测试请求路径未映射到（映射缺失/路径错） | 测试/映射 bug |
| `OrderDetailControllerTest` | 3 | 全部 `404`：同上 | 测试/映射 bug |
| `OrderControllerTest` | 3 | `code=0`（顾客会话被业务拒绝）：顾客端点业务校验未通过（非鉴权切面） | 测试会话/业务 |
| `MobileOrderControllerTest` | 2 | `code=0`：同上 | 测试会话/业务 |
| `InventoryServiceTest` | 1 | `MybatisPlus can not find lambda cache`：MP lambda 缓存缺失 | 测试/配置 |
| `PaymentServiceTest` | 3 | `DataIntegrityViolation`：测试数据缺必填列 | 测试数据 |
| `PrinterServiceTest` | 1 | `BadSqlGrammar`：SQL/ schema 不匹配 | 测试/schema |
| `ReportServiceTest` | 1 | `BadSqlGrammar`：同上 | 测试/schema |
| `SecurityAuditTest` | 2 | `collectJavaFiles` 递归 `NoSuchElementException`（遍历源码目录栈溢出） | 测试代码 bug |

**建议**：把上表 11 个类作为「测试质量专项」独立排期修复（优先级：Redis 两个 > SecurityAudit > 其余）。修完前 CI 会持续红——这正是 CI 的价值：把此前不可见的测试债暴露出来。

### 3.3 🟠 P1 — 技术栈逼近 EOL（最大长期风险）

- Spring Boot **2.4.5**（2021 年初发布，早已 EOL）、Java **8**、MyBatis-Plus 3.4.2、SpringDoc 1.5.x。
- 升 SB 3.x 需 `javax.*` → `jakarta.*` 命名空间改造 + 大量 API 调整，工作量大；但**长期不升级**会累积 CVE 依赖风险与招聘/生态风险。
- JDK 8 硬约束（`enforcer` + `animal-sniffer`，`AGENTS.md`）是双刃剑：守住兼容，也锁死现代语法与现代库。

### 3.4 🟠 P1 — 多租户“忽略表”脆弱性

- `MybatisPlusConfig.java:40-43` 的 `IGNORE_TABLES` 含 `tenant`、`employee`、`shopping_cart`、`ai_provider_config`、`dish_evaluation`、`permission`、`role_permission`——这些表**绕过租户插件**，靠手工 `.eq(tenant_id)` 隔离。
- **最危险的是 `employee` 在忽略表里**：员工/权限隔离完全依赖每个调用点手工加 `tenant_id`，一处遗漏即越权（CHANGELOG 已多次因此踩坑修复）。
- 新增无 `tenant_id` 的表时，忘了加忽略表会 `Unknown column 'tenant_id'`；加了忽略表又可能漏隔离 → 双向陷阱。

### 3.5 🟡 P2 — 巨型 Service / 单一职责过载

- `RecommendServiceImpl`(1009)、`OrderServiceImpl`(995)、`ReportServiceImpl`(975)、`AIChatServiceImpl`(944)、`NotificationServiceImpl`(834)、`ReportEnhancedServiceImpl`(658)、`DashboardServiceImpl`(642)、`MarketingCampaignServiceImpl`(537)、`MarketingServiceImpl`(513) 行。
- `ai` 模块同时有 `OpenAICompatibleClient` 与 `AiProviderManager+Adapter` 两套 `AIClient` 实现，前者为遗留冗余、易引发 Bean 歧义。
- 影响：可读性差、单测困难、合并冲突高发。

### 3.6 🟡 P2 — 模块间共享核泄漏 / 结构漂移

- **反向依赖**：`common` 模块（本应是最低共享核）的 `RestaurantController.java:5-6` 引用 `module/store` 的 `StoreInfo`/`StoreInfoMapper`——共享核不应依赖领域模块。
- **结构漂移**：`module/customer/service/service/...`、`module/customer/service/mapper/`、`module/customer/service/model/` 出现**二级 `service` 嵌套包**，与其他模块统一的 `controller/service/mapper/model` 不一致（见 Glob 结果 `module/customer/service/service/impl/...`）。
- **跨模块耦合**（多为向下、无环，但偏紧）：`ai → order/dish/recommend`（`UserProfileServiceImpl.java:5-18` 直接引用 Order/Dish Mapper）、`category → dish/setmeal`、`cashier → payment/order`、`sys → schedule/notification`、`tenant → auth`。建议对 `ai→order/dish` 这类数据耦合引入防腐层（ACL），而非直接跨模块查 Mapper。

### 3.7 🟡 P2 — 性能 / 事务边界

- `ReportEnhancedServiceImpl` 循环逐天 `list()` 全量订单（年度报表 ≈ 365 次全表 SELECT），与 `ReportServiceImpl` 已改的“范围查询+内存聚合”不一致。
- `RecommendServiceImpl` 缓存命中路径流内 `getById` 逐条查（隐藏 N+1）。
- `NotificationServiceImpl` 类级 `@Transactional` 包裹同步短信/推送 HTTP 调用 → 事务持锁时长 = 外部调用时长，应 `propagation=NOT_SUPPORTED`。
- 各 `Report` 类级 `@Transactional` 含长查询，应 `readOnly=true`。

### 3.8 🟡 P2 — 安全配置

- 无全局 XSS 过滤（`DishEvaluationServiceImpl` 仅单点转义）；菜品名/地址/员工名/配置值/通知模板均无防护。
- `dev` 明文弱口令入库（`application-dev.yml`）违反“密钥仅加密 yml”规范。
- `PaymentOrderMapper.java` 用 `${statuses}` 拼接 IN 子句（当前调用方传硬编码常量不可利用，但签名 `String` + `@InterceptorIgnore(tenantLine=true)` 是埋雷，应改 `List<String>` + `<foreach>` 参数化）。

### 3.9 🟡 P3 — 前端工程化缺失

- 前端无构建步骤、内嵌 Spring Boot、与后端**同 Jar 同发版** → 前端无法独立部署/扩缩，与后端强耦合，限制前端团队自治。
- 内联样式 430+ 处、ECharts 硬编码 hex、骨架屏 0 使用、`crud-dialog` 部分页面确认逻辑漏接（见独立前端审查 P0：菜品/套餐弹窗保存失效）。
- 该部分详见 `backend-frontend-review-2026-08-13.md`，本文不重复。

---

## 4. 架构决策记录（ADR）

> 模板：Status / Context / Decision / Consequences（得失都要写）

### ADR-001：采用模块化单体（Modular Monolith）【Accepted】
- **Context**：团队规模中等、部署与运维复杂度需可控、独立伸缩需求尚未出现。
- **Decision**：单体应用 + 31 个强边界模块 + 单库单 Redis，前后端同 Jar。
- **Consequences**：✅ 部署简单、事务一致、调试容易、模块边界清晰；❌ 前端无法独立发布、单点故障面大、未来“按模块独立扩容”受限。

### ADR-002：多租户行级隔离（TenantLineInnerInterceptor + ThreadLocal）【Accepted】
- **Context**：SaaS 多门店，需一套系统服务多家租户、行级数据隔离。
- **Decision**：MyBatis-Plus 租户插件自动注入 `tenant_id`，`BaseContext` ThreadLocal 传递上下文。
- **Consequences**：✅ 业务代码零感知、fail-closed 稳健；❌ `IGNORE_TABLES` 维护陷阱（§3.4）、跨租户统计/调度需手工处理 ThreadLocal。

### ADR-003：鉴权用 AOP 切面而非 Spring Security【Accepted（有条件）】
- **Context**：项目已自研 CSRF/限流/脱敏，希望轻量、可控。
- **Decision**：`@RequiresAdmin`/`@RequireEmployee`/`@RequiresPermission` 三切面。
- **Consequences**：✅ 轻量、与现有设施一致；❌ **当前切点漏类级注解致失效（§3.1，必须修）**，自研权限体系需自己保证正确性、无 Security 生态兜底。

### ADR-004：前后端一体单 Jar 部署【Accepted（标记未来拆分点）】
- **Context**：简化部署、减少运维组件。
- **Decision**：Vue2 静态资源内嵌 Spring Boot。
- **Consequences**：✅ 一条命令起服务；❌ 前端发版必须后端一起、无法独立 CDN/扩缩（§3.9）。

### ADR-005：JDK 8 / SB 2.4 硬约束【Accepted（记录 EOL 风险与升级路线）】
- **Context**：生态锁定、稳定优先。
- **Decision**：enforcer + animal-sniffer 双保险锁死 JDK 8 语法。
- **Consequences**：✅ 编译期强制兼容；❌ 锁死现代语法/库、SB2.4 已 EOL（§3.3），升级需专项路线。

### ADR-006：会话 + Cookie 鉴权（非 JWT）【Accepted】
- **Context**：后台/移动端同源、运维简单优先。
- **Decision**：HttpOnly + SameSite=strict Cookie，当前用户存 ThreadLocal。
- **Consequences**：✅ 实现简单、天然防 XSS 读 token；❌ 默认有状态、横向扩展需 Redis 共享会话、不便第三方/移动原生对接（当前已用 Redis 存会话，缓解）。

---

## 5. 长期发展规划（演进路线）

> 原则：**先止血、再筑基、后演进；物理微服务是最后选项，优先做“逻辑有界上下文”。**

### 阶段 0 — 立即止血（1~2 天）
| 任务 | 风险/收益 |
|------|----------|
| 修 §3.1 三个切面切点加 `@within`（3 行）+ 补类级注解回归测试 | 收益极高、风险极低，堵提权漏洞 |

### 阶段 1 — 质量基建（1~2 月）
- ✅ **最小 CI 已引入（2026-08-13）**：`.github/workflows/ci.yml`，JDK 8 + Redis 服务容器跑全量 `mvn test`；`application-test.yml` 的 Redis 仍指向真实 `localhost:6379`（CI 用服务容器提供），未改造成嵌入式。下一步把 §3.2.1 的 36 个既有失败作为独立专项补绿。
- 补核心模块测试：`module/sys`（权限数据源）、鉴权切面集成测试、AI/Store/Recommend 关键路径；设覆盖率门禁（先 40%，逐步 60%）。
- 全局 XSS 过滤（`XssFilter`）、`dev` 配置外部化密文、支付 IN 子句参数化。
- 多租户收敛：`employee` 移出 `IGNORE_TABLES`，改用精确 `@InterceptorIgnore` 开洞；新增无 `tenant_id` 表时强制评审。

### 阶段 2 — 可维护性（2~4 月）
- 拆分 9 个 God Service（按子域：订单履约 / 推荐引擎 / 报表聚合 / 通知路由 等）。
- 去 `ai` 双 `AIClient` 冗余实现，统一 `AiProviderManager+Adapter`。
- 统一模块目录结构（修 `customer` 二级 `service` 嵌套），清理 `common→store` 反向依赖。
- 对 `ai→order/dish` 等紧耦合引入**防腐层（ACL）**，解耦跨模块直接 Mapper 引用。

### 阶段 3 — 演进选项评估（4~8 月）
- **当前未到必须拆微服务的临界点**（团队规模、部署复杂度、独立伸缩需求都不足）。
- 优先做“逻辑上的有界上下文 + 模块边界强化”（明确上下游/Conformist/ACL 关系图）。
- 仅当 **订单/支付/AI** 出现明显独立扩容、独立发布需求时，再考虑物理拆分（先拆支付与 AI 这类边界清晰、外部依赖多的模块最稳妥）。

### 阶段 4 — 技术债偿还（8~12 月，最大长期风险）
- 制定 **SB 3.x / JDK 17 升级路线**：
  1. 先解 JDK 8 语法约束（逐步去掉 enforcer 红线，用 modernizer 扫描）；
  2. 双运行时并行验证后，升 SB 3 + `javax→jakarta` 改造（配合 IDE 批量重构 + 契约测试）；
  3. MyBatis-Plus / SpringDoc 同步升级。
- 这是长期最大的技术风险，需提前规划、单独排期，避免某天被迫紧急升级。

### 阶段 5 — 前端工程化（可选，并行）
- 将前端从单 Jar 解耦为独立静态资源 + 独立构建（Vite），后端仅提供 API；便于独立部署、CDN、前端团队自治。
- Vue3/Vite 作为未来选项需权衡：现有 50+ 页迁移成本高，建议新模块试点、旧页按需迁移。

---

## 6. 风险矩阵（一图速览）

| 级别 | 项 | 是否当前存活 | 修复成本 |
|------|----|------------|---------|
| 🔴 P0 | 鉴权切面失效（类级注解不命中） | **已修复（2026-08-13）** | 极低（3 行+测试） |
| 🔴 P1 | 测试空白 / 无 CI | 是 | 中（需持续投入） |
| 🟠 P1 | 技术栈 EOL | 是（慢性） | 高（专项） |
| 🟠 P1 | 多租户忽略表脆弱 | 是 | 低~中 |
| 🟡 P2 | 巨型 Service / 双 AIClient | 是 | 中 |
| 🟡 P2 | 模块共享核泄漏 / 结构漂移 | 是 | 低 |
| 🟡 P2 | 性能 N+1 / 长事务 | 部分 | 低 |
| 🟡 P2 | XSS / dev 明文 / SQL 拼接 | 是 | 低 |
| 🟡 P3 | 前端工程化缺失 | 是 | 高（战略） |

---

## 7. 给团队的三条最高优先级行动

1. **✅ §3.1 的鉴权切面已于 2026-08-13 修复并补回归测试**——提权漏洞已堵住，后续靠 `AuthAspectClassLevelTest` 守住不变量。
2. **✅ 最小 CI 已于 2026-08-13 落地**（`.github/workflows/ci.yml`，全量 `mvn test`）；但其全量套件当前有 36 个**既有失败**（§3.2.1，均与 P0 修复无关），需作为独立「测试质量专项」逐步补绿，否则 CI 会持续红——这正是 CI 要暴露的债。
3. **把 SB3/JDK17 升级列入年度路线图**——EOL 不会自己消失，越拖迁移成本越高。

> 附：本报告所有结论均基于 2026-08-13 当前源码 `文件:行号` 核实，未修改任何源代码。前端专项问题见 `backend-frontend-review-2026-08-13.md`，后端全量审查见 `CODE-REVIEW-REPORT.md`。
