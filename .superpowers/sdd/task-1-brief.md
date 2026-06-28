### Task 1: 菜品起售/停售功能

**Files:**
- Modify: `src/main/java/com/reggie/service/DishService.java`
- Modify: `src/main/java/com/reggie/service/impl/DishServiceImpl.java`
- Modify: `src/main/java/com/reggie/controller/DishController.java`
- Modify: `pom.xml`
- Create: `src/test/resources/schema.sql`
- Create: `src/test/resources/application-test.yml`
- Create: `src/test/java/com/reggie/controller/DishControllerTest.java`

**Step 1: DishService 新增接口方法**
添加到 `DishService.java`：
```java
public void updateStatus(Integer status, List<Long> ids);
```

**Step 2: DishServiceImpl 实现**
添加到 `DishServiceImpl.java`，注意需要 import `com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper`：
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

**Step 3: DishController 新增端点**
添加到 `DishController.java` `update` 方法之后：
```java
@PostMapping("/status/{status}")
public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
    dishService.updateStatus(status, ids);
    return R.success("操作成功");
}
```

**Step 4: pom.xml 添加 H2 依赖**
添加到 `<dependencies>` 中（在 `spring-boot-starter-test` 附近）：
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**Step 5: 创建测试 schema.sql**
创建 `src/test/resources/schema.sql`，包含以下表定义：
```sql
DROP TABLE IF EXISTS dish;
DROP TABLE IF EXISTS category;

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

CREATE TABLE category (
  id bigint(20) NOT NULL,
  type int(11) DEFAULT NULL,
  name varchar(64) COLLATE utf8_bin DEFAULT NULL,
  sort int(11) DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint(20) DEFAULT NULL,
  update_user bigint(20) DEFAULT NULL,
  is_deleted int(11) DEFAULT '0',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
```

**Step 6: 创建 application-test.yml**
创建 `src/test/resources/application-test.yml`：
```yaml
server:
  port: 8080
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

**Step 7: 创建 DishControllerTest**
创建 `src/test/java/com/reggie/controller/DishControllerTest.java`：
```java
package com.reggie.controller;

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
                .sessionAttr("employee", 1L)
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
                .sessionAttr("employee", 1L)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assert dishService.getById(1L).getStatus() == 0;
        assert dishService.getById(2L).getStatus() == 0;
    }
}
```

**Step 8: 运行测试验证通过**
Run: `mvn test -Dtest=DishControllerTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS
