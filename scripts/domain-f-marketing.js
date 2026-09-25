// domain-f-marketing.js — F域(优惠券/积分/充值/营销活动/拼团)真实数据生成器
// 用法: node scripts/domain-f-marketing.js
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const OUT = path.join(__dirname, '..', 'src/main/resources/db/seed/domain-f-marketing.sql');

let _s = 8181;
const rnd = () => { _s = (_s * 1103515245 + 12345) & 0x7fffffff; return _s / 0x7fffffff; };
const ri = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = a => a[Math.floor(rnd() * a.length)];
const rf = (a, b, d = 2) => +((a + rnd() * (b - a)).toFixed(d));
const pad2 = n => String(n).padStart(2, '0');
const dt = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie', charset: 'utf8mb4' });
  const q = async s => { const [r] = await c.query(s); return r; };
  const TID = 1;
  const NOW = new Date(2026, 8, 25, 18, 0, 0);
  const ago = (days, h) => { const d = new Date(NOW); d.setDate(d.getDate() - days); d.setHours(h || ri(9, 20), ri(0, 59), 0, 0); return d; };
  const later = (days, h) => { const d = new Date(NOW); d.setDate(d.getDate() + days); d.setHours(h || ri(9, 20), ri(0, 59), 0, 0); return d; };

  const members = (await q('SELECT id FROM member ORDER BY id')).map(r => +r.id);
  const users = (await q('SELECT id FROM user ORDER BY id')).map(r => +r.id);
  const dishes = (await q('SELECT id,name,price FROM dish WHERE is_deleted=0 ORDER BY id')).map(r => ({ id: +r.id, name: r.name, price: +r.price }));
  const orders = (await q('SELECT id,order_time,amount FROM orders WHERE is_deleted=0 ORDER BY id')).map(r => ({ id: +r.id, t: r.order_time, amt: +r.amount }));
  console.log(`引用: member=${members.length} user=${users.length} dish=${dishes.length} order=${orders.length}`);

  // ---------- 优惠券模板（12）----------
  const templates = [
    ['满减券·满30减5', 1, 30, 5, null],
    ['满减券·满50减10', 1, 50, 10, null],
    ['满减券·满80减20', 1, 80, 20, null],
    ['满减券·满100减30', 1, 100, 30, null],
    ['折扣券·全场85折', 2, 0, null, 0.85],
    ['折扣券·全场9折', 2, 0, null, 0.90],
    ['折扣券·全场8折', 2, 0, null, 0.80],
    ['新用户专享·满20减8', 1, 20, 8, null],
    ['周末特惠·满40减12', 1, 40, 12, null],
    ['节日限定·满60减18', 1, 60, 18, null],
    ['下午茶专享·满15减6', 1, 15, 6, null],
    ['会员回馈·满50减15', 1, 50, 15, null]
  ].map((t, i) => ({
    id: i + 1, tenant_id: TID, name: t[0], type: t[1],
    condition_amount: t[2], discount_amount: t[3], discount_rate: t[4],
    total_count: ri(200, 1000), remain_count: 0, valid_days: ri(7, 30),
    status: 1, created_time: ago(ri(5, 60)), update_time: NOW,
    create_user: 1, update_user: 1, is_deleted: 0
  }));
  // 领取统计回填
  const couponUsers = [];
  for (const t of templates) {
    // 唯一键 uk_member_template(member_id, template_id)：每个会员每模板最多领 1 张
    const shuffled = [...members].sort(() => rnd() - 0.5);
    const n = Math.min(ri(10, 18), shuffled.length);
    for (let i = 0; i < n; i++) {
      const m = shuffled[i];
      const used = rnd() < 0.45;
      const ord = used ? pick(orders) : null;
      const ctime = ago(ri(0, 30), ri(10, 21));
      couponUsers.push({
        id: couponUsers.length + 1, tenant_id: TID, member_id: m, template_id: t.id,
        code: 'CP' + (300000 + couponUsers.length * 7),
        status: used ? 2 : (ctime.getTime() + t.valid_days * 86400000 < NOW.getTime() ? 3 : 1),
        used_time: used ? ord.t : null, order_id: used ? ord.id : null,
        expire_time: new Date(ctime.getTime() + t.valid_days * 86400000),
        created_time: ctime, update_time: NOW, create_user: 0, update_user: 0, is_deleted: 0
      });
    }
    t.remain_count = Math.max(0, t.total_count - n);
  }

  // ---------- 积分记录（18）----------
  // 唯一键 uq_points_biz(member_id, biz_type, biz_id, type)：每个成员每笔订单只能有 1 条积分
  const points = [];
  let pid = 0;
  for (const m of members) {
    const ord = orders[pid];
    pid++;
    points.push({
      id: pid, tenant_id: TID, member_id: m, type: 1,
      points: ri(10, 100), biz_type: 'ORDER', biz_id: ord.id,
      remark: '消费奖励 订单#' + ord.id,
      expire_time: later(ri(30, 180), 23), created_time: ago(ri(0, 50), ri(9, 22)),
      update_time: NOW, create_user: 0, update_user: 0, is_deleted: 0
    });
  }

  // ---------- 充值记录（20）----------
  const recharges = members.slice(0, 18).map((m, i) => {
    const amt = pick([50, 100, 100, 200, 300, 500]);
    return {
      id: i + 1, tenant_id: TID, member_id: m, user_id: users[i % users.length],
      recharge_no: 'RC2026' + pad2(ago(1).getMonth() + 1) + pad2(ri(1, 25)) + String(1000 + i),
      status: pick([1, 1, 1, 1, 2, 3]),
      amount: amt, gift_amount: pick([0, 5, 10, 20, 30]),
      payment_method: pick([1, 1, 2, 3]), trade_no: 'PAY' + (2026090100000 + i),
      confirm_employee_id: 1, confirm_time: ago(ri(0, 20), 14),
      created_time: ago(ri(0, 20), 12), update_time: NOW,
      create_user: 0, update_user: 0, is_deleted: 0
    };
  });

  // ---------- 营销活动（12）----------
  const campaigns = [
    ['满30减5·周末狂欢', 1, 1, 0, 0, 1],
    ['满50减10·工作日午市', 1, 1, 0, 0, 1],
    ['全场85折·新品尝鲜', 2, 2, 0, 0, 1],
    ['满80减20·聚餐专场', 1, 1, 0, 0, 1],
    ['新用户立减8元', 1, 3, 0, 0, 1],
    ['下午茶专享满15减6', 1, 1, 0, 0, 1],
    ['会员日专属9折', 2, 2, 0, 0, 1],
    ['满减组合·满100减30', 1, 1, 0, 0, 1],
    ['节日特惠·满60减18', 1, 1, 0, 0, 1],
    ['周末折扣8折', 2, 2, 0, 0, 0],
    ['开学季满减', 1, 1, 0, 0, 1],
    ['双十一预热·满50减15', 1, 1, 0, 0, 0]
  ].map((t, i) => ({
    id: i + 1, tenant_id: TID, name: t[0], description: '限时优惠活动，详见活动说明',
    campaign_type: t[1], target_type: t[2], target_value: t[3],
    rule_json: JSON.stringify({ type: t[1], discount: t[1] === 1 ? t[4] : t[4] }),
    status: t[5], priority: i + 1,
    start_time: ago(ri(0, 40), 10), end_time: later(ri(3, 40), 23),
    max_participants: ri(100, 500), current_participants: ri(10, 300),
    coupon_template_id: i % templates.length + 1,
    create_user: 1, update_user: 1, create_time: ago(ri(0, 40), 10), update_time: NOW, is_deleted: 0
  }));

  // ---------- 营销消息（40）----------
  const messages = [];
  const titles = ['🎉 周末满减来袭', '🔥 新品上线特惠', '🎁 会员专享折扣', '📣 限时秒杀开启', '🍜 午市专享优惠', '💝 回馈老客户', '🎊 节日活动开启', '⚡ 闪购限时优惠'];
  for (let i = 0; i < 40; i++) {
    const st = pick([1, 2, 2, 3, 3, 4]);
    messages.push({
      id: i + 1, tenant_id: TID, campaign_id: ri(1, campaigns.length),
      user_id: pick(users), push_type: pick([1, 2, 3]),   // 1站内 2短信 3推送
      title: pick(titles), content: pick(['满减活动进行中，下单立减，数量有限！', '新品尝鲜，全场85折，快来试试！', '老会员专享优惠，积分可抵扣现金。', '限时秒杀，每日11点准时开抢！']),
      status: st,
      read_time: st >= 2 ? ago(ri(0, 15), ri(9, 22)) : null,
      use_time: st === 4 ? ago(ri(0, 10), ri(9, 22)) : null,
      create_time: ago(ri(0, 20), ri(9, 22)), is_deleted: 0,
      create_user: 1, update_time: NOW, update_user: 1
    });
  }

  // ---------- 满减规则（12）----------
  // 每个 campaign 一条规则，避免过滤后不足 10 条
  const fullReductions = campaigns.map((c, i) => ({
    id: i + 1, campaign_id: c.id, rule_name: c.name,
    discount_type: c.campaign_type === 1 ? 1 : 2,
    min_amount: c.campaign_type === 1 ? pick([20, 30, 40, 50, 60, 80, 100]) : 0,
    discount_value: c.campaign_type === 1 ? pick([5, 8, 10, 12, 15, 18, 20, 30]) : 0,
    discount_rate: c.campaign_type === 2 ? pick([0.80, 0.85, 0.90]) : null,
    max_discount_amount: pick([10, 20, 30]), gift_dish_id: null, gift_quantity: 0,
    stackable: 0, daily_limit: 200, per_user_limit: 2, sort_order: i + 1,
    status: c.status, tenant_id: TID, create_user: 1, update_user: 1,
    create_time: c.create_time, update_time: NOW
  }));

  // ---------- 折扣规则（10）----------
  const discountRules = [
    ['全场85折', 2, 0, 0.85, 30, null, null, null],
    ['全场9折', 2, 0, 0.90, 20, null, null, null],
    ['全场8折', 2, 0, 0.80, 40, null, null, null],
    ['新品尝鲜95折', 2, 0, 0.95, 15, null, null, null],
    ['素菜类9折', 3, 1, 0.90, 15, 3, null, null],
    ['汤品88折', 3, 1, 0.88, 20, 4, null, null],
    ['饮品85折', 3, 1, 0.85, 10, 6, null, null],
    ['主食95折', 3, 1, 0.95, 8, 5, null, null],
    ['套餐8折', 3, 1, 0.80, 25, null, null, 1],
    ['会员95折', 2, 0, 0.95, 10, null, null, null]
  ].map((t, i) => ({
    id: i + 1, campaign_id: (i % 2 === 0 ? 3 : 7), rule_name: t[0],
    scope: t[1], discount_rate: t[3], max_discount_amount: t[4],
    min_consumption: t[2], category_id: t[5], dish_id: t[6], setmeal_id: t[7],
    daily_limit: 150, per_user_limit: 3, sort_order: i + 1,
    status: 1, tenant_id: TID, create_time: ago(ri(0, 30), 10),
    update_time: NOW, create_user: 1, update_user: 1
  }));

  // ---------- 活动使用记录（50）----------
  const usages = [];
  const usedOrders = orders.slice(0, 50);
  usedOrders.forEach((o, i) => {
    const isFull = rnd() < 0.55;
    const rule = isFull ? pick(fullReductions) : pick(discountRules);
    const disc = rule.discount_type === 1 ? rule.discount_value : +(o.amt * (1 - (rule.discount_rate || 0.9))).toFixed(2);
    usages.push({
      id: i + 1, campaign_id: rule.campaign_id, rule_id: rule.id,
      rule_type: isFull ? 1 : 2, quantity: 1,
      order_id: o.id, order_number: '2026' + pad2(o.t.getMonth() + 1) + pad2(o.t.getDate()) + String(100000 + o.id).slice(1),
      user_id: users[ri(0, users.length - 1)], order_amount: o.amt,
      discount_amount: disc, actual_amount: +(o.amt - disc).toFixed(2),
      use_time: o.t, tenant_id: TID, create_time: o.t
    });
  });

  // ---------- 秒杀（10）----------
  const flashSales = [dishes[0], dishes[1], dishes[5], dishes[8], dishes[12], dishes[20], dishes[30], dishes[40], dishes[50], dishes[55]].filter(Boolean).map((d, i) => ({
    id: i + 1, name: d.name + '·限时秒杀', description: '每日限时限量，售完即止',
    dish_id: d.id, dish_name: d.name,
    original_price: d.price, flash_price: +(d.price * rf(0.4, 0.65)).toFixed(2),
    total_quantity: ri(30, 100), sold_quantity: ri(0, 30), max_per_user: ri(1, 3),
    start_time: new Date(NOW.getTime() + (i < 5 ? -1 : 1) * 86400000 / 2),
    end_time: new Date(NOW.getTime() + (i < 5 ? 0 : 1) * 86400000),
    status: pick([1, 2, 3]), tenant_id: TID,
    create_time: ago(ri(0, 20), 10), update_time: NOW, create_user: 1, update_user: 1
  }));

  // ---------- 买赠（10）----------
  const buyGets = [
    ['红烧肉买2赠1', 2, 1, 1], ['宫保鸡丁买1赠1', 1, 2, 1],
    ['米饭买3赠1', 3, 5, 1], ['饮品买1赠1', 1, 6, 1],
    ['主食买2赠1', 2, 10, 1], ['小食买3赠1', 3, 20, 1],
    ['凉菜买1赠1', 1, 25, 1], ['素菜买2赠1', 2, 30, 1],
    ['汤羹买1赠1', 1, 35, 1], ['甜品买1赠1', 1, 45, 1]
  ].map((t, i) => ({
    id: i + 1, name: t[0], description: '指定菜品按量赠送同款或指定赠品',
    buy_quantity: t[1], get_quantity: t[3],
    dish_id: t[2], setmeal_id: null, gift_dish_id: t[2],
    gift_dish_name: dishes[t[2] - 1] ? dishes[t[2] - 1].name : '指定菜品',
    min_order_amount: pick([15, 20, 30, 40]), max_times_per_order: ri(1, 3),
    start_time: ago(ri(0, 15), 10), end_time: later(ri(5, 30), 23),
    status: pick([1, 1, 1, 2]), usage_count: ri(3, 80),
    tenant_id: TID, create_time: ago(ri(0, 15), 10), update_time: NOW,
    create_user: 1, update_user: 1
  }));

  // ---------- 新客立减（10）----------
  const newCust = [
    ['新用户立减5元', 1, 5, 10], ['新用户立减8元', 1, 8, 15],
    ['新用户立减10元', 1, 10, 20], ['新用户立减15元', 1, 15, 30],
    ['新用户9折', 2, 0.90, 20], ['新用户88折', 2, 0.88, 30],
    ['新客专享立减6元', 1, 6, 15], ['新客专享立减12元', 1, 12, 25],
    ['首次下单立减', 1, 8, 15], ['开学季新客立减', 1, 10, 20]
  ].map((t, i) => ({
    id: i + 1, name: t[0], discount_type: t[1], discount_value: t[2],
    max_discount_amount: t[1] === 2 ? pick([15, 20, 30]) : 0,
    min_order_amount: t[3], valid_days: ri(3, 15),
    status: pick([1, 1, 1, 1, 2]),
    remark: '仅限首次下单用户', tenant_id: TID,
    create_time: ago(ri(0, 30), 10), update_time: NOW,
    create_user: 1, update_user: 1
  }));

  // ---------- 拼团活动（10）+ 参与记录 ----------
  const groups = [1, 2, 3, 5, 12, 18, 25, 32, 40, 50].filter(i => dishes[i]).map((i, k) => ({
    ID: k + 1, TENANT_ID: TID, NAME: dishes[i].name + '·超值拼团',
    DESCRIPTION: '3人成团，享超值拼团价',
    GROUP_ID: 100000 + k,
    STATUS: String(pick([1, 2, 3])),
    START_TIME: ago(ri(0, 12), 10), END_TIME: later(ri(1, 15), 23),
    MIN_MEMBERS: 2, MAX_MEMBERS: 5,
    ORIGINAL_PRICE: dishes[i].price,
    GROUP_PRICE: +(dishes[i].price * rf(0.55, 0.75)).toFixed(2),
    DISH_ID: dishes[i].id, DISH_NAME: dishes[i].name, IMAGE: null,
    CREATE_TIME: ago(ri(0, 12), 10), UPDATE_TIME: NOW, IS_DELETED: 0
  }));
  const participations = [];
  groups.forEach(g => {
    const n = ri(2, 6);
    for (let i = 0; i < n; i++) {
      const jt = new Date(g.START_TIME.getTime() + i * 3600000 * 2);
      participations.push({
        ID: participations.length + 1, TENANT_ID: TID, GROUP_BUY_ID: g.ID,
        ORDER_ID: orders[i % orders.length].id, USER_ID: users[ri(0, users.length - 1)],
        STATUS: i === 0 ? 1 : pick([1, 2, 2, 3]),
        JOIN_TIME: jt, PAY_TIME: i === 0 ? jt : new Date(jt.getTime() + 180000),
        CREATE_TIME: jt
      });
    }
  });

  // ---------- 写入 ----------
  const TABS = [
    ['coupon_template', templates], ['coupon_user', couponUsers],
    ['points_record', points], ['recharge_record', recharges],
    ['marketing_campaign', campaigns], ['marketing_message', messages],
    ['full_reduction_rule', fullReductions], ['discount_rule', discountRules],
    ['campaign_usage_record', usages], ['flash_sale', flashSales],
    ['buy_get_free', buyGets], ['new_customer_discount', newCust],
    ['group_buy_campaign', groups], ['group_buy_participation', participations]
  ];
  const esc = v => {
    if (v === null || v === undefined) return 'NULL';
    if (v instanceof Date) return "'" + dt(v) + "'";
    if (typeof v === 'boolean') return v ? '1' : '0';
    if (typeof v === 'number') return String(v);
    return "'" + String(v).replace(/\\/g, '\\\\').replace(/'/g, "''") + "'";
  };
  const parts = ['SET NAMES utf8mb4;'];
  for (const [t, rows] of TABS) {
    const cols = (await q(`SHOW COLUMNS FROM \`${t}\``)).map(r => r.Field);
    await c.query(`DELETE FROM \`${t}\``);
    const sql = `INSERT INTO \`${t}\` (${cols.map(x => '`' + x + '`').join(',')}) VALUES ${rows.map(r => '(' + cols.map(x => esc(r[x])).join(',') + ')').join(',\n')};`;
    await c.query(sql);
    parts.push(`-- ${t}: ${rows.length}`); parts.push(`DELETE FROM \`${t}\`;`); parts.push(sql);
    console.log(`  [OK] ${t.padEnd(26)} ${rows.length} 行`);
  }
  await c.end();
  fs.writeFileSync(OUT, parts.join('\n'), 'utf8');
  console.log(`\n[SQL 落盘] ${OUT}`);
})().catch(e => { console.error('FAIL:', e.message); process.exit(1); });
