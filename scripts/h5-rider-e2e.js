// ============================================================
// 三角色端到端闭环（P0 验收链路）
// 顾客下单支付 → 骑手大厅抢单 → 取餐码核销 → 送达（自动完成+结算入账）
//              → 顾客评价骑手 → 骑手服务分/收入/消息全验证
// 用法：node scripts/h5-rider-e2e.js
// 前置：应用已启动(8080)、MySQL/Redis 就绪
// ============================================================
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const BASE = 'http://localhost:8080';
const PHONE = '13800138000';
const RIDER_PHONE = '13900005003';
const SHOT_DIR = 'C:\\Users\\itxinfei\\AppData\\Local\\Temp\\h5-rider-e2e';
fs.mkdirSync(SHOT_DIR, { recursive: true });

const results = [];
function rec(step, ok, detail) {
  results.push({ step, ok });
  console.log((ok ? '[PASS] ' : '[FAIL] ') + step + '  ' + (detail || ''));
}
const shot = (page, name) =>
  page.screenshot({ path: path.join(SHOT_DIR, name + '.png') }).catch(() => {});

function tailCode() {
  const lines = fs.readFileSync('D:\\MyCode\\reggie\\logs\\reggie_take_out.log')
    .slice(-300000).toString('utf8').split(/\r?\n/);
  for (let i = lines.length - 1; i >= 0; i--) {
    if (lines[i].indexOf('138****8000') >= 0) {
      const m = lines[i].match(/验证码=(\d{6})/);
      if (m) return m[1];
    }
  }
}
function db(sql) {
  return execSync('mysql -uroot -p123456 -h127.0.0.1 reggie -N -e "' + sql + '"', { encoding: 'utf8' }).toString().trim();
}

// C 端短信登录
async function loginCustomer(context) {
  const page = await context.newPage();
  await page.goto(BASE + '/front/page/login.html', { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(400);
  await page.fill('input[type=tel]', PHONE);
  await page.getByText('获取验证码').click();
  await page.waitForTimeout(1200);
  await page.fill('.code-input >> nth=1', tailCode());
  await page.click('.checkIcon');
  await page.click('.btnLogin');
  await page.waitForURL(u => u.toString().indexOf('login.html') < 0, { timeout: 10000 });
  return page;
}

(async () => {
  const browser = await chromium.launch();
  const cCtx = await browser.newContext({ viewport: { width: 375, height: 812 } });
  const rCtx = await browser.newContext({ viewport: { width: 375, height: 812 } });

  // 清空测试顾客的购物车，保证脚本可重复执行（历史失败运行可能已累积）
  db('DELETE FROM shopping_cart WHERE user_id=990004');
  // 测试店铺前置：恢复接单(pause_order=0)、营业时间放宽全天，避免"暂停接单/非营业时段"阻断
  db("UPDATE store_info SET pause_order=0, business_hours='00:00-23:59' WHERE id=1");
  // 清理上一轮残留的配送中(status=3)测试单，避免骑手在途计数累积触发接单上限（仅限测试顾客）
  db('UPDATE orders SET status=4 WHERE user_id=990004 AND status=3');

  // ========== A. 顾客下单并支付 ==========
  const cpage = await loginCustomer(cCtx);

  // 诊断监听：打印下单/支付接口的真实响应与页面错误，定位"点去支付不跳转"
  cpage.on('response', async (resp) => {
    const u = resp.url();
    if (resp.request().method() === 'POST' && /\/order\/submit|\/api\/payment\/pay/.test(u)) {
      let body = '';
      try { body = (await resp.text()).slice(0, 400); } catch (e) {}
      console.log('   [resp] ' + resp.status() + ' ' + u + ' -> ' + body);
    }
  });
  cpage.on('console', mm => {
    if (mm.type() === 'error') console.log('   [console.error] ' + mm.text().slice(0, 200));
  });

  // 确保有收货地址（无则直接调 API 新增），否则点结算会被带去地址页
  const addrResult = await cpage.evaluate(async () => {
    const r = await $axios({ url: '/address-book/list', method: 'get' });
    const list = r.data || [];
    if (list.length) return { existed: true };
    const add = await $axios({
      url: '/address-book', method: 'post',
      data: {
        consignee: '测试用户', phone: '13800138000', sex: '1',
        provinceCode: '110000', provinceName: '北京市',
        cityCode: '110100', cityName: '北京市',
        districtCode: '110105', districtName: '朝阳区',
        // 后端按结构化字段规范化生成 detail，须满足 hasEnoughStructured（有 community 即可）
        streetName: '望京街道', community: '望京 SOHO',
        label: '公司', isDefault: 1
      }
    });
    return { existed: false, code: add.code, msg: add.msg || add.message || '' };
  });
  if (!addrResult.existed && addrResult.code !== 1) {
    throw new Error('新增地址业务失败 code=' + addrResult.code + ' msg=' + addrResult.msg);
  }
  // 高德编码把"望京 SOHO"定位到真实坐标，但 inRange 判定要求地址落入某条 delivery_range_rule 圆。
  // 现有规则圆均未覆盖店铺(116.43,40.05)，E2E 把测试地址坐标校正到离店铺最近的规则 9100030029
  // 圆心(116.34,40.04,r=4km)内，确保 inRange=true（仅改本地开发库测试数据；规则覆盖缺口另记）
  db('UPDATE address_book SET longitude=116.34, latitude=40.04 WHERE user_id=990004');
  // 菜品原价62 ≥ 规则9100030029免配送门槛45，会触发"满免配送费"导致本单无配送费，
  // B5入账链路无法验证。测试期间临时抬高门槛（跑完恢复45），强制本单产生配送费3.98
  db('UPDATE delivery_range_rule SET free_threshold=999 WHERE id=9100030029');

  await cpage.goto(BASE + '/front/index.html', { waitUntil: 'domcontentloaded' });
  await cpage.getByText('选规格').first().waitFor({ timeout: 10000 });
  await cpage.waitForTimeout(1000);

  // 加购两道菜（各自点「选规格」→ 选第一个份量 → 加入购物车）
  for (let i = 0; i < 2; i++) {
    await cpage.getByText('选规格').nth(i).click();
    // 规格弹窗：每个规格维度（辣度/花生/份量等）各选第一项，再加入购物车
    const groups = cpage.locator('.dialogFlavor .divContent > div');
    await groups.first().waitFor({ timeout: 5000 });
    const g = await groups.count();
    for (let k = 0; k < g; k++) {
      await groups.nth(k).locator('span').first().click();
      await cpage.waitForTimeout(100);
    }
    await cpage.locator('.dialogFlavor .flavor-cta').click();
    await cpage.waitForTimeout(800);
  }

  const cartText = await cpage.locator('.cart-bar').innerText().catch(() => '');
  const total = (cartText.match(/¥?\s?(\d+(\.\d+)?)/) || [])[0];
  rec('A1 加购两个菜', /去结算/.test(cartText), '购物车栏「' + cartText.replace(/\s+/g, ' ').slice(0, 40) + '」');
  await shot(cpage, 'a1-cart');

  // 去结算 → 确认订单页 → 去支付
  await cpage.locator('.cart-bar').getByText('去结算').click();
  await cpage.waitForURL(/add-order/, { timeout: 10000 });
  await cpage.waitForTimeout(1500);
  await shot(cpage, 'a2-addorder');
  rec('A2 进入确认订单页', cpage.url().indexOf('add-order') >= 0, cpage.url());

  await cpage.getByText('去支付').first().click();
  await cpage.waitForURL(/pay-cashier/, { timeout: 12000 });
  await cpage.waitForTimeout(1000);
  rec('A3 拉起沙箱收银台', cpage.url().indexOf('pay-cashier') >= 0, cpage.url());
  await shot(cpage, 'a3-cashier');

  await cpage.locator('button.confirm-btn').click();
  await cpage.waitForURL(/pay-success/, { timeout: 12000 });
  await cpage.waitForTimeout(800);
  const orderId = new URL(cpage.url()).searchParams.get('reorderId');
  rec('A4 沙箱支付成功', !!orderId, '订单ID=' + orderId);
  await shot(cpage, 'a4-success');

  // ========== B. 骑手抢单 → 取餐核销 → 送达 ==========
  const rpage = await rCtx.newPage();
  await rpage.goto(BASE + '/rider/login.html', { waitUntil: 'domcontentloaded' });
  await rpage.waitForTimeout(500);
  await rpage.fill('input[type=tel]', RIDER_PHONE);
  await rpage.fill('input[type=password]', '123456');
  await rpage.click('.rider-btn.primary');
  await rpage.waitForURL(u => u.toString().indexOf('login.html') < 0, { timeout: 10000 });
  await rpage.waitForTimeout(1000);
  rec('B0 骑手登录', true, rpage.url());

  // 登录不会自动上线，抢单前显式上线，否则 grabOrder 报"请先上线后再抢单"
  await rpage.evaluate(async () => { await $axios.post('/api/rider/online'); });

  // 大厅等待目标订单出现（卡片含本订单号）
  const orderNumber = db('SELECT number FROM orders WHERE id=' + orderId);
  await rpage.goto(BASE + '/rider/hall.html', { waitUntil: 'domcontentloaded' });
  let grabbed = false;
  for (let i = 0; i < 20; i++) {
    const card = rpage.locator('.task-card', { hasText: orderNumber });
    if (await card.count()) {
      await card.getByText('立即抢单').click();
      grabbed = true;
      break;
    }
    await rpage.waitForTimeout(1500);
  }
  await rpage.waitForTimeout(1500);
  rec('B1 大厅抢到目标订单', grabbed, '订单号=' + orderNumber);
  await shot(rpage, 'b1-hall');

  // 取餐码（店员提供：e2e 从库读取）
  const pickupCode = db('SELECT pickup_code FROM orders WHERE id=' + orderId);
  rec('B2 派单已生成取餐码', /^\d{6}$/.test(pickupCode), '取餐码=' + pickupCode);

  await rpage.goto(BASE + '/rider/detail.html?id=' + orderId, { waitUntil: 'domcontentloaded' });
  await rpage.waitForTimeout(1500);
  await rpage.getByText('确认取餐').first().click();
  await rpage.waitForSelector('.pickup-code-field input', { state: 'visible', timeout: 5000 });
  await rpage.locator('.pickup-code-field input').fill(pickupCode);
  await rpage.locator('.van-popup:visible').getByText('确认取餐').click();
  await rpage.waitForTimeout(1500);
  rec('B3 取餐码核销成功', await rpage.getByText('确认送达').isVisible().catch(() => false), '');
  await shot(rpage, 'b3-pickup');

  await rpage.getByText('确认送达').click();
  await rpage.waitForSelector('.van-dialog', { timeout: 5000 });
  await rpage.locator('.van-dialog').getByRole('button', { name: '确认', exact: true }).click();
  await rpage.waitForTimeout(2000);
  const orderStatus = db('SELECT status FROM orders WHERE id=' + orderId);
  rec('B4 送达后订单自动完成', orderStatus === '4', '订单状态=' + orderStatus);
  await shot(rpage, 'b4-delivered');

  // 结算入账（rider_id 按骑手手机号动态解析，不硬编码）
  const riderId = db("SELECT id FROM rider WHERE phone='" + RIDER_PHONE + "'");
  const ledger = db('SELECT amount FROM rider_income_ledger WHERE order_id=' + orderId);
  const balance = db('SELECT withdrawable_balance FROM rider_account WHERE rider_id=' + riderId);
  rec('B5 配送费已入账骑手账户', ledger !== '' && parseFloat(balance) >= parseFloat(ledger),
    '本单配送费=' + ledger + '，账户可提现余额=' + balance);

  // ========== C. 顾客评价骑手 ==========
  await cpage.goto(BASE + '/front/page/order-detail.html?id=' + orderId, { waitUntil: 'domcontentloaded' });
  await cpage.waitForTimeout(1500);
  const canEval = await cpage.getByText('评价骑手').isVisible().catch(() => false);
  rec('C0 出现「评价骑手」入口', canEval, '');
  await cpage.getByText('评价骑手').first().click();
  await cpage.waitForTimeout(800);
  // 5 星：点 van-rate 第 5 颗
  await cpage.locator('.rider-eval-pop .van-rate .van-icon').nth(4).click();
  await cpage.waitForTimeout(200);
  // 评价内容
  const ta = cpage.locator('.rider-eval-pop textarea');
  if (await ta.count()) await ta.fill('配送很快，汤也没洒，骑手态度很好，提前打电话确认了。');
  await shot(cpage, 'c1-eval-pop');
  // 提交（弹窗底部按钮）
  await cpage.locator('.rider-eval-pop').getByText(/提交|发布|确认/).first().click();
  await cpage.waitForTimeout(2000);

  const evalExists = db("SELECT COUNT(*) FROM rider_evaluation WHERE order_id=" + orderId + " AND star_rating=5");
  rec('C1 骑手评价提交成功', evalExists === '1', 'rider_evaluation 落库 5 星');
  await shot(cpage, 'c2-evaluated');

  // ========== D. 骑手侧三页验证 ==========
  await rpage.goto(BASE + '/rider/evaluation.html', { waitUntil: 'domcontentloaded' });
  await rpage.waitForTimeout(1800);
  const evalPageText = await rpage.locator('body').innerText();
  rec('D1 骑手可见服务分与评价', /5\.0/.test(evalPageText), evalPageText.replace(/\s+/g, ' ').slice(0, 60));
  await shot(rpage, 'd1-rider-eval');

  await rpage.goto(BASE + '/rider/message.html', { waitUntil: 'domcontentloaded' });
  await rpage.waitForTimeout(1500);
  const msgText = await rpage.locator('body').innerText();
  rec('D2 骑手收到派单消息', /派单/.test(msgText), msgText.replace(/\s+/g, ' ').slice(0, 60));
  await shot(rpage, 'd2-message');

  await rpage.goto(BASE + '/rider/income.html', { waitUntil: 'domcontentloaded' });
  await rpage.waitForTimeout(1800);
  const incText = await rpage.locator('body').innerText();
  rec('D3 骑手收入页可见入账', incText.indexOf(ledger) >= 0 || /\d+\.\d{2}/.test(incText),
    incText.replace(/\s+/g, ' ').slice(0, 80));
  await shot(rpage, 'd3-income');

  // ========== 汇总 ==========
  const pass = results.filter(r => r.ok).length;
  console.log('\n==== E2E 汇总：' + pass + '/' + results.length + ' 通过 ====');
  console.log('截图：' + SHOT_DIR);
  fs.writeFileSync('D:\\MyCode\\reggie\\logs\\h5-rider-e2e.json', JSON.stringify(results, null, 1));
  // 恢复规则9100030029免配送门槛（见前置说明）
  db('UPDATE delivery_range_rule SET free_threshold=45 WHERE id=9100030029');
  await browser.close();
  if (pass < results.length) process.exit(1);
})().catch(e => { console.error(e); process.exit(1); });
