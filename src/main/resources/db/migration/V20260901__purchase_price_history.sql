-- ============================================
-- V20260901__purchase_price_history.sql
-- 采购与价格历史表：采购单明细已存在，新增价格历史表
-- ============================================

-- 价格历史记录表
CREATE TABLE IF NOT EXISTS price_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    old_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '旧价格',
    new_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '新价格',
    change_reason VARCHAR(255) DEFAULT '' COMMENT '变动原因',
    operator_id BIGINT NOT NULL DEFAULT 0 COMMENT '操作人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_material_id (material_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格历史记录表';
