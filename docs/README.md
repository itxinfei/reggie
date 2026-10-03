# Reggie 外卖系统 — 项目文档

**版本**: 2026-08-29  
**范围**: 项目概览、技术栈、模块清单与职责、API 端点分组、数据模型、后台页面清单、关键设计决策

---

## 文档索引

| 文件 | 内容 |
|---|---|
| [PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md) | 项目概览、技术栈、目录结构、构建与运行 |
| [MODULES_AND_APIS.md](./MODULES_AND_APIS.md) | 36 个业务模块清单、73 个 Controller 与 API 端点分组 |
| [DATA_MODEL.md](./DATA_MODEL.md) | 117 个持久化实体、字段约定、多租户与乐观锁策略 |
| [BACKEND_PAGES.md](./BACKEND_PAGES.md) | 管理后台页面清单、目录结构、组件与样式规范 |
| [ARCHITECTURE_DECISIONS.md](./ARCHITECTURE_DECISIONS.md) | 分层架构、异常处理、多租户、安全、约定等关键设计决策 |

---

## 快速入口

- **想快速了解"这个系统是干什么的"** → [PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md)
- **想查某个业务模块提供了哪些 API** → [MODULES_AND_APIS.md](./MODULES_AND_APIS.md)
- **想查某张表对应的实体类和字段** → [DATA_MODEL.md](./DATA_MODEL.md)
- **想查某个功能在哪个页面** → [BACKEND_PAGES.md](./BACKEND_PAGES.md)
- **想理解"为什么代码是这样写的"** → [ARCHITECTURE_DECISIONS.md](./ARCHITECTURE_DECISIONS.md)

---

## 项目速览

- **性质**: 多租户餐饮全栈系统（开源自托管，面向商家自行部署与二开）
- **后端**: Spring Boot 2.4.5 + MyBatis-Plus 3.5.3.1 + JDK 1.8
- **前端**: 三端免构建静态资源 —— 管理后台 Vue2 + Element-UI、用户点餐端 Vue2 + Vant、骑手端 Vant（均为原生 JS，无编译步骤）
- **规模**: 39 个业务模块 / 105 个 Controller / 124 张表 / 78 个后台页面 / 38 个 API 模块
- **测试**: 124 个测试类、950+ 个测试用例（`mvn test`，含 JaCoCo 覆盖率报告）
