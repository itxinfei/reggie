-- ============================================================================
-- 标识符大小写统一脚本（库：reggie）
--
-- 背景：生产部署在 Linux，MySQL 的 lower_case_table_names=0 让**表名严格区分大小写**；
--       列名 MySQL 恒不区分，但混用会带来两类实际问题：
--       ① SELECT * 走 selectMaps / resultType=Map 时，返回的 key 就是建表时的大小写，
--          Java/前端按小写取值会拿到 null；
--       ② 同一套 SQL 迁移到区分大小写的引擎（PG 等）直接报错，且人工维护极易写错。
--
-- 现状（DbAuditProbe 只读扫描 target/db-ddl-dev.sql，117 张表 / 1724 列）：
--   · 表名全部小写，代码 SQL 里的表名引用 0 处大小写不一致 —— Linux 侧安全
--   · 16 张表共 190 个列名是全大写（dish_material / material / supplier / purchase_order /
--     stock_* / group_buy_* / member_level / price_history / withdrawal_* / material_category /
--     supplier_settlement / dish_material）
--
-- 下面每条 ALTER 由 information_schema 原样重建（类型 / 可空 / 默认值 / extra /
-- 列级字符集与排序规则 / 注释），只改标识符大小写，不动数据、不动语义。
--
-- 执行前务必备份：mysqldump -uroot -p reggie > reggie-backup-$(date +%F).sql
-- 只针对演示库 reggie。测试基线 src/test/resources/schema-mysql.sql 的列名已是全小写，
-- 本脚本正是把演示库拉齐到那个口径（两库结构一致，才不会出现"测试绿、演示库炸"）。
-- ============================================================================

USE reggie;



-- dish_material（11 列）
ALTER TABLE `dish_material` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `dish_material` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `dish_material` CHANGE COLUMN `DISH_ID` `dish_id` bigint NULL DEFAULT NULL COMMENT '鑿滃搧ID';
ALTER TABLE `dish_material` CHANGE COLUMN `MATERIAL_ID` `material_id` bigint NULL DEFAULT NULL COMMENT '椋熸潗ID';
ALTER TABLE `dish_material` CHANGE COLUMN `USAGE_QTY` `usage_qty` decimal(10,3) NULL DEFAULT NULL COMMENT '鍗曚唤鑿滃搧娑堣�楅鏉愭暟閲�';
ALTER TABLE `dish_material` CHANGE COLUMN `SORT` `sort` int NULL DEFAULT 0 COMMENT '鎺掑簭';
ALTER TABLE `dish_material` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `dish_material` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `dish_material` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `dish_material` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `dish_material` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';

-- group_buy_campaign（18 列）
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '绉熸埛ID';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `NAME` `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '娲诲姩鍚嶇О';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `DESCRIPTION` `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '娲诲姩鎻忚堪';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `GROUP_ID` `group_id` bigint NOT NULL DEFAULT 0 COMMENT '鎷煎洟缁処D';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `STATUS` `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OPEN' COMMENT '鐘舵�侊細OPEN/CLOSED/ENDED';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `START_TIME` `start_time` datetime NOT NULL COMMENT '寮�濮嬫椂闂�';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `END_TIME` `end_time` datetime NOT NULL COMMENT '缁撴潫鏃堕棿';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `MIN_MEMBERS` `min_members` int NOT NULL DEFAULT 2 COMMENT '鏈�灏戞垚鍥汉鏁�';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `MAX_MEMBERS` `max_members` int NOT NULL DEFAULT 10 COMMENT '鏈�澶氭垚鍥汉鏁�';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `ORIGINAL_PRICE` `original_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '鍘熶环';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `GROUP_PRICE` `group_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '鎷煎洟浠�';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `DISH_ID` `dish_id` bigint NOT NULL DEFAULT 0 COMMENT '鑿滃搧ID';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `DISH_NAME` `dish_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '鑿滃搧鍚嶇О';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `IMAGE` `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '娲诲姩鍥剧墖URL';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `group_buy_campaign` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎锛�0=鏈垹闄わ紝1=宸插垹闄�';

-- group_buy_participation（9 列）
ALTER TABLE `group_buy_participation` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `group_buy_participation` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '绉熸埛ID';
ALTER TABLE `group_buy_participation` CHANGE COLUMN `GROUP_BUY_ID` `group_buy_id` bigint NOT NULL COMMENT '鎷煎洟娲诲姩ID';
ALTER TABLE `group_buy_participation` CHANGE COLUMN `ORDER_ID` `order_id` bigint NOT NULL COMMENT '璁㈠崟ID';
ALTER TABLE `group_buy_participation` CHANGE COLUMN `USER_ID` `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID';
ALTER TABLE `group_buy_participation` CHANGE COLUMN `STATUS` `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'JOINED' COMMENT '鐘舵�侊細JOINED/PAID/CANCELLED';
ALTER TABLE `group_buy_participation` CHANGE COLUMN `JOIN_TIME` `join_time` datetime NOT NULL COMMENT '鍙傚洟鏃堕棿';
ALTER TABLE `group_buy_participation` CHANGE COLUMN `PAY_TIME` `pay_time` datetime NULL DEFAULT NULL COMMENT '鏀粯鏃堕棿';
ALTER TABLE `group_buy_participation` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';

-- material（16 列）
ALTER TABLE `material` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `material` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `material` CHANGE COLUMN `CATEGORY_ID` `category_id` bigint NULL DEFAULT NULL COMMENT '鍒嗙被ID';
ALTER TABLE `material` CHANGE COLUMN `NAME` `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鐗╂枡鍚嶇О';
ALTER TABLE `material` CHANGE COLUMN `UNIT` `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍗曚綅';
ALTER TABLE `material` CHANGE COLUMN `STOCK_QTY` `stock_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '搴撳瓨鏁伴噺';
ALTER TABLE `material` CHANGE COLUMN `MIN_STOCK` `min_stock` decimal(10,2) NULL DEFAULT NULL COMMENT '鏈�灏忓簱瀛�';
ALTER TABLE `material` CHANGE COLUMN `UNIT_PRICE` `unit_price` decimal(10,2) NULL DEFAULT NULL COMMENT '鍗曚环';
ALTER TABLE `material` CHANGE COLUMN `SUPPLIER_ID` `supplier_id` bigint NULL DEFAULT NULL COMMENT '渚涘簲鍟咺D';
ALTER TABLE `material` CHANGE COLUMN `BARCODE` `barcode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏉″舰鐮�';
ALTER TABLE `material` CHANGE COLUMN `STATUS` `status` int NULL DEFAULT NULL COMMENT '鐘舵�侊紙1-姝ｅ父锛�0-绂佺敤锛�';
ALTER TABLE `material` CHANGE COLUMN `CREATED_TIME` `created_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `material` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `material` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `material` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `material` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';

-- material_category（9 列）
ALTER TABLE `material_category` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `material_category` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `material_category` CHANGE COLUMN `NAME` `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍒嗙被鍚嶇О';
ALTER TABLE `material_category` CHANGE COLUMN `SORT` `sort` int NULL DEFAULT NULL COMMENT '鎺掑簭';
ALTER TABLE `material_category` CHANGE COLUMN `CREATED_TIME` `created_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `material_category` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `material_category` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `material_category` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `material_category` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';

-- member_level（2 列）
ALTER TABLE `member_level` CHANGE COLUMN `MIN_POINTS` `min_points` bigint NULL DEFAULT 0 COMMENT '鏈�浣庣Н鍒嗚姹�';
ALTER TABLE `member_level` CHANGE COLUMN `MAX_POINTS` `max_points` bigint NULL DEFAULT NULL COMMENT '鏈�楂樼Н鍒嗕笂闄�';

-- price_history（8 列）
ALTER TABLE `price_history` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `price_history` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `price_history` CHANGE COLUMN `MATERIAL_ID` `material_id` bigint NOT NULL COMMENT '鐗╂枡ID';
ALTER TABLE `price_history` CHANGE COLUMN `OLD_PRICE` `old_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '鏃т环鏍�';
ALTER TABLE `price_history` CHANGE COLUMN `NEW_PRICE` `new_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '鏂颁环鏍�';
ALTER TABLE `price_history` CHANGE COLUMN `CHANGE_REASON` `change_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '鍙樺姩鍘熷洜';
ALTER TABLE `price_history` CHANGE COLUMN `OPERATOR_ID` `operator_id` bigint NOT NULL DEFAULT 0 COMMENT '鎿嶄綔浜篒D';
ALTER TABLE `price_history` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';

-- purchase_order（15 列）
ALTER TABLE `purchase_order` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `purchase_order` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `purchase_order` CHANGE COLUMN `ORDER_NO` `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璁㈠崟缂栧彿';
ALTER TABLE `purchase_order` CHANGE COLUMN `SUPPLIER_ID` `supplier_id` bigint NULL DEFAULT NULL COMMENT '渚涘簲鍟咺D';
ALTER TABLE `purchase_order` CHANGE COLUMN `TOTAL_AMOUNT` `total_amount` decimal(10,2) NULL DEFAULT NULL COMMENT '鎬婚噾棰�';
ALTER TABLE `purchase_order` CHANGE COLUMN `STATUS` `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鐘舵��';
ALTER TABLE `purchase_order` CHANGE COLUMN `OPERATOR` `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔鍛�';
ALTER TABLE `purchase_order` CHANGE COLUMN `REMARK` `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞';
ALTER TABLE `purchase_order` CHANGE COLUMN `VOUCHER_IMAGES` `voucher_images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍑瘉鍥剧墖锛堥�楀彿鍒嗛殧锛屾渶澶�5寮狅級';
ALTER TABLE `purchase_order` CHANGE COLUMN `CREATED_TIME` `created_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `purchase_order` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `purchase_order` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `purchase_order` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `purchase_order` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';
ALTER TABLE `purchase_order` CHANGE COLUMN `VERSION` `version` int NOT NULL DEFAULT 0 COMMENT '止姹�';

-- purchase_order_detail（14 列）
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `PURCHASE_ORDER_ID` `purchase_order_id` bigint NULL DEFAULT NULL COMMENT '閲囪喘璁㈠崟ID';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `MATERIAL_ID` `material_id` bigint NULL DEFAULT NULL COMMENT '鐗╂枡ID';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `QTY` `qty` decimal(10,2) NULL DEFAULT NULL COMMENT '鏁伴噺';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `UNIT_PRICE` `unit_price` decimal(10,2) NULL DEFAULT NULL COMMENT '鍗曚环';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `AMOUNT` `amount` decimal(10,2) NULL DEFAULT NULL COMMENT '閲戦';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `RECEIVED_QTY` `received_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '鏀惰揣鏁伴噺';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `REMARK` `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `purchase_order_detail` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';

-- stock_check（13 列）
ALTER TABLE `stock_check` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `stock_check` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `stock_check` CHANGE COLUMN `CHECK_NO` `check_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鐩樼偣鍗曞彿';
ALTER TABLE `stock_check` CHANGE COLUMN `STATUS` `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鐘舵��';
ALTER TABLE `stock_check` CHANGE COLUMN `TOTAL_DIFF_AMOUNT` `total_diff_amount` decimal(10,2) NULL DEFAULT NULL COMMENT '鎬诲樊寮傞噾棰�';
ALTER TABLE `stock_check` CHANGE COLUMN `OPERATOR` `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔鍛�';
ALTER TABLE `stock_check` CHANGE COLUMN `REMARK` `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞';
ALTER TABLE `stock_check` CHANGE COLUMN `VOUCHER_IMAGES` `voucher_images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍑瘉鍥剧墖锛堥�楀彿鍒嗛殧锛屾渶澶�5寮狅級';
ALTER TABLE `stock_check` CHANGE COLUMN `CREATED_TIME` `created_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `stock_check` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `stock_check` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `stock_check` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `stock_check` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';

-- stock_check_detail（13 列）
ALTER TABLE `stock_check_detail` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `CHECK_ID` `check_id` bigint NULL DEFAULT NULL COMMENT '鐩樼偣ID';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `MATERIAL_ID` `material_id` bigint NULL DEFAULT NULL COMMENT '鐗╂枡ID';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `BOOK_QTY` `book_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '璐﹂潰鏁伴噺';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `ACTUAL_QTY` `actual_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '瀹為檯鏁伴噺';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `DIFF_QTY` `diff_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '宸紓鏁伴噺';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `REMARK` `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `stock_check_detail` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';

-- stock_record（16 列）
ALTER TABLE `stock_record` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `stock_record` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `stock_record` CHANGE COLUMN `MATERIAL_ID` `material_id` bigint NULL DEFAULT NULL COMMENT '鐗╂枡ID';
ALTER TABLE `stock_record` CHANGE COLUMN `TYPE` `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绫诲瀷';
ALTER TABLE `stock_record` CHANGE COLUMN `QTY` `qty` decimal(10,2) NULL DEFAULT NULL COMMENT '鏁伴噺';
ALTER TABLE `stock_record` CHANGE COLUMN `UNIT_PRICE` `unit_price` decimal(10,2) NULL DEFAULT NULL COMMENT '鍗曚环';
ALTER TABLE `stock_record` CHANGE COLUMN `TOTAL_AMOUNT` `total_amount` decimal(10,2) NULL DEFAULT NULL COMMENT '鎬婚噾棰�';
ALTER TABLE `stock_record` CHANGE COLUMN `BIZ_ID` `biz_id` bigint NULL DEFAULT NULL COMMENT '涓氬姟ID';
ALTER TABLE `stock_record` CHANGE COLUMN `REMARK` `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞';
ALTER TABLE `stock_record` CHANGE COLUMN `VOUCHER_IMAGES` `voucher_images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍑瘉鍥剧墖锛堥�楀彿鍒嗛殧锛屾渶澶�5寮狅級';
ALTER TABLE `stock_record` CHANGE COLUMN `OPERATOR` `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔鍛�';
ALTER TABLE `stock_record` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `stock_record` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `stock_record` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `stock_record` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `stock_record` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';

-- supplier（13 列）
ALTER TABLE `supplier` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `supplier` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `supplier` CHANGE COLUMN `NAME` `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渚涘簲鍟嗗悕绉�';
ALTER TABLE `supplier` CHANGE COLUMN `CONTACT` `contact` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鑱旂郴浜�';
ALTER TABLE `supplier` CHANGE COLUMN `PHONE` `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '系缁�';
ALTER TABLE `supplier` CHANGE COLUMN `ADDRESS` `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍦板潃';
ALTER TABLE `supplier` CHANGE COLUMN `LICENSE_IMAGES` `license_images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璧勮川鍥剧墖锛堥�楀彿鍒嗛殧锛屾渶澶�5寮狅級';
ALTER TABLE `supplier` CHANGE COLUMN `STATUS` `status` int NULL DEFAULT NULL COMMENT '鐘舵��';
ALTER TABLE `supplier` CHANGE COLUMN `CREATED_TIME` `created_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `supplier` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `supplier` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `supplier` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `supplier` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';

-- supplier_settlement（12 列）
ALTER TABLE `supplier_settlement` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NULL DEFAULT NULL COMMENT '绉熸埛ID';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `SUPPLIER_ID` `supplier_id` bigint NOT NULL COMMENT '渚涘簲鍟咺D';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `PERIOD` `period` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '缁撶畻鍛ㄦ湡';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `TOTAL_AMOUNT` `total_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '鎬婚噾棰�';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `PAID_AMOUNT` `paid_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '宸蹭粯閲戦';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `STATUS` `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '鐘舵��';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `UPDATE_TIME` `update_time` datetime NULL DEFAULT NULL COMMENT '鏇存柊鏃堕棿';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `CREATE_USER` `create_user` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `UPDATE_USER` `update_user` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D';
ALTER TABLE `supplier_settlement` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎';

-- withdrawal_record（8 列）
ALTER TABLE `withdrawal_record` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `withdrawal_record` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '绉熸埛ID';
ALTER TABLE `withdrawal_record` CHANGE COLUMN `WITHDRAWAL_ID` `withdrawal_id` bigint NOT NULL COMMENT '鎻愮幇鐢宠ID';
ALTER TABLE `withdrawal_record` CHANGE COLUMN `ACTUAL_AMOUNT` `actual_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '瀹為檯鍒拌处閲戦';
ALTER TABLE `withdrawal_record` CHANGE COLUMN `FEE` `fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '鎵嬬画璐�';
ALTER TABLE `withdrawal_record` CHANGE COLUMN `TRANSFER_TIME` `transfer_time` datetime NULL DEFAULT NULL COMMENT '杞处鏃堕棿';
ALTER TABLE `withdrawal_record` CHANGE COLUMN `BANK_TRACE_NO` `bank_trace_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '閾惰娴佹按鍙�';
ALTER TABLE `withdrawal_record` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';

-- withdrawal_request（13 列）
ALTER TABLE `withdrawal_request` CHANGE COLUMN `ID` `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `TENANT_ID` `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '绉熸埛ID';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `USER_ID` `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `AMOUNT` `amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '鎻愮幇閲戦';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `BANK_NAME` `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '閾惰鍚嶇О';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `ACCOUNT_NAME` `account_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '寮�鎴蜂汉濮撳悕';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `ACCOUNT_NUMBER` `account_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '閾惰璐﹀彿';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `STATUS` `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '鐘舵�侊細PENDING/APPROVED/REJECTED/TRANSFERRED';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `REJECT_REASON` `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '鎷掔粷鍘熷洜';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `CREATE_TIME` `create_time` datetime NULL DEFAULT NULL COMMENT '鍒涘缓鏃堕棿';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `APPROVE_TIME` `approve_time` datetime NULL DEFAULT NULL COMMENT '瀹℃壒鏃堕棿';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `APPROVE_USER_ID` `approve_user_id` bigint NULL DEFAULT NULL COMMENT '瀹℃壒浜篒D';
ALTER TABLE `withdrawal_request` CHANGE COLUMN `IS_DELETED` `is_deleted` int NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎锛�0=鏈垹闄わ紝1=宸插垹闄�';

-- ----------------------------------------------------------------------------
-- 复验：执行完应返回 0
-- ----------------------------------------------------------------------------
SELECT COUNT(1) AS 仍含大写的列数
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND STRCMP(column_name, LOWER(column_name)) <> 0;
