// 生成 region 表真实行政区划数据 SQL
// 数据源: github modood/Administrative-divisions-of-China dist/pca-code.json (省+市+区县, 6位行政区划码)
// 目标表 region(id auto, name, code, parent_id, level[1省2市3区], sort, create_time, update_time, create_user, update_user, is_deleted)
// 用法: 先把 pca.json 放到 scripts/pca.json，再 node scripts/gen-region-seed.js
const fs = require('fs');
const path = require('path');

const PCA_PATH = path.join(__dirname, 'pca.json');
const OUT_SQL = path.join(__dirname, '..', 'src', 'main', 'resources', 'db', 'seed', 'region-seed.sql');

const raw = fs.readFileSync(PCA_PATH, 'utf8');
const pca = JSON.parse(raw);

// 结构确认（打印到 stdout，便于核对）
console.log('=== pca 结构 ===');
console.log('省级数:', pca.length);
console.log('首省:', JSON.stringify(pca[0]).slice(0, 300));
const c0 = pca[0] && pca[0].children && pca[0].children[0];
console.log('首市:', c0 ? JSON.stringify(c0).slice(0, 300) : '无 children');
const a0 = c0 && c0.children && c0.children[0];
console.log('首区:', a0 ? JSON.stringify(a0).slice(0, 200) : '无 children');

const NOW = '2026-09-25 18:00:00';
const rows = [];
let id = 0;
function esc(s) { return String(s == null ? '' : s).replace(/\\/g, '\\\\').replace(/'/g, "''"); }

// 省级 id 1..N，市级 id 紧接其后，区级再紧接；parent_id 引用已分配的 id
pca.forEach((prov, pi) => {
  if (!prov || !prov.name) return;
  id++; const provId = id;
  rows.push([provId, prov.name, prov.code || '', 0, 1, pi + 1, NOW, NOW, 1, 1, 0]);
  (prov.children || []).forEach((city, ci) => {
    if (!city || !city.name) return;
    id++; const cityId = id;
    rows.push([cityId, city.name, city.code || '', provId, 2, ci + 1, NOW, NOW, 1, 1, 0]);
    (city.children || []).forEach((area, ai) => {
      if (!area || !area.name) return;
      id++;
      rows.push([id, area.name, area.code || '', cityId, 3, ai + 1, NOW, NOW, 1, 1, 0]);
    });
  });
});

// 拼 SQL：DELETE + 分批 INSERT（每 500 行一条）
const COLS = 'id,name,code,parent_id,level,sort,create_time,update_time,create_user,update_user,is_deleted';
function rowVals(r) {
  return '(' + r.map(v => typeof v === 'string' ? "'" + esc(v) + "'" : v).join(',') + ')';
}
const BATCH = 500;
let out = '';
out += '-- 行政区划真实数据（省/市/区县，来源: github modood/Administrative-divisions-of-China pca-code.json）\n';
out += '-- 生成器: scripts/gen-region-seed.js\n';
out += 'SET NAMES utf8mb4;\n';
out += 'DELETE FROM `region`;\n';
out += 'ALTER TABLE `region` AUTO_INCREMENT = 1;\n';
for (let i = 0; i < rows.length; i += BATCH) {
  const chunk = rows.slice(i, i + BATCH);
  out += 'INSERT INTO `region` (' + COLS + ') VALUES\n';
  out += chunk.map(rowVals).join(',\n') + ';\n';
}
fs.writeFileSync(OUT_SQL, out, 'utf8');

console.log('=== 生成结果 ===');
console.log('总行数:', rows.length);
console.log('省:', rows.filter(r => r[4] === 1).length,
  '市:', rows.filter(r => r[4] === 2).length,
  '区/县:', rows.filter(r => r[4] === 3).length);
console.log('写入:', OUT_SQL);
