-- 为 orders 表新增 used_coupon_id 列（本单使用的用户优惠券记录ID）
-- 背景：Orders 实体已有 @TableField("used_coupon_id")，收银核销/会员权益回退会读写该字段，
-- 但历史建表脚本与本地库均缺失此列，导致订单超时/库存退款定时任务查询报
-- "Unknown column 'used_coupon_id'"。
-- 已有数据默认 NULL（未使用优惠券），不破坏历史订单。
ALTER TABLE `orders`
ADD COLUMN `used_coupon_id` bigint NULL DEFAULT NULL COMMENT '本单使用的优惠券ID（用户优惠券记录ID），未使用为 NULL' AFTER `stock_refunded`;
