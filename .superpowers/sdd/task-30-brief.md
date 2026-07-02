# 修复 L1: ShoppingCartController 日志优化

**Files:**
- Modify: `src/main/java/com/reggie/controller/ShoppingCartController.java`

## 问题描述

第39行打印整个 shoppingCart 对象，可能包含用户ID、菜品名称等非必要信息。

## 修复内容

### 第39行

**修改前：**
```java
log.info("购物车数据:{}",shoppingCart);
```

**修改后：**
```java
log.info("购物车数据：userId={}, dishId={}, dishName={}, number={}", 
    shoppingCart.getUserId(), 
    shoppingCart.getDishId() != null ? shoppingCart.getDishId() : shoppingCart.getSetmealId(),
    shoppingCart.getName(),
    shoppingCart.getNumber());
```

## 验收标准

- [ ] 替换 toString() 为字段级日志
- [ ] 打印关键信息（用户ID、菜品ID、名称、数量）
- [ ] 编译通过
- [ ] 所有现有测试通过（ShoppingCartControllerTest: 2 PASSED）
