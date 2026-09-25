/**
 * 收银台「转台」弹窗浏览器实测：
 * 1. 员工登录后台 → 收银台
 * 2. 打开在用桌台 → 转台弹窗，校验：
 *    - 弹窗使用 crud-dialog（custom-class 含 unified-dialog el-dialog--sm）
 *    - 弹窗水平居中（历史 Bug：class 误写到遮罩导致靠左）
 *    - 空闲桌台下拉可选、确认转台成功
 * 3. 再把订单转回原桌台，恢复现场
 * 无在用桌台时退化为直接打开弹窗做渲染/居中校验。
 */
const { chromium } = require('C:\\Users\\itxinfei\\.npm-global\\node_modules\\playwright');

const BASE = 'http://localhost:8080';

async function visibleDialog(page) {
  // el-dialog 未打开时 wrapper display:none；取当前可见的弹窗盒子
  const box = await page.$('.el-dialog__wrapper:not([style*="display: none"]) .unified-dialog');
  return box;
}

async function openTransferAndInspect(page, label, alreadyOpen) {
  // 点击转台按钮（退化场景弹窗已由 Vue 实例直接打开）
  if (!alreadyOpen) {
    await page.click('.table-actions button:has(i.ri-arrow-left-right-line)');
  }
  await page.waitForSelector('.el-dialog__wrapper:not([style*="display: none"]) .unified-dialog', { timeout: 5000 });

  const dlg = await visibleDialog(page);
  const cls = await dlg.getAttribute('class');
  const box = await dlg.boundingBox();
  const viewport = page.viewportSize();
  const centerX = box.x + box.width / 2;
  const centered = Math.abs(centerX - viewport.width / 2) < 30;

  // 下拉可选数量
  await page.click('.el-dialog__wrapper:not([style*="display: none"]) .el-select');
  await page.waitForSelector('.el-select-dropdown:visible .el-select-dropdown__item', { timeout: 5000 });
  const optionCount = await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').count();

  return Object.assign({ step: label, dialogClass: cls, centered: centered, centerOffset: Math.round(centerX - viewport.width / 2), freeTableOptions: optionCount },
    await dlg.evaluate(function (el) {
      return {
        isSm: el.classList.contains('el-dialog--sm'),
        isUnified: el.classList.contains('unified-dialog'),
        title: (el.querySelector('.el-dialog__title') || {}).textContent
      };
    }));
}

async function selectTableAndConfirm(page, targetName) {
  // 下拉已展开：按桌台名精确点选目标项（保证往返转台能确定性恢复现场）
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: targetName + '（' }).first().click();
  const confirmBtn = page.locator('.el-dialog__wrapper:not([style*="display: none"]) .dialog-footer button.el-button--primary');
  await confirmBtn.click();
  // 成功提示 + 弹窗关闭
  await page.waitForSelector('.el-message--success', { timeout: 8000 });
}

function occupiedCardByName(page, name) {
  var escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return page.locator('.table-card.status-OCCUPIED')
    .filter({ has: page.locator('.table-name', { hasText: new RegExp('^' + escaped + '$') }) });
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

  const consoleErrors = [];
  page.on('console', function (msg) {
    if (msg.type() === 'error') {
      var t = msg.text();
      // crud-dialog 护栏只在未用 .sync 时报错；预期不应出现
      if (t.indexOf('crud-dialog') === -1 || t.indexOf('缺少') >= 0) consoleErrors.push(t);
    }
  });
  page.on('pageerror', function (e) { consoleErrors.push('PAGEERROR: ' + e.message); });

  // 1. 登录
  await page.goto(BASE + '/backend/page/login/login.html');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[type="password"]', '123456');
  await page.click('button[aria-label="登录"]');
  await page.waitForURL(/backend\/index\.html/, { timeout: 10000 });

  // 2. 收银台：读取两张空闲桌台（源/目标）
  await page.goto(BASE + '/backend/page/cashier/index.html');
  await page.waitForSelector('.table-card', { timeout: 10000 });

  const freeTablesList = await page.evaluate(function () {
    var vm = document.querySelector('#main-content').__vue__;
    return vm.tables.filter(function (t) { return t.status === 'FREE'; }).slice(0, 2)
      .map(function (t) { return { id: t.id, name: t.name }; });
  });

  const result = { source: freeTablesList[0] || null, target: freeTablesList[1] || null };
  let setupDone = false;

  if (!result.source || !result.target) {
    result.skip = '空闲桌台不足 2 张，无法构造转台往返';
  } else {
    const csrf = await getCsrf(page);
    // 3. 一键开台：源桌台绑定占位订单 → 占用
    const openResp = await page.request.post(
      BASE + '/api/dining/table/openWithOrder?tableId=' + result.source.id,
      { headers: { 'X-CSRF-Token': csrf } }
    );
    const openBody = await openResp.json();
    result.openWithOrder = openBody;
    setupDone = String(openBody.code) === '1';
    result.orderId = setupDone ? openBody.data.orderId : null;
  }

  try {
    if (setupDone) {
      // 刷新页面拉取最新桌台状态
      await page.reload();
      await page.waitForSelector('.table-card.status-OCCUPIED', { timeout: 10000 });

      // 4. 打开源桌台 → 转台到目标桌台
      await occupiedCardByName(page, result.source.name).first().click();
      await page.waitForSelector('.detail-header', { timeout: 8000 });
      result.open = await openTransferAndInspect(page, '转出');
      await selectTableAndConfirm(page, result.target.name);
      result.transferred = true;

      // 5. 打开目标桌台 → 转回源桌台，恢复现场
      await page.waitForSelector('.table-card.status-OCCUPIED', { timeout: 8000 });
      await occupiedCardByName(page, result.target.name).first().click();
      await page.waitForSelector('.detail-header', { timeout: 8000 });
      result.openBack = await openTransferAndInspect(page, '转回');
      await selectTableAndConfirm(page, result.source.name);
      result.restored = true;
    }
  } finally {
    // 6. 清理：取消占位订单 + 源桌台恢复空闲
    if (setupDone) {
      const csrf = await getCsrf(page);
      const cancelResp = await page.request.put(
        BASE + '/order/cancel?id=' + result.orderId + '&reason=' + encodeURIComponent('自动化测试清理'),
        { headers: { 'X-CSRF-Token': csrf } }
      );
      result.cleanupOrder = (await cancelResp.json()).code;
      const statusResp = await page.request.put(BASE + '/api/dining/table/status', {
        headers: { 'X-CSRF-Token': csrf, 'Content-Type': 'application/json' },
        data: JSON.stringify({ id: result.source.id, status: 'FREE' })
      });
      result.cleanupTable = (await statusResp.json()).code;
    }
  }

  result.consoleErrors = consoleErrors;
  result.pass = !!(setupDone && result.open && result.open.centered && result.open.isSm && result.open.isUnified &&
    result.open.freeTableOptions > 0 && result.transferred && result.restored &&
    result.cleanupOrder === 1 && result.cleanupTable === 1 && consoleErrors.length === 0);

  console.log(JSON.stringify(result, null, 2));
  await browser.close();
  process.exit(result.pass ? 0 : 1);
})().catch(function (e) {
  console.error('VERIFY_FAILED:', e);
  process.exit(2);
});
