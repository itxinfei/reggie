-- 催单记录表
-- 用于持久化催单操作记录，支持频率控制和统计分析

CREATE TABLE IF NOT EXISTS urgency_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    member_id BIGINT NOT NULL COMMENT '会员ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    times INT NOT NULL DEFAULT 1 COMMENT '催单次数',
    status VARCHAR(16) NOT NULL DEFAULT 'SENT' COMMENT '状态：SENT=已发送, PROCESSED=已处理, IGNORED=已忽略',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    INDEX idx_member_id (member_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催单记录表';
