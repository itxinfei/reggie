-- ============================================
-- 智能推荐模块 - 数据库迁移脚本
-- 模块：用户偏好分析、浏览记录、推荐缓存、营销活动
-- 日期：2026-07-09
-- ============================================

-- --------------------------------------------
-- 1. 用户偏好标签表
-- 基于用户历史订单分析得出的口味/品类偏好
-- --------------------------------------------
DROP TABLE IF EXISTS `user_preference_tag`;
CREATE TABLE `user_preference_tag` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
    `tag_type` tinyint NOT NULL COMMENT '标签类型 1:口味偏好 2:品类偏好 3:价格偏好 4:时段偏好',
    `tag_name` varchar(64) NOT NULL COMMENT '标签名称，如：辣味、川菜、20-30元、午餐时段',
    `tag_value` decimal(5,2) NOT NULL DEFAULT 1.00 COMMENT '偏好权重 0.00~1.00，越高越偏好',
    `source` varchar(20) NOT NULL DEFAULT 'ORDER' COMMENT '数据来源 ORDER:订单分析 BROWSE:浏览分析 MANUAL:手动标注',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime NOT NULL COMMENT '更新时间',
    `create_user` bigint NOT NULL COMMENT '创建人',
    `update_user` bigint NOT NULL COMMENT '修改人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_tag`(`user_id` ASC, `tag_type` ASC) USING BTREE,
    INDEX `idx_tenant`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户偏好标签' ROW_FORMAT = Dynamic;

-- --------------------------------------------
-- 2. 用户浏览记录表
-- 记录用户在前端浏览菜品的详细行为
-- --------------------------------------------
DROP TABLE IF EXISTS `user_browse_history`;
CREATE TABLE `user_browse_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
    `target_type` tinyint NOT NULL COMMENT '浏览对象类型 1:菜品 2:套餐',
    `target_id` bigint NOT NULL COMMENT '浏览对象ID',
    `target_name` varchar(128) DEFAULT NULL COMMENT '浏览对象名称',
    `duration_seconds` int NOT NULL DEFAULT 0 COMMENT '浏览停留时长(秒)',
    `action_type` tinyint NOT NULL DEFAULT 1 COMMENT '行为类型 1:浏览 2:收藏 3:加购 4:分享',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_time`(`user_id` ASC, `create_time` DESC) USING BTREE,
    INDEX `idx_tenant`(`tenant_id` ASC) USING BTREE,
    INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户浏览记录' ROW_FORMAT = Dynamic;

-- --------------------------------------------
-- 3. 推荐结果缓存表
-- 缓存用户个性化推荐结果，避免实时计算开销
-- --------------------------------------------
DROP TABLE IF EXISTS `recommendation_cache`;
CREATE TABLE `recommendation_cache` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
    `recommend_type` tinyint NOT NULL COMMENT '推荐类型 1:菜品推荐 2:套餐推荐 3:新品尝鲜',
    `dish_ids` text NOT NULL COMMENT '推荐菜品/套餐ID列表，JSON数组格式',
    `algorithm` varchar(32) NOT NULL COMMENT '算法名称，如：CF/ContentBased/Hybrid/HotRank',
    `score` decimal(3,2) NOT NULL DEFAULT 0.00 COMMENT '推荐置信度 0.00~1.00',
    `expire_time` datetime NOT NULL COMMENT '缓存过期时间',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_recommend`(`user_id` ASC, `recommend_type` ASC) USING BTREE,
    INDEX `idx_tenant`(`tenant_id` ASC) USING BTREE,
    INDEX `idx_expire`(`expire_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推荐结果缓存' ROW_FORMAT = Dynamic;

-- --------------------------------------------
-- 4. 营销活动表
-- 定义各种促销活动、满减、折扣、限时优惠等
-- --------------------------------------------
DROP TABLE IF EXISTS `marketing_campaign`;
CREATE TABLE `marketing_campaign` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `name` varchar(100) NOT NULL COMMENT '活动名称',
    `description` varchar(500) DEFAULT NULL COMMENT '活动描述',
    `campaign_type` tinyint NOT NULL COMMENT '活动类型 1:满减 2:折扣 3:赠品 4:首单优惠 5:会员专享 6:限时秒杀',
    `target_type` tinyint NOT NULL DEFAULT 1 COMMENT '目标类型 1:全部用户 2:新用户 3:高频用户 4:流失预警用户 5:指定等级',
    `target_value` varchar(500) DEFAULT NULL COMMENT '目标值(等级ID列表/用户ID列表，JSON)',
    `rule_json` text COMMENT '活动规则 JSON，如满减条件、折扣率等',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态 0:草稿 1:进行中 2:已结束 3:已暂停',
    `priority` int NOT NULL DEFAULT 0 COMMENT '优先级，数值越大优先级越高',
    `start_time` datetime NOT NULL COMMENT '活动开始时间',
    `end_time` datetime NOT NULL COMMENT '活动结束时间',
    `max_participants` int DEFAULT NULL COMMENT '最大参与人数，NULL表示不限',
    `current_participants` int NOT NULL DEFAULT 0 COMMENT '当前参与人数',
    `coupon_template_id` bigint DEFAULT NULL COMMENT '关联优惠券模板ID',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime NOT NULL COMMENT '更新时间',
    `create_user` bigint NOT NULL COMMENT '创建人',
    `update_user` bigint NOT NULL COMMENT '修改人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE,
    INDEX `idx_time`(`start_time` ASC, `end_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '营销活动' ROW_FORMAT = Dynamic;

-- --------------------------------------------
-- 5. 营销消息推送记录表
-- 记录每次营销消息的推送详情
-- --------------------------------------------
DROP TABLE IF EXISTS `marketing_message`;
CREATE TABLE `marketing_message` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `campaign_id` bigint NOT NULL COMMENT '关联营销活动ID',
    `user_id` bigint NOT NULL COMMENT '推送用户ID',
    `push_type` tinyint NOT NULL COMMENT '推送类型 1:首页弹窗 2:消息通知 3:短信 4:优惠券自动发放',
    `title` varchar(200) NOT NULL COMMENT '推送标题',
    `content` varchar(1000) NOT NULL COMMENT '推送内容',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态 0:待推送 1:已推送 2:已读 3:已使用',
    `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
    `use_time` datetime DEFAULT NULL COMMENT '使用时间',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user`(`user_id` ASC) USING BTREE,
    INDEX `idx_campaign`(`campaign_id` ASC) USING BTREE,
    INDEX `idx_tenant`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '营销消息推送记录' ROW_FORMAT = Dynamic;

-- --------------------------------------------
-- 6. 推荐反馈表
-- 记录用户对推荐结果的反馈，用于优化算法
-- --------------------------------------------
DROP TABLE IF EXISTS `recommendation_feedback`;
CREATE TABLE `recommendation_feedback` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `tenant_id` bigint NULL DEFAULT NULL COMMENT '租户ID',
    `recommend_cache_id` bigint DEFAULT NULL COMMENT '关联推荐缓存ID',
    `dish_id` bigint NOT NULL COMMENT '菜品/套餐ID',
    `feedback_type` tinyint NOT NULL COMMENT '反馈类型 1:点击 2:收藏 3:加购 4:下单 5:不感兴趣',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user`(`user_id` ASC) USING BTREE,
    INDEX `idx_dish`(`dish_id` ASC) USING BTREE,
    INDEX `idx_tenant`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推荐反馈' ROW_FORMAT = Dynamic;
