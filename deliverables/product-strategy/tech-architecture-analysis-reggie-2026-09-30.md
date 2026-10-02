# Reggie 外卖系统 · 技术架构与实现现状分析（数析 / data-analyst）

> 探查范围：`D:/MyCode/reggie`，重点 `src/main/java`（869 个 .java，约 121,862 行）、`src/main/resources`（后端配置 + 前端静态资源）、`db/`、`pom.xml`。
> 方法：静态代码与配置审阅（Read / Grep / Glob / Bash），未做运行时压测/Profiling，性能结论为静态推断。
> 量化信号：39 个业务模块目录（AGENTS.md 称"31 个"，已漂移）、103 个 Controller、117 个 `@Mapper`（无 `@MapperScan`）、26 个 `@Scheduled`、9 个 `@Async`、4 个 `@Cacheable`（仅 2 个缓存域真正使用）、19 处 `setIfAbsent` 分布式锁（约 15 个类各自复制）、111–113 个测试类（约 23k 行，强依赖真实 MySQL+Redis）。最大类：`OrderServiceImpl` 2269 行、`AIChatServiceImpl` 1705、`CashierServiceImpl` 1428、`EmployeeController` 1336、`PaymentController` 1247、`ReportServiceImpl` 1225、`NotificationServiceImpl` 1204。

---

## 一、最关键的 5 条发现摘要

1. **会话未落地 Redis，水平扩容需"粘性会话"**：鉴权为 Servlet `HttpSession`（内存态），`application.yml:4-9` 仅配置 `session.timeout=30m / http-only / same-site=strict`，无 `spring-session` / `RedisHttpSession`。多实例部署下会话不共享，必须靠网关 sticky session 或会话复制，否则用户频繁掉登录——这是可扩展性最大瓶颈。
2. **Redis 连接池偏小且被放进请求热路径**：`application-dev.yml:43-59` 中 `lettuce.pool.max-active=8`，但 `RateLimitAspect` 对每个 `@RateLimit` 接口在**请求线程**同步执行 Lua 限流脚本；缓存双删、AI 探活、幂等锁也都挤在这 8 条连接上。高并发下 8 连接 + 3s `max-wait` 会成为请求瓶颈，且 Redis 抖动会直接阻塞 HTTP 线程。
3. **上帝类 + 跨模块耦合严重**：`OrderServiceImpl`（2269 行）构造器注入了来自 13+ 个模块的服务（`OrderServiceImpl.java:12-49`），`EmployeeController` 1336 行、`PaymentController` 1247 行、`ReportServiceImpl` 1225 行均严重超胖，单点改动风险与回归面巨大。
4. **配置与 SQL 不进版本库 + 无 Flyway**：`application*.yml` 与 `db/migration/*.sql` 被 `.gitignore` 忽略（AGENTS.md:44-46），环境靠本地文件漂移；无自动迁移，表结构变更无版本契约。
5. **文档与代码现实持续漂移**：AGENTS.md 声称"31 个模块 / MyBatis-Plus 3.4.2"，实际为 39 个模块目录、pom 为 3.5.3.1；前端 crud-table 对齐约定在文档里写"金额/数字一律居中"，但 `components.js:844-847` 代码仍对 `money/number` 返回 `right`——规范与实现不一致，易引发回归争议。

---

## 二、性能维度

### P1. 数据库访问：连接池偏小且缺少关键配置
- **现象/证据**：`application-dev.yml:11-24` Druid `max-active: 20`、`max-wait: 60000ms`，未见 `pool-prepared-statements`/`max-open-prepared-statements`（PreparedStatement 缓存未开），未见 `remove-abandoned`/`log-abandoned`（连接泄漏难排查）。Tomcat 默认 200 工作线程 vs DB 池 20 → DB 池先成瓶颈，`max-wait=60000` 意味着争用时最多阻塞 60s。
- **影响**：大事务/慢 SQL 会快速占满 20 连接，200 个 HTTP 线程排队等连接，接口雪崩；无预编译缓存，高频相同 SQL 重复硬解析。
- **建议**：按业务峰期压测上调 `max-active`（如 50–100），开启 `pool-prepared-statements`，将 `max-wait` 收敛到 3–5s 以快速失败；接入 Druid `remove-abandoned` 与慢 SQL 日志（已开 `slow-sql-millis=1000`，可告警）。

### P2. 缓存防护不完整：缓存穿透 + 击穿/雪崩短板
- **现象/证据**：`RedisConfig.java:163` `disableCachingNullValues()`——空结果不缓存，恶意/大量不存在 key 直接穿透到 DB；仅 `setmeal` 用 `@Cacheable(sync=true)`（`SetmealServiceImpl.java:103`）防击穿，其余无 `sync`；无随机 TTL 抖动（防雪崩全靠固定 15m/1h）、无布隆过滤器。`@Cacheable` 全仓仅 4 处（setmeal 2 + systemConfig 2），`RedisConfig.java:175` 声明的 `permissions` 缓存域（1h TTL）**无任何 `@Cacheable(value="permissions")` 引用 → 死配置**。
- **影响**：setmeal/systemConfig 之外的读路径基本无缓存；穿透风险真实存在；`permissions` 缓存配置形同虚设。
- **建议**：对高基数/可枚举 key 引入空值短 TTL 或布隆过滤；为 `systemConfig`、热点字典统一加 `sync`/`@Cacheable`；删除或真正接入 `permissions` 缓存（见 M4）。

### P3. 授权走 DB 而非缓存，且权限服务类级事务放大连接占用
- **现象/证据**：`PermissionServiceImpl.java:34` **类级 `@Transactional(rollbackFor=Exception.class)`**（连只读 `getAllPermissions()` 也进事务，见 :54-56 直接 `permissionMapper.listAllEnabled()`）；其声明的 `sys:permissions:` 缓存键（:44-47）在解析路径上未见读取。鉴权切面 `PermissionAspect` 每次请求调权限查询。
- **影响**：每个需鉴权请求都开 DB 事务 + 查权限表，在 20 连接池下进一步吞噬连接；类级事务对纯读方法浪费事务资源。
- **建议**：权限解析结果按 `employee/role` 维度缓存（用现有 `permissions` 缓存域或 `sys:permissions:` 键落地），`@Transactional` 下沉到写方法、读方法标 `readOnly=true`。

### P4. 异步/定时：调度线程池仅 3 线程 + 自研锁逻辑散落 15+ 类
- **现象/证据**：`SchedulingConfig.java:40` `Executors.newScheduledThreadPool(3)` 承载全部 26 个 `@Scheduled`；扫描型任务（订单超时、未接单、库存退款补偿、优惠券过期等）若在 3 线程上互相挤占会饿死。`setIfAbsent` 分布式锁在 ~15 个类各自复制（如 `UnacceptedOrderScanTask.java:90`、`StockRefundCompensationTask.java:310`、`OrderTimeoutTask.java:356` ×4、`PlatformRetryTask.java:266` ×2……共 19 处），锁释放多为 TTL 自过期、无统一看门狗。
- **影响**：长任务 + 3 线程易互相阻塞；锁模式复制导致语义不一致（有的用 Lua 释放、有的纯 TTL），且锁 TTL 到期而任务未跑完时另一实例可重复执行（多实例重复处理）。
- **建议**：提炼统一 `RedisDistributedLock`（含 owner 校验 + 续期 watch-dog），按任务类型分组线程池；对扫描任务加批处理 + 时间分片，避免单任务长时间占线程。

### P5. 同步阻塞型重操作：图片/报表/AI 仍在请求线程
- **现象/证据**：`@Async` 仅 9 处，集中在缓存双删、推荐缓存、AI 探活（`AsyncConfig.java` 三池）。报表导出（POI 4.1.2 / itextpdf）、图片上传（`application.yml:28-31` 上限 10MB/12MB）、`AIChatServiceImpl` 同步 HTTP 调用外部 LLM（`OpenAICompatibleAdapter.java:70` `HttpURLConnection`，依赖 `aiHealthProbeExecutor` 仅做探活）均为请求线程同步执行。
- **影响**：大报表/批量导出/AI 对话会长时间占用 HTTP 线程与 DB 连接（在 20 连接约束下尤为敏感），高并发导出易拖垮整体。
- **建议**：导出/批量任务离线化（提交任务 + 异步生成 + 文件下载/回调）；AI 对话走流式（SSE，已有 `sse` 配置）避免同步长等待；上传走对象存储 + 异步转码。

### P6. 分页封顶已具备，但仅兜底、非约束
- **现象/证据**：`PageUtils.java:19` `MAX_PAGE_SIZE=100`，`of/cap` 可封顶；但仅部分 Controller 使用，未强制所有列表接口经 `PageUtils`，裸 `new Page<>()` 仍可能存在。
- **影响**：个别接口若绕过 `PageUtils` 透传超大 pageSize，仍可拖垮 DB（尤其报表类聚合查询）。
- **建议**：在统一响应/BaseController 或 MyBatis 拦截器层强制 pageSize 上限，弱化对逐接口人工约束的依赖。

---

## 三、可维护性维度

### M1. 上帝类与跨模块耦合
- **现象/证据**：`OrderServiceImpl` 2269 行且构造器注入 13+ 模块服务（`OrderServiceImpl.java:12-49`）；`EmployeeController` 1336、`PaymentController` 1247、`ReportServiceImpl` 1225、`NotificationServiceImpl` 1204、`RecommendServiceImpl` 1050、`OrderStatusFlowServiceImpl` 960 均超胖。大量 `@Autowired(required=false)` 可选服务暗示"一处汇聚多方逻辑"的反模式。
- **影响**：单文件 review/测试困难，改动爆炸半径大，新人上手成本高。
- **建议**：按聚合根拆分（下单、状态机、计费、优惠核销各自限界上下文）；引入防腐层隔离跨模块依赖；对 Controller 按资源/用例进一步拆分。

### M2. 文档与代码现实漂移（AGENTS.md / CLAUDE.md / README）
- **现象/证据**：AGENTS.md:27 称"31 个模块"，实际 39 个目录；AGENTS.md:11 称"MyBatis-Plus 3.4.2"，pom 为 3.5.3.1。前端约定 AGENTS.md:38-40 要求"金额/数字一律居中"，但 `components.js:844-847` 仍 `return 'right'`（且 :461 注释仍写 money/number 默认右对齐）。
- **影响**：规范不可信，评审/分工时易被"文档"误导，回归标准摇摆（AGENTS.md 自己承认默认值在 center↔left 间多次回退）。
- **建议**：以代码为唯一事实来源，文档改为自动生成或 CI 校验（如 crud-table 对齐策略写进代码常量并单测），杜绝"文档描述期望态而非现状"。

### M3. 配置与 SQL 不进版本库 + 无 Flyway
- **现象/证据**：`application*.yml`、`db/migration/*.sql` 被 `.gitignore` 忽略（AGENTS.md:44-46），本地存在但非版本化；无 Flyway 依赖，迁移靠手动执行参考脚本。
- **影响**：环境间配置漂移无法追溯，新人/CI 无法重建一致环境；表结构演进无版本契约，多环境易"表不存在"类故障。
- **建议**：将非敏感配置纳入版本库（敏感项走环境变量/配置中心），引入 Flyway/Liquibase 管理 schema 版本。

### M4. 测试对真实外部依赖强耦合，CI 难跑
- **现象/证据**：AGENTS.md:5 明确 742 个测试连真实 `localhost:3306/reggie` + `localhost:6379`，`surefire reuseForks=true`；测试 schema 靠 `@Sql` + `@DirtiesContext`。
- **影响**：无本地 DB/Redis 即无法跑测试，CI 需起全套中间件，构建脆弱、慢、难并行。
- **建议**：核心单测用 Testcontainers/H2/嵌入式 Redis 或 Mock 解耦；仅集成测试连真实库，分层跑；引入 CI 门禁（当前 AGENTS.md:4 称"无 CI、无 lint/format 门禁"）。

### M5. 依赖老化、JDK8 硬约束的安全/现代化阻碍
- **现象/证据**：`pom.xml`：Spring Boot 2.4.5（2021 年发布，官方已 EOL，无安全补丁）、MyBatis-Plus 3.5.3.1、Druid 1.2.21、Hutool 5.8.22、POI 4.1.2、itextpdf 5.5.13.3、wechatpay-java 0.2.14、alipay-sdk 4.39.79、springdoc 1.5.13；JDK8 硬约束（禁 `var`/`record`/`List.of`/`jakarta.*`）。
- **影响**：已知 CVE 无法随补丁升级；无法享用 Spring Boot 3 / Jakarta / 虚拟线程等现代化能力；安全合规压力大。
- **建议**：在不突破 JDK8 红线的前提下，先滚动升级到同代可兼容的最新补丁版（Spring Boot 2.7.x LTS、Druid/POI 安全版）以消除已知 CVE；将"升级路径评估"列为专项。

### M6. Mapper 注册仪式感重
- **现象/证据**：AGENTS.md:12 "无 `@MapperScan`，Mapper 接口逐接口 `@Mapper` 注册"，全仓 117 个 `@Mapper`。
- **影响**：新增 Mapper 漏写 `@Mapper` 即无法注入，隐性约定易踩坑。
- **建议**：在启动类加 `@MapperScan("com.reggie.module.**.mapper")`，移除逐接口注解。

### M7. 前端：设计令牌体系完整但"遗留"脏数据未清
- **现象/证据**：`tokens.css` 主体为良好单一事实来源（:1-209），但含 120+ 行 `--legacy-<hex>` 占位令牌（:360-487，由 `_tokenize_css.py` 自动收敛硬编码 hex 生成），且并存多套样式文件（`common.css`/`common-base.css`/`unified-components.css`/`design-system.css`/`consistency.css`/`components.css` 等），`tokens.css` 内仍保留 `--antd-*`/`--el-*` 双库调色板与 `--gray-333/666/999` 等散列灰阶。
- **影响**：令牌体系虽建，但"禁止硬编码"未彻底落地，长尾 `--legacy-*` 仍是语义真空的色值别名；多 CSS 入口易样式冲突、打包体积膨胀。
- **建议**：对 `--legacy-*` 做一次语义归并（映射到品牌/语义令牌），收敛 CSS 入口到 1–2 个主文件，CI 校验"禁止出现裸 hex"。

---

## 四、可扩展性维度

### E1. 会话/状态无分布式化，阻碍水平扩容
- **现象/证据**：`LoginCheckFilter.java:78-100` 用 `request.getSession()`（Servlet 内存会话）；无 `spring-session`/`RedisHttpSession`；`CsrfFilter`/`SecurityHeaderFilter` 亦基于单机上下文。`BaseContext` 用 ThreadLocal 在 `finally` 清理（:108），跨线程/异步需 `TaskDecorator` 手动搬运（AsyncConfig 各池均复制该逻辑）。
- **影响**：多实例必须 sticky session；无法无损滚动发布；异步链路上下文传递脆弱。
- **建议**：引入 Spring Session + Redis 统一会话（与现有 Redis 复用），或迁移到无状态 JWT/网关鉴权，从根本上解耦"会话=本机内存"。

### E2. 无消息队列/事件总线规模化为短板
- **现象/证据**：全仓仅 4 个文件使用 Spring `publishEvent`/`@EventListener`（`CashierServiceImpl`、`OrderCancelledEventListener`、`OrderCompletedListener`、`OrderStatusFlowServiceImpl`），无 Kafka/RabbitMQ/Redisson Topic；异步全靠进程内线程池 + `TaskDecorator`。
- **影响**：跨服务/跨实例最终一致性依赖同步调用或定时任务补偿；实例重启会丢失未完成的异步任务；无法削峰填谷。
- **建议**：对"下单→通知→积分→库存"等场景引入领域事件 + 持久化队列（至少逻辑事件总线 + 落库补偿），提升解耦与可靠性。

### E3. 供应商/渠道扩展机制分化：支付/AI 较好，推荐/报表/打印偏弱
- **现象/证据**：
  - **支付**：`module/payment/channel/` 有清晰 `PaymentChannel` 接口 + `PaymentChannelFactory` + `AlipayChannel`/`WechatPayChannel` + `real/RealAlipayChannel`/`WechatV3PayChannel` + `notify/NotifyUrlRouter`，mock/real 分离，扩展点清晰。
  - **AI**：`module/ai/` 有 `AiProviderManager` + `BaseModelAdapter` + `OpenAICompatibleAdapter`(927 行)/`AnthropicAdapter`/`BaiduAdapter` + `failover/`(AiFailoverExecutor) + `tool/` + `rag/`，provider 以 DB `ai_provider_config` 驱动，扩展性好。
  - **推荐/报表/打印/通知**：`RecommendServiceImpl` 1050、`ReportServiceImpl` 1225、`NotificationServiceImpl` 1204 为单体大服务，未见 Strategy/Factory 拆分（推荐算法、报表类型、打印驱动均内聚一处）。
- **影响**：新增支付/AI 供应商成本低；新增推荐算法、报表维度、打印厂商需改大类，回归风险高。
- **建议**：将支付/AI 的"接口 + 工厂 + 配置驱动"范式推广到推荐（策略模式）、报表（模板/Builder）、打印（驱动适配器），降低单体服务改动面。

### E4. 租户隔离健壮性：10 张表绕过插件，部分表物理无 `tenant_id`
- **现象/证据**：`MybatisPlusConfig.java:49-52` `IGNORE_TABLES` 含 10 张表（tenant、employee、shopping_cart、ai_provider_config、ai_prompt_template、dish_evaluation、permission、role_permission、region、store_sync_log）；其中 `shopping_cart`、`dish_evaluation`、`ai_provider_config` **物理上无 `tenant_id` 列**，靠 `userId`/全局共享隔离；插件对空上下文返回 `-1L`（fail-closed，:67-75）使查询返回空集而非报错。
- **影响**：多租户数据物理共表，存在跨租户串数据与查询语义歧义风险；白名单遗漏会以"静默空结果"而非明确错误暴露，难以察觉。
- **建议**：对确实需隔离的表（shopping_cart/dish_evaluation）补 `tenant_id` 列并纳入插件；将"白名单 + fail-closed"改为可观测的强校验（未知表 + 空上下文直接告警/拒绝），并补齐集成测试覆盖跨租户隔离。

### E5. 缺少开放 API / 灰度 / 特性开关能力
- **现象/证据**：统一响应 `R<T>`（`common/R`）、鉴权注解（`@RequiresAdmin`/`@RequiresPermission`/`@RequireEmployee`）契约稳定；但全局检索未见 feature-flag/灰度路由/开放 API 网关层。
- **影响**：新功能只能全量发布，无法按租户/比例灰度，回归风险集中于生产。
- **建议**：引入特性开关（配置中心驱动）+ 灰度路由；对外暴露能力走版本化开放 API 契约。

---

## 五、优化空间清单（按维度标注）

**性能（P）**
1. [P] Druid 上调 `max-active`（建议 50–100）、开启预编译缓存、收敛 `max-wait` 到 3–5s、开 `remove-abandoned`（`application-dev.yml:11-24`）。
2. [P] Redis lettuce 池 `max-active: 8` 提至 16–32，并将 `RateLimitAspect` 限流改为异步/旁路，避免占满请求热路径连接（`application-dev.yml:49-54`）。
3. [P] 接入缓存穿透防护（空值短 TTL 或布隆过滤）+ 热点缓存加 `sync`；清理死配置 `permissions` 缓存域或真正落地（`RedisConfig.java:163,175`）。
4. [P] 权限解析结果按员工/角色缓存，权限服务 `@Transactional` 下沉、读方法 `readOnly=true`（`PermissionServiceImpl.java:34,54-56`）。
5. [P] 报表/导出/AI 对话异步化与流式化，避免同步长占用 HTTP 线程与 DB 连接（AIChatServiceImpl / export 模块）。
6. [P] 统一 `PageUtils` 分页封顶到框架层，杜绝裸 `new Page<>()` 透传超大 pageSize（`PageUtils.java:19`）。

**可维护性（M）**
7. [M] 拆分上帝类（Order/EmployeeController/PaymentController/Report/Notification 等），按限界上下文与防腐层解耦跨模块依赖。
8. [M] 收敛文档漂移：AGENTS.md 模块数/MP 版本、crud-table 对齐策略以代码为事实源并加 CI 校验（`components.js:844-847` vs AGENTS.md:38-40）。
9. [M] 配置与 SQL 纳入版本库 + 引入 Flyway/Liquibase 做 schema 版本化（`AGENTS.md:44-46`）。
10. [M] 测试解耦真实中间件（Testcontainers/H2/嵌入式 Redis），分层跑并接入 CI/lint 门禁。
11. [M] 滚动升级安全补丁版（Spring Boot 2.7.x LTS、Druid/POI 安全版），立项评估 JDK/3.x 升级路径（`pom.xml`）。
12. [M] 加 `@MapperScan` 移除逐接口 `@Mapper`（117 处仪式）。
13. [M] 前端清理 `--legacy-*` 令牌、收敛 CSS 入口、CI 禁止裸 hex（`tokens.css:360-487`）。

**可扩展性（E）**
14. [E] 会话 Redis 化（Spring Session）或迁无状态鉴权，解除水平扩容对 sticky session 的依赖（`LoginCheckFilter.java:78-100`）。
15. [E] 引入持久化事件总线/消息队列支撑跨实例最终一致性与削峰；将支付/AI 的"接口+工厂+配置驱动"范式推广到推荐/报表/打印；补足租户隔离（白名单表补 `tenant_id` + 强校验告警）（`MybatisPlusConfig.java:49-52`、各单体服务）。

---

*说明：以上均为基于源码与配置的直接技术事实，含文件:行号引用；未做运行时压测/Profiling，性能结论为静态推断，建议以压测数据复核 P1–P3。本报告未修改 Reggie 任何源码，仅落盘为独立分析文档。*

---

## 六、四区速览（TL;DR / 核心结论卡片 / 行动清单 / Non-goals）

### TL;DR
Reggie 后端工程量大（869 类 / ~121.8k 行 / 39 模块），业务功能完整、支付与 AI 供应商扩展机制良好；但**会话非分布式、连接池与限流热路径、上帝类+文档漂移**三处分别卡住"扩容能力 / 高并发可用性 / 长期迭代速度"，需优先治理。性能结论为静态推断，落地前以压测复核。

### 核心结论卡片（3 大瓶颈 · 按优先级）
| 优先级 | 维度 | 瓶颈 | 关键证据 |
|---|---|---|---|
| P0 | 可扩展性 | 会话内存态、无分布式会话 → 必须 sticky session，水平扩容/无损发布被卡死 | `LoginCheckFilter.java:78-100`；`application.yml:4-9` |
| P0 | 性能 | Druid `max-active=20` 对抗 200 线程 + `max-wait=60000`；Redis lettuce 池仅 8 却让限流 Lua 在请求线程同步跑 → 雪崩/阻塞风险 | `application-dev.yml:11-24`、`application-dev.yml:43-59`；`RateLimitAspect.java` |
| P1 | 可维护性 | 上帝类（OrderServiceImpl 2269 行注入 13+ 模块）+ 文档/代码漂移（AGENTS.md 31模块↔实际39、`components.js` 对齐 center↔right） | `OrderServiceImpl.java:12-49`；`components.js:844-847` vs `AGENTS.md:38-40` |

### 行动清单（建议顺序）
1. **P0-扩**：会话 Redis 化（Spring Session）或迁无状态鉴权 → 解耦"会话=本机内存"。
2. **P0-性**：Druid `max-active` 提至 50–100 并开预编译缓存、`max-wait` 收敛 3–5s；Redis 池提至 16–32 并将限流改异步/旁路。
3. **P1-维**：拆分上帝类（按限界上下文）+ 收敛文档漂移（CI 校验代码为事实源）；配置/SQL 纳入版本库 + Flyway。
4. **P1-性**：缓存穿透防护（空值 TTL/布隆）+ 权限结果缓存；报表/导出/AI 异步化与流式。
5. **P2**：提炼统一 `RedisDistributedLock`；支付/AI 的"接口+工厂+配置驱动"范式推广到推荐/报表/打印；补齐租户隔离（白名单表补 `tenant_id` + 强校验）。

### Non-goals（本报告不覆盖）
- 不输出最终 PRD / 产品路线图（仅技术现状与优化空间证据）。
- 未做运行时压测 / Profiling / 慢 SQL 执行计划分析（性能结论为静态推断）。
- 未做安全渗透测试、依赖 CVE 逐项核验（仅指出 EOL/老化风险，升级路径需专项评估）。
- 未评估前端构建产物体积、首屏性能与包依赖；前端结论限于 crud-table 对齐与 tokens 令牌体系。
- 未涉及具体 SQL 索引建议与执行计划调优（属 DBA / 专项性能进一步优化范畴）。
