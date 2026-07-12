-- Inventory module test schema (H2 compatible)
-- Matches MyBatis-Plus 3.4.2 default UPPER_SNAKE_CASE column naming

-- Drop child tables first, parent tables last
DROP TABLE IF EXISTS stock_record;
DROP TABLE IF EXISTS stock_check_detail;
DROP TABLE IF EXISTS purchase_order_detail;
DROP TABLE IF EXISTS material;
DROP TABLE IF EXISTS purchase_order;
DROP TABLE IF EXISTS stock_check;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS material_category;

CREATE TABLE material_category (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NULL DEFAULT NULL COMMENT '租户ID',
  NAME varchar(50) NULL DEFAULT NULL COMMENT '分类名称',
  SORT int NULL DEFAULT NULL COMMENT '排序',
  CREATED_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  UPDATED_TIME datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (ID)
);

CREATE TABLE supplier (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NULL DEFAULT NULL COMMENT '租户ID',
  NAME varchar(100) NULL DEFAULT NULL COMMENT '供应商名称',
  CONTACT varchar(50) NULL DEFAULT NULL COMMENT '联系人',
  PHONE varchar(20) NULL DEFAULT NULL COMMENT '联系电话',
  ADDRESS varchar(255) NULL DEFAULT NULL COMMENT '地址',
  STATUS int NULL DEFAULT NULL COMMENT '状态',
  CREATED_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  UPDATED_TIME datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (ID)
);

CREATE TABLE material (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NULL DEFAULT NULL COMMENT '租户ID',
  CATEGORY_ID bigint NULL DEFAULT NULL COMMENT '分类ID',
  NAME varchar(100) NULL DEFAULT NULL COMMENT '物料名称',
  UNIT varchar(20) NULL DEFAULT NULL COMMENT '单位',
  STOCK_QTY decimal(10,2) NULL DEFAULT NULL COMMENT '库存数量',
  MIN_STOCK decimal(10,2) NULL DEFAULT NULL COMMENT '最小库存',
  UNIT_PRICE decimal(10,2) NULL DEFAULT NULL COMMENT '单价',
  SUPPLIER_ID bigint NULL DEFAULT NULL COMMENT '供应商ID',
  BARCODE varchar(50) NULL DEFAULT NULL COMMENT '条形码',
  STATUS int NULL DEFAULT NULL COMMENT '状态（1-正常，0-禁用）',
  CREATED_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  UPDATED_TIME datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (ID)
);

CREATE TABLE purchase_order (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NULL DEFAULT NULL COMMENT '租户ID',
  ORDER_NO varchar(50) NULL DEFAULT NULL COMMENT '订单编号',
  SUPPLIER_ID bigint NULL DEFAULT NULL COMMENT '供应商ID',
  TOTAL_AMOUNT decimal(10,2) NULL DEFAULT NULL COMMENT '总金额',
  STATUS varchar(32) NULL DEFAULT NULL COMMENT '状态',
  OPERATOR varchar(50) NULL DEFAULT NULL COMMENT '操作员',
  REMARK varchar(200) NULL DEFAULT NULL COMMENT '备注',
  CREATED_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  UPDATED_TIME datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (ID)
);

CREATE TABLE stock_check (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NULL DEFAULT NULL COMMENT '租户ID',
  CHECK_NO varchar(50) NULL DEFAULT NULL COMMENT '盘点单号',
  STATUS varchar(32) NULL DEFAULT NULL COMMENT '状态',
  TOTAL_DIFF_AMOUNT decimal(10,2) NULL DEFAULT NULL COMMENT '总差异金额',
  OPERATOR varchar(50) NULL DEFAULT NULL COMMENT '操作员',
  REMARK varchar(200) NULL DEFAULT NULL COMMENT '备注',
  CREATED_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  UPDATED_TIME datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (ID)
);

CREATE TABLE purchase_order_detail (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NULL DEFAULT NULL COMMENT '租户ID',
  PURCHASE_ORDER_ID bigint NULL DEFAULT NULL COMMENT '采购订单ID',
  MATERIAL_ID bigint NULL DEFAULT NULL COMMENT '物料ID',
  QTY decimal(10,2) NULL DEFAULT NULL COMMENT '数量',
  UNIT_PRICE decimal(10,2) NULL DEFAULT NULL COMMENT '单价',
  AMOUNT decimal(10,2) NULL DEFAULT NULL COMMENT '金额',
  RECEIVED_QTY decimal(10,2) NULL DEFAULT NULL COMMENT '收货数量',
  REMARK varchar(200) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (ID)
);

CREATE TABLE stock_check_detail (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NULL DEFAULT NULL COMMENT '租户ID',
  CHECK_ID bigint NULL DEFAULT NULL COMMENT '盘点ID',
  MATERIAL_ID bigint NULL DEFAULT NULL COMMENT '物料ID',
  BOOK_QTY decimal(10,2) NULL DEFAULT NULL COMMENT '账面数量',
  ACTUAL_QTY decimal(10,2) NULL DEFAULT NULL COMMENT '实际数量',
  DIFF_QTY decimal(10,2) NULL DEFAULT NULL COMMENT '差异数量',
  REMARK varchar(200) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (ID)
);

CREATE TABLE stock_record (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NULL DEFAULT NULL COMMENT '租户ID',
  MATERIAL_ID bigint NULL DEFAULT NULL COMMENT '物料ID',
  TYPE varchar(32) NULL DEFAULT NULL COMMENT '类型',
  QTY decimal(10,2) NULL DEFAULT NULL COMMENT '数量',
  UNIT_PRICE decimal(10,2) NULL DEFAULT NULL COMMENT '单价',
  TOTAL_AMOUNT decimal(10,2) NULL DEFAULT NULL COMMENT '总金额',
  BIZ_ID bigint NULL DEFAULT NULL COMMENT '业务ID',
  REMARK varchar(200) NULL DEFAULT NULL COMMENT '备注',
  OPERATOR varchar(50) NULL DEFAULT NULL COMMENT '操作员',
  CREATED_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (ID)
);

-- Separate CREATE INDEX statements for H2 compatibility
CREATE INDEX idx_material_category_tenant_id ON material_category(TENANT_ID);
CREATE INDEX idx_supplier_tenant_id ON supplier(TENANT_ID);
CREATE INDEX idx_material_tenant_id ON material(TENANT_ID);
CREATE INDEX idx_material_category_id ON material(CATEGORY_ID);
CREATE INDEX idx_material_supplier_id ON material(SUPPLIER_ID);
CREATE INDEX idx_purchase_order_tenant_id ON purchase_order(TENANT_ID);
CREATE INDEX idx_purchase_order_supplier_id ON purchase_order(SUPPLIER_ID);
CREATE INDEX idx_purchase_order_order_no ON purchase_order(ORDER_NO);
CREATE INDEX idx_stock_check_tenant_id ON stock_check(TENANT_ID);
CREATE INDEX idx_stock_check_check_no ON stock_check(CHECK_NO);
CREATE INDEX idx_purchase_order_detail_tenant_id ON purchase_order_detail(TENANT_ID);
CREATE INDEX idx_purchase_order_detail_purchase_order_id ON purchase_order_detail(PURCHASE_ORDER_ID);
CREATE INDEX idx_purchase_order_detail_material_id ON purchase_order_detail(MATERIAL_ID);
CREATE INDEX idx_stock_check_detail_tenant_id ON stock_check_detail(TENANT_ID);
CREATE INDEX idx_stock_check_detail_check_id ON stock_check_detail(CHECK_ID);
CREATE INDEX idx_stock_check_detail_material_id ON stock_check_detail(MATERIAL_ID);
CREATE INDEX idx_stock_record_tenant_id ON stock_record(TENANT_ID);
CREATE INDEX idx_stock_record_material_id ON stock_record(MATERIAL_ID);
CREATE INDEX idx_stock_record_biz_id ON stock_record(BIZ_ID);
CREATE INDEX idx_stock_record_type ON stock_record(TYPE);
