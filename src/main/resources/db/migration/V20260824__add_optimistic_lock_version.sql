-- V20260824__add_optimistic_lock_version
-- 为高风险并发操作表添加乐观锁 version 列，配合实体类 @Version 注解实现 CAS 更新
-- 修复 P3-8：关键表缺少 @Version 乐观锁，并发更新可能导致后写覆盖先写

-- 订单表：支付状态流转、取消订单等高频并发操作
ALTER TABLE `orders` ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐锁版朏' AFTER `is_deleted`;

-- 支付订单表：退款等并发操作（已有 SELECT FOR UPDATE，version 作为补充防护）
ALTER TABLE `payment_order` ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT 'ֹ汾' AFTER `is_deleted`;

-- 退款记录表：防止重复退款
ALTER TABLE `refund_record` ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT 'ֹ汾' AFTER `is_deleted`;

-- 配送订单表：接单、状态流转等并发操作
ALTER TABLE `delivery_order` ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐锁版朏' AFTER `is_deleted`;
