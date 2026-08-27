-- Customer Service Tables

-- Customer Service Session Table
CREATE TABLE IF NOT EXISTS `cs_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `session_no` varchar(50) NOT NULL COMMENT 'Session Number',
  `user_id` bigint NOT NULL COMMENT 'User ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT 'User Name',
  `agent_id` bigint DEFAULT NULL COMMENT 'Agent ID',
  `agent_name` varchar(50) DEFAULT NULL COMMENT 'Agent Name',
  `session_type` tinyint DEFAULT '1' COMMENT 'Session Type: 1-General, 2-Order, 3-Complaint',
  `order_id` bigint DEFAULT NULL COMMENT 'Related Order ID',
  `status` tinyint DEFAULT '0' COMMENT 'Status: 0-Waiting, 1-In Progress, 2-Closed',
  `first_response_time` datetime DEFAULT NULL COMMENT 'First Response Time',
  `close_time` datetime DEFAULT NULL COMMENT 'Close Time',
  `satisfaction_rating` int DEFAULT NULL COMMENT 'Satisfaction Rating (1-5)',
  `user_feedback` varchar(500) DEFAULT NULL COMMENT 'User Feedback',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Customer Service Session Table';

-- Customer Service Message Table
CREATE TABLE IF NOT EXISTS `cs_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `session_id` bigint NOT NULL COMMENT 'Session ID',
  `sender_type` tinyint NOT NULL COMMENT 'Sender Type: 1-User, 2-Agent, 3-System',
  `sender_id` bigint DEFAULT NULL COMMENT 'Sender ID',
  `sender_name` varchar(50) DEFAULT NULL COMMENT 'Sender Name',
  `message_type` tinyint DEFAULT '1' COMMENT 'Message Type: 1-Text, 2-Image, 3-Order Card',
  `content` text DEFAULT NULL COMMENT 'Message Content',
  `image_url` varchar(500) DEFAULT NULL COMMENT 'Image URL',
  `is_read` tinyint DEFAULT '0' COMMENT 'Is Read: 0-No, 1-Yes',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_sender_type` (`sender_type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Customer Service Message Table';

-- Complaint Table
CREATE TABLE IF NOT EXISTS `complaint` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `complaint_no` varchar(50) NOT NULL COMMENT 'Complaint Number',
  `user_id` bigint NOT NULL COMMENT 'User ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT 'User Name',
  `user_phone` varchar(20) DEFAULT NULL COMMENT 'User Phone',
  `order_id` bigint DEFAULT NULL COMMENT 'Order ID',
  `order_number` varchar(50) DEFAULT NULL COMMENT 'Order Number',
  `complaint_type` tinyint NOT NULL COMMENT 'Complaint Type: 1-Food Quality, 2-Delivery, 3-Service, 4-Price, 5-Other',
  `title` varchar(200) NOT NULL COMMENT 'Complaint Title',
  `content` text NOT NULL COMMENT 'Complaint Content',
  `image_urls` varchar(1000) DEFAULT NULL COMMENT 'Image URLs',
  `status` tinyint DEFAULT '0' COMMENT 'Status: 0-Pending, 1-Processing, 2-Resolved, 3-Closed',
  `handler_id` bigint DEFAULT NULL COMMENT 'Handler ID',
  `handler_name` varchar(50) DEFAULT NULL COMMENT 'Handler Name',
  `handle_result` text DEFAULT NULL COMMENT 'Handle Result',
  `compensation_amount` decimal(10,2) DEFAULT NULL COMMENT 'Compensation Amount',
  `handle_time` datetime DEFAULT NULL COMMENT 'Handle Time',
  `satisfaction` int DEFAULT NULL COMMENT 'User Satisfaction (1-5)',
  `user_feedback` varchar(500) DEFAULT NULL COMMENT 'User Feedback',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_complaint_no` (`complaint_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_complaint_type` (`complaint_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Complaint Table';
