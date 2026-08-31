-- Cashier module test schema (H2 compatible)
-- Matches entity column names from MyBatis-Plus default camelCase conversion

DROP TABLE IF EXISTS daily_settlement;
DROP TABLE IF EXISTS cashier_record;

-- CashierRecord entity (@TableName("cashier_record"))
-- Columns: id, orderId, orderNumber, payType, amount, actualAmount, changeAmount, cashierTime, cashierId, cashierName, remark, tenantId, createTime, createUser
CREATE TABLE cashier_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id bigint NULL DEFAULT NULL COMMENT '订单ID',
  order_number varchar(50) NULL DEFAULT NULL COMMENT '订单号',
  pay_type int NULL DEFAULT NULL COMMENT '收银类型 1-现金 2-微信 3-支付宝 4-银行卡 5-会员储值',
  amount decimal(10,2) NULL DEFAULT NULL COMMENT '收银金额',
  actual_amount decimal(10,2) NULL DEFAULT NULL COMMENT '实收金额',
  change_amount decimal(10,2) NULL DEFAULT NULL COMMENT '找零金额',
  cashier_time datetime NULL DEFAULT NULL COMMENT '收银时间',
  cashier_id bigint NULL DEFAULT NULL COMMENT '收银员ID',
  cashier_name varchar(50) NULL DEFAULT NULL COMMENT '收银员姓名',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (id)
);

-- DailySettlement entity (@TableName("daily_settlement"))
-- Columns: id, settlementDate, totalRevenue, cashIncome, wechatIncome, alipayIncome, bankcardIncome, otherIncome, orderCount, refundAmount, refundCount, netIncome, materialCost, laborCost, otherCost, totalCost, grossProfit, profitRate, status, settlementTime, settlementUserId, settlementUserName, remark, tenantId, createTime, updateTime, createUser, updateUser
CREATE TABLE daily_settlement (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  settlement_date date NULL DEFAULT NULL COMMENT '结算日期',
  total_revenue decimal(10,2) NULL DEFAULT NULL COMMENT '营业额',
  cash_income decimal(10,2) NULL DEFAULT NULL COMMENT '现金收入',
  wechat_income decimal(10,2) NULL DEFAULT NULL COMMENT '微信收入',
  alipay_income decimal(10,2) NULL DEFAULT NULL COMMENT '支付宝收入',
  bankcard_income decimal(10,2) NULL DEFAULT NULL COMMENT '银行卡收入',
  other_income decimal(10,2) NULL DEFAULT NULL COMMENT '其他收入',
  order_count int NULL DEFAULT NULL COMMENT '订单数量',
  refund_amount decimal(10,2) NULL DEFAULT NULL COMMENT '退款金额',
  refund_count int NULL DEFAULT NULL COMMENT '退款数量',
  net_income decimal(10,2) NULL DEFAULT NULL COMMENT '净收入',
  material_cost decimal(10,2) NULL DEFAULT NULL COMMENT '食材成本',
  labor_cost decimal(10,2) NULL DEFAULT NULL COMMENT '人工成本',
  other_cost decimal(10,2) NULL DEFAULT NULL COMMENT '其他成本',
  total_cost decimal(10,2) NULL DEFAULT NULL COMMENT '总成本',
  gross_profit decimal(10,2) NULL DEFAULT NULL COMMENT '毛利润',
  profit_rate decimal(10,2) NULL DEFAULT NULL COMMENT '毛利率(%)',
  status int NULL DEFAULT 0 COMMENT '结账状态 0-未结账 1-已结账',
  settlement_time datetime NULL DEFAULT NULL COMMENT '结账时间',
  settlement_user_id bigint NULL DEFAULT NULL COMMENT '结账人ID',
  settlement_user_name varchar(50) NULL DEFAULT NULL COMMENT '结账人姓名',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  version int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (id)
);