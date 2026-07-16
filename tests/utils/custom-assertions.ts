/**
 * 自定义 DOM 断言工具
 *
 * 提供超越 Playwright 原生断言的业务级断言方法
 */

import { type Page, type Locator } from '@playwright/test';

// expect 从 @playwright/test 导入
const { expect: pwExpect } = require('@playwright/test');
import { ApiInterceptor, ResponseRecord } from '../utils/api-interceptor';

export class CustomAssertions {
  /**
   * 断言页面标题包含指定文本
   */
  static async assertPageTitle(page: Page, expectedTitle: string | RegExp) {
    const title = await page.title();
    if (expectedTitle instanceof RegExp) {
      expect(expectedTitle.test(title)).toBe(true);
    } else {
      expect(title).toContain(expectedTitle);
    }
  }

  /**
   * 断言页面 URL 包含指定路径
   */
  static assertUrlContains(page: Page, expectedPath: string) {
    const url = page.url();
    expect(url).toContain(expectedPath);
  }

  /**
   * 断言 API 响应包含指定字段路径
   */
  static assertResponseHasField(response: ResponseRecord, fieldPath: string) {
    const parts = fieldPath.split('.');
    let current: any = response.body;

    for (const part of parts) {
      expect(current).toBeDefined();
      expect(current).toHaveProperty(part);
      current = current[part];
    }
  }

  /**
   * 断言 API 响应码为成功
   */
  static assertApiSuccess(response: ResponseRecord) {
    expect(response.status).toBe(1); // Reggie 系统 code=1 表示成功
  }

  /**
   * 断言 API 响应码为失败
   */
  static assertApiFailed(response: ResponseRecord, expectedCode?: number) {
    if (expectedCode !== undefined) {
      expect(response.status).toBe(expectedCode);
    } else {
      expect(response.status).not.toBe(1);
    }
  }

  /**
   * 断言表格行数匹配
   */
  static async assertTableRowCount(page: Page, selector: string, expectedCount: number) {
    const rows = await page.locator(selector).count();
    expect(rows).toBe(expectedCount);
  }

  /**
   * 断言表格中包含指定文本
   */
  static async assertTableContains(page: Page, selector: string, expectedText: string) {
    const row = page.locator(`${selector}:has-text("${expectedText}")`);
    await expect(row).toBeVisible({ timeout: 5000 });
  }

  /**
   * 断言表格中不包含指定文本
   */
  static async assertTableNotContains(page: Page, selector: string, expectedText: string) {
    const row = page.locator(`${selector}:has-text("${expectedText}")`);
    await expect(row).not.toBeVisible();
  }

  /**
   * 断言弹窗显示
   */
  static async assertDialogVisible(page: Page, dialogTitle?: string) {
    const dialog = page.locator('.el-dialog');
    await expect(dialog).toBeVisible({ timeout: 5000 });

    if (dialogTitle) {
      const title = page.locator('.el-dialog__title');
      await expect(title).toHaveText(dialogTitle);
    }
  }

  /**
   * 断言弹窗隐藏
   */
  static async assertDialogHidden(page: Page) {
    const dialog = page.locator('.el-dialog');
    await expect(dialog).not.toBeVisible();
  }

  /**
   * 断言提示消息
   */
  static async assertToastMessage(page: Page, expectedMessage: string, type: 'success' | 'error' | 'warning' = 'success') {
    const selector = type === 'success'
      ? '.el-message--success .el-message__content'
      : type === 'error'
        ? '.el-message--error .el-message__content'
        : '.el-message--warning .el-message__content';

    const message = page.locator(selector);
    await expect(message).toContainText(expectedMessage, { timeout: 5000 });
  }

  /**
   * 断言元素可见且包含文本
   */
  static async assertVisibleWithText(page: Page, selector: string, text: string) {
    const element = page.locator(selector);
    await expect(element).toBeVisible({ timeout: 5000 });
    await expect(element).toContainText(text);
  }

  /**
   * 断言元素不可见
   */
  static async assertNotVisible(page: Page, selector: string) {
    const element = page.locator(selector);
    await expect(element).not.toBeVisible();
  }

  /**
   * 断言分页总数
   */
  static async assertPaginationTotal(page: Page, expectedTotal: number) {
    const totalText = await page.locator('.el-pagination__total').textContent();
    expect(totalText).toContain(String(expectedTotal));
  }

  /**
   * 断言业务数据变化（前后对比）
   */
  static assertDataChanged(before: any, after: any, changedFields: string[]) {
    for (const field of changedFields) {
      const parts = field.split('.');
      let beforeVal: any = before;
      let afterVal: any = after;

      for (const part of parts) {
        beforeVal = beforeVal?.[part];
        afterVal = afterVal?.[part];
      }

      expect(afterVal).not.toEqual(beforeVal);
    }
  }

  /**
   * 软断言 - 多个断言不互相阻断
   */
  static async softAssert(promises: Promise<void>[]) {
    const results = await Promise.allSettled(promises);
    const failures: string[] = [];

    results.forEach((result, index) => {
      if (result.status === 'rejected') {
        failures.push(`断言 #${index + 1} 失败: ${result.reason}`);
      }
    });

    if (failures.length > 0) {
      throw new Error(`软断言失败 (${failures.length} 个):\n${failures.join('\n')}`);
    }
  }
}
