/**
 * 全局环境初始化
 *
 * 职责：
 *   1. 等待后端服务可用
 *   2. 通过浏览器登录获取 Session Cookie
 *   3. 保存 Session Cookie 供测试用例使用
 *   4. 初始化 Allure 报告环境
 */

import { chromium, type FullConfig } from '@playwright/test';
import { allure } from 'allure-playwright';
import fs from 'fs';
import path from 'path';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const TEST_REPORTS_DIR = path.join(__dirname, '..', 'reports');

interface TestEnvData {
  sessionCookie: { name: string; value: string; domain: string } | null;
  employeeId: number;
  tenantId: string;
  testTimestamp: string;
}

async function globalSetup(_config: FullConfig) {
  console.log('\n========================================');
  console.log('  全局环境初始化');
  console.log('========================================\n');

  const startTime = Date.now();

  // 确保目录存在
  [TEST_REPORTS_DIR, path.join(__dirname, '..', 'allure-results')].forEach(dir => {
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  });

  // 清理旧 allure 结果
  const allureDir = path.join(__dirname, '..', 'allure-results');
  if (fs.existsSync(allureDir)) fs.rmSync(allureDir, { recursive: true });
  fs.mkdirSync(allureDir, { recursive: true });

  // 添加 Allure 标签
  allure.label('environment', 'test');
  allure.label('framework', 'playwright');
  allure.label('project', 'reggie-take-out');
  allure.label('baseUrl', BASE_URL);

  // 等待后端可用（使用 Node.js http 模块）
  console.log('  等待后端服务就绪...');
  const parsedUrl = new URL(BASE_URL);
  const http = await import('http');
  const maxRetries = 30;
  let ready = false;

  for (let i = 0; i < maxRetries; i++) {
    try {
      await new Promise<void>((resolve, reject) => {
        const req = http.request({
          hostname: parsedUrl.hostname,
          port: parsedUrl.port || (parsedUrl.protocol === 'https:' ? 443 : 80),
          path: '/employee/login',
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
        }, (res: any) => {
          if (res.statusCode === 200 || res.statusCode === 404) {
            resolve();
          } else {
            reject(new Error(`HTTP ${res.statusCode}`));
          }
        });
        req.on('error', reject);
        req.write(JSON.stringify({ username: 'admin', password: '123456' }));
        req.end();
      });
      ready = true;
      break;
    } catch (err) {
      // 服务未就绪，继续等待
    }
    await new Promise(r => setTimeout(r, 2000));
  }
  if (!ready) throw new Error('后端服务未就绪');
  console.log('  后端服务已就绪');

  // 登录获取 Session Cookie
  console.log('  登录获取 Session Cookie...');
  const envData = await loginAndGetSession();
  console.log(`  JSESSIONID: ${envData.sessionCookie?.value?.substring(0, 20)}...`);

  // 保存环境数据
  const envPath = path.join(__dirname, '..', 'allure-results', 'environment.json');
  fs.writeFileSync(envPath, JSON.stringify({
    'Base.URL': BASE_URL,
    'Browser': 'Chromium',
    'Platform': process.platform,
    'Test.Time': new Date().toLocaleString('zh-CN'),
    'Employee.ID': envData.employeeId,
    'Tenant.ID': envData.tenantId,
  }, null, 2));

  process.env['REGGIE_ENV_DATA'] = JSON.stringify(envData);

  console.log(`\n  初始化完成 (${Date.now() - startTime}ms)`);
  console.log('========================================\n');
}

/**
 * 通过 Playwright 浏览器登录获取 Session Cookie
 */
async function loginAndGetSession(): Promise<TestEnvData> {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ baseURL: BASE_URL });
  const page = await context.newPage();

  // 登录
  await page.goto('/backend/page/login/login.html');
  await page.waitForSelector('input[placeholder="用户名"]', { timeout: 15000 });

  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', '123456');

  // 监听 API 响应
  let loginData: any = null;
  page.on('response', async (response) => {
    if (response.url().includes('/employee/login') && response.status() === 200) {
      try { loginData = await response.json(); } catch {}
    }
  });

  await Promise.all([
    page.waitForURL('**/backend/index.html', { timeout: 15000 }),
    page.click('button.btn-login'),
  ]);

  await page.waitForLoadState('networkidle', { timeout: 10000 });

  // 获取 Cookie
  const allCookies = await context.cookies();
  const sessionCookie = allCookies.find((c: any) => c.name === 'JSESSIONID');

  let employeeId = 1;
  let tenantId = '1';
  if (loginData?.data) {
    employeeId = loginData.data.id || 1;
    tenantId = String(loginData.data.tenantId || '1');
  }

  await browser.close();
  return {
    sessionCookie: sessionCookie ? {
      name: sessionCookie.name,
      value: sessionCookie.value,
      domain: sessionCookie.domain,
    } : null,
    employeeId,
    tenantId,
    testTimestamp: new Date().toISOString(),
  };
}

export default globalSetup;
