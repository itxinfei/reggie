-- 售后审核字段：审核人/审核时间/拒绝原因/退款完成时间
ALTER TABLE `refund_record`
    ADD COLUMN `audit_user_id` bigint DEFAULT NULL COMMENT '审核人ID' AFTER `apply_user_id`,
    ADD COLUMN `audit_time` datetime DEFAULT NULL COMMENT '审核时间' AFTER `audit_user_id`,
    ADD COLUMN `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因' AFTER `audit_time`,
    ADD COLUMN `refund_time` datetime DEFAULT NULL COMMENT '退款完成时间' AFTER `reject_reason`;

-- 修复 RefundStatus 枚举新增值对应的数据库注释
-- processing: 审核通过退款处理中
-- rejected: 审核拒绝
