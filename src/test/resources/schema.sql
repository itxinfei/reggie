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

-- 员工/权限
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS role;
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
  sex varchar(2) NULL DEFAULT NULL COMMENT '性别 0女 1男',
  id_number varchar(18) NULL DEFAULT NULL COMMENT '身份证号',
  avatar varchar(500) NULL DEFAULT NULL COMMENT '头像',
  status int NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
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
  sex tinyint NULL DEFAULT NULL COMMENT '性别 0 女 1 男',
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
  type int NOT NULL DEFAULT 1 COMMENT '类型 1:菜品分类 2:套餐分类',
  name varchar(64) NOT NULL COMMENT '分类名称',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
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
  category_id bigint NOT NULL COMMENT '菜品分类ID',
  name varchar(64) NOT NULL COMMENT '菜品名称',
  price decimal(10,2) NOT NULL COMMENT '菜品价格',
  code varchar(64) NULL DEFAULT NULL COMMENT '商品码',
  image varchar(255) NULL DEFAULT NULL COMMENT '图片',
  description varchar(400) NULL DEFAULT NULL COMMENT '描述信息',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0:停售 1:起售',
  sort int NULL DEFAULT NULL COMMENT '排序',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  stock_qty decimal(10,2) NOT NULL DEFAULT 0 COMMENT '库存数量',
  min_stock decimal(10,2) NOT NULL DEFAULT 0 COMMENT '最低库存预警',
  PRIMARY KEY (id)
);
CREATE INDEX idx_dish_category ON dish(category_id);
CREATE INDEX idx_dish_tenant ON dish(tenant_id);

-- ==================== 菜品口味表 ====================
CREATE TABLE dish_flavor (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  dish_id bigint NOT NULL COMMENT '菜品id',
  name varchar(64) NOT NULL COMMENT '口味名称',
  value varchar(255) NULL DEFAULT NULL COMMENT '口味值',
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
  category_id bigint NOT NULL COMMENT '套餐分类ID',
  name varchar(64) NOT NULL COMMENT '套餐名称',
  price decimal(10,2) NOT NULL COMMENT '套餐价格',
  code varchar(64) NULL DEFAULT NULL COMMENT '套餐编码',
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
  name varchar(64) NOT NULL COMMENT '菜品名称',
  price decimal(10,2) NOT NULL COMMENT '菜品单价',
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
  number varchar(50) NULL DEFAULT NULL COMMENT '订单号',
  status int NOT NULL DEFAULT 1 COMMENT '订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消',
  user_id bigint NULL DEFAULT NULL COMMENT '用户id',
  address_book_id bigint NULL DEFAULT NULL COMMENT '地址id',
  order_time datetime NULL DEFAULT NULL COMMENT '下单时间',
  checkout_time datetime NULL DEFAULT NULL COMMENT '结账时间',
  pay_method int NULL DEFAULT NULL COMMENT '支付方式 1微信 2支付宝',
  amount decimal(10,2) NOT NULL COMMENT '实收金额',
  remark varchar(100) NULL DEFAULT NULL COMMENT '备注',
  expect_delivery_time varchar(20) NULL DEFAULT NULL COMMENT '预计送达时间',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户名',
  phone varchar(255) NULL DEFAULT NULL COMMENT '手机号',
  address varchar(255) NULL DEFAULT NULL COMMENT '地址',
  consignee varchar(50) NULL DEFAULT NULL COMMENT '收货人',
  dining_type varchar(20) NULL DEFAULT 'OUTSIDE' COMMENT '用餐类型',
  table_id bigint NULL DEFAULT NULL COMMENT '堂食桌台ID',
  table_name varchar(32) NULL DEFAULT NULL COMMENT '堂食桌台名称',
  queue_id bigint NULL DEFAULT NULL COMMENT '排队记录ID',
  reservation_id bigint NULL DEFAULT NULL COMMENT '预订记录ID',
  customer_count int NULL DEFAULT NULL COMMENT '用餐人数',
  idempotency_key varchar(128) NULL DEFAULT NULL COMMENT '幂等键',
  stock_refunded int NULL DEFAULT 0 COMMENT '已退库存数量',
  used_coupon_id bigint NULL DEFAULT NULL COMMENT '本单使用的优惠券ID（用户优惠券记录ID），未使用为 NULL',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (id)
);
CREATE INDEX idx_order_user ON orders(user_id, order_time);
CREATE INDEX idx_order_number ON orders(number);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_tenant ON orders(tenant_id);

-- ==================== 订单明细表 ====================
CREATE TABLE order_detail (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(50) NOT NULL COMMENT '名称',
  order_id bigint NOT NULL COMMENT '订单id',
  dish_id bigint NULL DEFAULT NULL COMMENT '菜品id',
  setmeal_id bigint NULL DEFAULT NULL COMMENT '套餐id',
  dish_flavor varchar(50) NULL DEFAULT NULL COMMENT '口味',
  number int NOT NULL DEFAULT 1 COMMENT '数量',
  amount decimal(10,2) NOT NULL COMMENT '单价',
  remark varchar(255) NULL DEFAULT NULL COMMENT '订单明细备注',
  image varchar(255) NULL DEFAULT NULL COMMENT '图片',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);
CREATE INDEX idx_order_detail_order ON order_detail(order_id);

-- ==================== 购物车 ====================
CREATE TABLE shopping_cart (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(50) NOT NULL COMMENT '名称',
  user_id bigint NOT NULL COMMENT '用户id',
  dish_id bigint NULL DEFAULT NULL COMMENT '菜品id',
  setmeal_id bigint NULL DEFAULT NULL COMMENT '套餐id',
  dish_flavor varchar(50) NULL DEFAULT NULL COMMENT '口味',
  number int NOT NULL DEFAULT 1 COMMENT '数量',
  amount decimal(10,2) NOT NULL COMMENT '单价',
  image varchar(255) NULL DEFAULT NULL COMMENT '图片',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
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
  sex varchar(2) NULL DEFAULT NULL COMMENT '性别 0女 1男',
  id_number varchar(18) NULL DEFAULT NULL COMMENT '身份证号',
  status int NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  password varchar(255) NOT NULL COMMENT '密码',
  password_type varchar(32) NOT NULL DEFAULT 'MD5' COMMENT '密码类型 MD5/BCRYPT',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  role int NOT NULL DEFAULT 2 COMMENT '角色 1:管理员 2:普通员工',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_employee_username ON employee(username);
CREATE INDEX idx_employee_tenant ON employee(tenant_id);

-- ==================== 角色表 ====================
CREATE TABLE role (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(50) NOT NULL COMMENT '角色名称',
  role_key varchar(50) NOT NULL COMMENT '角色权限字符串',
  status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_role_key ON role(role_key);

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

-- ==================== 区域表 ====================
CREATE TABLE region (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(32) NOT NULL COMMENT '区域名称',
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
  phone varchar(20) NULL DEFAULT NULL COMMENT '联系电话',
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
