/**
 * 管理后台 - 分类管理 Page Object
 *
 * 对应页面：backend/page/category/category.html
 */

import { type Page, expect } from '@playwright/test';
import { ApiInterceptor, ApiAssertions } from '../utils/api-interceptor';

export class CategoryPage {
  readonly page: Page;
  private interceptor: ApiInterceptor;

  readonly selectors = {
    addCategoryButton: 'button:has-text("新增分类")',
    dialog: '.el-dialog',
    dialogTitle: '.el-dialog__title',
    saveButton: 'button:has-text("保存")',
    cancelButton: 'button:has-text("取消")',
    deleteButton: 'button:has-text("删除")',
    editButton: 'button:has-text("修改")',
    nameInput: 'input[placeholder="请输入菜品分类名称"]',
    typeSelect: '.el-select',
    sortInput: 'input[placeholder*="排序"], input[placeholder*="序号"]',
    tableRows: '.el-table__body tr.el-table__row',
    confirmDialog: '.el-message-box',
    confirmButton: 'button:has-text("确定")',
  };

  constructor(page: Page) {
    this.page = page;
    this.interceptor = new ApiInterceptor(page);
  }

  async goto() {
    await this.page.goto('/backend/page/category/category.html');
    await this.page.waitForSelector(this.selectors.tableRows, { timeout: 10000 });
  }

  /**
   * 新增分类
   */
  async addCategory(name: string, type: number = 1, sort: number = 0) {
    this.interceptor.clear();

    await this.page.click(this.selectors.addCategoryButton);
    await this.page.waitForSelector(this.selectors.dialog, { timeout: 5000 });

    await this.page.fill(this.selectors.nameInput, name);

    if (type > 0) {
      await this.page.click(this.selectors.typeSelect);
      await this.page.waitForSelector('.el-select-dropdown__item', { timeout: 3000 });
      const label = type === 1 ? '菜品分类' : '套餐分类';
      await this.page.locator(`.el-select-dropdown__item:has-text("${label}")`).click();
    }

    if (sort > 0) {
      const sortInput = this.page.locator(this.selectors.sortInput).first();
      if (await sortInput.count() > 0) {
        await sortInput.fill(String(sort));
      }
    }

    this.interceptor.clear();
    await this.page.click(this.selectors.saveButton);
    await this.page.waitForSelector(this.selectors.dialog, { state: 'hidden', timeout: 5000 });

    const responses = this.interceptor.getResponses({ urlPattern: '*/category' });
    const response = responses[responses.length - 1];
    if (response) {
      ApiAssertions.assertStatus(response, 1);
    }

    return response;
  }

  /**
   * 编辑分类
   */
  async editCategory(oldName: string, newName: string) {
    const row = this.page.locator(`.el-table__body tr:has-text("${oldName}")`);
    await row.locator(this.selectors.editButton).click();

    await this.page.waitForSelector(this.selectors.dialog, { timeout: 5000 });

    const nameInput = this.page.locator(this.selectors.nameInput).first();
    if (await nameInput.count() > 0) {
      await nameInput.clear();
      await nameInput.fill(newName);
    }

    this.interceptor.clear();
    await this.page.click(this.selectors.saveButton);
    await this.page.waitForSelector(this.selectors.dialog, { state: 'hidden', timeout: 5000 });

    const responses = this.interceptor.getResponses({ urlPattern: '*/category' });
    return responses[responses.length - 1];
  }

  /**
   * 删除分类
   */
  async deleteCategory(name: string) {
    this.interceptor.clear();

    const row = this.page.locator(`.el-table__body tr:has-text("${name}")`);
    await row.locator(this.selectors.deleteButton).click();

    // 等待确认弹窗
    await this.page.waitForSelector(this.selectors.confirmDialog, { timeout: 3000 });
    await this.page.click(this.selectors.confirmButton);

    await this.page.waitForSelector(this.selectors.confirmDialog, { state: 'hidden', timeout: 5000 });

    const responses = this.interceptor.getResponses({ urlPattern: '*/category' });
    return responses;
  }

  /**
   * 验证分类存在
   */
  async assertCategoryExists(name: string) {
    const row = this.page.locator(`.el-table__body tr:has-text("${name}")`);
    await expect(row).toBeVisible({ timeout: 5000 });
  }

  /**
   * 验证分类不存在
   */
  async assertCategoryNotExists(name: string) {
    const row = this.page.locator(`.el-table__body tr:has-text("${name}")`);
    await expect(row).not.toBeVisible();
  }
}
