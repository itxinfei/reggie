-- 后厨出餐 KDS 模块测试 schema（H2 / MySQL 兼容）
-- 依赖主 schema.sql 中的 orders、order_detail 表（测试类同时加载）。

-- ==================== 后厨厨房工单（带 tenant_id，受多租户隔离）====================
CREATE TABLE IF NOT EXISTS kitchen_ticket (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  tenant_id bigint DEFAULT NULL COMMENT '租户ID',
  order_id bigint NOT NULL COMMENT '来源订单ID（幂等）',
  order_no varchar(50) DEFAULT NULL COMMENT '取餐号/订单号快照',
  order_type varchar(20) DEFAULT NULL COMMENT '用餐类型快照',
  table_name varchar(32) DEFAULT NULL COMMENT '堂食桌台',
  customer_count int DEFAULT NULL COMMENT '用餐人数',
  status tinyint NOT NULL DEFAULT 1 COMMENT '状态：1待制作，2制作中，3待取餐，4已完成，5已取消',
  urgent tinyint NOT NULL DEFAULT 0 COMMENT '是否加急：0否，1是',
  dish_summary varchar(255) DEFAULT NULL COMMENT '菜品摘要快照',
  receive_time datetime DEFAULT NULL COMMENT '接单/生成时间',
  cook_start_time datetime DEFAULT NULL COMMENT '开始制作时间',
  ready_time datetime DEFAULT NULL COMMENT '叫号时间',
  finish_time datetime DEFAULT NULL COMMENT '出餐完成时间',
  cancel_time datetime DEFAULT NULL COMMENT '取消时间',
  cook_duration_seconds bigint DEFAULT NULL COMMENT '制作耗时（秒）',
  station_code varchar(20) DEFAULT NULL COMMENT '档口编码（如 HOT/COLD/DRINK/DESSERT）',
  remark varchar(255) DEFAULT NULL COMMENT '备注',
  create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  create_user bigint DEFAULT NULL COMMENT '创建人',
  update_user bigint DEFAULT NULL COMMENT '修改人',
  is_deleted int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id)
);

-- 清理测试残留数据
DELETE FROM kitchen_ticket WHERE tenant_id = 999;
