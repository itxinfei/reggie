-- 20260930: store_info 增加门店公告列（C 端首页 notice 展示，商家"店铺设置"可维护）
-- 关联计划：deliverables/product-strategy/cend-shop-plan-2026-09-30.md Phase 2-A（Task #7）
-- 说明：项目无 Flyway，本脚本为手工执行迁移；测试 schema（schema.sql / schema-mysql.sql）已同步。
--
-- 2026-09-30 起测试库 reggie_test 由 TestDbProvisioner 按结构指纹自动重建，
-- 【只需在开发库 reggie 执行本脚本】（测试库会随 schema.sql 自动带上新列）：

ALTER TABLE store_info
    ADD COLUMN notice varchar(200) NULL DEFAULT NULL COMMENT '门店公告，C端首页展示' AFTER business_hours;
