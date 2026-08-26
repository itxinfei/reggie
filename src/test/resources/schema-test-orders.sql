-- Test orders data (MySQL compatible)
-- 只插入数据，不创建表（生产 schema 已存在）

DELETE FROM order_detail WHERE order_id IN (1, 2, 3);
DELETE FROM orders WHERE id IN (1, 2, 3);

INSERT INTO orders (id, number, status, user_id, address_book_id, order_time, checkout_time, pay_method, amount, remark, phone, address, user_name, consignee, dining_type, create_time, update_time, create_user, update_user, is_deleted, tenant_id)
VALUES (1, 'TEST001', 5, 1, 1, '2026-07-01 08:00:00', '2026-07-01 08:30:00', 1, 100.00, NULL, '13900139001', '测试地址', '测试用户', '测试用户', 'OUTSIDE', '2026-07-01 08:00:00', '2026-07-01 08:30:00', 1, 1, 0, 1);

INSERT INTO orders (id, number, status, user_id, address_book_id, order_time, checkout_time, pay_method, amount, remark, phone, address, user_name, consignee, dining_type, create_time, update_time, create_user, update_user, is_deleted, tenant_id)
VALUES (2, 'TEST002', 5, 1, 1, '2026-07-01 12:00:00', '2026-07-01 12:30:00', 2, 200.00, NULL, '13900139001', '测试地址', '测试用户', '测试用户', 'OUTSIDE', '2026-07-01 12:00:00', '2026-07-01 12:30:00', 1, 1, 0, 1);

INSERT INTO orders (id, number, status, user_id, address_book_id, order_time, checkout_time, pay_method, amount, remark, phone, address, user_name, consignee, dining_type, create_time, update_time, create_user, update_user, is_deleted, tenant_id)
VALUES (3, 'TEST003', 6, 1, 1, '2026-07-01 18:00:00', NULL, 1, 50.00, '测试取消', '13900139001', '测试地址', '测试用户', '测试用户', 'OUTSIDE', '2026-07-01 18:00:00', '2026-07-01 18:00:00', 1, 1, 0, 1);

INSERT INTO order_detail (id, name, order_id, dish_id, setmeal_id, dish_flavor, number, amount, image, tenant_id, create_time, update_time, create_user, update_user, is_deleted)
VALUES (1, '测试菜品1', 1, 1, NULL, NULL, 2, 50.00, 'test.jpg', 1, '2026-07-01 08:00:00', '2026-07-01 08:00:00', 1, 1, 0);

INSERT INTO order_detail (id, name, order_id, dish_id, setmeal_id, dish_flavor, number, amount, image, tenant_id, create_time, update_time, create_user, update_user, is_deleted)
VALUES (2, '测试菜品2', 2, 2, NULL, NULL, 1, 200.00, 'test2.jpg', 1, '2026-07-01 12:00:00', '2026-07-01 12:00:00', 1, 1, 0);
