-- ============================================
-- V20260901__supplier.sql
-- 供应商对账/结算单扩展表
-- 注意：supplier 表已存在，此处仅新增结算单表
-- ============================================

-- 供应商结算单表
CREATE TABLE IF NOT EXISTS supplier_settlement (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    period VARCHAR(20) NOT NULL COMMENT '结算周期，如 202609',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PAID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_period (period),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商结算单表';
