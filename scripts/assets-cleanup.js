// assets-cleanup.js — 图片资源最终收口
// 1) 统一重命名磁盘文件（去掉 -1 / -1-1 后缀）
// 2) DB 图片路径统一为 images/dishes/<文件名>
// 3) 去重（字节完全相同的保留 1 份）
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const UP = path.join(ROOT, 'uploads');
const DI = path.join(UP, 'images', 'dishes');
const mkd = d => { if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true }); };
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

console.log('===== 1. 磁盘文件名统一 =====');
mkd(DI);
let renamed = 0;
for (const f of walk(DI)) {
  let base = path.basename(f);
  const norm = base.replace(/-1(-1)?\.(jpg|jpeg|png|webp|gif|svg)$/i, '.$2');
  if (norm !== base) {
    let dest = path.join(DI, norm);
    let k = 2;
    while (fs.existsSync(dest)) {
      const stem = path.basename(norm, path.extname(norm));
      dest = path.join(DI, stem + '-' + k + path.extname(norm));
      k++;
    }
    fs.renameSync(f, dest);
    renamed++;
  }
}
console.log('  重命名 ' + renamed + ' 个文件');

console.log('===== 2. 按字节去重 =====');
const files = walk(DI);
const bySize = {};
for (const f of files) (bySize[fs.statSync(f).size] = bySize[fs.statSync(f).size] || []).push(f);
let dup = 0;
const kept = new Set();
for (const s of Object.keys(bySize)) {
  const arr = bySize[s].sort();
  kept.add(arr[0]);
  for (let i = 1; i < arr.length; i++) { fs.unlinkSync(arr[i]); dup++; }
}
console.log('  删除重复 ' + dup + ' 个，保留 ' + kept.size + ' 个');

console.log('\n===== 3. 磁盘清单 =====');
const final = walk(DI).sort();
console.log('  images/dishes 共 ' + final.length + ' 张');
final.forEach(f => console.log('    ' + path.basename(f)));

// 中文文件名统一改为拼音（凉拌黄瓜.png）
const cn = final.filter(f => /[一-龥]/.test(path.basename(f)));
for (const f of cn) {
  const dest = path.join(DI, 'liangbanhuanggua.jpg');
  if (fs.existsSync(dest)) fs.unlinkSync(f);
  else fs.renameSync(f, dest);
}

console.log('\n===== 4. DB 路径同步 =====');
(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie' });
  const names = fs.readdirSync(DI).map(n => path.basename(n, path.extname(n)).toLowerCase());
  // dish.image -> images/dishes/<name>.jpg
  const [dish] = await c.query('SELECT id,image FROM dish');
  let fixed = 0;
  for (const r of dish) {
    const v = r.image ? String(r.image).trim() : '';
    if (!v) continue;
    const stem = path.basename(v, path.extname(v)).replace(/-1(-1)?$/i, '').toLowerCase();
    const ext = path.extname(v) || '.jpg';
    if (names.indexOf(stem) >= 0) {
      const nv = 'images/dishes/' + stem + path.extname(stem + ext).toLowerCase().replace('.jpg','.jpg').replace('.png','.png');
      const final = 'images/dishes/' + stem + '.jpg';
      if (v !== final) {
        await c.query('UPDATE dish SET image=? WHERE id=?', [final, r.id]);
        fixed++;
      }
    } else {
      console.log('  [缺失] dish#' + r.id + ' -> ' + v);
    }
  }
  console.log('  dish.image 修正 ' + fixed + ' 行');

  const [r1] = await c.query('UPDATE order_detail od JOIN dish d ON od.dish_id=d.id SET od.image=d.image WHERE od.image IS NULL OR od.image<>d.image');
  console.log('  order_detail 同步 ' + r1.affectedRows + ' 行');
  const [r2] = await c.query('UPDATE shopping_cart sc JOIN dish d ON sc.dish_id=d.id SET sc.image=d.image WHERE sc.image IS NULL OR sc.image<>d.image');
  console.log('  shopping_cart 同步 ' + r2.affectedRows + ' 行');
  await c.end();

  // 最终对账
  const c2 = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie' });
  const diskAll = walk(UP);
  const disk = new Set(diskAll.map(f => f.substring(UP.length + 1)));
  const pairs = [['dish','image'],['setmeal','image'],['order_detail','image'],['shopping_cart','image'],['dish_evaluation','images'],['cs_message','image_url'],['group_buy_campaign','IMAGE'],['employee','avatar'],['user','avatar'],['rider','avatar'],['store','logo'],['tenant','logo'],['supplier','LICENSE_IMAGES']];
  const refs = []; let blank = 0;
  for (const [t, col] of pairs) {
    let r; try { r = (await c2.query('SELECT ' + col + ' v FROM ' + t))[0]; } catch (e) { continue; }
    for (const x of r) {
      const v = x.v;
      if (v == null || String(v).trim() === '') { blank++; continue; }
      String(v).trim().split(',').map(z => z.trim()).filter(Boolean).forEach(p => refs.push(p));
    }
  }
  const uniq = [...new Set(refs)];
  const miss = uniq.filter(u => !disk.has(u));
  console.log('\n===== 5. 最终对账 =====');
  console.log('  磁盘图片 ' + diskAll.length + ' 张');
  console.log('  DB 引用单元格 ' + refs.length + ' 个（去重 ' + uniq.length + ' 条路径），空白 ' + blank);
  console.log('  缺失 ' + miss.length);
  miss.slice(0, 30).forEach(m => console.log('    ' + m));
  await c2.end();
})();
