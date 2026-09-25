// domain-d-order.js — 瑞吉外卖 D域(订单/履约/支付)真实数据生成器
// 用法: node scripts/domain-d-order.js
// 行为: 删除并重写 14 张表，金额逻辑自洽；外部引用(dish/user/address/dining_table/store/region)只读
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const OUT = path.join(__dirname, '..', 'src/main/resources/db/seed/domain-d-order.sql');

// 可复现的确定性随机（保证金额自洽与重复运行一致）
let _seed = 20260925;
function rnd() { _seed = (_seed * 1103515245 + 12345) & 0x7fffffff; return _seed / 0x7fffffff; }
const ri = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = arr => arr[Math.floor(rnd() * arr.length)];
const rf = (a, b, d = 2) => +( (a + rnd() * (b - a)).toFixed(d) );
const pad2 = n => String(n).padStart(2, '0');
const pad3 = n => String(n).padStart(3, '0');
function dt(d) {
  return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) + ' ' +
    pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());
}

(async () => {
  const c = await mysql.createConnection({
    host: 'localhost', port: 3306, user: 'root', password: '123456',
    database: 'reggie', charset: 'utf8mb4', decimalNumbers: false
  });
  const q = async s => { const [r] = await c.query(s); return r; };
  const one = async s => { const r = await q(s); return r[0]; };

  // ---- 读取引用数据 ----
  const dishes = (await q("SELECT id,name,price,image FROM dish WHERE is_deleted=0 AND status=1 ORDER BY id")).map(r =>
    ({ id: +r.id, name: r.name, price: +r.price, image: r.image }));
  const users = (await q("SELECT id,name,tenant_id FROM user ORDER BY id")).map(r => ({ id: +r.id, name: r.name, tid: +r.tenant_id }));
  const addrs = (await q("SELECT id,user_id FROM address_book ORDER BY id")).map(r => ({ id: +r.id, uid: +r.user_id }));
  const tables = (await q("SELECT id,name FROM dining_table ORDER BY id")).map(r => ({ id: +r.id, name: r.name }));
  const tenantId = 1;

  if (!dishes.length || !users.length || !addrs.length || !tables.length)
    throw new Error('引用数据缺失: dish/user/address/dining_table');
  console.log(`引用: dish=${dishes.length} user=${users.length} addr=${addrs.length} table=${tables.length}`);

  // ---- 构造订单（先算金额，再落库）----
  const N = 60;
  const orders = [];       // 待插入
  const details = [];      // 待插入
  const dstart = new Date(2026, 7, 25, 9, 0, 0);  // 2026-08-25
  const dend = new Date(2026, 8, 24, 21, 0, 0);   // 2026-09-24
  const span = dend - dstart;

  // 状态分布：待付款/已支付/已完成/已取消/已退款
  function pickStatus(i) {
    if (i < 3) return 1;            // 待付款
    if (i < 5) return 5;            // 已取消
    if (i < 7) return 4;            // 已退款
    if (i < 13) return 2;           // 已支付（配送中）
    return 3;                       // 已完成（主流）
  }

  const dishById = Object.fromEntries(dishes.map(d => [d.id, d]));

  for (let i = 0; i < N; i++) {
    const id = 1000 + i + 1;                    // 1001-1060
    const status = pickStatus(i);
    const isTakeaway = rnd() < 0.72;            // 约 72% 外卖
    const u = pick(users);
    // 下单时间
    let t = new Date(dstart.getTime() + rnd() * span);
    if (t.getHours() < 9) t = new Date(t.getTime() + (9 - t.getHours()) * 3600000);
    if (t.getHours() > 21) t = new Date(t.getTime() - (t.getHours() - 21) * 3600000);

    // 明细 2-6 行
    const cnt = ri(2, 6);
    const chosen = [...dishes].sort(() => rnd() - 0.5).slice(0, cnt);
    let sub = 0, qtyTotal = 0;
    const lines = chosen.map(d => {
      const q = ri(1, 3);
      const amt = +(d.price * q).toFixed(2);
      sub = +(sub + amt).toFixed(2); qtyTotal += q;
      return { ...d, qty: q, amount: amt };
    });

    const packAmount = +(qtyTotal * 1.0).toFixed(2);
    const deliveryFee = isTakeaway ? rf(3, 9, 2) : 0;
    const fullReduction = rf(0, 8, 2);
    const newCust = (u.id % 7 === 0) ? rf(0, 5, 2) : 0;
    const total = +(sub + packAmount + deliveryFee - fullReduction - newCust).toFixed(2);

    const addr = isTakeaway ? pick(addrs.filter(a => a.uid === u.id) || addrs) : null;
    const table = !isTakeaway ? pick(tables) : null;
    const paid = status >= 2;
    const orderTime = t;
    const checkoutTime = paid ? new Date(t.getTime() + ri(1, 5) * 60000) : null;
    const expectDelivery = isTakeaway ? new Date(t.getTime() + 45 * 60000) : null;
    const dispatchTime = (status === 2 || status === 3) ? new Date(t.getTime() + ri(6, 15) * 60000) : null;

    orders.push({
      id, number: '2026' + pad2(t.getMonth() + 1) + pad2(t.getDate()) + String(100000 + id).slice(1),
      status, user_id: u.id, address_book_id: addr ? addr.id : null,
      order_time: orderTime, checkout_time: checkoutTime,
      pay_method: isTakeaway ? (rnd() < 0.6 ? 1 : 2) : (rnd() < 0.5 ? 3 : 1), // 1微信 2支付宝 3现金
      amount: total, delivery_fee: deliveryFee, full_reduction_amount: fullReduction,
      new_customer_discount_amount: newCust,
      remark: pick([null, '微辣', '不要香菜', '多放餐具', '尽快送达', '少油少盐']),
      internal_remark: null, expect_delivery_time: expectDelivery,
      user_name: u.name, phone: null, address: isTakeaway ? '北京市海淀区中关村大街1号' : null,
      consignee: isTakeaway ? u.name : null,
      dining_type: isTakeaway ? 1 : 2,
      table_id: table ? table.id : null, table_name: table ? table.name : null,
      idempotency_key: 'idem-' + id, stock_refunded: 0, used_coupon_id: null, rider_id: null,
      dispatch_time: dispatchTime, platform_type: 1, platform_order_id: null,
      platform_shop_id: null, platform_raw: null,
      create_time: orderTime, update_time: paid ? checkoutTime : orderTime,
      create_user: 0, update_user: 0, is_deleted: 0, tenant_id: tenantId,
      version: 1, master_order_id: null, split_count: 1
    });
    lines.forEach((l, k) => details.push({
      id: details.length + 1, name: l.name, order_id: id, dish_id: l.id, setmeal_id: null,
      dish_flavor: ri(1, 2) ? '默认' : '微辣', number: l.qty, amount: l.amount,
      remark: null, image: l.image, tenant_id: tenantId,
      create_time: orderTime, update_time: orderTime, create_user: 0, update_user: 0, is_deleted: 0
    }));
  }

  // ---- 购物车（15 用户 × 2 行）----
  const carts = [];
  for (let i = 0; i < 30; i++) {
    const u = users[i % Math.min(15, users.length)];
    const d = pick(dishes);
    const qty = ri(1, 3);
    carts.push({
      id: 100 + i, name: d.name, user_id: u.id, dish_id: d.id, setmeal_id: null,
      dish_flavor: '默认', number: qty, amount: +(d.price * qty).toFixed(2),
      image: d.image, tenant_id: tenantId,
      create_time: new Date(dstart.getTime() + ri(0, 5) * 86400000)
    });
  }

  // ---- 支付单：已支付及以上(>=2)订单 ----
  const paidOrders = orders.filter(o => o.status >= 2);
  const pays = paidOrders.map((o, i) => ({
    id: 5000 + i, order_id: o.id, tenant_id: tenantId,
    trade_no: 'T202609' + String(100000 + o.id),
    channel_trade_no: 'C' + String(20260901000000 + o.id),
    channel: o.pay_method === 3 ? 3 : (o.pay_method === 1 ? 1 : 2),
    amount: o.amount, status: o.status === 4 ? 2 : 1,  // 1成功 2退款
    paid_time: o.checkout_time, notify_time: new Date(o.checkout_time.getTime() + ri(1, 20) * 1000),
    created_time: o.create_time, update_time: o.checkout_time, is_deleted: 0, version: 1,
    create_user: 0, update_user: 0
  }));

  // ---- 退款单：已退款订单(status=4) ----
  const refunded = orders.filter(o => o.status === 4);
  const refunds = refunded.slice(0, 5).map((o, i) => ({
    id: 9000 + i, payment_order_id: 5000 + paidOrders.indexOf(o), order_id: o.id, tenant_id: tenantId,
    refund_no: 'R202609' + String(100000 + o.id), amount: o.amount,
    reason: pick(['用户取消', '配送超时', '菜品口味问题', '重复下单']),
    status: 2, refund_type: 1, apply_user_id: o.user_id,
    created_time: new Date(o.create_time.getTime() + 3600000), is_deleted: 0, version: 1,
    create_user: o.user_id, update_user: 0,
    update_time: new Date(o.create_time.getTime() + 7300000),
    audit_user_id: 1, audit_time: new Date(o.create_time.getTime() + 7200000),
    reject_reason: null, refund_time: new Date(o.create_time.getTime() + 7300000)
  }));

  // ---- 骑手 10 人 ----
  const riderNames = ['张伟', '李强', '王磊', '赵刚', '刘洋', '陈涛', '杨帆', '黄海', '周军', '吴斌'];
  const riders = riderNames.map((n, i) => ({
    id: i + 1, name: n, phone: '138' + String(10000000 + i * 37).slice(0, 8),
    password: null, avatar: null,
    current_longitude: +(116.3 + rnd() * 0.2).toFixed(6),
    current_latitude: +(39.9 + rnd() * 0.2).toFixed(6),
    status: pick([0, 1, 2, 3]),            // 0离线 1空闲 2配送中 3忙碌
    current_order_count: pick([0, 0, 1, 1, 2]),
    total_order_count: ri(800, 3200), rating: rf(4.6, 5.0, 1),
    last_location_time: new Date(dend.getTime() - ri(0, 12) * 3600000),
    tenant_id: tenantId,
    create_time: new Date(2026, 4, 1), update_time: new Date(2026, 8, 25)
  }));

  // ---- 配送单：外卖且已支付以上 ----
  const delivOrders = orders.filter(o => o.dining_type === 1 && o.status >= 2);
  const delivs = delivOrders.map((o, i) => {
    const r = pick(riders);
    return {
      id: 3000 + i, tenant_id: tenantId, platform_order_id: null, platform: 1,
      order_id: o.id, dish_summary: details.filter(d => d.order_id === o.id).map(d => d.name + '×' + d.number).join('、'),
      amount: o.amount, user_name: o.user_name, phone: null,
      address: '北京市' + pick(['海淀区中关村', '朝阳区望京', '东城区东直门', '丰台区南三环']) + '附近',
      status: o.status === 3 ? 3 : (o.status === 2 ? 2 : 1),   // 1待接 2配送中 3已送达
      order_time: o.order_time,
      created_time: o.dispatch_time || o.create_time,
      update_time: o.checkout_time, created_user: 0, update_user: 0, is_deleted: 0, version: 1
    };
  });

  // ---- 配送范围规则（门店 1-10）----
  const storeIds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  const ranges = storeIds.map((sid, i) => ({
    id: i + 1, rule_name: pick(['默认配送范围', '工作日配送范围', '节假日配送范围']),
    range_type: 1,                                  // 1圆形
    center_longitude: +(116.3 + i * 0.02).toFixed(6),
    center_latitude: +(39.9 + i * 0.02).toFixed(6),
    radius: ri(2, 5), polygon_points: null, fee_type: 1,
    base_fee: rf(2, 4, 2), fee_per_km: rf(1, 2.5, 2), min_fee: rf(3, 5, 2),
    max_fee: rf(10, 15, 2), free_threshold: ri(25, 50),
    status: 1, sort_order: i + 1,
    remark: null, tenant_id: sid,
    create_time: new Date(2026, 7, 1), update_time: new Date(2026, 8, 20),
    create_user: 1, update_user: 1
  }));

  // ---- 配送费阶梯（15 行，挂在规则上）----
  const feeSteps = [];
  let fsId = 1;
  ranges.forEach((r, i) => {
    const steps = ri(1, 3);
    for (let k = 0; k < steps; k++) {
      const sd = +(k * 1.5).toFixed(1);
      const ed = +(sd + 1.5).toFixed(1);
      feeSteps.push({
        id: fsId++, rule_id: r.id,
        start_distance: sd, end_distance: ed,
        fee: +(r.base_fee + sd * r.fee_per_km).toFixed(2),
        increment_distance: 1, increment_fee: r.fee_per_km, sort_order: k + 1,
        tenant_id: r.tenant_id, create_time: new Date(2026, 7, 1), update_time: new Date(2026, 8, 20)
      });
    }
  });

  // ---- 骑手位置记录 25 条 ----
  const locs = [];
  for (let i = 0; i < 25; i++) {
    const r = pick(riders);
    locs.push({
      id: i + 1, rider_id: r.id, order_id: pick(delivOrders).id,
      longitude: +(116.3 + rnd() * 0.2).toFixed(6),
      latitude: +(39.9 + rnd() * 0.2).toFixed(6),
      speed: ri(15, 40), direction: ri(0, 359),
      record_time: new Date(dend.getTime() - ri(0, 48) * 3600000),
      tenant_id: tenantId, create_time: new Date(2026, 7, 1)
    });
  }

  // ---- 配送时效 20 条 ----
  const doneOrders = orders.filter(o => o.status === 3);
  const times = doneOrders.slice(0, 20).map((o, i) => {
    const accept = new Date(o.order_time.getTime() + ri(3, 10) * 60000);
    const pickup = new Date(accept.getTime() + ri(8, 18) * 60000);
    const deliver = new Date(pickup.getTime() + ri(20, 35) * 60000);
    return {
      id: i + 1, order_id: o.id, order_number: o.number, rider_id: pick(riders).id,
      rider_name: pick(riderNames), order_time: o.order_time, accept_time: accept,
      pickup_time: pickup, deliver_time: deliver,
      estimated_minutes: 45, actual_minutes: Math.round((deliver - o.order_time) / 60000),
      distance: rf(1.2, 6.5, 1), status: 3, remark: null,
      tenant_id: tenantId, create_time: deliver, update_time: deliver
    };
  });

  // ---- 后厨小票 25 条 ----
  const kitchenStatus = [1, 1, 2, 2, 3, 3];   // 1待做 2制作中 3已出餐
  const tickets = orders.filter(o => o.status >= 2).slice(0, 25).map((o, i) => {
    const dt0 = o.order_time;
    const recv = new Date(dt0.getTime() + 60000);
    const start = new Date(recv.getTime() + ri(1, 4) * 60000);
    const ready = new Date(start.getTime() + ri(4, 10) * 60000);
    const st = pick(kitchenStatus);
    return {
      id: 7000 + i, tenant_id: tenantId, order_id: o.id, order_no: o.number,
      order_type: o.dining_type, table_name: o.table_name,
      customer_count: pick([1, 2, 2, 3, 4]),
      status: st, urgent: rnd() < 0.15 ? 1 : 0,
      dish_summary: details.filter(d => d.order_id === o.id).map(d => d.name + '×' + d.number).join('、'),
      receive_time: recv, cook_start_time: start,
      ready_time: st >= 3 ? ready : null,
      finish_time: st >= 3 ? new Date(ready.getTime() + 60000) : null,
      cancel_time: null,
      cook_duration_seconds: st >= 3 ? Math.round((ready - start) / 1000) : null,
      station_code: pick(['MAIN', 'FRIED', 'COOL', 'DESSERT']),
      remark: null, create_time: recv, update_time: st >= 3 ? ready : start,
      create_user: 0, update_user: 0, is_deleted: 0
    };
  });

  // ---- 收银记录 25 条 ----
  const cashiers = paidOrders.slice(0, 25).map((o, i) => {
    const cashier = pick([1, 2, 3, 4, 5]);   // 收银员工号
    return {
      id: 6000 + i, order_id: o.id, order_number: o.number,
      pay_type: o.pay_method, amount: o.amount, actual_amount: o.amount,
      change_amount: 0, cashier_time: o.checkout_time,
      cashier_id: cashier, cashier_name: pick(['张敏', '李芳', '王娟', '刘丽', '陈燕']),
      remark: null, tenant_id: tenantId, create_time: o.checkout_time, create_user: 0
    };
  });

  // ---- 日结 15 天 ----
  const daily = [];
  for (let i = 0; i < 15; i++) {
    const d = new Date(2026, 8, 11); d.setDate(d.getDate() + i);
    const oc = ri(120, 180);
    const total = +(oc * rf(38, 62, 2)).toFixed(2);
    const cash = +(total * rf(0.10, 0.18)).toFixed(2);
    const wechat = +(total * rf(0.35, 0.45)).toFixed(2);
    const alipay = +(total * rf(0.25, 0.35)).toFixed(2);
    const other = +(total * rf(0.03, 0.08)).toFixed(2);
    const refund = +(total * rf(0.005, 0.02)).toFixed(2);
    const material = +(total * rf(0.35, 0.45)).toFixed(2);
    const labor = +(total * rf(0.12, 0.18)).toFixed(2);
    const otherCost = +(total * rf(0.05, 0.09)).toFixed(2);
    const totalCost = +(material + labor + otherCost).toFixed(2);
    const gross = +(total - totalCost).toFixed(2);
    const rate = +(gross / total * 100).toFixed(2);
    daily.push({
      id: 8000 + i, settlement_date: new Date(d.getFullYear(), d.getMonth(), d.getDate()),
      total_revenue: total, cash_income: cash, wechat_income: wechat, alipay_income: alipay,
      bankcard_income: 0, other_income: other,
      order_count: oc, refund_amount: refund, refund_count: ri(1, 4),
      net_income: total - refund,
      material_cost: material, labor_cost: labor, other_cost: otherCost,
      total_cost: totalCost, gross_profit: gross, profit_rate: rate,
      status: 1,
      settlement_time: new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1, 9, 30, 0),
      settlement_user_id: 1, settlement_user_name: '张建国',
      remark: null, tenant_id: tenantId, create_time: new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1, 9, 30, 0),
      update_time: new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1, 9, 30, 0),
      create_user: 1, update_user: 1, version: 1
    });
  }

  // ================= 执行 DELETE + INSERT =================
  const TABLES = [
    ['rider', riders], ['orders', orders], ['order_detail', details], ['shopping_cart', carts],
    ['payment_order', pays], ['refund_record', refunds], ['delivery_order', delivs],
    ['delivery_range_rule', ranges], ['delivery_fee_step', feeSteps],
    ['rider_location_record', locs], ['delivery_time_record', times],
    ['kitchen_ticket', tickets], ['cashier_record', cashiers], ['daily_settlement', daily]
  ];

  const esc = v => {
    if (v === null || v === undefined) return 'NULL';
    if (v instanceof Date) return "'" + dt(v) + "'";
    if (typeof v === 'boolean') return v ? '1' : '0';
    if (typeof v === 'number') return String(v);
    return "'" + String(v).replace(/\\/g, '\\\\').replace(/'/g, "''") + "'";
  };

  const sqlParts = ['SET NAMES utf8mb4;'];
  for (const [tbl, rows] of TABLES) {
    const cols = (await q(`SHOW COLUMNS FROM \`${tbl}\``)).map(r => r.Field);
    await c.query(`DELETE FROM \`${tbl}\``);
    const insCols = cols;
    const vals = rows.map(r => '(' + insCols.map(cc => esc(r[cc])).join(',') + ')');
    const sql = `INSERT INTO \`${tbl}\` (${insCols.map(cc => '`' + cc + '`').join(',')}) VALUES ${vals.join(',\n')};`;
    await c.query(sql);
    sqlParts.push(`-- ${tbl}: ${rows.length} 行`);
    sqlParts.push(`DELETE FROM \`${tbl}\`;`);
    sqlParts.push(sql);
    console.log(`  [OK] ${tbl.padEnd(24)} ${rows.length} 行`);
  }
  await c.end();

  fs.writeFileSync(OUT, sqlParts.join('\n'), 'utf8');
  console.log(`\n[SQL 落盘] ${OUT}`);
  console.log('总写入: ' + TABLES.reduce((s, t) => s + t[1].length, 0) + ' 行');
})().catch(e => { console.error('FAIL:', e.message, e.stack); process.exit(1); });
