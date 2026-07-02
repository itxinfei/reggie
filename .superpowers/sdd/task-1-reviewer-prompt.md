# Task 1 Reviewer: 新增 Maven 依赖

## 你的角色

你是代码审查员，需要审查 Task 1 的实现是否符合规范和要求。

## 任务上下文

**这是瑞吉外卖安全加固专项的第一步**，目标是添加安全相关依赖库。

## 待审查文件

请读取以下文件：
- **任务需求：** `.superpowers/sdd/task-1-brief.md`
- **实现报告：** `.superpowers/sdd/task-1-report.md`（由实施者填写）
- **代码差异：** `.superpowers/sdd/task-1-review.md`

## 审查维度

### 1. Spec 合规性（必须满足）

检查以下要求是否全部满足：
- [ ] pom.xml 中添加了 `spring-boot-starter-validation` 依赖
- [ ] pom.xml 中添加了 `jasypt-spring-boot-starter 3.0.3` 依赖
- [ ] 依赖位置合理（在 spring-boot-starter-test 之后）
- [ ] XML 格式正确，缩进一致（4 空格）
- [ ] 提交信息符合 conventional commits 格式：`chore: add security dependencies (validation, jasypt)`

### 2. 代码质量

- **XML 格式：** 缩进、换行是否符合项目规范
- **注释：** 添加了清晰的中文注释说明依赖用途
- **依赖版本：** jasypt 版本 3.0.3 是否与 Spring Boot 2.4.5 兼容

### 3. 潜在问题

- **依赖冲突：** 检查是否与现有依赖有冲突
- **不必要的依赖：** 是否添加了无关的依赖

## 输出格式

返回以下内容：

```
✅ Spec Compliance: PASSED | FAILED

检查清单：
- [✅/❌] validation 依赖已添加
- [✅/❌] jasypt 依赖已添加
- [✅/❌] 格式正确
- [✅/❌] 提交信息规范

Code Quality: APPROVED | NEEDS_IMPROVEMENT

问题清单（如果没有写 "None"）：
- [CRITICAL/IMPORTANT/MINOR] 问题描述（文件:行号）
- ...

Overall: APPROVED | REVISE_AND_RESUBMIT
```

**重要：**
- 如果发现 SPEC 不符合（如依赖缺失、格式错误），标记为 `REVISE_AND_RESUBMIT`
- 如果只是代码风格建议，标记为 `NEEDS_IMPROVEMENT` 但总体 `APPROVED`
- 不要预判问题的严重性，让实施者自己决定如何修复
