# Task 7: 补充 API 文档注解

**Files:**
- Modify: 所有 Controller 文件（添加 Swagger/OpenAPI 注解）

## 任务描述

为所有 Controller 方法添加 Swagger/OpenAPI 文档注解，提升 API 可读性和可维护性。

## 前置要求

**检查 pom.xml 是否已有 Swagger/OpenAPI 依赖：**

如果没有，添加 SpringDoc OpenAPI 3（替代 Springfox）：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.6.9</version>
</dependency>
```

## 具体要求

### 1. Controller 类添加 @Tag

```java
@RestController
@RequestMapping("/dish")
@Slf4j
@Tag(name = "菜品管理", description = "菜品CRUD接口")
public class DishController {
    // ...
}
```

### 2. 方法添加 @Operation 和参数注解

```java
/**
 * 新增菜品
 * @param dishDto
 * @return
 */
@PostMapping
@Operation(summary = "新增菜品", description = "保存菜品基本信息及口味")
@Parameter(name = "dishDto", description = "菜品DTO", required = true)
@ApiResponse(responseCode = "200", description = "新增成功")
@ApiResponse(responseCode = "400", description = "参数错误")
public R<String> save(@RequestBody DishDto dishDto){
    // ...
}
```

### 3. 需要添加注解的 Controller

- [ ] EmployeeController.java (5个方法)
- [ ] UserController.java (3个方法)
- [ ] CategoryController.java (4个方法)
- [ ] DishController.java (6个方法)
- [ ] SetmealController.java (6个方法)
- [ ] OrderController.java (4个方法)
- [ ] AddressBookController.java (7个方法)
- [ ] ShoppingCartController.java (4个方法)
- [ ] CommonController.java (2个方法)

**总计：约 40+ 个方法**

### 4. 添加 import

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
```

## 验收标准

- [ ] 所有 Controller 类添加 @Tag 注解
- [ ] 所有 public 方法添加 @Operation 注解
- [ ] 关键参数添加 @Parameter 注解
- [ ] 添加 Swagger/OpenAPI 依赖（如果需要）
- [ ] 编译通过
- [ ] 所有现有测试通过

## 验证命令

```bash
mvn test -DfailIfNoTests=false
```

启动应用后访问：
- http://localhost:8080/doc.html（SpringDoc OpenAPI 3）

