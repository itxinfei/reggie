-- 成本核算模块表结构

-- 菜品成本表
CREATE TABLE IF NOT EXISTS `dish_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `dish_name` varchar(100) NOT NULL COMMENT '菜品名称',
  `material_cost` decimal(10,2) DEFAULT '0.00' COMMENT '食材成本',
  `labor_cost` decimal(10,2) DEFAULT '0.00' COMMENT '人工成本',
  `other_cost` decimal(10,2) DEFAULT '0.00' COMMENT '其他成本',
  `total_cost` decimal(10,2) DEFAULT '0.00' COMMENT '总成本',
  `sale_price` decimal(10,2) DEFAULT '0.00' COMMENT '售价',
  `profit_rate` decimal(5,2) DEFAULT '0.00' COMMENT '毛利率',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品成本表';

-- 成本记录表
CREATE TABLE IF NOT EXISTS `cost_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cost_type` tinyint NOT NULL COMMENT '成本类型：1-食材成本，2-人工成本，3-其他成本',
  `ref_id` bigint DEFAULT NULL COMMENT '关联ID（菜品ID/员工ID等）',
  `ref_name` varchar(100) DEFAULT NULL COMMENT '关联名称',
  `amount` decimal(10,2) NOT NULL COMMENT '成本金额',
  `cost_date` datetime NOT NULL COMMENT '成本日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  KEY `idx_cost_type` (`cost_type`),
  KEY `idx_cost_date` (`cost_date`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成本记录表';

-- 人工成本表
CREATE TABLE IF NOT EXISTS `labor_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `employee_name` varchar(50) NOT NULL COMMENT '员工姓名',
  `salary` decimal(10,2) DEFAULT '0.00' COMMENT '工资',
  `social_insurance` decimal(10,2) DEFAULT '0.00' COMMENT '社保',
  `housing_fund` decimal(10,2) DEFAULT '0.00' COMMENT '公积金',
  `other_benefits` decimal(10,2) DEFAULT '0.00' COMMENT '其他福利',
  `total_cost` decimal(10,2) DEFAULT '0.00' COMMENT '总成本',
  `cost_month` date NOT NULL COMMENT '成本月份',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_cost_month` (`cost_month`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工成本表';

-- 其他成本表
CREATE TABLE IF NOT EXISTS `other_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '成本名称',
  `cost_type` tinyint NOT NULL COMMENT '成本类型：1-租金，2-水电，3-设备，4-耗材，5-营销，6-其他',
  `amount` decimal(10,2) NOT NULL COMMENT '成本金额',
  `cost_date` datetime NOT NULL COMMENT '成本日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_cost_type` (`cost_type`),
  KEY `idx_cost_date` (`cost_date`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='其他成本表';

-- 收银记录表
CREATE TABLE IF NOT EXISTS `cashier_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `order_number` varchar(50) DEFAULT NULL COMMENT '订单号',
  `pay_type` tinyint NOT NULL COMMENT '收银类型：1-现金，2-微信，3-支付宝，4-银行卡，5-会员储值',
  `amount` decimal(10,2) NOT NULL COMMENT '收银金额',
  `actual_amount` decimal(10,2) DEFAULT NULL COMMENT '实收金额',
  `change_amount` decimal(10,2) DEFAULT '0.00' COMMENT '找零金额',
  `cashier_time` datetime NOT NULL COMMENT '收银时间',
  `cashier_id` bigint DEFAULT NULL COMMENT '收银员ID',
  `cashier_name` varchar(50) DEFAULT NULL COMMENT '收银员姓名',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`) COMMENT '每笔订单只能有一条收银记录，DB 层幂等兜底',
  KEY `idx_cashier_time` (`cashier_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收银记录表';

-- 日结表
CREATE TABLE IF NOT EXISTS `daily_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_date` date NOT NULL COMMENT '结算日期',
  `total_revenue` decimal(12,2) DEFAULT '0.00' COMMENT '营业额',
  `cash_income` decimal(12,2) DEFAULT '0.00' COMMENT '现金收入',
  `wechat_income` decimal(12,2) DEFAULT '0.00' COMMENT '微信收入',
  `alipay_income` decimal(12,2) DEFAULT '0.00' COMMENT '支付宝收入',
  `bankcard_income` decimal(12,2) DEFAULT '0.00' COMMENT '银行卡收入',
  `other_income` decimal(12,2) DEFAULT '0.00' COMMENT '其他收入',
  `order_count` int DEFAULT '0' COMMENT '订单数量',
  `refund_amount` decimal(12,2) DEFAULT '0.00' COMMENT '退款金额',
  `refund_count` int DEFAULT '0' COMMENT '退款数量',
  `net_income` decimal(12,2) DEFAULT '0.00' COMMENT '净收入',
  `material_cost` decimal(12,2) DEFAULT '0.00' COMMENT '食材成本',
  `labor_cost` decimal(12,2) DEFAULT '0.00' COMMENT '人工成本',
  `other_cost` decimal(12,2) DEFAULT '0.00' COMMENT '其他成本',
  `total_cost` decimal(12,2) DEFAULT '0.00' COMMENT '总成本',
  `gross_profit` decimal(12,2) DEFAULT '0.00' COMMENT '毛利润',
  `profit_rate` decimal(5,2) DEFAULT '0.00' COMMENT '毛利率',
  `status` tinyint DEFAULT '0' COMMENT '结账状态：0-未结账，1-已结账',
  `settlement_time` datetime DEFAULT NULL COMMENT '结账时间',
  `settlement_user_id` bigint DEFAULT NULL COMMENT '结账人ID',
  `settlement_user_name` varchar(50) DEFAULT NULL COMMENT '结账人姓名',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_date_tenant` (`settlement_date`, `tenant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日结表';
