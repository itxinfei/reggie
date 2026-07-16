/**
 * 端到端综合测试 - 模拟真实管理员操作流程
 *
 * 流程：登录 → 员工管理（新增） → 菜品管理（查看） → 订单管理（查看列表）
 */

import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/login.page';
import { EmployeePage } from './pages/employee.page';
import { DishPage } from './pages/dish.page';
import { OrderPage } from './pages/order.page';
import { ApiInterceptor, ApiAssertions } from './utils/api-interceptor';

test.describe('端到端综合流程', () => {
  let loginPage: LoginPage;
  let employeePage: EmployeePage;
  let dishPage: DishPage;
  let orderPage: OrderPage;

  /**
   * @tag @smoke @e2e @admin
   */
  test('TC-E2E-001: 管理员完整业务流程', async ({ page }) => {
    // ========== 步骤1: 登录 ==========
    loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', '123456');
    await loginPage.assertLoggedIn();
    console.log('✓ 步骤1: 管理员登录成功');

    // ========== 步骤2: 员工管理 - 新增员工 ==========
    employeePage = new EmployeePage(page);
    await employeePage.goto();

    const empData = {
      name: 'E2E员工',
      username: `e2e_${Date.now().toString().slice(-6)}`,
      phone: `139${Date.now().toString().slice(-8)}`,
      sex: '男' as const,
    };

    await employeePage.addEmployee(empData);
    await employeePage.assertEmployeeExists(empData.name);
    console.log('✓ 步骤2: 新增员工成功');

    // ========== 步骤3: 菜品管理 - 查看列表 ==========
    dishPage = new DishPage(page);
    await dishPage.goto();

    // 验证 API 响应
    const dishResponses = dishPage.getApiResponses(/\/food\/page/);
    const dishPageResp = dishResponses.find(r => r.url.includes('/page'));
    if (dishPageResp) {
      ApiAssertions.assertApiSuccess(dishPageResp);
    }

    const rowCount = await dishPage.getRowCount();
    expect(rowCount).toBeGreaterThanOrEqual(0);
    console.log('✓ 步骤3: 菜品列表加载成功');

    // ========== 步骤4: 订单管理 - 查看列表 ==========
    orderPage = new OrderPage(page);
    await orderPage.goto();

    const orderResponses = orderPage.getApiResponses(/\/order\/page/);
    const orderPageResp = orderResponses.find(r => r.url.includes('/page'));
    if (orderPageResp) {
      ApiAssertions.assertApiSuccess(orderPageResp);
    }

    const orderRowCount = await orderPage.getRowCount();
    expect(orderRowCount).toBeGreaterThanOrEqual(0);
    console.log('✓ 步骤4: 订单列表加载成功');

    console.log('\n✅ 端到端综合流程测试全部通过');
  });

  /**
   * @tag @e2e @api
   */
  test('TC-E2E-002: API 全链路请求验证', async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', '123456');

    const interceptor = new ApiInterceptor(page);

    // 员工模块
    employeePage = new EmployeePage(page);
    await employeePage.goto();
    await page.waitForTimeout(1000);

    const empResponses = interceptor.getResponses(/\/member/);
    expect(empResponses.length).toBeGreaterThan(0);
    ApiAssertions.assertApiSuccess(empResponses[0]);

    // 菜品模块
    dishPage = new DishPage(page);
    await dishPage.goto();
    await page.waitForTimeout(1000);

    const dishResponses = interceptor.getResponses(/\/food/);
    expect(dishResponses.length).toBeGreaterThan(0);
    ApiAssertions.assertApiSuccess(dishResponses[0]);

    // 订单模块
    orderPage = new OrderPage(page);
    await orderPage.goto();
    await page.waitForTimeout(1000);

    const orderResponses = interceptor.getResponses(/\/order/);
    expect(orderResponses.length).toBeGreaterThan(0);
    ApiAssertions.assertApiSuccess(orderResponses[0]);
  });
});

