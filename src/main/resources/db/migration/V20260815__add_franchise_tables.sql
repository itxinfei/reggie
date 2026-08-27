-- ==================== 加盟分账/抽成模块（P0 缺口补齐） ====================
-- 背景：产品定位为连锁加盟品牌，总部统一运营、加盟商分账/抽成在系统内做；
-- 但此前无加盟商独立实体/合同/抽成规则/分账结算模块，本次补齐。
-- 关联：store_info.store_type=3(加盟店)、store_info.parent_tenant_id(上级总店)

-- 加盟商表
CREATE TABLE IF NOT EXISTS `franchisee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '加盟商ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '所属总部租户ID',
  `name` varchar(100) NOT NULL COMMENT '加盟商名称',
  `contact_person` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) DEFAULT NULL COMMENT '联系地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=禁用，1=启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_franchisee_tenant` (`tenant_id`),
  KEY `idx_franchisee_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加盟商';

-- 加盟合同表（含抽成规则）
CREATE TABLE IF NOT EXISTS `franchise_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '所属总部租户ID',
  `franchisee_id` bigint NOT NULL COMMENT '加盟商ID',
  `store_tenant_id` bigint DEFAULT NULL COMMENT '加盟门店租户ID（关联 store_info.tenant_id，store_type=3）',
  `contract_no` varchar(50) NOT NULL COMMENT '合同编号',
  `start_date` date DEFAULT NULL COMMENT '合同开始日期',
  `end_date` date DEFAULT NULL COMMENT '合同结束日期',
  `commission_type` tinyint NOT NULL DEFAULT '1' COMMENT '抽成方式：1=按营业额比例，2=固定金额/周期',
  `commission_rate` decimal(6,4) DEFAULT NULL COMMENT '抽成比例（commission_type=1时使用，如0.0500=5%）',
  `commission_amount` decimal(12,2) DEFAULT NULL COMMENT '固定抽成金额/周期（commission_type=2时使用）',
  `settle_cycle` tinyint NOT NULL DEFAULT '1' COMMENT '结算周期：1=月结',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '合同状态：0=已终止，1=生效中',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_contract_franchisee` (`franchisee_id`),
  KEY `idx_contract_store` (`store_tenant_id`),
  KEY `idx_contract_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加盟合同（含抽成规则）';

-- 分账结算单表
CREATE TABLE IF NOT EXISTS `franchise_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结算单ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '所属总部租户ID',
  `contract_id` bigint NOT NULL COMMENT '加盟合同ID',
  `franchisee_id` bigint NOT NULL COMMENT '加盟商ID',
  `store_tenant_id` bigint DEFAULT NULL COMMENT '加盟门店租户ID',
  `settle_period` varchar(10) NOT NULL COMMENT '结算周期，如 2026-08',
  `order_count` int NOT NULL DEFAULT '0' COMMENT '周期内已完成订单数',
  `sales_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '周期营业额',
  `commission_type` tinyint NOT NULL DEFAULT '1' COMMENT '抽成方式（冗余合同快照）',
  `commission_rate` decimal(6,4) DEFAULT NULL COMMENT '抽成比例（冗余合同快照）',
  `commission_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '应抽成金额',
  `settle_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '加盟商应结算金额（营业额-抽成）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=待确认，1=已确认，2=已结算',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `settle_time` datetime DEFAULT NULL COMMENT '结算时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settle_contract_period` (`contract_id`, `settle_period`),
  KEY `idx_settle_franchisee` (`franchisee_id`),
  KEY `idx_settle_store` (`store_tenant_id`),
  KEY `idx_settle_period` (`settle_period`),
  KEY `idx_settle_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加盟分账结算单';
