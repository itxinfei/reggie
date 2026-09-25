-- 瑞吉外卖测试数据库 Schema (H2兼容)
-- 从 reggie.sql 提取并转换为 H2 语法

-- 核心表

-- 菜品/套餐

-- 分类

-- 系统配置

-- 员工/权限

-- 操作日志

-- 区域

-- 门店

-- ==================== 用户表 ====================
CREATE TABLE IF NOT EXISTS user (
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

-- ==================== 地址簿 ====================
CREATE TABLE IF NOT EXISTS address_book (
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
  street_name varchar(50) NULL DEFAULT NULL COMMENT '街道/乡镇',
  community varchar(100) NULL DEFAULT NULL COMMENT '小区/大厦',
  building varchar(25) NULL DEFAULT NULL COMMENT '楼栋',
  unit varchar(25) NULL DEFAULT NULL COMMENT '单元',
  floor varchar(25) NULL DEFAULT NULL COMMENT '楼层',
  room_no varchar(25) NULL DEFAULT NULL COMMENT '门牌号',
  detail varchar(255) NULL DEFAULT NULL COMMENT '详细地址',
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

-- ==================== 分类表 ====================
CREATE TABLE IF NOT EXISTS category (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  type int NOT NULL DEFAULT 1 COMMENT '类型 1 菜品分类 2 套餐分类',
  name varchar(64) NOT NULL COMMENT '分类名称',
  sort int NOT NULL DEFAULT 0 COMMENT '顺序',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (id)
);

-- ==================== 菜品表 ====================
CREATE TABLE IF NOT EXISTS dish (
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

-- ==================== 菜品口味表 ====================
CREATE TABLE IF NOT EXISTS dish_flavor (
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

-- ==================== 套餐表 ====================
CREATE TABLE IF NOT EXISTS setmeal (
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

-- ==================== 套餐菜品关联表 ====================
CREATE TABLE IF NOT EXISTS setmeal_dish (
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

-- ==================== 订单表 ====================
CREATE TABLE IF NOT EXISTS orders (
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
  full_reduction_amount decimal(10,2) NULL DEFAULT 0.00 COMMENT '满减优惠金额（满减活动扣减，未享受为0）',
  new_customer_discount_amount decimal(10,2) NULL DEFAULT 0.00 COMMENT '新客立减金额（新客活动扣减，未享受为0）',
  remark varchar(100) NULL DEFAULT NULL COMMENT '备注',
  internal_remark varchar(500) NULL DEFAULT NULL COMMENT '内部备注（仅后台可见）',
  expect_delivery_time varchar(20) NULL DEFAULT NULL COMMENT '预送达时间',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户',
  phone varchar(255) NULL DEFAULT NULL COMMENT '手机',
  address varchar(255) NULL DEFAULT NULL COMMENT '地址',
  consignee varchar(50) NULL DEFAULT NULL COMMENT '收货',
  dining_type varchar(20) NULL DEFAULT 'OUTSIDE' COMMENT '用类型',
  table_id bigint NULL DEFAULT NULL COMMENT '堂桌台ID',
  table_name varchar(32) NULL DEFAULT NULL COMMENT '堂桌台名称',
  idempotency_key varchar(128) NULL DEFAULT NULL COMMENT '幂等',
  stock_refunded int NULL DEFAULT 0 COMMENT '已库存数量',
  used_coupon_id bigint NULL DEFAULT NULL COMMENT '优惠券ID',
  rider_id bigint NULL DEFAULT NULL COMMENT '配送骑手ID（店长派单/骑手抢单后写入）',
  dispatch_time datetime NULL DEFAULT NULL COMMENT '派单/抢单时间（超时回流判断）',
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

-- ==================== 订单明细表 ====================
CREATE TABLE IF NOT EXISTS order_detail (
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

-- ==================== 购物车 ====================
CREATE TABLE IF NOT EXISTS shopping_cart (
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

-- ==================== 员工表 ====================
CREATE TABLE IF NOT EXISTS employee (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  username varchar(50) NOT NULL COMMENT '用户名',
  name varchar(50) NULL DEFAULT NULL COMMENT '姓名',
  phone varchar(11) NULL DEFAULT NULL COMMENT '手机号',
  sex varchar(2) NULL DEFAULT NULL COMMENT '性别',
  id_number varchar(18) NULL DEFAULT NULL COMMENT '身份证号',
  avatar varchar(255) NULL DEFAULT NULL COMMENT '头像图片相对路径',
  job_number varchar(32) NULL DEFAULT NULL COMMENT '工号（租户内唯一）',
  position varchar(32) NULL DEFAULT NULL COMMENT '岗位',
  status int NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NOT NULL COMMENT '创建人',
  update_user bigint NOT NULL COMMENT '修改人',
  password varchar(255) NOT NULL COMMENT '密码',
  password_type varchar(32) NOT NULL DEFAULT 'MD5' COMMENT '密码加密类型 MD5/BCRYPT',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  role int NOT NULL DEFAULT 2 COMMENT '角色 1:超级管理员 2:普通员工',
  PRIMARY KEY (id)
);
-- 工号租户内唯一（job_number 为 NULL 时不参与约束，允许多个未设工号员工）

-- ==================== 角色表 ====================
CREATE TABLE IF NOT EXISTS role (
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

-- ==================== 权限表 ====================
CREATE TABLE IF NOT EXISTS permission (
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

-- ==================== 角色权限关联表 ====================
CREATE TABLE IF NOT EXISTS role_permission (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id bigint NOT NULL COMMENT '角色ID',
  permission_id bigint NOT NULL COMMENT '权限ID',
  create_time datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id)
);

-- ==================== 员工角色关联表（RBAC 闭环：用户→角色，多对多） ====================
CREATE TABLE IF NOT EXISTS employee_role (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  employee_id bigint NOT NULL COMMENT '员工ID',
  role_id bigint NOT NULL COMMENT '角色ID',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_employee_role (employee_id, role_id)
);

-- ==================== 系统配置表 ====================
CREATE TABLE IF NOT EXISTS system_config (
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

-- ==================== 菜单表 ====================
CREATE TABLE IF NOT EXISTS menu (
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

-- AI供应商配置

-- ==================== 操作日志表 ====================
CREATE TABLE IF NOT EXISTS log (
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

-- ==================== AI供应商配置表 ====================
CREATE TABLE IF NOT EXISTS ai_provider_config (
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

-- ==================== 区域表 ====================
-- 字段与 Region 实体（com.reggie.module.region.model.Region）及
-- src/test/resources/schema-mysql.sql 中的权威定义保持一致。
-- 说明：早期此处仅有 id/name/sort 等 6 列，缺少实体映射的
-- code/parent_id/level/create_user/update_user，任何走 RegionMapper 的
-- 查询都会报 Unknown column 'code' in 'field list'。
CREATE TABLE IF NOT EXISTS region (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(50) NOT NULL COMMENT '地区名称',
  code varchar(20) NULL DEFAULT NULL COMMENT '行政区划代码',
  parent_id bigint NOT NULL DEFAULT 0 COMMENT '父级ID，0为省份',
  level tinyint NOT NULL DEFAULT 1 COMMENT '层级：1省 2市 3区/县',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);

-- ==================== 商品评价表（菜品/套餐通用） ====================
-- 字段与 DishEvaluation 实体及 schema-mysql.sql 权威定义保持一致。
-- 套餐下单明细只有 setmealId，故 dish_id/setmeal_id 互斥可空。
CREATE TABLE IF NOT EXISTS dish_evaluation (
  id bigint NOT NULL COMMENT '评价ID(雪花算法)',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  order_id bigint NULL DEFAULT NULL COMMENT '订单id',
  user_id bigint NULL DEFAULT NULL COMMENT '评价用户id',
  user_name varchar(64) NULL DEFAULT NULL COMMENT '评价用户',
  dish_id bigint NULL DEFAULT NULL COMMENT '菜品id（菜品下单时填）',
  setmeal_id bigint NULL DEFAULT NULL COMMENT '套餐id（套餐下单时填，与dish_id互斥）',
  dish_name varchar(64) NULL DEFAULT NULL COMMENT '商品名称（菜品名/套餐名）',
  star_rating int NULL DEFAULT NULL COMMENT '评分(1-5)',
  content varchar(500) NULL DEFAULT NULL COMMENT '评价内容',
  images varchar(2000) NULL DEFAULT NULL COMMENT '评价图片JSON数组',
  anonymous int NOT NULL DEFAULT 0 COMMENT '是否匿名 0=实名 1=匿名',
  reply_content varchar(500) NULL DEFAULT NULL COMMENT '商家回复内容',
  reply_time datetime NULL DEFAULT NULL COMMENT '商家回复时间',
  status int NULL DEFAULT 0 COMMENT '审核状态：0待审核，1通过，2拒绝',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (id)
);

-- ==================== 门店表 ====================
CREATE TABLE IF NOT EXISTS store (
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

-- ==================== 门店配置表 ====================
CREATE TABLE IF NOT EXISTS store_config (
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
CREATE TABLE IF NOT EXISTS store_info (
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
  pause_order int NOT NULL DEFAULT 0 COMMENT '暂停接单 0:正常 1:暂停',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建用户',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
  PRIMARY KEY (id)
);

-- ==================== 门店日报汇总 ====================
CREATE TABLE IF NOT EXISTS store_daily_summary (
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
CREATE TABLE IF NOT EXISTS store_employee_permission (
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
CREATE TABLE IF NOT EXISTS store_sync_log (
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
CREATE TABLE IF NOT EXISTS platform_config (
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

-- ==================== tenant ====================
CREATE TABLE IF NOT EXISTS tenant (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(64) NULL DEFAULT NULL COMMENT '租户名称',
  phone varchar(20) NULL DEFAULT NULL COMMENT '电话',
  address varchar(255) NULL DEFAULT NULL COMMENT '地址',
  contact varchar(32) NULL DEFAULT NULL COMMENT '联系人',
  logo varchar(255) NULL DEFAULT NULL COMMENT 'Logo图片相对路径',
  license_image varchar(255) NULL DEFAULT NULL COMMENT '营业执照图片相对路径',
  package_name varchar(32) NULL DEFAULT NULL COMMENT '套餐名称',
  expire_time datetime NULL DEFAULT NULL COMMENT '套餐到期时间',
  password_type varchar(20) NULL DEFAULT 'MD5' COMMENT '密码加密类型',
  status int NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (id)
);

-- ==================== operation_log ====================
CREATE TABLE IF NOT EXISTS operation_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  operator_id bigint NULL DEFAULT NULL COMMENT '操作人ID',
  operator_name varchar(50) NULL DEFAULT NULL COMMENT '操作人姓名',
  operator_ip varchar(50) NULL DEFAULT NULL COMMENT '操作人IP',
  module varchar(50) NULL DEFAULT NULL COMMENT '操作模块',
  operation_type varchar(20) NULL DEFAULT NULL COMMENT '操作类型',
  table_name varchar(50) NULL DEFAULT NULL COMMENT '业务表名',
  biz_id bigint NULL DEFAULT NULL COMMENT '业务记录ID',
  description varchar(500) NULL DEFAULT NULL COMMENT '操作描述',
  old_value text NULL COMMENT '变更前值(JSON)',
  new_value text NULL COMMENT '变更后值(JSON)',
  request_url varchar(500) NULL DEFAULT NULL COMMENT '请求URL',
  request_method varchar(10) NULL DEFAULT NULL COMMENT '请求方法',
  request_params text NULL COMMENT '请求参数(JSON)',
  duration bigint NULL DEFAULT NULL COMMENT '执行时长(毫秒)',
  is_success int NOT NULL DEFAULT 0 COMMENT '是否成功:0失败 1成功',
  error_msg varchar(1000) NULL DEFAULT NULL COMMENT '错误信息',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NOT NULL COMMENT '创建时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

-- ==================== 营销活动 / 满减规则 / 核销（满减引擎计费依赖） ====================
-- 与本脚本其他表保持一致：建表前先 DROP，保证脚本在测试上下文里被重复执行时幂等
CREATE TABLE IF NOT EXISTS marketing_campaign (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NOT NULL COMMENT '租户ID',
  name varchar(100) NOT NULL COMMENT '活动名称',
  description varchar(500) NULL DEFAULT NULL COMMENT '活动描述',
  campaign_type int NOT NULL COMMENT '活动类型 1:满减 2:折扣 3:赠品 4:首单 5:会员专享 6:秒杀',
  target_type int NOT NULL DEFAULT 1 COMMENT '目标类型 1全部 2新用户 3高价值 4流失 5指定等级',
  target_value varchar(500) NULL DEFAULT NULL COMMENT '目标值',
  rule_json longtext NULL COMMENT '规则JSON',
  status int NOT NULL DEFAULT 0 COMMENT '状态 0草稿 1进行中 2已结束 3暂停',
  priority int NOT NULL DEFAULT 0 COMMENT '优先级',
  start_time datetime NOT NULL COMMENT '开始时间',
  end_time datetime NOT NULL COMMENT '结束时间',
  max_participants int NULL DEFAULT NULL COMMENT '最大参与人数',
  current_participants int NOT NULL DEFAULT 0 COMMENT '当前参与人数',
  coupon_template_id bigint NULL DEFAULT NULL COMMENT '关联券模板ID',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS full_reduction_rule (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  campaign_id bigint NOT NULL COMMENT '活动ID',
  rule_name varchar(100) NULL DEFAULT NULL COMMENT '规则名称',
  discount_type int NOT NULL COMMENT '类型 1减固定 2打折 3赠品',
  min_amount decimal(10,2) NOT NULL COMMENT '门槛金额',
  discount_value decimal(10,2) NOT NULL COMMENT '优惠值/折扣率',
  max_discount_amount decimal(10,2) NULL DEFAULT NULL COMMENT '最大优惠金额',
  gift_dish_id bigint NULL DEFAULT NULL COMMENT '赠品菜品ID',
  gift_quantity int NULL DEFAULT NULL COMMENT '赠品数量',
  stackable int NULL DEFAULT 0 COMMENT '是否可叠加',
  daily_limit int NULL DEFAULT NULL COMMENT '每日限次',
  per_user_limit int NULL DEFAULT NULL COMMENT '每人限次',
  sort_order int NULL DEFAULT 0 COMMENT '排序',
  status int NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人',
  create_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS campaign_usage_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  campaign_id bigint NOT NULL COMMENT '活动ID',
  rule_id bigint NULL DEFAULT NULL COMMENT '规则ID',
  rule_type int NULL DEFAULT NULL COMMENT '类型 1满减 2折扣 3秒杀 4新客立减 5买赠',
  quantity int NULL DEFAULT NULL COMMENT '数量（秒杀购买件数/买赠赠品件数，满减可空）',
  order_id bigint NULL DEFAULT NULL COMMENT '订单ID',
  order_number varchar(50) NULL DEFAULT NULL COMMENT '订单号',
  user_id bigint NULL DEFAULT NULL COMMENT '用户ID',
  order_amount decimal(10,2) NULL DEFAULT NULL COMMENT '商品金额',
  discount_amount decimal(10,2) NULL DEFAULT NULL COMMENT '优惠金额',
  actual_amount decimal(10,2) NULL DEFAULT NULL COMMENT '满减后金额',
  use_time datetime NULL DEFAULT NULL COMMENT '使用时间',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id)
);

-- 秒杀活动（C 端下单秒杀替换价 / CAS 扣库存）
CREATE TABLE IF NOT EXISTS flash_sale (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(100) NOT NULL COMMENT '活动名称',
  description varchar(500) NULL DEFAULT NULL COMMENT '描述',
  dish_id bigint NOT NULL COMMENT '菜品ID',
  dish_name varchar(100) NULL DEFAULT NULL COMMENT '菜品名称',
  original_price decimal(10,2) NULL DEFAULT NULL COMMENT '原价',
  flash_price decimal(10,2) NOT NULL COMMENT '秒杀价',
  total_quantity int NOT NULL DEFAULT 0 COMMENT '总库存',
  sold_quantity int NOT NULL DEFAULT 0 COMMENT '已售数量',
  max_per_user int NULL DEFAULT NULL COMMENT '每人限购',
  start_time datetime NOT NULL COMMENT '开始时间',
  end_time datetime NOT NULL COMMENT '结束时间',
  status int NOT NULL DEFAULT 0 COMMENT '状态 0草稿 1进行中 2暂停 3结束',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (id)
);

-- 买赠活动
CREATE TABLE IF NOT EXISTS buy_get_free (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(100) NOT NULL COMMENT '活动名称',
  description varchar(500) NULL DEFAULT NULL COMMENT '描述',
  buy_quantity int NOT NULL COMMENT '购买数量',
  get_quantity int NOT NULL COMMENT '赠品数量',
  dish_id bigint NULL DEFAULT NULL COMMENT '适用菜品ID',
  setmeal_id bigint NULL DEFAULT NULL COMMENT '适用套餐ID',
  gift_dish_id bigint NOT NULL COMMENT '赠品菜品ID',
  gift_dish_name varchar(100) NULL DEFAULT NULL COMMENT '赠品菜品名称',
  min_order_amount decimal(10,2) NULL DEFAULT NULL COMMENT '最低订单金额',
  max_times_per_order int NULL DEFAULT NULL COMMENT '每单最多触发次数',
  start_time datetime NOT NULL COMMENT '开始时间',
  end_time datetime NOT NULL COMMENT '结束时间',
  status tinyint NULL DEFAULT 0 COMMENT '状态 0草稿 1生效 2暂停 3结束',
  usage_count int NULL DEFAULT 0 COMMENT '已使用次数',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (id)
);

-- 新客立减
CREATE TABLE IF NOT EXISTS new_customer_discount (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(100) NOT NULL COMMENT '活动名称',
  discount_type tinyint NOT NULL COMMENT '优惠类型 1固定金额 2百分比',
  discount_value decimal(10,2) NOT NULL COMMENT '优惠值',
  max_discount_amount decimal(10,2) NULL DEFAULT NULL COMMENT '最大优惠金额',
  min_order_amount decimal(10,2) NULL DEFAULT NULL COMMENT '最低订单金额',
  valid_days int NULL DEFAULT NULL COMMENT '注册后有效天数',
  status tinyint NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
  remark varchar(500) NULL DEFAULT NULL COMMENT '备注',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人',
  update_user bigint NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (id)
);

-- ==================== 用户收藏 ====================
CREATE TABLE IF NOT EXISTS user_favorite (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id bigint NOT NULL COMMENT '用户ID',
  target_type int NOT NULL COMMENT '类型 1菜品 2商家',
  target_id bigint NOT NULL COMMENT '收藏对象ID',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (id)
);


-- ==================== 在线客服 / 投诉 ====================
CREATE TABLE IF NOT EXISTS cs_session (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_no varchar(50) NULL DEFAULT NULL COMMENT '会话编号',
  user_id bigint NULL DEFAULT NULL COMMENT '用户ID',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户',
  agent_id bigint NULL DEFAULT NULL COMMENT '客服ID',
  agent_name varchar(50) NULL DEFAULT NULL COMMENT '客服姓名',
  session_type int NULL DEFAULT NULL COMMENT '会话类型 1通用 2订单 3投诉',
  order_id bigint NULL DEFAULT NULL COMMENT '关联订单ID',
  status int NULL DEFAULT 0 COMMENT '状态 0等待 1进行中 2已关闭',
  first_response_time datetime NULL DEFAULT NULL COMMENT '首次响应时间',
  close_time datetime NULL DEFAULT NULL COMMENT '关闭时间',
  satisfaction_rating int NULL DEFAULT NULL COMMENT '满意度评分(1-5)',
  user_feedback varchar(500) NULL DEFAULT NULL COMMENT '用户反馈',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS cs_message (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id bigint NOT NULL COMMENT '会话ID',
  sender_type int NULL DEFAULT NULL COMMENT '发送方 1用户 2客服 3系统',
  sender_id bigint NULL DEFAULT NULL COMMENT '发送方ID',
  sender_name varchar(50) NULL DEFAULT NULL COMMENT '发送方',
  message_type int NULL DEFAULT 1 COMMENT '消息类型 1文本 2图片 3订单卡片',
  content text NULL DEFAULT NULL COMMENT '消息内容',
  image_url varchar(500) NULL DEFAULT NULL COMMENT '图片URL',
  is_read int NULL DEFAULT 0 COMMENT '是否已读 0否 1是',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS complaint (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  complaint_no varchar(50) NULL DEFAULT NULL COMMENT '投诉编号',
  user_id bigint NULL DEFAULT NULL COMMENT '用户ID',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户',
  user_phone varchar(20) NULL DEFAULT NULL COMMENT '用户手机',
  order_id bigint NULL DEFAULT NULL COMMENT '订单ID',
  order_number varchar(50) NULL DEFAULT NULL COMMENT '订单编号',
  complaint_type int NULL DEFAULT NULL COMMENT '投诉类型 1食品质量 2配送 3服务态度 4价格 5其他',
  title varchar(200) NULL DEFAULT NULL COMMENT '投诉标题',
  content text NULL DEFAULT NULL COMMENT '投诉内容',
  image_urls varchar(1000) NULL DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  status int NULL DEFAULT 0 COMMENT '状态 0待处理 1处理中 2已解决 3已关闭',
  handler_id bigint NULL DEFAULT NULL COMMENT '处理人ID',
  handler_name varchar(50) NULL DEFAULT NULL COMMENT '处理人',
  handle_result varchar(500) NULL DEFAULT NULL COMMENT '处理结果',
  compensation_amount decimal(10,2) NULL DEFAULT NULL COMMENT '补偿金',
  handle_time datetime NULL DEFAULT NULL COMMENT '处理时间',
  satisfaction int NULL DEFAULT NULL COMMENT '满意度 1-5',
  user_feedback varchar(500) NULL DEFAULT NULL COMMENT '用户反馈',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户ID',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id)
);
