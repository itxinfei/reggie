-- 大写列名归一化为小写（MySQL 列名本就不区分大小写，此为规范统一，不改变任何数据与类型）
-- 幂等：仅当列确实为大写形态、且目标小写名不存在时才 RENAME，可重复执行；自动保留索引。
-- 适用 MySQL 8.0；执行前建议备份。

DROP PROCEDURE IF EXISTS rename_col_ci;
DELIMITER $$
CREATE PROCEDURE rename_col_ci(IN p_tbl VARCHAR(64), IN p_old VARCHAR(64), IN p_new VARCHAR(64))
BEGIN
  IF BINARY p_old <> BINARY p_new
     AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_tbl
                   AND COLUMN_NAME = p_old AND BINARY COLUMN_NAME = p_old)
     AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_tbl
                   AND BINARY COLUMN_NAME = p_new) THEN
    SET @s = CONCAT('ALTER TABLE `', p_tbl, '` RENAME COLUMN `', p_old, '` TO `', p_new, '`');
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$
DELIMITER ;

-- dish_material（11 列）
CALL rename_col_ci('dish_material', 'ID', 'id');
CALL rename_col_ci('dish_material', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('dish_material', 'DISH_ID', 'dish_id');
CALL rename_col_ci('dish_material', 'MATERIAL_ID', 'material_id');
CALL rename_col_ci('dish_material', 'USAGE_QTY', 'usage_qty');
CALL rename_col_ci('dish_material', 'SORT', 'sort');
CALL rename_col_ci('dish_material', 'CREATE_TIME', 'create_time');
CALL rename_col_ci('dish_material', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('dish_material', 'CREATE_USER', 'create_user');
CALL rename_col_ci('dish_material', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('dish_material', 'IS_DELETED', 'is_deleted');

-- group_buy_campaign（18 列）
CALL rename_col_ci('group_buy_campaign', 'ID', 'id');
CALL rename_col_ci('group_buy_campaign', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('group_buy_campaign', 'NAME', 'name');
CALL rename_col_ci('group_buy_campaign', 'DESCRIPTION', 'description');
CALL rename_col_ci('group_buy_campaign', 'GROUP_ID', 'group_id');
CALL rename_col_ci('group_buy_campaign', 'STATUS', 'status');
CALL rename_col_ci('group_buy_campaign', 'START_TIME', 'start_time');
CALL rename_col_ci('group_buy_campaign', 'END_TIME', 'end_time');
CALL rename_col_ci('group_buy_campaign', 'MIN_MEMBERS', 'min_members');
CALL rename_col_ci('group_buy_campaign', 'MAX_MEMBERS', 'max_members');
CALL rename_col_ci('group_buy_campaign', 'ORIGINAL_PRICE', 'original_price');
CALL rename_col_ci('group_buy_campaign', 'GROUP_PRICE', 'group_price');
CALL rename_col_ci('group_buy_campaign', 'DISH_ID', 'dish_id');
CALL rename_col_ci('group_buy_campaign', 'DISH_NAME', 'dish_name');
CALL rename_col_ci('group_buy_campaign', 'IMAGE', 'image');
CALL rename_col_ci('group_buy_campaign', 'CREATE_TIME', 'create_time');
CALL rename_col_ci('group_buy_campaign', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('group_buy_campaign', 'IS_DELETED', 'is_deleted');

-- group_buy_participation（9 列）
CALL rename_col_ci('group_buy_participation', 'ID', 'id');
CALL rename_col_ci('group_buy_participation', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('group_buy_participation', 'GROUP_BUY_ID', 'group_buy_id');
CALL rename_col_ci('group_buy_participation', 'ORDER_ID', 'order_id');
CALL rename_col_ci('group_buy_participation', 'USER_ID', 'user_id');
CALL rename_col_ci('group_buy_participation', 'STATUS', 'status');
CALL rename_col_ci('group_buy_participation', 'JOIN_TIME', 'join_time');
CALL rename_col_ci('group_buy_participation', 'PAY_TIME', 'pay_time');
CALL rename_col_ci('group_buy_participation', 'CREATE_TIME', 'create_time');

-- material（16 列）
CALL rename_col_ci('material', 'ID', 'id');
CALL rename_col_ci('material', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('material', 'CATEGORY_ID', 'category_id');
CALL rename_col_ci('material', 'NAME', 'name');
CALL rename_col_ci('material', 'UNIT', 'unit');
CALL rename_col_ci('material', 'STOCK_QTY', 'stock_qty');
CALL rename_col_ci('material', 'MIN_STOCK', 'min_stock');
CALL rename_col_ci('material', 'UNIT_PRICE', 'unit_price');
CALL rename_col_ci('material', 'SUPPLIER_ID', 'supplier_id');
CALL rename_col_ci('material', 'BARCODE', 'barcode');
CALL rename_col_ci('material', 'STATUS', 'status');
CALL rename_col_ci('material', 'CREATED_TIME', 'created_time');
CALL rename_col_ci('material', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('material', 'CREATE_USER', 'create_user');
CALL rename_col_ci('material', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('material', 'IS_DELETED', 'is_deleted');

-- material_category（9 列）
CALL rename_col_ci('material_category', 'ID', 'id');
CALL rename_col_ci('material_category', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('material_category', 'NAME', 'name');
CALL rename_col_ci('material_category', 'SORT', 'sort');
CALL rename_col_ci('material_category', 'CREATED_TIME', 'created_time');
CALL rename_col_ci('material_category', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('material_category', 'CREATE_USER', 'create_user');
CALL rename_col_ci('material_category', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('material_category', 'IS_DELETED', 'is_deleted');

-- member_level（2 列）
CALL rename_col_ci('member_level', 'MIN_POINTS', 'min_points');
CALL rename_col_ci('member_level', 'MAX_POINTS', 'max_points');

-- price_history（8 列）
CALL rename_col_ci('price_history', 'ID', 'id');
CALL rename_col_ci('price_history', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('price_history', 'MATERIAL_ID', 'material_id');
CALL rename_col_ci('price_history', 'OLD_PRICE', 'old_price');
CALL rename_col_ci('price_history', 'NEW_PRICE', 'new_price');
CALL rename_col_ci('price_history', 'CHANGE_REASON', 'change_reason');
CALL rename_col_ci('price_history', 'OPERATOR_ID', 'operator_id');
CALL rename_col_ci('price_history', 'CREATE_TIME', 'create_time');

-- purchase_order（15 列）
CALL rename_col_ci('purchase_order', 'ID', 'id');
CALL rename_col_ci('purchase_order', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('purchase_order', 'ORDER_NO', 'order_no');
CALL rename_col_ci('purchase_order', 'SUPPLIER_ID', 'supplier_id');
CALL rename_col_ci('purchase_order', 'TOTAL_AMOUNT', 'total_amount');
CALL rename_col_ci('purchase_order', 'STATUS', 'status');
CALL rename_col_ci('purchase_order', 'OPERATOR', 'operator');
CALL rename_col_ci('purchase_order', 'REMARK', 'remark');
CALL rename_col_ci('purchase_order', 'VOUCHER_IMAGES', 'voucher_images');
CALL rename_col_ci('purchase_order', 'CREATED_TIME', 'created_time');
CALL rename_col_ci('purchase_order', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('purchase_order', 'CREATE_USER', 'create_user');
CALL rename_col_ci('purchase_order', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('purchase_order', 'IS_DELETED', 'is_deleted');
CALL rename_col_ci('purchase_order', 'VERSION', 'version');

-- purchase_order_detail（14 列）
CALL rename_col_ci('purchase_order_detail', 'ID', 'id');
CALL rename_col_ci('purchase_order_detail', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('purchase_order_detail', 'PURCHASE_ORDER_ID', 'purchase_order_id');
CALL rename_col_ci('purchase_order_detail', 'MATERIAL_ID', 'material_id');
CALL rename_col_ci('purchase_order_detail', 'QTY', 'qty');
CALL rename_col_ci('purchase_order_detail', 'UNIT_PRICE', 'unit_price');
CALL rename_col_ci('purchase_order_detail', 'AMOUNT', 'amount');
CALL rename_col_ci('purchase_order_detail', 'RECEIVED_QTY', 'received_qty');
CALL rename_col_ci('purchase_order_detail', 'REMARK', 'remark');
CALL rename_col_ci('purchase_order_detail', 'CREATE_TIME', 'create_time');
CALL rename_col_ci('purchase_order_detail', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('purchase_order_detail', 'CREATE_USER', 'create_user');
CALL rename_col_ci('purchase_order_detail', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('purchase_order_detail', 'IS_DELETED', 'is_deleted');

-- stock_check（13 列）
CALL rename_col_ci('stock_check', 'ID', 'id');
CALL rename_col_ci('stock_check', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('stock_check', 'CHECK_NO', 'check_no');
CALL rename_col_ci('stock_check', 'STATUS', 'status');
CALL rename_col_ci('stock_check', 'TOTAL_DIFF_AMOUNT', 'total_diff_amount');
CALL rename_col_ci('stock_check', 'OPERATOR', 'operator');
CALL rename_col_ci('stock_check', 'REMARK', 'remark');
CALL rename_col_ci('stock_check', 'VOUCHER_IMAGES', 'voucher_images');
CALL rename_col_ci('stock_check', 'CREATED_TIME', 'created_time');
CALL rename_col_ci('stock_check', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('stock_check', 'CREATE_USER', 'create_user');
CALL rename_col_ci('stock_check', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('stock_check', 'IS_DELETED', 'is_deleted');

-- stock_check_detail（13 列）
CALL rename_col_ci('stock_check_detail', 'ID', 'id');
CALL rename_col_ci('stock_check_detail', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('stock_check_detail', 'CHECK_ID', 'check_id');
CALL rename_col_ci('stock_check_detail', 'MATERIAL_ID', 'material_id');
CALL rename_col_ci('stock_check_detail', 'BOOK_QTY', 'book_qty');
CALL rename_col_ci('stock_check_detail', 'ACTUAL_QTY', 'actual_qty');
CALL rename_col_ci('stock_check_detail', 'DIFF_QTY', 'diff_qty');
CALL rename_col_ci('stock_check_detail', 'REMARK', 'remark');
CALL rename_col_ci('stock_check_detail', 'CREATE_TIME', 'create_time');
CALL rename_col_ci('stock_check_detail', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('stock_check_detail', 'CREATE_USER', 'create_user');
CALL rename_col_ci('stock_check_detail', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('stock_check_detail', 'IS_DELETED', 'is_deleted');

-- stock_record（16 列）
CALL rename_col_ci('stock_record', 'ID', 'id');
CALL rename_col_ci('stock_record', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('stock_record', 'MATERIAL_ID', 'material_id');
CALL rename_col_ci('stock_record', 'TYPE', 'type');
CALL rename_col_ci('stock_record', 'QTY', 'qty');
CALL rename_col_ci('stock_record', 'UNIT_PRICE', 'unit_price');
CALL rename_col_ci('stock_record', 'TOTAL_AMOUNT', 'total_amount');
CALL rename_col_ci('stock_record', 'BIZ_ID', 'biz_id');
CALL rename_col_ci('stock_record', 'REMARK', 'remark');
CALL rename_col_ci('stock_record', 'VOUCHER_IMAGES', 'voucher_images');
CALL rename_col_ci('stock_record', 'OPERATOR', 'operator');
CALL rename_col_ci('stock_record', 'CREATE_TIME', 'create_time');
CALL rename_col_ci('stock_record', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('stock_record', 'CREATE_USER', 'create_user');
CALL rename_col_ci('stock_record', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('stock_record', 'IS_DELETED', 'is_deleted');

-- supplier（13 列）
CALL rename_col_ci('supplier', 'ID', 'id');
CALL rename_col_ci('supplier', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('supplier', 'NAME', 'name');
CALL rename_col_ci('supplier', 'CONTACT', 'contact');
CALL rename_col_ci('supplier', 'PHONE', 'phone');
CALL rename_col_ci('supplier', 'ADDRESS', 'address');
CALL rename_col_ci('supplier', 'LICENSE_IMAGES', 'license_images');
CALL rename_col_ci('supplier', 'STATUS', 'status');
CALL rename_col_ci('supplier', 'CREATED_TIME', 'created_time');
CALL rename_col_ci('supplier', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('supplier', 'CREATE_USER', 'create_user');
CALL rename_col_ci('supplier', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('supplier', 'IS_DELETED', 'is_deleted');

-- supplier_settlement（12 列）
CALL rename_col_ci('supplier_settlement', 'ID', 'id');
CALL rename_col_ci('supplier_settlement', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('supplier_settlement', 'SUPPLIER_ID', 'supplier_id');
CALL rename_col_ci('supplier_settlement', 'PERIOD', 'period');
CALL rename_col_ci('supplier_settlement', 'TOTAL_AMOUNT', 'total_amount');
CALL rename_col_ci('supplier_settlement', 'PAID_AMOUNT', 'paid_amount');
CALL rename_col_ci('supplier_settlement', 'STATUS', 'status');
CALL rename_col_ci('supplier_settlement', 'CREATE_TIME', 'create_time');
CALL rename_col_ci('supplier_settlement', 'UPDATE_TIME', 'update_time');
CALL rename_col_ci('supplier_settlement', 'CREATE_USER', 'create_user');
CALL rename_col_ci('supplier_settlement', 'UPDATE_USER', 'update_user');
CALL rename_col_ci('supplier_settlement', 'IS_DELETED', 'is_deleted');

-- withdrawal_record（8 列）
CALL rename_col_ci('withdrawal_record', 'ID', 'id');
CALL rename_col_ci('withdrawal_record', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('withdrawal_record', 'WITHDRAWAL_ID', 'withdrawal_id');
CALL rename_col_ci('withdrawal_record', 'ACTUAL_AMOUNT', 'actual_amount');
CALL rename_col_ci('withdrawal_record', 'FEE', 'fee');
CALL rename_col_ci('withdrawal_record', 'TRANSFER_TIME', 'transfer_time');
CALL rename_col_ci('withdrawal_record', 'BANK_TRACE_NO', 'bank_trace_no');
CALL rename_col_ci('withdrawal_record', 'CREATE_TIME', 'create_time');

-- withdrawal_request（13 列）
CALL rename_col_ci('withdrawal_request', 'ID', 'id');
CALL rename_col_ci('withdrawal_request', 'TENANT_ID', 'tenant_id');
CALL rename_col_ci('withdrawal_request', 'USER_ID', 'user_id');
CALL rename_col_ci('withdrawal_request', 'AMOUNT', 'amount');
CALL rename_col_ci('withdrawal_request', 'BANK_NAME', 'bank_name');
CALL rename_col_ci('withdrawal_request', 'ACCOUNT_NAME', 'account_name');
CALL rename_col_ci('withdrawal_request', 'ACCOUNT_NUMBER', 'account_number');
CALL rename_col_ci('withdrawal_request', 'STATUS', 'status');
CALL rename_col_ci('withdrawal_request', 'REJECT_REASON', 'reject_reason');
CALL rename_col_ci('withdrawal_request', 'CREATE_TIME', 'create_time');
CALL rename_col_ci('withdrawal_request', 'APPROVE_TIME', 'approve_time');
CALL rename_col_ci('withdrawal_request', 'APPROVE_USER_ID', 'approve_user_id');
CALL rename_col_ci('withdrawal_request', 'IS_DELETED', 'is_deleted');

DROP PROCEDURE IF EXISTS rename_col_ci;