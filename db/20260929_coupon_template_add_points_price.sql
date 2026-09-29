-- 积分兑换优惠券：coupon_template 增加积分价字段
-- points_price > 0 表示该券支持积分兑换；NULL/0 仅支持普通领取
ALTER TABLE `coupon_template`
    ADD COLUMN `points_price` int NULL DEFAULT NULL COMMENT '积分兑换所需积分' AFTER `valid_days`;
