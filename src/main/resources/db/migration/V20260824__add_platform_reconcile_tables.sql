-- 平台同步操作日志表（对账与异常追踪）
DROP TABLE IF EXISTS platform_sync_log;
CREATE TABLE platform_sync_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  platform_type varchar(32) NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  platform_order_id varchar(128) NULL DEFAULT NULL COMMENT '平台订单号',
  local_order_id bigint NULL DEFAULT NULL COMMENT '本地订单ID',
  action varchar(32) NOT NULL COMMENT '动作 PULL/ACCEPT/REJECT/PREPARE/COMPLETE/CANCEL/DISH_ON/DISH_OFF/STOCK/BUSINESS/HEALTH',
  direction varchar(16) NOT NULL DEFAULT 'IN' COMMENT '方向 IN=拉单 OUT=回传',
  request_body text NULL COMMENT '请求内容（脱敏）',
  response_body text NULL COMMENT '响应内容',
  status int NOT NULL DEFAULT 0 COMMENT '结果 0=成功 1=失败',
  error_message varchar(512) NULL DEFAULT NULL COMMENT '错误信息',
  retry_count int NOT NULL DEFAULT 0 COMMENT '重试次数',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  INDEX idx_sync_log_platform_order (platform_type, platform_order_id),
  INDEX idx_sync_log_local_order (local_order_id),
  INDEX idx_sync_log_create_time (create_time)
);

-- 平台对账任务表
DROP TABLE IF EXISTS platform_reconcile_task;
CREATE TABLE platform_reconcile_task (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  platform_type varchar(32) NOT NULL COMMENT '平台类型',
  reconcile_date date NOT NULL COMMENT '对账日期',
  begin_time datetime NOT NULL COMMENT '对账开始时间',
  end_time datetime NOT NULL COMMENT '对账结束时间',
  total_platform_count int NOT NULL DEFAULT 0 COMMENT '平台侧订单数',
  total_local_count int NOT NULL DEFAULT 0 COMMENT '本地订单数',
  match_count int NOT NULL DEFAULT 0 COMMENT '匹配成功数',
  missing_local_count int NOT NULL DEFAULT 0 COMMENT '平台有本地无',
  missing_platform_count int NOT NULL DEFAULT 0 COMMENT '本地有平台无',
  status int NOT NULL DEFAULT 0 COMMENT '状态 0=进行中 1=完成 2=失败',
  error_message varchar(512) NULL DEFAULT NULL COMMENT '错误信息',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE INDEX idx_reconcile_task_date_platform (reconcile_date, platform_type, tenant_id)
);
