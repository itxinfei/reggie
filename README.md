# 瑞吉外卖 (Reggie Takeout)

餐饮企业一站式外卖管理系统，含管理后台和移动端 C 端应用。**Java 1.8 + Spring Boot 2.4.5 + MyBatis Plus 3.4.2**。

## 快速开始

```bash
# 启动（H2 内存数据库，无需安装 MySQL）
mvn spring-boot:run

# 运行全部测试（17 个用例）
mvn test -DfailIfNoTests=false
```

| 入口 | 地址 |
|------|------|
| 管理后台 | http://localhost:8080/backend/index.html |
| 移动端 | http://localhost:8080/front/index.html |

## 技术栈

| 层 | 技术 |
|---|------|
| 后端框架 | Spring Boot 2.4.5, Spring MVC, Spring Session |
| ORM | MyBatis Plus 3.4.2（多租户行级隔离） |
| 数据库 | MySQL 8.0（生产） / H2（测试） |
| 前端(后台) | Vue.js 2 + ElementUI（静态资源） |
| 前端(移动) | Vant UI（静态资源） |
| 构建 | Maven, Git |
| 测试 | JUnit 5, MockMvc, H2 |

## 项目结构

```
src/main/java/com/reggie
├── common/          # 统一响应 R、全局异常、上下文 BaseContext
├── config/          # MyBatis Plus 多租户拦截器、WebMvc 配置
├── controller/      # 10 个 REST Controller
├── dto/             # DishDto, SetmealDto, OrderDto
├── entity/          # 12 个实体类
├── filter/          # LoginCheckFilter（认证 + 租户上下文）
├── mapper/          # MyBatis Plus Mapper 接口
├── service/         # 业务接口 + 实现
└── utils/           # SMSUtils, ValidateCodeUtils

src/main/resources
├── backend/         # 管理后台静态页面（ElementUI）
├── front/           # 移动端静态页面（Vant UI）
└── application.yml  # 主配置

src/test/java        # 6 个测试类，17 个用例
```

## 功能清单

### 管理后台
- 员工登录/退出、员工管理（CRUD + 禁用）
- 分类管理（菜品分类/套餐分类 CRUD）
- 菜品管理（CRUD + 起售/停售 + 口味管理）
- 套餐管理（CRUD + 起售/停售 + 套餐详情）
- 订单管理（分页查询 + 状态更新 + 订单明细）

### 移动端
- 手机号登录/退出
- 菜品浏览（按分类查询）
- 购物车（添加/减少/列表/清空）
- 下单 + 再来一单
- 历史订单查询
- 地址管理（CRUD + 默认地址）

### 多租户（SaaS）
- 共享数据库 + `tenant_id` 行级隔离
- `TenantLineInnerInterceptor` 自动注入
- 租户注册接口 `/tenant/register`
- 员工表手动租户过滤

## API 端点

| 模块 | 端点 | 说明 |
|------|------|------|
| 员工 | `POST /employee/login` `GET /employee/page` `POST /employee` `PUT /employee` `GET /employee/{id}` | 登录/分页/新增/修改/查询 |
| 分类 | `GET /category/page` `GET /category/list` `GET /category/{id}` `POST /category` `PUT /category` `DELETE /category/{id}` | 分页/列表/详情/新增/修改/删除 |
| 菜品 | `GET /dish/page` `GET /dish/list` `GET /dish/{id}` `POST /dish` `PUT /dish` `DELETE /dish` `POST /dish/status/{status}` | 分页/列表/详情/新增/修改/删除/启停售 |
| 套餐 | `GET /setmeal/page` `GET /setmeal/list` `GET /setmeal/{id}` `GET /setmeal/dish/{id}` `POST /setmeal` `PUT /setmeal` `DELETE /setmeal` `POST /setmeal/status/{status}` | 分页/列表/详情/套餐内菜品/新增/修改/删除/启停售 |
| 订单 | `POST /order/submit` `GET /order/page` `GET /order/list` `GET /order/userPage` `POST /order/again` `PUT /order` | 提交/管理端分页/用户列表/用户分页/再来一单/状态更新 |
| 购物车 | `GET /shoppingCart/list` `POST /shoppingCart/add` `POST /shoppingCart/sub` `DELETE /shoppingCart/clean` | 列表/添加/减少/清空 |
| 地址 | `GET /addressBook/list` `GET /addressBook/{id}` `GET /addressBook/lastUpdate` `GET /addressBook/default` `POST /addressBook` `PUT /addressBook` `PUT /addressBook/default` `DELETE /addressBook` | 列表/详情/最近/默认/新增/修改/设默认/删除 |
| 用户 | `POST /user/login` `POST /user/sendMsg` `POST /user/loginout` | 登录/验证码/退出 |
| 租户 | `POST /tenant/register` | 注册租户 |
| 通用 | `POST /common/upload` `GET /common/download` | 文件上传/下载 |

## 测试

```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
```

`@SpringBootTest` + `@AutoConfigureMockMvc` + H2 内存数据库，无需 Docker。

## 许可证

MIT
