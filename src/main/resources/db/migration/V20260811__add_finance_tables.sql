-- Finance Module Table Structure

-- Withdrawal Application Table
CREATE TABLE IF NOT EXISTS `withdrawal_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `application_no` varchar(50) NOT NULL COMMENT 'Application Number',
  `applicant_id` bigint NOT NULL COMMENT 'Applicant ID',
  `applicant_name` varchar(50) DEFAULT NULL COMMENT 'Applicant Name',
  `amount` decimal(12,2) NOT NULL COMMENT 'Withdrawal Amount',
  `withdraw_method` tinyint DEFAULT NULL COMMENT 'Withdraw Method: 1-Bank Card, 2-Alipay, 3-WeChat',
  `receive_account` varchar(100) DEFAULT NULL COMMENT 'Receive Account',
  `receive_name` varchar(50) DEFAULT NULL COMMENT 'Receive Name',
  `status` tinyint DEFAULT '0' COMMENT 'Status: 0-Pending, 1-Approved, 2-Paid, 3-Rejected, 4-Cancelled',
  `reviewer_id` bigint DEFAULT NULL COMMENT 'Reviewer ID',
  `reviewer_name` varchar(50) DEFAULT NULL COMMENT 'Reviewer Name',
  `review_time` datetime DEFAULT NULL COMMENT 'Review Time',
  `review_remark` varchar(500) DEFAULT NULL COMMENT 'Review Remark',
  `payment_time` datetime DEFAULT NULL COMMENT 'Payment Time',
  `payment_no` varchar(100) DEFAULT NULL COMMENT 'Payment Number',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Withdrawal Application Table';

-- Reconciliation Statement Table
CREATE TABLE IF NOT EXISTS `reconciliation_statement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `statement_no` varchar(50) NOT NULL COMMENT 'Statement Number',
  `statement_date` date NOT NULL COMMENT 'Statement Date',
  `platform` varchar(20) DEFAULT 'all' COMMENT 'Platform: all, wechat, alipay, bank',
  `system_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'System Amount',
  `platform_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'Platform Amount',
  `difference_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'Difference Amount',
  `order_count` int DEFAULT '0' COMMENT 'Order Count',
  `refund_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'Refund Amount',
  `refund_count` int DEFAULT '0' COMMENT 'Refund Count',
  `fee_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'Fee Amount',
  `net_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'Net Amount',
  `status` tinyint DEFAULT '0' COMMENT 'Status: 0-Unreconciled, 1-Reconciled, 2-Discrepancy',
  `reconcile_time` datetime DEFAULT NULL COMMENT 'Reconcile Time',
  `reconcile_user_id` bigint DEFAULT NULL COMMENT 'Reconcile User ID',
  `reconcile_user_name` varchar(50) DEFAULT NULL COMMENT 'Reconcile User Name',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_statement_no` (`statement_no`),
  KEY `idx_statement_date` (`statement_date`),
  KEY `idx_platform` (`platform`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reconciliation Statement Table';

-- Profit Analysis Table
CREATE TABLE IF NOT EXISTS `profit_analysis` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `analysis_date` date NOT NULL COMMENT 'Analysis Date',
  `total_revenue` decimal(12,2) DEFAULT '0.00' COMMENT 'Total Revenue',
  `food_cost` decimal(12,2) DEFAULT '0.00' COMMENT 'Food Cost',
  `labor_cost` decimal(12,2) DEFAULT '0.00' COMMENT 'Labor Cost',
  `other_cost` decimal(12,2) DEFAULT '0.00' COMMENT 'Other Cost',
  `total_cost` decimal(12,2) DEFAULT '0.00' COMMENT 'Total Cost',
  `gross_profit` decimal(12,2) DEFAULT '0.00' COMMENT 'Gross Profit',
  `gross_profit_rate` decimal(5,2) DEFAULT '0.00' COMMENT 'Gross Profit Rate',
  `operating_expense` decimal(12,2) DEFAULT '0.00' COMMENT 'Operating Expense',
  `net_profit` decimal(12,2) DEFAULT '0.00' COMMENT 'Net Profit',
  `net_profit_rate` decimal(5,2) DEFAULT '0.00' COMMENT 'Net Profit Rate',
  `order_count` int DEFAULT '0' COMMENT 'Order Count',
  `customer_count` int DEFAULT '0' COMMENT 'Customer Count',
  `average_order_value` decimal(10,2) DEFAULT '0.00' COMMENT 'Average Order Value',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_analysis_date_tenant` (`analysis_date`, `tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Profit Analysis Table';
