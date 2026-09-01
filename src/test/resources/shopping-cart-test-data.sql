-- 购物车测试基础数据（H2 / MySQL 兼容）
-- 配合 schema.sql（DROP+CREATE 重建核心表）后执行，保证 dish/setmeal 存在且可售。
-- 注意：dish.category_id / create_user / update_user 为 NOT NULL，须显式提供。

INSERT INTO dish (id, category_id, name, price, code, image, description, status, sort, create_time, update_time, create_user, update_user, is_deleted, tenant_id, stock_qty, min_stock)
VALUES (1, 1, '测试菜品', 10.00, 'T001', 'test.jpg', '测试菜品描述', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1, 100, 0);

INSERT INTO setmeal (id, category_id, name, price, code, image, description, status, create_time, update_time, create_user, update_user, is_deleted, tenant_id)
VALUES (1, 1, '测试套餐', 50.00, 'S001', 'setmeal.jpg', '测试套餐描述', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);
