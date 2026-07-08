-- ===================================================
-- 多租户数据库迁移脚本（安全版 - 仅添加不存在的列）
-- 执行时间: 2026-07-08
-- 说明: 基于实际数据库结构，orders 表已添加 tenant_id
-- ===================================================

USE reggie;

-- ===================================================
-- 1. address_book 表（当前19列，无 tenant_id）
-- ===================================================
ALTER TABLE `address_book`
ADD COLUMN `tenant_id` bigint DEFAULT NULL COMMENT '租户id' AFTER `is_deleted`,
ADD INDEX `idx_tenant` (`tenant_id`);

UPDATE `address_book` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- ===================================================
-- 2. user 表（当前7列，无 tenant_id）
-- ===================================================
ALTER TABLE `user`
ADD COLUMN `tenant_id` bigint DEFAULT NULL COMMENT '租户id' AFTER `status`,
ADD INDEX `idx_tenant` (`tenant_id`);

UPDATE `user` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- ===================================================
-- 3. order_detail 表（当前9列，无 tenant_id）
-- ===================================================
ALTER TABLE `order_detail`
ADD COLUMN `tenant_id` bigint DEFAULT NULL COMMENT '租户id' AFTER `amount`,
ADD INDEX `idx_tenant` (`tenant_id`);

-- 从关联的订单继承 tenant_id
UPDATE `order_detail` od
INNER JOIN `orders` o ON od.order_id = o.id
SET od.tenant_id = o.tenant_id
WHERE od.tenant_id IS NULL;

-- ===================================================
-- 4. shopping_cart 表（当前10列，无 tenant_id）
-- ===================================================
ALTER TABLE `shopping_cart`
ADD COLUMN `tenant_id` bigint DEFAULT NULL COMMENT '租户id' AFTER `create_time`,
ADD INDEX `idx_tenant` (`tenant_id`);

-- 从关联的用户继承 tenant_id
UPDATE `shopping_cart` sc
INNER JOIN `user` u ON sc.user_id = u.id
SET sc.tenant_id = u.tenant_id
WHERE sc.tenant_id IS NULL;

-- ===================================================
-- 5. purchase_order_detail 表（当前8列，无 tenant_id）
-- ===================================================
ALTER TABLE `purchase_order_detail`
ADD COLUMN `tenant_id` bigint DEFAULT NULL COMMENT '租户id' AFTER `remark`,
ADD INDEX `idx_tenant` (`tenant_id`);

-- 从关联的采购单继承 tenant_id
UPDATE `purchase_order_detail` pod
INNER JOIN `purchase_order` po ON pod.purchase_order_id = po.id
SET pod.tenant_id = po.tenant_id
WHERE pod.tenant_id IS NULL;

-- ===================================================
-- 6. printer_log 表（当前8列，无 tenant_id）
-- ===================================================
ALTER TABLE `printer_log`
ADD COLUMN `tenant_id` bigint DEFAULT NULL COMMENT '租户id' AFTER `created_time`,
ADD INDEX `idx_tenant` (`tenant_id`);

-- 从关联的订单继承 tenant_id
UPDATE `printer_log` pl
INNER JOIN `orders` o ON pl.order_id = o.id
SET pl.tenant_id = o.tenant_id
WHERE pl.tenant_id IS NULL;

-- ===================================================
-- 7. employee 表（当前13列，新增 role 字段）
-- ===================================================
ALTER TABLE `employee`
ADD COLUMN `role` int NOT NULL DEFAULT 2 COMMENT '角色 1:超级管理员 2:普通员工' AFTER `status`;

-- 将第一个管理员设为超级管理员
UPDATE `employee` SET `role` = 1 WHERE `id` = 1;

-- ===================================================
-- 验证修复结果
-- ===================================================
SELECT 'address_book' AS table_name, COUNT(*) AS total, SUM(tenant_id IS NULL) AS missing_tenant FROM address_book
UNION ALL
SELECT 'user', COUNT(*), SUM(tenant_id IS NULL) FROM user
UNION ALL
SELECT 'order_detail', COUNT(*), SUM(tenant_id IS NULL) FROM order_detail
UNION ALL
SELECT 'shopping_cart', COUNT(*), SUM(tenant_id IS NULL) FROM shopping_cart
UNION ALL
SELECT 'purchase_order_detail', COUNT(*), SUM(tenant_id IS NULL) FROM purchase_order_detail
UNION ALL
SELECT 'printer_log', COUNT(*), SUM(tenant_id IS NULL) FROM printer_log
UNION ALL
SELECT 'employee_role', COUNT(*), SUM(role IS NULL) FROM employee;

-- 完成提示
SELECT '多租户数据库修复完成！' AS message;
