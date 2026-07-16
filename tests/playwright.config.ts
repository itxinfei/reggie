import { defineConfig, devices } from '@playwright/test';

/**
 * Reggie 外卖系统 - Playwright 配置
 *
 * 实际路径（已确认）：
 * - 管理后台页面：/backend/page/login/login.html
 * - 管理后台首页：/backend/index.html
 * - 用户端页面：/index.html
 * - API 路径：/employee/login, /employee/page 等
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const API_BASE_URL = process.env.API_BASE_URL || `${BASE_URL}`;

export default defineConfig({
  testDir: '.',
  testMatch: '**/*.spec.ts',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'tests/reports/html-report', open: 'never' }],
    ['allure-playwright', { outputFolder: 'allure-results' }],
    ['json', { outputFile: 'tests/reports/test-results.json' }],
    ['junit', { outputFile: 'tests/reports/junit-results.xml' }]
  ],

  use: {
    baseURL: BASE_URL,
    headless: !process.env.HEADED,
    viewport: { width: 1920, height: 1080 },
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
    actionTimeout: 10000,
    navigationTimeout: 30000,
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
  },

  globalSetup: require.resolve('./utils/global-setup.ts'),
  globalTeardown: require.resolve('./utils/global-teardown.ts'),

  projects: [
    {
      name: 'admin-backend',
      use: {
        ...devices['Desktop Chrome'],
      },
    },
  ],
});
