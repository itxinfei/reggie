-- =============================================================
-- V20260904__region_补齐行政区划字段.sql
-- -------------------------------------------------------------
-- 背景：
--   Region 实体（com.reggie.module.region.model.Region）映射了
--   code / parent_id / level / create_user / update_user 五个字段，
--   但线上 region 表仍是早期精简版（只有 id/name/sort/create_time/
--   update_time/is_deleted），导致 /region/tree、/region/options 等
--   接口全部报错：
--     Unknown column 'code' in 'field list'
--
-- 处理：
--   1. 补齐缺失的 5 个列（与 src/test/resources/schema-mysql.sql 中
--      region 表的权威定义保持一致）
--   2. 补齐 idx_parent_id / idx_level 两个索引
--   3. 回填存量数据：既有 10 条「大区」数据（华北区、东北区…）无层级
--      信息，统一按「一级、顶级节点」处理（level=1、parent_id=0），
--      保证 getRegionTree() 能正常挂载为根节点
--
-- 注意：
--   - 本项目未引入 Flyway，db/migration 下脚本需手动执行
--   - 本脚本幂等，可重复执行（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，
--     故用存储过程查询 information_schema 判断后再执行）
-- =============================================================

USE `reggie`;

-- ---------- 幂等加列工具存储过程 ----------
DROP PROCEDURE IF EXISTS `sp_add_col_if_missing`;
DELIMITER $$
CREATE PROCEDURE `sp_add_col_if_missing`(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_ddl    TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND COLUMN_NAME  = p_column
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[ADD ] ', p_table, '.', p_column) AS migrate_result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_column, ' (exists)') AS migrate_result;
    END IF;
END$$
DELIMITER ;

-- ---------- 幂等加索引工具存储过程 ----------
DROP PROCEDURE IF EXISTS `sp_add_idx_if_missing`;
DELIMITER $$
CREATE PROCEDURE `sp_add_idx_if_missing`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_ddl   TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND INDEX_NAME   = p_index
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[ADD ] ', p_table, '.', p_index) AS migrate_result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index, ' (exists)') AS migrate_result;
    END IF;
END$$
DELIMITER ;

-- ---------- 补齐缺失列 ----------
CALL sp_add_col_if_missing('region', 'code',
    'ALTER TABLE `region` ADD COLUMN `code` varchar(20) DEFAULT NULL COMMENT ''行政区划代码'' AFTER `name`');

CALL sp_add_col_if_missing('region', 'parent_id',
    'ALTER TABLE `region` ADD COLUMN `parent_id` bigint NOT NULL DEFAULT 0 COMMENT ''父级ID，0为省份'' AFTER `code`');

CALL sp_add_col_if_missing('region', 'level',
    'ALTER TABLE `region` ADD COLUMN `level` tinyint NOT NULL DEFAULT 1 COMMENT ''层级：1省 2市 3区/县'' AFTER `parent_id`');

CALL sp_add_col_if_missing('region', 'create_user',
    'ALTER TABLE `region` ADD COLUMN `create_user` bigint DEFAULT NULL COMMENT ''创建人'' AFTER `update_time`');

CALL sp_add_col_if_missing('region', 'update_user',
    'ALTER TABLE `region` ADD COLUMN `update_user` bigint DEFAULT NULL COMMENT ''修改人'' AFTER `create_user`');

-- ---------- 补齐索引 ----------
CALL sp_add_idx_if_missing('region', 'idx_parent_id',
    'ALTER TABLE `region` ADD KEY `idx_parent_id` (`parent_id`)');

CALL sp_add_idx_if_missing('region', 'idx_level',
    'ALTER TABLE `region` ADD KEY `idx_level` (`level`)');

-- ---------- 回填存量数据 ----------
-- 既有「大区」数据无层级信息，统一置为一级顶级节点，避免树形接口漏数据
UPDATE `region` SET `parent_id` = 0 WHERE `parent_id` IS NULL;
UPDATE `region` SET `level` = 1 WHERE `level` IS NULL OR `level` = 0;

-- ---------- 清理临时存储过程 ----------
DROP PROCEDURE IF EXISTS `sp_add_col_if_missing`;
DROP PROCEDURE IF EXISTS `sp_add_idx_if_missing`;

-- ---------- 结果校验 ----------
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'region'
ORDER BY ORDINAL_POSITION;
