// 后台 delivery 审核三页快速验证：rider-withdrawal / rider-evaluation / rider-exception
// 用法：node scripts/ui-audit-delivery-admin.js
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');
const path = require('path');
const OUT_DIR = 'C:\\Users\\itxinfei\\AppData\\Local\\Temp\\ui-audit\\backend';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errs = [];
  page.on('response', r => {
    if (r.status() >= 400) errs.push(r.status() + ' ' + r.url().replace('http://localhost:8080', '').slice(0, 100));
  });

  await page.goto('http://localhost:8080/backend/page/login/login.html');
  await page.waitForTimeout(500);
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[type=password]', '123456');
  await page.click('button[aria-label="登录"]');
  await page.waitForTimeout(1500);

  for (const f of ['rider-withdrawal', 'rider-evaluation', 'rider-exception']) {
    errs.length = 0;
    await page.goto('http://localhost:8080/backend/page/delivery/' + f + '.html');
    await page.waitForTimeout(1800);
    await page.screenshot({ path: path.join(OUT_DIR, 'admin_' + f + '.png') });
    console.log('[' + f + '] ' + (errs.length ? '⚠ ' + JSON.stringify(errs) : 'ok'));
  }
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
