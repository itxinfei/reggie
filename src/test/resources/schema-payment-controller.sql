-- Payment Controller test schema (MySQL compatible)
-- 只插入数据，不创建表（生产 schema 已存在）

DELETE FROM refund_record WHERE tenant_id = 1;
DELETE FROM payment_order WHERE tenant_id = 1;
DELETE FROM orders WHERE id IN (200, 201, 202);
DELETE FROM order_detail WHERE order_id IN (200, 201, 202);

-- 测试支付单需要的业务订单
INSERT INTO orders (id, number, status, user_id, address_book_id, order_time, amount, user_name, phone, address, consignee, dining_type, create_time, update_time, tenant_id, is_deleted)
VALUES
  (200, 'PAYC001', 1, 1, 1, CURRENT_TIMESTAMP, 99.99, '测试用户', '13800000010', '测试地址', '张三', 'OUTSIDE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0),
  (201, 'PAYC002', 1, 1, 1, CURRENT_TIMESTAMP, 50.00, '测试用户', '13800000011', '测试地址', '李四', 'OUTSIDE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0),
  (202, 'PAYC003', 1, 1, 1, CURRENT_TIMESTAMP, 200.00, '测试用户', '13800000012', '测试地址', '王五', 'OUTSIDE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0);