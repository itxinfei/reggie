-- ============================================================
-- 权限种子数据（对齐前端菜单结构 index.html menuList）
-- 背景：permission 表为空 → 角色管理"权限分配"打开权限树为"暂无数据"，
--       分配后也无法真正拦截。本脚本补齐：
--         1) 菜单权限（9 顶级模块 + 66 页面，permission_type=1）
--         2) 按钮权限（对齐后端 @RequiresPermission 全部 key，permission_type=2）
--         3) 角色默认分配（超级管理员/店长全量；收银员/厨师按职责裁剪）
-- 说明：permission_key 全局唯一（UNI）；id 手动指定便于 parent_id 引用，
--       新插入记录自增继续；重复执行会因 UNIQUE 冲突失败，需先清空再导。
-- ============================================================

-- 1) 菜单权限（顶级模块 + 页面，parent_id=0 为顶级）
INSERT INTO permission (id, permission_name, permission_key, permission_type, parent_id, route_path, icon, sort, status, create_time) VALUES
(1,   '数据概览',   'module:dashboard',     1, 0, 'page/dashboard/overview.html',        'ri-dashboard-line',       1,  1, NOW()),
(2,   '商品管理',   'module:dish',          1, 0, NULL,                                  'ri-shopping-bag-3-line',  2,  1, NOW()),
(3,   '订单中心',   'module:order',         1, 0, NULL,                                  'ri-file-list-3-line',     3,  1, NOW()),
(4,   '堂食管理',   'module:dining',        1, 0, NULL,                                  'ri-store-2-line',         4,  1, NOW()),
(5,   '进销存',     'module:inventory',     1, 0, NULL,                                  'ri-archive-line',         5,  1, NOW()),
(6,   '会员与用户', 'module:member',        1, 0, NULL,                                  'ri-group-line',           6,  1, NOW()),
(7,   '经营分析',   'module:report',        1, 0, NULL,                                  'ri-bar-chart-line',       7,  1, NOW()),
(8,   '营销与门店', 'module:marketing',     1, 0, NULL,                                  'ri-megaphone-line',       8,  1, NOW()),
(9,   '系统管理',   'module:sys',           1, 0, NULL,                                  'ri-settings-3-line',      9,  1, NOW()),

-- 商品管理（2xx）
(201, '分类管理',   'page:category-list',     1, 2, 'page/category/list.html',             'ri-apps-2-line',           1, 1, NOW()),
(202, '菜品管理',   'page:dish-list',         1, 2, 'page/food/list.html',                 'ri-restaurant-2-line',     2, 1, NOW()),
(203, '套餐管理',   'page:combo-list',        1, 2, 'page/combo/list.html',                'ri-stack-line',            3, 1, NOW()),
(204, '菜品规格',   'page:spec-management',   1, 2, 'page/food/spec-management.html',      'ri-list-check-2',          4, 1, NOW()),

-- 订单中心（3xx）
(301, '订单明细',     'page:order-list',              1, 3, 'page/order/list.html',                'ri-file-list-3-line',       1,  1, NOW()),
(302, '支付管理',     'page:payment-list',            1, 3, 'page/payment/order-list.html',         'ri-money-dollar-circle-line', 2,  1, NOW()),
(303, '收银台',       'page:cashier',                 1, 3, 'page/cashier/index.html',              'ri-money-cny-circle-line',  3,  1, NOW()),
(304, '收银日结',     'page:cashier-daily',           1, 3, 'page/cashier/daily-settlement.html',   'ri-calendar-check-line',    4,  1, NOW()),
(305, '配送订单',     'page:delivery-order-list',     1, 3, 'page/delivery/order-list.html',         'ri-map-pin-line',           5,  1, NOW()),
(306, '配送范围',     'page:delivery-range',          1, 3, 'page/delivery/range-management.html',   'ri-road-map-line',          6,  1, NOW()),
(307, '平台接入配置', 'page:platform-config',         1, 3, 'page/platform/config.html',            'ri-settings-3-line',        7,  1, NOW()),
(308, '平台订单',     'page:platform-order-list',     1, 3, 'page/platform/order-list.html',         'ri-list-check-2',           8,  1, NOW()),
(309, '商品映射',     'page:platform-dish-mapping',   1, 3, 'page/platform/dish-mapping.html',       'ri-price-tag-3-line',       9,  1, NOW()),
(310, '打印终端',     'page:printer-terminal',        1, 3, 'page/printer/terminal-list.html',       'ri-printer-line',          10,  1, NOW()),
(311, '打印任务',     'page:printer-task',            1, 3, 'page/printer/task-list.html',           'ri-file-history-line',     11,  1, NOW()),
(312, '紧急催菜',     'page:urgency',                 1, 3, 'page/urgency/urgency.html',             'ri-alarm-warning-line',    12,  1, NOW()),
(313, '未接单监控',   'page:order-pending-monitor',   1, 3, 'page/order/pending-monitor.html',       'ri-radar-line',            13,  1, NOW()),

-- 堂食管理（4xx）
(401, '桌台管理', 'page:table-list',       1, 4, 'page/dining/table-list.html',       'ri-layout-grid-line',   1, 1, NOW()),
(402, '区域管理', 'page:area-list',        1, 4, 'page/dining/area-list.html',        'ri-map-pin-2-line',     2, 1, NOW()),
(403, '排队管理', 'page:queue-list',       1, 4, 'page/dining/queue-list.html',       'ri-ticket-line',        3, 1, NOW()),
(404, '预订管理', 'page:reservation-list', 1, 4, 'page/dining/reservation-list.html', 'ri-calendar-check-line', 4, 1, NOW()),

-- 进销存（5xx）
(501, '原料管理',   'page:material-list',      1, 5, 'page/inventory/material-list.html',     'ri-leaf-line',          1, 1, NOW()),
(502, '食材分类',   'page:inventory-category', 1, 5, 'page/inventory/category-list.html',     'ri-folder-3-line',      2, 1, NOW()),
(503, '库存盘点',   'page:stock-check',        1, 5, 'page/inventory/stock-check.html',       'ri-check-double-line',  3, 1, NOW()),
(504, '库存流水',   'page:stock-record',       1, 5, 'page/inventory/stock-record.html',      'ri-route-line',         4, 1, NOW()),
(505, '采购管理',   'page:purchase-list',      1, 5, 'page/inventory/purchase-list.html',     'ri-shopping-cart-2-line', 5, 1, NOW()),
(506, '供应商管理', 'page:supplier-list',      1, 5, 'page/inventory/supplier-list.html',     'ri-truck-line',         6, 1, NOW()),
(507, '库存预警',   'page:material-warning',   1, 5, 'page/inventory/material-warning.html',  'ri-alert-line',         7, 1, NOW()),

-- 会员与用户（6xx）
(601, '会员等级', 'page:level-list',    1, 6, 'page/member-center/level-list.html',   'ri-vip-crown-line',    1, 1, NOW()),
(602, '会员列表', 'page:member-list',   1, 6, 'page/member-center/member-list.html',  'ri-user-star-line',    2, 1, NOW()),
(603, '积分管理', 'page:points-list',   1, 6, 'page/member-center/points-list.html',  'ri-coin-line',         3, 1, NOW()),
(604, '优惠券',   'page:coupon-list',   1, 6, 'page/member-center/coupon-list.html',  'ri-coupon-line',       4, 1, NOW()),
(605, '充值记录', 'page:recharge-list', 1, 6, 'page/member-center/recharge-list.html', 'ri-bank-card-line',    5, 1, NOW()),
(606, 'C端用户',  'page:user-list',     1, 6, 'page/user/list.html',                  'ri-user-3-line',       6, 1, NOW()),
(607, '会员挽留', 'page:retention',     1, 6, 'page/retention/retention.html',        'ri-heart-line',        7, 1, NOW()),

-- 经营分析（7xx）
(701, '营业日报',   'page:daily-report',     1, 7, 'page/report/daily.html',                'ri-file-chart-line',        1,  1, NOW()),
(702, '经营报表',   'page:business-report',  1, 7, 'page/report/business-report.html',      'ri-line-chart-line',        2,  1, NOW()),
(703, '销售报表',   'page:sales-report',     1, 7, 'page/report/sales-report.html',         'ri-bar-chart-grouped-line', 3,  1, NOW()),
(704, '菜品排行',   'page:dish-ranking',     1, 7, 'page/report/dish-ranking.html',         'ri-trophy-line',            4,  1, NOW()),
(705, '支付分析',   'page:payment-analysis', 1, 7, 'page/report/payment-analysis.html',     'ri-wallet-line',            5,  1, NOW()),
(706, '时段分析',   'page:time-slot',        1, 7, 'page/report/time-slot.html',            'ri-time-line',              6,  1, NOW()),
(707, '评价管理',   'page:evaluation-list',  1, 7, 'page/report/evaluation-list.html',      'ri-star-line',              7,  1, NOW()),
(708, '智能推荐',   'page:recommend',        1, 7, 'page/recommend/overview.html',          'ri-lightbulb-flash-line',   8,  1, NOW()),
(709, '成本概览',   'page:cost-overview',    1, 7, 'page/cost/overview.html',               'ri-pie-chart-line',         9,  1, NOW()),
(710, '菜品成本',   'page:dish-cost',        1, 7, 'page/cost/dish-cost.html',              'ri-restaurant-line',       10,  1, NOW()),
(711, '提现管理',   'page:withdrawal',       1, 7, 'page/finance/withdrawal.html',          'ri-bank-card-2-line',      11,  1, NOW()),
(712, '导出中心',   'page:export-center',    1, 7, 'page/export/index.html',                'ri-file-download-line',    12,  1, NOW()),
(713, '发票管理',   'page:invoice-list',     1, 7, 'page/finance/invoice-list.html',        'ri-receipt-line',          13,  1, NOW()),

-- 营销与门店（8xx）
(801, '营销活动',   'page:marketing',        1, 8, 'page/marketing/index.html',            'ri-flag-line',           1, 1, NOW()),
(802, '加盟商',     'page:franchisee-list',  1, 8, 'page/franchise/franchisee-list.html',  'ri-user-star-line',      2, 1, NOW()),
(803, '加盟合同',   'page:contract-list',    1, 8, 'page/franchise/contract-list.html',    'ri-file-list-3-line',    3, 1, NOW()),
(804, '分账结算',   'page:settlement-list',  1, 8, 'page/franchise/settlement-list.html',  'ri-bank-card-line',      4, 1, NOW()),
(805, '门店列表',   'page:store-list',       1, 8, 'page/store/list.html',                 'ri-store-line',          5, 1, NOW()),
(806, '总部控制台', 'page:store-dashboard',  1, 8, 'page/store/dashboard.html',            'ri-dashboard-3-line',    6, 1, NOW()),

-- 系统管理（9xx）
(901, '员工管理', 'page:employee-list',      1, 9, 'page/member/list.html',              'ri-user-settings-line',      1,  1, NOW()),
(902, '角色管理', 'page:role-list',          1, 9, 'page/sys/role-list.html',            'ri-shield-user-line',        2,  1, NOW()),
(903, '通知模板', 'page:template-list',      1, 9, 'page/sys/template-list.html',        'ri-message-2-line',          3,  1, NOW()),
(904, '系统配置', 'page:config-list',        1, 9, 'page/sys/config-list.html',          'ri-tools-line',              4,  1, NOW()),
(905, '操作日志', 'page:operation-log',      1, 9, 'page/sys/operation-log.html',        'ri-file-list-2-line',        5,  1, NOW()),
(906, '地区管理', 'page:region-list',        1, 9, 'page/region/list.html',              'ri-map-2-line',              6,  1, NOW()),
(907, '消息通知', 'page:notification-list',  1, 9, 'page/notification/list.html',        'ri-notification-3-line',     7,  1, NOW()),
(908, '客服管理', 'page:customer-service',   1, 9, 'page/customer-service/list.html',    'ri-customer-service-2-line', 8,  1, NOW()),
(909, '考勤管理', 'page:attendance',         1, 9, 'page/attendance/attendance.html',    'ri-calendar-check-line',     9,  1, NOW()),
(910, '排班管理', 'page:schedule',           1, 9, 'page/schedule/schedule.html',        'ri-calendar-2-line',        10,  1, NOW()),
(911, 'AI对话',   'page:ai-assistant',       1, 9, 'page/ai/assistant.html',             'ri-chat-3-line',            11,  1, NOW()),
(912, '模型配置', 'page:provider-config',    1, 9, 'page/ai/provider-config.html',       'ri-cpu-line',               12,  1, NOW());

-- 2) 按钮权限（对齐后端 @RequiresPermission 全部 key，parent 挂在对应页面下）
INSERT INTO permission (id, permission_name, permission_key, permission_type, parent_id, route_path, icon, sort, status, create_time) VALUES
(10001, '加盟管理',       'franchise:manage',    2, 802, NULL, NULL, 1, 1, NOW()),
(10002, '评价查看',       'evaluation:view',     2, 707, NULL, NULL, 1, 1, NOW()),
(10003, '评价审核',       'evaluation:audit',    2, 707, NULL, NULL, 2, 1, NOW()),
(10004, '评价回复',       'evaluation:reply',    2, 707, NULL, NULL, 3, 1, NOW()),
(10005, '平台接入管理',   'platform:manage',     2, 307, NULL, NULL, 1, 1, NOW()),
(10006, '地区新增',       'region:add',          2, 906, NULL, NULL, 1, 1, NOW()),
(10007, '地区修改',       'region:edit',         2, 906, NULL, NULL, 2, 1, NOW()),
(10008, '地区删除',       'region:delete',       2, 906, NULL, NULL, 3, 1, NOW()),
(10009, '消息发送',       'notification:send',   2, 907, NULL, NULL, 1, 1, NOW());

-- 3) 角色默认权限分配
-- 超级管理员（18）：全量（75 菜单 + 9 按钮）
INSERT INTO role_permission (role_id, permission_id, create_time)
SELECT 18, id, NOW() FROM permission WHERE status = 1;

-- 店长（19）：全量（管理员可在界面按门店裁剪）
INSERT INTO role_permission (role_id, permission_id, create_time)
SELECT 19, id, NOW() FROM permission WHERE status = 1;

-- 公共收银员（20）：数据概览 + 商品管理 + 订单核心(收银/支付/明细/日结) + 堂食管理 + 会员(不含C端/挽留) + 报表只读
INSERT INTO role_permission (role_id, permission_id, create_time) VALUES
(20, 1, NOW()),
(20, 201, NOW()), (20, 202, NOW()), (20, 203, NOW()), (20, 204, NOW()),
(20, 301, NOW()), (20, 302, NOW()), (20, 303, NOW()), (20, 304, NOW()),
(20, 401, NOW()), (20, 402, NOW()), (20, 403, NOW()), (20, 404, NOW()),
(20, 601, NOW()), (20, 602, NOW()), (20, 603, NOW()), (20, 604, NOW()), (20, 605, NOW()),
(20, 701, NOW()), (20, 702, NOW()), (20, 703, NOW()), (20, 705, NOW()), (20, 706, NOW());

-- 门店厨师（21）：数据概览 + 菜品规格 + 紧急催菜/未接单监控/订单明细
INSERT INTO role_permission (role_id, permission_id, create_time) VALUES
(21, 1, NOW()),
(21, 204, NOW()),
(21, 301, NOW()), (21, 312, NOW()), (21, 313, NOW());
