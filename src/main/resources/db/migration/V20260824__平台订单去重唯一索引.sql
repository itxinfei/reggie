-- 平台订单去重：同一租户下同一平台类型的平台订单号唯一，防止定时拉单重复落库
-- 说明：本地手动执行（项目约定 db/migration 仅作参考脚本，不自动迁移）
-- 注意：仅对 platform_order_id 非空的平台订单生效；历史空值会被忽略（NULL 不违反唯一索引）

ALTER TABLE `orders`
    ADD UNIQUE KEY `uq_orders_platform` (`tenant_id`, `platform_type`, `platform_order_id`);
