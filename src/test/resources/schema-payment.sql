-- Payment module test data (MySQL compatible)
-- 只插入数据，不创建表（生产 schema 已存在）

DELETE FROM refund_record WHERE tenant_id = 1;
DELETE FROM payment_order WHERE tenant_id = 1;
DELETE FROM orders WHERE id IN (100, 101, 102);
DELETE FROM order_detail WHERE order_id IN (100, 101, 102);

-- 测试支付单需要的业务订单（orderId=100/101/102）
INSERT INTO orders (id, number, status, user_id, address_book_id, order_time, amount, user_name, phone, address, consignee, dining_type, create_time, update_time, tenant_id, is_deleted)
VALUES
  (100, 'PAY001', 1, 1, 1, CURRENT_TIMESTAMP, 99.99, '测试用户', '13800000001', '测试地址', '张三', 'OUTSIDE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0),
  (101, 'PAY002', 1, 1, 1, CURRENT_TIMESTAMP, 50.00, '测试用户', '13800000002', '测试地址', '李四', 'OUTSIDE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0),
  (102, 'PAY003', 1, 1, 1, CURRENT_TIMESTAMP, 200.00, '测试用户', '13800000003', '测试地址', '王五', 'OUTSIDE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0);
