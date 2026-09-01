-- ============================================
-- V20260901__withdraw.sql
-- 提现审批流表：提现申请 + 提现记录
-- ============================================

-- 提现申请表
CREATE TABLE IF NOT EXISTS withdrawal_request (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '提现金额',
    bank_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '银行名称',
    account_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '开户人姓名',
    account_number VARCHAR(100) NOT NULL DEFAULT '' COMMENT '银行账号',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
    reject_reason VARCHAR(255) DEFAULT '' COMMENT '拒绝原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    approve_time DATETIME DEFAULT NULL COMMENT '审批时间',
    approve_user_id BIGINT DEFAULT NULL COMMENT '审批人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';

-- 提现记录表
CREATE TABLE IF NOT EXISTS withdrawal_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    withdrawal_id BIGINT NOT NULL COMMENT '提现申请ID',
    actual_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实际到账金额',
    fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '手续费',
    transfer_time DATETIME NOT NULL COMMENT '转账时间',
    bank_trace_no VARCHAR(100) NOT NULL DEFAULT '' COMMENT '银行流水号',
    PRIMARY KEY (id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_withdrawal_id (withdrawal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现记录表';
