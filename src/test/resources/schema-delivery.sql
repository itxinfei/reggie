-- Delivery module test schema (H2 compatible)
-- Matches entity column names from MyBatis-Plus 3.4.2 default camelCase conversion
-- Note: createdUser → create_user, updatedUser → update_user (explicit @TableField)

DROP TABLE IF EXISTS delivery_track;
DROP TABLE IF EXISTS delivery_order;

-- DeliveryOrder entity (@TableName("delivery_order"))
-- Columns: id, tenantId, platformOrderId, platform, dishSummary, amount,
--          userName, phone, address, status, orderTime, createdTime, updatedTime,
--          createdUser(→create_user), updatedUser(→update_user)
CREATE TABLE delivery_order (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  platform_order_id varchar(64) NULL DEFAULT NULL COMMENT '平台订单号',
  platform varchar(20) NULL DEFAULT NULL COMMENT '配送平台',
  dish_summary varchar(255) NULL DEFAULT NULL COMMENT '菜品摘要',
  amount decimal(10,2) NULL DEFAULT NULL COMMENT '订单金额',
  user_name varchar(50) NULL DEFAULT NULL COMMENT '用户姓名',
  phone varchar(20) NULL DEFAULT NULL COMMENT '联系电话',
  address varchar(255) NULL DEFAULT NULL COMMENT '配送地址',
  status varchar(20) NULL DEFAULT NULL COMMENT '订单状态',
  order_time datetime NULL DEFAULT NULL COMMENT '下单时间',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  updated_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (id)
);

-- DeliveryTrack entity (@TableName("delivery_track"))
-- Columns: id, tenantId, orderId, status, location, remark, createdTime
CREATE TABLE delivery_track (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  order_id bigint NULL DEFAULT NULL COMMENT '配送订单ID',
  status varchar(20) NULL DEFAULT NULL COMMENT '状态',
  location varchar(100) NULL DEFAULT NULL COMMENT '位置',
  remark varchar(200) NULL DEFAULT NULL COMMENT '备注',
  created_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id)
);

CREATE INDEX idx_delivery_order_tenant_id ON delivery_order(tenant_id);
CREATE INDEX idx_delivery_order_platform_order_id ON delivery_order(platform_order_id);
CREATE INDEX idx_delivery_track_tenant_id ON delivery_track(tenant_id);
CREATE INDEX idx_delivery_track_order_id ON delivery_track(order_id);
