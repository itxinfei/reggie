-- P2-2 骑手记住登录令牌表
-- 每个骑手一条持久令牌；登录勾选"记住我"时颁发，30 天有效；
-- LoginCheckFilter 在骑手会话缺失时凭 rider_remember cookie 自动重建会话。
CREATE TABLE IF NOT EXISTS `rider_remember_token` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rider_id` bigint NOT NULL COMMENT '骑手ID',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `token` varchar(64) NOT NULL COMMENT '记住登录随机令牌',
    `expire_time` datetime NOT NULL COMMENT '过期时间',
    `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_token` (`token`) USING BTREE,
    KEY `idx_rider` (`rider_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='骑手记住登录令牌';
