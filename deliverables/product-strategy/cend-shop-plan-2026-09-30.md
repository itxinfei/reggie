# Reggie · C 端店铺品牌化 + 公开端点租户解析 · 实施计划（Todo 清单）

**日期**：2026-09-30
**类型**：实施计划 / Todo（由用户问题"C 端名字无法更改 / 多店铺路径与名字 / 连锁还是自营"触发）
**来源调研**：见 `r21-tenant-isolation-hardening-2026-09-30.md` 决策 A 及补充调研（2026-09-30）
**关联需求**：R-21-A（公开端点租户解析，M1 P0）、R-14（多租户，M3–M4，本期不做选店列表页）
**估算汇总**：**约 3–3.5 人周**（Phase 1 约 1 / Phase 2 约 1–1.5 / Phase 3 约 0.5 / Phase 4 约 0.5）

---

## 〇、问题与代码事实（全部已核实，带文件:行号）

| # | 问题 | 代码证据 |
|---|------|----------|
| P1 | 匿名顾客端外卖首页 = 空菜单 + 写死店名"瑞吉外卖" | `LoginCheckFilter.java:132-146` 匿名公开请求不设租户上下文；`DishController.java:144-146` `tenantId==null` 时跳过手动过滤 → 插件兜底 `tenant_id=-1` → 空集 |
| P2 | 店名唯一来源 `tenant` 表，但改名仅超管接口，无商家自助页 | `RestaurantController.java:83-93`（tenant 表是唯一名称来源）；`TenantController.java:156-160`（`PUT /tenant` + `@RequiresAdmin`）；backend 前端无"店铺设置"页 |
| P3 | C 端 20+ 处写死"瑞吉外卖"：`<title>`、匿名店名回落、公告文案 | `front/index.html:9` 等全部页面 `<title>`；`RestaurantController.java:84`（匿名回落）、`:127`（公告写死） |
| P4 | logo 动态化零成本（写死注释是错的） | `RestaurantController.java:94` 注释称"均无 logo 列"——**实际 `tenant` 表已有 `logo` 列**（`schema.sql:594`、`Tenant.java:52`） |
| P5 | 正面范本：堂食扫码已实现"参数→解析租户→公开查询" | `DiningTablePublicController.java:60-68`（`/api/dining/table/public/{tableId}/menu`，"租户由桌台反查确定，匿名可访问"） |
| P6 | 品牌动态化可一处覆盖全端 | `front/js/base.js` 被 `index.html:12` 与全部 25 个 `page/*.html` 加载（head 早期执行） |

---

## 一、目标（3 个）

| # | 目标 | 验收信号 |
|---|------|----------|
| G1 | **匿名顾客端可浏览真实店铺**：未登录打开外卖首页能看到默认租户（或指定店铺）的菜单与真实店名 | 匿名 GET `/restaurant/info`、`/category/list`、`/dish/list` 返回真实数据，非空集 |
| G2 | **商家自助改店铺**：店长（员工登录）可在后台改店名/logo/公告/营业时间，改完 C 端即时生效 | 后台"店铺设置"页保存成功；C 端首页标题/头部店名/公告同步更新 |
| G3 | **C 端零写死品牌**：20+ 处 `<title>` 与公告由数据驱动 | 全端 `document.title` 与公告来自 tenant/store_info；代码中"瑞吉外卖"仅剩兜底默认值 |

---

## 二、Phase 0 · 决策项（开工前须确认，共 3 条）

- [ ] **D1 · 匿名租户解析方案**（推荐 A+B 组合）
  - A. **请求参数**：公开端点支持 `?tenantId=` 或 `?storeId=`（storeId 经 `store_info` 反查 tenantId，参照堂食"桌台反查"模式 P5）；
  - B. **默认租户兜底**：配置 `reggie.public-tenant-id`（Environment 读取，**默认空 = 行为完全不变**，保证回归安全）；覆盖"一实例=一家店"的自托管主形态。
  - 多租户共享实例的"店铺列表页选店"**不做**（归 R-14，M3–M4）。
- [ ] **D2 · 店铺设置权限**（推荐：员工即可改，限定当前租户）
  - `PUT /restaurant/settings` 用 `@RequireEmployee`（任何本租户员工/店长），**只允许改** name / logo / notice / businessHours 四个字段，**强制以 `BaseContext.getCurrentTenantId()` 为准**，忽略 body 里的 tenantId——杜绝越权改他租户。`status`/`phone`/`passwordType` 等敏感字段不在白名单内，仍走超管 `PUT /tenant`。
- [ ] **D3 · notice 公告存储位置**（推荐：`store_info` 加列）
  - `store_info` 新增 `notice varchar(200)`（营业公告本属门店运营信息）；同步改 `src/test/resources/schema-mysql.sql` + 对应 `schema-<module>.sql`（AGENTS.md 硬性要求：新增列必须同步测试 schema，否则测试报表不存在）。
  - 备选：塞进 tenant 表（不推荐——公告是门店级不是合同主体级）。

---

## 三、Phase 1 · 公开端点租户解析（R-21-A 核心，约 1 人周）

- [ ] **T1.1 `LoginCheckFilter.restoreExcludeContext` 增强**（`filter/LoginCheckFilter.java:132-146`）
  - 现逻辑：仅当会话已有 tenantId 才恢复。增强为三级回落：
    1. 会话恢复（不变）；
    2. 失败 → 读请求参数 `tenantId`（或 `storeId` → 经 `WebApplicationContextUtils` 取 `StoreService` 反查，模式同 `RiderRememberTokenService` 的取 bean 方式）→ `BaseContext.setCurrentTenantId(...)`；
    3. 仍无 → 读 Environment 属性 `reggie.public-tenant-id`（取 bean `Environment`），有值则设置。
  - **红线**：三级全空时**必须保持现状**（不设置，交给插件 `-1` 兜底）——保证未配置环境的绝对回归安全。
- [ ] **T1.2 配置项与文档**：`reggie.public-tenant-id` 由代码 `@Value`/Environment 读取（`application*.yml` 不进版本库，故用代码默认值 + Swagger/README 说明，勿只写在 yml）。
- [ ] **T1.3 堂食链路回归确认**：`/api/dining/table/public/**`（`AuthConstants.java:96`）自解析租户，不受 T1.1 影响；匿名首页与扫码页互不干扰。
- [ ] **T1.4 验收**：匿名 `GET /category/list?storeId=X` 返回 X 店分类；配置默认租户后匿名首页出菜单与真实店名；不配置不带参时返回空集（与现在一致）。

## 四、Phase 2 · 店铺设置（后端 + 管理端，约 1–1.5 人周）

- [ ] **T2.1 DDL**：`store_info` 加 `notice varchar(200) NULL COMMENT '门店公告'`
  - `db/20260930_store_info_notice.sql`（手工迁移脚本，项目无 Flyway）；
  - 同步 `src/test/resources/schema-mysql.sql`（store_info 表定义处）与 `schema-store.sql`（若存在该模块 schema）+ 检查 `@Sql` 引用。
- [ ] **T2.2 `StoreInfo` 实体加字段** `private String notice;`（`module/store/model/StoreInfo.java`）。
- [ ] **T2.3 商家自助接口**：`RestaurantController` 新增 `PUT /restaurant/settings`（`@RequireEmployee`）
  - 请求体仅收 `name`/`logo`/`notice`/`businessHours` 四字段（DTO 校验：name 非空 ≤64，对齐 `Tenant.java:32-34` 约束；notice ≤200）；
  - 更新目标租户 = `BaseContext.getCurrentTenantId()`（**不用 body 的 tenantId**）；
  - name/logo 写 `tenant` 表（`TenantService.updateById`，仅 set 白名单字段）；notice/businessHours 写 `store_info`（`StoreService.findByTenantId` 后 update，无记录则新建）；
  - 返回 `R<String>`；加 `@RateLimit` 防刷（参照 `:142` 写法）。
- [ ] **T2.4 管理端"店铺设置"页**：`backend/page/shop-settings.html` + 对应 api js
  - Vue2 + Element UI，遵循既有页面骨架（iframe 壳层、`js/request.js`、tokens.css 设计令牌，禁硬编码 hex）；
  - 表单四字段 + 图片上传（logo 复用既有 `el-upload` 上传通道）；
  - 后台菜单加入口（按现有菜单注册方式）；页面列对齐规范不涉及（表单页）。
- [ ] **T2.5 `info()` 去硬编码**（`RestaurantController.java`）
  - `:127` 公告改读 `storeInfo.getNotice()`，空则回落现文案；
  - `:94-95` logo 改读 `tenant.getLogo()`（列已存在，P4），空则回落 `images/common/logo.png`；删除过时注释。
- [ ] **T2.6 验收**：店长登录后台改四字段 → C 端匿名/登录首页店名、logo、公告、营业时间全部更新；越权字段（status/phone）不可改；他租户员工改不到本店。

## 五、Phase 3 · C 端去硬编码（约 0.5 人周）

- [ ] **T3.1 `front/js/base.js` 增加品牌动态化**（一处覆盖全端 25 页）
  - 逻辑：读取 sessionStorage 缓存（key 如 `brandInfo`，含 `{name, logo}`，TTL 如 10 分钟）→ 无缓存则 `GET /restaurant/info`（已在 `LOGIN_EXCLUDE_URLS`，匿名可用）→ `document.title = document.title.replace('瑞吉外卖', name)`；头部有 `.rc-name`/logo 元素的页面同时替换文本与 `img.src`（存在才替换，防御式）。
  - 失败静默（网络异常不阻断页面），写死文案保留为最终兜底。
- [ ] **T3.2 `index.html` 头部**：`restaurantInfo.logo` 有值时替换 `<img class="rc-logo">` 的 src（`:144`）；无值保持静态资源。
- [ ] **T3.3 清点**：`grep -c "瑞吉外卖" front/` 预期仅剩 base.js 兜底与 tokens/注释类；`page/*.html` 的 20+ 处 `<title>` **不改文件**（由 T3.1 运行时替换），避免逐页 diff。
- [ ] **T3.4 验收**：任意 C 端页面 `document.title` 显示真实店名；断网/接口失败时仍显示"瑞吉外卖"兜底。

## 六、Phase 4 · 验证与回归（约 0.5 人周）

- [ ] **T4.1 离线编译**：`mvn -o compile`（JDK 8，系统 mvn）通过。
- [ ] **T4.2 全量回归**：本地起真实 MySQL(localhost:3306/reggie) + Redis(localhost:6379) 后 `mvn test`（742 用例）；若 T2.1 加列，确认测试 schema 同步无"表不存在/列不存在"。
- [ ] **T4.3 手工冒烟清单**：① 匿名首页（默认租户配置开/关各一次）；② 匿名带 `?storeId=`；③ 扫码堂食点餐；④ 后台改店铺设置 → C 端刷新生效；⑤ 顾客登录后首页；⑥ 后台其他页面回归（管理端 shell 未被新页破坏）。
- [ ] **T4.4 文档同步**：AGENTS.md 若涉及（新增公开接口白名单不变，无需改）；`.workbuddy/memory` 记录。

---

## 七、风险与红线

| 风险 | 缓解 |
|------|------|
| `restoreExcludeContext` 改动波及所有公开端点（登录/静态资源/回调等 40+ 条白名单路径） | 三级回落全部失败时**行为不变**；回调类端点（`/api/payment/notify/**` 等）无参数无配置即不设上下文，与现状一致 |
| storeId 反查被伪造枚举他店 | 反查仅取 `store_info.tenant_id`（只读公开字段，同堂食范本 `DiningTablePublicController.java:22`"不暴露内部数据"），不返回敏感信息 |
| 加列引发测试失败 | 严格按 AGENTS.md：DDL 同步进 `schema-mysql.sql`/`schema-<module>.sql` + `@Sql` 检查 |
| JDK 8 语法红线 | 全程禁 `var`/`List.of`/text block 等；`javax.*` |
| 前端规范 | 新页走 tokens.css 设计令牌，禁硬编码 hex；不引 Vue3/TS |

## 八、完成定义（DoD）

1. 匿名顾客端（配置默认租户或带参）能浏览真实店铺菜单/店名/logo/公告/营业时间；
2. 店长在后台"店铺设置"改四字段，C 端刷新即生效；越权不可行；
3. C 端 `document.title`/公告全部数据驱动，写死值仅剩兜底；
4. `mvn -o compile` 通过 + 真实中间件 `mvn test` 全绿 + 冒烟六项通过；
5. 未配置任何新配置项的既有部署，行为与上线前**完全一致**（零破坏升级）。

---

> 决策 D1–D3 确认后即可按 T1.1→T4.4 顺序开工；Phase 3 与 Phase 2 的 T2.1–T2.3 可并行。本计划与 R-21 规格决策 A 同源，完成后 R-21-B（fail-fast 翻面）的前置即告解除。
