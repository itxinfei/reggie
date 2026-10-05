/* =========================================================
   瑞吉外卖 · 文档 / API 查看器（原生 JS，零依赖）
   - 自研轻量 GFM 渲染：标题/表格/列表/引用/围栏代码/代码/粗体/链接
   - API 模式：模块目录 + 实时搜索 + HTTP 方法徽章 + 路径点击复制
   ========================================================= */
(function () {
  'use strict';

  /* ---------------- 基础转义 / 行内 ---------------- */
  function esc(s) {
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function escReg(s) { return String(s).replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }

  /* 高亮命中词 */
  function hl(s, q) {
    if (!q) return s;
    return s.replace(new RegExp('(' + escReg(esc(q)) + ')', 'gi'), '<mark>$1</mark>');
  }

  function reshapeUrl(u) {
    if (/^(https?:|mailto:|#)/.test(u)) return { url: u, ext: /^https?:/.test(u) };
    var f = u.replace(/^\.\//, '').split('#')[0];
    if (/\.md$/i.test(f)) {
      if (f === '模块与接口.md') return { url: 'api.html', ext: false };
      return { url: 'doc.html?p=' + encodeURIComponent(f), ext: false };
    }
    return { url: u, ext: false };
  }

  /* 行内：代码 / 粗体 / 链接 / 高亮 */
  function inline(raw, q) {
    var parts = String(raw).split(/(`[^`]+`|\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))/);
    return parts.map(function (p) {
      if (!p) return '';
      if (p.charAt(0) === '`') return '<code>' + hl(esc(p.slice(1, -1)), q) + '</code>';
      if (p.length > 3 && p.slice(0, 2) === '**' && p.slice(-2) === '**')
        return '<strong>' + hl(esc(p.slice(2, -2)), q) + '</strong>';
      var lm = /^\[([^\]]+)\]\(([^)]+)\)$/.exec(p);
      if (lm) {
        var r = reshapeUrl(lm[2].trim());
        var tgt = r.ext ? ' target="_blank" rel="noopener"' : '';
        return '<a class="link" href="' + esc(r.url) + '"' + tgt + '>' + hl(esc(lm[1]), q) + '</a>';
      }
      return hl(esc(p), q);
    }).join('');
  }

  function stripInline(raw) {
    return String(raw).replace(/`|\*\*/g, '').replace(/\[([^\]]+)\]\([^)]+\)/g, '$1').trim();
  }

  function slug(s) {
    var x = String(s).toLowerCase().replace(/<[^>]+>/g, '')
      .replace(/[^\w一-龥]+/g, '-').replace(/^-+|-+$/g, '');
    return x || 'sec';
  }

  /* ---------------- 块级解析（原始文本，渲染延迟到带 q 时） ---------------- */
  function splitCells(line) {
    var t = line.trim();
    t = t.replace(/^\|/, '').replace(/\|$/, '');
    return t.split('|').map(function (c) { return c.trim(); });
  }

  function isTableRow(line) { return /^\s*\|.*\|\s*$/.test(line); }
  function isSepRow(line) {
    return /^\s*\|?[\s:|-]+\|?\s*$/.test(line) && line.indexOf('-') !== -1 && !line.match(/[^\s:|-]/);
  }

  function parseBlocks(md) {
    var lines = md.replace(/\r\n?/g, '\n').split('\n');
    var blocks = [];
    var i = 0;
    function paraPush(text) {
      blocks.push({ t: 'p', raw: text });
    }
    while (i < lines.length) {
      var line = lines[i];

      // 围栏代码
      var fm = /^```(.*)$/.exec(line);
      if (fm) {
        var lang = fm[1].trim(), buf = [];
        i++;
        while (i < lines.length && !/^```/.test(lines[i])) { buf.push(lines[i]); i++; }
        i++;
        blocks.push({ t: 'code', code: buf.join('\n'), lang: lang });
        continue;
      }

      // 标题
      var hm = /^(#{1,4})\s+(.*)$/.exec(line);
      if (hm) { blocks.push({ t: 'h', lvl: hm[1].length, raw: hm[2].trim() }); i++; continue; }

      // 分隔线
      if (/^---+\s*$/.test(line)) { blocks.push({ t: 'hr' }); i++; continue; }

      // 表格
      if (isTableRow(line) && i + 1 < lines.length && isSepRow(lines[i + 1])) {
        var head = splitCells(line);
        i += 2;
        var rows = [];
        while (i < lines.length && isTableRow(lines[i])) { rows.push(splitCells(lines[i])); i++; }
        var methodCol = -1, pathCol = -1;
        head.forEach(function (hcell, k) {
          var hc = stripInline(hcell);
          if (hc === '方法' || hc === 'HTTP 方法') methodCol = k;
          if (hc === '路径' || hc === 'URL' || hc === '端点') pathCol = k;
        });
        blocks.push({ t: 'table', head: head, rows: rows, endpoint: methodCol !== -1 && pathCol !== -1, methodCol: methodCol, pathCol: pathCol });
        continue;
      }

      // 引用（连续）
      if (/^>\s?/.test(line)) {
        var qb = [];
        while (i < lines.length && /^>\s?/.test(lines[i])) { qb.push(lines[i].replace(/^>\s?/, '')); i++; }
        blocks.push({ t: 'quote', raw: qb.join(' ').trim() });
        continue;
      }

      // 无序列表（连续）
      if (/^\s*-\s+/.test(line)) {
        var items = [];
        while (i < lines.length && /^\s*-\s+/.test(lines[i])) { items.push(lines[i].replace(/^\s*-\s+/, '').trim()); i++; }
        blocks.push({ t: 'ul', items: items });
        continue;
      }

      // 空行
      if (/^\s*$/.test(line)) { i++; continue; }

      // 段落（聚合连续普通行）
      var pb = [line.trim()];
      i++;
      while (i < lines.length && !/^\s*$/.test(lines[i]) &&
        !/^(#{1,4})\s/.test(lines[i]) && !/^```/.test(lines[i]) &&
        !/^---+\s*$/.test(lines[i]) && !/^>/.test(lines[i]) &&
        !/^\s*-\s+/.test(lines[i]) && !isTableRow(lines[i])) {
        pb.push(lines[i].trim()); i++;
      }
      paraPush(pb.join(' '));
    }
    return blocks;
  }

  /* ---------------- 块渲染 ---------------- */
  function renderBlock(b, q) {
    switch (b.t) {
      case 'h': {
        var id = b._id || (b._id = slug(stripInline(b.raw)));
        return '<h' + b.lvl + ' id="' + id + '">' + inline(b.raw, q) + '</h' + b.lvl + '>';
      }
      case 'hr': return '<hr>';
      case 'quote': return '<blockquote>' + inline(b.raw, q) + '</blockquote>';
      case 'p': return '<p>' + inline(b.raw, q) + '</p>';
      case 'code':
        return '<pre class="fence' + (b.lang ? ' lang-' + esc(b.lang) : '') + '"><code>' + esc(b.code) + '</code></pre>';
      case 'ul':
        return '<ul>' + b.items.map(function (it) { return '<li>' + inline(it, q) + '</li>'; }).join('') + '</ul>';
      case 'table': return renderTable(b, q);
      default: return '';
    }
  }

  function methodBadge(raw) {
    var m = stripInline(raw).toUpperCase();
    var cls = { GET: 'get', POST: 'post', PUT: 'put', DELETE: 'delete', PATCH: 'patch' }[m] || 'patch';
    return '<span class="m m-' + cls + '">' + esc(m) + '</span>';
  }

  function renderTable(b, q) {
    var h = '<div class="table-wrap"><table><thead><tr>' +
      b.head.map(function (c) { return '<th>' + inline(c, q) + '</th>'; }).join('') +
      '</tr></thead><tbody>' +
      b.rows.map(function (r) {
        return '<tr>' + r.map(function (c, k) {
          if (b.endpoint && k === b.methodCol) return '<td class="num">' + methodBadge(c) + '</td>';
          if (b.endpoint && k === b.pathCol) {
            return '<td><code class="path" title="点击复制路径">' + hl(esc(stripInline(c)), q) + '</code></td>';
          }
          return '<td>' + inline(c, q) + '</td>';
        }).join('') + '</tr>';
      }).join('') +
      '</tbody></table></div>';
    return h;
  }

  /* ---------------- 渲染：普通文档模式 ---------------- */
  function renderDoc(blocks, q, skipH1) {
    var toc = [];
    var used = {};
    blocks.forEach(function (b) {
      if (b.t === 'h') {
        if (skipH1 && b.lvl === 1) return;
        var base = slug(stripInline(b.raw)), id = base, n = 1;
        while (used[id]) { id = base + '-' + (++n); }
        used[id] = 1; b._id = id;
        if (b.lvl >= 2) toc.push({ lvl: b.lvl, text: stripInline(b.raw), id: id });
      }
    });
    var html = blocks.map(function (b) {
      if (b.t === 'h' && skipH1 && b.lvl === 1) return '';
      return renderBlock(b, q);
    }).join('\n');
    return { html: html, toc: toc };
  }

  /* ---------------- 渲染：API 模式（模块卡片） ---------------- */
  function parseModule(raw) {
    var m = /^(\d+)[.、]\s*([A-Za-z0-9_-]+)\s*[（(]([^）)]*)[）)]\s*$/.exec(stripInline(raw));
    if (m) return { no: m[1], en: m[2], zh: m[3] };
    return { no: '', en: stripInline(raw), zh: '' };
  }

  function renderApi(blocks, q) {
    var firstH3 = -1, lastH3 = -1;
    blocks.forEach(function (b, k) {
      if (b.t === 'h' && b.lvl === 3) { if (firstH3 === -1) firstH3 = k; lastH3 = k; }
    });

    var overhead = [], notes = [], sections = [];
    blocks.forEach(function (b, k) {
      if (b.t === 'h' && b.lvl === 3) return;
      if (k < firstH3) { if (b.t !== 'h') overhead.push(b); }
      else if (k > lastH3) { if (b.t !== 'h') notes.push(b); }
    });

    for (var i = firstH3; i <= lastH3; i++) {
      var b = blocks[i];
      if (b.t === 'h' && b.lvl === 3) {
        sections.push({ meta: parseModule(b.raw), body: [] });
      } else if (sections.length && b.t !== 'h') {
        sections[sections.length - 1].body.push(b);
      }
    }

    var endpointCount = 0;
    sections.forEach(function (s) {
      s.body.forEach(function (b) { if (b.t === 'table' && b.endpoint) endpointCount += b.rows.length; });
    });

    var headHtml = '<div class="api-overhead">' + overhead.map(function (b) { return renderBlock(b, q); }).join('') + '</div>';
    var secHtml = sections.map(function (s, k) {
      var id = 'sec-' + slug(s.meta.en) + '-' + (k + 1);
      var body = s.body.map(function (b) { return renderBlock(b, q); }).join('');
      return '<section class="api-section" id="' + id + '" data-en="' + esc(s.meta.en.toLowerCase()) +
        '" data-zh="' + esc(s.meta.zh) + '">' +
        '<div class="sec-head"><span class="tag-no">#' + String(k + 1).padStart(2, '0') + '</span>' +
        '<h3><span class="mod-en">' + esc(s.meta.en) + '</span></h3>' +
        (s.meta.zh ? '<span class="mod-zh">' + esc(s.meta.zh) + '</span>' : '') +
        '</div><div class="sec-body">' + body + '</div></section>';
    }).join('');
    var notesHtml = '<div class="api-notes">' + notes.map(function (b) { return renderBlock(b, q); }).join('') + '</div>';

    return {
      html: headHtml + secHtml + notesHtml,
      toc: sections.map(function (s, k) {
        return { lvl: 3, no: String(k + 1).padStart(2, '0'), en: s.meta.en, zh: s.meta.zh, id: 'sec-' + slug(s.meta.en) + '-' + (k + 1) };
      }),
      endpointCount: endpointCount,
      moduleCount: sections.length
    };
  }

  /* ---------------- 查看器启动 ---------------- */
  var DOCS = {
    'README.md': '项目文档',
    '项目总览.md': '项目总览',
    '架构设计.md': '架构设计',
    '数据模型.md': '数据模型',
    '后台页面清单.md': '后台页面清单'
  };

  function getParam(name) {
    var m = new RegExp('[?&]' + name + '=([^&]*)').exec(location.search);
    return m ? decodeURIComponent(m[1]) : null;
  }

  function copyText(text, ok) {
    function fallback() {
      var ta = document.createElement('textarea');
      ta.value = text; document.body.appendChild(ta); ta.select();
      try { document.execCommand('copy'); ok(); } catch (e) {}
      document.body.removeChild(ta);
    }
    if (navigator.clipboard && navigator.clipboard.writeText)
      navigator.clipboard.writeText(text).then(ok).catch(fallback);
    else fallback();
  }

  function initTheme() {
    var root = document.documentElement, btn = document.getElementById('themeToggle');
    var saved = null;
    try { saved = localStorage.getItem('reggie-theme'); } catch (e) {}
    var dark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    setTheme(saved || (dark ? 'dark' : 'light'));
    function setTheme(t) {
      root.setAttribute('data-theme', t);
      if (btn) btn.textContent = t === 'dark' ? '☀️' : '🌙';
      try { localStorage.setItem('reggie-theme', t); } catch (e) {}
    }
    if (btn) btn.addEventListener('click', function () {
      setTheme(root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
    });
  }

  function initShell() {
    var sidebar = document.getElementById('sidebar');
    var backdrop = document.getElementById('sideBackdrop');
    var toggle = document.getElementById('sideToggle');
    function close() { sidebar.classList.remove('show'); backdrop.classList.remove('show'); }
    if (toggle) toggle.addEventListener('click', function () {
      sidebar.classList.toggle('show'); backdrop.classList.toggle('show');
    });
    if (backdrop) backdrop.addEventListener('click', close);
    if (sidebar) sidebar.addEventListener('click', function (e) {
      if (e.target.closest('a')) setTimeout(close, 120);
    });
    return close;
  }

  function buildSidebarApi(toc) {
    document.getElementById('sideTitle').textContent = '模块导航 · ' + toc.length;
    document.getElementById('sideNav').innerHTML = toc.map(function (t) {
      return '<a href="#' + t.id + '" data-target="' + t.id + '"><span class="idx">' + t.no + '</span>' +
        '<span class="en">' + esc(t.en) + '</span>' + (t.zh ? '<span class="zh">' + esc(t.zh) + '</span>' : '') + '</a>';
    }).join('');
  }

  function buildSidebarDoc(toc) {
    document.getElementById('sideTitle').textContent = '本页目录';
    document.getElementById('sideNav').innerHTML = toc.map(function (t) {
      return '<a class="lvl-' + t.lvl + '" href="#' + t.id + '" data-target="' + t.id + '">' + esc(t.text) + '</a>';
    }).join('');
  }

  var spyObserver = null;
  function startSpy(closeSidebar) {
    if (spyObserver) spyObserver.disconnect();
    var links = document.querySelectorAll('#sideNav a');
    var map = {};
    links.forEach(function (a) { map[a.getAttribute('data-target')] = a; });
    spyObserver = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting && map[en.target.id]) {
          links.forEach(function (a) { a.classList.remove('active'); });
          map[en.target.id].classList.add('active');
        }
      });
    }, { rootMargin: '0px 0px -80% 0px' });
    var targets = document.querySelectorAll('.api-section');
    if (!targets.length) {
      // doc 模式：观察 h2/h3
      document.querySelectorAll('.doc-body h2[id],.doc-body h3[id]').forEach(function (h) { spyObserver.observe(h); });
    } else {
      targets.forEach(function (s) { spyObserver.observe(s); });
    }
  }

  /* ---------------- API 搜索 ---------------- */
  function applyFilter(q) {
    var term = q.trim().toLowerCase();
    var sections = document.querySelectorAll('.api-section');
    var visibleEndpoints = 0, visibleSections = 0;
    sections.forEach(function (sec) {
      var moduleHit = !term ||
        (sec.getAttribute('data-en') + ' ' + sec.getAttribute('data-zh')).indexOf(term) !== -1;
      var rows = sec.querySelectorAll('tbody tr');
      var anyRow = false;
      rows.forEach(function (tr) {
        var show = moduleHit || tr.textContent.toLowerCase().indexOf(term) !== -1;
        tr.hidden = !show;
        if (show) { anyRow = true; visibleEndpoints++; }
      });
      sec.classList.toggle('is-hidden', term && !anyRow);
      if (!term || anyRow) visibleSections++;
    });
    document.getElementById('apiOverhead').classList.toggle('is-hidden', !!term);
    document.getElementById('apiNotes').classList.toggle('is-hidden', !!term);
    document.getElementById('noResult').classList.toggle('show', !!term && visibleSections === 0);

    // 侧栏随命中置灰
    document.querySelectorAll('#sideNav a').forEach(function (a) {
      var sec = document.getElementById(a.getAttribute('data-target'));
      a.classList.toggle('dim', !!term && (!sec || sec.classList.contains('is-hidden')));
    });

    var cnt = document.getElementById('resultCount');
    if (cnt) cnt.innerHTML = term
      ? '命中 <b>' + visibleEndpoints + '</b> 个接口 · ' + visibleSections + ' 个模块'
      : '共 <b>' + document.querySelectorAll('.api-section tbody tr').length + '</b> 个接口';
  }

  /* ---------------- 主入口 ---------------- */
  function boot() {
    initTheme();
    var closeSidebar = initShell();
    var content = document.getElementById('docContent');
    var mode = document.body.getAttribute('data-mode'); // 'api' | 'doc'

    if (mode === 'api') {
      showApi(content, closeSidebar);
    } else {
      var file = getParam('p') || 'README.md';
      if (!DOCS[file]) file = 'README.md';
      document.title = DOCS[file] + ' · 瑞吉外卖文档';
      showDoc(content, file, closeSidebar);
    }
  }

  function fail(content, msg) {
    content.innerHTML = '<div class="doc-status">⚠️ ' + esc(msg) +
      '<br><br><a class="btn btn-ghost btn-sm" href="index.html">返回官网</a></div>';
  }

  var apiState = { blocks: null, closeSidebar: function () {} };

  /* 依据当前关键词重绘正文（保留 #apiBody 外壳与工具条），返回本次视图 */
  function paintApi(q) {
    var view = renderApi(apiState.blocks, q);
    var apiBody = document.getElementById('apiBody');
    var noResult = document.getElementById('noResult');
    apiBody.querySelectorAll('.api-section,.api-notes').forEach(function (n) { n.remove(); });

    var detached = document.createElement('div');
    detached.innerHTML = view.html;
    var oh = detached.querySelector('.api-overhead');
    var nt = detached.querySelector('.api-notes');
    document.getElementById('apiOverhead').innerHTML = oh ? oh.innerHTML : '';
    detached.querySelectorAll('.api-section').forEach(function (s) { apiBody.insertBefore(s, noResult); });
    if (nt && nt.innerHTML.trim()) {
      var nw = document.createElement('div');
      nw.id = 'apiNotes';
      nw.className = 'api-notes';
      nw.innerHTML = nt.innerHTML;
      apiBody.insertBefore(nw, noResult);
    }
    startSpy(apiState.closeSidebar);
    return view;
  }

  /* 从内嵌数据包读取文档（doc-data.js 用 <script> 同步加载，file:// 下也可用） */
  function getDoc(file) {
    var store = window.__DOC_DATA__ || {};
    return Object.prototype.hasOwnProperty.call(store, file) ? store[file] : null;
  }

  function showApi(content, closeSidebar) {
    var md = getDoc('模块与接口.md');
    if (md == null) { fail(content, '接口数据未找到，请确认 doc-data.js 已随页面一起加载。'); return; }
    var blocks = parseBlocks(md);
    apiState.blocks = blocks;
    apiState.closeSidebar = closeSidebar;
    var initView = renderApi(blocks, '');

    content.innerHTML =
        '<div class="page-head"><h1>模块与接口</h1>' +
        '<p>36 个业务模块、73 个 Controller 的完整 REST 端点清单，支持按模块 / 路径 / 说明实时检索。</p>' +
        '<div class="head-stats">' +
        '<span class="chip">📦 <small>业务模块</small> ' + initView.moduleCount + '</span>' +
        '<span class="chip">🔌 <small>REST 接口</small> ' + initView.endpointCount + '</span>' +
        '<span class="chip">🧩 <small>Controller</small> 73</span>' +
        '<span class="chip">📄 <small>统一响应</small> R&lt;T&gt;</span>' +
        '</div></div>' +
        '<div class="api-toolbar"><div class="search-box"><span class="ico">🔎</span>' +
        '<input id="searchInput" type="search" placeholder="搜索模块 / 路径 / 说明，如：退款、/api/ai、GET…" autocomplete="off">' +
        '<button class="clear" id="searchClear" hidden>×</button></div>' +
        '<div class="result-count" id="resultCount"></div></div>' +
        '<div class="doc-body" id="apiBody">' +
        '<div id="apiOverhead" class="api-overhead"></div>' +
        '<div id="noResult"><span class="big">🔍</span>没有找到匹配的接口，换个关键词试试～</div></div>';

      buildSidebarApi(initView.toc);
      paintApi('');
      applyFilter('');

      var input = document.getElementById('searchInput');
      var clearBtn = document.getElementById('searchClear');
      var timer = null;
      function doSearch() {
        var q = input.value;
        clearBtn.hidden = !q;
        if (timer) clearTimeout(timer);
        timer = setTimeout(function () {
          paintApi(q.trim());
          applyFilter(q);
        }, 130);
      }
      input.addEventListener('input', doSearch);
      clearBtn.addEventListener('click', function () {
        input.value = '';
        input.focus();
        doSearch();
      });

      // 路径点击复制（#apiBody 持久存在，事件委托对重绘免疫）
      document.getElementById('apiBody').addEventListener('click', function (e) {
        var code = e.target.closest('code.path');
        if (!code) return;
        copyText(code.textContent.trim(), function () {
          var old = code.textContent;
          code.classList.add('copied');
          code.textContent = '已复制 ✓';
          setTimeout(function () { code.classList.remove('copied'); code.textContent = old; }, 1300);
        });
      });
  }

  function showDoc(content, file, closeSidebar) {
    var md = getDoc(file);
    if (md == null) { fail(content, '文档「' + DOCS[file] + '」数据未找到。'); return; }
    var blocks = parseBlocks(md);
    var view = renderDoc(blocks, '', true);
    content.innerHTML =
      '<div class="page-head"><h1>' + esc(DOCS[file]) + '</h1></div>' +
      '<div class="doc-body">' + view.html + '</div>';
    buildSidebarDoc(view.toc);
    startSpy(closeSidebar);
  }

  document.addEventListener('DOMContentLoaded', boot);
})();
