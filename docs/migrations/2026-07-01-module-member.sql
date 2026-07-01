-- 会员营销模块
-- 瑞吉外卖商业功能补充 - Phase 1

DROP TABLE IF EXISTS `coupon_user`;
DROP TABLE IF EXISTS `coupon_template`;
DROP TABLE IF EXISTS `recharge_record`;
DROP TABLE IF EXISTS `points_record`;
DROP TABLE IF EXISTS `member`;

CREATE TABLE `member` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `user_id` bigint(20) DEFAULT NULL COMMENT '关联用户id',
    `level_id` bigint(20) DEFAULT NULL COMMENT '会员等级',
    `name` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '姓名',
    `phone` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '手机号',
    `points` bigint(20) DEFAULT '0' COMMENT '积分',
    `balance` decimal(10,2) DEFAULT '0.00' COMMENT '储值余额',
    `total_consumption` decimal(10,2) DEFAULT '0.00' COMMENT '累计消费',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1正常',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='会员';

CREATE TABLE `member_level` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '等级名称',
    `min_points` bigint(20) DEFAULT '0' COMMENT '所需积分',
    `discount` decimal(3,2) DEFAULT '1.00' COMMENT '折扣 0.95=95折',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='会员等级';

CREATE TABLE `points_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id` bigint(20) NOT NULL COMMENT '会员id',
    `type` varchar(10) COLLATE utf8_bin NOT NULL COMMENT '类型 IN增加/OUT消耗',
    `points` int(11) NOT NULL COMMENT '积分变动',
    `biz_type` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '业务类型 CONSUME/SIGN/REFUND',
    `biz_id` bigint(20) DEFAULT NULL COMMENT '关联业务id',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='积分记录';

CREATE TABLE `recharge_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id` bigint(20) NOT NULL COMMENT '会员id',
    `amount` decimal(10,2) NOT NULL COMMENT '充值金额',
    `gift_amount` decimal(10,2) DEFAULT '0.00' COMMENT '赠送金额',
    `payment_method` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '支付方式',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='充值记录';

CREATE TABLE `coupon_template` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(100) COLLATE utf8_bin NOT NULL COMMENT '优惠券名称',
    `type` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '类型 FULL_REDUCE满减/DISCOUNT折扣/NEW_USER新客',
    `condition_amount` decimal(10,2) DEFAULT NULL COMMENT '满减条件 满多少',
    `discount_amount` decimal(10,2) DEFAULT NULL COMMENT '减免金额',
    `discount_rate` decimal(3,2) DEFAULT NULL COMMENT '折扣率',
    `total_count` int(11) DEFAULT '0' COMMENT '发行总量',
    `remain_count` int(11) DEFAULT '0' COMMENT '剩余数量',
    `valid_days` int(11) DEFAULT NULL COMMENT '有效天数',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1启用',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='优惠券模板';

CREATE TABLE `coupon_user` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id` bigint(20) NOT NULL COMMENT '会员id',
    `template_id` bigint(20) NOT NULL COMMENT '模板id',
    `code` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '券码',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'UNUSED' COMMENT '状态 UNUSED/USED/EXPIRED',
    `used_time` datetime DEFAULT NULL COMMENT '使用时间',
    `order_id` bigint(20) DEFAULT NULL COMMENT '使用订单',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    PRIMARY KEY (`id`),
    KEY `idx_member` (`member_id`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='用户优惠券';
