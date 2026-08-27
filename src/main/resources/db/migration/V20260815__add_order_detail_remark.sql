-- 为 order_detail 表新增 remark 列（订单明细备注）
-- 背景：OrderDetail 实体已有 remark 字段，但历史建表脚本与本地库均缺失此列，
-- 导致订单查询/下单保存明细时报 "Unknown column 'remark'"。
-- 已有数据默认 NULL，不影响历史订单。
ALTER TABLE `order_detail`
ADD COLUMN `remark` varchar(255) NULL DEFAULT NULL COMMENT '订单明细备注' AFTER `amount`;
