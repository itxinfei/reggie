# Reggie · R-14 多租户（合同差异）+ 总部-门店管控 + 按合同类型分账结算 · 实施规格书

**日期**：2026-09-30
**类型**：功能实施规格书（PRD 式，承接路线图 R-14）
**作者**：析客（需求分析师 / 产品战略团队）
**关联需求**：R-21（租户隔离健壮性，M1 前置）、R-09（Flyway 版本化）、R-15（全渠道四通）、R-16（会员私域）
**定位基线**：Reggie 是给餐饮商家"自己部署、自己用"的开源 / 可二开系统；唯一不做的是"公有云 SaaS 平台 + 我们收订阅费"。**多租户隔离本身是刚需（已部分存在），本期强化而非新建。**

---

## 📌 TL;DR（执行摘要）

- **核心命题**：同一自托管实例需服务多家**不同合同关系**的店铺（直营 / 加盟 / 租用 / 租赁），既能数据强隔离，又能按合同类型分账结算——命中商家"数据自主 / 连锁管控 / 规避平台抽佣"痛点。
- **复用优先**：现有 `tenant` 表、`BaseContext`（`TenantLineInnerInterceptor` + `getCurrentTenantId()`）已就位；`store_info` 已有 `store_type`+`parent_tenant_id` 层级、`store_sync_log`+`StoreSyncService` 已有总部→门店下发机制——本期是"扩展 + 加固 + 接通结算"，**不另起炉灶**。
- **安全护栏**：`MybatisPlusConfig` 当前"空上下文返回 `-1L` 致静默空集"（:70-78，:101-106）是真实串数据风险，由 **R-21（M1，P0）先行交付 fail-fast + 两表（shopping_cart/dish_evaluation）`tenant_id` 回填**（`ai_provider_config` 经核验为系统级全局表，保留白名单、不补列，见决策⑤）；R-14-B 仅做白名单收敛与合同感知审计，避免与 R-21 重复计工。
- **资金明确后置**：分账只做到"规则计算 + 分账单 / 对账单输出"，**不接任何真实资金通道**（银行 / 三方支付），`billing` 订阅计费模块已删除（:47），本期不复活。
- **投入与位置**：R-14 合计 **17–24 人周**，落 M3–M4（与 R-15/R-21 协同），是 R-14 竞争力主线的实体交付。

---

## 🎯 核心结论卡片

| 项目 | 内容 |
|------|------|
| 推荐方案 | **复用现有租户基座**：A 扩展数据模型（tenant 加 `contract_type`/`parent_tenant_id`/`contract_id` + 新建 `contract`/`settlement_rule`/`settlement_bill`）→ B 收敛隔离白名单 + 合同感知审计（R-21 已交付基底）→ C 总部-门店管控（复用 `store_info` 层级 + `store_sync_log` 下发）→ D 按合同类型分账规则引擎（镜像 `PaymentChannelFactory` 工厂范式，只出账单不接资金） |
| 优先级 | **P0（竞争力）**。但**隔离加固（R-21）先于 R-14 于 M1 落地**，是 R-14 的硬前置；分账资金通道后置为 P1+ 待决项 |
| 预期影响 | 单实例服务多家不同合同关系店铺（含租用 / 租赁 / 联营 / 代销）；数据强隔离防事故；直营全额 / 加盟分账 / 租用收费 / 联营代销分账多种模式可结算；连锁总部统一管控 + 跨店看板 |
| 资源需求 | **17–24 人周**（R-14-A 6–8 / R-14-B 3–4 / R-14-C 4–6 / R-14-D 4–6），建议预留 15% 安全垫；后端架构师 + 资深后端 + DBA |
| 风险等级 | **中**。隔离改动面覆盖 39 模块回归；历史 `tenant_id` 回填（仅 shopping_cart/dish_evaluation 两表）存在脏数据风险；合同类型 / 分账粒度 / 租用开票 / 总部冲突规则等开放问题已由用户于 2026-09-30 审定（见第十节决策①~⑤） |

---

## 一、产品目标（3 个正交目标）

> 三个目标两两正交，分别对齐"自托管定位 / 多租户合同差异 / 按合同分账"，共同服务于"数据自主 + 连锁管控 + 规避抽佣"。

| # | 目标 | 对齐定位 | 成功信号 |
|---|------|----------|----------|
| G1 | **单实例多合同关系强隔离** | 自托管 + 多租户（租用 / 租赁使隔离成为必需） | 任意租户无法读到他租户业务数据；缺上下文访问隔离表必报错而非空集 |
| G2 | **总部-门店按合同关系管控** | 连锁数据自主 / 管控 | 总部可聚合下属门店、统一下发菜单 / 库存 / 价格，跨店看板可用 |
| G3 | **按合同类型分账结算（无资金通道）** | 规避平台抽佣 + 数据自主 | 直营 / 加盟 / 租用三模式规则可配、分账单 / 对账单可输出、可线下核对 |

---

## 二、用户故事

### US-1 · 连锁总部 IT / 运营（总部视角）
> 作为连锁总部的 IT 管理员，我希望在**一个自托管实例**里管理多家不同合同关系（直营 / 加盟 / 租用）的门店，统一下发菜单、库存与价格策略，并查看跨店汇总看板，以便低成本管控且各门店数据互不泄露。

### US-2 · 租用店铺老板
> 作为**租用** Reggie 系统的店铺老板，我希望我的经营数据与其他租户（总部及其他租户）严格隔离，并能看到基于租用合同的清晰账单（租金 / 服务费模式），以便放心使用、无需自建系统。

### US-3 · 加盟商
> 作为加盟商，我希望系统按**加盟合同比例**自动计算我与总部的分账，并生成可对账的分账单 / 对账单（资金通道后置、线下打款），以便每月核对无误、减少纠纷。

### US-4 · 自托管部署方 / ISV（平台运营方）
> 作为为多家餐饮部署 Reggie 的 ISV，我希望系统按**合同类型**区分结算模式（直营全额入自有账户 / 加盟按比例分账 / 租用收租金或服务费），并输出结构化分账单，以便向各合作方清晰结算、留痕可审计。

### US-5 · 总部财务
> 作为总部财务，我希望按**账期**（日 / 周 / 月）批量生成加盟与租用的分账单与对账单，并标记"待确认 / 已确认 / 线下已结"，以便线下核对与打款，资金不经过系统。

---

## 三、数据依据（代码事实，带文件行号）

> 以下事实由数析探查 + 主理人复核，析客据此撰写规格；均以**只读引用**方式核验，未改动任何源码。

| 事实 | 证据（文件:行号） | 对 R-14 的含义 |
|---|---|---|
| 已配置 `TenantLineInnerInterceptor`，`IGNORE_TABLES` 白名单含 11 张表 | `MybatisPlusConfig.java:49-55`（`tenant`、`employee`、`shopping_cart`、`ai_provider_config`、`ai_prompt_template`、`dish_evaluation`、`permission`、`role_permission`、`region`、`store_sync_log`、`rider_remember_token`） | 隔离底座存在；本期需**收敛白名单**并补 `tenant_id` |
| 空上下文 `getTenantId()` 返回 `new LongValue(-1L)` 兜底（fail-closed 但返回空集） | `MybatisPlusConfig.java:70-78` | **静默串数据风险**；R-21 改为 fail-fast 抛异常 |
| `ignoreTable()` 仅做白名单判断；空上下文且非白名单表仅 `log.warn` 仍返回空集 | `MybatisPlusConfig.java:94-108`（:101-106） | 缺上下文访问隔离表会"安静地"返回空集而非报错 → 事故温床 |
| `BaseContext.getCurrentTenantId()` 已存在（ThreadLocal），租户上下文由登录态注入 | `BaseContext.java:22`（`TENANT_THREAD_LOCAL`）、`:64`（`getCurrentTenantId()`） | 复用现有上下文注入机制，不重建 |
| `shopping_cart`/`dish_evaluation` **`schema.sql` 已有 `tenant_id` 列（`bigint NULL`，:236/:432），但白名单中、未回填、靠 userId 隔离**；`ai_provider_config` 物理无 `tenant_id` 列为**系统级全局表**（非租户级，注释 `MybatisPlusConfig.java:35-37` 对前两者已过时） | `schema.sql:236`/`schema.sql:432`；`AiProviderConfig.java`、`AiProviderConfigServiceImpl.java`、`AiPromptTemplate.java:16` | `shopping_cart`/`dish_evaluation` 由 R-21 **回填（非加列）+ 从白名单移除**；`ai_provider_config` 保留白名单、不补列、不回填（决策⑤） |
| `billing` 模块已删除（"假需求：默认 MOCK 模拟开通"） | `MybatisPlusConfig.java:47` 注释 | 本期**不复活订阅计费**；Non-goals 明确 |
| `tenant` 表存在但无 `contract_type`/`contract_id`/`parent_tenant_id`；无 Tenant 实体类 | `src/test/resources/schema.sql:588-605`；全仓无 `entity/Tenant*.java` | R-14-A 需扩展 `tenant` 并新建实体 / Mapper |
| `store` 表已带 `tenant_id`（可空）；无 `store_type` | `src/test/resources/schema.sql:455-468`（:466 `tenant_id`） | 加 `store_type` + `contract_id` 对齐合同类型 |
| `store_info` **已建模层级**：`store_type`(1直营总店/2直营分店/3加盟) + `parent_tenant_id`(上级总店tenantId, NULL=总店) | `src/test/resources/schema.sql:486-509`（:490、:491） | 总部-门店层级**已部分存在**，R-14-C 复用而非新建 |
| `store_sync_log` + `StoreSyncService` 已实现总部→门店下发（菜品/分类/套餐/配置/优惠券，全量/增量/选择性） | `src/test/resources/schema.sql:547-566`；`module/store/service/StoreSyncService.java`、`dto/SyncCategoriesDTO.java` 等 | R-14-C 下发能力**已有底座**，扩展总部视角聚合即可 |
| `daily_settlement`（收银日结）DDL 未见 `tenant_id` 列 | `src/test/resources/schema-mysql.sql:660-682` | **隔离审计需覆盖的潜在缺口**：该表是否被租户插件正确隔离待核验（见 R-14-B 审计项） |
| `supplier_settlement` 已有 `tenant_id`+`period`+`total_amount`+`paid_amount`+`status` | `src/test/resources/schema-mysql.sql:3144-3162` | 分账表的**良好先例**（tenant_id + 账期 + 状态范式），R-14-D 参照其结构 |
| 支付渠道已采用"接口 + 工厂 + DB 配置"范式 | `module/payment/channel/PaymentChannelFactory.java`、`PaymentChannel.java`、`real/*` | R-14-D 分账规则引擎**镜像该工厂范式**，但只产出账单、不接真实通道 |
| 隔离审计测试底座已存在 | `src/test/java/.../TenantTestExecutionListener.java`、`DbAuditProbe.java`、`StoreSyncLogTenantWhitelistTest.java` | R-14-B 审计定时任务可**复用 / 提升**该底座到生产 |

---

## 四、需求拆解（R-14 子需求表）

> **范围主见（重要）**：R-14-B 的"两表（shopping_cart/dish_evaluation）补 `tenant_id` + fail-fast + 审计"与 **R-21（M1, P0, 3–4 人周）** 高度重合。为避免重复计工，本规格将**隔离加固的"硬交付"归口 R-21**，R-14-B 仅负责"白名单收敛至仅全局表 + 合同感知审计扩展 + 39 模块回归"。估算已据此下调（见末列）。路线图中 M3 的 `R-14-A(6-8)` 与 M4 的 `R-14-B(3-4)` 对应本表"数据模型 + 总部管控 + 分账"与"隔离收敛 + 全模块回归"两大桶（另含 R-14-C 4–6 / R-14-D 4–6），合计 17–24 人周。

| 子编号 | 子方向 | 优先级 | 验收标准（摘要） | 估算人周 |
|---|---|---|---|---|
| **R-14-A** | 多租户数据模型完善 | P0 | `tenant` 扩展 `contract_type`/`parent_tenant_id`/`contract_id` 并建实体；新建 `contract`、`settlement_rule`；`store` 加 `store_type`/`contract_id`；合同类型→分账模式映射可配 | 6–8 |
| **R-14-B** | 隔离策略加固（收敛 + 审计） | P0 | 白名单收敛为仅真正全局共享表（`ai_provider_config` 确认保留）；两表（shopping_cart/dish_evaluation）`tenant_id` 回填完成（R-21）并移出白名单；合同感知隔离审计定时任务上线；39 模块回归无串数据 | 3–4（依赖 R-21） |
| **R-14-C** | 总部-门店管控 | P0 | 总部视角聚合下属门店数据；统一菜单 / 库存 / 价格下发到门店；跨店数据汇总看板 | 4–6 |
| **R-14-D** | 按合同类型分账结算 | P0 | 分账规则引擎（contract_type + 比例/固定费/周期/方式）；计算流程跑通；输出分账单 / 对账单（不接资金通道） | 4–6 |
| **合计** | — | — | — | **17–24** |

---

## 五、数据模型设计

### 5.1 设计原则
- **复用不新建**：租户上下文、`tenant` 主键、`store` 主键、`store_info.parent_tenant_id` 层级全部复用。
- **层级模型**：`tenant（合同主体 / 可充当总部） → store（门店） → user`；总部即 `parent_tenant_id IS NULL` 且拥有子租户的 tenant。
- **合同类型枚举（contract_type）**：`DIRECT` 直营 / `FRANCHISE` 加盟 / `LEASE` 租用 / `LEASEBACK` 租赁 / `JOINT` 联营 / `CONSIGNMENT` 代销（已确认纳入，见第十节决策①）。联营（按利润/销量分账）、代销（按销量结算代销费）的结算语义由 R-14-D 规则引擎支撑；非餐饮金融联营不纳入。
- **分账只建模"规则 + 账单"**，资金通道后置，不建支付/清结算流水表。

### 5.2 实体字段表

#### 5.2.1 `tenant` 表扩展（已有表，加列）
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键（已有） |
| `name`/`phone`/`address`/`contact`/`logo`/`license_image`/`status` 等 | — | — | 已有字段，保持不变 |
| **`contract_type`** | varchar(20) | 否 | 合同类型：DIRECT/FRANCHISE/LEASE/LEASEBACK/JOINT/CONSIGNMENT；NULL 表示历史未分类（需回填，决策①） |
| **`parent_tenant_id`** | bigint | 否 | 上级总部 tenantId；NULL = 总部自身（层级锚点，复用 store_info 语义） |
| **`contract_id`** | bigint | 否 | 外键 → `contract.id`；当前生效合同 |
| **`is_hq`** | tinyint(1) | 是 | 派生/冗余标记：是否作为连锁总部（可由 `parent_tenant_id IS NULL` 且有子节点推导，落库便于查询） |

> 注：`tenant` 表本身**保留在白名单**（租户表无 `tenant_id`，全局共享，见 5.4）。

#### 5.2.2 `contract` 表（新建）
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 |
| `tenant_id` | bigint | 是 | 合同归属租户（= 被服务方，如加盟店/租用店） |
| `contract_type` | varchar(20) | 是 | DIRECT/FRANCHISE/LEASE/LEASEBACK/JOINT/CONSIGNMENT |
| `partner_name` | varchar(128) | 否 | 合作方名称（如总部 / 出租方） |
| `counterparty_tenant_id` | bigint | 否 | 合作方 tenantId（如总部 tenantId） |
| `start_date` / `end_date` | date | 是/否 | 合同起止；租赁/租用需明确期限 |
| `settlement_rule_id` | bigint | 否 | 外键 → `settlement_rule.id` |
| `status` | varchar(20) | 是 | DRAFT/ACTIVE/EXPIRED/TERMINATED |
| `signer` / `remark` | varchar | 否 | 签署人 / 备注 |
| `create_time`/`update_time`/`create_user`/`update_user`/`is_deleted` | — | — | 标准审计列 |

#### 5.2.3 `store` 表调整（已有表，加列）
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id`/`name`/`address`/`tenant_id`（已有） | — | — | 复用；`tenant_id` 已存在（:466） |
| **`store_type`** | tinyint | 否 | 1直营总店 2直营分店 3加盟 4租用 5租赁（与 `contract_type` 对齐；与 `store_info.store_type` 保持一致的字典） |
| **`contract_id`** | bigint | 否 | 门店级合同关联（可选；多数场景沿用 tenant 级合同即可） |

> 设计说明：`store_info` 已有 `store_type`+`parent_tenant_id`，与 `store` 存在字段重叠。本期**不强行合并两表**，仅在 `store` 补 `store_type`/`contract_id` 以支撑"免 join 查询 + 租户隔离过滤"，`store_info` 继续作为门店扩展档案；两处 `store_type` 字典须统一（含新增联营/代销，见第十节剩余待确认）。

#### 5.2.4 `settlement_rule` 表（新建，分账规则）
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 |
| `name` | varchar(64) | 是 | 规则名称（如"加盟标准分账-2026"） |
| `contract_type` | varchar(20) | 是 | 适用合同类型 |
| `calc_mode` | varchar(20) | 是 | RATIO 按比例 / FIXED 固定费 / FEE 服务费（租金归 FEE 或独立 RENT 模式） |
| `ratio` | decimal(5,4) | 否 | 分账比例（0~1，如加盟总部抽 0.15） |
| `fixed_amount` | decimal(12,2) | 否 | 固定费 / 租金金额 |
| `granularity` | varchar(20) | 是 | ORDER 逐单结算 / PERIOD 账期聚合结算——**用户已确认两种都需支持（决策②）**，由本字段驱动 7.3 计算流程 |
| `cycle` | varchar(20) | 否 | PERIOD 模式下的 DAILY/WEEKLY/MONTHLY；ORDER 逐单模式下可空 |
| `settlement_method` | varchar(20) | 否 | 结算方式标记（线下转账/支票等，仅记录不执行） |
| `effective_start` / `effective_end` | date | 否 | 规则有效期 |
| `priority` | int | 否 | 多规则命中时的优先级 |
| `status` | varchar(20) | 是 | ACTIVE/INACTIVE |
| `tenant_id` | bigint | 是 | 规则归属（通常为总部 tenantId） |
| 审计列 | — | — | create/update/user/deleted |

#### 5.2.5 `settlement_bill` / `settlement_detail` 表（新建，账单输出）
**`settlement_bill`**
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 |
| `tenant_id` | bigint | 是 | 付款/收款方租户（如加盟店） |
| `counterparty_tenant_id` | bigint | 否 | 对方（如总部） |
| `contract_id` | bigint | 否 | 关联合同 |
| `settlement_rule_id` | bigint | 否 | 关联规则 |
| `period` | varchar(20) | 是 | 账期（如 2026-09 / 2026-W39） |
| `bill_date` | date | 是 | 出账日 |
| `total_amount` | decimal(12,2) | 是 | 基数金额（如门店营业额） |
| `payable_amount` | decimal(12,2) | 是 | 应付（如加盟抽成） |
| `receivable_amount` | decimal(12,2) | 是 | 应收（如租用租金） |
| `status` | varchar(20) | 是 | DRAFT/CONFIRMED/SETTLED_OFFLINE（线下已结） |
| 审计列 | — | — | — |

**`settlement_detail`**（分账明细，支撑对账）
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 |
| `bill_id` | bigint | 是 | 关联 `settlement_bill` |
| `ref_order_id` / `ref_flow_id` | bigint | 否 | 来源订单 / 流水（可空，聚合账可不逐单） |
| `base_amount` | decimal(12,2) | 是 | 该行基数 |
| `share_type` | varchar(20) | 是 | PLATFORM 平台抽成 / FRANCHISE_FEE 加盟费 / RENT 租金 / SERVICE_FEE 服务费 |
| `share_amount` | decimal(12,2) | 是 | 该行分账金额 |
| `remark` | varchar | 否 | 备注 |

> **不新建**：`daily_settlement`（收银日结）、`supplier_settlement`（采购结算）属不同关注点，本期限不并入合同分账；但 `supplier_settlement` 的 `tenant_id`+`period`+`status` 范式被 `settlement_bill` 借鉴。

### 5.3 Flyway / SQL 迁移脚本要点

> 当前 `db/` 下为 `YYYYMMDD_*.sql` 手工迁移（如 `20260927_identifier_case_normalization.sql`），Flyway 由 **R-09** 正式引入。R-14 迁移脚本按现有 `db/YYYYMMDD_*.sql` 命名先行，**并在 R-09 落地后对齐为 `Vx__*.sql` Flyway 格式**（同一 SQL，仅改名），保证可回滚、可审计。

| 版本（占位） | 动作 | 要点 |
|---|---|---|
| `V14_1__tenant_contract_columns.sql` | `tenant` 加 `contract_type`/`parent_tenant_id`/`contract_id`/`is_hq` | 新列可空，兼容存量 |
| `V14_2__store_type_contract.sql` | `store` 加 `store_type`/`contract_id` | `store_type` 字典与 `store_info` 对齐 |
| `V14_3__contract_table.sql` | 新建 `contract` | 含 `tenant_id` 索引 |
| `V14_4__settlement_rule.sql` | 新建 `settlement_rule` | `tenant_id`+`contract_type` 复合索引 |
| `V14_5__settlement_bill_detail.sql` | 新建 `settlement_bill`/`settlement_detail` | 全表带 `tenant_id` 索引；**不进白名单**（需隔离） |
| `V14_6__backfill_tenant_contract.sql` | 历史数据回填（仅 shopping_cart/dish_evaluation 两表；`ai_provider_config` 不回填，见决策⑤） | 见 5.4 回填策略（**高风险**） |

### 5.4 历史数据 `tenant_id` 回填策略（两表 + 合同分类）
- **两表（`shopping_cart`/`dish_evaluation`）回填**：由 `user_id → user.tenant_id` 解析租户（需确认 user 表是否带 `tenant_id`）。回填脚本须幂等、可回滚、带校验（回填后 NULL 行数应趋零）。
- **`ai_provider_config` 回填结论（已确认，决策⑤）**：经代码核验该表为**系统级全局配置**——`AiProviderConfig.java` 无 `tenantId` 字段、`AiProviderConfigServiceImpl` 查询不带租户过滤、`MybatisPlusConfig.java:36` 与 `AiPromptTemplate.java:16` 注释明确"系统级...不需要租户隔离"。**结论：保留白名单、不补 `tenant_id`、不回填**，历史 tenant_id 回填风险在此表消除，仅剩上述两表需评估。
- **`tenant.contract_type` 回填**：存量租户默认 `DIRECT` 或标记 `NULL` 待人工分类（不可静默假设），提供管理端"合同类型补录"流程。
- **隔离校验**：回填后运行审计（见 6.3）确认无 `tenant_id IS NULL` 的隔离表孤儿行。

---

## 六、隔离策略设计（R-14-B，承接 R-21）

### 6.1 `TenantLineInnerInterceptor` 改造点
- **fail-fast 异常类**：在 `com.reggie.config` 或 `common` 包新增 `TenantContextMissingException`（继承 `RuntimeException`）。
  - **定义位置**：`com.reggie.common.exception.TenantContextMissingException.java`（与现有异常体系同包）。
- **`getTenantId()` 改造**（`MybatisPlusConfig.java:70-78`）：
  - 仅对**非白名单表**才会进入 `getTenantId()`（MP 先 `ignoreTable` 再取租户值）。
  - 上下文为 `null` 时**抛 `TenantContextMissingException`**，替代 `return new LongValue(-1L)`。
  - **关键**：白名单表的全局查询（登录、加载租户目录、权限目录）不经过 `getTenantId()`，fail-fast **不会**误伤它们。
- **`ignoreTable()` 收敛**（`:94-108`）：移除已补 `tenant_id` 的表；保留告警仅用于"真正应全局共享却被遗漏"的异常路径。

### 6.2 白名单收敛清单
| 分类 | 表 | 去留 | 理由 |
|---|---|---|---|
| 保留（真正全局共享） | `tenant`、`permission`、`role_permission`、`region`、`rider_remember_token`、`store_sync_log`、`ai_provider_config` | **保留** | 无 `tenant_id` 且全局语义（租户目录/权限目录/行政区字典/骑手令牌/跨店同步日志/系统级 AI 大模型配置）；`ai_provider_config` 已代码核验为系统级全局表（无 tenantId 字段、service 查询不带租户过滤、配置注释明确），确认保留白名单、不补列、不回填（决策⑤） |
| 移出（列已存在，回填即受隔离） | `shopping_cart`、`dish_evaluation` | **移出** | 两表 `schema.sql` 已有 `tenant_id` 列（:236/:432），回填 `user_id→user.tenant_id` 后移出白名单受隔离（R-21 交付，详见 `r21-tenant-isolation-hardening-2026-09-30.md`） |
| 待定 | `employee`、`ai_prompt_template` | **复核** | `employee` 当前由 Controller 手动过滤（:34）；`ai_prompt_template` 与 `ai_provider_config` 同源（注释"系统级表...无 tenant_id"），倾向保留白名单，待显式确认（决策⑤同口径） |
| 新增需隔离 | `contract`、`settlement_rule`、`settlement_bill`、`settlement_detail` | **不进白名单** | 新建表自带 `tenant_id`，强制隔离 |

### 6.3 隔离审计定时任务（合同感知）
- **复用提升**：将测试底座 `DbAuditProbe.java` / `TenantTestExecutionListener.java` 提升为生产级 `@Scheduled` 审计任务（注意：现有 26 个 `@Scheduled` 共用 3 线程池，R-07 治理前本任务须独立分组，避免长任务互阻塞）。
- **审计项**：
  1. 扫描所有"应隔离表"是否存在 `tenant_id IS NULL` 孤儿行（回填后应为 0）。
  2. 校验白名单表确实无 `tenant_id` 列（防止误加导致 Unknown column）。
  3. 检测跨租户聚合 / 调度查询是否**显式声明**了租户逃逸（见 6.4），未声明却在无上下文执行的隔离表访问应告警。
  4. 周期性抽样：以两个不同租户上下文查询同一隔离表，断言结果集不相交。
- **产出**：审计日志表 `tenant_isolation_audit_log` + 失败即告警（对接现有告警通道）。

### 6.4 跨租户查询逃逸机制（总部聚合必需）
- 总部聚合 / 调度任务需跨租户读取（如跨店看板、全量分账计算）。fail-fast 后这些查询**必须显式逃逸**：
  - 方案 A：使用 MP `@InterceptorIgnore(tenantLine = "1")` 注解于聚合 Mapper 方法。
  - 方案 B：在 `BaseContext` 注入"超级租户哨兵"（如 `SUPER_TENANT_ID = 0`）绕过插件，并在审计中登记。
  - **R-14-C / R-14-D 的聚合查询统一采用方案 A**，并在审计项 3 中登记，避免"为图省事全局关隔离"。

---

## 七、分账规则引擎设计（R-14-D）

### 7.1 设计原则
- **镜像支付工厂范式**：参照 `module/payment/channel/PaymentChannelFactory` 的"接口 + 工厂 + DB 配置"结构，定义 `SettlementRuleEngine` + `SettlementStrategy`（按 `contract_type` 分派），**但只产出账单、不调用任何真实资金通道**。
- **输入→输出**：输入（订单 / 流水汇总 + 合同类型 + 生效规则）→ 输出 `settlement_bill` + `settlement_detail`。

### 7.2 规则数据结构
- `settlement_rule`（见 5.2.4）：`contract_type` + `calc_mode`(RATIO/FIXED/FEE) + `ratio`/`fixed_amount` + `cycle` + `settlement_method` + 有效期 + 优先级。
- 支持"合同类型 → 分账模式"映射在规则表配置化，而非硬编码。

### 7.3 计算过程
```
1. 取 scope 内目标租户的有效订单/流水（**逐单与账期两种粒度都需支持，决策②**）：
   - `granularity=ORDER` 逐单：按单笔订单生成明细，逐单计算 share，`settlement_detail.ref_order_id` 必填，利于逐笔对账。
   - `granularity=PERIOD` 账期：按日/周/月聚合汇总 → `base_amount`，`settlement_detail` 可抽样或省略 `ref_order_id`，轻量但需抽样佐证。
   - 由 `settlement_rule.granularity` 驱动选择上述路径。
2. 解析租户 contract_type → 命中 settlement_rule（按 priority + effective 日期）
3. 按 calc_mode 计算：
   - RATIO：share = base_amount * ratio   （加盟：总部抽成）
   - FIXED：share = fixed_amount          （固定管理费）
   - FEE/RENT：share = fixed_amount 或 base_amount * rate （租用租金/服务费）
4. 生成 settlement_bill（total/payable/receivable/status=DRAFT）
5. 逐单或聚合生成 settlement_detail（share_type 标记）
6. 输出可下载对账文件（CSV/PDF，复用现有报表导出能力 R-06）
```
- **三模式区分**：直营（全额入自有账户，`receivable_amount = base_amount`，无抽成）；加盟（按比例分账，`payable_amount = 抽成`）；租用（租金 / 服务费模式，`payable_amount = 租金/服务费`）。

### 7.4 与现有模块对接点
- **订单 / 支付数据**：复用 `module/payment/channel` 的订单 / 支付聚合结果（不接 `PaymentChannel` 真实通道，仅取金额流水）。
- **规则配置 UI**：复用 `store_config`（`tenant_id`+`config_key`+`config_value`）或独立规则管理页；规则存入 `settlement_rule`。
- **报表导出**：复用 R-06 异步报表能力生成对账单。
- **明确不接**：任何银行 / 三方支付 API；`billing` 订阅计费不复活。

---

## 八、验收标准（Given-When-Then）

### R-14-A 多租户数据模型完善
- **AC-A1**：Given 新建租户时选择合同类型，When 保存租户，Then `tenant.contract_type` 与 `parent_tenant_id` 正确落库且可被 Mapper 读取。
- **AC-A2**：Given 存在总部租户 T0 与子租户 T1（`parent_tenant_id=T0`），When 查询 T0 的下属租户，Then 返回 T1 且不返回无隶属关系的 T2。
- **AC-A3**：Given 迁移脚本执行完毕，When 检查 schema，Then `contract`/`settlement_rule`/`settlement_bill`/`settlement_detail` 表存在且均带 `tenant_id` 索引。
- **AC-A4**：Given 存量租户 `contract_type` 为 NULL，When 进入管理端，Then 提示"需补录合同类型"且无静默默认。

### R-14-B 隔离策略加固（承接 R-21）
- **AC-B1**：Given 白名单已收敛，When 访问 `shopping_cart` 等非白名单隔离表且租户上下文为空，Then 抛出 `TenantContextMissingException` 而非返回空集（R-21 交付后由本项回归验证）。
- **AC-B2**：Given 两表（shopping_cart/dish_evaluation）`tenant_id` 回填完成（R-21），When 运行隔离审计，Then 隔离表 `tenant_id IS NULL` 孤儿行数为 0。
- **AC-B3**：Given 白名单仅含真正全局表，When 以两个不同租户上下文查询同一隔离表，Then 结果集不相交。
- **AC-B4**：Given 总部聚合查询使用 `@InterceptorIgnore`，When 审计扫描，Then 该查询被登记为"已声明逃逸"且无未声明跨租户访问。

### R-14-C 总部-门店管控
- **AC-C1**：Given 总部 T0 拥有门店 T1/T2（不同合同类型），When 总部查看跨店汇总看板，Then 仅聚合 T0 下属门店数据且各门店原始数据仍隔离。
- **AC-C2**：Given 总部下发菜单 / 库存 / 价格，When 触发 `StoreSyncService` 同步，Then 目标门店 `store_sync_log` 记录成功且门店侧配置更新（复用现有下发机制）。
- **AC-C3**：Given 加盟店与租用店（及联营/代销）并存且总部下发与门店自定义冲突，When 触发下发，Then 系统**检测冲突并提示**、以**总部为最终仲裁方**生效、门店覆盖须显式标记"脱离总部管控"并留痕（决策④）；可按 `store_type` 差异化下发范围。

### R-14-D 按合同类型分账结算
- **AC-D1**：Given 账期内加盟店营业额 10000 元、规则 ratio=0.15，When 运行分账计算，Then 生成 `settlement_bill`（`payable_amount=1500`，`share_type=FRANCHISE_FEE`，`status=DRAFT`）。
- **AC-D2**：Given 租用店规则为固定租金 3000 元/月，When 月度分账，Then `settlement_bill.receivable_amount=3000`（`share_type=RENT`）。
- **AC-D3**：Given 直营店，When 分账计算，Then 无抽成项，`receivable_amount=营业额全额`。
- **AC-D4**：Given 分账单生成，When 导出对账单，Then 产出 CSV/PDF 且不包含任何真实支付/扣款动作（资金通道零调用）。
- **AC-D5**：Given 规则 `effective_end` 已过，When 计算，Then 不命中该规则并回退到默认/告警。

---

## 九、Non-goals（本期明确不做什么）

- ❌ **不做公有云 SaaS 平台 + 我们收订阅费**（唯一被砍掉的运营形态；多租户隔离本身保留）。
- ❌ **不接真实资金通道**：银行 / 三方支付 / 清结算 API 全部后置；分账只产出规则计算 + 分账单 / 对账单。
- ❌ **不重建 `billing` 订阅计费模块**（已删除，:47 确认）。
- ❌ **不引入分库分表 / 多数据源**（单实例内逻辑隔离，沿用 `TenantLineInnerInterceptor`）。
- ❌ **不做完整低代码 / 可视化流程搭建**。
- ❌ **不替换 MySQL / Redis 技术栈**（隔离改造仅加列 + 配置，不换引擎）。
- ❌ **不扩张合同类型到非餐饮合作形态**（如金融联营）超出本期定义（决策①已明确仅含餐饮合作形态：直营/加盟/租用/租赁/联营/代销）。

### 🅿️ 停车场（已记录但本期不纳入的需求）
- 分账资金通道对接（银行直连 / 微信分账 / 支付宝分账）—— 后置至 P1+，待商务与合规确认。
- 租用租户独立开票 / 电子发票对接 —— **已确认本期不纳入（决策③）**：租用/租赁结算只在本系统产出金额清晰的账单（租金/服务费），线下开票由业务方自理；如后续有合规强制要求再评估独立开票。
- 合同类型低代码自定义扩展位（动态加类型）—— 先固定枚举，动态化后置。
- 多实例跨部署的租户联邦（非单实例内）—— 超出自托管单实例定位。

---

## 十、决策记录（已确认事项）+ 剩余待确认

### 🔒 已确认决策（2026-09-30 用户审定，正文已同步）
1. **合同类型分类法（决策①）**：在 DIRECT/FRANCHISE/LEASE/LEASEBACK 基础上，**明确纳入 `JOINT` 联营 与 `CONSIGNMENT` 代销**（见 5.1 / 5.2.1 / 5.2.4 枚举）。联营（按利润/销量分账）、代销（按销量结算代销费）的结算语义由 R-14-D 规则引擎支撑；非餐饮金融联营不纳入。
2. **分账粒度（决策②）**：**逐单（ORDER）与账期（PERIOD）两种都要支持**，不可二选一。由 `settlement_rule.granularity` 驱动（见 5.2.4、7.3）：逐单利对账（detail.ref_order_id 必填），账期聚合轻量（detail 可抽样）。
3. **租用租户开票（决策③）**：**不要求系统提供独立开票 / 独立账单主体**。租用/租赁结算只产出金额清晰的账单（租金/服务费），线下开票由业务方自理；`settlement_bill` 不增加开票字段/开票流程（见第九节停车场）。
4. **总部-门店冲突解决（决策④）**：**总部下发与门店自定义冲突时，规则必须"沟通"——系统须提供冲突检测与提示，且以总部为最终仲裁方（总部确定谁的为准）**。门店侧覆盖即视为"脱离总部管控"并须显式标记/留痕，不允许静默覆盖（见 6.4 / 8 AC-C3）。
5. **`ai_provider_config` 租户级判定（决策⑤）**：**已代码核验为系统级全局表**——`AiProviderConfig.java` 无 `tenantId` 字段、`AiProviderConfigServiceImpl` 查询不带租户过滤、`MybatisPlusConfig.java:36` / `AiPromptTemplate.java:16` 注释明确"系统级...不需要租户隔离"。**结论：保留白名单、不补 `tenant_id`、不回填**；历史 tenant_id 回填风险在此表消除，仅剩 `shopping_cart`/`dish_evaluation` 两表需评估（见 5.4、6.2）。

### ❓ 剩余待确认（非阻塞，可并行推进）
- **R-14-A 关联**：存量 `tenant.contract_type=NULL` 的人工补录流程与责任方（总部运营 vs 门店自助），由 US-2 / AC-A4 提示驱动，具体责任归属待产品/运营确认。
- **store_type 字典统一**：`store` 与 `store_info` 两处 `store_type` 字典须对齐（直营总店/分店/加盟/租用/租赁 + 联营/代销），避免双源漂移（见 5.2.3）。
- **逐单分账明细量**：逐单模式 `settlement_detail` 行量可能极大，需确认是否抽样存储或仅保留对账文件（影响存储与导出性能）。

---

## 十一、行动清单 + 数据来源索引

### 行动清单
| # | 行动 | 负责方 | 时间窗 |
|---|------|--------|--------|
| 1 | R-21 先行：两表（shopping_cart/dish_evaluation）`tenant_id` 回填 + `getTenantId` fail-fast + 基础隔离审计（R-14-B 的硬前置；`ai_provider_config` 保留白名单不回填） | 后端架构师 + 资深后端 | M1（2026Q4） |
| 2 | R-14-A：数据模型扩展 + 新建 `contract`/`settlement_rule`/`settlement_bill` + Flyway 迁移 + 历史回填脚本 | 后端架构师 + DBA | M3（2027Q2） |
| 3 | R-14-C：总部视角聚合 + 复用 `store_sync_log` 下发 + 跨店看板（含 `@InterceptorIgnore` 逃逸登记） | 资深后端 + 前端 | M3–M4（2027Q2–Q3） |
| 4 | R-14-B（收敛）：白名单收敛至仅全局表 + 合同感知审计定时任务 + 39 模块回归 | 后端架构师 + QA | M4（2027Q3，R-21 后） |
| 5 | R-14-D：分账规则引擎（镜像支付工厂）+ 三模式计算 + 账单/对账单输出（不接资金） | 资深后端 + 前端 | M4（2027Q3） |
| 6 | 回填风险评估（仅 shopping_cart/dish_evaluation 两表，`ai_provider_config` 已确认保留白名单）+ 合同类型补录流程评审 | 产品 + DBA + 法务（如适用） | M3 前 |

### 数据来源 & 成员产出索引
- **数析（数据分析师）**：代码架构三维度证据（39 模块 / 117 Mapper / 26 Scheduled / 9 Async / 最大类 2269 行）；`MybatisPlusConfig`、`tenant`/`store`/`store_info`/`store_sync_log`/`daily_settlement`/`supplier_settlement` 等 DDL 事实。
- **主理人（产品总监）**：R-14 路线图定义（P0 / 16–24 人周 / A-B-C-D 子项）、定位修正（自托管 + 多租户合同差异 + 分账后置）、R-21 与 R-14 边界复核。
- **析客（需求分析师 / 本文档作者）**：本《R-14 实施规格书》——目标 / 用户故事 / 数据依据 / 需求拆解 / 数据模型 / 隔离策略 / 分账引擎 / 验收标准 / Non-goals / 决策记录+待确认 / 行动清单；并提出"R-14-B 与 R-21 范围重叠、归口 R-21"的范围主见。
- **协同引用**：R-09（Flyway 版本化，承接迁移脚本格式）、R-15（全渠道四通，同窗口 M3–M4）、R-16（会员私域）、R-07（调度线程池治理，影响审计任务落地）。

---

> 本规格由产品战略团队 AI 协作生成，关键范围决策（R-21/R-14-B 边界、合同分类法、分账粒度、租用开票）须经产品负责人与业务方审定后实施。
