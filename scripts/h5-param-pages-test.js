// H5 需参数页面 · 真人 e2e 补测
// 外卖：首页加购 → 确认订单 → 沙箱收银台 → 支付成功 → 订单详情 → 配送跟踪
// 堂食：扫码页两栏点餐 → 到店支付 → 下单成功
// 用法：node scripts/h5-param-pages-test.js
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');
const fs = require('fs');

const LOG = 'D:\\MyCode\\reggie\\logs\\reggie_take_out.log';
const PHONE = '13800138000';
const SHOT_DIR = 'C:\\Users\\itxinfei\\AppData\\Local\\Temp\\h5-shots';
if (!fs.existsSync(SHOT_DIR)) fs.mkdirSync(SHOT_DIR, { recursive: true });

function tailCode() {
  const tail = fs.readFileSync(LOG).slice(-300000).toString('utf8');
  const lines = tail.split(/\r?\n/);
  for (let i = lines.length - 1; i >= 0; i--) {
    if (lines[i].indexOf('138****8000') >= 0) {
      const m = lines[i].match(/验证码=(\d{6})/);
      if (m) return m[1];
    }
  }
  return null;
}

const results = [];
function rec(step, ok, detail) {
  results.push({ step, ok, detail: String(detail || '').slice(0, 160) });
  console.log((ok ? '[PASS] ' : '[FAIL] ') + step + '  ' + (detail || ''));
}
const shot = (page, name) =>
  page.screenshot({ path: SHOT_DIR + '\\' + name + '.png' }).catch(() => {});

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 375, height: 812 } });
  const page = await ctx.newPage();

  const errors = [];
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text().slice(0, 180)); });
  page.on('pageerror', e => errors.push('PAGEERROR ' + e.message.slice(0, 180)));
  let errMark = 0;
  const newErrors = () => errors.slice(errMark);
  const markErrors = () => { const ne = newErrors(); errMark = errors.length; return ne; };

  // ---------- 登录 ----------
  await page.goto('http://localhost:8080/front/page/login.html', { waitUntil: 'networkidle' });
  await page.fill('input[type=tel]', PHONE);
  await page.getByText('获取验证码').click();
  await page.waitForTimeout(1000);
  const code = tailCode();
  await page.fill('.code-input >> nth=1', code);
  await page.click('.checkIcon');
  await page.click('.btnLogin');
  await page.waitForURL(u => u.toString().indexOf('login.html') < 0, { timeout: 8000 });
  console.log('登录成功 -> ' + page.url());

  // ---------- 1. 首页加载 + 加购 ----------
  try {
    await page.goto('http://localhost:8080/front/index.html', { waitUntil: 'networkidle' });
    await page.waitForSelector('.ri-add-line', { timeout: 8000 });
    const kk = await page.locator('.kingkong').count();
    await page.locator('.ri-add-line').nth(0).click();
    await page.waitForTimeout(400);
    await page.locator('.ri-add-line').nth(1).click();
    await page.waitForTimeout(800);
    const canSettle = await page.locator('.cart-bar').innerText();
    rec('1 首页/金刚区/加购', kk > 0 && canSettle.indexOf('去结算') >= 0,
      '金刚区=' + kk + '；购物车栏「' + canSettle.replace(/\s+/g, ' ').slice(0, 40) + '」');
    await shot(page, 'p1-index');
  } catch (e) { rec('1 首页/金刚区/加购', false, e.message); }

  // ---------- 2. 去结算 → 确认订单页 ----------
  let orderId = null;
  try {
    await page.locator('.cart-bar').getByText('去结算').click();
    await page.waitForURL(/add-order/, { timeout: 8000 });
    await page.waitForTimeout(1500);
    const btn = page.getByText('去支付');
    const n = await btn.count();
    rec('2 进入确认订单页', page.url().indexOf('add-order') >= 0 && n > 0, page.url());
    await shot(page, 'p2-add-order');
  } catch (e) { rec('2 进入确认订单页', false, e.message); }

  // ---------- 3. 去支付 → 沙箱收银台 ----------
  try {
    await page.getByText('去支付').first().click();
    await page.waitForURL(/pay-cashier/, { timeout: 12000 });
    await page.waitForTimeout(1000);
    const cbtn = await page.locator('button.confirm-btn').count();
    rec('3 拉起沙箱收银台', page.url().indexOf('pay-cashier') >= 0 && cbtn > 0, page.url());
    await shot(page, 'p3-cashier');
  } catch (e) {
    rec('3 拉起沙箱收银台', false, e.message + ' | 当前URL=' + page.url());
    await shot(page, 'p3-cashier-fail');
  }

  // ---------- 4. 模拟付款 → 支付成功 ----------
  try {
    await page.locator('button.confirm-btn').click();
    await page.waitForURL(/pay-success/, { timeout: 12000 });
    await page.waitForSelector('.success-icon-svg', { timeout: 8000 });
    orderId = new URL(page.url()).searchParams.get('reorderId');
    rec('4 沙箱付款/成功页', !!orderId, '订单ID=' + orderId);
    await shot(page, 'p4-success');
  } catch (e) { rec('4 沙箱付款/成功页', false, e.message); }

  // ---------- 5. 订单详情 ----------
  try {
    await page.goto('http://localhost:8080/front/page/order-detail.html?id=' + orderId,
      { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    const body = await page.locator('body').innerText();
    const ok = page.url().indexOf('login') < 0 && body.indexOf('黄焖鸡') >= 0;
    rec('5 订单详情页', ok, '含菜品名/未跳登录');
    await shot(page, 'p5-detail');
  } catch (e) { rec('5 订单详情页', false, e.message); }

  // ---------- 6. 配送跟踪 ----------
  try {
    await page.goto('http://localhost:8080/front/page/tracking.html?orderId=' + orderId,
      { waitUntil: 'networkidle' });
    await page.waitForTimeout(1200);
    const body = await page.locator('body').innerText();
    const bad = /参数错误|订单不存在|系统异常/.test(body);
    rec('6 配送跟踪页', page.url().indexOf('login') < 0 && !bad,
      bad ? '页面报错' : '正常渲染 ' + body.replace(/\s+/g, ' ').slice(0, 40));
    await shot(page, 'p6-tracking');
  } catch (e) { rec('6 配送跟踪页', false, e.message); }

  // ---------- 7. 堂食扫码：两栏点餐 + 到店支付 ----------
  try {
    await page.goto('http://localhost:8080/front/page/qrcode-order.html?tableId=3001',
      { waitUntil: 'networkidle' });
    await page.waitForSelector('.category-rail', { timeout: 8000 });
    await page.waitForSelector('.dish-rows .step-plus', { timeout: 8000 });
    await page.locator('.dish-rows .step-plus').first().click();
    await page.waitForTimeout(300);
    await page.locator('.dish-rows .step-plus').first().click();
    await page.waitForTimeout(600);
    await page.locator('.cart-btn').click();
    await page.waitForSelector('.submit-btn', { timeout: 5000 });
    await page.locator('.pay-method').nth(2).click(); // 到店支付
    await page.locator('.submit-btn').click();
    await page.waitForTimeout(1500);
    const dlg = await page.getByText('下单成功').count();
    rec('7 堂食扫码/到店支付', dlg > 0, '两栏布局 + 加购 + 提交成功弹窗');
    await shot(page, 'p7-qrcode');
  } catch (e) { rec('7 堂食扫码/到店支付', false, e.message); }

  const ne = markErrors();
  console.log('n--- 汇总 ---');
  const pass = results.filter(r => r.ok).length;
  results.forEach(r => console.log((r.ok ? 'PASS ' : 'FAIL ') + r.step + '  ' + r.detail));
  console.log('n' + pass + '/' + results.length + ' 通过；新增 console error：' + ne.length);
  if (ne.length) console.log(ne.slice(0, 10).join('n'));
  console.log('截图目录：' + SHOT_DIR);
  await browser.close();
  process.exit(pass === results.length ? 0 : 1);
})();
