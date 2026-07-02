-- 打印小票模块
-- 瑞吉外卖商业功能补充 - Phase 1

DROP TABLE IF EXISTS `printer_log`;
DROP TABLE IF EXISTS `printer_config`;

CREATE TABLE `printer_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `store_id` bigint(20) DEFAULT NULL COMMENT '门店id',
    `name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '打印机名称',
    `type` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '连接类型 USB/TCP/CLOUD/BLUETOOTH',
    `brand` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '品牌 佳博/芯烨/商米',
    `device_id` varchar(100) COLLATE utf8_bin DEFAULT NULL COMMENT '设备标识 MAC/SN',
    `ip_address` varchar(15) COLLATE utf8_bin DEFAULT NULL COMMENT 'IP地址',
    `port` int(11) DEFAULT NULL COMMENT '端口',
    `paper_size` varchar(10) COLLATE utf8_bin DEFAULT '58mm' COMMENT '纸张规格 58mm/80mm',
    `print_type` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '打印类型 BILL/KITCHEN/DELIVERY',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1启用',
    `sort` int(11) DEFAULT '0' COMMENT '排序',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='打印机配置';

CREATE TABLE `printer_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` bigint(20) DEFAULT NULL COMMENT '订单id',
    `print_type` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '打印类型',
    `printer_id` bigint(20) DEFAULT NULL COMMENT '打印机id',
    `content` text COLLATE utf8_bin COMMENT '打印内容',
    `status` int(11) DEFAULT '0' COMMENT '状态 0失败 1成功',
    `error_msg` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '错误信息',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_printer` (`printer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='打印日志';
