-- ============================================================
-- P0-7 拼团防重最终防线（2026-10-02 资金/并发修复批次）
-- 手工执行脚本：无 Flyway，需 DBA 在开发库 reggie（及测试库如有需要）手动执行。
-- 执行前请先清理存量重复参与记录，否则 ALTER 会因重复键失败：
--   SELECT group_buy_id, user_id, COUNT(*) c FROM group_buy_participation
--    WHERE status IN ('JOINED','PAID') GROUP BY group_buy_id, user_id HAVING c > 1;
-- 注意：加上唯一索引后，同一用户对同一活动永久只能存在一条参与记录
--（含历史 CANCELLED 记录；当前服务链路不会产生 CANCELLED，如未来引入
-- "取消后允许重新参团"需把状态纳入索引列或改为条件防重）。
-- ============================================================

ALTER TABLE `group_buy_participation`
    ADD UNIQUE INDEX `uk_groupbuy_tenant_user` (`group_buy_id`, `user_id`);
