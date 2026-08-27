-- 营销活动模块表结构

-- 满减规则表
CREATE TABLE IF NOT EXISTS `full_reduction_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `campaign_id` bigint NOT NULL COMMENT '活动ID',
  `rule_name` varchar(100) DEFAULT NULL COMMENT '规则名称',
  `discount_type` tinyint NOT NULL COMMENT '优惠类型：1-减固定金额，2-打折，3-赠品',
  `min_amount` decimal(10,2) NOT NULL COMMENT '满多少金额',
  `discount_value` decimal(10,2) NOT NULL COMMENT '优惠值（减金额/折扣率）',
  `max_discount_amount` decimal(10,2) DEFAULT NULL COMMENT '最大优惠金额',
  `gift_dish_id` bigint DEFAULT NULL COMMENT '赠品菜品ID',
  `gift_quantity` int DEFAULT NULL COMMENT '赠品数量',
  `stackable` tinyint DEFAULT '0' COMMENT '是否可叠加：0-否，1-是',
  `daily_limit` int DEFAULT NULL COMMENT '每日限用次数',
  `per_user_limit` int DEFAULT NULL COMMENT '每人限用次数',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_campaign_id` (`campaign_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减规则表';

-- 折扣规则表
CREATE TABLE IF NOT EXISTS `discount_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `campaign_id` bigint NOT NULL COMMENT '活动ID',
  `rule_name` varchar(100) DEFAULT NULL COMMENT '规则名称',
  `scope` tinyint NOT NULL COMMENT '折扣范围：1-全场，2-指定分类，3-指定菜品，4-指定套餐',
  `discount_rate` decimal(5,4) NOT NULL COMMENT '折扣率（如8折传0.8）',
  `max_discount_amount` decimal(10,2) DEFAULT NULL COMMENT '最大优惠金额',
  `min_consumption` decimal(10,2) DEFAULT NULL COMMENT '最低消费金额',
  `category_id` bigint DEFAULT NULL COMMENT '适用分类ID',
  `dish_id` bigint DEFAULT NULL COMMENT '适用菜品ID',
  `setmeal_id` bigint DEFAULT NULL COMMENT '适用套餐ID',
  `daily_limit` int DEFAULT NULL COMMENT '每日限用次数',
  `per_user_limit` int DEFAULT NULL COMMENT '每人限用次数',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_campaign_id` (`campaign_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='折扣规则表';

-- 营销活动使用记录表
CREATE TABLE IF NOT EXISTS `campaign_usage_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `campaign_id` bigint NOT NULL COMMENT '活动ID',
  `rule_id` bigint DEFAULT NULL COMMENT '规则ID',
  `rule_type` tinyint DEFAULT NULL COMMENT '规则类型：1-满减，2-折扣',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `order_number` varchar(50) DEFAULT NULL COMMENT '订单号',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `order_amount` decimal(10,2) DEFAULT NULL COMMENT '订单金额',
  `discount_amount` decimal(10,2) DEFAULT NULL COMMENT '优惠金额',
  `actual_amount` decimal(10,2) DEFAULT NULL COMMENT '实付金额',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_campaign_id` (`campaign_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_use_time` (`use_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动使用记录表';
