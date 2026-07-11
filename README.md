<h1 align="center">🍜 瑞吉外卖 (Reggie Takeout)</h1>

<h3 align="center">搭载 AI 大模型的餐饮全栈管理系统</h3>

<p align="center">

<img src="https://img.shields.io/badge/Java-1.8-orange?logo=openjdk" alt="Java 1.8">
<img src="https://img.shields.io/badge/Spring_Boot-2.4.5-6db33f?logo=springboot" alt="Spring Boot 2.4.5">
<img src="https://img.shields.io/badge/MyBatis_Plus-3.4.2-1677ff?logo=mybatis" alt="MyBatis Plus 3.4.2">
<img src="https://img.shields.io/badge/Vue.js-2.6.14-4fc08d?logo=vuedotjs" alt="Vue.js 2.6.14">
<img src="https://img.shields.io/badge/Redis-6.0-DC382D?logo=redis" alt="Redis">
<img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql" alt="MySQL 8.0">

<br>

<img src="https://img.shields.io/badge/Element_UI-2.15.10-409eff?logo=element" alt="Element UI">
<img src="https://img.shields.io/badge/Vant_UI-2.12.0-07c160?logo=vant" alt="Vant UI">
<img src="https://img.shields.io/badge/H2_Database-1.4.200-0066cc?logo=h2database" alt="H2">
<img src="https://img.shields.io/badge/AI-DeepSeek_/_通义千问_/_OpenAI-8a2be2?logo=openai" alt="AI LLM">
<img src="https://img.shields.io/badge/License-Apache_2.0-333333?logo=apache" alt="License">

<br>

<img src="https://img.shields.io/badge/Modules-12+-1677ff?logo=spring" alt="12+ Modules">
<img src="https://img.shields.io/badge/Data_Tables-50+-ff6b6b?logo=postgresql" alt="50+ Tables">
<img src="https://img.shields.io/badge/Java_Files-407-4379a7?logo=java" alt="407 Java files">
<img src="https://img.shields.io/badge/Total_Commits-271-success?logo=git" alt="271 commits">
<a href="https://gitee.com/itxinfei/reggie"><img src="https://img.shields.io/badge/Gitee-itxinfei/reggie-c71d23?logo=gitee" alt="Gitee"></a>
<a href="https://github.com/itxinfei/reggie"><img src="https://img.shields.io/badge/GitHub-Mirror-181717?logo=github" alt="GitHub Mirror"></a>

</p>

---

## 📖 项目介绍

**瑞吉外卖**是一套完整的餐饮管理系统，前后端分离架构，覆盖堂食、外卖、进销存、会员、支付、打印、报表等餐饮全业务场景。系统核心创新在于 **AI 智能引擎**，通过接入大语言模型实现智能点餐推荐、菜品描述生成、经营分析等能力。

<div align="center">

| 亮点 | 说明 |
|------|------|
| 🏢 **企业级架构** | Spring Boot 2.4.5 + MyBatis Plus 3.4.2，RESTful API，分层清晰 |
| 📱 **双端覆盖** | 管理后台（Element UI）+ 移动端（Vant UI / H5） |
| 🔐 **多租户 SaaS** | 行级数据隔离，一套系统服务多家门店 |
| 🧪 **开箱即用** | H2 内存数据库，无需安装 MySQL，一键启动 |
| 📦 **全业务覆盖** | 堂食 + 外卖配送 + 进销存 + 会员营销 + 支付 + 打印 + 报表 + 数据导出 |
| 🏪 **多门店管理** | 门店 CRUD、数据同步、门店仪表盘、员工权限隔离 |
| 💾 **50+ 张数据表** | 完整数据库设计，满足企业级数据管理需求 |
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
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│   管理后台 (PC)    │◄────►│   Spring Boot    │◄────►│   MySQL 数据库     │
│  Element UI 2.x   │      │   REST API       │      │   50+ 张数据表    │
└──────────────────┘      └────────┬─────────┘      └──────────────────┘
                                    │
┌──────────────────┐               ├──────────────┐
│   移动端 (H5)     │               │              │
│  Vant UI + AI点餐 │───────────────┘              │
└──────────────────┘               ┌──────────────┼──────────────┐
                                    │              │              │
                              ┌─────▼──────┐ ┌────▼─────┐ ┌──────▼─────┐
                              │   Redis    │ │ AI 服务  │ │  第三方服务  │
                              │  缓存/限流  │ │ DeepSeek │ │ 支付/短信   │
                              │   Session  │ │ Qwen/GPT │ │ 打印/配送   │
                              └────────────┘ └──────────┘ └────────────┘
```

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| ☕ JDK | 8+ | 主流 LTS 版本均可 |
| 📦 Maven | 3.6+ | 构建和依赖管理 |
| 🌐 浏览器 | Chrome / Edge / Firefox | 现代浏览器即可 |

### 一键启动

> 项目默认使用 **H2 内存数据库**，无需安装 MySQL，克隆即可运行。

```bash
git clone https://gitee.com/itxinfei/reggie.git
cd reggie
mvn clean package spring-boot:run -DskipTests
```

启动后访问：

| 应用 | 地址 | 说明 |
|------|------|------|
| 🖥️ 管理后台 | http://localhost:8080/backend/index.html | PC 后台管理系统 |
| 📱 移动端 | http://localhost:8080/front/index.html | 手机 H5 点餐端 |
| 🔌 Swagger UI | http://localhost:8080/swagger-ui.html | API 接口文档 |

### 生产部署

```bash
# 1. 导入数据库
mysql -u root -p < reggie.sql

# 2. 修改 src/main/resources/application.yml 数据库配置

# 3. 打包运行
mvn clean package -DskipTests
java -jar target/reggie_take_out-1.0-SNAPSHOT.jar
```

---

## 📋 功能清单

### 核心业务模块

| 模块 | 功能详情 |
|------|----------|
| 👨‍💼 **员工管理** | 登录/退出、员工 CRUD、Session 会话管理、账号锁定（5次失败/2小时） |
| 🍱 **分类管理** | 菜品分类、套餐分类，全量 CRUD |
| 🍜 **菜品管理** | 菜品 CRUD、口味管理、图片上传、起售/停售 |
| 🍚 **套餐管理** | 套餐 CRUD、套餐详情管理、起售/停售 |
| 📦 **订单管理** | 分页查询、状态流转（下单→支付→出餐→完成）、订单明细 |
| 🛒 **购物车** | 增减数量、清空购物车、多品类混合下单 |
| 📍 **地址管理** | 收货地址 CRUD、默认地址、多地址管理 |

### 扩展业务模块

| 模块 | 功能 |
|------|------|
| 🍽️ **堂食管理** | 桌台/区域管理、预订跟踪、取号排队、桌台状态实时看板 |
| 📦 **进销存** | 原料管理、供应商管理、采购入库、库存盘点、库存记录追踪 |
| 👤 **会员体系** | 会员等级、积分管理、优惠券发放/核销、余额充值 |
| 💰 **支付集成** | 支付单创建/查询、退款处理、多支付渠道 |
| 🚚 **外卖配送** | 配送订单管理、美团/饿了么/抖音平台对接 |
| 🖨️ **小票打印** | 飞鹅/易联云/芯烨多品牌适配，订单自动打印 |
| 📊 **经营报表** | 日/周/月销售统计、菜品销量排行、时段经营分析 |
| 🎯 **智能推荐** | 协同过滤推荐引擎、偏好分析、营销活动管理、批量推送 |
| 🔔 **消息通知** | 模板管理、短信通知、APP 推送、多渠道路由、定时触发 |
| 📥 **数据导出** | 订单/菜品/员工/报表多维度 Excel & PDF 导出 |
| 🏪 **多门店** | 门店 CRUD、数据同步、门店仪表盘、员工门店权限隔离 |

### 安全防护体系

| 防护层 | 技术方案 |
|--------|----------|
| 🛡️ **CSRF 防护** | 轻量级 Token 验证，不依赖 Spring Security |
| 🔒 **暴力破解防护** | IP + 用户名双重锁定，5次失败/2小时 |
| ⚡ **API 限流** | Redis 滑动窗口算法，可配置阈值 |
| 🔍 **日志脱敏** | 自动脱敏手机号、身份证、地址等敏感信息 |
| 🔐 **密码加密** | BCrypt 强度因子 10，防彩虹表 |
| 🍪 **Session 安全** | Cookie-Only 模式，HttpOnly + Secure |

---

## 💻 技术栈

<div align="center">

### 后端

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 1.8 |
| 框架 | Spring Boot | 2.4.5 |
| Web | Spring MVC | 5.3.6 |
| ORM | MyBatis Plus | 3.4.2 |
| 缓存 | Redis + Spring Data Redis | 6.0+ |
| 连接池 | Druid + HikariCP | 1.1.23 / 3.4.5 |
| 数据库 | MySQL Driver | 8.0.23 |
| 测试 | H2 Database + JUnit 5 | 1.4.200 |
| 文档 | Springdoc OpenAPI | 1.6.9 |
| 安全 | Spring Security Crypto + Jasypt | 5.4.6 / 3.0.3 |
| 工具 | ZXing 二维码 / AliYun SMS | 3.5.1 / 4.5.16 |
| **AI** | **DeepSeek / 通义千问 / OpenAI / GLM / Claude** | **LLM API** |

### 前端

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue.js | 2.6.14 |
| PC UI | Element UI | 2.15.10 |
| 移动 UI | Vant UI | 2.12.0 |
| HTTP | Axios | 0.21.1 |
| 图标 | Remix Icon | 4.6 (CDN) |

</div>

---

## 📁 项目结构

```
reggie/
├── src/main/java/com/reggie/
│   ├── common/           # 公共组件（R响应封装、全局异常、限流、暴力破解、日志脱敏）
│   ├── config/           # 配置类（WebMvc、MyBatis多租户、Redis、线程池）
│   ├── controller/       # 核心业务 Controller
│   ├── dto/              # 数据传输对象（24+ 个）
│   ├── entity/           # 实体类（50+ 张表）
│   ├── enums/            # 状态枚举（13 个）
│   ├── filter/           # 登录拦截过滤器
│   ├── mapper/           # MyBatis Plus Mapper 接口
│   ├── module/           # 🧩 扩展模块（12 个）
│   │   ├── ai/           #   🤖 AI智能引擎（点餐推荐/描述生成/经营分析/对话管理/用户画像）
│   │   ├── dining/       #   🍽️ 堂食（桌台/区域/预订/排队）
│   │   ├── inventory/    #   📦 进销存（原料/供应商/采购/盘点）
│   │   ├── member/       #   👤 会员（等级/积分/优惠券/充值）
│   │   ├── payment/      #   💰 支付（支付单/退款）
│   │   ├── delivery/     #   🚚 外卖配送（平台对接/状态跟踪）
│   │   ├── printer/      #   🖨️ 小票打印（飞鹅/易联云/芯烨多品牌适配）
│   │   ├── recommend/    #   🎯 智能推荐（协同过滤/偏好分析/营销活动）
│   │   ├── report/       #   📊 经营报表（销售统计/菜品排行/时段分析）
│   │   ├── store/        #   🏪 多门店（门店管理/数据同步/权限隔离）
│   │   ├── notification/ #   🔔 消息通知（模板管理/短信/推送/多渠道路由）
│   │   └── export/       #   📥 数据导出（Excel/PDF多格式）
│   ├── service/          # 业务接口 + 实现类
│   ├── util/             # 工具类（二维码、验证码、文件操作）
│   └── utils/            # 工具类（SMS、测试图片生成）
├── src/main/resources/
│   ├── backend/          # 🖥️ 管理后台（Element UI，50 个页面）
│   ├── front/            # 📱 移动端（Vant UI，12 个页面）
│   ├── scripts/          # 📜 数据库初始化脚本
│   └── application.yml   # 主配置文件
├── src/test/java/        # 🧪 单元测试 + 集成测试
├── reggie.sql            # 🗄️ 数据库建表脚本（50+ 张表）
└── pom.xml               # Maven 配置
```

---

## 🗺️ 开发路线图

### ✅ 已完成

| 里程碑 | 内容 |
|--------|------|
| 🏗️ **基础框架** | Spring Boot + MyBatis Plus 架构搭建、多租户拦截器 |
| 🧑‍💼 **核心业务** | 员工/分类/菜品/套餐/订单/购物车/地址 完整 CRUD |
| 🔐 **安全加固** | CSRF 防护、暴力破解、API 限流、日志脱敏、密码加密 |
| 📦 **扩展模块** | 堂食、进销存、会员、支付、配送、打印、报表（11 个模块） |
| 🎯 **智能推荐** | 协同过滤推荐引擎、偏好分析、营销活动管理 |
| 🏪 **多门店** | 门店 CRUD、数据同步、仪表盘、权限隔离 |
| 🔔 **消息通知** | 模板管理、短信/APP 推送、多渠道路由、定时发送 |
| 📥 **数据导出** | 订单/菜品/员工/报表多维度 Excel & PDF 导出 |
| 🤖 **AI 引擎 v2.0** | 多模型适配器（支持 9 种模型）、SSE 流式输出、对话管理、用户画像 |
| ⚙️ **自动化任务** | 支付订单超时处理、定时订单回收、幂等防重校验 |
| 🎨 **前端优化** | 全站按钮样式统一、表格美化、响应式布局、图标库迁移（iconfont → Remix Icon 4.6，2700+ 图标） |

### 🔮 规划中

| 功能 | 说明 |
|------|------|
| 📈 **AI 销量预测** | 基于历史数据 + LLM 预测菜品销量，辅助采购决策 |
| 💬 **智能客服** | AI 自动回复用户咨询，常见问题自动处理 |
| 🎙️ **语音点餐** | 接入语音识别，支持语音下单 |
| 📊 **数据大屏** | 实时经营数据可视化大屏 |
| 🐳 **Docker 部署** | 一键 Docker Compose 部署方案 |

---

## 🤖 AI 配置指南

### 开发模式（零配置）

AI 引擎默认使用 **Mock 模拟模式**，无需任何配置即可体验基础 AI 功能。

### 接入真实 AI

推荐使用 **DeepSeek**，性价比高：

1. 获取 API Key：[DeepSeek 开放平台](https://platform.deepseek.com/)
2. 配置 `application.yml`：

```yaml
reggie:
  ai:
    enabled: true
    provider: deepseek
    api-key: sk-your-api-key-here
    base-url: https://api.deepseek.com/v1
    model: deepseek-chat
```

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

```bash
mvn test -DfailIfNoTests=false
```

| 测试类型 | 覆盖范围 | 状态 |
|---------|---------|------|
| 集成测试 | Controller 层（@SpringBootTest + MockMvc） | ✅ |
| H2 内存数据库 | 无需 Docker/MySQL，开箱即用 | ✅ |
| 核心业务测试 | 员工登录、菜品查询、订单提交、购物车 | ✅ |
| 多租户测试 | 数据隔离验证 | ✅ |
| 安全组件测试 | 暴力破解、限流、CSRF、密码加密 | ✅ |
| 扩展模块测试 | 堂食、进销存、会员、支付、配送、打印、报表 | ✅ |

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

- 遵循 [Conventional Commits](https://www.conventional-commits.org/) 提交规范
- 代码格式参照阿里巴巴 Java 开发手册
- 提交前确保编译通过

---

## ❓ 常见问题

<details>
<summary><b>如何重置管理员密码？</b></summary>

项目使用 BCrypt 加密，不能用明文 SQL。推荐通过 API 修改：

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
<summary><b>切换到 MySQL？</b></summary>

```sql
CREATE DATABASE reggie CHARACTER SET utf8mb4;
-- mysql -u root -p reggie < reggie.sql
```

修改 `application.yml`：

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/reggie?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: your_password
      driver-class-name: com.mysql.cj.jdbc.Driver
```
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

1. 检查 `reggie.ai.enabled: true` 是否配置
2. 检查 API Key 是否有效且余额充足
3. 检查网络能否访问 AI 服务 API
4. 查看控制台 AI 相关错误日志
5. 设置 `reggie.ai.enabled: false` 可回退到 Mock 模式
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
| 🌐 Gitee | [itxinfei/reggie](https://gitee.com/itxinfei/reggie) |
| 💬 QQ 群 | [661543188](https://qm.qq.com/cgi-bin/qm/qr?k=9yLlyD1dRBL97xmBKw43zRt0-6xg8ohb&jump_from=webapi) |
| 📧 邮箱 | [747011882@qq.com](mailto:747011882@qq.com) |
| 🐛 Bug 反馈 | [提交 Issue](https://gitee.com/itxinfei/reggie/issues) |

</div>

---

<div align="center">

### ⭐ 如果这个项目对你有帮助，请给一个 Star 支持！

Made with ❤️ by [itxinfei](https://gitee.com/itxinfei)

**407** Java 源文件 · **50** 管理后台页面 · **12** 移动端页面 · **271** 次提交

</div>
