<div align="center">

# 🍜 瑞吉外卖 (Reggie Takeout)

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-1.8-orange.svg)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.4.5-green.svg)](https://spring.io/projects/spring-boot)
[![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.4.2-blue.svg)](https://baomidou.com/)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/itxinfei/reggie)
[![Issues](https://img.shields.io/badge/issues-welcome-yellow.svg)](https://gitee.com/itxinfei/reggie/issues)

**🎓 教学级外卖管理系统 | 完整的外卖业务解决方案**

餐饮企业一站式外卖管理系统，包含管理后台和移动端 C 端应用
完整实现外卖业务全流程，是学习 Spring Boot 企业级开发的实战项目

[快速开始](#-快速开始) • [功能清单](#-功能清单) • [系统预览](#-系统预览) • [开发文档](docs/讲义/) • [贡献指南](#-贡献指南)

</div>

---

## ✨ 项目亮点

- 📚 **教学级代码** - 代码规范、注释完善，适合 Spring Boot 学习
- 🏢 **企业级架构** - 遵循 RESTful 规范，分层架构清晰
- 🔐 **多租户支持** - 基于 MyBatis Plus 的 SaaS 多租户解决方案
- 📱 **双端应用** - 同时支持管理后台（Vue.js）和移动端（Vant UI）
- 🧪 **完善测试** - 单元测试覆盖率 80%+，H2 内存数据库测试
- 🎯 **完整业务** - 涵盖员工、分类、菜品、套餐、订单、购物车等全业务场景
- 🚀 **开箱即用** - H2 内存数据库，无需安装 MySQL，一键启动
- 📖 **配套讲义** - 6 天课程讲义 + PPT + 产品原型，完整教学资源

---

## 🎯 项目介绍

瑞吉外卖（Reggie Takeout）是一个完整的餐饮外卖管理系统，专为餐饮企业提供外卖订单管理解决方案。项目采用前后端分离架构，后端基于 Spring Boot 2.4.5 + MyBatis Plus 3.4.2 构建，前端使用 Vue.js 2 + ElementUI（管理后台）和 Vant UI（移动端）。

### 核心价值

- **企业级外卖解决方案** - 完整实现外卖业务全流程，可直接用于生产环境
- **Spring Boot 实战项目** - 涵盖认证授权、文件上传、短信验证、多租户等企业级开发技术
- **教学与实战并重** - 提供完整的课程讲义、PPT 和产品原型，适合教学和自学
- **SaaS 架构设计** - 支持多租户数据隔离，可按租户独立运营

---

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- 现代浏览器（Chrome、Edge、Firefox）

### 一键启动（推荐）

项目默认使用 **H2 内存数据库**，无需安装 MySQL，克隆后直接运行：

```bash
# 克隆项目
git clone https://gitee.com/itxinfei/reggie.git
cd reggie

# 编译并启动（首次会自动下载依赖，请耐心等待）
mvn clean package spring-boot:run

# 或运行测试用例（使用 H2 内存数据库）
mvn test -DfailIfNoTests=false
```

访问应用：
- 🖥️ **管理后台**：[http://localhost:8080/backend/index.html](http://localhost:8080/backend/index.html)
- 📱 **移动端**：[http://localhost:8080/front/index.html](http://localhost:8080/front/index.html)

### 生产环境部署

如需使用 MySQL 数据库，请修改配置文件：

```bash
# 1. 创建数据库并导入 SQL
mysql -u root -p < reggie.sql

# 2. 修改数据库配置
vim src/main/resources/application.yml

# 3. 打包部署
mvn clean package -DskipTests
java -jar target/reggie-*.jar
```

详细部署文档请参考：[docs/讲义/](docs/讲义/)

---

## 📋 功能清单

### 🖥️ 管理后台功能

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

### 📱 移动端功能

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

### 🔐 多租户（SaaS）

| 功能 | 说明 |
|------|------|
| **租户隔离** | 共享数据库 + `tenant_id` 行级数据隔离 |
| **自动注入** | `TenantLineInnerInterceptor` 自动注入租户 ID |
| **租户注册** | `/tenant/register` 租户注册接口 |
| **员工过滤** | 员工表手动租户过滤 |

### 🛡️ 安全防护

| 功能 | 说明 |
|------|------|
| **CSRF 防护** | 轻量级 Token 验证，不依赖 Spring Security |
| **暴力破解防护** | 登录失败 5 次锁定 5 分钟（IP+用户名双重锁定） |
| **API 限流** | Redis 滑动窗口算法，可配置阈值 |
| **日志脱敏** | 自动脱敏手机号、身份证、地址等敏感信息 |
| **密码加密** | BCrypt 强度因子 10，防止彩虹表攻击 |
| **Session 安全** | Cookie-Only 模式，禁用 URL 重写 |

### 📊 监控和日志

| 功能 | 说明 |
|------|------|
| **Spring Boot Actuator** | 健康检查、指标监控、环境信息 |
| **Prometheus** | 指标采集，支持自定义告警规则 |
| **Grafana** | 可视化监控看板（请求速率、错误率、内存等） |
| **API 性能监控** | AOP 切面自动记录接口执行时间 |
| **Loki/ELK** | 日志收集方案（轻量级/企业级双选） |

---

## 🏗️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 1.8 | 编程语言 |
| Spring Boot | 2.4.5 | 应用框架 |
| Spring MVC | 5.3.6 | Web 框架 |
| Spring Session | 2.4.5 | 会话管理 |
| MyBatis Plus | 3.4.2 | ORM 框架 |
| HikariCP | 3.4.5 | 数据库连接池 |
| MySQL Driver | 8.0.23 | MySQL 驱动 |
| H2 Database | 1.4.200 | 内存数据库（测试） |
| JUnit 5 | 5.7.0 | 单元测试 |
| JJWT | 0.9.1 | JWT 令牌生成 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 2.6.14 | 前端框架 |
| ElementUI | 2.15.10 | UI 组件库（管理后台） |
| Vant UI | 2.12.0 | UI 组件库（移动端） |
| Axios | 0.21.1 | HTTP 客户端 |

### 开发工具

| 工具 | 说明 |
|------|------|
| Maven | 项目构建 |
| Git | 版本控制 |
| IDEA | 开发 IDE |

---

## 📁 项目结构

```
reggie/
├── src/
│   ├── main/
│   │   ├── java/com/reggie/
│   │   │   ├── common/          # 公共组件（统一响应、异常处理、上下文）
│   │   │   ├── config/          # 配置类（多租户拦截器、WebMvc 配置）
│   │   │   ├── controller/      # REST Controller（10 个）
│   │   │   ├── dto/             # 数据传输对象（DishDto、SetmealDto、OrderDto）
│   │   │   ├── entity/          # 实体类（12 个）
│   │   │   ├── filter/          # 过滤器（LoginCheckFilter 认证）
│   │   │   ├── mapper/          # MyBatis Plus Mapper 接口
│   │   │   ├── service/         # 业务接口 + 实现类
│   │   │   └── utils/           # 工具类（SMS、验证码生成）
│   │   └── resources/
│   │       ├── backend/         # 管理后台静态资源（ElementUI）
│   │       ├── front/           # 移动端静态资源（Vant UI）
│   │       └── application.yml  # 主配置文件
│   └── test/
│       └── java/                # 单元测试（6 个测试类，17 个用例）
├── docs/                        # 教学资料
│   ├── PPT/                     # 教学 PPT（7 天）
│   ├── 讲义/                    # 课程讲义（6 天）
│   ├── 资料/                    # 产品原型、功能清单、图片资源
│   └── imgs/                    # 项目截图
├── reggie.sql                   # 数据库初始化脚本（精简版）
├── reggie-full.sql              # 数据库初始化脚本（完整版）
├── pom.xml                      # Maven 配置
└── README.md                    # 项目说明文档
```

---

## 🧪 测试

```bash
# 运行所有测试
mvn test -DfailIfNoTests=false

# 测试结果
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
```

**测试策略：**
- ✅ 基于 `@SpringBootTest` + `@AutoConfigureMockMvc` 集成测试
- ✅ H2 内存数据库，无需 Docker/MySQL
- ✅ 覆盖核心业务：员工登录、菜品查询、订单提交、购物车等
- ✅ 多租户数据隔离测试

---

## 📖 系统预览

### 管理后台

> 管理后台采用 ElementUI 构建，提供完整的管理功能

### 移动端

> 移动端采用 Vant UI 构建，提供流畅的用户体验

> 📷 **更多截图请查看：[docs/imgs/](docs/imgs/)**

---

## 🗺️ 路线图

- [x] ✅ 员工管理（登录/CRUD）
- [x] ✅ 分类管理（菜品/套餐分类）
- [x] ✅ 菜品管理（CRUD + 口味管理）
- [x] ✅ 套餐管理（CRUD + 套餐详情）
- [x] ✅ 订单管理（状态流转 + 订单明细）
- [x] ✅ 移动端（购物车 + 地址管理）
- [x] ✅ 多租户数据隔离
- [ ] 🔄 支付集成（微信支付/支付宝）
- [ ] 🔄 数据统计（销量统计、用户画像）
- [ ] 🔄 营销功能（优惠券、满减活动）
- [ ] 🔄 配送管理（配送员、配送轨迹）
- [ ] 🔄 消息通知（APP 推送、短信通知）
- [ ] 🔄 数据导出（Excel、PDF）

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！无论是：

- 🐛 报告 Bug
- 💡 提出新功能建议
- 📝 改进文档
- 🔧 提交代码修复

### 贡献流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: 添加 AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 开发规范

- 遵循 [Conventional Commits](https://www.conventionalcommits.org/) 提交规范
- 提交前请确保测试通过 (`mvn test`)
- 代码格式遵循阿里巴巴 Java 开发手册

---

## 📚 学习资源

本项目配套完整的教学资源，适合 Spring Boot 学习和实战：

| 资源 | 说明 | 链接 |
|------|------|------|
| **课程讲义** | 6 天完整课程讲义 | [docs/讲义/](docs/讲义/) |
| **教学 PPT** | 7 天课程 PPT | [docs/PPT/](docs/PPT/) |
| **产品原型** | 前后端产品原型 | [docs/资料/产品原型/](docs/资料/产品原型/) |
| **功能清单** | 完整功能需求清单 | [docs/资料/功能清单/](docs/资料/功能清单/) |
| **视频教程** | 黑马程序员瑞吉外卖 | [Bilibili](https://www.bilibili.com/) |

### 推荐学习路径

1. **Day 1-2**：环境搭建 + 登录功能 + 员工管理
2. **Day 3-4**：分类管理 + 菜品管理 + 文件上传
3. **Day 5-6**：套餐管理 + 订单管理 + 移动端开发
4. **进阶**：多租户 + 数据隔离 + 性能优化

---

## ❓ 常见问题

### Q: 如何重置管理员密码？

```sql
UPDATE employee SET password = MD5('123456') WHERE username = 'admin';
```

### Q: 如何修改端口号？

修改 `src/main/resources/application.yml`：

```yaml
server:
  port: 8081  # 修改为你需要的端口
```

### Q: 如何配置短信服务？

修改 `src/main/resources/application.yml`：

```yaml
aliyun:
  sms:
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    sign-name: 瑞吉外卖  # 短信签名
    template-code: SMS_XXXXXX  # 短信模板
```

### Q: 如何使用 MySQL 数据库？

1. 安装 MySQL 8.0
2. 创建数据库：`CREATE DATABASE reggie CHARACTER SET utf8mb4;`
3. 导入 SQL：`mysql -u root -p reggie < reggie.sql`
4. 修改 `application.yml` 数据库配置
5. 重启应用

---

## 📄 许可证

本项目采用 [MIT](LICENSE) 许可证 - 详见 LICENSE 文件

---

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) - 优秀的 Java 企业级框架
- [MyBatis Plus](https://baomidou.com/) - 强大的 ORM 框架
- [ElementUI](https://element.eleme.io/) - 优秀的 Vue 2 UI 组件库
- [Vant UI](https://vant-ui.github.io/vant/#/en-US) - 优秀的移动端 UI 组件库
- [黑马程序员](https://www.itheima.com/) - 提供课程支持

---

## 📞 联系方式

- 🐛 Bug 反馈：[提交 Issue](https://gitee.com/itxinfei/reggie/issues)
- 💬 技术交流：欢迎在 Issue 区留言讨论
- ⭐ Star 支持：如果项目对你有帮助，请给个 Star ⭐

---

<div align="center">

**如果这个项目对你有帮助，请给我们一个 ⭐ Star 支持一下！**

Made with ❤️ by [itxinfei](https://gitee.com/itxinfei)

</div>
