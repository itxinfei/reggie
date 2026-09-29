// 后台管理端页面巡检：admin 登录 → 逐页真实打开 → 收集渲染缺陷 → 截图取证
// 检测：console错误/pageerror、HTTP 4xx5xx、横向溢出、文字截断、裂图、
//       Vue未编译({{}})、空白页、loading卡死、fixed元素越界
// 用法：node scripts/ui-audit-backend.js
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');
const fs = require('fs');
const path = require('path');

const BASE = 'http://localhost:8080';
const PAGE_ROOT = 'D:\\MyCode\\reggie\\src\\main\\resources\\backend\\page';
const OUT_DIR = 'C:\\Users\\itxinfei\\AppData\\Local\\Temp\\ui-audit\\backend';
const REPORT = 'D:\\MyCode\\reggie\\logs\\ui-audit-backend.json';
fs.mkdirSync(OUT_DIR, { recursive: true });
fs.mkdirSync('D:\\MyCode\\reggie\\logs', { recursive: true });

function walkHtml(dir) {
  const out = [];
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name);
    if (fs.statSync(full).isDirectory()) out.push.apply(out, walkHtml(full));
    else if (/\.html?$/i.test(name)) out.push(full);
  }
  return out;
}

const files = walkHtml(PAGE_ROOT)
  .map(f => path.relative(PAGE_ROOT, f).replace(/\\/g, '/'))
  .filter(f => f.indexOf('login/') !== 0 && f.indexOf('_templates/') !== 0)
  .sort();

// 这类页面必须带 ?id= 等参数，裸打开出现接口报错属正常，不计硬伤
const needsParam = f => /(detail|edit|form|view|record)\.html/i.test(f);

(async () => {
  const browser = await chromium.launch();
  const results = [];

  for (const vp of [{ label: '1440', w: 1440, h: 900 }, { label: '1366', w: 1366, h: 768 }]) {
    const ctx = await browser.newContext({ viewport: { width: vp.w, height: vp.h } });
    const page = await ctx.newPage();

    // ---- 登录 ----
    await page.goto(BASE + '/backend/page/login/login.html', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(600);
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[type="password"]', '123456');
    await page.click('button[aria-label="登录"]');
    await page.waitForURL(u => u.toString().indexOf('login.html') < 0, { timeout: 10000 });
    await page.waitForTimeout(800);

    for (const f of files) {
      const url = BASE + '/backend/page/' + f;
      const cerr = [];
      const perr = [];
      const badHttp = [];
      const onConsole = m => { if (m.type() === 'error') cerr.push(m.text().slice(0, 200)); };
      const onPageErr = e => perr.push(String(e.message || e).slice(0, 200));
      const onResp = r => {
        try {
          const s = r.status();
          if (s >= 400 && r.url().indexOf(BASE) >= 0) badHttp.push(s + ' ' + r.url().replace(BASE, '').slice(0, 120));
        } catch (e) {}
      };
      page.on('console', onConsole);
      page.on('pageerror', onPageErr);
      page.on('response', onResp);

      let gotoFail = null;
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 15000 })
        .catch(e => { gotoFail = String(e.message || e).slice(0, 150); });
      await page.waitForTimeout(1800);

      const checks = await page.evaluate(() => {
        const r = { hOverflow: 0, textClip: [], brokenImg: [], uncompiled: 0, blank: 0, stuckLoading: 0, fixedOutOfView: [] };
        const de = document.documentElement;
        r.hOverflow = Math.max(0, de.scrollWidth - window.innerWidth);

        // 文字截断：叶子元素内容撑破盒子
        const all = document.querySelectorAll('#app *:not(script):not(style)');
        for (const el of all) {
          if (el.children.length > 0) continue;
          const t = (el.textContent || '').trim();
          if (t.length < 2) continue;
          if (el.clientWidth > 20 && el.scrollWidth > el.clientWidth + 6) {
            r.textClip.push(((el.className || '') + '').toString().slice(0, 50) + ' | ' + t.slice(0, 16));
            if (r.textClip.length >= 5) break;
          }
        }
        // 裂图
        document.querySelectorAll('img').forEach(img => {
          if (img.offsetWidth > 0 && img.complete && img.naturalWidth === 0) {
            r.brokenImg.push((img.src || '').slice(-60));
          }
        });
        // Vue 挂载失败会残留 {{ }}
        const html = document.body.innerHTML;
        const m = html.match(/\{\{[^}]+\}\}/g);
        r.uncompiled = m ? m.length : 0;
        // 空白页：app 无实质内容
        const app = document.querySelector('#app');
        const txt = app ? app.textContent.replace(/\s/g, '') : '';
        const rows = document.querySelectorAll('.el-table__row').length;
        const hasDialog = document.querySelector('.el-dialog__wrapper:not([style*="display: none"])');
        r.blank = (!hasDialog && rows === 0 && txt.length < 8) ? 1 : 0;
        // loading 卡死
        const mask = document.querySelector('.el-loading-mask');
        if (mask && mask.style.display !== 'none' && mask.offsetParent !== null) r.stuckLoading = 1;
        // fixed 元素越出视口
        document.querySelectorAll('*').forEach(el => {
          const st = getComputedStyle(el);
          if (st.position !== 'fixed') return;
          const b = el.getBoundingClientRect();
          if (b.width === 0 || b.height === 0) return;
          if (b.left < -2 || b.top < -2 || b.right > window.innerWidth + 2 || b.bottom > window.innerHeight + 2) {
            r.fixedOutOfView.push(((el.className || '') + '').toString().slice(0, 50));
          }
        });
        return r;
      }).catch(e => ({ evalError: String(e.message || e).slice(0, 120) }));

      const landedLogin = page.url().indexOf('login.html') >= 0;
      const shotPath = vp.label + '_' + f.replace(/[\\/]/g, '_').replace(/\.html?$/i, '') + '.png';
      await page.screenshot({ path: path.join(OUT_DIR, shotPath) }).catch(() => {});

      page.off('console', onConsole);
      page.off('pageerror', onPageErr);
      page.off('response', onResp);

      const paramPage = needsParam(f);
      results.push({
        page: f, vp: vp.label, paramPage, landedLogin, gotoFail,
        consoleErrors: cerr, pageErrors: perr,
        badHttp: paramPage ? badHttp.filter(x => { const s = parseInt(x, 10); return s !== 404 && s !== 400; }) : badHttp,
        checks,
        shot: shotPath
      });
      const bad = cerr.length + perr.length + (checks.hOverflow > 4 ? 1 : 0) + (checks.blank) + (checks.uncompiled) + gotoFail;
      console.log('[' + vp.label + '] ' + f + (bad || badHttp.length ? '  ⚠ ' + JSON.stringify({
        cerr: cerr.length, perr: perr.length, http: badHttp.length,
        ovf: checks.hOverflow, clip: checks.textClip.length, img: checks.brokenImg.length,
        vue: checks.uncompiled, blank: checks.blank, stuck: checks.stuckLoading
      }) : '  ok'));
    }
    await ctx.close();
  }

  fs.writeFileSync(REPORT, JSON.stringify(results, null, 1));
  const issues = results.filter(r =>
    r.gotoFail || r.landedLogin || r.consoleErrors.length || r.pageErrors.length ||
    r.badHttp.length || r.checks.blank || r.checks.uncompiled ||
    (r.checks.hOverflow > 4) || r.checks.textClip.length ||
    r.checks.brokenImg.length || r.checks.stuckLoading || r.checks.fixedOutOfView.length);
  console.log('\n==== 汇总：' + results.length + ' 页次，' + issues.length + ' 页次有问题 ====');
  console.log('JSON: ' + REPORT);
  console.log('截图: ' + OUT_DIR);
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
