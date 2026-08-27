-- Finance module test schema (MySQL compatible)
-- Matches entity column names from MyBatis-Plus default camelCase conversion

DROP TABLE IF EXISTS profit_analysis;
DROP TABLE IF EXISTS reconciliation_statement;
DROP TABLE IF EXISTS withdrawal_application;

-- WithdrawalApplication entity (@TableName("withdrawal_application"))
CREATE TABLE withdrawal_application (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  application_no varchar(50) NULL DEFAULT NULL COMMENT '申请编号',
  applicant_id bigint NULL DEFAULT NULL COMMENT '申请人ID',
  applicant_name varchar(50) NULL DEFAULT NULL COMMENT '申请人姓名',
  amount decimal(10,2) NULL DEFAULT NULL COMMENT '提现金额',
  withdraw_method int NULL DEFAULT NULL COMMENT '提现方式 1-银行卡 2-支付宝 3-微信',
  receive_account varchar(100) NULL DEFAULT NULL COMMENT '收款账号',
  receive_name varchar(50) NULL DEFAULT NULL COMMENT '收款人姓名',
  status int NULL DEFAULT 0 COMMENT '状态 0-待审核 1-已审批 2-已付款 3-已拒绝 4-已取消',
  reviewer_id bigint NULL DEFAULT NULL COMMENT '审核人ID',
  reviewer_name varchar(50) NULL DEFAULT NULL COMMENT '审核人姓名',
  review_time datetime NULL DEFAULT NULL COMMENT '审核时间',
  review_remark varchar(200) NULL DEFAULT NULL COMMENT '审核备注',
  payment_time datetime NULL DEFAULT NULL COMMENT '付款时间',
  payment_no varchar(50) NULL DEFAULT NULL COMMENT '付款编号',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);

-- ReconciliationStatement entity (@TableName("reconciliation_statement"))
CREATE TABLE reconciliation_statement (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  statement_no varchar(50) NULL DEFAULT NULL COMMENT '对账单编号',
  statement_date date NULL DEFAULT NULL COMMENT '对账日期',
  platform varchar(20) NULL DEFAULT 'all' COMMENT '平台 all/wechat/alipay/bank',
  system_amount decimal(12,2) NULL DEFAULT NULL COMMENT '系统金额',
  platform_amount decimal(12,2) NULL DEFAULT NULL COMMENT '平台金额',
  difference_amount decimal(12,2) NULL DEFAULT NULL COMMENT '差额',
  order_count int NULL DEFAULT NULL COMMENT '订单数',
  refund_amount decimal(12,2) NULL DEFAULT NULL COMMENT '退款金额',
  refund_count int NULL DEFAULT NULL COMMENT '退款数',
  fee_amount decimal(10,2) NULL DEFAULT NULL COMMENT '手续费',
  net_amount decimal(12,2) NULL DEFAULT NULL COMMENT '净额',
  status int NULL DEFAULT 0 COMMENT '状态 0-未对账 1-已对账 2-有差异',
  reconcile_time datetime NULL DEFAULT NULL COMMENT '对账时间',
  reconcile_user_id bigint NULL DEFAULT NULL COMMENT '对账人ID',
  reconcile_user_name varchar(50) NULL DEFAULT NULL COMMENT '对账人姓名',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);

-- ProfitAnalysis entity (@TableName("profit_analysis"))
CREATE TABLE profit_analysis (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  analysis_date date NULL DEFAULT NULL COMMENT '分析日期',
  total_revenue decimal(12,2) NULL DEFAULT NULL COMMENT '总营收',
  food_cost decimal(12,2) NULL DEFAULT NULL COMMENT '食材成本',
  labor_cost decimal(12,2) NULL DEFAULT NULL COMMENT '人工成本',
  other_cost decimal(12,2) NULL DEFAULT NULL COMMENT '其他成本',
  total_cost decimal(12,2) NULL DEFAULT NULL COMMENT '总成本',
  gross_profit decimal(12,2) NULL DEFAULT NULL COMMENT '毛利润',
  gross_profit_rate decimal(10,4) NULL DEFAULT NULL COMMENT '毛利率(%)',
  operating_expense decimal(12,2) NULL DEFAULT NULL COMMENT '运营费用',
  net_profit decimal(12,2) NULL DEFAULT NULL COMMENT '净利润',
  net_profit_rate decimal(10,4) NULL DEFAULT NULL COMMENT '净利率(%)',
  order_count int NULL DEFAULT NULL COMMENT '订单数',
  customer_count int NULL DEFAULT NULL COMMENT '客户数',
  average_order_value decimal(10,2) NULL DEFAULT NULL COMMENT '客单价',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);