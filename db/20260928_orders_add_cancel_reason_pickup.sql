-- ============================================================
-- orders 表补齐两列（2026-09-28）
-- P0-5 cancel_reason 取消/拒单回执（顾客端可见，不再覆盖 remark）
-- P0-6 pickup_code 取餐码（派单/抢单生成，骑手取餐须核销）
-- 与 src/test/resources/schema.sql 第 173-174 行保持同步
-- MySQL 不支持 ADD COLUMN IF NOT EXISTS，执行前确认列不存在
-- ============================================================

ALTER TABLE orders
  ADD COLUMN cancel_reason varchar(255) NULL DEFAULT NULL
  COMMENT '取消/拒单原因（P0-5 回执，顾客端可见；不再覆盖 remark）'
  AFTER internal_remark,
  ADD COLUMN pickup_code varchar(16) NULL DEFAULT NULL
  COMMENT '取餐码（派单/抢单时生成，骑手取餐须校验）'
  AFTER cancel_reason;
