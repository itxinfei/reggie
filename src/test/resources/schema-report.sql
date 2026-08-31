-- Report module test schema (H2 compatible)

DROP TABLE IF EXISTS order_detail;
DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
  id bigint NOT NULL COMMENT '主键',
  number varchar(50) NULL DEFAULT NULL COMMENT '订单号',
  status int NOT NULL DEFAULT 1 COMMENT '订单状态',
  user_id bigint NULL DEFAULT NULL COMMENT '用户id',
  address_book_id bigint NULL DEFAULT NULL COMMENT '地址id',
  order_time datetime NOT NULL COMMENT '下单时间',
  checkout_time datetime NULL DEFAULT NULL COMMENT '结账时间',
  pay_method int NULL DEFAULT NULL COMMENT '支付方式',
  amount decimal(10,2) NOT NULL COMMENT '实收金额',
  delivery_fee decimal(10,2) NULL DEFAULT NULL COMMENT '配送费（外卖单配送费，堂食为 null）',
  remark varchar(100) NULL DEFAULT NULL COMMENT '备注',
  expect_delivery_time varchar(20) NULL DEFAULT NULL COMMENT '预计送达时间',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户名',
  phone varchar(255) NULL DEFAULT NULL COMMENT '手机号',
  address varchar(255) NULL DEFAULT NULL COMMENT '地址',
  consignee varchar(50) NULL DEFAULT NULL COMMENT '收货人',
  dining_type varchar(20) NULL DEFAULT 'OUTSIDE' COMMENT '用餐类型',
  table_id bigint NULL DEFAULT NULL COMMENT '堂食桌台ID',
  table_name varchar(32) NULL DEFAULT NULL COMMENT '堂食桌台名称',
  queue_id bigint NULL DEFAULT NULL COMMENT '排队记录ID',
  reservation_id bigint NULL DEFAULT NULL COMMENT '预订记录ID',
  customer_count int NULL DEFAULT NULL COMMENT '用餐人数',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  idempotency_key varchar(128) NULL DEFAULT NULL COMMENT '幂等键',
  stock_refunded int NULL DEFAULT 0 COMMENT '已退库存数量',
  used_coupon_id bigint NULL DEFAULT NULL COMMENT '优惠券ID',
  platform_type varchar(32) NULL DEFAULT NULL COMMENT '平台来源',
  platform_order_id varchar(64) NULL DEFAULT NULL COMMENT '平台订单号',
  platform_shop_id varchar(64) NULL DEFAULT NULL COMMENT '平台门店ID',
  platform_raw text NULL DEFAULT NULL COMMENT '平台原始订单JSON',
  master_order_id bigint NULL DEFAULT NULL COMMENT '父订单ID（AA分账时指向主订单）',
  split_count int NULL DEFAULT NULL COMMENT '分账份数（AA分账时记录拆分数量）',
  version int NULL DEFAULT NULL COMMENT '乐观锁版本号',
  PRIMARY KEY (id)
);

CREATE TABLE order_detail (
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
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);
CREATE INDEX idx_order_detail_order ON order_detail(order_id);
