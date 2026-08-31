-- Printer module test schema (MySQL compatible)
-- 打印终端 / 打印任务：代理端表，测试库无生产表时自建
CREATE TABLE IF NOT EXISTS print_terminal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    store_code VARCHAR(50) NOT NULL DEFAULT '',
    terminal_code VARCHAR(64) NOT NULL DEFAULT '',
    token VARCHAR(64) NOT NULL DEFAULT '',
    name VARCHAR(100) NOT NULL DEFAULT '',
    printer_name VARCHAR(200) NOT NULL DEFAULT '',
    paper_size VARCHAR(20) NOT NULL DEFAULT '80mm',
    print_types VARCHAR(50) NOT NULL DEFAULT 'BILL',
    status TINYINT NOT NULL DEFAULT 0,
    last_heartbeat DATETIME DEFAULT NULL,
    client_version VARCHAR(30) NOT NULL DEFAULT '',
    created_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_print_terminal_code (terminal_code),
    KEY idx_print_terminal_tenant (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS print_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    store_code VARCHAR(50) NOT NULL DEFAULT '',
    order_id BIGINT DEFAULT NULL,
    task_type VARCHAR(20) NOT NULL DEFAULT 'BILL',
    content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    terminal_id BIGINT DEFAULT NULL,
    terminal_code VARCHAR(64) NOT NULL DEFAULT '',
    error_msg VARCHAR(500) NOT NULL DEFAULT '',
    retry_count INT NOT NULL DEFAULT 0,
    created_time DATETIME DEFAULT NULL,
    pulled_time DATETIME DEFAULT NULL,
    done_time DATETIME DEFAULT NULL,
    KEY idx_print_task_status (status),
    KEY idx_print_task_order (order_id),
    KEY idx_print_task_terminal (terminal_id, status)
);

-- 清理并插入数据（生产 schema 已存在）
DELETE FROM print_task WHERE tenant_id = 1;
DELETE FROM print_terminal WHERE tenant_id = 1;
DELETE FROM printer_log WHERE tenant_id = 1;
DELETE FROM printer_template WHERE 1=1;
DELETE FROM printer_config WHERE tenant_id = 1;
