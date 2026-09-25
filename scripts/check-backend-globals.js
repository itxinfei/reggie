// 临时校验脚本：检查 backend/ 下每个 HTML 页面内联 script 调用的全局函数
// 是否在页面加载的 js 文件（api/*.js 等）中有定义。
// 用法: node scripts/check-backend-globals.js
const fs = require('fs');
const path = require('path');

const RES = path.resolve(__dirname, '..', 'src', 'main', 'resources');
const BACKEND = path.join(RES, 'backend');

function walk(dir, ext, out) {
  if (!fs.existsSync(dir)) return;
  for (const f of fs.readdirSync(dir)) {
    const p = path.join(dir, f);
    const st = fs.statSync(p);
    if (st.isDirectory()) walk(p, ext, out);
    else if (p.endsWith(ext)) out.push(p);
  }
}

const htmls = [];
walk(BACKEND, '.html', htmls);

const builtin = new Set([
  'function', 'return', 'if', 'else', 'for', 'while', 'var', 'let', 'const', 'new',
  'this', 'typeof', 'instanceof', 'catch', 'try', 'finally', 'switch', 'case',
  'break', 'continue', 'delete', 'in', 'of', 'class', 'extends', 'super', 'throw',
  'yield', 'async', 'await', 'true', 'false', 'null', 'undefined', 'NaN',
  'Array', 'Object', 'String', 'Number', 'Boolean', 'JSON', 'Math', 'Date', 'RegExp',
  'Promise', 'console', 'window', 'document', 'navigator', 'location', 'history',
  'sessionStorage', 'localStorage', 'setTimeout', 'setInterval', 'clearTimeout',
  'clearInterval', 'requestAnimationFrame', 'cancelAnimationFrame',
  'encodeURIComponent', 'decodeURIComponent', 'parseInt', 'parseFloat', 'isNaN',
  'isFinite', 'String', 'Symbol', 'Error', 'Map', 'Set', 'WeakMap', 'WeakSet',
  'Infinity', 'BigInt', 'TextEncoder', 'TextDecoder', 'URLSearchParams', 'URL',
  'FormData', 'Blob', 'File', 'FileReader', 'Image', 'Audio',
]);

const noCheck = /^(el|van|Vue|axios|ri|ReggieUI|forceUpdate|on|emit|next|prev|push|pop|map|filter|reduce|forEach|find|some|every|sort|slice|splice|join|concat|indexOf|includes|startsWith|endsWith|trim|split|replace|match|exec|test|toFixed|toLocaleString|valueOf|toString|charAt|substring|call|apply|bind|assign|keys|values|entries|createElement|getElementById|querySelector|querySelectorAll|getElementsByTagName|getElementsByClassName|appendChild|removeChild|addEventListener|removeEventListener|setAttribute|getAttribute|scrollTo|scrollIntoView|focus|blur|click|stopPropagation|preventDefault|toJSON)$/i;

let problems = 0;
for (const h of htmls) {
  const src = fs.readFileSync(h, 'utf8').replace(/<!--[\s\S]*?-->/g, '');

  const jsFiles = [];
  const re = /<script[^>]*src=["']([^"']+)["']/g;
  let m;
  while ((m = re.exec(src))) jsFiles.push(m[1].split('?')[0]);

  let allJs = '';
  for (const jf of jsFiles) {
    try { allJs += fs.readFileSync(path.normalize(path.join(path.dirname(h), jf)), 'utf8') + '\n'; } catch (e) {}
  }

  const blocks = [...src.matchAll(/<script(?![^>]*src)[^>]*>([\s\S]*?)<\/script>/g)].map(x => x[1]).join('\n');

  const calls = [...blocks.matchAll(/\b([A-Za-z_$][\w$]*)\s*\(/g)].map(x => x[1]);
  const assignments = new Set();
  for (const am of src.matchAll(/@click=["']([A-Za-z_$][\w$]*)\s*=[^"']*["']/g)) assignments.add(am[1]);
  const defined = new Set([...allJs.matchAll(/(?:function|const|let|var)\s+([A-Za-z_$][\w$]*)/g)].map(x => x[1]));
  for (const mm of blocks.matchAll(/(?:^|\n)\s*(?:async\s+)?([A-Za-z_$][\w$]*)\s*\([^)]*\)\s*\{/g)) defined.add(mm[1]);

  for (const c of new Set(calls)) {
    if (builtin.has(c)) continue;
    if (defined.has(c)) continue;
    if (assignments.has(c)) continue;
    if (noCheck.test(c)) continue;
    if (new RegExp('function\\s+' + c + '\\b|var\\s+' + c + '\\b|const\\s+' + c + '\\b|let\\s+' + c + '\\b').test(blocks)) continue;
    if (new RegExp('\\.' + c + '\\s*\\(', 'g').test(blocks)) continue;
    problems++;
    console.log(path.relative(RES, h).replace(/\\/g, '/') + '  ->  undefined fn: ' + c);
  }
}
console.log('--- undefined fn refs: ' + problems);
