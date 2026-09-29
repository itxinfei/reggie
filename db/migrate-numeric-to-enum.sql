-- 旧「数字状态码」种子数据 → 现行「字符串枚举码」一次性迁移
-- 仅更新数字行（WHERE 精确匹配数字），已存在的字符串行（EAT_IN/PENDING/SUCCESS/IN 等）原样保留。
-- 执行前已备份（db/reggie-backup-20260927.sql）。

-- 订单：就餐方式 / 平台（'1' 有地址无桌台=外卖 TAKEOUT；'2' 有桌台=堂食 EAT_IN）
UPDATE orders SET dining_type='TAKEOUT' WHERE dining_type='1';
UPDATE orders SET dining_type='EAT_IN'  WHERE dining_type='2';
UPDATE orders SET platform_type='MEITUAN' WHERE platform_type='1';

-- 退款单：1退款中 2处理中 3成功
UPDATE refund_record SET status='pending'    WHERE status='1';
UPDATE refund_record SET status='processing' WHERE status='2';
UPDATE refund_record SET status='SUCCESS'    WHERE status='3';

-- 配送单（旧三档制，跳过 PICKING/DELIVERING 细分态）：1待接单 2已接单 3已送达
UPDATE delivery_order SET status='PENDING'   WHERE status='1';
UPDATE delivery_order SET status='ACCEPTED'  WHERE status='2';
UPDATE delivery_order SET status='DELIVERED' WHERE status='3';

-- 团购活动：1进行中 2已关闭 3已结束
UPDATE group_buy_campaign SET status='OPEN'   WHERE status='1';
UPDATE group_buy_campaign SET status='CLOSED' WHERE status='2';
UPDATE group_buy_campaign SET status='ENDED'  WHERE status='3';
-- 团购参与：1已参团 2已支付 3已取消
UPDATE group_buy_participation SET status='JOINED'    WHERE status='1';
UPDATE group_buy_participation SET status='PAID'      WHERE status='2';
UPDATE group_buy_participation SET status='CANCELLED' WHERE status='3';

-- 采购单（旧三档制，跳过 PARTIAL 细分态）：1草稿 2已下单 3已收货
UPDATE purchase_order SET status='DRAFT'   WHERE status='1';
UPDATE purchase_order SET status='ORDERED' WHERE status='2';
UPDATE purchase_order SET status='RECEIVED' WHERE status='3';

-- 盘点单：1草稿 2进行中 3已完成
UPDATE stock_check SET status='DRAFT'       WHERE status='1';
UPDATE stock_check SET status='IN_PROGRESS' WHERE status='2';
UPDATE stock_check SET status='DONE'        WHERE status='3';

-- 库存流水：1入库 2出库
UPDATE stock_record SET type='IN'  WHERE type='1';
UPDATE stock_record SET type='OUT' WHERE type='2';

-- 用户券：1未使用 2已使用 3已过期
UPDATE coupon_user SET status='unused'  WHERE status='1';
UPDATE coupon_user SET status='used'    WHERE status='2';
UPDATE coupon_user SET status='expired' WHERE status='3';

-- 积分流水：1增加
UPDATE points_record SET type='IN' WHERE type='1';

-- 充值记录：1待支付 2成功 3已取消
UPDATE recharge_record SET status='PENDING'   WHERE status='1';
UPDATE recharge_record SET status='SUCCESS'   WHERE status='2';
UPDATE recharge_record SET status='CANCELLED' WHERE status='3';

-- 支付单：1成功（均有 paid_time） 2已退款
UPDATE payment_order SET status='SUCCESS' WHERE status='1';
UPDATE payment_order SET status='REFUND'  WHERE status='2';

-- 打印任务：1待打印 2已拉取 3成功
UPDATE print_task SET status='PENDING' WHERE status='1';
UPDATE print_task SET status='PULLED'  WHERE status='2';
UPDATE print_task SET status='SUCCESS' WHERE status='3';
