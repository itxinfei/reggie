-- 会员挽留会员表
-- 供会员挽留自动化模块统计等级概览、积分排行、流失预警与智能推荐
-- 此前 Mapper XML 以 WHERE 1=0 假空查询占位，但 MySQL 解析阶段即因表不存在报 500，现补齐真表

CREATE TABLE IF NOT EXISTS retention_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    member_name VARCHAR(50) NOT NULL COMMENT '会员姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    level VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '等级：GOLD/SILVER/NORMAL',
    points INT NOT NULL DEFAULT 0 COMMENT '积分',
    last_order_date DATE DEFAULT NULL COMMENT '最近下单日期',
    total_orders INT NOT NULL DEFAULT 0 COMMENT '累计下单数',
    total_spent DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE=活跃, DORMANT=沉睡, CHURNED=流失',
    tag VARCHAR(50) DEFAULT NULL COMMENT 'ǩ磺߼ֵ۸У',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_status (status),
    INDEX idx_last_order_date (last_order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员挽留会员表';
