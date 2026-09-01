-- Customer Service module test schema (MySQL compatible)
-- Matches entity column names from MyBatis-Plus default camelCase conversion

DROP TABLE IF EXISTS cs_message;
DROP TABLE IF EXISTS cs_session;
DROP TABLE IF EXISTS complaint;

-- CsSession entity (@TableName("cs_session"))
CREATE TABLE cs_session (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_no varchar(50) NULL DEFAULT NULL COMMENT '会话编号',
  user_id bigint NULL DEFAULT NULL COMMENT '用户ID',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户',
  agent_id bigint NULL DEFAULT NULL COMMENT '客服ID',
  agent_name varchar(50) NULL DEFAULT NULL COMMENT '客服姓名',
  session_type int NULL DEFAULT NULL COMMENT '会话类型 1- 2-订单 3-投诉',
  order_id bigint NULL DEFAULT NULL COMMENT '关联订单ID',
  status int NULL DEFAULT 0 COMMENT '状 0-等待 1-进 2-已关',
  first_response_time datetime NULL DEFAULT NULL COMMENT '首响应时间',
  close_time datetime NULL DEFAULT NULL COMMENT '关闭时间',
  satisfaction_rating int NULL DEFAULT NULL COMMENT '满意度评(1-5)',
  user_feedback varchar(500) NULL DEFAULT NULL COMMENT '用户反',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);

-- CsMessage entity (@TableName("cs_message"))
CREATE TABLE cs_message (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id bigint NOT NULL COMMENT '会话ID',
  sender_type int NULL DEFAULT NULL COMMENT '发类 1-用户 2-客服 3-系统',
  sender_id bigint NULL DEFAULT NULL COMMENT '发ID',
  sender_name varchar(50) NULL DEFAULT NULL COMMENT '发',
  message_type int NULL DEFAULT 1 COMMENT '消息类型 1-文本 2-图片 3-订单卡片',
  content text NULL DEFAULT NULL COMMENT '消息内',
  image_url varchar(500) NULL DEFAULT NULL COMMENT '图片URL',
  is_read int NULL DEFAULT 0 COMMENT '昐已 0- 1-已',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id)
);

-- Complaint entity (@TableName("complaint"))
CREATE TABLE complaint (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  complaint_no varchar(50) NULL DEFAULT NULL COMMENT '投诉编号',
  user_id bigint NULL DEFAULT NULL COMMENT '用户ID',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户',
  user_phone varchar(20) NULL DEFAULT NULL COMMENT '用户手机',
  order_id bigint NULL DEFAULT NULL COMMENT '订单ID',
  order_number varchar(50) NULL DEFAULT NULL COMMENT '订单编号',
  complaint_type int NULL DEFAULT NULL COMMENT '投诉类型 1-食品质量 2-配服 3-服务态度 4-价格 5-其他',
  title varchar(200) NULL DEFAULT NULL COMMENT '投诉标',
  content text NULL DEFAULT NULL COMMENT '投诉内',
  image_urls varchar(1000) NULL DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  status int NULL DEFAULT 0 COMMENT '状 0-待 1-处理 2-已解 3-已关',
  handler_id bigint NULL DEFAULT NULL COMMENT '处理人ID',
  handler_name varchar(50) NULL DEFAULT NULL COMMENT '处理人',
  handle_result varchar(500) NULL DEFAULT NULL COMMENT '处理结果',
  compensation_amount decimal(10,2) NULL DEFAULT NULL COMMENT '补偿金',
  handle_time datetime NULL DEFAULT NULL COMMENT '处理时间',
  satisfaction int NULL DEFAULT NULL COMMENT '满意 1-非常不满 2-不满 3- 4-满意 5-非常满意',
  user_feedback varchar(500) NULL DEFAULT NULL COMMENT '用户反',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);