-- 瑞吉外卖测试数据库 Schema (H2兼容)
-- 从 reggie.sql 提取并转换为 H2 语法

-- 核心表
DROP TABLE IF EXISTS order_detail;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS shopping_cart;
DROP TABLE IF EXISTS address_book;
DROP TABLE IF EXISTS user;

-- 菜品/套餐
DROP TABLE IF EXISTS dish_flavor;
DROP TABLE IF EXISTS dish;
DROP TABLE IF EXISTS setmeal_dish;
DROP TABLE IF EXISTS setmeal;

-- 分类
DROP TABLE IF EXISTS category;

-- 系统配置
DROP TABLE IF EXISTS system_config;

-- 员工/权限
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS employee_role;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS menu;

-- 操作日志
DROP TABLE IF EXISTS log;

-- 区域
DROP TABLE IF EXISTS region;

-- 门店
DROP TABLE IF EXISTS store;

-- ==================== 用户表 ====================
CREATE TABLE user (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(50) NULL DEFAULT NULL COMMENT '姓名',
  phone varchar(100) NOT NULL COMMENT '手机号',
  sex varchar(2) NULL DEFAULT NULL COMMENT '性别',
  id_number varchar(18) NULL DEFAULT NULL COMMENT '身份证号',
  avatar varchar(500) NULL DEFAULT NULL COMMENT '头像',
  status int NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '用户信息',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (id)
);
CREATE INDEX idx_user_phone ON user(phone);
CREATE INDEX idx_user_tenant ON user(tenant_id);

-- ==================== 地址簿 ====================
CREATE TABLE address_book (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id bigint NOT NULL COMMENT '用户id',
  consignee varchar(50) NOT NULL COMMENT '收货人',
  sex tinyint NULL DEFAULT NULL COMMENT '性别 0 男 1 女',
  phone varchar(11) NOT NULL COMMENT '手机号',
  province_code varchar(12) NULL DEFAULT NULL COMMENT '省级区划编号',
  province_name varchar(32) NULL DEFAULT NULL COMMENT '省级名称',
  city_code varchar(12) NULL DEFAULT NULL COMMENT '市级区划编号',
  city_name varchar(32) NULL DEFAULT NULL COMMENT '市级名称',
  district_code varchar(12) NULL DEFAULT NULL COMMENT '区级区划编号',
  district_name varchar(32) NULL DEFAULT NULL COMMENT '区级名称',
  detail varchar(200) NULL DEFAULT NULL COMMENT '详细地址',
  label varchar(100) NULL DEFAULT NULL COMMENT '标签',
  is_default tinyint(1) NOT NULL DEFAULT 0 COMMENT '默认 0 否 1是',
  longitude decimal(10,6) NULL DEFAULT NULL COMMENT '经度（GCJ-02）',
  latitude decimal(10,6) NULL DEFAULT NULL COMMENT '纬度（GCJ-02）',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (id)
);
CREATE INDEX idx_address_user ON address_book(user_id);
CREATE INDEX idx_address_tenant ON address_book(tenant_id);

-- ==================== 分类表 ====================
CREATE TABLE category (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  type int NOT NULL DEFAULT 1 COMMENT '类型 1 菜品分类 2 套餐分类',
  name varchar(64) NOT NULL COMMENT '分类名称',
  sort int NOT NULL DEFAULT 0 COMMENT '顺序',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '菜品及套餐分类',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (id)
);
CREATE INDEX idx_category_type ON category(type);
CREATE INDEX idx_category_tenant ON category(tenant_id);

-- ==================== 菜品表 ====================
CREATE TABLE dish (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_id bigint NOT NULL COMMENT '菜品分类id',
  name varchar(64) NOT NULL COMMENT '菜品名称',
  price decimal(10,2) NOT NULL COMMENT '菜品价格',
  code varchar(64) NULL DEFAULT NULL COMMENT '商品码',
  image varchar(255) NULL DEFAULT NULL COMMENT '图片',
  description varchar(400) NULL DEFAULT NULL COMMENT '描述信息',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '0 停售 1 起售',
  sort int NULL DEFAULT NULL COMMENT '顺序',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  stock_qty decimal(10,2) NOT NULL DEFAULT 0 COMMENT '当前库存数量',
  min_stock decimal(10,2) NOT NULL DEFAULT 0 COMMENT '最低库存预警阈值',
  PRIMARY KEY (id)
);
CREATE INDEX idx_dish_category ON dish(category_id);
CREATE INDEX idx_dish_tenant ON dish(tenant_id);

-- ==================== 菜品口味表 ====================
CREATE TABLE dish_flavor (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  dish_id bigint NOT NULL COMMENT '菜品',
  name varchar(64) NOT NULL COMMENT '口味名称',
  value varchar(255) NULL DEFAULT NULL COMMENT '口味数据list',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);
CREATE INDEX idx_dish_flavor_dish ON dish_flavor(dish_id);

-- ==================== 套餐表 ====================
CREATE TABLE setmeal (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_id bigint NOT NULL COMMENT '菜品分类id',
  name varchar(64) NOT NULL COMMENT '套餐名称',
  price decimal(10,2) NOT NULL COMMENT '套餐价格',
  code varchar(64) NULL DEFAULT NULL COMMENT '编码',
  image varchar(255) NULL DEFAULT NULL COMMENT '图片',
  description varchar(400) NULL DEFAULT NULL COMMENT '描述信息',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0:停用 1:启用',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (id)
);
CREATE INDEX idx_setmeal_category ON setmeal(category_id);
CREATE INDEX idx_setmeal_tenant ON setmeal(tenant_id);

-- ==================== 套餐菜品关联表 ====================
CREATE TABLE setmeal_dish (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  setmeal_id bigint NOT NULL COMMENT '套餐id',
  dish_id bigint NOT NULL COMMENT '菜品id',
  name varchar(64) NOT NULL COMMENT '菜品名称（冗余）',
  price decimal(10,2) NOT NULL COMMENT '菜品原价（冗余）',
  copies int NOT NULL DEFAULT 1 COMMENT '份数',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);
CREATE INDEX idx_setmeal_dish_setmeal ON setmeal_dish(setmeal_id);

-- ==================== 订单表 ====================
CREATE TABLE orders (
  id bigint NOT NULL COMMENT '主键',
  number varchar(50) NULL DEFAULT NULL COMMENT '订单',
  status int NOT NULL DEFAULT 1 COMMENT '订单状',
  user_id bigint NULL DEFAULT NULL COMMENT '用户id',
  address_book_id bigint NULL DEFAULT NULL COMMENT '地址id',
  order_time datetime NULL DEFAULT NULL COMMENT '下单时间',
  checkout_time datetime NULL DEFAULT NULL COMMENT '结账时间',
  pay_method int NULL DEFAULT NULL COMMENT '攻方式',
  amount decimal(10,2) NOT NULL COMMENT '实收金',
  delivery_fee decimal(10,2) NULL DEFAULT NULL COMMENT '配送费（外卖单独立存储，堂食为0）',
  remark varchar(100) NULL DEFAULT NULL COMMENT '备注',
  expect_delivery_time varchar(20) NULL DEFAULT NULL COMMENT '预送达时间',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户',
  phone varchar(255) NULL DEFAULT NULL COMMENT '手机',
  address varchar(255) NULL DEFAULT NULL COMMENT '地址',
  consignee varchar(50) NULL DEFAULT NULL COMMENT '收货',
  dining_type varchar(20) NULL DEFAULT 'OUTSIDE' COMMENT '用类型',
  table_id bigint NULL DEFAULT NULL COMMENT '堂桌台ID',
  table_name varchar(32) NULL DEFAULT NULL COMMENT '堂桌台名称',
  queue_id bigint NULL DEFAULT NULL COMMENT '排队记录ID',
  reservation_id bigint NULL DEFAULT NULL COMMENT '预记录ID',
  customer_count int NULL DEFAULT NULL COMMENT '用人数',
  idempotency_key varchar(128) NULL DEFAULT NULL COMMENT '幂等',
  stock_refunded int NULL DEFAULT 0 COMMENT '已库存数量',
  used_coupon_id bigint NULL DEFAULT NULL COMMENT '优惠券ID',
  platform_type varchar(32) NULL DEFAULT NULL COMMENT '平台来源',
  platform_order_id varchar(128) NULL DEFAULT NULL COMMENT '平台订单',
  platform_shop_id varchar(128) NULL DEFAULT NULL COMMENT '平台门店ID',
  platform_raw longtext NULL COMMENT '平台原订单JSON',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建',
  update_user bigint NULL DEFAULT NULL COMMENT '俔',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '昐删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  version int NOT NULL DEFAULT 0 COMMENT '乐锁版朏',
  master_order_id bigint NULL DEFAULT NULL COMMENT '父订单ID（AA分账时指向主订单）',
  split_count int NULL DEFAULT NULL COMMENT '分账份数（AA分账记录拆分数量）',
  PRIMARY KEY (id)
);
CREATE INDEX idx_order_user ON orders(user_id, order_time);
CREATE INDEX idx_order_number ON orders(number);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_tenant ON orders(tenant_id);
CREATE UNIQUE INDEX uq_orders_platform ON orders(tenant_id, platform_type, platform_order_id);

-- ==================== 订单明细表 ====================
CREATE TABLE order_detail (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(50) NOT NULL COMMENT '名称',
  order_id bigint NOT NULL COMMENT '订单id',
  dish_id bigint NULL DEFAULT NULL COMMENT '菜品id',
  setmeal_id bigint NULL DEFAULT NULL COMMENT '套id',
  dish_flavor varchar(50) NULL DEFAULT NULL COMMENT '口味',
  number int NOT NULL DEFAULT 1 COMMENT '数量',
  amount decimal(10,2) NOT NULL COMMENT '单价',
  remark varchar(255) NULL DEFAULT NULL COMMENT '订单明细备注',
  image varchar(255) NULL DEFAULT NULL COMMENT '图片',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);
CREATE INDEX idx_order_detail_order ON order_detail(order_id);

-- ==================== 购物车 ====================
CREATE TABLE shopping_cart (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(50) NOT NULL COMMENT '名称',
  user_id bigint NOT NULL COMMENT '主键',
  dish_id bigint NULL DEFAULT NULL COMMENT '菜品id',
  setmeal_id bigint NULL DEFAULT NULL COMMENT '套餐id',
  dish_flavor varchar(50) NULL DEFAULT NULL COMMENT '口味',
  number int NOT NULL DEFAULT 1 COMMENT '数量',
  amount decimal(10,2) NOT NULL COMMENT '金额',
  image varchar(255) NULL DEFAULT NULL COMMENT '图片',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_cart_user ON shopping_cart(user_id);

-- ==================== 员工表 ====================
CREATE TABLE employee (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  username varchar(50) NOT NULL COMMENT '用户名',
  name varchar(50) NULL DEFAULT NULL COMMENT '姓名',
  phone varchar(11) NULL DEFAULT NULL COMMENT '手机号',
  sex varchar(2) NULL DEFAULT NULL COMMENT '性别',
  id_number varchar(18) NULL DEFAULT NULL COMMENT '身份证号',
  status int NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  password varchar(255) NOT NULL COMMENT '密码',
  password_type varchar(32) NOT NULL DEFAULT 'MD5' COMMENT '密码加密类型 MD5/BCRYPT',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '员工信息',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  role int NOT NULL DEFAULT 2 COMMENT '角色 1:超级管理员 2:普通员工',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_employee_username ON employee(username);
CREATE INDEX idx_employee_tenant ON employee(tenant_id);

-- ==================== 角色表 ====================
CREATE TABLE role (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID（NULL=全局角色）',
  role_name varchar(50) NOT NULL COMMENT '角色名称',
  role_key varchar(50) NOT NULL COMMENT '角色权限字符串',
  description varchar(200) NULL DEFAULT NULL COMMENT '角色描述',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_role_key ON role(role_key);

-- ==================== 权限表 ====================
CREATE TABLE permission (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  permission_name varchar(50) NOT NULL COMMENT '权限名称',
  permission_key varchar(100) NOT NULL COMMENT '权限标识',
  permission_type int NOT NULL DEFAULT 1 COMMENT '权限类型 1:菜单 2:按钮 3:数据',
  parent_id bigint NULL DEFAULT 0 COMMENT '父权限ID（0=顶级）',
  route_path varchar(200) NULL DEFAULT NULL COMMENT '路由路径',
  icon varchar(100) NULL DEFAULT NULL COMMENT '菜单图标',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  status int NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_permission_key ON permission(permission_key);
CREATE INDEX idx_permission_parent ON permission(parent_id);

-- ==================== 角色权限关联表 ====================
CREATE TABLE role_permission (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id bigint NOT NULL COMMENT '角色ID',
  permission_id bigint NOT NULL COMMENT '权限ID',
  create_time datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_role_permission_role ON role_permission(role_id);
CREATE INDEX idx_role_permission_permission ON role_permission(permission_id);

-- ==================== 员工角色关联表（RBAC 闭环：用户→角色，多对多） ====================
CREATE TABLE employee_role (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  employee_id bigint NOT NULL COMMENT '员工ID',
  role_id bigint NOT NULL COMMENT '角色ID',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_employee_role (employee_id, role_id)
);
CREATE INDEX idx_employee_role_employee ON employee_role(employee_id);
CREATE INDEX idx_employee_role_role ON employee_role(role_id);

-- ==================== 系统配置表 ====================
CREATE TABLE system_config (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID（NULL=全局配置）',
  config_key varchar(100) NOT NULL COMMENT '配置键',
  config_value varchar(500) NOT NULL COMMENT '配置值',
  config_type int NOT NULL DEFAULT 1 COMMENT '配置类型 1:功能开关 2:运营参数 3:显示设置 4:其他',
  description varchar(200) NULL DEFAULT NULL COMMENT '配置说明',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_system_config_key_tenant ON system_config(config_key, tenant_id);

-- ==================== 菜单表 ====================
CREATE TABLE menu (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id bigint NULL DEFAULT 0 COMMENT '父菜单ID',
  name varchar(50) NOT NULL COMMENT '菜单名称',
  path varchar(200) NULL DEFAULT NULL COMMENT '路由路径',
  component varchar(255) NULL DEFAULT NULL COMMENT '组件路径',
  perms varchar(100) NULL DEFAULT NULL COMMENT '权限标识',
  icon varchar(100) NULL DEFAULT NULL COMMENT '菜单图标',
  type int NOT NULL DEFAULT 1 COMMENT '类型 1:菜单 2:按钮',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);
CREATE INDEX idx_menu_parent ON menu(parent_id);

-- AI供应商配置
DROP TABLE IF EXISTS ai_provider_config;

-- ==================== 操作日志表 ====================
CREATE TABLE log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  operate_user bigint NULL DEFAULT NULL COMMENT '操作人ID',
  operate_name varchar(50) NULL DEFAULT NULL COMMENT '操作人姓名',
  module varchar(50) NULL DEFAULT NULL COMMENT '操作模块',
  type varchar(50) NULL DEFAULT NULL COMMENT '操作类型',
  method varchar(200) NULL DEFAULT NULL COMMENT '请求方法',
  request_url varchar(200) NULL DEFAULT NULL COMMENT '请求URL',
  request_params text NULL DEFAULT NULL COMMENT '请求参数',
  response_data text NULL DEFAULT NULL COMMENT '响应数据',
  ip varchar(50) NULL DEFAULT NULL COMMENT 'IP地址',
  status int NULL DEFAULT NULL COMMENT '状态 0:失败 1:成功',
  error_msg text NULL DEFAULT NULL COMMENT '错误信息',
  cost_time bigint NULL DEFAULT NULL COMMENT '耗时(ms)',
  create_time datetime NOT NULL COMMENT '创建时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (id)
);
CREATE INDEX idx_log_user ON log(operate_user);
CREATE INDEX idx_log_time ON log(create_time);
CREATE INDEX idx_log_tenant ON log(tenant_id);

-- ==================== AI供应商配置表 ====================
CREATE TABLE ai_provider_config (
  id bigint NOT NULL COMMENT '主键',
  provider_code varchar(50) NOT NULL COMMENT '供应商编码',
  provider_name varchar(100) NOT NULL COMMENT '供应商名称',
  base_url varchar(500) NULL DEFAULT NULL COMMENT 'API基础URL',
  model_name varchar(100) NULL DEFAULT NULL COMMENT '模型名称',
  api_key varchar(500) NULL DEFAULT NULL COMMENT 'APIԿܴ洢',
  timeout int NULL DEFAULT 30 COMMENT '请求超时时间（秒）',
  max_tokens int NULL DEFAULT 2048 COMMENT '最大Token数',
  temperature double NULL DEFAULT 0.7 COMMENT '温度参数',
  api_format varchar(50) NULL DEFAULT NULL COMMENT 'API格式类型',
  extra_headers text NULL DEFAULT NULL COMMENT '额外请求头（JSON）',
  request_template text NULL DEFAULT NULL COMMENT '请求体映射模板',
  response_path varchar(200) NULL DEFAULT NULL COMMENT '响应解析路径',
  icon_url varchar(500) NULL DEFAULT NULL COMMENT '图标URL',
  enabled int NOT NULL DEFAULT 1 COMMENT '是否启用',
  is_active int NOT NULL DEFAULT 0 COMMENT '是否激活',
  last_test_time datetime NULL DEFAULT NULL COMMENT '最后测试时间',
  last_test_result varchar(50) NULL DEFAULT NULL COMMENT '最后测试结果',
  sort int NOT NULL DEFAULT 0 COMMENT '排序号',
  remark varchar(500) NULL DEFAULT NULL COMMENT '备注',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);
CREATE INDEX idx_ai_provider_config_code ON ai_provider_config(provider_code);
CREATE INDEX idx_ai_provider_config_active ON ai_provider_config(is_active);

-- ==================== 区域表 ====================
CREATE TABLE region (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(32) NOT NULL COMMENT '地区名称',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);

-- ==================== 门店表 ====================
CREATE TABLE store (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(100) NOT NULL COMMENT '门店名称',
  address varchar(255) NOT NULL COMMENT '门店地址',
  phone varchar(20) NULL DEFAULT NULL COMMENT 'ϵ绰',
  business_hours varchar(100) NULL DEFAULT NULL COMMENT '营业时间',
  logo varchar(255) NULL DEFAULT NULL COMMENT '门店Logo',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0:停业 1:营业',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (id)
);
CREATE INDEX idx_store_tenant ON store(tenant_id);

-- ==================== 门店配置表 ====================
DROP TABLE IF EXISTS store_config;
CREATE TABLE store_config (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户/门店ID',
  config_key varchar(100) NULL DEFAULT NULL COMMENT '配置键',
  config_value varchar(500) NULL DEFAULT NULL COMMENT '配置值',
  config_type int NULL DEFAULT NULL COMMENT '配置类型 1:功能配置 2:运营参数 3:显示设置 4:其他',
  description varchar(200) NULL DEFAULT NULL COMMENT '配置说明',
  created_by bigint NULL DEFAULT NULL COMMENT '配置创建人(总部管理员)',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (id)
);

-- ==================== 门店基础信息表 ====================
DROP TABLE IF EXISTS store_info;
CREATE TABLE store_info (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '所属租户/门店ID',
  store_code varchar(50) NULL DEFAULT NULL COMMENT '门店编码，如：BJ001、SH001',
  store_type int NULL DEFAULT NULL COMMENT '门店类型 1:直营总店 2:直营分店 3:加盟',
  parent_tenant_id bigint NULL DEFAULT NULL COMMENT '上级总店tenantId，NULL表示总店本身',
  business_hours varchar(200) NULL DEFAULT NULL COMMENT '营业时间，如 9:00-22:00',
  delivery_radius int NULL DEFAULT NULL COMMENT '配送半径(米)',
  min_delivery_amount decimal(10,2) NULL DEFAULT NULL COMMENT '最低起送金额',
  delivery_fee decimal(10,2) NULL DEFAULT NULL COMMENT '配送费',
  is_delivery_enabled int NULL DEFAULT NULL COMMENT '是否外卖 0:否 1:是',
  is_dine_in_enabled int NULL DEFAULT NULL COMMENT '是否堂食 0:否 1:是',
  contact_person varchar(50) NULL DEFAULT NULL COMMENT '门店联系',
  contact_phone varchar(20) NULL DEFAULT NULL COMMENT 'ŵϵ绰',
  longitude decimal(10,7) NULL DEFAULT NULL COMMENT '经度',
  latitude decimal(10,7) NULL DEFAULT NULL COMMENT '纬度',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建用户',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (id)
);

-- ==================== 门店日报汇总 ====================
DROP TABLE IF EXISTS store_daily_summary;
CREATE TABLE store_daily_summary (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '门店ID',
  summary_date date NULL DEFAULT NULL COMMENT '统计日期',
  total_orders int NULL DEFAULT NULL COMMENT '订单总数',
  completed_orders int NULL DEFAULT NULL COMMENT '已完成订单数',
  cancelled_orders int NULL DEFAULT NULL COMMENT '取消订单',
  total_amount decimal(12,2) NULL DEFAULT NULL COMMENT '订单总金额',
  actual_amount decimal(12,2) NULL DEFAULT NULL COMMENT '实收金额',
  new_users int NULL DEFAULT NULL COMMENT '新增用户',
  avg_order_amount decimal(10,2) NULL DEFAULT NULL COMMENT '平均订单金额',
  top_dish_json varchar(500) NULL DEFAULT NULL COMMENT '热销菜品TOP10 JSON',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (id)
);

-- ==================== 门店员工权限 ====================
DROP TABLE IF EXISTS store_employee_permission;
CREATE TABLE store_employee_permission (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  employee_id bigint NULL DEFAULT NULL COMMENT '员工ID',
  tenant_id bigint NULL DEFAULT NULL COMMENT '门店ID',
  role_type int NULL DEFAULT NULL COMMENT '角色类型 1:店长 2:厨师 3:服务员 4:收银员 5:配菜员',
  permissions varchar(500) NULL DEFAULT NULL COMMENT '权限列表 JSON，如 ["dish:view","dish:edit","order:view"]',
  is_active int NULL DEFAULT NULL COMMENT '是否生效 0:否 1:是',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建用户',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (id)
);

-- ==================== 门店同步日志 ====================
DROP TABLE IF EXISTS store_sync_log;
CREATE TABLE store_sync_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  source_tenant_id bigint NULL DEFAULT NULL COMMENT '来源门店ID(通常指总部)',
  target_tenant_id bigint NULL DEFAULT NULL COMMENT '目标门店ID',
  sync_type int NULL DEFAULT NULL COMMENT '同步类型 1:菜品同步 2:分类同步 3:套餐同步 4:配置同步 5:优惠券同步',
  sync_mode int NULL DEFAULT NULL COMMENT '同步模式 1:全量同步 2:增量同步 3:选择性同步',
  sync_status int NULL DEFAULT NULL COMMENT '同步状态 0:进行中 1:成功 2:失败 3:部分成功',
  sync_count int NULL DEFAULT NULL COMMENT '同步数量',
  fail_count int NULL DEFAULT NULL COMMENT '失败数量',
  error_detail varchar(1000) NULL DEFAULT NULL COMMENT '错误详情',
  operator_id bigint NULL DEFAULT NULL COMMENT '操作人ID',
  start_time datetime NULL DEFAULT NULL COMMENT '同步开始时间',
  end_time datetime NULL DEFAULT NULL COMMENT '结束时间',
  create_time datetime NULL DEFAULT NULL COMMENT '门店同步日志',
  update_time datetime NULL DEFAULT NULL COMMENT '门店同步日志',
  create_user bigint NULL DEFAULT NULL COMMENT '门店同步日志',
  update_user bigint NULL DEFAULT NULL COMMENT '门店同步日志',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (id)
);

-- ==================== 外卖平台接入配置 ====================
DROP TABLE IF EXISTS platform_config;
CREATE TABLE platform_config (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  platform_type varchar(32) NOT NULL COMMENT '平台类型 MEITUAN/ELEME/DOUYIN/SELF/OTHER',
  platform_name varchar(128) NULL DEFAULT NULL COMMENT '平台展示名称',
  shop_id varchar(128) NULL DEFAULT NULL COMMENT '平台侧门店ID',
  app_key varchar(512) NULL DEFAULT NULL COMMENT '应用标识(加密)',
  app_secret varchar(512) NULL DEFAULT NULL COMMENT '应用密钥(加密)',
  access_token varchar(512) NULL DEFAULT NULL COMMENT '访问令牌(加密)',
  enabled int NOT NULL DEFAULT 1 COMMENT '昐吔 0停用 1吔',
  sync_scope int NOT NULL DEFAULT 1 COMMENT '同范围位标 1订单2商品4库存8营业状',
  remark varchar(500) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);
CREATE INDEX idx_platform_type_shop ON platform_config(platform_type, shop_id);
CREATE INDEX idx_platform_tenant ON platform_config(tenant_id);

