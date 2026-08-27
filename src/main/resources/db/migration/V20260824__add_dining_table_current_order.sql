-- Stage 4: 开台/转台/并台 — 为 dining_table 表添加 current_order_id 列
-- 用于记录开台后绑定的订单ID，支持转台时追踪订单归属

ALTER TABLE dining_table ADD COLUMN current_order_id bigint NULL DEFAULT NULL COMMENT '当前关联订单ID（开台后绑定）';
