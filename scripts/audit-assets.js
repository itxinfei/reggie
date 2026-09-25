// audit-assets.js — 图片资源最终对账（DB 引用 vs 磁盘）
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

const UP = path.normalize('D:/MyCode/reggie/uploads');
const FWD = s => path.normalize(s).split(path.sep).join('/');

function walk(d) {
  let o = [];
  if (!fs.existsSync(d)) return o;
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) o = o.concat(walk(p));
    else if (/\.(jpg|jpeg|png|webp|gif|svg)$/i.test(e.name)) o.push(p);
  }
  return o;
}

(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie' });

  const diskRel = new Set(walk(UP).map(f => FWD(f).substring(FWD(UP).length + 1)));
  const byBase = {};
  for (const r of diskRel) {
    const b = r.split('/').pop().toLowerCase();
    (byBase[b] = byBase[b] || []).push(r);
  }

  const pairs = [
    ['dish', 'image'], ['setmeal', 'image'], ['order_detail', 'image'], ['shopping_cart', 'image'],
    ['dish_evaluation', 'images'], ['cs_message', 'image_url'], ['group_buy_campaign', 'IMAGE'],
    ['employee', 'avatar'], ['user', 'avatar'], ['rider', 'avatar'],
    ['store', 'logo'], ['tenant', 'logo'], ['tenant', 'license_image'], ['supplier', 'LICENSE_IMAGES']
  ];

  const refs = [];
  let blank = 0, errT = [];
  for (const [t, col] of pairs) {
    let r;
    try { r = (await c.query('SELECT ' + col + ' v FROM ' + t))[0]; }
    catch (e) { errT.push(t + '.' + col); continue; }
    for (const x of r) {
      const v = x.v;
      if (v == null || String(v).trim() === '') { blank++; continue; }
      String(v).trim().split(',').map(z => z.trim()).filter(Boolean).forEach(p => refs.push(FWD(p)));
    }
  }
  const uniq = [...new Set(refs)];
  const pref = {};
  for (const u of uniq) { const p = u.split('/')[0]; pref[p] = (pref[p] || 0) + 1; }

  const miss = uniq.filter(u => !diskRel.has(u) && !(byBase[u.split('/').pop().toLowerCase()] || []).length);
  const baseMatch = uniq.filter(u => !diskRel.has(u) && (byBase[u.split('/').pop().toLowerCase()] || []).length);

  const unused = [...diskRel].filter(d => !uniq.includes(d));

  console.log('===== 磁盘 =====');
  console.log('  图片总数: ' + diskRel.size);
  const dirs = {};
  for (const f of diskRel) { const s = f.split('/').slice(0, 2).join('/'); dirs[s] = (dirs[s] || 0) + 1; }
  let bytes = 0;
  for (const f of walk(UP)) bytes += fs.statSync(f).size;
  for (const k of Object.keys(dirs).sort()) console.log('    ' + k.padEnd(24) + dirs[k] + ' 张');
  console.log('    总大小 ' + (bytes / 1048576).toFixed(1) + ' MB');

  console.log('\n===== DB 引用 =====');
  console.log('  单元格有值: ' + refs.length + '  空白/NULL: ' + blank);
  console.log('  去重路径: ' + uniq.length);
  console.log('  前缀分布: ' + JSON.stringify(pref));
  console.log('  路径不存在但文件名可在磁盘找到: ' + baseMatch.length);
  baseMatch.slice(0, 20).forEach(b => console.log('    ' + b));

  console.log('\n===== 真缺失（需下载或建图） =====');
  console.log('  数量: ' + miss.length);
  miss.slice(0, 40).forEach(m => console.log('    ' + m));

  console.log('\n===== 磁盘未被任何引用 =====');
  console.log('  数量: ' + unused.length);
  unused.slice(0, 40).forEach(u => console.log('    ' + u));

  if (errT.length) console.log('\n  跳过列: ' + errT.join(', '));
  await c.end();
})();
