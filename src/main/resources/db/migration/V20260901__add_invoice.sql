-- ============================================================
-- 电子发票模块建表（P0-2 发票管理）
-- 表：invoice_title（发票抬头）、invoice_record（开票申请记录）
-- 手动执行：mysql -uroot -p reggie < V20260901__add_invoice.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS invoice_title (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  title varchar(200) NOT NULL COMMENT '发票抬头',
  tax_number varchar(100) NULL DEFAULT NULL COMMENT '税号',
  company_name varchar(200) NULL DEFAULT NULL COMMENT '公司名称（企业用）',
  type int NOT NULL DEFAULT 1 COMMENT '类型：1=个人，2=企业',
  tenant_id bigint NOT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_title_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票抬头';

CREATE TABLE IF NOT EXISTS invoice_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id bigint NOT NULL COMMENT '关联订单ID',
  order_no varchar(32) NULL DEFAULT NULL COMMENT '订单号',
  title_id bigint NULL DEFAULT NULL COMMENT '发票抬头ID',
  title varchar(200) NULL DEFAULT NULL COMMENT '发票抬头（冗余）',
  tax_number varchar(100) NULL DEFAULT NULL COMMENT '税号（冗余）',
  type int NOT NULL DEFAULT 1 COMMENT '类型：1=个人，2=企业',
  amount decimal(10,2) NULL DEFAULT NULL COMMENT '开票金额',
  status int NOT NULL DEFAULT 0 COMMENT '状态：0=待申请，1=已申请，2=已开具，3=已作废',
  invoice_no varchar(64) NULL DEFAULT NULL COMMENT '发票号码',
  invoice_code varchar(64) NULL DEFAULT NULL COMMENT '发票代码',
  invoice_url varchar(500) NULL DEFAULT NULL COMMENT '发票PDF地址',
  apply_time datetime NULL DEFAULT NULL COMMENT '申请时间',
  issue_time datetime NULL DEFAULT NULL COMMENT '开具时间',
  tenant_id bigint NOT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_record_order (order_id),
  KEY idx_record_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开票申请记录';
