# 修复 M3: UserController 验证码日志

**Files:**
- Modify: `src/main/java/com/reggie/controller\UserController.java`

## 问题描述

UserController.sendMsg() 方法中，验证码明文打印到日志，存在安全风险。

## 修复内容

### sendMsg() 方法（第47行）

**修改前：**
```java
String code = ValidateCodeUtils.generateValidateCode(4).toString();
log.info("code={}",code);
```

**修改后（推荐方案 - 完全删除日志）：**
```java
String code = ValidateCodeUtils.generateValidateCode(4).toString();
// 验证码已保存到Session，无需打印日志
```

**备选方案（如果仍需调试日志）：**
```java
String code = ValidateCodeUtils.generateValidateCode(4).toString();
// 脱敏日志：只显示前1位
log.info("验证码已生成：{}***", code.substring(0, 1));
```

## 验收标准

- [ ] 删除或脱敏验证码明文日志
- [ ] 验证码仍正确保存到 Session
- [ ] 编译通过
- [ ] 所有现有测试通过
- [ ] sendMsg 接口功能正常

