/**
 * 管理后台 - 登录页 Page Object
 *
 * 对应页面：/backend/page/login/login.html
 *
 * 认证方式：Session Cookie (JSESSIONID)
 * 登录成功跳转：/backend/index.html（iframe 架构）
 */

import { type Page, expect } from '@playwright/test';
import { ApiInterceptor } from '../utils/api-interceptor';

export class LoginPage {
  readonly page: Page;
  private interceptor: ApiInterceptor;

  readonly selectors = {
    usernameInput: 'input[placeholder="用户名"]',
    passwordInput: 'input[placeholder="密码"]',
    loginButton: 'button.btn-login',
    errorMessage: '.el-message--error',
    loginForm: '#login-app .login-form',
  };

  constructor(page: Page) {
    this.page = page;
    this.interceptor = new ApiInterceptor(page);
  }

  /**
   * 导航到登录页
   */
  async goto() {
    await this.page.goto('/backend/page/login/login.html');
    await this.page.waitForSelector(this.selectors.loginButton, { timeout: 15000 });
  }

  /**
   * 执行登录
   */
  async login(username: string, password: string) {
    this.interceptor.clear();

    await this.page.fill(this.selectors.usernameInput, username);
    await this.page.fill(this.selectors.passwordInput, password);

    // 监听登录 API 响应
    let loginResponse: any = null;
    this.page.on('response', async (response) => {
      if (response.url().includes('/employee/login') && response.status() === 200) {
        try { loginResponse = await response.json(); } catch {}
      }
    });

    await this.page.click(this.selectors.loginButton);

    // 等待跳转或错误提示
    try {
      await this.page.waitForURL('**/backend/index.html', { timeout: 15000 });
    } catch {
      // 等待错误提示出现（不重复检查导致浏览器关闭）
      await this.page.waitForTimeout(2000);
      const currentUrl = this.page.url();
      if (currentUrl.includes('/backend/page/login')) {
        // 仍在登录页，检查是否有错误消息
        const errorText = await this.page.locator('.el-message__content').textContent().catch(() => '');
        throw new Error(`登录失败: ${errorText || '用户名或密码错误'}`);
      }
      // URL 已改变但没匹配到 index.html，可能是其他跳转
    }

    // 等待首页加载完成
    await this.page.waitForSelector('iframe#cIframe', { timeout: 10000 });
    await this.page.waitForTimeout(2000); // 等待 iframe 内容加载

    return loginResponse;
  }

  /**
   * 使用默认账号登录
   */
  async loginWithDefaults() {
    return this.login('admin', '123456');
  }

  /**
   * 验证登录成功
   */
  async assertLoggedIn() {
    const url = this.page.url();
    expect(url).toContain('/backend/index.html');
    await this.page.waitForSelector('iframe#cIframe', { timeout: 10000 });
  }

  /**
   * 验证仍停留在登录页
   */
  async assertStillOnLoginPage() {
    expect(this.page.url()).toContain('/backend/page/login');
  }

  /**
   * 验证登录错误提示
   */
  async assertLoginError(expectedMessage?: string) {
    const errorEl = this.page.locator('.el-message--error .el-message__content');
    await expect(errorEl).toBeVisible({ timeout: 5000 });
    if (expectedMessage) {
      await expect(errorEl).toContainText(expectedMessage);
    }
  }
}
