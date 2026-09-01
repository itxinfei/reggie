-- 外卖配送增强：地址簿补经纬度列 + 订单补配送费列
-- 背景：
--   1. AddressBook 实体已添加 longitude/latitude（GCJ-02 高德坐标系），用于配送范围匹配；
--      历史建表脚本与本地库均缺失这两列，新地址保存时字段不会被持久化。
--   2. Orders 实体已添加 delivery_fee 字段（外卖单配送费单独存储，堂食为 null）；
--      历史建表脚本与本地库均缺失此列，新订单配送费字段不会被持久化。
-- 降级策略：Key 未配置时经纬度为空，下单时按门店坐标兜底校验；两者均为空时不阻断下单。
-- 已有数据：address_book.longitude/latitude 默认 NULL（需重新保存地址触发 GeoUtils 回填）；
--           orders.delivery_fee 默认 NULL（历史订单无配送费概念，兼容旧数据）。

-- 1. address_book：新增 longitude（经度）、latitude（纬度）列
ALTER TABLE `address_book`
  ADD COLUMN `longitude` decimal(10,6) NULL DEFAULT NULL COMMENT '经度（高德坐标系 GCJ-02）' AFTER `label`,
  ADD COLUMN `latitude`  decimal(10,6) NULL DEFAULT NULL COMMENT '纬度（高德坐标系 GCJ-02）' AFTER `longitude`;

-- 2. orders：新增 delivery_fee 列（配送费，单独存储便于财务对账）
ALTER TABLE `orders`
  ADD COLUMN `delivery_fee` decimal(10,2) NULL DEFAULT NULL COMMENT '配费（卖单配费，堂食为 null' AFTER `amount`;
