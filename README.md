<h1 align="center">🍜 瑞吉外卖 (Reggie Takeout)</h1>

<h3 align="center">搭载 AI 大模型的餐饮全栈管理系统</h3>

<p align="center">

<img src="https://img.shields.io/badge/Java-1.8-orange?logo=openjdk" alt="Java 1.8">
<img src="https://img.shields.io/badge/Spring_Boot-2.4.5-6db33f?logo=springboot" alt="Spring Boot 2.4.5">
<img src="https://img.shields.io/badge/MyBatis_Plus-3.5.3.1-1677ff" alt="MyBatis Plus 3.5.3.1">
<img src="https://img.shields.io/badge/Vue.js-2.6.12-4fc08d?logo=vuedotjs" alt="Vue.js 2.6.12">
<img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql" alt="MySQL 8.0">
<img src="https://img.shields.io/badge/Redis-6.0-DC382D?logo=redis" alt="Redis">

<br>

<img src="https://img.shields.io/badge/Element_UI-2.15.10-409eff" alt="Element UI">
<img src="https://img.shields.io/badge/Vant_UI-2.12.0-07c160" alt="Vant UI">
<img src="https://img.shields.io/badge/License-Apache_2.0-333333" alt="License">
<img src="https://img.shields.io/badge/Modules-39-1677ff" alt="39 Modules">
<img src="https://img.shields.io/badge/Tables-124-ff6b6b" alt="124 Tables">
<a href="https://gitee.com/itxinfei/reggie"><img src="https://img.shields.io/badge/Gitee-itxinfei/reggie-c71d23?logo=gitee" alt="Gitee"></a>

</p>

---

## 📖 项目介绍

**瑞吉外卖**是一套完整的餐饮管理系统，基于 Spring Boot + Vue 单体架构，覆盖堂食、外卖、进销存、会员、支付、打印、报表等餐饮全业务场景。前端**三端覆盖**、单 Jar 一并伺服：

- 🖥️ **管理后台**（Vue2 + Element-UI，78 页）
- 📱 **用户点餐 H5**（Vue2 + Vant，26 页）
- 🛵 **骑手配送 H5**（Vant，8 页）

系统在数据层内置 **MyBatis-Plus 行级租户隔离**（自动注入 `tenant_id`），一套实例可服务多品牌/多门店，数据互不穿透；同时预留美团 / 京东 / 饿了么 / 抖音多平台外卖对接骨架（工厂 + 适配器，默认关闭，协议层待按官方文档对接）。

**核心亮点：AI 智能引擎**——接入大语言模型实现智能点餐推荐（SSE 流式）、菜品文案生成、经营分析与多轮对话；配送侧内置**自有骑手体系**（骑手接单、店长派单、GPS 实时追踪），不依赖第三方配送平台。

---

## 📸 界面预览

### 🖥️ 管理后台

<p align="center">
  <img src="docs/screenshots/02-dashboard.png" width="90%" alt="工作台"
       style="border-radius:10px; border:1px solid #e5e7eb; box-shadow:0 2px 8px rgba(0,0,0,.06);">
  <br><sub>📊 工作台：今日订单、营业额、待办、近 7 日趋势、热销菜品、状态分布</sub>
</p>

<table align="center">
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/01-login.png" width="96%" alt="登录页" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🔐 登录页</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/03-orders.png" width="96%" alt="订单管理" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>📋 订单管理</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/04-dishes.png" width="96%" alt="菜品管理" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🍽️ 菜品管理</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/05-setmeals.png" width="96%" alt="套餐管理" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🍱 套餐管理</sub></td>
  </tr>
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/06-categories.png" width="96%" alt="分类管理" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🗂️ 分类管理</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/07-employees.png" width="96%" alt="员工管理" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>👥 员工管理</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/08-dining.png" width="96%" alt="桌台管理" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🪑 桌台管理</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/09-cashier.png" width="96%" alt="收银台" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>💰 收银台</sub></td>
  </tr>
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/10-inventory.png" width="96%" alt="采购入库" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>📦 采购入库</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/11-reports.png" width="96%" alt="经营报表" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>📈 经营报表</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/13-ai.png" width="96%" alt="AI 助手" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🤖 AI 助手</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/15-marketing.png" width="96%" alt="营销活动" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🎯 营销活动</sub></td>
  </tr>
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/12-evaluations.png" width="96%" alt="评价管理" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>⭐ 评价管理</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/14-roles.png" width="96%" alt="角色权限" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🛡️ 角色权限</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/16-payment.png" width="96%" alt="支付配置" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>💳 支付配置</sub></td>
    <td align="center" width="25%"><sub>后台共 <b>78</b> 个功能页<br>（此处为代表页面）</sub></td>
  </tr>
</table>

### 📱 用户点餐 H5

<table align="center">
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/c-02-home.png" width="96%" alt="首页" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🏠 首页</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/c-03-menu.png" width="96%" alt="点餐" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🍽️ 点餐</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/c-04-orders.png" width="96%" alt="订单" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>📋 订单</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/c-06-member.png" width="96%" alt="会员中心" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>💎 会员中心</sub></td>
  </tr>
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/c-07-ai.png" width="96%" alt="AI 助手" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🤖 AI 助手</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/c-05-profile.png" width="96%" alt="个人中心" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>👤 个人中心</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/c-10-qrcode.png" width="96%" alt="扫码点餐" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>📷 扫码点餐</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/c-12-message.png" width="96%" alt="消息中心" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🔔 消息中心</sub></td>
  </tr>
</table>

### 🛵 骑手配送 H5

<table align="center">
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/rider-02-hall.png" width="96%" alt="接单大厅" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>📋 接单大厅</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/rider-03-my-tasks.png" width="96%" alt="我的任务" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>🛵 我的任务</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/rider-04-detail.png" width="96%" alt="任务详情" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>📍 任务详情</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/rider-01-login.png" width="96%" alt="骑手登录" style="border-radius:8px;border:1px solid #e5e7eb;"><br><sub>📱 骑手登录</sub></td>
  </tr>
</table>

---

## 🧩 功能清单

| 分类 | 功能 |
|------|------|
| 🍳 交易主链路 | 分类、菜品（口味/规格/BOM 配方）、套餐、购物车、下单、订单状态机、收货地址 |
| 🚚 履约配送 | 配送围栏、阶梯配送费、**自有骑手体系**（抢单/派单/GPS 实时追踪）、堂食、打印、出餐大屏 KDS |
| 💰 收银支付 | 多渠道支付、货到付款、退款、收银日结、线下收款凭证、发票、提现 |
| 📦 进销存 | 原料、供应商、采购入库、库存盘点、库存流水、成本核算 |
| 👥 会员营销 | 会员等级/积分/余额/优惠券、秒杀、满减、买赠、新客优惠、拼团、推荐、用户收藏 |
| 📊 经营管理 | 工作台、经营报表、财务利润分析、多门店、考勤排班、催单预警、数据导出 |
| 🔔 平台与系统 | AI 引擎、多平台外卖对接骨架、消息通知（短信/推送/SSE）、RBAC 权限、操作日志、定时任务 |

### 安全防护

Session + HttpOnly Cookie 鉴权 · MyBatis-Plus 行级租户隔离（上下文缺失 fail-closed）· CSRF Token · Redis 滑动窗口限流（`@RateLimit`）· 全量 `#{}` 预编译防 SQL 注入 · XSS 转义 · BCrypt 密码加密（兼容历史 MD5 并自动升级）· 日志敏感信息脱敏。

---

## 💻 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 1.8（强制）· Spring Boot 2.4.5 · Spring MVC · MyBatis-Plus 3.5.3.1 · Druid · Redis · MySQL 8 |
| 前端 | Vue 2.6（Options API）· Element-UI 2.15（后台）· Vant 2.12（移动）· Axios · ECharts · Remix Icon |
| 测试 / 工具 | JUnit 5 + Mockito + JaCoCo · SpringDoc OpenAPI · Playwright · Hutool · Lombok |
| AI | DeepSeek / 通义千问 / OpenAI / GLM 等多模型，适配器模式，后台可热切换 |

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 |
|---|---|
| ☕ JDK | **必须 JDK 8**（pom 用 enforcer + animal-sniffer 双保险拦截高版本） |
| 🗄️ MySQL | 8.0 |
| ⚡ Redis | 6.0+ |
| 📦 Maven | 3.6+ |
| 🟢 Node.js | 16+（仅 Playwright 截图/E2E 需要） |

### 启动步骤

```bash
# 1. 克隆项目
git clone https://gitee.com/itxinfei/reggie.git
cd reggie

# 2. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS reggie CHARACTER SET utf8mb4;"

# 3. 初始化数据表：系统共 124 张表。完整建库脚本（含与截图一致的演示数据）暂未随仓库
#    发布，可通过文末交流群 / QQ 联系作者获取，拿到后按 UTF-8 导入：
#      mysql -u root -p --default-character-set=utf8mb4 reggie < reggie.sql
#    也可参考 docs/DATA_MODEL.md 自行建表

# 4. 启动（application-dev.yml 默认连本地 localhost:3306/reggie 与 localhost:6379；
#    如账号/端口不同，修改其中的数据源与 spring.redis 即可）
mvn spring-boot:run
```

启动后访问：

| 应用 | 地址 | 默认账号 |
|---|---|---|
| 🖥️ 管理后台 | http://localhost:8080/backend/index.html | `admin` / `123456` |
| 📱 用户点餐 | http://localhost:8080/front/index.html | 手机号验证码登录（新号自动注册） |
| 🛵 骑手端 | http://localhost:8080/rider/login.html | `13900005003` / `123456` |
| 🔍 Swagger | http://localhost:8080/swagger-ui.html | — |

> **改了前端页面不生效？** `spring-boot:run` 从 `target/classes` 伺服静态资源，改完 HTML/JS/CSS 后执行 `mvn process-resources` 同步，再 `Ctrl+Shift+R` 硬刷新。

### 生产部署

```bash
mvn clean package -DskipTests
java -jar target/reggie_take_out-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

生产参数走 `application-prod.yml`（环境变量注入，敏感值支持 Jasypt `ENC(...)` 加密）。

---

## 🤖 AI 配置

- **零配置体验**：未配置任何 AI 供应商时自动降级到 Mock 模式，系统照常运行。
- **接入真实模型（推荐）**：在后台「AI 供应商管理」配置供应商与 API Key，存于数据库表 `ai_provider_config`，支持多供应商故障转移与后台热切换，无需重启。
- 支持 DeepSeek、通义千问（Qwen）、OpenAI、智谱 GLM 等模型；AI 能力覆盖智能点餐推荐、菜品文案、经营分析、多轮对话与用户画像。

---

## 🧪 测试

```bash
# 全量测试：测试与开发共用本地 reggie 单库（数据按租户隔离：演示租户 1、测试租户 999）。
# 红线：严禁把测试连接指向公网/生产库。跑测试前请先启动本地 MySQL + Redis。
mvn test

# 编译校验（改完 Java 必做）
mvn clean compile

# JDK 8 字节码级兼容检查
mvn animal-sniffer:check
```

后端含 128 个测试类（单元 + Spring 集成测试，JaCoCo 覆盖率 80%+）。E2E 与文档截图基于 Playwright，脚本位于 `tests/`。

---

## 📁 项目结构

```
reggie/
├── src/main/java/com/reggie/
│   ├── common/      # 公共组件（R 响应、全局异常、限流、脱敏、租户上下文）
│   ├── config/      # 配置（WebMvc、MyBatis 租户/分页、Redis、OpenAPI）
│   ├── filter/      # 过滤器（登录校验、CSRF、安全头、链路追踪）
│   └── module/      # 39 个业务模块（各含 controller/service/mapper/model）
├── src/main/resources/
│   ├── backend/     # 管理后台（Element-UI，78 页）
│   ├── front/       # 用户点餐 H5（Vant，26 页）
│   ├── rider/       # 骑手配送 H5（Vant，8 页）
│   └── application.yml / application-dev.yml / application-prod.yml
├── src/test/        # JUnit 测试（连本地 reggie 单库，按 schema-*.sql 建表）
├── db/             # 数据库脚本（全量 reggie.sql 暂未随仓库发布，仅本地）
├── tests/           # Playwright E2E + 文档截图脚本
└── docs/            # 架构 / 数据模型 / 模块 API 文档 + screenshots/
```

---

## 📚 延伸文档

| 文档 | 说明 |
|---|---|
| [项目总览](docs/PROJECT_OVERVIEW.md) | 业务背景与整体设计 |
| [模块与 API](docs/MODULES_AND_APIS.md) | 各模块接口清单 |
| [数据模型](docs/DATA_MODEL.md) | 数据表与关系设计 |
| [架构决策](docs/ARCHITECTURE_DECISIONS.md) | 关键技术选型与决策记录 |
| [后台页面清单](docs/BACKEND_PAGES.md) | 管理后台全部页面索引 |

---

## ❓ 常见问题

<details>
<summary><b>如何重置管理员密码？</b></summary>

密码统一用 BCrypt（强度 10）加密、兼容历史 MD5。建议登录后通过接口修改：

```bash
curl -c c.txt -X POST http://localhost:8080/employee/login \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"原密码"}'
curl -b c.txt -X PUT http://localhost:8080/employee/password \
  -H "Content-Type: application/json" -d '{"oldPassword":"原密码","newPassword":"新密码"}'
```
</details>

<details>
<summary><b>AI 助手不工作？</b></summary>

1. 后台「AI 供应商管理」中是否有已启用的供应商、API Key 是否有效有余额；
2. 查看应用日志中的 AI 错误；临时可设 `reggie.ai.enabled=false` 回退 Mock 模式。
</details>

<details>
<summary><b>平台外卖定时任务报 UnknownHostException？</b></summary>

多平台对接当前为占位骨架，默认受 `reggie.platform.sync-enabled=false` 控制不外呼。出现该日志说明开关被打开；未按官方文档实现协议层前保持 `false` 即可。
</details>

<details>
<summary><b>测试报「表不存在」？</b></summary>

新增业务表需同步到测试 schema：基础表写入 `src/test/resources/schema.sql`、模块表写入 `schema-<module>.sql`，测试类用 `@Sql` 加载。项目未启用 Flyway。
</details>

---

## 📞 联系与支持

<div align="center">

<img src="docs/心飞为你飞.jpg" width="160" alt="微信公众号二维码" style="border-radius:8px;border:1px solid #e5e7eb;">

<sub>

🔗 仓库：[gitee.com/itxinfei/reggie](https://gitee.com/itxinfei/reggie)
💬 交流群：[661543188](https://qm.qq.com/cgi-bin/qm/qr?k=gNgch-wCkfUu-QbI7DZSudrax2BN7vY0)
🛠️ 二开 / 技术支持：QQ **747011882**（功能定制 · 部署答疑 · 项目讲解）

</sub>

</div>

<div align="center">

### ⭐ 如果这个项目对你有帮助，欢迎点个 Star 支持！

</div>
