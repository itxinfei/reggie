# 项目概览

## 1. 系统定位

**瑞吉外卖 (reggie_take_out)** 是一个面向餐饮门店的**多租户 SaaS 外卖管理系统**，覆盖从商家端（管理后台）到消费者端（C 端用户）的完整业务闭环：

- **商家侧**: 员工登录 → 菜品/套餐/分类维护 → 接单 → 收银/日结 → 财务对账 → 报表分析
- **消费者侧**: 用户登录 → 选品 → 购物车 → 下单 → 支付 → 收货/堂食
- **平台侧**: 与第三方平台（美团/饿了么）双向同步菜品、库存、订单与对账
- **运营侧**: 库存/成本/营销/会员/留存/推荐/客服/门店多店联动/加盟结算

### 三种业态

| 业态 | 标识 | 说明 |
|---|---|---|
| 外卖 | `TAKEOUT` | 传统外卖配送，含骑手、配送时效、地址簿 |
| 堂食 | `EAT_IN` | 桌台、桌区、开台结账，二维码点餐 |
| 排队 | `QUEUE` / `RESERVATION` | 排队叫号、预约到店 |

---

## 2. 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 2.4.5 |
| ORM | MyBatis-Plus | 3.4.2 |
| 数据库 | MySQL | 8.x（生产） / H2（测试） |
| 连接池 | Alibaba Druid | — |
| 缓存 | Redis + Commons Pool2 | — |
| 工具库 | Hutool | 5.8.22 |
| 简化 | Lombok | 1.18.20 |
| API 文档 | SpringDoc OpenAPI | 1.5.13 |
| Excel | Apache POI (SXSSFWorkbook 流式) | — |
| PDF | iText | 5.x（含中文 fallback 字体链） |
| 短信 | 阿里云 SMS SDK | — |
| 管理后台 | Vue2 + Element-UI 2.x + axios | 原生 JS，无编译 |
| C 端 | 原生 JS + Vant + RemixIcon | — |
| 构建 | Maven (Java 1.8) | enforcer + animal-sniffer 双保险 |

### JDK 1.8 硬约束

项目锁定 Java 1.8 语法，禁止使用：

- `var`、`String.isBlank()`、`strip()`、`String.join()`
- `List.of()` / `Set.of()` / `Map.of()` / `Map.ofEntries()`
- `Stream.toList()`
- `jakarta.*` 命名空间（Spring Boot 2.4 使用 `javax.*`）
- 传统 `switch` 表达式、text block、record、sealed
- `Optional.orElseThrow()`（无参形式）、`@Repeatable`
- `CompletableFuture` JDK 9+ 新方法、`java.net.http.HttpClient`

`pom.xml` 中 `maven-enforcer-plugin` + `animal-sniffer-plugin` 双保险，违规构建直接失败。

---

## 3. 目录结构

### 3.1 后端源码

```
src/main/java/com/reggie/
├── ReggieApplication.java       # 启动类
├── common/                      # 公共组件
│   ├── CustomException.java     # 业务异常（GlobalExceptionHandler 返回 422）
│   ├── GlobalExceptionHandler.java
│   ├── BaseContext.java         # ThreadLocal：当前用户/租户上下文
│   ├── AuthConstants.java       # 鉴权常量
│   ├── SecurityConstants.java
│   ├── CsrfTokenUtil.java       # CSRF Token 生成器
│   ├── RateLimit*.java          # 限流注解与切面
│   ├── PasswordUtils.java       # BCrypt 加密
│   ├── LogMaskUtils.java        # 日志脱敏
│   ├── VerifyCodeUtils.java     # 验证码
│   ├── MyMetaObjectHandler.java # MP 元数据填充
│   ├── R.java                   # 统一响应结构
│   └── aspect/ validation/ annotation/ event/ utils/
├── config/                      # Spring 配置类
│   ├── MybatisPlusConfig.java   # MP 拦截器（多租户、分页、乐观锁）
│   ├── JasyptConfig.java        # 生产密钥解密（@Profile("prod")）
│   ├── RedisConfig.java
│   ├── WebMvcConfig.java
│   ├── CorsConfig.java
│   ├── AsyncConfig.java
│   ├── SmsConfig.java           # 短信凭证初始化
│   ├── SessionTimeoutConfig.java
│   ├── SchedulingConfig.java
│   ├── OpenApiConfig.java       # SpringDoc 配置
│   └── WebSocketConfig.java
├── filter/                      # Servlet 过滤器
│   ├── LoginCheckFilter.java    # 登录校验（放行 public/white）
│   ├── SecurityHeaderFilter.java# HTTP 安全头（CSP/nosniff/HSTS...）
│   ├── CsrfFilter.java
│   └── RequestCachingFilter.java
├── enums/                       # 枚举（OrderStatus、DishStatus 等）
├── dto/                         # 遗留通用 DTO（已迁移大部分）
├── utils/                       # 工具类（二维码、短信、日期等）
└── module/                      # ★ 36 个业务模块，见 MODULES_AND_APIS.md
```

### 3.2 前端资源

```
src/main/resources/backend/        # 管理后台
├── index.html                     # 主入口 + 路由
├── page/                          # 页面 HTML（33 个模块）
├── api/                           # API 封装（35 个 .js）
├── js/                            # 公共 JS（组件、请求、校验等）
├── styles/                        # 样式（含 tokens.css 设计令牌）
└── plugins/                       # 第三方资源

src/main/resources/front/          # C 端用户（原生 JS + Vant）
```

### 3.3 配置文件

| 文件 | 用途 |
|---|---|
| `application.yml` | 主配置 |
| `application-dev.yml` | 开发环境（本地 MySQL + Redis） |
| `application-prod.yml` | 生产环境（Jasypt `ENC(...)` + 环境变量注入） |
| `logback-spring.xml` | 日志配置 |
| `scripts/` | Redis Lua 限流脚本 |
| `db/migration/` | 数据库迁移 SQL（例外被 gitignore） |

---

## 4. 构建与运行

```bash
mvn clean compile              # 编译
mvn test                       # 运行测试（402 个测试全部通过）
mvn package -DskipTests        # 打包
```

**开发环境**：本地 MySQL + Redis + Jasypt 关闭（明文配置）  
**生产环境**：敏感值全部 `ENC(...)` 包裹，`JASYPT_ENCRYPTOR_PASSWORD` 环境变量注入

---

## 5. 规模快照

| 维度 | 数量 |
|---|---|
| 业务模块（`module/*`） | 36 |
| Controller 类 | 73 |
| 持久化实体（`@TableName`） | 117 |
| 后台页面模块（`backend/page/*`） | 33 |
| 后台 API 封装（`backend/api/*.js`） | 35 |
| 自动化测试 | 402（100% 通过） |
| 前端 CSS 文件 | 21（含 tokens.css 设计令牌） |

---

## 6. 已交付能力（36 模块清单）

| 类别 | 模块 |
|---|---|
| 基础业务 | auth · user · address · dish · setmeal · category · order · shopping · payment · delivery |
| 堂食 | dining（桌台/排队/预约） |
| 财务 | cashier · finance · cost · export |
| 会员 | member · marketing · recommend · retention |
| 库存 | inventory · cost · franchise |
| 平台对接 | platform（美团/饿了么同步与对账） |
| 运营 | notification · customer-service · printer · attendance · schedule · report · dashboard |
| 组织 | tenant · store · region · sys |
| 智能 | ai（大模型接入/对话/供应商管理） · urgency（异常催单） |

完整清单见 [MODULES_AND_APIS.md](./MODULES_AND_APIS.md)。
