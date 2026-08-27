-- 配送增强模块表结构

-- 配送范围规则表
CREATE TABLE IF NOT EXISTS `delivery_range_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `range_type` tinyint NOT NULL COMMENT '范围类型：1-圆形，2-多边形',
  `center_longitude` decimal(12,8) DEFAULT NULL COMMENT '中心经度',
  `center_latitude` decimal(12,8) DEFAULT NULL COMMENT '中心纬度',
  `radius` decimal(10,2) DEFAULT NULL COMMENT '半径（米）',
  `polygon_points` text DEFAULT NULL COMMENT '多边形坐标点JSON',
  `fee_type` tinyint DEFAULT '1' COMMENT '配送费类型：1-固定，2-距离阶梯，3-基础+距离',
  `base_fee` decimal(10,2) DEFAULT '0.00' COMMENT '基础配送费',
  `fee_per_km` decimal(10,2) DEFAULT NULL COMMENT '每公里配送费',
  `min_fee` decimal(10,2) DEFAULT NULL COMMENT '最低配送费',
  `max_fee` decimal(10,2) DEFAULT NULL COMMENT '最高配送费',
  `free_threshold` decimal(10,2) DEFAULT NULL COMMENT '免费配送金额门槛',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送范围规则表';

-- 配送费阶梯规则表
CREATE TABLE IF NOT EXISTS `delivery_fee_step` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `start_distance` decimal(10,2) NOT NULL COMMENT '起始距离（米）',
  `end_distance` decimal(10,2) NOT NULL COMMENT '结束距离（米）',
  `fee` decimal(10,2) NOT NULL COMMENT '配送费',
  `increment_distance` decimal(10,2) DEFAULT NULL COMMENT '每增加距离（米）',
  `increment_fee` decimal(10,2) DEFAULT NULL COMMENT '增加费用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送费阶梯规则表';
