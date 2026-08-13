# Reggie 外卖系统 — AI 开发指引

## 构建与命令
- Maven 构建（**无 wrapper**，用系统 `mvn`），Java 1.8；**无 CI、无 lint/format 门禁**。
- `mvn test`：单元/集成测试，H2 内存库（`MODE=MYSQL`），无需 MySQL；部分 `@SpringBootTest` 会连 Redis（localhost:6379）。
- `mvn verify`：额外跑 animal-sniffer 字节码级 JDK 8 兼容检查（用 JDK 9+ 会在 enforcer 阶段直接失败）。
- 本地运行：`mvn spring-boot:run`（需本地 MySQL `reggie` 库 + Redis）。入口：后台 `/backend/index.html`、移动端 `/front/index.html`、Swagger `/swagger-ui.html`。

## 技术栈
### Java 后端
- Spring Boot 2.4.5（**禁止升级到 3.x**，需 Jakarta 命名空间）、MyBatis-Plus 3.4.2 + Druid、Redis + Commons Pool2、MySQL 8.x（测试用 H2）、Lombok 1.18.20、Hutool 5.8.22、SpringDoc OpenAPI 1.5.13、Maven，Java 1.8。
- Mapper 接口用 `@Mapper` 注解逐接口注册（**无 @MapperScan**）。

### 前端（均无编译步骤，静态资源由 Spring Boot 直接伺服，改完刷新即生效）
- 管理后台 `src/main/resources/backend/`：Vue 2 + Element UI（原生 JS）、ECharts、RemixIcon。
- 用户端 `src/main/resources/front/`：Vue 2 + Vant。
- API 统一走 `api/*.js` → `js/request.js`（Axios 封装）；**禁止引入 Vue 3 / Composition API / TypeScript / Vite**。
- 颜色一律引设计令牌 `styles/tokens.css`，禁硬编码 hex；前端 UI 规范另见 `.codebuddy/rules/前端.mdc`。

## 工程约定
### JDK 1.8 硬约束
- 禁止 JDK 9+ 语法：`var`、`String.isBlank()`、`List.of()`、`Map.of()`、`switch` 表达式、`record`、`sealed`、text blocks；禁止 `jakarta.*`，必须用 `javax.*`。
- 替代：`StringUtils.isBlank()`（Hutool/Commons-Lang3）、`Arrays.asList()`、`Collectors.toList()`。
- `pom.xml` 已配 animal-sniffer + enforcer（`[1.8,1.9)`）双保险。

### 后端分层与模块化
- 已重构为 31 个模块：`com.reggie.module.<module>`（controller / mapper / model / service），如 ai、payment、printer、recommend、report、store、sys、delivery、dining、inventory、member 等；`common/`（R 响应、全局异常、限流、日志脱敏）、`config/`、`dto/`、`enums/`、`filter/`（LoginCheckFilter）、`utils/` 在顶层。新增业务代码先进对应 module。
- REST 统一响应 `com.reggie.common.R<T>`。

### 鉴权与租户（易踩坑）
- 登录是 **Session + Cookie**（HttpOnly、SameSite=strict），非 JWT；当前用户存 ThreadLocal。
- 接口鉴权注解：`@RequiresAdmin`（仅超管）、`@RequiresPermission`、`@RequireEmployee`（员工会话）。**混合公开/顾客端点的 Controller（Dish/Setmeal/Category/Order 等）只能方法级加 `@RequireEmployee`，不能类级**，否则挡掉公开端点。
- MyBatis-Plus 租户插件自动注入 `tenant_id`；`permission`、`role_permission` 两表**无 tenant_id**，已在 `MybatisPlusConfig.IGNORE_TABLES`，查这两表须走专用 Mapper 或原生 SQL，否则 `Unknown column 'tenant_id'`。
- 分页用 `PageUtils.of/cap`（上限 100），禁止裸 `new Page<>` 或不封顶透传 pageSize。

### crud-table 列对齐规范（强制，易回归）
- **唯一对齐来源**：`js/components.js` 的 `resolveColAlign(col)`，禁止在页面列配置里零散写 `align`（除非覆盖默认）。
- **默认（2026-08-13 起，用户要求"尽量都居中"）**：文本列 → `center`；金额/数字列（`type: 'money'`/`'number'`）→ `right`；页面显式 `align` 优先。
- **表头**：模板已用 `:header-align="resolveColAlign(col)"` 跟随数据列，禁止单独给表头写死对齐。
- **禁止翻转默认值**：默认分支曾在 center ↔ left 间多次回退，每次翻转都改变所有未显式 `align` 列的行为。当前规范 = 全局居中，仅金额/数字右对齐；改动须同步更新 `.codebuddy/memory/MEMORY.md`。
- **CSS 三态单一来源**：`styles/consistency.css` 的 `.is-left/is-center/is-right .cell`（均 `!important`），禁止在 components.css/page.css 再写 `text-align`。
- **选择器陷阱**：`components.js` 模板 `<el-table class="tableBox">` 使 `.tableBox` 与 `.el-table` 是**同一元素**，`.tableBox .el-table xxx`（后代选择器）永远不匹配，必须写 `.tableBox.el-table xxx`（复合选择器，无空格）。

## 文件与 Git 注意（非显而易见）
- **`application*.yml` 与 `db/migration/*.sql` 均被 `.gitignore` 忽略、不在版本库**（`.gitignore` 只放行 `src/test/resources/*.sql`）。改配置/表结构后不会被 git 跟踪，属本地维护。
- **没有 Flyway 依赖**：`db/migration` 的 `V<日期>__描述.sql` 只是参考脚本（手动执行），不会自动迁移。
- 新增表时测试必须同步：基础表进 `src/test/resources/schema.sql`，模块表进 `schema-<module>.sql`，测试类用 `@Sql` 加载 + `@DirtiesContext(BEFORE_EACH_TEST_METHOD)`，否则测试报"表不存在"。
- Mapper XML（复杂查询）放 `src/main/resources/com/reggie/<包路径>/`，与 Java 包同路径（不在 resources/mapper/）。
- AI 供应商/密钥在数据库 `ai_provider_config` 表管理（yml 中 `reggie.ai.*` 已废弃，仅兜底）；短信/推送 dev 默认 mock-mode 不真发。

## 参考
- Java 命名/结构/异常/注释规范：`docs/CODE_STANDARDS.md`。
- 项目详细记忆（含编译验证铁律）：`.codebuddy/memory/MEMORY.md`。
- E2E 套件在 `tests/`（Playwright + TS + Allure），需先启动服务。
