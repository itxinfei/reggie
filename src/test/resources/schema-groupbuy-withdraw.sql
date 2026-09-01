-- Group buy and withdrawal test schema (H2 compatible)
-- Matches MyBatis-Plus 3.4.2 default UPPER_SNAKE_CASE column naming
-- Note: supplier_settlement is already in schema-inventory.sql, NOT duplicated here

DROP TABLE IF EXISTS group_buy_participation;
DROP TABLE IF EXISTS group_buy_campaign;
DROP TABLE IF EXISTS withdrawal_record;
DROP TABLE IF EXISTS withdrawal_request;

-- 拼团活动表
CREATE TABLE group_buy_campaign (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  NAME varchar(100) NOT NULL COMMENT '活动名称',
  DESCRIPTION varchar(500) NULL DEFAULT '' COMMENT '活动描述',
  GROUP_ID bigint NOT NULL DEFAULT 0 COMMENT '拼团组ID',
  STATUS varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/CLOSED/ENDED',
  START_TIME datetime NOT NULL COMMENT '开始时间',
  END_TIME datetime NOT NULL COMMENT '结束时间',
  MIN_MEMBERS int NOT NULL DEFAULT 2 COMMENT '最少成团人数',
  MAX_MEMBERS int NOT NULL DEFAULT 10 COMMENT '最多成团人数',
  ORIGINAL_PRICE decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '原价',
  GROUP_PRICE decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '拼团价',
  DISH_ID bigint NOT NULL DEFAULT 0 COMMENT '菜品ID',
  DISH_NAME varchar(100) NULL DEFAULT '' COMMENT '菜品名称',
  IMAGE varchar(255) NULL DEFAULT '' COMMENT '活动图片URL',
  CREATE_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  UPDATE_TIME datetime NULL DEFAULT NULL COMMENT '更新时间',
  IS_DELETED int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (ID)
);

CREATE INDEX idx_group_buy_campaign_tenant_id ON group_buy_campaign(TENANT_ID);
CREATE INDEX idx_group_buy_campaign_status ON group_buy_campaign(STATUS);

-- 拼团参与记录表
CREATE TABLE group_buy_participation (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  GROUP_BUY_ID bigint NOT NULL COMMENT '拼团活动ID',
  ORDER_ID bigint NOT NULL COMMENT '订单ID',
  USER_ID bigint NOT NULL COMMENT '用户ID',
  STATUS varchar(20) NOT NULL DEFAULT 'JOINED' COMMENT '状态：JOINED/PAID/CANCELLED',
  JOIN_TIME datetime NOT NULL COMMENT '参团时间',
  PAY_TIME datetime NULL DEFAULT NULL COMMENT '支付时间',
  CREATE_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (ID)
);

CREATE INDEX idx_group_buy_participation_tenant_id ON group_buy_participation(TENANT_ID);
CREATE INDEX idx_group_buy_participation_group_buy_id ON group_buy_participation(GROUP_BUY_ID);
CREATE INDEX idx_group_buy_participation_order_id ON group_buy_participation(ORDER_ID);

-- 提现申请表
CREATE TABLE withdrawal_request (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  USER_ID bigint NOT NULL COMMENT '用户ID',
  AMOUNT decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '提现金额',
  BANK_NAME varchar(100) NOT NULL DEFAULT '' COMMENT '银行名称',
  ACCOUNT_NAME varchar(100) NOT NULL DEFAULT '' COMMENT '开户人姓名',
  ACCOUNT_NUMBER varchar(100) NOT NULL DEFAULT '' COMMENT '银行账号',
  STATUS varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/TRANSFERRED',
  REJECT_REASON varchar(255) NULL DEFAULT '' COMMENT '拒绝原因',
  CREATE_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  APPROVE_TIME datetime NULL DEFAULT NULL COMMENT '审批时间',
  APPROVE_USER_ID bigint NULL DEFAULT NULL COMMENT '审批人ID',
  IS_DELETED int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (ID)
);

CREATE INDEX idx_withdrawal_request_tenant_id ON withdrawal_request(TENANT_ID);
CREATE INDEX idx_withdrawal_request_user_id ON withdrawal_request(USER_ID);
CREATE INDEX idx_withdrawal_request_status ON withdrawal_request(STATUS);

-- 提现记录表
CREATE TABLE withdrawal_record (
  ID bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  TENANT_ID bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  WITHDRAWAL_ID bigint NOT NULL COMMENT '提现申请ID',
  ACTUAL_AMOUNT decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '实际到账金额',
  FEE decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '手续费',
  TRANSFER_TIME datetime NULL DEFAULT NULL COMMENT '转账时间',
  BANK_TRACE_NO varchar(100) NULL DEFAULT '' COMMENT '银行流水号',
  CREATE_TIME datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (ID)
);

CREATE INDEX idx_withdrawal_record_tenant_id ON withdrawal_record(TENANT_ID);
CREATE INDEX idx_withdrawal_record_withdrawal_id ON withdrawal_record(WITHDRAWAL_ID);
