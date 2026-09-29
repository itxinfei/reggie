-- P0-6 取餐码核销
-- 目的：为 orders 增加取餐码字段。派单/抢单时生成，骑手确认取餐时必须核销，
--       防止"未到店即点取餐"造成的配送时效数据失真。
-- 说明：本目录脚本为参考迁移，不会自动执行（项目无 Flyway），需 DBA 评审后手动执行。
-- 幂等：MySQL 原生不支持 ADD COLUMN IF NOT EXISTS，重复执行前请先确认列不存在。

ALTER TABLE orders
  ADD COLUMN pickup_code varchar(16) NULL DEFAULT NULL
  COMMENT '取餐码（派单/抢单时生成，骑手取餐须校验）'
  AFTER cancel_reason;
