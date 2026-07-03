DROP TABLE IF EXISTS coupon_user;
DROP TABLE IF EXISTS coupon_template;
DROP TABLE IF EXISTS recharge_record;
DROP TABLE IF EXISTS points_record;
DROP TABLE IF EXISTS member_level;
DROP TABLE IF EXISTS member;
DROP TABLE IF EXISTS stock_check_detail;
DROP TABLE IF EXISTS stock_check;
DROP TABLE IF EXISTS purchase_order_detail;
DROP TABLE IF EXISTS purchase_order;
DROP TABLE IF EXISTS stock_record;
DROP TABLE IF EXISTS material;
DROP TABLE IF EXISTS material_category;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS delivery_order;
DROP TABLE IF EXISTS dining_queue;
DROP TABLE IF EXISTS dining_reservation;
DROP TABLE IF EXISTS dining_table;
DROP TABLE IF EXISTS dining_area;
DROP TABLE IF EXISTS refund_record;
DROP TABLE IF EXISTS payment_order;
DROP TABLE IF EXISTS printer_log;
DROP TABLE IF EXISTS printer_config;
DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS order_detail;
DROP TABLE IF EXISTS shopping_cart;
DROP TABLE IF EXISTS address_book;
DROP TABLE IF EXISTS dish_flavor;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS setmeal_dish;
DROP TABLE IF EXISTS setmeal;
DROP TABLE IF EXISTS dish;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS tenant;
DROP TABLE IF EXISTS employee;

CREATE TABLE employee (
  id bigint NOT NULL,
  username varchar(64) DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  password varchar(64) DEFAULT NULL,
  password_type varchar(32) DEFAULT 'MD5',
  phone varchar(16) DEFAULT NULL,
  sex varchar(8) DEFAULT NULL,
  id_number varchar(32) DEFAULT NULL,
  status int DEFAULT '1',
  tenant_id bigint DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE tenant (
  id bigint NOT NULL,
  name varchar(64) DEFAULT NULL,
  phone varchar(16) DEFAULT NULL,
  address varchar(255) DEFAULT NULL,
  password_type varchar(20) DEFAULT 'MD5',
  status int DEFAULT '1',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE category (
  id bigint NOT NULL,
  tenant_id bigint DEFAULT NULL,
  type int DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  sort int DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  is_deleted int DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE dish (
  id bigint NOT NULL,
  tenant_id bigint DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  category_id bigint DEFAULT NULL,
  price decimal(10,2) DEFAULT NULL,
  code varchar(64) NOT NULL,
  image varchar(200) DEFAULT NULL,
  description varchar(400) DEFAULT NULL,
  status int DEFAULT '1',
  sort int DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  is_deleted int DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE dish_flavor (
  id bigint NOT NULL,
  tenant_id bigint DEFAULT NULL,
  dish_id bigint DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  value varchar(500) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  is_deleted int DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE setmeal (
  id bigint NOT NULL,
  tenant_id bigint DEFAULT NULL,
  category_id bigint DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  price decimal(10,2) DEFAULT NULL,
  status int DEFAULT '1',
  code varchar(32) DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  image varchar(255) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  is_deleted int DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE setmeal_dish (
  id bigint NOT NULL,
  tenant_id bigint DEFAULT NULL,
  setmeal_id varchar(32) DEFAULT NULL,
  dish_id varchar(32) DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  price decimal(10,2) DEFAULT NULL,
  copies int DEFAULT NULL,
  sort int DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  is_deleted int DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE orders (
  id bigint NOT NULL,
  number varchar(50) DEFAULT NULL,
  status int DEFAULT '2',
  user_id bigint DEFAULT NULL,
  address_book_id bigint DEFAULT NULL,
  order_time datetime DEFAULT NULL,
  checkout_time datetime DEFAULT NULL,
  pay_method int DEFAULT '1',
  amount decimal(10,2) DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  phone varchar(32) DEFAULT NULL,
  address varchar(255) DEFAULT NULL,
  user_name varchar(64) DEFAULT NULL,
  consignee varchar(32) DEFAULT NULL,
  table_id bigint DEFAULT NULL,
  dining_type varchar(20) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE order_detail (
  id bigint NOT NULL,
  name varchar(50) DEFAULT NULL,
  image varchar(200) DEFAULT NULL,
  order_id bigint DEFAULT NULL,
  dish_id bigint DEFAULT NULL,
  setmeal_id bigint DEFAULT NULL,
  dish_flavor varchar(50) DEFAULT NULL,
  number int DEFAULT '1',
  amount decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE address_book (
  id bigint NOT NULL,
  user_id bigint DEFAULT NULL,
  consignee varchar(32) DEFAULT NULL,
  sex varchar(8) DEFAULT NULL,
  phone varchar(16) DEFAULT NULL,
  province_code varchar(16) DEFAULT NULL,
  province_name varchar(32) DEFAULT NULL,
  city_code varchar(16) DEFAULT NULL,
  city_name varchar(32) DEFAULT NULL,
  district_code varchar(16) DEFAULT NULL,
  district_name varchar(32) DEFAULT NULL,
  detail varchar(255) DEFAULT NULL,
  label varchar(32) DEFAULT NULL,
  is_default int DEFAULT '0',
  create_time datetime DEFAULT NULL,
  update_time datetime DEFAULT NULL,
  create_user bigint DEFAULT NULL,
  update_user bigint DEFAULT NULL,
  is_deleted int DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE shopping_cart (
  id bigint NOT NULL,
  name varchar(50) DEFAULT NULL,
  user_id bigint DEFAULT NULL,
  dish_id bigint DEFAULT NULL,
  setmeal_id bigint DEFAULT NULL,
  dish_flavor varchar(50) DEFAULT NULL,
  number int DEFAULT '1',
  amount decimal(10,2) DEFAULT NULL,
  image varchar(200) DEFAULT NULL,
  create_time datetime DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE user (
  id bigint NOT NULL,
  tenant_id bigint DEFAULT NULL,
  name varchar(50) DEFAULT NULL,
  phone varchar(100) DEFAULT NULL,
  sex varchar(2) DEFAULT NULL,
  id_number varchar(18) DEFAULT NULL,
  avatar varchar(500) DEFAULT NULL,
  status int DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE printer_config (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  store_id bigint DEFAULT NULL,
  name varchar(50) NOT NULL,
  type varchar(20) NOT NULL,
  brand varchar(20) DEFAULT NULL,
  device_id varchar(100) DEFAULT NULL,
  ip_address varchar(15) DEFAULT NULL,
  port int DEFAULT NULL,
  paper_size varchar(10) DEFAULT '58mm',
  print_type varchar(20) NOT NULL,
  status int DEFAULT '1',
  sort int DEFAULT '0',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  system_printer_name varchar(200) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_tenant (tenant_id)
);

CREATE TABLE printer_log (
  id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint DEFAULT NULL,
  print_type varchar(20) NOT NULL,
  printer_id bigint DEFAULT NULL,
  content text,
  status int DEFAULT '0',
  error_msg varchar(255) DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order (order_id),
  KEY idx_printer (printer_id)
);

CREATE TABLE payment_order (
  id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  tenant_id bigint DEFAULT NULL,
  trade_no varchar(64) NOT NULL,
  channel_trade_no varchar(128) DEFAULT NULL,
  channel varchar(20) NOT NULL,
  amount decimal(10,2) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  paid_time datetime DEFAULT NULL,
  notify_time datetime DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE refund_record (
  id bigint NOT NULL AUTO_INCREMENT,
  payment_order_id bigint NOT NULL,
  refund_no varchar(64) NOT NULL,
  amount decimal(10,2) NOT NULL,
  reason varchar(255) DEFAULT NULL,
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE dining_area (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  name varchar(50) NOT NULL,
  sort int DEFAULT '0',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE dining_table (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  area_id bigint DEFAULT NULL,
  name varchar(20) NOT NULL,
  seat_count int DEFAULT '4',
  status varchar(20) NOT NULL DEFAULT 'FREE',
  min_amount decimal(10,2) DEFAULT NULL,
  qr_code_url varchar(255) DEFAULT NULL,
  sort int DEFAULT '0',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE dining_queue (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  queue_no varchar(20) NOT NULL,
  phone varchar(20) DEFAULT NULL,
  seat_count int DEFAULT NULL,
  status varchar(20) NOT NULL DEFAULT 'WAITING',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE dining_reservation (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  table_id bigint DEFAULT NULL,
  customer_name varchar(50) NOT NULL,
  phone varchar(20) NOT NULL,
  reserved_time datetime NOT NULL,
  seat_count int DEFAULT NULL,
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  remark varchar(255) DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE delivery_order (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  platform_order_id varchar(128) NOT NULL,
  platform varchar(20) NOT NULL,
  dish_summary varchar(500) DEFAULT NULL,
  amount decimal(10,2) DEFAULT NULL,
  user_name varchar(50) DEFAULT NULL,
  phone varchar(20) DEFAULT NULL,
  address varchar(255) DEFAULT NULL,
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  order_time datetime DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE supplier (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  name varchar(100) NOT NULL,
  contact varchar(50) DEFAULT NULL,
  phone varchar(20) DEFAULT NULL,
  address varchar(255) DEFAULT NULL,
  status int DEFAULT '1',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE material_category (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  name varchar(50) NOT NULL,
  sort int DEFAULT '0',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE material (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  category_id bigint DEFAULT NULL,
  name varchar(100) NOT NULL,
  unit varchar(10) NOT NULL,
  stock_qty decimal(10,2) DEFAULT '0.00',
  min_stock decimal(10,2) DEFAULT '0.00',
  unit_price decimal(10,2) DEFAULT NULL,
  supplier_id bigint DEFAULT NULL,
  barcode varchar(50) DEFAULT NULL,
  status int DEFAULT '1',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE stock_record (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  material_id bigint NOT NULL,
  type varchar(10) NOT NULL,
  qty decimal(10,2) NOT NULL,
  unit_price decimal(10,2) DEFAULT NULL,
  total_amount decimal(10,2) DEFAULT NULL,
  biz_id bigint DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  operator varchar(50) DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE stock_check (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  check_no varchar(64) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'DRAFT',
  total_diff_amount decimal(10,2) DEFAULT NULL,
  operator varchar(50) DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE stock_check_detail (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  check_id bigint NOT NULL,
  material_id bigint NOT NULL,
  book_qty decimal(10,2) NOT NULL,
  actual_qty decimal(10,2) NOT NULL,
  diff_qty decimal(10,2) NOT NULL,
  remark varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE purchase_order (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  order_no varchar(64) NOT NULL,
  supplier_id bigint DEFAULT NULL,
  total_amount decimal(10,2) DEFAULT NULL,
  status varchar(20) NOT NULL DEFAULT 'DRAFT',
  operator varchar(50) DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE purchase_order_detail (
  id bigint NOT NULL AUTO_INCREMENT,
  purchase_order_id bigint NOT NULL,
  material_id bigint NOT NULL,
  qty decimal(10,2) NOT NULL,
  unit_price decimal(10,2) DEFAULT NULL,
  amount decimal(10,2) DEFAULT NULL,
  received_qty decimal(10,2) DEFAULT '0.00',
  remark varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE member (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  user_id bigint DEFAULT NULL,
  level_id bigint DEFAULT NULL,
  name varchar(50) DEFAULT NULL,
  phone varchar(20) NOT NULL,
  points bigint DEFAULT '0',
  balance decimal(10,2) DEFAULT '0.00',
  total_consumption decimal(10,2) DEFAULT '0.00',
  status int DEFAULT '1',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE member_level (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  name varchar(50) NOT NULL,
  min_points bigint DEFAULT '0',
  discount decimal(3,2) DEFAULT '1.00',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE points_record (
  id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  type varchar(10) NOT NULL,
  points int NOT NULL,
  biz_type varchar(50) DEFAULT NULL,
  biz_id bigint DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE recharge_record (
  id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  amount decimal(10,2) NOT NULL,
  gift_amount decimal(10,2) DEFAULT '0.00',
  payment_method varchar(20) DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE coupon_template (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  name varchar(100) NOT NULL,
  type varchar(20) NOT NULL,
  condition_amount decimal(10,2) DEFAULT NULL,
  discount_amount decimal(10,2) DEFAULT NULL,
  discount_rate decimal(3,2) DEFAULT NULL,
  total_count int DEFAULT '0',
  remain_count int DEFAULT '0',
  valid_days int DEFAULT NULL,
  status int DEFAULT '1',
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  updated_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE coupon_user (
  id bigint NOT NULL AUTO_INCREMENT,
  member_id bigint NOT NULL,
  template_id bigint NOT NULL,
  code varchar(32) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'UNUSED',
  used_time datetime DEFAULT NULL,
  order_id bigint DEFAULT NULL,
  expire_time datetime DEFAULT NULL,
  created_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE INDEX idx_employee_tenant ON employee(tenant_id);
CREATE INDEX idx_dish_tenant_category ON dish(tenant_id, category_id);
CREATE INDEX idx_setmeal_tenant_category ON setmeal(tenant_id, category_id);
CREATE INDEX idx_order_user ON orders(user_id, order_time);
CREATE INDEX idx_address_user ON address_book(user_id);
CREATE INDEX idx_cart_user ON shopping_cart(user_id);
CREATE INDEX idx_dish_flavor_tenant_dish ON dish_flavor(tenant_id, dish_id);
CREATE INDEX idx_order_detail_order ON order_detail(order_id);
