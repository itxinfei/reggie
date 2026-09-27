// 生成「大写列名 → 小写」幂等迁移脚本
// 解析 db/reggie.sql，对所有含大写列的表生成 RENAME COLUMN（经存储过程判幂等）
// 运行：node scripts/gen-rename-columns.js
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SQL_FILE = path.join(ROOT, 'db', 'reggie.sql');
const OUT_FILE = path.join(ROOT, 'db', 'normalize-uppercase-columns.sql');

function parseTables(sql) {
  const tables = new Map();
  let cur = null;
  for (const raw of sql.split(/\r?\n/)) {
    const line = raw.trim();
    const m = line.match(/^CREATE TABLE\s+`?(\w+)`?\s*\(/);
    if (m) { cur = { name: m[1], cols: [] }; continue; }
    if (cur) {
      if (/^\)/.test(line)) { tables.set(cur.name, cur.cols); cur = null; continue; }
      const mc = line.match(/^`(\w+)`\s+\w/);
      if (mc) cur.cols.push(mc[1]);
    }
  }
  return tables;
}

const tables = parseTables(fs.readFileSync(SQL_FILE, 'utf8'));

// 收集 大写列 → 小写
const tasks = [];
for (const [table, cols] of tables) {
  const renames = [];
  const lowerSet = new Set(cols.map((c) => c.toLowerCase()));
  for (const c of cols) {
    const low = c.toLowerCase();
    if (c !== low) renames.push([c, low]);
  }
  if (renames.length) tasks.push({ table, renames });
}

let colCount = 0;
const body = [];
body.push('-- 大写列名归一化为小写（MySQL 列名本就不区分大小写，此为规范统一，不改变任何数据与类型）');
body.push('-- 幂等：仅当列确实为大写形态、且目标小写名不存在时才 RENAME，可重复执行；自动保留索引。');
body.push('-- 适用 MySQL 8.0；执行前建议备份。');
body.push('');
body.push('DROP PROCEDURE IF EXISTS rename_col_ci;');
body.push('DELIMITER $$');
body.push('CREATE PROCEDURE rename_col_ci(IN p_tbl VARCHAR(64), IN p_old VARCHAR(64), IN p_new VARCHAR(64))');
body.push('BEGIN');
body.push('  IF BINARY p_old <> BINARY p_new');
body.push('     AND EXISTS (SELECT 1 FROM information_schema.COLUMNS');
body.push('                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_tbl');
body.push('                   AND COLUMN_NAME = p_old AND BINARY COLUMN_NAME = p_old)');
body.push('     AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS');
body.push('                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_tbl');
body.push('                   AND BINARY COLUMN_NAME = p_new) THEN');
body.push('    SET @s = CONCAT(\'ALTER TABLE `\', p_tbl, \'` RENAME COLUMN `\', p_old, \'` TO `\', p_new, \'`\');');
body.push('    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;');
body.push('  END IF;');
body.push('END$$');
body.push('DELIMITER ;');
body.push('');

for (const t of tasks.sort((a, b) => a.table.localeCompare(b.table))) {
  body.push('-- ' + t.table + '（' + t.renames.length + ' 列）');
  for (const [oldC, newC] of t.renames) {
    body.push('CALL rename_col_ci(\'' + t.table + '\', \'' + oldC + '\', \'' + newC + '\');');
    colCount++;
  }
  body.push('');
}
body.push('DROP PROCEDURE IF EXISTS rename_col_ci;');

fs.writeFileSync(OUT_FILE, body.join('\n'), 'utf8');
console.log('涉及表数:', tasks.length, ' 重命名列数:', colCount);
console.log('已写出:', path.relative(ROOT, OUT_FILE));
tasks.forEach((t) => console.log('  ' + t.table + ' ' + t.renames.length + '列'));
