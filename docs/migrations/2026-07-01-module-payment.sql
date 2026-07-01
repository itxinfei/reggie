-- 聚合支付模块
-- 瑞吉外卖商业功能补充 - Phase 2

DROP TABLE IF EXISTS `refund_record`;
DROP TABLE IF EXISTS `payment_order`;

CREATE TABLE `payment_order` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` bigint(20) NOT NULL COMMENT '业务订单id',
    `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户id',
    `trade_no` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '系统交易号',
    `channel_trade_no` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '通道交易号',
    `channel` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '支付通道 ALIPAY/WECHAT/UNIONPAY',
    `amount` decimal(10,2) NOT NULL COMMENT '金额',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL/REFUND',
    `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
    `notify_time` datetime DEFAULT NULL COMMENT '回调时间',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_no` (`trade_no`),
    KEY `idx_order` (`order_id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_channel_trade` (`channel_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='支付订单';

CREATE TABLE `refund_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `payment_order_id` bigint(20) NOT NULL COMMENT '支付订单id',
    `refund_no` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '退款单号',
    `amount` decimal(10,2) NOT NULL COMMENT '退款金额',
    `reason` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '退款原因',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_payment` (`payment_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='退款记录';
