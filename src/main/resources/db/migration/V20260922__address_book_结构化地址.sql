-- =============================================================
-- V20260922__address_book_结构化地址.sql
-- -------------------------------------------------------------
-- 背景：
--   收货地址此前只有 省/市/区 + detail 大文本，街道、小区、楼栋、
--   单元、楼层、门牌号全挤在一句话里，骑手难以精确定位。
--   参照美团/淘宝的成熟做法，将"最后一公里"信息拆为独立字段入库，
--   detail 保留为后端按结构化字段规范化拼接的冗余完整地址
--   （订单快照/配送/打印等既有展示继续读 detail，无需改动）。
--
-- 新增列（位于 district_name 之后）：
--   street_name 街道/乡镇(文本,选填) varchar(50)
--   community   小区/大厦/学校       varchar(100)
--   building    楼栋                 varchar(25)  —— 20字输入+补后缀仍不超长
--   unit        单元                 varchar(25)
--   floor       楼层                 varchar(25)  —— 反引号包裹，规避函数名
--   room_no     门牌号               varchar(25)
-- 另将既有 detail 列由 varchar(200) 扩为 varchar(255)。
--
-- 注意：
--   - 本项目未引入 Flyway，db/migration 下脚本需手动执行
--   - 本脚本幂等，可重复执行
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

-- ---------- 幂等加列 ----------
CALL sp_add_col_if_missing('address_book', 'street_name',
    'ALTER TABLE `address_book` ADD COLUMN `street_name` varchar(50) DEFAULT NULL COMMENT ''街道/乡镇'' AFTER `district_name`');

CALL sp_add_col_if_missing('address_book', 'community',
    'ALTER TABLE `address_book` ADD COLUMN `community` varchar(100) DEFAULT NULL COMMENT ''小区/大厦/学校'' AFTER `street_name`');

CALL sp_add_col_if_missing('address_book', 'building',
    'ALTER TABLE `address_book` ADD COLUMN `building` varchar(25) DEFAULT NULL COMMENT ''楼栋'' AFTER `community`');

CALL sp_add_col_if_missing('address_book', 'unit',
    'ALTER TABLE `address_book` ADD COLUMN `unit` varchar(25) DEFAULT NULL COMMENT ''单元'' AFTER `building`');

CALL sp_add_col_if_missing('address_book', 'floor',
    'ALTER TABLE `address_book` ADD COLUMN `floor` varchar(25) DEFAULT NULL COMMENT ''楼层'' AFTER `unit`');

CALL sp_add_col_if_missing('address_book', 'room_no',
    'ALTER TABLE `address_book` ADD COLUMN `room_no` varchar(25) DEFAULT NULL COMMENT ''门牌号'' AFTER `floor`');

-- ---------- 清理临时存储过程 ----------
DROP PROCEDURE IF EXISTS `sp_add_col_if_missing`;

-- ---------- 扩展既有 detail 列至 255（幂等，重复执行无副作用）----------
ALTER TABLE `address_book`
    MODIFY COLUMN `detail` varchar(255) CHARACTER SET utf8mb4
    DEFAULT NULL COMMENT '详细地址';

-- ---------- 结果校验 ----------
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'address_book'
ORDER BY ORDINAL_POSITION;
