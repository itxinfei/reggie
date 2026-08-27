-- 菜品规格模块表结构

-- 菜品规格组表
CREATE TABLE IF NOT EXISTS `dish_spec_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '规格组名称',
  `type` tinyint DEFAULT '1' COMMENT '类型：1-单选，2-多选',
  `required` tinyint DEFAULT '0' COMMENT '是否必选：0-否，1-是',
  `max_select` int DEFAULT NULL COMMENT '最大可选数量',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品规格组表';

-- 菜品规格选项表
CREATE TABLE IF NOT EXISTS `dish_spec_option` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` bigint NOT NULL COMMENT '规格组ID',
  `name` varchar(50) NOT NULL COMMENT '选项名称',
  `price_adjust` decimal(10,2) DEFAULT '0.00' COMMENT '价格调整',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品规格选项表';

-- 菜品规格关联表
CREATE TABLE IF NOT EXISTS `dish_spec_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `group_id` bigint NOT NULL COMMENT '规格组ID',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_group` (`dish_id`, `group_id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品规格关联表';

-- 为订单明细表添加备注字段
ALTER TABLE `order_detail` ADD COLUMN `remark` varchar(500) DEFAULT NULL COMMENT '菜品备注' AFTER `dish_flavor`;
