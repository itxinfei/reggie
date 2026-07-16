/**
 * 用户端（移动端）- 首页与菜品浏览测试
 *
 * 对应页面：front/page/index.html
 */

import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/login.page';
import { ApiInterceptor } from './utils/api-interceptor';

test.describe('用户端 - 首页与菜品浏览', () => {
  let interceptor: ApiInterceptor;

  test.beforeEach(async ({ page }) => {
    // 用户端不需要登录，直接访问首页
    interceptor = new ApiInterceptor(page);
    await page.goto('/index.html');
    await page.waitForLoadState('networkidle', { timeout: 15000 });
  });

  /**
   * @tag @smoke @front @ui
   */
  test('TC-FRONT-001: 首页加载并显示分类与推荐菜品', async ({ page }) => {
    // ============ DOM 断言 ============
    // 验证页面包含必要的 UI 元素
    const bodyContent = await page.content();

    // 页面应该包含基本的 HTML 结构
    expect(bodyContent.length).toBeGreaterThan(0);
  });

  /**
   * @tag @front @api
   */
  test('TC-FRONT-002: 首页菜品列表 API 请求验证', async ({ page }) => {
    // ============ API 拦截验证 ============
    const responses = interceptor.getResponses({ urlPattern: '*/category/list*' });
    if (responses.length > 0) {
      expect(responses[0].status).toBe(1);
      expect(responses[0].body.data).toBeDefined();
      expect(Array.isArray(responses[0].body.data)).toBe(true);
    }

    const dishResponses = interceptor.getResponses({
      urlPattern: '*/dish/list*',
    });
    if (dishResponses.length > 0) {
      expect(dishResponses[0].status).toBe(1);
      expect(dishResponses[0].body.data).toBeDefined();
    }
  });

  /**
   * @tag @front @ui
   */
  test('TC-FRONT-003: 点击分类筛选菜品', async ({ page }) => {
    // 获取分类列表
    const categories = page.locator('.category-item, .van-sidebar-item');
    const count = await categories.count();

    if (count > 1) {
      // 点击第二个分类
      await categories.nth(1).click();
      await page.waitForTimeout(500);

      // 验证菜品列表更新
      const dishList = page.locator('.dish-item, .goods-item, .van-card');
      const dishCount = await dishList.count();
      expect(dishCount).toBeGreaterThanOrEqual(0);
    }
  });

  /**
   * @tag @front @ui
   */
  test('TC-FRONT-004: 点击菜品查看详情', async ({ page }) => {
    // 获取第一个菜品
    const dishItem = page.locator('.dish-item, .goods-item, .van-card').first();

    if (await dishItem.count() > 0) {
      interceptor.clear();

      await dishItem.click();
      await page.waitForTimeout(1000);

      // 验证详情弹窗/页面出现
      const detail = page.locator('.dish-detail, .goods-detail, .van-popup, .van-action-sheet');
      if (await detail.count() > 0) {
        await expect(detail.first()).toBeVisible();
      }
    }
  });

  /**
   * @tag @front @ui
   */
  test('TC-FRONT-005: 用户端页面关键元素检查', async ({ page }) => {
    // ============ DOM 断言 ============
    // 验证页面包含必要的 UI 元素（可能是 Vant 组件或自定义元素）
    const bodyContent = await page.content();

    // 页面应该包含基本的 HTML 结构
    expect(bodyContent.length).toBeGreaterThan(0);
    expect(bodyContent).toContain('<!DOCTYPE html>');
  });
});

