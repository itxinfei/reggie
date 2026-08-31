-- V20260829__add_version_for_financial_entities
-- 为会员、日结、成本记录、采购订单四个高并发财务实体添加乐观锁 version 列
-- 扩展 P3-8 的 @Version 覆盖范围：从 4 张表扩至 8 张表
-- 与 ConcurrentHashMap 幂等锁配合，双重防御并发修改

-- 会员表：保护 balance/points/totalConsumption 并发充值/扣款/积分变动
ALTER TABLE `member` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- 日结表：保护 status/settlementTime/各项金额字段的并发日结操作
ALTER TABLE `daily_settlement` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- 成本记录表：保护 amount 字段的并发记账操作，防止重复记账金额漂移
ALTER TABLE `cost_record` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- 采购订单表：保护 status 状态机与 totalAmount 金额的并发审核/取消/完成操作
ALTER TABLE `purchase_order` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;
