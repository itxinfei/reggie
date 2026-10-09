-- 购物车测试基础数据（MySQL，幂等）
-- schema.sql 使用 CREATE TABLE IF NOT EXISTS，连开发库时表与存量数据已存在；
-- 这里统一用 INSERT IGNORE，固定 id=990001、tenant_id=999，重复执行不报错、不覆盖开发数据。
-- 注意：dish / setmeal / category 的 category_id、create_user、update_user、create_time、update_time、status 多为 NOT NULL，须显式提供。

-- 菜品分类（id=990001）
INSERT IGNORE INTO category (id, type, name, sort, create_time, update_time, create_user, update_user, is_deleted, tenant_id)
VALUES (990001, 1, '测试分类', 1, NOW(), NOW(), 990001, 990001, 0, 999);

-- 菜品（id=990001，category_id=990001）
INSERT IGNORE INTO dish (id, category_id, name, price, code, image, description, status, sort, create_time, update_time, create_user, update_user, is_deleted, tenant_id, stock_qty, min_stock)
VALUES (990001, 990001, '测试菜品', 10.00, 'T990001', 'test.jpg', '测试菜品描述', 1, 1, NOW(), NOW(), 990001, 990001, 0, 999, 100, 0);

-- 套餐（id=990001，category_id=990001）
INSERT IGNORE INTO setmeal (id, category_id, name, price, code, image, description, status, create_time, update_time, create_user, update_user, is_deleted, tenant_id)
VALUES (990001, 990001, '测试套餐', 50.00, 'S990001', 'setmeal.jpg', '测试套餐描述', 1, NOW(), NOW(), 990001, 990001, 0, 999);
