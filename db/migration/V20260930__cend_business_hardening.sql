-- V20260930: C 端业务加固（全量 C 端审查后的数据层兜底）
-- 注意：本脚本需手动执行（项目无 Flyway 自动迁移）。

-- 1) 排队号唯一性兜底：应用层已改为全局分布式锁 + 租户内序列（原锁按 seatCount 分片，
--    不同桌型并发取号会产生重复排队号，叫号凭证失效）。唯一索引防御残余竞态。
--    如历史数据存在 (tenant_id, queue_no) 重复行，需先手工清理再执行本语句，否则索引创建失败。
ALTER TABLE dining_queue ADD UNIQUE KEY uk_queue_tenant_no (tenant_id, queue_no);

-- 说明：shopping_cart 未加 (user_id, dish_id, dish_flavor, setmeal_id) 唯一索引——
-- MySQL 唯一索引对 NULL 不去重，而 setmeal_id/dish_flavor 可空，索引兜底不可靠；
-- 已改为应用层自愈（加购时合并历史重复行、减购容忍多行），见 ShoppingCartController。
