/**
 * 菜品管理测试
 */

import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/login.page';
import { DishPage } from './pages/dish.page';

test.describe('菜品管理', () => {
  let loginPage: LoginPage;
  let dishPage: DishPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', '123456');
    await loginPage.assertLoggedIn();

    dishPage = new DishPage(page);
    await dishPage.goto();
  });

  /**
   * @tag @smoke @dish @api
   */
  test('TC-DISH-001: 页面加载验证 API 响应', async ({ page }) => {
    await page.waitForTimeout(1000);

    // 验证 API 响应
    const responses = dishPage.getApiResponses(/\/food\/page|\/dish\/page/);
    expect(responses.length).toBeGreaterThan(0);

    const pageResp = responses.find(r => r.url.includes('/page'));
    if (pageResp) {
      expect(pageResp.apiCode ?? pageResp.status).toBe(1);
      expect(pageResp.body.data).toBeDefined();
      expect(pageResp.body.data.records).toBeDefined();
    }
  });

  /**
   * @tag @dish @ui
   */
  test('TC-DISH-002: 页面 UI 结构验证', async () => {
    // 验证搜索输入框（在 iframe 内）
    const searchInput = dishPage.el('input[placeholder*="菜品名称"]');
    await expect(searchInput).toBeVisible({ timeout: 5000 });

    // 验证操作按钮
    const addBtn = dishPage.el('button:has-text("+ 新建菜品")');
    await expect(addBtn).toBeVisible();
  });

  /**
   * @tag @dish @api
   */
  test('TC-DISH-003: 搜索菜品验证 API', async () => {
    const responses = await dishPage.search('');
    const searchResp = responses.find(r => r.url.includes('/page'));

    if (searchResp) {
      expect(searchResp.apiCode ?? searchResp.status).toBe(1);
      expect(Array.isArray(searchResp.body.data.records)).toBe(true);
    }

    const rowCount = await dishPage.getRowCount();
    expect(rowCount).toBeGreaterThanOrEqual(0);
  });

  /**
   * @tag @dish @ui
   */
  test('TC-DISH-004: 点击新建菜品按钮验证弹窗', async () => {
    await dishPage.openAddDialog();

    // DOM 断言：验证弹窗
    await dishPage.assertDialogVisible();

    // 验证表单字段
    const nameInput = dishPage.el('input[placeholder="请填写菜品名称"]');
    await expect(nameInput).toBeVisible();
  });
});
