-- Payment Controller test schema (H2 / MySQL compatible)
-- H2 内存库为空库，测试脚本须自建模块表（原"只插入数据不建表"仅适用于直连生产库，已废弃）。

-- ==================== 业务订单表（定义与 schema.sql 一致） ====================
CREATE TABLE IF NOT EXISTS orders (
  id bigint NOT NULL COMMENT '主键',
  number varchar(50) NULL DEFAULT NULL COMMENT '订单号',
  status int NOT NULL DEFAULT 1 COMMENT '订单状态',
  user_id bigint NULL DEFAULT NULL COMMENT '用户id',
  address_book_id bigint NULL DEFAULT NULL COMMENT '地址id',
  order_time datetime NULL DEFAULT NULL COMMENT '下单时间',
  checkout_time datetime NULL DEFAULT NULL COMMENT '结账时间',
  pay_method int NULL DEFAULT NULL COMMENT '支付方式',
  amount decimal(10,2) NOT NULL COMMENT '实收金额',
  delivery_fee decimal(10,2) NULL DEFAULT NULL COMMENT '配送费（外卖单独立存储，堂食为0）',
  remark varchar(100) NULL DEFAULT NULL COMMENT '备注',
  expect_delivery_time varchar(20) NULL DEFAULT NULL COMMENT '预送达时间',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户名',
  phone varchar(255) NULL DEFAULT NULL COMMENT '手机号',
  address varchar(255) NULL DEFAULT NULL COMMENT '地址',
  consignee varchar(50) NULL DEFAULT NULL COMMENT '收货人',
  dining_type varchar(20) NULL DEFAULT 'OUTSIDE' COMMENT '用餐类型',
  table_id bigint NULL DEFAULT NULL COMMENT '堂食桌台ID',
  table_name varchar(32) NULL DEFAULT NULL COMMENT '堂食桌台名称',
  queue_id bigint NULL DEFAULT NULL COMMENT '排队记录ID',
  reservation_id bigint NULL DEFAULT NULL COMMENT '预约记录ID',
  customer_count int NULL DEFAULT NULL COMMENT '用餐人数',
  idempotency_key varchar(128) NULL DEFAULT NULL COMMENT '幂等键',
  stock_refunded int NULL DEFAULT 0 COMMENT '已退库存数量',
  used_coupon_id bigint NULL DEFAULT NULL COMMENT '优惠券ID',
  platform_type varchar(32) NULL DEFAULT NULL COMMENT '平台来源',
  platform_order_id varchar(128) NULL DEFAULT NULL COMMENT '平台订单号',
  platform_shop_id varchar(128) NULL DEFAULT NULL COMMENT '平台门店ID',
  platform_raw longtext NULL COMMENT '平台原始订单JSON',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  version int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  master_order_id bigint NULL DEFAULT NULL COMMENT '父订单ID（AA分账时指向主订单）',
  split_count int NULL DEFAULT NULL COMMENT '分账份数（AA分账记录拆分数量）',
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_order_user ON orders(user_id, order_time);
CREATE INDEX IF NOT EXISTS idx_order_number ON orders(number);
CREATE INDEX IF NOT EXISTS idx_order_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_tenant ON orders(tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_platform ON orders(tenant_id, platform_type, platform_order_id);

-- ==================== 订单明细表 ====================
CREATE TABLE IF NOT EXISTS order_detail (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(50) NOT NULL COMMENT '名称',
  order_id bigint NOT NULL COMMENT '订单id',
  dish_id bigint NULL DEFAULT NULL COMMENT '菜品id',
  setmeal_id bigint NULL DEFAULT NULL COMMENT '套餐id',
  dish_flavor varchar(50) NULL DEFAULT NULL COMMENT '口味',
  number int NOT NULL DEFAULT 1 COMMENT '数量',
  amount decimal(10,2) NOT NULL COMMENT '单价',
  remark varchar(255) NULL DEFAULT NULL COMMENT '订单明细备注',
  image varchar(255) NULL DEFAULT NULL COMMENT '图片',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_order_detail_order ON order_detail(order_id);

-- ==================== 支付订单表 ====================
CREATE TABLE IF NOT EXISTS payment_order (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id bigint NOT NULL COMMENT '业务订单id',
  tenant_id bigint DEFAULT NULL COMMENT '租户id',
  trade_no varchar(64) NOT NULL COMMENT '系统交易号',
  channel_trade_no varchar(128) DEFAULT NULL COMMENT '通道交易号',
  channel varchar(20) NOT NULL COMMENT '支付通道 ALIPAY/WECHAT/UNIONPAY',
  amount decimal(10,2) NOT NULL COMMENT '金额',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL/REFUND',
  paid_time datetime DEFAULT NULL COMMENT '支付时间',
  notify_time datetime DEFAULT NULL COMMENT '回调时间',
  created_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT NULL,
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  version int NOT NULL DEFAULT 1 COMMENT '版本号',
  create_user bigint DEFAULT NULL COMMENT '创建人ID',
  update_user bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_trade_no ON payment_order(trade_no);
CREATE INDEX IF NOT EXISTS idx_payment_order ON payment_order(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_tenant ON payment_order(tenant_id);
CREATE INDEX IF NOT EXISTS idx_channel_trade ON payment_order(channel_trade_no);

-- ==================== 退款记录表 ====================
CREATE TABLE IF NOT EXISTS refund_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  payment_order_id bigint NOT NULL COMMENT '支付订单id',
  order_id bigint DEFAULT NULL COMMENT '业务订单ID',
  tenant_id bigint DEFAULT NULL COMMENT '租户ID',
  refund_no varchar(64) NOT NULL COMMENT '退款单号',
  amount decimal(10,2) NOT NULL COMMENT '退款金额',
  reason varchar(255) DEFAULT NULL COMMENT '退款原因',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL',
  refund_type int DEFAULT NULL COMMENT '售后类型：1=整单退款 2=部分退款',
  apply_user_id bigint DEFAULT NULL COMMENT '申请人ID',
  created_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  version int NOT NULL DEFAULT 1 COMMENT '版本号',
  create_user bigint DEFAULT NULL COMMENT '创建人ID',
  update_time datetime NOT NULL COMMENT '更新时间',
  update_user bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_refund_no ON refund_record(refund_no);
CREATE INDEX IF NOT EXISTS idx_payment_refund ON refund_record(payment_order_id);
CREATE INDEX IF NOT EXISTS idx_refund_tenant ON refund_record(tenant_id);

-- 清理测试残留数据
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
