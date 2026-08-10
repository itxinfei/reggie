-- Member module test schema (H2 compatible)

DROP TABLE IF EXISTS points_record;
DROP TABLE IF EXISTS recharge_record;
DROP TABLE IF EXISTS coupon_user;
DROP TABLE IF EXISTS coupon_template;
DROP TABLE IF EXISTS member;
DROP TABLE IF EXISTS member_level;

CREATE TABLE member_level (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  name varchar(50) NULL DEFAULT NULL COMMENT '等级名称',
  MIN_POINTS bigint NULL DEFAULT 0 COMMENT '最低积分要求',
  discount decimal(3,2) NULL DEFAULT 1.00 COMMENT '折扣率',
  sort int NULL DEFAULT NULL COMMENT '排序',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

CREATE TABLE member (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  user_id bigint NULL DEFAULT NULL COMMENT '关联用户ID',
  level_id bigint NULL DEFAULT NULL COMMENT '会员等级ID',
  name varchar(50) NULL DEFAULT NULL COMMENT '会员姓名',
  phone varchar(11) NULL DEFAULT NULL COMMENT '手机号',
  points bigint NULL DEFAULT 0 COMMENT '积分',
  balance decimal(10,2) NULL DEFAULT 0.00 COMMENT '余额',
  total_consumption decimal(10,2) NULL DEFAULT 0.00 COMMENT '累计消费金额',
  status int NULL DEFAULT 1 COMMENT '状态',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

CREATE TABLE coupon_template (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  name varchar(100) NULL DEFAULT NULL COMMENT '模板名称',
  type varchar(20) NULL DEFAULT NULL COMMENT '类型',
  condition_amount decimal(10,2) NULL DEFAULT NULL COMMENT '满减条件金额',
  discount_amount decimal(10,2) NULL DEFAULT NULL COMMENT '优惠金额',
  discount_rate decimal(3,2) NULL DEFAULT NULL COMMENT '折扣率',
  total_count int NULL DEFAULT 0 COMMENT '发放总数',
  remain_count int NULL DEFAULT 0 COMMENT '剩余数量',
  valid_days int NULL DEFAULT NULL COMMENT '有效天数',
  status int NULL DEFAULT 1 COMMENT '状态',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

CREATE TABLE coupon_user (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  member_id bigint NULL DEFAULT NULL COMMENT '会员ID',
  template_id bigint NULL DEFAULT NULL COMMENT '优惠券模板ID',
  code varchar(64) NULL DEFAULT NULL COMMENT '优惠券码',
  status varchar(20) NULL DEFAULT 'unused' COMMENT '状态',
  used_time datetime NULL DEFAULT NULL COMMENT '使用时间',
  order_id bigint NULL DEFAULT NULL COMMENT '使用订单ID',
  expire_time datetime NULL DEFAULT NULL COMMENT '过期时间',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);
-- 修改点：防重复领取唯一索引（与 reggie.sql uk_member_template 对齐）
CREATE UNIQUE INDEX uk_member_template ON coupon_user(member_id, template_id);

CREATE TABLE points_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  member_id bigint NULL DEFAULT NULL COMMENT '会员ID',
  type varchar(20) NULL DEFAULT NULL COMMENT '类型',
  points int NULL DEFAULT NULL COMMENT '积分数量',
  biz_type varchar(50) NULL DEFAULT NULL COMMENT '关联业务类型',
  biz_id bigint NULL DEFAULT NULL COMMENT '关联业务ID',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

CREATE TABLE recharge_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  member_id bigint NULL DEFAULT NULL COMMENT '会员ID',
  amount decimal(10,2) NULL DEFAULT NULL COMMENT '充值金额',
  gift_amount decimal(10,2) NULL DEFAULT 0.00 COMMENT '赠送金额',
  payment_method varchar(20) NULL DEFAULT NULL COMMENT '支付方式',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);
