# Task 8 Report: 购物车减商品

## Status
✅ Completed

## Changes
- **ShoppingCartService.java** — Added `sub(ShoppingCart)` method signature
- **ShoppingCartServiceImpl.java** — Implemented `sub()`: queries by userId+dishId/setmealId, decrements quantity if >1, removes item if quantity == 1
- **ShoppingCartController.java** — Added `POST /shoppingCart/sub` endpoint
- **schema.sql** — Added `shopping_cart` table DDL for H2 test schema
- **ShoppingCartControllerTest.java** — Created with 2 tests: `testSubReduceQuantity` (3→2) and `testSubRemoveWhenOne` (remove item)

## Test Results
```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Concerns
- Pre-existing compilation error in `OrderServiceImpl` (missing `again` method) appeared during initial build due to stale artifacts; resolved with `mvn clean compile`
