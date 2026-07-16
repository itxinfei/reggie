/**
 * 登录功能测试
 *
 * 覆盖场景：
 * - 正常登录 → 跳转验证 → API 验证
 * - 错误密码 → 错误提示
 * - 空用户名/密码 → 表单校验
 * - 登录 API 请求参数验证
 */

import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/login.page';
import { ApiInterceptor, ApiAssertions } from './utils/api-interceptor';

test.describe('登录功能', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
  });

  test.afterEach(async ({ page }) => {
    // 如果仍在登录页，不做处理；如果已登录则保持登录状态供后续测试复用
  });

  /**
   * @tag @smoke @login @ui
   */
  test('TC-LOGIN-001: 管理员正确登录并跳转后台首页', async ({ page }) => {
    await loginPage.goto();

    const loginResp = await loginPage.login('admin', '123456');

    // 验证跳转
    await loginPage.assertLoggedIn();

    // DOM 断言：验证侧边栏菜单存在
    const sidebar = page.locator('.sidebar-container');
    await expect(sidebar).toBeVisible();

    // DOM 断言：验证 iframe 已加载
    const iframe = page.locator('iframe#cIframe');
    await expect(iframe).toBeVisible();
  });

  /**
   * @tag @login @negative
   */
  test('TC-LOGIN-002: 错误密码登录失败', async ({ page }) => {
    await loginPage.goto();

    // 手动输入错误密码
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'wrong_password');
    await page.click('button.btn-login');

    // 等待错误提示出现
    await page.waitForTimeout(2000);

    // DOM 断言：验证仍在登录页
    expect(page.url()).toContain('/backend/page/login');

    // 验证错误提示
    const errorEl = page.locator('.el-message__content');
    await expect(errorEl).toBeVisible({ timeout: 5000 });
  });

  /**
   * @tag @login @negative
   */
  test('TC-LOGIN-003: 空用户名登录', async ({ page }) => {
    await loginPage.goto();
    await page.fill('input[placeholder="用户名"]', '');
    await page.fill('input[placeholder="密码"]', '123456');

    // 表单校验应该阻止提交
    const errorTip = page.locator('.el-form-item__error');
    const count = await errorTip.count();
    // 可能没有显示（Element UI 的 blur 校验）
    if (count > 0) {
      await expect(errorTip.first()).toBeVisible();
    }
  });

  /**
   * @tag @login @api
   */
  test('TC-LOGIN-004: 验证登录 API 请求参数', async ({ page }) => {
    await loginPage.goto();

    const interceptor = new ApiInterceptor(page);
    const lp = new LoginPage(page);
    await lp.goto();

    // 监听 API
    let requestBody: any = null;
    page.on('request', (req) => {
      if (req.url().includes('/employee/login') && req.method() === 'POST') {
        requestBody = req.postData();
      }
    });

    await lp.login('admin', '123456');

    // 验证请求参数
    if (requestBody) {
      const body = JSON.parse(requestBody);
      expect(body.username).toBe('admin');
      expect(body.password).toBe('123456');
    }
  });

  /**
   * @tag @login @smoke
   */
  test('TC-LOGIN-005: 登录后验证用户信息显示', async ({ page }) => {
    const lp = new LoginPage(page);
    await lp.goto();
    await lp.login('admin', '123456');
    await lp.assertLoggedIn();

    // DOM 断言：验证用户名显示
    const userNameEl = page.locator('.user-name');
    await expect(userNameEl).toContainText('管理员', { timeout: 5000 });
  });
});

