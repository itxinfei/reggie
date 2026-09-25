-- 骑手 + 配送时长记录（H2 MySQL 兼容模式）。
-- 配合主 schema.sql 使用：主 schema 已含 orders/order_detail/address_book/tenant/store_info，
-- 本文件补骑手链路所需的 rider 与 delivery_time_record 两表。

CREATE TABLE IF NOT EXISTS rider (
  id bigint NOT NULL AUTO_INCREMENT,
  name varchar(50) NOT NULL,
  phone varchar(20) DEFAULT NULL,
  password varchar(100) DEFAULT NULL,
  avatar varchar(200) DEFAULT NULL,
  current_longitude decimal(12,8) DEFAULT NULL,
  current_latitude decimal(12,8) DEFAULT NULL,
  status int DEFAULT 0,
  current_order_count int DEFAULT 0,
  total_order_count int DEFAULT 0,
  rating decimal(3,1) DEFAULT 5.0,
  last_location_time datetime DEFAULT NULL,
  tenant_id bigint DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS delivery_time_record (
  id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  order_number varchar(50) DEFAULT NULL,
  rider_id bigint DEFAULT NULL,
  rider_name varchar(50) DEFAULT NULL,
  order_time datetime DEFAULT NULL,
  accept_time datetime DEFAULT NULL,
  pickup_time datetime DEFAULT NULL,
  deliver_time datetime DEFAULT NULL,
  estimated_minutes int DEFAULT NULL,
  actual_minutes int DEFAULT NULL,
  distance decimal(10,2) DEFAULT NULL,
  status int DEFAULT 0,
  remark varchar(500) DEFAULT NULL,
  tenant_id bigint DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (order_id)
);

CREATE TABLE IF NOT EXISTS rider_location_record (
  id bigint NOT NULL AUTO_INCREMENT,
  rider_id bigint NOT NULL,
  order_id bigint DEFAULT NULL,
  longitude decimal(12,8) NOT NULL,
  latitude decimal(12,8) NOT NULL,
  speed decimal(5,2) DEFAULT NULL,
  direction decimal(5,2) DEFAULT NULL,
  record_time datetime NOT NULL,
  tenant_id bigint DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
