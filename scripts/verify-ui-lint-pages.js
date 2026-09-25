/**
 * UI 规范修复页面冒烟：登录后台后逐页加载，
 * 校验无 JS 控制台错误、页面已挂载 Vue、改动后的按钮（btn-delete / type=text）存在。
 */
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');

const BASE = 'http://localhost:8080';

// 每页 Vue 挂载点 + 服务端 HTML 中应出现的改动标记
const PAGES = [
  { path: '/backend/page/ai/provider-config.html', mount: '#provider-app', expect: 'btn-delete' },
  { path: '/backend/page/dining/table-list.html', mount: '#table-app', expect: 'btn-delete' },
  { path: '/backend/page/food/spec-management.html', mount: '#main-content', expect: 'btn-delete' },
  { path: '/backend/page/inventory/purchase-list.html', mount: '#purchase-app', expect: 'btn-delete' },
  { path: '/backend/page/kds/board.html', mount: '#app', expect: 'type="text"' },
  { path: '/backend/page/member-center/coupon-list.html', mount: '#coupon-app', expect: 'btn-delete' },
  { path: '/backend/page/member-center/member-list.html', mount: '#member-app', expect: 'btn-delete' },
  { path: '/backend/page/recommend/campaigns.html', mount: '#app', expect: 'btn-delete' },
  { path: '/backend/page/order/pending-monitor.html', mount: '#app', expect: 'type="text"' },
  { path: '/backend/page/sys/role-list.html', mount: '#role-app', expect: 'type="text"' }
];

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });

  await page.goto(BASE + '/backend/page/login/login.html');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[type="password"]', '123456');
  await page.click('button[aria-label="登录"]');
  await page.waitForURL(/backend\/index\.html/, { timeout: 10000 });

  const results = [];
  for (const p of PAGES) {
    const errors = [];
    const onError = function (msg) { if (msg.type() === 'error') errors.push(msg.text()); };
    const onPageError = function (e) { errors.push('PAGEERROR: ' + e.message); };
    page.on('console', onError);
    page.on('pageerror', onPageError);

    await page.goto(BASE + p.path);
    // 等 Vue 挂载（数据行可能为空，按钮渲染依赖数据，故改动标记改从服务端 HTML 校验）
    await page.waitForSelector(p.mount, { timeout: 10000 });
    await page.waitForTimeout(1500);
    const mounted = await page.evaluate(function (sel) {
      var el = document.querySelector(sel);
      return !!(el && el.__vue__);
    }, p.mount);
    // 直接取服务端原始 HTML（绕开「无数据行则按钮不渲染」）
    const rawResp = await page.request.get(BASE + p.path + '?src=1');
    const rawHtml = await rawResp.text();
    const expectFound = rawHtml.indexOf(p.expect) >= 0;
    page.off('console', onError);
    page.off('pageerror', onPageError);

    results.push({ page: p.path, mounted: mounted, expectFound: expectFound, errors: errors });
  }

  const pass = results.every(function (r) { return r.mounted && r.expectFound && r.errors.length === 0; });
  console.log(JSON.stringify({ pass: pass, pages: results }, null, 2));
  await browser.close();
  process.exit(pass ? 0 : 1);
})().catch(function (e) {
  console.error('SMOKE_FAILED:', e);
  process.exit(2);
});
