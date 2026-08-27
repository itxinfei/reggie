-- Delivery Tracking Tables

-- Rider Table
CREATE TABLE IF NOT EXISTS `rider` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `name` varchar(50) NOT NULL COMMENT 'Rider Name',
  `phone` varchar(20) DEFAULT NULL COMMENT 'Phone Number',
  `avatar` varchar(200) DEFAULT NULL COMMENT 'Avatar URL',
  `current_longitude` decimal(12,8) DEFAULT NULL COMMENT 'Current Longitude',
  `current_latitude` decimal(12,8) DEFAULT NULL COMMENT 'Current Latitude',
  `status` tinyint DEFAULT '0' COMMENT 'Status: 0-Offline, 1-Online, 2-Busy',
  `current_order_count` int DEFAULT '0' COMMENT 'Current Order Count',
  `total_order_count` int DEFAULT '0' COMMENT 'Total Order Count',
  `rating` decimal(3,1) DEFAULT '5.0' COMMENT 'Rating',
  `last_location_time` datetime DEFAULT NULL COMMENT 'Last Location Update Time',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Rider Table';

-- Rider Location Record Table
CREATE TABLE IF NOT EXISTS `rider_location_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `rider_id` bigint NOT NULL COMMENT 'Rider ID',
  `order_id` bigint DEFAULT NULL COMMENT 'Order ID',
  `longitude` decimal(12,8) NOT NULL COMMENT 'Longitude',
  `latitude` decimal(12,8) NOT NULL COMMENT 'Latitude',
  `speed` decimal(5,2) DEFAULT NULL COMMENT 'Speed (km/h)',
  `direction` decimal(5,2) DEFAULT NULL COMMENT 'Direction (degrees)',
  `record_time` datetime NOT NULL COMMENT 'Record Time',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  PRIMARY KEY (`id`),
  KEY `idx_rider_id` (`rider_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_record_time` (`record_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Rider Location Record Table';

-- Delivery Time Record Table
CREATE TABLE IF NOT EXISTS `delivery_time_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `order_id` bigint NOT NULL COMMENT 'Order ID',
  `order_number` varchar(50) DEFAULT NULL COMMENT 'Order Number',
  `rider_id` bigint DEFAULT NULL COMMENT 'Rider ID',
  `rider_name` varchar(50) DEFAULT NULL COMMENT 'Rider Name',
  `order_time` datetime DEFAULT NULL COMMENT 'Order Time',
  `accept_time` datetime DEFAULT NULL COMMENT 'Accept Time',
  `pickup_time` datetime DEFAULT NULL COMMENT 'Pickup Time',
  `deliver_time` datetime DEFAULT NULL COMMENT 'Deliver Time',
  `estimated_minutes` int DEFAULT NULL COMMENT 'Estimated Delivery Time (minutes)',
  `actual_minutes` int DEFAULT NULL COMMENT 'Actual Delivery Time (minutes)',
  `distance` decimal(10,2) DEFAULT NULL COMMENT 'Distance (meters)',
  `status` tinyint DEFAULT '0' COMMENT 'Status: 0-Pending, 1-Accepted, 2-Picked up, 3-Delivering, 4-Delivered, 5-Cancelled',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_rider_id` (`rider_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Delivery Time Record Table';
