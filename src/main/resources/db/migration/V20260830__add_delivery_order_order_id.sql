-- V20260830__add_delivery_order_order_id
-- 为配送订单表补充本地订单外键 order_id（关联 orders.id），作为外卖平台订单与本地订单对账的挂载点。
-- 背景：delivery_order 记录外卖平台（美团/饿了么/抖音）订单，本地 orders 记录自营订单，
-- 两套体系来源不同。order_id 设计为可空，平台订单未打通本地时保持 NULL，不强制外键约束，
-- 待"平台订单转本地订单"链路落地后回填。
ALTER TABLE `delivery_order` ADD COLUMN `order_id` bigint NULL DEFAULT NULL COMMENT '本地订单ID（关联 orders.id，可空）' AFTER `platform`;
CREATE INDEX `idx_delivery_order_order_id` ON `delivery_order` (`order_id`);
