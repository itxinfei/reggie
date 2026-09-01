-- ==================== 业务订单表（H2 内存库自建，定义与 schema.sql 一致） ====================
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

-- 清理平台订单残留数据（之前测试运行插入的订单），避免 @DirtiesContext 重启后数据残留导致去重误判
DELETE FROM order_detail WHERE order_id IN (SELECT id FROM orders WHERE platform_type IS NOT NULL);
DELETE FROM orders WHERE platform_type IS NOT NULL;

-- 外卖平台接入配置 测试库建表（H2 / MySQL 兼容）
DROP TABLE IF EXISTS platform_config;
CREATE TABLE platform_config (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  platform_type varchar(32) NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  platform_name varchar(128) NULL DEFAULT NULL COMMENT '平台展示名称',
  shop_id varchar(128) NULL DEFAULT NULL COMMENT '平台侧门店ID',
  app_key varchar(512) NULL DEFAULT NULL COMMENT '应用标识(加密)',
  app_secret varchar(512) NULL DEFAULT NULL COMMENT '应用密钥(加密)',
  access_token varchar(512) NULL DEFAULT NULL COMMENT '访问令牌(加密)',
  enabled int NOT NULL DEFAULT 1 COMMENT '昐吔 0停用 1吔',
  sync_scope int NOT NULL DEFAULT 1 COMMENT '同范围位标 1订单2商品4库存8营业状',
  remark varchar(500) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_platform_type_shop ON platform_config(platform_type, shop_id);
CREATE INDEX idx_platform_tenant ON platform_config(tenant_id);

-- ==================== 商品平台映射表 ====================
DROP TABLE IF EXISTS dish_platform_mapping;
CREATE TABLE dish_platform_mapping (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  dish_id bigint NOT NULL COMMENT '朳统菜品ID',
  platform_type varchar(32) NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  platform_shop_id varchar(128) NULL DEFAULT NULL COMMENT '平台侧门店ID',
  platform_dish_id varchar(128) NULL DEFAULT NULL COMMENT '平台菜品ID',
  platform_sku_id varchar(128) NULL DEFAULT NULL COMMENT '平台SKU ID',
  price decimal(10,2) NULL DEFAULT NULL COMMENT '平台价格',
  status int NOT NULL DEFAULT 1 COMMENT '状 0下架 1上架',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE INDEX idx_mapping_dish_platform (dish_id, platform_type, platform_dish_id)
);

-- ==================== 平台同步操作日志表 ====================
DROP TABLE IF EXISTS platform_sync_log;
CREATE TABLE platform_sync_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  platform_type varchar(32) NOT NULL COMMENT '平台类型',
  platform_order_id varchar(128) NULL DEFAULT NULL COMMENT '平台订单ID',
  local_order_id bigint NULL DEFAULT NULL COMMENT '朜订单ID',
  action varchar(32) NOT NULL COMMENT '动作 PULL/ACCEPT/REJECT',
  direction varchar(16) NOT NULL DEFAULT 'IN' COMMENT '方向 IN=拉单 OUT=回传',
  request_body text NULL COMMENT '请求内',
  response_body text NULL COMMENT '响应内',
  status int NOT NULL DEFAULT 0 COMMENT '结果 0=成功 1=失败',
  error_message varchar(512) NULL DEFAULT NULL COMMENT '错信息',
  retry_count int NOT NULL DEFAULT 0 COMMENT '重试次数',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_sync_log_platform_order ON platform_sync_log(platform_type, platform_order_id);
CREATE INDEX idx_sync_log_local_order ON platform_sync_log(local_order_id);
CREATE INDEX idx_sync_log_create_time ON platform_sync_log(create_time);

-- ==================== 平台对账任务表 ====================
DROP TABLE IF EXISTS platform_reconcile_task;
CREATE TABLE platform_reconcile_task (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  platform_type varchar(32) NOT NULL COMMENT '平台类型',
  reconcile_date date NOT NULL COMMENT '对账日期',
  begin_time datetime NOT NULL COMMENT '对账始时',
  end_time datetime NOT NULL COMMENT '对账结束时间',
  total_platform_count int NOT NULL DEFAULT 0 COMMENT '平台侧订单数',
  total_local_count int NOT NULL DEFAULT 0 COMMENT '本地订单数',
  match_count int NOT NULL DEFAULT 0 COMMENT '匹配成功',
  missing_local_count int NOT NULL DEFAULT 0 COMMENT '平台有本地无',
  missing_platform_count int NOT NULL DEFAULT 0 COMMENT '朜有平台无',
  status int NOT NULL DEFAULT 0 COMMENT '状 0=进 1=完成 2=失败',
  error_message varchar(512) NULL DEFAULT NULL COMMENT '错信息',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_reconcile_task_date_platform ON platform_reconcile_task(reconcile_date, platform_type, tenant_id);
