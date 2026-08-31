-- 堂食管理 P1：orders 表新增 AA 分账相关列

ALTER TABLE `orders`
  ADD COLUMN `master_order_id` bigint NULL DEFAULT NULL COMMENT '父订单ID（AA分账时指向主订单）' AFTER `table_id`,
  ADD COLUMN `split_count` int NULL DEFAULT NULL COMMENT '分账份数（AA分账时记录拆分数量）' AFTER `master_order_id`;
