---
name: security-hardening-plan
description: 瑞吉外卖安全加固专项 - 6大维度风险识别与方案设计
metadata:
  type: plan
---

# 瑞吉外卖安全加固专项

## 决策记录

| 项目 | 决策 |
|------|------|
| 密码加密强度 | BCrypt strength=10（平衡方案，约30-50ms） |
| 优先级排序 | P0 → P1 → P2 |
| 实施策略 | 分阶段向后兼容，避免全量破坏性变更 |

---

## 风险点清单（6大维度）

### 1️⃣ 密码加密（CRITICAL）
**现状：**
- ❌ MD5 加密（EmployeeController、TenantController）
- ❌ 硬编码初始密码 "123456"（明文）
- ❌ 密码比对使用 String.equals（时序攻击风险）

**风险：** 彩虹表攻击、GPU 破解、数据库泄露即全站沦陷

**改进：**
- [ ] 引入 BCryptPasswordEncoder
- [ ] 新增 `PasswordUtils` 工具类（统一加密/校验）
- [ ] 批量升级现有密码（双写过渡期，兼容MD5→BCrypt）
- [ ] 密码复杂度校验（长度、大小写、数字、特殊字符）

---

### 2️⃣ 配置管理（CRITICAL）
**现状：**
- ❌ 数据库密码硬编码 `root/root`（application.yml）
- ❌ 无配置加密机制
- ❌ 敏感信息可能在 Git 历史泄露

**风险：** 代码仓库泄露 = 数据库沦陷

**改进：**
- [ ] 引入 Jasypt 或 Spring Cloud Config 加密
- [ ] 使用环境变量/外部配置（Spring Profile）
- [ ] 扫描 Git 历史敏感信息（建议单独处理）
- [ ] 添加 `.env.example` 模板（不含真实密钥）

---

### 3️⃣ 参数校验（HIGH）
**现状：**
- ❌ 所有 Controller 参数无 @Valid 校验
- ❌ 可直接传空值、超长字符串、非法格式
- ❌ 无防 XSS/注入基础过滤

**风险：** 脏数据入库、空指针异常、潜在注入

**改进：**
- [ ] 统一添加 `@Valid` + 校验注解（@NotBlank、@Size、@Pattern）
- [ ] 创建全局异常处理器细化（MethodArgumentNotValidException）
- [ ] 敏感字段脱敏注解（@Sensitive）
- [ ] 分页参数默认值（page=1, pageSize=10）

---

### 4️⃣ 日志脱敏（HIGH）
**现状：**
- ❌ 手机号明文日志（UserController、EmployeeController）
- ❌ 身份证/地址直接打印
- ❌ 订单详情（含用户手机号）大量 log.info

**风险：** 日志泄露 = 用户隐私泄露 = 合规风险

**改进：**
- [ ] 创建 `LogMaskUtils` 工具类
- [ ] 手机号：`138****1234`
- [ ] 身份证：`110***********1234`
- [ ] 地址：保留前3后3
- [ ] 统一替换现有 log 语句

---

### 5️⃣ 防刷限流（MEDIUM）
**现状：**
- ❌ 登录接口无限流（暴力破解）
- ❌ 短信发送接口无频率限制
- ❌ 下单接口无幂等性保护

**风险：** 暴力破解、短信轰炸、重复下单

**改进：**
- [ ] 引入 Redis + RedisRateLimiter（或 AOP 注解）
- [ ] 登录接口：5次/分钟/IP
- [ ] 短信接口：1次/60秒/手机号
- [ ] 下单接口：添加幂等 token

---

### 6️⃣ 会话安全（MEDIUM）
**现状：**
- ❌ Session 无超时配置
- ❌ 无 concurrent session 控制
- ❌ 登录成功后 Session fixation 风险

**风险：** 会话被盗用、越权访问

**改进：**
- [ ] 配置 session-timeout（建议 30 分钟）
- [ ] 配置 concurrent-session 控制（同一账号单点登录）
- [ ] 登录成功后 invalidate + 重新创建 session
- [ ] 添加 remember-me 可选机制（Token 而非 Session）

---

## 实施优先级

| 优先级 | 维度 | 预估工作量 | 风险级别 |
|--------|------|-----------|---------|
| **P0** | 密码加密 | 1天 | CRITICAL |
| **P0** | 配置管理 | 0.5天 | CRITICAL |
| **P1** | 参数校验 | 1天 | HIGH |
| **P1** | 日志脱敏 | 0.5天 | HIGH |
| **P2** | 防刷限流 | 1天 | MEDIUM |
| **P2** | 会话安全 | 0.5天 | MEDIUM |

**总预估：4.5 天**

---

## 向后兼容策略

1. **密码迁移**：新增 `password_type` 字段（MD5/BCrypt），登录时双校验，用户下次登录自动升级
2. **配置迁移**：新增 `application-prod.yml`，本地默认值保留，生产使用加密配置
3. **参数校验**：`@Valid` 新增，老字段保留 optional，逐步收紧
4. **限流**：默认关闭，通过配置开关，白名单放通测试环境

---

## 验收标准

- [ ] 所有接口通过 OWASP Top 10 基础扫描
- [ ] 密码强度符合企业标准（BCrypt strength=10）
- [ ] 无硬编码密钥在代码仓库
- [ ] 日志中无明文手机号/身份证
- [ ] 登录接口可抵御 1000次/分钟暴力破解
- [ ] Session 30分钟无操作自动过期
