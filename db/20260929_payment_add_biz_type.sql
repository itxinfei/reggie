-- ============================================================
-- payment_order 表补齐 biz_type 列（2026-09-29）
-- P1-4 会员充值线上化：支付单需区分业务类型
--   ORDER    普通外卖/堂食订单（默认，order_id 指向 orders.id）
--   RECHARGE 会员充值（order_id 指向 recharge_record.id）
-- 支付成功回调 handlePaymentSuccess 按 biz_type 分别联动订单 / 充值入账。
-- 与 src/test/resources/schema-payment.sql、schema-payment-controller.sql 保持同步
-- MySQL 不支持 ADD COLUMN IF NOT EXISTS，执行前确认列不存在
-- ============================================================

ALTER TABLE payment_order
  ADD COLUMN biz_type varchar(20) NOT NULL DEFAULT 'ORDER'
  COMMENT '业务类型 ORDER=订单 / RECHARGE=会员充值'
  AFTER order_id;
