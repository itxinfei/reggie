// 临时校验脚本：检查 front/ 与 backend/ 下所有 HTML 的相对路径资源引用是否存在
// 用法: node scripts/check-front-refs.js
const fs = require('fs');
const path = require('path');

const RES_ROOT = path.resolve(__dirname, '..', 'src', 'main', 'resources');
const FRONT = path.join(RES_ROOT, 'front');
const BACKEND = path.join(RES_ROOT, 'backend');

function walk(dir, ext, out) {
  if (!fs.existsSync(dir)) return;
  for (const f of fs.readdirSync(dir)) {
    const p = path.join(dir, f);
    const st = fs.statSync(p);
    if (st.isDirectory()) walk(p, ext, out);
    else if (p.endsWith(ext)) out.push(p);
  }
}

// 大小写精确校验：Windows/macOS 文件系统默认大小写不敏感，fs.existsSync 查不出
// 「引用名与真实文件名仅大小写不同」，而 Linux（及 jar 内 classpath 条目）是大小写敏感的，
// 这种引用在 Linux 部署后会 404。逐段 readdir 比对真实条目名。
// 返回 'ok' | 'case'（仅大小写不匹配）| 'missing'（文件不存在）
function checkPathCaseExact(p) {
  const parsed = path.parse(p);
  let cur = parsed.root;
  const segs = p.slice(parsed.root.length).split(path.sep).filter(Boolean);
  for (const seg of segs) {
    let entries;
    try {
      entries = fs.readdirSync(cur);
    } catch (e) {
      return 'missing';
    }
    if (!entries.includes(seg)) {
      return entries.some((e) => e.toLowerCase() === seg.toLowerCase()) ? 'case' : 'missing';
    }
    cur = path.join(cur, seg);
  }
  return fs.existsSync(cur) ? 'ok' : 'missing';
}

const htmls = [];
walk(FRONT, '.html', htmls);
walk(BACKEND, '.html', htmls);

let bad = 0;
const badRefs = [];
for (const h of htmls) {
  const src = fs.readFileSync(h, 'utf8').replace(/<!--[\s\S]*?-->/g, '');
  const refPattern = /(?:src|href)=("([^"]+)"|'([^']+)')/g;
  let m;
  while ((m = refPattern.exec(src))) {
    const ref = (m[2] || m[3] || '').split('?')[0];
    if (!ref || ref.startsWith('http') || ref.startsWith('//') || ref.startsWith('#') ||
        ref.startsWith('data:') || ref.startsWith('javascript:')) continue;
    if (!ref.startsWith('.')) continue; // 只校验相对路径
    const resolved = path.normalize(path.join(path.dirname(h), ref));
    const status = checkPathCaseExact(resolved);
    if (status !== 'ok') {
      bad++;
      badRefs.push((status === 'case' ? 'CASE    ' : 'MISSING ')
        + path.relative(RES_ROOT, h).replace(/\\/g, '/') + '  ->  ' + ref);
    }
  }
}

console.log('Pages: ' + htmls.length + ', relative refs broken: ' + bad);
badRefs.forEach((x) => console.log('  MISSING: ' + x));
if (bad === 0) console.log('ALL LOCAL RESOURCE REFS OK');
