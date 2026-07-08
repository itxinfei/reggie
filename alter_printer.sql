-- ============================================================
-- 打印机模块数据库修复脚本
-- 适用：已有 reggie.sql 初始化过数据库的存量环境
-- 日期：2026-07-08
-- 说明：仅添加缺失的 system_printer_name 字段
--       printer_log 已有 tenant_id + idx_tenant，无需修复
-- ============================================================

-- 添加 system_printer_name 字段（Windows 系统打印机名称）
-- 已有此字段的库执行会忽略（MySQL 8.0.29+），MySQL 5.7 请手动检查后执行
ALTER TABLE printer_config 
  ADD COLUMN IF NOT EXISTS `system_printer_name` varchar(200) DEFAULT NULL 
  COMMENT '系统打印机名称（Windows下为驱动名称）' AFTER `device_id`;
