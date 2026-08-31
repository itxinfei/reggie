-- 打印代理接口测试数据（使用独立租户 9999，避免与现有 tenant_id=1 的 uk_tenant 唯一键冲突）
DELETE FROM print_task;
DELETE FROM print_terminal;
DELETE FROM store_info WHERE store_code = 'S0001' OR tenant_id = 9999;
INSERT INTO store_info (id, tenant_id, store_code, store_type, create_time, update_time,
                        create_user, update_user, is_deleted)
VALUES (9999, 9999, 'S0001', 1, NOW(), NOW(), 1, 1, 0);
