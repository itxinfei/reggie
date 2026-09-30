-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: reggie_test
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
  `consignee` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货人',
  `sex` tinyint DEFAULT NULL COMMENT '性别 0 男 1 女',
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号',
  `province_code` varchar(12) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区级名称',
  `street_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '街道/乡镇',
  `community` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '小区/大厦',
  `building` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼栋',
  `unit` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单元',
  `floor` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼层',
  `room_no` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '门牌号',
  `detail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址',
  `label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度（GCJ-02）',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度（GCJ-02）',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `address_book`
--

LOCK TABLES `address_book` WRITE;
/*!40000 ALTER TABLE `address_book` DISABLE KEYS */;
INSERT INTO `address_book` VALUES (1,1,'张芳',NULL,'13800000231','常规方案','常规项目','常规方案','常规项目','常规方案','常规项目','常规项目',NULL,NULL,NULL,NULL,'AB2607140232',NULL,NULL,0,116.330000,39.920000,'2026-07-14 00:53:36','2026-07-17 00:53:36',1,1,0,1),(2,2,'王芳',NULL,'13900000233','默认方案','默认项目','默认方案','默认项目','默认方案','默认项目','默认项目',NULL,NULL,NULL,NULL,'AB2607190234',NULL,NULL,0,116.340000,39.930000,'2026-07-19 02:06:36','2026-07-23 02:06:36',2,2,0,1),(3,3,'李磊',NULL,'13600000235','补充方案','补充项目','补充方案','补充项目','补充方案','补充项目','补充项目',NULL,NULL,NULL,NULL,'AB2607240236',NULL,NULL,0,116.350000,39.940000,'2026-07-24 03:19:36','2026-07-29 03:19:36',3,3,0,1),(4,1,'赵磊',NULL,'13500000237','备用方案','备用配置','备用方案','备用配置','备用方案','备用配置','备用配置',NULL,NULL,NULL,NULL,'AB2607290238',NULL,NULL,0,116.360000,39.950000,'2026-07-29 04:32:36','2026-08-04 04:32:36',1,1,0,1),(5,2,'刘敏',NULL,'18800000239','扩展方案','扩展配置','扩展方案','扩展配置','扩展方案','扩展配置','扩展配置',NULL,NULL,NULL,NULL,'AB2608030240',NULL,NULL,0,116.370000,39.960000,'2026-08-03 05:45:36','2026-08-10 05:45:36',2,2,0,1),(6,3,'陈敏',NULL,'17700000241','标准方案','标准配置','标准方案','标准配置','标准方案','标准配置','标准配置',NULL,NULL,NULL,NULL,'AB2608080242',NULL,NULL,0,116.380000,39.970000,'2026-08-08 05:58:36','2026-08-16 05:58:36',3,3,0,1),(7,1,'杨静',NULL,'15000000243','增值方案','增值条目','增值方案','增值条目','增值方案','增值条目','增值条目',NULL,NULL,NULL,NULL,'AB2608130244',NULL,NULL,0,116.390000,39.980000,'2026-08-13 07:11:36','2026-08-22 07:11:36',1,1,0,1),(8,2,'黄静',NULL,'16600000245','临时方案','临时条目','临时方案','临时条目','临时方案','临时条目','临时条目',NULL,NULL,NULL,NULL,'AB2608180246',NULL,NULL,0,116.310000,39.990000,'2026-08-18 08:24:36','2026-08-28 08:24:36',2,2,0,1),(9,3,'周强',NULL,'13800000247','长期方案','长期条目','长期方案','长期条目','长期方案','长期条目','长期条目',NULL,NULL,NULL,NULL,'AB2608230248',NULL,NULL,0,116.311000,39.910000,'2026-08-23 09:37:36','2026-09-03 09:37:36',3,3,0,1),(10,1,'吴强',NULL,'13900000249','专项方案','专项记录','专项方案','专项记录','专项方案','专项记录','专项记录',NULL,NULL,NULL,NULL,'AB2608280250',NULL,NULL,0,116.312000,39.911000,'2026-08-28 10:50:36','2026-09-09 10:50:36',1,1,0,1);
/*!40000 ALTER TABLE `address_book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_attachment`
--

DROP TABLE IF EXISTS `ai_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_attachment` (
  `id` bigint NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `actor_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_id` bigint NOT NULL,
  `scene` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint NOT NULL DEFAULT '0',
  `width` int DEFAULT NULL,
  `height` int DEFAULT NULL,
  `sha256` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_attachment`
--

LOCK TABLES `ai_attachment` WRITE;
/*!40000 ALTER TABLE `ai_attachment` DISABLE KEYS */;
INSERT INTO `ai_attachment` VALUES (900000341,1,'DEFAULT',1,NULL,'常规项目','常规项目','用于演示环境的常规记录，可随时调整。',0,NULL,NULL,'常规选项','images/demo/dish-01.jpg',0,'2026-07-14 00:53:36','2026-07-17 00:53:36',NULL,NULL),(900000342,1,'NORMAL',2,NULL,'默认项目','默认项目','系统自动补齐的示例数据。',0,NULL,NULL,'默认选项','images/demo/store-02.jpg',0,'2026-07-19 02:06:36','2026-07-23 02:06:36',NULL,NULL),(900000343,1,'SPECIAL',3,NULL,'补充项目','补充项目','按业务流程录入的一条典型记录。',0,NULL,NULL,'补充选项','images/demo/banner-03.jpg',0,'2026-07-24 03:19:36','2026-07-29 03:19:36',NULL,NULL),(900000344,1,'TEMP',1,NULL,'备用配置','备用配置','运营日常维护产生的记录。',0,NULL,NULL,'备用选项','images/demo/avatar-04.png',0,'2026-07-29 04:32:36','2026-08-04 04:32:36',NULL,NULL),(900000345,1,'CUSTOM',2,NULL,'扩展配置','扩展配置','供联调与走查使用的样例内容。',0,NULL,NULL,'扩展选项','images/demo/logo-05.png',0,'2026-08-03 05:45:36','2026-08-10 05:45:36',NULL,NULL),(900000346,1,'BASIC',3,NULL,'标准配置','标准配置','用于演示环境的常规记录，可随时调整。',0,NULL,NULL,'标准选项','images/demo/dish-01.jpg',0,'2026-08-08 05:58:36','2026-08-16 05:58:36',NULL,NULL),(900000347,1,'DEFAULT',1,NULL,'增值条目','增值条目','系统自动补齐的示例数据。',0,NULL,NULL,'增值选项','images/demo/store-02.jpg',0,'2026-08-13 07:11:36','2026-08-22 07:11:36',NULL,NULL),(900000348,1,'NORMAL',2,NULL,'临时条目','临时条目','按业务流程录入的一条典型记录。',0,NULL,NULL,'临时选项','images/demo/banner-03.jpg',0,'2026-08-18 08:24:36','2026-08-28 08:24:36',NULL,NULL),(900000349,1,'SPECIAL',3,NULL,'长期条目','长期条目','运营日常维护产生的记录。',0,NULL,NULL,'长期选项','images/demo/avatar-04.png',0,'2026-08-23 09:37:36','2026-09-03 09:37:36',NULL,NULL),(900000350,1,'TEMP',1,NULL,'专项记录','专项记录','供联调与走查使用的样例内容。',0,NULL,NULL,'专项选项','images/demo/logo-05.png',0,'2026-08-28 10:50:36','2026-09-09 10:50:36',NULL,NULL);
/*!40000 ALTER TABLE `ai_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_conversation`
--

DROP TABLE IF EXISTS `ai_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_conversation` (
  `id` bigint NOT NULL,
  `conversation_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `actor_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNKNOWN',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scene` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message_count` int DEFAULT '0',
  `is_deleted` int NOT NULL DEFAULT '0',
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_conversation`
--

LOCK TABLES `ai_conversation` WRITE;
/*!40000 ALTER TABLE `ai_conversation` DISABLE KEYS */;
INSERT INTO `ai_conversation` VALUES (900000251,'常规条目',NULL,'UNKNOWN','周末特惠通知',NULL,0,0,NULL,NULL,'2026-07-14 00:53:36','2026-07-17 00:53:36',1),(900000252,'默认条目',NULL,'UNKNOWN','会员日活动说明',NULL,0,0,NULL,NULL,'2026-07-19 02:06:36','2026-07-23 02:06:36',1),(900000253,'补充条目',NULL,'UNKNOWN','配送范围调整公告',NULL,0,0,NULL,NULL,'2026-07-24 03:19:36','2026-07-29 03:19:36',1),(900000254,'备用条目',NULL,'UNKNOWN','菜单更新说明',NULL,0,0,NULL,NULL,'2026-07-29 04:32:36','2026-08-04 04:32:36',1),(900000255,'扩展条目',NULL,'UNKNOWN','门店歇业通知',NULL,0,0,NULL,NULL,'2026-08-03 05:45:36','2026-08-10 05:45:36',1),(900000256,'标准条目',NULL,'UNKNOWN','新品上市介绍',NULL,0,0,NULL,NULL,'2026-08-08 05:58:36','2026-08-16 05:58:36',1),(900000257,'增值条目',NULL,'UNKNOWN','服务流程规范',NULL,0,0,NULL,NULL,'2026-08-13 07:11:36','2026-08-22 07:11:36',1),(900000258,'临时条目',NULL,'UNKNOWN','月度经营小结',NULL,0,0,NULL,NULL,'2026-08-18 08:24:36','2026-08-28 08:24:36',1),(900000259,'长期条目',NULL,'UNKNOWN','客户反馈处理记录',NULL,0,0,NULL,NULL,'2026-08-23 09:37:36','2026-09-03 09:37:36',1),(900000260,'专项条目',NULL,'UNKNOWN','系统升级安排',NULL,0,0,NULL,NULL,'2026-08-28 10:50:36','2026-09-09 10:50:36',1);
/*!40000 ALTER TABLE `ai_conversation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_chunk`
--

DROP TABLE IF EXISTS `ai_knowledge_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_chunk` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `doc_id` bigint NOT NULL,
  `chunk_index` int NOT NULL,
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `embedding` mediumtext COLLATE utf8mb4_unicode_ci,
  `embed_model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_chunk`
--

LOCK TABLES `ai_knowledge_chunk` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_chunk` DISABLE KEYS */;
INSERT INTO `ai_knowledge_chunk` VALUES (900000101,1,1,1,'用于演示环境的常规记录，可随时调整。',NULL,NULL,'2026-07-14 00:53:36','2026-07-17 00:53:36'),(900000102,1,2,2,'系统自动补齐的示例数据。',NULL,NULL,'2026-07-19 02:06:36','2026-07-23 02:06:36'),(900000103,1,3,3,'按业务流程录入的一条典型记录。',NULL,NULL,'2026-07-24 03:19:36','2026-07-29 03:19:36'),(900000104,1,1,1,'运营日常维护产生的记录。',NULL,NULL,'2026-07-29 04:32:36','2026-08-04 04:32:36'),(900000105,1,2,2,'供联调与走查使用的样例内容。',NULL,NULL,'2026-08-03 05:45:36','2026-08-10 05:45:36'),(900000106,1,3,3,'用于演示环境的常规记录，可随时调整。',NULL,NULL,'2026-08-08 05:58:36','2026-08-16 05:58:36'),(900000107,1,1,1,'系统自动补齐的示例数据。',NULL,NULL,'2026-08-13 07:11:36','2026-08-22 07:11:36'),(900000108,1,2,2,'按业务流程录入的一条典型记录。',NULL,NULL,'2026-08-18 08:24:36','2026-08-28 08:24:36'),(900000109,1,3,3,'运营日常维护产生的记录。',NULL,NULL,'2026-08-23 09:37:36','2026-09-03 09:37:36'),(900000110,1,1,1,'供联调与走查使用的样例内容。',NULL,NULL,'2026-08-28 10:50:36','2026-09-09 10:50:36');
/*!40000 ALTER TABLE `ai_knowledge_chunk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_doc`
--

DROP TABLE IF EXISTS `ai_knowledge_doc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_doc` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `doc_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT',
  `audience` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BOTH',
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `chunk_count` int NOT NULL DEFAULT '0',
  `error_msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_doc`
--

LOCK TABLES `ai_knowledge_doc` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_doc` DISABLE KEYS */;
INSERT INTO `ai_knowledge_doc` VALUES (900000181,1,'周末特惠通知','TEXT','BOTH','用于演示环境的常规记录，可随时调整。','DRAFT',0,NULL,'2026-07-14 00:53:36','2026-07-17 00:53:36',NULL,NULL,0),(900000182,1,'会员日活动说明','TEXT','BOTH','系统自动补齐的示例数据。','DRAFT',0,NULL,'2026-07-19 02:06:36','2026-07-23 02:06:36',NULL,NULL,0),(900000183,1,'配送范围调整公告','TEXT','BOTH','按业务流程录入的一条典型记录。','DRAFT',0,NULL,'2026-07-24 03:19:36','2026-07-29 03:19:36',NULL,NULL,0),(900000184,1,'菜单更新说明','TEXT','BOTH','运营日常维护产生的记录。','DRAFT',0,NULL,'2026-07-29 04:32:36','2026-08-04 04:32:36',NULL,NULL,0),(900000185,1,'门店歇业通知','TEXT','BOTH','供联调与走查使用的样例内容。','DRAFT',0,NULL,'2026-08-03 05:45:36','2026-08-10 05:45:36',NULL,NULL,0),(900000186,1,'新品上市介绍','TEXT','BOTH','用于演示环境的常规记录，可随时调整。','DRAFT',0,NULL,'2026-08-08 05:58:36','2026-08-16 05:58:36',NULL,NULL,0),(900000187,1,'服务流程规范','TEXT','BOTH','系统自动补齐的示例数据。','DRAFT',0,NULL,'2026-08-13 07:11:36','2026-08-22 07:11:36',NULL,NULL,0),(900000188,1,'月度经营小结','TEXT','BOTH','按业务流程录入的一条典型记录。','DRAFT',0,NULL,'2026-08-18 08:24:36','2026-08-28 08:24:36',NULL,NULL,0),(900000189,1,'客户反馈处理记录','TEXT','BOTH','运营日常维护产生的记录。','DRAFT',0,NULL,'2026-08-23 09:37:36','2026-09-03 09:37:36',NULL,NULL,0),(900000190,1,'系统升级安排','TEXT','BOTH','供联调与走查使用的样例内容。','DRAFT',0,NULL,'2026-08-28 10:50:36','2026-09-09 10:50:36',NULL,NULL,0);
/*!40000 ALTER TABLE `ai_knowledge_doc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_message`
--

DROP TABLE IF EXISTS `ai_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_message` (
  `id` bigint NOT NULL,
  `conversation_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `attachments` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'text',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'completed',
  `client_msg_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `feedback` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dish_ids` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tokens_used` int DEFAULT '0',
  `is_deleted` int NOT NULL DEFAULT '0',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_message`
--

LOCK TABLES `ai_message` WRITE;
/*!40000 ALTER TABLE `ai_message` DISABLE KEYS */;
INSERT INTO `ai_message` VALUES (900000161,'常规选项',NULL,'DEFAULT',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-07-14 00:53:36',1),(900000162,'默认选项',NULL,'NORMAL',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-07-19 02:06:36',1),(900000163,'补充选项',NULL,'SPECIAL',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-07-24 03:19:36',1),(900000164,'备用选项',NULL,'TEMP',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-07-29 04:32:36',1),(900000165,'扩展选项',NULL,'CUSTOM',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-08-03 05:45:36',1),(900000166,'标准选项',NULL,'BASIC',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-08-08 05:58:36',1),(900000167,'增值选项',NULL,'DEFAULT',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-08-13 07:11:36',1),(900000168,'临时选项',NULL,'NORMAL',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-08-18 08:24:36',1),(900000169,'长期选项',NULL,'SPECIAL',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-08-23 09:37:36',1),(900000170,'专项选项',NULL,'TEMP',NULL,NULL,'text','completed',NULL,NULL,NULL,0,0,'2026-09-30 15:53:51',NULL,NULL,'2026-08-28 10:50:36',1);
/*!40000 ALTER TABLE `ai_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_prompt_template`
--

DROP TABLE IF EXISTS `ai_prompt_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_prompt_template` (
  `id` bigint NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scene` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `quick_questions` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `builtin` tinyint(1) NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `sort` int NOT NULL DEFAULT '0',
  `version` int NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_prompt_template`
--

LOCK TABLES `ai_prompt_template` WRITE;
/*!40000 ALTER TABLE `ai_prompt_template` DISABLE KEYS */;
INSERT INTO `ai_prompt_template` VALUES (2105204451836002305,'system_order_assistant','order_assistant','SYSTEM','点餐助手 · 系统提示词','你是一个专业的餐饮推荐助手，名叫「小吉」。你的任务是根据用户的需求和偏好，从当前门店的菜品中智能推荐最合适的菜品。\n规则：\n1. 只推荐门店真实存在的菜品，不要编造菜品\n2. 考虑用户的口味偏好、预算、人数等因素\n3. 推荐要多样化，荤素搭配\n4. 回复简洁友好，用中文\n5. 推荐理由要具体，说明为什么适合用户\n6. 输出格式为JSON数组，每个菜品包含：dishId（菜品ID）、name（菜名）、reason（推荐理由）',NULL,1,1,41,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204451873751042,'system_dish_desc','dish_desc','SYSTEM','菜品描述 · 系统提示词','你是一个专业的美食文案写手。请根据菜名和基本信息，生成一段吸引人的菜品描述。\n要求：\n1. 描述食材、口味、烹饪方式\n2. 语言生动诱人，适合外卖平台展示\n3. 长度控制在50-150字\n4. 返回纯文本，不要加任何标记',NULL,1,1,21,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204451873751043,'system_business_analysis','business_analysis','SYSTEM','经营分析 · 系统提示词','你是一个餐饮经营数据分析师。请根据提供的经营数据，回答用户关于经营状况的问题。\n要求：\n1. 基于数据事实回答，不要编造数据\n2. 给出具体数字和趋势分析\n3. 提供可行的经营建议\n4. 回复简洁专业',NULL,1,1,11,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204451873751044,'system_marketing','marketing','SYSTEM','营销文案 · 系统提示词','你是一个营销文案专家。请根据用户需求生成吸引人的营销文案。文案要有感染力，适合外卖平台推送。',NULL,1,1,31,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204451924082690,'welcome_business_analysis','business_analysis','WELCOME','经营分析 · 欢迎语','你好，我是你的经营分析助手，可以帮你分析营业额、热销菜品、客流时段等经营问题。',NULL,1,1,12,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204451940859905,'welcome_dish_desc','dish_desc','WELCOME','菜品描述 · 欢迎语','你好，告诉我菜品名称和主要食材，我可以帮你生成诱人的菜品描述。',NULL,1,1,22,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204451957637121,'welcome_marketing','marketing','WELCOME','营销文案 · 欢迎语','你好，描述你的活动内容和目标客群，我来帮你生成适合外卖平台推送的营销文案。',NULL,1,1,32,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204451957637122,'welcome_order_assistant','order_assistant','WELCOME','点餐助手 · 欢迎语','你好呀！我是点餐小助手，可以帮你推荐菜品、介绍口味和份量，有什么想吃的尽管问我～',NULL,1,1,42,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204452070883330,'quick_business_analysis','business_analysis','QUICK','经营分析 · 快捷问题',NULL,'[\"最近7天的营业额趋势怎么样？\",\"热销菜品 Top10 是哪些？\",\"午市和晚市的销售占比如何？\",\"顾客的复购情况怎么样？\"]',1,1,13,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204452096049154,'quick_dish_desc','dish_desc','QUICK','菜品描述 · 快捷问题',NULL,'[\"帮我写一份宫保鸡丁的菜品描述\",\"生成一段麻辣香锅的外卖介绍\",\"鱼香肉丝怎么描述更吸引人？\"]',1,1,23,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204452096049155,'quick_marketing','marketing','QUICK','营销文案 · 快捷问题',NULL,'[\"写一条周末满减活动的推送文案\",\"新客首单立减活动怎么宣传？\",\"帮我写会员日充值活动文案\"]',1,1,33,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0),(2105204452096049156,'quick_order_assistant','order_assistant','QUICK','点餐助手 · 快捷问题',NULL,'[\"今天有什么好吃的推荐？\",\"3个人吃饭点什么比较合适？\",\"有什么不辣的菜吗？\",\"店里的人气招牌菜有哪些？\"]',1,1,43,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL,0);
/*!40000 ALTER TABLE `ai_prompt_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_prompt_template_history`
--

DROP TABLE IF EXISTS `ai_prompt_template_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_prompt_template_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scene` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `quick_questions` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `version` int NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_apt_history_template` (`template_id`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_prompt_template_history`
--

LOCK TABLES `ai_prompt_template_history` WRITE;
/*!40000 ALTER TABLE `ai_prompt_template_history` DISABLE KEYS */;
INSERT INTO `ai_prompt_template_history` VALUES (1,1,'APTH2607140211','DEFAULT','DEFAULT','周末特惠通知',NULL,NULL,1,0,NULL,'2026-07-14 00:53:36'),(2,2,'APTH2607190212','NORMAL','NORMAL','会员日活动说明',NULL,NULL,1,0,NULL,'2026-07-19 02:06:36'),(3,3,'APTH2607240213','SPECIAL','SPECIAL','配送范围调整公告',NULL,NULL,1,0,NULL,'2026-07-24 03:19:36'),(4,1,'APTH2607290214','TEMP','TEMP','菜单更新说明',NULL,NULL,1,0,NULL,'2026-07-29 04:32:36'),(5,2,'APTH2608030215','CUSTOM','CUSTOM','门店歇业通知',NULL,NULL,1,0,NULL,'2026-08-03 05:45:36'),(6,3,'APTH2608080216','BASIC','BASIC','新品上市介绍',NULL,NULL,1,0,NULL,'2026-08-08 05:58:36'),(7,1,'APTH2608130217','DEFAULT','DEFAULT','服务流程规范',NULL,NULL,1,0,NULL,'2026-08-13 07:11:36'),(8,2,'APTH2608180218','NORMAL','NORMAL','月度经营小结',NULL,NULL,1,0,NULL,'2026-08-18 08:24:36'),(9,3,'APTH2608230219','SPECIAL','SPECIAL','客户反馈处理记录',NULL,NULL,1,0,NULL,'2026-08-23 09:37:36'),(10,1,'APTH2608280220','TEMP','TEMP','系统升级安排',NULL,NULL,1,0,NULL,'2026-08-28 10:50:36');
/*!40000 ALTER TABLE `ai_prompt_template_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_provider_config`
--

DROP TABLE IF EXISTS `ai_provider_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_provider_config` (
  `id` bigint NOT NULL COMMENT '主键',
  `provider_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商编码',
  `provider_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商名称',
  `base_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API基础URL',
  `model_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型名称',
  `api_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API密钥（加密存储）',
  `timeout` int DEFAULT '30' COMMENT '请求超时时间（秒）',
  `max_tokens` int DEFAULT '2048' COMMENT '最大Token数',
  `temperature` double DEFAULT '0.7' COMMENT '温度参数',
  `api_format` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API格式类型',
  `extra_headers` text COLLATE utf8mb4_unicode_ci COMMENT '额外请求头（JSON）',
  `request_template` text COLLATE utf8mb4_unicode_ci COMMENT '请求体映射模板',
  `response_path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '响应解析路径',
  `icon_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标URL',
  `enabled` int NOT NULL DEFAULT '1' COMMENT '是否启用',
  `is_active` int NOT NULL DEFAULT '0' COMMENT '是否激活',
  `last_test_time` datetime DEFAULT NULL COMMENT '最后测试时间',
  `last_test_result` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后测试结果',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `capabilities` text COLLATE utf8mb4_unicode_ci COMMENT '模型能力声明（JSON 数组：chat/embedding/vision/tool_call 等）',
  `embedding_dimensions` int DEFAULT NULL COMMENT '向量维度（embedding 模型专用）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_provider_config`
--

LOCK TABLES `ai_provider_config` WRITE;
/*!40000 ALTER TABLE `ai_provider_config` DISABLE KEYS */;
INSERT INTO `ai_provider_config` VALUES (900000301,'常规参数','常规项目',NULL,'常规项目',NULL,30,2048,10,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'用于演示环境的常规记录，可随时调整。','2026-07-14 00:53:36',NULL,NULL,NULL,0,NULL,NULL),(900000302,'默认参数','默认项目',NULL,'默认项目',NULL,30,2048,11,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'系统自动补齐的示例数据。','2026-07-19 02:06:36',NULL,NULL,NULL,0,NULL,NULL),(900000303,'补充参数','补充项目',NULL,'补充项目',NULL,30,2048,12,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'按业务流程录入的一条典型记录。','2026-07-24 03:19:36',NULL,NULL,NULL,0,NULL,NULL),(900000304,'备用参数','备用配置',NULL,'备用配置',NULL,30,2048,13,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'运营日常维护产生的记录。','2026-07-29 04:32:36',NULL,NULL,NULL,0,NULL,NULL),(900000305,'扩展参数','扩展配置',NULL,'扩展配置',NULL,30,2048,14,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'供联调与走查使用的样例内容。','2026-08-03 05:45:36',NULL,NULL,NULL,0,NULL,NULL),(900000306,'标准参数','标准配置',NULL,'标准配置',NULL,30,2048,15,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'用于演示环境的常规记录，可随时调整。','2026-08-08 05:58:36',NULL,NULL,NULL,0,NULL,NULL),(900000307,'增值参数','增值条目',NULL,'增值条目',NULL,30,2048,16,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'系统自动补齐的示例数据。','2026-08-13 07:11:36',NULL,NULL,NULL,0,NULL,NULL),(900000308,'临时参数','临时条目',NULL,'临时条目',NULL,30,2048,17,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'按业务流程录入的一条典型记录。','2026-08-18 08:24:36',NULL,NULL,NULL,0,NULL,NULL),(900000309,'长期参数','长期条目',NULL,'长期条目',NULL,30,2048,18,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'运营日常维护产生的记录。','2026-08-23 09:37:36',NULL,NULL,NULL,0,NULL,NULL),(900000310,'专项参数','专项记录',NULL,'专项记录',NULL,30,2048,19,NULL,NULL,NULL,NULL,NULL,1,0,NULL,NULL,0,'供联调与走查使用的样例内容。','2026-08-28 10:50:36',NULL,NULL,NULL,0,NULL,NULL);
/*!40000 ALTER TABLE `ai_provider_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_user_profile`
--

DROP TABLE IF EXISTS `ai_user_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_user_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `taste_tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `category_tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `disliked_tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `allergies` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price_preference` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avg_order_amount` int DEFAULT NULL,
  `usual_diners` int DEFAULT NULL,
  `user_tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `frequent_dish_ids` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `preferred_dining_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `preferred_time_slot` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `delivery_fee_sensitive` tinyint(1) DEFAULT '1',
  `confidence` decimal(5,2) DEFAULT '0.00',
  `last_analyzed_time` datetime DEFAULT NULL,
  `total_conversations` int DEFAULT '0',
  `total_feedbacks` int DEFAULT '0',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_user_profile`
--

LOCK TABLES `ai_user_profile` WRITE;
/*!40000 ALTER TABLE `ai_user_profile` DISABLE KEYS */;
INSERT INTO `ai_user_profile` VALUES (1,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,10.00,NULL,0,0,'2026-07-14 00:53:36','2026-07-17 00:53:36',NULL,NULL,0),(2,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,11.00,NULL,0,0,'2026-07-19 02:06:36','2026-07-23 02:06:36',NULL,NULL,0),(3,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,12.00,NULL,0,0,'2026-07-24 03:19:36','2026-07-29 03:19:36',NULL,NULL,0),(4,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,13.00,NULL,0,0,'2026-07-29 04:32:36','2026-08-04 04:32:36',NULL,NULL,0),(5,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,14.00,NULL,0,0,'2026-08-03 05:45:36','2026-08-10 05:45:36',NULL,NULL,0),(6,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,15.00,NULL,0,0,'2026-08-08 05:58:36','2026-08-16 05:58:36',NULL,NULL,0),(7,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,16.00,NULL,0,0,'2026-08-13 07:11:36','2026-08-22 07:11:36',NULL,NULL,0),(8,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,17.00,NULL,0,0,'2026-08-18 08:24:36','2026-08-28 08:24:36',NULL,NULL,0),(9,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,18.00,NULL,0,0,'2026-08-23 09:37:36','2026-09-03 09:37:36',NULL,NULL,0),(10,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,19.00,NULL,0,0,'2026-08-28 10:50:36','2026-09-09 10:50:36',NULL,NULL,0);
/*!40000 ALTER TABLE `ai_user_profile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance`
--

DROP TABLE IF EXISTS `attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date` date NOT NULL,
  `check_in_time` datetime DEFAULT NULL,
  `check_out_time` datetime DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `work_hours` decimal(5,2) DEFAULT '0.00',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance`
--

LOCK TABLES `attendance` WRITE;
/*!40000 ALTER TABLE `attendance` DISABLE KEYS */;
INSERT INTO `attendance` VALUES (1,6,'常规项目','2026-07-14',NULL,NULL,0,10.00,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(2,9,'默认项目','2026-07-19',NULL,NULL,0,11.00,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(3,2,'补充项目','2026-07-24',NULL,NULL,0,12.00,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(4,5,'备用配置','2026-07-29',NULL,NULL,0,13.00,'运营日常维护产生的记录。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(5,8,'扩展配置','2026-08-03',NULL,NULL,0,14.00,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(6,1,'标准配置','2026-08-08',NULL,NULL,0,15.00,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(7,4,'增值条目','2026-08-13',NULL,NULL,0,16.00,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(8,7,'临时条目','2026-08-18',NULL,NULL,0,17.00,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(9,10,'长期条目','2026-08-23',NULL,NULL,0,18.00,'运营日常维护产生的记录。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(10,3,'专项记录','2026-08-28',NULL,NULL,0,19.00,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50');
/*!40000 ALTER TABLE `attendance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `buy_get_free`
--

DROP TABLE IF EXISTS `buy_get_free`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `buy_get_free` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `buy_quantity` int NOT NULL COMMENT '购买数量',
  `get_quantity` int NOT NULL COMMENT '赠品数量',
  `dish_id` bigint DEFAULT NULL COMMENT '适用菜品ID',
  `setmeal_id` bigint DEFAULT NULL COMMENT '适用套餐ID',
  `gift_dish_id` bigint NOT NULL COMMENT '赠品菜品ID',
  `gift_dish_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '赠品菜品名称',
  `min_order_amount` decimal(10,2) DEFAULT NULL COMMENT '最低订单金额',
  `max_times_per_order` int DEFAULT NULL COMMENT '每单最多触发次数',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `status` tinyint DEFAULT '0' COMMENT '状态 0草稿 1生效 2暂停 3结束',
  `usage_count` int DEFAULT '0' COMMENT '已使用次数',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `buy_get_free`
--

LOCK TABLES `buy_get_free` WRITE;
/*!40000 ALTER TABLE `buy_get_free` DISABLE KEYS */;
INSERT INTO `buy_get_free` VALUES (1,'常规项目','用于演示环境的常规记录，可随时调整。',5,5,NULL,NULL,1,'常规项目',18.00,NULL,'2026-07-14 00:53:36','2026-07-14 02:53:36',0,0,1,NULL,NULL,NULL,NULL),(2,'默认项目','系统自动补齐的示例数据。',7,7,NULL,NULL,2,'默认项目',25.00,NULL,'2026-07-19 02:06:36','2026-07-19 05:06:36',0,0,1,NULL,NULL,NULL,NULL),(3,'补充项目','按业务流程录入的一条典型记录。',9,9,NULL,NULL,3,'补充项目',32.00,NULL,'2026-07-24 03:19:36','2026-07-24 07:19:36',0,0,1,NULL,NULL,NULL,NULL),(4,'备用配置','运营日常维护产生的记录。',11,11,NULL,NULL,1,'备用配置',39.00,NULL,'2026-07-29 04:32:36','2026-07-29 09:32:36',0,0,1,NULL,NULL,NULL,NULL),(5,'扩展配置','供联调与走查使用的样例内容。',13,13,NULL,NULL,2,'扩展配置',46.00,NULL,'2026-08-03 05:45:36','2026-08-03 11:45:36',0,0,1,NULL,NULL,NULL,NULL),(6,'标准配置','用于演示环境的常规记录，可随时调整。',15,15,NULL,NULL,3,'标准配置',53.00,NULL,'2026-08-08 05:58:36','2026-08-08 12:58:36',0,0,1,NULL,NULL,NULL,NULL),(7,'增值条目','系统自动补齐的示例数据。',17,17,NULL,NULL,1,'增值条目',60.00,NULL,'2026-08-13 07:11:36','2026-08-13 15:11:36',0,0,1,NULL,NULL,NULL,NULL),(8,'临时条目','按业务流程录入的一条典型记录。',19,19,NULL,NULL,2,'临时条目',67.00,NULL,'2026-08-18 08:24:36','2026-08-18 17:24:36',0,0,1,NULL,NULL,NULL,NULL),(9,'长期条目','运营日常维护产生的记录。',21,21,NULL,NULL,3,'长期条目',74.00,NULL,'2026-08-23 09:37:36','2026-08-23 19:37:36',0,0,1,NULL,NULL,NULL,NULL),(10,'专项记录','供联调与走查使用的样例内容。',23,23,NULL,NULL,1,'专项记录',81.00,NULL,'2026-08-28 10:50:36','2026-08-28 21:50:36',0,0,1,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `buy_get_free` ENABLE KEYS */;
UNLOCK TABLES;

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
  `rule_type` int DEFAULT NULL COMMENT '类型 1满减 2折扣 3秒杀 4新客立减 5买赠',
  `quantity` int DEFAULT NULL COMMENT '数量（秒杀购买件数/买赠赠品件数，满减可空）',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `order_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单号',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `order_amount` decimal(10,2) DEFAULT NULL COMMENT '商品金额',
  `discount_amount` decimal(10,2) DEFAULT NULL COMMENT '优惠金额',
  `actual_amount` decimal(10,2) DEFAULT NULL COMMENT '满减后金额',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign_usage_record`
--

LOCK TABLES `campaign_usage_record` WRITE;
/*!40000 ALTER TABLE `campaign_usage_record` DISABLE KEYS */;
INSERT INTO `campaign_usage_record` VALUES (1,1,NULL,NULL,NULL,NULL,NULL,NULL,18.00,18.00,18.00,NULL,1,'2026-07-14 00:53:36'),(2,2,NULL,NULL,NULL,NULL,NULL,NULL,25.00,25.00,25.00,NULL,1,'2026-07-19 02:06:36'),(3,3,NULL,NULL,NULL,NULL,NULL,NULL,32.00,32.00,32.00,NULL,1,'2026-07-24 03:19:36'),(4,1,NULL,NULL,NULL,NULL,NULL,NULL,39.00,39.00,39.00,NULL,1,'2026-07-29 04:32:36'),(5,2,NULL,NULL,NULL,NULL,NULL,NULL,46.00,46.00,46.00,NULL,1,'2026-08-03 05:45:36'),(6,3,NULL,NULL,NULL,NULL,NULL,NULL,53.00,53.00,53.00,NULL,1,'2026-08-08 05:58:36'),(7,1,NULL,NULL,NULL,NULL,NULL,NULL,60.00,60.00,60.00,NULL,1,'2026-08-13 07:11:36'),(8,2,NULL,NULL,NULL,NULL,NULL,NULL,67.00,67.00,67.00,NULL,1,'2026-08-18 08:24:36'),(9,3,NULL,NULL,NULL,NULL,NULL,NULL,74.00,74.00,74.00,NULL,1,'2026-08-23 09:37:36'),(10,1,NULL,NULL,NULL,NULL,NULL,NULL,81.00,81.00,81.00,NULL,1,'2026-08-28 10:50:36');
/*!40000 ALTER TABLE `campaign_usage_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cashier_record`
--

DROP TABLE IF EXISTS `cashier_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cashier_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `order_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单',
  `pay_type` int DEFAULT NULL COMMENT '收银类型 1-现金 2-徿 3-攻 4-银 5-会员储',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '收银金',
  `actual_amount` decimal(10,2) DEFAULT NULL COMMENT '实收金',
  `change_amount` decimal(10,2) DEFAULT NULL COMMENT '找零金',
  `cashier_time` datetime DEFAULT NULL COMMENT '收银时间',
  `cashier_id` bigint DEFAULT NULL COMMENT '收银员ID',
  `cashier_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收银员',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `voucher_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '付款凭证图片相对路径',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cashier_record`
--

LOCK TABLES `cashier_record` WRITE;
/*!40000 ALTER TABLE `cashier_record` DISABLE KEYS */;
INSERT INTO `cashier_record` VALUES (1,NULL,NULL,NULL,18.00,18.00,18.00,NULL,NULL,'常规项目','用于演示环境的常规记录，可随时调整。',NULL,1,NULL,NULL),(2,NULL,NULL,NULL,25.00,25.00,25.00,NULL,NULL,'默认项目','系统自动补齐的示例数据。',NULL,1,NULL,NULL),(3,NULL,NULL,NULL,32.00,32.00,32.00,NULL,NULL,'补充项目','按业务流程录入的一条典型记录。',NULL,1,NULL,NULL),(4,NULL,NULL,NULL,39.00,39.00,39.00,NULL,NULL,'备用配置','运营日常维护产生的记录。',NULL,1,NULL,NULL),(5,NULL,NULL,NULL,46.00,46.00,46.00,NULL,NULL,'扩展配置','供联调与走查使用的样例内容。',NULL,1,NULL,NULL),(6,NULL,NULL,NULL,53.00,53.00,53.00,NULL,NULL,'标准配置','用于演示环境的常规记录，可随时调整。',NULL,1,NULL,NULL),(7,NULL,NULL,NULL,60.00,60.00,60.00,NULL,NULL,'增值条目','系统自动补齐的示例数据。',NULL,1,NULL,NULL),(8,NULL,NULL,NULL,67.00,67.00,67.00,NULL,NULL,'临时条目','按业务流程录入的一条典型记录。',NULL,1,NULL,NULL),(9,NULL,NULL,NULL,74.00,74.00,74.00,NULL,NULL,'长期条目','运营日常维护产生的记录。',NULL,1,NULL,NULL),(10,NULL,NULL,NULL,81.00,81.00,81.00,NULL,NULL,'专项记录','供联调与走查使用的样例内容。',NULL,1,NULL,NULL);
/*!40000 ALTER TABLE `cashier_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` int NOT NULL DEFAULT '1' COMMENT '类型 1 菜品分类 2 套餐分类',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '顺序',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,1,'热菜',0,'2026-07-14 00:53:36','2026-07-17 00:53:36',1,1,0,1),(2,1,'凉菜',0,'2026-07-19 02:06:36','2026-07-23 02:06:36',2,2,0,1),(3,1,'主食',0,'2026-07-24 03:19:36','2026-07-29 03:19:36',3,3,0,1),(4,1,'汤羹',0,'2026-07-29 04:32:36','2026-08-04 04:32:36',1,1,0,1),(5,1,'饮品',0,'2026-08-03 05:45:36','2026-08-10 05:45:36',2,2,0,1),(6,1,'小食',0,'2026-08-08 05:58:36','2026-08-16 05:58:36',3,3,0,1),(7,1,'甜点',0,'2026-08-13 07:11:36','2026-08-22 07:11:36',1,1,0,1),(8,1,'招牌推荐',0,'2026-08-18 08:24:36','2026-08-28 08:24:36',2,2,0,1),(9,1,'热菜-2',0,'2026-08-23 09:37:36','2026-09-03 09:37:36',3,3,0,1),(10,1,'凉菜-2',0,'2026-08-28 10:50:36','2026-09-09 10:50:36',1,1,0,1);
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `complaint`
--

DROP TABLE IF EXISTS `complaint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `complaint` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `complaint_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '投诉编号',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户',
  `user_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户手机',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `order_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单编号',
  `complaint_type` int DEFAULT NULL COMMENT '投诉类型 1食品质量 2配送 3服务态度 4价格 5其他',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '投诉标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '投诉内容',
  `image_urls` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `status` int DEFAULT '0' COMMENT '状态 0待处理 1处理中 2已解决 3已关闭',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理人',
  `handle_result` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理结果',
  `compensation_amount` decimal(10,2) DEFAULT NULL COMMENT '补偿金',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `satisfaction` int DEFAULT NULL COMMENT '满意度 1-5',
  `user_feedback` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户反馈',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `complaint`
--

LOCK TABLES `complaint` WRITE;
/*!40000 ALTER TABLE `complaint` DISABLE KEYS */;
INSERT INTO `complaint` VALUES (1,'C2607140131',NULL,'常规项目',NULL,NULL,NULL,NULL,'周末特惠通知',NULL,NULL,0,NULL,'常规项目',NULL,18.00,NULL,NULL,NULL,1,NULL,NULL),(2,'C2607190132',NULL,'默认项目',NULL,NULL,NULL,NULL,'会员日活动说明',NULL,NULL,0,NULL,'默认项目',NULL,25.00,NULL,NULL,NULL,1,NULL,NULL),(3,'C2607240133',NULL,'补充项目',NULL,NULL,NULL,NULL,'配送范围调整公告',NULL,NULL,0,NULL,'补充项目',NULL,32.00,NULL,NULL,NULL,1,NULL,NULL),(4,'C2607290134',NULL,'备用配置',NULL,NULL,NULL,NULL,'菜单更新说明',NULL,NULL,0,NULL,'备用配置',NULL,39.00,NULL,NULL,NULL,1,NULL,NULL),(5,'C2608030135',NULL,'扩展配置',NULL,NULL,NULL,NULL,'门店歇业通知',NULL,NULL,0,NULL,'扩展配置',NULL,46.00,NULL,NULL,NULL,1,NULL,NULL),(6,'C2608080136',NULL,'标准配置',NULL,NULL,NULL,NULL,'新品上市介绍',NULL,NULL,0,NULL,'标准配置',NULL,53.00,NULL,NULL,NULL,1,NULL,NULL),(7,'C2608130137',NULL,'增值条目',NULL,NULL,NULL,NULL,'服务流程规范',NULL,NULL,0,NULL,'增值条目',NULL,60.00,NULL,NULL,NULL,1,NULL,NULL),(8,'C2608180138',NULL,'临时条目',NULL,NULL,NULL,NULL,'月度经营小结',NULL,NULL,0,NULL,'临时条目',NULL,67.00,NULL,NULL,NULL,1,NULL,NULL),(9,'C2608230139',NULL,'长期条目',NULL,NULL,NULL,NULL,'客户反馈处理记录',NULL,NULL,0,NULL,'长期条目',NULL,74.00,NULL,NULL,NULL,1,NULL,NULL),(10,'C2608280140',NULL,'专项记录',NULL,NULL,NULL,NULL,'系统升级安排',NULL,NULL,0,NULL,'专项记录',NULL,81.00,NULL,NULL,NULL,1,NULL,NULL);
/*!40000 ALTER TABLE `complaint` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cost_record`
--

DROP TABLE IF EXISTS `cost_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cost_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cost_type` int DEFAULT NULL COMMENT '成本类型 1-食材/2-人工/3-其他',
  `ref_id` bigint DEFAULT NULL COMMENT '关联ID',
  `ref_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联名称',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '成本金',
  `cost_date` datetime DEFAULT NULL COMMENT '成本日期',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐锁版朏',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cost_record`
--

LOCK TABLES `cost_record` WRITE;
/*!40000 ALTER TABLE `cost_record` DISABLE KEYS */;
INSERT INTO `cost_record` VALUES (1,NULL,NULL,'常规项目',18.00,NULL,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,0),(2,NULL,NULL,'默认项目',25.00,NULL,'系统自动补齐的示例数据。',1,NULL,NULL,0),(3,NULL,NULL,'补充项目',32.00,NULL,'按业务流程录入的一条典型记录。',1,NULL,NULL,0),(4,NULL,NULL,'备用配置',39.00,NULL,'运营日常维护产生的记录。',1,NULL,NULL,0),(5,NULL,NULL,'扩展配置',46.00,NULL,'供联调与走查使用的样例内容。',1,NULL,NULL,0),(6,NULL,NULL,'标准配置',53.00,NULL,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,0),(7,NULL,NULL,'增值条目',60.00,NULL,'系统自动补齐的示例数据。',1,NULL,NULL,0),(8,NULL,NULL,'临时条目',67.00,NULL,'按业务流程录入的一条典型记录。',1,NULL,NULL,0),(9,NULL,NULL,'长期条目',74.00,NULL,'运营日常维护产生的记录。',1,NULL,NULL,0),(10,NULL,NULL,'专项记录',81.00,NULL,'供联调与走查使用的样例内容。',1,NULL,NULL,0);
/*!40000 ALTER TABLE `cost_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupon_template`
--

DROP TABLE IF EXISTS `coupon_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板名称',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型',
  `condition_amount` decimal(10,2) DEFAULT NULL COMMENT '满减条件金',
  `discount_amount` decimal(10,2) DEFAULT NULL COMMENT '优惠金',
  `discount_rate` decimal(3,2) DEFAULT NULL COMMENT '折扣',
  `total_count` int DEFAULT '0' COMMENT '发放总数',
  `remain_count` int DEFAULT '0' COMMENT '剩余数量',
  `valid_days` int DEFAULT NULL COMMENT '有效天数',
  `points_price` int DEFAULT NULL COMMENT '积分兑换所需积分',
  `status` int DEFAULT '1' COMMENT '状',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupon_template`
--

LOCK TABLES `coupon_template` WRITE;
/*!40000 ALTER TABLE `coupon_template` DISABLE KEYS */;
INSERT INTO `coupon_template` VALUES (1,1,'周末满减酬宾',NULL,18.00,18.00,0.55,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0),(2,1,'新客首单立减',NULL,25.00,25.00,0.59,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0),(3,1,'老客回馈券',NULL,32.00,32.00,0.63,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0),(4,1,'周三会员日五折',NULL,39.00,39.00,0.67,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0),(5,1,'下单返券活动',NULL,46.00,46.00,0.71,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0),(6,1,'下午茶专享券',NULL,53.00,53.00,0.75,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0),(7,1,'雨天暖心补贴',NULL,60.00,60.00,0.79,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0),(8,1,'周末满减酬宾-2',NULL,67.00,67.00,0.83,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0),(9,1,'新客首单立减-2',NULL,74.00,74.00,0.87,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0),(10,1,'老客回馈券-2',NULL,81.00,81.00,0.91,0,0,NULL,NULL,1,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `coupon_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupon_user`
--

DROP TABLE IF EXISTS `coupon_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `member_id` bigint DEFAULT NULL COMMENT '会员ID',
  `template_id` bigint DEFAULT NULL COMMENT '优惠券模板ID',
  `code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '优惠券码',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'unused' COMMENT '状态 UNUSED/USED/EXPIRED',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `order_id` bigint DEFAULT NULL COMMENT '使用订单ID',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupon_user`
--

LOCK TABLES `coupon_user` WRITE;
/*!40000 ALTER TABLE `coupon_user` DISABLE KEYS */;
INSERT INTO `coupon_user` VALUES (1,1,NULL,NULL,'CU2607140171','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(2,1,NULL,NULL,'CU2607190172','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(3,1,NULL,NULL,'CU2607240173','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(4,1,NULL,NULL,'CU2607290174','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,'CU2608030175','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(6,1,NULL,NULL,'CU2608080176','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(7,1,NULL,NULL,'CU2608130177','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(8,1,NULL,NULL,'CU2608180178','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(9,1,NULL,NULL,'CU2608230179','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(10,1,NULL,NULL,'CU2608280180','unused',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `coupon_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cs_message`
--

DROP TABLE IF EXISTS `cs_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cs_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_type` int DEFAULT NULL COMMENT '发送方 1用户 2客服 3系统',
  `sender_id` bigint DEFAULT NULL COMMENT '发送方ID',
  `sender_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发送方',
  `message_type` int DEFAULT '1' COMMENT '消息类型 1文本 2图片 3订单卡片',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片URL',
  `is_read` int DEFAULT '0' COMMENT '是否已读 0否 1是',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cs_message`
--

LOCK TABLES `cs_message` WRITE;
/*!40000 ALTER TABLE `cs_message` DISABLE KEYS */;
INSERT INTO `cs_message` VALUES (1,1,NULL,NULL,'常规项目',1,NULL,NULL,0,1,NULL),(2,2,NULL,NULL,'默认项目',1,NULL,NULL,0,1,NULL),(3,3,NULL,NULL,'补充项目',1,NULL,NULL,0,1,NULL),(4,1,NULL,NULL,'备用配置',1,NULL,NULL,0,1,NULL),(5,2,NULL,NULL,'扩展配置',1,NULL,NULL,0,1,NULL),(6,3,NULL,NULL,'标准配置',1,NULL,NULL,0,1,NULL),(7,1,NULL,NULL,'增值条目',1,NULL,NULL,0,1,NULL),(8,2,NULL,NULL,'临时条目',1,NULL,NULL,0,1,NULL),(9,3,NULL,NULL,'长期条目',1,NULL,NULL,0,1,NULL),(10,1,NULL,NULL,'专项记录',1,NULL,NULL,0,1,NULL);
/*!40000 ALTER TABLE `cs_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cs_session`
--

DROP TABLE IF EXISTS `cs_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cs_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话编号',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户',
  `agent_id` bigint DEFAULT NULL COMMENT '客服ID',
  `agent_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客服姓名',
  `session_type` int DEFAULT NULL COMMENT '会话类型 1通用 2订单 3投诉',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `status` int DEFAULT '0' COMMENT '状态 0等待 1进行中 2已关闭',
  `first_response_time` datetime DEFAULT NULL COMMENT '首次响应时间',
  `close_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `satisfaction_rating` int DEFAULT NULL COMMENT '满意度评分(1-5)',
  `user_feedback` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户反馈',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cs_session`
--

LOCK TABLES `cs_session` WRITE;
/*!40000 ALTER TABLE `cs_session` DISABLE KEYS */;
INSERT INTO `cs_session` VALUES (1,'CS2607140051',NULL,'常规项目',NULL,'常规项目',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL),(2,'CS2607190052',NULL,'默认项目',NULL,'默认项目',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL),(3,'CS2607240053',NULL,'补充项目',NULL,'补充项目',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL),(4,'CS2607290054',NULL,'备用配置',NULL,'备用配置',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL),(5,'CS2608030055',NULL,'扩展配置',NULL,'扩展配置',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL),(6,'CS2608080056',NULL,'标准配置',NULL,'标准配置',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL),(7,'CS2608130057',NULL,'增值条目',NULL,'增值条目',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL),(8,'CS2608180058',NULL,'临时条目',NULL,'临时条目',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL),(9,'CS2608230059',NULL,'长期条目',NULL,'长期条目',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL),(10,'CS2608280060',NULL,'专项记录',NULL,'专项记录',NULL,NULL,0,NULL,NULL,NULL,NULL,1,NULL,NULL);
/*!40000 ALTER TABLE `cs_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `daily_settlement`
--

DROP TABLE IF EXISTS `daily_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_date` date DEFAULT NULL COMMENT '结算日期',
  `total_revenue` decimal(10,2) DEFAULT NULL COMMENT '营业',
  `cash_income` decimal(10,2) DEFAULT NULL COMMENT '现金收入',
  `wechat_income` decimal(10,2) DEFAULT NULL COMMENT '徿收入',
  `alipay_income` decimal(10,2) DEFAULT NULL COMMENT '攻宝收',
  `bankcard_income` decimal(10,2) DEFAULT NULL COMMENT '银卡收',
  `other_income` decimal(10,2) DEFAULT NULL COMMENT '其他收入',
  `order_count` int DEFAULT NULL COMMENT '订单数量',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '款金',
  `refund_count` int DEFAULT NULL COMMENT '款数',
  `net_income` decimal(10,2) DEFAULT NULL COMMENT '收入',
  `material_cost` decimal(10,2) DEFAULT NULL COMMENT '食材成本',
  `labor_cost` decimal(10,2) DEFAULT NULL COMMENT '人工成本',
  `other_cost` decimal(10,2) DEFAULT NULL COMMENT '其他成本',
  `total_cost` decimal(10,2) DEFAULT NULL COMMENT '总成',
  `gross_profit` decimal(10,2) DEFAULT NULL COMMENT '毛利',
  `profit_rate` decimal(10,2) DEFAULT NULL COMMENT '毛利(%)',
  `status` int DEFAULT '0' COMMENT '结账状 0-朻 1-已结',
  `settlement_time` datetime DEFAULT NULL COMMENT '结账时间',
  `settlement_user_id` bigint DEFAULT NULL COMMENT '结账人ID',
  `settlement_user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结账人',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐锁版朏',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `daily_settlement`
--

LOCK TABLES `daily_settlement` WRITE;
/*!40000 ALTER TABLE `daily_settlement` DISABLE KEYS */;
INSERT INTO `daily_settlement` VALUES (1,NULL,18.00,10.00,10.00,18.00,10.00,10.00,NULL,18.00,NULL,10.00,18.00,18.00,18.00,18.00,10.00,0.55,0,NULL,NULL,'常规项目','用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL,0),(2,NULL,25.00,11.00,11.00,25.00,11.00,11.00,NULL,25.00,NULL,11.00,25.00,25.00,25.00,25.00,11.00,0.59,0,NULL,NULL,'默认项目','系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL,0),(3,NULL,32.00,12.00,12.00,32.00,12.00,12.00,NULL,32.00,NULL,12.00,32.00,32.00,32.00,32.00,12.00,0.63,0,NULL,NULL,'补充项目','按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL,0),(4,NULL,39.00,13.00,13.00,39.00,13.00,13.00,NULL,39.00,NULL,13.00,39.00,39.00,39.00,39.00,13.00,0.67,0,NULL,NULL,'备用配置','运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL,0),(5,NULL,46.00,14.00,14.00,46.00,14.00,14.00,NULL,46.00,NULL,14.00,46.00,46.00,46.00,46.00,14.00,0.71,0,NULL,NULL,'扩展配置','供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL,0),(6,NULL,53.00,15.00,15.00,53.00,15.00,15.00,NULL,53.00,NULL,15.00,53.00,53.00,53.00,53.00,15.00,0.75,0,NULL,NULL,'标准配置','用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL,0),(7,NULL,60.00,16.00,16.00,60.00,16.00,16.00,NULL,60.00,NULL,16.00,60.00,60.00,60.00,60.00,16.00,0.79,0,NULL,NULL,'增值条目','系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL,0),(8,NULL,67.00,17.00,17.00,67.00,17.00,17.00,NULL,67.00,NULL,17.00,67.00,67.00,67.00,67.00,17.00,0.83,0,NULL,NULL,'临时条目','按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL,0),(9,NULL,74.00,18.00,18.00,74.00,18.00,18.00,NULL,74.00,NULL,18.00,74.00,74.00,74.00,74.00,18.00,0.87,0,NULL,NULL,'长期条目','运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL,0),(10,NULL,81.00,19.00,19.00,81.00,19.00,19.00,NULL,81.00,NULL,19.00,81.00,81.00,81.00,81.00,19.00,0.91,0,NULL,NULL,'专项记录','供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `daily_settlement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `delivery_fee_step`
--

DROP TABLE IF EXISTS `delivery_fee_step`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_fee_step` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_id` bigint NOT NULL,
  `start_distance` decimal(10,2) NOT NULL,
  `end_distance` decimal(10,2) NOT NULL,
  `fee` decimal(10,2) NOT NULL,
  `increment_distance` decimal(10,2) DEFAULT NULL,
  `increment_fee` decimal(10,2) DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `delivery_fee_step`
--

LOCK TABLES `delivery_fee_step` WRITE;
/*!40000 ALTER TABLE `delivery_fee_step` DISABLE KEYS */;
INSERT INTO `delivery_fee_step` VALUES (1,1,1.50,1.50,2.00,1.50,2.00,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(2,2,2.00,2.00,2.50,2.00,2.50,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(3,3,2.50,2.50,3.00,2.50,3.00,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(4,1,3.00,3.00,3.50,3.00,3.50,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(5,2,3.50,3.50,4.00,3.50,4.00,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(6,3,4.00,4.00,4.50,4.00,4.50,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(7,1,4.50,4.50,5.00,4.50,5.00,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(8,2,5.00,5.00,5.50,5.00,5.50,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(9,3,5.50,5.50,6.00,5.50,6.00,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(10,1,6.00,6.00,6.50,6.00,6.50,0,1,'2026-09-30 15:53:52','2026-09-30 15:53:52');
/*!40000 ALTER TABLE `delivery_fee_step` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `delivery_order`
--

DROP TABLE IF EXISTS `delivery_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `platform_order_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台订单',
  `platform` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配平',
  `order_id` bigint DEFAULT NULL COMMENT '本地订单ID（关联 orders.id，可空）',
  `dish_summary` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜品摘',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '订单金',
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户姓名',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ϵ绰',
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配地',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单状',
  `order_time` datetime DEFAULT NULL COMMENT '下单时间',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `created_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `version` int DEFAULT NULL COMMENT '乐锁版朏',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `delivery_order`
--

LOCK TABLES `delivery_order` WRITE;
/*!40000 ALTER TABLE `delivery_order` DISABLE KEYS */;
INSERT INTO `delivery_order` VALUES (1,1,NULL,NULL,NULL,NULL,18.00,'三公里基础范围',NULL,'北京市朝阳区望京街道广顺北大街33号院',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL),(2,1,NULL,NULL,NULL,NULL,25.00,'五公里扩展范围',NULL,'北京市海淀区中关村大街27号',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL),(3,1,NULL,NULL,NULL,NULL,32.00,'核心商务区',NULL,'北京市东城区建国门内大街5号',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL),(4,1,NULL,NULL,NULL,NULL,39.00,'高校聚集区',NULL,'北京市西城区金融大街28号',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL),(5,1,NULL,NULL,NULL,NULL,46.00,'三公里基础范围-2',NULL,'北京市丰台区南三环西路5号',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL),(6,1,NULL,NULL,NULL,NULL,53.00,'五公里扩展范围-2',NULL,'北京市通州区新华大街16号',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL),(7,1,NULL,NULL,NULL,NULL,60.00,'核心商务区-2',NULL,'北京市石景山区鲁谷路12号',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL),(8,1,NULL,NULL,NULL,NULL,67.00,'高校聚集区-2',NULL,'北京市昌平区回龙观东大街18号',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL),(9,1,NULL,NULL,NULL,NULL,74.00,'三公里基础范围-3',NULL,'北京市朝阳区望京街道广顺北大街33号院',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL),(10,1,NULL,NULL,NULL,NULL,81.00,'五公里扩展范围-3',NULL,'北京市海淀区中关村大街27号',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL);
/*!40000 ALTER TABLE `delivery_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `delivery_range_rule`
--

DROP TABLE IF EXISTS `delivery_range_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_range_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `range_type` tinyint NOT NULL,
  `center_longitude` decimal(12,8) DEFAULT NULL,
  `center_latitude` decimal(12,8) DEFAULT NULL,
  `radius` decimal(10,2) DEFAULT NULL,
  `polygon_points` text COLLATE utf8mb4_unicode_ci,
  `fee_type` tinyint DEFAULT '1',
  `base_fee` decimal(10,2) DEFAULT '0.00',
  `fee_per_km` decimal(10,2) DEFAULT NULL,
  `min_fee` decimal(10,2) DEFAULT NULL,
  `max_fee` decimal(10,2) DEFAULT NULL,
  `free_threshold` decimal(10,2) DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `delivery_range_rule`
--

LOCK TABLES `delivery_range_rule` WRITE;
/*!40000 ALTER TABLE `delivery_range_rule` DISABLE KEYS */;
INSERT INTO `delivery_range_rule` VALUES (1,'三公里基础范围',1,116.33000000,39.92000000,1.50,NULL,1,2.00,2.00,2.00,2.00,10.00,1,0,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(2,'五公里扩展范围',2,116.34000000,39.93000000,2.00,NULL,1,2.50,2.50,2.50,2.50,11.00,1,0,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(3,'核心商务区',3,116.35000000,39.94000000,2.50,NULL,1,3.00,3.00,3.00,3.00,12.00,1,0,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(4,'高校聚集区',4,116.36000000,39.95000000,3.00,NULL,1,3.50,3.50,3.50,3.50,13.00,1,0,'运营日常维护产生的记录。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(5,'三公里基础范围-2',1,116.37000000,39.96000000,3.50,NULL,1,4.00,4.00,4.00,4.00,14.00,1,0,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(6,'五公里扩展范围-2',2,116.38000000,39.97000000,4.00,NULL,1,4.50,4.50,4.50,4.50,15.00,1,0,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(7,'核心商务区-2',3,116.39000000,39.98000000,4.50,NULL,1,5.00,5.00,5.00,5.00,16.00,1,0,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(8,'高校聚集区-2',4,116.31000000,39.99000000,5.00,NULL,1,5.50,5.50,5.50,5.50,17.00,1,0,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(9,'三公里基础范围-3',1,116.31100000,39.91000000,5.50,NULL,1,6.00,6.00,6.00,6.00,18.00,1,0,'运营日常维护产生的记录。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(10,'五公里扩展范围-3',2,116.31200000,39.91100000,6.00,NULL,1,6.50,6.50,6.50,6.50,19.00,1,0,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL);
/*!40000 ALTER TABLE `delivery_range_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `delivery_time_record`
--

DROP TABLE IF EXISTS `delivery_time_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_time_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rider_id` bigint DEFAULT NULL,
  `rider_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_time` datetime DEFAULT NULL,
  `accept_time` datetime DEFAULT NULL,
  `pickup_time` datetime DEFAULT NULL,
  `deliver_time` datetime DEFAULT NULL,
  `estimated_minutes` int DEFAULT NULL,
  `actual_minutes` int DEFAULT NULL,
  `distance` decimal(10,2) DEFAULT NULL,
  `status` int DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900015 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `delivery_time_record`
--

LOCK TABLES `delivery_time_record` WRITE;
/*!40000 ALTER TABLE `delivery_time_record` DISABLE KEYS */;
INSERT INTO `delivery_time_record` VALUES (1,102,NULL,NULL,'三公里基础范围',NULL,NULL,NULL,NULL,NULL,NULL,1.50,0,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49'),(2,101,NULL,NULL,'五公里扩展范围',NULL,NULL,NULL,NULL,NULL,NULL,2.00,0,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49'),(3,100,NULL,NULL,'核心商务区',NULL,NULL,NULL,NULL,NULL,NULL,2.50,0,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:49','2026-09-30 15:53:49'),(900008,900000122,NULL,NULL,'五公里扩展范围',NULL,NULL,NULL,NULL,NULL,NULL,2.50,0,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 16:08:55','2026-09-30 16:08:55'),(900009,900000128,NULL,NULL,'三公里基础范围',NULL,NULL,NULL,NULL,NULL,NULL,2.00,0,'按业务流程录入的一条典型记录。',1,'2026-09-30 16:08:55','2026-09-30 16:08:55'),(900010,900000121,NULL,NULL,'核心商务区',NULL,NULL,NULL,NULL,NULL,NULL,1.50,0,'系统自动补齐的示例数据。',1,'2026-09-30 16:08:55','2026-09-30 16:08:55'),(900011,900000127,NULL,NULL,'高校聚集区',NULL,NULL,NULL,NULL,NULL,NULL,2.50,0,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 16:08:55','2026-09-30 16:08:55'),(900013,900000126,NULL,NULL,'五公里扩展范围-2',NULL,NULL,NULL,NULL,NULL,NULL,1.50,0,'系统自动补齐的示例数据。',1,'2026-09-30 16:08:56','2026-09-30 16:08:56');
/*!40000 ALTER TABLE `delivery_time_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dining_area`
--

DROP TABLE IF EXISTS `dining_area`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_area` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区域名称',
  `sort` int DEFAULT '0' COMMENT '排序',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dining_area`
--

LOCK TABLES `dining_area` WRITE;
/*!40000 ALTER TABLE `dining_area` DISABLE KEYS */;
INSERT INTO `dining_area` VALUES (1,1,'三公里基础范围',0,NULL,NULL,NULL,NULL,0),(2,1,'五公里扩展范围',0,NULL,NULL,NULL,NULL,0),(3,1,'核心商务区',0,NULL,NULL,NULL,NULL,0),(4,1,'高校聚集区',0,NULL,NULL,NULL,NULL,0),(5,1,'三公里基础范围-2',0,NULL,NULL,NULL,NULL,0),(6,1,'五公里扩展范围-2',0,NULL,NULL,NULL,NULL,0),(7,1,'核心商务区-2',0,NULL,NULL,NULL,NULL,0),(8,1,'高校聚集区-2',0,NULL,NULL,NULL,NULL,0),(9,1,'三公里基础范围-3',0,NULL,NULL,NULL,NULL,0),(10,1,'五公里扩展范围-3',0,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `dining_area` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dining_queue`
--

DROP TABLE IF EXISTS `dining_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_queue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `queue_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '排队',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机',
  `user_id` bigint DEFAULT NULL COMMENT '取号顾客用户ID',
  `seat_count` int DEFAULT NULL COMMENT '人数',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态 WAITING/CALLED/CANCELLED/SERVED',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dining_queue`
--

LOCK TABLES `dining_queue` WRITE;
/*!40000 ALTER TABLE `dining_queue` DISABLE KEYS */;
INSERT INTO `dining_queue` VALUES (1,1,'DQ2607140061',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(2,1,'DQ2607190062',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(3,1,'DQ2607240063',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(4,1,'DQ2607290064',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(5,1,'DQ2608030065',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(6,1,'DQ2608080066',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(7,1,'DQ2608130067',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(8,1,'DQ2608180068',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(9,1,'DQ2608230069',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(10,1,'DQ2608280070',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `dining_queue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dining_reservation`
--

DROP TABLE IF EXISTS `dining_reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `table_id` bigint DEFAULT NULL COMMENT '桌台ID',
  `customer_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '顾姓名',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机',
  `user_id` bigint DEFAULT NULL COMMENT '预订顾客用户ID',
  `reserved_time` datetime DEFAULT NULL COMMENT '预时间',
  `seat_count` int DEFAULT NULL COMMENT '人数',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态 PENDING/CONFIRMED/CANCELLED/ARRIVED',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dining_reservation`
--

LOCK TABLES `dining_reservation` WRITE;
/*!40000 ALTER TABLE `dining_reservation` DISABLE KEYS */;
INSERT INTO `dining_reservation` VALUES (1,1,NULL,'常规项目',NULL,NULL,NULL,NULL,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,0),(2,1,NULL,'默认项目',NULL,NULL,NULL,NULL,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,0),(3,1,NULL,'补充项目',NULL,NULL,NULL,NULL,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,0),(4,1,NULL,'备用配置',NULL,NULL,NULL,NULL,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,0),(5,1,NULL,'扩展配置',NULL,NULL,NULL,NULL,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,0),(6,1,NULL,'标准配置',NULL,NULL,NULL,NULL,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,0),(7,1,NULL,'增值条目',NULL,NULL,NULL,NULL,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,0),(8,1,NULL,'临时条目',NULL,NULL,NULL,NULL,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,0),(9,1,NULL,'长期条目',NULL,NULL,NULL,NULL,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,0),(10,1,NULL,'专项记录',NULL,NULL,NULL,NULL,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `dining_reservation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dining_table`
--

DROP TABLE IF EXISTS `dining_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `area_id` bigint DEFAULT NULL COMMENT '区域ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '桌台名称',
  `seat_count` int DEFAULT NULL COMMENT '座位',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态 FREE/OCCUPIED/RESERVED/CLEANING',
  `min_amount` decimal(10,2) DEFAULT NULL COMMENT '低消',
  `qr_code_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '二维码URL',
  `current_order_id` bigint DEFAULT NULL COMMENT '当前关联订单ID（开台后绑定',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dining_table`
--

LOCK TABLES `dining_table` WRITE;
/*!40000 ALTER TABLE `dining_table` DISABLE KEYS */;
INSERT INTO `dining_table` VALUES (1,1,NULL,'常规项目',NULL,NULL,18.00,'images/demo/dish-01.jpg',NULL,0,NULL,NULL,NULL,NULL,0),(2,1,NULL,'默认项目',NULL,NULL,25.00,'images/demo/store-02.jpg',NULL,0,NULL,NULL,NULL,NULL,0),(3,1,NULL,'补充项目',NULL,NULL,32.00,'images/demo/banner-03.jpg',NULL,0,NULL,NULL,NULL,NULL,0),(4,1,NULL,'备用配置',NULL,NULL,39.00,'images/demo/avatar-04.png',NULL,0,NULL,NULL,NULL,NULL,0),(5,1,NULL,'扩展配置',NULL,NULL,46.00,'images/demo/logo-05.png',NULL,0,NULL,NULL,NULL,NULL,0),(6,1,NULL,'标准配置',NULL,NULL,53.00,'images/demo/dish-01.jpg',NULL,0,NULL,NULL,NULL,NULL,0),(7,1,NULL,'增值条目',NULL,NULL,60.00,'images/demo/store-02.jpg',NULL,0,NULL,NULL,NULL,NULL,0),(8,1,NULL,'临时条目',NULL,NULL,67.00,'images/demo/banner-03.jpg',NULL,0,NULL,NULL,NULL,NULL,0),(9,1,NULL,'长期条目',NULL,NULL,74.00,'images/demo/avatar-04.png',NULL,0,NULL,NULL,NULL,NULL,0),(10,1,NULL,'专项记录',NULL,NULL,81.00,'images/demo/logo-05.png',NULL,0,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `dining_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `discount_rule`
--

DROP TABLE IF EXISTS `discount_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `discount_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scope` tinyint NOT NULL,
  `discount_rate` decimal(5,4) NOT NULL,
  `max_discount_amount` decimal(10,2) DEFAULT NULL,
  `min_consumption` decimal(10,2) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `dish_id` bigint DEFAULT NULL,
  `setmeal_id` bigint DEFAULT NULL,
  `daily_limit` int DEFAULT NULL,
  `per_user_limit` int DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `discount_rule`
--

LOCK TABLES `discount_rule` WRITE;
/*!40000 ALTER TABLE `discount_rule` DISABLE KEYS */;
INSERT INTO `discount_rule` VALUES (1,1,'常规项目',1,0.5500,18.00,18.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(2,2,'默认项目',2,0.5900,25.00,25.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(3,3,'补充项目',3,0.6300,32.00,32.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(4,1,'备用配置',4,0.6700,39.00,39.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(5,2,'扩展配置',1,0.7100,46.00,46.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(6,3,'标准配置',2,0.7500,53.00,53.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(7,1,'增值条目',3,0.7900,60.00,60.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(8,2,'临时条目',4,0.8300,67.00,67.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(9,3,'长期条目',1,0.8700,74.00,74.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(10,1,'专项记录',2,0.9100,81.00,81.00,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL);
/*!40000 ALTER TABLE `discount_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish`
--

DROP TABLE IF EXISTS `dish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '菜品分类id',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜品名称',
  `price` decimal(10,2) NOT NULL COMMENT '菜品价格',
  `code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商品码',
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片',
  `description` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述信息',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0 停售 1 起售',
  `sort` int DEFAULT NULL COMMENT '顺序',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `stock_qty` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '当前库存数量',
  `min_stock` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '最低库存预警阈值',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish`
--

LOCK TABLES `dish` WRITE;
/*!40000 ALTER TABLE `dish` DISABLE KEYS */;
INSERT INTO `dish` VALUES (1,1,'宫保鸡丁',18.00,'D2607140091',NULL,'用于演示环境的常规记录，可随时调整。',1,NULL,'2026-07-14 00:53:36','2026-07-17 00:53:36',1,1,0,1,20.00,20.00),(2,2,'鱼香肉丝',25.00,'D2607190092',NULL,'系统自动补齐的示例数据。',1,NULL,'2026-07-19 02:06:36','2026-07-23 02:06:36',2,2,0,1,21.00,21.00),(3,3,'麻婆豆腐',32.00,'D2607240093',NULL,'按业务流程录入的一条典型记录。',1,NULL,'2026-07-24 03:19:36','2026-07-29 03:19:36',3,3,0,1,22.00,22.00),(4,1,'红烧狮子头',39.00,'D2607290094',NULL,'运营日常维护产生的记录。',1,NULL,'2026-07-29 04:32:36','2026-08-04 04:32:36',1,1,0,1,23.00,23.00),(5,2,'酸汤肥牛',46.00,'D2608030095',NULL,'供联调与走查使用的样例内容。',1,NULL,'2026-08-03 05:45:36','2026-08-10 05:45:36',2,2,0,1,24.00,24.00),(6,3,'干锅花菜',53.00,'D2608080096',NULL,'用于演示环境的常规记录，可随时调整。',1,NULL,'2026-08-08 05:58:36','2026-08-16 05:58:36',3,3,0,1,25.00,25.00),(7,1,'手撕包菜',60.00,'D2608130097',NULL,'系统自动补齐的示例数据。',1,NULL,'2026-08-13 07:11:36','2026-08-22 07:11:36',1,1,0,1,26.00,26.00),(8,2,'蒜蓉粉丝虾',67.00,'D2608180098',NULL,'按业务流程录入的一条典型记录。',1,NULL,'2026-08-18 08:24:36','2026-08-28 08:24:36',2,2,0,1,27.00,27.00),(9,3,'京味小酥肉',74.00,'D2608230099',NULL,'运营日常维护产生的记录。',1,NULL,'2026-08-23 09:37:36','2026-09-03 09:37:36',3,3,0,1,28.00,28.00),(10,1,'口水鸡',81.00,'D2608280100',NULL,'供联调与走查使用的样例内容。',1,NULL,'2026-08-28 10:50:36','2026-09-09 10:50:36',1,1,0,1,29.00,29.00);
/*!40000 ALTER TABLE `dish` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_cost`
--

DROP TABLE IF EXISTS `dish_cost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品ID',
  `dish_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜品名称',
  `material_cost` decimal(10,2) DEFAULT NULL COMMENT '食材成本',
  `labor_cost` decimal(10,2) DEFAULT NULL COMMENT '人工成本',
  `other_cost` decimal(10,2) DEFAULT NULL COMMENT '其他成本',
  `total_cost` decimal(10,2) DEFAULT NULL COMMENT '总成',
  `sale_price` decimal(10,2) DEFAULT NULL COMMENT '唻',
  `profit_rate` decimal(10,2) DEFAULT NULL COMMENT '毛利(%)',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_cost`
--

LOCK TABLES `dish_cost` WRITE;
/*!40000 ALTER TABLE `dish_cost` DISABLE KEYS */;
INSERT INTO `dish_cost` VALUES (1,NULL,'宫保鸡丁',18.00,18.00,18.00,18.00,18.00,0.55,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL),(2,NULL,'鱼香肉丝',25.00,25.00,25.00,25.00,25.00,0.59,'系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL),(3,NULL,'麻婆豆腐',32.00,32.00,32.00,32.00,32.00,0.63,'按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL),(4,NULL,'红烧狮子头',39.00,39.00,39.00,39.00,39.00,0.67,'运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL),(5,NULL,'酸汤肥牛',46.00,46.00,46.00,46.00,46.00,0.71,'供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL),(6,NULL,'干锅花菜',53.00,53.00,53.00,53.00,53.00,0.75,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL),(7,NULL,'手撕包菜',60.00,60.00,60.00,60.00,60.00,0.79,'系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL),(8,NULL,'蒜蓉粉丝虾',67.00,67.00,67.00,67.00,67.00,0.83,'按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL),(9,NULL,'京味小酥肉',74.00,74.00,74.00,74.00,74.00,0.87,'运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL),(10,NULL,'口水鸡',81.00,81.00,81.00,81.00,81.00,0.91,'供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `dish_cost` ENABLE KEYS */;
UNLOCK TABLES;

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
  `user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评价用户',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id（菜品下单时填）',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id（套餐下单时填，与dish_id互斥）',
  `dish_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商品名称（菜品名/套餐名）',
  `star_rating` int DEFAULT NULL COMMENT '评分(1-5)',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评价内容',
  `images` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评价图片JSON数组',
  `anonymous` int NOT NULL DEFAULT '0' COMMENT '是否匿名 0=实名 1=匿名',
  `reply_content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商家回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '商家回复时间',
  `status` int DEFAULT '0' COMMENT '审核状态：0待审核，1通过，2拒绝',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_evaluation`
--

LOCK TABLES `dish_evaluation` WRITE;
/*!40000 ALTER TABLE `dish_evaluation` DISABLE KEYS */;
INSERT INTO `dish_evaluation` VALUES (900000221,1,NULL,NULL,'宫保鸡丁',NULL,NULL,'宫保鸡丁',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0),(900000222,1,NULL,NULL,'鱼香肉丝',NULL,NULL,'鱼香肉丝',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0),(900000223,1,NULL,NULL,'麻婆豆腐',NULL,NULL,'麻婆豆腐',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0),(900000224,1,NULL,NULL,'红烧狮子头',NULL,NULL,'红烧狮子头',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0),(900000225,1,NULL,NULL,'酸汤肥牛',NULL,NULL,'酸汤肥牛',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0),(900000226,1,NULL,NULL,'干锅花菜',NULL,NULL,'干锅花菜',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0),(900000227,1,NULL,NULL,'手撕包菜',NULL,NULL,'手撕包菜',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0),(900000228,1,NULL,NULL,'蒜蓉粉丝虾',NULL,NULL,'蒜蓉粉丝虾',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0),(900000229,1,NULL,NULL,'京味小酥肉',NULL,NULL,'京味小酥肉',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0),(900000230,1,NULL,NULL,'口水鸡',NULL,NULL,'口水鸡',NULL,NULL,NULL,0,NULL,NULL,0,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `dish_evaluation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_flavor`
--

DROP TABLE IF EXISTS `dish_flavor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_flavor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '菜品',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '口味名称',
  `value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '口味数据list',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_flavor`
--

LOCK TABLES `dish_flavor` WRITE;
/*!40000 ALTER TABLE `dish_flavor` DISABLE KEYS */;
INSERT INTO `dish_flavor` VALUES (1,1,'宫保鸡丁',NULL,1,'2026-07-14 00:53:36','2026-07-17 00:53:36',1,1,0),(2,2,'鱼香肉丝',NULL,1,'2026-07-19 02:06:36','2026-07-23 02:06:36',2,2,0),(3,3,'麻婆豆腐',NULL,1,'2026-07-24 03:19:36','2026-07-29 03:19:36',3,3,0),(4,1,'红烧狮子头',NULL,1,'2026-07-29 04:32:36','2026-08-04 04:32:36',1,1,0),(5,2,'酸汤肥牛',NULL,1,'2026-08-03 05:45:36','2026-08-10 05:45:36',2,2,0),(6,3,'干锅花菜',NULL,1,'2026-08-08 05:58:36','2026-08-16 05:58:36',3,3,0),(7,1,'手撕包菜',NULL,1,'2026-08-13 07:11:36','2026-08-22 07:11:36',1,1,0),(8,2,'蒜蓉粉丝虾',NULL,1,'2026-08-18 08:24:36','2026-08-28 08:24:36',2,2,0),(9,3,'京味小酥肉',NULL,1,'2026-08-23 09:37:36','2026-09-03 09:37:36',3,3,0),(10,1,'口水鸡',NULL,1,'2026-08-28 10:50:36','2026-09-09 10:50:36',1,1,0);
/*!40000 ALTER TABLE `dish_flavor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_material`
--

DROP TABLE IF EXISTS `dish_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_material` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `DISH_ID` bigint DEFAULT NULL COMMENT '菜品ID',
  `MATERIAL_ID` bigint DEFAULT NULL COMMENT '食材ID',
  `USAGE_QTY` decimal(10,3) DEFAULT NULL COMMENT '单份菜品消耗食材数量',
  `SORT` int DEFAULT '0' COMMENT '排序',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_material`
--

LOCK TABLES `dish_material` WRITE;
/*!40000 ALTER TABLE `dish_material` DISABLE KEYS */;
INSERT INTO `dish_material` VALUES (1,1,NULL,NULL,10.000,0,NULL,NULL,NULL,NULL,0),(2,1,NULL,NULL,11.000,0,NULL,NULL,NULL,NULL,0),(3,1,NULL,NULL,12.000,0,NULL,NULL,NULL,NULL,0),(4,1,NULL,NULL,13.000,0,NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,14.000,0,NULL,NULL,NULL,NULL,0),(6,1,NULL,NULL,15.000,0,NULL,NULL,NULL,NULL,0),(7,1,NULL,NULL,16.000,0,NULL,NULL,NULL,NULL,0),(8,1,NULL,NULL,17.000,0,NULL,NULL,NULL,NULL,0),(9,1,NULL,NULL,18.000,0,NULL,NULL,NULL,NULL,0),(10,1,NULL,NULL,19.000,0,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `dish_material` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_platform_mapping`
--

DROP TABLE IF EXISTS `dish_platform_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_platform_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '朳统菜品ID',
  `platform_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  `platform_shop_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台侧门店ID',
  `platform_dish_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台菜品ID',
  `platform_sku_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台SKU ID',
  `price` decimal(10,2) DEFAULT NULL COMMENT '平台价格',
  `status` int NOT NULL DEFAULT '1' COMMENT '状 0下架 1上架',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_mapping_dish_platform` (`dish_id`,`platform_type`,`platform_dish_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_platform_mapping`
--

LOCK TABLES `dish_platform_mapping` WRITE;
/*!40000 ALTER TABLE `dish_platform_mapping` DISABLE KEYS */;
INSERT INTO `dish_platform_mapping` VALUES (1,1,'DEFAULT01',NULL,NULL,NULL,18.00,1,1,0,NULL,NULL),(2,2,'NORMAL02',NULL,NULL,NULL,25.00,1,1,0,NULL,NULL),(3,3,'SPECIAL03',NULL,NULL,NULL,32.00,1,1,0,NULL,NULL),(4,1,'TEMP04',NULL,NULL,NULL,39.00,1,1,0,NULL,NULL),(5,2,'CUSTOM05',NULL,NULL,NULL,46.00,1,1,0,NULL,NULL),(6,3,'BASIC06',NULL,NULL,NULL,53.00,1,1,0,NULL,NULL),(7,1,'DEFAULT07',NULL,NULL,NULL,60.00,1,1,0,NULL,NULL),(8,2,'NORMAL08',NULL,NULL,NULL,67.00,1,1,0,NULL,NULL),(9,3,'SPECIAL09',NULL,NULL,NULL,74.00,1,1,0,NULL,NULL),(10,1,'TEMP10',NULL,NULL,NULL,81.00,1,1,0,NULL,NULL);
/*!40000 ALTER TABLE `dish_platform_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_spec_group`
--

DROP TABLE IF EXISTS `dish_spec_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_spec_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` tinyint DEFAULT '1',
  `required` tinyint DEFAULT '0',
  `max_select` int DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_spec_group`
--

LOCK TABLES `dish_spec_group` WRITE;
/*!40000 ALTER TABLE `dish_spec_group` DISABLE KEYS */;
INSERT INTO `dish_spec_group` VALUES (1,'宫保鸡丁',1,0,NULL,0,1,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(2,'鱼香肉丝',1,0,NULL,0,1,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(3,'麻婆豆腐',1,0,NULL,0,1,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(4,'红烧狮子头',1,0,NULL,0,1,'运营日常维护产生的记录。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(5,'酸汤肥牛',1,0,NULL,0,1,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(6,'干锅花菜',1,0,NULL,0,1,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(7,'手撕包菜',1,0,NULL,0,1,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(8,'蒜蓉粉丝虾',1,0,NULL,0,1,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(9,'京味小酥肉',1,0,NULL,0,1,'运营日常维护产生的记录。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(10,'口水鸡',1,0,NULL,0,1,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL);
/*!40000 ALTER TABLE `dish_spec_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_spec_option`
--

DROP TABLE IF EXISTS `dish_spec_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_spec_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price_adjust` decimal(10,2) DEFAULT '0.00',
  `sort_order` int DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_spec_option`
--

LOCK TABLES `dish_spec_option` WRITE;
/*!40000 ALTER TABLE `dish_spec_option` DISABLE KEYS */;
INSERT INTO `dish_spec_option` VALUES (1,1,'宫保鸡丁',18.00,0,1,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(2,2,'鱼香肉丝',25.00,0,1,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(3,3,'麻婆豆腐',32.00,0,1,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(4,1,'红烧狮子头',39.00,0,1,'运营日常维护产生的记录。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(5,2,'酸汤肥牛',46.00,0,1,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(6,3,'干锅花菜',53.00,0,1,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(7,1,'手撕包菜',60.00,0,1,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(8,2,'蒜蓉粉丝虾',67.00,0,1,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(9,3,'京味小酥肉',74.00,0,1,'运营日常维护产生的记录。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL),(10,1,'口水鸡',81.00,0,1,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:51','2026-09-30 15:53:51',NULL,NULL);
/*!40000 ALTER TABLE `dish_spec_option` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_spec_relation`
--

DROP TABLE IF EXISTS `dish_spec_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_spec_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dish_id` bigint NOT NULL,
  `group_id` bigint NOT NULL,
  `sort_order` int DEFAULT '0',
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_spec_relation`
--

LOCK TABLES `dish_spec_relation` WRITE;
/*!40000 ALTER TABLE `dish_spec_relation` DISABLE KEYS */;
INSERT INTO `dish_spec_relation` VALUES (1,1,1,0,1,'2026-09-30 15:53:49',NULL),(2,2,2,0,1,'2026-09-30 15:53:49',NULL),(3,3,3,0,1,'2026-09-30 15:53:49',NULL),(4,1,1,0,1,'2026-09-30 15:53:49',NULL),(5,2,2,0,1,'2026-09-30 15:53:49',NULL),(6,3,3,0,1,'2026-09-30 15:53:49',NULL),(7,1,1,0,1,'2026-09-30 15:53:49',NULL),(8,2,2,0,1,'2026-09-30 15:53:49',NULL),(9,3,3,0,1,'2026-09-30 15:53:49',NULL),(10,1,1,0,1,'2026-09-30 15:53:49',NULL);
/*!40000 ALTER TABLE `dish_spec_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `sex` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像图片相对路径',
  `job_number` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工号（租户内唯一）',
  `position` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '岗位',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `password_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MD5' COMMENT '密码加密类型 MD5/BCRYPT',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `role` int NOT NULL DEFAULT '2' COMMENT '角色 1:超级管理员 2:普通员工',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee`
--

LOCK TABLES `employee` WRITE;
/*!40000 ALTER TABLE `employee` DISABLE KEYS */;
INSERT INTO `employee` VALUES (1,'admin','管理员','13800000000','1',NULL,NULL,NULL,NULL,1,'2026-09-30 15:53:49','2026-09-30 15:53:49',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,1),(2,'admin','管理员',NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-07-14 00:53:36','2026-07-17 00:53:36',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,2),(3,'王芳','王芳',NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-07-19 02:06:36','2026-07-23 02:06:36',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,2),(4,'李磊','李磊',NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-07-24 03:19:36','2026-07-29 03:19:36',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,2),(5,'赵磊','赵磊',NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-07-29 04:32:36','2026-08-04 04:32:36',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,2),(6,'刘敏','刘敏',NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-08-03 05:45:36','2026-08-10 05:45:36',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,2),(7,'陈敏','陈敏',NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-08-08 05:58:36','2026-08-16 05:58:36',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,2),(8,'杨静','杨静',NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-08-13 07:11:36','2026-08-22 07:11:36',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,2),(9,'黄静','黄静',NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-08-18 08:24:36','2026-08-28 08:24:36',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,2),(10,'周强','周强',NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-08-23 09:37:36','2026-09-03 09:37:36',1,1,'e10adc3949ba59abbe56e057f20f883e','MD5',1,2);
/*!40000 ALTER TABLE `employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee_role`
--

DROP TABLE IF EXISTS `employee_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_role` (`employee_id`,`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee_role`
--

LOCK TABLES `employee_role` WRITE;
/*!40000 ALTER TABLE `employee_role` DISABLE KEYS */;
INSERT INTO `employee_role` VALUES (1,6,6,1,'2026-09-30 15:53:51'),(2,9,9,1,'2026-09-30 15:53:51'),(3,2,2,1,'2026-09-30 15:53:51'),(4,5,5,1,'2026-09-30 15:53:51'),(5,8,8,1,'2026-09-30 15:53:51'),(6,1,1,1,'2026-09-30 15:53:51'),(7,4,4,1,'2026-09-30 15:53:51'),(8,7,7,1,'2026-09-30 15:53:51'),(9,10,10,1,'2026-09-30 15:53:51'),(10,3,3,1,'2026-09-30 15:53:51');
/*!40000 ALTER TABLE `employee_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flash_sale`
--

DROP TABLE IF EXISTS `flash_sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flash_sale` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `dish_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜品名称',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `flash_price` decimal(10,2) NOT NULL COMMENT '秒杀价',
  `total_quantity` int NOT NULL DEFAULT '0' COMMENT '总库存',
  `sold_quantity` int NOT NULL DEFAULT '0' COMMENT '已售数量',
  `max_per_user` int DEFAULT NULL COMMENT '每人限购',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态 0草稿 1进行中 2暂停 3结束',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flash_sale`
--

LOCK TABLES `flash_sale` WRITE;
/*!40000 ALTER TABLE `flash_sale` DISABLE KEYS */;
INSERT INTO `flash_sale` VALUES (1,'常规项目','用于演示环境的常规记录，可随时调整。',1,'常规项目',18.00,18.00,0,0,NULL,'2026-07-14 00:53:36','2026-07-14 02:53:36',0,1,NULL,NULL,NULL,NULL),(2,'默认项目','系统自动补齐的示例数据。',2,'默认项目',25.00,25.00,0,0,NULL,'2026-07-19 02:06:36','2026-07-19 05:06:36',0,1,NULL,NULL,NULL,NULL),(3,'补充项目','按业务流程录入的一条典型记录。',3,'补充项目',32.00,32.00,0,0,NULL,'2026-07-24 03:19:36','2026-07-24 07:19:36',0,1,NULL,NULL,NULL,NULL),(4,'备用配置','运营日常维护产生的记录。',1,'备用配置',39.00,39.00,0,0,NULL,'2026-07-29 04:32:36','2026-07-29 09:32:36',0,1,NULL,NULL,NULL,NULL),(5,'扩展配置','供联调与走查使用的样例内容。',2,'扩展配置',46.00,46.00,0,0,NULL,'2026-08-03 05:45:36','2026-08-03 11:45:36',0,1,NULL,NULL,NULL,NULL),(6,'标准配置','用于演示环境的常规记录，可随时调整。',3,'标准配置',53.00,53.00,0,0,NULL,'2026-08-08 05:58:36','2026-08-08 12:58:36',0,1,NULL,NULL,NULL,NULL),(7,'增值条目','系统自动补齐的示例数据。',1,'增值条目',60.00,60.00,0,0,NULL,'2026-08-13 07:11:36','2026-08-13 15:11:36',0,1,NULL,NULL,NULL,NULL),(8,'临时条目','按业务流程录入的一条典型记录。',2,'临时条目',67.00,67.00,0,0,NULL,'2026-08-18 08:24:36','2026-08-18 17:24:36',0,1,NULL,NULL,NULL,NULL),(9,'长期条目','运营日常维护产生的记录。',3,'长期条目',74.00,74.00,0,0,NULL,'2026-08-23 09:37:36','2026-08-23 19:37:36',0,1,NULL,NULL,NULL,NULL),(10,'专项记录','供联调与走查使用的样例内容。',1,'专项记录',81.00,81.00,0,0,NULL,'2026-08-28 10:50:36','2026-08-28 21:50:36',0,1,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `flash_sale` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `full_reduction_rule`
--

DROP TABLE IF EXISTS `full_reduction_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `full_reduction_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `campaign_id` bigint NOT NULL COMMENT '活动ID',
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规则名称',
  `discount_type` int NOT NULL COMMENT '类型 1减固定 2打折 3赠品',
  `min_amount` decimal(10,2) NOT NULL COMMENT '门槛金额',
  `discount_value` decimal(10,2) NOT NULL COMMENT '优惠值/折扣率',
  `max_discount_amount` decimal(10,2) DEFAULT NULL COMMENT '最大优惠金额',
  `gift_dish_id` bigint DEFAULT NULL COMMENT '赠品菜品ID',
  `gift_quantity` int DEFAULT NULL COMMENT '赠品数量',
  `stackable` int DEFAULT '0' COMMENT '是否可叠加',
  `daily_limit` int DEFAULT NULL COMMENT '每日限次',
  `per_user_limit` int DEFAULT NULL COMMENT '每人限次',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` int DEFAULT '1' COMMENT '状态 0禁用 1启用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `full_reduction_rule`
--

LOCK TABLES `full_reduction_rule` WRITE;
/*!40000 ALTER TABLE `full_reduction_rule` DISABLE KEYS */;
INSERT INTO `full_reduction_rule` VALUES (1,1,'常规项目',5,18.00,20.00,18.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-07-14 00:53:36','2026-07-17 00:53:36'),(2,2,'默认项目',7,25.00,21.00,25.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-07-19 02:06:36','2026-07-23 02:06:36'),(3,3,'补充项目',9,32.00,22.00,32.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-07-24 03:19:36','2026-07-29 03:19:36'),(4,1,'备用配置',11,39.00,23.00,39.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-07-29 04:32:36','2026-08-04 04:32:36'),(5,2,'扩展配置',13,46.00,24.00,46.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-08-03 05:45:36','2026-08-10 05:45:36'),(6,3,'标准配置',15,53.00,25.00,53.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-08-08 05:58:36','2026-08-16 05:58:36'),(7,1,'增值条目',17,60.00,26.00,60.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-08-13 07:11:36','2026-08-22 07:11:36'),(8,2,'临时条目',19,67.00,27.00,67.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-08-18 08:24:36','2026-08-28 08:24:36'),(9,3,'长期条目',21,74.00,28.00,74.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-08-23 09:37:36','2026-09-03 09:37:36'),(10,1,'专项记录',23,81.00,29.00,81.00,NULL,NULL,0,NULL,NULL,0,1,1,NULL,NULL,'2026-08-28 10:50:36','2026-09-09 10:50:36');
/*!40000 ALTER TABLE `full_reduction_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_buy_campaign`
--

DROP TABLE IF EXISTS `group_buy_campaign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_buy_campaign` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `DESCRIPTION` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '活动描述',
  `GROUP_ID` bigint NOT NULL DEFAULT '0' COMMENT '拼团组ID',
  `STATUS` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/CLOSED/ENDED',
  `START_TIME` datetime NOT NULL COMMENT '开始时间',
  `END_TIME` datetime NOT NULL COMMENT '结束时间',
  `MIN_MEMBERS` int NOT NULL DEFAULT '2' COMMENT '最少成团人数',
  `MAX_MEMBERS` int NOT NULL DEFAULT '10' COMMENT '最多成团人数',
  `ORIGINAL_PRICE` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '原价',
  `GROUP_PRICE` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '拼团价',
  `DISH_ID` bigint NOT NULL DEFAULT '0' COMMENT '菜品ID',
  `DISH_NAME` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '菜品名称',
  `IMAGE` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '活动图片URL',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_buy_campaign`
--

LOCK TABLES `group_buy_campaign` WRITE;
/*!40000 ALTER TABLE `group_buy_campaign` DISABLE KEYS */;
INSERT INTO `group_buy_campaign` VALUES (1,1,'周末满减酬宾','用于演示环境的常规记录，可随时调整。',0,'ENDED','2026-07-14 00:53:36','2026-07-14 02:53:36',2,10,18.00,18.00,0,'周末满减酬宾','',NULL,'2026-09-30 15:53:51',0),(2,1,'新客首单立减','系统自动补齐的示例数据。',0,'ENDED','2026-07-19 02:06:36','2026-07-19 05:06:36',2,10,25.00,25.00,0,'新客首单立减','',NULL,'2026-09-30 15:53:51',0),(3,1,'老客回馈券','按业务流程录入的一条典型记录。',0,'ENDED','2026-07-24 03:19:36','2026-07-24 07:19:36',2,10,32.00,32.00,0,'老客回馈券','',NULL,'2026-09-30 15:53:51',0),(4,1,'周三会员日五折','运营日常维护产生的记录。',0,'ENDED','2026-07-29 04:32:36','2026-07-29 09:32:36',2,10,39.00,39.00,0,'周三会员日五折','',NULL,'2026-09-30 15:53:51',0),(5,1,'下单返券活动','供联调与走查使用的样例内容。',0,'ENDED','2026-08-03 05:45:36','2026-08-03 11:45:36',2,10,46.00,46.00,0,'下单返券活动','',NULL,'2026-09-30 15:53:51',0),(6,1,'下午茶专享券','用于演示环境的常规记录，可随时调整。',0,'ENDED','2026-08-08 05:58:36','2026-08-08 12:58:36',2,10,53.00,53.00,0,'下午茶专享券','',NULL,'2026-09-30 15:53:51',0),(7,1,'雨天暖心补贴','系统自动补齐的示例数据。',0,'ENDED','2026-08-13 07:11:36','2026-08-13 15:11:36',2,10,60.00,60.00,0,'雨天暖心补贴','',NULL,'2026-09-30 15:53:51',0),(8,1,'周末满减酬宾-2','按业务流程录入的一条典型记录。',0,'ENDED','2026-08-18 08:24:36','2026-08-18 17:24:36',2,10,67.00,67.00,0,'周末满减酬宾-2','',NULL,'2026-09-30 15:53:51',0),(9,1,'新客首单立减-2','运营日常维护产生的记录。',0,'ENDED','2026-08-23 09:37:36','2026-08-23 19:37:36',2,10,74.00,74.00,0,'新客首单立减-2','',NULL,'2026-09-30 15:53:52',0),(10,1,'老客回馈券-2','供联调与走查使用的样例内容。',0,'ENDED','2026-08-28 10:50:36','2026-08-28 21:50:36',2,10,81.00,81.00,0,'老客回馈券-2','',NULL,'2026-09-30 15:53:52',0);
/*!40000 ALTER TABLE `group_buy_campaign` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_buy_participation`
--

DROP TABLE IF EXISTS `group_buy_participation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_buy_participation` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `GROUP_BUY_ID` bigint NOT NULL COMMENT '拼团活动ID',
  `ORDER_ID` bigint NOT NULL COMMENT '订单ID',
  `USER_ID` bigint NOT NULL COMMENT '用户ID',
  `STATUS` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'JOINED' COMMENT '状态：JOINED/PAID/CANCELLED',
  `JOIN_TIME` datetime NOT NULL COMMENT '参团时间',
  `PAY_TIME` datetime DEFAULT NULL COMMENT '支付时间',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_buy_participation`
--

LOCK TABLES `group_buy_participation` WRITE;
/*!40000 ALTER TABLE `group_buy_participation` DISABLE KEYS */;
INSERT INTO `group_buy_participation` VALUES (1,1,1,102,1,'JOINED','2026-07-14 00:53:36',NULL,NULL),(2,1,2,101,2,'JOINED','2026-07-19 02:06:36',NULL,NULL),(3,1,3,100,3,'JOINED','2026-07-24 03:19:36',NULL,NULL),(4,1,1,102,1,'JOINED','2026-07-29 04:32:36',NULL,NULL),(5,1,2,101,2,'JOINED','2026-08-03 05:45:36',NULL,NULL),(6,1,3,100,3,'JOINED','2026-08-08 05:58:36',NULL,NULL),(7,1,1,102,1,'JOINED','2026-08-13 07:11:36',NULL,NULL),(8,1,2,101,2,'JOINED','2026-08-18 08:24:36',NULL,NULL),(9,1,3,100,3,'JOINED','2026-08-23 09:37:36',NULL,NULL),(10,1,1,102,1,'JOINED','2026-08-28 10:50:36',NULL,NULL);
/*!40000 ALTER TABLE `group_buy_participation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invoice_record`
--

DROP TABLE IF EXISTS `invoice_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoice_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `user_id` bigint DEFAULT NULL COMMENT '申请用户ID（用户端归属列）',
  `order_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单号',
  `title_id` bigint DEFAULT NULL COMMENT '发票抬头ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发票抬头（冗余）',
  `tax_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '税号（冗余）',
  `type` int NOT NULL DEFAULT '1' COMMENT '类型：1=个人，2=企业',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '开票金额',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态：0=待申请，1=已申请，2=已开具，3=已作废',
  `invoice_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发票号码',
  `invoice_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发票代码',
  `invoice_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发票PDF地址',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `issue_time` datetime DEFAULT NULL COMMENT '开具时间',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invoice_record`
--

LOCK TABLES `invoice_record` WRITE;
/*!40000 ALTER TABLE `invoice_record` DISABLE KEYS */;
INSERT INTO `invoice_record` VALUES (1,102,NULL,'IR2607140191',NULL,'周末特惠通知',NULL,1,18.00,0,'IR2607140192','常规记录',NULL,NULL,NULL,1,NULL,NULL),(2,101,NULL,'IR2607190193',NULL,'会员日活动说明',NULL,1,25.00,0,'IR2607190194','默认记录',NULL,NULL,NULL,1,NULL,NULL),(3,100,NULL,'IR2607240195',NULL,'配送范围调整公告',NULL,1,32.00,0,'IR2607240196','补充记录',NULL,NULL,NULL,1,NULL,NULL),(4,102,NULL,'IR2607290197',NULL,'菜单更新说明',NULL,1,39.00,0,'IR2607290198','备用记录',NULL,NULL,NULL,1,NULL,NULL),(5,101,NULL,'IR2608030199',NULL,'门店歇业通知',NULL,1,46.00,0,'IR2608030200','扩展记录',NULL,NULL,NULL,1,NULL,NULL),(6,100,NULL,'IR2608080201',NULL,'新品上市介绍',NULL,1,53.00,0,'IR2608080202','标准记录',NULL,NULL,NULL,1,NULL,NULL),(7,102,NULL,'IR2608130203',NULL,'服务流程规范',NULL,1,60.00,0,'IR2608130204','增值记录',NULL,NULL,NULL,1,NULL,NULL),(8,101,NULL,'IR2608180205',NULL,'月度经营小结',NULL,1,67.00,0,'IR2608180206','临时记录',NULL,NULL,NULL,1,NULL,NULL),(9,100,NULL,'IR2608230207',NULL,'客户反馈处理记录',NULL,1,74.00,0,'IR2608230208','长期记录',NULL,NULL,NULL,1,NULL,NULL),(10,102,NULL,'IR2608280209',NULL,'系统升级安排',NULL,1,81.00,0,'IR2608280210','专项记录',NULL,NULL,NULL,1,NULL,NULL);
/*!40000 ALTER TABLE `invoice_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invoice_title`
--

DROP TABLE IF EXISTS `invoice_title`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoice_title` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发票抬头',
  `tax_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '税号',
  `company_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公司名称（企业用）',
  `type` int NOT NULL DEFAULT '1' COMMENT '类型：1=个人，2=企业',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint DEFAULT NULL COMMENT '归属用户ID（用户端隔离）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invoice_title`
--

LOCK TABLES `invoice_title` WRITE;
/*!40000 ALTER TABLE `invoice_title` DISABLE KEYS */;
INSERT INTO `invoice_title` VALUES (1,'周末特惠通知',NULL,'常规项目',1,1,NULL,NULL,NULL),(2,'会员日活动说明',NULL,'默认项目',1,1,NULL,NULL,NULL),(3,'配送范围调整公告',NULL,'补充项目',1,1,NULL,NULL,NULL),(4,'菜单更新说明',NULL,'备用配置',1,1,NULL,NULL,NULL),(5,'门店歇业通知',NULL,'扩展配置',1,1,NULL,NULL,NULL),(6,'新品上市介绍',NULL,'标准配置',1,1,NULL,NULL,NULL),(7,'服务流程规范',NULL,'增值条目',1,1,NULL,NULL,NULL),(8,'月度经营小结',NULL,'临时条目',1,1,NULL,NULL,NULL),(9,'客户反馈处理记录',NULL,'长期条目',1,1,NULL,NULL,NULL),(10,'系统升级安排',NULL,'专项记录',1,1,NULL,NULL,NULL);
/*!40000 ALTER TABLE `invoice_title` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `kitchen_ticket`
--

DROP TABLE IF EXISTS `kitchen_ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kitchen_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `order_id` bigint NOT NULL COMMENT '来源订单ID（幂等）',
  `order_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '取餐号/订单号快照',
  `order_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用餐类型快照',
  `table_name` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '堂食桌台',
  `customer_count` int DEFAULT NULL COMMENT '用餐人数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1待制作，2制作中，3待取餐，4已完成，5已取消',
  `urgent` tinyint NOT NULL DEFAULT '0' COMMENT '是否加急：0否，1是',
  `dish_summary` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜品摘要快照',
  `receive_time` datetime DEFAULT NULL COMMENT '接单/生成时间',
  `cook_start_time` datetime DEFAULT NULL COMMENT '开始制作时间',
  `ready_time` datetime DEFAULT NULL COMMENT '叫号时间',
  `finish_time` datetime DEFAULT NULL COMMENT '出餐完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cook_duration_seconds` bigint DEFAULT NULL COMMENT '制作耗时（秒）',
  `station_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '档口编码（如 HOT/COLD/DRINK/DESSERT）',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `kitchen_ticket`
--

LOCK TABLES `kitchen_ticket` WRITE;
/*!40000 ALTER TABLE `kitchen_ticket` DISABLE KEYS */;
INSERT INTO `kitchen_ticket` VALUES (1,1,102,'KT2607140111',NULL,'常规项目',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'常规通道','用于演示环境的常规记录，可随时调整。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0),(2,1,101,'KT2607190112',NULL,'默认项目',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'默认通道','系统自动补齐的示例数据。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0),(3,1,100,'KT2607240113',NULL,'补充项目',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'补充通道','按业务流程录入的一条典型记录。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0),(4,1,102,'KT2607290114',NULL,'备用配置',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'备用通道','运营日常维护产生的记录。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0),(5,1,101,'KT2608030115',NULL,'扩展配置',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'扩展通道','供联调与走查使用的样例内容。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0),(6,1,100,'KT2608080116',NULL,'标准配置',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'标准通道','用于演示环境的常规记录，可随时调整。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0),(7,1,102,'KT2608130117',NULL,'增值条目',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'增值通道','系统自动补齐的示例数据。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0),(8,1,101,'KT2608180118',NULL,'临时条目',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'临时通道','按业务流程录入的一条典型记录。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0),(9,1,100,'KT2608230119',NULL,'长期条目',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'长期通道','运营日常维护产生的记录。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0),(10,1,102,'KT2608280120',NULL,'专项记录',NULL,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'专项通道','供联调与走查使用的样例内容。','2026-09-30 15:53:50','2026-09-30 15:53:50',NULL,NULL,0);
/*!40000 ALTER TABLE `kitchen_ticket` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `labor_cost`
--

DROP TABLE IF EXISTS `labor_cost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `labor_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint DEFAULT NULL COMMENT '员工ID',
  `employee_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '员工姓名',
  `salary` decimal(10,2) DEFAULT NULL COMMENT '工资',
  `social_insurance` decimal(10,2) DEFAULT NULL COMMENT '籣',
  `housing_fund` decimal(10,2) DEFAULT NULL COMMENT '內',
  `other_benefits` decimal(10,2) DEFAULT NULL COMMENT '其他福利',
  `total_cost` decimal(10,2) DEFAULT NULL COMMENT '总成',
  `cost_month` date DEFAULT NULL COMMENT '成本月份',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `labor_cost`
--

LOCK TABLES `labor_cost` WRITE;
/*!40000 ALTER TABLE `labor_cost` DISABLE KEYS */;
INSERT INTO `labor_cost` VALUES (1,NULL,'常规项目',10.00,10.00,10.00,10.00,18.00,NULL,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL),(2,NULL,'默认项目',11.00,11.00,11.00,11.00,25.00,NULL,'系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL),(3,NULL,'补充项目',12.00,12.00,12.00,12.00,32.00,NULL,'按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL),(4,NULL,'备用配置',13.00,13.00,13.00,13.00,39.00,NULL,'运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL),(5,NULL,'扩展配置',14.00,14.00,14.00,14.00,46.00,NULL,'供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL),(6,NULL,'标准配置',15.00,15.00,15.00,15.00,53.00,NULL,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL),(7,NULL,'增值条目',16.00,16.00,16.00,16.00,60.00,NULL,'系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL),(8,NULL,'临时条目',17.00,17.00,17.00,17.00,67.00,NULL,'按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL),(9,NULL,'长期条目',18.00,18.00,18.00,18.00,74.00,NULL,'运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL),(10,NULL,'专项记录',19.00,19.00,19.00,19.00,81.00,NULL,'供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `labor_cost` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log`
--

DROP TABLE IF EXISTS `log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operate_user` bigint DEFAULT NULL COMMENT '操作人ID',
  `operate_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `module` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作模块',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作类型',
  `method` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL',
  `request_params` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数',
  `response_data` text COLLATE utf8mb4_unicode_ci COMMENT '响应数据',
  `ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `status` int DEFAULT NULL COMMENT '状态 0:失败 1:成功',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `cost_time` bigint DEFAULT NULL COMMENT '耗时(ms)',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log`
--

LOCK TABLES `log` WRITE;
/*!40000 ALTER TABLE `log` DISABLE KEYS */;
/*!40000 ALTER TABLE `log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `marketing_campaign`
--

DROP TABLE IF EXISTS `marketing_campaign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_campaign` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '活动描述',
  `campaign_type` int NOT NULL COMMENT '活动类型 1:满减 2:折扣 3:赠品 4:首单 5:会员专享 6:秒杀',
  `target_type` int NOT NULL DEFAULT '1' COMMENT '目标类型 1全部 2新用户 3高价值 4流失 5指定等级',
  `target_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标值',
  `rule_json` longtext COLLATE utf8mb4_unicode_ci COMMENT '规则JSON',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态 0草稿 1进行中 2已结束 3暂停',
  `priority` int NOT NULL DEFAULT '0' COMMENT '优先级',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `max_participants` int DEFAULT NULL COMMENT '最大参与人数',
  `current_participants` int NOT NULL DEFAULT '0' COMMENT '当前参与人数',
  `coupon_template_id` bigint DEFAULT NULL COMMENT '关联券模板ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `marketing_campaign`
--

LOCK TABLES `marketing_campaign` WRITE;
/*!40000 ALTER TABLE `marketing_campaign` DISABLE KEYS */;
INSERT INTO `marketing_campaign` VALUES (1,1,'周末满减酬宾','用于演示环境的常规记录，可随时调整。',1,1,NULL,NULL,0,0,'2026-07-14 00:53:36','2026-07-14 02:53:36',NULL,0,NULL,NULL,NULL,'2026-07-14 00:53:36','2026-07-17 00:53:36',0),(2,1,'新客首单立减','系统自动补齐的示例数据。',2,1,NULL,NULL,0,0,'2026-07-19 02:06:36','2026-07-19 05:06:36',NULL,0,NULL,NULL,NULL,'2026-07-19 02:06:36','2026-07-23 02:06:36',0),(3,1,'老客回馈券','按业务流程录入的一条典型记录。',3,1,NULL,NULL,0,0,'2026-07-24 03:19:36','2026-07-24 07:19:36',NULL,0,NULL,NULL,NULL,'2026-07-24 03:19:36','2026-07-29 03:19:36',0),(4,1,'周三会员日五折','运营日常维护产生的记录。',4,1,NULL,NULL,0,0,'2026-07-29 04:32:36','2026-07-29 09:32:36',NULL,0,NULL,NULL,NULL,'2026-07-29 04:32:36','2026-08-04 04:32:36',0),(5,1,'下单返券活动','供联调与走查使用的样例内容。',1,1,NULL,NULL,0,0,'2026-08-03 05:45:36','2026-08-03 11:45:36',NULL,0,NULL,NULL,NULL,'2026-08-03 05:45:36','2026-08-10 05:45:36',0),(6,1,'下午茶专享券','用于演示环境的常规记录，可随时调整。',2,1,NULL,NULL,0,0,'2026-08-08 05:58:36','2026-08-08 12:58:36',NULL,0,NULL,NULL,NULL,'2026-08-08 05:58:36','2026-08-16 05:58:36',0),(7,1,'雨天暖心补贴','系统自动补齐的示例数据。',3,1,NULL,NULL,0,0,'2026-08-13 07:11:36','2026-08-13 15:11:36',NULL,0,NULL,NULL,NULL,'2026-08-13 07:11:36','2026-08-22 07:11:36',0),(8,1,'周末满减酬宾-2','按业务流程录入的一条典型记录。',4,1,NULL,NULL,0,0,'2026-08-18 08:24:36','2026-08-18 17:24:36',NULL,0,NULL,NULL,NULL,'2026-08-18 08:24:36','2026-08-28 08:24:36',0),(9,1,'新客首单立减-2','运营日常维护产生的记录。',1,1,NULL,NULL,0,0,'2026-08-23 09:37:36','2026-08-23 19:37:36',NULL,0,NULL,NULL,NULL,'2026-08-23 09:37:36','2026-09-03 09:37:36',0),(10,1,'老客回馈券-2','供联调与走查使用的样例内容。',2,1,NULL,NULL,0,0,'2026-08-28 10:50:36','2026-08-28 21:50:36',NULL,0,NULL,NULL,NULL,'2026-08-28 10:50:36','2026-09-09 10:50:36',0);
/*!40000 ALTER TABLE `marketing_campaign` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `marketing_message`
--

DROP TABLE IF EXISTS `marketing_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `campaign_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `push_type` tinyint NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `read_time` datetime DEFAULT NULL,
  `use_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  `create_user` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `marketing_message`
--

LOCK TABLES `marketing_message` WRITE;
/*!40000 ALTER TABLE `marketing_message` DISABLE KEYS */;
INSERT INTO `marketing_message` VALUES (1,1,1,1,1,'周末特惠通知','用于演示环境的常规记录，可随时调整。',0,NULL,NULL,'2026-07-14 00:53:36',0,NULL,'2026-09-30 15:53:52',NULL),(2,1,2,2,2,'会员日活动说明','系统自动补齐的示例数据。',0,NULL,NULL,'2026-07-19 02:06:36',0,NULL,'2026-09-30 15:53:52',NULL),(3,1,3,3,3,'配送范围调整公告','按业务流程录入的一条典型记录。',0,NULL,NULL,'2026-07-24 03:19:36',0,NULL,'2026-09-30 15:53:52',NULL),(4,1,1,1,4,'菜单更新说明','运营日常维护产生的记录。',0,NULL,NULL,'2026-07-29 04:32:36',0,NULL,'2026-09-30 15:53:52',NULL),(5,1,2,2,1,'门店歇业通知','供联调与走查使用的样例内容。',0,NULL,NULL,'2026-08-03 05:45:36',0,NULL,'2026-09-30 15:53:52',NULL),(6,1,3,3,2,'新品上市介绍','用于演示环境的常规记录，可随时调整。',0,NULL,NULL,'2026-08-08 05:58:36',0,NULL,'2026-09-30 15:53:52',NULL),(7,1,1,1,3,'服务流程规范','系统自动补齐的示例数据。',0,NULL,NULL,'2026-08-13 07:11:36',0,NULL,'2026-09-30 15:53:52',NULL),(8,1,2,2,4,'月度经营小结','按业务流程录入的一条典型记录。',0,NULL,NULL,'2026-08-18 08:24:36',0,NULL,'2026-09-30 15:53:52',NULL),(9,1,3,3,1,'客户反馈处理记录','运营日常维护产生的记录。',0,NULL,NULL,'2026-08-23 09:37:36',0,NULL,'2026-09-30 15:53:52',NULL),(10,1,1,1,2,'系统升级安排','供联调与走查使用的样例内容。',0,NULL,NULL,'2026-08-28 10:50:36',0,NULL,'2026-09-30 15:53:52',NULL);
/*!40000 ALTER TABLE `marketing_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `material`
--

DROP TABLE IF EXISTS `material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `CATEGORY_ID` bigint DEFAULT NULL COMMENT '分类ID',
  `NAME` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物料名称',
  `UNIT` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位',
  `STOCK_QTY` decimal(10,2) DEFAULT NULL COMMENT '库存数量',
  `MIN_STOCK` decimal(10,2) DEFAULT NULL COMMENT '最小库存',
  `UNIT_PRICE` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `SUPPLIER_ID` bigint DEFAULT NULL COMMENT '供应商ID',
  `BARCODE` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '条形码',
  `STATUS` int DEFAULT NULL COMMENT '状态（1-正常，0-禁用）',
  `CREATED_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `material`
--

LOCK TABLES `material` WRITE;
/*!40000 ALTER TABLE `material` DISABLE KEYS */;
INSERT INTO `material` VALUES (1,1,NULL,'常规项目',NULL,20.00,20.00,18.00,NULL,'常规记录',NULL,NULL,NULL,NULL,NULL,0),(2,1,NULL,'默认项目',NULL,21.00,21.00,25.00,NULL,'默认记录',NULL,NULL,NULL,NULL,NULL,0),(3,1,NULL,'补充项目',NULL,22.00,22.00,32.00,NULL,'补充记录',NULL,NULL,NULL,NULL,NULL,0),(4,1,NULL,'备用配置',NULL,23.00,23.00,39.00,NULL,'备用记录',NULL,NULL,NULL,NULL,NULL,0),(5,1,NULL,'扩展配置',NULL,24.00,24.00,46.00,NULL,'扩展记录',NULL,NULL,NULL,NULL,NULL,0),(6,1,NULL,'标准配置',NULL,25.00,25.00,53.00,NULL,'标准记录',NULL,NULL,NULL,NULL,NULL,0),(7,1,NULL,'增值条目',NULL,26.00,26.00,60.00,NULL,'增值记录',NULL,NULL,NULL,NULL,NULL,0),(8,1,NULL,'临时条目',NULL,27.00,27.00,67.00,NULL,'临时记录',NULL,NULL,NULL,NULL,NULL,0),(9,1,NULL,'长期条目',NULL,28.00,28.00,74.00,NULL,'长期记录',NULL,NULL,NULL,NULL,NULL,0),(10,1,NULL,'专项记录',NULL,29.00,29.00,81.00,NULL,'专项记录',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `material` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `material_category`
--

DROP TABLE IF EXISTS `material_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material_category` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `NAME` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类名称',
  `SORT` int DEFAULT NULL COMMENT '排序',
  `CREATED_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `material_category`
--

LOCK TABLES `material_category` WRITE;
/*!40000 ALTER TABLE `material_category` DISABLE KEYS */;
INSERT INTO `material_category` VALUES (1,1,'热菜',NULL,NULL,NULL,NULL,NULL,0),(2,1,'凉菜',NULL,NULL,NULL,NULL,NULL,0),(3,1,'主食',NULL,NULL,NULL,NULL,NULL,0),(4,1,'汤羹',NULL,NULL,NULL,NULL,NULL,0),(5,1,'饮品',NULL,NULL,NULL,NULL,NULL,0),(6,1,'小食',NULL,NULL,NULL,NULL,NULL,0),(7,1,'甜点',NULL,NULL,NULL,NULL,NULL,0),(8,1,'招牌推荐',NULL,NULL,NULL,NULL,NULL,0),(9,1,'热菜-2',NULL,NULL,NULL,NULL,NULL,0),(10,1,'凉菜-2',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `material_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `user_id` bigint DEFAULT NULL COMMENT '关联用户ID',
  `level_id` bigint DEFAULT NULL COMMENT '会员等级ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会员姓名',
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机',
  `points` bigint DEFAULT '0' COMMENT '秈',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余',
  `total_consumption` decimal(10,2) DEFAULT '0.00' COMMENT '消费金',
  `status` int DEFAULT '1' COMMENT '状',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐锁版朏',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member`
--

LOCK TABLES `member` WRITE;
/*!40000 ALTER TABLE `member` DISABLE KEYS */;
INSERT INTO `member` VALUES (1,1,NULL,NULL,'张芳',NULL,0,18.00,18.00,1,NULL,NULL,NULL,NULL,0,0),(2,1,NULL,NULL,'王芳',NULL,0,25.00,25.00,1,NULL,NULL,NULL,NULL,0,0),(3,1,NULL,NULL,'李磊',NULL,0,32.00,32.00,1,NULL,NULL,NULL,NULL,0,0),(4,1,NULL,NULL,'赵磊',NULL,0,39.00,39.00,1,NULL,NULL,NULL,NULL,0,0),(5,1,NULL,NULL,'刘敏',NULL,0,46.00,46.00,1,NULL,NULL,NULL,NULL,0,0),(6,1,NULL,NULL,'陈敏',NULL,0,53.00,53.00,1,NULL,NULL,NULL,NULL,0,0),(7,1,NULL,NULL,'杨静',NULL,0,60.00,60.00,1,NULL,NULL,NULL,NULL,0,0),(8,1,NULL,NULL,'黄静',NULL,0,67.00,67.00,1,NULL,NULL,NULL,NULL,0,0),(9,1,NULL,NULL,'周强',NULL,0,74.00,74.00,1,NULL,NULL,NULL,NULL,0,0),(10,1,NULL,NULL,'吴强',NULL,0,81.00,81.00,1,NULL,NULL,NULL,NULL,0,0);
/*!40000 ALTER TABLE `member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_level`
--

DROP TABLE IF EXISTS `member_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_level` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '等级名称',
  `MIN_POINTS` bigint DEFAULT '0' COMMENT '最低积分要求',
  `MAX_POINTS` bigint DEFAULT NULL COMMENT '最高积分上限',
  `discount` decimal(3,2) DEFAULT '1.00' COMMENT '折扣',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '等级说明',
  `sort` int DEFAULT NULL COMMENT '排序',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_level`
--

LOCK TABLES `member_level` WRITE;
/*!40000 ALTER TABLE `member_level` DISABLE KEYS */;
INSERT INTO `member_level` VALUES (1,1,'张芳',0,NULL,1.00,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,0),(2,1,'王芳',0,NULL,1.00,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,0),(3,1,'李磊',0,NULL,2.00,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,0),(4,1,'赵磊',0,NULL,3.00,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,0),(5,1,'刘敏',0,NULL,4.00,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,0),(6,1,'陈敏',0,NULL,5.00,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,0),(7,1,'杨静',0,NULL,6.00,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,0),(8,1,'黄静',0,NULL,7.00,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,0),(9,1,'周强',0,NULL,8.00,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,0),(10,1,'吴强',0,NULL,9.00,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `member_level` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_tag`
--

DROP TABLE IF EXISTS `member_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `tag_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag_type` tinyint DEFAULT '1',
  `biz_tag` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tag_color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '#409EFF',
  `created_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  `create_user` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_tag`
--

LOCK TABLES `member_tag` WRITE;
/*!40000 ALTER TABLE `member_tag` DISABLE KEYS */;
INSERT INTO `member_tag` VALUES (1,1,4,'张芳',1,NULL,'#409EFF','2026-07-14 00:53:36',0,NULL,'2026-09-30 15:53:51',NULL),(2,1,7,'王芳',1,NULL,'#409EFF','2026-07-19 02:06:36',0,NULL,'2026-09-30 15:53:51',NULL),(3,1,10,'李磊',1,NULL,'#409EFF','2026-07-24 03:19:36',0,NULL,'2026-09-30 15:53:51',NULL),(4,1,3,'赵磊',1,NULL,'#409EFF','2026-07-29 04:32:36',0,NULL,'2026-09-30 15:53:51',NULL),(5,1,6,'刘敏',1,NULL,'#409EFF','2026-08-03 05:45:36',0,NULL,'2026-09-30 15:53:51',NULL),(6,1,9,'陈敏',1,NULL,'#409EFF','2026-08-08 05:58:36',0,NULL,'2026-09-30 15:53:51',NULL),(7,1,2,'杨静',1,NULL,'#409EFF','2026-08-13 07:11:36',0,NULL,'2026-09-30 15:53:51',NULL),(8,1,5,'黄静',1,NULL,'#409EFF','2026-08-18 08:24:36',0,NULL,'2026-09-30 15:53:51',NULL),(9,1,8,'周强',1,NULL,'#409EFF','2026-08-23 09:37:36',0,NULL,'2026-09-30 15:53:51',NULL),(10,1,1,'吴强',1,NULL,'#409EFF','2026-08-28 10:50:36',0,NULL,'2026-09-30 15:53:51',NULL);
/*!40000 ALTER TABLE `member_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `menu`
--

DROP TABLE IF EXISTS `menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由路径',
  `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组件路径',
  `perms` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜单图标',
  `type` int NOT NULL DEFAULT '1' COMMENT '类型 1:菜单 2:按钮',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menu`
--

LOCK TABLES `menu` WRITE;
/*!40000 ALTER TABLE `menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `new_customer_discount`
--

DROP TABLE IF EXISTS `new_customer_discount`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `new_customer_discount` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `discount_type` tinyint NOT NULL COMMENT '优惠类型 1固定金额 2百分比',
  `discount_value` decimal(10,2) NOT NULL COMMENT '优惠值',
  `max_discount_amount` decimal(10,2) DEFAULT NULL COMMENT '最大优惠金额',
  `min_order_amount` decimal(10,2) DEFAULT NULL COMMENT '最低订单金额',
  `valid_days` int DEFAULT NULL COMMENT '注册后有效天数',
  `status` tinyint DEFAULT '1' COMMENT '状态 0停用 1启用',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `new_customer_discount`
--

LOCK TABLES `new_customer_discount` WRITE;
/*!40000 ALTER TABLE `new_customer_discount` DISABLE KEYS */;
INSERT INTO `new_customer_discount` VALUES (1,'张芳',5,20.00,18.00,18.00,NULL,1,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL),(2,'王芳',7,21.00,25.00,25.00,NULL,1,'系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL),(3,'李磊',9,22.00,32.00,32.00,NULL,1,'按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL),(4,'赵磊',11,23.00,39.00,39.00,NULL,1,'运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL),(5,'刘敏',13,24.00,46.00,46.00,NULL,1,'供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL),(6,'陈敏',15,25.00,53.00,53.00,NULL,1,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL),(7,'杨静',17,26.00,60.00,60.00,NULL,1,'系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL),(8,'黄静',19,27.00,67.00,67.00,NULL,1,'按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL),(9,'周强',21,28.00,74.00,74.00,NULL,1,'运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL),(10,'吴强',23,29.00,81.00,81.00,NULL,1,'供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `new_customer_discount` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_record`
--

DROP TABLE IF EXISTS `notification_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL,
  `template_id` bigint DEFAULT NULL,
  `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel` tinyint NOT NULL,
  `target_type` tinyint NOT NULL DEFAULT '1',
  `target_value` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_count` int NOT NULL DEFAULT '0',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `send_time` datetime DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `success_count` int NOT NULL DEFAULT '0',
  `fail_count` int NOT NULL DEFAULT '0',
  `fail_reason` text COLLATE utf8mb4_unicode_ci,
  `ext_data` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_record`
--

LOCK TABLES `notification_record` WRITE;
/*!40000 ALTER TABLE `notification_record` DISABLE KEYS */;
INSERT INTO `notification_record` VALUES (1,1,NULL,'DEFAULT',1,1,'常规记录',0,'用于演示环境的常规记录，可随时调整。',NULL,0,0,0,NULL,NULL,'2026-07-14 00:53:36','2026-07-17 00:53:36',NULL,NULL,0),(2,1,NULL,'NORMAL',2,1,'默认记录',0,'系统自动补齐的示例数据。',NULL,0,0,0,NULL,NULL,'2026-07-19 02:06:36','2026-07-23 02:06:36',NULL,NULL,0),(3,1,NULL,'SPECIAL',3,1,'补充记录',0,'按业务流程录入的一条典型记录。',NULL,0,0,0,NULL,NULL,'2026-07-24 03:19:36','2026-07-29 03:19:36',NULL,NULL,0),(4,1,NULL,'TEMP',1,1,'备用记录',0,'运营日常维护产生的记录。',NULL,0,0,0,NULL,NULL,'2026-07-29 04:32:36','2026-08-04 04:32:36',NULL,NULL,0),(5,1,NULL,'CUSTOM',2,1,'扩展记录',0,'供联调与走查使用的样例内容。',NULL,0,0,0,NULL,NULL,'2026-08-03 05:45:36','2026-08-10 05:45:36',NULL,NULL,0),(6,1,NULL,'BASIC',3,1,'标准记录',0,'用于演示环境的常规记录，可随时调整。',NULL,0,0,0,NULL,NULL,'2026-08-08 05:58:36','2026-08-16 05:58:36',NULL,NULL,0),(7,1,NULL,'DEFAULT',1,1,'增值记录',0,'系统自动补齐的示例数据。',NULL,0,0,0,NULL,NULL,'2026-08-13 07:11:36','2026-08-22 07:11:36',NULL,NULL,0),(8,1,NULL,'NORMAL',2,1,'临时记录',0,'按业务流程录入的一条典型记录。',NULL,0,0,0,NULL,NULL,'2026-08-18 08:24:36','2026-08-28 08:24:36',NULL,NULL,0),(9,1,NULL,'SPECIAL',3,1,'长期记录',0,'运营日常维护产生的记录。',NULL,0,0,0,NULL,NULL,'2026-08-23 09:37:36','2026-09-03 09:37:36',NULL,NULL,0),(10,1,NULL,'TEMP',1,1,'专项记录',0,'供联调与走查使用的样例内容。',NULL,0,0,0,NULL,NULL,'2026-08-28 10:50:36','2026-09-09 10:50:36',NULL,NULL,0);
/*!40000 ALTER TABLE `notification_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_template`
--

DROP TABLE IF EXISTS `notification_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL,
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `channel` tinyint NOT NULL DEFAULT '1',
  `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `param_list` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sign_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_template`
--

LOCK TABLES `notification_template` WRITE;
/*!40000 ALTER TABLE `notification_template` DISABLE KEYS */;
INSERT INTO `notification_template` VALUES (1,1,'常规项目','常规方案',1,'DEFAULT','周末特惠通知',NULL,NULL,'常规项目',1,'用于演示环境的常规记录，可随时调整。','2026-07-14 00:53:36','2026-07-17 00:53:36',NULL,NULL,0),(2,1,'默认项目','默认方案',1,'NORMAL','会员日活动说明',NULL,NULL,'默认项目',1,'系统自动补齐的示例数据。','2026-07-19 02:06:36','2026-07-23 02:06:36',NULL,NULL,0),(3,1,'补充项目','补充方案',1,'SPECIAL','配送范围调整公告',NULL,NULL,'补充项目',1,'按业务流程录入的一条典型记录。','2026-07-24 03:19:36','2026-07-29 03:19:36',NULL,NULL,0),(4,1,'备用配置','备用方案',1,'TEMP','菜单更新说明',NULL,NULL,'备用配置',1,'运营日常维护产生的记录。','2026-07-29 04:32:36','2026-08-04 04:32:36',NULL,NULL,0),(5,1,'扩展配置','扩展方案',1,'CUSTOM','门店歇业通知',NULL,NULL,'扩展配置',1,'供联调与走查使用的样例内容。','2026-08-03 05:45:36','2026-08-10 05:45:36',NULL,NULL,0),(6,1,'标准配置','标准方案',1,'BASIC','新品上市介绍',NULL,NULL,'标准配置',1,'用于演示环境的常规记录，可随时调整。','2026-08-08 05:58:36','2026-08-16 05:58:36',NULL,NULL,0),(7,1,'增值条目','增值方案',1,'DEFAULT','服务流程规范',NULL,NULL,'增值条目',1,'系统自动补齐的示例数据。','2026-08-13 07:11:36','2026-08-22 07:11:36',NULL,NULL,0),(8,1,'临时条目','临时方案',1,'NORMAL','月度经营小结',NULL,NULL,'临时条目',1,'按业务流程录入的一条典型记录。','2026-08-18 08:24:36','2026-08-28 08:24:36',NULL,NULL,0),(9,1,'长期条目','长期方案',1,'SPECIAL','客户反馈处理记录',NULL,NULL,'长期条目',1,'运营日常维护产生的记录。','2026-08-23 09:37:36','2026-09-03 09:37:36',NULL,NULL,0),(10,1,'专项记录','专项方案',1,'TEMP','系统升级安排',NULL,NULL,'专项记录',1,'供联调与走查使用的样例内容。','2026-08-28 10:50:36','2026-09-09 10:50:36',NULL,NULL,0);
/*!40000 ALTER TABLE `notification_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `operation_log`
--

DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `operator_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人IP',
  `module` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作模块',
  `operation_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作类型',
  `table_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务表名',
  `biz_id` bigint DEFAULT NULL COMMENT '业务记录ID',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作描述',
  `old_value` text COLLATE utf8mb4_unicode_ci COMMENT '变更前值(JSON)',
  `new_value` text COLLATE utf8mb4_unicode_ci COMMENT '变更后值(JSON)',
  `request_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL',
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法',
  `request_params` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数(JSON)',
  `duration` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  `is_success` int NOT NULL DEFAULT '0' COMMENT '是否成功:0失败 1成功',
  `error_msg` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `operation_log`
--

LOCK TABLES `operation_log` WRITE;
/*!40000 ALTER TABLE `operation_log` DISABLE KEYS */;
INSERT INTO `operation_log` VALUES (1,NULL,'常规项目',NULL,NULL,NULL,'常规项目',NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-07-14 00:53:36',0),(2,NULL,'默认项目',NULL,NULL,NULL,'默认项目',NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-07-19 02:06:36',0),(3,NULL,'补充项目',NULL,NULL,NULL,'补充项目',NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-07-24 03:19:36',0),(4,NULL,'备用配置',NULL,NULL,NULL,'备用配置',NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-07-29 04:32:36',0),(5,NULL,'扩展配置',NULL,NULL,NULL,'扩展配置',NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-08-03 05:45:36',0),(6,NULL,'标准配置',NULL,NULL,NULL,'标准配置',NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-08-08 05:58:36',0),(7,NULL,'增值条目',NULL,NULL,NULL,'增值条目',NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-08-13 07:11:36',0),(8,NULL,'临时条目',NULL,NULL,NULL,'临时条目',NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-08-18 08:24:36',0),(9,NULL,'长期条目',NULL,NULL,NULL,'长期条目',NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-08-23 09:37:36',0),(10,NULL,'专项记录',NULL,NULL,NULL,'专项记录',NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,1,'2026-08-28 10:50:36',0);
/*!40000 ALTER TABLE `operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_detail`
--

DROP TABLE IF EXISTS `order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名称',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套id',
  `dish_flavor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '单价',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单明细备注',
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_detail`
--

LOCK TABLES `order_detail` WRITE;
/*!40000 ALTER TABLE `order_detail` DISABLE KEYS */;
INSERT INTO `order_detail` VALUES (1,'常规项目',102,NULL,NULL,NULL,1,18.00,'用于演示环境的常规记录，可随时调整。',NULL,1,'2026-07-14 00:53:36','2026-07-17 00:53:36',NULL,NULL,0),(2,'默认项目',101,NULL,NULL,NULL,1,25.00,'系统自动补齐的示例数据。',NULL,1,'2026-07-19 02:06:36','2026-07-23 02:06:36',NULL,NULL,0),(3,'补充项目',100,NULL,NULL,NULL,1,32.00,'按业务流程录入的一条典型记录。',NULL,1,'2026-07-24 03:19:36','2026-07-29 03:19:36',NULL,NULL,0),(4,'备用配置',102,NULL,NULL,NULL,1,39.00,'运营日常维护产生的记录。',NULL,1,'2026-07-29 04:32:36','2026-08-04 04:32:36',NULL,NULL,0),(5,'扩展配置',101,NULL,NULL,NULL,1,46.00,'供联调与走查使用的样例内容。',NULL,1,'2026-08-03 05:45:36','2026-08-10 05:45:36',NULL,NULL,0),(6,'标准配置',100,NULL,NULL,NULL,1,53.00,'用于演示环境的常规记录，可随时调整。',NULL,1,'2026-08-08 05:58:36','2026-08-16 05:58:36',NULL,NULL,0),(7,'增值条目',102,NULL,NULL,NULL,1,60.00,'系统自动补齐的示例数据。',NULL,1,'2026-08-13 07:11:36','2026-08-22 07:11:36',NULL,NULL,0),(8,'临时条目',101,NULL,NULL,NULL,1,67.00,'按业务流程录入的一条典型记录。',NULL,1,'2026-08-18 08:24:36','2026-08-28 08:24:36',NULL,NULL,0),(9,'长期条目',100,NULL,NULL,NULL,1,74.00,'运营日常维护产生的记录。',NULL,1,'2026-08-23 09:37:36','2026-09-03 09:37:36',NULL,NULL,0),(10,'专项记录',102,NULL,NULL,NULL,1,81.00,'供联调与走查使用的样例内容。',NULL,1,'2026-08-28 10:50:36','2026-09-09 10:50:36',NULL,NULL,0);
/*!40000 ALTER TABLE `order_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL COMMENT '主键',
  `number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单',
  `status` int NOT NULL DEFAULT '1' COMMENT '订单状',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `address_book_id` bigint DEFAULT NULL COMMENT '地址id',
  `order_time` datetime DEFAULT NULL COMMENT '下单时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '结账时间',
  `pay_method` int DEFAULT NULL COMMENT '攻方式',
  `amount` decimal(10,2) NOT NULL COMMENT '实收金',
  `delivery_fee` decimal(10,2) DEFAULT NULL COMMENT '配送费（外卖单独立存储，堂食为0）',
  `full_reduction_amount` decimal(10,2) DEFAULT '0.00' COMMENT '满减优惠金额（满减活动扣减，未享受为0）',
  `new_customer_discount_amount` decimal(10,2) DEFAULT '0.00' COMMENT '新客立减金额（新客活动扣减，未享受为0）',
  `remark` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `internal_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内部备注（仅后台可见）',
  `cancel_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '取消/拒单原因（P0-5 回执，顾客端可见；不再覆盖 remark）',
  `pickup_code` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '取餐码（P0-6 核销：派单/抢单时生成，骑手取餐须校验）',
  `expect_delivery_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预送达时间',
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户',
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机',
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `consignee` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货',
  `dining_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'OUTSIDE' COMMENT '用类型',
  `table_id` bigint DEFAULT NULL COMMENT '堂桌台ID',
  `table_name` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '堂桌台名称',
  `idempotency_key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '幂等',
  `stock_refunded` int DEFAULT '0' COMMENT '已库存数量',
  `used_coupon_id` bigint DEFAULT NULL COMMENT '优惠券ID',
  `rider_id` bigint DEFAULT NULL COMMENT '配送骑手ID（店长派单/骑手抢单后写入）',
  `dispatch_time` datetime DEFAULT NULL COMMENT '派单/抢单时间（超时回流判断）',
  `platform_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台来源',
  `platform_order_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台订单',
  `platform_shop_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台门店ID',
  `platform_raw` longtext COLLATE utf8mb4_unicode_ci COMMENT '平台原订单JSON',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建',
  `update_user` bigint DEFAULT NULL COMMENT '俔',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '昐删除',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐锁版朏',
  `master_order_id` bigint DEFAULT NULL COMMENT '父订单ID（AA分账时指向主订单）',
  `split_count` int DEFAULT NULL COMMENT '分账份数（AA分账记录拆分数量）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (100,'PAY001',1,1,1,'2026-09-30 15:53:25',NULL,NULL,99.99,NULL,0.00,0.00,NULL,NULL,NULL,NULL,NULL,'测试用户','13800000001','测试地址','张三','OUTSIDE',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:25','2026-09-30 15:53:25',NULL,NULL,0,999,0,NULL,NULL),(101,'PAY002',1,1,1,'2026-09-30 15:53:25',NULL,NULL,50.00,NULL,0.00,0.00,NULL,NULL,NULL,NULL,NULL,'测试用户','13800000002','测试地址','李四','OUTSIDE',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:25','2026-09-30 15:53:25',NULL,NULL,0,999,0,NULL,NULL),(102,'PAY003',1,1,1,'2026-09-30 15:53:25',NULL,NULL,200.00,NULL,0.00,0.00,NULL,NULL,NULL,NULL,NULL,'测试用户','13800000003','测试地址','王五','OUTSIDE',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:25','2026-09-30 15:53:25',NULL,NULL,0,999,0,NULL,NULL),(900000121,NULL,1,NULL,NULL,NULL,NULL,NULL,99.99,2.00,0.00,0.00,'用于演示环境的常规记录，可随时调整。','用于演示环境的常规记录，可随时调整。',NULL,'常规通道',NULL,'测试用户',NULL,'测试地址',NULL,'OUTSIDE',NULL,'常规项目',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-14 00:53:36','2026-07-17 00:53:36',NULL,NULL,0,1,0,NULL,NULL),(900000122,NULL,1,NULL,NULL,NULL,NULL,NULL,200.00,2.50,0.00,0.00,'系统自动补齐的示例数据。','系统自动补齐的示例数据。',NULL,'默认通道',NULL,'默认项目',NULL,'测试地址',NULL,'OUTSIDE',NULL,'默认项目',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-19 02:06:36','2026-07-23 02:06:36',NULL,NULL,0,1,0,NULL,NULL),(900000123,NULL,1,NULL,NULL,NULL,NULL,NULL,50.00,3.00,0.00,0.00,'按业务流程录入的一条典型记录。','按业务流程录入的一条典型记录。',NULL,'补充通道',NULL,'补充项目',NULL,'测试地址',NULL,'OUTSIDE',NULL,'补充项目',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-24 03:19:36','2026-07-29 03:19:36',NULL,NULL,0,1,0,NULL,NULL),(900000124,NULL,1,NULL,NULL,NULL,NULL,NULL,99.99,3.50,0.00,0.00,'运营日常维护产生的记录。','运营日常维护产生的记录。',NULL,'备用通道',NULL,'备用配置',NULL,'测试地址',NULL,'OUTSIDE',NULL,'备用配置',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-29 04:32:36','2026-08-04 04:32:36',NULL,NULL,0,1,0,NULL,NULL),(900000125,NULL,1,NULL,NULL,NULL,NULL,NULL,200.00,4.00,0.00,0.00,'供联调与走查使用的样例内容。','供联调与走查使用的样例内容。',NULL,'扩展通道',NULL,'扩展配置',NULL,'测试地址',NULL,'OUTSIDE',NULL,'扩展配置',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-03 05:45:36','2026-08-10 05:45:36',NULL,NULL,0,1,0,NULL,NULL),(900000126,NULL,1,NULL,NULL,NULL,NULL,NULL,50.00,4.50,0.00,0.00,'用于演示环境的常规记录，可随时调整。','用于演示环境的常规记录，可随时调整。',NULL,'标准通道',NULL,'标准配置',NULL,'测试地址',NULL,'OUTSIDE',NULL,'标准配置',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-08 05:58:36','2026-08-16 05:58:36',NULL,NULL,0,1,0,NULL,NULL),(900000127,NULL,1,NULL,NULL,NULL,NULL,NULL,99.99,5.00,0.00,0.00,'系统自动补齐的示例数据。','系统自动补齐的示例数据。',NULL,'增值通道',NULL,'增值条目',NULL,'测试地址',NULL,'OUTSIDE',NULL,'增值条目',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-13 07:11:36','2026-08-22 07:11:36',NULL,NULL,0,1,0,NULL,NULL),(900000128,NULL,1,NULL,NULL,NULL,NULL,NULL,200.00,5.50,0.00,0.00,'按业务流程录入的一条典型记录。','按业务流程录入的一条典型记录。',NULL,'临时通道',NULL,'临时条目',NULL,'测试地址',NULL,'OUTSIDE',NULL,'临时条目',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-18 08:24:36','2026-08-28 08:24:36',NULL,NULL,0,1,0,NULL,NULL),(900000129,NULL,1,NULL,NULL,NULL,NULL,NULL,50.00,6.00,0.00,0.00,'运营日常维护产生的记录。','运营日常维护产生的记录。',NULL,'长期通道',NULL,'长期条目',NULL,'测试地址',NULL,'OUTSIDE',NULL,'长期条目',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-23 09:37:36','2026-09-03 09:37:36',NULL,NULL,0,1,0,NULL,NULL),(900000130,NULL,1,NULL,NULL,NULL,NULL,NULL,99.99,6.50,0.00,0.00,'供联调与走查使用的样例内容。','供联调与走查使用的样例内容。',NULL,'专项通道',NULL,'专项记录',NULL,'测试地址',NULL,'OUTSIDE',NULL,'专项记录',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 10:50:36','2026-09-09 10:50:36',NULL,NULL,0,1,0,NULL,NULL);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `other_cost`
--

DROP TABLE IF EXISTS `other_cost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `other_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '成本名称',
  `cost_type` int DEFAULT NULL COMMENT '成本类型 1-租金/2-水电/3-设/4-耗材/5-营销/6-其他',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '成本金',
  `cost_date` datetime DEFAULT NULL COMMENT '成本日期',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `other_cost`
--

LOCK TABLES `other_cost` WRITE;
/*!40000 ALTER TABLE `other_cost` DISABLE KEYS */;
INSERT INTO `other_cost` VALUES (1,'常规项目',NULL,18.00,NULL,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL),(2,'默认项目',NULL,25.00,NULL,'系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL),(3,'补充项目',NULL,32.00,NULL,'按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL),(4,'备用配置',NULL,39.00,NULL,'运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL),(5,'扩展配置',NULL,46.00,NULL,'供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL),(6,'标准配置',NULL,53.00,NULL,'用于演示环境的常规记录，可随时调整。',1,NULL,NULL,NULL,NULL),(7,'增值条目',NULL,60.00,NULL,'系统自动补齐的示例数据。',1,NULL,NULL,NULL,NULL),(8,'临时条目',NULL,67.00,NULL,'按业务流程录入的一条典型记录。',1,NULL,NULL,NULL,NULL),(9,'长期条目',NULL,74.00,NULL,'运营日常维护产生的记录。',1,NULL,NULL,NULL,NULL),(10,'专项记录',NULL,81.00,NULL,'供联调与走查使用的样例内容。',1,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `other_cost` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_channel_config`
--

DROP TABLE IF EXISTS `payment_channel_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_channel_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '渠道 WECHAT/ALIPAY',
  `wx_app_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信appId',
  `wx_mch_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信商户号',
  `wx_api_v3_key` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信APIv3密钥(加密)',
  `wx_mch_cert_serial_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商户证书序列号',
  `wx_mch_private_key` varchar(4096) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商户私钥(加密)',
  `wx_public_key_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信支付公钥ID',
  `wx_public_key` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信支付公钥',
  `ali_app_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付宝APPID',
  `ali_private_key` varchar(4096) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付宝应用私钥(加密)',
  `ali_public_key` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付宝公钥',
  `pay_notify_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付回调地址',
  `refund_notify_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退款回调地址',
  `enabled` int NOT NULL DEFAULT '1' COMMENT '启用 0停 1启',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_channel_config`
--

LOCK TABLES `payment_channel_config` WRITE;
/*!40000 ALTER TABLE `payment_channel_config` DISABLE KEYS */;
INSERT INTO `payment_channel_config` VALUES (1,'常规项目','WECHAT',NULL,NULL,NULL,'PCC2607140041',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/dish-01.jpg','images/demo/dish-01.jpg',1,'用于演示环境的常规记录，可随时调整。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(2,'默认项目','ALIPAY',NULL,NULL,NULL,'PCC2607190042',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/store-02.jpg','images/demo/store-02.jpg',1,'系统自动补齐的示例数据。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(3,'补充项目','UNIONPAY',NULL,NULL,NULL,'PCC2607240043',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/banner-03.jpg','images/demo/banner-03.jpg',1,'按业务流程录入的一条典型记录。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(4,'备用配置','CASH',NULL,NULL,NULL,'PCC2607290044',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/avatar-04.png','images/demo/avatar-04.png',1,'运营日常维护产生的记录。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(5,'扩展配置','BANKCARD',NULL,NULL,NULL,'PCC2608030045',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/logo-05.png','images/demo/logo-05.png',1,'供联调与走查使用的样例内容。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(6,'标准配置','OTHER',NULL,NULL,NULL,'PCC2608080046',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/dish-01.jpg','images/demo/dish-01.jpg',1,'用于演示环境的常规记录，可随时调整。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(7,'增值条目','WECHAT',NULL,NULL,NULL,'PCC2608130047',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/store-02.jpg','images/demo/store-02.jpg',1,'系统自动补齐的示例数据。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(8,'临时条目','ALIPAY',NULL,NULL,NULL,'PCC2608180048',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/banner-03.jpg','images/demo/banner-03.jpg',1,'按业务流程录入的一条典型记录。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(9,'长期条目','UNIONPAY',NULL,NULL,NULL,'PCC2608230049',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/avatar-04.png','images/demo/avatar-04.png',1,'运营日常维护产生的记录。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL),(10,'专项记录','CASH',NULL,NULL,NULL,'PCC2608280050',NULL,NULL,NULL,NULL,NULL,NULL,'images/demo/logo-05.png','images/demo/logo-05.png',1,'供联调与走查使用的样例内容。',1,0,0,'2026-09-30 15:53:49','2026-09-30 15:53:49',NULL,NULL);
/*!40000 ALTER TABLE `payment_channel_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_order`
--

DROP TABLE IF EXISTS `payment_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '业务订单id',
  `biz_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ORDER' COMMENT '业务类型 ORDER/RECHARGE',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `trade_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '系统交易号',
  `channel_trade_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通道交易号',
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付通道 ALIPAY/WECHAT/UNIONPAY',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL/REFUND',
  `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
  `notify_time` datetime DEFAULT NULL COMMENT '回调时间',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_order`
--

LOCK TABLES `payment_order` WRITE;
/*!40000 ALTER TABLE `payment_order` DISABLE KEYS */;
INSERT INTO `payment_order` VALUES (1,102,'ORDER',1,'PO2607140331','WECHAT','WECHAT',18.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL),(2,101,'ORDER',1,'PO2607190332','ALIPAY','ALIPAY',25.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL),(3,100,'ORDER',1,'PO2607240333','UNIONPAY','UNIONPAY',32.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL),(4,102,'ORDER',1,'PO2607290334','CASH','CASH',39.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL),(5,101,'ORDER',1,'PO2608030335','BANKCARD','BANKCARD',46.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL),(6,100,'ORDER',1,'PO2608080336','OTHER','OTHER',53.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL),(7,102,'ORDER',1,'PO2608130337','WECHAT','WECHAT',60.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL),(8,101,'ORDER',1,'PO2608180338','ALIPAY','ALIPAY',67.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL),(9,100,'ORDER',1,'PO2608230339','UNIONPAY','UNIONPAY',74.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL),(10,102,'ORDER',1,'PO2608280340','CASH','CASH',81.00,'PENDING',NULL,NULL,'2026-09-30 15:53:52',NULL,0,1,NULL,NULL);
/*!40000 ALTER TABLE `payment_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `permission_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `permission_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限标识',
  `permission_type` int NOT NULL DEFAULT '1' COMMENT '权限类型 1:菜单 2:按钮 3:数据',
  `parent_id` bigint DEFAULT '0' COMMENT '父权限ID（0=顶级）',
  `route_path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由路径',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permission`
--

LOCK TABLES `permission` WRITE;
/*!40000 ALTER TABLE `permission` DISABLE KEYS */;
INSERT INTO `permission` VALUES (1,'常规项目','P2607140011',1,0,NULL,NULL,0,1,'2026-09-30 15:53:48',NULL),(2,'默认项目','P2607190012',1,0,NULL,NULL,0,1,'2026-09-30 15:53:48',NULL),(3,'补充项目','P2607240013',1,0,NULL,NULL,0,1,'2026-09-30 15:53:48',NULL),(4,'备用配置','P2607290014',1,0,NULL,NULL,0,1,'2026-09-30 15:53:48',NULL),(5,'扩展配置','P2608030015',1,0,NULL,NULL,0,1,'2026-09-30 15:53:48',NULL),(6,'标准配置','P2608080016',1,0,NULL,NULL,0,1,'2026-09-30 15:53:48',NULL),(7,'增值条目','P2608130017',1,0,NULL,NULL,0,1,'2026-09-30 15:53:48',NULL),(8,'临时条目','P2608180018',1,0,NULL,NULL,0,1,'2026-09-30 15:53:48',NULL),(9,'长期条目','P2608230019',1,0,NULL,NULL,0,1,'2026-09-30 15:53:49',NULL),(10,'专项记录','P2608280020',1,0,NULL,NULL,0,1,'2026-09-30 15:53:49',NULL);
/*!40000 ALTER TABLE `permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_config`
--

DROP TABLE IF EXISTS `platform_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  `platform_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台展示名称',
  `shop_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台侧门店ID',
  `app_key` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用标识(加密)',
  `app_secret` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用密钥(加密)',
  `access_token` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '访问令牌(加密)',
  `enabled` int NOT NULL DEFAULT '1' COMMENT '昐吔 0停用 1吔',
  `sync_scope` int NOT NULL DEFAULT '1' COMMENT '同范围位标 1订单2商品4库存8营业状',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_config`
--

LOCK TABLES `platform_config` WRITE;
/*!40000 ALTER TABLE `platform_config` DISABLE KEYS */;
INSERT INTO `platform_config` VALUES (1,'DEFAULT','常规项目',NULL,NULL,NULL,NULL,1,1,'用于演示环境的常规记录，可随时调整。',1,0,NULL,NULL),(2,'NORMAL','默认项目',NULL,NULL,NULL,NULL,1,1,'系统自动补齐的示例数据。',1,0,NULL,NULL),(3,'SPECIAL','补充项目',NULL,NULL,NULL,NULL,1,1,'按业务流程录入的一条典型记录。',1,0,NULL,NULL),(4,'TEMP','备用配置',NULL,NULL,NULL,NULL,1,1,'运营日常维护产生的记录。',1,0,NULL,NULL),(5,'CUSTOM','扩展配置',NULL,NULL,NULL,NULL,1,1,'供联调与走查使用的样例内容。',1,0,NULL,NULL),(6,'BASIC','标准配置',NULL,NULL,NULL,NULL,1,1,'用于演示环境的常规记录，可随时调整。',1,0,NULL,NULL),(7,'DEFAULT','增值条目',NULL,NULL,NULL,NULL,1,1,'系统自动补齐的示例数据。',1,0,NULL,NULL),(8,'NORMAL','临时条目',NULL,NULL,NULL,NULL,1,1,'按业务流程录入的一条典型记录。',1,0,NULL,NULL),(9,'SPECIAL','长期条目',NULL,NULL,NULL,NULL,1,1,'运营日常维护产生的记录。',1,0,NULL,NULL),(10,'TEMP','专项记录',NULL,NULL,NULL,NULL,1,1,'供联调与走查使用的样例内容。',1,0,NULL,NULL);
/*!40000 ALTER TABLE `platform_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_reconcile_task`
--

DROP TABLE IF EXISTS `platform_reconcile_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_reconcile_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `platform_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台类型',
  `reconcile_date` date NOT NULL COMMENT '对账日期',
  `begin_time` datetime NOT NULL COMMENT '对账始时',
  `end_time` datetime NOT NULL COMMENT '对账结束时间',
  `total_platform_count` int NOT NULL DEFAULT '0' COMMENT '平台侧订单数',
  `total_local_count` int NOT NULL DEFAULT '0' COMMENT '本地订单数',
  `match_count` int NOT NULL DEFAULT '0' COMMENT '匹配成功',
  `missing_local_count` int NOT NULL DEFAULT '0' COMMENT '平台有本地无',
  `missing_platform_count` int NOT NULL DEFAULT '0' COMMENT '朜有平台无',
  `status` int NOT NULL DEFAULT '0' COMMENT '状 0=进 1=完成 2=失败',
  `error_message` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错信息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_reconcile_task`
--

LOCK TABLES `platform_reconcile_task` WRITE;
/*!40000 ALTER TABLE `platform_reconcile_task` DISABLE KEYS */;
INSERT INTO `platform_reconcile_task` VALUES (1,1,'DEFAULT','2026-07-14','2026-07-14 00:53:36','2026-07-14 02:53:36',0,0,0,0,0,0,NULL,NULL,NULL),(2,1,'NORMAL','2026-07-19','2026-07-19 02:06:36','2026-07-19 05:06:36',0,0,0,0,0,0,NULL,NULL,NULL),(3,1,'SPECIAL','2026-07-24','2026-07-24 03:19:36','2026-07-24 07:19:36',0,0,0,0,0,0,NULL,NULL,NULL),(4,1,'TEMP','2026-07-29','2026-07-29 04:32:36','2026-07-29 09:32:36',0,0,0,0,0,0,NULL,NULL,NULL),(5,1,'CUSTOM','2026-08-03','2026-08-03 05:45:36','2026-08-03 11:45:36',0,0,0,0,0,0,NULL,NULL,NULL),(6,1,'BASIC','2026-08-08','2026-08-08 05:58:36','2026-08-08 12:58:36',0,0,0,0,0,0,NULL,NULL,NULL),(7,1,'DEFAULT','2026-08-13','2026-08-13 07:11:36','2026-08-13 15:11:36',0,0,0,0,0,0,NULL,NULL,NULL),(8,1,'NORMAL','2026-08-18','2026-08-18 08:24:36','2026-08-18 17:24:36',0,0,0,0,0,0,NULL,NULL,NULL),(9,1,'SPECIAL','2026-08-23','2026-08-23 09:37:36','2026-08-23 19:37:36',0,0,0,0,0,0,NULL,NULL,NULL),(10,1,'TEMP','2026-08-28','2026-08-28 10:50:36','2026-08-28 21:50:36',0,0,0,0,0,0,NULL,NULL,NULL);
/*!40000 ALTER TABLE `platform_reconcile_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_sync_log`
--

DROP TABLE IF EXISTS `platform_sync_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `platform_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台类型',
  `platform_order_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台订单ID',
  `local_order_id` bigint DEFAULT NULL COMMENT '朜订单ID',
  `action` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作 PULL/ACCEPT/REJECT',
  `direction` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'IN' COMMENT '方向 IN=拉单 OUT=回传',
  `request_body` text COLLATE utf8mb4_unicode_ci COMMENT '请求内',
  `response_body` text COLLATE utf8mb4_unicode_ci COMMENT '响应内',
  `status` int NOT NULL DEFAULT '0' COMMENT '结果 0=成功 1=失败',
  `error_message` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错信息',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_sync_log`
--

LOCK TABLES `platform_sync_log` WRITE;
/*!40000 ALTER TABLE `platform_sync_log` DISABLE KEYS */;
INSERT INTO `platform_sync_log` VALUES (1,1,'DEFAULT',NULL,NULL,'常规选项','IN',NULL,NULL,0,NULL,0,NULL),(2,1,'NORMAL',NULL,NULL,'默认选项','IN',NULL,NULL,0,NULL,0,NULL),(3,1,'SPECIAL',NULL,NULL,'补充选项','IN',NULL,NULL,0,NULL,0,NULL),(4,1,'TEMP',NULL,NULL,'备用选项','IN',NULL,NULL,0,NULL,0,NULL),(5,1,'CUSTOM',NULL,NULL,'扩展选项','IN',NULL,NULL,0,NULL,0,NULL),(6,1,'BASIC',NULL,NULL,'标准选项','IN',NULL,NULL,0,NULL,0,NULL),(7,1,'DEFAULT',NULL,NULL,'增值选项','IN',NULL,NULL,0,NULL,0,NULL),(8,1,'NORMAL',NULL,NULL,'临时选项','IN',NULL,NULL,0,NULL,0,NULL),(9,1,'SPECIAL',NULL,NULL,'长期选项','IN',NULL,NULL,0,NULL,0,NULL),(10,1,'TEMP',NULL,NULL,'专项选项','IN',NULL,NULL,0,NULL,0,NULL);
/*!40000 ALTER TABLE `platform_sync_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `points_record`
--

DROP TABLE IF EXISTS `points_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `points_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `member_id` bigint DEFAULT NULL COMMENT '会员ID',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型',
  `points` int DEFAULT NULL COMMENT '秈数量',
  `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联业务类型',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `expire_time` datetime DEFAULT NULL COMMENT '积分过期时间',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_points_biz` (`tenant_id`,`biz_type`,`biz_id`,`type`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `points_record`
--

LOCK TABLES `points_record` WRITE;
/*!40000 ALTER TABLE `points_record` DISABLE KEYS */;
INSERT INTO `points_record` VALUES (1,1,NULL,NULL,NULL,NULL,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,0),(2,1,NULL,NULL,NULL,NULL,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,0),(3,1,NULL,NULL,NULL,NULL,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,0),(4,1,NULL,NULL,NULL,NULL,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,NULL,NULL,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,0),(6,1,NULL,NULL,NULL,NULL,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,0),(7,1,NULL,NULL,NULL,NULL,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,0),(8,1,NULL,NULL,NULL,NULL,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,0),(9,1,NULL,NULL,NULL,NULL,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,0),(10,1,NULL,NULL,NULL,NULL,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `points_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `price_history`
--

DROP TABLE IF EXISTS `price_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `price_history` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `MATERIAL_ID` bigint NOT NULL COMMENT '物料ID',
  `OLD_PRICE` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '旧价格',
  `NEW_PRICE` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '新价格',
  `CHANGE_REASON` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '变动原因',
  `OPERATOR_ID` bigint NOT NULL DEFAULT '0' COMMENT '操作人ID',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `price_history`
--

LOCK TABLES `price_history` WRITE;
/*!40000 ALTER TABLE `price_history` DISABLE KEYS */;
INSERT INTO `price_history` VALUES (1,1,7,18.00,18.00,'',0,NULL),(2,1,10,25.00,25.00,'',0,NULL),(3,1,3,32.00,32.00,'',0,NULL),(4,1,6,39.00,39.00,'',0,NULL),(5,1,9,46.00,46.00,'',0,NULL),(6,1,2,53.00,53.00,'',0,NULL),(7,1,5,60.00,60.00,'',0,NULL),(8,1,8,67.00,67.00,'',0,NULL),(9,1,1,74.00,74.00,'',0,NULL),(10,1,4,81.00,81.00,'',0,NULL);
/*!40000 ALTER TABLE `price_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `print_task`
--

DROP TABLE IF EXISTS `print_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `print_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `store_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `order_id` bigint DEFAULT NULL,
  `task_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BILL',
  `content` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `terminal_id` bigint DEFAULT NULL,
  `terminal_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `error_msg` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `retry_count` int NOT NULL DEFAULT '0',
  `created_time` datetime DEFAULT NULL,
  `pulled_time` datetime DEFAULT NULL,
  `done_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_print_task_status` (`status`),
  KEY `idx_print_task_order` (`order_id`),
  KEY `idx_print_task_terminal` (`terminal_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `print_task`
--

LOCK TABLES `print_task` WRITE;
/*!40000 ALTER TABLE `print_task` DISABLE KEYS */;
INSERT INTO `print_task` VALUES (1,1,'常规配置',NULL,'BILL',NULL,'PENDING',NULL,'常规配置','',0,NULL,NULL,NULL),(2,1,'默认配置',NULL,'BILL',NULL,'PENDING',NULL,'默认配置','',0,NULL,NULL,NULL),(3,1,'补充配置',NULL,'BILL',NULL,'PENDING',NULL,'补充配置','',0,NULL,NULL,NULL),(4,1,'备用配置',NULL,'BILL',NULL,'PENDING',NULL,'备用配置','',0,NULL,NULL,NULL),(5,1,'扩展配置',NULL,'BILL',NULL,'PENDING',NULL,'扩展配置','',0,NULL,NULL,NULL),(6,1,'标准配置',NULL,'BILL',NULL,'PENDING',NULL,'标准配置','',0,NULL,NULL,NULL),(7,1,'增值配置',NULL,'BILL',NULL,'PENDING',NULL,'增值配置','',0,NULL,NULL,NULL),(8,1,'临时配置',NULL,'BILL',NULL,'PENDING',NULL,'临时配置','',0,NULL,NULL,NULL),(9,1,'长期配置',NULL,'BILL',NULL,'PENDING',NULL,'长期配置','',0,NULL,NULL,NULL),(10,1,'专项配置',NULL,'BILL',NULL,'PENDING',NULL,'专项配置','',0,NULL,NULL,NULL);
/*!40000 ALTER TABLE `print_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `print_terminal`
--

DROP TABLE IF EXISTS `print_terminal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `print_terminal` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `store_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `terminal_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `printer_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `paper_size` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '80mm',
  `print_types` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BILL',
  `status` tinyint NOT NULL DEFAULT '0',
  `last_heartbeat` datetime DEFAULT NULL,
  `client_version` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `created_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_print_terminal_code` (`terminal_code`),
  KEY `idx_print_terminal_tenant` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `print_terminal`
--

LOCK TABLES `print_terminal` WRITE;
/*!40000 ALTER TABLE `print_terminal` DISABLE KEYS */;
/*!40000 ALTER TABLE `print_terminal` ENABLE KEYS */;
UNLOCK TABLES;

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
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '打印机名称',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '连接类型 USB/TCP/CLOUD/BLUETOOTH',
  `brand` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌 佳博/芯烨/商米',
  `device_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备标识 MAC/SN',
  `system_printer_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '系统打印机名称（Windows下为驱动名称）',
  `ip_address` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `port` int DEFAULT NULL COMMENT '端口',
  `paper_size` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT '58mm' COMMENT '纸张规格 58mm/80mm',
  `print_types` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打印类型',
  `status` int DEFAULT '1' COMMENT '状态 0禁用 1启用',
  `sort` int DEFAULT '0' COMMENT '排序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `printer_config`
--

LOCK TABLES `printer_config` WRITE;
/*!40000 ALTER TABLE `printer_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `printer_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `printer_log`
--

DROP TABLE IF EXISTS `printer_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `printer_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint DEFAULT NULL COMMENT '订单id',
  `print_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '打印类型',
  `printer_id` bigint DEFAULT NULL COMMENT '打印机id',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '打印内容',
  `status` int DEFAULT '0' COMMENT '状态 0失败 1成功',
  `error_msg` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `printer_log`
--

LOCK TABLES `printer_log` WRITE;
/*!40000 ALTER TABLE `printer_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `printer_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `profit_analysis`
--

DROP TABLE IF EXISTS `profit_analysis`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `profit_analysis` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `analysis_date` date DEFAULT NULL COMMENT '分析日期',
  `total_revenue` decimal(12,2) DEFAULT NULL COMMENT '总营',
  `food_cost` decimal(12,2) DEFAULT NULL COMMENT '食材成本',
  `labor_cost` decimal(12,2) DEFAULT NULL COMMENT '人工成本',
  `other_cost` decimal(12,2) DEFAULT NULL COMMENT '其他成本',
  `total_cost` decimal(12,2) DEFAULT NULL COMMENT '总成',
  `gross_profit` decimal(12,2) DEFAULT NULL COMMENT '毛利',
  `gross_profit_rate` decimal(10,4) DEFAULT NULL COMMENT '毛利(%)',
  `operating_expense` decimal(12,2) DEFAULT NULL COMMENT '运营费用',
  `net_profit` decimal(12,2) DEFAULT NULL COMMENT '利润',
  `net_profit_rate` decimal(10,4) DEFAULT NULL COMMENT '利率(%)',
  `order_count` int DEFAULT NULL COMMENT '订单',
  `customer_count` int DEFAULT NULL COMMENT '客户',
  `average_order_value` decimal(10,2) DEFAULT NULL COMMENT '客单',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `profit_analysis`
--

LOCK TABLES `profit_analysis` WRITE;
/*!40000 ALTER TABLE `profit_analysis` DISABLE KEYS */;
INSERT INTO `profit_analysis` VALUES (1,NULL,18.00,18.00,18.00,18.00,18.00,10.00,0.5500,10.00,10.00,0.5500,NULL,NULL,10.00,1,NULL,NULL),(2,NULL,25.00,25.00,25.00,25.00,25.00,11.00,0.5900,11.00,11.00,0.5900,NULL,NULL,11.00,1,NULL,NULL),(3,NULL,32.00,32.00,32.00,32.00,32.00,12.00,0.6300,12.00,12.00,0.6300,NULL,NULL,12.00,1,NULL,NULL),(4,NULL,39.00,39.00,39.00,39.00,39.00,13.00,0.6700,13.00,13.00,0.6700,NULL,NULL,13.00,1,NULL,NULL),(5,NULL,46.00,46.00,46.00,46.00,46.00,14.00,0.7100,14.00,14.00,0.7100,NULL,NULL,14.00,1,NULL,NULL),(6,NULL,53.00,53.00,53.00,53.00,53.00,15.00,0.7500,15.00,15.00,0.7500,NULL,NULL,15.00,1,NULL,NULL),(7,NULL,60.00,60.00,60.00,60.00,60.00,16.00,0.7900,16.00,16.00,0.7900,NULL,NULL,16.00,1,NULL,NULL),(8,NULL,67.00,67.00,67.00,67.00,67.00,17.00,0.8300,17.00,17.00,0.8300,NULL,NULL,17.00,1,NULL,NULL),(9,NULL,74.00,74.00,74.00,74.00,74.00,18.00,0.8700,18.00,18.00,0.8700,NULL,NULL,18.00,1,NULL,NULL),(10,NULL,81.00,81.00,81.00,81.00,81.00,19.00,0.9100,19.00,19.00,0.9100,NULL,NULL,19.00,1,NULL,NULL);
/*!40000 ALTER TABLE `profit_analysis` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_order`
--

DROP TABLE IF EXISTS `purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `ORDER_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单编号',
  `SUPPLIER_ID` bigint DEFAULT NULL COMMENT '供应商ID',
  `TOTAL_AMOUNT` decimal(10,2) DEFAULT NULL COMMENT '总金额',
  `STATUS` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态',
  `OPERATOR` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作员',
  `REMARK` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `VOUCHER_IMAGES` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '凭证图片（逗号分隔，最多5张）',
  `CREATED_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `VERSION` int NOT NULL DEFAULT '0' COMMENT 'ֹ汾',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_order`
--

LOCK TABLES `purchase_order` WRITE;
/*!40000 ALTER TABLE `purchase_order` DISABLE KEYS */;
INSERT INTO `purchase_order` VALUES (1,1,'PO2607140141',NULL,18.00,NULL,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,0,0),(2,1,'PO2607190142',NULL,25.00,NULL,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,0,0),(3,1,'PO2607240143',NULL,32.00,NULL,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,0,0),(4,1,'PO2607290144',NULL,39.00,NULL,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,0,0),(5,1,'PO2608030145',NULL,46.00,NULL,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,0,0),(6,1,'PO2608080146',NULL,53.00,NULL,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,0,0),(7,1,'PO2608130147',NULL,60.00,NULL,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,0,0),(8,1,'PO2608180148',NULL,67.00,NULL,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,0,0),(9,1,'PO2608230149',NULL,74.00,NULL,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,0,0),(10,1,'PO2608280150',NULL,81.00,NULL,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,0,0);
/*!40000 ALTER TABLE `purchase_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_order_detail`
--

DROP TABLE IF EXISTS `purchase_order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order_detail` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `PURCHASE_ORDER_ID` bigint DEFAULT NULL COMMENT '采购订单ID',
  `MATERIAL_ID` bigint DEFAULT NULL COMMENT '物料ID',
  `QTY` decimal(10,2) DEFAULT NULL COMMENT '数量',
  `UNIT_PRICE` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `AMOUNT` decimal(10,2) DEFAULT NULL COMMENT '金额',
  `RECEIVED_QTY` decimal(10,2) DEFAULT NULL COMMENT '收货数量',
  `REMARK` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_order_detail`
--

LOCK TABLES `purchase_order_detail` WRITE;
/*!40000 ALTER TABLE `purchase_order_detail` DISABLE KEYS */;
INSERT INTO `purchase_order_detail` VALUES (1,1,NULL,NULL,10.00,18.00,18.00,10.00,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,0),(2,1,NULL,NULL,11.00,25.00,25.00,11.00,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,0),(3,1,NULL,NULL,12.00,32.00,32.00,12.00,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,0),(4,1,NULL,NULL,13.00,39.00,39.00,13.00,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,14.00,46.00,46.00,14.00,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,0),(6,1,NULL,NULL,15.00,53.00,53.00,15.00,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,0),(7,1,NULL,NULL,16.00,60.00,60.00,16.00,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,0),(8,1,NULL,NULL,17.00,67.00,67.00,17.00,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,0),(9,1,NULL,NULL,18.00,74.00,74.00,18.00,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,0),(10,1,NULL,NULL,19.00,81.00,81.00,19.00,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `purchase_order_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recharge_record`
--

DROP TABLE IF EXISTS `recharge_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recharge_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `member_id` bigint DEFAULT NULL COMMENT '会员ID',
  `user_id` bigint DEFAULT NULL COMMENT '归属用户ID（C端自助充值冗余）',
  `recharge_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '充值单号（业务唯一）',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SUCCESS' COMMENT '状态 PENDING/SUCCESS/CANCELLED',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '充金',
  `gift_amount` decimal(10,2) DEFAULT '0.00' COMMENT '赠金',
  `payment_method` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '渠道 WECHAT/ALIPAY/CASH（预留在线支付）',
  `trade_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '渠道交易号（预留在线支付回填）',
  `confirm_employee_id` bigint DEFAULT NULL COMMENT '确认到账员工ID',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认到账时间',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recharge_record`
--

LOCK TABLES `recharge_record` WRITE;
/*!40000 ALTER TABLE `recharge_record` DISABLE KEYS */;
INSERT INTO `recharge_record` VALUES (1,1,NULL,NULL,'RR2607140281','SUCCESS',18.00,18.00,NULL,'RR2607140282',NULL,NULL,NULL,NULL,NULL,NULL,0),(2,1,NULL,NULL,'RR2607190283','SUCCESS',25.00,25.00,NULL,'RR2607190284',NULL,NULL,NULL,NULL,NULL,NULL,0),(3,1,NULL,NULL,'RR2607240285','SUCCESS',32.00,32.00,NULL,'RR2607240286',NULL,NULL,NULL,NULL,NULL,NULL,0),(4,1,NULL,NULL,'RR2607290287','SUCCESS',39.00,39.00,NULL,'RR2607290288',NULL,NULL,NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,'RR2608030289','SUCCESS',46.00,46.00,NULL,'RR2608030290',NULL,NULL,NULL,NULL,NULL,NULL,0),(6,1,NULL,NULL,'RR2608080291','SUCCESS',53.00,53.00,NULL,'RR2608080292',NULL,NULL,NULL,NULL,NULL,NULL,0),(7,1,NULL,NULL,'RR2608130293','SUCCESS',60.00,60.00,NULL,'RR2608130294',NULL,NULL,NULL,NULL,NULL,NULL,0),(8,1,NULL,NULL,'RR2608180295','SUCCESS',67.00,67.00,NULL,'RR2608180296',NULL,NULL,NULL,NULL,NULL,NULL,0),(9,1,NULL,NULL,'RR2608230297','SUCCESS',74.00,74.00,NULL,'RR2608230298',NULL,NULL,NULL,NULL,NULL,NULL,0),(10,1,NULL,NULL,'RR2608280299','SUCCESS',81.00,81.00,NULL,'RR2608280300',NULL,NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `recharge_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recommendation_cache`
--

DROP TABLE IF EXISTS `recommendation_cache`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommendation_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `recommend_type` tinyint NOT NULL,
  `dish_ids` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `algo_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `score` decimal(3,2) NOT NULL DEFAULT '0.00',
  `expire_time` datetime NOT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recommendation_cache`
--

LOCK TABLES `recommendation_cache` WRITE;
/*!40000 ALTER TABLE `recommendation_cache` DISABLE KEYS */;
INSERT INTO `recommendation_cache` VALUES (1,1,1,1,'[]','常规项目',1.00,'2026-12-29 15:53:52','2026-07-14 00:53:36','2026-07-17 00:53:36',0,NULL,NULL),(2,2,1,2,'[]','默认项目',1.00,'2027-01-13 15:53:52','2026-07-19 02:06:36','2026-07-23 02:06:36',0,NULL,NULL),(3,3,1,3,'[]','补充项目',2.00,'2027-01-28 15:53:52','2026-07-24 03:19:36','2026-07-29 03:19:36',0,NULL,NULL),(4,1,1,4,'[]','备用配置',3.00,'2027-02-12 15:53:52','2026-07-29 04:32:36','2026-08-04 04:32:36',0,NULL,NULL),(5,2,1,1,'[]','扩展配置',4.00,'2027-02-27 15:53:52','2026-08-03 05:45:36','2026-08-10 05:45:36',0,NULL,NULL),(6,3,1,2,'[]','标准配置',5.00,'2027-03-14 15:53:52','2026-08-08 05:58:36','2026-08-16 05:58:36',0,NULL,NULL),(7,1,1,3,'[]','增值条目',6.00,'2027-03-29 15:53:52','2026-08-13 07:11:36','2026-08-22 07:11:36',0,NULL,NULL),(8,2,1,4,'[]','临时条目',7.00,'2027-04-13 15:53:52','2026-08-18 08:24:36','2026-08-28 08:24:36',0,NULL,NULL),(9,3,1,1,'[]','长期条目',8.00,'2027-04-28 15:53:52','2026-08-23 09:37:36','2026-09-03 09:37:36',0,NULL,NULL),(10,1,1,2,'[]','专项记录',9.00,'2027-05-13 15:53:52','2026-08-28 10:50:36','2026-09-09 10:50:36',0,NULL,NULL);
/*!40000 ALTER TABLE `recommendation_cache` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recommendation_feedback`
--

DROP TABLE IF EXISTS `recommendation_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommendation_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `recommend_cache_id` bigint DEFAULT NULL,
  `dish_id` bigint NOT NULL,
  `feedback_type` tinyint NOT NULL,
  `create_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  `create_user` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recommendation_feedback`
--

LOCK TABLES `recommendation_feedback` WRITE;
/*!40000 ALTER TABLE `recommendation_feedback` DISABLE KEYS */;
INSERT INTO `recommendation_feedback` VALUES (1,1,1,NULL,1,1,'2026-07-14 00:53:36',0,NULL,'2026-09-30 15:53:51',NULL),(2,2,1,NULL,2,2,'2026-07-19 02:06:36',0,NULL,'2026-09-30 15:53:51',NULL),(3,3,1,NULL,3,3,'2026-07-24 03:19:36',0,NULL,'2026-09-30 15:53:51',NULL),(4,1,1,NULL,1,4,'2026-07-29 04:32:36',0,NULL,'2026-09-30 15:53:51',NULL),(5,2,1,NULL,2,1,'2026-08-03 05:45:36',0,NULL,'2026-09-30 15:53:51',NULL),(6,3,1,NULL,3,2,'2026-08-08 05:58:36',0,NULL,'2026-09-30 15:53:51',NULL),(7,1,1,NULL,1,3,'2026-08-13 07:11:36',0,NULL,'2026-09-30 15:53:51',NULL),(8,2,1,NULL,2,4,'2026-08-18 08:24:36',0,NULL,'2026-09-30 15:53:51',NULL),(9,3,1,NULL,3,1,'2026-08-23 09:37:36',0,NULL,'2026-09-30 15:53:51',NULL),(10,1,1,NULL,1,2,'2026-08-28 10:50:36',0,NULL,'2026-09-30 15:53:51',NULL);
/*!40000 ALTER TABLE `recommendation_feedback` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reconciliation_statement`
--

DROP TABLE IF EXISTS `reconciliation_statement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reconciliation_statement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `statement_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对账单编',
  `statement_date` date DEFAULT NULL COMMENT '对账日期',
  `platform` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'all' COMMENT '平台 all/wechat/alipay/bank',
  `system_amount` decimal(12,2) DEFAULT NULL COMMENT '系统金',
  `platform_amount` decimal(12,2) DEFAULT NULL COMMENT '平台金',
  `difference_amount` decimal(12,2) DEFAULT NULL COMMENT '差异金额',
  `order_count` int DEFAULT NULL COMMENT '订单',
  `refund_amount` decimal(12,2) DEFAULT NULL COMMENT '款金',
  `refund_count` int DEFAULT NULL COMMENT '款数',
  `fee_amount` decimal(10,2) DEFAULT NULL COMMENT '手续',
  `net_amount` decimal(12,2) DEFAULT NULL COMMENT '净额',
  `status` int DEFAULT '0' COMMENT '状 0- 1-已 2-有差',
  `reconcile_time` datetime DEFAULT NULL COMMENT '对账时间',
  `reconcile_user_id` bigint DEFAULT NULL COMMENT '对账人ID',
  `reconcile_user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对账人',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reconciliation_statement`
--

LOCK TABLES `reconciliation_statement` WRITE;
/*!40000 ALTER TABLE `reconciliation_statement` DISABLE KEYS */;
INSERT INTO `reconciliation_statement` VALUES (1,'ENABLED',NULL,'all',18.00,39.92,18.00,NULL,18.00,NULL,2.00,18.00,0,NULL,NULL,'常规项目','用于演示环境的常规记录，可随时调整。',1,NULL,NULL),(2,'DISABLED',NULL,'all',25.00,39.93,25.00,NULL,25.00,NULL,2.50,25.00,0,NULL,NULL,'默认项目','系统自动补齐的示例数据。',1,NULL,NULL),(3,'PENDING',NULL,'all',32.00,39.94,32.00,NULL,32.00,NULL,3.00,32.00,0,NULL,NULL,'补充项目','按业务流程录入的一条典型记录。',1,NULL,NULL),(4,'SUCCESS',NULL,'all',39.00,39.95,39.00,NULL,39.00,NULL,3.50,39.00,0,NULL,NULL,'备用配置','运营日常维护产生的记录。',1,NULL,NULL),(5,'PROCESSING',NULL,'all',46.00,39.96,46.00,NULL,46.00,NULL,4.00,46.00,0,NULL,NULL,'扩展配置','供联调与走查使用的样例内容。',1,NULL,NULL),(6,'CLOSED',NULL,'all',53.00,39.97,53.00,NULL,53.00,NULL,4.50,53.00,0,NULL,NULL,'标准配置','用于演示环境的常规记录，可随时调整。',1,NULL,NULL),(7,'ENABLED',NULL,'all',60.00,39.98,60.00,NULL,60.00,NULL,5.00,60.00,0,NULL,NULL,'增值条目','系统自动补齐的示例数据。',1,NULL,NULL),(8,'DISABLED',NULL,'all',67.00,39.99,67.00,NULL,67.00,NULL,5.50,67.00,0,NULL,NULL,'临时条目','按业务流程录入的一条典型记录。',1,NULL,NULL),(9,'PENDING',NULL,'all',74.00,39.91,74.00,NULL,74.00,NULL,6.00,74.00,0,NULL,NULL,'长期条目','运营日常维护产生的记录。',1,NULL,NULL),(10,'SUCCESS',NULL,'all',81.00,39.91,81.00,NULL,81.00,NULL,6.50,81.00,0,NULL,NULL,'专项记录','供联调与走查使用的样例内容。',1,NULL,NULL);
/*!40000 ALTER TABLE `reconciliation_statement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refund_record`
--

DROP TABLE IF EXISTS `refund_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refund_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payment_order_id` bigint NOT NULL COMMENT '支付订单id',
  `order_id` bigint DEFAULT NULL COMMENT '业务订单ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `refund_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '退款单号',
  `amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退款原因',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/SUCCESS/FAIL',
  `refund_type` int DEFAULT NULL COMMENT '售后类型：1=整单退款 2=部分退款',
  `apply_user_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `audit_user_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reject_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拒绝原因',
  `refund_time` datetime DEFAULT NULL COMMENT '退款完成时间',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refund_record`
--

LOCK TABLES `refund_record` WRITE;
/*!40000 ALTER TABLE `refund_record` DISABLE KEYS */;
INSERT INTO `refund_record` VALUES (1,1,NULL,1,'RR2607140001',18.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-07-17 00:53:36',NULL),(2,2,NULL,1,'RR2607190002',25.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-07-23 02:06:36',NULL),(3,3,NULL,1,'RR2607240003',32.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-07-29 03:19:36',NULL),(4,1,NULL,1,'RR2607290004',39.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-08-04 04:32:36',NULL),(5,2,NULL,1,'RR2608030005',46.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-08-10 05:45:36',NULL),(6,3,NULL,1,'RR2608080006',53.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-08-16 05:58:36',NULL),(7,1,NULL,1,'RR2608130007',60.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-08-22 07:11:36',NULL),(8,2,NULL,1,'RR2608180008',67.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-08-28 08:24:36',NULL),(9,3,NULL,1,'RR2608230009',74.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-09-03 09:37:36',NULL),(10,1,NULL,1,'RR2608280010',81.00,NULL,'PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 15:53:48',0,1,NULL,'2026-09-09 10:50:36',NULL);
/*!40000 ALTER TABLE `refund_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `region`
--

DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '地区名称',
  `code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '行政区划代码',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级ID，0为省份',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '层级：1省 2市 3区/县',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `region`
--

LOCK TABLES `region` WRITE;
/*!40000 ALTER TABLE `region` DISABLE KEYS */;
INSERT INTO `region` VALUES (1,'常规项目','R2607140311',0,1,0,'2026-07-14 00:53:36','2026-07-17 00:53:36',NULL,NULL,0),(2,'默认项目','R2607190312',0,1,0,'2026-07-19 02:06:36','2026-07-23 02:06:36',NULL,NULL,0),(3,'补充项目','R2607240313',0,1,0,'2026-07-24 03:19:36','2026-07-29 03:19:36',NULL,NULL,0),(4,'备用配置','R2607290314',0,1,0,'2026-07-29 04:32:36','2026-08-04 04:32:36',NULL,NULL,0),(5,'扩展配置','R2608030315',0,1,0,'2026-08-03 05:45:36','2026-08-10 05:45:36',NULL,NULL,0),(6,'标准配置','R2608080316',0,1,0,'2026-08-08 05:58:36','2026-08-16 05:58:36',NULL,NULL,0),(7,'增值条目','R2608130317',0,1,0,'2026-08-13 07:11:36','2026-08-22 07:11:36',NULL,NULL,0),(8,'临时条目','R2608180318',0,1,0,'2026-08-18 08:24:36','2026-08-28 08:24:36',NULL,NULL,0),(9,'长期条目','R2608230319',0,1,0,'2026-08-23 09:37:36','2026-09-03 09:37:36',NULL,NULL,0),(10,'专项记录','R2608280320',0,1,0,'2026-08-28 10:50:36','2026-09-09 10:50:36',NULL,NULL,0);
/*!40000 ALTER TABLE `region` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rider`
--

DROP TABLE IF EXISTS `rider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `current_longitude` decimal(12,8) DEFAULT NULL,
  `current_latitude` decimal(12,8) DEFAULT NULL,
  `status` int DEFAULT '0',
  `current_order_count` int DEFAULT '0',
  `total_order_count` int DEFAULT '0',
  `rating` decimal(3,1) DEFAULT '5.0',
  `last_location_time` datetime DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rider`
--

LOCK TABLES `rider` WRITE;
/*!40000 ALTER TABLE `rider` DISABLE KEYS */;
INSERT INTO `rider` VALUES (1,'张芳',NULL,NULL,NULL,116.33000000,39.92000000,0,0,0,10.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(2,'王芳',NULL,NULL,NULL,116.34000000,39.93000000,0,0,0,11.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(3,'李磊',NULL,NULL,NULL,116.35000000,39.94000000,0,0,0,12.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(4,'赵磊',NULL,NULL,NULL,116.36000000,39.95000000,0,0,0,13.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(5,'刘敏',NULL,NULL,NULL,116.37000000,39.96000000,0,0,0,14.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(6,'陈敏',NULL,NULL,NULL,116.38000000,39.97000000,0,0,0,15.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(7,'杨静',NULL,NULL,NULL,116.39000000,39.98000000,0,0,0,16.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(8,'黄静',NULL,NULL,NULL,116.31000000,39.99000000,0,0,0,17.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(9,'周强',NULL,NULL,NULL,116.31100000,39.91000000,0,0,0,18.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52'),(10,'吴强',NULL,NULL,NULL,116.31200000,39.91100000,0,0,0,19.0,NULL,1,'2026-09-30 15:53:52','2026-09-30 15:53:52');
/*!40000 ALTER TABLE `rider` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rider_account`
--

DROP TABLE IF EXISTS `rider_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `rider_id` bigint DEFAULT NULL COMMENT '骑手id',
  `withdrawable_balance` decimal(12,2) DEFAULT '0.00' COMMENT '可提现余额',
  `frozen_balance` decimal(12,2) DEFAULT '0.00' COMMENT '冻结中金额',
  `total_income` decimal(12,2) DEFAULT '0.00' COMMENT '累计收入',
  `total_withdrawn` decimal(12,2) DEFAULT '0.00' COMMENT '累计已提现',
  `version` int DEFAULT '0' COMMENT '乐观锁',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rider_id` (`rider_id`,`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rider_account`
--

LOCK TABLES `rider_account` WRITE;
/*!40000 ALTER TABLE `rider_account` DISABLE KEYS */;
INSERT INTO `rider_account` VALUES (1,1,NULL,18.00,18.00,18.00,18.00,0,NULL,NULL),(2,1,NULL,25.00,25.00,25.00,25.00,0,NULL,NULL),(3,1,NULL,32.00,32.00,32.00,32.00,0,NULL,NULL),(4,1,NULL,39.00,39.00,39.00,39.00,0,NULL,NULL),(5,1,NULL,46.00,46.00,46.00,46.00,0,NULL,NULL),(6,1,NULL,53.00,53.00,53.00,53.00,0,NULL,NULL),(7,1,NULL,60.00,60.00,60.00,60.00,0,NULL,NULL),(8,1,NULL,67.00,67.00,67.00,67.00,0,NULL,NULL),(9,1,NULL,74.00,74.00,74.00,74.00,0,NULL,NULL),(10,1,NULL,81.00,81.00,81.00,81.00,0,NULL,NULL);
/*!40000 ALTER TABLE `rider_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rider_evaluation`
--

DROP TABLE IF EXISTS `rider_evaluation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider_evaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `order_id` bigint DEFAULT NULL COMMENT '订单id',
  `user_id` bigint DEFAULT NULL COMMENT '评价用户id',
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评价用户名',
  `rider_id` bigint DEFAULT NULL COMMENT '骑手id',
  `rider_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '骑手姓名',
  `star_rating` int DEFAULT NULL COMMENT '评分1-5',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评价内容',
  `tags` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签JSON数组',
  `anonymous` int DEFAULT '0' COMMENT '0实名1匿名',
  `status` int DEFAULT '1' COMMENT '0待审1通过2拒绝',
  `reply_content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商家回复',
  `reply_time` datetime DEFAULT NULL COMMENT '回复时间',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_id` (`order_id`,`rider_id`,`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rider_evaluation`
--

LOCK TABLES `rider_evaluation` WRITE;
/*!40000 ALTER TABLE `rider_evaluation` DISABLE KEYS */;
/*!40000 ALTER TABLE `rider_evaluation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rider_exception_order`
--

DROP TABLE IF EXISTS `rider_exception_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider_exception_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单id',
  `order_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单号',
  `rider_id` bigint DEFAULT NULL COMMENT '上报骑手id',
  `rider_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上报骑手姓名',
  `exception_type` int DEFAULT NULL COMMENT '1联系不上顾客2商品破损3地址有误4顾客拒收5申请转单6其他',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常描述',
  `status` int DEFAULT '0' COMMENT '0待处理1已处理2已转单3已关闭',
  `handle_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理备注',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人id',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rider_exception_order`
--

LOCK TABLES `rider_exception_order` WRITE;
/*!40000 ALTER TABLE `rider_exception_order` DISABLE KEYS */;
INSERT INTO `rider_exception_order` VALUES (1,1,NULL,NULL,NULL,'张芳',NULL,'用于演示环境的常规记录，可随时调整。',0,'常规参数',NULL,NULL,NULL,NULL,NULL,NULL,0),(2,1,NULL,NULL,NULL,'王芳',NULL,'系统自动补齐的示例数据。',0,'默认参数',NULL,NULL,NULL,NULL,NULL,NULL,0),(3,1,NULL,NULL,NULL,'李磊',NULL,'按业务流程录入的一条典型记录。',0,'补充参数',NULL,NULL,NULL,NULL,NULL,NULL,0),(4,1,NULL,NULL,NULL,'赵磊',NULL,'运营日常维护产生的记录。',0,'备用参数',NULL,NULL,NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,NULL,'刘敏',NULL,'供联调与走查使用的样例内容。',0,'扩展参数',NULL,NULL,NULL,NULL,NULL,NULL,0),(6,1,NULL,NULL,NULL,'陈敏',NULL,'用于演示环境的常规记录，可随时调整。',0,'标准参数',NULL,NULL,NULL,NULL,NULL,NULL,0),(7,1,NULL,NULL,NULL,'杨静',NULL,'系统自动补齐的示例数据。',0,'增值参数',NULL,NULL,NULL,NULL,NULL,NULL,0),(8,1,NULL,NULL,NULL,'黄静',NULL,'按业务流程录入的一条典型记录。',0,'临时参数',NULL,NULL,NULL,NULL,NULL,NULL,0),(9,1,NULL,NULL,NULL,'周强',NULL,'运营日常维护产生的记录。',0,'长期参数',NULL,NULL,NULL,NULL,NULL,NULL,0),(10,1,NULL,NULL,NULL,'吴强',NULL,'供联调与走查使用的样例内容。',0,'专项参数',NULL,NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `rider_exception_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rider_income_ledger`
--

DROP TABLE IF EXISTS `rider_income_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider_income_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `rider_id` bigint DEFAULT NULL COMMENT '骑手id',
  `order_id` bigint DEFAULT NULL COMMENT '订单id（幂等键）',
  `amount` decimal(12,2) DEFAULT NULL COMMENT '入账金额',
  `status` int DEFAULT '1' COMMENT '状态',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rider_income_ledger`
--

LOCK TABLES `rider_income_ledger` WRITE;
/*!40000 ALTER TABLE `rider_income_ledger` DISABLE KEYS */;
INSERT INTO `rider_income_ledger` VALUES (1,1,NULL,NULL,18.00,1,NULL),(2,1,NULL,NULL,25.00,1,NULL),(3,1,NULL,NULL,32.00,1,NULL),(4,1,NULL,NULL,39.00,1,NULL),(5,1,NULL,NULL,46.00,1,NULL),(6,1,NULL,NULL,53.00,1,NULL),(7,1,NULL,NULL,60.00,1,NULL),(8,1,NULL,NULL,67.00,1,NULL),(9,1,NULL,NULL,74.00,1,NULL),(10,1,NULL,NULL,81.00,1,NULL);
/*!40000 ALTER TABLE `rider_income_ledger` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rider_location_record`
--

DROP TABLE IF EXISTS `rider_location_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider_location_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rider_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `longitude` decimal(12,8) NOT NULL,
  `latitude` decimal(12,8) NOT NULL,
  `speed` decimal(5,2) DEFAULT NULL,
  `direction` decimal(5,2) DEFAULT NULL,
  `record_time` datetime NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rider_location_record`
--

LOCK TABLES `rider_location_record` WRITE;
/*!40000 ALTER TABLE `rider_location_record` DISABLE KEYS */;
INSERT INTO `rider_location_record` VALUES (1,1,NULL,116.33000000,39.92000000,10.00,10.00,'2026-07-14 00:53:36',1,'2026-09-30 15:53:50'),(2,2,NULL,116.34000000,39.93000000,11.00,11.00,'2026-07-19 02:06:36',1,'2026-09-30 15:53:50'),(3,3,NULL,116.35000000,39.94000000,12.00,12.00,'2026-07-24 03:19:36',1,'2026-09-30 15:53:50'),(4,1,NULL,116.36000000,39.95000000,13.00,13.00,'2026-07-29 04:32:36',1,'2026-09-30 15:53:50'),(5,2,NULL,116.37000000,39.96000000,14.00,14.00,'2026-08-03 05:45:36',1,'2026-09-30 15:53:50'),(6,3,NULL,116.38000000,39.97000000,15.00,15.00,'2026-08-08 05:58:36',1,'2026-09-30 15:53:50'),(7,1,NULL,116.39000000,39.98000000,16.00,16.00,'2026-08-13 07:11:36',1,'2026-09-30 15:53:50'),(8,2,NULL,116.31000000,39.99000000,17.00,17.00,'2026-08-18 08:24:36',1,'2026-09-30 15:53:50'),(9,3,NULL,116.31100000,39.91000000,18.00,18.00,'2026-08-23 09:37:36',1,'2026-09-30 15:53:50'),(10,1,NULL,116.31200000,39.91100000,19.00,19.00,'2026-08-28 10:50:36',1,'2026-09-30 15:53:50');
/*!40000 ALTER TABLE `rider_location_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rider_message`
--

DROP TABLE IF EXISTS `rider_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `rider_id` bigint DEFAULT NULL COMMENT '接收骑手id',
  `type` int DEFAULT NULL COMMENT '1派单提醒2催单提醒3异常处理4系统公告5其他',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '消息标题',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '消息内容',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务id（订单id）',
  `is_read` int DEFAULT '0' COMMENT '0未读1已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_rider_read` (`rider_id`,`is_read`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rider_message`
--

LOCK TABLES `rider_message` WRITE;
/*!40000 ALTER TABLE `rider_message` DISABLE KEYS */;
INSERT INTO `rider_message` VALUES (1,1,NULL,NULL,'周末特惠通知',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0),(2,1,NULL,NULL,'会员日活动说明',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0),(3,1,NULL,NULL,'配送范围调整公告',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0),(4,1,NULL,NULL,'菜单更新说明',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,'门店歇业通知',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0),(6,1,NULL,NULL,'新品上市介绍',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0),(7,1,NULL,NULL,'服务流程规范',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0),(8,1,NULL,NULL,'月度经营小结',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0),(9,1,NULL,NULL,'客户反馈处理记录',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0),(10,1,NULL,NULL,'系统升级安排',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `rider_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rider_remember_token`
--

DROP TABLE IF EXISTS `rider_remember_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider_remember_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rider_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expire_time` datetime NOT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_rider` (`rider_id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rider_remember_token`
--

LOCK TABLES `rider_remember_token` WRITE;
/*!40000 ALTER TABLE `rider_remember_token` DISABLE KEYS */;
INSERT INTO `rider_remember_token` VALUES (1,1,1,'RRT1607a315c2e401','2026-12-29 15:53:50','2026-09-30 15:53:50'),(2,2,1,'RRT1607a345bbc102','2027-01-13 15:53:50','2026-09-30 15:53:50'),(3,3,1,'RRT1607a374938e03','2027-01-28 15:53:50','2026-09-30 15:53:50'),(4,1,1,'RRT1607a39d6dc304','2027-02-12 15:53:50','2026-09-30 15:53:50'),(5,2,1,'RRT1607a3c97d1005','2027-02-27 15:53:50','2026-09-30 15:53:50'),(6,3,1,'RRT1607a3f624b506','2027-03-14 15:53:50','2026-09-30 15:53:50'),(7,1,1,'RRT1607a41eaed607','2027-03-29 15:53:50','2026-09-30 15:53:50'),(8,2,1,'RRT1607a447b72308','2027-04-13 15:53:50','2026-09-30 15:53:50'),(9,3,1,'RRT1607a46fdc1809','2027-04-28 15:53:50','2026-09-30 15:53:50'),(10,1,1,'RRT1607a4a047cd10','2027-05-13 15:53:50','2026-09-30 15:53:50');
/*!40000 ALTER TABLE `rider_remember_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rider_withdrawal`
--

DROP TABLE IF EXISTS `rider_withdrawal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rider_withdrawal` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `rider_id` bigint DEFAULT NULL COMMENT '骑手id',
  `amount` decimal(12,2) DEFAULT NULL COMMENT '提现金额',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人id',
  `reviewer_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核人名称',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注/拒绝原因',
  `version` int DEFAULT '0' COMMENT '乐观锁',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rider_withdrawal`
--

LOCK TABLES `rider_withdrawal` WRITE;
/*!40000 ALTER TABLE `rider_withdrawal` DISABLE KEYS */;
/*!40000 ALTER TABLE `rider_withdrawal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（NULL=全局角色）',
  `role_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色权限字符串',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,1,'常规项目','DEFAULT','用于演示环境的常规记录，可随时调整。',0,1,'2026-07-14 00:53:36','2026-07-17 00:53:36',NULL,NULL,0),(2,1,'默认项目','NORMAL','系统自动补齐的示例数据。',0,1,'2026-07-19 02:06:36','2026-07-23 02:06:36',NULL,NULL,0),(3,1,'补充项目','SPECIAL','按业务流程录入的一条典型记录。',0,1,'2026-07-24 03:19:36','2026-07-29 03:19:36',NULL,NULL,0),(4,1,'备用配置','TEMP','运营日常维护产生的记录。',0,1,'2026-07-29 04:32:36','2026-08-04 04:32:36',NULL,NULL,0),(5,1,'扩展配置','CUSTOM','供联调与走查使用的样例内容。',0,1,'2026-08-03 05:45:36','2026-08-10 05:45:36',NULL,NULL,0),(6,1,'标准配置','BASIC','用于演示环境的常规记录，可随时调整。',0,1,'2026-08-08 05:58:36','2026-08-16 05:58:36',NULL,NULL,0),(7,1,'增值条目','DEFAULT','系统自动补齐的示例数据。',0,1,'2026-08-13 07:11:36','2026-08-22 07:11:36',NULL,NULL,0),(8,1,'临时条目','NORMAL','按业务流程录入的一条典型记录。',0,1,'2026-08-18 08:24:36','2026-08-28 08:24:36',NULL,NULL,0),(9,1,'长期条目','SPECIAL','运营日常维护产生的记录。',0,1,'2026-08-23 09:37:36','2026-09-03 09:37:36',NULL,NULL,0),(10,1,'专项记录','TEMP','供联调与走查使用的样例内容。',0,1,'2026-08-28 10:50:36','2026-09-09 10:50:36',NULL,NULL,0);
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permission`
--

LOCK TABLES `role_permission` WRITE;
/*!40000 ALTER TABLE `role_permission` DISABLE KEYS */;
INSERT INTO `role_permission` VALUES (1,6,5,'2026-07-14 00:53:36'),(2,9,8,'2026-07-19 02:06:36'),(3,2,1,'2026-07-24 03:19:36'),(4,5,4,'2026-07-29 04:32:36'),(5,8,7,'2026-08-03 05:45:36'),(6,1,10,'2026-08-08 05:58:36'),(7,4,3,'2026-08-13 07:11:36'),(8,7,6,'2026-08-18 08:24:36'),(9,10,9,'2026-08-23 09:37:36'),(10,3,2,'2026-08-28 10:50:36');
/*!40000 ALTER TABLE `role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `setmeal`
--

DROP TABLE IF EXISTS `setmeal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '菜品分类id',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL COMMENT '套餐价格',
  `code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '编码',
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片',
  `description` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述信息',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 0:停用 1:启用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `setmeal`
--

LOCK TABLES `setmeal` WRITE;
/*!40000 ALTER TABLE `setmeal` DISABLE KEYS */;
INSERT INTO `setmeal` VALUES (1,1,'双人精选套餐',18.00,'S2607140021',NULL,'用于演示环境的常规记录，可随时调整。',1,'2026-07-14 00:53:36','2026-07-17 00:53:36',1,1,0,1),(2,2,'家庭分享餐',25.00,'S2607190022',NULL,'系统自动补齐的示例数据。',1,'2026-07-19 02:06:36','2026-07-23 02:06:36',2,2,0,1),(3,3,'商务简餐',32.00,'S2607240023',NULL,'按业务流程录入的一条典型记录。',1,'2026-07-24 03:19:36','2026-07-29 03:19:36',3,3,0,1),(4,1,'轻食沙拉餐',39.00,'S2607290024',NULL,'运营日常维护产生的记录。',1,'2026-07-29 04:32:36','2026-08-04 04:32:36',1,1,0,1),(5,2,'麻辣香锅套餐',46.00,'S2608030025',NULL,'供联调与走查使用的样例内容。',1,'2026-08-03 05:45:36','2026-08-10 05:45:36',2,2,0,1),(6,3,'元气早餐组合',53.00,'S2608080026',NULL,'用于演示环境的常规记录，可随时调整。',1,'2026-08-08 05:58:36','2026-08-16 05:58:36',3,3,0,1),(7,1,'下午茶套餐',60.00,'S2608130027',NULL,'系统自动补齐的示例数据。',1,'2026-08-13 07:11:36','2026-08-22 07:11:36',1,1,0,1),(8,2,'深夜食堂套餐',67.00,'S2608180028',NULL,'按业务流程录入的一条典型记录。',1,'2026-08-18 08:24:36','2026-08-28 08:24:36',2,2,0,1),(9,3,'双人精选套餐-2',74.00,'S2608230029',NULL,'运营日常维护产生的记录。',1,'2026-08-23 09:37:36','2026-09-03 09:37:36',3,3,0,1),(10,1,'家庭分享餐-2',81.00,'S2608280030',NULL,'供联调与走查使用的样例内容。',1,'2026-08-28 10:50:36','2026-09-09 10:50:36',1,1,0,1);
/*!40000 ALTER TABLE `setmeal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `setmeal_dish`
--

DROP TABLE IF EXISTS `setmeal_dish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal_dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` bigint NOT NULL COMMENT '套餐id',
  `dish_id` bigint NOT NULL COMMENT '菜品id',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜品名称（冗余）',
  `price` decimal(10,2) NOT NULL COMMENT '菜品原价（冗余）',
  `copies` int NOT NULL DEFAULT '1' COMMENT '份数',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_user` bigint NOT NULL COMMENT '创建人',
  `update_user` bigint NOT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `setmeal_dish`
--

LOCK TABLES `setmeal_dish` WRITE;
/*!40000 ALTER TABLE `setmeal_dish` DISABLE KEYS */;
INSERT INTO `setmeal_dish` VALUES (1,7,1,'宫保鸡丁',18.00,1,0,1,'2026-07-14 00:53:36','2026-07-17 00:53:36',1,1,0),(2,10,2,'鱼香肉丝',25.00,1,0,1,'2026-07-19 02:06:36','2026-07-23 02:06:36',2,2,0),(3,3,3,'麻婆豆腐',32.00,1,0,1,'2026-07-24 03:19:36','2026-07-29 03:19:36',3,3,0),(4,6,1,'红烧狮子头',39.00,1,0,1,'2026-07-29 04:32:36','2026-08-04 04:32:36',1,1,0),(5,9,2,'酸汤肥牛',46.00,1,0,1,'2026-08-03 05:45:36','2026-08-10 05:45:36',2,2,0),(6,2,3,'干锅花菜',53.00,1,0,1,'2026-08-08 05:58:36','2026-08-16 05:58:36',3,3,0),(7,5,1,'手撕包菜',60.00,1,0,1,'2026-08-13 07:11:36','2026-08-22 07:11:36',1,1,0),(8,8,2,'蒜蓉粉丝虾',67.00,1,0,1,'2026-08-18 08:24:36','2026-08-28 08:24:36',2,2,0),(9,1,3,'京味小酥肉',74.00,1,0,1,'2026-08-23 09:37:36','2026-09-03 09:37:36',3,3,0),(10,4,1,'口水鸡',81.00,1,0,1,'2026-08-28 10:50:36','2026-09-09 10:50:36',1,1,0);
/*!40000 ALTER TABLE `setmeal_dish` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shopping_cart`
--

DROP TABLE IF EXISTS `shopping_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名称',
  `user_id` bigint NOT NULL COMMENT '主键',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shopping_cart`
--

LOCK TABLES `shopping_cart` WRITE;
/*!40000 ALTER TABLE `shopping_cart` DISABLE KEYS */;
INSERT INTO `shopping_cart` VALUES (1,'瑞吉望京店',1,NULL,NULL,NULL,1,18.00,NULL,1,NULL),(2,'瑞吉中关村店',2,NULL,NULL,NULL,1,25.00,NULL,1,NULL),(3,'瑞吉国贸店',3,NULL,NULL,NULL,1,32.00,NULL,1,NULL),(4,'瑞吉西单店',1,NULL,NULL,NULL,1,39.00,NULL,1,NULL),(5,'瑞吉五道口店',2,NULL,NULL,NULL,1,46.00,NULL,1,NULL),(6,'瑞吉回龙观店',3,NULL,NULL,NULL,1,53.00,NULL,1,NULL),(7,'瑞吉亦庄店',1,NULL,NULL,NULL,1,60.00,NULL,1,NULL),(8,'瑞吉双井店',2,NULL,NULL,NULL,1,67.00,NULL,1,NULL),(9,'瑞吉亚运村店',3,NULL,NULL,NULL,1,74.00,NULL,1,NULL),(10,'瑞吉丽泽店',1,NULL,NULL,NULL,1,81.00,NULL,1,NULL);
/*!40000 ALTER TABLE `shopping_cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_check`
--

DROP TABLE IF EXISTS `stock_check`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_check` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `CHECK_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '盘点单号',
  `STATUS` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态',
  `TOTAL_DIFF_AMOUNT` decimal(10,2) DEFAULT NULL COMMENT '总差异金额',
  `OPERATOR` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作员',
  `REMARK` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `VOUCHER_IMAGES` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '凭证图片（逗号分隔，最多5张）',
  `CREATED_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_check`
--

LOCK TABLES `stock_check` WRITE;
/*!40000 ALTER TABLE `stock_check` DISABLE KEYS */;
INSERT INTO `stock_check` VALUES (1,1,'SC2607140081',NULL,18.00,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,0),(2,1,'SC2607190082',NULL,25.00,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,0),(3,1,'SC2607240083',NULL,32.00,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,0),(4,1,'SC2607290084',NULL,39.00,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,0),(5,1,'SC2608030085',NULL,46.00,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,0),(6,1,'SC2608080086',NULL,53.00,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,0),(7,1,'SC2608130087',NULL,60.00,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,0),(8,1,'SC2608180088',NULL,67.00,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,0),(9,1,'SC2608230089',NULL,74.00,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,0),(10,1,'SC2608280090',NULL,81.00,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `stock_check` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_check_detail`
--

DROP TABLE IF EXISTS `stock_check_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_check_detail` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `CHECK_ID` bigint DEFAULT NULL COMMENT '盘点ID',
  `MATERIAL_ID` bigint DEFAULT NULL COMMENT '物料ID',
  `BOOK_QTY` decimal(10,2) DEFAULT NULL COMMENT '账面数量',
  `ACTUAL_QTY` decimal(10,2) DEFAULT NULL COMMENT '实际数量',
  `DIFF_QTY` decimal(10,2) DEFAULT NULL COMMENT '差异数量',
  `REMARK` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_check_detail`
--

LOCK TABLES `stock_check_detail` WRITE;
/*!40000 ALTER TABLE `stock_check_detail` DISABLE KEYS */;
INSERT INTO `stock_check_detail` VALUES (1,1,NULL,NULL,10.00,10.00,10.00,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,0),(2,1,NULL,NULL,11.00,11.00,11.00,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,0),(3,1,NULL,NULL,12.00,12.00,12.00,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,0),(4,1,NULL,NULL,13.00,13.00,13.00,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,14.00,14.00,14.00,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,0),(6,1,NULL,NULL,15.00,15.00,15.00,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,0),(7,1,NULL,NULL,16.00,16.00,16.00,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,0),(8,1,NULL,NULL,17.00,17.00,17.00,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,0),(9,1,NULL,NULL,18.00,18.00,18.00,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,0),(10,1,NULL,NULL,19.00,19.00,19.00,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `stock_check_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_record`
--

DROP TABLE IF EXISTS `stock_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_record` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `MATERIAL_ID` bigint DEFAULT NULL COMMENT '物料ID',
  `TYPE` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型',
  `QTY` decimal(10,2) DEFAULT NULL COMMENT '数量',
  `UNIT_PRICE` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `TOTAL_AMOUNT` decimal(10,2) DEFAULT NULL COMMENT '总金额',
  `BIZ_ID` bigint DEFAULT NULL COMMENT '业务ID',
  `REMARK` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `VOUCHER_IMAGES` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '凭证图片（逗号分隔，最多5张）',
  `OPERATOR` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作员',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_record`
--

LOCK TABLES `stock_record` WRITE;
/*!40000 ALTER TABLE `stock_record` DISABLE KEYS */;
INSERT INTO `stock_record` VALUES (1,1,NULL,NULL,10.00,18.00,18.00,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,NULL,0),(2,1,NULL,NULL,11.00,25.00,25.00,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,NULL,0),(3,1,NULL,NULL,12.00,32.00,32.00,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,NULL,0),(4,1,NULL,NULL,13.00,39.00,39.00,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,14.00,46.00,46.00,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,NULL,0),(6,1,NULL,NULL,15.00,53.00,53.00,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,NULL,NULL,NULL,0),(7,1,NULL,NULL,16.00,60.00,60.00,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,NULL,NULL,NULL,0),(8,1,NULL,NULL,17.00,67.00,67.00,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,NULL,NULL,NULL,0),(9,1,NULL,NULL,18.00,74.00,74.00,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,NULL,NULL,NULL,0),(10,1,NULL,NULL,19.00,81.00,81.00,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `stock_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store`
--

DROP TABLE IF EXISTS `store`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '门店名称',
  `address` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '门店地址',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ϵ绰',
  `business_hours` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '营业时间',
  `logo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '门店Logo',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 0:停业 1:营业',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store`
--

LOCK TABLES `store` WRITE;
/*!40000 ALTER TABLE `store` DISABLE KEYS */;
/*!40000 ALTER TABLE `store` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_config`
--

DROP TABLE IF EXISTS `store_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户/门店ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置键',
  `config_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置值',
  `config_type` int DEFAULT NULL COMMENT '配置类型 1:功能配置 2:运营参数 3:显示设置 4:其他',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置说明',
  `created_by` bigint DEFAULT NULL COMMENT '配置创建人(总部管理员)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_config`
--

LOCK TABLES `store_config` WRITE;
/*!40000 ALTER TABLE `store_config` DISABLE KEYS */;
INSERT INTO `store_config` VALUES (1,1,NULL,NULL,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,0),(2,1,NULL,NULL,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,0),(3,1,NULL,NULL,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,0),(4,1,NULL,NULL,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,0),(5,1,NULL,NULL,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,0),(6,1,NULL,NULL,NULL,'用于演示环境的常规记录，可随时调整。',NULL,NULL,NULL,0),(7,1,NULL,NULL,NULL,'系统自动补齐的示例数据。',NULL,NULL,NULL,0),(8,1,NULL,NULL,NULL,'按业务流程录入的一条典型记录。',NULL,NULL,NULL,0),(9,1,NULL,NULL,NULL,'运营日常维护产生的记录。',NULL,NULL,NULL,0),(10,1,NULL,NULL,NULL,'供联调与走查使用的样例内容。',NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `store_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_daily_summary`
--

DROP TABLE IF EXISTS `store_daily_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_daily_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '门店ID',
  `summary_date` date DEFAULT NULL COMMENT '统计日期',
  `total_orders` int DEFAULT NULL COMMENT '订单总数',
  `completed_orders` int DEFAULT NULL COMMENT '已完成订单数',
  `cancelled_orders` int DEFAULT NULL COMMENT '取消订单',
  `total_amount` decimal(12,2) DEFAULT NULL COMMENT '订单总金额',
  `actual_amount` decimal(12,2) DEFAULT NULL COMMENT '实收金额',
  `new_users` int DEFAULT NULL COMMENT '新增用户',
  `avg_order_amount` decimal(10,2) DEFAULT NULL COMMENT '平均订单金额',
  `top_dish_json` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '热销菜品TOP10 JSON',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_daily_summary`
--

LOCK TABLES `store_daily_summary` WRITE;
/*!40000 ALTER TABLE `store_daily_summary` DISABLE KEYS */;
INSERT INTO `store_daily_summary` VALUES (1,1,NULL,NULL,NULL,NULL,18.00,18.00,NULL,18.00,NULL,NULL,NULL,0),(2,1,NULL,NULL,NULL,NULL,25.00,25.00,NULL,25.00,NULL,NULL,NULL,0),(3,1,NULL,NULL,NULL,NULL,32.00,32.00,NULL,32.00,NULL,NULL,NULL,0),(4,1,NULL,NULL,NULL,NULL,39.00,39.00,NULL,39.00,NULL,NULL,NULL,0),(5,1,NULL,NULL,NULL,NULL,46.00,46.00,NULL,46.00,NULL,NULL,NULL,0),(6,1,NULL,NULL,NULL,NULL,53.00,53.00,NULL,53.00,NULL,NULL,NULL,0),(7,1,NULL,NULL,NULL,NULL,60.00,60.00,NULL,60.00,NULL,NULL,NULL,0),(8,1,NULL,NULL,NULL,NULL,67.00,67.00,NULL,67.00,NULL,NULL,NULL,0),(9,1,NULL,NULL,NULL,NULL,74.00,74.00,NULL,74.00,NULL,NULL,NULL,0),(10,1,NULL,NULL,NULL,NULL,81.00,81.00,NULL,81.00,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `store_daily_summary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_employee_permission`
--

DROP TABLE IF EXISTS `store_employee_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_employee_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint DEFAULT NULL COMMENT '员工ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '门店ID',
  `role_type` int DEFAULT NULL COMMENT '角色类型 1:店长 2:厨师 3:服务员 4:收银员 5:配菜员',
  `permissions` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限列表 JSON，如 ["dish:view","dish:edit","order:view"]',
  `is_active` int DEFAULT NULL COMMENT '是否生效 0:否 1:是',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建用户',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_employee_permission`
--

LOCK TABLES `store_employee_permission` WRITE;
/*!40000 ALTER TABLE `store_employee_permission` DISABLE KEYS */;
INSERT INTO `store_employee_permission` VALUES (1,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(2,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(3,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(4,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(5,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(6,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(7,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(8,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(9,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(10,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `store_employee_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_info`
--

DROP TABLE IF EXISTS `store_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '所属租户/门店ID',
  `store_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '门店编码，如：BJ001、SH001',
  `store_type` int DEFAULT NULL COMMENT '门店类型 1:直营总店 2:直营分店 3:加盟',
  `parent_tenant_id` bigint DEFAULT NULL COMMENT '上级总店tenantId，NULL表示总店本身',
  `business_hours` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '营业时间，如 9:00-22:00',
  `notice` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '门店公告，C端首页展示',
  `delivery_radius` int DEFAULT NULL COMMENT '配送半径(米)',
  `min_delivery_amount` decimal(10,2) DEFAULT NULL COMMENT '最低起送金额',
  `delivery_fee` decimal(10,2) DEFAULT NULL COMMENT '配送费',
  `is_delivery_enabled` int DEFAULT NULL COMMENT '是否外卖 0:否 1:是',
  `is_dine_in_enabled` int DEFAULT NULL COMMENT '是否堂食 0:否 1:是',
  `contact_person` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '门店联系',
  `contact_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ŵϵ绰',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `pause_order` int NOT NULL DEFAULT '0' COMMENT '暂停接单 0:正常 1:暂停',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建用户',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_info`
--

LOCK TABLES `store_info` WRITE;
/*!40000 ALTER TABLE `store_info` DISABLE KEYS */;
INSERT INTO `store_info` VALUES (1,1,'常规项目',NULL,NULL,NULL,NULL,NULL,18.00,2.00,NULL,NULL,'常规项目',NULL,116.3300000,39.9200000,0,NULL,NULL,NULL,NULL,0),(2,1,'默认项目',NULL,NULL,NULL,NULL,NULL,25.00,2.50,NULL,NULL,'默认项目',NULL,116.3400000,39.9300000,0,NULL,NULL,NULL,NULL,0),(3,1,'补充项目',NULL,NULL,NULL,NULL,NULL,32.00,3.00,NULL,NULL,'补充项目',NULL,116.3500000,39.9400000,0,NULL,NULL,NULL,NULL,0),(4,1,'备用项目',NULL,NULL,NULL,NULL,NULL,39.00,3.50,NULL,NULL,'备用项目',NULL,116.3600000,39.9500000,0,NULL,NULL,NULL,NULL,0),(5,1,'扩展项目',NULL,NULL,NULL,NULL,NULL,46.00,4.00,NULL,NULL,'扩展项目',NULL,116.3700000,39.9600000,0,NULL,NULL,NULL,NULL,0),(6,1,'标准项目',NULL,NULL,NULL,NULL,NULL,53.00,4.50,NULL,NULL,'标准项目',NULL,116.3800000,39.9700000,0,NULL,NULL,NULL,NULL,0),(7,1,'增值项目',NULL,NULL,NULL,NULL,NULL,60.00,5.00,NULL,NULL,'增值项目',NULL,116.3900000,39.9800000,0,NULL,NULL,NULL,NULL,0),(8,1,'临时项目',NULL,NULL,NULL,NULL,NULL,67.00,5.50,NULL,NULL,'临时项目',NULL,116.3100000,39.9900000,0,NULL,NULL,NULL,NULL,0),(9,1,'长期项目',NULL,NULL,NULL,NULL,NULL,74.00,6.00,NULL,NULL,'长期项目',NULL,116.3110000,39.9100000,0,NULL,NULL,NULL,NULL,0),(10,1,'专项项目',NULL,NULL,NULL,NULL,NULL,81.00,6.50,NULL,NULL,'专项项目',NULL,116.3120000,39.9110000,0,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `store_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `store_sync_log`
--

DROP TABLE IF EXISTS `store_sync_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_tenant_id` bigint DEFAULT NULL COMMENT '来源门店ID(通常指总部)',
  `target_tenant_id` bigint DEFAULT NULL COMMENT '目标门店ID',
  `sync_type` int DEFAULT NULL COMMENT '同步类型 1:菜品同步 2:分类同步 3:套餐同步 4:配置同步 5:优惠券同步',
  `sync_mode` int DEFAULT NULL COMMENT '同步模式 1:全量同步 2:增量同步 3:选择性同步',
  `sync_status` int DEFAULT NULL COMMENT '同步状态 0:进行中 1:成功 2:失败 3:部分成功',
  `sync_count` int DEFAULT NULL COMMENT '同步数量',
  `fail_count` int DEFAULT NULL COMMENT '失败数量',
  `error_detail` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误详情',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `start_time` datetime DEFAULT NULL COMMENT '同步开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime DEFAULT NULL COMMENT '门店同步日志',
  `update_time` datetime DEFAULT NULL COMMENT '门店同步日志',
  `create_user` bigint DEFAULT NULL COMMENT '门店同步日志',
  `update_user` bigint DEFAULT NULL COMMENT '门店同步日志',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `store_sync_log`
--

LOCK TABLES `store_sync_log` WRITE;
/*!40000 ALTER TABLE `store_sync_log` DISABLE KEYS */;
INSERT INTO `store_sync_log` VALUES (1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(2,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(3,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(4,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(5,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(6,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(7,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(8,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0),(10,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `store_sync_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier`
--

DROP TABLE IF EXISTS `supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `NAME` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `CONTACT` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人',
  `PHONE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ϵ绰',
  `ADDRESS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `LICENSE_IMAGES` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资质图片（逗号分隔，最多5张）',
  `STATUS` int DEFAULT NULL COMMENT '状态',
  `CREATED_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier`
--

LOCK TABLES `supplier` WRITE;
/*!40000 ALTER TABLE `supplier` DISABLE KEYS */;
INSERT INTO `supplier` VALUES (1,1,'常规项目','张芳',NULL,'北京市朝阳区望京街道广顺北大街33号院',NULL,NULL,NULL,NULL,NULL,NULL,0),(2,1,'默认项目','王芳',NULL,'北京市海淀区中关村大街27号',NULL,NULL,NULL,NULL,NULL,NULL,0),(3,1,'补充项目','李磊',NULL,'北京市东城区建国门内大街5号',NULL,NULL,NULL,NULL,NULL,NULL,0),(4,1,'备用配置','赵磊',NULL,'北京市西城区金融大街28号',NULL,NULL,NULL,NULL,NULL,NULL,0),(5,1,'扩展配置','刘敏',NULL,'北京市丰台区南三环西路5号',NULL,NULL,NULL,NULL,NULL,NULL,0),(6,1,'标准配置','陈敏',NULL,'北京市通州区新华大街16号',NULL,NULL,NULL,NULL,NULL,NULL,0),(7,1,'增值条目','杨静',NULL,'北京市石景山区鲁谷路12号',NULL,NULL,NULL,NULL,NULL,NULL,0),(8,1,'临时条目','黄静',NULL,'北京市昌平区回龙观东大街18号',NULL,NULL,NULL,NULL,NULL,NULL,0),(9,1,'长期条目','周强',NULL,'北京市朝阳区望京街道广顺北大街33号院',NULL,NULL,NULL,NULL,NULL,NULL,0),(10,1,'专项记录','吴强',NULL,'北京市海淀区中关村大街27号',NULL,NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier_settlement`
--

DROP TABLE IF EXISTS `supplier_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier_settlement` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint DEFAULT NULL COMMENT '租户ID',
  `SUPPLIER_ID` bigint NOT NULL COMMENT '供应商ID',
  `PERIOD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '结算周期',
  `TOTAL_AMOUNT` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '总金额',
  `PAID_AMOUNT` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '已付金额',
  `STATUS` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `CREATE_USER` bigint DEFAULT NULL COMMENT '创建人ID',
  `UPDATE_USER` bigint DEFAULT NULL COMMENT '更新人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier_settlement`
--

LOCK TABLES `supplier_settlement` WRITE;
/*!40000 ALTER TABLE `supplier_settlement` DISABLE KEYS */;
INSERT INTO `supplier_settlement` VALUES (1,1,1,'常规方案',18.00,18.00,'PENDING',NULL,NULL,NULL,NULL,0),(2,1,2,'默认方案',25.00,25.00,'PENDING',NULL,NULL,NULL,NULL,0),(3,1,3,'补充方案',32.00,32.00,'PENDING',NULL,NULL,NULL,NULL,0),(4,1,1,'备用方案',39.00,39.00,'PENDING',NULL,NULL,NULL,NULL,0),(5,1,2,'扩展方案',46.00,46.00,'PENDING',NULL,NULL,NULL,NULL,0),(6,1,3,'标准方案',53.00,53.00,'PENDING',NULL,NULL,NULL,NULL,0),(7,1,1,'增值方案',60.00,60.00,'PENDING',NULL,NULL,NULL,NULL,0),(8,1,2,'临时方案',67.00,67.00,'PENDING',NULL,NULL,NULL,NULL,0),(9,1,3,'长期方案',74.00,74.00,'PENDING',NULL,NULL,NULL,NULL,0),(10,1,1,'专项方案',81.00,81.00,'PENDING',NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `supplier_settlement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_config`
--

DROP TABLE IF EXISTS `system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（NULL=全局配置）',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置值',
  `config_type` int NOT NULL DEFAULT '1' COMMENT '配置类型 1:功能开关 2:运营参数 3:显示设置 4:其他',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_user` bigint DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_config`
--

LOCK TABLES `system_config` WRITE;
/*!40000 ALTER TABLE `system_config` DISABLE KEYS */;
INSERT INTO `system_config` VALUES (1,1,'SC2607140151','常规条目',1,'用于演示环境的常规记录，可随时调整。','2026-09-30 15:53:51',NULL,NULL,NULL),(2,1,'SC2607190152','默认条目',1,'系统自动补齐的示例数据。','2026-09-30 15:53:51',NULL,NULL,NULL),(3,1,'SC2607240153','补充条目',1,'按业务流程录入的一条典型记录。','2026-09-30 15:53:51',NULL,NULL,NULL),(4,1,'SC2607290154','备用条目',1,'运营日常维护产生的记录。','2026-09-30 15:53:51',NULL,NULL,NULL),(5,1,'SC2608030155','扩展条目',1,'供联调与走查使用的样例内容。','2026-09-30 15:53:51',NULL,NULL,NULL),(6,1,'SC2608080156','标准条目',1,'用于演示环境的常规记录，可随时调整。','2026-09-30 15:53:51',NULL,NULL,NULL),(7,1,'SC2608130157','增值条目',1,'系统自动补齐的示例数据。','2026-09-30 15:53:51',NULL,NULL,NULL),(8,1,'SC2608180158','临时条目',1,'按业务流程录入的一条典型记录。','2026-09-30 15:53:51',NULL,NULL,NULL),(9,1,'SC2608230159','长期条目',1,'运营日常维护产生的记录。','2026-09-30 15:53:51',NULL,NULL,NULL),(10,1,'SC2608280160','专项条目',1,'供联调与走查使用的样例内容。','2026-09-30 15:53:51',NULL,NULL,NULL);
/*!40000 ALTER TABLE `system_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant`
--

DROP TABLE IF EXISTS `tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户名称',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `contact` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人',
  `logo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Logo图片相对路径',
  `license_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '营业执照图片相对路径',
  `package_name` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '套餐名称',
  `expire_time` datetime DEFAULT NULL COMMENT '套餐到期时间',
  `password_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'MD5' COMMENT '密码加密类型',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant`
--

LOCK TABLES `tenant` WRITE;
/*!40000 ALTER TABLE `tenant` DISABLE KEYS */;
INSERT INTO `tenant` VALUES (1,'瑞吉主门店','13800000000',NULL,'管理员',NULL,NULL,NULL,NULL,'MD5',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',1,1),(999,'自动化测试租户','13900000099',NULL,'测试',NULL,NULL,NULL,NULL,'MD5',1,'2026-09-30 15:53:49','2026-09-30 15:53:49',1,1),(1000,'瑞吉主门店',NULL,'北京市朝阳区望京街道广顺北大街33号院','测试',NULL,NULL,'常规项目',NULL,'MD5',1,NULL,NULL,NULL,NULL),(1001,'自动化测试租户',NULL,'北京市海淀区中关村大街27号','管理员',NULL,NULL,'默认项目',NULL,'MD5',1,NULL,NULL,NULL,NULL),(1002,'补充项目',NULL,'北京市东城区建国门内大街5号','测试',NULL,NULL,'补充项目',NULL,'MD5',1,NULL,NULL,NULL,NULL),(1003,'备用配置',NULL,'北京市西城区金融大街28号','管理员',NULL,NULL,'备用配置',NULL,'MD5',1,NULL,NULL,NULL,NULL),(1004,'扩展配置',NULL,'北京市丰台区南三环西路5号','测试',NULL,NULL,'扩展配置',NULL,'MD5',1,NULL,NULL,NULL,NULL),(1005,'标准配置',NULL,'北京市通州区新华大街16号','管理员',NULL,NULL,'标准配置',NULL,'MD5',1,NULL,NULL,NULL,NULL),(1006,'增值条目',NULL,'北京市石景山区鲁谷路12号','测试',NULL,NULL,'增值条目',NULL,'MD5',1,NULL,NULL,NULL,NULL),(1007,'临时条目',NULL,'北京市昌平区回龙观东大街18号','管理员',NULL,NULL,'临时条目',NULL,'MD5',1,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `tenant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `urgency_record`
--

DROP TABLE IF EXISTS `urgency_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `urgency_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `order_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `times` int NOT NULL DEFAULT '1',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SENT',
  `tenant_id` bigint NOT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `urgency_record`
--

LOCK TABLES `urgency_record` WRITE;
/*!40000 ALTER TABLE `urgency_record` DISABLE KEYS */;
INSERT INTO `urgency_record` VALUES (1,102,4,'UR2607140321',1,'SENT',1,'2026-07-14 00:53:36',NULL),(2,101,7,'UR2607190322',1,'SENT',1,'2026-07-19 02:06:36',NULL),(3,100,10,'UR2607240323',1,'SENT',1,'2026-07-24 03:19:36',NULL),(4,102,3,'UR2607290324',1,'SENT',1,'2026-07-29 04:32:36',NULL),(5,101,6,'UR2608030325',1,'SENT',1,'2026-08-03 05:45:36',NULL),(6,100,9,'UR2608080326',1,'SENT',1,'2026-08-08 05:58:36',NULL),(7,102,2,'UR2608130327',1,'SENT',1,'2026-08-13 07:11:36',NULL),(8,101,5,'UR2608180328',1,'SENT',1,'2026-08-18 08:24:36',NULL),(9,100,8,'UR2608230329',1,'SENT',1,'2026-08-23 09:37:36',NULL),(10,102,1,'UR2608280330',1,'SENT',1,'2026-08-28 10:50:36',NULL);
/*!40000 ALTER TABLE `urgency_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '姓名',
  `phone` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号',
  `sex` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '用户信息',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'张芳','13800000071',NULL,NULL,NULL,1,'2026-07-14 00:53:36',NULL,1),(2,'王芳','13900000072',NULL,NULL,NULL,1,'2026-07-19 02:06:36',NULL,1),(3,'李磊','13600000073',NULL,NULL,NULL,1,'2026-07-24 03:19:36',NULL,1),(4,'赵磊','13500000074',NULL,NULL,NULL,1,'2026-07-29 04:32:36',NULL,1),(5,'刘敏','18800000075',NULL,NULL,NULL,1,'2026-08-03 05:45:36',NULL,1),(6,'陈敏','17700000076',NULL,NULL,NULL,1,'2026-08-08 05:58:36',NULL,1),(7,'杨静','15000000077',NULL,NULL,NULL,1,'2026-08-13 07:11:36',NULL,1),(8,'黄静','16600000078',NULL,NULL,NULL,1,'2026-08-18 08:24:36',NULL,1),(9,'周强','13800000079',NULL,NULL,NULL,1,'2026-08-23 09:37:36',NULL,1),(10,'吴强','13900000080',NULL,NULL,NULL,1,'2026-08-28 10:50:36',NULL,1);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_browse_history`
--

DROP TABLE IF EXISTS `user_browse_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_browse_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `target_type` tinyint NOT NULL,
  `target_id` bigint NOT NULL,
  `target_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duration_seconds` int NOT NULL DEFAULT '0',
  `action_type` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_browse_history`
--

LOCK TABLES `user_browse_history` WRITE;
/*!40000 ALTER TABLE `user_browse_history` DISABLE KEYS */;
INSERT INTO `user_browse_history` VALUES (1,1,1,1,1,'张芳',0,1,'2026-07-14 00:53:36',0),(2,2,1,2,2,'王芳',0,1,'2026-07-19 02:06:36',0),(3,3,1,3,3,'李磊',0,1,'2026-07-24 03:19:36',0),(4,1,1,4,1,'赵磊',0,1,'2026-07-29 04:32:36',0),(5,2,1,1,2,'刘敏',0,1,'2026-08-03 05:45:36',0),(6,3,1,2,3,'陈敏',0,1,'2026-08-08 05:58:36',0),(7,1,1,3,1,'杨静',0,1,'2026-08-13 07:11:36',0),(8,2,1,4,2,'黄静',0,1,'2026-08-18 08:24:36',0),(9,3,1,1,3,'周强',0,1,'2026-08-23 09:37:36',0),(10,1,1,2,1,'吴强',0,1,'2026-08-28 10:50:36',0);
/*!40000 ALTER TABLE `user_browse_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_device`
--

DROP TABLE IF EXISTS `user_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_device` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `platform` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `app_version` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `push_enabled` tinyint NOT NULL DEFAULT '1',
  `last_active_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_device`
--

LOCK TABLES `user_device` WRITE;
/*!40000 ALTER TABLE `user_device` DISABLE KEYS */;
INSERT INTO `user_device` VALUES (1,1,1,'DEFAULT',NULL,NULL,1,NULL,'2026-07-14 00:53:36','2026-07-17 00:53:36',0,NULL,NULL),(2,1,2,'NORMAL',NULL,NULL,1,NULL,'2026-07-19 02:06:36','2026-07-23 02:06:36',0,NULL,NULL),(3,1,3,'SPECIAL',NULL,NULL,1,NULL,'2026-07-24 03:19:36','2026-07-29 03:19:36',0,NULL,NULL),(4,1,1,'TEMP',NULL,NULL,1,NULL,'2026-07-29 04:32:36','2026-08-04 04:32:36',0,NULL,NULL),(5,1,2,'CUSTOM',NULL,NULL,1,NULL,'2026-08-03 05:45:36','2026-08-10 05:45:36',0,NULL,NULL),(6,1,3,'BASIC',NULL,NULL,1,NULL,'2026-08-08 05:58:36','2026-08-16 05:58:36',0,NULL,NULL),(7,1,1,'DEFAULT',NULL,NULL,1,NULL,'2026-08-13 07:11:36','2026-08-22 07:11:36',0,NULL,NULL),(8,1,2,'NORMAL',NULL,NULL,1,NULL,'2026-08-18 08:24:36','2026-08-28 08:24:36',0,NULL,NULL),(9,1,3,'SPECIAL',NULL,NULL,1,NULL,'2026-08-23 09:37:36','2026-09-03 09:37:36',0,NULL,NULL),(10,1,1,'TEMP',NULL,NULL,1,NULL,'2026-08-28 10:50:36','2026-09-09 10:50:36',0,NULL,NULL);
/*!40000 ALTER TABLE `user_device` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_favorite`
--

DROP TABLE IF EXISTS `user_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_type` int NOT NULL COMMENT '类型 1菜品 2商家',
  `target_id` bigint NOT NULL COMMENT '收藏对象ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_favorite`
--

LOCK TABLES `user_favorite` WRITE;
/*!40000 ALTER TABLE `user_favorite` DISABLE KEYS */;
INSERT INTO `user_favorite` VALUES (1,1,1,1,1,'2026-07-14 00:53:36'),(2,2,2,2,1,'2026-07-19 02:06:36'),(3,3,3,3,1,'2026-07-24 03:19:36'),(4,1,4,1,1,'2026-07-29 04:32:36'),(5,2,1,2,1,'2026-08-03 05:45:36'),(6,3,2,3,1,'2026-08-08 05:58:36'),(7,1,3,1,1,'2026-08-13 07:11:36'),(8,2,4,2,1,'2026-08-18 08:24:36'),(9,3,1,3,1,'2026-08-23 09:37:36'),(10,1,2,1,1,'2026-08-28 10:50:36');
/*!40000 ALTER TABLE `user_favorite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_preference_tag`
--

DROP TABLE IF EXISTS `user_preference_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preference_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `tag_type` tinyint NOT NULL,
  `tag_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag_value` decimal(5,2) NOT NULL DEFAULT '1.00',
  `source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ORDER',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint NOT NULL,
  `update_user` bigint NOT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_preference_tag`
--

LOCK TABLES `user_preference_tag` WRITE;
/*!40000 ALTER TABLE `user_preference_tag` DISABLE KEYS */;
INSERT INTO `user_preference_tag` VALUES (1,1,1,1,'张芳',10.00,'ORDER','2026-07-14 00:53:36','2026-07-17 00:53:36',1,1,0),(2,2,1,2,'王芳',11.00,'ORDER','2026-07-19 02:06:36','2026-07-23 02:06:36',2,2,0),(3,3,1,3,'李磊',12.00,'ORDER','2026-07-24 03:19:36','2026-07-29 03:19:36',3,3,0),(4,1,1,4,'赵磊',13.00,'ORDER','2026-07-29 04:32:36','2026-08-04 04:32:36',1,1,0),(5,2,1,1,'刘敏',14.00,'ORDER','2026-08-03 05:45:36','2026-08-10 05:45:36',2,2,0),(6,3,1,2,'陈敏',15.00,'ORDER','2026-08-08 05:58:36','2026-08-16 05:58:36',3,3,0),(7,1,1,3,'杨静',16.00,'ORDER','2026-08-13 07:11:36','2026-08-22 07:11:36',1,1,0),(8,2,1,4,'黄静',17.00,'ORDER','2026-08-18 08:24:36','2026-08-28 08:24:36',2,2,0),(9,3,1,1,'周强',18.00,'ORDER','2026-08-23 09:37:36','2026-09-03 09:37:36',3,3,0),(10,1,1,2,'吴强',19.00,'ORDER','2026-08-28 10:50:36','2026-09-09 10:50:36',1,1,0);
/*!40000 ALTER TABLE `user_preference_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `withdrawal_application`
--

DROP TABLE IF EXISTS `withdrawal_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `withdrawal_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `application_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申编号',
  `applicant_id` bigint DEFAULT NULL COMMENT '申人ID',
  `applicant_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申人',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '提现金',
  `withdraw_method` int DEFAULT NULL COMMENT '提现方式 1-银 2-攻 3-徿',
  `receive_account` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收账号',
  `receive_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收人',
  `status` int DEFAULT '0' COMMENT '状 0-待 1-已 2-已付 3-已拒 4-已取',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核人',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `review_remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核备注',
  `payment_time` datetime DEFAULT NULL COMMENT '付时间',
  `payment_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '付编号',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `withdrawal_application`
--

LOCK TABLES `withdrawal_application` WRITE;
/*!40000 ALTER TABLE `withdrawal_application` DISABLE KEYS */;
INSERT INTO `withdrawal_application` VALUES (1,'WA2607140261',NULL,'常规项目',18.00,NULL,NULL,'常规项目',0,NULL,'常规项目',NULL,'用于演示环境的常规记录，可随时调整。',NULL,'WA2607140262','用于演示环境的常规记录，可随时调整。',1,NULL,NULL),(2,'WA2607190263',NULL,'默认项目',25.00,NULL,NULL,'默认项目',0,NULL,'默认项目',NULL,'系统自动补齐的示例数据。',NULL,'WA2607190264','系统自动补齐的示例数据。',1,NULL,NULL),(3,'WA2607240265',NULL,'补充项目',32.00,NULL,NULL,'补充项目',0,NULL,'补充项目',NULL,'按业务流程录入的一条典型记录。',NULL,'WA2607240266','按业务流程录入的一条典型记录。',1,NULL,NULL),(4,'WA2607290267',NULL,'备用配置',39.00,NULL,NULL,'备用配置',0,NULL,'备用配置',NULL,'运营日常维护产生的记录。',NULL,'WA2607290268','运营日常维护产生的记录。',1,NULL,NULL),(5,'WA2608030269',NULL,'扩展配置',46.00,NULL,NULL,'扩展配置',0,NULL,'扩展配置',NULL,'供联调与走查使用的样例内容。',NULL,'WA2608030270','供联调与走查使用的样例内容。',1,NULL,NULL),(6,'WA2608080271',NULL,'标准配置',53.00,NULL,NULL,'标准配置',0,NULL,'标准配置',NULL,'用于演示环境的常规记录，可随时调整。',NULL,'WA2608080272','用于演示环境的常规记录，可随时调整。',1,NULL,NULL),(7,'WA2608130273',NULL,'增值条目',60.00,NULL,NULL,'增值条目',0,NULL,'增值条目',NULL,'系统自动补齐的示例数据。',NULL,'WA2608130274','系统自动补齐的示例数据。',1,NULL,NULL),(8,'WA2608180275',NULL,'临时条目',67.00,NULL,NULL,'临时条目',0,NULL,'临时条目',NULL,'按业务流程录入的一条典型记录。',NULL,'WA2608180276','按业务流程录入的一条典型记录。',1,NULL,NULL),(9,'WA2608230277',NULL,'长期条目',74.00,NULL,NULL,'长期条目',0,NULL,'长期条目',NULL,'运营日常维护产生的记录。',NULL,'WA2608230278','运营日常维护产生的记录。',1,NULL,NULL),(10,'WA2608280279',NULL,'专项记录',81.00,NULL,NULL,'专项记录',0,NULL,'专项记录',NULL,'供联调与走查使用的样例内容。',NULL,'WA2608280280','供联调与走查使用的样例内容。',1,NULL,NULL);
/*!40000 ALTER TABLE `withdrawal_application` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `withdrawal_record`
--

DROP TABLE IF EXISTS `withdrawal_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `withdrawal_record` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `WITHDRAWAL_ID` bigint NOT NULL COMMENT '提现申请ID',
  `ACTUAL_AMOUNT` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '实际到账金额',
  `FEE` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '手续费',
  `TRANSFER_TIME` datetime DEFAULT NULL COMMENT '转账时间',
  `BANK_TRACE_NO` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '银行流水号',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `withdrawal_record`
--

LOCK TABLES `withdrawal_record` WRITE;
/*!40000 ALTER TABLE `withdrawal_record` DISABLE KEYS */;
INSERT INTO `withdrawal_record` VALUES (1,1,1,18.00,2.00,NULL,'WR2607140031',NULL),(2,1,2,25.00,2.50,NULL,'WR2607190032',NULL),(3,1,3,32.00,3.00,NULL,'WR2607240033',NULL),(4,1,1,39.00,3.50,NULL,'WR2607290034',NULL),(5,1,2,46.00,4.00,NULL,'WR2608030035',NULL),(6,1,3,53.00,4.50,NULL,'WR2608080036',NULL),(7,1,1,60.00,5.00,NULL,'WR2608130037',NULL),(8,1,2,67.00,5.50,NULL,'WR2608180038',NULL),(9,1,3,74.00,6.00,NULL,'WR2608230039',NULL),(10,1,1,81.00,6.50,NULL,'WR2608280040',NULL);
/*!40000 ALTER TABLE `withdrawal_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `withdrawal_request`
--

DROP TABLE IF EXISTS `withdrawal_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `withdrawal_request` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TENANT_ID` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `USER_ID` bigint NOT NULL COMMENT '用户ID',
  `AMOUNT` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '提现金额',
  `BANK_NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '银行名称',
  `ACCOUNT_NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '开户人姓名',
  `ACCOUNT_NUMBER` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '银行账号',
  `STATUS` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/TRANSFERRED',
  `REJECT_REASON` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '拒绝原因',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `APPROVE_TIME` datetime DEFAULT NULL COMMENT '审批时间',
  `APPROVE_USER_ID` bigint DEFAULT NULL COMMENT '审批人ID',
  `IS_DELETED` int NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `withdrawal_request`
--

LOCK TABLES `withdrawal_request` WRITE;
/*!40000 ALTER TABLE `withdrawal_request` DISABLE KEYS */;
INSERT INTO `withdrawal_request` VALUES (1,1,1,18.00,'常规项目','常规项目','','PENDING','',NULL,NULL,NULL,0),(2,1,2,25.00,'默认项目','默认项目','','PENDING','',NULL,NULL,NULL,0),(3,1,3,32.00,'补充项目','补充项目','','PENDING','',NULL,NULL,NULL,0),(4,1,1,39.00,'备用配置','备用配置','','PENDING','',NULL,NULL,NULL,0),(5,1,2,46.00,'扩展配置','扩展配置','','PENDING','',NULL,NULL,NULL,0),(6,1,3,53.00,'标准配置','标准配置','','PENDING','',NULL,NULL,NULL,0),(7,1,1,60.00,'增值条目','增值条目','','PENDING','',NULL,NULL,NULL,0),(8,1,2,67.00,'临时条目','临时条目','','PENDING','',NULL,NULL,NULL,0),(9,1,3,74.00,'长期条目','长期条目','','PENDING','',NULL,NULL,NULL,0),(10,1,1,81.00,'专项记录','专项记录','','PENDING','',NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `withdrawal_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `work_schedule`
--

DROP TABLE IF EXISTS `work_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `schedule_date` date NOT NULL,
  `shift` tinyint DEFAULT '3',
  `shift_start` time DEFAULT NULL,
  `shift_end` time DEFAULT NULL,
  `work_date_str` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_schedule`
--

LOCK TABLES `work_schedule` WRITE;
/*!40000 ALTER TABLE `work_schedule` DISABLE KEYS */;
INSERT INTO `work_schedule` VALUES (1,6,'常规项目','2026-07-14',3,NULL,NULL,NULL,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(2,9,'默认项目','2026-07-19',3,NULL,NULL,NULL,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(3,2,'补充项目','2026-07-24',3,NULL,NULL,NULL,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(4,5,'备用配置','2026-07-29',3,NULL,NULL,NULL,'运营日常维护产生的记录。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(5,8,'扩展配置','2026-08-03',3,NULL,NULL,NULL,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(6,1,'标准配置','2026-08-08',3,NULL,NULL,NULL,'用于演示环境的常规记录，可随时调整。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(7,4,'增值条目','2026-08-13',3,NULL,NULL,NULL,'系统自动补齐的示例数据。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(8,7,'临时条目','2026-08-18',3,NULL,NULL,NULL,'按业务流程录入的一条典型记录。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(9,10,'长期条目','2026-08-23',3,NULL,NULL,NULL,'运营日常维护产生的记录。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50'),(10,3,'专项记录','2026-08-28',3,NULL,NULL,NULL,'供联调与走查使用的样例内容。',1,'2026-09-30 15:53:50','2026-09-30 15:53:50');
/*!40000 ALTER TABLE `work_schedule` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-30 16:21:22
