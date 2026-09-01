-- 外卖售后申请：refund_record 扩展用户端售后字段
-- 背景：用户端"申请售后"需直接关联订单（而非支付单），并记录售后类型与申请人
--   1. order_id：业务订单ID，用户端视角的售后申请锚点（兼容历史 NULL，渠道退款仍用 payment_order_id）
--   2. refund_type：售后类型 1=整单退款 2=部分退款（历史记录默认 NULL，按整单退款语义兼容）
--   3. apply_user_id：申请人ID（用户端售后申请记录发起人，渠道回调退款可为 NULL）
-- 已有数据：三列默认 NULL，历史退款记录不受影响；新售后申请由 OrderController.userApplyRefund 写入。

ALTER TABLE `refund_record`
  ADD COLUMN `order_id` bigint NULL DEFAULT NULL COMMENT '业务订单ID' AFTER `payment_order_id`,
  ADD COLUMN `refund_type` int NULL DEFAULT NULL COMMENT '售后类型：1=整单退款 2=部分退款' AFTER `status`,
  ADD COLUMN `apply_user_id` bigint NULL DEFAULT NULL COMMENT '申请人ID' AFTER `refund_type`;
