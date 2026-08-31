-- Cost module test schema (H2 compatible)
-- Matches entity column names from MyBatis-Plus default camelCase conversion
-- Note: cost module tables do NOT have is_deleted column (physical delete)

DROP TABLE IF EXISTS other_cost;
DROP TABLE IF EXISTS labor_cost;
DROP TABLE IF EXISTS cost_record;
DROP TABLE IF EXISTS dish_cost;

-- DishCost entity (@TableName("dish_cost"))
-- Columns: id, dishId, dishName, materialCost, laborCost, otherCost, totalCost, salePrice, profitRate, remark, tenantId, createTime, updateTime, createUser, updateUser
CREATE TABLE dish_cost (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  dish_id bigint NULL DEFAULT NULL COMMENT '菜品ID',
  dish_name varchar(100) NULL DEFAULT NULL COMMENT '菜品名称',
  material_cost decimal(10,2) NULL DEFAULT NULL COMMENT '食材成本',
  labor_cost decimal(10,2) NULL DEFAULT NULL COMMENT '人工成本',
  other_cost decimal(10,2) NULL DEFAULT NULL COMMENT '其他成本',
  total_cost decimal(10,2) NULL DEFAULT NULL COMMENT '总成本',
  sale_price decimal(10,2) NULL DEFAULT NULL COMMENT '售价',
  profit_rate decimal(10,2) NULL DEFAULT NULL COMMENT '毛利率(%)',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (id)
);

-- CostRecord entity (@TableName("cost_record"))
-- Columns: id, costType, refId, refName, amount, costDate, remark, tenantId, createTime, createUser
CREATE TABLE cost_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  cost_type int NULL DEFAULT NULL COMMENT '成本类型 1-食材/2-人工/3-其他',
  ref_id bigint NULL DEFAULT NULL COMMENT '关联ID',
  ref_name varchar(100) NULL DEFAULT NULL COMMENT '关联名称',
  amount decimal(10,2) NULL DEFAULT NULL COMMENT '成本金额',
  cost_date datetime NULL DEFAULT NULL COMMENT '成本日期',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  version int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (id)
);

-- LaborCost entity (@TableName("labor_cost"))
-- Columns: id, employeeId, employeeName, salary, socialInsurance, housingFund, otherBenefits, totalCost, costMonth, remark, tenantId, createTime, updateTime, createUser, updateUser
CREATE TABLE labor_cost (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  employee_id bigint NULL DEFAULT NULL COMMENT '员工ID',
  employee_name varchar(50) NULL DEFAULT NULL COMMENT '员工姓名',
  salary decimal(10,2) NULL DEFAULT NULL COMMENT '工资',
  social_insurance decimal(10,2) NULL DEFAULT NULL COMMENT '社保',
  housing_fund decimal(10,2) NULL DEFAULT NULL COMMENT '公积金',
  other_benefits decimal(10,2) NULL DEFAULT NULL COMMENT '其他福利',
  total_cost decimal(10,2) NULL DEFAULT NULL COMMENT '总成本',
  cost_month date NULL DEFAULT NULL COMMENT '成本月份',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (id)
);

-- OtherCost entity (@TableName("other_cost"))
-- Columns: id, name, costType, amount, costDate, remark, tenantId, createTime, updateTime, createUser, updateUser
CREATE TABLE other_cost (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(100) NULL DEFAULT NULL COMMENT '成本名称',
  cost_type int NULL DEFAULT NULL COMMENT '成本类型 1-租金/2-水电/3-设备/4-耗材/5-营销/6-其他',
  amount decimal(10,2) NULL DEFAULT NULL COMMENT '成本金额',
  cost_date datetime NULL DEFAULT NULL COMMENT '成本日期',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (id)
);