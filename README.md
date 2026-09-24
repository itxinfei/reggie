<h1 align="center">🍜 瑞吉外卖 (Reggie Takeout)</h1>

<h3 align="center">搭载 AI 大模型的餐饮全栈管理系统</h3>

<p align="center">

<img src="https://img.shields.io/badge/Java-1.8-orange?logo=openjdk" alt="Java 1.8">
<img src="https://img.shields.io/badge/Spring_Boot-2.4.5-6db33f?logo=springboot" alt="Spring Boot 2.4.5">
<img src="https://img.shields.io/badge/MyBatis_Plus-3.4.2-1677ff?logo=mybatis" alt="MyBatis Plus 3.4.2">
<img src="https://img.shields.io/badge/Vue.js-2.6.12-4fc08d?logo=vuedotjs" alt="Vue.js 2.6.12">
<img src="https://img.shields.io/badge/Redis-6.0-DC382D?logo=redis" alt="Redis">
<img src="https://img.shields.io/badge/MySQL-5.7_|_8.0-4479A1?logo=mysql" alt="MySQL 5.7/8.0">

<br>

<img src="https://img.shields.io/badge/Element_UI-2.15.10-409eff?logo=element" alt="Element UI">
<img src="https://img.shields.io/badge/Vant_UI-2.12.0-07c160?logo=vant" alt="Vant UI">
<img src="https://img.shields.io/badge/Druid-1.1.23-ff69b4?logo=apache" alt="Druid">
<img src="https://img.shields.io/badge/AI-DeepSeek_/_通义千问_/_OpenAI-8a2be2?logo=openai" alt="AI LLM">
<img src="https://img.shields.io/badge/License-Apache_2.0-333333?logo=apache" alt="License">

<br>

<img src="https://img.shields.io/badge/Modules-39-1677ff?logo=spring" alt="39 Modules">
<img src="https://img.shields.io/badge/Data_Tables-110-ff6b6b?logo=postgresql" alt="110 Tables">
<img src="https://img.shields.io/badge/Java_Files-907-4379a7?logo=java" alt="907 Java files">
<img src="https://img.shields.io/badge/Total_Commits-671-success?logo=git" alt="671 commits">
<a href="https://gitee.com/itxinfei/reggie"><img src="https://img.shields.io/badge/Gitee-itxinfei/reggie-c71d23?logo=gitee" alt="Gitee"></a>
<a href="https://github.com/itxinfei/reggie"><img src="https://img.shields.io/badge/GitHub-Mirror-181717?logo=github" alt="GitHub Mirror"></a>

</p>

---

<div align="center">

### 💼 付费二开 · 技术支持

**500 元 / 次** · 功能定制 | 缺陷修复 | 部署答疑 | 项目讲解

> 📲 二开 / 技术支持请直接联系我：QQ **747011882**（邮箱同号）· 交流群 [661543188](https://qm.qq.com/cgi-bin/qm/qr?k=gNgch-wCkfUu-QbI7DZSudrax2BN7vY0)

<img src="docs/心飞为你飞.jpg" width="170" alt="微信公众号二维码" style="border-radius:8px; border:1px solid #e5e7eb;">

<sub>📱 微信公众号（推广号）· 项目仓库 [gitee.com/itxinfei/reggie](https://gitee.com/itxinfei/reggie)</sub>

</div>

---

## 📖 项目介绍

**瑞吉外卖**是一套完整的餐饮管理系统，基于 Spring Boot + Vue 的单体应用架构，覆盖堂食、外卖、进销存、会员、支付、打印、报表等餐饮全业务场景。前端为**三端覆盖**：管理后台（Element UI）、C 端点餐 H5（Vant）、骑手配送 H5（Vant），单 Jar 一并伺服。

**部署形态**：面向连锁加盟品牌总部的**私有化部署**方案——1 台云服务器 + 1 个数据库，总部统一运维，各门店终端直连总部（对标：银豹 / 二维火 / 美团收银本地版）。系统同时在数据层保留**行级租户隔离**能力（MyBatis-Plus 租户插件自动注入 `tenant_id`），一套实例可服务多品牌/多门店，数据互不穿透。

**渠道接入**：系统设计了私域（自有 H5）与多平台外卖渠道（美团 / 京东 / 饿了么 / 抖音）的对接**骨架**——工厂模式 + 适配器（Factory + Adapter），统一 `PlatformAdapter` 接口，定时任务（拉单 30s / 重试 2min / 恢复 5min / 对账每日）已就位。

> ⚠️ **平台外卖对接当前为"占位脚手架"，尚未接通真实开放平台 API**：4 个适配器（JD / Meituan / Eleme / Douyin）的接口路径、鉴权与签名（`buildSign`）均为 AI 生成的"通用约定"，**`buildSign` 未被实际调用**，且京东原网关域名 `openo2o.jddj.com` 为不存在的假域名（已修正为真实网关 `openapi.jddj.com`）。因此默认配置下**定时任务不会真正外呼**——总开关 `reggie.platform.sync-enabled` 默认为 `false`，4 个任务入口直接 `return`。要真正对接某家平台，需入驻其开放平台（美团 developer.waimai.meituan.com、京东 opendj.jd.com、饿了么 open.faas.ele.me、抖音 developer.open-douyin.com），按官方文档重写对应适配器的协议层（含真正调用签名），再于 `application.yml` 置 `reggie.platform.sync-enabled: true`。

系统核心创新在于 **AI 智能引擎**，通过接入大语言模型实现智能点餐推荐、菜品描述生成、经营分析等能力；配送侧内置**自有骑手体系**（骑手 H5 端 + 店长派单 + GPS 上报与订单实时追踪），不依赖第三方配送平台。

### 🏗️ 模块化架构

项目采用 **模块化设计**，业务功能按领域划分为 39 个模块（位于 `com.reggie.module.*`），每个模块包含完整的 Controller → Service → Mapper → Model 分层：

<details>
<summary><b>📦 展开完整模块清单（39 个）</b></summary>

| 模块 | 说明 | 核心功能 |
|------|------|----------|
| 🔐 **auth** | 认证授权 | 员工/用户登录登出、Session、密码管理 |
| 👤 **user** | C 端用户 | 手机号登录、用户信息、会员绑定 |
| 🏢 **tenant** | 租户管理 | 租户注册、活跃租户枚举、隔离上下文 |
| 🏪 **store** | 门店管理 | 总部-分店、数据同步、门店配置 |
| 🍽️ **dish** | 菜品管理 | 菜品 CRUD、口味、规格、BOM 配方 |
| 📂 **category** | 分类管理 | 菜品分类、套餐分类 |
| 🥡 **setmeal** | 套餐管理 | 套餐组合、起售停售 |
| 🛒 **shopping** | 购物车 | 增减、清空、结算 |
| 📋 **order** | 订单管理 | 下单、状态流转、订单明细 |
| 📍 **address** | 地址管理 | 收货地址 CRUD、默认地址 |
| 🚚 **delivery** | 配送管理 | 配送范围、配送费规则、自有骑手（H5 接单）、店长派单、GPS 上报与实时追踪 |
| ⭐ **favorite** | 用户收藏 | C 端菜品收藏/星标、收藏列表 |
| 🖥️ **kds** | 出餐大屏 | 厨房显示（KDS）：厨显看板、出票、超时任务 |
| 🌐 **platform** | 平台外卖（对接骨架） | 美团/京东/饿了么/抖音适配器 + 工厂模式；拉单/落库/重试/对账任务已搭好，协议层待按官方文档对接（默认关闭） |
| 🪑 **dining** | 堂食管理 | 桌台区域、排队取号、预约、叫号 |
| 💰 **payment** | 支付管理 | 支付单、退款、回调、多支付渠道 |
| 💳 **cashier** | 收银管理 | 收银记录、日结对账 |
| 🧾 **invoice** | 发票管理 | 抬头管理、开票申请、开具/作废 |
| 💸 **withdraw** | 提现管理 | 提现申请、审核、打款 |
| 🎉 **groupbuy** | 拼团管理 | 拼团活动、成团判定 |
| 📦 **inventory** | 进销存 | 原料、供应商、采购入库、盘点、流水 |
| 💲 **cost** | 成本管理 | 菜品成本、人工、其他成本 |
| 💹 **finance** | 财务管理 | 收支明细、利润分析 |
| 👥 **member** | 会员管理 | 等级、积分、余额、优惠券 |
| 🎯 **recommend** | 推荐引擎 | 协同过滤、用户画像、偏好分析 |
| 🎁 **marketing** | 营销管理 | 秒杀、满减、买赠、新客优惠 |
| ⏱️ **urgency** | 催单预警 | 未接单实时扫描、分级告警、接单大屏 |
| 👔 **attendance** | 考勤管理 | 员工打卡、排班 |
| 🛎️ **customer** | 客服管理 | 会话、工单、投诉处理 |
| 📊 **report** | 报表管理 | 日报、菜品排行、时段分析、经营报表 |
| 📈 **dashboard** | 仪表盘 | 经营概览、实时数据 |
| 🖨️ **printer** | 打印管理 | 浏览器本地打印、小票/后厨/配送单模板、打印记录、桌贴二维码打印中心（海报生成+批量打印） |
| 🔔 **notification** | 通知管理 | 模板、短信、推送、多渠道路由 |
| 📤 **export** | 数据导出 | Excel / PDF 多维度导出 |
| ⏰ **schedule** | 定时任务 | 订单超时、数据统计、操作日志 |
| 📋 **sys** | 系统管理 | 角色、权限、配置、操作日志 |
| 🌍 **region** | 区域管理 | 省市区数据 |
| 🤖 **ai** | AI 引擎 | 智能推荐、文案生成、经营分析、对话 |
| 🔧 **common** | 公共模块 | 公共模型、通用服务、跨模块复用 |

</details>

## 📸 界面预览

### 🖥️ 后台管理系统

> 以下截图为项目**当前运行版本**实拍（1920×1080，Playwright 自动截图，2026-09-24），
> 覆盖登录、工作台、订单、菜品、收银、库存、报表、AI 等核心功能页。

<p align="center">
  <img src="docs/screenshots/02-dashboard.png" width="88%" alt="工作台 - 今日订单/营收/待办/趋势图"
       style="border-radius:10px; border:1px solid #e5e7eb; box-shadow:0 2px 8px rgba(0,0,0,.06);">
  <br><sub>📊 工作台：今日订单、营业额、待办事项、近 7 日趋势、订单状态分布</sub>
</p>

<table align="center">
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/01-login.png" width="96%" alt="登录页"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🔐 登录页</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/03-orders.png" width="96%" alt="订单管理"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>📋 订单管理</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/04-dishes.png" width="96%" alt="菜品管理"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🍽️ 菜品管理</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/05-setmeals.png" width="96%" alt="套餐管理"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🍱 套餐管理</sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/06-categories.png" width="96%" alt="分类管理"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🗂️ 分类管理</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/07-employees.png" width="96%" alt="员工管理"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>👥 员工管理</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/08-dining.png" width="96%" alt="桌台管理"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🪑 桌台管理</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/09-cashier.png" width="96%" alt="收银台"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>💰 收银台</sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/10-inventory.png" width="96%" alt="采购入库"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>📦 采购入库</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/11-reports.png" width="96%" alt="经营报表"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>📈 经营报表</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/12-evaluations.png" width="96%" alt="评价管理"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>⭐ 评价管理</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/13-ai.png" width="96%" alt="AI 助手"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🤖 AI 助手</sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/14-roles.png" width="96%" alt="角色权限"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🛡️ 角色权限</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/15-marketing.png" width="96%" alt="营销活动"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🎯 营销活动</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/16-payment.png" width="96%" alt="支付配置"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>💳 支付配置</sub>
    </td>
    <td align="center" width="25%">
      <sub>🧩 后台共 <b>75</b> 个功能页<br>（此处为代表页面）</sub>
    </td>
  </tr>
</table>

<details>
<summary><b>🖼️ 旧版界面存档（历史版本截图）</b></summary>

<p align="center">
  <img src="docs/imgs/后台管理系统.png" width="85%" alt="瑞吉外卖后台管理系统（旧版）"
       style="border-radius:10px; border:1px solid #e5e7eb; box-shadow:0 2px 8px rgba(0,0,0,.06);">
</p>

</details>

### 📱 C 端用户（Vant H5）

> 手机号验证码登录（新号自动注册）→ 点餐 → 支付 → 订单/评价/收藏/会员/开票：C 端共 22 个页面。
> 以下为**当前运行版本**实拍（390×844 竖屏，Playwright 自动截图，2026-09-24）。

<table align="center">
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-01-login.png" width="96%" alt="登录页"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>📱 登录页</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-02-home.png" width="96%" alt="首页"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🏠 首页</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-03-menu.png" width="96%" alt="点餐"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🍽️ 点餐</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-04-orders.png" width="96%" alt="订单列表"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>📋 订单</sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-05-profile.png" width="96%" alt="个人中心"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>👤 个人中心</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-06-member.png" width="96%" alt="会员中心"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>💎 会员中心</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-07-ai.png" width="96%" alt="AI 点餐助手"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🤖 AI 助手</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-08-evaluations.png" width="96%" alt="我的评价"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>⭐ 我的评价</sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-09-favorites.png" width="96%" alt="我的收藏"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>⭐ 我的收藏</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-10-qrcode.png" width="96%" alt="扫码点餐"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>📷 扫码点餐</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-11-service.png" width="96%" alt="在线客服"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🛎️ 在线客服</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/c-12-message.png" width="96%" alt="消息中心"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🔔 消息中心</sub>
    </td>
  </tr>
</table>

### 🛵 骑手端（Vant H5）

> **自有骑手配送体系**：骑手账号登录 → 接单大厅抢单 → 我的任务 → 送达上报；
> 店长在后台「配送订单」派单，C 端订单页可实时查看骑手位置。共 4 个页面。

<table align="center">
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/rider-01-login.png" width="96%" alt="骑手登录"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>📱 骑手登录</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/rider-02-hall.png" width="96%" alt="接单大厅"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>📋 接单大厅</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/rider-03-my-tasks.png" width="96%" alt="我的任务"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>🛵 我的任务</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/rider-04-detail.png" width="96%" alt="任务详情"
           style="border-radius:8px; border:1px solid #e5e7eb;"><br><sub>📍 任务详情</sub>
    </td>
  </tr>
</table>

<div align="center">

| 亮点 | 说明 |
|------|------|
| 🏢 **企业级架构** | Spring Boot 2.4.5 + MyBatis Plus 3.4.2，RESTful API，39 个领域模块分层清晰 |
| 📱 **三端覆盖** | 管理后台（Element UI，75 页）+ C 端点餐 H5（Vant，22 页）+ 骑手配送 H5（4 页），单 Jar 一并伺服 |
| 🛵 **自有骑手配送** | 骑手 H5 接单（大厅抢单/任务/送达）+ 店长后台派单 + GPS 上报与订单实时追踪，不依赖三方配送 |
| 🔐 **行级租户隔离** | MyBatis-Plus 租户插件自动注入 `tenant_id`，多品牌/多门店数据互不穿透 |
| ⚡ **前后端一体** | 前端页面内嵌于 Spring Boot，单 Jar 部署，无需分离部署 |
| 📦 **全业务覆盖** | 堂食 + 外卖配送 + 进销存 + 会员营销 + 支付 + 打印 + 发票 + 报表 + 多平台渠道 |
| 🌐 **平台外卖对接骨架** | 适配器 + 工厂模式 + 拉单/重试/对账定时任务已就位；协议层与签名待按官方文档实现，开关默认关闭（不空转外呼） |
| 🖨️ **门店本地打印** | 员工在后台一键发起，浏览器调本地打印机出小票；打印记录落库，无需额外代理、服务器无需装打印机 |
| 📷 **桌贴二维码打印中心** | 桌台二维码 → 海报生成 → 批量打印，C 端扫码即点餐 |
| 💾 **110 张数据表** | 完整数据库设计 + 44 个迁移脚本（`db/` 本地维护，不入版本库） |
| 🗂️ **运行时图片统一存储** | 上传/访问按 `uploads/{public\|private}/来源/业务/月份` 分层，公私分流鉴权，三端共享 `imgPath()` 单一真源 |
| 🧪 **双层测试** | 742 个 JUnit 单测/集成测试 + Playwright E2E（Allure 报告） |
| 🤖 **AI 智能引擎** | DeepSeek/通义千问/OpenAI 多模型，点餐推荐 + 描述生成 + 经营分析 + 对话管理 |

</div>

### 🤖 AI 智能引擎

本系统深度集成 LLM 大模型，不是"套壳对话"，而是融入业务全链路：

| 能力 | 描述 | 技术细节 |
|------|------|----------|
| 🎯 **智能点餐推荐** | 用户以自然语言描述需求，AI 匹配门店真实菜品 | 用户画像 + 协同过滤 + LLM 语义理解，SSE 流式输出 |
| 📝 **菜品描述生成** | 输入菜名和原料，AI 生成专业美食文案 | DeepSeek/通义千问 API，50-150 字专业文案 |
| 📊 **经营数据分析** | 上传经营数据 JSON，AI 输出分析报告和建议 | LLM 数据解读 + 趋势判断 + 行动建议 |
| 📢 **营销文案生成** | AI 生成优惠活动、推送通知等营销内容 | 多场景 Prompt 模板，速率限制 |
| 💬 **对话管理** | 多轮对话、历史回溯、会话持久化 | 数据库持久化 + 长期记忆 + 用户反馈闭环 |
| 👤 **用户画像** | 基于行为和偏好构建长期记忆 | 口味/价格/菜品偏好分析，置信度评分 |
| 🔌 **多模型切换** | 后台动态切换 AI 供应商，无需重启 | 9 种预设模型（DeepSeek/Qwen/GLM/Claude 等），适配器模式 |
| ⚡ **Mock 模式** | 未配置 AI 时自动降级，系统仍可运行 | @Primary MockAIClient，零成本体验 |

### 系统架构

```
┌────────────────┐   ┌────────────────┐   ┌────────────────┐
│  管理后台 (PC)  │   │  C 端点餐 (H5)  │   │  骑手端 (H5)    │
│ Element UI 2.x │   │ Vant UI + AI点餐│   │ Vant 接单/送达  │
│ 一键打印/派单   │   │   22 个页面     │   │   4 个页面      │
└───────┬────────┘   └───────┬────────┘   └───────┬────────┘
        │                    │                    │
        └──────────┬─────────┴────────────────────┘
                   ▼
        ┌────────────────────────────────┐
        │   Spring Boot 2.4.5            │
        │   REST API（39 个领域模块）      │
        └───┬───────────┬───────────┬────┘
            │           │           │
   ┌────────▼─────┐ ┌───▼──────┐ ┌──▼──────────────┐
   │    MySQL     │ │  Redis   │ │    AI 服务       │
   │  110 张数据表 │ │ 缓存/限流 │ │ DeepSeek / Qwen │
   │ 行级租户隔离  │ │ Session  │ │ OpenAI / GLM    │
   │ 图片分层存储  │ └──────────┘ └─────────────────┘
   │ public/private│
   └──────────────┘

   ┌───────────────────────────────────────────────┐
   │   多平台渠道对接层（工厂模式 + 适配器，占位协议）  │
   │    ┌──────┐    ┌──────┐    ┌──────┐    ┌──────┐ │
   │    │ 美团 │    │ 京东 │    │饿了么 │    │ 抖音 │ │
   │    └──┬───┘    └──┬───┘    └──┬───┘    └──┬───┘ │
   │       └───────────┼───────────┘                │
   │   拉单 → 落库去重 → 对账重试（默认关闭）          │
   └───────────────────────────────────────────────┘
   ⚠️ 4 个适配器为占位脚手架：接口/签名未接官方文档，buildSign 未调用；
      定时任务受 reggie.platform.sync-enabled（默认 false）控制，不实际外呼。

   ┌───────────────────────────────────────────────┐
   │   第三方服务：支付 / 短信 / 推送 / 地图 / 发票    │
   └───────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| ☕ JDK | 8 | **必须 JDK 8**：`pom.xml` 用 maven-enforcer（锁 `[1.8,1.9)`）+ animal-sniffer（字节码级）双保险拦截高版本 |
| 🗄️ MySQL | 5.7+ / 8.0 | 必须安装，用于存储业务数据（110 张表） |
| 📦 Maven | 3.6+ | 构建和依赖管理 |
| ⚡ Redis | 6.0+ | 缓存、Session 共享、API 限流、分布式锁 |
| 🟢 Node.js | 16+ | 仅 E2E 测试（Playwright）需要 |
| 🌐 浏览器 | Chrome / Edge | 现代浏览器即可 |

### 启动步骤

```bash
# 1. 克隆项目
git clone https://gitee.com/itxinfei/reggie.git
cd reggie

# 2. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS reggie CHARACTER SET utf8mb4;"

# 3. 依次执行迁移脚本（项目未启用 Flyway，按文件名顺序手动执行）
cd src/main/resources/db/migration
for f in $(ls V*.sql | sort); do mysql -u root -p reggie < "$f"; done

# 4.（可选）导入演示数据 —— db/ 目录整体不入版本库（.gitignore 忽略），
#    迁移与演示脚本均本地维护；有 seed 脚本时按文件名顺序 source 即可

# 5. 配置数据源与 Redis
#    ⚠️ application*.yml 被 .gitignore 忽略（不入库），需自行创建 application-dev.yml
#    spring.datasource.druid.url=jdbc:mysql://localhost:3306/reggie?...
#    spring.redis.host=localhost

# 6. 编译并启动
mvn clean package -DskipTests
java -jar target/reggie_take_out-1.0-SNAPSHOT.jar
# 或直接运行
mvn spring-boot:run
```

启动后访问：

| 应用 | 地址 | 默认账号 |
|------|------|----------|
| 🖥️ 管理后台 | http://localhost:8080/backend/index.html | `admin` / `123456` |
| 📱 C 端点餐 | http://localhost:8080/front/index.html | 手机号验证码登录（新号自动注册） |
| 🛵 骑手端 | http://localhost:8080/rider/login.html | `13900005003` / `123456`（演示账号） |
| 🔌 Swagger UI | http://localhost:8080/swagger-ui.html | — |

> **⚠️ 两个高频踩坑**
>
> 1. **严禁对本地 `reggie` 库执行 `mvn test`**：测试用 `@Sql` 会执行 `schema.sql` 的 `DROP TABLE`，且 `application-test.yml` 直连 `reggie` 库——跑测试即清空全库数据。
> 2. **改完前端页面不生效**：若服务从 `target/classes/backend` 伺服静态资源，需同步一次：
>    ```bash
>    rm -rf target/classes/backend && cp -r src/main/resources/backend target/classes/backend
>    ```

### 生产部署

```bash
# 1. 导入数据库（迁移脚本按序执行，见启动步骤第 3 步）

# 2. 激活 prod 环境并创建 application-prod.yml
#    配置数据库、Redis 等生产参数（支持环境变量注入）

# 3. 打包运行
mvn clean package -DskipTests
java -jar target/reggie_take_out-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### 打印（浏览器调本地打印机）

打印机装在门店 PC 时，员工在后台「订单明细 → 打印」一键发起：浏览器弹出系统
打印对话框，选择本地打印机即可出小票；发起时自动落一条打印记录（菜单
「打印记录」可查）。无需安装任何代理，服务器也无需连接打印机。

> 提示：在打印对话框中关闭「页眉和页脚」、边距选「无」，小票更整洁。

---

## 📋 功能清单

### 核心业务模块

| 模块 | 功能详情 |
|------|----------|
| 👨‍💼 **员工管理** | 登录/退出、员工 CRUD、Session 会话管理、角色分配 |
| 🍱 **分类管理** | 菜品分类、套餐分类，全量 CRUD |
| 🍜 **菜品管理** | 菜品 CRUD、口味管理、图片上传、起售/停售 |
| 🍚 **套餐管理** | 套餐 CRUD、套餐详情管理、起售/停售 |
| 📦 **订单管理** | 分页查询、状态流转（下单→支付→出餐→完成）、订单明细 |
| 🛒 **购物车** | 增减数量、清空购物车、多品类混合下单 |
| 📍 **地址管理** | 收货地址 CRUD、默认地址、多地址管理 |

### 扩展业务模块

| 模块 | 功能 |
|------|------|
| 🌐 **平台外卖（对接骨架）** | 美团/京东/饿了么/抖音适配器 + 工厂模式；拉单/幂等落库/失败重试/日结对账/同步日志任务已搭好，协议层待按官方文档对接（默认 `sync-enabled: false` 不实际外呼） |
| 🖨️ **打印管理** | 浏览器调本地打印机（小票/后厨/外卖单）、打印记录流水、桌贴二维码打印中心（海报生成+批量打印） |
| 🚚 **外卖配送** | 配送范围围栏、配送费规则、**自有骑手体系**（骑手 H5 接单、店长派单、GPS 上报与实时追踪）、平台对接骨架 |
| 🍽️ **堂食管理** | 桌台/区域管理、预订跟踪、取号排队、桌台状态实时看板 |
| 📦 **进销存** | 原料管理、供应商管理、采购入库、库存盘点、库存流水追踪、智能补货 |
| 👤 **会员体系** | 会员等级成长、积分管理/调整、优惠券发放核销、余额充值、积分排行 |
| 💰 **支付集成** | 支付单创建/查询、退款处理、多支付渠道 |
| 🧾 **发票管理** | 抬头管理、开票申请、开具/作废状态机 |
| 💸 **提现管理** | 提现申请、审核、打款 |
| 🎉 **拼团管理** | 拼团活动、成团判定 |
| 🎁 **营销管理** | 秒杀、满减、买赠、新客优惠 |
| 💳 **收银管理** | 收银记录、日结对账 |
| 📊 **经营报表** | 日/周/月销售统计、菜品销量排行、时段经营分析、经营报表四合一 |
| 🎯 **智能推荐** | 协同过滤推荐引擎、偏好分析、营销活动管理 |
| 🔔 **消息通知** | 模板管理、短信通知、APP 推送、多渠道路由、定时触发 |
| 📥 **数据导出** | 订单/菜品/员工/报表多维度 Excel & PDF 导出 |
| 🏪 **多门店** | 门店 CRUD、数据同步、门店仪表盘、员工门店权限隔离 |
| 💹 **财务管理** | 收支明细、利润分析、成本核算 |
| 👔 **考勤管理** | 员工打卡、排班管理 |
| 🛎️ **客服管理** | 会话管理、工单处理、投诉跟踪 |
| ⏱️ **催单预警** | 未接单实时扫描（30s）、分级告警、语音播报、接单大屏 |
| 🍳 **出餐大屏（KDS）** | 厨房显示看板、厨显出票、出餐超时任务 |
| ⭐ **用户收藏** | C 端菜品收藏/星标、收藏列表 |
| 📋 **系统管理** | 角色权限（RBAC）、菜单权限、参数配置、操作日志 |
| ⏰ **定时任务** | 订单超时取消、数据统计、平台拉单/重试/对账、未接单扫描 |

### 安全防护体系

| 防护层 | 技术方案 |
|--------|----------|
| 🔐 **认证鉴权** | Session + Cookie（HttpOnly、SameSite=strict），注解式鉴权 `@RequiresAdmin` / `@RequiresPermission` / `@RequireEmployee` |
| 🏢 **租户隔离** | MyBatis-Plus 租户插件行级 `tenant_id` 自动注入；上下文缺失时 fail-closed，绝不返回跨租户数据 |
| 🛡️ **CSRF 防护** | 轻量级 Token 验证，不依赖 Spring Security |
| ⚡ **API 限流** | Redis 滑动窗口算法（`@RateLimit`），阈值可配 |
| 💉 **SQL 注入** | 全量 MyBatis `#{}` 预编译，禁止拼接 SQL |
| 🧼 **XSS 防护** | 用户输入转义，前端禁用未清洗的 `v-html` |
| 🔍 **日志脱敏** | 自动脱敏手机号、身份证、地址等敏感信息 |
| 🔐 **密码加密** | BCrypt 强度因子 10，防彩虹表 |
| 🍪 **Session 安全** | Cookie-Only 模式，HttpOnly + SameSite=strict |
| 🧾 **越权防护** | 后台接口租户 ID 一律从会话上下文 `BaseContext` 取，禁止前端传参 |

---

## 💻 技术栈

<div align="center">

### 后端

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 1.8（硬约束：enforcer + animal-sniffer 双保险） |
| 框架 | Spring Boot | 2.4.5 |
| Web | Spring MVC | 5.3.6 |
| ORM | MyBatis Plus | 3.4.2 |
| 缓存 | Redis + Commons Pool2 | 6.0+ |
| 连接池 | Druid | 1.1.23 |
| 数据库 | MySQL（测试亦连真实库） | 5.7+ / 8.0 |
| 测试 | JUnit 5 + Mockito + JaCoCo | 0.8.10 |
| 文档 | Springdoc OpenAPI | 1.5.13 |
| 安全 | Spring Security Crypto + Jasypt | 5.4.6 / 2.1.2 |
| 工具 | Hutool / ZXing 二维码 / AliYun SMS | 5.8.22 / 3.5.1 |
| 效率 | Lombok | 1.18.20 |
| **AI** | **DeepSeek / 通义千问 / OpenAI / GLM / Claude** | **LLM API** |

### 前端

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue.js | 2.6.12（**仅 Options API，禁 Vue 3 / TS / Vite**） |
| PC UI | Element UI | 2.15.10 |
| 移动 UI | Vant UI | 2.12.0 |
| HTTP | Axios | 0.21.1 |
| 图表 | ECharts（本地 plugins） | 5.x |
| 图标 | Remix Icon | 4.6 |
| E2E | Playwright + Allure | TypeScript |

</div>

---

## 📁 项目结构

```
reggie/
├── src/main/java/com/reggie/
│   ├── common/           # 公共组件（R 响应封装、全局异常、限流、日志脱敏、租户上下文）
│   ├── config/           # 配置类（WebMvc、MyBatis 多租户、Redis、线程池、OpenAPI）
│   ├── dto/              # 通用数据传输对象
│   ├── enums/            # 状态枚举
│   ├── filter/           # 登录拦截过滤器（LoginCheckFilter）
│   ├── module/           # 🧩 业务模块（39 个，各含 controller/service/mapper/model）
│   │   ├── platform/     #   🌐 平台外卖对接骨架（美团/京东/饿了么/抖音适配器，默认关闭，协议层待对接）
│   │   ├── printer/      #   🖨️ 打印（本地打印/任务流水/桌贴二维码打印中心）
│   │   ├── delivery/     #   🚚 配送（范围/运费规则/自有骑手/派单/GPS 实时追踪）
│   │   ├── kds/          #   🍳 出餐大屏（厨显看板/厨显出票/超时任务）
│   │   ├── favorite/     #   ⭐ 用户收藏（菜品收藏/星标/收藏列表）
│   │   ├── invoice/      #   🧾 发票（抬头管理/开票申请/开具作废）
│   │   ├── urgency/      #   ⏱️ 未接单预警（30s 实时扫描/分级告警/语音播报）
│   │   ├── ai/           #   🤖 AI 引擎（推荐/文案生成/经营分析/对话/用户画像）
│   │   ├── inventory/    #   📦 进销存（原料/供应商/采购/盘点/库存流水）
│   │   ├── member/       #   👤 会员（等级成长/积分/优惠券/余额充值）
│   │   ├── dining/       #   🍽️ 堂食（桌台区域/预订/排队/叫号/桌贴二维码）
│   │   ├── report/       #   📊 报表（销售统计/菜品排行/时段分析）
│   │   ├── sys/          #   📋 系统（角色权限 RBAC/菜单/参数配置/操作日志）
│   │   └── ...           #   另有 auth/user/dish/order/category/setmeal/shopping/address/
│   │                     #      store/tenant/dashboard/payment/cashier/cost/finance/
│   │                     #      marketing/recommend/notification/export/customer/
│   │                     #      attendance/groupbuy/withdraw/region/schedule/common
│   ├── service/          # 业务接口 + 实现类
│   └── utils/            # 工具类（二维码、验证码、SMS、文件操作）
├── src/main/resources/
│   ├── backend/          # 🖥️ 管理后台（Element UI，75 个页面 + 设计令牌 tokens.css）
│   ├── front/            # 📱 C 端点餐 H5（Vant UI，22 个页面）
│   ├── rider/            # 🛵 骑手配送 H5（Vant UI，4 个页面：登录/接单大厅/任务/详情）
│   ├── shared/           # 🔗 三端共享静态资源（img-path.js 等单一真源）
│   ├── db/
│   │   └── migration/    # 🗄️ 44 个迁移脚本（V<日期>__描述.sql；db/ 整体 gitignore，本地维护，无 Flyway）
│   ├── com/reggie/.../   # Mapper XML（与 Java 包同路径，非 resources/mapper/）
│   └── application.yml   # 主配置（application-dev/prod/test.yml 被 gitignore，需自建）
├── src/test/java/        # 🧪 742 个单元/集成测试
├── src/test/resources/   # schema.sql + schema-<module>.sql（测试库专用）
├── tests/                # 🎭 Playwright E2E 测试（TypeScript + Allure 报告）+ 文档截图脚本
├── docs/                 # 📚 架构决策/数据模型/模块 API/后台页面清单 + screenshots/ 实拍图
└── pom.xml               # Maven 配置
```

---

## 📚 文档索引

| 文档 | 说明 |
|------|------|
| [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md) | 项目总览：背景、目标、范围、角色与核心流程 |
| [docs/ARCHITECTURE_DECISIONS.md](docs/ARCHITECTURE_DECISIONS.md) | 架构决策记录（ADR）：关键技术选型与取舍原因 |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | 数据模型：110 张表的领域划分与核心表结构 |
| [docs/MODULES_AND_APIS.md](docs/MODULES_AND_APIS.md) | 模块与接口清单：39 个模块能力与主要 API |
| [docs/BACKEND_PAGES.md](docs/BACKEND_PAGES.md) | 后台页面清单：75 个页面的实现状态与要点 |
| `CHANGELOG.md` | 变更记录（**本地维护**：被 `.gitignore` 忽略，不进版本库） |

---

## 🗺️ 开发路线图

### ✅ 已完成

| 里程碑 | 内容 |
|--------|------|
| 🏗️ **基础框架** | Spring Boot + MyBatis Plus 分层架构、多租户拦截器、统一响应与全局异常处理 |
| 🧑‍💼 **核心业务** | 员工/分类/菜品/套餐/订单/购物车/地址 完整 CRUD |
| 🔐 **安全加固** | CSRF 防护、API 限流、日志脱敏、BCrypt 密码、租户行级隔离、越权防护 |
| 🛵 **自有骑手配送体系** | 骑手 H5（登录/接单大厅/我的任务/送达上报）+ 店长后台派单 + GPS 上报与 C 端订单实时追踪 |
| 📷 **桌贴二维码打印中心** | 桌台二维码海报生成与批量打印，C 端扫码即点餐；二维码/AI 配置改为页面内弹窗 |
| 🗂️ **运行时图片统一存储** | 上传/访问按 public/private 分层落盘、公私分流鉴权（canonical 相对路径防绕过）、三端共享 `imgPath()`、存量迁移工具 |
| 🧾 **C 端开票与收藏** | 用户端发票申请/抬头/记录页；菜品收藏/星标/收藏列表 |
| 🍳 **出餐大屏（KDS）** | 厨房显示看板、厨显出票、出餐超时任务 |
| 🌐 **平台外卖对接骨架** | 美团/京东/饿了么/抖音工厂模式 + 适配器 + 拉单/重试/恢复/对账定时任务（默认关闭，协议层待按官方文档实现） |
| 🖨️ **门店本地打印** | 浏览器调本地打印机 + 打印记录落库，无需额外代理，服务器无需装打印机 |
| ⏱️ **未接单预警** | 30s 实时扫描分级告警（Redis 锁防重、每单每级仅告警一次）+ 接单大屏语音播报 |
| 🧾 **发票与资金** | 发票抬头管理、开票申请、开具/作废状态机；提现申请、审核、打款 |
| 🎉 **拼团营销** | 拼团活动管理、成团判定 |
| 📋 **RBAC 权限闭环** | 75 个菜单 + 9 个按钮权限 seed、角色分配、权限树分配弹窗、操作日志 |
| 🤖 **AI 引擎 v2.0** | 多模型适配器（9 种模型）、SSE 流式输出、对话管理、用户画像、后台动态切换供应商 |
| ⚙️ **自动化任务** | 支付订单超时、定时订单回收、平台拉单/重试/对账、未接单扫描、操作日志归档 |
| 🧪 **测试体系** | 742 个 JUnit 单测/集成测试（JaCoCo 覆盖率）+ Playwright E2E（Allure 报告） |
| 🎨 **前端设计系统** | 设计令牌 `tokens.css`、`crud-table`/`crud-dialog` 统一组件、全站表格列宽与居中治理、响应式与 a11y 对比度达标 |
| 🛡️ **批量质量治理** | 三批缺陷修复（首页初始化/快捷入口/发票隔离/地址去重/AI 助手等）+ 桌台结账源头收敛 + 全站弹窗修复 |

### 🔮 规划中

| 功能 | 说明 |
|------|------|
| 📈 **AI 销量预测** | 基于历史数据 + LLM 预测菜品销量，辅助采购决策 |
| 💬 **智能客服** | AI 自动回复用户咨询，常见问题自动处理 |
| 📊 **数据大屏** | 实时经营数据可视化大屏 |
| 🐳 **Docker 部署** | 一键 Docker Compose 部署方案 |
| 🎙️ **语音点餐** | 接入语音识别，支持语音下单 |
| 🌐 **平台外卖真实对接** | 入驻美团/京东/饿了么/抖音开放平台，按官方文档实现签名与协议层，开启 `sync-enabled` 接通真实订单 |

---

## 🤖 AI 配置指南

### 开发模式（零配置）

AI 引擎默认使用 **Mock 模拟模式**，无需任何配置即可体验基础 AI 功能。

### 接入真实 AI（推荐：数据库配置，支持热切换）

AI 供应商与密钥统一在数据库表 **`ai_provider_config`** 中管理，**后台动态切换、无需重启**，也避免密钥随代码入库：

1. 获取 API Key：[DeepSeek 开放平台](https://platform.deepseek.com/)
2. 登录管理后台 → **AI 供应商管理** → 新增供应商（填写 `base-url`、`model`、`api-key`）并激活
3. 未配置任何供应商时自动降级为 **Mock 模式**，功能仍可演示

> ⚠️ `application.yml` 中的 `reggie.ai.*` 配置项**已废弃，仅作兜底**。优先级：**数据库 `ai_provider_config` > yml 兜底**。
> 请勿将 API Key 写入配置文件并提交到仓库。

### 支持的 AI 模型

| 供应商 | base-url | model 示例 | 推荐 |
|--------|----------|-----------|------|
| 🥇 DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` | ⭐⭐⭐⭐⭐ |
| 🥈 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` | ⭐⭐⭐⭐ |
| 🥉 OpenAI | `https://api.openai.com/v1` | `gpt-4o-mini` | ⭐⭐⭐⭐ |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` | `glm-4-flash` | ⭐⭐⭐⭐ |
| 文心一言 | `https://aip.baidubce.com/rpc/2.0/ai_custom/v1` | `ernie-speed` | ⭐⭐⭐ |
| Claude | `https://api.anthropic.com` | `claude-3-haiku` | ⭐⭐⭐⭐ |
| 本地 Ollama | `http://localhost:11434/v1` | `qwen2.5:7b` | ⭐⭐⭐ |
| Kimi | `https://api.moonshot.cn/v1` | `moonshot-v1-8k` | ⭐⭐⭐⭐ |
| MiniMax | `https://api.minimax.chat/v1` | `abab6.5s-chat` | ⭐⭐⭐ |

**动态切换**：管理后台 → AI 供应商管理 → 添加/激活，无需重启应用。

### AI 功能入口

| 端 | 入口 | 功能 |
|----|------|------|
| 📱 用户端 | 首页 🤖 图标 | AI 智能点餐助手 |
| 🖥️ 管理端 | 左侧菜单 → AI 助手 | AI 对话、模型管理 |
| 🖥️ 管理端 | AI 供应商管理 | 配置和切换 LLM 模型 |

---

## 🧪 测试

### 后端（JUnit 5 + Mockito + JaCoCo）

```bash
# 全量测试（需先启动本地 MySQL + Redis，测试 profile 连真实库）
mvn test

# 单个测试类
mvn test -Dtest=DishControllerTest

# 编译校验 —— 改完 Java 必做（见下方 ⚠️）
mvn -o clean compile

# 额外跑 JDK 8 字节码级兼容检查（animal-sniffer）
mvn verify
```

> ⚠️ **严禁对本地 `reggie` 业务库执行 `mvn test`**：`application-test.yml` 直连真实 `reggie` 库，且测试的 `@Sql` 会执行 `schema.sql` 中的 `DROP TABLE`——**跑测试会清空全库**。请使用独立测试库，或跑完重灌 seed。
>
> ⚠️ **增量编译会"假成功"**：源码有错时 ECJ 会生成占位 class 仍报 BUILD SUCCESS。改过 Java 必须 `mvn -o clean compile`（clean 前先停掉运行中的服务）。

| 测试类型 | 覆盖范围 | 说明 |
|---------|---------|------|
| 单元 / 集成测试 | Controller（@SpringBootTest + MockMvc）、Service、Mapper | 742 个，全绿 |
| 覆盖率 | JaCoCo 0.8.10，`mvn verify` 生成报告 | — |
| 核心业务 | 员工登录、菜品查询、订单提交、购物车 | ✅ |
| 多租户 | 租户行级隔离、忽略表（`permission` / `role_permission`）回归 | ✅ |
| 安全组件 | 限流、CSRF、密码加密、日志脱敏 | ✅ |
| 扩展模块 | 平台外卖、打印、堂食、进销存、会员、支付、配送、报表 | ✅ |
| JDK 8 兼容 | `mvn verify` 跑 animal-sniffer 字节码级检查 | ✅ |

### E2E（Playwright + Allure）

```bash
cd tests
npm install
npx playwright test          # 需先启动后端服务（localhost:8080）
npm run report               # 生成并打开 Allure 报告
```

覆盖：登录、员工、菜品、订单、前台点餐等主流程冒烟。

---

## 🤝 贡献指南

```text
1. Fork 本仓库
     ↓
2. 创建特性分支 (git checkout -b feature/AmazingFeature)
     ↓
3. 提交更改 (git commit -m 'feat: 添加 AmazingFeature')
     ↓
4. 推送到分支 (git push origin feature/AmazingFeature)
     ↓
5. 发起 Pull Request 🎉
```

### 提交规范

遵循 [Conventional Commits](https://www.conventional-commits.org/)：

| 前缀 | 场景 |
|------|------|
| `feat:` | 新增功能 |
| `fix:` | 修复 bug |
| `docs:` | 文档更新 |
| `style:` | 格式调整（不影响逻辑） |
| `refactor:` | 重构 |
| `test:` | 测试修改 |

分支结构：`master`（主干，日常直接提交）→ `backup-before-push`（推送前备份）。禁止提交 `.log`、`.pyc`、`target/`、`node_modules/` 等临时文件与缓存。

### 开发约定（改代码前必读）

**后端**

- **JDK 8 硬约束**：禁用 `var`、`List.of()`、`Map.of()`、`String.isBlank()`、`switch` 表达式、`record`、text block、`jakarta.*`；替代为 `StringUtils.isBlank()`、`Arrays.asList()`、`javax.*`。
- 分页一律走 `PageUtils.of/cap`（上限 100），禁止裸 `new Page<>()` 或不封顶透传 `pageSize`；统计走后端聚合。
- 查 `permission` / `role_permission` 两张表须走专用 Mapper 或原生 SQL（这两表无 `tenant_id`，已在 `MybatisPlusConfig.IGNORE_TABLES`）。
- 混合公开/顾客端点的 Controller（Dish / Setmeal / Category / Order 等）**只能方法级**加 `@RequireEmployee`，类级会误挡公开接口。
- 定时任务查业务表前必须先注入租户上下文（遍历 `listActiveTenants()` 逐租户设置），否则租户插件 fail-closed 查询恒空。
- Entity 用 `@TableName` + `@TableId` + `@Data`，非库字段标 `@TableField(exist=false)`。

**前端**

- Vue 2 **仅 Options API**，禁止 Vue 3 语法（`setup` / `ref` / `reactive` / composables）、TypeScript、Vite。
- 颜色一律引用设计令牌 `styles/tokens.css`，禁止硬编码 hex；品牌金底必须配 `--text-on-brand` 深字（金底白字不满足 a11y 对比度）。
- 表格列对齐唯一来源是 `js/components.js` 的 `resolveColAlign`（默认**全列居中**，含金额/数字列）；中文表头列宽基线：2 字 ≥ 100px、4 字 ≥ 140px，表头 `nowrap` + 省略号 + tooltip。
- 弹窗统一用 `<crud-dialog>`；数据录入弹窗必须 `:close-on-click-modal="false"` 防误关，提交按钮绑 `:submit-loading`。
- 列表加载用骨架屏（`showSkeleton = loading && data.length === 0`），首次加载完成前不显示空态，加载失败不显示"暂无数据"。

**测试与文档**

- 新增表必须同步 `src/test/resources/schema*.sql` + `@Sql` + `@DirtiesContext(BEFORE_EACH_TEST_METHOD)`。
- 代码格式参照阿里巴巴 Java 开发手册；修改后同步函数注释、接口描述（Swagger）与本地 CHANGELOG。

---

## ❓ 常见问题

<details>
<summary><b>如何重置管理员密码？</b></summary>

项目新密码统一用 **BCrypt**（强度因子 10）加密，同时兼容历史 **MD5** 密码——MD5 校验通过后会自动升级为 BCrypt。不能用明文 SQL 直接改，推荐通过 API 修改：

```bash
# 1. 登录获取 Session
curl -c cookies.txt -X POST http://localhost:8080/employee/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"原密码"}'

# 2. 修改密码
curl -b cookies.txt -X PUT http://localhost:8080/employee/password \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"原密码","newPassword":"123456"}'
```

或通过 Java 代码生成 BCrypt 哈希后更新数据库：

```java
String hash = BCrypt.hashpw("123456", BCrypt.gensalt(10));
System.out.println(hash); // 将输出值更新到 employee 表
```
</details>

<details>
<summary><b>如何配置数据库连接？</b></summary>

项目默认使用 `dev` 环境（MySQL + Redis）。⚠️ `application*.yml` **被 `.gitignore` 忽略、不入库**，需自行创建 `application-dev.yml`：

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/reggie?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: 123456
      driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
    database: 0
```

> 首次使用需先创建数据库 `CREATE DATABASE reggie CHARACTER SET utf8mb4;`，再按文件名顺序执行 `src/main/resources/db/migration/V*.sql`（项目未启用 Flyway，需手动 source）。⚠️ `db/` 整体被 `.gitignore` 忽略、不入版本库，迁移与演示脚本均本地维护。
</details>

<details>
<summary><b>启用 Redis？</b></summary>

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```
</details>

<details>
<summary><b>AI 助手不工作？</b></summary>

1. 检查数据库 `ai_provider_config` 表中是否有**已启用**的供应商（后台 → AI 供应商管理）
2. 检查 API Key 是否有效且余额充足
3. 检查 `base-url` 网络可达
4. 查看控制台 AI 相关错误日志
5. 排障可临时设 `reggie.ai.enabled: false` 回退到 Mock 模式

> ⚠️ AI 配置以**数据库 `ai_provider_config` 表**为准；`application.yml` 的 `reggie.ai.*` 已废弃，仅作兜底。
</details>

<details>
<summary><b>平台外卖定时任务一直报「拉单失败 / UnknownHostException」？</b></summary>

这是**预期行为（旧版本的噪音日志）**，不是真故障。当前平台外卖为对接骨架：

- 4 个适配器（京东/美团/饿了么/抖音）的接口路径、鉴权与签名均为占位协议，**未接通真实开放平台 API**；京东原网关 `openo2o.jddj.com` 甚至是不存在的假域名（已修正为 `openapi.jddj.com`，但协议仍是占位）。
- 定时任务（拉单 30s / 重试 2min / 恢复 5min）默认受总开关 `reggie.platform.sync-enabled`（默认 `false`）控制——**关闭时任务入口直接跳过，不会再外呼、不再刷 ERROR**。
- 若看到该日志，说明 `sync-enabled` 被误设为 `true`：要么改回 `false`（推荐，未对接前），要么按各平台官方文档重写适配器协议层后再启用。

```yaml
reggie:
  platform:
    sync-enabled: false   # 未真正对接某平台前保持 false，避免空转外呼刷日志
```
</details>

<details>
<summary><b>前端用什么图标库？</b></summary>

本项目已从 iconfont（仅7个图标）迁移到 **Remix Icon 4.6**（Apache 2.0 许可证，2700+ 图标）。

**使用方式**：
```html
<!-- CDN 已引入在 backend/index.html -->
<i class="ri-user-line"></i>         <!-- 用户图标 -->
<i class="ri-robot-3-line"></i>      <!-- AI 图标 -->
```

**常见图标速查**：
| 场景 | 图标类名 |
|------|----------|
| 用户/员工 | `ri-user-3-line` |
| 订单/单据 | `ri-file-list-3-line` |
| 商品/菜品 | `ri-restaurant-2-line` |
| 分类/模块 | `ri-apps-2-line` |
| 锁/安全 | `ri-lock-line` |
| 设置 | `ri-settings-3-line` |
| AI | `ri-robot-3-line` |
| 对话 | `ri-chat-3-line` |

完整图标列表：https://remixicon.com/

> ⚠️ **禁止使用旧 iconfont 类名**（`icon-category`、`icon-member` 等），旧字体文件已全部删除。
</details>

<details>
<summary><b>改了前端页面但刷新不生效？</b></summary>

本机 8080 服务是从 `target/classes/backend` 伺服静态资源的（不是 `src/main/resources/backend`），改完源码必须同步一次：

```bash
rm -rf target/classes/backend && cp -r src/main/resources/backend target/classes/backend
```

同步后强制刷新浏览器（Ctrl + F5）。
</details>

<details>
<summary><b>定时任务日志报「租户上下文缺失，跳过」？</b></summary>

定时任务线程没有登录会话，若直接查业务表，MyBatis-Plus 租户插件会 fail-closed 注入 `tenant_id = -1`，导致查询恒空、任务形同虚设。

正确写法是遍历活跃租户、逐租户注入上下文：

```java
List<Tenant> tenants = tenantService.listActiveTenants();
for (Tenant tenant : tenants) {
    try {
        BaseContext.setCurrentTenantId(tenant.getId());
        // ... 业务查询
    } finally {
        BaseContext.remove();   // 或恢复原有上下文
    }
}
```
</details>

<details>
<summary><b>MyBatis XML 报非法 token / SQL 语法错误？</b></summary>

XML 中 `<if>` 等标签必须写成**真实标签**，不能转义成 `&lt;if&gt;`（否则被当字面文本）；而 SQL 里的比较符 `<`、`<=` 必须写成 `&lt;`、`&lt;=`，否则 Druid 会当作非法 token 拦截。
</details>

<details>
<summary><b>编译 BUILD SUCCESS 但功能不对？</b></summary>

Maven 增量编译会"假成功"：源码有错时 ECJ 生成 `Unresolved compilation problems` 占位 class，仍报 BUILD SUCCESS。改过 Java 后必须：

```bash
mvn -o clean compile    # clean 前先停掉运行中的服务
```
</details>

<details>
<summary><b>测试报「表不存在」？</b></summary>

新增业务表后必须同步到测试 schema，否则测试报 "Table 'xxx' doesn't exist"：

1. 基础表 → `src/test/resources/schema.sql`
2. 模块表 → `src/test/resources/schema-<module>.sql`
3. 测试类加 `@Sql` 加载脚本 + `@DirtiesContext(BEFORE_EACH_TEST_METHOD)`

项目**未启用 Flyway**，`db/migration/` 下的脚本需手动 source 执行。
</details>

<details>
<summary><b>如何修改服务端口？</b></summary>

修改 `application.yml`：

```yaml
server:
  port: 8081
```
</details>

---

## 📞 联系方式

<div align="center">

| 渠道 | 信息 |
|------|------|
| 📲 **付费二开 / 技术支持** | QQ **747011882** · 500 元/次（详见文首） |
| 🔗 项目地址 | [https://gitee.com/itxinfei/reggie](https://gitee.com/itxinfei/reggie) |
| 💬 QQ 交流群 | [661543188](https://qm.qq.com/cgi-bin/qm/qr?k=gNgch-wCkfUu-QbI7DZSudrax2BN7vY0) |
| 📧 邮箱 | [747011882@qq.com](mailto:747011882@qq.com) |
| 🐛 Bug 反馈 | [提交 Issue](https://gitee.com/itxinfei/reggie/issues) |

</div>

---

<div align="center">

### ⭐ 如果这个项目对你有帮助，请给一个 Star 支持！

Made with ❤️ by [itxinfei](https://gitee.com/itxinfei)

**907** Java 源文件 · **75** 管理后台页面 · **22** C 端页面 · **4** 骑手端页面 · **39** 业务模块 · **110** 张数据表 · **671** 次提交

</div>


