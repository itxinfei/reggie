# Task 11: UserController 参数校验

**Files:**
- Modify: `src/main/java/com/reggie/entity\User.java`
- Modify: `src/main/java/com/reggie/controller\UserController.java`（如需要）

## 任务描述

为 User 实体添加参数校验注解。

## 具体要求

### 1. User.java 添加校验注解

```java
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class User implements Serializable {

    // ... id, tenantId 保持不变

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 30, message = "姓名不能超过30位")
    private String name;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = SecurityConstants.PHONE_PATTERN, message = "手机号格式不正确")
    private String phone;

    // ... sex, idNumber, avatar, status 保持不变
}
```

## 验收标准

- [ ] User.java 的 name 和 phone 字段添加校验注解
- [ ] 编译通过

