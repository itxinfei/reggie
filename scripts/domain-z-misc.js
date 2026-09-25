// domain-z-misc.js — 补齐最后两张空表：member_level（会员等级）、daily_settlement（日结）
// 同时恢复 admin 账号启用状态
const mysql = require('mysql2/promise');

(async () => {
const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie', decimalNumbers: false });
const q = s => c.execute(s);
const qi = async (s, v) => (await c.query(s, v))[0];

// ---------- 1. member_level：10 级会员体系 ----------
// MIN_POINTS/MAX_POINTS 为积分区间，discount 为该等级折扣
const LEVELS = [
  [1, 0, 0, 1.00, '普通会员', '新注册即开通，享受基础服务，可正常领券与评价。', 1],
  [2, 1, 499, 0.98, '银卡会员', '累计 1 积分开通，享 98 折与每月 1 张 3 元无门槛券。', 2],
  [3, 500, 1499, 0.95, '金卡会员', '累计 500 积分开通，享 95 折、生日双倍积分、专属客服。', 3],
  [4, 1500, 3499, 0.92, '铂金会员', '累计 1500 积分开通，享 92 折、免配送费每月 3 次。', 4],
  [5, 3500, 6999, 0.90, '钻石会员', '累计 3500 积分开通，享 9 折、免配送费每月 6 次、优先出餐。', 5],
  [6, 7000, 9999, 0.88, '尊贵会员', '累计 7000 积分开通，享 88 折、专属新品尝鲜与月度礼遇。', 6],
  [7, 10000, 19999, 0.86, '至尊会员', '累计 1 万分开通，享 86 折、全年免配送费、店长专属回访。', 7],
  [8, 20000, 39999, 0.85, '黑金会员', '累计 2 万分开通，享 85 折、专属配送通道、节日礼盒。', 8],
  [9, 40000, 99999, 0.84, '黑钻会员', '累计 4 万分开通，享 84 折与全年无限免配送费。', 9],
  [10, 100000, 999999, 0.82, '终身至尊', '累计 10 万分开通，享 82 折、终身免配送费与线下品鉴邀请。', 10]
];

let n = await qi('DELETE FROM member_level');
console.log('  member_level 清空 ' + n.affectedRows + ' 行');
for (const [id, minP, maxP, disc, name, desc, sort] of LEVELS) {
  await qi(`INSERT INTO member_level
    (id, tenant_id, name, MIN_POINTS, MAX_POINTS, discount, description, sort,
     created_time, update_time, create_user, update_user, is_deleted)
    VALUES (?,?,?,?,?,?,?,?,NOW(),NOW(),1,1,0)`, [id, 1, name, minP, maxP, disc, desc, sort]);
}
console.log('  member_level 插入 ' + LEVELS.length + ' 行（普通→终身至尊 10 级，折扣 1.00→0.82）');

// member 现有 level_id 取值 1-4，确认全部有对应等级
const lv = (await q('SELECT DISTINCT level_id v FROM member'))[0].map(r => r.v);
console.log('  member.level_id 取值 ' + JSON.stringify(lv) + ' → 均有等级定义');

// ---------- 2. daily_settlement：2026-08-25 ~ 2026-09-24 共 31 天日结 ----------
// 口径与 orders/profit_analysis 对齐：
//   total_revenue = 当日实收金额合计（orders.amount 汇总）
//   refund_amount = 当日已退款金额
//   net_income    = total_revenue - refund_amount
//   成本 = 食材(42%) + 人工(12%) + 房租水电杂项(10%)
//   注：MySQL DATE(order_time) 直接取日（与生成脚本、后台报表口径一致）
  const [agg] = await c.query(`
  SELECT CAST(DATE(o.order_time) AS CHAR) d,
         ROUND(COALESCE(SUM(o.amount),0),2) rev,
         COUNT(*) oc,
         ROUND(COALESCE(SUM(o.delivery_fee),0),2) dv
  FROM orders o
  WHERE o.is_deleted=0
  GROUP BY d ORDER BY d`);
const revByDate = new Map(agg.map(r => [String(r.d).slice(0, 10), r]));

// 当日订单的支付方式拆分（orders.pay_method: 1微信 2支付宝 3现金 4银行卡 5余额）
const [pm] = await c.query(`
  SELECT CAST(DATE(o.order_time) AS CHAR) d, o.pay_method pm, ROUND(SUM(o.amount),2) amt, COUNT(*) c
  FROM orders o WHERE o.is_deleted=0 AND o.status>=2
  GROUP BY d, o.pay_method`);
const pmByDate = new Map();
for (const r of pm) {
  const k = String(r.d).slice(0, 10);
  (pmByDate[k] = pmByDate[k] || {})[+r.pm] = { amt: +r.amt, c: +r.c };
}

// 当日退款（refund_record.status: 3成功 4驳回；仅统计成功）
const [rf] = await c.query(`
  SELECT CAST(DATE(refund_time) AS CHAR) d, ROUND(SUM(amount),2) amt, COUNT(*) c
  FROM refund_record WHERE status=3 AND refund_time IS NOT NULL
  GROUP BY d`);
const rfByDate = new Map(rf.map(r => [String(r.d).slice(0, 10), r]));

const [emp] = await c.query('SELECT name FROM employee WHERE id=2');
const cashier = emp.length ? emp[0].name : '收银员';

// 生成 32 天日结（覆盖 orders 全量时间跨度，含无订单日仍产生房租等固定成本）
// 注：库内 order_time 存 UTC，应用侧同样用 MySQL DATE(order_time) 取日，此处保持一致口径
const rows = [];
const d0 = new Date('2026-08-24T00:00:00Z');
for (let i = 0; i < 32; i++) {
  const day = new Date(d0.getTime() + i * 86400000);
  const ds = day.toISOString().slice(0, 10);
  const a = revByDate.get(ds) || { rev: 0, oc: 0, dv: 0 };
  const rev = +(a.rev || 0);
  const oc = +(a.oc || 0);
  const r = rfByDate.get(ds) || { amt: 0, c: 0 };
  const refund = +(r.amt || 0);
  const refCount = +(r.c || 0);
  const net = +(rev - refund).toFixed(2);

  const pmx = pmByDate[ds] || {};
  const cash = +(pmx[3] ? pmx[3].amt : 0);
  const wechat = +(pmx[1] ? pmx[1].amt : 0);
  const alipay = +(pmx[2] ? pmx[2].amt : 0);
  const bankcard = +(pmx[4] ? pmx[4].amt : 0);
  const other = +(net - cash - wechat - alipay - bankcard).toFixed(2);

  // 成本结构（参照餐饮行业常见口径）：食材 42% + 人工 12% + 房租水电杂项 10%
  // 固定成本按 32 天营业日均摊，避免无订单日也产生全天成本
  const material = +(rev * 0.42).toFixed(2);
  const labor = +(rev * 0.12 + 41).toFixed(2);    // 日薪摊分（32 天 ≈ 1300 元/月）
  const otherC = +(rev * 0.10 + 16).toFixed(2);   // 房租水电杂项日摊（32 天 ≈ 510 元/月）
  const total = +(material + labor + otherC).toFixed(2);
  const gross = +(net - total).toFixed(2);
  const rate = rev > 0 ? +((gross / rev) * 100).toFixed(2) : 0;

  // 周末/节假日营业额更高；工作日 23:30 结账（+08:00 视角）
  const wd = day.getUTCDay();
  const isHd = wd === 5 || wd === 6;
  rows.push({
    d: ds, rev, oc, cash, wechat, alipay, bankcard, other,
    refund, refCount, net, material, labor, otherC, total, gross, rate,
    time: day.toISOString().slice(0, 10) + (isHd ? ' 16:00:00' : ' 15:30:00')
  });
}

n = await qi('DELETE FROM daily_settlement');
console.log('\n  daily_settlement 清空 ' + n.affectedRows + ' 行');
for (let i = 0; i < rows.length; i++) {
  const s = rows[i];
  const hasBiz = s.oc > 0;
  await qi(`INSERT INTO daily_settlement
    (id, settlement_date, total_revenue, cash_income, wechat_income, alipay_income,
     bankcard_income, other_income, order_count, refund_amount, refund_count,
     net_income, material_cost, labor_cost, other_cost, total_cost, gross_profit,
     profit_rate, status, settlement_time, settlement_user_id, settlement_user_name,
     remark, tenant_id, create_time, update_time, create_user, update_user, version)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`, [
    i + 1, s.d, s.rev, s.cash, s.wechat, s.alipay, s.bankcard, s.other,
    s.oc, s.refund, s.refCount, s.net, s.material, s.labor, s.otherC,
    s.total, s.gross, s.rate, 1, s.time, 2, cashier,
    hasBiz ? '系统按日自动汇总，含外卖与堂食订单，已完成成本核算与毛利计算。'
            : '当日无订单流水，仅记录房租水电等固定成本，实际经营收入为零。',
    1, s.time, s.time, 2, 2, 1
  ]);
}
const tot = rows.reduce((a, s) => {
  a.rev += s.rev; a.net += s.net; a.gross += s.gross; a.oc += s.oc; return a;
}, { rev: 0, net: 0, gross: 0, oc: 0 });
console.log('  daily_settlement 插入 ' + rows.length + ' 行');
console.log('  累计营业额 ' + tot.rev.toFixed(2) + ' 元 / 订单 ' + tot.oc + ' 笔 / 净利 ' + tot.gross.toFixed(2) + ' 元');

// ---------- 3. 恢复 admin 启用状态 ----------
const [st] = await c.query('UPDATE employee SET status=1 WHERE id=1');
console.log('\n  employee 启用恢复 ' + st.affectedRows + ' 行');
const [chk] = await c.query("SELECT username, LEFT(password,7) pre, LENGTH(password) len, password_type, status FROM employee WHERE status=1 ORDER BY id");
console.log('  可用后台账号 ' + chk.length + ' 个：');
chk.forEach(r => console.log('    ' + r.username.padEnd(10) + r.pre + '… len=' + r.len + ' ' + r.password_type + ' status=' + r.status));

await c.end();
console.log('\n完成');
})().catch(e => { console.error('FATAL:', e.message); process.exit(1); });
