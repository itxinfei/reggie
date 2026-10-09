# 贡献指南 · Contributing

首先，感谢你对瑞吉外卖（Reggie Takeout）的关注！欢迎通过各种方式参与贡献。本项目基于 **Apache License 2.0** 开源。

## 一、你可以这样贡献

- 🐛 提交 Issue：报告 Bug、提出功能建议；
- 🔀 提交 Pull Request：修复问题、改进代码、完善文档；
- 📝 完善文档：订正文档、补充使用说明；
- 💬 在交流群帮助其他用户答疑。

## 二、开发环境准备

- **JDK 8**（强制）、Maven 3.6+、MySQL 8、Redis 6+；
- 前端为免构建静态资源，无需 Node.js（仅跑 E2E 时需要）；
- 按 README「快速开始」把项目在本地跑通。

## 三、代码规范（务必遵守）

1. **JDK 8 语法**：禁止 `var`、`String.isBlank()`、`List.of()/Map.of()`、`Stream.toList()`、`jakarta.*`、switch 表达式、record 等 JDK 9+ 特性（构建期 enforcer + animal-sniffer 会拦截）。
2. **分层**：Controller → Service(Impl) → Mapper；统一返回 `R<T>`，业务异常抛 `CustomException`。
3. **模块化**：新增业务放在 `com.reggie.module.{模块名}` 下，禁止在根包平铺。
4. **多租户**：查询需按租户隔离；异步 / 定时任务手动管理 `BaseContext` 的 ThreadLocal。
5. **前端后台**：Vue2 + Element-UI，默认极简、功能优先，不做视觉美化；弹窗样式用 `custom-class`。
6. 禁止 `System.out.println`，使用 SLF4J；不提交密钥、密码等敏感信息。

> 更完整的开发约定见项目 Wiki「二次开发指南」。

## 四、提交信息规范

遵循 [Conventional Commi­ts](https://www.conventionalcommits.org/)：

```
<type>: <description>
```

`type` 可选：`feat`（新功能）、`fix`（修复）、`refactor`（重构）、`perf`（性能）、`docs`（文档）、`test`（测试）、`chore`（杂项）、`ci`。

示例：`feat: 新增骑手接单批量抢单接口`

## 五、Pull Request 流程

1. Fork 本仓库并新建分支，分支名建议 `feat/xxx`、`fix/xxx`；
2. 在分支上开发并本地验证：`mvn clean compile`，必要时 `mvn test`；
3. 提交信息遵循上述规范；
4. 发起 PR，并按 PR 模板填写说明与自检清单；
5. 等待维护者 Review，根据反馈修改。

> 请基于 `master` 分支开发。涉及数据库表结构变更时，记得同步 `src/test/resources/schema-*.sql`。

## 六、反馈与交流

- QQ 交流群：**661543188**
- 商务 / 技术支持 QQ：**747011882**
- Issue：[gitee.com/itxinfei/reggie/issues](https://gitee.com/itxinfei/reggie/issues)

再次感谢你的贡献，一起把项目做得更好！🎉
