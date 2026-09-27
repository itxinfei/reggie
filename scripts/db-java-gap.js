// 数据库结构 vs Java 实体 双向差距分析
// 解析 db/reggie.sql 的表/列，扫描 src/main/java 下带 @TableName 的实体
// 运行：node scripts/db-java-gap.js
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SQL_FILE = process.argv[2] ? path.resolve(process.argv[2]) : path.join(ROOT, 'db', 'reggie.sql');
const JAVA_DIR = path.join(ROOT, 'src', 'main', 'java');

// ---------- 工具 ----------
function camelToSnake(s) {
  return s.replace(/([a-z0-9])([A-Z])/g, '$1_$2')
          .replace(/([A-Z]+)([A-Z][a-z])/g, '$1_$2')
          .toLowerCase();
}

function walkJava(dir, out) {
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name);
    const st = fs.statSync(p);
    if (st.isDirectory()) walkJava(p, out);
    else if (name.endsWith('.java')) out.push(p);
  }
}

// ---------- 1. 解析 SQL：表 -> 列（有序、去重） ----------
function parseSql(sql) {
  const tables = new Map();
  const lines = sql.split(/\r?\n/);
  let cur = null;
  for (const raw of lines) {
    const line = raw.trim();
    const mCreate = line.match(/^CREATE TABLE\s+`?(\w+)`?\s*\(/);
    if (mCreate) { cur = { name: mCreate[1], cols: [] }; continue; }
    if (cur) {
      // 建表块结束
      if (/^\)/.test(line)) { tables.set(cur.name, cur.cols); cur = null; continue; }
      // 列行：以反引号包裹的列名开头，后面紧跟类型
      const mCol = line.match(/^`(\w+)`\s+\w/);
      if (mCol) cur.cols.push(mCol[1]);
      // 约束行（PRIMARY KEY / INDEX / CONSTRAINT 等）忽略
    }
  }
  return tables;
}

// ---------- 2. 解析 Java 实体：表 -> 列 ----------
function parseEntity(file) {
  const text = fs.readFileSync(file, 'utf8');
  const mTable = text.match(/@TableName\s*\(\s*(?:value\s*=\s*)?"([^"]+)"/);
  if (!mTable) return null;
  const table = mTable[1];

  const cols = new Set();
  const lines = text.split(/\r?\n/);
  let pendingAnno = [];

  const consume = (declLine) => {
    const joined = pendingAnno.join('\n');
    pendingAnno = [];
    if (/\bstatic\b/.test(declLine)) return; // 静态字段（serialVersionUID 等）
    const mField = declLine.match(/(?:private|protected|public)\s+[^=;]*?\s(\w+)\s*[;=]/);
    if (!mField) return;
    const fieldName = mField[1];
    // @TableField(exist = false) 非表字段
    if (/exist\s*=\s*false/.test(joined)) return;
    // 显式列名：@TableField("x") / @TableField(value="x") / @TableId("x")
    let explicit = null;
    const mExplicit = joined.match(/@(?:TableField|TableId)\s*\(\s*(?:value\s*=\s*)?"([^"]+)"/);
    if (mExplicit) explicit = mExplicit[1];
    // 同行注解（声明同一行内）
    const mInline = declLine.match(/@(?:TableField|TableId)\s*\(\s*(?:value\s*=\s*)?"([^"]+)"/);
    if (mInline) explicit = mInline[1];
    if (/exist\s*=\s*false/.test(declLine)) return;
    cols.add(explicit || camelToSnake(fieldName));
  };

  for (const line of lines) {
    const t = line.trim();
    if (t.startsWith('@')) { pendingAnno.push(t); continue; }
    if (/\b(private|protected|public)\s+/.test(t)) { consume(t); continue; }
    if (t === '' ) { /* 保留注解挂接？空行则断开 */ pendingAnno = []; continue; }
    // 其他代码行：重置待挂注解
    pendingAnno = [];
  }
  return { table, cols, file: path.relative(ROOT, file) };
}

// ---------- 3. 对比 ----------
const sql = fs.readFileSync(SQL_FILE, 'utf8');
const dbTables = parseSql(sql);

const javaFiles = [];
walkJava(JAVA_DIR, javaFiles);
const entityByTable = new Map();
const noAnnoEntities = [];
for (const f of javaFiles) {
  const ent = parseEntity(f);
  if (ent) {
    if (entityByTable.has(ent.table)) {
      entityByTable.get(ent.table).push(ent);
    } else {
      entityByTable.set(ent.table, [ent]);
    }
  }
}

const dbOnlyTables = [];   // DB 有表、无实体
const entityOnlyTables = []; // 实体有表、DB 无
const fieldGaps = [];      // 同表列差距

for (const t of dbTables.keys()) {
  if (!entityByTable.has(t)) dbOnlyTables.push(t);
}
for (const t of entityByTable.keys()) {
  if (!dbTables.has(t)) entityOnlyTables.push(t);
}

for (const t of dbTables.keys()) {
  const ents = entityByTable.get(t);
  if (!ents) continue;
  // 合并多个同表实体的列（取并集）
  const javaCols = new Set();
  for (const e of ents) e.cols.forEach((c) => javaCols.add(c));
  const dbCols = dbTables.get(t);

  // 以小写归一化判定“是否存在”，再单独记录大小写不匹配
  const dbLowerToRaw = new Map();
  dbCols.forEach((c) => dbLowerToRaw.set(c.toLowerCase(), c));
  const javaLowerToRaw = new Map();
  javaCols.forEach((c) => javaLowerToRaw.set(c.toLowerCase(), c));

  const missingInJava = []; // DB 有、实体完全没有（忽略大小写）
  const caseMismatch = [];  // 仅列名大小写不同（MySQL 不报错，属规范不统一）
  for (const c of dbCols) {
    const low = c.toLowerCase();
    if (!javaLowerToRaw.has(low)) missingInJava.push(c);
    else if (javaLowerToRaw.get(low) !== c) caseMismatch.push(c + ' ≠ 实体:' + javaLowerToRaw.get(low));
  }
  const missingInDb = [];   // 实体有、DB 完全没有（会真报错）
  for (const c of javaCols) {
    if (!dbLowerToRaw.has(c.toLowerCase())) missingInDb.push(c);
  }
  if (missingInJava.length || missingInDb.length || caseMismatch.length) {
    fieldGaps.push({ table: t, missingInJava, missingInDb, caseMismatch, files: ents.map((e) => e.file) });
  }
}

// ---------- 4. 输出 ----------
const totalDbCols = [...dbTables.values()].reduce((a, c) => a + c.length, 0);
console.log('================ 总览 ================');
console.log('数据库表数:', dbTables.size, ' 数据库列总数:', totalDbCols);
console.log('Java 实体映射表数（去重）:', entityByTable.size, ' 实体类文件数:', [...entityByTable.values()].reduce((a, e) => a + e.length, 0));
console.log('');
console.log('数据库有表 / 无对应实体 (' + dbOnlyTables.length + '):');
dbOnlyTables.sort().forEach((t) => console.log('  - ' + t));
console.log('');
console.log('Java 有实体 / 数据库缺表 (' + entityOnlyTables.length + '):');
entityOnlyTables.sort().forEach((t) => console.log('  - ' + t));
console.log('');

let missJavaCount = 0, missDbCount = 0, caseCount = 0;
fieldGaps.forEach((g) => {
  missJavaCount += g.missingInJava.length;
  missDbCount += g.missingInDb.length;
  caseCount += g.caseMismatch.length;
});
console.log('================ 同表字段差距 ================');
console.log('实体真缺字段(DB有,实体无):', missJavaCount,
  '｜数据库真缺列(实体有,查询会报错):', missDbCount,
  '｜仅列名大小写不一致(MySQL不影响运行):', caseCount);
console.log('');

// 高频“实体普遍真缺失”的列（多为框架/父类统一处理）
const freqMiss = new Map();
fieldGaps.forEach((g) => g.missingInJava.forEach((c) => {
  const k = c.toLowerCase();
  freqMiss.set(k, (freqMiss.get(k) || 0) + 1);
}));
const common = [...freqMiss.entries()].filter(([, n]) => n >= 5).sort((a, b) => b[1] - a[1]);
if (common.length) {
  console.log('【高频：≥5 张表的实体都未声明该列，通常由框架/父类统一处理，可忽略】');
  common.forEach(([c, n]) => console.log('  ' + c + ' × ' + n + ' 张表'));
  console.log('');
}

// 仅大小写不一致的表
const caseTables = fieldGaps.filter((g) => g.caseMismatch.length);
if (caseTables.length) {
  console.log('【列名大小写不统一的表 (' + caseTables.length + ')：DB 大写、实体小写，建议统一但不影响运行】');
  caseTables.forEach((g) => console.log('  - ' + g.table + '（' + g.caseMismatch.length + ' 列）'));
  console.log('');
}

console.log('---------------- 真实差距明细（已剔除高频框架列噪音）----------------');
const commonCols = new Set(common.map(([c]) => c));
fieldGaps.sort((a, b) => a.table.localeCompare(b.table));
for (const g of fieldGaps) {
  const mj = g.missingInJava.filter((c) => !commonCols.has(c.toLowerCase()));
  if (!mj.length && !g.missingInDb.length) continue;
  console.log('■ ' + g.table);
  if (mj.length) console.log('   实体缺字段(DB有): ' + mj.join(', '));
  if (g.missingInDb.length) console.log('   数据库缺列(实体有,查询会报错): ' + g.missingInDb.join(', '));
}
