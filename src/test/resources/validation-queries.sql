-- 瑞吉外卖 - 业务数据验证脚本
-- 执行日期: 2026-07-03
-- 用途: 验证订单流程数据完整性

-- ========================================
-- 1. 基础数据验证
-- ========================================

SELECT '=== 租户数据 ===' AS '检查项';
SELECT id, name, phone, status FROM tenant LIMIT 5;

SELECT '\n=== 员工数据 ===' AS '检查项';
SELECT id, username, name, phone, status FROM employee LIMIT 5;

SELECT '\n=== 用户数据 ===' AS '检查项';
SELECT id, name, phone, sex FROM user LIMIT 5;

-- ========================================
-- 2. 商品数据验证
-- ========================================

SELECT '\n=== 分类数据 ===' AS '检查项';
SELECT id, name, type FROM category LIMIT 10;

SELECT '\n=== 菜品数据 ===' AS '检查项';
SELECT id, name, price, status FROM dish LIMIT 5;

SELECT '\n=== 套餐数据 ===' AS '检查项';
SELECT id, name, price, status FROM setmeal LIMIT 5;

-- ========================================
-- 3. 订单核心数据验证
-- ========================================

SELECT '\n=== 订单列表 ===' AS '检查项';
SELECT
    id AS '订单ID',
    number AS '订单号',
    CASE status
        WHEN 1 THEN '待付款'
        WHEN 2 THEN '待派送'
        WHEN 3 THEN '已派送'
        WHEN 4 THEN '已完成'
        WHEN 5 THEN '已取消'
        WHEN 6 THEN '已退款'
    END AS '订单状态',
    amount AS '订单金额',
    user_name AS '用户名',
    dining_type AS '就餐方式'
FROM orders
ORDER BY order_time DESC;

SELECT '\n=== 订单状态分布 ===' AS '检查项';
SELECT
    CASE status
        WHEN 1 THEN '待付款'
        WHEN 2 THEN '待派送'
        WHEN 3 THEN '已派送'
        WHEN 4 THEN '已完成'
        WHEN 5 THEN '已取消'
        WHEN 6 THEN '已退款'
    END AS '订单状态',
    COUNT(*) AS '订单数量',
    CONCAT(FORMAT(SUM(amount), 2), ' 元') AS '总金额',
    CONCAT(FORMAT(AVG(amount), 2), ' 元') AS '平均金额'
FROM orders
GROUP BY status
ORDER BY status;

-- ========================================
-- 4. 订单明细验证
-- ========================================

SELECT '\n=== 订单明细 (订单ID=1) ===' AS '检查项';
SELECT
    od.id AS '明细ID',
    od.name AS '商品名称',
    od.number AS '数量',
    od.amount AS '单价',
    od.dish_flavor AS '口味',
    o.number AS '订单号',
    o.status AS '订单状态'
FROM order_detail od
JOIN orders o ON od.order_id = o.id
WHERE od.order_id = 1;

-- ========================================
-- 5. 购物车验证
-- ========================================

SELECT '\n=== 用户购物车 ===' AS '检查项';
SELECT
    sc.id AS '购物车ID',
    sc.name AS '商品名称',
    sc.number AS '数量',
    sc.amount AS '单价',
    sc.dish_flavor AS '口味',
    u.name AS '用户名'
FROM shopping_cart sc
JOIN user u ON sc.user_id = u.id
WHERE sc.user_id = 1
ORDER BY sc.create_time DESC;

-- ========================================
-- 6. 地址簿验证
-- ========================================

SELECT '\n=== 用户地址簿 ===' AS '检查项';
SELECT
    id AS '地址ID',
    user_id AS '用户ID',
    consignee AS '收货人',
    phone AS '手机号',
    CONCAT(province_name, city_name, district_name, detail) AS '完整地址',
    is_default AS '是否默认'
FROM address_book
WHERE user_id = 1;

-- ========================================
-- 7. 会员数据验证
-- ========================================

SELECT '\n=== 会员数据 ===' AS '检查项';
SELECT
    m.id AS '会员ID',
    m.name AS '会员姓名',
    m.phone AS '手机号',
    ml.name AS '会员等级',
    m.points AS '积分',
    CONCAT(FORMAT(m.balance, 2), ' 元') AS '余额'
FROM member m
LEFT JOIN member_level ml ON m.level_id = ml.id
LIMIT 5;

-- ========================================
-- 8. 库存数据验证
-- ========================================

SELECT '\n=== 库存记录 ===' AS '检查项';
SELECT
    sr.id AS '记录ID',
    m.name AS '物料名称',
    sr.type AS '类型(IN/OUT)',
    sr.qty AS '数量',
    sr.unit_price AS '单价',
    sr.total_amount AS '总金额',
    sr.remark AS '备注'
FROM stock_record sr
JOIN material m ON sr.material_id = m.id
LIMIT 10;

-- ========================================
-- 9. 数据完整性检查
-- ========================================

SELECT '\n=== 数据完整性检查 ===' AS '检查项';

-- 检查订单金额是否与明细合计一致
SELECT
    o.id AS '订单ID',
    o.amount AS '订单金额',
    COALESCE(SUM(od.amount * od.number), 0) AS '明细合计',
    CASE
        WHEN o.amount = COALESCE(SUM(od.amount * od.number), 0) THEN '✓ 一致'
        ELSE '✗ 不一致'
    END AS '验证结果'
FROM orders o
LEFT JOIN order_detail od ON o.id = od.order_id
GROUP BY o.id, o.amount
HAVING o.amount != COALESCE(SUM(od.amount * od.number), 0)
LIMIT 5;

-- 检查购物车金额计算
SELECT
    sc.id AS '购物车ID',
    sc.amount AS '单价',
    sc.number AS '数量',
    sc.amount * sc.number AS '应付金额'
FROM shopping_cart sc
WHERE sc.user_id = 1;

-- ========================================
-- 10. 统计汇总
-- ========================================

SELECT '\n=== 数据统计汇总 ===' AS '检查项';
SELECT
    (SELECT COUNT(*) FROM tenant) AS '租户数',
    (SELECT COUNT(*) FROM user) AS '用户数',
    (SELECT COUNT(*) FROM dish) AS '菜品数',
    (SELECT COUNT(*) FROM setmeal) AS '套餐数',
    (SELECT COUNT(*) FROM orders) AS '订单数',
    (SELECT COUNT(*) FROM order_detail) AS '订单明细数',
    CONCAT(FORMAT((SELECT SUM(amount) FROM orders), 2), ' 元') AS '订单总金额',
    (SELECT COUNT(*) FROM member) AS '会员数';

-- ========================================
-- 11. 关键业务逻辑验证
-- ========================================

SELECT '\n=== 订单状态流转验证 ===' AS '检查项';

-- 验证订单号格式
SELECT
    id,
    number,
    CASE
        WHEN LENGTH(number) >= 10 THEN '✓ 正常'
        ELSE '✗ 异常'
    END AS '订单号格式'
FROM orders
LIMIT 5;

-- 验证订单时间完整性
SELECT
    id,
    number,
    order_time,
    checkout_time,
    CASE
        WHEN order_time IS NOT NULL AND checkout_time IS NOT NULL THEN '✓ 完整'
        WHEN order_time IS NOT NULL AND checkout_time IS NULL THEN '⚠ 待支付'
        ELSE '✗ 异常'
    END AS '时间完整性'
FROM orders
LIMIT 5;

-- ========================================
-- 12. 打印模块验证
-- ========================================

SELECT '\n=== 打印机配置 ===' AS '检查项';
SELECT
    id,
    name,
    type,
    brand,
    system_printer_name,
    status
FROM printer_config
LIMIT 5;

SELECT '\n=== 打印日志 ===' AS '检查项';
SELECT
    id,
    order_id,
    print_type,
    printer_id,
    status,
    created_time
FROM printer_log
LIMIT 5;

SELECT '\n=== 验证完成 ===' AS '状态';
