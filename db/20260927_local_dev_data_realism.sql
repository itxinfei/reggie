-- ai_prompt_template.update_time 全部 24 行同一天
UPDATE `ai_prompt_template` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- ai_user_profile.update_time 全部 20 行同一天
UPDATE `ai_user_profile` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- attendance.update_time 全部 60 行同一天
UPDATE `attendance` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- buy_get_free.update_time 全部 10 行同一天
UPDATE `buy_get_free` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- category.create_time 全部 10 行同一天
UPDATE `category` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- category.update_time 全部 10 行同一天
UPDATE `category` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- complaint.update_time 全部 10 行同一天
UPDATE `complaint` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- coupon_template.update_time 全部 12 行同一天
UPDATE `coupon_template` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- coupon_user.update_time 全部 165 行同一天
UPDATE `coupon_user` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- dining_area.created_time 全部 5 行同一天
UPDATE `dining_area` SET `created_time` = `created_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `created_time` IS NOT NULL;
-- dining_area.update_time 全部 5 行同一天
UPDATE `dining_area` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- dining_queue.created_time 全部 14 行同一天
UPDATE `dining_queue` SET `created_time` = `created_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `created_time` IS NOT NULL;
-- dining_queue.update_time 全部 14 行同一天
UPDATE `dining_queue` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- dining_table.create_time 全部 30 行同一天
UPDATE `dining_table` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- dining_table.update_time 全部 30 行同一天
UPDATE `dining_table` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- discount_rule.update_time 全部 10 行同一天
UPDATE `discount_rule` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- dish.create_time 全部 60 行同一天
UPDATE `dish` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- dish_cost.create_time 全部 60 行同一天
UPDATE `dish_cost` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- dish_cost.update_time 全部 60 行同一天
UPDATE `dish_cost` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- dish_material.create_time 全部 48 行同一天
UPDATE `dish_material` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- dish_material.update_time 全部 48 行同一天
UPDATE `dish_material` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- dish_platform_mapping.update_time 全部 40 行同一天
UPDATE `dish_platform_mapping` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- dish_spec_group.create_time 全部 14 行同一天
UPDATE `dish_spec_group` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- dish_spec_group.update_time 全部 14 行同一天
UPDATE `dish_spec_group` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- dish_spec_option.create_time 全部 42 行同一天
UPDATE `dish_spec_option` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- dish_spec_option.update_time 全部 42 行同一天
UPDATE `dish_spec_option` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- dish_spec_relation.create_time 全部 97 行同一天
UPDATE `dish_spec_relation` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- employee.update_time 全部 15 行同一天
UPDATE `employee` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- flash_sale.update_time 全部 10 行同一天
UPDATE `flash_sale` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- full_reduction_rule.update_time 全部 12 行同一天
UPDATE `full_reduction_rule` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- group_buy_campaign.update_time 全部 10 行同一天
UPDATE `group_buy_campaign` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- invoice_record.update_time 全部 20 行同一天
UPDATE `invoice_record` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- invoice_title.update_time 全部 15 行同一天
UPDATE `invoice_title` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- labor_cost.update_time 全部 15 行同一天
UPDATE `labor_cost` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- marketing_campaign.update_time 全部 12 行同一天
UPDATE `marketing_campaign` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- material.created_time 全部 43 行同一天
UPDATE `material` SET `created_time` = `created_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `created_time` IS NOT NULL;
-- material.update_time 全部 43 行同一天
UPDATE `material` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- material_category.created_time 全部 10 行同一天
UPDATE `material_category` SET `created_time` = `created_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `created_time` IS NOT NULL;
-- material_category.update_time 全部 10 行同一天
UPDATE `material_category` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- member_level.created_time 全部 10 行同一天
UPDATE `member_level` SET `created_time` = `created_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `created_time` IS NOT NULL;
-- member_level.update_time 全部 10 行同一天
UPDATE `member_level` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- menu.update_time 全部 36 行同一天
UPDATE `menu` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- new_customer_discount.update_time 全部 10 行同一天
UPDATE `new_customer_discount` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- notification_template.update_time 全部 10 行同一天
UPDATE `notification_template` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- other_cost.update_time 全部 15 行同一天
UPDATE `other_cost` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- permission.update_time 全部 10 行同一天
UPDATE `permission` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- platform_config.update_time 全部 12 行同一天
UPDATE `platform_config` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- platform_reconcile_task.update_time 全部 15 行同一天
UPDATE `platform_reconcile_task` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- points_record.update_time 全部 19 行同一天
UPDATE `points_record` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- print_terminal.created_time 全部 12 行同一天
UPDATE `print_terminal` SET `created_time` = `created_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `created_time` IS NOT NULL;
-- print_terminal.update_time 全部 12 行同一天
UPDATE `print_terminal` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- printer_config.created_time 全部 12 行同一天
UPDATE `printer_config` SET `created_time` = `created_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `created_time` IS NOT NULL;
-- printer_config.update_time 全部 12 行同一天
UPDATE `printer_config` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- printer_log.update_time 全部 20 行同一天
UPDATE `printer_log` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- profit_analysis.update_time 全部 20 行同一天
UPDATE `profit_analysis` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- recommendation_feedback.update_time 全部 25 行同一天
UPDATE `recommendation_feedback` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- reconciliation_statement.update_time 全部 15 行同一天
UPDATE `reconciliation_statement` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- refund_record.update_time 全部 18 行同一天
UPDATE `refund_record` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- rider.create_time 全部 11 行同一天
UPDATE `rider` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- rider.update_time 全部 11 行同一天
UPDATE `rider` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- rider_location_record.create_time 全部 25 行同一天
UPDATE `rider_location_record` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- role.update_time 全部 10 行同一天
UPDATE `role` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- role_permission.create_time 全部 55 行同一天
UPDATE `role_permission` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- setmeal.create_time 全部 12 行同一天
UPDATE `setmeal` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- setmeal.update_time 全部 12 行同一天
UPDATE `setmeal` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- setmeal_dish.create_time 全部 46 行同一天
UPDATE `setmeal_dish` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- setmeal_dish.update_time 全部 46 行同一天
UPDATE `setmeal_dish` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- stock_record.update_time 全部 45 行同一天
UPDATE `stock_record` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- store_config.create_time 全部 10 行同一天
UPDATE `store_config` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- store_config.update_time 全部 10 行同一天
UPDATE `store_config` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- store_employee_permission.create_time 全部 15 行同一天
UPDATE `store_employee_permission` SET `create_time` = `create_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `create_time` IS NOT NULL;
-- store_employee_permission.update_time 全部 15 行同一天
UPDATE `store_employee_permission` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- store_info.update_time 全部 10 行同一天
UPDATE `store_info` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- supplier.created_time 全部 15 行同一天
UPDATE `supplier` SET `created_time` = `created_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE WHERE `created_time` IS NOT NULL;
-- supplier.update_time 全部 15 行同一天
UPDATE `supplier` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `created_time`) WHERE `update_time` IS NOT NULL;
-- supplier_settlement.update_time 全部 15 行同一天
UPDATE `supplier_settlement` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- system_config.update_time 全部 30 行同一天
UPDATE `system_config` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- urgency_record.update_time 全部 20 行同一天
UPDATE `urgency_record` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- user.update_time 全部 29 行同一天
UPDATE `user` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- withdrawal_application.update_time 全部 20 行同一天
UPDATE `withdrawal_application` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
-- work_schedule.update_time 全部 40 行同一天
UPDATE `work_schedule` SET `update_time` = GREATEST(`update_time` - INTERVAL (`id` % 45) DAY - INTERVAL (`id` DIV 45 % 24) HOUR - INTERVAL (`id` DIV 1080 % 60) MINUTE, `create_time`) WHERE `update_time` IS NOT NULL;
