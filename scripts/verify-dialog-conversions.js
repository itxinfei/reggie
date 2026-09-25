/**
 * 5 处弹窗 crud-dialog 转换的浏览器实测：
 * A. 桌台列表「二维码打印」iframe 弹窗（fullscreen 壳）
 * B. AI 助手「模型配置」iframe 弹窗（fullscreen 壳 + close 回调）
 * C. 二维码中心：勾选 2 桌 → 生成（ReggieUI 本机地址确认）→ 预览弹窗（xl 壳 + 自定义页脚）
 * D. 收银台「加菜」弹窗（lg 壳 + 自定义菜品网格；一键开台造数据，测完清理）
 */
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');

const BASE = 'http://localhost:8080';

function visibleDialog(page) {
  return page.$('.el-dialog__wrapper:not([style*="display: none"]) .unified-dialog');
}

async function inspectVisibleDialog(page) {
  const dlg = await visibleDialog(page);
  const box = await dlg.boundingBox();
  const viewport = page.viewportSize();
  const info = await dlg.evaluate(function (el) {
    return {
      classes: el.className,
      title: (el.querySelector('.el-dialog__title') || {}).textContent,
      iframeCount: el.querySelectorAll('iframe').length
    };
  });
  return Object.assign({}, info, {
    centered: Math.abs(box.x + box.width / 2 - viewport.width / 2) < 30,
    width: Math.round(box.width)
  });
}

async function closeByX(page) {
  await page.click('.el-dialog__wrapper:not([style*="display: none"]) .el-dialog__headerbtn');
  await page.waitForTimeout(600);
}

function getCsrf(page) {
  return page.context().cookies().then(function (cookies) {
    for (var i = 0; i < cookies.length; i++) {
      if (cookies[i].name === 'csrfToken') return cookies[i].value;
    }
    return '';
  });
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const allErrors = [];
  page.on('console', function (msg) {
    if (msg.type() === 'error') allErrors.push(msg.text());
  });
  page.on('pageerror', function (e) { allErrors.push('PAGEERROR: ' + e.message); });

  await page.goto(BASE + '/backend/page/login/login.html');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[type="password"]', '123456');
  await page.click('button[aria-label="登录"]');
  await page.waitForURL(/backend\/index\.html/, { timeout: 10000 });

  const result = {};

  // ===== A. 桌台列表：二维码打印 iframe 弹窗 =====
  await page.goto(BASE + '/backend/page/dining/table-list.html');
  await page.waitForSelector('#table-app', { timeout: 10000 });
  await page.waitForTimeout(2000);
  // table-bar 上的「二维码打印」操作按钮
  await page.locator('button', { hasText: '打印桌贴' }).first().click();
  await page.waitForSelector('.el-dialog__wrapper:not([style*="display: none"]) .unified-dialog iframe', { timeout: 8000 });
  result.A_posterDialog = await inspectVisibleDialog(page);
  await closeByX(page);

  // ===== B. AI 助手：模型配置 iframe 弹窗 =====
  await page.goto(BASE + '/backend/page/ai/assistant.html');
  await page.waitForSelector('button[aria-label="模型配置"]', { timeout: 10000 });
  await page.click('button[aria-label="模型配置"]');
  await page.waitForSelector('.el-dialog__wrapper:not([style*="display: none"]) .unified-dialog iframe', { timeout: 8000 });
  result.B_providerDialog = await inspectVisibleDialog(page);
  await closeByX(page);

  // ===== C. 二维码中心：勾选 2 桌 → 生成 → 预览弹窗 =====
  await page.goto(BASE + '/backend/page/printer/qrcode-center.html');
  await page.waitForSelector('.el-table .el-checkbox', { timeout: 10000 });
  // 勾选前两行
  await page.locator('.el-table__body-wrapper .el-checkbox').nth(0).click();
  await page.locator('.el-table__body-wrapper .el-checkbox').nth(1).click();
  // 站点地址填本机
  await page.fill('.el-form input[placeholder*="IP"]', 'http://localhost:8080');
  await page.locator('button', { hasText: '生成并预览' }).first().click();
  // ReggieUI 本机地址确认框
  await page.waitForSelector('.el-message-box', { timeout: 5000 });
  await page.locator('.el-message-box__btns button.el-button--primary').click();
  // 等生成结束（轮询内部状态，兼容偶发时序）
  let genOk = false;
  for (let i = 0; i < 20; i++) {
    await page.waitForTimeout(1500);
    const st = await page.evaluate(function () {
      var v = document.querySelector('#app').__vue__;
      return { generating: v.generating, posters: v.posters.length, preview: v.previewVisible };
    });
    if (!st.generating && st.posters > 0) { genOk = true; break; }
  }
  // 自动预览没弹就手动点「查看预览」兜底
  let visible = await page.locator('.el-dialog__wrapper:not([style*="display: none"]) .qr-preview-dialog').count();
  if (genOk && !visible) {
    await page.locator('button', { hasText: '查看预览' }).first().click();
  }
  // 等预览弹窗（含 pv-stage）
  await page.waitForSelector('.el-dialog__wrapper:not([style*="display: none"]) .qr-preview-dialog .pv-stage', { timeout: 10000 });
  result.C_previewDialog = await inspectVisibleDialog(page);
  // 预览交互：下一页箭头 + 页脚打印按钮存在
  result.C_hasNext = await page.locator('.qr-preview-dialog button.pv-nav').nth(1).isEnabled();
  result.C_footerPrint = await page.locator('.el-dialog__wrapper:not([style*="display: none"]) .dialog-footer button.el-button--primary').count();
  await closeByX(page);

  // ===== D. 收银台加菜（先一键开台造数据） =====
  await page.goto(BASE + '/backend/page/cashier/index.html');
  await page.waitForSelector('.table-card', { timeout: 10000 });
  const freeTable = await page.evaluate(function () {
    var vm = document.querySelector('#main-content').__vue__;
    var t = vm.tables.filter(function (x) { return x.status === 'FREE'; })[0];
    return t ? { id: t.id, name: t.name } : null;
  });
  let orderId = null;
  if (freeTable) {
    const csrf = await getCsrf(page);
    const resp = await page.request.post(BASE + '/api/dining/table/openWithOrder?tableId=' + freeTable.id,
      { headers: { 'X-CSRF-Token': csrf } });
    const body = await resp.json();
    orderId = body.data.orderId;

    await page.reload();
    await page.waitForSelector('.table-card.status-OCCUPIED', { timeout: 10000 });
    await page.locator('.table-card.status-OCCUPIED').first().click();
    await page.waitForSelector('.detail-header', { timeout: 8000 });
    await page.locator('.detail-header button', { hasText: '加菜' }).click();
    await page.waitForSelector('.el-dialog__wrapper:not([style*="display: none"]) .add-items-grid', { timeout: 8000 });
    result.D_addItemsDialog = await inspectVisibleDialog(page);
    result.D_dishCards = await page.locator('.add-items-grid .dish-pick-card').count();
    await closeByX(page);

    // 清理
    const csrf2 = await getCsrf(page);
    await page.request.put(BASE + '/order/cancel?id=' + orderId + '&reason=' + encodeURIComponent('自动化测试清理'),
      { headers: { 'X-CSRF-Token': csrf2 } });
    const stResp = await page.request.put(BASE + '/api/dining/table/status', {
      headers: { 'X-CSRF-Token': csrf2, 'Content-Type': 'application/json' },
      data: JSON.stringify({ id: freeTable.id, status: 'FREE' })
    });
    result.D_cleanup = (await stResp.json()).code;
  }

  result.consoleErrors = allErrors;
  result.pass =
    result.A_posterDialog && result.A_posterDialog.centered && /fullscreen/.test(result.A_posterDialog.classes) && result.A_posterDialog.iframeCount === 1 &&
    result.B_providerDialog && result.B_providerDialog.centered && /fullscreen/.test(result.B_providerDialog.classes) && result.B_providerDialog.iframeCount === 1 &&
    result.C_previewDialog && result.C_previewDialog.centered && /el-dialog--xl/.test(result.C_previewDialog.classes) &&
    /qr-preview-dialog/.test(result.C_previewDialog.classes) && result.C_footerPrint === 1 &&
    result.D_addItemsDialog && result.D_addItemsDialog.centered && /el-dialog--lg/.test(result.D_addItemsDialog.classes) &&
    result.D_dishCards > 0 && result.D_cleanup === 1 && allErrors.length === 0;

  console.log(JSON.stringify(result, null, 2));
  await browser.close();
  process.exit(result.pass ? 0 : 1);
})().catch(function (e) {
  console.error('VERIFY_FAILED:', e);
  process.exit(2);
});
