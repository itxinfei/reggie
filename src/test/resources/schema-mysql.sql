-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: reggie
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `address_book`
--

DROP TABLE IF EXISTS `address_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `consignee` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '收货人',
  `sex` tinyint NOT NULL COMMENT '性别 0 男 1 女',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '详细地址',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度（高德坐标系 GCJ-02）',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度（高德坐标系 GCJ-02）',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_address_user` (`user_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_address_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='地址管理';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_conversation`
--

DROP TABLE IF EXISTS `ai_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_conversation` (
  `id` bigint NOT NULL COMMENT '主键',
  `conversation_id` varchar(64) NOT NULL COMMENT '会话ID（UUID）',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `actor_type` varchar(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT '归属身份类型：EMPLOYEE/CUSTOMER',
  `title` varchar(200) DEFAULT NULL COMMENT '会话标题',
  `scene` varchar(50) DEFAULT NULL COMMENT '场景',
  `message_count` int DEFAULT '0' COMMENT '消息数量',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT 'is_deleted',
  `update_user` bigint DEFAULT NULL COMMENT 'create_user',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_id` (`conversation_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_actor` (`tenant_id`,`actor_type`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_message`
--

DROP TABLE IF EXISTS `ai_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_message` (
  `id` bigint NOT NULL COMMENT '主键',
  `conversation_id` varchar(64) NOT NULL COMMENT '会话ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `role` varchar(20) NOT NULL COMMENT '角色：user/assistant',
  `content` text COMMENT '消息内容',
  `attachments` varchar(1000) DEFAULT NULL COMMENT '附件列表JSON',
  `message_type` varchar(20) DEFAULT 'text' COMMENT '消息类型',
  `status` varchar(20) NOT NULL DEFAULT 'completed' COMMENT '消息状态：completed/stopped/failed',
  `client_msg_id` varchar(64) DEFAULT NULL COMMENT '前端消息幂等键',
  `feedback` varchar(10) DEFAULT NULL COMMENT '反馈类型',
  `dish_ids` varchar(500) DEFAULT NULL COMMENT '推荐菜品ID（JSON）',
  `tokens_used` int DEFAULT '0' COMMENT 'Token使用量',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'is_deleted',
  `create_user` bigint DEFAULT NULL COMMENT 'update_time',
  `update_user` bigint DEFAULT NULL COMMENT 'create_user',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_client_msg` (`conversation_id`,`client_msg_id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI消息记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_attachment`
--

DROP TABLE IF EXISTS `ai_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_attachment` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `actor_type` varchar(16) NOT NULL COMMENT '上传者身份：EMPLOYEE/CUSTOMER',
  `owner_id` bigint NOT NULL COMMENT '上传者用户ID',
  `scene` varchar(50) DEFAULT NULL COMMENT '上传场景',
  `file_name` varchar(200) NOT NULL COMMENT '存储文件名（UUID）',
  `original_name` varchar(200) DEFAULT NULL COMMENT '原始文件名',
  `content_type` varchar(100) NOT NULL COMMENT 'MIME类型：image/jpeg、image/png、image/webp',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小（字节）',
  `width` int DEFAULT NULL COMMENT '图片宽度（像素）',
  `height` int DEFAULT NULL COMMENT '图片高度（像素）',
  `sha256` varchar(64) NOT NULL COMMENT '文件内容SHA256',
  `storage_path` varchar(500) NOT NULL COMMENT '相对存储路径：images/ai/{tenantId}/{actorType}/{uuid}.{ext}',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_owner` (`tenant_id`,`actor_type`,`owner_id`),
  KEY `idx_owner_scene` (`owner_id`,`scene`),
  KEY `idx_sha256` (`sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI聊天图片附件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_prompt_template`
--

DROP TABLE IF EXISTS `ai_prompt_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_prompt_template` (
  `id` bigint NOT NULL COMMENT '主键',
  `code` varchar(64) NOT NULL COMMENT '模板编码：{type}_{scene}',
  `scene` varchar(50) NOT NULL COMMENT '场景',
  `type` varchar(16) NOT NULL COMMENT '类型：SYSTEM/WELCOME/QUICK',
  `title` varchar(100) NOT NULL COMMENT '模板名称',
  `content` text COMMENT '提示词/欢迎语文本',
  `quick_questions` varchar(1000) DEFAULT NULL COMMENT '快捷问题JSON数组',
  `builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `version` int NOT NULL DEFAULT '1' COMMENT '模板版本',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_scene_type` (`scene`,`type`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI提示词模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_provider_config`
--

DROP TABLE IF EXISTS `ai_provider_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_provider_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `provider_code` varchar(50) DEFAULT NULL COMMENT '供应商编码',
  `provider_name` varchar(100) DEFAULT NULL COMMENT '供应商名称',
  `base_url` varchar(500) DEFAULT NULL COMMENT 'API基础URL',
  `model_name` varchar(100) DEFAULT NULL COMMENT '模型名称',
  `api_key` varchar(500) DEFAULT NULL COMMENT 'APIԿܴ洢',
  `timeout` int DEFAULT '60' COMMENT '请求超时时间（秒）',
  `max_tokens` int DEFAULT '4096' COMMENT '最大Token数',
  `temperature` decimal(3,2) DEFAULT '0.70' COMMENT '温度参数',
  `api_format` varchar(50) DEFAULT 'openai_compatible' COMMENT 'API格式类型',
  `capabilities` varchar(200) DEFAULT NULL COMMENT '能力开关JSON：{chat,vision,tools,embedding}',
  `embedding_dimensions` int DEFAULT NULL COMMENT '向量维度（embedding模型）',
  `extra_headers` varchar(1000) DEFAULT NULL COMMENT '额外请求头（JSON）',
  `request_template` varchar(2000) DEFAULT NULL COMMENT '请求体映射模板',
  `response_path` varchar(500) DEFAULT NULL COMMENT '响应解析路径',
  `icon_url` varchar(500) DEFAULT NULL COMMENT '图标URL',
  `enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用',
  `is_active` tinyint(1) DEFAULT '0' COMMENT '是否激活',
  `last_test_time` datetime DEFAULT NULL COMMENT '最后测试时间',
  `last_test_result` varchar(20) DEFAULT NULL COMMENT '最后测试结果',
  `sort` int DEFAULT '0' COMMENT '排序号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_code` (`provider_code`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI供应商配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_knowledge_doc`
--

DROP TABLE IF EXISTS `ai_knowledge_doc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_doc` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `doc_type` varchar(16) NOT NULL DEFAULT 'TEXT' COMMENT '类型：FAQ/TEXT',
  `audience` varchar(16) NOT NULL DEFAULT 'BOTH' COMMENT '受众：CUSTOMER/MERCHANT/BOTH',
  `content` mediumtext NOT NULL COMMENT '原文内容',
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/INDEXING/READY/FAILED',
  `chunk_count` int NOT NULL DEFAULT '0' COMMENT '切块数量',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败/降级原因',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_tenant_audience` (`tenant_id`,`audience`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI知识库文档';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_knowledge_chunk`
--

DROP TABLE IF EXISTS `ai_knowledge_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_chunk` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `doc_id` bigint NOT NULL COMMENT '文档ID',
  `chunk_index` int NOT NULL COMMENT '序号',
  `content` varchar(1000) NOT NULL COMMENT '切块内容',
  `embedding` mediumtext DEFAULT NULL COMMENT '向量JSON',
  `embed_model` varchar(100) DEFAULT NULL COMMENT '向量模型名',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_doc` (`tenant_id`,`doc_id`),
  FULLTEXT KEY `ft_content` (`content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI知识库切块';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_user_profile`
--

DROP TABLE IF EXISTS `ai_user_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_user_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `taste_tags` varchar(500) DEFAULT NULL COMMENT 'ƫõĿζǩJSON飩',
  `category_tags` varchar(500) DEFAULT NULL COMMENT 'ƫõĲƷࣨJSON飩',
  `disliked_tags` varchar(500) DEFAULT NULL COMMENT 'ϲĿζǩJSON飩',
  `allergies` varchar(500) DEFAULT NULL COMMENT '忌口/过敏信息',
  `price_preference` varchar(20) DEFAULT NULL COMMENT '价格偏好标签',
  `avg_order_amount` int DEFAULT NULL COMMENT '客单价均值（分）',
  `usual_diners` int DEFAULT NULL COMMENT '常用就餐人数',
  `user_tags` varchar(500) DEFAULT NULL COMMENT 'ûǩJSON飩',
  `frequent_dish_ids` varchar(500) DEFAULT NULL COMMENT 'ƷIDбJSON飩',
  `preferred_dining_type` varchar(20) DEFAULT NULL COMMENT '偏好的配送方式',
  `preferred_time_slot` varchar(20) DEFAULT NULL COMMENT '偏好的送达时段',
  `delivery_fee_sensitive` tinyint(1) DEFAULT '1' COMMENT '是否在意配送费',
  `confidence` decimal(5,2) DEFAULT '0.00' COMMENT '画像置信度',
  `last_analyzed_time` datetime DEFAULT NULL COMMENT '最后更新画像的时间',
  `total_conversations` int DEFAULT '0' COMMENT '总对话次数',
  `total_feedbacks` int DEFAULT '0' COMMENT '总反馈次数',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2090132116699402244 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI用户画像';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attendance`
--

DROP TABLE IF EXISTS `attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `employee_name` varchar(50) DEFAULT NULL COMMENT '员工姓名',
  `date` date NOT NULL COMMENT '考勤日期',
  `check_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '签退时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '考勤状态：0=缺勤,1=正常,2=迟到,3=早退,4=请假,5=出差',
  `work_hours` decimal(5,2) DEFAULT '0.00' COMMENT '工时（小时）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_attendance_emp_date` (`employee_id`,`date`),
  KEY `idx_attendance_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `buy_get_free`
--

DROP TABLE IF EXISTS `buy_get_free`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `buy_get_free` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Buy Get Free Table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `campaign_usage_record`
--

DROP TABLE IF EXISTS `campaign_usage_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign_usage_record` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='营销活动使用记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cashier_record`
--

DROP TABLE IF EXISTS `cashier_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cashier_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `order_number` varchar(50) DEFAULT NULL COMMENT '订单',
  `pay_type` tinyint NOT NULL COMMENT '收银类型 1-现金 2-徿 3-攻 4-银 5-会员储',
  `amount` decimal(10,2) NOT NULL COMMENT '收银金',
  `actual_amount` decimal(10,2) DEFAULT NULL COMMENT '实收金',
  `change_amount` decimal(10,2) DEFAULT '0.00' COMMENT '找零金',
  `cashier_time` datetime NOT NULL COMMENT '收银时间',
  `cashier_id` bigint DEFAULT NULL COMMENT '收银员ID',
  `cashier_name` varchar(50) DEFAULT NULL COMMENT '收银员',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`) COMMENT '订单ID',
  KEY `idx_cashier_time` (`cashier_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` int DEFAULT NULL COMMENT '类型 1 菜品分类 2 套餐分类',
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '顺序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_category_name` (`name`) USING BTREE,
  KEY `idx_category_type` (`type`),
  KEY `idx_category_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='菜品及套餐分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `complaint`
--

DROP TABLE IF EXISTS `complaint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `complaint` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `complaint_no` varchar(50) NOT NULL COMMENT '投诉编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户',
  `user_phone` varchar(20) DEFAULT NULL COMMENT '用户手机',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `order_number` varchar(50) DEFAULT NULL COMMENT '订单编号',
  `complaint_type` tinyint NOT NULL COMMENT '投诉类型 1-食品质量 2-配服 3-服务态度 4-价格 5-其他',
  `title` varchar(200) NOT NULL COMMENT '投诉标',
  `content` text NOT NULL COMMENT '投诉内',
  `image_urls` varchar(1000) DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `status` tinyint DEFAULT '0' COMMENT '状 0-待 1-处理 2-已解 3-已关',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(50) DEFAULT NULL COMMENT '处理人',
  `handle_result` text COMMENT '处理结果',
  `compensation_amount` decimal(10,2) DEFAULT NULL COMMENT '补偿金',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `satisfaction` int DEFAULT NULL COMMENT '满意 1-非常不满 2-不满 3- 4-满意 5-非常满意',
  `user_feedback` varchar(500) DEFAULT NULL COMMENT '用户反',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_complaint_no` (`complaint_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_complaint_type` (`complaint_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cost_record`
--

DROP TABLE IF EXISTS `cost_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cost_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cost_type` tinyint NOT NULL COMMENT '成本类型 1-食材/2-人工/3-其他',
  `ref_id` bigint DEFAULT NULL COMMENT '关联ID',
  `ref_name` varchar(100) DEFAULT NULL COMMENT '关联名称',
  `amount` decimal(10,2) NOT NULL COMMENT '成本金',
  `cost_date` datetime NOT NULL COMMENT '成本日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`),
  KEY `idx_cost_type` (`cost_type`),
  KEY `idx_cost_date` (`cost_date`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupon_template`
--

DROP TABLE IF EXISTS `coupon_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '模板名称',
  `type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '类型',
  `condition_amount` decimal(10,2) DEFAULT NULL COMMENT '满减条件金',
  `discount_amount` decimal(10,2) DEFAULT NULL COMMENT '优惠金',
  `discount_rate` decimal(3,2) DEFAULT NULL COMMENT '折扣',
  `total_count` int DEFAULT '0' COMMENT '发放总数',
  `remain_count` int DEFAULT '0' COMMENT '剩余数量',
  `valid_days` int DEFAULT NULL COMMENT '有效天数',
  `status` int DEFAULT '1' COMMENT '状',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户id';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupon_user`
--

DROP TABLE IF EXISTS `coupon_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户id',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `template_id` bigint NOT NULL COMMENT '优惠券模板ID',
  `code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '优惠券码',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'UNUSED' COMMENT '状态 UNUSED/USED/EXPIRED',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `order_id` bigint DEFAULT NULL COMMENT '使用订单ID',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code` (`code`) USING BTREE,
  UNIQUE KEY `uk_member_template` (`member_id`,`template_id`),
  KEY `idx_member` (`member_id`) USING BTREE,
  KEY `idx_template` (`template_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户id';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cs_message`
--

DROP TABLE IF EXISTS `cs_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cs_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_type` tinyint NOT NULL COMMENT '发类 1-用户 2-客服 3-系统',
  `sender_id` bigint DEFAULT NULL COMMENT '发ID',
  `sender_name` varchar(50) DEFAULT NULL COMMENT '发',
  `message_type` tinyint DEFAULT '1' COMMENT '消息类型 1-文本 2-图片 3-订单卡片',
  `content` text COMMENT '消息内',
  `image_url` varchar(500) DEFAULT NULL COMMENT '图片URL',
  `is_read` tinyint DEFAULT '0' COMMENT '昐已 0- 1-已',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_sender_type` (`sender_type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cs_session`
--

DROP TABLE IF EXISTS `cs_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cs_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_no` varchar(50) NOT NULL COMMENT '会话编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户',
  `agent_id` bigint DEFAULT NULL COMMENT '客服ID',
  `agent_name` varchar(50) DEFAULT NULL COMMENT '客服姓名',
  `session_type` tinyint DEFAULT '1' COMMENT '会话类型 1- 2-订单 3-投诉',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `status` tinyint DEFAULT '0' COMMENT '状 0-等待 1-进 2-已关',
  `first_response_time` datetime DEFAULT NULL COMMENT '首响应时间',
  `close_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `satisfaction_rating` int DEFAULT NULL COMMENT '满意度评(1-5)',
  `user_feedback` varchar(500) DEFAULT NULL COMMENT '用户反',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `daily_settlement`
--

DROP TABLE IF EXISTS `daily_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_date` date NOT NULL COMMENT '结算日期',
  `total_revenue` decimal(12,2) DEFAULT '0.00' COMMENT '营业',
  `cash_income` decimal(12,2) DEFAULT '0.00' COMMENT '现金收入',
  `wechat_income` decimal(12,2) DEFAULT '0.00' COMMENT '徿收入',
  `alipay_income` decimal(12,2) DEFAULT '0.00' COMMENT '攻宝收',
  `bankcard_income` decimal(12,2) DEFAULT '0.00' COMMENT '银卡收',
  `other_income` decimal(12,2) DEFAULT '0.00' COMMENT '其他收入',
  `order_count` int DEFAULT '0' COMMENT '订单数量',
  `refund_amount` decimal(12,2) DEFAULT '0.00' COMMENT '款金',
  `refund_count` int DEFAULT '0' COMMENT '款数',
  `net_income` decimal(12,2) DEFAULT '0.00' COMMENT '收入',
  `material_cost` decimal(12,2) DEFAULT '0.00' COMMENT '食材成本',
  `labor_cost` decimal(12,2) DEFAULT '0.00' COMMENT '人工成本',
  `other_cost` decimal(12,2) DEFAULT '0.00' COMMENT '其他成本',
  `total_cost` decimal(12,2) DEFAULT '0.00' COMMENT '总成',
  `gross_profit` decimal(12,2) DEFAULT '0.00' COMMENT '毛利',
  `profit_rate` decimal(5,2) DEFAULT '0.00' COMMENT '毛利(%)',
  `status` tinyint DEFAULT '0' COMMENT '结账状 0-朻 1-已结',
  `settlement_time` datetime DEFAULT NULL COMMENT '结账时间',
  `settlement_user_id` bigint DEFAULT NULL COMMENT '结账人ID',
  `settlement_user_name` varchar(50) DEFAULT NULL COMMENT '结账人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_date_tenant` (`settlement_date`,`tenant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `delivery_fee_step`
--

DROP TABLE IF EXISTS `delivery_fee_step`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_fee_step` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配送费阶梯规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `delivery_order`
--

DROP TABLE IF EXISTS `delivery_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `platform_order_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '平台订单',
  `platform` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '配平',
  `dish_summary` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '菜品摘',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '订单金',
  `user_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户姓名',
  `phone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'ϵ绰',
  `address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '配地',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'PENDING' COMMENT '订单状',
  `order_time` datetime DEFAULT NULL COMMENT '下单时间',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `version` int NOT NULL DEFAULT '1' COMMENT '乐锁版朏',
  `created_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_platform_order` (`platform`,`platform_order_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_platform` (`platform`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_order_time` (`order_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='下单时间';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `delivery_range_rule`
--

DROP TABLE IF EXISTS `delivery_range_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_range_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `range_type` tinyint NOT NULL COMMENT '范围类型：1-圆形，2-多边形',
  `center_longitude` decimal(12,8) DEFAULT NULL COMMENT '中心经度',
  `center_latitude` decimal(12,8) DEFAULT NULL COMMENT '中心纬度',
  `radius` decimal(10,2) DEFAULT NULL COMMENT '半径（米）',
  `polygon_points` text COMMENT '多边形坐标点JSON',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配送范围规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `delivery_time_record`
--

DROP TABLE IF EXISTS `delivery_time_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_time_record` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Delivery Time Record Table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dining_area`
--

DROP TABLE IF EXISTS `dining_area`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_area` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '区域名称',
  `sort` int DEFAULT '0' COMMENT '排序',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户id';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dining_queue`
--

DROP TABLE IF EXISTS `dining_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_queue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `queue_no` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '排队',
  `phone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '手机',
  `seat_count` int DEFAULT NULL COMMENT '人数',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'WAITING' COMMENT '状态 WAITING/CALLED/CANCELLED/SERVED',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2077635924742098946 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='状态 WAITING/CALLED/CANCELLED/SERVED';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dining_reservation`
--

DROP TABLE IF EXISTS `dining_reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `table_id` bigint DEFAULT NULL COMMENT '桌台ID',
  `customer_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '顾姓名',
  `phone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机',
  `reserved_time` datetime NOT NULL COMMENT '预时间',
  `seat_count` int DEFAULT NULL COMMENT '人数',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/CONFIRMED/CANCELLED/ARRIVED',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_table` (`table_id`) USING BTREE,
  KEY `idx_time` (`reserved_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='预时间';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dining_table`
--

DROP TABLE IF EXISTS `dining_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `area_id` bigint DEFAULT NULL COMMENT '区域ID',
  `name` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '桌台名称',
  `seat_count` int DEFAULT '4' COMMENT '座位',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'FREE' COMMENT '状态 FREE/OCCUPIED/RESERVED/CLEANING',
  `min_amount` decimal(10,2) DEFAULT NULL COMMENT '低消',
  `qr_code_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '二维码URL',
  `current_order_id` bigint DEFAULT NULL COMMENT '当前关联订单ID（开台后绑定',
  `sort` int DEFAULT '0' COMMENT '排序',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_area` (`area_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='区域ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `discount_rule`
--

DROP TABLE IF EXISTS `discount_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `discount_rule` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='折扣规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish`
--

DROP TABLE IF EXISTS `dish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '菜品名称',
  `category_id` bigint NOT NULL COMMENT '菜品分类id',
  `price` decimal(10,2) DEFAULT NULL COMMENT '菜品价格',
  `code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '商品码',
  `image` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '图片',
  `description` varchar(400) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '描述信息',
  `status` int NOT NULL DEFAULT '1' COMMENT '0 停售 1 起售',
  `sort` int NOT NULL DEFAULT '0' COMMENT '顺序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `stock_qty` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '当前库存数量',
  `min_stock` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '最低库存预警阈值',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_dish_name` (`name`) USING BTREE,
  KEY `idx_dish_tenant_category` (`tenant_id`,`category_id`) USING BTREE,
  KEY `idx_dish_category` (`category_id`),
  KEY `idx_dish_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='菜品管理';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_cost`
--

DROP TABLE IF EXISTS `dish_cost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `dish_name` varchar(100) NOT NULL COMMENT '菜品名称',
  `material_cost` decimal(10,2) DEFAULT '0.00' COMMENT '食材成本',
  `labor_cost` decimal(10,2) DEFAULT '0.00' COMMENT '人工成本',
  `other_cost` decimal(10,2) DEFAULT '0.00' COMMENT '其他成本',
  `total_cost` decimal(10,2) DEFAULT '0.00' COMMENT '总成',
  `sale_price` decimal(10,2) DEFAULT '0.00' COMMENT '唻',
  `profit_rate` decimal(5,2) DEFAULT '0.00' COMMENT '毛利(%)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_evaluation`
--

DROP TABLE IF EXISTS `dish_evaluation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_evaluation` (
  `id` bigint NOT NULL COMMENT '评价ID(雪花算法)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `order_id` bigint DEFAULT NULL COMMENT '订单id',
  `user_id` bigint DEFAULT NULL COMMENT '评价用户id',
  `user_name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '评价用户',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `dish_name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '菜品名称',
  `star_rating` int DEFAULT NULL COMMENT '评分(1-5)',
  `content` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '评价内容',
  `images` varchar(2000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '评价图片JSON数组',
  `anonymous` int DEFAULT '0' COMMENT '是否匿名 0=实名 1=匿名',
  `reply_content` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '商品回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '商品回复时间',
  `status` int DEFAULT '0' COMMENT '审核状态：0待审核，1通过，2拒绝',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建用户',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_dish_order` (`user_id`,`dish_id`,`order_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_dish_name` (`dish_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='菜品评价';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_flavor`
--

DROP TABLE IF EXISTS `dish_flavor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_flavor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '菜品',
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '口味名称',
  `value` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '口味数据list',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dish_flavor_tenant_dish` (`tenant_id`,`dish_id`) USING BTREE,
  KEY `idx_dish_flavor_dish` (`dish_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='菜品口味关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_material`
--

DROP TABLE IF EXISTS `dish_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `material_id` bigint NOT NULL COMMENT '食材ID',
  `usage_qty` decimal(10,3) DEFAULT NULL COMMENT '单份菜品消材数',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_dish_material_dish` (`dish_id`),
  KEY `idx_dish_material_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食材ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_platform_mapping`
--

DROP TABLE IF EXISTS `dish_platform_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_platform_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '朳统菜品ID',
  `platform_type` varchar(32) NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  `platform_shop_id` varchar(128) DEFAULT NULL COMMENT '平台侧门店ID',
  `platform_dish_id` varchar(128) DEFAULT NULL COMMENT '平台菜品ID',
  `platform_sku_id` varchar(128) DEFAULT NULL COMMENT '平台SKU ID',
  `price` decimal(10,2) DEFAULT NULL COMMENT '平台价格',
  `status` int NOT NULL DEFAULT '1' COMMENT '状 0下架 1上架',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_mapping_dish_platform` (`dish_id`,`platform_type`,`platform_dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_spec_group`
--

DROP TABLE IF EXISTS `dish_spec_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_spec_group` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品规格组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_spec_option`
--

DROP TABLE IF EXISTS `dish_spec_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_spec_option` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品规格选项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_spec_relation`
--

DROP TABLE IF EXISTS `dish_spec_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_spec_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `group_id` bigint NOT NULL COMMENT '规格组ID',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_group` (`dish_id`,`group_id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品规格关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '密码',
  `password_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT 'MD5' COMMENT '密码加密类型 MD5/BCRYPT',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '身份证号',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
  `role` int NOT NULL DEFAULT '2' COMMENT '角色 1:超级管理员 2:普通员工',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_username` (`username`) USING BTREE,
  KEY `idx_employee_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='员工信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flash_sale`
--

DROP TABLE IF EXISTS `flash_sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flash_sale` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '活动名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `dish_name` varchar(100) DEFAULT NULL COMMENT '菜品名称',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `flash_price` decimal(10,2) NOT NULL COMMENT 'Flash Sale Price',
  `total_quantity` int NOT NULL DEFAULT '0' COMMENT '总库',
  `sold_quantity` int NOT NULL DEFAULT '0' COMMENT '已售数量',
  `max_per_user` int DEFAULT NULL COMMENT '每人限购',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT 'End Time',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态：0-草稿，1-进行中，2-暂停，3-结束',
  `tenant_id` bigint DEFAULT NULL COMMENT 'Tenant ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  `create_user` bigint DEFAULT NULL COMMENT 'Create User',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`,`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀活动';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `full_reduction_rule`
--

DROP TABLE IF EXISTS `full_reduction_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `full_reduction_rule` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='满减规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `labor_cost`
--

DROP TABLE IF EXISTS `labor_cost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `labor_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `employee_name` varchar(50) NOT NULL COMMENT '员工姓名',
  `salary` decimal(10,2) DEFAULT '0.00' COMMENT '工资',
  `social_insurance` decimal(10,2) DEFAULT '0.00' COMMENT '籣',
  `housing_fund` decimal(10,2) DEFAULT '0.00' COMMENT '內',
  `other_benefits` decimal(10,2) DEFAULT '0.00' COMMENT '其他福利',
  `total_cost` decimal(10,2) DEFAULT '0.00' COMMENT '总成',
  `cost_month` date NOT NULL COMMENT '成本月份',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_cost_month` (`cost_month`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketing_campaign`
--

DROP TABLE IF EXISTS `marketing_campaign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_campaign` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `name` varchar(100) NOT NULL COMMENT '活动名称',
  `description` varchar(500) DEFAULT NULL COMMENT '活动描述',
  `campaign_type` tinyint NOT NULL COMMENT '活动类型 1:满减 2:折扣 3:赠品 4:首单优惠 5:会员专享 6:限时秒杀',
  `target_type` tinyint NOT NULL DEFAULT '1' COMMENT '目标类型 1:全部用户 2:新用户 3:高价值用户 4:流失预警用户 5:指定等级',
  `target_value` varchar(500) DEFAULT NULL COMMENT '目标值等级ID列表/用户ID列表，JSON)',
  `rule_json` text COMMENT '活动规则 JSON，含满减条件、折扣率',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0:草稿 1:进行中 2:已结束 3:已暂停',
  `priority` int NOT NULL DEFAULT '0' COMMENT '优先级，数值越大优先级越高',
  `start_time` datetime NOT NULL COMMENT '活动开始时间',
  `end_time` datetime NOT NULL COMMENT '活动结束时间',
  `max_participants` int DEFAULT NULL COMMENT '最大参与人数，NULL表示不限',
  `current_participants` int NOT NULL DEFAULT '0' COMMENT '当前参与人数',
  `coupon_template_id` bigint DEFAULT NULL COMMENT '关联优惠券模板ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建用户',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_status` (`tenant_id`,`status`) USING BTREE,
  KEY `idx_time` (`start_time`,`end_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='营销活动';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketing_message`
--

DROP TABLE IF EXISTS `marketing_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `campaign_id` bigint NOT NULL COMMENT '关联营销活动ID',
  `user_id` bigint NOT NULL COMMENT '推送用户ID',
  `push_type` tinyint NOT NULL COMMENT '推送类型 1:首页弹窗 2:消息通知 3:短信 4:优惠券自动发',
  `title` varchar(200) NOT NULL COMMENT '推送标题',
  `content` varchar(1000) NOT NULL COMMENT '推送内容',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0:待推送 1:已推送 2:已领取 3:已使用',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_campaign` (`campaign_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='营销消息推送记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `material`
--

DROP TABLE IF EXISTS `material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '物料名称',
  `unit` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '单位',
  `stock_qty` decimal(10,2) DEFAULT '0.00' COMMENT '库存数量',
  `min_stock` decimal(10,2) DEFAULT '0.00' COMMENT '小库',
  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `barcode` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '条形',
  `status` int DEFAULT '1' COMMENT '状态 0禁用 1正常',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_category` (`category_id`) USING BTREE,
  KEY `idx_supplier` (`supplier_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='供应商ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `material_category`
--

DROP TABLE IF EXISTS `material_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '分类名称',
  `sort` int DEFAULT '0' COMMENT '排序',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `user_id` bigint DEFAULT NULL COMMENT '关联用户ID',
  `level_id` bigint DEFAULT NULL COMMENT '会员等级ID',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '会员姓名',
  `phone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机',
  `points` bigint DEFAULT '0' COMMENT '秈',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余',
  `total_consumption` decimal(10,2) DEFAULT '0.00' COMMENT '消费金',
  `status` int DEFAULT '1' COMMENT '状',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_phone` (`phone`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='关联用户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member_level`
--

DROP TABLE IF EXISTS `member_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_level` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) DEFAULT NULL COMMENT '等级说明',
  `min_points` bigint DEFAULT '0',
  `max_points` bigint DEFAULT NULL COMMENT '高积分上',
  `discount` decimal(4,2) DEFAULT '1.00' COMMENT '折扣',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member_tag`
--

DROP TABLE IF EXISTS `member_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `tag_name` varchar(50) NOT NULL COMMENT '标签名称',
  `tag_type` tinyint DEFAULT '1' COMMENT '标签类型（1手动添加 2自动生成）',
  `biz_tag` varchar(50) DEFAULT NULL COMMENT '业务标签',
  `tag_color` varchar(20) DEFAULT '#409EFF' COMMENT '标签颜色',
  `created_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_biz` (`tenant_id`,`member_id`,`biz_tag`),
  UNIQUE KEY `uk_tenant_member_biz` (`tenant_id`,`member_id`,`biz_tag`),
  KEY `idx_member_tenant` (`member_id`,`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `new_customer_discount`
--

DROP TABLE IF EXISTS `new_customer_discount`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `new_customer_discount` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='New Customer Discount Table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_record`
--

DROP TABLE IF EXISTS `notification_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `template_id` bigint DEFAULT NULL COMMENT '模板ID',
  `biz_type` varchar(50) NOT NULL COMMENT '业务类型',
  `channel` tinyint NOT NULL COMMENT '发送渠道: 1=短信 2=APP推送',
  `target_type` tinyint NOT NULL DEFAULT '1' COMMENT '目标类型: 1=单个用户 2=用户分组 3=全部用户',
  `target_value` text NOT NULL COMMENT '目标值手机号或用户ID列表/分群ID，JSON数组',
  `target_count` int NOT NULL DEFAULT '0' COMMENT '目标数量',
  `content` text NOT NULL COMMENT '实际发送内容',
  `send_time` datetime DEFAULT NULL COMMENT '定时发送时间，NULL表示立即发送',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0=待发送 1=发送中 2=成功 3=失败 4=部分成功',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功',
  `fail_count` int NOT NULL DEFAULT '0' COMMENT '失败',
  `fail_reason` text COMMENT '失败原因汇总',
  `ext_data` varchar(500) DEFAULT NULL COMMENT '扩展数据JSON',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建用户',
  `update_user` bigint DEFAULT NULL COMMENT '更新',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_biz_type` (`biz_type`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2075156164464025603 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知发送记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_template`
--

DROP TABLE IF EXISTS `notification_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_code` varchar(64) DEFAULT NULL COMMENT 'ⲿģ(簢SMSģCODE)',
  `channel` tinyint NOT NULL DEFAULT '1' COMMENT '通知渠道: 1=短信 2=APP推送 3=短信+APP推送',
  `biz_type` varchar(50) NOT NULL COMMENT '业务类型: ORDER_NOTICE/PROMOTION/VERIFY_CODE/SYSTEM/JOB_NOTICE',
  `title` varchar(200) DEFAULT NULL COMMENT '推送标题(APP推送用)',
  `content` text NOT NULL COMMENT '打印内容',
  `param_list` varchar(500) DEFAULT NULL COMMENT '参数列表JSON，如 ["orderNo","amount"]',
  `sign_name` varchar(50) DEFAULT NULL COMMENT '短信签名',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1=启用 0=停用',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建用户',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_biz` (`tenant_id`,`biz_type`),
  KEY `idx_template_code` (`template_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2075143570177110018 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `operation_log`
--

DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `operator_ip` varchar(50) DEFAULT NULL COMMENT '操作人IP',
  `module` varchar(50) DEFAULT NULL COMMENT '操作模块',
  `operation_type` varchar(20) DEFAULT NULL COMMENT '操作类型：INSERT/UPDATE/DELETE/OTHER',
  `table_name` varchar(50) DEFAULT NULL COMMENT '业务表名',
  `biz_id` bigint DEFAULT NULL COMMENT '业务记录ID',
  `description` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `old_value` text COMMENT '变更前的值（JSON）',
  `new_value` text COMMENT '变更后的值（JSON）',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
  `request_params` text COMMENT '请求参数（JSON）',
  `duration` bigint DEFAULT NULL COMMENT '执行时长（毫秒）',
  `is_success` int DEFAULT '0' COMMENT '是否成功：0失败 1成功',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=718 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_detail`
--

DROP TABLE IF EXISTS `order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '名称',
  `image` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套id',
  `dish_flavor` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '单价',
  `remark` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '订单明细备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_detail_order` (`order_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '订单',
  `status` int NOT NULL DEFAULT '1' COMMENT '订单状',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `address_book_id` bigint NOT NULL COMMENT '地址id',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `checkout_time` datetime NOT NULL COMMENT '结账时间',
  `pay_method` int NOT NULL DEFAULT '1' COMMENT '攻方式',
  `amount` decimal(10,2) NOT NULL COMMENT '实收金',
  `delivery_fee` decimal(10,2) DEFAULT NULL COMMENT '配费（卖单配费，堂食为 null',
  `full_reduction_amount` decimal(10,2) DEFAULT '0.00' COMMENT '满减优惠金额（满减活动扣减，未享受为0）',
  `remark` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `expect_delivery_time` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '预送达时间',
  `phone` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `user_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `consignee` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `table_id` bigint DEFAULT NULL COMMENT '堂桌台ID',
  `dining_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT 'DELIVERY' COMMENT '用类型',
  `idempotency_key` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '幂等',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建',
  `update_user` bigint DEFAULT NULL COMMENT '俔',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '昐删除',
  `version` int NOT NULL DEFAULT '1' COMMENT '乐锁版朏',
  `stock_refunded` int DEFAULT '0' COMMENT '已库存数量',
  `used_coupon_id` bigint DEFAULT NULL COMMENT '优惠券ID',
  `platform_order_id` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '平台订单',
  `platform_type` varchar(20) COLLATE utf8mb3_bin DEFAULT 'SELF' COMMENT '平台来源',
  `platform_shop_id` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '平台门店ID',
  `platform_raw` text COLLATE utf8mb3_bin COMMENT '平台原订单JSON',
  `master_order_id` bigint DEFAULT NULL COMMENT '父单ID（AA分账时指向主订单',
  `split_count` int DEFAULT NULL COMMENT '分账份数（AA分账时录拆分数量）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_idempotency_key` (`idempotency_key`,`tenant_id`),
  KEY `idx_order_user` (`user_id`,`order_time`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_order_number` (`number`),
  KEY `idx_order_status` (`status`),
  KEY `idx_order_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户id';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `other_cost`
--

DROP TABLE IF EXISTS `other_cost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `other_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '成本名称',
  `cost_type` tinyint NOT NULL COMMENT '成本类型 1-租金/2-水电/3-设/4-耗材/5-营销/6-其他',
  `amount` decimal(10,2) NOT NULL COMMENT '成本金',
  `cost_date` datetime NOT NULL COMMENT '成本日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  KEY `idx_cost_type` (`cost_type`),
  KEY `idx_cost_date` (`cost_date`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_order`
--

DROP TABLE IF EXISTS `payment_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '业务订单id',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `trade_no` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '系统交易号',
  `channel_trade_no` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '通道交易号',
  `channel` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '支付通道 ALIPAY/WECHAT/UNIONPAY',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL/REFUND',
  `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
  `notify_time` datetime DEFAULT NULL COMMENT '回调时间',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `version` int NOT NULL DEFAULT '1' COMMENT 'ֹ汾',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_trade_no` (`trade_no`) USING BTREE,
  KEY `idx_order` (`order_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_channel_trade` (`channel_trade_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='支付订单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `permission_name` varchar(50) NOT NULL COMMENT '权限名称',
  `permission_key` varchar(100) NOT NULL COMMENT '权限标识',
  `permission_type` int NOT NULL DEFAULT '1' COMMENT '权限类型 1:菜单 2:按钮 3:数据',
  `parent_id` bigint DEFAULT '0' COMMENT '父权限ID（0=顶级）',
  `route_path` varchar(200) DEFAULT NULL COMMENT '路由路径',
  `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `platform_config`
--

DROP TABLE IF EXISTS `platform_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_type` varchar(32) NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  `platform_name` varchar(128) DEFAULT NULL COMMENT '平台展示名称',
  `shop_id` varchar(128) DEFAULT NULL COMMENT '平台侧门店ID',
  `app_key` varchar(512) DEFAULT NULL COMMENT '应用标识(加密)',
  `app_secret` varchar(512) DEFAULT NULL COMMENT '应用密钥(加密)',
  `access_token` varchar(512) DEFAULT NULL COMMENT '访问令牌(加密)',
  `enabled` int NOT NULL DEFAULT '1' COMMENT '昐吔 0停用 1吔',
  `sync_scope` int NOT NULL DEFAULT '1' COMMENT '同范围位标 1订单2商品4库存8营业状',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_platform_type_shop` (`platform_type`,`shop_id`),
  KEY `idx_platform_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户id';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `points_record`
--

DROP TABLE IF EXISTS `points_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `points_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户id',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '类型',
  `points` int NOT NULL COMMENT '秈数量',
  `biz_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '关联业务类型',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `expire_time` datetime DEFAULT NULL COMMENT '积分过期时间',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_member` (`member_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户id';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `printer_config`
--

DROP TABLE IF EXISTS `printer_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `printer_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户id',
  `store_id` bigint DEFAULT NULL COMMENT '门店id',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '打印机名称',
  `type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '连接类型 USB/TCP/CLOUD/BLUETOOTH',
  `brand` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '品牌 佳博/芯烨/商米',
  `device_id` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '豸ʶ MAC/SN',
  `system_printer_name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '系统打印机名称（Windows下为驱动名称）',
  `ip_address` varchar(15) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'IP地址',
  `port` int DEFAULT NULL COMMENT '端口',
  `paper_size` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '58mm' COMMENT '纸张规格 58mm/80mm',
  `print_types` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `status` int DEFAULT '1' COMMENT '状态 0禁用 1启用',
  `sort` int DEFAULT '0' COMMENT '排序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='打印机配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `printer_log`
--

DROP TABLE IF EXISTS `printer_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `printer_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint DEFAULT NULL COMMENT '订单id',
  `print_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '打印类型',
  `printer_id` bigint DEFAULT NULL COMMENT '打印机id',
  `content` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT '打印内容',
  `status` int DEFAULT '0' COMMENT '状态 0失败 1成功',
  `error_msg` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '错误信息',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order` (`order_id`) USING BTREE,
  KEY `idx_printer` (`printer_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2077998017890689030 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='打印日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `profit_analysis`
--

DROP TABLE IF EXISTS `profit_analysis`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `profit_analysis` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `analysis_date` date NOT NULL COMMENT '分析日期',
  `total_revenue` decimal(12,2) DEFAULT '0.00' COMMENT '总营',
  `food_cost` decimal(12,2) DEFAULT '0.00' COMMENT '食材成本',
  `labor_cost` decimal(12,2) DEFAULT '0.00' COMMENT '人工成本',
  `other_cost` decimal(12,2) DEFAULT '0.00' COMMENT '其他成本',
  `total_cost` decimal(12,2) DEFAULT '0.00' COMMENT '总成',
  `gross_profit` decimal(12,2) DEFAULT '0.00' COMMENT '毛利',
  `gross_profit_rate` decimal(5,2) DEFAULT '0.00' COMMENT '毛利(%)',
  `operating_expense` decimal(12,2) DEFAULT '0.00' COMMENT '运营费用',
  `net_profit` decimal(12,2) DEFAULT '0.00' COMMENT '利润',
  `net_profit_rate` decimal(5,2) DEFAULT '0.00' COMMENT '利率(%)',
  `order_count` int DEFAULT '0' COMMENT '订单',
  `customer_count` int DEFAULT '0' COMMENT '客户',
  `average_order_value` decimal(10,2) DEFAULT '0.00' COMMENT '客单',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_analysis_date_tenant` (`analysis_date`,`tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_order`
--

DROP TABLE IF EXISTS `purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `order_no` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '订单编号',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT '总金',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `operator` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '操作',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_supplier` (`supplier_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='供应商ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_order_detail`
--

DROP TABLE IF EXISTS `purchase_order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `purchase_order_id` bigint NOT NULL COMMENT '采购订单ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `qty` decimal(10,2) NOT NULL COMMENT '数量',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '金',
  `received_qty` decimal(10,2) DEFAULT '0.00' COMMENT '收货数量',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_purchase` (`purchase_order_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recharge_record`
--

DROP TABLE IF EXISTS `recharge_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recharge_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户id',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `user_id` bigint DEFAULT NULL COMMENT '归属用户ID（C端自助充值冗余）',
  `recharge_no` varchar(64) DEFAULT NULL COMMENT '充值单号（业务唯一）',
  `status` varchar(20) DEFAULT 'SUCCESS' COMMENT '状态 PENDING/SUCCESS/CANCELLED',
  `amount` decimal(10,2) NOT NULL COMMENT '充值金额',
  `gift_amount` decimal(10,2) DEFAULT '0.00' COMMENT '赠金',
  `payment_method` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '渠道 WECHAT/ALIPAY/CASH（预留在线支付）',
  `trade_no` varchar(64) DEFAULT NULL COMMENT '渠道交易号（预留在线支付回填）',
  `confirm_employee_id` bigint DEFAULT NULL COMMENT '确认到账员工ID',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认到账时间',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_recharge_no` (`recharge_no`) USING BTREE,
  KEY `idx_member` (`member_id`) USING BTREE,
  KEY `idx_rr_member_status` (`member_id`,`status`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='充值记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recommendation_cache`
--

DROP TABLE IF EXISTS `recommendation_cache`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommendation_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `recommend_type` tinyint NOT NULL COMMENT '推荐类型 1:菜品推荐 2:套餐推荐 3:新品尝鲜',
  `dish_ids` text NOT NULL COMMENT '推荐菜品/套餐ID列表，JSON数组格式',
  `algo_name` varchar(32) NOT NULL COMMENT '算法名称',
  `score` decimal(3,2) NOT NULL DEFAULT '0.00' COMMENT '推荐评分0.00~1.00',
  `expire_time` datetime NOT NULL COMMENT '缓存过期时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_recommend` (`user_id`,`recommend_type`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_expire` (`expire_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='推荐结果缓存';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recommendation_feedback`
--

DROP TABLE IF EXISTS `recommendation_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommendation_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `recommend_cache_id` bigint DEFAULT NULL COMMENT '关联推荐缓存ID',
  `dish_id` bigint NOT NULL COMMENT '菜品/套餐ID',
  `feedback_type` tinyint NOT NULL COMMENT '反馈类型 1:点击 2:收藏 3:加购 4:下单 5:不感兴趣',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_dish` (`dish_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='推荐反馈';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reconciliation_statement`
--

DROP TABLE IF EXISTS `reconciliation_statement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reconciliation_statement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `statement_no` varchar(50) NOT NULL COMMENT '对账单编',
  `statement_date` date NOT NULL COMMENT '对账日期',
  `platform` varchar(20) DEFAULT 'all' COMMENT '平台 all/wechat/alipay/bank',
  `system_amount` decimal(12,2) DEFAULT '0.00' COMMENT '系统金',
  `platform_amount` decimal(12,2) DEFAULT '0.00' COMMENT '平台金',
  `difference_amount` decimal(12,2) DEFAULT '0.00' COMMENT '差异金额',
  `order_count` int DEFAULT '0' COMMENT '订单',
  `refund_amount` decimal(12,2) DEFAULT '0.00' COMMENT '款金',
  `refund_count` int DEFAULT '0' COMMENT '款数',
  `fee_amount` decimal(12,2) DEFAULT '0.00' COMMENT '手续',
  `net_amount` decimal(12,2) DEFAULT '0.00' COMMENT '净额',
  `status` tinyint DEFAULT '0' COMMENT '状 0- 1-已 2-有差',
  `reconcile_time` datetime DEFAULT NULL COMMENT '对账时间',
  `reconcile_user_id` bigint DEFAULT NULL COMMENT '对账人ID',
  `reconcile_user_name` varchar(50) DEFAULT NULL COMMENT '对账人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_statement_no` (`statement_no`),
  KEY `idx_statement_date` (`statement_date`),
  KEY `idx_platform` (`platform`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `refund_record`
--

DROP TABLE IF EXISTS `refund_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refund_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payment_order_id` bigint NOT NULL COMMENT '支付订单id',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `refund_no` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '退款单号',
  `amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '退款原因',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL',
  `refund_type` int DEFAULT NULL COMMENT '售后类型：1=整单退款 2=部分退款',
  `order_id` bigint DEFAULT NULL COMMENT '业务订单ID',
  `apply_user_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `version` int NOT NULL DEFAULT '1' COMMENT 'ֹ汾',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_refund_no` (`refund_no`) USING BTREE,
  KEY `idx_payment` (`payment_order_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='退款记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_channel_config`
--

DROP TABLE IF EXISTS `payment_channel_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '配置名称',
  `channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '渠道 WECHAT/ALIPAY',
  `wx_app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '微信appId',
  `wx_mch_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '微信商户号',
  `wx_api_v3_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '微信APIv3密钥(加密)',
  `wx_mch_cert_serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '商户证书序列号',
  `wx_mch_private_key` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '商户私钥(加密)',
  `wx_public_key_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '微信支付公钥ID',
  `wx_public_key` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '微信支付公钥',
  `ali_app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '支付宝APPID',
  `ali_private_key` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '支付宝应用私钥(加密)',
  `ali_public_key` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '支付宝公钥',
  `pay_notify_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '支付回调地址',
  `refund_notify_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '退款回调地址',
  `enabled` int NOT NULL DEFAULT '1' COMMENT '启用 0停 1启',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0未删 1已删',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_channel` (`tenant_id`,`channel`,`is_deleted`) USING BTREE,
  KEY `idx_pcc_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='支付渠道配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `region`
--

DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '地区名称',
  `code` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '行政区划代码',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级ID，0为省份',
  `level` tinyint NOT NULL COMMENT '层级：1省 2市 3区/县',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_level` (`level`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=44704 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='行政区划数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rider`
--

DROP TABLE IF EXISTS `rider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Rider Table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rider_location_record`
--

DROP TABLE IF EXISTS `rider_location_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider_location_record` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Rider Location Record Table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（NULL=全局角色）',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_key` varchar(50) NOT NULL COMMENT '角色权限字符串',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `sort` int DEFAULT NULL COMMENT '排序',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_role_key` (`role_key`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role_permission`
--

DROP TABLE IF EXISTS `role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_perm_id` (`permission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `setmeal`
--

DROP TABLE IF EXISTS `setmeal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '菜品分类id',
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL COMMENT '套餐价格',
  `status` int DEFAULT NULL COMMENT '状态 0:停用 1:启用',
  `code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '编码',
  `description` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '描述信息',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_setmeal_name` (`name`) USING BTREE,
  KEY `idx_setmeal_tenant_category` (`tenant_id`,`category_id`) USING BTREE,
  KEY `idx_setmeal_category` (`category_id`),
  KEY `idx_setmeal_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='套餐';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `setmeal_dish`
--

DROP TABLE IF EXISTS `setmeal_dish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal_dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '套餐id',
  `dish_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '菜品id',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '菜品名称（冗余）',
  `price` decimal(10,2) DEFAULT NULL COMMENT '菜品原价（冗余）',
  `copies` int NOT NULL COMMENT '份数',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_setmeal_dish_setmeal` (`setmeal_id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='套餐菜品关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shopping_cart`
--

DROP TABLE IF EXISTS `shopping_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '名称',
  `image` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `user_id` bigint NOT NULL COMMENT '主键',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cart_user` (`user_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2075775824800747524 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='购物车';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_check`
--

DROP TABLE IF EXISTS `stock_check`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_check` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `check_no` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '盘点单号',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `total_diff_amount` decimal(10,2) DEFAULT NULL COMMENT '总差异金',
  `operator` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '操作',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_check_detail`
--

DROP TABLE IF EXISTS `stock_check_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_check_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `check_id` bigint NOT NULL COMMENT '盘点ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `book_qty` decimal(10,2) NOT NULL COMMENT '账面数量',
  `actual_qty` decimal(10,2) NOT NULL COMMENT '实际数量',
  `diff_qty` decimal(10,2) NOT NULL COMMENT '巼数量',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_check` (`check_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='盘点ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_record`
--

DROP TABLE IF EXISTS `stock_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '类型',
  `qty` decimal(10,2) NOT NULL COMMENT '数量',
  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT '总金',
  `biz_id` bigint DEFAULT NULL COMMENT '业务ID',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `operator` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '操作',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_material` (`material_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='类型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store_config`
--

DROP TABLE IF EXISTS `store_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户/门店ID',
  `config_key` varchar(64) NOT NULL COMMENT '配置键',
  `config_value` varchar(2000) NOT NULL COMMENT '配置值',
  `config_type` tinyint NOT NULL DEFAULT '1' COMMENT '配置类型 1:功能配置 2:运营参数 3:显示设置 4:其他',
  `description` varchar(200) DEFAULT NULL COMMENT '配置说明',
  `created_by` bigint NOT NULL COMMENT '配置创建人(总部管理员)',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tenant_key` (`tenant_id`,`config_key`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='门店配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store_daily_summary`
--

DROP TABLE IF EXISTS `store_daily_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_daily_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '门店ID',
  `summary_date` date NOT NULL COMMENT '统计日期',
  `total_orders` int NOT NULL DEFAULT '0' COMMENT '订单总数',
  `completed_orders` int NOT NULL DEFAULT '0' COMMENT '已完成订单数',
  `cancelled_orders` int NOT NULL DEFAULT '0' COMMENT '取消订单',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '订单总金额',
  `actual_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '实收金额',
  `new_users` int NOT NULL DEFAULT '0' COMMENT '新增用户',
  `avg_order_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '平均订单金额',
  `top_dish_json` varchar(1000) DEFAULT NULL COMMENT '热销菜品TOP10 JSON',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tenant_date` (`tenant_id`,`summary_date`) USING BTREE,
  KEY `idx_date` (`summary_date`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='门店每日经营汇总';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store_employee_permission`
--

DROP TABLE IF EXISTS `store_employee_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_employee_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `tenant_id` bigint NOT NULL COMMENT '门店ID',
  `role_type` tinyint NOT NULL COMMENT '角色类型 1:店长 2:厨师 3:服务员 4:收银员 5:配菜员',
  `permissions` text COMMENT '权限列表 JSON，如 ["dish:view","dish:edit","order:view"]',
  `is_active` tinyint NOT NULL DEFAULT '1' COMMENT '是否生效 0:否 1:是',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建用户',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_emp_tenant` (`employee_id`,`tenant_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='门店员工权限关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store_info`
--

DROP TABLE IF EXISTS `store_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '所属租户/门店ID',
  `store_code` varchar(32) NOT NULL COMMENT '门店编码，如：BJ001、SH001',
  `store_type` tinyint NOT NULL DEFAULT '1' COMMENT '门店类型 1:直营总店 2:直营分店 3:加盟',
  `parent_tenant_id` bigint DEFAULT NULL COMMENT '上级总店tenantId，NULL表示总店本身',
  `business_hours` varchar(100) DEFAULT NULL COMMENT '营业时间，如 9:00-22:00',
  `delivery_radius` int NOT NULL DEFAULT '3000' COMMENT '配送半径(米)',
  `min_delivery_amount` decimal(10,2) NOT NULL DEFAULT '20.00' COMMENT '最低起送金额',
  `delivery_fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '配送费',
  `is_delivery_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否外卖 0:否 1:是',
  `is_dine_in_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否堂食 0:否 1:是',
  `contact_person` varchar(64) DEFAULT NULL COMMENT '门店联系',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT 'ŵϵ绰',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `pause_order` int NOT NULL DEFAULT '0' COMMENT '暂停接单 0:正常 1:暂停',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建用户',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_parent` (`parent_tenant_id`) USING BTREE,
  KEY `idx_code` (`store_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='门店扩展信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store_sync_log`
--

DROP TABLE IF EXISTS `store_sync_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_tenant_id` bigint NOT NULL COMMENT '来源门店ID(通常指总部)',
  `target_tenant_id` bigint NOT NULL COMMENT '目标门店ID',
  `sync_type` tinyint NOT NULL COMMENT '同步类型 1:菜品同步 2:分类同步 3:套餐同步 4:配置同步 5:优惠券同步',
  `sync_mode` tinyint NOT NULL DEFAULT '1' COMMENT '同步模式 1:全量同步 2:增量同步 3:选择性同步',
  `sync_status` tinyint NOT NULL DEFAULT '0' COMMENT '同步状态 0:进行中 1:成功 2:失败 3:部分成功',
  `sync_count` int NOT NULL DEFAULT '0' COMMENT '同步数量',
  `fail_count` int NOT NULL DEFAULT '0' COMMENT '失败数量',
  `error_detail` text COMMENT '错误详情',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `start_time` datetime NOT NULL COMMENT '同步开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_source` (`source_tenant_id`) USING BTREE,
  KEY `idx_target` (`target_tenant_id`) USING BTREE,
  KEY `idx_status` (`sync_status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='门店同步日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `supplier`
--

DROP TABLE IF EXISTS `supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '供应商名称',
  `contact` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '联系',
  `phone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'ϵ绰',
  `address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '地址',
  `status` int DEFAULT '1' COMMENT '状态',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_config`
--

DROP TABLE IF EXISTS `system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（NULL=全局配置）',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) NOT NULL COMMENT '配置值',
  `config_type` int DEFAULT NULL COMMENT '配置类型 1:功能开关 2:运营参数 3:显示设置 4:其他',
  `description` varchar(255) DEFAULT NULL COMMENT '配置说明',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tenant_key` (`tenant_id`,`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tenant`
--

DROP TABLE IF EXISTS `tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '租户名称',
  `phone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'ϵ绰',
  `address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '地址',
  `password_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT 'MD5' COMMENT '密码加密类型',
  `status` int DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2075157889845473283 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='租户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '姓名',
  `phone` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '头像',
  `status` int DEFAULT '0' COMMENT '状态 0:禁用 1:正常',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_user_phone` (`phone`),
  KEY `idx_user_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='用户信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_browse_history`
--

DROP TABLE IF EXISTS `user_browse_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_browse_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `target_type` tinyint NOT NULL COMMENT '浏览对象类型 1:菜品 2:套餐',
  `target_id` bigint NOT NULL COMMENT '浏览对象ID',
  `target_name` varchar(128) DEFAULT NULL COMMENT '浏览对象名称',
  `duration_seconds` int NOT NULL DEFAULT '0' COMMENT '浏览停留时长(秒)',
  `action_type` tinyint NOT NULL DEFAULT '1' COMMENT '行为类型 1:浏览 2:收藏 3:加购 4:分享',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_time` (`user_id`,`create_time` DESC) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_target` (`target_type`,`target_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户浏览记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_device`
--

DROP TABLE IF EXISTS `user_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `platform` varchar(20) NOT NULL COMMENT '平台: ANDROID/IOS/H5',
  `device_token` varchar(255) DEFAULT NULL COMMENT '设备推送Token',
  `app_version` varchar(20) DEFAULT NULL COMMENT 'APP汾',
  `push_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否推送: 1=是 0=否',
  `last_active_time` datetime DEFAULT NULL COMMENT '最后活跃时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_platform` (`user_id`,`platform`),
  KEY `idx_device_token` (`device_token`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户设备';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_preference_tag`
--

DROP TABLE IF EXISTS `user_preference_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preference_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `tag_type` tinyint NOT NULL COMMENT '标签类型 1:口味偏好 2:品类偏好 3:价格偏好 4:时段偏好',
  `tag_name` varchar(64) NOT NULL COMMENT '标签名称，如：辣味、川菜、0-30元、午市时段',
  `tag_value` decimal(5,2) NOT NULL DEFAULT '1.00' COMMENT '偏好权重 0.00~1.00，越高越偏好',
  `source` varchar(20) NOT NULL DEFAULT 'ORDER' COMMENT '数据来源 ORDER:订单分析 BROWSE:浏览分析 MANUAL:手动标注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建用户',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_tag` (`user_id`,`tag_type`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户偏好标签';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `withdrawal_application`
--

DROP TABLE IF EXISTS `withdrawal_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `withdrawal_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `application_no` varchar(50) NOT NULL COMMENT '申编号',
  `applicant_id` bigint NOT NULL COMMENT '申人ID',
  `applicant_name` varchar(50) DEFAULT NULL COMMENT '申人',
  `amount` decimal(12,2) NOT NULL COMMENT '提现金',
  `withdraw_method` tinyint DEFAULT NULL COMMENT '提现方式 1-银 2-攻 3-徿',
  `receive_account` varchar(100) DEFAULT NULL COMMENT '收账号',
  `receive_name` varchar(50) DEFAULT NULL COMMENT '收人',
  `status` tinyint DEFAULT '0' COMMENT '状 0-待 1-已 2-已付 3-已拒 4-已取',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(50) DEFAULT NULL COMMENT '审核人',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `review_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `payment_time` datetime DEFAULT NULL COMMENT '付时间',
  `payment_no` varchar(100) DEFAULT NULL COMMENT '付编号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户ID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `work_schedule`
--

DROP TABLE IF EXISTS `work_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `employee_name` varchar(50) DEFAULT NULL COMMENT '员工姓名',
  `schedule_date` date NOT NULL COMMENT '排班日期',
  `shift` tinyint DEFAULT '3' COMMENT '班次：0=早班,1=中班,2=晚班,3=全天',
  `shift_start` time DEFAULT NULL COMMENT '班次开始时间',
  `shift_end` time DEFAULT NULL COMMENT '班次结束时间',
  `work_date_str` varchar(20) DEFAULT NULL COMMENT '工作日期字符串',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_schedule_emp_date` (`employee_id`,`schedule_date`),
  KEY `idx_schedule_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='排班记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'reggie'
--
/*!50003 DROP PROCEDURE IF EXISTS `add_col_if_not_exists` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = gbk */ ;
/*!50003 SET character_set_results = gbk */ ;
/*!50003 SET collation_connection  = gbk_chinese_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `add_col_if_not_exists`(
    IN p_table VARCHAR(64),
    IN p_col VARCHAR(64),
    IN p_def TEXT
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_col;
    IF col_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `create_index_if_not_exists` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = gbk */ ;
/*!50003 SET character_set_results = gbk */ ;
/*!50003 SET collation_connection  = gbk_chinese_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `create_index_if_not_exists`(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN col_spec TEXT)
BEGIN

    DECLARE cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO cnt FROM information_schema.statistics

    WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx;

    IF cnt = 0 THEN

        SET @sql = CONCAT('ALTER TABLE ', tbl, ' ADD INDEX ', idx, ' (', col_spec, ')');

        PREPARE stmt FROM @sql;

        EXECUTE stmt;

        DEALLOCATE PREPARE stmt;

    END IF;

END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `safe_create_index` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = gbk */ ;
/*!50003 SET character_set_results = gbk */ ;
/*!50003 SET collation_connection  = gbk_chinese_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `safe_create_index`(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN col_spec TEXT)
BEGIN

    DECLARE cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO cnt FROM information_schema.statistics

    WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx;

    IF cnt = 0 THEN

        SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD INDEX `', idx, '` (', col_spec, ')');

        PREPARE stmt FROM @sql;

        EXECUTE stmt;

        DEALLOCATE PREPARE stmt;

        SELECT CONCAT('Created index: ', idx, ' on ', tbl) AS action;

    ELSE

        SELECT CONCAT('Skip existing index: ', idx, ' on ', tbl) AS action;

    END IF;

END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

-- ============================================
-- 新增：拼团营销表 (2026-09-01)
-- ============================================
DROP TABLE IF EXISTS `group_buy_participation`;
DROP TABLE IF EXISTS `group_buy_campaign`;
CREATE TABLE `group_buy_campaign` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  `name` varchar(100) NOT NULL COMMENT '活动名称',
  `description` varchar(500) DEFAULT '' COMMENT '活动描述',
  `group_id` bigint NOT NULL DEFAULT 0 COMMENT '拼团组ID',
  `status` varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/CLOSED/ENDED',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `min_members` int NOT NULL DEFAULT 2 COMMENT '最少成团人数',
  `max_members` int NOT NULL DEFAULT 10 COMMENT '最多成团人数',
  `original_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '原价',
  `group_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '拼团价',
  `dish_id` bigint NOT NULL DEFAULT 0 COMMENT '菜品ID',
  `dish_name` varchar(100) DEFAULT '' COMMENT '菜品名称',
  `image` varchar(255) DEFAULT '' COMMENT '活动图片URL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_dish_id` (`dish_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团活动表';

CREATE TABLE `group_buy_participation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  `group_buy_id` bigint NOT NULL COMMENT '拼团活动ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `status` varchar(20) NOT NULL DEFAULT 'JOINED' COMMENT '状态：JOINED/PAID/CANCELLED',
  `join_time` datetime NOT NULL COMMENT '参团时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_group_buy_id` (`group_buy_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团参与记录表';

-- ============================================
-- 新增：提现审批流表 (2026-09-01)
-- ============================================
DROP TABLE IF EXISTS `withdrawal_record`;
DROP TABLE IF EXISTS `withdrawal_request`;
CREATE TABLE `withdrawal_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '提现金额',
  `bank_name` varchar(100) NOT NULL DEFAULT '' COMMENT '银行名称',
  `account_name` varchar(100) NOT NULL DEFAULT '' COMMENT '开户人姓名',
  `account_number` varchar(100) NOT NULL DEFAULT '' COMMENT '银行账号',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  `reject_reason` varchar(255) DEFAULT '' COMMENT '拒绝原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_user_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';

CREATE TABLE `withdrawal_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  `withdrawal_id` bigint NOT NULL COMMENT '提现申请ID',
  `actual_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '实际到账金额',
  `fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '手续费',
  `transfer_time` datetime NOT NULL COMMENT '转账时间',
  `bank_trace_no` varchar(100) NOT NULL DEFAULT '' COMMENT '银行流水号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_withdrawal_id` (`withdrawal_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现记录表';

-- ============================================
-- 新增：采购与价格历史表 (2026-09-01)
-- ============================================
DROP TABLE IF EXISTS `price_history`;
CREATE TABLE `price_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `old_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '旧价',
  `new_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '新价',
  `change_reason` varchar(255) DEFAULT '' COMMENT '变动原因',
  `operator_id` bigint NOT NULL DEFAULT 0 COMMENT '操作人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_material_id` (`material_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创建时间';

-- ============================================
-- 新增：供应商对账/结算单表 (2026-09-01)
-- ============================================
DROP TABLE IF EXISTS `supplier_settlement`;
CREATE TABLE `supplier_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `period` varchar(20) NOT NULL COMMENT '结算周期',
  `total_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '总金',
  `paid_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '已付金',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_supplier_id` (`supplier_id`) USING BTREE,
  KEY `idx_period` (`period`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='状';

--
-- Table structure for table `user_favorite`
--

DROP TABLE IF EXISTS `user_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_type` tinyint NOT NULL COMMENT '收藏类型 1=菜品 2=商家',
  `target_id` bigint NOT NULL COMMENT '收藏对象ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_target` (`user_id`,`target_type`,`target_id`,`tenant_id`) USING BTREE,
  KEY `idx_uf_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-25 17:45:58
