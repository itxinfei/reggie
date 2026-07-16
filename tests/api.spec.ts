/**
 * API 独立测试（不依赖 UI）
 *
 * 使用 page.evaluate + XMLHttpRequest（自动携带 Session Cookie）
 */

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

/**
 * 在浏览器上下文中执行 API 请求
 */
async function apiFetch(page: any, url: string, method: string = 'GET', body?: string): Promise<{ status: number; body: any }> {
  return page.evaluate(({ url, method, body }: { url: string; method: string; body?: string }) => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open(method, url, true);
      xhr.setRequestHeader('Content-Type', 'application/json');
      xhr.onload = () => {
        try {
          resolve({ status: xhr.status, body: JSON.parse(xhr.responseText) });
        } catch {
          resolve({ status: xhr.status, body: xhr.responseText });
        }
      };
      xhr.onerror = () => reject(new Error('XHR error'));
      xhr.send(body || null);
    });
  }, { url, method, body });
}

test.describe('API 独立测试', () => {

  test.beforeEach(async ({ page }) => {
    // 导航到登录页并登录获取 Session
    await page.goto('/backend/page/login/login.html');
    await page.waitForSelector('input[placeholder="用户名"]', { timeout: 15000 });
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', '123456');
    await Promise.all([
      page.waitForURL('**/backend/index.html', { timeout: 15000 }),
      page.click('button.btn-login'),
    ]);
    await page.waitForTimeout(2000);
  });

  /**
   * @tag @api @smoke @employee
   */
  test('TC-API-001: 员工登录 API', async ({ page }) => {
    // 先跳转到登录页
    await page.goto('/backend/page/login/login.html');
    await page.waitForSelector('input[placeholder="用户名"]', { timeout: 15000 });

    const result = await apiFetch(page, `${BASE_URL}/employee/login`, 'POST',
      JSON.stringify({ username: 'admin', password: '123456' }));
    expect(result.status).toBe(200);
    expect(result.body.code).toBe(1);
    expect(result.body.data).toBeDefined();
    expect(result.body.data.username).toBe('admin');
  });

  /**
   * @tag @api @employee
   */
  test('TC-API-002: 员工分页查询', async ({ page }) => {
    const result = await apiFetch(page, `${BASE_URL}/employee/page?page=1&pageSize=10`, 'GET');
    expect(result.status).toBe(200);
    expect(result.body.code).toBe(1);
    expect(result.body.data).toBeDefined();
  });

  /**
   * @tag @api @employee
   */
  test('TC-API-003: 新增员工 API', async ({ page }) => {
    const testPhone = `138${Date.now().toString().slice(-8)}`;
    const result = await apiFetch(page, `${BASE_URL}/employee`, 'POST',
      JSON.stringify({
        name: 'API测试员工',
        username: `api_${Date.now().toString().slice(-6)}`,
        phone: testPhone,
        sex: 1,
      }));
    expect(result.status).toBe(200);
    expect(result.body.code).toBe(1);
    expect(result.body.data.id).toBeGreaterThan(0);
  });

  /**
   * @tag @api @category
   */
  test('TC-API-004: 分类列表查询', async ({ page }) => {
    const result = await apiFetch(page, `${BASE_URL}/category/list?type=1`, 'GET');
    expect(result.status).toBe(200);
    expect(result.body.code).toBe(1);
    expect(Array.isArray(result.body.data)).toBe(true);
  });

  /**
   * @tag @api @dish
   */
  test('TC-API-005: 菜品分页查询', async ({ page }) => {
    const result = await apiFetch(page, `${BASE_URL}/food/page?page=1&pageSize=5`, 'GET');
    expect(result.status).toBe(200);
    expect(result.body.code).toBe(1);
    expect(result.body.data).toBeDefined();
  });

  /**
   * @tag @api @order
   */
  test('TC-API-006: 订单分页查询', async ({ page }) => {
    const result = await apiFetch(page, `${BASE_URL}/order/page?page=1&pageSize=10`, 'GET');
    expect(result.status).toBe(200);
    expect(result.body.code).toBe(1);
    expect(result.body.data).toBeDefined();
  });

  /**
   * @tag @api @auth
   */
  test('TC-API-007: 未认证访问验证', async ({ page }) => {
    // 不登录，直接请求
    await page.goto('about:blank');
    const result = await apiFetch(page, `${BASE_URL}/employee/page?page=1&pageSize=10`, 'GET');
    expect(result.status).toBe(200);
    expect(result.body.code).not.toBe(1);
  });

  /**
   * @tag @api @perf
   */
  test('TC-API-008: API 响应时间性能验证', async ({ page }) => {
    const endpoints = [
      { method: 'GET', url: `${BASE_URL}/employee/page?page=1&pageSize=10` },
      { method: 'GET', url: `${BASE_URL}/category/list?type=1` },
      { method: 'GET', url: `${BASE_URL}/food/page?page=1&pageSize=5` },
    ];

    for (const ep of endpoints) {
      const start = Date.now();
      const result = await apiFetch(page, ep.url, ep.method);
      const elapsed = Date.now() - start;
      expect(result.status).toBe(200);
      expect(elapsed).toBeLessThan(5000);
    }
  });
});
