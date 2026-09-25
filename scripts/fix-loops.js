// fix-loops.js — 补齐 4 处业务闭环缺口（支付/配送单据、口味标签、角色权限）
// 目标库：仅本地 localhost:3306/reggie
const mysql = require('mysql2/promise');

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie' });
  const qi = async (s, v) => (await c.query(s, v))[0];
  const q = async s => (await c.query(s));
  // 取单值：驱动返回的列键不稳定（可能为 '0'），故遍历取值并转 number
  const num = async s => {
    const res = await c.query(s);
    const rows = Array.isArray(res) ? res[0] : res;
    const row = rows[0];
    const v = Object.values(row)[0];
    return +v;
  };

  // ---------- 1. 已完成订单补支付单与配送单 ----------
  // payment_order.channel: 1微信 2支付宝 3现金 4银行卡
  // delivery_order.status: 1待接单 2配送中 3已送达
  const missPay = (await c.query("SELECT o.id oid, o.number ordNo, o.status ost, o.pay_method pm, o.amount amt, o.dining_type dt, o.order_time ot FROM orders o WHERE o.is_deleted=0 AND o.status>=2 AND NOT EXISTS (SELECT 1 FROM payment_order po WHERE po.order_id=o.id)"))[0];
  const missDlv = (await c.query("SELECT o.id oid, o.amount amt, o.order_time ot FROM orders o WHERE o.is_deleted=0 AND o.dining_type=1 AND NOT EXISTS (SELECT 1 FROM delivery_order dd WHERE dd.order_id=o.id)"))[0];

  let maxPo = +(await num('SELECT COALESCE(MAX(id),0) mx FROM payment_order'));
  let poN = 0;
  for (const o of missPay) {
    maxPo++;
        const channel = String(o.pm || 1);
    // 微信支付单号 WECH + 年月日时分秒 + 序号；支付宝 ALIPAY + 同格式
    const ts = new Date(Date.parse(o.ot) + 120000);
    const stamp = ts.toISOString().slice(2, 10).replace(/-/g, '') + ts.toISOString().slice(11, 19).replace(/:/g, '');
    const tradeNo = (channel === '2' ? 'ALI' : 'WECH') + stamp + String(maxPo).padStart(4, '0');
    const paid = new Date(Date.parse(o.ot) + 150000);
    await qi(`INSERT INTO payment_order
      (id, order_id, tenant_id, trade_no, channel_trade_no, channel, amount, status,
       paid_time, notify_time, created_time, update_time, is_deleted, version, create_user, update_user)
      VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),0,0,0,0)`, [
      maxPo, o.oid, 1, tradeNo, 'PT' + stamp + maxPo, channel, o.amt, '1', paid, paid
    ]);
    poN++;
  }
  console.log('payment_order 补 ' + poN + ' 张（覆盖已完成但未落支付单的订单）');

  let maxDo = await num('SELECT COALESCE(MAX(id),0) mx FROM delivery_order');
  let doN = 0;
  for (const o of missDlv) {
    maxDo++;
    // 已完成订单(dining_type=1)：外卖配送已完成 → status=3 已送达
    const dt = new Date(Date.parse(o.ot) + 60000);
    const dstr = dt.toISOString().slice(2, 10).replace(/-/g, '') + dt.toISOString().slice(11, 19).replace(/:/g, '');
    const [det] = await c.query('SELECT name, number FROM order_detail WHERE order_id=? AND is_deleted=0', [o.oid]);
    const summary = det.map(x => x.name + '×' + x.number).join('、');
    const [usr] = await c.query('SELECT user_name, phone, address FROM orders WHERE id=?', [o.oid]);
    await qi(`INSERT INTO delivery_order
      (id, tenant_id, platform_order_id, platform, order_id, dish_summary, amount,
       user_name, phone, address, status, order_time, created_time, update_time,
       created_user, update_user, is_deleted, version)
      VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),0,0,0,0)`, [
      maxDo, 1, 'DLY' + dstr + maxDo, '1', o.oid, summary, o.amt,
      usr[0] ? usr[0].user_name : null, usr[0] ? usr[0].phone : null,
      usr[0] ? usr[0].address : null, '3', dt
    ]);
    doN++;
  }
  console.log('delivery_order 补 ' + doN + ' 张（覆盖外卖已完成但无配送单的订单）');

  // ---------- 2. 缺口味的菜品补口味标签 ----------
  // 口味标签按菜名关键词映射；汤/凉菜/主食类也给出真实选项
  const flavorBy = (name) => {
    if (/汤|羹|豆腐汤|蛋汤|肉饼汤|骨汤/.test(name)) return [['份量', ['小份', '大份']], ['咸淡', ['正常', '少盐', '免盐']]];
    if (/凉|拌|拍|沙拉/.test(name)) return [['份量', ['小份', '大份']], ['辣度', ['不辣', '微辣', '中辣']], ['酸甜', ['正常', '多酸', '多甜']]];
    if (/饼|饭|面|粥|饺|馄饨|包子|馒头/.test(name)) return [['份量', ['小份', '大份']], ['辣度', ['不辣', '微辣']]];
    if (/肉|排骨|鸡|鸭|鱼|虾|牛|猪|羊|翅|腿|肘|蹄|腰|肝|肚|肠/.test(name)) return [['辣度', ['不辣', '微辣', '中辣', '重辣']], ['咸淡', ['正常', '少盐', '免盐']], ['份量', ['小份', '大份']]];
    if (/蛋|豆腐|豆花|腐竹/.test(name)) return [['咸淡', ['正常', '少盐', '免盐']], ['份量', ['小份', '大份']]];
    return [['辣度', ['不辣', '微辣', '中辣']], ['咸淡', ['正常', '少盐']], ['份量', ['小份', '大份']]];
  };

  const missF = (await c.query('SELECT d.id oid, d.name nm FROM dish d WHERE d.is_deleted=0 AND NOT EXISTS (SELECT 1 FROM dish_flavor f WHERE f.dish_id=d.id AND f.is_deleted=0)'))[0];
  let maxF = await num('SELECT COALESCE(MAX(id),0) mx FROM dish_flavor');
  let fN = 0;
  for (const d of missF) {
    for (const [nm, vals] of flavorBy(d.nm)) {
      maxF++;
      await qi(`INSERT INTO dish_flavor (id, dish_id, name, value, tenant_id, create_time, update_time, create_user, update_user, is_deleted)
        VALUES (?,?,?,?,1,NOW(),NOW(),1,1,0)`, [maxF, d.oid, nm, JSON.stringify(vals)]);
      fN++;
    }
  }
  console.log('dish_flavor 补 ' + fN + ' 条（覆盖 ' + missF.length + ' 道缺口味菜品）');

  // ---------- 3. 角色权限补齐（role 1 超管全量；其余按岗位合理授权） ----------
  const menuIds = (await c.query('SELECT id FROM menu WHERE is_deleted=0 ORDER BY id'))[0].map(r => r.id);
  const grant = {
    1: menuIds.slice(),                                          // 超级管理员：全部
    2: menuIds.slice(),                                          // 店长：全部
    3: [1, 2, 3, 5, 7, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 22, 23, 27, 28, 29, 32, 33, 34],  // 副店长
    4: [1, 3, 5, 9, 10, 11, 12, 13, 14, 18, 19, 22, 23, 32, 33, 34, 35, 36],                 // 大堂经理
    5: [1, 2, 3, 4, 5, 7, 10, 13, 14, 18, 19, 20, 21, 22, 23, 27, 28, 29, 30, 31, 35, 36],   // 收银员
    6: [1, 2, 6, 10, 13, 14, 15, 16, 24, 25, 26, 35, 36],                                  // 厨师长
    7: [1, 2, 10, 13, 14, 15, 16, 35, 36],                                                  // 厨师
    8: [1, 3, 10, 18, 19, 35, 36],                                                           // 服务员
    9: [1, 6, 10, 24, 25, 26, 35, 36],                                                      // 采购员
    10: [1, 6, 10, 24, 25, 26, 35, 36]                                                       // 仓库管理员
  };
  const [clr] = await c.query('DELETE FROM role_permission');
  let maxRp = await num('SELECT COALESCE(MAX(id),0) mx FROM role_permission');
  let rpN = 0;
  for (const [roleId, pids] of Object.entries(grant)) {
    for (const pid of pids) {
      if (menuIds.indexOf(pid) < 0) continue;
      maxRp++;
      await qi('INSERT INTO role_permission (id, role_id, permission_id, create_time) VALUES (?,?,?,NOW())', [maxRp, roleId, pid]);
      rpN++;
    }
  }
  console.log('role_permission 重建 ' + rpN + ' 条（清除旧 ' + clr.affectedRows + ' 条，10 角色 × 36 菜单按岗位授权）');

  // ---------- 4. 复核 ----------
  const chk = [
    ['订单-支付', 'SELECT COUNT(*) cnt FROM (SELECT id FROM orders WHERE is_deleted=0 AND status>=2) o WHERE NOT EXISTS (SELECT 1 FROM payment_order p WHERE p.order_id=o.id)'],
    ['订单-配送', 'SELECT COUNT(*) cnt FROM (SELECT id FROM orders WHERE is_deleted=0 AND dining_type=1) o WHERE NOT EXISTS (SELECT 1 FROM delivery_order d WHERE d.order_id=o.id)'],
    ['菜品-口味', 'SELECT COUNT(*) cnt FROM (SELECT id FROM dish WHERE is_deleted=0) d WHERE NOT EXISTS (SELECT 1 FROM dish_flavor f WHERE f.dish_id=d.id AND f.is_deleted=0)'],
    ['菜单-角色', 'SELECT COUNT(*) cnt FROM (SELECT id FROM menu WHERE is_deleted=0) m WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.permission_id=m.id)']
  ];
  console.log('\n===== 闭环复核 =====');
  for (const [label, sql] of chk) {
    const n = await num(sql);
    console.log('  ' + (n === 0 ? '[OK]  ' : '[FAIL] ') + label + ' 缺失 ' + n);
  }
  await c.end();
})();
