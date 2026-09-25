// all-counts.js — 全库 117 表逐表真实行数统计
const mysql = require('mysql2/promise');
(async () => {
  const c = await mysql.createConnection({ host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'reggie' });
  const [t] = await c.query(`SELECT table_name FROM information_schema.tables WHERE table_schema='reggie' ORDER BY table_name`);
  const rows = [];
  for (const r of t) {
    const n = r.table_name;
    try {
      const [r2] = await c.query(`SELECT COUNT(*) n FROM \`${n}\``);
      rows.push([n, r2[0].n]);
    } catch (e) { rows.push([n, 'ERR']); }
  }
  rows.sort((a, b) => a[1] - b[1]);
  console.log('表数:', rows.length);
  const empty = rows.filter(r => r[1] === 0 || r[1] === 'ERR');
  const few = rows.filter(r => r[1] !== 0 && r[1] < 10);
  console.log('\n== 空表/异常 (' + empty.length + ') ==');
  for (const r of empty) console.log('  ' + r[0] + '\t' + r[1]);
  console.log('\n== 行数 < 10 (' + few.length + ') ==');
  for (const r of few) console.log('  ' + r[0].padEnd(32) + '\t' + r[1]);
  console.log('\n== 总行数 ==', rows.reduce((s, r) => s + (+r[1] || 0), 0));
  await c.end();
})();
