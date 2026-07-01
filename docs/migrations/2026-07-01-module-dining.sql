-- 堂食管理模块
-- 瑞吉外卖商业功能补充 - Phase 2

DROP TABLE IF EXISTS `dining_queue`;
DROP TABLE IF EXISTS `dining_reservation`;
DROP TABLE IF EXISTS `dining_table`;
DROP TABLE IF EXISTS `dining_area`;

CREATE TABLE `dining_area` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '区域名称',
    `sort` int(11) DEFAULT '0' COMMENT '排序',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='堂食区域';

CREATE TABLE `dining_table` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `area_id` bigint(20) DEFAULT NULL COMMENT '区域id',
    `name` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '桌号/桌名',
    `seat_count` int(11) DEFAULT '4' COMMENT '座位数',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'FREE' COMMENT '状态 FREE/OCCUPIED/RESERVED/CLEANING',
    `min_amount` decimal(10,2) DEFAULT NULL COMMENT '最低消费',
    `qr_code_url` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '桌边点餐二维码',
    `sort` int(11) DEFAULT '0' COMMENT '排序',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_area` (`area_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='堂食桌台';

CREATE TABLE `dining_queue` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `queue_no` varchar(10) COLLATE utf8_bin NOT NULL COMMENT '排队号 A001',
    `phone` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '手机号',
    `seat_count` int(11) DEFAULT NULL COMMENT '人数',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'WAITING' COMMENT '状态 WAITING/CALLED/CANCELLED/SERVED',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='排队记录';

CREATE TABLE `dining_reservation` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `table_id` bigint(20) DEFAULT NULL COMMENT '桌台id',
    `customer_name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '顾客姓名',
    `phone` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '联系电话',
    `reserved_time` datetime NOT NULL COMMENT '预订时间',
    `seat_count` int(11) DEFAULT NULL COMMENT '人数',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/CONFIRMED/CANCELLED/ARRIVED',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_table` (`table_id`),
    KEY `idx_time` (`reserved_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='预订记录';
