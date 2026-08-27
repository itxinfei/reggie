-- 统一 coupon_user.status 值为小写（与 CouponStatus 枚举 value 对齐）
-- 背景：CouponStatus 枚举 value 为小写（unused/used/expired），
-- 但存量数据中存在历史写入的大写值（UNUSED/USED/EXPIRED），
-- 导致按枚举值（小写）查询/核销时匹配不到存量券。
-- 幂等：只更新大写值，已符合小写约定或其它值不受影响。
UPDATE `coupon_user`
SET `status` = LOWER(`status`)
WHERE `status` IN ('UNUSED', 'USED', 'EXPIRED');
