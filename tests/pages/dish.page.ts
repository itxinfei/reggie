/**
 * 菜品管理 Page Object
 *
 * 实际路径：/backend/page/food/list.html（通过 iframe 加载）
 *
 * 页面结构：
 * - <stat-cards> 统计卡片
 * - <table-bar> 搜索栏 + 操作按钮（新建菜品/批量操作/导出）
 * - <crud-table> 数据表格（名称/图片/分类/售价/状态/库存/最后操作时间/操作）
 * - <el-dialog> 新增/修改菜品弹窗（含分类/价格/库存/口味/图片上传）
 */

import { type Page } from '@playwright/test';
import { BasePage } from './base.page';
import { ApiInterceptor, ApiAssertions } from '../utils/api-interceptor';

export class DishPage extends BasePage {
  readonly selectors = {
    // 操作按钮
    addButton: 'button:has-text("+ 新建菜品")',
    exportButton: 'button:has-text("导出")',

    // 表格行
    tableRows: '.el-table__body tr',

    // 搜索输入框
    searchInput: 'input[placeholder*="菜品名称"]',

    // 弹窗
    dialog: '.el-dialog',
    dialogTitle: '.el-dialog__title',

    // 菜品表单字段
    nameInput: 'input[placeholder="请填写菜品名称"]',
    codeInput: 'input[placeholder="请输入商品码"]',
    categorySelect: '.el-select',
    priceInput: 'input[placeholder="请设置菜品价格"]',
    sortInput: '.el-input-number__input',
    stockInput: 'input[placeholder*="库存"]',
    minStockInput: 'input[placeholder*="最低库存"]',
    descriptionInput: 'textarea[placeholder*="菜品描述"]',
    imageUpload: '.avatar-uploader',
    uploadInput: 'input[type="file"]',

    // 口味配置
    addFlavorButton: 'button:has-text("添加口味配置")',
    flavorNameInput: 'input[placeholder*="口味名称"]',
    flavorTagInput: 'input[placeholder*="添加标签"]',
    flavorTagConfirm: 'button:has-text("添加标签")',

    // 弹窗底部按钮
    confirmButton: 'button:has-text("确 定")',

    // 操作列按钮
    editButton: 'button:has-text("修改")',
    deleteButton: 'button:has-text("删除")',
    statusButton: 'button:has-text("启售"), button:has-text("停售")',
    stockButton: 'button:has-text("库存")',
  };

  constructor(page: Page) {
    super(page);
  }

  protected getExpectedPath(): string {
    return 'page/food/list.html';
  }

  async goto() {
    await this.navigateTo('菜品管理');
    await this.waitForFrameLoad(10000);
  }

  /**
   * 点击新建菜品
   */
  async openAddDialog() {
    this.clearApi();

    const addBtn = this.el(this.selectors.addButton);
    await addBtn.click();
    await this.page.waitForTimeout(500);

    // 等待弹窗出现
    await this.el(this.selectors.dialog).waitFor({ state: 'visible', timeout: 5000 });
    await this.el('input[placeholder="请填写菜品名称"]').waitFor({ state: 'visible', timeout: 5000 });
  }

  /**
   * 填写菜品基本信息（不含口味和图片）
   */
  async fillBasicInfo(data: {
    name: string;
    code?: string;
    categoryId: string;
    price: string;
    sort?: number;
    stockQty?: number;
    minStock?: number;
    description?: string;
  }) {
    await this.el(this.selectors.nameInput).fill(data.name);

    if (data.code) {
      await this.el(this.selectors.codeInput).fill(data.code);
    }

    // 选择分类
    await this.el(this.selectors.categorySelect).click();
    await this.page.waitForSelector('.el-select-dropdown__item', { timeout: 3000 }).catch(() => {});
    const option = this.el(`.el-select-dropdown__item:has-text("${data.categoryId}")`);
    if (await option.count() > 0) {
      await option.click();
    }

    // 填写价格
    await this.el(this.selectors.priceInput).fill(data.price);

    // 排序
    if (data.sort !== undefined) {
      await this.el(this.selectors.sortInput).fill(String(data.sort));
    }

    // 库存
    if (data.stockQty !== undefined) {
      const stockInput = this.el(this.selectors.stockInput);
      if (await stockInput.count() > 0) {
        await stockInput.fill(String(data.stockQty));
      }
    }

    // 最低库存预警
    if (data.minStock !== undefined) {
      const minInput = this.el(this.selectors.minStockInput);
      if (await minInput.count() > 0) {
        await minInput.fill(String(data.minStock));
      }
    }

    // 描述
    if (data.description) {
      await this.el(this.selectors.descriptionInput).fill(data.description);
    }
  }

  /**
   * 添加口味
   */
  async addFlavor(name: string, tags: string[]) {
    // 点击"添加口味配置"
    const addBtn = this.el(this.selectors.addFlavorButton);
    if (await addBtn.count() > 0) {
      await addBtn.click();
    }

    // 填写口味名称
    const nameInput = this.el(this.selectors.flavorNameInput);
    if (await nameInput.count() > 0) {
      await nameInput.last().fill(name);
    }

    // 添加标签
    for (const tag of tags) {
      const tagInput = this.el('.el-input--mini input[placeholder*="添加标签"], .flavor-tags-dialog input');
      if (await tagInput.count() > 0) {
        await tagInput.last().fill(tag);
        await tagInput.last().press('Enter');
        await this.page.waitForTimeout(300);
      }
    }
  }

  /**
   * 提交菜品表单
   */
  async submitDish() {
    this.clearApi();

    await this.el(this.selectors.confirmButton).click();

    // 等待弹窗关闭
    await this.el(this.selectors.dialog).waitFor({ state: 'hidden', timeout: 10000 });
    await this.page.waitForTimeout(1000);

    // 验证 API
    const responses = this.getApiResponses(/\/food|\/dish/);
    const successResp = responses.find(r => {
      const body = r.body;
      return body && (body.code === 1 || body.code === '1');
    });

    if (successResp) {
      ApiAssertions.assertApiSuccess(successResp);
    }

    return responses[responses.length - 1];
  }

  /**
   * 完整新增菜品流程
   */
  async addDish(data: {
    name: string;
    code?: string;
    categoryId: string;
    price: string;
    description?: string;
    flavors?: Array<{ name: string; tags: string[] }>;
  }) {
    await this.openAddDialog();
    await this.fillBasicInfo(data);

    if (data.flavors) {
      for (const f of data.flavors) {
        await this.addFlavor(f.name, f.tags);
      }
    }

    return this.submitDish();
  }

  /**
   * 搜索菜品
   */
  async search(name: string) {
    this.clearApi();

    const searchInput = this.el(this.selectors.searchInput);
    if (await searchInput.count() > 0) {
      await searchInput.first().fill(name);
    }

    // 点击查询
    const queryBtn = this.el('button:has-text("查询")');
    if (await queryBtn.count() > 0) {
      await queryBtn.click();
    }

    await this.page.waitForTimeout(1000);
    return this.getApiResponses(/\/food\/page/);
  }

  /**
   * 获取表格行数
   */
  async getRowCount(): Promise<number> {
    return await this.el(this.selectors.tableRows).count();
  }

  /**
   * 验证菜品存在
   */
  async assertDishExists(name: string) {
    const row = this.el(`.el-table__body tr:has-text("${name}")`);
    await expect(row).toBeVisible({ timeout: 5000 });
  }

  /**
   * 验证弹窗可见
   */
  async assertDialogVisible(title?: string) {
    await expect(this.el(this.selectors.dialog)).toBeVisible({ timeout: 5000 });
    if (title) {
      await expect(this.el(this.selectors.dialogTitle)).toContainText(title);
    }
  }
}
