-- ============================================================
-- 电子发票模块：invoice_record 增加 user_id（申请用户ID）列
-- 用途：C 端「我的发票」按当前登录用户归属查询，防跨用户越权
-- 手动执行：mysql -uroot -p reggie < V20260905__invoice_user_id.sql
-- ============================================================

ALTER TABLE invoice_record
  ADD COLUMN user_id bigint NULL DEFAULT NULL COMMENT '申请用户ID（用户端归属列，防止跨用户越权查询）' AFTER order_id;

ALTER TABLE invoice_record
  ADD KEY idx_record_user (tenant_id, user_id);
