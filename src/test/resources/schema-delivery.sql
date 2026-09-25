-- Delivery module test schema (H2 compatible)
-- Matches entity column names from MyBatis-Plus 3.4.2 default camelCase conversion
-- Note: createdUser → create_user, updateUser → update_user (explicit @TableField)


-- DeliveryOrder entity (@TableName("delivery_order"))
-- Columns: id, tenantId, platformOrderId, platform, dishSummary, amount,
--          userName, phone, address, status, orderTime, createdTime, updateTime,
--          createdUser(→create_user), updateUser(→update_user)
CREATE TABLE IF NOT EXISTS delivery_order (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  platform_order_id varchar(64) NULL DEFAULT NULL COMMENT '平台订单',
  platform varchar(20) NULL DEFAULT NULL COMMENT '配平',
  order_id bigint NULL DEFAULT NULL COMMENT '本地订单ID（关联 orders.id，可空）',
  dish_summary varchar(255) NULL DEFAULT NULL COMMENT '菜品摘',
  amount decimal(10,2) NULL DEFAULT NULL COMMENT '订单金',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户姓名',
  phone varchar(20) NULL DEFAULT NULL COMMENT 'ϵ绰',
  address varchar(255) NULL DEFAULT NULL COMMENT '配地',
  status varchar(20) NULL DEFAULT NULL COMMENT '订单状',
  order_time datetime NULL DEFAULT NULL COMMENT '下单时间',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  created_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version int NULL DEFAULT NULL COMMENT '乐锁版朏',
  PRIMARY KEY (id)
);

