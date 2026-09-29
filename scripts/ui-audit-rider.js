// 骑手端页面巡检：演示账号登录 → 打开 index/hall/income/detail → 缺陷检测 → 截图
// 用法：node scripts/ui-audit-rider.js
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');
const fs = require('fs');
const path = require('path');

const BASE = 'http://localhost:8080';
const OUT_DIR = 'C:\\Users\\itxinfei\\AppData\\Local\\Temp\\ui-audit\\rider';
const REPORT = 'D:\\MyCode\\reggie\\logs\\ui-audit-rider.json';
fs.mkdirSync(OUT_DIR, { recursive: true });

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 375, height: 812 } });
  const page = await ctx.newPage();

  const cerr = [];
  const perr = [];
  const badHttp = [];
  page.on('console', m => { if (m.type() === 'error') cerr.push(m.text().slice(0, 200)); });
  page.on('pageerror', e => perr.push(String(e.message || e).slice(0, 200)));
  page.on('response', r => {
    try {
      const s = r.status();
      if (s >= 400 && r.url().indexOf(BASE) >= 0) badHttp.push(s + ' ' + r.url().replace(BASE, '').slice(0, 120));
    } catch (e) {}
  });

  // ---- 登录 ----
  await page.goto(BASE + '/rider/login.html', { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(600);
  await page.fill('input[type=tel]', '13900005003');
  await page.fill('input[type=password]', '123456');
  await page.click('.rider-btn.primary');
  await page.waitForURL(u => u.toString().indexOf('login.html') < 0, { timeout: 10000 });
  await page.waitForTimeout(1000);
  console.log('登录成功 → ' + page.url());

  // 从大厅/首页找一个真实订单详情链接
  await page.goto(BASE + '/rider/hall.html', { waitUntil: 'domcontentloaded' }).catch(() => {});
  await page.waitForTimeout(1500);
  let detailHref = await page.$$eval('a[href*="detail"]', as => as.map(a => a.getAttribute('href')).filter(Boolean)[0] || '').catch(() => '');
  if (!detailHref) {
    await page.goto(BASE + '/rider/index.html', { waitUntil: 'domcontentloaded' }).catch(() => {});
    await page.waitForTimeout(1200);
    detailHref = await page.$$eval('a[href*="detail"]', as => as.map(a => a.getAttribute('href')).filter(Boolean)[0] || '').catch(() => '');
  }

  const PAGES = [
    ['index', '/rider/index.html'],
    ['hall', '/rider/hall.html'],
    ['income', '/rider/income.html'],
    ['evaluation', '/rider/evaluation.html'],
    ['exception', '/rider/exception.html'],
    ['message', '/rider/message.html'],
    ['detail', detailHref ? ('/rider/' + detailHref.replace(/^\.?\//, '').replace(/^rider\//, '')) : null]
  ];

  const results = [];
  for (const [name, rel] of PAGES) {
    if (!rel) { results.push({ page: name, skipped: '无真实订单链接' }); console.log('[skip] ' + name + ' 无订单链接'); continue; }
    cerr.length = 0; perr.length = 0; badHttp.length = 0;
    let gotoFail = null;
    await page.goto(BASE + rel, { waitUntil: 'domcontentloaded', timeout: 15000 })
      .catch(e => { gotoFail = String(e.message || e).slice(0, 150); });
    await page.waitForTimeout(1800);

    const checks = await page.evaluate(() => {
      const r = { hOverflow: 0, textClip: [], brokenImg: [], uncompiled: 0, blank: 0, fixedOutOfView: [] };
      r.hOverflow = Math.max(0, document.documentElement.scrollWidth - window.innerWidth);
      document.querySelectorAll('body *:not(script):not(style)').forEach(el => {
        if (el.children.length === 0) {
          const t = (el.textContent || '').trim();
          if (t.length >= 2 && el.clientWidth > 20 && el.scrollWidth > el.clientWidth + 6) {
            if (r.textClip.length < 5) r.textClip.push(((el.className || '') + '').toString().slice(0, 50) + ' | ' + t.slice(0, 16));
          }
        }
      });
      document.querySelectorAll('img').forEach(img => {
        if (img.offsetWidth > 0 && img.complete && img.naturalWidth === 0) r.brokenImg.push((img.src || '').slice(-60));
      });
      const m = document.body.innerHTML.match(/\{\{[^}]+\}\}/g);
      r.uncompiled = m ? m.length : 0;
      const txt = document.body.textContent.replace(/\s/g, '');
      r.blank = txt.length < 8 ? 1 : 0;
      document.querySelectorAll('*').forEach(el => {
        if (getComputedStyle(el).position !== 'fixed') return;
        const b = el.getBoundingClientRect();
        if (b.width && b.height && (b.left < -2 || b.top < -2 || b.right > window.innerWidth + 2 || b.bottom > window.innerHeight + 2)) {
          r.fixedOutOfView.push(((el.className || '') + '').toString().slice(0, 50));
        }
      });
      return r;
    }).catch(e => ({ evalError: String(e.message || e).slice(0, 120) }));

    const shotPath = name + '.png';
    await page.screenshot({ path: path.join(OUT_DIR, shotPath) }).catch(() => {});
    results.push({ page: name, url: rel, gotoFail, consoleErrors: cerr.slice(), pageErrors: perr.slice(), badHttp: badHttp.slice(), checks, shot: shotPath });
    console.log('[' + name + ']' + (gotoFail || cerr.length || perr.length || badHttp.length || checks.blank || checks.hOverflow ? '  ⚠ ' + JSON.stringify({
      cerr: cerr.length, perr: perr.length, http: badHttp.length, ovf: checks.hOverflow,
      clip: checks.textClip.length, img: checks.brokenImg.length, vue: checks.uncompiled, blank: checks.blank
    }) : '  ok'));
  }

  fs.writeFileSync(REPORT, JSON.stringify(results, null, 1));
  console.log('\nJSON: ' + REPORT + '\n截图: ' + OUT_DIR);
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
