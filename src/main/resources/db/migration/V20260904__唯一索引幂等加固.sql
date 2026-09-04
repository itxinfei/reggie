-- 20260904 生产加固：资金/并发/幂等唯一索引补充
-- 说明：CREATE INDEX IF NOT EXISTS 在 MySQL 8.x 不存在，使用 DROP + ADD 方式需要手工执行；
--       为兼容重复执行，这里用存储过程判断列/索引是否存在后再创建。

-- 1. reconciliation_statement 对账单幂等：同一租户同一日期同一平台仅一条
DROP PROCEDURE IF EXISTS add_reconciliation_unique;
DELIMITER $$
CREATE PROCEDURE add_reconciliation_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE table_schema = DATABASE() AND table_name = 'reconciliation_statement'
          AND index_name = 'uk_statement_date_platform_tenant'
    ) THEN
        ALTER TABLE `reconciliation_statement`
            ADD UNIQUE KEY `uk_statement_date_platform_tenant` (`statement_date`, `platform`, `tenant_id`);
    END IF;
END$$
DELIMITER ;
CALL add_reconciliation_unique();
DROP PROCEDURE IF EXISTS add_reconciliation_unique;

-- 2. shopping_cart 购物车幂等：同一租户同一用户同一菜品/套餐同一口味仅一行（SQL 累加数量）
DROP PROCEDURE IF EXISTS add_shopping_cart_unique;
DELIMITER $$
CREATE PROCEDURE add_shopping_cart_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE table_schema = DATABASE() AND table_name = 'shopping_cart'
          AND index_name = 'uk_cart_tenant_user_dish'
    ) THEN
        ALTER TABLE `shopping_cart`
            ADD UNIQUE KEY `uk_cart_tenant_user_dish` (`tenant_id`, `user_id`, `dish_id`, `setmeal_id`, `dish_flavor`);
    END IF;
END$$
DELIMITER ;
CALL add_shopping_cart_unique();
DROP PROCEDURE IF EXISTS add_shopping_cart_unique;

-- 3. purchase_order.order_no / stock_check.check_no 单号唯一（并发幂等）
DROP PROCEDURE IF EXISTS add_purchase_order_no_unique;
DELIMITER $$
CREATE PROCEDURE add_purchase_order_no_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE table_schema = DATABASE() AND table_name = 'purchase_order'
          AND index_name = 'uk_order_no'
    ) THEN
        ALTER TABLE `purchase_order`
            ADD UNIQUE KEY `uk_order_no` (`order_no`);
    END IF;
END$$
DELIMITER ;
CALL add_purchase_order_no_unique();
DROP PROCEDURE IF EXISTS add_purchase_order_no_unique;

DROP PROCEDURE IF EXISTS add_stock_check_no_unique;
DELIMITER $$
CREATE PROCEDURE add_stock_check_no_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE table_schema = DATABASE() AND table_name = 'stock_check'
          AND index_name = 'uk_check_no'
    ) THEN
        ALTER TABLE `stock_check`
            ADD UNIQUE KEY `uk_check_no` (`check_no`);
    END IF;
END$$
DELIMITER ;
CALL add_stock_check_no_unique();
DROP PROCEDURE IF EXISTS add_stock_check_no_unique;

-- 5. cashier_record 收银幂等：同一订单仅一条收银记录（收银重复扣款第二道防线）
--    说明：V20260810 建表已含 uk_order_id(order_id) 唯一键，此段仅在存量库缺索引时补齐，
--    IF NOT EXISTS 保证重复执行幂等（索引已存在则跳过）。
DROP PROCEDURE IF EXISTS add_cashier_record_order_unique;
DELIMITER $$
CREATE PROCEDURE add_cashier_record_order_unique()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE table_schema = DATABASE() AND table_name = 'cashier_record'
          AND index_name IN ('uk_cashier_order', 'uk_order_id')
    ) THEN
        ALTER TABLE `cashier_record`
            ADD UNIQUE KEY `uk_cashier_order` (`order_id`);
    END IF;
END$$
DELIMITER ;
CALL add_cashier_record_order_unique();
DROP PROCEDURE IF EXISTS add_cashier_record_order_unique;
