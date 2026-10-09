-- ============================================================
-- 餐补模块表（企业内部订餐：餐补账户 + 餐补流水，2026-10-06）
-- 按模块约定拆分至 schema-subsidy.sql（AGENTS.md：模块表进 schema-<module>.sql）。
-- ============================================================

-- ==================== 餐补账户表 ====================
CREATE TABLE IF NOT EXISTS meal_subsidy_account (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NOT NULL COMMENT '租户ID',
  user_id bigint NOT NULL COMMENT '顾客用户ID（C端下单人）',
  department_id bigint NULL DEFAULT NULL COMMENT '部门ID（发放时的归属部门快照）',
  balance decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '当前余额',
  total_granted decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计发放',
  total_used decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计核销',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0:冻结 1:正常',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_subsidy_tenant_user (tenant_id, user_id)
);

-- ==================== 餐补流水表 ====================
CREATE TABLE IF NOT EXISTS meal_subsidy_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NOT NULL COMMENT '租户ID',
  user_id bigint NOT NULL COMMENT '顾客用户ID',
  account_id bigint NULL DEFAULT NULL COMMENT '账户ID',
  record_type varchar(16) NOT NULL COMMENT '流水类型 GRANT:发放 CONSUME:核销 REFUND:回充 ADJUST:调整',
  amount decimal(10,2) NOT NULL COMMENT '发生金额（正向）',
  balance_after decimal(10,2) NOT NULL COMMENT '交易后余额',
  order_id bigint NULL DEFAULT NULL COMMENT '关联订单ID（核销/回充）',
  trade_no varchar(64) NULL DEFAULT NULL COMMENT '关联交易号（支付单tradeNo/退款单refundNo），幂等键',
  operator_id bigint NULL DEFAULT NULL COMMENT '操作人（管理员发放时）',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  create_time datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_subsidy_record_trade (trade_no, record_type),
  KEY idx_subsidy_record_user (tenant_id, user_id, create_time),
  KEY idx_subsidy_record_order (order_id)
);
