-- ============================================================
-- P0-12 日结/拼团 并发兜底唯一索引（2026-10-02 资金/并发修复批次）
-- 手工执行脚本：无 Flyway，需 DBA 在开发库 reggie 手动执行（测试库如需同步
-- 请一并更新 src/test/resources/schema.sql 对应建表语句）。
-- 执行前请先清理存量重复行，否则 ALTER 会因重复键失败：
--   SELECT tenant_id, settlement_date, COUNT(*) c FROM daily_settlement
--    GROUP BY tenant_id, settlement_date HAVING c > 1;
-- 说明：daily_settlement.tenant_id 允许 NULL，MySQL 唯一索引不约束 NULL 组合，
-- 多租户链路（收银日结均带 tenantId）下有效；NULL 租户的全局日结仍靠应用层锁。
-- ============================================================

ALTER TABLE `daily_settlement`
    ADD UNIQUE INDEX `uk_daily_settlement_tenant_date` (`tenant_id`, `settlement_date`);
