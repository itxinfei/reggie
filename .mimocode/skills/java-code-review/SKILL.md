---
name: java-code-review
description: 针对瑞吉外卖项目的Java代码审查，遵循JDK 8语法和阿里巴巴Java开发手册规范。审查范围涵盖命名规约、OOP规约、集合处理、并发处理、异常处理、MyBatis-Plus用法、多租户架构合规性。
allowed-tools: Read, Grep, Glob, Bash
---

# Java Code Review - 瑞吉外卖项目

针对本项目（Spring Boot 2.4.5 + MyBatis-Plus 3.4.2 + JDK 8）的专项代码审查。

## 审查范围

### 1. JDK 8 语法合规性
- 禁止使用 JDK 9+ API（如 `List.of()`, `Map.of()`, `String.isBlank()`, `var` 关键字）
- 禁止使用 `record` 类、`sealed` 类、`switch` 表达式
- 禁止使用 `HttpClient`（JDK 11+）
- 集合初始化必须指定容量：`new ArrayList<>(16)`, `new HashMap<>(16)`

### 2. 阿里巴巴 Java 开发手册规范
- **命名规约**：类名 UpperCamelCase，方法名 lowerCamelCase，常量全大写+下划线
- **常量定义**：Long 赋值使用大写 L（`Long id = 1L` 不是 `1l`）
- **OOP规约**：包装类比较用 `equals()`，禁止 `==`；`@SuppressWarnings` 必须注明理由
- **集合处理**：禁止在 foreach 循环中进行元素的 remove/add 操作；集合初始化指定容量
- **并发处理**：线程池禁止使用 Executors 创建；高并发下同步调用考虑锁性能
- **异常处理**：catch 异常必须打日志；不要吞掉异常（禁止空 catch 块）；finally 块必须对资源进行释放
- **日志规约**：使用 SLF4J + 占位符，禁止 `System.out.println`；日志禁止使用 emoji

### 3. MyBatis-Plus 规范
- Mapper 方法必须标注 `@Override`
- 禁止在 XML 中使用 `${}` 拼接 SQL（防注入）
- 分页查询使用 `Page` 对象，不要手写 LIMIT
- 多租户场景注意 `TenantLineInnerInterceptor` 的 `IGNORE_TABLES` 配置

### 4. 项目特定规则
- `BaseContext` (ThreadLocal) 用于存储当前用户/租户信息，使用后必须 `remove()`
- `LoginCheckFilter` 白名单路径需保持一致
- `MyMetaObjecthandler` 自动填充字段：createTime/updateTime/createUser/updateUser/tenantId
- 上传路径配置 `reggie.path` 必须有尾部斜杠（`./uploads/`）

## 审查流程

1. **读取 CLAUDE.md** 获取项目最新规范
2. **扫描 Java 源文件**：`src/main/java/com/reggie/**/*.java`
3. **逐模块审查**：controller → service → entity → dto → mapper → config → common
4. **输出报告**：按严重程度分类（P0必须修复 / P1建议修复 / P2可选优化）

## 输出格式

```markdown
## 代码审查报告

### P0 - 必须修复
- [JDK8] 文件:行号 - 使用了 JDK 9+ API `xxx`，应替换为 `yyy`
- [规范] 文件:行号 - 违反阿里巴巴规范：xxx

### P1 - 建议修复
- [性能] 文件:行号 - 建议优化 xxx
- [安全] 文件:行号 - 潜在安全风险 xxx

### P2 - 可选优化
- [代码质量] 文件:行号 - 建议重构 xxx

### 统计
- 审查文件数：N
- P0 问题：N 个
- P1 问题：N 个
- P2 问题：N 个
```

## 审查清单

- [ ] 所有 Java 文件无 JDK 9+ API
- [ ] 常量 Long 使用大写 L
- [ ] 包装类比较使用 equals()
- [ ] 集合初始化指定容量
- [ ] 无空 catch 块
- [ ] 日志使用 SLF4J 占位符
- [ ] 无 System.out.println
- [ ] ThreadLocal 使用后 remove()
- [ ] @Override 标注完整
- [ ] @SuppressWarnings 有注释理由
