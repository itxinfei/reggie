/* =========================================================
   文档数据构建器（Node，无依赖）
   把 docs/*.md 打包成 docs/site/doc-data.js，
   使页面用 <script> 同步加载，避免 file:// 下 fetch 被禁。

   用法：node docs/site/build-doc-data.js
   （改了任何 md 后需重新执行一次）
   ========================================================= */
const fs = require('fs');
const path = require('path');

const HERE = __dirname;                              // docs/site
const DOCS_DIR = path.resolve(HERE, '..');           // docs

const FILES = [
  '模块与接口.md',
  'README.md',
  '项目总览.md',
  '架构设计.md',
  '数据模型.md',
  '后台页面清单.md'
];

let out = '/* 自动生成，请勿手改。由 build-doc-data.js 依据 docs/*.md 生成。\n' +
          '   修改 md 后请重跑：node docs/site/build-doc-data.js */\n' +
          'window.__DOC_DATA__ = window.__DOC_DATA__ || {};\n';

FILES.forEach(function (f) {
  const md = fs.readFileSync(path.join(DOCS_DIR, f), 'utf8');
  // "</" 转成 "<\/"：既不改变 JS 字符串取值，又防止源码里的 </script> 提前闭合标签
  const json = JSON.stringify(md).replace(/<\//g, '<\\/');
  out += 'window.__DOC_DATA__[' + JSON.stringify(f) + '] = ' + json + ';\n';
});

fs.writeFileSync(path.join(HERE, 'doc-data.js'), out, 'utf8');
console.log('已生成 doc-data.js，字节数 = ' + Buffer.byteLength(out, 'utf8'));
