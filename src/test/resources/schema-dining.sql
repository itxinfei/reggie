-- Dining module test schema (H2 compatible)
-- Matches entity column names from MyBatis-Plus default camelCase conversion


-- TableArea entity (@TableName("dining_area"))
-- Columns: id, tenantId, name, sort, createdTime, updateTime
CREATE TABLE IF NOT EXISTS dining_area (
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
CREATE TABLE IF NOT EXISTS dining_table (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  area_id bigint NULL DEFAULT NULL COMMENT '区域ID',
  name varchar(50) NULL DEFAULT NULL COMMENT '桌台名称',
  seat_count int NULL DEFAULT NULL COMMENT '座位',
  status varchar(20) NULL DEFAULT NULL COMMENT '状态 FREE/OCCUPIED/RESERVED/CLEANING',
  min_amount decimal(10,2) NULL DEFAULT NULL COMMENT '低消',
  qr_code_url varchar(255) NULL DEFAULT NULL COMMENT '二维码URL',
  current_order_id bigint NULL DEFAULT NULL COMMENT '当前关联订单ID（开台后绑定',
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
CREATE TABLE IF NOT EXISTS dining_queue (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  queue_no varchar(32) NULL DEFAULT NULL COMMENT '排队',
  phone varchar(20) NULL DEFAULT NULL COMMENT '手机',
  user_id bigint NULL DEFAULT NULL COMMENT '取号顾客用户ID',
  seat_count int NULL DEFAULT NULL COMMENT '人数',
  status varchar(20) NULL DEFAULT NULL COMMENT '状态 WAITING/CALLED/CANCELLED/SERVED',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

-- Reservation entity (@TableName("dining_reservation"))
-- Columns: id, tenantId, tableId, customerName, phone, reservedTime, seatCount, status, remark, createdTime, updateTime
CREATE TABLE IF NOT EXISTS dining_reservation (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  table_id bigint NULL DEFAULT NULL COMMENT '桌台ID',
  customer_name varchar(50) NULL DEFAULT NULL COMMENT '顾姓名',
  phone varchar(20) NULL DEFAULT NULL COMMENT '手机',
  user_id bigint NULL DEFAULT NULL COMMENT '预订顾客用户ID',
  reserved_time datetime NULL DEFAULT NULL COMMENT '预时间',
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

-- 修改点(2026-09-26)：每方法执行前清空模块表，消除跨方法残留导致的计数/主键冲突
-- 单库隔离（2026-10-06）：仅清理测试租户 999 的残留，禁止无条件全表 DELETE（会清掉租户 1 演示数据）
DELETE FROM dining_area WHERE tenant_id = 999;
DELETE FROM dining_table WHERE tenant_id = 999;
DELETE FROM dining_queue WHERE tenant_id = 999;
DELETE FROM dining_reservation WHERE tenant_id = 999;

-- ==================== 团餐预订表（企业内部订餐，2026-10-06） ====================
CREATE TABLE IF NOT EXISTS group_meal_booking (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NOT NULL COMMENT '租户ID',
  department_id bigint NULL DEFAULT NULL COMMENT '部门ID',
  department_name varchar(50) NULL DEFAULT NULL COMMENT '部门名快照',
  contact_name varchar(50) NOT NULL COMMENT '联系人',
  contact_phone varchar(20) NOT NULL COMMENT '联系电话',
  meal_date date NOT NULL COMMENT '用餐日期',
  meal_type varchar(10) NOT NULL DEFAULT 'LUNCH' COMMENT '餐段 LUNCH:午餐 DINNER:晚餐',
  status tinyint NOT NULL DEFAULT 0 COMMENT '状态 0:待确认 1:已确认 2:已完成 3:已取消',
  total_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '总金额（下单时快照）',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id),
  KEY idx_gmb_tenant_date (tenant_id, meal_date)
);

-- ==================== 团餐预订明细表 ====================
CREATE TABLE IF NOT EXISTS group_meal_booking_item (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NOT NULL COMMENT '租户ID',
  booking_id bigint NOT NULL COMMENT '预订ID',
  item_type varchar(10) NOT NULL COMMENT '条目类型 DISH:菜品 SETMEAL:套餐',
  item_id bigint NOT NULL COMMENT '菜品/套餐ID',
  item_name varchar(100) NOT NULL COMMENT '名称快照',
  price decimal(10,2) NOT NULL COMMENT '单价快照',
  quantity int NOT NULL COMMENT '份数',
  PRIMARY KEY (id),
  KEY idx_gmbi_booking (booking_id)
);
