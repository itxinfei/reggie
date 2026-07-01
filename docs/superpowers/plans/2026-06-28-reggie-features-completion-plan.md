# 瑞吉外卖功能补全实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补全瑞吉外卖项目中所有未实现的后台和移动端功能，并编写通过测试

**架构:** 在现有 Spring Boot + MyBatis Plus 分层架构上扩展，遵循已有编码风格（Controller → Service → ServiceImpl → Mapper），新增 H2 内存数据库测试环境

**Tech Stack:** Spring Boot 2.4.5, MyBatis Plus 3.4.2, Java 8, H2 Database, JUnit 5, MockMvc

**全局约束:**
- Java 1.8 编译级别
- 遵循现有项目编码风格（无注解式 @Valid、无 Swagger）
- 所有前端请求参数格式和返回格式与前端 JS 定义一致
- 使用 `R<T>` 统一响应封装（code=1 成功, code=0 失败）
- 使用 MyBatis Plus `ASSIGN_ID` 作为 ID 生成策略
- 前端传递的 JSON 字段使用下划线命名（如 `dish_status`），后端实体使用驼峰命名
- H2 兼容模式使用 `MODE=MYSQL`

---
### Task 1: 菜品起售/停售功能

**Files:**
- Modify: `src/main/java/com/reggie/service/DishService.java` — 新增 `updateStatus(Integer status, List<Long> ids)` 方法
- Modify: `src/main/java/com/reggie/service/impl/DishServiceImpl.java` — 实现批量状态更新
- Modify: `src/main/java/com/reggie/controller/DishController.java` — 新增 `POST /dish/status/{status}` 端点
- Test: `src/test/java/com/reggie/controller/DishControllerTest.java`

**Interfaces:**
- Produces: `DishService.updateStatus(Integer status, List<Long> ids)`
- Consumes: 前端 `POST /dish/status/{status}?ids=1,2,3`

- [x] **Step 1: DishService 新增接口方法**

添加到 `DishService.java`：
```java
public void updateStatus(Integer status, List<Long> ids);
```

- [x] **Step 2: DishServiceImpl 实现**

添加到 `DishServiceImpl.java`：
```java
@Override
@Transactional
public void updateStatus(Integer status, List<Long> ids) {
    LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
    updateWrapper.in(ids != null, Dish::getId, ids);
    updateWrapper.set(Dish::getStatus, status);
    this.update(updateWrapper);
}
```
`import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;`

- [x] **Step 3: DishController 新增端点**

添加到 `DishController.java` `update` 方法之后：
```java
/**
 * 批量起售/停售
 */
@PostMapping("/status/{status}")
public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
    dishService.updateStatus(status, ids);
    return R.success("操作成功");
}
```

- [x] **Step 4: 测试配置——pom.xml 添加 H2 依赖**

添加到 `pom.xml` `<dependencies>` 中：
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

- [x] **Step 5: 创建测试 schema.sql**

创建 `src/test/resources/schema.sql`：
```sql
DROP TABLE IF EXISTS dish;

CREATE TABLE dish (
  id bigint(20) NOT NULL,
  name varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '菜品名称',
  category_id bigint(20) DEFAULT NULL COMMENT '菜品分类id',
  price decimal(10,2) DEFAULT NULL COMMENT '菜品价格',
  code varchar(64) COLLATE utf8_bin NOT NULL COMMENT '商品码',
  image varchar(200) COLLATE utf8_bin DEFAULT NULL COMMENT '图片',
  description varchar(400) COLLATE utf8_bin DEFAULT NULL COMMENT '描述信息',
  status int(11) DEFAULT '1' COMMENT '0 停售 1 起售',
  sort int(11) DEFAULT '0' COMMENT '顺序',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  is_deleted int(11) DEFAULT '0',
  PRIMARY KEY (id) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin ROW_FORMAT=COMPACT;
```

- [x] **Step 6: 创建 application-test.yml**

创建 `src/test/resources/application-test.yml`：
```yaml
spring:
  datasource:
    druid:
      driver-class-name: org.h2.Driver
      url: jdbc:h2:mem:reggie;MODE=MYSQL;DB_CLOSE_DELAY=-1
      username: sa
      password:
  sql:
    init:
      schema-locations: classpath:schema.sql
      mode: always
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: ASSIGN_ID
```

- [x] **Step 7: 创建 DishControllerTest**

创建 `src/test/java/com/reggie/controller/DishControllerTest.java`：
```java
package com.reggie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.entity.Dish;
import com.reggie.service.DishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DishService dishService;

    @BeforeEach
    void setUp() {
        Dish dish = new Dish();
        dish.setId(1L);
        dish.setName("测试菜品");
        dish.setCategoryId(1L);
        dish.setPrice(new BigDecimal("10.00"));
        dish.setCode("001");
        dish.setImage("test.jpg");
        dish.setStatus(1);
        dish.setSort(1);
        dishService.save(dish);
    }

    @Test
    void testUpdateStatus() throws Exception {
        mockMvc.perform(post("/dish/status/0")
                .param("ids", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        Dish updated = dishService.getById(1L);
        assert updated.getStatus() == 0;
    }

    @Test
    void testUpdateStatusBatch() throws Exception {
        Dish dish2 = new Dish();
        dish2.setId(2L);
        dish2.setName("测试菜品2");
        dish2.setCategoryId(1L);
        dish2.setPrice(new BigDecimal("20.00"));
        dish2.setCode("002");
        dish2.setImage("test2.jpg");
        dish2.setStatus(1);
        dish2.setSort(2);
        dishService.save(dish2);

        mockMvc.perform(post("/dish/status/0")
                .param("ids", "1,2")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assert dishService.getById(1L).getStatus() == 0;
        assert dishService.getById(2L).getStatus() == 0;
    }
}
```

- [x] **Step 8: 运行测试验证通过**

Run: `mvn test -Dtest=DishControllerTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS

---
### Task 2: 套餐修改功能

**Files:**
- Modify: `src/main/java/com/reggie/service/SetmealService.java` — 新增 `getByIdWithDish(Long id)` 和 `updateWithDish(SetmealDto setmealDto)`
- Modify: `src/main/java/com/reggie/service/impl/SetmealServiceImpl.java` — 实现
- Modify: `src/main/java/com/reggie/controller/SetmealController.java` — 新增 `GET /setmeal/{id}` 和 `PUT /setmeal`
- Test: `src/test/java/com/reggie/controller/SetmealControllerTest.java`

**Interfaces:**
- Produces: `SetmealService.getByIdWithDish(Long)`, `SetmealService.updateWithDish(SetmealDto)`
- Consumes: 前端 `PUT /setmeal` (SetmealDto JSON), `GET /setmeal/{id}`

- [x] **Step 1: SetmealService 新增接口方法**

```java
public SetmealDto getByIdWithDish(Long id);
public void updateWithDish(SetmealDto setmealDto);
```

- [x] **Step 2: SetmealServiceImpl 实现**

```java
@Override
public SetmealDto getByIdWithDish(Long id) {
    Setmeal setmeal = this.getById(id);
    SetmealDto setmealDto = new SetmealDto();
    BeanUtils.copyProperties(setmeal, setmealDto);

    LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
    List<SetmealDish> dishes = setmealDishService.list(queryWrapper);
    setmealDto.setSetmealDishes(dishes);
    return setmealDto;
}

@Override
@Transactional
public void updateWithDish(SetmealDto setmealDto) {
    this.updateById(setmealDto);

    LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
    setmealDishService.remove(queryWrapper);

    List<SetmealDish> dishes = setmealDto.getSetmealDishes();
    dishes = dishes.stream().map((item) -> {
        item.setSetmealId(setmealDto.getId());
        return item;
    }).collect(Collectors.toList());
    setmealDishService.saveBatch(dishes);
}
```
需要添加 imports: `import com.reggie.entity.SetmealDish;`

- [x] **Step 3: SetmealController 新增端点**

添加到 `list` 方法之前：
```java
@GetMapping("/{id}")
public R<SetmealDto> get(@PathVariable Long id) {
    SetmealDto setmealDto = setmealService.getByIdWithDish(id);
    return R.success(setmealDto);
}

@PutMapping
public R<String> update(@RequestBody SetmealDto setmealDto) {
    setmealService.updateWithDish(setmealDto);
    return R.success("修改套餐成功");
}
```

- [x] **Step 4: schema.sql 新增 setmeal 和 setmeal_dish 表**

```sql
DROP TABLE IF EXISTS setmeal;
CREATE TABLE setmeal (
  id bigint(20) NOT NULL,
  category_id bigint(20) DEFAULT NULL,
  name varchar(64) COLLATE utf8_bin DEFAULT NULL,
  price decimal(10,2) DEFAULT NULL,
  status int(11) DEFAULT '1',
  code varchar(32) COLLATE utf8_bin DEFAULT NULL,
  description varchar(255) COLLATE utf8_bin DEFAULT NULL,
  image varchar(255) COLLATE utf8_bin DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  is_deleted int(11) DEFAULT '0',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

DROP TABLE IF EXISTS setmeal_dish;
CREATE TABLE setmeal_dish (
  id bigint(20) NOT NULL,
  setmeal_id bigint(20) DEFAULT NULL,
  dish_id bigint(20) DEFAULT NULL,
  name varchar(64) COLLATE utf8_bin DEFAULT NULL,
  price decimal(10,2) DEFAULT NULL,
  copies int(11) DEFAULT NULL,
  sort int(11) DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  is_deleted int(11) DEFAULT '0',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
```

- [x] **Step 5: 编写 SetmealControllerTest**

创建 `src/test/java/com/reggie/controller/SetmealControllerTest.java`：
内容包含测试：查询套餐详情、修改套餐（含关联菜品变更）

- [x] **Step 6: 运行测试验证**

Run: `mvn test -Dtest=SetmealControllerTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS

---
### Task 3: 套餐起售/停售功能

**Files:**
- Modify: `src/main/java/com/reggie/service/SetmealService.java` — 新增 `updateStatus(Integer status, List<Long> ids)`
- Modify: `src/main/java/com/reggie/service/impl/SetmealServiceImpl.java` — 实现
- Modify: `src/main/java/com/reggie/controller/SetmealController.java` — 新增 `POST /setmeal/status/{status}`

- [x] **Step 1: SetmealService 新增接口**

```java
public void updateStatus(Integer status, List<Long> ids);
```

- [x] **Step 2: SetmealServiceImpl 实现**

```java
@Override
@Transactional
public void updateStatus(Integer status, List<Long> ids) {
    LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
    updateWrapper.in(Setmeal::getId, ids);
    updateWrapper.set(Setmeal::getStatus, status);
    this.update(updateWrapper);
}
```

- [x] **Step 3: SetmealController 新增端点**

```java
@PostMapping("/status/{status}")
public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
    setmealService.updateStatus(status, ids);
    return R.success("操作成功");
}
```

- [x] **Step 4: 追加测试到 SetmealControllerTest**

测试起售/停售端点。

- [x] **Step 5: 运行测试**

Run: `mvn test -Dtest=SetmealControllerTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS

---
### Task 4: 订单管理后台功能

**Files:**
- Modify: `src/main/java/com/reggie/service/OrderService.java` — 新增 `pageWithDetails`, `updateStatus`
- Modify: `src/main/java/com/reggie/service/impl/OrderServiceImpl.java` — 实现
- Modify: `src/main/java/com/reggie/controller/OrderController.java` — 新增 `GET /order/page`, `PUT /order`
- Modify: `src/main/java/com/reggie/controller/OrderDetailController.java` — 新增 `GET /orderDetail/{id}`
- Test: `src/test/java/com/reggie/controller/OrderControllerTest.java`

**Frontend expectations:**
- `GET /order/page?page=1&pageSize=10&number=XXX&beginTime=XXX&endTime=XXX` → `{records: [{id, number, status, userId, amount, orderTime, ...}], total, pages}`
- `PUT /order` body: `{status: 3, id: 123}` → `{code: 1, msg: "操作成功"}`
- `GET /orderDetail/{id}` → `{code: 1, data: {id, orderId, name, number, amount, ...}}`

- [x] **Step 1: schema.sql 新增 orders, order_detail, dish_flavor, category, employee, address_book, user, shopping_cart 表**

需要为所有表创建测试 schema 以便运行集成测试。

- [x] **Step 2: OrderService 新增接口**

```java
public Page orderPage(int page, int pageSize, String number, String beginTime, String endTime);
public void updateStatus(Integer status, Long id);
```

- [x] **Step 3: OrderServiceImpl 实现**

`orderPage` 方法：支持按订单号和下单时间范围筛选
`updateStatus` 方法：更新订单状态

- [x] **Step 4: OrderController 新增端点**

```java
@GetMapping("/page")
public R<Page> page(int page, int pageSize, String number, String beginTime, String endTime) {
    Page pageInfo = orderService.orderPage(page, pageSize, number, beginTime, endTime);
    return R.success(pageInfo);
}

@PutMapping
public R<String> updateStatus(@RequestBody Orders orders) {
    orderService.updateStatus(orders.getStatus(), orders.getId());
    return R.success("操作成功");
}
```

- [x] **Step 5: OrderDetailController 新增端点**

```java
@GetMapping("/{id}")
public R<OrderDetail> get(@PathVariable Long id) {
    OrderDetail orderDetail = orderDetailService.getById(id);
    if (orderDetail != null) {
        return R.success(orderDetail);
    }
    return R.error("没有找到该对象");
}
```

- [x] **Step 6: 编写测试**

- [x] **Step 7: 运行测试**

---
### Task 5: 移动端退出登录

**Files:**
- Modify: `src/main/java/com/reggie/controller/UserController.java` — 新增 `POST /user/loginout`

- [x] **Step 1: UserController 新增端点**

```java
@PostMapping("/loginout")
public R<String> loginout(HttpSession session) {
    session.removeAttribute("user");
    return R.success("退出成功");
}
```

---
### Task 6: 移动端地址管理（修改/删除/最近地址）

**Files:**
- Modify: `src/main/java/com/reggie/controller/AddressBookController.java` — 新增 `PUT`, `DELETE`, `GET /lastUpdate`
- Test: `src/test/java/com/reggie/controller/AddressBookControllerTest.java`

- [x] **Step 1: AddressBookController 新增端点**

添加到 `save` 之后：
```java
@PutMapping
public R<AddressBook> update(@RequestBody AddressBook addressBook) {
    addressBookService.updateById(addressBook);
    return R.success(addressBook);
}

@DeleteMapping
public R<String> delete(@RequestParam List<Long> ids) {
    addressBookService.removeByIds(ids);
    return R.success("删除成功");
}

@GetMapping("/lastUpdate")
public R<AddressBook> lastUpdate() {
    LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
    queryWrapper.orderByDesc(AddressBook::getUpdateTime);
    queryWrapper.last("LIMIT 1");
    AddressBook addressBook = addressBookService.getOne(queryWrapper);
    if (addressBook != null) {
        return R.success(addressBook);
    }
    return R.error("没有找到该对象");
}
```

- [x] **Step 2: 编写测试并运行**

---
### Task 7: 移动端历史订单 + 再来一单

**Files:**
- Modify: `src/main/java/com/reggie/service/OrderService.java` — 新增 `userPage`, `list`, `again`
- Modify: `src/main/java/com/reggie/service/impl/OrderServiceImpl.java` — 实现
- Modify: `src/main/java/com/reggie/controller/OrderController.java` — 新增 `GET /order/userPage`, `GET /order/list`, `POST /order/again`

- [x] **Step 1: OrderService 新增接口**

```java
public Page userPage(int page, int pageSize);
public List<Orders> list();
public void again(Long orderId);
```

- [x] **Step 2: OrderServiceImpl 实现**

`userPage` 方法：按当前用户 ID 查询订单，按时间降序
`list` 方法：按当前用户 ID 查询所有订单
`again` 方法：根据原订单 ID 查询订单详情，将菜品/套餐重新添加到购物车

```java
@Override
public Page userPage(int page, int pageSize) {
    Page<Orders> pageInfo = new Page<>(page, pageSize);
    LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
    queryWrapper.orderByDesc(Orders::getOrderTime);
    this.page(pageInfo, queryWrapper);
    return pageInfo;
}

@Override
public List<Orders> list() {
    LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
    queryWrapper.orderByDesc(Orders::getOrderTime);
    return this.list(queryWrapper);
}

@Override
@Transactional
public void again(Long orderId) {
    Long userId = BaseContext.getCurrentId();

    LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(OrderDetail::getOrderId, orderId);
    List<OrderDetail> orderDetails = orderDetailService.list(queryWrapper);

    List<ShoppingCart> shoppingCarts = orderDetails.stream().map((item) -> {
        ShoppingCart cart = new ShoppingCart();
        cart.setName(item.getName());
        cart.setImage(item.getImage());
        cart.setUserId(userId);
        cart.setDishId(item.getDishId());
        cart.setSetmealId(item.getSetmealId());
        cart.setDishFlavor(item.getDishFlavor());
        cart.setNumber(item.getNumber());
        cart.setAmount(item.getAmount());
        cart.setCreateTime(LocalDateTime.now());
        return cart;
    }).collect(Collectors.toList());

    shoppingCartService.saveBatch(shoppingCarts);
}
```
需要添加：`import com.reggie.entity.ShoppingCart;`

- [x] **Step 3: OrderController 新增端点**

```java
@GetMapping("/userPage")
public R<Page> userPage(int page, int pageSize) {
    Page pageInfo = orderService.userPage(page, pageSize);
    return R.success(pageInfo);
}

@GetMapping("/list")
public R<List<Orders>> list() {
    List<Orders> list = orderService.list();
    return R.success(list);
}

@PostMapping("/again")
public R<String> again(@RequestBody Map<String, Long> map) {
    Long orderId = map.get("id");
    orderService.again(orderId);
    return R.success("再来一单成功");
}
```

- [x] **Step 4: 编写测试并运行**

---
### Task 8: 购物车减商品功能

**Files:**
- Modify: `src/main/java/com/reggie/controller/ShoppingCartController.java` — 新增 `POST /shoppingCart/sub`

- [x] **Step 1: ShoppingCartController 新增端点**

添加到 `clean` 方法之前：
```java
@PostMapping("/sub")
public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
    Long currentId = BaseContext.getCurrentId();

    LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(ShoppingCart::getUserId, currentId);

    if (shoppingCart.getDishId() != null) {
        queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
    } else {
        queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
    }

    ShoppingCart cartItem = shoppingCartService.getOne(queryWrapper);

    if (cartItem != null) {
        Integer number = cartItem.getNumber();
        if (number > 1) {
            cartItem.setNumber(number - 1);
            shoppingCartService.updateById(cartItem);
        } else {
            shoppingCartService.removeById(cartItem.getId());
        }
    }

    return R.success(cartItem);
}
```

- [x] **Step 2: 编写测试并运行**

---
### Task 9: 套餐详情（移动端）

**Files:**
- Modify: `src/main/java/com/reggie/controller/SetmealController.java` — 新增 `GET /setmeal/dish/{id}`
- Test: `src/test/java/com/reggie/controller/SetmealControllerTest.java`

- [x] **Step 1: SetmealController 新增端点**

```java
@GetMapping("/dish/{id}")
public R<List<SetmealDish>> dish(@PathVariable Long id) {
    LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(SetmealDish::getSetmealId, id);
    List<SetmealDish> list = setmealDishService.list(queryWrapper);
    return R.success(list);
}
```

- [x] **Step 2: 追加测试到 SetmealControllerTest**

---
### Task 10: 更新 LoginCheckFilter 白名单

**Files:**
- Modify: `src/main/java/com/reggie/filter/LoginCheckFilter.java`

- [x] **Step 1: 将新的公开端点加入白名单**

在 urls 数组末尾添加 `"/user/loginout"`：
```java
String[] urls = new String[]{
    "/employee/login",
    "/employee/logout",
    "/backend/**",
    "/front/**",
    "/common/**",
    "/user/sendMsg",
    "/user/login",
    "/user/loginout"
};
```

---
### Task 11: 全局测试运行验证

- [x] **Step 1: 创建完整 schema.sql**

确保所有测试用表都已包含。

- [x] **Step 2: 运行全部测试**

Run: `mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS, 所有测试通过
