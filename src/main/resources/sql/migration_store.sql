-- ============================================
-- 多门店管理模块 - 数据库迁移脚本
-- 模块：门店配置、数据同步、跨店管理
-- 日期：2026-07-09
-- ============================================

-- --------------------------------------------
-- 1. 门店扩展信息表 (增强现有tenant表)
-- 在tenant表基础上补充门店运营信息
-- --------------------------------------------
DROP TABLE IF EXISTS `store_info`;
CREATE TABLE `store_info` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint NOT NULL COMMENT '所属租户/门店ID',
    `store_code` varchar(32) NOT NULL COMMENT '门店编码，如：BJ001、SH001',
    `store_type` tinyint NOT NULL DEFAULT 1 COMMENT '门店类型 1:直营总店 2:直营分店 3:加盟店',
    `parent_tenant_id` bigint DEFAULT NULL COMMENT '上级总店tenantId，NULL表示总店自身',
    `business_hours` varchar(100) DEFAULT NULL COMMENT '营业时间，如：09:00-22:00',
    `delivery_radius` int NOT NULL DEFAULT 3000 COMMENT '配送半径(米)',
    `min_delivery_amount` decimal(10,2) NOT NULL DEFAULT 20.00 COMMENT '最低起送金额',
    `delivery_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送费',
    `is_delivery_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否支持外卖 0:否 1:是',
    `is_dine_in_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否支持堂食 0:否 1:是',
    `contact_person` varchar(64) DEFAULT NULL COMMENT '门店联系人',
    `contact_phone` varchar(20) DEFAULT NULL COMMENT '门店联系电话',
    `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
    `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime NOT NULL COMMENT '更新时间',
    `create_user` bigint NOT NULL COMMENT '创建人',
    `update_user` bigint NOT NULL COMMENT '修改人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_tenant`(`tenant_id` ASC) USING BTREE,
    INDEX `idx_parent`(`parent_tenant_id` ASC) USING BTREE,
    INDEX `idx_code`(`store_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门店扩展信息' ROW_FORMAT = Dynamic;

-- --------------------------------------------
-- 2. 门店配置表
-- 门店级别的功能开关和运营参数
-- --------------------------------------------
DROP TABLE IF EXISTS `store_config`;
CREATE TABLE `store_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint NOT NULL COMMENT '租户/门店ID',
    `config_key` varchar(64) NOT NULL COMMENT '配置键',
    `config_value` varchar(2000) NOT NULL COMMENT '配置值',
    `config_type` tinyint NOT NULL DEFAULT 1 COMMENT '配置类型 1:功能开关 2:运营参数 3:显示设置 4:其他',
    `description` varchar(200) DEFAULT NULL COMMENT '配置说明',
    `created_by` bigint NOT NULL COMMENT '配置创建人(总部管理员)',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_tenant_key`(`tenant_id` ASC, `config_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门店配置' ROW_FORMAT = Dynamic;

-- --------------------------------------------
-- 3. 门店同步日志表
-- 记录总部向分店同步数据的操作日志
-- --------------------------------------------
DROP TABLE IF EXISTS `store_sync_log`;
CREATE TABLE `store_sync_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `source_tenant_id` bigint NOT NULL COMMENT '来源门店ID(通常是总部)',
    `target_tenant_id` bigint NOT NULL COMMENT '目标门店ID',
    `sync_type` tinyint NOT NULL COMMENT '同步类型 1:菜品同步 2:分类同步 3:套餐同步 4:配置同步 5:优惠券同步',
    `sync_mode` tinyint NOT NULL DEFAULT 1 COMMENT '同步模式 1:全量同步 2:增量同步 3:选择性同步',
    `sync_status` tinyint NOT NULL DEFAULT 0 COMMENT '同步状态 0:进行中 1:成功 2:失败 3:部分成功',
    `sync_count` int NOT NULL DEFAULT 0 COMMENT '同步数量',
    `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数量',
    `error_detail` text COMMENT '错误详情',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `start_time` datetime NOT NULL COMMENT '开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_source`(`source_tenant_id` ASC) USING BTREE,
    INDEX `idx_target`(`target_tenant_id` ASC) USING BTREE,
    INDEX `idx_status`(`sync_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门店同步日志' ROW_FORMAT = Dynamic;

-- --------------------------------------------
-- 4. 门店员工权限关联表
-- 总部管理员管理各门店员工的权限分配
-- --------------------------------------------
DROP TABLE IF EXISTS `store_employee_permission`;
CREATE TABLE `store_employee_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `employee_id` bigint NOT NULL COMMENT '员工ID',
    `tenant_id` bigint NOT NULL COMMENT '门店ID',
    `role_type` tinyint NOT NULL COMMENT '角色类型 1:店长 2:厨师 3:服务员 4:收银员 5:配送员',
    `permissions` text COMMENT '权限列表 JSON数组，如：["dish:view","dish:edit","order:view"]',
    `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '是否生效 0:否 1:是',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime NOT NULL COMMENT '更新时间',
    `create_user` bigint NOT NULL COMMENT '创建人',
    `update_user` bigint NOT NULL COMMENT '修改人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_emp_tenant`(`employee_id` ASC, `tenant_id` ASC) USING BTREE,
    INDEX `idx_tenant`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门店员工权限关联' ROW_FORMAT = Dynamic;

-- --------------------------------------------
-- 5. 门店经营汇总表（每日汇总快照）
-- 总部控制台聚合查询用
-- --------------------------------------------
DROP TABLE IF EXISTS `store_daily_summary`;
CREATE TABLE `store_daily_summary` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint NOT NULL COMMENT '门店ID',
    `summary_date` date NOT NULL COMMENT '统计日期',
    `total_orders` int NOT NULL DEFAULT 0 COMMENT '订单总数',
    `completed_orders` int NOT NULL DEFAULT 0 COMMENT '已完成订单数',
    `cancelled_orders` int NOT NULL DEFAULT 0 COMMENT '取消订单数',
    `total_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单总额',
    `actual_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '实收金额',
    `new_users` int NOT NULL DEFAULT 0 COMMENT '新增用户数',
    `avg_order_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '平均订单金额',
    `top_dish_json` varchar(1000) DEFAULT NULL COMMENT '热销菜品TOP10 JSON',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_tenant_date`(`tenant_id` ASC, `summary_date` ASC) USING BTREE,
    INDEX `idx_date`(`summary_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门店每日经营汇总' ROW_FORMAT = Dynamic;
