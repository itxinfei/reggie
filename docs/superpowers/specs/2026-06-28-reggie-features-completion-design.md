# 瑞吉外卖功能补全设计

## 背景
瑞吉外卖项目为一个餐饮管理系统的教学 Demo，包含后端管理（ElementUI）和移动端（Vant UI）。本项目旨在补全所有未完成的功能，使其达到可用状态，并为后续 SaaS 多租户改造奠定基础。

## 需补全功能

### 后端管理
1. **菜品起售/停售** — `POST /dish/status/{status}`，支持批量操作
2. **套餐修改** — `PUT /setmeal` + `GET /setmeal/{id}`，含关联菜品数据
3. **套餐起售/停售** — `POST /setmeal/status/{status}`，支持批量操作
4. **订单管理** — `GET /order/page`（分页+按单号/时间筛选）+ `PUT /order`（状态变更：派送/完成）
5. **订单详情查看** — `GET /orderDetail/{id}`

### 移动端
6. **退出登录** — `POST /user/loginout`
7. **历史订单分页** — `GET /order/userPage`
8. **再来一单** — `POST /order/again`，复制原订单菜品到购物车
9. **地址修改** — `PUT /addressBook`
10. **地址删除** — `DELETE /addressBook?ids=`
11. **获取最近更新地址** — `GET /addressBook/lastUpdate`
12. **购物车减商品** — `POST /shoppingCart/sub`
13. **套餐详情** — `GET /setmeal/dish/{id}`

## 测试方案
- 使用 H2 内存数据库 + schema.sql 初始化表结构
- Spring Boot `@WebMvcTest` + `@AutoConfigureMockMvc` 编写 Controller 测试
- 测试覆盖所有新增和已有核心接口

## 实施顺序
1. Service 层方法（批量状态变更、套餐更新、再来一单等）
2. Controller 层端点
3. 新增路径到 LoginCheckFilter 白名单
4. H2 测试配置 + schema.sql
5. Controller 测试
