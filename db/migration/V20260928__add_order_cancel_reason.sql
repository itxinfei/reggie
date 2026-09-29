-- P0-5 三端语义落地与取消/退款回执
-- 目的：为 orders 增加独立的取消原因字段，取消/拒单时写入该字段，
--       不再覆盖顾客下单备注 remark（此前 cancelOrder 会把原因写进 remark，导致备注丢失）。
-- 说明：本目录脚本为参考迁移，不会自动执行（项目无 Flyway），需 DBA 评审后手动执行。
-- 幂等：MySQL 原生不支持 ADD COLUMN IF NOT EXISTS，重复执行前请先确认列不存在。

ALTER TABLE orders
  ADD COLUMN cancel_reason varchar(255) NULL DEFAULT NULL
  COMMENT '取消/拒单原因（顾客端可见回执；不覆盖 remark）'
  AFTER internal_remark;
