-- Payment module test schema (H2 compatible)

DROP TABLE IF EXISTS refund_record;
DROP TABLE IF EXISTS payment_order;

CREATE TABLE payment_order (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id bigint NOT NULL COMMENT '业务订单id',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  trade_no varchar(64) NOT NULL COMMENT '系统交易号',
  channel_trade_no varchar(128) NULL DEFAULT NULL COMMENT '通道交易号',
  channel varchar(20) NOT NULL COMMENT '支付通道',
  amount decimal(10,2) NOT NULL COMMENT '金额',
  status varchar(20) NOT NULL COMMENT '支付状态',
  paid_time datetime NULL DEFAULT NULL COMMENT '支付时间',
  notify_time datetime NULL DEFAULT NULL COMMENT '通知时间',
  created_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NOT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

CREATE TABLE refund_record (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  payment_order_id bigint NOT NULL COMMENT '支付订单ID',
  tenant_id bigint NULL DEFAULT NULL COMMENT '租户id',
  refund_no varchar(64) NOT NULL COMMENT '退款流水号',
  amount decimal(10,2) NOT NULL COMMENT '退款金额',
  reason varchar(200) NULL DEFAULT NULL COMMENT '退款原因',
  status varchar(20) NOT NULL COMMENT '退款状态',
  created_time datetime NOT NULL COMMENT '创建时间',
  update_time datetime NULL DEFAULT NULL COMMENT '更新时间',
  create_user bigint NULL DEFAULT NULL COMMENT '创建人ID',
  update_user bigint NULL DEFAULT NULL COMMENT '更新人ID',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
);

CREATE INDEX idx_payment_order_tenant ON payment_order (tenant_id);
CREATE INDEX idx_payment_order_order ON payment_order (order_id);
CREATE INDEX idx_payment_order_trade ON payment_order (trade_no);
CREATE INDEX idx_refund_record_payment ON refund_record (payment_order_id);
CREATE INDEX idx_refund_record_tenant ON refund_record (tenant_id);
