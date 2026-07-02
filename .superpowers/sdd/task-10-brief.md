# Task 10: EmployeeController 参数校验

**Files:**
- Modify: `src/main/java/com/reggie/entity/Employee.java`
- Modify: `src/main/java/com/reggie/controller/EmployeeController.java`

## 任务描述

为 Employee 实体类和 EmployeeController 添加参数校验注解，防止非法数据入库。

## 具体要求

### 1. Employee.java 添加校验注解

在 Employee 实体的字段上添加以下注解：

```java
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 员工
 */
@Data
public class Employee implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度4-20位")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 30, message = "姓名不能超过30位")
    private String name;

    @Pattern(regexp = SecurityConstants.PHONE_PATTERN, message = "手机号格式不正确")
    private String phone;

    // ... 其他字段保持不变
}
```

**注意：** 只修改 username、name、phone 三个字段，其他字段（password、id、status 等）保持不变。

### 2. EmployeeController.java 添加 @Valid

在 save() 方法的 @RequestBody 参数上添加 @Valid：

```java
@PostMapping
public R<String> save(HttpServletRequest request, @Valid @RequestBody Employee employee) {
    // ... 原有逻辑保持不变
}
```

### 3. 添加 import

```java
import javax.validation.Valid;
```

### 4. 创建测试验证

创建一个简单的测试验证校验生效：

```java
// 在 EmployeeControllerTest 中添加 testSaveEmployeeWithInvalidData() 测试
// 发送空用户名，期望返回校验失败
```

## 验收标准

- [ ] Employee.java 的 username、name、phone 字段添加校验注解
- [ ] EmployeeController.save() 添加 @Valid
- [ ] 添加 Valid import
- [ ] 编译通过
- [ ] 所有现有测试通过

