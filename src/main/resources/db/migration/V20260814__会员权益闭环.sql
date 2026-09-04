-- 会员权益闭环：订单记录使用的优惠券
-- 说明：本地手动执行（项目约定 db/migration 仅作参考脚本，不自动迁移）
-- 作用：orders.used_coupon_id 记录本单使用的优惠券（用户优惠券记录ID），供成交后核销

ALTER TABLE `orders`
    ADD COLUMN `used_coupon_id` BIGINT NULL DEFAULT NULL COMMENT '本单使用的优惠券ID（用户优惠券记录ID）'
    AFTER `stock_refunded`;
