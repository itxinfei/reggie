/**
 * 订单管理测试
 */

import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/login.page';
import { OrderPage } from './pages/order.page';

test.describe('订单管理', () => {
  let loginPage: LoginPage;
  let orderPage: OrderPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', '123456');
    await loginPage.assertLoggedIn();

    orderPage = new OrderPage(page);
    await orderPage.goto();
  });

  /**
   * @tag @smoke @order @api
   */
  test('TC-ORDER-001: 订单列表加载验证', async () => {
    // 等待 iframe 加载
    await orderPage.page.waitForTimeout(1000);

    // 验证 API 响应
    const responses = orderPage.getApiResponses(/\/order\/page/);
    const pageResp = responses.find(r => r.url.includes('/page'));
    if (pageResp) {
      expect(pageResp.apiCode ?? pageResp.status).toBe(1);
      expect(pageResp.body.data).toBeDefined();
      expect(pageResp.body.data.records).toBeDefined();
    }

    // DOM 断言（在 iframe 内）
    await expect(orderPage.el('.el-table')).toBeVisible({ timeout: 5000 });
    await expect(orderPage.el('.el-pagination')).toBeVisible({ timeout: 5000 });
  });

  /**
   * @tag @order @ui
   */
  test('TC-ORDER-002: 页面 UI 结构验证', async () => {
    // 验证搜索输入框
    const numberInput = orderPage.el('input[placeholder*="订单号"]');
    await expect(numberInput).toBeVisible();

    // 验证查询按钮
    const queryBtn = orderPage.el('button:has-text("查询")');
    await expect(queryBtn).toBeVisible();

    // 验证重置按钮
    const resetBtn = orderPage.el('button:has-text("重置")');
    await expect(resetBtn).toBeVisible();

    // 验证表格和分页
    await expect(orderPage.el('.el-table')).toBeVisible({ timeout: 5000 });
    await expect(orderPage.el('.el-pagination')).toBeVisible({ timeout: 5000 });
  });

  /**
   * @tag @order @api
   */
  test('TC-ORDER-003: 筛选订单验证 API', async () => {
    const responses = await orderPage.filter({});

    const pageResp = responses.find(r => r.url.includes('/page'));
    if (pageResp) {
      expect(pageResp.apiCode ?? pageResp.status).toBe(1);
      expect(pageResp.body.data.total).toBeDefined();
    }
  });

  /**
   * @tag @order @ui
   */
  test('TC-ORDER-004: 重置筛选条件', async () => {
    await orderPage.filter({ number: '2024' });
    await orderPage.reset();

    const numberInput = orderPage.el('input[placeholder*="订单号"]');
    if (await numberInput.count() > 0) {
      const value = await numberInput.inputValue();
      expect(value).toBe('');
    }
  });
});
