# Reggie 外卖系统 — Codex 开发指引

## 技术栈

### Java 后端
- Spring Boot 2.4.5（**禁止升级到 3.x**，需 Jakarta 命名空间）
- MyBatis-Plus 3.4.2 + Druid 连接池
- Redis + Commons Pool2
- MySQL 8.x（测试用 H2）
- Lombok 1.18.20
- Hutool 5.8.22
- SpringDoc OpenAPI 1.5.13
- Maven 构建，Java 1.8

### 前端管理后台（src/main/resources/backend/）
- Vue 2 + Element UI（原生 JS，无编译、无 TypeScript）
- ECharts 图表
- RemixIcon 图标
- 传统 HTML 多页面架构

### 前端用户端（src/main/resources/front/）
- Vant.js 移动端组件库（Vue 2 版本）
- 原生 JS，无 TypeScript
- 传统 HTML 多页面架构

## 工程约定

### JDK 1.8 硬约束
- 禁止使用 JDK 9+ 语法：`var`、`String.isBlank()`、`List.of()`、`Map.of()`、`switch` 表达式、`record`、`sealed`、text blocks
- 禁止使用 `jakarta.*` 命名空间，必须用 `javax.*`
- `pom.xml` 已配置 `animal-sniffer-plugin` + `maven-enforcer-plugin` 双保险
- 替代方案：`StringUtils.isBlank()`（Hutool/Commons-Lang3）、`Arrays.asList()`、`Collectors.toList()`

### 后端分层
- Controller → Service → Mapper（MyBatis-Plus）
- DTO 按业务域分子包（auth, dish, order 等）
- 模块化：module/payment, module/printer, module/recommend, module/report, module/store, module/sys 等

### 前端规范
- 管理后台：原生 JS + Vue 2 options API，引入 Element UI 组件
- 用户端：原生 JS + Vant 组件
- API 调用统一通过 `api/*.js` 封装 Axios 请求
- **禁止引入 Vue 3 / Composition API / TypeScript / Vite**

### REST API
- 统一响应结构：`com.reggie.common.R<T>`
- OpenAPI/Swagger 文档：`/swagger-ui.html`

## 文件优先级
- 优先在已有的工具类和辅助方法中寻找复用机会
- 新增依赖前先评估是否已有等效实现
- 数据库迁移脚本放 `src/main/resources/db/migration`

## 测试
- 单元测试用 H2 内存数据库
- 测试类在 `src/test/java/com/reggie/`
- 使用 Spring Boot Test + MockMvc
