-- SaaS 订阅计费模块测试 schema（H2 / MySQL 兼容）
-- H2 内存库为空库，测试脚本须自建模块表。

-- ==================== SaaS 套餐方案（平台级全局表，无 tenant_id）====================
CREATE TABLE IF NOT EXISTS billing_plan (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  plan_code varchar(50) NOT NULL COMMENT '套餐编码',
  plan_name varchar(100) NOT NULL COMMENT '套餐名称',
  price decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '月付价格',
  annual_price decimal(12,2) DEFAULT NULL COMMENT '年付价格',
  max_stores int NOT NULL DEFAULT -1 COMMENT '最大门店数，-1不限',
  max_employees int NOT NULL DEFAULT -1 COMMENT '最大员工数，-1不限',
  features varchar(500) DEFAULT NULL COMMENT '功能权益，逗号分隔',
  sort_order int NOT NULL DEFAULT 0 COMMENT '排序',
  status tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=下架，1=上架',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  create_user bigint DEFAULT NULL COMMENT '创建人',
  update_user bigint DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_plan_code ON billing_plan(plan_code);
CREATE INDEX IF NOT EXISTS idx_billing_plan_status ON billing_plan(status);

-- ==================== 租户订阅记录（带 tenant_id，受多租户隔离）====================
CREATE TABLE IF NOT EXISTS tenant_subscription (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '订阅记录ID',
  tenant_id bigint DEFAULT NULL COMMENT '租户ID',
  plan_id bigint DEFAULT NULL COMMENT '套餐ID',
  plan_code varchar(50) DEFAULT NULL COMMENT '套餐编码快照',
  plan_name varchar(100) DEFAULT NULL COMMENT '套餐名称快照',
  order_no varchar(50) NOT NULL COMMENT '订阅单号',
  billing_cycle tinyint NOT NULL DEFAULT 1 COMMENT '计费周期：1月付，2年付',
  amount decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  start_time datetime DEFAULT NULL COMMENT '生效开始时间',
  end_time datetime DEFAULT NULL COMMENT '到期时间',
  status tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待支付，1生效中，2已过期，3已取消',
  pay_time datetime DEFAULT NULL COMMENT '支付时间',
  pay_channel varchar(20) DEFAULT NULL COMMENT '支付渠道',
  max_stores int DEFAULT NULL COMMENT '门店数上限快照',
  max_employees int DEFAULT NULL COMMENT '员工数上限快照',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  create_user bigint DEFAULT NULL COMMENT '创建人',
  update_user bigint DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sub_order_no ON tenant_subscription(order_no);
CREATE INDEX IF NOT EXISTS idx_sub_tenant_status ON tenant_subscription(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_sub_end_time ON tenant_subscription(end_time);

-- 清理测试残留数据
DELETE FROM tenant_subscription WHERE tenant_id = 1;
DELETE FROM billing_plan WHERE plan_code IN ('BASIC', 'STANDARD', 'PREMIUM');
