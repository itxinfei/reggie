-- 进销存/供应链模块
-- 瑞吉外卖商业功能补充 - Phase 3

DROP TABLE IF EXISTS `stock_record`;
DROP TABLE IF EXISTS `stock_check`;
DROP TABLE IF EXISTS `purchase_order_detail`;
DROP TABLE IF EXISTS `purchase_order`;
DROP TABLE IF EXISTS `material`;
DROP TABLE IF EXISTS `material_category`;
DROP TABLE IF EXISTS `supplier`;

CREATE TABLE `supplier` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(100) COLLATE utf8_bin NOT NULL COMMENT '供应商名称',
    `contact` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '联系人',
    `phone` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '联系电话',
    `address` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '地址',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1启用',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='供应商';

CREATE TABLE `material_category` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '分类名称',
    `sort` int(11) DEFAULT '0' COMMENT '排序',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='食材分类';

CREATE TABLE `material` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `category_id` bigint(20) DEFAULT NULL COMMENT '分类id',
    `name` varchar(100) COLLATE utf8_bin NOT NULL COMMENT '食材名称',
    `unit` varchar(10) COLLATE utf8_bin NOT NULL COMMENT '单位 斤/个/包/箱',
    `stock_qty` decimal(10,2) DEFAULT '0.00' COMMENT '当前库存',
    `min_stock` decimal(10,2) DEFAULT '0.00' COMMENT '最低库存预警',
    `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
    `supplier_id` bigint(20) DEFAULT NULL COMMENT '默认供应商',
    `barcode` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '条码',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1启用',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='食材';

CREATE TABLE `stock_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `material_id` bigint(20) NOT NULL COMMENT '食材id',
    `type` varchar(10) COLLATE utf8_bin NOT NULL COMMENT '类型 IN入库/OUT出库/CHECK盘点',
    `qty` decimal(10,2) NOT NULL COMMENT '数量',
    `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
    `total_amount` decimal(10,2) DEFAULT NULL COMMENT '总金额',
    `biz_id` bigint(20) DEFAULT NULL COMMENT '关联业务id 采购单/订单',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `operator` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '操作人',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_material` (`material_id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='出入库记录';

CREATE TABLE `stock_check` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `check_no` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '盘点单号',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/IN_PROGRESS/DONE',
    `total_diff_amount` decimal(10,2) DEFAULT NULL COMMENT '盈亏总金额',
    `operator` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '操作人',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='盘点单';

CREATE TABLE `purchase_order` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `order_no` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '采购单号',
    `supplier_id` bigint(20) DEFAULT NULL COMMENT '供应商',
    `total_amount` decimal(10,2) DEFAULT NULL COMMENT '总金额',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/ORDERED/PARTIAL/RECEIVED/CANCELLED',
    `operator` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '操作人',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='采购单';

CREATE TABLE `purchase_order_detail` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `purchase_order_id` bigint(20) NOT NULL COMMENT '采购单id',
    `material_id` bigint(20) NOT NULL COMMENT '食材id',
    `qty` decimal(10,2) NOT NULL COMMENT '数量',
    `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
    `amount` decimal(10,2) DEFAULT NULL COMMENT '金额',
    `received_qty` decimal(10,2) DEFAULT '0.00' COMMENT '已收货数量',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_purchase` (`purchase_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='采购单明细';
