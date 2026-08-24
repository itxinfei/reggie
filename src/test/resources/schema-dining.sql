-- Dining module test schema (H2 compatible)
-- Matches entity column names from MyBatis-Plus default camelCase conversion

DROP TABLE IF EXISTS dining_queue;
DROP TABLE IF EXISTS dining_reservation;
DROP TABLE IF EXISTS dining_table;
DROP TABLE IF EXISTS dining_area;

-- TableArea entity (@TableName("dining_area"))
-- Columns: id, tenantId, name, sort, createdTime, updateTime
CREATE TABLE dining_area (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  name varchar(50) NULL DEFAULT NULL COMMENT '区域名称',
  sort int NULL DEFAULT 0 COMMENT '排序',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

-- DiningTable entity (table name from class: dining_table)
-- Columns: id, tenantId, areaId, name, seatCount, status, minAmount, qrCodeUrl, currentOrderId, sort, createdTime, updateTime
-- areaName is @TableField(exist=false), not persisted
CREATE TABLE dining_table (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  area_id bigint NULL DEFAULT NULL COMMENT '区域ID',
  name varchar(50) NULL DEFAULT NULL COMMENT '桌台名称',
  seat_count int NULL DEFAULT NULL COMMENT '座位数',
  status varchar(20) NULL DEFAULT NULL COMMENT '状态 FREE/OCCUPIED/RESERVED/CLEANING',
  min_amount decimal(10,2) NULL DEFAULT NULL COMMENT '最低消费',
  qr_code_url varchar(255) NULL DEFAULT NULL COMMENT '二维码URL',
  current_order_id bigint NULL DEFAULT NULL COMMENT '当前关联订单ID（开台后绑定）',
  sort int NULL DEFAULT 0 COMMENT '排序',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

-- QueueRecord entity (@TableName("dining_queue"))
-- Columns: id, tenantId, queueNo, phone, seatCount, status, createdTime, updateTime
CREATE TABLE dining_queue (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  queue_no varchar(32) NULL DEFAULT NULL COMMENT '排队号',
  phone varchar(20) NULL DEFAULT NULL COMMENT '手机号',
  seat_count int NULL DEFAULT NULL COMMENT '人数',
  status varchar(20) NULL DEFAULT NULL COMMENT '状态 WAITING/CALLED/COMPLETED/CANCELLED',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

-- Reservation entity (@TableName("dining_reservation"))
-- Columns: id, tenantId, tableId, customerName, phone, reservedTime, seatCount, status, remark, createdTime, updateTime
CREATE TABLE dining_reservation (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  table_id bigint NULL DEFAULT NULL COMMENT '桌台ID',
  customer_name varchar(50) NULL DEFAULT NULL COMMENT '顾客姓名',
  phone varchar(20) NULL DEFAULT NULL COMMENT '手机号',
  reserved_time datetime NULL DEFAULT NULL COMMENT '预订时间',
  seat_count int NULL DEFAULT NULL COMMENT '人数',
  status varchar(20) NULL DEFAULT NULL COMMENT '状态 PENDING/CONFIRMED/CANCELLED/ARRIVED',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);
