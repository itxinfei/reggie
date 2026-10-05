<div align="center">

# 🍜 瑞吉外卖 · Reggie Takeout

### 一套搭载 AI 大模型、覆盖「堂食 + 外卖 + 进销存 + 配送」的餐饮全栈管理系统

<b>管理后台 · 用户点餐 H5 · 骑手配送 H5 —— 三端一体，单 Jar 交付</b>

<br>

<!-- ============ 徽章墙：核心框架 ============ -->
<img src="docs/badges/shield-01.svg" alt="JDK 1.8">
<img src="docs/badges/shield-02.svg" alt="Spring Boot 2.4.5">
<img src="docs/badges/shield-03.svg" alt="Spring MVC">
<img src="docs/badges/shield-04.svg" alt="Spring AOP">
<img src="docs/badges/shield-05.svg" alt="MyBatis-Plus">
<img src="docs/badges/shield-06.svg" alt="Druid 连接池">

<br>

<!-- ============ 徽章墙：数据 & 中间件 ============ -->
<img src="docs/badges/shield-07.svg" alt="MySQL 8">
<img src="docs/badges/shield-08.svg" alt="Redis">
<img src="docs/badges/shield-09.svg" alt="Hutool">
<img src="docs/badges/shield-10.svg" alt="Lombok">
<img src="docs/badges/shield-11.svg" alt="Bean Validation">
<img src="docs/badges/shield-12.svg" alt="Jasypt 配置加密">

<br>

<!-- ============ 徽章墙：前端 ============ -->
<img src="docs/badges/shield-13.svg" alt="Vue 2.6">
<img src="docs/badges/shield-14.svg" alt="Element-UI">
<img src="docs/badges/shield-15.svg" alt="Vant">
<img src="docs/badges/shield-16.svg" alt="Axios">
<img src="docs/badges/shield-17.svg" alt="ECharts">
<img src="docs/badges/shield-18.svg" alt="Remix Icon">

<br>

<!-- ============ 徽章墙：集成能力（7 个 / 745px） ============ -->
<img src="docs/badges/shield-19.svg" alt="微信支付">
<img src="docs/badges/shield-20.svg" alt="支付宝">
<img src="docs/badges/shield-21.svg" alt="阿里云短信">
<img src="docs/badges/shield-22.svg" alt="SSE">
<img src="docs/badges/shield-23.svg" alt="WebSocket">
<img src="docs/badges/shield-24.svg" alt="OpenAPI 3">
<img src="docs/badges/shield-26.svg" alt="Apache POI">

<br>

<!-- ============ 徽章墙：质量与工具（7 个 / 626px） ============ -->
<img src="docs/badges/shield-25.svg" alt="ZXing 二维码">
<img src="docs/badges/shield-27.svg" alt="iText PDF">
<img src="docs/badges/shield-28.svg" alt="JUnit 5">
<img src="docs/badges/shield-29.svg" alt="Mockito">
<img src="docs/badges/shield-30.svg" alt="JaCoCo">
<img src="docs/badges/shield-31.svg" alt="Maven">
<img src="docs/badges/shield-32.svg" alt="Apache 2.0">

<br>

<!-- ============ 徽章墙：系统能力与规模（6 个 / 592px） ============ -->
<img src="docs/badges/shield-33.svg" alt="多租户隔离">
<img src="docs/badges/shield-34.svg" alt="39 模块">
<img src="docs/badges/shield-35.svg" alt="124 表">
<img src="docs/badges/shield-36.svg" alt="943 测试">
<img src="docs/badges/shield-37.svg" alt="111 页面">
<img src="docs/badges/shield-38.svg" alt="873 Java 文件">

<br>
<br>

<a href="https://itxinfei.github.io/reggie/"><img src="docs/badges/shield-39.svg" alt="项目官网"></a>
<a href="https://gitee.com/itxinfei/reggie"><img src="docs/badges/shield-40.svg" alt="Gitee"></a>

</div>

<!-- ============ 醒目：开源 + 商业服务横幅 ============ -->
<table align="center">
<tr>
<td align="center" width="100%">
<b>🎁 本项目完全开源（Apache-2.0），可学习、可毕设、可二开、可商用</b><br>
🛠️ 同时提供 <b>商业级二次开发 · 私有化部署 · 功能定制 · 项目讲解</b> 服务 &nbsp;|&nbsp;
💬 <b>QQ：<code>747011882</code></b> &nbsp;·&nbsp; 交流群：<code>661543188</code>
</td>
</tr>
</table>

---

## 📖 项目介绍

**瑞吉外卖**是一套在真实餐饮业务上反复打磨的全栈管理系统，基于 **Spring Boot + Vue 单体架构**，一套实例、一个 Jar 即可同时驱动「管理后台 / 用户点餐 / 骑手配送」三端，覆盖**堂食、外卖、进销存、会员营销、多渠道支付、票据打印、经营报表**等餐饮经营全场景。

- 🖥️ **管理后台**：Vue2 + Element-UI，**78 个功能页**，员工日常经营的生产力工具
- 📱 **用户点餐 H5**：Vue2 + Vant，**25 个页面**，扫码点餐 / 外卖到家 / 会员中心
- 🛵 **骑手配送 H5**：Vant，**8 个页面**，抢单派单 / GPS 实时追踪

数据层内置 **MyBatis-Plus 行级租户隔离**（自动注入 `tenant_id`，上下文缺失时 **fail-closed** 拒绝查询），一套系统可服务多品牌、多门店，数据互不穿透；同时预留美团 / 京东 / 饿了么 / 抖音多平台外卖对接骨架（工厂 + 适配器模式，默认关闭，协议层可按官方文档快速接入）。

> ### 🥇 这不是一个玩具级 Demo
>
> **39 个业务模块 · 124 张数据表 · 943 个自动化测试 · 873 个 Java 类**，系统性地解决了多租户隔离、资金安全、库存成本、履约配送、并发下单、幂等退款等企业级难题。**克隆即可运行，开箱即可交付。**

### 🧭 内容导航

[项目介绍](#-项目介绍) · [核心优势](#-核心优势) · [界面预览](#-界面预览) · [功能清单](#-功能清单) · [技术栈](#-技术栈) · [快速开始](#-快速开始) · [AI 配置](#-ai-配置) · [测试](#-测试) · [项目结构](#-项目结构) · [延伸文档](#-延伸文档) · [常见问题](#-常见问题) · [商业服务与联系](#-商业服务与联系)

---

## 💎 核心优势

<table>
<tr>
<td width="50%">

### 🤖 AI 大模型深度融合
接入 DeepSeek / 通义千问 / OpenAI / 智谱 GLM 等主流大模型，**SSE 流式输出**：智能点餐推荐、菜品营销文案生成、经营分析、多轮对话、用户画像。**多供应商故障转移 + 后台热切换**，首 Token 哨兵熔断，零配置自动降级 Mock。

### 🏢 原生多租户架构
MyBatis-Plus 行级租户插件自动注入 `tenant_id`，基于 `ThreadLocal` 上下文，**上下文缺失 fail-closed**。一套系统服务多个商家 / 品牌，SaaS 化能力内建。

### 🛵 自有骑手履约体系
不依赖第三方配送平台：骑手抢单大厅、店长手动派单、接单 / 取餐 / 送达全状态流转、**GPS 实时位置追踪**、配送围栏与阶梯配送费、配送费自动入账。

</td>
<td width="50%">

### 💰 真实资金链路
微信支付 V3 + 支付宝 + 货到付款三通道，支付回调状态机、**幂等退款**（全额自动回补库存 / 部分退款）、收银日结、线下收款凭证、商家提现、发票管理——资金缺陷已在实战中修复。

### 📦 业财一体 · 进销存
原料 BOM 配方、供应商、采购入库、库存盘点、库存流水、成本核算，菜品销售自动扣减原料库存，退款自动回补，**经营报表 + 财务利润分析**打通业财。

### 🛡️ 企业级安全 & 质量
行级租户隔离、CSRF Token、Redis 滑动窗口限流（降级内存仍 fail-closed）、BCrypt 加密（兼容 MD5 自动升级）、`#{}` 预编译防注入、XSS 转义、日志脱敏、Jasypt 配置加密。**943 个自动化测试 + JaCoCo 覆盖率**为每次改动兜底。

</td>
</tr>
</table>

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

| 分类 | 功能模块 |
|------|------|
| 🍳 **交易主链路** | 菜品分类、菜品管理（多口味 / 多规格 / 图片 / BOM 配方）、套餐组合、购物车、收货地址、下单、**订单状态机**、订单备注、催单、用户评价、再来一单 |
| 🚚 **履约配送** | 配送围栏校验、阶梯 / 按距离配送费、**自有骑手体系**（抢单大厅 / 店长派单 / GPS 实时追踪）、到店堂食、桌台开台、**后厨出餐大屏 KDS**、收银 / 后厨小票打印、自提核销 |
| 💰 **收银支付** | **微信支付 V3**、**支付宝**、货到付款、多渠道后台配置、支付回调对账、**全额 / 部分退款（幂等）**、收银台、收银日结、线下收款凭证上传、发票抬头 / 开票记录、商家提现 |
| 📦 **进销存成本** | 原料档案、供应商管理、采购入库、库存盘点、库存流水台账、菜品 BOM 自动扣料、**成本核算**、退款自动回补库存 |
| 👥 **会员营销** | 会员等级、积分、余额储值、优惠券（满减 / 折扣 / 无门槛）、**秒杀、满减、买赠、新客立减、拼团团购**、推荐有礼、用户收藏、用户画像 |
| 📊 **经营管理** | 数据工作台、**经营报表**（营业额 / 订单 / 客单价 / 热销）、财务利润分析、多门店管理、员工考勤、排班排班、催单超时预警、Excel / PDF 数据导出 |
| 🔔 **平台与系统** | **AI 智能引擎**、多平台外卖对接骨架（美团 / 京东 / 饿了么 / 抖音）、消息通知（短信 / 站内 / SSE 推送）、RBAC 角色权限、操作日志审计、定时任务调度、行政区域、数据字典 |

### 🛡️ 安全防护体系

Session + HttpOnly Cookie 鉴权 · **MyBatis-Plus 行级租户隔离（上下文缺失 fail-closed）** · CSRF Token · Redis 滑动窗口限流 `@RateLimit`（Redis 故障自动降级本地内存，仍拒绝放行）· 全量 `#{}` 预编译防 SQL 注入 · XSS 输出转义 · 安全响应头 · BCrypt 密码加密（兼容历史 MD5 并在登录时自动升级）· 登录 / 关键操作日志审计 · 日志敏感信息脱敏 · 配置项 Jasypt `ENC()` 加密。

---

## 🎯 适用人群

| 你是… | 瑞吉能给你 |
|---|---|
| 🎓 计算机专业学生 | 罕见「大而全」的全栈实战项目：三端 + AI + 多租户 + 真实支付，简历 / 毕设硬核加分项 |
| 🍜 餐饮商家 / 创业者 | 自助部署、数据完全自持，一套系统管门店，**无需为 SaaS 持续付费** |
| 👨‍💻 全栈开发者 | 标准分层 + 多租户 + 支付 + 943 测试，可直接二开或作为企业脚手架 |
| 🏢 外包 / 技术团队 | Apache-2.0 协议允许商用交付，省去从零搭建的数周成本 |

---

## 💻 技术栈

### 后端技术

| 分类 | 技术选型 |
|---|---|
| 核心框架 | Java 1.8（强制）· Spring Boot 2.4.5 · Spring MVC · Spring AOP |
| 持久层 | MyBatis-Plus 3.5.3.1 · MyBatis · MySQL Connector/J 8 · Druid 1.2.21 连接池 |
| 缓存 & 锁 | Redis · Commons Pool2 · Spring Data Redis · 分布式锁 · 滑动窗口限流 |
| 业务工具 | Hutool 5.8.22 · Commons Lang3 · Commons Text · Lombok 1.18.20 · Bean Validation |
| 支付集成 | 微信支付 V3（wechatpay-java 0.2.14）· 支付宝 SDK 4.39.79 · 货到付款 |
| 消息 & 实时 | 阿里云短信 SDK · SSE（SseEmitter）· WebSocket · Spring 异步线程池 |
| 文档 & 导出 | SpringDoc OpenAPI 1.5.13 · Apache POI 4.1.2（Excel）· iText 5.5 + 亚洲字体（PDF）· ZXing 3.5.1（二维码） |
| 安全 & 运维 | Jasypt 2.1.2 配置加密 · Spring Security Crypto · Actuator 监控 · 全局异常处理 · 链路追踪 TraceId |

### 前端技术

| 端 | 技术选型 |
|---|---|
| 🖥️ 管理后台 | Vue 2.6（Options API）· Element-UI 2.15 · Axios · ECharts 5 · 原生 JS（免构建） |
| 📱 用户点餐 H5 | Vue 2.6 · Vant 2.12 · Remix Icon · Axios · 原生 JS（免构建） |
| 🛵 骑手配送 H5 | Vant 2.12 · Remix Icon · GPS 定位 · Axios |
| 🎨 设计体系 | 三端共享样式令牌（Design Tokens）· 统一 `crud-table` / `crud-dialog` 组件规范 |

### AI & 测试

| 分类 | 技术选型 |
|---|---|
| AI 引擎 | DeepSeek · 通义千问 Qwen · OpenAI · 智谱 GLM；适配器 + 工厂模式，多供应商故障转移，后台热切换 |
| 测试框架 | JUnit 5 · Mockito · Spring Boot Test · MockMvc · 真实 MySQL + Redis 集成测试 |
| 质量度量 | JaCoCo 0.8.10 覆盖率 · Maven Enforcer · Animal-Sniffer（JDK 8 字节码双保险） |

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 |
|---|---|
| ☕ JDK | **必须 JDK 8**（pom 用 enforcer + animal-sniffer 双保险拦截高版本语法） |
| 🗄️ MySQL | 8.0 |
| ⚡ Redis | 6.0+ |
| 📦 Maven | 3.6+ |

> 三端前端均为**免构建**静态资源（Vue 2 全局引入 + 浏览器原生 JS），**不需要 Node.js** 即可运行。Node.js 仅在跑 E2E 脚本时才需要。

### 启动步骤

```bash
# 1. 克隆项目
git clone https://gitee.com/itxinfei/reggie.git
cd reggie

# 2. 创建数据库（务必指定 utf8mb4，否则中文数据乱码）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS reggie CHARACTER SET utf8mb4;"

# 3. 建表：导入仓库内建表脚本（18 个文件，覆盖 122 张表）
#    注意 --default-character-set=utf8mb4 不可省略
for f in src/test/resources/schema*.sql; do
  mysql -uroot -p --default-character-set=utf8mb4 reggie < "$f"
done

# 4. 插入最小初始化数据（租户 / 门店 / 超管账号，脚本可重复执行）
mysql -uroot -p --default-character-set=utf8mb4 reggie < db/seed-minimal.sql

# 5. 启动（application-dev.yml 默认连 localhost:3306/reggie 与 localhost:6379）
mvn spring-boot:run
```

> 建表脚本原为自动化测试所用（位于 `src/test/resources/`），内容与生产一致（`CREATE TABLE IF NOT EXISTS`，可重复执行）。它覆盖 **122 张表**，比生产库少 `invoice_record`、`invoice_title` 两张发票表——需要时可在 `docs/数据模型.md` 查结构补建，或联系作者获取全量建库脚本。
>
> 💡 **想要带演示数据的完整环境？** 含 124 张表与各端演示数据的全量建库脚本未随仓库发布（体积大且含本地定制），可通过文末 **QQ 747011882 / 交流群** 获取。

启动后访问：

| 应用 | 地址 | 默认账号 |
|---|---|---|
| 🖥️ 管理后台 | http://localhost:8080/backend/index.html | `admin` / `123456` |
| 📱 用户点餐 | http://localhost:8080/front/index.html | 手机号验证码登录（新号自动注册，dev 模式验证码见日志） |
| 🛵 骑手端 | http://localhost:8080/rider/login.html | 自助建表无骑手数据，需管理员先在后台创建骑手 |
| 🔍 接口文档 | http://localhost:8080/swagger-ui.html | — |

> ⚠️ **首次登录后请立即修改默认密码。**

<details>
<summary><b>🔧 前端改完不生效？</b></summary>

`spring-boot:run` 从 `target/classes` 伺服静态资源，改完 HTML/JS/CSS 后需同步再硬刷新（`Ctrl + Shift + R`）：

```bash
mvn process-resources
```
</details>

### 生产部署

```bash
mvn clean package -DskipTests
java -jar target/reggie_take_out-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

生产参数走 `application-prod.yml`（环境变量注入，敏感值支持 Jasypt `ENC(...)` 加密）。

---

## 🤖 AI 配置

- **零配置体验**：未配置任何 AI 供应商时，AI 能力自动走 Mock 模式，系统照常运行。
- **接入真实模型（推荐）**：在后台「AI 供应商管理」配置供应商与 API Key（存于 `ai_provider_config` 表），支持**多供应商故障转移与后台热切换，无需重启**。
- 支持 DeepSeek、通义千问（Qwen）、OpenAI、智谱 GLM 等；能力覆盖智能点餐推荐、菜品文案、经营分析、多轮对话与用户画像。
- 短信 / 推送同样默认 `mock-mode=true`（仅打印日志不真发），接入真实通道见 `application.yml` 配置项。

---

## 🧪 测试

> ⚠️ **测试与开发共用本地 `reggie` 单库**，靠「租户 + 命名空间」隔离：演示数据挂租户 1，自动化测试数据挂租户 999（高 ID 防冲突）。
> **红线：严禁把测试指向公网 / 生产库。** 部分 `@Sql` 脚本含无条件全表 `DELETE FROM`，跑测试会清掉演示数据——**跑前请先备份 `db/reggie.sql`，跑完用它恢复演示态。**

```bash
# 全量测试（需先启动本地 MySQL + Redis）
mvn test

# 编译校验（改完 Java 必做）
mvn clean compile

# JDK 8 字节码级兼容检查
mvn animal-sniffer:check
```

后端含 **126 个测试类、943 个测试用例**（单元测试 + Spring 集成测试 + 并发负载测试），JaCoCo 产出覆盖率报告。

---

## 📁 项目结构

```
reggie/
├── src/main/java/com/reggie/
│   ├── ReggieApplication.java     # 启动类
│   ├── common/                    # 公共组件
│   │   ├── R.java                 #   统一响应结构
│   │   ├── BaseContext.java       #   ThreadLocal 用户/租户上下文
│   │   ├── GlobalExceptionHandler.java   # 全局异常处理
│   │   ├── RateLimit(+Aspect).java       # Redis 滑动窗口限流
│   │   ├── MyMetaObjectHandler.java      # 自动填充 createTime/updateUser
│   │   ├── PasswordUtils / CsrfTokenUtil / LogMaskUtils
│   │   ├── annotation/            #   自定义注解
│   │   ├── aspect/ event/ validation/ utils/
│   ├── config/                    # 配置类（WebMvc / MyBatis / Redis / OpenAPI /
│   │   │                          #   CORS / 异步 / 调度 / 过滤器注册 / Jasypt / WebSocket）
│   ├── filter/                    # 过滤器（登录校验、安全头、链路追踪）
│   ├── dto/  enums/  utils/
│   └── module/                    # ★ 39 个业务模块，各含 controller/service/mapper/model/dto
│       │
│       ├── 【交易主链路】
│       │   ├── order/             #   订单（状态机）
│       │   ├── dish/              #   菜品（口味/规格/BOM）
│       │   ├── setmeal/           #   套餐
│       │   ├── category/          #   分类
│       │   ├── shopping/          #   购物车
│       │   ├── address/           #   收货地址
│       │   ├── payment/           #   支付渠道与回调
│       │   └── groupbuy/          #   拼团团购
│       │
│       ├── 【履约配送】
│       │   ├── delivery/          #   骑手配送（抢单/派单/GPS）
│       │   ├── platform/          #   第三方外卖平台适配
│       │   ├── dining/            #   堂食桌台
│       │   ├── printer/           #   小票打印引擎
│       │   └── kds/               #   后厨出餐大屏
│       │
│       ├── 【经营管理】
│       │   ├── store/             #   门店
│       │   ├── dashboard/         #   工作台
│       │   ├── report/            #   经营报表
│       │   ├── cost/              #   成本核算
│       │   ├── finance/           #   财务利润
│       │   ├── withdraw/          #   商家提现
│       │   ├── cashier/           #   收银台/日结
│       │   ├── attendance/        #   考勤
│       │   ├── schedule/          #   排班
│       │   └── urgency/           #   催单预警
│       │
│       ├── 【会员营销】
│       │   ├── member/            #   会员等级/积分/余额
│       │   ├── user/              #   C 端用户
│       │   ├── customer/          #   客户/画像
│       │   ├── marketing/         #   优惠券/秒杀/满减/买赠
│       │   ├── recommend/         #   推荐有礼
│       │   ├── notification/      #   短信/站内/SSE 消息
│       │   ├── invoice/           #   发票
│       │   └── favorite/          #   用户收藏
│       │
│       └── 【平台与系统】
│           ├── ai/                #   AI 引擎（多模型适配/故障转移）
│           ├── auth/              #   认证授权
│           ├── sys/               #   系统/字典
│           ├── tenant/            #   租户管理
│           ├── region/            #   行政区域
│           ├── common/            #   模块公共能力
│           ├── export/            #   Excel/PDF 导出
│           └── inventory/         #   进销存（原料/供应商/采购/盘点）
│
├── src/main/resources/
│   ├── backend/                   # 🖥️ 管理后台（Element-UI，78 页，api/page/js/styles）
│   ├── front/                     # 📱 用户点餐 H5（Vant，25 页）
│   ├── rider/                     # 🛵 骑手配送 H5（Vant，8 页）
│   ├── shared/                    # 三端共享（CSRF 核心、状态字典、样式令牌）
│   ├── scripts/rate_limit.lua     # Redis 限流 Lua 脚本
│   └── application.yml / application-dev.yml / application-prod.yml
│
├── src/test/                      # JUnit 5 测试（126 个测试类 / 943 用例）
│   └── resources/schema*.sql      # 按模块拆分建表脚本（亦供本地建表复用）
│
├── db/
│   └── seed-minimal.sql           # 最小初始化数据（建表后必执行）
├── docs/                          # 架构 / 数据模型 / 模块 API 文档 + screenshots/
├── scripts/                       # Node 前端检查 / H5 体验巡逻脚本（非运行时）
└── pom.xml
```

---

## 📚 延伸文档

| 文档 | 说明 |
|---|---|
| [项目总览](docs/项目总览.md) | 业务背景与整体设计 |
| [模块与接口](docs/模块与接口.md) | 各模块接口清单 |
| [数据模型](docs/数据模型.md) | 124 张数据表与关系设计 |
| [架构设计](docs/架构设计.md) | 关键技术选型与决策记录 |
| [后台页面清单](docs/后台页面清单.md) | 管理后台全部页面索引 |

> 仓库还附带**自研文档站**（零依赖 GFM 渲染器），本地启动后可可视化浏览模块接口与通用文档。

---

## ❓ 常见问题

<details>
<summary><b>导入 SQL 时中文乱码 / 建表报错？</b></summary>

1. 漏了 `--default-character-set=utf8mb4`（最常见）——不指定时 MySQL 客户端会按 GBK 解释脚本；
2. 建库没指定字符集：`CREATE DATABASE reggie CHARACTER SET utf8mb4;`（已有库可 `ALTER DATABASE reggie CHARACTER SET utf8mb4;`）；
3. `schema*.sql` 可重复执行，报「表已存在」属正常；如需重来，`DROP DATABASE reggie;` 后重建即可。
</details>

<details>
<summary><b>建完表登录不了 / 提示用户名或密码错误？</b></summary>

你多半只执行了建表、没执行第 4 步。建表脚本**不含任何初始数据**，必须再导入 `db/seed-minimal.sql`，否则 `employee` 表为空、无可登录账号。
</details>

<details>
<summary><b>如何重置管理员密码？</b></summary>

密码统一用 BCrypt（强度 10）加密、兼容历史 MD5 并在登录时自动升级。忘记密码时可直接改库（`password_type` 填 `BCRYPT`）：

```sql
UPDATE employee SET password = '<BCrypt哈希>', password_type = 'BCRYPT' WHERE username = 'admin';
```

已登录时也可通过接口修改（需旧密码，新密码至少 6 位）：

```bash
curl -c c.txt -X POST http://localhost:8080/employee/login \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"原密码"}'
curl -b c.txt -X PUT http://localhost:8080/employee/password \
  -H "Content-Type: application/json" -d '{"oldPassword":"原密码","newPassword":"新密码"}'
```
</details>

<details>
<summary><b>AI 助手不工作 / 答得不对？</b></summary>

1. 未配置供应商时走 Mock 模式（返回占位内容，属预期）；
2. 已配置则检查后台「AI 供应商管理」中供应商是否启用、API Key 是否有效且有余额；
3. 查看应用日志中的 AI 调用错误。企业微信 / 短信 / 推送同样默认 `mock-mode=true`，需在 `application.yml` 显式配置真实通道。
</details>

<details>
<summary><b>平台外卖定时任务报 UnknownHostException？</b></summary>

多平台对接当前为占位骨架，默认受 `reggie.platform.sync-enabled=false` 控制不外呼。出现该日志说明开关被打开；未按官方文档实现协议层前保持 `false` 即可。
</details>

<details>
<summary><b>接口报「表不存在」？</b></summary>

本地库通过 `schema*.sql` 建表；新增业务表后需把 DDL 同步到对应文件（基础表 → `schema.sql`，模块表 → `schema-<module>.sql`）。项目**未启用 Flyway**，脚本不会自动迁移已有库，需手动补执行。
</details>

<details>
<summary><b>mvn 编译报 JDK 版本相关错误？</b></summary>

项目强制 JDK 8（enforcer + animal-sniffer 双重拦截），用 JDK 9+ 会在 enforcer 阶段直接失败。切换到 JDK 8 后重新构建即可。
</details>

<details>
<summary><b>接口莫名 429 / Redis 报错？</b></summary>

限流基于 Redis 滑动窗口。Redis 不可用时会**降级为本地内存滑动窗口**（fail-closed，不会直接放行），日志中会有告警；Redis 恢复后自愈。开发期可用 `application-dev.yml` 调低对应接口的限流阈值。
</details>

---

## 🤝 商业服务与联系

<div align="center">

<table>
<tr>
<td align="center" width="50%">

### 🛠️ 付费技术服务

本项目 **Apache-2.0 完全开源免费**。同时作者提供商业支持，助你少走弯路：

- 🔧 **二次开发 / 功能定制**（对接平台、新增业务、UI 调整）
- 🚀 **私有化部署交付**（服务器、域名、支付商户配置一条龙）
- 🎓 **项目讲解 / 毕设答疑**（架构、源码、面试考点）
- 📦 **全量建库脚本 + 演示数据**
- 💬 日常技术答疑与长期维护支持

<b>👉 商务联系 QQ：<code>747011882</code></b>

</td>
<td align="center" width="50%">

### 💬 加入社区

<img src="docs/心飞为你飞.jpg" width="170" alt="微信公众号二维码" style="border-radius:8px;border:1px solid #e5e7eb;">

<sub>扫码关注微信公众号「心飞为你飞」</sub>

<br>

🔗 仓库：[gitee.com/itxinfei/reggie](https://gitee.com/itxinfei/reggie)
🏠 官网：[itxinfei.github.io/reggie](https://itxinfei.github.io/reggie/)
👥 QQ 交流群：**[661543188](https://qm.qq.com/cgi-bin/qm/qr?k=gNgch-wCkfUu-QbI7DZSudrax2BN7vY0)**

</td>
</tr>
</table>

<br>

### ⭐ 如果这个项目对你有帮助，欢迎点个 Star 支持，这是对我最大的鼓励！

<a href="https://gitee.com/itxinfei/reggie"><img src="docs/badges/shield-41.svg" alt="Star"></a>

</div>
