-- Printer module test schema (MySQL compatible)
-- 打印终端 / 打印任务：代理端表，测试库无生产表时自建
CREATE TABLE IF NOT EXISTS print_terminal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    store_code VARCHAR(50) NOT NULL DEFAULT '',
    terminal_code VARCHAR(64) NOT NULL DEFAULT '',
    token VARCHAR(64) NOT NULL DEFAULT '',
    name VARCHAR(100) NOT NULL DEFAULT '',
    printer_name VARCHAR(200) NOT NULL DEFAULT '',
    paper_size VARCHAR(20) NOT NULL DEFAULT '80mm',
    print_types VARCHAR(50) NOT NULL DEFAULT 'BILL',
    status TINYINT NOT NULL DEFAULT 0,
    last_heartbeat DATETIME DEFAULT NULL,
    client_version VARCHAR(30) NOT NULL DEFAULT '',
    created_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_print_terminal_code (terminal_code),
    KEY idx_print_terminal_tenant (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS print_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    store_code VARCHAR(50) NOT NULL DEFAULT '',
    order_id BIGINT DEFAULT NULL,
    task_type VARCHAR(20) NOT NULL DEFAULT 'BILL',
    content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    terminal_id BIGINT DEFAULT NULL,
    terminal_code VARCHAR(64) NOT NULL DEFAULT '',
    error_msg VARCHAR(500) NOT NULL DEFAULT '',
    retry_count INT NOT NULL DEFAULT 0,
    created_time DATETIME DEFAULT NULL,
    pulled_time DATETIME DEFAULT NULL,
    done_time DATETIME DEFAULT NULL,
    KEY idx_print_task_status (status),
    KEY idx_print_task_order (order_id),
    KEY idx_print_task_terminal (terminal_id, status)
);

-- ==================== 打印日志表 ====================
CREATE TABLE IF NOT EXISTS printer_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id bigint DEFAULT NULL COMMENT '订单id',
  print_type varchar(20) NOT NULL COMMENT '打印类型',
  printer_id bigint DEFAULT NULL COMMENT '打印机id',
  content text COMMENT '打印内容',
  status int DEFAULT 0 COMMENT '状态 0失败 1成功',
  error_msg varchar(255) DEFAULT NULL COMMENT '错误信息',
  created_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  create_user bigint DEFAULT NULL COMMENT '创建人ID',
  update_time datetime NOT NULL COMMENT '更新时间',
  update_user bigint DEFAULT NULL COMMENT '修改人ID',
  tenant_id bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_printer_log_order ON printer_log(order_id);
CREATE INDEX IF NOT EXISTS idx_printer_log_printer ON printer_log(printer_id);
CREATE INDEX IF NOT EXISTS idx_printer_log_tenant ON printer_log(tenant_id);

-- ==================== 打印模板表 ====================
CREATE TABLE IF NOT EXISTS printer_template (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(100) NOT NULL COMMENT '模板名称',
  type varchar(50) NOT NULL COMMENT '模板类型',
  content text NOT NULL COMMENT '模板内容',
  created_time datetime DEFAULT NULL COMMENT '创建时间',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);

-- ==================== 打印机配置表 ====================
CREATE TABLE IF NOT EXISTS printer_config (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NOT NULL COMMENT '租户id',
  store_id bigint DEFAULT NULL COMMENT '门店id',
  name varchar(50) NOT NULL COMMENT '打印机名称',
  type varchar(20) NOT NULL COMMENT '连接类型 USB/TCP/CLOUD/BLUETOOTH',
  brand varchar(20) DEFAULT NULL COMMENT '品牌 佳博/芯烨/商米',
  device_id varchar(100) DEFAULT NULL COMMENT '设备标识 MAC/SN',
  system_printer_name varchar(200) DEFAULT NULL COMMENT '系统打印机名称（Windows下为驱动名称）',
  ip_address varchar(15) DEFAULT NULL COMMENT 'IP地址',
  port int DEFAULT NULL COMMENT '端口',
  paper_size varchar(10) DEFAULT '58mm' COMMENT '纸张规格 58mm/80mm',
  print_types varchar(100) DEFAULT NULL COMMENT '打印类型',
  status int DEFAULT 1 COMMENT '状态 0禁用 1启用',
  sort int DEFAULT 0 COMMENT '排序',
  created_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT NULL,
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  create_user bigint DEFAULT NULL COMMENT '创建人ID',
  update_user bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_printer_config_tenant ON printer_config(tenant_id);

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

-- 清理并插入数据（生产 schema 已存在）
DELETE FROM print_task WHERE tenant_id = 1;
DELETE FROM print_terminal WHERE tenant_id = 1;
DELETE FROM printer_log WHERE tenant_id = 1;
DELETE FROM printer_template WHERE 1=1;
DELETE FROM printer_config WHERE tenant_id = 1;
