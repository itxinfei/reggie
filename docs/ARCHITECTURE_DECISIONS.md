# 架构与关键设计决策

---

## 1. 分层架构

```
Controller → Service → ServiceImpl → Mapper → XML
     ↓          ↓          ↓         ↓       ↓
   DTO        DTO       Entity    Entity  SQL
```

| 层 | 命名规则 | 示例 |
|---|---|---|
| Controller | `*Controller` | `EmployeeController` |
| DTO (请求) | `*DTO` | `EmployeeLoginDTO` |
| DTO (响应) | `*VO` | `EmployeeVO` |
| Service 接口 | `*Service` | `EmployeeService` |
| Service 实现 | `*ServiceImpl` | `EmployeeServiceImpl` |
| Mapper 接口 | `*Mapper` 继承 `BaseMapper<Entity>` | `EmployeeMapper` |
| Mapper XML | `*Mapper.xml`（按需） | `EmployeeMapper.xml` |
| Entity | 与表名对应 | `Employee` |

### 模块内目录规范

```
module/{模块名}/
├── controller/       # REST 控制器
├── service/
│   └── impl/         # Service 实现
├── mapper/           # MyBatis-Plus Mapper 接口
├── model/            # 模块实体类
├── dto/              # 模块私有 DTO
├── platform/         # 第三方平台适配（如 delivery）
├── provider/         # 外部服务提供者（如 AI、通知）
├── channel/          # 支付渠道
├── core/             # 核心工具（如打印引擎）
├── config/           # 模块配置
└── task/             # 定时任务
```

---

## 2. 统一响应结构 `R<T>`

**位置**: `com.reggie.common.R`

**字段**:
- `code` — 状态码
- `message` — 提示信息
- `data` — 业务数据
- `timestamp` — 服务器时间
- `requestId` — 链路追踪 ID

**状态码约定**:

| Code | 含义 |
|---|---|
| 200 | 成功 |
| 401 / NOTLOGIN | 未登录 |
| 403 | 无权限 |
| 422 | 业务异常（CustomException） |
| 500 | 系统异常（Exception 兜底） |

---

## 3. 异常处理机制（**关键决策**）

**位置**: `com.reggie.common.GlobalExceptionHandler`

| 异常类型 | HTTP Status | 返回 code | 用途 |
|---|---|---|---|
| `CustomException` | 422 | 422 | 业务异常，向用户返回实际消息 |
| `MethodArgumentNotValidException` | 400 | 400 | 参数校验失败（`@Validated`） |
| `ConstraintViolationException` | 400 | 400 | 单参数校验失败 |
| `Exception`（兜底） | 500 | 500 | 未知异常，统一"系统繁忙" |

### 关键决策：`RuntimeException` 一律改为 `CustomException`

**问题**: `throw new RuntimeException("业务错误消息")` 会被 `@ExceptionHandler(Exception.class)` 兜底返回 500 + "系统繁忙"，**业务错误消息丢失**。

**决策**: 项目内所有 `throw new RuntimeException` 全部替换为 `throw new CustomException`（2026-08 迁移完成，`grep -rn "throw new RuntimeException" src/main/java` 返回空）。

**已迁移文件（10 个）**:
- `common/CsrfTokenUtil.java` — 生成 CSRF Token 失败
- `module/ai/service/impl/AiProviderConfigServiceImpl.java` — API 密钥加密失败（2 处）
- `module/cashier/service/impl/CashierServiceImpl.java` — 日结业务（重复日结/记录不存在/已取消，3 处）
- `module/dining/service/impl/QueueServiceImpl.java` — 排队锁获取失败
- `module/export/util/ExportUtil.java` — 导出失败（5 处）
- `module/member/controller/FrontCouponController.java` — NOTLOGIN
- `module/platform/service/impl/PlatformReconcileTaskServiceImpl.java` — 租户上下文缺失
- `module/platform/service/impl/PlatformSyncServiceImpl.java` — 平台同步执行失败（2 处）
- `module/report/service/impl/ReportServiceImpl.java` — 报表导出失败
- `utils/SMSUtils.java` — 短信 4 处

---

## 4. 多租户隔离

### 4.1 上下文（`BaseContext`）

**ThreadLocal** 存储：
- `BASE_CONTEXT_EID` — 当前员工 ID
- `BASE_CONTEXT_TID` — 当前租户 ID

每次请求由 `LoginCheckFilter` 在登录校验通过后写入，请求结束清理。

### 4.2 MyBatis-Plus 租户拦截器

**位置**: `com.reggie.config.MybatisPlusConfig`

通过 `TenantLineInnerInterceptor` 全局在 SQL 中自动注入 `tenant_id = ?` 条件：
- SELECT：追加 `WHERE tenant_id = ?`
- INSERT：自动填充 `tenant_id` 字段
- UPDATE：追加 `WHERE tenant_id = ?`
- DELETE：追加 `WHERE tenant_id = ?`

### 4.3 手动绕过租户拦截（`@InterceptorIgnore`）

在以下场景**必须**加 `@InterceptorIgnore(tenantLine="true")`：

| Mapper | 场景 |
|---|---|
| `UserMapper` | C 端用户查询（无租户上下文） |
| `OperationLogMapper` | 审计日志（fail-closed） |
| `DashboardMapper` | 看板聚合（5 处） |
| `RecommendationFeedbackMapper`、`RecommendationCacheMapper`、`BrowseHistoryMapper` | 推荐/浏览（无租户上下文） |
| `DeliveryOrderMapper` | 平台配送回传 |

> **风险**: 绕过拦截器后，SQL 中必须**手动添加** `tenantId = #{tenantId}` 条件，否则构成越权。

### 4.4 无租户字段例外表

- `tenant`（根表）
- `region`（全局行政区划）

---

## 5. 事务约定

**规范**: 全部使用 `@Transactional(rollbackFor = Exception.class)`（不写 `rollbackFor` 时仅回滚 `RuntimeException`，会漏回滚 checked exception）。

**全局 174 处**已统一补齐。

**高并发业务（TOCTOU 修复）**:
- 财务对账（`generateReconciliation`）与利润分析（`generateProfitAnalysis`）：`ConcurrentHashMap` + `synchronized` 串行化
- 收银日结（`executeSettlement`）：幂等锁防重复日结
- 平台订单同步：去重键 `platformOrderId`

---

## 6. 安全机制

### 6.1 过滤器链（`filter/`）

| 过滤器 | 顺序 | 作用 |
|---|---|---|
| `SecurityHeaderFilter` | `@Order(0)` | HTTP 安全头（CSP、nosniff、X-Frame-Options DENY、X-XSS-Protection、Referrer-Policy、HSTS） |
| `RequestCachingFilter` | — | 请求体缓存（可重复读取） |
| `CsrfFilter` | — | CSRF Token 校验 |
| `LoginCheckFilter` | — | 登录校验，未登录返 `NOTLOGIN` |
| `BruteForceProtectionFilter` | — | 暴力破解防护 |
| `TraceIdFilter` | — | 链路追踪 ID 生成 |

### 6.2 密码与加密

- **员工密码**: BCrypt（`PasswordUtils`）
- **多租户敏感值**: Jasypt `ENC(...)` 加密（`application-prod.yml`），`JASYPT_ENCRYPTOR_PASSWORD` 环境变量注入
- **Jasypt 配置**: `config/JasyptConfig.java`，`@Profile("prod")`，`@Bean("jasyptStringEncryptor")` 命名规范被 starter 检测，`@Value` 无默认值 fail-closed
- **AI API Key**: 加密存储（`AiProviderConfig`）

### 6.3 限流与防重放

- **注解**: `@RateLimit`（`common/RateLimit.java`）
- **切面**: `RateLimitAspect`（`common/RateLimitAspect.java`）
- **底层**: Redis Lua 脚本（`scripts/`），原子操作防竞态
- **场景**: 登录防暴力、短信验证码、高频接口

### 6.4 CSRF 防护

- **Token 生成**: `CsrfTokenUtil.generateToken()` — 32 字节随机 + 时间戳 + Base64 URL 编码
- **验证**: `CsrfTokenUtil.validateToken()` 使用 `MessageDigest.isEqual`（防时序攻击）
- **注入**: axios 请求拦截器（`js/request.js`、`js/export.js`）

### 6.5 日志脱敏

- `LogMaskUtils.maskPhone()` — 手机号脱敏
- `LogMaskUtils.maskIdCard()` — 身份证脱敏
- 全项目统一使用，**禁止**手写正则

### 6.6 XSS 防护

- `@XssFilter` 或 `XssHttpServletRequestWrapper`
- HTML 转义由前端 + 后端双重防护
- Element-UI `v-html` 使用需谨慎

### 6.7 生产安全头（`SecurityHeaderFilter`）

| Header | 值 | 说明 |
|---|---|---|
| `Content-Security-Policy` | 适配 Vue2 内联脚本 | 严格 CSP |
| `X-Content-Type-Options` | `nosniff` | 禁 MIME sniffing |
| `X-Frame-Options` | `DENY` | 禁 iframe 嵌套 |
| `X-XSS-Protection` | `1; mode=block` | 浏览器 XSS 过滤 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | 严格 referrer |
| `Strict-Transport-Security` | 按 profile 分级 | HSTS 仅生产启用 |

---

## 7. 幂等与并发（TOCTOU 修复）

**TOCTOU（Time-of-Check-Time-of-Use）**: 检查条件与执行之间存在竞态窗口，可能导致重复生成、重复发放等。

### 已修复的 TOCTOU 场景

| 场景 | 方案 |
|---|---|
| 财务对账 `generateReconciliation` | `ConcurrentHashMap` + `synchronized` 串行化 |
| 利润分析 `generateProfitAnalysis` | 同上 |
| 收银日结 `executeSettlement` | 幂等键 + 状态检查（重复日结抛 CustomException） |
| 平台订单同步 | 唯一键 `platformOrderId` 去重 |
| 订单/支付/退款/配送 | `@Version` 乐观锁 |

### 遗留风险（未加乐观锁但涉及金额）

- `Member.balance` — 会员余额扣减
- `DailySettlement` — 日结
- `CostRecord` — 成本记录
- `PurchaseOrder` — 采购单状态

---

## 8. 关键工具类

| 类 | 位置 | 用途 |
|---|---|---|
| `BaseContext` | `common/` | ThreadLocal 上下文 |
| `CustomException` | `common/` | 业务异常 |
| `GlobalExceptionHandler` | `common/` | 全局异常处理器 |
| `R` | `common/` | 统一响应结构 |
| `PasswordUtils` | `common/` | BCrypt 加密 |
| `LogMaskUtils` | `common/` | 日志脱敏 |
| `VerifyCodeUtils` | `common/` | 验证码 |
| `CsrfTokenUtil` | `common/` | CSRF Token |
| `MyMetaObjectHandler` | `common/` | MP 元数据填充 |
| `RedisCacheUtil` | `common/` | Redis 缓存 |
| `ObjectMapperHolder` | `common/` | Jackson 单例（避免多次创建） |
| `JacksonObjectMapper` | `common/` | Jackson 配置 |
| `BatchFillHelper` | `common/` | 批量填充工具 |
| `ApiClient` | `utils/` | 二维码生成 |
| `SMSUtils` | `utils/` | 阿里云短信 |
| `DateUtils` | `utils/` | 日期工具 |
| `ExportUtil` | `module/export/util/` | Excel/PDF 导出（SXSSF 流式，防 OOM） |

---

## 9. 定时任务（`module/schedule/task/`）

| 任务 | 用途 |
|---|---|
| `CouponExpirationTask` | 优惠券到期处理 |
| `*ReconcileTask` | 平台对账 |
| `*RetentionTask` | 留存分析 |
| 其他 | 见 `schedule/` 目录 |

**多租户注意**: 定时任务需手动设置/清理 `BaseContext` 的 ThreadLocal。

---

## 10. 导出机制（`ExportUtil`）

- **Excel**: `SXSSFWorkbook(100)` 滑动窗口 100 行，防 OOM
- **PDF**: 横向 A4，中文字体多级 fallback（STSong-Light → SimSun → SimHei → STSong → Helvetica）
- **统一入口**: `ExportController`，`/export/orders/excel` 等
- **LIMIT 保护**: 所有导出接口 `LIMIT 10000` 防 OOM

---

## 11. 关键设计观察（**待改进点**）

1. **前缀约定不统一**: `/backend/`、`/front/`、`/api/`、`/admin/`、以及无前缀共存，需核实 `LoginCheckFilter` 覆盖范围。
2. **`@Version` 覆盖偏窄**: 仅 4 个核心实体，其他金额变更实体无保护。
3. **命名双风格**: `createTime` vs `createdTime`、`createUser` vs `createdUser`，MyBatis-Plus 元数据填充器兼容但代码可读性受影响。
4. **重复类级前缀**: `/marketing` 被 `MarketingController` 与 `MarketingCampaignController` 共用，`/orders/platform` 被 `PlatformOrderPullController` 与 `PlatformSyncController` 共用。
5. **逻辑删除不一致**: 部分财务/报表类表无 `@TableLogic isDeleted`。
6. **C 端与后台混布**: `UserController` 同时承载 C 端登录（`/user/login`）与后台管理（`/user/page`），权限隔离依赖路径判断。
7. **文档语言不一致**: `ReportEnhancedController` 的 `@Operation(summary=...)` 全为英文，其余中文。
8. **`@InterceptorIgnore` 越权风险面**: 7 个 Mapper 绕过租户拦截，需人工确认每个 SQL 都手动加了租户过滤。

---

## 12. 已完成的安全修复里程碑

| 时间 | 内容 |
|---|---|
| 2026-08 | P0-1~P0-5：日志脱敏/验证码上限/密码加密/XSS/SQL 注入 |
| 2026-08 | P1-1~P1-7：状态机/支付校验/CSRF/审计日志/文件上传/越权/防重放 |
| 2026-08 | P2-1~P2-5：依赖升级/超时配置/限流/输入校验/日志规范 |
| 2026-08 | P3-3：44 个实体添加 `@TableField(fill = FieldFill.INSERT)` tenantId 注解 |
| 2026-08 | P3-4：导出接口添加 `LIMIT 10000` 防 OOM |
| 2026-08 | P3-5：统一 `LogMaskUtils.maskPhone()` |
| 2026-08 | P3-7：员工批量状态更新添加 tenantId 条件防越权 |
| 2026-08 | P3-8：4 个核心实体（Orders/PaymentOrder/RefundRecord/DeliveryOrder）添加 `@Version` + 数据库迁移 SQL |
| 2026-08 | P3-9：13 页内联 URL 迁移至 API 模块 |
| 2026-08 | TOCTOU：财务对账 + 利润分析 + 收银日结并发修复 |
| 2026-08 | 阶段 3：174 处 `@Transactional` 补齐 `rollbackFor = Exception.class` |
| 2026-08 | 阶段 3：HTTP 安全头配置（`SecurityHeaderFilter`） |
| 2026-08 | 阶段 3：Jasypt 集成（`@Profile("prod")`） |
| 2026-08 | 全部 `throw new RuntimeException` → `throw new CustomException` 迁移完成 |

**当前测试状态**: 402 个测试全部通过（100%）。
