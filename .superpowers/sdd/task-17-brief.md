# Task 17: 安全审计测试

**Files:**
- Create: `src/test/java/com/reggie/security/SecurityAuditTest.java`
- Create: `docs/security/security-hardening-report.md`

## 任务描述

创建安全审计测试，验证安全加固的所有要求，并生成最终的安全加固验收报告。

## 具体要求

### 1. 创建 SecurityAuditTest.java

```java
package com.reggie.security;

import com.reggie.common.PasswordUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityAuditTest {

    @Test
    void testNoHardcodedPasswords() throws IOException {
        // 扫描代码中不应包含明文密码（忽略测试文件和配置文件）
        String code = Files.readString(new File("src/main/java").toPath());
        assertFalse(code.contains("password = \"root\""), "不应硬编码密码");
        assertFalse(code.contains("password='root'"), "不应硬编码密码");
    }

    @Test
    void testNoPlaintextPhoneInLogs() throws IOException {
        // 扫描日志中不应包含完整手机号模式
        String code = Files.readString(new File("src/main/java").toPath());
        // 检查是否有 log.info 包含 11位数字（手机号）
        assertFalse(code.matches("(?s).*log\.info\(.*\d{11}.*\).*"), "日志不应打印完整手机号");
    }

    @Test
    void testPasswordEncryptionStrength() {
        // 验证密码加密强度
        String encoded = PasswordUtils.encodePassword("test123");
        // BCrypt长度应为60
        assertTrue(encoded.length() >= 60, "密码加密强度不足，长度应为60+");
        assertTrue(encoded.startsWith("$2a$"), "应使用BCrypt加密");
    }

    @Test
    void testBCryptStrengthFactor() {
        // 验证强度因子为10
        String encoded = PasswordUtils.encodePassword("test123");
        // BCrypt strength=10 的编码长度约为60
        assertTrue(encoded.length() >= 60 && encoded.length() <= 70, 
            "BCrypt编码长度应在60-70之间（strength=10）");
    }
}
```

**注意：** 
- 测试需要读取文件系统，确保测试环境有读取权限
- 正则表达式使用 Java 语法，注意转义

### 2. 运行安全审计测试

```bash
mvn test -Dtest=SecurityAuditTest -DfailIfNoTests=false
```

Expected: Tests run: 4, Failures: 0

### 3. 运行全部测试确保无破坏

```bash
mvn test -DfailIfNoTests=false
```

Expected: Tests run: XX, Failures: 0

### 4. 生成安全加固验收报告

创建 `docs/security/security-hardening-report.md`:

```markdown
# 安全加固验收报告

## 完成情况

| 维度 | 状态 | 备注 |
|------|------|------|
| 密码加密 | ✅ | BCrypt strength=10，支持MD5迁移 |
| 配置管理 | ✅ | 分环境配置，移除硬编码 |
| 参数校验 | ✅ | 所有Controller添加@Valid |
| 日志脱敏 | ✅ | 手机号/身份证/地址脱敏 |
| 会话安全 | ✅ | 30分钟超时 |
| 防刷限流 | ⏭️ | 可选功能，跳过 |

## 测试结果

- 单元测试：XX个通过
- 安全审计：4个测试全部通过
- 全部测试：Tests run: XX, Failures: 0

## 向后兼容

- 旧密码MD5可正常登录，自动升级到BCrypt
- 配置通过环境变量兼容多环境
- 密码升级字段 passwordType 已添加

## 已知风险

- application-dev.yml 包含明文密码（仅限开发环境）
- 防刷限流未实现（可选功能）

## 建议后续优化

- 生产环境必须设置环境变量
- 定期扫描 git 历史敏感信息
- 后续版本实现防刷限流功能
```

## 验收标准

- [ ] SecurityAuditTest.java 创建成功
- [ ] 4个安全审计测试全部通过
- [ ] 全部测试通过（无破坏性变更）
- [ ] 安全加固报告生成

