-- ============================================================
-- 电子发票模块：invoice_title 增加 user_id（归属用户ID）列
-- 用途：发票抬头按当前登录用户归属查询/编辑/删除，修复原仅按租户过滤
--       导致全店企业名称、税号对所有C端用户可见的信息泄露
-- 手动执行：mysql -uroot -p reggie < V20260923__invoice_title_user_id.sql
-- 说明：历史抬头 user_id 保持 NULL，应用层不再对任何用户返回（含敏感税号，
--       不做批量归属，避免把他人抬头错误判给用户）；新抬头由 saveTitle 自动写入归属。
-- ============================================================

ALTER TABLE invoice_title
  ADD COLUMN user_id bigint NULL DEFAULT NULL COMMENT '归属用户ID（用户端隔离，防止跨用户越权）' AFTER tenant_id;

ALTER TABLE invoice_title
  ADD KEY idx_title_user (tenant_id, user_id);
