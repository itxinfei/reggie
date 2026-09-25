// H5 真人体验巡逻：登录 → 逐页打开 → 检测 navbar 元素几何重叠 → 截图取证
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');
const fs = require('fs');

const LOG = 'D:\\MyCode\\reggie\\logs\\reggie_take_out.log';
const PHONE = '13800138000';
const SHOT_DIR = 'C:\\Users\\itxinfei\\AppData\\Local\\Temp\\h5-shots';
if (!fs.existsSync(SHOT_DIR)) fs.mkdirSync(SHOT_DIR);

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

// 可直接打开的 C 端页面
const PAGES = [
  ['index', 'http://localhost:8080/front/index.html'],
  ['order', 'http://localhost:8080/front/page/order.html'],
  ['user', 'http://localhost:8080/front/page/user.html'],
  ['address', 'http://localhost:8080/front/page/address.html'],
  ['address-edit', 'http://localhost:8080/front/page/address-edit.html'],
  ['member-center', 'http://localhost:8080/front/page/member-center.html'],
  ['my-invoice', 'http://localhost:8080/front/page/my-invoice.html'],
  ['my-evaluations', 'http://localhost:8080/front/page/my-evaluations.html'],
  ['my-favorites', 'http://localhost:8080/front/page/my-favorites.html'],
  ['frequent-orders', 'http://localhost:8080/front/page/frequent-orders.html'],
  ['message', 'http://localhost:8080/front/page/message.html'],
  ['customer-service', 'http://localhost:8080/front/page/customer-service.html'],
  ['ai-assistant', 'http://localhost:8080/front/page/ai-assistant.html'],
];

function intersect(a, b) {
  return !(a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top);
}

(async () => {
  const browser = await chromium.launch();
  const report = [];

  for (const vpName of [['mobile', { width: 375, height: 812 }], ['desktop', { width: 1280, height: 800 }]]) {
    const [vpLabel, vp] = vpName;
    const ctx = await browser.newContext({ viewport: vp });
    const page = await ctx.newPage();

    // 真人式 UI 登录
    await page.goto('http://localhost:8080/front/page/login.html', { waitUntil: 'networkidle' });
    await page.fill('input[type=tel]', PHONE);
    await page.getByText('获取验证码').click();
    await page.waitForTimeout(1000);
    const code = tailCode();
    await page.fill('.code-input >> nth=1', code);
    await page.click('.checkIcon');
    await page.click('.btnLogin');
    await page.waitForURL(u => u.toString().indexOf('login.html') < 0, { timeout: 8000 });
    await page.waitForTimeout(500);

    for (const [name, url] of PAGES) {
      await page.goto(url, { waitUntil: 'networkidle' }).catch(() => {});
      await page.waitForTimeout(700);
      const landedLogin = page.url().indexOf('login.html') >= 0;

      // 收集导航栏内所有可见元素的盒子
      const elems = await page.$$eval('.cend-navbar *', nodes => {
        return nodes.map(n => {
          const r = n.getBoundingClientRect();
          return {
            cls: n.className && n.className.toString ? n.className.toString().slice(0, 60) : '',
            tag: n.tagName.toLowerCase(),
            text: (n.textContent || '').trim().slice(0, 12),
            left: Math.round(r.left), right: Math.round(r.right),
            top: Math.round(r.top), bottom: Math.round(r.bottom),
            w: Math.round(r.width), h: Math.round(r.height)
          };
        }).filter(e => e.w > 0 && e.h > 0);
      }).catch(() => []);

      // 只比较"叶子级"交互/文字元素之间的重叠（按钮 vs 标题）
      const players = elems.filter(e =>
        e.cls.indexOf('cend-navbar__btn') >= 0 ||
        e.cls.indexOf('cend-navbar__title') >= 0 ||
        e.tag === 'i');
      const overlaps = [];
      for (let i = 0; i < players.length; i++) {
        for (let j = i + 1; j < players.length; j++) {
          // i 标签嵌套在按钮内时跳过（按 class 归属已独立收集，用包含关系判断）
          const a = players[i], b = players[j];
          const aContainsB = a.left <= b.left && a.right >= b.right && a.top <= b.top && a.bottom >= b.bottom;
          const bContainsA = b.left <= a.left && b.right >= a.right && b.top <= b.top && b.bottom >= a.bottom;
          if (aContainsB || bContainsA) continue;
          if (intersect(a, b)) {
            overlaps.push([a.cls || a.tag + ':' + a.text, b.cls || b.tag + ':' + b.text,
              { a: [a.left, a.top, a.right, a.bottom], b: [b.left, b.top, b.right, b.bottom] }]);
          }
        }
      }

      if (overlaps.length > 0) {
        await page.screenshot({ path: SHOT_DIR + '\\' + vpLabel + '-' + name + '.png' });
      }
      report.push({ vp: vpLabel, page: name, landedLogin,
        navbarElems: players.length ? players.map(e => (e.cls || e.tag) + '[' + e.left + ',' + e.right + ']') : [],
        overlaps });
    }
    await ctx.close();
  }

  // 只输出有问题或需关注的条目
  const bad = report.filter(r => r.landedLogin || r.overlaps.length > 0);
  console.log(JSON.stringify(bad, null, 2));
  await browser.close();
})();
