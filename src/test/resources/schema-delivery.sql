-- Delivery module test schema (H2 compatible)
-- Matches entity column names from MyBatis-Plus 3.4.2 default camelCase conversion
-- Note: createdUser → create_user, updateUser → update_user (explicit @TableField)


-- DeliveryOrder entity (@TableName("delivery_order"))
-- Columns: id, tenantId, platformOrderId, platform, dishSummary, amount,
--          userName, phone, address, status, orderTime, createdTime, updateTime,
--          createdUser(→create_user), updateUser(→update_user)
CREATE TABLE IF NOT EXISTS delivery_order (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  platform_order_id varchar(64) NULL DEFAULT NULL COMMENT '平台订单',
  platform varchar(20) NULL DEFAULT NULL COMMENT '配平',
  order_id bigint NULL DEFAULT NULL COMMENT '本地订单ID（关联 orders.id，可空）',
  dish_summary varchar(255) NULL DEFAULT NULL COMMENT '菜品摘',
  amount decimal(10,2) NULL DEFAULT NULL COMMENT '订单金',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户姓名',
  phone varchar(20) NULL DEFAULT NULL COMMENT 'ϵ绰',
  address varchar(255) NULL DEFAULT NULL COMMENT '配地',
  status varchar(20) NULL DEFAULT NULL COMMENT '订单状',
  order_time datetime NULL DEFAULT NULL COMMENT '下单时间',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  created_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version int NULL DEFAULT NULL COMMENT '乐锁版朏',
  PRIMARY KEY (id)
);


-- 修改点(2026-09-26)：每方法执行前清空模块表，消除跨方法残留导致的计数/主键冲突
DELETE FROM delivery_order;

-- ==================== 骑手结算（P0-1 增量2，2026-09-28）====================
-- 与会员余额解耦：骑手独立账户 / 收入流水（按 order_id 幂等）/ 提现申请

CREATE TABLE IF NOT EXISTS rider_account (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  rider_id bigint NULL DEFAULT NULL COMMENT '骑手id',
  withdrawable_balance decimal(12,2) NULL DEFAULT 0.00 COMMENT '可提现余额',
  frozen_balance decimal(12,2) NULL DEFAULT 0.00 COMMENT '冻结中金额',
  total_income decimal(12,2) NULL DEFAULT 0.00 COMMENT '累计收入',
  total_withdrawn decimal(12,2) NULL DEFAULT 0.00 COMMENT '累计已提现',
  version int NULL DEFAULT 0 COMMENT '乐观锁',
  create_time datetime NULL DEFAULT NULL,
  update_time datetime NULL DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE (rider_id, tenant_id)
);

CREATE TABLE IF NOT EXISTS rider_income_ledger (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  rider_id bigint NULL DEFAULT NULL COMMENT '骑手id',
  order_id bigint NULL DEFAULT NULL COMMENT '订单id（幂等键）',
  amount decimal(12,2) NULL DEFAULT NULL COMMENT '入账金额',
  status int NULL DEFAULT 1 COMMENT '状态',
  create_time datetime NULL DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE (order_id)
);

CREATE TABLE IF NOT EXISTS rider_withdrawal (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  rider_id bigint NULL DEFAULT NULL COMMENT '骑手id',
  amount decimal(12,2) NULL DEFAULT NULL COMMENT '提现金额',
  status varchar(20) NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  apply_time datetime NULL DEFAULT NULL COMMENT '申请时间',
  review_time datetime NULL DEFAULT NULL COMMENT '审核时间',
  reviewer_id bigint NULL DEFAULT NULL COMMENT '审核人id',
  reviewer_name varchar(50) NULL DEFAULT NULL COMMENT '审核人名称',
  remark varchar(255) NULL DEFAULT NULL COMMENT '备注/拒绝原因',
  version int NULL DEFAULT 0 COMMENT '乐观锁',
  create_time datetime NULL DEFAULT NULL,
  update_time datetime NULL DEFAULT NULL,
  PRIMARY KEY (id)
);

DELETE FROM rider_account;
DELETE FROM rider_income_ledger;
DELETE FROM rider_withdrawal;

-- ==================== 骑手评价（P0-2，2026-09-28）================
-- 顾客对骑手配送服务评分（1-5），每个订单对骑手仅一次有效评价，自动通过并即时计入骑手评分
CREATE TABLE IF NOT EXISTS rider_evaluation (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  order_id bigint NULL DEFAULT NULL COMMENT '订单id',
  user_id bigint NULL DEFAULT NULL COMMENT '评价用户id',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '评价用户名',
  rider_id bigint NULL DEFAULT NULL COMMENT '骑手id',
  rider_name varchar(50) NULL DEFAULT NULL COMMENT '骑手姓名',
  star_rating int NULL DEFAULT NULL COMMENT '评分1-5',
  content varchar(500) NULL DEFAULT NULL COMMENT '评价内容',
  tags varchar(255) NULL DEFAULT NULL COMMENT '标签JSON数组',
  anonymous int NULL DEFAULT 0 COMMENT '0实名1匿名',
  status int NULL DEFAULT 1 COMMENT '0待审1通过2拒绝',
  reply_content varchar(500) NULL DEFAULT NULL COMMENT '商家回复',
  reply_time datetime NULL DEFAULT NULL COMMENT '回复时间',
  create_time datetime NULL DEFAULT NULL,
  update_time datetime NULL DEFAULT NULL,
  create_user bigint NULL DEFAULT NULL,
  update_user bigint NULL DEFAULT NULL,
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE (order_id, rider_id, is_deleted)
);

DELETE FROM rider_evaluation;

-- ==================== 骑手异常工单（P0-3，2026-09-28）================
-- 骑手配送异常上报与转单改派：联系不上顾客/商品破损/地址有误/顾客拒收/申请转单/其他
CREATE TABLE IF NOT EXISTS rider_exception_order (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  order_id bigint NULL DEFAULT NULL COMMENT '关联订单id',
  order_number varchar(50) NULL DEFAULT NULL COMMENT '订单号',
  rider_id bigint NULL DEFAULT NULL COMMENT '上报骑手id',
  rider_name varchar(50) NULL DEFAULT NULL COMMENT '上报骑手姓名',
  exception_type int NULL DEFAULT NULL COMMENT '1联系不上顾客2商品破损3地址有误4顾客拒收5申请转单6其他',
  description varchar(500) NULL DEFAULT NULL COMMENT '异常描述',
  status int NULL DEFAULT 0 COMMENT '0待处理1已处理2已转单3已关闭',
  handle_note varchar(500) NULL DEFAULT NULL COMMENT '处理备注',
  handler_id bigint NULL DEFAULT NULL COMMENT '处理人id',
  handle_time datetime NULL DEFAULT NULL COMMENT '处理时间',
  create_time datetime NULL DEFAULT NULL,
  update_time datetime NULL DEFAULT NULL,
  create_user bigint NULL DEFAULT NULL,
  update_user bigint NULL DEFAULT NULL,
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_tenant_status (tenant_id, status),
  KEY idx_order (order_id)
);

DELETE FROM rider_exception_order;

-- ==================== 骑手消息中心（P0-4，2026-09-28）================
-- 骑手个人收件箱：派单提醒/催单提醒/异常处理结果/系统公告，支持未读数与标记已读
CREATE TABLE IF NOT EXISTS rider_message (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  rider_id bigint NULL DEFAULT NULL COMMENT '接收骑手id',
  type int NULL DEFAULT NULL COMMENT '1派单提醒2催单提醒3异常处理4系统公告5其他',
  title varchar(100) NULL DEFAULT NULL COMMENT '消息标题',
  content varchar(500) NULL DEFAULT NULL COMMENT '消息内容',
  biz_id bigint NULL DEFAULT NULL COMMENT '关联业务id（订单id）',
  is_read int NULL DEFAULT 0 COMMENT '0未读1已读',
  read_time datetime NULL DEFAULT NULL COMMENT '阅读时间',
  create_time datetime NULL DEFAULT NULL,
  update_time datetime NULL DEFAULT NULL,
  create_user bigint NULL DEFAULT NULL,
  update_user bigint NULL DEFAULT NULL,
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_rider_read (rider_id, is_read),
  KEY idx_tenant (tenant_id)
);

DELETE FROM rider_message;

