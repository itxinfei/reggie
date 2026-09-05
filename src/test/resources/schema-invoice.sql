-- Invoice module test schema (MySQL compatible)
-- Matches entity column names from MyBatis-Plus default camelCase conversion

DROP TABLE IF EXISTS invoice_record;
DROP TABLE IF EXISTS invoice_title;

-- InvoiceTitle entity (@TableName("invoice_title"))
-- Columns: id, title, taxNumber, companyName, type, tenantId, createTime, updateTime
CREATE TABLE invoice_title (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  title varchar(200) NOT NULL COMMENT '发票抬头',
  tax_number varchar(100) NULL DEFAULT NULL COMMENT '税号',
  company_name varchar(200) NULL DEFAULT NULL COMMENT '公司名称（企业用）',
  type int NOT NULL DEFAULT 1 COMMENT '类型：1=个人，2=企业',
  tenant_id bigint NOT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);

-- InvoiceRecord entity (@TableName("invoice_record"))
-- Columns: id, orderId, userId, orderNo, titleId, title, taxNumber, type, amount, status,
--          invoiceNo, invoiceCode, invoiceUrl, applyTime, issueTime, tenantId, createTime, updateTime
-- userId 为真实列（申请用户ID，用户端归属过滤），对应迁移 V20260905__invoice_user_id.sql
CREATE TABLE invoice_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id bigint NOT NULL COMMENT '关联订单ID',
  user_id bigint NULL DEFAULT NULL COMMENT '申请用户ID（用户端归属列）',
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
  PRIMARY KEY (id)
);
