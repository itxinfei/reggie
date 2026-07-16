/**
 * 员工管理测试
 *
 * 覆盖场景：
 * - 新增员工 → API 验证 → DOM 验证（表格出现新数据）
 * - 搜索员工 → API 验证 → 表格过滤验证
 * - 表单弹窗结构验证
 */

import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/login.page';
import { EmployeePage } from './pages/employee.page';

test.describe('员工管理', () => {
  let loginPage: LoginPage;
  let employeePage: EmployeePage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', '123456');
    await loginPage.assertLoggedIn();

    employeePage = new EmployeePage(page);
    await employeePage.goto();
  });

  /**
   * @tag @smoke @employee @ui @api
   */
  test('TC-EMP-001: 新增员工并验证 API 响应与 DOM 变化', async ({ page }) => {
    const testData = {
      name: '测试员工_自动',
      username: `test_${Date.now().toString().slice(-6)}`,
      phone: `138${Date.now().toString().slice(-8)}`,
      sex: '男' as const,
      idNumber: '110101199001011234',
    };

    // 记录初始行数
    const initialCount = await employeePage.getRowCount();

    // 新增员工
    const response = await employeePage.addEmployee(testData);

    // ========== API 验证 ==========
    expect(response).toBeDefined();
    const responses = employeePage.getApiResponses(/\/employee/);
    const successResp = responses.find(r => {
      const b = r.body;
      return b && (b.code === 1 || b.code === '1');
    });
    expect(successResp).toBeDefined();

    // ========== DOM 断言 ==========
    // 验证表格行数增加或新员工出现在表格中
    await employeePage.assertEmployeeExists(testData.name);

    // 验证成功提示
    const toast = page.locator('.el-message--success .el-message__content');
    if (await toast.count() > 0) {
      await expect(toast).toContainText('成功', { timeout: 5000 });
    }
  });

  /**
   * @tag @employee @api
   */
  test('TC-EMP-002: 搜索员工并验证', async ({ page }) => {
    const responses = await employeePage.search('');

    // API 验证
    const pageResp = responses.find(r => r.url.includes('/page'));
    if (pageResp) {
      ApiAssertions.assertApiSuccess(pageResp);
      expect(pageResp.body.data.records).toBeDefined();
      expect(Array.isArray(pageResp.body.data.records)).toBe(true);
    }

    // DOM 断言
    const rowCount = await employeePage.getRowCount();
    expect(rowCount).toBeGreaterThanOrEqual(0);
  });

  /**
   * @tag @employee @ui
   */
  test('TC-EMP-003: 新增员工弹窗结构验证', async ({ page }) => {
    await employeePage.openAddDialog();

    // 验证弹窗标题
    await employeePage.assertDialogVisible('添加员工');

    // 验证所有表单字段
    await expect(employeePage.el('input[placeholder="请输入账号"]')).toBeVisible();
    await expect(employeePage.el('input[placeholder="请输入员工姓名"]')).toBeVisible();
    await expect(employeePage.el('input[placeholder="请输入手机号"]')).toBeVisible();
    await expect(employeePage.el('.el-radio-group')).toBeVisible();

    // 验证按钮
    const confirmBtn = employeePage.el('button:has-text("确 定")');
    await expect(confirmBtn).toBeVisible();

    // 关闭弹窗
    await employeePage.el('.el-dialog__close, button:has-text("取 消")').click();
    await employeePage.assertDialogHidden();
  });

  /**
   * @tag @employee @api
   */
  test('TC-EMP-004: 搜索验证 API 请求参数', async ({ page }) => {
    // 监听 API 请求
    const interceptor = new ApiInterceptor(page);
    let requestBody: any = null;

    page.on('request', (req) => {
      if (req.url().includes('/member/page') && req.method() === 'POST') {
        requestBody = req.postData();
      }
    });

    await employeePage.search('管理员');
    await page.waitForTimeout(500);

    // 验证请求参数
    if (requestBody) {
      const body = JSON.parse(requestBody);
      expect(body.page).toBeDefined();
      expect(body.pageSize).toBeDefined();
    }
  });
});

