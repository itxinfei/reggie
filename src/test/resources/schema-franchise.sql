-- 加盟分账模块测试表结构（H2 MODE=MYSQL）
DROP TABLE IF EXISTS franchise_settlement;
DROP TABLE IF EXISTS franchise_contract;
DROP TABLE IF EXISTS franchisee;

CREATE TABLE franchisee (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '加盟商ID',
  tenant_id bigint NULL DEFAULT NULL COMMENT '所属总部租户ID',
  name varchar(100) NOT NULL COMMENT '加盟商名称',
  contact_person varchar(50) NULL DEFAULT NULL COMMENT '联系人',
  contact_phone varchar(20) NULL DEFAULT NULL COMMENT '联系电话',
  address varchar(255) NULL DEFAULT NULL COMMENT '联系地址',
  status tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
  remark varchar(500) NULL DEFAULT NULL COMMENT '备注',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);
CREATE INDEX idx_franchisee_tenant ON franchisee(tenant_id);

CREATE TABLE franchise_contract (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  tenant_id bigint NULL DEFAULT NULL COMMENT '所属总部租户ID',
  franchisee_id bigint NOT NULL COMMENT '加盟商ID',
  store_tenant_id bigint NULL DEFAULT NULL COMMENT '加盟门店租户ID',
  contract_no varchar(50) NOT NULL COMMENT '合同编号',
  start_date date NULL DEFAULT NULL COMMENT '合同开始日期',
  end_date date NULL DEFAULT NULL COMMENT '合同结束日期',
  commission_type tinyint NOT NULL DEFAULT 1 COMMENT '抽成方式：1=比例，2=固定金额',
  commission_rate decimal(6,4) NULL DEFAULT NULL COMMENT '抽成比例',
  commission_amount decimal(12,2) NULL DEFAULT NULL COMMENT '固定抽成金额/周期',
  settle_cycle tinyint NOT NULL DEFAULT 1 COMMENT '结算周期：1=月结',
  status tinyint NOT NULL DEFAULT 1 COMMENT '合同状态：0=已终止，1=生效中',
  remark varchar(500) NULL DEFAULT NULL COMMENT '备注',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  CONSTRAINT uk_contract_no UNIQUE (contract_no)
);
CREATE INDEX idx_contract_franchisee ON franchise_contract(franchisee_id);

CREATE TABLE franchise_settlement (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '结算单ID',
  tenant_id bigint NULL DEFAULT NULL COMMENT '所属总部租户ID',
  contract_id bigint NOT NULL COMMENT '加盟合同ID',
  franchisee_id bigint NOT NULL COMMENT '加盟商ID',
  store_tenant_id bigint NULL DEFAULT NULL COMMENT '加盟门店租户ID',
  settle_period varchar(10) NOT NULL COMMENT '结算周期，如 2026-08',
  order_count int NOT NULL DEFAULT 0 COMMENT '周期内已完成订单数',
  sales_amount decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '周期营业额',
  commission_type tinyint NOT NULL DEFAULT 1 COMMENT '抽成方式',
  commission_rate decimal(6,4) NULL DEFAULT NULL COMMENT '抽成比例',
  commission_amount decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '应抽成金额',
  settle_amount decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '加盟商应结算金额',
  status tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=待确认，1=已确认，2=已结算',
  confirm_time datetime NULL DEFAULT NULL COMMENT '确认时间',
  settle_time datetime NULL DEFAULT NULL COMMENT '结算时间',
  remark varchar(500) NULL DEFAULT NULL COMMENT '备注',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  CONSTRAINT uk_settle_contract_period UNIQUE (contract_id, settle_period)
);
CREATE INDEX idx_settle_franchisee ON franchise_settlement(franchisee_id);
CREATE INDEX idx_settle_period ON franchise_settlement(settle_period);
