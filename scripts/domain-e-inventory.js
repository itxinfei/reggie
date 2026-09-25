// domain-e-inventory.js — E域(原料/供应商/采购/盘点/成本)真实数据生成器
// 用法: node scripts/domain-e-inventory.js
// 大写列名表：material_category supplier material purchase_order(_detail) stock_check(_detail) stock_record price_history supplier_settlement
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const OUT = path.join(__dirname, '..', 'src/main/resources/db/seed/domain-e-inventory.sql');

let _s = 31337;
const rnd = () => { _s = (_s * 1103515245 + 12345) & 0x7fffffff; return _s / 0x7fffffff; };
const ri = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = a => a[Math.floor(rnd() * a.length)];
const rf = (a, b, d = 2) => +((a + rnd() * (b - a)).toFixed(d));
const pad2 = n => String(n).padStart(2, '0');
const dt = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());
const d0 = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie', charset: 'utf8mb4' });
  const q = async s => { const [r] = await c.query(s); return r; };
  const TID = 1;

  const NOW = new Date(2026, 8, 25, 18, 0, 0);
  const base = new Date(2026, 7, 20, 9, 0, 0);
  const span = NOW - base;
  const dayTime = (daysAgo, h, m) => { const d = new Date(NOW); d.setDate(d.getDate() - daysAgo); d.setHours(h, m || ri(0, 59), 0, 0); return d; };

  // ---------- 原料分类（10）----------
  const cats = ['蔬菜类', '肉禽蛋类', '水产类', '米面粮油', '调味干货', '豆制品', '饮品冲调', '包材耗材', '冻品半成品', '果品类'];
  const categories = cats.map((n, i) => ({
    ID: i + 1, TENANT_ID: TID, NAME: n, SORT: i + 1, CREATED_TIME: base, UPDATE_TIME: base,
    CREATE_USER: 1, UPDATE_USER: 1, IS_DELETED: 0
  }));

  // ---------- 供应商（15）----------
  const supNames = ['京华食材配送中心', '永辉生鲜', '鑫隆肉业', '海之鲜水产', '金龙鱼粮油', '味事达调味', '老北京豆制品', '农夫山泉', '绿萌食品', '冷链速配', '丰乐农场', '康美食品', '优品包材', '顺发冻品', '华鲜果蔬'];
  const sups = supNames.map((n, i) => ({
    ID: i + 1, TENANT_ID: TID, NAME: n,
    CONTACT: pick(['张伟', '李强', '王磊', '赵刚', '刘洋', '陈涛', '杨帆', '黄海', '周军', '吴斌', '郑勇', '冯亮', '蒋健', '沈国', '朱军']),
    PHONE: '138' + String(10000000 + i * 137).slice(0, 8),
    ADDRESS: '北京市' + pick(['朝阳区酒仙桥', '丰台区南四环', '海淀区西三旗', '通州区马驹桥', '大兴区黄村']) + '物流园' + ri(1, 20) + '号',
    LICENSE_IMAGES: null, STATUS: 1,
    CREATED_TIME: base, UPDATE_TIME: base, CREATE_USER: 1, UPDATE_USER: 1, IS_DELETED: 0
  }));

  // ---------- 原料（38）----------
  const M = [
    ['西红柿', 'kg', 5.8], ['土豆', 'kg', 3.2], ['青椒', 'kg', 4.5], ['白菜', 'kg', 2.4], ['黄瓜', 'kg', 4.2], ['茄子', 'kg', 3.8], ['豆角', 'kg', 6.5], ['西兰花', 'kg', 5.6], ['香菇', 'kg', 18], ['豆腐', 'kg', 4.8],
    ['猪五花肉', 'kg', 22], ['猪里脊', 'kg', 26], ['牛肉', 'kg', 38], ['排骨', 'kg', 32], ['鸡翅', 'kg', 24], ['鸡胸肉', 'kg', 16], ['鸡蛋', '个', 0.8], ['鸭腿', 'kg', 20],
    ['鲈鱼', '条', 18], ['草鱼', 'kg', 12], ['虾仁', 'kg', 58], ['豆腐皮', 'kg', 8],
    ['大米', 'kg', 6.5], ['面粉', 'kg', 5.2], ['食用油', 'L', 12], ['生抽', 'L', 10], ['老抽', 'L', 11], ['食盐', 'kg', 3], ['白糖', 'kg', 6], ['味精', 'kg', 12], ['花椒', 'kg', 45], ['干辣椒', 'kg', 30], ['料酒', 'L', 8], ['淀粉', 'kg', 8], ['葱姜', 'kg', 5],
    ['酸梅汤料', 'kg', 25], ['珍珠粉圆', 'kg', 12],
    ['餐盒', '个', 1.2], ['餐巾纸', '包', 2], ['吸管', '包', 3], ['打包袋', '个', 0.5],
    ['手抓饼胚', 'kg', 6], ['速冻牛肉丸', 'kg', 16]
  ];
  const CAT_OF = name => {
    if (/猪肉|排骨|鸡翅|里脊|牛肉|鸭腿|鸡胸/.test(name)) return 2;
    if (/鱼|虾/.test(name)) return 3;
    if (/米|面|油|抽|盐|糖|味精|花椒|辣椒|料酒|淀粉|葱姜/.test(name)) return 5;
    if (/豆腐|豆/.test(name)) return 6;
    if (/酸梅|珍珠/.test(name)) return 7;
    if (/餐盒|餐巾|吸管|打包/.test(name)) return 8;
    if (/速冻|丸/.test(name)) return 9;
    if (/饼|粉/.test(name)) return 4;
    return 1;
  };
  const materials = M.map((m, i) => {
    const stock = ri(5, 200);
    const min = ri(5, 30);
    return {
      ID: i + 1, TENANT_ID: TID, CATEGORY_ID: CAT_OF(m[0]), NAME: m[0], UNIT: m[1],
      STOCK_QTY: stock, MIN_STOCK: min, UNIT_PRICE: m[2],
      SUPPLIER_ID: ri(1, 15), BARCODE: '69' + String(10000000000 + i * 397).slice(0, 11),
      STATUS: 1, CREATED_TIME: base, UPDATE_TIME: NOW, CREATE_USER: 1, UPDATE_USER: 1, IS_DELETED: 0
    };
  });

  // ---------- 采购单（25）+ 明细（~100）----------
  const purchases = [];
  const details = [];
  for (let i = 0; i < 25; i++) {
    const poId = i + 1;
    const sup = pick(sups);
    const dt0 = dayTime(ri(0, 35), ri(8, 18));
    const n = ri(3, 6);
    const items = [...materials].sort(() => rnd() - 0.5).slice(0, n);
    let total = 0;
    for (const m of items) {
      const qty = rf(2, 30, 1);
      const amt = +(qty * m.UNIT_PRICE).toFixed(2);
      total = +(total + amt).toFixed(2);
      details.push({
        ID: details.length + 1, TENANT_ID: TID, PURCHASE_ORDER_ID: poId, MATERIAL_ID: m.ID,
        QTY: qty, UNIT_PRICE: m.UNIT_PRICE, AMOUNT: amt, RECEIVED_QTY: qty, REMARK: null,
        CREATE_TIME: dt0, UPDATE_TIME: dt0, CREATE_USER: 1, UPDATE_USER: 1, IS_DELETED: 0
      });
    }
    const st = pick([1, 2, 2, 3, 3]);   // 1待入库 2部分 3已完成
    purchases.push({
      ID: poId, TENANT_ID: TID,
      ORDER_NO: 'PO2026' + pad2(dt0.getMonth() + 1) + pad2(dt0.getDate()) + String(100 + i),
      SUPPLIER_ID: sup.ID, TOTAL_AMOUNT: total, STATUS: st,
      OPERATOR: '采购员', REMARK: null, VOUCHER_IMAGES: null,
      CREATED_TIME: dt0, UPDATE_TIME: st === 3 ? new Date(dt0.getTime() + 3600000) : dt0,
      CREATE_USER: 1, UPDATE_USER: 1, IS_DELETED: 0, VERSION: st === 3 ? 2 : 1
    });
  }

  // ---------- 盘点（12）+ 明细 ----------
  const checks = [];
  const checkDetails = [];
  for (let i = 0; i < 12; i++) {
    const ckId = i + 1;
    const dt0 = dayTime(ri(0, 30), 10);
    const n = ri(6, 12);
    const items = [...materials].sort(() => rnd() - 0.5).slice(0, n);
    let diffTotal = 0;
    for (const m of items) {
      const book = ri(10, 150);
      const diff = ri(-3, 3);
      const actual = book + diff;
      diffTotal = +(diffTotal + diff * m.UNIT_PRICE).toFixed(2);
      checkDetails.push({
        ID: checkDetails.length + 1, TENANT_ID: TID, CHECK_ID: ckId, MATERIAL_ID: m.ID,
        BOOK_QTY: book, ACTUAL_QTY: actual, DIFF_QTY: diff, REMARK: diff < 0 ? '损耗' : (diff > 0 ? '盘盈' : null),
        CREATE_TIME: dt0, UPDATE_TIME: dt0, CREATE_USER: 1, UPDATE_USER: 1, IS_DELETED: 0
      });
    }
    checks.push({
      ID: ckId, TENANT_ID: TID, CHECK_NO: 'CK2026' + pad2(dt0.getMonth() + 1) + pad2(dt0.getDate()) + String(100 + i),
      STATUS: pick([1, 2, 2, 3]), TOTAL_DIFF_AMOUNT: diffTotal, OPERATOR: '仓库管理员',
      REMARK: null, VOUCHER_IMAGES: null, CREATED_TIME: dt0, UPDATE_TIME: dt0,
      CREATE_USER: 1, UPDATE_USER: 1, IS_DELETED: 0
    });
  }

  // ---------- 出入库流水（45）----------
  const stockRecords = [];
  for (let i = 0; i < 45; i++) {
    const m = pick(materials);
    const isOut = rnd() < 0.65;
    const qty = rf(1, 20, 1);
    const amount = +(qty * m.UNIT_PRICE).toFixed(2);
    stockRecords.push({
      ID: i + 1, TENANT_ID: TID, MATERIAL_ID: m.ID,
      TYPE: isOut ? 2 : 1,            // 1入库 2出库
      QTY: isOut ? -qty : qty, UNIT_PRICE: m.UNIT_PRICE, TOTAL_AMOUNT: amount,
      BIZ_ID: isOut ? null : purchases[ri(0, purchases.length - 1)].ID,
      REMARK: isOut ? pick(['销售出库', '生产领用', '报损']) : pick(['采购入库', '退货入库', '盘盈入库']),
      VOUCHER_IMAGES: null, OPERATOR: pick(['仓库管理员', '采购员']),
      CREATE_TIME: dayTime(ri(0, 30), ri(9, 20)), UPDATE_TIME: NOW,
      CREATE_USER: 1, UPDATE_USER: 1, IS_DELETED: 0
    });
  }

  // ---------- 价格历史（45）----------
  const priceHist = [];
  materials.forEach(m => {
    const baseP = m.UNIT_PRICE;
    const steps = ri(2, 3);
    for (let k = 0; k < steps; k++) {
      const old = baseP * (1 - (steps - k) * 0.04);
      priceHist.push({
        ID: priceHist.length + 1, TENANT_ID: TID, MATERIAL_ID: m.ID,
        OLD_PRICE: +old.toFixed(2), NEW_PRICE: baseP,
        CHANGE_REASON: pick(['市场波动', '供应商调价', '季节变动', '批量采购优惠']),
        OPERATOR_ID: 1, CREATE_TIME: dayTime(ri(1, 35), 14)
      });
    }
  });

  // ---------- 供应商结算（15）----------
  const settlements = sups.map((s, i) => {
    const poTotal = purchases.filter(p => p.SUPPLIER_ID === s.ID).reduce((t, p) => t + p.TOTAL_AMOUNT, 0);
    const total = +(poTotal + rf(300, 2500, 2)).toFixed(2);
    const paid = +(total * rf(0.4, 1.0)).toFixed(2);
    return {
      ID: i + 1, TENANT_ID: TID, SUPPLIER_ID: s.ID, PERIOD: '2026-' + pick(['08', '09']),
      TOTAL_AMOUNT: total, PAID_AMOUNT: paid, STATUS: paid >= total ? 2 : (paid > 0 ? 1 : 0),
      CREATE_TIME: dayTime(ri(0, 10), 10), UPDATE_TIME: NOW, CREATE_USER: 1, UPDATE_USER: 1, IS_DELETED: 0
    };
  });

  // ---------- 写入 ----------
  const TABS = [
    ['material_category', categories], ['supplier', sups], ['material', materials],
    ['purchase_order', purchases], ['purchase_order_detail', details],
    ['stock_check', checks], ['stock_check_detail', checkDetails],
    ['stock_record', stockRecords], ['price_history', priceHist],
    ['supplier_settlement', settlements]
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
    console.log(`  [OK] ${t.padEnd(22)} ${rows.length} 行`);
  }
  await c.end();
  fs.writeFileSync(OUT, parts.join('\n'), 'utf8');
  console.log(`\n[SQL 落盘] ${OUT}`);
})().catch(e => { console.error('FAIL:', e.message); process.exit(1); });
