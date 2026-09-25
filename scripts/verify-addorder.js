// add-order 结算预览接入验证：登录 → 保证购物车有货 → 打开确认页 → 校验 /api/order/preview 与页面金额一致
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');
const fs = require('fs');

const LOG = 'D:\\MyCode\\reggie\\logs\\reggie_take_out.log';
const PHONE = '13800138000';
const SHOT = 'C:\\Users\\itxinfei\\AppData\\Local\\Temp\\addorder-preview.png';

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

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 } });
  const page = await ctx.newPage();

  let preview = null;
  page.on('response', async resp => {
    if (resp.url().indexOf('/api/order/preview') >= 0) {
      let body = null;
      try { body = await resp.json(); } catch (e) {}
      preview = { status: resp.status(), body };
    }
  });

  // 1) 真人登录
  await page.goto('http://localhost:8080/front/page/login.html', { waitUntil: 'networkidle' });
  await page.fill('input[type=tel]', PHONE);
  await page.getByText('获取验证码').click();
  await page.waitForTimeout(1000);
  const code = tailCode();
  await page.fill('.code-input >> nth=1', code);
  await page.click('.checkIcon');
  await page.click('.btnLogin');
  await page.waitForURL(u => u.toString().indexOf('login.html') < 0, { timeout: 8000 });

  // 2) 首页确保购物车至少一件商品
  await page.goto('http://localhost:8080/front/index.html', { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  const plus = page.locator('i.ri-add-line').first();
  if (await plus.count() > 0) {
    await plus.click().catch(() => {});
    await page.waitForTimeout(600);
  }

  // 3) 打开确认页
  await page.goto('http://localhost:8080/front/page/add-order.html', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);

  // 4) 校验
  const displayed = await page.locator('.num-main').first().textContent().catch(() => '');
  const p = preview && preview.body ? preview.body.data : null;
  const result = {
    previewHttpStatus: preview ? preview.status : null,
    previewCode: preview && preview.body ? preview.body.code : null,
    serverPayAmount: p ? p.payAmount : null,
    serverGoodsAmount: p ? p.goodsAmount : null,
    serverDeliveryFee: p ? p.deliveryFee : null,
    giftCount: p && p.gifts ? p.gifts.length : 0,
    detailCount: p && p.details ? p.details.length : 0,
    belowMinOrder: p ? p.belowMinOrder : null,
    rangeChecked: p ? p.rangeChecked : null,
    inRange: p ? p.inRange : null,
    displayedPay: displayed.trim()
  };
  result.amountMatch = Number(result.serverPayAmount) === Number(result.displayedPay);

  await page.screenshot({ path: SHOT, fullPage: true });
  result.screenshot = SHOT;
  console.log(JSON.stringify(result, null, 2));
  await browser.close();
})();
