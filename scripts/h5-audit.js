// C 端全面体检：登录态逐页检测 错误/坏响应/失败请求/横向溢出/元素越界/裂图/navbar重叠/长任务
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');
const fs = require('fs');
const LOG = 'D:\\MyCode\\reggie\\logs\\reggie_take_out.log';
const PHONE = '13800138000';
const SHOT = 'C:\\Users\\itxinfei\\AppData\\Local\\Temp\\h5-audit-shots';
if (!fs.existsSync(SHOT)) fs.mkdirSync(SHOT);
function tailCode() {
  const lines = fs.readFileSync(LOG).slice(-300000).toString('utf8').split(/\r?\n/);
  for (let i = lines.length - 1; i >= 0; i--) {
    if (lines[i].indexOf('138****8000') >= 0) {
      const m = lines[i].match(/验证码=(\d{6})/); if (m) return m[1];
    }
  }
}
const BASE = 'http://localhost:8080';
const PAGES = [
  ['index', '/front/index.html'],
  ['order', '/front/page/order.html'],
  ['user', '/front/page/user.html'],
  ['address', '/front/page/address.html'],
  ['address-edit', '/front/page/address-edit.html'],
  ['add-order', '/front/page/add-order.html'],
  ['member-center', '/front/page/member-center.html'],
  ['my-invoice', '/front/page/my-invoice.html'],
  ['my-evaluations', '/front/page/my-evaluations.html'],
  ['my-favorites', '/front/page/my-favorites.html'],
  ['frequent-orders', '/front/page/frequent-orders.html'],
  ['message', '/front/page/message.html'],
  ['customer-service', '/front/page/customer-service.html'],
  ['ai-assistant', '/front/page/ai-assistant.html'],
  ['qrcode-order', '/front/page/qrcode-order.html'],
  ['pay-cashier', '/front/page/pay-cashier.html'],
  ['pay-success', '/front/page/pay-success.html'],
  ['no-wifi', '/front/page/no-wifi.html'],
];
const intersect = (a,b) => !(a.right<=b.left||b.right<=a.left||a.bottom<=b.top||b.bottom<=a.top);

(async () => {
  const browser = await chromium.launch();
  const report = [];
  for (const [vpLabel,vp] of [['mobile',{width:375,height:812}],['desktop',{width:1280,height:800}]]) {
    const ctx = await browser.newContext({ viewport: vp });
    const page = await ctx.newPage();
    await page.addInitScript(() => {
      window.__lt = [];
      try { new PerformanceObserver(l => l.getEntries().forEach(e => window.__lt.push(Math.round(e.duration)))).observe({entryTypes:['longtask']}); } catch(e){}
    });
    const cerr=[], perr=[], badHttp=new Set(), freq=[];
    page.on('console', m => { if (m.type()==='error') cerr.push(m.text().slice(0,180)); });
    page.on('pageerror', e => perr.push(e.message.slice(0,180)));
    page.on('response', r => { if (r.status()>=400) badHttp.add(r.status()+' '+r.url().replace(BASE,'').slice(0,90)); });
    page.on('requestfailed', r => freq.push(r.url().replace(BASE,'').slice(0,80)));

    await page.goto(BASE+'/front/page/login.html',{waitUntil:'networkidle'});
    await page.fill('input[type=tel]',PHONE);
    await page.getByText('获取验证码').click();
    await page.waitForTimeout(1000);
    await page.fill('.code-input >> nth=1',tailCode());
    await page.click('.checkIcon'); await page.click('.btnLogin');
    await page.waitForURL(u=>u.toString().indexOf('login.html')<0);

    for (const [name,path] of PAGES) {
      cerr.length=0; perr.length=0; badHttp.clear(); freq.length=0;
      let navTimeout=false;
      try { await page.goto(BASE+path,{waitUntil:'load',timeout:12000}); }
      catch(e){ navTimeout=true; }
      await page.waitForTimeout(1200);
      const issues = await page.evaluate(vp => {
        const out = {};
        // 横向溢出
        if (document.documentElement.scrollWidth > vp.width + 2)
          out.hOverflow = document.documentElement.scrollWidth + '>' + vp.width;
        // fixed/absolute 元素横向越界（排除 tabbar/全屏弹层）
        const oob = [];
        document.querySelectorAll('body *').forEach(el => {
          const cs = getComputedStyle(el);
          if (cs.position!=='fixed' && cs.position!=='absolute') return;
          const r = el.getBoundingClientRect();
          if (r.width===0) return;
          if (r.left < -2 || r.right > vp.width + 2) {
            const cls = (el.className && el.className.toString)?el.className.toString().slice(0,40):el.tagName;
            oob.push(cls+'['+Math.round(r.left)+','+Math.round(r.right)+']');
          }
        });
        if (oob.length) out.outOfBounds = [...new Set(oob)].slice(0,6);
        // 裂图
        const broken = [];
        document.querySelectorAll('img').forEach(im => {
          if (im.src && im.complete && im.naturalWidth===0) broken.push((im.alt||im.src).slice(0,30));
        });
        if (broken.length) out.brokenImgs = [...new Set(broken)].slice(0,6);
        // navbar 重叠
        const nodes = Array.from(document.querySelectorAll('.cend-navbar *')).map(n=>{
          const r=n.getBoundingClientRect();
          return {cls:(n.className||'').toString(),left:r.left,right:r.right,top:r.top,bottom:r.bottom,w:r.width,h:r.height};
        }).filter(e=>e.w>0&&e.h>0);
        const players = nodes.filter(e=>e.cls.indexOf('cend-navbar__btn')>=0||e.cls.indexOf('cend-navbar__title')>=0);
        const ov=[];
        for(let i=0;i<players.length;i++)for(let j=i+1;j<players.length;j++){
          const a=players[i],b=players[j];
          const acb=a.left<=b.left&&a.right>=b.right&&a.top<=b.top&&a.bottom>=b.bottom;
          const bca=b.left<=a.left&&b.right>=a.right&&b.top<=a.top&&b.bottom>=a.bottom;
          if(acb||bca)continue;
          if(intersect(a,b))ov.push((a.cls||'').slice(0,30)+' x '+(b.cls||'').slice(0,30));
        }
        if(ov.length) out.navbarOverlap=[...new Set(ov)];
        // 长任务
        if (window.__lt && window.__lt.length) out.longtasks = window.__lt;
        return out;
      }, vp).catch(()=>({}));

      const all = {
        page:name, vp:vpLabel,
        redirectedToLogin: page.url().indexOf('login.html')>=0,
        navTimeout,
        consoleErrors: [...new Set(cerr)].slice(0,4),
        pageErrors: [...new Set(perr)].slice(0,4),
        badHttp: [...badHttp].slice(0,6),
        failedReq: [...new Set(freq)].slice(0,6),
        ...issues
      };
      const hasIssue = Object.keys(all).some(k=>!['page','vp'].includes(k)&&all[k]&&(!Array.isArray(all[k])||all[k].length));
      if (hasIssue) {
        report.push(all);
        await page.screenshot({path: SHOT+'\\'+vpLabel+'-'+name+'.png'}).catch(()=>{});
      }
    }
    await ctx.close();
  }
  console.log(JSON.stringify(report,null,2));
  await browser.close();
})();
