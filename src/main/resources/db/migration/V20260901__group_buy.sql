-- ============================================
-- V20260901__group_buy.sql
-- 拼团营销表：活动表 + 参与记录表
-- ============================================

-- 拼团活动表
CREATE TABLE IF NOT EXISTS group_buy_campaign (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    name VARCHAR(100) NOT NULL COMMENT '活动名称',
    description VARCHAR(500) DEFAULT '' COMMENT '活动描述',
    group_id BIGINT NOT NULL DEFAULT 0 COMMENT '拼团组ID',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/CLOSED/ENDED',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    min_members INT NOT NULL DEFAULT 2 COMMENT '最少成团人数',
    max_members INT NOT NULL DEFAULT 10 COMMENT '最多成团人数',
    original_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '原价',
    group_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '拼团价',
    dish_id BIGINT NOT NULL DEFAULT 0 COMMENT '菜品ID',
    dish_name VARCHAR(100) DEFAULT '' COMMENT '菜品名称',
    image VARCHAR(255) DEFAULT '' COMMENT '活动图片URL',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_status (status),
    INDEX idx_group_id (group_id),
    INDEX idx_dish_id (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团活动表';

-- 拼团参与记录表
CREATE TABLE IF NOT EXISTS group_buy_participation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    group_buy_id BIGINT NOT NULL COMMENT '拼团活动ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'JOINED' COMMENT '状态：JOINED/PAID/CANCELLED',
    join_time DATETIME NOT NULL COMMENT '参团时间',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_group_buy_id (group_buy_id),
    INDEX idx_user_id (user_id),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团参与记录表';
