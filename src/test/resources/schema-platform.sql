-- 清理平台订单残留数据（之前测试运行插入的订单），避免 @DirtiesContext 重启后数据残留导致去重误判
DELETE FROM order_detail WHERE order_id IN (SELECT id FROM orders WHERE platform_type IS NOT NULL);
DELETE FROM orders WHERE platform_type IS NOT NULL;

-- 外卖平台接入配置 测试库建表（H2 / MySQL 兼容）
DROP TABLE IF EXISTS platform_config;
CREATE TABLE platform_config (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  platform_type varchar(32) NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  platform_name varchar(128) NULL DEFAULT NULL COMMENT '平台展示名称',
  shop_id varchar(128) NULL DEFAULT NULL COMMENT '平台侧门店ID',
  app_key varchar(512) NULL DEFAULT NULL COMMENT '应用标识(加密)',
  app_secret varchar(512) NULL DEFAULT NULL COMMENT '应用密钥(加密)',
  access_token varchar(512) NULL DEFAULT NULL COMMENT '访问令牌(加密)',
  enabled int NOT NULL DEFAULT 1 COMMENT '是否启用 0停用 1启用',
  sync_scope int NOT NULL DEFAULT 1 COMMENT '同步范围位标记 1订单2商品4库存8营业状态',
  remark varchar(500) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_platform_type_shop ON platform_config(platform_type, shop_id);
CREATE INDEX idx_platform_tenant ON platform_config(tenant_id);

-- ==================== 商品平台映射表 ====================
DROP TABLE IF EXISTS dish_platform_mapping;
CREATE TABLE dish_platform_mapping (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  dish_id bigint NOT NULL COMMENT '本系统菜品ID',
  platform_type varchar(32) NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  platform_shop_id varchar(128) NULL DEFAULT NULL COMMENT '平台侧门店ID',
  platform_dish_id varchar(128) NULL DEFAULT NULL COMMENT '平台菜品ID',
  platform_sku_id varchar(128) NULL DEFAULT NULL COMMENT '平台SKU ID',
  price decimal(10,2) NULL DEFAULT NULL COMMENT '平台价格',
  status int NOT NULL DEFAULT 1 COMMENT '状态 0下架 1上架',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE INDEX idx_mapping_dish_platform (dish_id, platform_type, platform_dish_id)
);

-- ==================== 平台同步操作日志表 ====================
DROP TABLE IF EXISTS platform_sync_log;
CREATE TABLE platform_sync_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  platform_type varchar(32) NOT NULL COMMENT '平台类型',
  platform_order_id varchar(128) NULL DEFAULT NULL COMMENT '平台订单号',
  local_order_id bigint NULL DEFAULT NULL COMMENT '本地订单ID',
  action varchar(32) NOT NULL COMMENT '动作 PULL/ACCEPT/REJECT等',
  direction varchar(16) NOT NULL DEFAULT 'IN' COMMENT '方向 IN=拉单 OUT=回传',
  request_body text NULL COMMENT '请求内容',
  response_body text NULL COMMENT '响应内容',
  status int NOT NULL DEFAULT 0 COMMENT '结果 0=成功 1=失败',
  error_message varchar(512) NULL DEFAULT NULL COMMENT '错误信息',
  retry_count int NOT NULL DEFAULT 0 COMMENT '重试次数',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_sync_log_platform_order ON platform_sync_log(platform_type, platform_order_id);
CREATE INDEX idx_sync_log_local_order ON platform_sync_log(local_order_id);
CREATE INDEX idx_sync_log_create_time ON platform_sync_log(create_time);

-- ==================== 平台对账任务表 ====================
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
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_reconcile_task_date_platform ON platform_reconcile_task(reconcile_date, platform_type, tenant_id);
