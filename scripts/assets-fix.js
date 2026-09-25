// assets-fix.js — 图片资源收口
// 1) DB 图片路径统一为 images/...（去掉 uploads/ public/ 前缀）
// 2) 磁盘 public/common/dishes -> images/dishes、public/common/setmeal -> images/dishes
// 3) 删除拼写错误/重复的无效图片
// 4) 给空白头像/logo 列填真实存在的图片
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const UP = path.join(ROOT, 'uploads');
const DISH = path.join(UP, 'images', 'dishes');
const SET = path.join(UP, 'images', 'setmeals');
const AV = path.join(UP, 'images', 'avatar');
const LOGO = path.join(UP, 'images', 'logos');
const SUP = path.join(UP, 'images', 'suppliers');
const STORE = path.join(UP, 'images', 'stores');
const EVALIMG = path.join(UP, 'images', 'evaluations');

const mkd = d => { if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true }); };
const log = m => console.log(m);

function walk(dir) {
  let out = [];
  if (!fs.existsSync(dir)) return out;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out = out.concat(walk(p));
    else if (/\.(jpg|jpeg|png|webp|gif|svg)$/i.test(e.name)) out.push(p);
  }
  return out;
}

// ---------- 步骤 1：整理磁盘 ----------
function dedupeMove(src, destDir) {
  const bySize = {};
  for (const f of walk(src)) {
    const s = fs.statSync(f).size;
    (bySize[s] = bySize[s] || []).push(f);
  }
  let moved = 0, removed = 0;
  for (const f of walk(src)) {
    const size = fs.statSync(f).size;
    const sib = bySize[size] || [];
    const targetName = path.basename(f);
    const dup = sib.filter(x => x !== f && fs.existsSync(x)).sort();
    const destBase = destDir + path.sep + targetName;
    if (dup.length && dup[0] < f) {
      fs.unlinkSync(f); removed++; continue;   // 保留字典序更靠前的一份
    }
    mkd(destDir);
    let dest = destBase;
    let k = 1;
    while (fs.existsSync(dest)) {
      dest = path.join(destDir, path.basename(targetName, path.extname(targetName)) + '-' + k + path.extname(targetName));
      k++;
    }
    if (dest !== f) { fs.renameSync(f, dest); moved++; }
  }
  return { moved, removed };
}

console.log('===== 磁盘整理 =====');
mkd(DISH); mkd(SET); mkd(AV); mkd(LOGO); mkd(SUP); mkd(STORE); mkd(EVALIMG);

// setmeal-*.jpg 留在 dishes 目录里更合理？——保留在 dishes，避免破坏 dish 已引用的路径
const r1 = dedupeMove(path.join(UP, 'public', 'common', 'dishes'), DISH);
console.log('  public/common/dishes -> images/dishes : 移动 ' + r1.moved + ' 删除重复 ' + r1.removed);

const r2 = dedupeMove(path.join(UP, 'public', 'common', 'setmeal'), DISH);
console.log('  public/common/setmeal -> images/dishes: 移动 ' + r2.moved + ' 删除重复 ' + r2.removed);

// 清理已清空的 public 树（保留 admin/system/user 等非菜品目录）
try {
  fs.rmdirSync(path.join(UP, 'public', 'common', 'dishes'));
  fs.rmdirSync(path.join(UP, 'public', 'common', 'setmeal'));
} catch (e) { }
try { fs.rmdirSync(path.join(UP, 'public', 'common')); } catch (e) { }

// images/dishes 内部再清一轮重复
const r3 = dedupeMove(DISH, DISH);
console.log('  images/dishes 内部去重: 删除 ' + r3.removed);

// ---------- 步骤 2：DB 路径统一 ----------
console.log('\n===== DB 路径统一 =====');
(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie', charset: 'utf8mb4' });
  const q = async s => { const [r] = await c.query(s); return r; };

  // 2a. 去掉 uploads/ public/ 前缀
  const normTables = [
    ['dish', 'image'], ['setmeal', 'image'], ['order_detail', 'image'],
    ['shopping_cart', 'image'], ['dish_evaluation', 'images'], ['cs_message', 'image_url'],
    ['group_buy_campaign', 'IMAGE'], ['employee', 'avatar'], ['user', 'avatar'],
    ['rider', 'avatar'], ['store', 'logo'], ['tenant', 'logo'], ['supplier', 'LICENSE_IMAGES']
  ];
  for (const [t, col] of normTables) {
    try {
      const [r] = await c.query('UPDATE `' + t + '` SET `' + col + '` = ' +
        'REPLACE(REPLACE(`' + col + '`, "uploads/", ""), "public/", "") WHERE `' + col + '` LIKE "uploads/%" OR `' + col + '` LIKE "public/%"');
      if (r.affectedRows) console.log('  ' + t + '.' + col + ' 归一 ' + r.affectedRows + ' 行');
    } catch (e) { console.log('  skip ' + t + '.' + col + ': ' + e.message); }
  }

  // 2b. 修正拼写错误的文件名
  const fixes = [
    ['dish', 'image', 'images/dishes/fanqiejidantang.jpg', 'images/dishes/fanqijidantang.jpg'],
    ['dish', 'image', 'images/dishes/suarongxilanhua.jpg', 'images/dishes/suorongxilanhua.jpg'],
    ['dish', 'image', 'images/dishes/yuxiangrous.jpg', 'images/dishes/yuxiangrousi.jpg']
  ];
  for (const [t, col, from, to] of fixes) {
    const [r] = await c.query('UPDATE `' + t + '` SET `' + col + '` = ? WHERE `' + col + '` = ?', [to, from]);
    if (r.affectedRows) console.log('  修正 ' + t + '.' + col + ' ' + from + ' -> ' + to);
  }

  // 2c. 同步 order_detail / shopping_cart 里的图片到 dish 当前值
  const [r2] = await c.query(
    'UPDATE order_detail od JOIN dish d ON od.dish_id = d.id SET od.image = d.image WHERE d.image IS NOT NULL AND d.image <> ""');
  console.log('  order_detail 按 dish 同步 ' + r2.affectedRows + ' 行');
  const [r3] = await c.query(
    'UPDATE shopping_cart sc JOIN dish d ON sc.dish_id = d.id SET sc.image = d.image WHERE d.image IS NOT NULL AND d.image <> ""');
  console.log('  shopping_cart 按 dish 同步 ' + r3.affectedRows + ' 行');

  // ---------- 步骤 3：补头像 / logo ----------
  console.log('\n===== 补头像/logo =====');
  const avatars = fs.readdirSync(AV).filter(f => /\.(jpg|png|jpeg)$/i.test(f)).sort();
  log('  可用头像: ' + avatars.length);
  // tenant.logo —— 10 家门店各给一个 logo（用 dishes 里较方形的图不合适，改用 avatars 之外的门店图）
  // 没有专门门店 logo 时，用 setmeal 图当门面图
  const setmealImgs = fs.readdirSync(DISH).filter(f => /^setmeal-.+\.jpg$/i.test(f));
  const [tenants] = await c.query('SELECT id,name FROM tenant ORDER BY id');
  for (let i = 0; i < tenants.length; i++) {
    const img = 'images/dishes/' + (setmealImgs[i % setmealImgs.length] || 'setmeal-shangwu.jpg');
    await c.query('UPDATE tenant SET logo = ?, license_image = ? WHERE id = ?',
      [img, 'images/logos/store-license-' + tenants[i].id + '.jpg', tenants[i].id]);
  }
  console.log('  tenant.logo/license_image 填充 ' + tenants.length + ' 行');

  const [stores] = await c.query('SELECT id FROM store ORDER BY id');
  for (let i = 0; i < stores.length; i++) {
    const img = 'images/dishes/' + (setmealImgs[i % setmealImgs.length] || 'setmeal-shangwu.jpg');
    await c.query('UPDATE store SET logo = ? WHERE id = ?', [img, stores[i].id]);
  }
  console.log('  store.logo 填充 ' + stores.length + ' 行');

  const [emps] = await c.query('SELECT id,avatar FROM employee ORDER BY id');
  for (let i = 0; i < emps.length; i++) {
    const v = emps[i].avatar;
    if (v === null || String(v).trim() === '') {
      await c.query('UPDATE employee SET avatar = ? WHERE id = ?', ['images/avatar/' + avatars[i % avatars.length], emps[i].id]);
    }
  }
  console.log('  employee.avatar 填充 ' + emps.length + ' 行');

  const [users] = await c.query('SELECT id,avatar FROM user ORDER BY id');
  for (let i = 0; i < users.length; i++) {
    const v = users[i].avatar;
    if (v === null || String(v).trim() === '') {
      await c.query('UPDATE user SET avatar = ? WHERE id = ?', ['images/avatar/' + avatars[(i + 3) % avatars.length], users[i].id]);
    }
  }
  console.log('  user.avatar 填充 ' + users.length + ' 行');

  const [riders] = await c.query('SELECT id,avatar FROM rider ORDER BY id');
  for (let i = 0; i < riders.length; i++) {
    const v = riders[i].avatar;
    if (v === null || String(v).trim() === '') {
      await c.query('UPDATE rider SET avatar = ? WHERE id = ?', ['images/avatar/' + avatars[(i + 5) % avatars.length], riders[i].id]);
    }
  }
  console.log('  rider.avatar 填充 ' + riders.length + ' 行');

  // 供应商营业执照图（用 logos 目录名占位，稍后下载生成）
  const [sups] = await c.query('SELECT id,name,LICENSE_IMAGES FROM supplier ORDER BY id');
  for (const s of sups) {
    const v = s.LICENSE_IMAGES;
    if (v === null || String(v).trim() === '') {
      await c.query('UPDATE supplier SET LICENSE_IMAGES = ? WHERE id = ?', ['images/suppliers/license-' + s.id + '.jpg', s.id]);
    }
  }
  console.log('  supplier.LICENSE_IMAGES 填充 ' + sups.length + ' 行');

  await c.end();

  // ---------- 步骤 4：报告 ----------
  const all = walk(UP);
  let bytes = 0; for (const f of all) bytes += fs.statSync(f).size;
  const dirs = {};
  for (const f of all) {
    const rel = f.substring(UP.length + 1);
    const seg = rel.split(path.sep).slice(0, 2).join('/');
    dirs[seg] = (dirs[seg] || 0) + 1;
  }
  console.log('\n===== 磁盘结果 =====');
  console.log('  图片总数 ' + all.length + '  总大小 ' + (bytes / 1048576).toFixed(1) + ' MB');
  for (const k of Object.keys(dirs).sort()) console.log('  ' + k.padEnd(22) + dirs[k] + ' 张');
})();
