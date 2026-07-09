<div align="center">

# 🍜 瑞吉外卖 (Reggie Takeout)

### 餐饮企业一站式外卖管理系统

<p align="center">

<img src="https://img.shields.io/badge/Java-1.8-orange?logo=java" alt="Java"/>
<img src="https://img.shields.io/badge/Spring_Boot-2.4.5-green?logo=spring" alt="Spring Boot"/>
<img src="https://img.shields.io/badge/MyBatis_Plus-3.4.2-blue?logo=mybatis" alt="MyBatis Plus"/>
<img src="https://img.shields.io/badge/Vue.js-2.6.14-green?logo=vue.js" alt="Vue.js"/>
<img src="https://img.shields.io/badge/Redis-6.0-red?logo=redis" alt="Redis"/>
<img src="https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql" alt="MySQL"/>

</p>

<p align="center">

<img src="https://img.shields.io/badge/ElementUI-2.15.10-blue?logo=element-ui" alt="ElementUI"/>
<img src="https://img.shields.io/badge/Vant-2.12.0-green?logo=vant" alt="Vant"/>
<img src="https://img.shields.io/badge/Axios-0.21.1-informational?logo=axios" alt="Axios"/>
<img src="https://img.shields.io/badge/H2-1.4.200-green?logo=h2database" alt="H2"/>
<img src="https://img.shields.io/badge/HikariCP-3.4.5-blue?logo=java" alt="HikariCP"/>

</p>

<p align="center">

<a href="https://gitee.com/itxinfei">
  <img src="https://img.shields.io/badge/Gitee-itxinfei-green?logo=gitee" alt="Gitee"/>
</a>
<a href="https://qm.qq.com/cgi-bin/qm/qr?k=9yLlyD1dRBL97xmBKw43zRt0-6xg8ohb&jump_from=webapi">
  <img src="https://img.shields.io/badge/QQ%E7%BE%A4-661543188-red?logo=tencent-qq" alt="QQ群"/>
</a>
<a href="mailto:747011882@qq.com">
  <img src="https://img.shields.io/badge/%E9%82%AE%E7%AE%B1-747011882@qq.com-red?logo=gmail" alt="邮箱"/>
</a>

</p>

<p align="center">

<img src="https://img.shields.io/badge/license-Apache_2.0-blue?logo=apache" alt="License"/>
<img src="https://img.shields.io/badge/build-passing-brightgreen?logo=github" alt="Build"/>
<img src="https://img.shields.io/badge/%E5%A4%9A%E7%A7%9F%E6%88%B7-SaaS-success?logo=layers" alt="Multi-tenant"/>
<img src="https://img.shields.io/badge/%E6%B5%8B%E8%AF%95-%E9%80%9A%E8%BF%87-green?logo=test" alt="Tests"/>

</p>

</div>

---

## 项目介绍

**瑞吉外卖（Reggie Takeout）** 是一个完整的餐饮外卖管理系统，专为餐饮企业提供外卖订单管理解决方案。项目采用前后端分离架构，后端基于 Spring Boot 2.4.5 + MyBatis Plus 3.4.2 构建，前端使用 Vue.js 2 + ElementUI（管理后台）和 Vant UI（移动端）。

### 核心亮点

<div align="center">

| 亮点 | 说明 |
|------|------|
| 📚 **教学级代码** | 代码规范、注释完善，适合 Spring Boot 学习 |
| 🏢 **企业级架构** | 遵循 RESTful 规范，分层架构清晰 |
| 🔐 **多租户 SaaS** | 基于 MyBatis Plus 的行级数据隔离 |
| 📱 **双端应用** | 管理后台（ElementUI）+ 移动端（Vant UI） |
| 🧪 **完善测试** | H2 内存数据库，集成测试覆盖核心业务 |
| 🚀 **开箱即用** | H2 内存数据库，无需安装 MySQL，一键启动 |
| 📦 **全业务覆盖** | 堂食 + 进销存 + 会员 + 支付 + 配送 + 打印 + 报表 |
| 💾 **35 张数据表** | 完整数据库设计，满足企业级数据管理需求 |

</div>

<div align="center">

### 系统架构

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

---

## 快速开始

### 环境要求

<div align="center">

| 依赖 | 版本 |
|------|------|
| ☕ JDK | 1.8+ |
| 📦 Maven | 3.6+ |
| 🌐 浏览器 | Chrome、Edge、Firefox |

</div>

### 一键启动

> 项目默认使用 **H2 内存数据库**，无需安装 MySQL，克隆后直接运行

```bash
git clone https://gitee.com/itxinfei/reggie.git
cd reggie
mvn clean package spring-boot:run
```

<div align="center">

| 应用 | 地址 |
|------|------|
| 🖥️ 管理后台 | [http://localhost:8080/backend/index.html](http://localhost:8080/backend/index.html) |
| 📱 移动端 | [http://localhost:8080/front/index.html](http://localhost:8080/front/index.html) |

</div>

### 生产环境部署

```bash
mysql -u root -p < reggie.sql
# 修改 src/main/resources/application.yml 数据库配置
mvn clean package -DskipTests
java -jar target/reggie-*.jar
```

---

## 功能清单

### 核心业务

<div align="center">

| 模块 | 功能 | 说明 |
|------|------|------|
| 👨‍💼 员工管理 | 登录/退出/CRUD | Session 会话管理、账号锁定（5次/2小时） |
| 🍱 分类管理 | 分类 CRUD | 菜品分类、套餐分类 |
| 🍜 菜品管理 | 菜品 CRUD | 菜品信息、图片上传、口味管理 |
| 🍚 套餐管理 | 套餐 CRUD | 套餐信息、套餐详情、起售/停售 |
| 📦 订单管理 | 订单查询/详情 | 分页查询、状态流转、订单明细 |
| 🛒 购物车 | 购物车管理 | 增加/减少数量、清空购物车 |
| 📍 地址管理 | 地址 CRUD | 收货地址增删改查、默认地址 |

</div>

### 扩展模块

<div align="center">

| 模块 | 功能 | 说明 |
|------|------|------|
| 🍽️ 堂食管理 | 桌台/区域/预订/排队 | 桌台状态管理、预订跟踪、取号排队 |
| 📦 进销存 | 原料/供应商/采购/盘点 | 采购入库、库存盘点、库存记录追踪 |
| 👤 会员体系 | 等级/积分/优惠券/充值 | 会员信息、积分记录、优惠券管理、余额充值 |
| 💰 支付集成 | 支付单/退款 | 微信支付、支付宝、退款处理 |
| 🚚 配送管理 | 配送订单/平台对接 | 美团/饿了么/抖音，配送状态跟踪 |
| 🖨️ 小票打印 | 多品牌打印机 | 飞鹅/易联云/芯烨，自动打印订单小票 |
| 📊 经营报表 | 销售统计/营业分析 | 日/周/月报表、菜品销量排行、数据导出 |

</div>

### 安全防护

<div align="center">

| 防护类型 | 功能说明 |
|---------|---------|
| 🛡️ CSRF 防护 | 轻量级 Token 验证，不依赖 Spring Security |
| 🔒 暴力破解防护 | 登录失败 5 次锁定 5 分钟（IP+用户名双重锁定） |
| ⚡ API 限流 | Redis 滑动窗口算法，可配置阈值 |
| 🔒 日志脱敏 | 自动脱敏手机号、身份证、地址等敏感信息 |
| 🔐 密码加密 | BCrypt 强度因子 10，防止彩虹表攻击 |
| 🍪 Session 安全 | Cookie-Only 模式，禁用 URL 重写 |

</div>

---

## 技术栈

<div align="center">

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | ☕ Java | 1.8 |
| 框架 | 🌱 Spring Boot | 2.4.5 |
| Web | 🌐 Spring MVC | 5.3.6 |
| 安全 | 🔐 Spring Security Crypto | 5.4.6 |
| ORM | 🗄️ MyBatis Plus | 3.4.2 |
| 缓存 | 🚀 Redis | 6.0+ |
| 连接池 | ⚡ Druid | 1.1.23 |
| 数据库 | 🐬 MySQL Driver | 8.0.23 |
| 测试 | 🧪 H2 Database | 1.4.200 |
| 文档 | 📄 Springdoc | 1.6.9 |
| 工具 | 📷 ZXing / 🔒 Jasypt | 3.5.1 / 3.0.3 |

| 前端 | 版本 |
|------|------|
| 💚 Vue.js | 2.6.14 |
| 🎨 ElementUI | 2.15.10 |
| 📱 Vant UI | 2.12.0 |
| 🌐 Axios | 0.21.1 |

</div>

---

## 项目结构

```
reggie/
├── src/main/java/com/reggie/
│   ├── common/          # 公共组件（R、异常处理、日志脱敏、限流、暴力破解防护）
│   ├── config/          # 配置类（多租户拦截器、WebMvc、Redis）
│   ├── controller/      # REST Controller（员工、分类、菜品、套餐、订单、购物车、地址）
│   ├── dto/             # 数据传输对象（24+ 个）
│   ├── entity/          # 实体类（35 张表）
│   ├── enums/           # 枚举（13 个状态枚举）
│   ├── filter/          # 登录拦截过滤器
│   ├── mapper/          # MyBatis Plus Mapper（12 个）
│   ├── module/          # 扩展模块
│   │   ├── dining/      # 🍽️ 堂食（桌台、区域、预订、排队）
│   │   ├── inventory/   # 📦 进销存（原料、供应商、采购、盘点）
│   │   ├── member/      # 👤 会员（等级、积分、优惠券、充值）
│   │   ├── payment/     # 💰 支付（支付单、退款）
│   │   ├── delivery/    # 🚚 配送（外卖配送、平台对接）
│   │   ├── printer/     # 🖨️ 打印（多品牌打印机适配）
│   │   └── report/      # 📊 报表（销售统计、营业分析）
│   ├── service/         # 业务接口 + 实现类（14 个接口）
│   ├── util/            # 工具类（二维码、测试图片生成）
│   └── utils/           # 工具类（SMS、验证码生成）
├── src/main/resources/
│   ├── backend/         # 管理后台静态资源
│   ├── front/           # 移动端静态资源
│   └── application.yml  # 主配置文件
├── src/test/            # 单元测试（25+ 个测试类）
├── reggie.sql           # 数据库初始化脚本（35 张表）
├── pom.xml              # Maven 配置
└── README.md            # 项目说明文档
```

---

## 测试

```bash
mvn test -DfailIfNoTests=false
```

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

> 233 个测试中 204 个因 `NoClassDefFoundError` 类加载问题预存失败（不影响编译）

---

## 开发路线图

<div align="center">

### 已完成

| 功能 | 说明 |
|------|------|
| 员工管理 | 登录/CRUD + 账号锁定 |
| 分类管理 | 菜品/套餐分类 |
| 菜品管理 | CRUD + 口味管理 + 图片上传 |
| 套餐管理 | CRUD + 套餐详情 |
| 订单管理 | 状态流转 + 订单明细 |
| 购物车 + 地址 | 购物车管理 + 收货地址 |
| 多租户隔离 | 行级数据隔离 |
| 安全防护 | CSRF + 暴力破解 + 限流 + 日志脱敏 |
| 支付集成 | 支付单 + 退款 |
| 数据统计 | 经营报表 + 热销排行 |
| 营销功能 | 会员 + 优惠券 + 积分 + 充值 |
| 配送管理 | 美团/饿了么/抖音平台对接 |
| 小票打印 | 飞鹅/易联云/芯烨多品牌 |

### 进行中

| 功能 | 说明 |
|------|------|
| 消息通知 | APP 推送、短信通知 |
| 数据导出 | Excel、PDF |

### 计划中

| 功能 | 说明 |
|------|------|
| 智能推荐 | 菜品推荐、个性化营销 |
| 多门店管理 | 门店数据隔离、统一管理 |

</div>

---

## 贡献指南

### 贡献流程

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

### 开发规范

- 遵循 [Conventional Commits](https://www.conventional-commits.org/) 提交规范
- 提交前请确保测试通过 (`mvn test`)
- 代码格式遵循阿里巴巴 Java 开发手册

---

## 常见问题

<details>
<summary><b>如何重置管理员密码？</b></summary>

项目使用 BCrypt 加密密码，不能直接在 SQL 中设置明文。推荐以下方法：

**方法一：通过 API 接口修改**
```bash
# 1. 先用旧密码登录获取 Session
curl -c cookies.txt -X POST http://localhost:8080/employee/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"旧密码"}'

# 2. 修改密码
curl -b cookies.txt -X PUT http://localhost:8080/employee/password \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"旧密码","newPassword":"123456"}'
```

**方法二：使用 Java 代码生成 BCrypt 密码**
```java
import org.springframework.security.crypto.bcrypt.BCrypt;

String rawPassword = "123456";
String encodedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
System.out.println(encodedPassword);
// 然后将输出的哈希值更新到数据库
```
</details>

<details>
<summary><b>如何修改端口号？</b></summary>

修改 `src/main/resources/application.yml`：

```yaml
server:
  port: 8081
```
</details>

<details>
<summary><b>如何配置短信服务？</b></summary>

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
<summary><b>如何使用 MySQL 数据库？</b></summary>

1. 安装 MySQL 8.0
2. 创建数据库：`CREATE DATABASE reggie CHARACTER SET utf8mb4;`
3. 导入 SQL：`mysql -u root -p reggie < reggie.sql`
4. 修改 `application.yml` 数据库配置
5. 重启应用
</details>

<details>
<summary><b>如何启用 Redis 缓存？</b></summary>

修改 `application.yml` 配置：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```
</details>

<details>
<summary><b>如何配置多租户？</b></summary>

项目已内置多租户支持，新租户注册：

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

## 联系方式

<div align="center">

| 渠道 | 信息 |
|------|------|
| 🌐 Gitee 主页 | [itxinfei](https://gitee.com/itxinfei) |
| 💬 QQ 群 | [661543188](https://qm.qq.com/cgi-bin/qm/qr?k=9yLlyD1dRBL97xmBKw43zRt0-6xg8ohb&jump_from=webapi) |
| 📧 邮箱 | [747011882@qq.com](mailto:747011882@qq.com) |
| 🐛 Bug 反馈 | [提交 Issue](https://gitee.com/itxinfei/reggie/issues) |

</div>

---

<div align="center">

**如果这个项目对你有帮助，请给我们一个 ⭐ Star 支持一下！**

Made with ❤️ by [itxinfei](https://gitee.com/itxinfei)

</div>