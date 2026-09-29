-- ============================================================
-- 骑手 P0 闭环建表脚本（2026-09-28）
-- 对应修复计划：P0-1 收入结算 / P0-2 评价 / P0-3 异常工单 / P0-4 消息中心
-- 仅新增表，不改动既有表；与 src/test/resources/schema-delivery.sql 保持同步
-- 执行：mysql -uroot -p reggie < db/20260928_rider_p0_tables.sql
-- ============================================================

-- P0-1 骑手独立账户（与会员余额解耦）
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
  UNIQUE KEY uk_rider_tenant (rider_id, tenant_id)
) COMMENT='骑手结算账户';

-- P0-1 收入流水（按 order_id 幂等）
CREATE TABLE IF NOT EXISTS rider_income_ledger (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  rider_id bigint NULL DEFAULT NULL COMMENT '骑手id',
  order_id bigint NULL DEFAULT NULL COMMENT '订单id（幂等键）',
  amount decimal(12,2) NULL DEFAULT NULL COMMENT '入账金额',
  status int NULL DEFAULT 1 COMMENT '状态',
  create_time datetime NULL DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order (order_id)
) COMMENT='骑手收入流水';

-- P0-1 提现申请
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
) COMMENT='骑手提现申请';

-- P0-2 顾客对骑手的配送服务评价（每订单对每骑手仅一次，自动通过即时计分）
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
  UNIQUE KEY uk_order_rider_del (order_id, rider_id, is_deleted)
) COMMENT='骑手配送服务评价';

-- P0-3 骑手配送异常工单与转单
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
) COMMENT='骑手异常工单';

-- P0-4 骑手个人消息收件箱
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
) COMMENT='骑手消息';
