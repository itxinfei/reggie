/**
 * 订单管理 Page Object
 *
 * 实际路径：/backend/page/order/list.html（通过 iframe 加载）
 */

import { type Page } from '@playwright/test';
import { BasePage } from './base.page';
import { ApiInterceptor, ApiAssertions } from '../utils/api-interceptor';

export interface OrderFilters {
  number?: string;
  phone?: string;
  status?: string;
  beginTime?: string;
  endTime?: string;
}

export class OrderPage extends BasePage {
  readonly selectors = {
    // 搜索输入框
    numberInput: 'input[placeholder*="订单号"]',
    phoneInput: 'input[placeholder*="手机号"]',

    // 按钮
    queryButton: 'button:has-text("查询")',
    resetButton: 'button:has-text("重置")',
    detailButton: 'button:has-text("查看"), a:has-text("查看")',

    // 表格
    tableRows: '.el-table__body tr',

    // 详情弹窗
    detailDialog: '.el-dialog__body, .order-detail-dialog',

    // 分页
    pagination: '.el-pagination',
    totalText: '.el-pagination__total',
  };

  constructor(page: Page) {
    super(page);
  }

  protected getExpectedPath(): string {
    return 'page/order/list.html';
  }

  async goto() {
    await this.navigateTo('订单明细');
    await this.waitForFrameLoad(10000);
  }

  /**
   * 筛选订单
   */
  async filter(filters: OrderFilters) {
    this.clearApi();

    if (filters.number) {
      const input = this.el(this.selectors.numberInput);
      if (await input.count() > 0) {
        await input.first().fill(filters.number);
      }
    }
    if (filters.phone) {
      const input = this.el(this.selectors.phoneInput);
      if (await input.count() > 0) {
        await input.first().fill(filters.phone);
      }
    }
    if (filters.status) {
      // 点击状态筛选卡片
      const card = this.el(`stat-cards .stat-card:has-text("${filters.status}")`);
      if (await card.count() > 0) {
        await card.click();
      }
    }

    // 点击查询
    const queryBtn = this.el(this.selectors.queryButton);
    if (await queryBtn.count() > 0) {
      await queryBtn.click();
    }

    await this.page.waitForTimeout(1000);
    return this.getApiResponses(/\/order\/page/);
  }

  /**
   * 重置筛选
   */
  async reset() {
    const resetBtn = this.el(this.selectors.resetButton);
    if (await resetBtn.count() > 0) {
      await resetBtn.click();
      await this.page.waitForTimeout(500);
    }
  }

  /**
   * 查看订单详情
   */
  async viewDetail(orderId: string | number) {
    this.clearApi();

    const row = this.el(`.el-table__body tr:has-text("${orderId}")`);
    if (await row.count() > 0) {
      await row.locator(this.selectors.detailButton).first().click();
    }

    // 等待详情弹窗
    await this.page.waitForTimeout(1000);

    // 验证详情弹窗
    const detailEl = this.el(this.selectors.detailDialog);
    if (await detailEl.count() > 0) {
      await expect(detailEl).toBeVisible({ timeout: 5000 });
    }

    const responses = this.getApiResponses(new RegExp(`/order.*${orderId}`));
    return responses;
  }

  /**
   * 获取行数
   */
  async getRowCount(): Promise<number> {
    return await this.el(this.selectors.tableRows).count();
  }

  /**
   * 验证订单存在
   */
  async assertOrderExists(orderNumber: string) {
    const row = this.el(`.el-table__body tr:has-text("${orderNumber}")`);
    await expect(row).toBeVisible({ timeout: 5000 });
  }
}
