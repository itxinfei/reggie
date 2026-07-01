/*
 * 瑞吉外卖 - 完整数据库脚本
 * 包含：原系统表 + 安全加固字段 + 性能优化索引 + 7个商业补充模块表
 * 直接导入 MySQL 即可一键建库
 *
 * 使用方式:
 *   mysql -u root -p reggie < reggie-full.sql
 *
 * 版本: v2.0 (商业功能补充)
 * 日期: 2026-07-01
 */

SET FOREIGN_KEY_CHECKS=0;
SET NAMES utf8mb4;

-- ============================================================
-- 第一部分：原系统已有表（reggie.sql）
-- ============================================================

-- ----------------------------
-- 员工信息
-- ----------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `name` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '用户名',
  `password` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '密码',
  `password_type` varchar(20) COLLATE utf8_bin DEFAULT 'MD5' COMMENT '密码加密类型 MD5/BCRYPT',
  `phone` varchar(11) COLLATE utf8_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) COLLATE utf8_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) COLLATE utf8_bin NOT NULL COMMENT '身份证号',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_username` (`username`),
  KEY `idx_employee_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='员工信息';

-- ----------------------------
-- 租户
-- ----------------------------
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `name` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '租户名称',
  `phone` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '地址',
  `password_type` varchar(20) COLLATE utf8_bin DEFAULT 'MD5' COMMENT '密码加密类型',
  `status` int(11) DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint(20) DEFAULT NULL COMMENT '创建人',
  `update_user` bigint(20) DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='租户';

-- ----------------------------
-- 菜品及套餐分类
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `type` int(11) DEFAULT NULL COMMENT '类型 1 菜品分类 2 套餐分类',
  `name` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '分类名称',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '顺序',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='菜品及套餐分类';

-- ----------------------------
-- 菜品管理
-- ----------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `name` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '菜品名称',
  `category_id` bigint(20) NOT NULL COMMENT '菜品分类id',
  `price` decimal(10,2) DEFAULT NULL COMMENT '菜品价格',
  `code` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '商品码',
  `image` varchar(200) COLLATE utf8_bin NOT NULL COMMENT '图片',
  `description` varchar(400) COLLATE utf8_bin DEFAULT NULL COMMENT '描述信息',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '0 停售 1 起售',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '顺序',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `is_deleted` int(11) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_dish_name` (`name`),
  KEY `idx_dish_tenant_category` (`tenant_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='菜品管理';

-- ----------------------------
-- 菜品口味关系表
-- ----------------------------
DROP TABLE IF EXISTS `dish_flavor`;
CREATE TABLE `dish_flavor` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品',
  `name` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '口味名称',
  `value` varchar(500) COLLATE utf8_bin DEFAULT NULL COMMENT '口味数据list',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `is_deleted` int(11) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dish_flavor_tenant_dish` (`tenant_id`, `dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='菜品口味关系表';

-- ----------------------------
-- 套餐管理
-- ----------------------------
DROP TABLE IF EXISTS `setmeal`;
CREATE TABLE `setmeal` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `category_id` bigint(20) NOT NULL COMMENT '菜品分类id',
  `name` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL COMMENT '套餐价格',
  `status` int(11) DEFAULT NULL COMMENT '状态 0:停用 1:启用',
  `code` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '编码',
  `description` varchar(512) COLLATE utf8_bin DEFAULT NULL COMMENT '描述信息',
  `image` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '图片',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `is_deleted` int(11) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_setmeal_name` (`name`),
  KEY `idx_setmeal_tenant_category` (`tenant_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='套餐';

-- ----------------------------
-- 套餐菜品关系
-- ----------------------------
DROP TABLE IF EXISTS `setmeal_dish`;
CREATE TABLE `setmeal_dish` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `setmeal_id` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '套餐id',
  `dish_id` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '菜品id',
  `name` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '菜品名称（冗余）',
  `price` decimal(10,2) DEFAULT NULL COMMENT '菜品原价（冗余）',
  `copies` int(11) NOT NULL COMMENT '份数',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `is_deleted` int(11) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='套餐菜品关系';

-- ----------------------------
-- 订单表
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `number` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '订单号',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '订单状态 1待付款 2待派送 3已派送 4已完成 5已取消',
  `user_id` bigint(20) NOT NULL COMMENT '下单用户',
  `address_book_id` bigint(20) NOT NULL COMMENT '地址id',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `checkout_time` datetime NOT NULL COMMENT '结账时间',
  `pay_method` int(11) NOT NULL DEFAULT '1' COMMENT '支付方式 1微信 2支付宝',
  `amount` decimal(10,2) NOT NULL COMMENT '实收金额',
  `remark` varchar(100) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
  `phone` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `address` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `user_name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `consignee` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `table_id` bigint(20) DEFAULT NULL COMMENT '堂食桌台id',
  `dining_type` varchar(20) COLLATE utf8_bin DEFAULT 'DELIVERY' COMMENT '用餐类型 DELIVERY/DINE_IN/TAKEOUT',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_user` (`user_id`, `order_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='订单表';

-- ----------------------------
-- 订单明细表
-- ----------------------------
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `name` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '名字',
  `image` varchar(100) COLLATE utf8_bin DEFAULT NULL COMMENT '图片',
  `order_id` bigint(20) NOT NULL COMMENT '订单id',
  `dish_id` bigint(20) DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint(20) DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '口味',
  `number` int(11) NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_detail_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='订单明细表';

-- ----------------------------
-- 购物车
-- ----------------------------
DROP TABLE IF EXISTS `shopping_cart`;
CREATE TABLE `shopping_cart` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `name` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '名称',
  `image` varchar(100) COLLATE utf8_bin DEFAULT NULL COMMENT '图片',
  `user_id` bigint(20) NOT NULL COMMENT '主键',
  `dish_id` bigint(20) DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint(20) DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '口味',
  `number` int(11) NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cart_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='购物车';

-- ----------------------------
-- 用户信息
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `name` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '姓名',
  `phone` varchar(100) COLLATE utf8_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) COLLATE utf8_bin DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) COLLATE utf8_bin DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) COLLATE utf8_bin DEFAULT NULL COMMENT '头像',
  `status` int(11) DEFAULT '0' COMMENT '状态 0:禁用 1:正常',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='用户信息';

-- ----------------------------
-- 地址管理
-- ----------------------------
DROP TABLE IF EXISTS `address_book`;
CREATE TABLE `address_book` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `consignee` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '收货人',
  `sex` tinyint(4) NOT NULL COMMENT '性别 0 女 1 男',
  `phone` varchar(11) COLLATE utf8_bin NOT NULL COMMENT '手机号',
  `province_code` varchar(12) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(200) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '详细地址',
  `label` varchar(100) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint(20) NOT NULL COMMENT '创建人',
  `update_user` bigint(20) NOT NULL COMMENT '修改人',
  `is_deleted` int(11) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_address_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='地址管理';


-- ============================================================
-- 第二部分：打印小票模块表
-- ============================================================

DROP TABLE IF EXISTS `printer_config`;
CREATE TABLE `printer_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `store_id` bigint(20) DEFAULT NULL COMMENT '门店id',
    `name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '打印机名称',
    `type` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '连接类型 USB/TCP/CLOUD/BLUETOOTH',
    `brand` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '品牌 佳博/芯烨/商米',
    `device_id` varchar(100) COLLATE utf8_bin DEFAULT NULL COMMENT '设备标识 MAC/SN',
    `ip_address` varchar(15) COLLATE utf8_bin DEFAULT NULL COMMENT 'IP地址',
    `port` int(11) DEFAULT NULL COMMENT '端口',
    `paper_size` varchar(10) COLLATE utf8_bin DEFAULT '58mm' COMMENT '纸张规格 58mm/80mm',
    `print_type` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '打印类型 BILL/KITCHEN/DELIVERY',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1启用',
    `sort` int(11) DEFAULT '0' COMMENT '排序',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='打印机配置';

DROP TABLE IF EXISTS `printer_log`;
CREATE TABLE `printer_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` bigint(20) DEFAULT NULL COMMENT '订单id',
    `print_type` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '打印类型',
    `printer_id` bigint(20) DEFAULT NULL COMMENT '打印机id',
    `content` text COLLATE utf8_bin COMMENT '打印内容',
    `status` int(11) DEFAULT '0' COMMENT '状态 0失败 1成功',
    `error_msg` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '错误信息',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_printer` (`printer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='打印日志';


-- ============================================================
-- 第三部分：聚合支付模块表
-- ============================================================

DROP TABLE IF EXISTS `refund_record`;
DROP TABLE IF EXISTS `payment_order`;
CREATE TABLE `payment_order` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` bigint(20) NOT NULL COMMENT '业务订单id',
    `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户id',
    `trade_no` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '系统交易号',
    `channel_trade_no` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '通道交易号',
    `channel` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '支付通道 ALIPAY/WECHAT/UNIONPAY',
    `amount` decimal(10,2) NOT NULL COMMENT '金额',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL/REFUND',
    `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
    `notify_time` datetime DEFAULT NULL COMMENT '回调时间',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_no` (`trade_no`),
    KEY `idx_order` (`order_id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_channel_trade` (`channel_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='支付订单';

CREATE TABLE `refund_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `payment_order_id` bigint(20) NOT NULL COMMENT '支付订单id',
    `refund_no` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '退款单号',
    `amount` decimal(10,2) NOT NULL COMMENT '退款金额',
    `reason` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '退款原因',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_payment` (`payment_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='退款记录';


-- ============================================================
-- 第四部分：堂食管理模块表
-- ============================================================

DROP TABLE IF EXISTS `dining_queue`;
DROP TABLE IF EXISTS `dining_reservation`;
DROP TABLE IF EXISTS `dining_table`;
DROP TABLE IF EXISTS `dining_area`;

CREATE TABLE `dining_area` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '区域名称',
    `sort` int(11) DEFAULT '0' COMMENT '排序',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='堂食区域';

CREATE TABLE `dining_table` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `area_id` bigint(20) DEFAULT NULL COMMENT '区域id',
    `name` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '桌号/桌名',
    `seat_count` int(11) DEFAULT '4' COMMENT '座位数',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'FREE' COMMENT '状态 FREE/OCCUPIED/RESERVED/CLEANING',
    `min_amount` decimal(10,2) DEFAULT NULL COMMENT '最低消费',
    `qr_code_url` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '桌边点餐二维码',
    `sort` int(11) DEFAULT '0' COMMENT '排序',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_area` (`area_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='堂食桌台';

CREATE TABLE `dining_queue` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `queue_no` varchar(10) COLLATE utf8_bin NOT NULL COMMENT '排队号 A001',
    `phone` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '手机号',
    `seat_count` int(11) DEFAULT NULL COMMENT '人数',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'WAITING' COMMENT '状态 WAITING/CALLED/CANCELLED/SERVED',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='排队记录';

CREATE TABLE `dining_reservation` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `table_id` bigint(20) DEFAULT NULL COMMENT '桌台id',
    `customer_name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '顾客姓名',
    `phone` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '联系电话',
    `reserved_time` datetime NOT NULL COMMENT '预订时间',
    `seat_count` int(11) DEFAULT NULL COMMENT '人数',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/CONFIRMED/CANCELLED/ARRIVED',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_table` (`table_id`),
    KEY `idx_time` (`reserved_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='预订记录';


-- ============================================================
-- 第五部分：外卖平台模块表
-- ============================================================

DROP TABLE IF EXISTS `delivery_order`;
CREATE TABLE `delivery_order` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `platform_order_id` varchar(128) COLLATE utf8_bin NOT NULL COMMENT '平台订单号',
    `platform` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '平台 MEITUAN/ELEME/DOUYIN',
    `dish_summary` varchar(500) COLLATE utf8_bin DEFAULT NULL COMMENT '菜品摘要',
    `amount` decimal(10,2) DEFAULT NULL COMMENT '订单金额',
    `user_name` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '用户姓名',
    `phone` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '联系电话',
    `address` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '配送地址',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/ACCEPTED/COMPLETED/CANCELLED',
    `order_time` datetime DEFAULT NULL COMMENT '下单时间',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_platform` (`platform`),
    KEY `idx_status` (`status`),
    KEY `idx_order_time` (`order_time`),
    UNIQUE KEY `uk_platform_order` (`platform`, `platform_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='外卖平台订单';

-- ============================================================
-- 第六部分：进销存/供应链模块表
-- ============================================================

DROP TABLE IF EXISTS `stock_check_detail`;
DROP TABLE IF EXISTS `stock_check`;
DROP TABLE IF EXISTS `purchase_order_detail`;
DROP TABLE IF EXISTS `purchase_order`;
DROP TABLE IF EXISTS `stock_record`;
DROP TABLE IF EXISTS `material`;
DROP TABLE IF EXISTS `material_category`;
DROP TABLE IF EXISTS `supplier`;

CREATE TABLE `supplier` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(100) COLLATE utf8_bin NOT NULL COMMENT '供应商名称',
    `contact` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '联系人',
    `phone` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '联系电话',
    `address` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '地址',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1启用',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='供应商';

CREATE TABLE `material_category` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '分类名称',
    `sort` int(11) DEFAULT '0' COMMENT '排序',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='食材分类';

CREATE TABLE `material` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `category_id` bigint(20) DEFAULT NULL COMMENT '分类id',
    `name` varchar(100) COLLATE utf8_bin NOT NULL COMMENT '食材名称',
    `unit` varchar(10) COLLATE utf8_bin NOT NULL COMMENT '单位 斤/个/包/箱',
    `stock_qty` decimal(10,2) DEFAULT '0.00' COMMENT '当前库存',
    `min_stock` decimal(10,2) DEFAULT '0.00' COMMENT '最低库存预警',
    `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
    `supplier_id` bigint(20) DEFAULT NULL COMMENT '默认供应商',
    `barcode` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '条码',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1启用',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='食材';

CREATE TABLE `stock_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `material_id` bigint(20) NOT NULL COMMENT '食材id',
    `type` varchar(10) COLLATE utf8_bin NOT NULL COMMENT '类型 IN入库/OUT出库/CHECK盘点',
    `qty` decimal(10,2) NOT NULL COMMENT '数量',
    `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
    `total_amount` decimal(10,2) DEFAULT NULL COMMENT '总金额',
    `biz_id` bigint(20) DEFAULT NULL COMMENT '关联业务id 采购单/订单',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `operator` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '操作人',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_material` (`material_id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='出入库记录';

CREATE TABLE `stock_check` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `check_no` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '盘点单号',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/IN_PROGRESS/DONE',
    `total_diff_amount` decimal(10,2) DEFAULT NULL COMMENT '盈亏总金额',
    `operator` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '操作人',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='盘点单';

CREATE TABLE `stock_check_detail` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `check_id` bigint(20) NOT NULL COMMENT '盘点单id',
    `material_id` bigint(20) NOT NULL COMMENT '食材id',
    `book_qty` decimal(10,2) NOT NULL COMMENT '账面数量',
    `actual_qty` decimal(10,2) NOT NULL COMMENT '实际数量',
    `diff_qty` decimal(10,2) NOT NULL COMMENT '差异数量',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_check` (`check_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='盘点明细';

CREATE TABLE `purchase_order` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `order_no` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '采购单号',
    `supplier_id` bigint(20) DEFAULT NULL COMMENT '供应商',
    `total_amount` decimal(10,2) DEFAULT NULL COMMENT '总金额',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/ORDERED/PARTIAL/RECEIVED/CANCELLED',
    `operator` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '操作人',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='采购单';

CREATE TABLE `purchase_order_detail` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `purchase_order_id` bigint(20) NOT NULL COMMENT '采购单id',
    `material_id` bigint(20) NOT NULL COMMENT '食材id',
    `qty` decimal(10,2) NOT NULL COMMENT '数量',
    `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
    `amount` decimal(10,2) DEFAULT NULL COMMENT '金额',
    `received_qty` decimal(10,2) DEFAULT '0.00' COMMENT '已收货数量',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_purchase` (`purchase_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='采购单明细';


-- ============================================================
-- 第六部分：会员营销模块表
-- ============================================================

DROP TABLE IF EXISTS `coupon_user`;
DROP TABLE IF EXISTS `coupon_template`;
DROP TABLE IF EXISTS `recharge_record`;
DROP TABLE IF EXISTS `points_record`;
DROP TABLE IF EXISTS `member`;

CREATE TABLE `member` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `user_id` bigint(20) DEFAULT NULL COMMENT '关联用户id',
    `level_id` bigint(20) DEFAULT NULL COMMENT '会员等级',
    `name` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '姓名',
    `phone` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '手机号',
    `points` bigint(20) DEFAULT '0' COMMENT '积分',
    `balance` decimal(10,2) DEFAULT '0.00' COMMENT '储值余额',
    `total_consumption` decimal(10,2) DEFAULT '0.00' COMMENT '累计消费',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1正常',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='会员';

CREATE TABLE `member_level` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(50) COLLATE utf8_bin NOT NULL COMMENT '等级名称',
    `min_points` bigint(20) DEFAULT '0' COMMENT '所需积分',
    `discount` decimal(3,2) DEFAULT '1.00' COMMENT '折扣 0.95=95折',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='会员等级';

CREATE TABLE `points_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id` bigint(20) NOT NULL COMMENT '会员id',
    `type` varchar(10) COLLATE utf8_bin NOT NULL COMMENT '类型 IN增加/OUT消耗',
    `points` int(11) NOT NULL COMMENT '积分变动',
    `biz_type` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '业务类型 CONSUME/SIGN/REFUND',
    `biz_id` bigint(20) DEFAULT NULL COMMENT '关联业务id',
    `remark` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='积分记录';

CREATE TABLE `recharge_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id` bigint(20) NOT NULL COMMENT '会员id',
    `amount` decimal(10,2) NOT NULL COMMENT '充值金额',
    `gift_amount` decimal(10,2) DEFAULT '0.00' COMMENT '赠送金额',
    `payment_method` varchar(20) COLLATE utf8_bin DEFAULT NULL COMMENT '支付方式',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='充值记录';

CREATE TABLE `coupon_template` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户id',
    `name` varchar(100) COLLATE utf8_bin NOT NULL COMMENT '优惠券名称',
    `type` varchar(20) COLLATE utf8_bin NOT NULL COMMENT '类型 FULL_REDUCE满减/DISCOUNT折扣/NEW_USER新客',
    `condition_amount` decimal(10,2) DEFAULT NULL COMMENT '满减条件 满多少',
    `discount_amount` decimal(10,2) DEFAULT NULL COMMENT '减免金额',
    `discount_rate` decimal(3,2) DEFAULT NULL COMMENT '折扣率',
    `total_count` int(11) DEFAULT '0' COMMENT '发行总量',
    `remain_count` int(11) DEFAULT '0' COMMENT '剩余数量',
    `valid_days` int(11) DEFAULT NULL COMMENT '有效天数',
    `status` int(11) DEFAULT '1' COMMENT '状态 0禁用 1启用',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='优惠券模板';

CREATE TABLE `coupon_user` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id` bigint(20) NOT NULL COMMENT '会员id',
    `template_id` bigint(20) NOT NULL COMMENT '模板id',
    `code` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '券码',
    `status` varchar(20) COLLATE utf8_bin NOT NULL DEFAULT 'UNUSED' COMMENT '状态 UNUSED/USED/EXPIRED',
    `used_time` datetime DEFAULT NULL COMMENT '使用时间',
    `order_id` bigint(20) DEFAULT NULL COMMENT '使用订单',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    PRIMARY KEY (`id`),
    KEY `idx_member` (`member_id`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='用户优惠券';


-- ============================================================
-- 第七部分：初始数据
-- ============================================================

-- 管理员（密码: 123456, MD5加密）
INSERT INTO `employee` (`id`, `name`, `username`, `password`, `password_type`, `phone`, `sex`, `id_number`, `status`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES ('1', '管理员', 'admin', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13812312312', '1', '110101199001010047', '1', '2021-05-06 17:20:07', '2021-05-10 02:24:09', '1', '1');

-- 默认会员等级
INSERT INTO `member_level` (`tenant_id`, `name`, `min_points`, `discount`) VALUES
(0, '普通会员', 0, 1.00),
(0, '银卡会员', 1000, 0.95),
(0, '金卡会员', 5000, 0.90),
(0, '钻石会员', 20000, 0.85);

SET FOREIGN_KEY_CHECKS=1;
