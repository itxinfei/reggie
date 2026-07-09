-- ============================================================
-- 消息通知模块迁移脚本
-- 功能：短信通知、APP推送、通知模板管理
-- 创建时间：2026-07-09
-- ============================================================

-- 1. 通知模板表
CREATE TABLE IF NOT EXISTS `notification_template` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` BIGINT NULL COMMENT '租户ID',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_code` VARCHAR(64) NULL COMMENT '外部模板编码(如阿里云SMS模板CODE)',
    `channel` TINYINT NOT NULL DEFAULT 1 COMMENT '通知渠道: 1=短信 2=APP推送 3=短信+APP推送',
    `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型: ORDER_NOTICE/PROMOTION/VERIFY_CODE/SYSTEM/JOB_NOTICE',
    `title` VARCHAR(200) NULL COMMENT '推送标题(APP推送用)',
    `content` TEXT NOT NULL COMMENT '模板内容，支持占位符 ${param}',
    `param_list` VARCHAR(500) NULL COMMENT '参数列表JSON，如 ["orderNo","amount"]',
    `sign_name` VARCHAR(50) NULL COMMENT '短信签名',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=启用 0=停用',
    `remark` VARCHAR(200) NULL COMMENT '备注说明',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `create_user` BIGINT NULL COMMENT '创建人',
    `update_user` BIGINT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_biz` (`tenant_id`, `biz_type`),
    KEY `idx_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板表';

-- 2. 通知记录表
CREATE TABLE IF NOT EXISTS `notification_record` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` BIGINT NULL COMMENT '租户ID',
    `template_id` BIGINT NULL COMMENT '模板ID',
    `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型',
    `channel` TINYINT NOT NULL COMMENT '发送渠道: 1=短信 2=APP推送',
    `target_type` TINYINT NOT NULL DEFAULT 1 COMMENT '目标类型: 1=单个用户 2=用户分组 3=全部用户',
    `target_value` TEXT NOT NULL COMMENT '目标值(手机号/用户ID列表/分群ID，JSON数组)',
    `target_count` INT NOT NULL DEFAULT 0 COMMENT '目标数量',
    `content` TEXT NOT NULL COMMENT '实际发送内容',
    `send_time` DATETIME NULL COMMENT '定时发送时间，NULL表示立即发送',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=待发送 1=发送中 2=成功 3=失败 4=部分成功',
    `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数',
    `fail_reason` TEXT NULL COMMENT '失败原因汇总',
    `ext_data` VARCHAR(500) NULL COMMENT '扩展数据JSON',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `create_user` BIGINT NULL COMMENT '创建人',
    `update_user` BIGINT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_biz_type` (`biz_type`),
    KEY `idx_send_time` (`send_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知发送记录表';

-- 3. 用户设备表 (APP推送需要设备Token)
CREATE TABLE IF NOT EXISTS `user_device` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `platform` VARCHAR(20) NOT NULL COMMENT '平台: ANDROID/IOS/H5',
    `device_token` VARCHAR(255) NULL COMMENT '设备推送Token',
    `app_version` VARCHAR(20) NULL COMMENT 'APP版本号',
    `push_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否开启推送: 1=是 0=否',
    `last_active_time` DATETIME NULL COMMENT '最后活跃时间',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_platform` (`user_id`, `platform`),
    KEY `idx_device_token` (`device_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设备表';

-- 4. 插入默认通知模板数据
INSERT INTO `notification_template` (`id`, `tenant_id`, `template_name`, `template_code`, `channel`, `biz_type`, `title`, `content`, `param_list`, `sign_name`, `status`, `remark`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES
(1, NULL, '订单确认通知', 'SMS_ORDER_CONFIRM', 1, 'ORDER_NOTICE', NULL, '尊敬的${userName}，您的订单${orderNo}已确认，预计${deliveryTime}送达，如有问题请联系商家。', '["userName","orderNo","deliveryTime"]', '瑞吉外卖', 1, '下单成功后通知用户', NOW(), NOW(), 1, 1, 0),
(2, NULL, '订单完成通知', 'SMS_ORDER_DONE', 1, 'ORDER_NOTICE', NULL, '${userName}您好，订单${orderNo}已送达，请及时取餐。如有任何问题请随时联系我们。', '["userName","orderNo"]', '瑞吉外卖', 1, '订单完成后通知用户', NOW(), NOW(), 1, 1, 0),
(3, NULL, '验证码短信', 'SMS_VERIFY_CODE', 1, 'VERIFY_CODE', NULL, '验证码：${code}，您正在登录瑞吉外卖，请在${expireMin}分钟内完成验证。请勿泄露给他人。', '["code","expireMin"]', '瑞吉外卖', 1, '登录/注册验证码', NOW(), NOW(), 1, 1, 0),
(4, NULL, '新优惠券通知', 'SMS_COUPON_NOTICE', 2, 'PROMOTION', '您有一张新优惠券', '亲爱的${userName}，您获得了一张${couponName}（满${minAmount}减${discountAmount}），有效期至${expireDate}，快去使用吧！', '["userName","couponName","minAmount","discountAmount","expireDate"]', NULL, 1, '发券后APP推送', NOW(), NOW(), 1, 1, 0),
(5, NULL, '促销活动推送', 'APP_PROMOTION', 2, 'PROMOTION', '${title}', '${content}', '["title","content"]', NULL, 1, '营销活动APP推送', NOW(), NOW(), 1, 1, 0),
(6, NULL, '新品尝鲜推送', 'APP_NEW_DISH', 2, 'PROMOTION', '尝鲜推荐：${dishName}', '${dishName} 新品上市！${dishDesc}，现在下单享新品特惠，快来尝尝吧~', '["dishName","dishDesc"]', NULL, 1, '新菜品上线通知', NOW(), NOW(), 1, 1, 0);
