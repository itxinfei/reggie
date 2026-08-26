-- Printer module test data (MySQL compatible)
-- 只清理并插入数据，不创建表（生产 schema 已存在）

DELETE FROM printer_log WHERE tenant_id = 1;
DELETE FROM printer_template WHERE 1=1;
DELETE FROM printer_config WHERE tenant_id = 1;