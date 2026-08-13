# Reggie 外卖系统 — Java 代码规范

> 版本：v1.0  
> 更新日期：2026-08-12  
> 适用范围：Spring Boot 2.4.5 + MyBatis-Plus 3.4.2 + JDK 1.8

---

## 一、命名规范

### 1.1 类命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 实体类 | 名词，大驼峰 | `Employee`, `Orders`, `DishFlavor` |
| DAO 接口 | 实体名 + Mapper | `EmployeeMapper`, `OrderMapper` |
| Service 接口 | 实体名 + Service | `EmployeeService`, `OrderService` |
| Service 实现 | 接口名 + Impl | `EmployeeServiceImpl`, `OrderServiceImpl` |
| Controller | 实体名（复数）+ Controller | `EmployeeController`, `DishController` |
| DTO | 业务语义 + DTO | `EmployeeLoginDTO`, `DishSaveDTO` |
| 枚举 | 名词，大驼峰 | `DishStatus`, `OrderStatus`, `EmployeeRole` |
| 工具类 | 功能 + Utils/Helper | `PasswordUtils`, `BatchFillHelper` |
| 配置类 | 功能 + Config | `RedisConfig`, `MybatisPlusConfig` |
| 切面类 | 功能 + Aspect | `RateLimitAspect`, `PermissionAspect` |
| 事件类 | 业务 + Event | `OrderCompletedEvent` |
| 监听器 | 业务 + EventListener | `OrderCompletedEventListener` |

### 1.2 方法命名

| 场景 | 规范 | 示例 |
|------|------|------|
| 查询单个 | get + By + 条件 | `getByUsername()`, `getByIdWithFlavor()` |
| 查询列表 | list + By + 条件 | `listByCategoryId()`, `list()` |
| 分页查询 | page + 条件 | `orderPage()`, `userPage()` |
| 新增 | save / create | `saveDish()`, `createPaymentOrder()` |
| 修改 | update + 什么 | `updateStatus()`, `updateWithFlavor()` |
| 删除 | delete / remove | `deleteWithFlavorCheck()` |
| 判断方法 | is / has / can | `isAdmin()`, `hasVerifyCode()` |
| 转换方法 | to + 目标类型 | `toDTO()`, `toEntity()` |
| 清除方法 | clear / reset | `clearCache()`, `resetFailedAttempts()` |
| 批量操作 | batch + 操作 | `batchDelete()`, `batchUpdate()` |

### 1.3 变量命名

```java
// ✅ 正确
Long employeeId;
String passwordType;
List<DishFlavor> flavors;
Map<String, Object> result;

// ❌ 错误
Long empId;           // 不够清晰
String pwdType;       // 过度缩写
List<DishFlavor> f;   // 单字母变量
```

### 1.4 常量命名

```java
// ✅ 正确：全大写下划线分隔
private static final int MAX_FAILED_ATTEMPTS = 5;
private static final long CACHE_TTL_HOURS = 1;
private static final String PERMISSION_PREFIX = "sys:employee:permissions:";

// ✅ 状态常量使用枚举或接口常量
public static final int STATUS_PENDING_PAY = 1;
public static final int STATUS_ORDERED = 2;

// ❌ 错误
private static final int maxFailed = 5;      // 非全大写
private static final String PREFIX = "sys:";  // 含义不清
```

### 1.5 包命名

```
com.reggie
├── common          # 通用组件（工具类、注解、切面、事件）
│   ├── annotation  # 自定义注解
│   ├── aspect      # AOP 切面
│   ├── event       # 领域事件
│   │   └── listener
│   ├── utils       # 通用工具类
│   └── validation  # 校验器
├── config          # Spring 配置类
├── controller      # 控制器层
├── dto             # 数据传输对象
│   ├── auth
│   ├── dish
│   └── order
├── entity          # 数据库实体
├── enums           # 枚举类
├── filter          # Servlet 过滤器
├── mapper          # MyBatis Mapper 接口
├── module          # 功能模块
│   ├── ai
│   │   ├── adapter
│   │   ├── config
│   │   ├── controller
│   │   ├── mapper
│   │   ├── model
│   │   ├── provider
│   │   └── service
│   │       └── impl
│   ├── payment
│   └── ...
├── service         # 业务服务层
│   └── impl
└── utils           # 项目工具类
```

---

## 二、代码结构规范

### 2.1 类成员顺序

```java
public class XxxController {

    // 1. 常量（private static final）
    private static final int PAGE_SIZE = 10;

    // 2. 字段（@Autowired 注入）
    @Autowired
    private XxxService xxxService;

    // 3. 构造方法
    public XxxController(XxxService xxxService) {
        this.xxxService = xxxService;
    }

    // 4. 公开方法（按 HTTP 方法分组：GET → POST → PUT → DELETE）
    @GetMapping
    public R<List<Xxx>> list() { }

    @PostMapping
    public R<String> save() { }

    @PutMapping
    public R<String> update() { }

    @DeleteMapping
    public R<String> delete() { }

    // 5. 私有方法
    private void validate() { }
}
```

### 2.2 Service 层结构

```java
@Service
@Slf4j
public class XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx> implements XxxService {

    // 依赖注入
    @Autowired
    private OtherService otherService;

    // ========== 查询方法 ==========

    @Override
    public Xxx getByIdWithDetail(Long id) { }

    // ========== 写操作方法 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveXxx(Xxx xxx) { }

    // ========== 私有辅助方法 ==========

    private void validateXxx(Xxx xxx) { }
}
```

### 2.3 import 规范

```java
// ✅ 正确：分组排列，组间空行
package com.reggie.controller;

// 1. Java 标准库
import java.util.List;
import java.util.Map;

// 2. 第三方库
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;

// 3. Spring 框架
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;

// 4. 项目内部
import com.reggie.common.R;
import com.reggie.entity.Employee;
import com.reggie.service.EmployeeService;

// ❌ 错误：通配符导入（注解类除外）
import org.springframework.web.bind.annotation.*;
import java.util.*;
```

> **例外**：注解类的通配符导入可接受（如 `import java.lang.annotation.*;`）

---

## 三、注释规范

### 3.1 类注释（必须）

```java
/**
 * 员工管理控制器
 * <p>
 * 提供员工登录、CRUD、密码管理等接口
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/employee")
public class EmployeeController { }
```

### 3.2 方法注释（公开方法必须）

```java
/**
 * 员工登录
 * <p>
 * 支持密码登录和验证码登录，登录成功后创建 Session
 * </p>
 *
 * @param request HTTP 请求对象
 * @param loginDTO 登录信息（用户名、密码）
 * @return 登录结果（包含员工信息和 token）
 */
@PostMapping("/login")
@Operation(summary = "员工登录", description = "员工账号密码登录")
public R<Map<String, Object>> login(HttpServletRequest request,
                                     @Valid @RequestBody EmployeeLoginDTO loginDTO) { }
```

### 3.3 字段注释

```java
// ✅ 正确：解释业务含义
/** 验证码过期时间（秒） */
private static final int VERIFY_CODE_EXPIRE_SECONDS = 300;

/** 当前用户ID存储 */
private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();

// ❌ 错误：重复描述类型
/** 验证码过期时间常量 */
private static final int VERIFY_CODE_EXPIRE_SECONDS = 300;
```

### 3.4 TODO 规范

```java
// ✅ 正确：包含责任人和时间
// TODO(zhangsan, 2026-08-15): 接入抖音开放平台真实 API

// ✅ 正确：包含 Jira/Issue 编号
// TODO(REG-123): 修复并发库存扣减问题

// ❌ 错误：无责任人
// TODO: 后续优化
```

---

## 四、异常处理规范

### 4.1 异常类型选择

| 场景 | 异常类型 | 示例 |
|------|----------|------|
| 业务校验失败 | `CustomException` | "菜品不存在"、"库存不足" |
| 参数校验失败 | `ConstraintViolationException` | @Valid 自动触发 |
| 数据库约束违反 | `SQLIntegrityConstraintViolationException` | 唯一键冲突 |
| 系统内部错误 | `RuntimeException` | 不应暴露给前端 |
| 预期可恢复异常 | 自定义业务异常 | "订单状态不允许操作" |

### 4.2 异常处理原则

```java
// ✅ 正确：捕获特定异常，记录日志，安全降级
try {
    redisTemplate.opsForValue().set(key, value);
} catch (Exception e) {
    log.warn("[缓存] Redis 写入失败，降级处理: key={}, error={}", key, e.getMessage());
    // 不抛出异常，业务继续
}

// ❌ 错误：捕获所有异常后吞掉
try {
    doSomething();
} catch (Exception e) {
    // 什么都不做
}

// ❌ 错误：暴露内部异常信息
catch (Exception e) {
    return R.error(e.getMessage());  // 可能暴露 SQL、堆栈等敏感信息
}
```

### 4.3 Service 层异常

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void saveDish(Dish dish, List<DishFlavor> flavors) {
    // 业务校验：抛出 CustomException
    if (dish.getCategoryId() != null) {
        Category category = categoryService.getById(dish.getCategoryId());
        if (category == null) {
            throw new CustomException("菜品分类不存在，请先创建分类");
        }
    }

    // 库存校验
    if (dish.getStockQty() != null && dish.getStockQty().compareTo(BigDecimal.ZERO) < 0) {
        throw new CustomException("库存数量不能小于0");
    }

    // 业务逻辑
    this.save(dish);
}
```

---

## 五、安全编码规范

### 5.1 输入校验

```java
// ✅ 正确：使用 JSR-303 注解校验
@PostMapping
public R<String> save(@Valid @RequestBody DishSaveDTO dto) { }

public class DishSaveDTO {
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 50, message = "菜品名称不能超过50个字符")
    private String name;

    @NotNull(message = "菜品价格不能为空")
    @DecimalMin(value = "0.01", message = "菜品价格必须大于0")
    private BigDecimal price;
}

// ✅ 正确：手动校验复杂业务规则
if (!isAdmin(request)) {
    return R.error("权限不足，仅管理员可操作");
}
```

### 5.2 密码安全

```java
// ✅ 正确：使用 BCrypt 加密
String encodedPassword = PasswordUtils.encodePassword(rawPassword);

// ✅ 正确：使用安全随机数生成密码
String randomPassword = SecurityConstants.generateRandomPassword();

// ❌ 错误：MD5 加密（不安全）
String md5 = DigestUtils.md5DigestAsHex(password.getBytes());

// ❌ 错误：硬编码密码
private static final String DEFAULT_PASSWORD = "123456";
```

### 5.3 SQL 注入防护

```java
// ✅ 正确：使用 MyBatis-Plus 的 LambdaQueryWrapper
LambdaQueryWrapper<Employee> qw = new LambdaQueryWrapper<>();
qw.eq(Employee::getUsername, username);

// ✅ 正确：使用参数化查询
@Select("SELECT * FROM employee WHERE username = #{username}")
Employee getByUsername(@Param("username") String username);

// ❌ 错误：字符串拼接 SQL
@Select("SELECT * FROM employee WHERE username = '" + username + "'")
```

### 5.4 敏感信息脱敏

```java
// ✅ 正确：使用 LogMaskUtils 脱敏
log.info("用户登录：手机号={}", LogMaskUtils.maskPhone(phone));
log.info("员工信息：username={}", LogMaskUtils.maskUsername(username));

// ❌ 错误：直接打印敏感信息
log.info("用户登录：phone={}", phone);
log.info("密码：{}", password);
```

### 5.5 权限控制

```java
// ✅ 正确：使用注解控制权限
@RequireEmployee          // 要求员工登录
@RequiresAdmin            // 要求管理员
@RequiresPermission("dish:edit")  // 要求具体权限
@RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)  // 限流
```

---

## 六、性能优化规范

### 6.1 数据库查询

```java
// ✅ 正确：分页查询限制条数
Page<Employee> page = PageUtils.of(pageNum, pageSize);  // 自动限制 max=100

// ✅ 正确：只查询需要的字段
qw.select(Employee::getId, Employee::getName, Employee::getPhone);

// ✅ 正确：批量查询替代循环查询
Set<Long> ids = orders.stream().map(Orders::getUserId).collect(Collectors.toSet());
Map<Long, User> userMap = userService.listByIds(ids).stream()
    .collect(Collectors.toMap(User::getId, Function.identity()));

// ❌ 错误：循环中查询数据库
for (Order order : orders) {
    User user = userService.getById(order.getUserId());  // N+1 问题
}
```

### 6.2 缓存使用

```java
// ✅ 正确：使用缓存双删策略
redisCacheUtil.doubleDelete("setmeal", id);

// ✅ 正确：设置合理的缓存过期时间
@Cacheable(value = "setmeal", key = "#id")  // 15分钟过期（RedisConfig 配置）

// ✅ 正确：Redis 操作降级
@Autowired(required = false)
private RedisTemplate<String, Object> redisTemplate;

if (redisTemplate == null) {
    // 降级到数据库查询
}
```

### 6.3 线程池使用

```java
// ✅ 正确：使用 Spring 管理的线程池
@Resource(name = "recommendExecutor")
private ThreadPoolTaskExecutor executor;

// ✅ 正确：使用 @Async 注解
@Async("eventListenerExecutor")
@EventListener
public void handleOrderCompleted(OrderCompletedEvent event) { }

// ❌ 错误：手动创建线程池
ExecutorService executor = Executors.newFixedThreadPool(4);  // 无法优雅关闭
```

### 6.4 集合操作

```java
// ✅ 正确：预估容量
List<Employee> list = new ArrayList<>(expectedSize);
Map<String, Object> map = new HashMap<>(16);

// ✅ 正确：使用 Stream 高效处理
List<String> names = employees.stream()
    .filter(emp -> emp.getStatus() == 1)
    .map(Employee::getName)
    .collect(Collectors.toList());

// ❌ 错误：循环中频繁创建对象
for (int i = 0; i < 1000; i++) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  // 每次创建新实例
}
```

---

## 七、日志规范

### 7.1 日志级别使用

| 级别 | 场景 | 示例 |
|------|------|------|
| ERROR | 系统异常、需要人工介入 | 数据库连接失败、第三方 API 异常 |
| WARN | 可恢复异常、降级处理 | Redis 不可用降级、缓存写入失败 |
| INFO | 关键业务节点 | 用户登录、订单创建、支付成功 |
| DEBUG | 调试信息 | 方法入参出参、SQL 执行 |

### 7.2 日志格式

```java
// ✅ 正确：使用占位符，包含业务标识
log.info("员工登录成功：userId={}, username={}", empId, LogMaskUtils.maskUsername(username));
log.warn("Redis 缓存写入失败，降级处理：key={}, error={}", key, e.getMessage());
log.error("订单创建失败：orderId={}, userId={}, error={}", orderId, userId, e.getMessage(), e);

// ❌ 错误：字符串拼接
log.info("员工登录成功：" + username);  // 性能差，且 username 为 null 时会 NPE
log.error("异常：" + e.getMessage());    // 缺少堆栈信息
```

### 7.3 日志脱敏

```java
// ✅ 正确：敏感信息必须脱敏
log.info("用户注册：phone={}", LogMaskUtils.maskPhone(phone));
log.info("身份证：idCard={}", LogMaskUtils.maskIdCard(idCard));
log.info("地址：address={}", LogMaskUtils.maskAddress(address));

// ❌ 错误：直接打印敏感信息
log.info("用户注册：phone={}", phone);  // 可能泄露用户隐私
```

---

## 八、数据库规范

### 8.1 实体类

```java
@Data
@TableName("employee")  // 明确指定表名
@Schema(description = "员工")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)      // 自动填充创建时间
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)  // 自动填充更新时间
    private LocalDateTime updateTime;

    @TableLogic                                // 逻辑删除
    private Integer deleted;
}
```

### 8.2 Mapper 接口

```java
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    // ✅ 正确：使用 @Param 注解
    @Select("SELECT * FROM employee WHERE username = #{username} AND tenant_id = #{tenantId}")
    Employee findByUsernameAndTenantId(@Param("username") String username,
                                       @Param("tenantId") Long tenantId);

    // ✅ 正确：复杂查询使用 XML
    List<EmployeeVO> selectWithRole(@Param("tenantId") Long tenantId);
}
```

### 8.3 多租户隔离

```java
// ✅ 正确：MybatisPlusConfig 中配置租户拦截器
// 自动为 SQL 添加 WHERE tenant_id = ?

// ✅ 忽略表（无 tenant_id 列）
private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
    "tenant", "employee", "shopping_cart"
));

// ✅ Mapper 方法上跳过租户过滤
@InterceptorIgnore(tenantLine = "true")
Tenant getSystemTenant();
```

---

## 九、REST API 规范

### 9.1 URL 设计

```
GET    /employee          # 查询列表
GET    /employee/{id}     # 查询详情
POST   /employee          # 新增
PUT    /employee          # 修改
DELETE /employee          # 删除（支持批量）
GET    /employee/options  # 下拉选项
```

### 9.2 响应格式

```java
// ✅ 成功响应
{
    "code": 1,
    "msg": null,
    "data": { ... },
    "requestId": "abc123",
    "timestamp": 1691836800000
}

// ✅ 失败响应
{
    "code": 0,
    "msg": "用户名或密码错误",
    "data": null,
    "requestId": "abc123",
    "timestamp": 1691836800000
}

// ✅ 分页响应
{
    "code": 1,
    "data": {
        "records": [...],
        "total": 100,
        "size": 10,
        "current": 1
    }
}
```

### 9.3 Controller 方法

```java
@GetMapping("/page")
@Operation(summary = "分页查询", description = "支持按名称、状态筛选")
public R<Page<Employee>> page(
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
        @Parameter(description = "员工姓名") @RequestParam(required = false) String name) {

    Page<Employee> pageInfo = PageUtils.of(page, pageSize);
    // 查询逻辑...
    return R.success(pageInfo);
}
```

---

## 十、Git 提交规范

### 10.1 Commit Message 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 10.2 Type 类型

| Type | 说明 | 示例 |
|------|------|------|
| feat | 新功能 | feat(order): 添加订单取消功能 |
| fix | 修复 Bug | fix(payment): 修复支付回调签名验证 |
| refactor | 重构 | refactor(common): 统一 ObjectMapper 为单例 |
| style | 代码格式 | style(controller): 统一 import 排序 |
| docs | 文档 | docs(api): 更新 Swagger 接口说明 |
| test | 测试 | test(order): 添加订单状态流转测试 |
| chore | 构建/工具 | chore(deps): 升级 MyBatis-Plus 到 3.5.x |

### 10.3 示例

```
fix(auth): 修复员工登录后 session 未设置 roleKey 的问题

- LoginCheckFilter 中补充 roleKey 的 session 写入
- PermissionAspect 中增加 roleKey 为空的防御处理

Closes #123
```

---

## 十一、前端 Vue 规范（参考）

### 11.1 文件命名

```
components/
├── EmployeeList.vue       # 组件名大驼峰
├── OrderDetail.vue
└── common/
    ├── SearchBar.vue
    └── Pagination.vue
```

### 11.2 API 调用

```javascript
// ✅ 正确：统一在 api/ 目录封装
// api/employee.js
import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/employee/login',
    method: 'post',
    data
  })
}

// ❌ 错误：直接在组件中写 axios
axios.post('/employee/login', { username, password })
```

---

## 附录：IDE 配置建议

### IntelliJ IDEA

1. **Code Style** → **Java** → **Imports**
   - 勾选 "Use single class import"
   - 勾选 "Use fully qualified class names" 仅限冲突时

2. **Inspections** → **Java**
   - 启用 "Unused import" 警告
   - 启用 "Wildcard import" 警告

3. **Save Actions** 插件
   - 自动格式化
   - 自动整理 import
   - 自动添加 final 修饰符

---

> 📌 本规范基于 Reggie 外卖项目实际代码制定，建议团队统一执行。
