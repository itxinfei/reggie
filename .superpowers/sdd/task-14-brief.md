# Task 14: 替换明文日志

**Files:**
- Modify: 所有包含敏感数据日志的 Controller 文件

## 任务描述

扫描并替换所有 Controller 中的明文手机号/身份证/地址日志，使用 LogMaskUtils 脱敏。

## 扫描范围

执行以下搜索，找出所有需要修改的日志：

```bash
grep -rn "log.info.*phone" src/main/java/com/reggie/controller/
grep -rn "log.info.*user" src/main/java/com/reggie/controller/
grep -rn "log.info.*employee" src/main/java/com/reggie/controller/
grep -rn "log.info.*address" src/main/java/com/reggie/controller/
```

## 具体修改规则

### 1. 手机号日志

**修改前：**
```java
log.info("用户手机号：{}", user.getPhone());
log.info("员工手机号：{}", employee.getPhone());
```

**修改后：**
```java
log.info("用户手机号：{}", LogMaskUtils.maskPhone(user.getPhone()));
log.info("员工手机号：{}", LogMaskUtils.maskPhone(employee.getPhone()));
```

### 2. 身份证日志（如果有）

**修改前：**
```java
log.info("身份证号：{}", user.getIdNumber());
```

**修改后：**
```java
log.info("身份证号：{}", LogMaskUtils.maskIdCard(user.getIdNumber()));
```

### 3. 地址日志（如果有）

**修改前：**
```java
log.info("地址：{}", addressBook.getAddress());
```

**修改后：**
```java
log.info("地址：{}", LogMaskUtils.maskAddress(addressBook.getAddress()));
```

### 4. 添加 import

在每个修改的文件中添加：

```java
import com.reggie.common.LogMaskUtils;
```

## 验收标准

- [ ] 所有包含 phone 的 log 语句已脱敏
- [ ] 所有包含 idNumber 的 log 语句已脱敏（如果有）
- [ ] 所有包含 address 的 log 语句已脱敏（如果有）
- [ ] 添加 LogMaskUtils import
- [ ] 编译通过
- [ ] 所有现有测试通过

## 验证命令

```bash
# 再次扫描确认无遗漏
grep -rn "log.info.*phone" src/main/java/com/reggie/controller/
# 应该没有匹配到包含完整手机号的日志

# 运行测试
mvn test -DfailIfNoTests=false
```

