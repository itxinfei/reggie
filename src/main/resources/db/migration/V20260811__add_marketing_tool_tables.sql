-- Marketing Tool Tables

-- New Customer Discount Table
CREATE TABLE IF NOT EXISTS `new_customer_discount` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `name` varchar(100) NOT NULL COMMENT 'Discount Name',
  `discount_type` tinyint NOT NULL COMMENT 'Discount Type: 1-Fixed Amount, 2-Percentage',
  `discount_value` decimal(10,2) NOT NULL COMMENT 'Discount Value',
  `max_discount_amount` decimal(10,2) DEFAULT NULL COMMENT 'Maximum Discount Amount',
  `min_order_amount` decimal(10,2) DEFAULT NULL COMMENT 'Minimum Order Amount',
  `valid_days` int DEFAULT NULL COMMENT 'Valid Days After Registration',
  `status` tinyint DEFAULT '1' COMMENT 'Status: 0-Disabled, 1-Enabled',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  `create_user` bigint DEFAULT NULL COMMENT 'Create User',
  `update_user` bigint DEFAULT NULL COMMENT 'Update User',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='New Customer Discount Table';

-- Buy Get Free Table
CREATE TABLE IF NOT EXISTS `buy_get_free` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `name` varchar(100) NOT NULL COMMENT 'Activity Name',
  `description` varchar(500) DEFAULT NULL COMMENT 'Description',
  `buy_quantity` int NOT NULL COMMENT 'Buy Quantity',
  `get_quantity` int NOT NULL COMMENT 'Get Quantity',
  `dish_id` bigint DEFAULT NULL COMMENT 'Applicable Dish ID',
  `setmeal_id` bigint DEFAULT NULL COMMENT 'Applicable Setmeal ID',
  `gift_dish_id` bigint NOT NULL COMMENT 'Gift Dish ID',
  `gift_dish_name` varchar(100) DEFAULT NULL COMMENT 'Gift Dish Name',
  `min_order_amount` decimal(10,2) DEFAULT NULL COMMENT 'Minimum Order Amount',
  `max_times_per_order` int DEFAULT NULL COMMENT 'Max Times Per Order',
  `start_time` datetime NOT NULL COMMENT 'Start Time',
  `end_time` datetime NOT NULL COMMENT 'End Time',
  `status` tinyint DEFAULT '0' COMMENT 'Status: 0-Draft, 1-Active, 2-Paused, 3-Ended',
  `usage_count` int DEFAULT '0' COMMENT 'Current Usage Count',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  `create_user` bigint DEFAULT NULL COMMENT 'Create User',
  `update_user` bigint DEFAULT NULL COMMENT 'Update User',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_gift_dish_id` (`gift_dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Buy Get Free Table';

-- Flash Sale Table
CREATE TABLE IF NOT EXISTS `flash_sale` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `name` varchar(100) NOT NULL COMMENT 'Activity Name',
  `description` varchar(500) DEFAULT NULL COMMENT 'Description',
  `dish_id` bigint NOT NULL COMMENT 'Dish ID',
  `dish_name` varchar(100) DEFAULT NULL COMMENT 'Dish Name',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT 'Original Price',
  `flash_price` decimal(10,2) NOT NULL COMMENT 'Flash Sale Price',
  `total_quantity` int NOT NULL COMMENT 'Total Quantity',
  `sold_quantity` int DEFAULT '0' COMMENT 'Sold Quantity',
  `max_per_user` int DEFAULT NULL COMMENT 'Max Per User',
  `start_time` datetime NOT NULL COMMENT 'Start Time',
  `end_time` datetime NOT NULL COMMENT 'End Time',
  `status` tinyint DEFAULT '0' COMMENT 'Status: 0-Draft, 1-Active, 2-Paused, 3-Ended',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  `create_user` bigint DEFAULT NULL COMMENT 'Create User',
  `update_user` bigint DEFAULT NULL COMMENT 'Update User',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flash Sale Table';
