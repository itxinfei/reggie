// domain-g-finance.js — G域(收银结算/成本利润/考勤排班/催单/对账/提现)
// 用法: node scripts/domain-g-finance.js
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const OUT = path.join(__dirname, '..', 'src/main/resources/db/seed/domain-g-finance.sql');

let _s = 67890;
const rnd = () => { _s = (_s * 1103515245 + 12345) & 0x7fffffff; return _s / 0x7fffffff; };
const ri = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = a => a[Math.floor(rnd() * a.length)];
const rf = (a, b, d = 2) => +((a + rnd() * (b - a)).toFixed(d));
const pad2 = n => String(n).padStart(2, '0');
const dt = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());
const d0 = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
const dstr = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie', charset: 'utf8mb4' });
  const q = async s => { const [r] = await c.query(s); return r; };
  const TID = 1;
  const NOW = new Date(2026, 8, 25, 18, 0, 0);
  const ago = (d, h) => { const x = new Date(NOW); x.setDate(x.getDate() - d); x.setHours(h || ri(9, 22), ri(0, 59), 0, 0); return x; };
  const later = (d, h) => { const x = new Date(NOW); x.setDate(x.getDate() + d); x.setHours(h || 12, 0, 0, 0); return x; };

  const orders = (await q('SELECT id,order_time,amount,user_id,pay_method,number AS ord_no,dining_type FROM orders WHERE is_deleted=0 ORDER BY id')).map(r => ({ id: +r.id, t: r.order_time, amt: +r.amount, uid: +r.user_id, pay: +r.pay_method, no: r.ord_no, type: r.dining_type }));
  const employees = (await q('SELECT id,name,position FROM employee WHERE status=1 ORDER BY id')).map(r => ({ id: +r.id, name: r.name, pos: r.position }));
  const users = (await q('SELECT id,name FROM user ORDER BY id')).map(r => ({ id: +r.id, name: r.name }));
  const suppliers = (await q('SELECT id,name FROM supplier WHERE is_deleted=0 ORDER BY id')).map(r => ({ id: +r.id, name: r.name }));
  console.log(`引用: order=${orders.length} employee=${employees.length} user=${users.length} supplier=${suppliers.length}`);

  // ============ 1. reconciliation_statement 15 行 ============
  const reconcileStmts = [];
  for (let i = 0; i < 15; i++) {
    const d = ago(ri(0, 25), 10);
    const total = ri(8000, 22000);
    const refund = ri(50, 300);
    const fee = Math.round(total * 0.02);
    const net = total - refund - fee;
    const platformAmt = net - ri(-10, 10);
    const diff = total - platformAmt;
    reconcileStmts.push({
      id: i + 1, statement_no: 'RS2026' + (100000 + i),
      statement_date: dstr(d),
      platform: pick(['微信支付', '支付宝', 'POS刷卡', '现金']),
      system_amount: total, platform_amount: platformAmt,
      difference_amount: diff, order_count: ri(80, 200),
      refund_amount: refund, refund_count: ri(3, 8),
      fee_amount: fee, net_amount: net,
      status: diff === 0 ? 2 : 1,
      reconcile_time: new Date(d.getTime() + 3600000),
      reconcile_user_id: 1, reconcile_user_name: '王建国',
      remark: diff === 0 ? '对账一致' : '差异需人工核对',
      tenant_id: TID, create_time: d, update_time: NOW
    });
  }

  // ============ 2. profit_analysis 20 行 ============
  const profitAnalysis = [];
  for (let i = 0; i < 20; i++) {
    const d = ago(ri(0, 25), 10);
    const rev = ri(8000, 22000);
    const food = Math.round(rev * rf(0.35, 0.45));
    const labor = Math.round(rev * rf(0.15, 0.20));
    const other = Math.round(rev * rf(0.08, 0.15));
    const total = food + labor + other;
    const gross = rev - total;
    const opex = Math.round(rev * rf(0.03, 0.08));
    const net = gross - opex;
    const oc = ri(100, 220);
    profitAnalysis.push({
      id: i + 1, analysis_date: dstr(d),
      total_revenue: rev, food_cost: food, labor_cost: labor, other_cost: other,
      total_cost: total, gross_profit: gross,
      gross_profit_rate: +(gross / rev * 100).toFixed(2),
      operating_expense: opex, net_profit: net,
      net_profit_rate: +(net / rev * 100).toFixed(2),
      order_count: oc, customer_count: ri(60, 150),
      average_order_value: +(rev / oc).toFixed(2),
      tenant_id: TID, create_time: d, update_time: NOW
    });
  }

  // ============ 3. cost_record 30 行 ============
  const costRecords = [];
  // cost_type: 1食材 2人工 3房租 4水电 5设备 6营销 7损耗 8杂项
  const costTypes = [
    [1, '食材采购', 3], [2, '员工薪资', 2], [3, '门店租金', 1],
    [4, '水电气', 1], [5, '设备维修', 1], [6, '推广活动', 2],
    [7, '食材损耗', 1], [8, '日常杂项', 2]
  ];
  for (let i = 0; i < 30; i++) {
    const ct = costTypes[i % costTypes.length];
    const d = ago(ri(0, 30), ri(9, 22));
    costRecords.push({
      id: i + 1, cost_type: ct[0],
      ref_id: ct[2] === 2 ? ri(1, 5) : null,
      ref_name: ct[1] + '-' + pick(['月付', '周付', '临采', '采购']),
      amount: ri(50, 5000),
      cost_date: d,
      remark: pick(['月度支出', '日常采购', '计划内', '临时增加', null]),
      tenant_id: TID, create_time: d, create_user: 1, version: 1
    });
  }

  // ============ 4. labor_cost 15 行 ============
  const laborCost = [];
  for (let i = 0; i < 15; i++) {
    const e = pick(employees);
    const salary = ri(3000, 15000);
    const si = Math.round(salary * 0.18);
    const hf = Math.round(salary * 0.05);
    const ob = ri(0, 1000);
    laborCost.push({
      id: i + 1, employee_id: e.id, employee_name: e.name,
      salary, social_insurance: si, housing_fund: hf,
      other_benefits: ob,
      total_cost: salary + si + hf + ob,
      cost_month: '2026-08-15',
      remark: null, tenant_id: TID,
      create_time: ago(ri(0, 20), ri(9, 22)), update_time: NOW,
      create_user: 1, update_user: 1
    });
  }

  // ============ 5. other_cost 15 行 ============
  const otherCost = [];
  const ocNames = ['房租', '水电费', '燃气费', '物业费', '网络费', '垃圾清运', '设备维护', '广告宣传', '办公用品', '保险费', '税务', '培训费', '保洁费', '维修费', '其他杂项'];
  for (let i = 0; i < 15; i++) {
    const d = ago(ri(0, 30), ri(9, 22));
    otherCost.push({
      id: i + 1, name: ocNames[i],
      cost_type: pick([3, 4, 6, 8]),
      amount: ri(500, 20000),
      cost_date: dstr(d),
      remark: pick(['月度支出', '季度支出', null]),
      tenant_id: TID, create_time: d, update_time: NOW,
      create_user: 1, update_user: 1
    });
  }

  // ============ 6. attendance 60 行 ============
  const attendance = [];
  // status: 1正常 2迟到 3早退 4缺勤 5请假 6加班
  const attTypes = [1, 2, 3, 4, 5, 6];
  for (let i = 0; i < 60; i++) {
    const e = pick(employees);
    const d = ago(ri(0, 20), 9);
    const st = pick(attTypes);
    const cin = new Date(d.getTime() + ri(-30, 30) * 60000);
    const cout = new Date(d.getTime() + 8 * 3600000 + ri(-30, 60) * 60000);
    const hours = Math.max(0, Math.round((cout - cin) / 3600000 * 10) / 10);
    attendance.push({
      id: i + 1, employee_id: e.id, employee_name: e.name,
      date: dstr(d),
      check_in_time: st === 4 ? null : dt(cin),
      check_out_time: st === 4 ? null : dt(cout),
      status: st, work_hours: hours,
      remark: pick([null, '正常上班', '请假半天', '加班处理', '迟到', '事假', '病假']),
      tenant_id: TID, create_time: d, update_time: NOW
    });
  }

  // ============ 7. work_schedule 40 行 ============
  const workSchedule = [];
  // shift: 1早班 2中班 3晚班 4全日 5全天
  const shifts = [
    [1, '早班', '09:00', '14:00'], [2, '中班', '11:00', '16:00'],
    [3, '晚班', '14:00', '22:00'], [4, '全日', '09:00', '18:00'],
    [5, '全天', '10:00', '22:00']
  ];
  for (let i = 0; i < 40; i++) {
    const e = pick(employees);
    const d = later(ri(0, 7), 9);
    const sh = shifts[i % shifts.length];
    workSchedule.push({
      id: i + 1, employee_id: e.id, employee_name: e.name,
      schedule_date: dstr(d),
      shift: sh[0], shift_start: sh[2], shift_end: sh[3],
      work_date_str: dstr(d),
      remark: pick([null, '正常排班', '调班', '顶班', '兼职']),
      tenant_id: TID, create_time: ago(ri(0, 10), ri(9, 22)), update_time: NOW
    });
  }

  // ============ 8. urgency_record 20 行 ============
  const urgencyRecords = [];
  for (let i = 0; i < 20; i++) {
    const o = pick(orders);
    urgencyRecords.push({
      id: i + 1, order_id: o.id,
      member_id: o.uid, order_no: o.no,
      times: ri(1, 5),
      status: pick([1, 2, 2, 3]),
      tenant_id: TID, create_time: o.t, update_time: NOW
    });
  }

  // ============ 9. withdrawal_application 20 行 ============
  const withdrawApps = [];
  for (let i = 0; i < 20; i++) {
    const e = pick(employees);
    const amt = ri(1000, 15000);
    const st = pick([1, 1, 2, 2, 3, 3, 4]);
    const t = ago(ri(0, 25), ri(9, 22));
    withdrawApps.push({
      id: i + 1, application_no: 'WA2026' + (100000 + i),
      applicant_id: e.id, applicant_name: e.name,
      amount: amt, withdraw_method: pick([1, 2, 3]),   // 1银行卡 2支付宝 3微信
      receive_account: '62' + ri(1000000000000, 9999999999999),
      receive_name: e.name,
      status: st,
      reviewer_id: st >= 2 ? 1 : null,
      reviewer_name: st >= 2 ? '王建国' : null,
      review_time: st >= 2 ? new Date(t.getTime() + 3600000) : null,
      review_remark: st === 4 ? '金额需核实' : null,
      payment_time: st === 3 ? new Date(t.getTime() + 7200000) : null,
      payment_no: st === 3 ? 'PAY' + (2026090000 + i) : null,
      remark: pick([null, '工资提现', '报销款', '预支', '奖金']),
      tenant_id: TID, create_time: t, update_time: NOW
    });
  }

  // ============ 10. withdrawal_request 20 行 ============
  const withdrawReqs = [];
  const banks = ['中国工商银行', '中国建设银行', '中国农业银行', '招商银行', '交通银行'];
  for (let i = 0; i < 20; i++) {
    const u = pick(users);
    const st = pick([1, 1, 2, 2, 3]);
    const t = ago(ri(0, 25), ri(9, 22));
    withdrawReqs.push({
      ID: i + 1, TENANT_ID: TID, USER_ID: u.id,
      AMOUNT: ri(100, 5000),
      BANK_NAME: pick(banks),
      ACCOUNT_NAME: u.name,
      ACCOUNT_NUMBER: '62' + ri(1000000000000, 9999999999999),
      STATUS: st,
      REJECT_REASON: st === 3 ? '账户信息不完整' : null,
      CREATE_TIME: t,
      APPROVE_TIME: st >= 2 ? new Date(t.getTime() + 3600000) : null,
      APPROVE_USER_ID: st >= 2 ? 1 : null,
      IS_DELETED: 0
    });
  }

  // ============ 11. withdrawal_record 15 行 ============
  const withdrawRecords = [];
  const completedApps = withdrawApps.filter(a => a.status === 3);
  for (let i = 0; i < 15; i++) {
    const app = completedApps[i % completedApps.length] || withdrawApps[i];
    const amt = app.amount;
    const fee = Math.max(0, +(amt * 0.005).toFixed(2));
    const actual = +(amt - fee).toFixed(2);
    withdrawRecords.push({
      ID: i + 1, TENANT_ID: TID, WITHDRAWAL_ID: app.id,
      ACTUAL_AMOUNT: actual, FEE: fee,
      TRANSFER_TIME: new Date(app.payment_time.getTime() + 60000),
      BANK_TRACE_NO: 'BN' + (2026090000000 + i * 17),
      CREATE_TIME: app.payment_time
    });
  }

  // ================= 写入 =================
  const TABS = [
    ['reconciliation_statement', reconcileStmts],
    ['profit_analysis', profitAnalysis],
    ['cost_record', costRecords],
    ['labor_cost', laborCost],
    ['other_cost', otherCost],
    ['attendance', attendance],
    ['work_schedule', workSchedule],
    ['urgency_record', urgencyRecords],
    ['withdrawal_application', withdrawApps],
    ['withdrawal_request', withdrawReqs],
    ['withdrawal_record', withdrawRecords]
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
