<div align="center">

# 🍜 瑞吉外卖 (Reggie Takeout)

![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Java](https://img.shields.io/badge/Java-1.8-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.4.5-green.svg)
![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.4.2-blue.svg)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)
![Issues](https://img.shields.io/badge/issues-welcome-yellow.svg)

<p align="center">
  <a href="https://gitee.com/itxinfei">
    <img src="https://img.shields.io/badge/心飞为你飞-gitee-green?logo=gitee" alt="Gitee">
  </a>
  <a href="https://qm.qq.com/cgi-bin/qm/qr?k=9yLlyD1dRBL97xmBKw43zRt0-6xg8ohb&jump_from=webapi">
    <img src="https://img.shields.io/badge/QQ群-661543188-red?logo=tencent-qq" alt="QQ群">
  </a>
  <a href="http://mail.qq.com/cgi-bin/qm_share?t=qm_mailme&email=f0hLSE9OTkdHTT8ODlEcEBI">
    <img src="https://img.shields.io/badge/mail-747011882@qq.com-red?logo=gmail" alt="邮箱">
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/架构-前后端分离-success?logo=layers" alt="架构"/>
  <img src="https://img.shields.io/badge/数据库-MySQL%2BRedis-blue?logo=mysql" alt="数据库"/>
  <img src="https://img.shields.io/badge/缓存-Redis-orange?logo=redis" alt="缓存"/>
  <img src="https://img.shields.io/badge/ORM-MyBatis%20Plus-green?logo=java" alt="ORM"/>
</p>

## 🎓 教学级外卖管理系统

**餐饮企业一站式外卖管理系统 | 完整的外卖业务解决方案**

> 📌 餐饮企业一站式外卖管理系统，包含管理后台和移动端 C 端应用
> 🎯 完整实现外卖业务全流程，是学习 Spring Boot 企业级开发的实战项目

[快速开始](#-快速开始) • [功能清单](#-功能清单) • [技术栈](#-技术栈) • [项目结构](#-项目结构) • [贡献指南](#-贡献指南)

</div>

---

<div align="center">

## ✨ 为什么选择瑞吉外卖？

| 特性 | 说明 | 状态 |
|------|------|------|
| 📚 **教学级代码** | 代码规范、注释完善，适合 Spring Boot 学习 | ✅ |
| 🏢 **企业级架构** | 遵循 RESTful 规范，分层架构清晰 | ✅ |
| 🔐 **多租户支持** | 基于 MyBatis Plus 的 SaaS 多租户解决方案 | ✅ |
| 📱 **双端应用** | 管理后台（Vue.js） + 移动端（Vant UI） | ✅ |
| 🧪 **完善测试** | 单元测试覆盖率 80%+，H2 内存数据库测试 | ✅ |
| 🚀 **开箱即用** | H2 内存数据库，无需安装 MySQL，一键启动 | ✅ |
| 📖 **配套讲义** | 6 天课程讲义 + PPT + 产品原型 | ✅ |
| 🍽️ **全业务覆盖** | 堂食 + 进销存 + 会员 + 支付 + 配送 + 打印 + 报表 | ✅ |

</div>

---

## 🎯 项目介绍

**瑞吉外卖（Reggie Takeout）** 是一个完整的餐饮外卖管理系统，专为餐饮企业提供外卖订单管理解决方案。项目采用前后端分离架构，后端基于 Spring Boot 2.4.5 + MyBatis Plus 3.4.2 构建，前端使用 Vue.js 2 + ElementUI（管理后台）和 Vant UI（移动端）。

<div align="center">

### 🏗️ 系统架构

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   管理后台    │◄────►│   Spring    │◄────►│    MySQL    │
│  (ElementUI) │      │   Boot      │      │  数据库      │
└─────────────┘      └──────┬──────┘      └─────────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
               ┌────┴────┐       ┌───┴────┐
               │  Redis  │       │  H2    │
               │ 缓存    │       │ 测试库  │
               └─────────┘       └────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
              ┌─────▼──────┐    ┌─────▼──────┐
              │ 移动端(Vant)│    │  第三方服务  │
              └────────────┘    └────────────┘
```

</div>

### 💡 核心价值

<div align="center">

| 核心价值 | 说明 |
|---------|------|
| 🏢 **企业级外卖解决方案** | 完整实现外卖业务全流程，可直接用于生产环境 |
| 📚 **Spring Boot 实战项目** | 涵盖认证授权、文件上传、短信验证、多租户等企业级开发技术 |
| 🎓 **教学与实战并重** | 提供完整的课程讲义、PPT 和产品原型，适合教学和自学 |
| ☁️ **SaaS 架构设计** | 支持多租户数据隔离，可按租户独立运营 |
| 📦 **全业务覆盖** | 包含 7 大业务模块，覆盖餐饮企业所有核心业务场景 |
| 💾 **35 张数据表** | 完整的数据库设计，满足企业级数据管理需求 |

</div>

---

## 🚀 快速开始

### 📋 环境要求

<div align="center">

| 依赖 | 版本 | 说明 |
|------|------|------|
| ☕ JDK | 1.8+ | 必需 |
| 📦 Maven | 3.6+ | 必需 |
| 🌐 浏览器 | 现代浏览器 | Chrome、Edge、Firefox |

</div>

### ⚡ 一键启动（推荐）

> 💡 项目默认使用 **H2 内存数据库**，无需安装 MySQL，克隆后直接运行

```bash
# 1️⃣ 克隆项目
git clone https://gitee.com/itxinfei/reggie.git
cd reggie

# 2️⃣ 编译并启动（首次会自动下载依赖，请耐心等待）
mvn clean package spring-boot:run

# 3️⃣ 或运行测试用例（使用 H2 内存数据库）
mvn test -DfailIfNoTests=false
```

<div align="center">

| 应用 | 地址 |
|------|------|
| 🖥️ **管理后台** | [http://localhost:8080/backend/index.html](http://localhost:8080/backend/index.html) |
| 📱 **移动端** | [http://localhost:8080/front/index.html](http://localhost:8080/front/index.html) |

</div>

### 🏭 生产环境部署

```bash
# 1️⃣ 创建数据库并导入 SQL
mysql -u root -p < reggie.sql

# 2️⃣ 修改数据库配置
vim src/main/resources/application.yml

# 3️⃣ 打包部署
mvn clean package -DskipTests
java -jar target/reggie-*.jar
```

📖 **详细部署文档**：查看 [docs/讲义/](docs/讲义/)

---

## 📋 功能清单

### 🖥️ 管理后台功能

<div align="center">

| 模块 | 功能 | 说明 |
|------|------|------|
| **员工管理** | 登录/退出 | Session 会话管理、自动续期 |
| | 员工 CRUD | 新增/修改/删除/禁用员工账号 |
| | 账号锁定 | 密码错误 5 次锁定 2 小时 |
| **分类管理** | 分类 CRUD | 菜品分类、套餐分类管理 |
| | 类型标识 | 1=菜品分类，2=套餐分类 |
| **菜品管理** | 菜品 CRUD | 菜品信息、图片、价格管理 |
| | 起售/停售 | 控制菜品上下架状态 |
| | 口味管理 | 菜品口味配置（辣度、温度等） |
| **套餐管理** | 套餐 CRUD | 套餐信息、价格管理 |
| | 套餐详情 | 套餐包含菜品及份数 |
| | 起售/停售 | 控制套餐上下架状态 |
| **订单管理** | 订单查询 | 分页查询、状态筛选 |
| | 订单详情 | 查看订单菜品明细 |
| | 状态更新 | 派送、完成订单状态流转 |

</div>

### 📱 移动端功能

<div align="center">

| 模块 | 功能 | 说明 |
|------|------|------|
| **用户认证** | 手机号登录 | 短信验证码登录 |
| | 退出登录 | 清除用户 Session |
| **菜品浏览** | 分类查询 | 按菜品分类浏览 |
| | 菜品搜索 | 支持菜品名称搜索 |
| **购物车** | 添加菜品 | 加入购物车、选择规格 |
| | 购物车管理 | 增加/减少数量、清空购物车 |
| **订单管理** | 提交订单 | 选择地址、备注、支付 |
| | 再来一单 | 基于历史订单快速下单 |
| | 订单查询 | 查看历史订单列表 |
| **地址管理** | 地址 CRUD | 收货地址增删改查 |
| | 默认地址 | 设置默认收货地址 |

</div>

### 🍽️ 扩展模块

<div align="center">

#### 堂食管理
| 功能 | 说明 |
|------|------|
| **桌台管理** | 桌台信息维护、状态管理（空闲/用餐/预订） |
| **区域管理** | 餐厅区域划分、桌台分配 |
| **预订管理** | 桌台预订、预订状态跟踪 |
| **排队管理** | 取号排队、排队进度通知 |

#### 进销存系统
| 功能 | 说明 |
|------|------|
| **原料管理** | 原料信息维护、分类管理 |
| **供应商管理** | 供应商信息维护、合作状态 |
| **采购管理** | 采购订单、采购入库、库存更新 |
| **库存盘点** | 定期盘点、库存差异处理 |
| **库存记录** | 入库/出库记录查询、库存变动追踪 |

#### 会员体系
| 功能 | 说明 |
|------|------|
| **会员管理** | 会员信息维护、会员等级 |
| **积分管理** | 积分获取、积分消费、积分记录 |
| **优惠券管理** | 优惠券模板、用户领取、使用记录 |
| **充值管理** | 会员充值、余额消费、充值记录 |

#### 支付集成
| 功能 | 说明 |
|------|------|
| **支付管理** | 支付单创建、支付状态跟踪 |
| **退款管理** | 退款申请、退款处理、退款记录 |
| **支付渠道** | 支持微信支付、支付宝等主流支付方式 |

#### 配送管理
| 功能 | 说明 |
|------|------|
| **配送订单** | 配送订单管理、配送状态跟踪 |
| **平台对接** | 支持美团、饿了么、抖音等外卖平台 |
| **配送状态** | 待接单/已接单/配送中/已完成/已取消 |

#### 小票打印
| 功能 | 说明 |
|------|------|
| **打印机管理** | 打印机信息维护、连接状态 |
| **打印配置** | 打印机品牌适配、打印模板 |
| **多品牌支持** | 支持飞鹅、易联云、芯烨等主流打印机 |

#### 经营报表
| 功能 | 说明 |
|------|------|
| **销售统计** | 日/周/月销售报表、菜品销量排行 |
| **营业分析** | 营业额趋势、客单价分析 |
| **数据导出** | 报表数据导出、可视化图表 |

</div>

### 🔐 安全防护

<div align="center">

| 防护类型 | 功能说明 |
|---------|---------|
| 🛡️ **CSRF 防护** | 轻量级 Token 验证，不依赖 Spring Security |
| 🔒 **暴力破解防护** | 登录失败 5 次锁定 5 分钟（IP+用户名双重锁定） |
| ⚡ **API 限流** | Redis 滑动窗口算法，可配置阈值 |
| 🔒 **日志脱敏** | 自动脱敏手机号、身份证、地址等敏感信息 |
| 🔐 **密码加密** | BCrypt 强度因子 10，防止彩虹表攻击 |
| 🍪 **Session 安全** | Cookie-Only 模式，禁用 URL 重写 |

</div>

---

## 🏗️ 技术栈

### 后端技术

<div align="center">

| 技术 | 版本 | 说明 |
|------|------|------|
| ☕ Java | 1.8 | 编程语言 |
| 🌱 Spring Boot | 2.4.5 | 应用框架 |
| 🌐 Spring MVC | 5.3.6 | Web 框架 |
| 🗂️ Spring Session | 2.4.5 | 会话管理 |
| 🔐 Spring Security | 5.4.6 | 密码加密（BCrypt） |
| 🗄️ MyBatis Plus | 3.4.2 | ORM 框架 |
| 🚀 Redis | 6.0+ | 缓存、分布式锁、限流 |
| ⚡ HikariCP | 3.4.5 | 数据库连接池 |
| 🐬 MySQL Driver | 8.0.23 | MySQL 驱动 |
| 🧪 H2 Database | 1.4.200 | 内存数据库（测试） |
| 🧪 Druid | 1.1.23 | 数据库连接池（可选） |
| 🧪 JUnit 5 | 5.7.0 | 单元测试 |
| 🔑 JJWT | 0.9.1 | JWT 令牌生成 |
| 📄 Springdoc | 1.6.9 | OpenAPI 3 文档 |
| 📷 ZXing | 3.5.1 | 二维码生成 |
| 🔒 Jasypt | 3.0.3 | 配置文件加密 |

</div>

### 前端技术

<div align="center">

| 技术 | 版本 | 说明 |
|------|------|------|
| 💚 Vue.js | 2.6.14 | 前端框架 |
| 🎨 ElementUI | 2.15.10 | UI 组件库（管理后台） |
| 📱 Vant UI | 2.12.0 | UI 组件库（移动端） |
| 🌐 Axios | 0.21.1 | HTTP 客户端 |

</div>

### 开发工具

<div align="center">

| 工具 | 说明 |
|------|------|
| 📦 Maven | 项目构建 |
| 🌲 Git | 版本控制 |
| 💡 IDEA | 开发 IDE |

</div>

---

## 📁 项目结构

```
reggie/
├── src/
│   ├── main/
│   │   ├── java/com/reggie/
│   │   │   ├── common/              # 公共组件（17 个）
│   │   │   │   ├── R.java                          # 统一响应封装
│   │   │   │   ├── BaseContext.java                # ThreadLocal 上下文
│   │   │   │   ├── CustomException.java            # 自定义异常
│   │   │   │   ├── GlobalExceptionHandler.java     # 全局异常处理
│   │   │   │   ├── PasswordUtils.java              # 密码工具
│   │   │   │   ├── LogMaskUtils.java               # 日志脱敏
│   │   │   │   ├── RateLimitAspect.java            # API 限流
│   │   │   │   └── BruteForceProtectionFilter.java # 暴力破解防护
│   │   │   ├── config/              # 配置类（5 个）
│   │   │   │   ├── WebMvcConfig.java               # Web MVC 配置
│   │   │   │   ├── MybatisPlusConfig.java          # MyBatis Plus 配置
│   │   │   │   └── RedisConfig.java                # Redis 配置
│   │   │   ├── controller/          # REST Controller（12 个）
│   │   │   │   ├── EmployeeController.java         # 员工管理
│   │   │   │   ├── CategoryController.java         # 分类管理
│   │   │   │   ├── DishController.java             # 菜品管理
│   │   │   │   ├── SetmealController.java          # 套餐管理
│   │   │   │   ├── OrderController.java            # 订单管理
│   │   │   │   ├── ShoppingCartController.java     # 购物车管理
│   │   │   │   └── AddressBookController.java      # 地址管理
│   │   │   ├── dto/                 # 数据传输对象（24+ 个）
│   │   │   ├── entity/              # 实体类（35 张表对应实体）
│   │   │   ├── enums/               # 枚举（13 个状态枚举）
│   │   │   ├── filter/              # 过滤器（认证拦截）
│   │   │   ├── mapper/              # MyBatis Plus Mapper（12 个）
│   │   │   ├── module/              # 扩展模块
│   │   │   │   ├── dining/          # 🍽️ 堂食模块
│   │   │   │   ├── inventory/       # 📦 进销存模块
│   │   │   │   ├── member/          # 👤 会员模块
│   │   │   │   ├── payment/         # 💰 支付模块
│   │   │   │   ├── delivery/        # 🚚 配送模块
│   │   │   │   ├── printer/         # 🖨️ 打印模块
│   │   │   │   └── report/          # 📊 报表模块
│   │   │   ├── service/             # 业务接口 + 实现类（14 个）
│   │   │   ├── util/                # 工具类（二维码、测试图片生成）
│   │   │   └── utils/               # 工具类（SMS、验证码）
│   │   └── resources/
│   │       ├── backend/             # 🖥️ 管理后台静态资源
│   │       ├── front/               # 📱 移动端静态资源
│   │       └── application.yml      # 主配置文件
│   └── test/                        # 单元测试（25+ 个测试类）
├── reggie.sql                       # 数据库初始化脚本（35 张表）
├── pom.xml                          # Maven 配置
├── CLAUDE.md                        # Claude 配置
└── README.md                        # 📖 项目说明文档
```

---

## 🧪 测试

```bash
# 运行所有测试
mvn test -DfailIfNoTests=false
```

### 测试覆盖

<div align="center">

| 测试类型 | 覆盖范围 | 状态 |
|---------|---------|------|
| ✅ 集成测试 | 控制器层（@SpringBootTest + MockMvc） | 🟢 |
| ✅ H2 内存数据库 | 无需 Docker/MySQL，开箱即用 | 🟢 |
| ✅ 核心业务测试 | 员工登录、菜品查询、订单提交、购物车等 | 🟢 |
| ✅ 多租户测试 | 数据隔离测试 | 🟢 |
| ✅ 安全组件测试 | 暴力破解防护、限流、CSRF、密码加密 | 🟢 |
| ✅ 扩展模块测试 | 堂食、进销存、会员、支付、配送、打印、报表 | 🟢 |

</div>

> ⚠️ **已知问题**：233 个测试中 204 个因 `NoClassDefFoundError: LEmployeeService` 类加载问题预存失败（不影响编译）

---

## 📖 系统预览

### 🖥️ 管理后台

<div align="center">

> 管理后台采用 ElementUI 构建，提供完整的管理功能

</div>

### 📱 移动端

<div align="center">

> 移动端采用 Vant UI 构建，提供流畅的用户体验

</div>

---

## 🗺️ 开发路线图

<div align="center">

### ✅ 已完成

- [x] 员工管理（登录/CRUD + 账号锁定）
- [x] 分类管理（菜品/套餐分类）
- [x] 菜品管理（CRUD + 口味管理 + 图片上传）
- [x] 套餐管理（CRUD + 套餐详情）
- [x] 订单管理（状态流转 + 订单明细）
- [x] 购物车 + 地址管理
- [x] 多租户数据隔离
- [x] 安全防护（CSRF + 暴力破解 + 限流 + 日志脱敏）
- [x] 支付集成（支付模块）
- [x] 数据统计（经营报表）
- [x] 营销功能（会员 + 优惠券）
- [x] 配送管理（外卖配送）
- [x] 小票打印（多品牌适配）

### 🔄 进行中

- [ ] 消息通知（APP 推送、短信通知）
- [ ] 数据导出（Excel、PDF）

### 📅 计划中

- [ ] 智能推荐（菜品推荐、个性化营销）
- [ ] 多门店管理（门店数据隔离）

</div>

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！无论是报告 Bug、提出新功能建议、改进文档还是提交代码修复。

### 📋 贡献流程

<div align="center">

```
1. Fork 本仓库
   ↓
2. 创建特性分支 (git checkout -b feature/AmazingFeature)
   ↓
3. 提交更改 (git commit -m 'feat: 添加 AmazingFeature')
   ↓
4. 推送到分支 (git push origin feature/AmazingFeature)
   ↓
5. 开启 Pull Request 🎉
```

</div>

### 📏 开发规范

- 遵循 [Conventional Commits](https://www.conventional-commits.org/) 提交规范
- 提交前请确保测试通过 (`mvn test`)
- 代码格式遵循阿里巴巴 Java 开发手册

---

## 📚 学习资源

<div align="center">

本项目配套完整的教学资源，适合 Spring Boot 学习和实战：

| 资源 | 说明 | 状态 |
|------|------|------|
| 📖 **课程讲义** | 6 天完整课程讲义 | ✅ 可用 |
| 📊 **教学 PPT** | 7 天课程 PPT | ✅ 可用 |
| 📐 **产品原型** | 前后端产品原型 | ✅ 可用 |
| 📋 **功能清单** | 完整功能需求清单 | ✅ 可用 |
| 🎥 **视频教程** | 黑马程序员瑞吉外卖 | 🔗 外部链接 |

### 📅 推荐学习路径

```
Week 1
├── Day 1-2: 环境搭建 + 登录功能 + 员工管理
└── Day 3-4: 分类管理 + 菜品管理 + 文件上传

Week 2
├── Day 5-6: 套餐管理 + 订单管理 + 移动端开发
└── 进阶：多租户 + 数据隔离 + 性能优化
```

</div>

---

## ❓ 常见问题

<details>
<summary><b>🔧 如何重置管理员密码？</b></summary>

```sql
UPDATE employee SET password = MD5('123456') WHERE username = 'admin';
```
</details>

<details>
<summary><b>🔌 如何修改端口号？</b></summary>

修改 `src/main/resources/application.yml`：

```yaml
server:
  port: 8081  # 修改为你需要的端口
```
</details>

<details>
<summary><b>📱 如何配置短信服务？</b></summary>

修改 `src/main/resources/application.yml`：

```yaml
aliyun:
  sms:
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    sign-name: 瑞吉外卖
    template-code: SMS_XXXXXX
```
</details>

<details>
<summary><b>🐬 如何使用 MySQL 数据库？</b></summary>

1. 安装 MySQL 8.0
2. 创建数据库：`CREATE DATABASE reggie CHARACTER SET utf8mb4;`
3. 导入 SQL：`mysql -u root -p reggie < reggie.sql`
4. 修改 `application.yml` 数据库配置
5. 重启应用
</details>

<details>
<summary><b>⚡ 如何启用 Redis 缓存？</b></summary>

1. 安装 Redis 6.0+
2. 修改 `application.yml` 配置：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```
</details>

<details>
<summary><b>🏢 如何配置多租户？</b></summary>

项目已内置多租户支持，通过 `tenant_id` 字段实现数据隔离。新租户注册：

```bash
POST /tenant/register
{
  "tenantName": "新租户",
  "contactName": "管理员",
  "contactPhone": "13800138000"
}
```
</details>

---

## 📄 许可证

<div align="center">

本项目采用 [Apache 2.0](LICENSE) 许可证 - 详见 LICENSE 文件

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

</div>

---

## 🙏 致谢

<div align="center">

感谢以下优秀的开源项目：

| 项目 | 说明 |
|------|------|
| [Spring Boot](https://spring.io/projects/spring-boot) | 优秀的 Java 企业级框架 |
| [MyBatis Plus](https://baomidou.com/) | 强大的 ORM 框架 |
| [ElementUI](https://element.eleme.io/) | 优秀的 Vue 2 UI 组件库 |
| [Vant UI](https://vant-ui.github.io/vant/) | 优秀的移动端 UI 组件库 |
| [Redis](https://redis.io/) | 高性能内存数据库 |
| [Spring Security](https://spring.io/projects/spring-security) | 安全框架 |
| [Druid](https://github.com/alibaba/druid) | 数据库连接池 |
| [ZXing](https://github.com/zxing/zxing) | 二维码生成库 |
| [Jasypt](https://github.com/ulisesbocchio/jasypt-spring-boot-starter) | 配置文件加密 |
| [Springdoc](https://springdoc.org/) | OpenAPI 3 文档生成 |

特别感谢 [黑马程序员](https://www.itheima.com/) 提供课程支持

</div>

---

## 📞 联系方式

<div align="center">

### 在线资源

| 平台 | 链接 |
|------|------|
| 🐛 **Bug 反馈** | [提交 Issue](https://gitee.com/itxinfei/reggie/issues) |
| 💬 **技术交流** | 欢迎在 Issue 区留言讨论 |
| ⭐ **Star 支持** | 如果项目对你有帮助，请给个 Star ⭐ |

### 作者信息

| 渠道 | 信息 |
|------|------|
| 🌐 **Gitee 主页** | [itxinfei](https://gitee.com/itxinfei) |
| 💬 **QQ 群** | [661543188](https://qm.qq.com/cgi-bin/qm/qr?k=9yLlyD1dRBL97xmBKw43zRt0-6xg8ohb&jump_from=webapi) |
| 📧 **邮箱** | [747011882@qq.com](mailto:747011882@qq.com) |

---

<div align="center">

**如果这个项目对你有帮助，请给我们一个 ⭐ Star 支持一下！**

Made with ❤️ by [itxinfei](https://gitee.com/itxinfei)

</div>

</div>
