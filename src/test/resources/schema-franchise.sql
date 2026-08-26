-- 加盟分账模块测试数据（MySQL compatible）
-- 只清理数据，不创建表（生产 schema 已存在）

DELETE FROM franchise_settlement WHERE tenant_id = 1 OR store_tenant_id = 2;
DELETE FROM franchise_contract WHERE tenant_id = 1;
DELETE FROM franchisee WHERE tenant_id = 1;