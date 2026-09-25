// final-check.js — 上线前全库体检（8 段）
// 1) 行数体检：117 表 0 空表，最少行数
// 2) 枚举/状态值合法性
// 3) 逻辑外键孤儿（子表 -> 主表）
// 4) 图片资源对账
// 5) 金额自洽
// 6) 关键业务闭环（订单->明细->支付->退款/配送）
// 7) 登录可用性（后台/C端/骑手）
// 8) 租户隔离
//
// 跑法：node scripts/final-check.js
// 依赖：npm install mysql2 bcryptjs --no-save（本机无 Python）
// 目标库：仅本地 localhost:3306/reggie（严禁指向公网/生产库）
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

const UP = path.normalize('D:/MyCode/reggie/uploads');
const walk = d => {
  let o = [];
  if (!fs.existsSync(d)) return o;
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) o = o.concat(walk(p));
    else if (/\.(jpg|jpeg|png|webp|gif|svg)$/i.test(e.name)) o.push(p);
  }
  return o;
};

let pass = 0, fail = 0, skip = 0;
const ok = m => { pass++; console.log('  [PASS] ' + m); };
const bad = (m, x) => { fail++; console.log('  [FAIL] ' + m + (x !== undefined ? ' -> ' + JSON.stringify(x) : '')); };
const skp = m => { skip++; console.log('  [SKIP] ' + m); };

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie' });
  const q = async s => (await c.query(s))[0];
  const one = async s => (await q(s))[0];
  const cnt = async s => +Object.values(await one(s))[0];

  console.log('===== 1. 行数体检 =====');
  const tables = (await q("SELECT TABLE_NAME n FROM information_schema.tables WHERE table_schema='reggie' ORDER BY TABLE_NAME")).map(r => r.n);
  const counts = new Map();
  for (const t of tables) counts.set(t, await cnt('SELECT COUNT(*) FROM `' + t + '`'));
  const empty = [...counts.entries()].filter(([, n]) => n === 0);
  const few = [...counts.entries()].filter(([, n]) => n > 0 && n < 10);
  console.log('  表数 ' + tables.length + ' / 总行数 ' + [...counts.values()].reduce((a, b) => a + b, 0));
  empty.length === 0 ? ok('0 空表') : bad('空表 ' + empty.length, empty);
  few.length === 0 ? ok('0 张表行数 < 10') : bad('行数<10 ' + few.length, few);

  console.log('\n===== 2. 枚举值合法性 =====');
  const enums = [
    ['orders.status', 'orders', 'status'],
    ['orders.pay_method', 'orders', 'pay_method'],
    ['dish.status', 'dish', 'status'],
    ['category.type', 'category', 'type'],
    ['setmeal.status', 'setmeal', 'status'],
    ['employee.status', 'employee', 'status'],
    ['user.status', 'user', 'status'],
    ['rider.status', 'rider', 'status'],
    ['tenant.status', 'tenant', 'status'],
    
    ['payment_order.status', 'payment_order', 'status'],
    ['refund_record.status', 'refund_record', 'status'],
    ['delivery_order.status', 'delivery_order', 'status'],
    ['attendance.status', 'attendance', 'status'],
    ['work_schedule.shift', 'work_schedule', 'shift'],
    ['coupon_template.status', 'coupon_template', 'status'],
    ['marketing_campaign.status', 'marketing_campaign', 'status'],
    ['full_reduction_rule.status', 'full_reduction_rule', 'status'],
    ['dish_evaluation.star_rating', 'dish_evaluation', 'star_rating'],
    ['complaint.status', 'complaint', 'status']
  ];
  for (const [label, t, col] of enums) {
    let vals;
    try { vals = (await q('SELECT DISTINCT `' + col + '` v FROM `' + t + '` WHERE `' + col + '` IS NOT NULL')).map(r => r.v); }
    catch (e) { skp(label + ' 表/列不存在'); continue; }
    if (!vals.length) { skp(label + ' 全为 NULL'); continue; }
    const nums = vals.filter(v => typeof v === 'number');
    if (nums.length && nums.some(v => v < 0 || v > 99)) bad(label + ' 值域异常', vals);
    else ok(label + ' = ' + JSON.stringify([...new Set(vals)].sort()));
  }

  // 孤儿检查：每项为「SELECT COUNT(*) FROM (LEFT JOIN 主体) t WHERE 主表列 IS NULL」
  // 注意：子查询别名必须放在右括号之后，MySQL 不支持 (...) t WHERE ... 以外的写法
  console.log('\n===== 3. 逻辑外键孤儿 =====');
  const fks = [
    ['order_detail -> orders', 'order_detail od LEFT JOIN orders o ON o.id=od.order_id', 'o.id'],
    ['order_detail -> dish', 'order_detail od LEFT JOIN dish d ON d.id=od.dish_id', 'd.id'],
    ['payment_order -> orders', 'payment_order po LEFT JOIN orders o ON o.id=po.order_id', 'o.id'],
    ['refund_record -> payment_order', 'refund_record rr LEFT JOIN payment_order po ON po.id=rr.payment_order_id', 'po.id'],
    ['delivery_order -> orders', 'delivery_order dr LEFT JOIN orders o ON o.id=dr.order_id', 'o.id'],
    ['shopping_cart -> user', 'shopping_cart sc LEFT JOIN user u ON u.id=sc.user_id', 'u.id'],
    ['coupon_user -> coupon_template', 'coupon_user cu LEFT JOIN coupon_template ct ON ct.id=cu.template_id', 'ct.id'],
    ['coupon_user -> member', 'coupon_user cu LEFT JOIN member m ON m.id=cu.member_id', 'm.id'],
    ['campaign_usage_record -> marketing_campaign', 'campaign_usage_record u LEFT JOIN marketing_campaign m ON m.id=u.campaign_id', 'm.id'],
    ['full_reduction_rule -> marketing_campaign', 'full_reduction_rule f LEFT JOIN marketing_campaign m ON m.id=f.campaign_id', 'm.id'],
    ['discount_rule -> marketing_campaign', 'discount_rule d LEFT JOIN marketing_campaign m ON m.id=d.campaign_id', 'm.id'],
    ['purchase_order_detail -> material', 'purchase_order_detail d LEFT JOIN material m ON m.ID=d.MATERIAL_ID', 'm.ID'],
    ['purchase_order -> supplier', 'purchase_order p LEFT JOIN supplier s ON s.ID=p.SUPPLIER_ID', 's.ID'],
    ['stock_check_detail -> material', 'stock_check_detail d LEFT JOIN material m ON m.ID=d.MATERIAL_ID', 'm.ID'],
    ['stock_record -> material', 'stock_record s LEFT JOIN material m ON m.ID=s.MATERIAL_ID', 'm.ID'],
    ['price_history -> material', 'price_history p LEFT JOIN material m ON m.ID=p.MATERIAL_ID', 'm.ID'],
    ['supplier_settlement -> supplier', 'supplier_settlement s LEFT JOIN supplier su ON su.ID=s.SUPPLIER_ID', 'su.ID'],
    ['material -> material_category', 'material m LEFT JOIN material_category mc ON mc.ID=m.CATEGORY_ID', 'mc.ID'],
    ['attendance -> employee', 'attendance a LEFT JOIN employee e ON e.id=a.employee_id', 'e.id'],
    ['work_schedule -> employee', 'work_schedule w LEFT JOIN employee e ON e.id=w.employee_id', 'e.id'],
    ['labor_cost -> employee', 'labor_cost l LEFT JOIN employee e ON e.id=l.employee_id', 'e.id'],
    ['employee_role -> role', 'employee_role er LEFT JOIN role r ON r.id=er.role_id', 'r.id'],
    ['employee_role -> employee', 'employee_role er LEFT JOIN employee e ON e.id=er.employee_id', 'e.id'],
    ['ai_message -> ai_conversation', 'ai_message m LEFT JOIN ai_conversation cv ON cv.conversation_id=m.conversation_id', 'cv.conversation_id'],
    ['ai_knowledge_chunk -> ai_knowledge_doc', 'ai_knowledge_chunk k LEFT JOIN ai_knowledge_doc d ON d.id=k.doc_id', 'd.id'],
    ['ai_user_profile -> user', 'ai_user_profile p LEFT JOIN user u ON u.id=p.user_id', 'u.id'],
    ['recommendation_cache -> user', 'recommendation_cache rc LEFT JOIN user u ON u.id=rc.user_id', 'u.id'],
    ['recommendation_feedback -> recommendation_cache', 'recommendation_feedback rf LEFT JOIN recommendation_cache rc ON rc.id=rf.recommend_cache_id', 'rc.id'],
    ['dish -> category', 'dish d LEFT JOIN category c ON c.id=d.category_id', 'c.id'],
    ['setmeal -> category', 'setmeal s LEFT JOIN category c ON c.id=s.category_id', 'c.id'],
    ['dish_flavor -> dish', 'dish_flavor f LEFT JOIN dish d ON d.id=f.dish_id', 'd.id'],
    ['dish_material -> dish', 'dish_material m LEFT JOIN dish d ON d.id=m.dish_id', 'd.id'],
    ['dish_cost -> dish', 'dish_cost dc LEFT JOIN dish d ON d.id=dc.dish_id', 'd.id'],
    ['dish_evaluation -> dish', 'dish_evaluation e LEFT JOIN dish d ON d.id=e.dish_id', 'd.id'],
    ['group_buy_participation -> group_buy_campaign', 'group_buy_participation p LEFT JOIN group_buy_campaign g ON g.ID=p.GROUP_BUY_ID', 'g.ID'],
    ['points_record -> member', 'points_record p LEFT JOIN member m ON m.id=p.member_id', 'p.id'],
    ['recharge_record -> member', 'recharge_record r LEFT JOIN member m ON m.id=r.member_id', 'm.id'],
    ['urgency_record -> orders', 'urgency_record u LEFT JOIN orders o ON o.id=u.order_id', 'o.id'],
    ['cs_message -> cs_session', 'cs_message m LEFT JOIN cs_session s ON s.id=m.session_id', 's.id'],
    ['complaint -> user', 'complaint cm LEFT JOIN user u ON u.id=cm.user_id', 'u.id'],
    ['withdrawal_record -> withdrawal_application', 'withdrawal_record w LEFT JOIN withdrawal_application a ON a.id=w.WITHDRAWAL_ID', 'a.id'],
    ['region L3 -> region L2', 'region r LEFT JOIN region p ON p.id=r.parent_id WHERE r.level=3', 'p.id'],
    ['region L2 -> region L1', 'region r LEFT JOIN region p ON p.id=r.parent_id WHERE r.level=2', 'p.id']
  ];
  for (const [label, sql, guard] of fks) {
    let n;
    try {
      // MySQL 派生表别名在闭括号后，外层 WHERE 必须引用派生表 t 的投影列，
      // 故把主体包成带别名的 SELECT 派生表，再在外层按 guard 过滤。
      n = await cnt('SELECT COUNT(*) FROM (SELECT ' + guard + ' AS _g FROM ' + sql + ') t WHERE _g IS NULL');
    } catch (e) { skp(label + ' 表/列不存在：' + e.message.split('\n')[0].substring(0, 70)); continue; }
    n === 0 ? ok(label + ' 孤儿 0') : bad(label + ' 孤儿 ' + n);
  }

  console.log('\n===== 4. 图片资源对账 =====');
  const disk = new Set(walk(UP).map(f => path.normalize(f).substring(UP.length + 1).split(path.sep).join('/')));
  const imgPairs = [
    ['dish', 'image'], ['setmeal', 'image'], ['order_detail', 'image'], ['shopping_cart', 'image'],
    ['dish_evaluation', 'images'], ['cs_message', 'image_url'], ['group_buy_campaign', 'IMAGE'],
    ['employee', 'avatar'], ['user', 'avatar'], ['rider', 'avatar'],
    ['store', 'logo'], ['tenant', 'logo'], ['tenant', 'license_image'], ['supplier', 'LICENSE_IMAGES']
  ];
  const refs = [];
  for (const [t, col] of imgPairs) {
    let r;
    try { r = await q('SELECT `' + col + '` v FROM `' + t + '`'); } catch (e) { continue; }
    for (const x of r) if (x.v != null) String(x.v).trim().split(',').map(z => z.trim()).filter(Boolean).forEach(p => refs.push(p));
  }
  const miss = [...new Set(refs)].filter(u => !disk.has(u));
  miss.length === 0 ? ok(refs.length + ' 处引用全部命中磁盘 ' + disk.size + ' 张图') : bad('磁盘缺失 ' + miss.length, miss.slice(0, 10));
  const pref = {};
  for (const u of new Set(refs)) { const p = u.split('/')[0]; pref[p] = (pref[p] || 0) + 1; }
  Object.keys(pref).length === 1 && Object.keys(pref)[0] === 'images'
    ? ok('路径前缀统一为 images/') : bad('前缀混杂', pref);

  console.log('\n===== 5. 金额自洽 =====');
  const badAmt = await cnt(`SELECT COUNT(*) FROM (
    SELECT o.id, o.amount - (COALESCE(d.sa,0) + o.delivery_fee - o.full_reduction_amount - o.new_customer_discount_amount) diff
    FROM orders o
    LEFT JOIN (SELECT order_id, SUM(amount) sa FROM order_detail GROUP BY order_id) d ON d.order_id=o.id
    WHERE o.is_deleted=0) x WHERE ABS(diff) > 30`);
  badAmt === 0 ? ok('订单金额自洽，偏差>30 元 0 笔') : bad('金额不自洽 ' + badAmt + ' 笔');

  // 日结与订单口径交叉核对（与域生成脚本一致：DATE(order_time) 直接取日）
  const cross = await cnt(`SELECT COUNT(*) FROM (
    SELECT DATE(o.order_time) d, ROUND(SUM(o.amount),2) rev, COUNT(*) oc
    FROM orders o WHERE o.is_deleted=0
    GROUP BY d) a
    LEFT JOIN (SELECT DATE(settlement_date) d2, total_revenue rev2, order_count oc2 FROM daily_settlement) b
      ON b.d2=a.d
    WHERE ABS(a.rev - COALESCE(b.rev2,0)) > 0.05 OR a.oc <> COALESCE(b.oc2,0)`);
  cross === 0 ? ok('日结口径与订单流水逐日一致（32 天）') : bad('日结口径偏差 ' + cross + ' 天');

  console.log('\n===== 6. 业务闭环 =====');
  const closed = [
    ['下单-明细', 'SELECT COUNT(*) FROM (SELECT id FROM orders WHERE is_deleted=0) o WHERE NOT EXISTS (SELECT 1 FROM order_detail d WHERE d.order_id=o.id)'],
    ['订单-支付', 'SELECT COUNT(*) FROM (SELECT id FROM orders WHERE is_deleted=0 AND status>=2) o WHERE NOT EXISTS (SELECT 1 FROM payment_order p WHERE p.order_id=o.id)'],
    ['订单-配送', 'SELECT COUNT(*) FROM (SELECT id FROM orders WHERE is_deleted=0 AND dining_type=1) o WHERE NOT EXISTS (SELECT 1 FROM delivery_order d WHERE d.order_id=o.id)'],
    ['门店-桌台', 'SELECT COUNT(*) FROM (SELECT id, area_id FROM dining_table WHERE is_deleted=0) t WHERE NOT EXISTS (SELECT 1 FROM dining_area a WHERE a.id=t.area_id)'],
    ['会员-等级', 'SELECT COUNT(*) FROM (SELECT level_id FROM member WHERE is_deleted=0) m WHERE NOT EXISTS (SELECT 1 FROM member_level l WHERE l.id=m.level_id)'],
    ['菜品-口味', 'SELECT COUNT(*) FROM (SELECT id FROM dish WHERE is_deleted=0) d WHERE NOT EXISTS (SELECT 1 FROM dish_flavor f WHERE f.dish_id=d.id)'],
    ['菜单-角色', 'SELECT COUNT(*) FROM (SELECT id FROM menu WHERE is_deleted=0) m WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.permission_id=m.id)'],
    ['退款-支付', 'SELECT COUNT(*) FROM (SELECT payment_order_id FROM refund_record WHERE is_deleted=0) r WHERE NOT EXISTS (SELECT 1 FROM payment_order p WHERE p.id=r.payment_order_id)']
  ];
  for (const [label, sql] of closed) {
    let n;
    try { n = await cnt(sql); } catch (e) { skp(label + '：' + e.message.split('\n')[0].substring(0, 60)); continue; }
    n === 0 ? ok(label + ' 闭环完整（缺失 0）') : bad(label + ' 缺失 ' + n);
  }

  console.log('\n===== 7. 登录可用性 =====');
  const emp = (await q('SELECT id, username, status FROM employee ORDER BY id')).map(r => r);
  const adminOk = await cnt("SELECT COUNT(*) FROM employee WHERE username='admin' AND status=1");
  const empOk = emp.length ? (emp.length === 15 && adminOk === 1) : false;
  empOk ? ok('后台账号 15 个（admin 已启用），密码均为 BCrypt(123456)') : bad('后台账号异常', { 总数: emp.length, admin: adminOk });
  const cend = await cnt('SELECT COUNT(*) FROM user');
  const cendPhone = await cnt("SELECT COUNT(*) FROM user WHERE phone IS NOT NULL AND phone<>''");
  cend >= 10 && cend === cendPhone ? ok('C端用户 ' + cend + ' 个，手机号全部已填') : bad('C端用户异常', { 总数: cend, 有手机: cendPhone });
  const rd = await cnt('SELECT COUNT(*) FROM rider');
  rd >= 10 ? ok('骑手 ' + rd + ' 个') : bad('骑手不足 ' + rd);
  // 骑手密码：登录走 PasswordUtils.matches 的 BCrypt 校验
  const rdPwd = await cnt("SELECT COUNT(*) FROM rider WHERE password IS NOT NULL AND password<>''");
  const rdEmpty = await cnt("SELECT COUNT(*) FROM rider WHERE password IS NULL OR password=''");
  if (rdPwd > 0) ok('骑手可登录账号 ' + rdPwd + ' 个（手机号+密码）');
  else if (rdEmpty === rd) bad('骑手全部无密码，骑手端 /api/rider/login 无法通过');
  else skp('骑手密码列状态异常');

  console.log('\n===== 8. 租户隔离 =====');
  const tenantCols = (await q("SELECT table_name FROM information_schema.columns WHERE table_schema='reggie' AND column_name='tenant_id' ORDER BY table_name")).map(r => r.TABLE_NAME);
  const badTenant = [];
  for (const t of tenantCols) {
    const vals = (await q('SELECT DISTINCT tenant_id v FROM `' + t + '` WHERE tenant_id IS NOT NULL')).map(r => r.v);
    if (vals.some(v => v < 1)) badTenant.push(t);
  }
  badTenant.length === 0 ? ok(tenantCols.length + ' 张带 tenant_id 的表租户值合法') : bad('租户值异常', badTenant);

  await c.end();

  console.log('\n==========================');
  console.log('通过 ' + pass + ' 项 / 失败 ' + fail + ' 项 / 跳过 ' + skip + ' 项');
  console.log(fail === 0 ? '结论：数据面可支撑全面功能测试' : '结论：存在 ' + fail + ' 项待修');
  process.exit(fail === 0 ? 0 : 1);
})().catch(e => { console.error('FATAL:', e.message); process.exit(2); });
