/**
 * 员工管理 Page Object
 *
 * 实际路径：/backend/page/member/list.html（通过 iframe 加载）
 *
 * 页面结构（通过 iframe）：
 * - <stat-cards> 统计卡片（员工总数/在职/已禁用/本月新入职）
 * - <table-bar> 搜索栏 + 操作按钮（添加员工/导出）
 * - <crud-table> 数据表格（姓名/账号/手机号/状态/操作）
 * - <el-dialog> 新增/修改员工弹窗
 */

import { type Page } from '@playwright/test';
import { BasePage } from './base.page';
import { ApiInterceptor, ApiAssertions } from '../utils/api-interceptor';

export class EmployeePage extends BasePage {
  readonly selectors = {
    // table-bar 操作按钮
    addButton: '.btn-add, button:has-text("+ 添加员工"), .el-button--primary:has-text("添加")',
    exportButton: 'button:has-text("导出")',

    // 搜索（table-bar 内部）
    searchInput: 'input[placeholder*="员工姓名"], input[placeholder*="请输入"]',

    // crud-table
    tableRows: '.el-table__body tr.el-table__row, .el-table__body tr',
    tableBody: '.el-table__body',

    // 弹窗
    dialog: '.el-dialog',
    dialogTitle: '.el-dialog__title',
    closeDialogButton: '.el-dialog__close, button:has-text("取 消")',

    // 表单字段（在 dialog 内）
    usernameInput: 'input[placeholder="请输入账号"]',
    nameInput: 'input[placeholder="请输入员工姓名"]',
    phoneInput: 'input[placeholder="请输入手机号"]',
    maleRadio: '.el-radio:has(span:has-text("男"))',
    femaleRadio: '.el-radio:has(span:has-text("女"))',
    idNumberInput: 'input[placeholder="请输入身份证号"]',

    // 弹窗底部按钮
    confirmButton: 'button:has-text("确 定"), .el-button--primary:has-text("确 定")',

    // 操作列按钮
    editButton: '.btn-view:has-text("编辑")',
    disableButton: '.btn-delete:has-text("禁用")',
    enableButton: '.btn-delete:has-text("启用")',

    // 统计卡片
    statCards: 'stat-cards .stat-card',

    // 分页
    pagination: '.el-pagination',
    totalText: '.el-pagination__total',
  };

  constructor(page: Page) {
    super(page);
  }

  protected getExpectedPath(): string {
    return 'page/member/list.html';
  }

  /**
   * 导航到员工管理页
   */
  async goto() {
    await this.navigateTo('员工管理');
    await this.waitForFrameLoad(10000);
  }

  /**
   * 点击添加员工按钮，打开弹窗
   */
  async openAddDialog() {
    this.clearApi();

    // 点击"添加员工"按钮（在 table-bar 的 actions 中）
    const addBtn = this.el('button:has-text("+ 添加员工"), .btn-add');
    await addBtn.click();
    await this.page.waitForTimeout(500);

    // 等待弹窗出现
    await this.el(this.selectors.dialog).waitFor({ state: 'visible', timeout: 5000 });

    // 等待弹窗内表单渲染
    await this.el('input[placeholder="请输入账号"]').waitFor({ state: 'visible', timeout: 5000 });
  }

  /**
   * 填写并提交员工表单
   */
  async submitEmployee(data: {
    name: string;
    username: string;
    phone: string;
    sex: '男' | '女';
    idNumber?: string;
  }) {
    // 填写表单
    await this.el(this.selectors.nameInput).fill(data.name);
    await this.el(this.selectors.usernameInput).fill(data.username);
    await this.el(this.selectors.phoneInput).fill(data.phone);

    // 选择性别
    const sexRadio = data.sex === '男' ? this.el(this.selectors.maleRadio) : this.el(this.selectors.femaleRadio);
    if (await sexRadio.count() > 0) {
      await sexRadio.click();
    }

    if (data.idNumber) {
      await this.el(this.selectors.idNumberInput).fill(data.idNumber);
    }

    // 清除之前的 API 记录，只捕获本次提交
    this.clearApi();

    // 点击确定
    await this.el(this.selectors.confirmButton).click();

    // 等待弹窗关闭
    await this.el(this.selectors.dialog).waitFor({ state: 'hidden', timeout: 10000 });

    // 等待表格刷新
    await this.page.waitForTimeout(1000);

    // 验证 API 响应
    const responses = this.getApiResponses(/\/employee/);
    const addResponse = responses.find(r => {
      const body = r.body;
      return body && (body.code === 1 || body.code === '1');
    });

    if (addResponse) {
      ApiAssertions.assertApiSuccess(addResponse);
    }

    return responses[responses.length - 1];
  }

  /**
   * 新增员工（完整流程）
   */
  async addEmployee(data: {
    name: string;
    username: string;
    phone: string;
    sex: '男' | '女';
    idNumber?: string;
  }) {
    await this.openAddDialog();
    return this.submitEmployee(data);
  }

  /**
   * 搜索员工
   */
  async search(name: string) {
    this.clearApi();

    // 在 table-bar 的搜索输入框中输入
    const searchInputs = this.el('input[placeholder*="员工姓名"], input[placeholder*="请输入"]');
    if (await searchInputs.count() > 0) {
      await searchInputs.first().fill(name);
    }

    // 点击查询按钮
    const searchBtn = this.el('button:has-text("查询")');
    if (await searchBtn.count() > 0) {
      await searchBtn.click();
    }

    await this.page.waitForTimeout(1000);

    const responses = this.getApiResponses(/\/member\/page|\/employee\/page/);
    return responses;
  }

  /**
   * 重置搜索
   */
  async resetSearch() {
    const resetBtn = this.el('button:has-text("重置")');
    if (await resetBtn.count() > 0) {
      await resetBtn.click();
      await this.page.waitForTimeout(500);
    }
  }

  /**
   * 获取表格行数
   */
  async getRowCount(): Promise<number> {
    return await this.el(this.selectors.tableRows).count();
  }

  /**
   * 验证表格包含指定员工姓名
   */
  async assertEmployeeExists(name: string) {
    const row = this.el(`.el-table__body tr:has-text("${name}")`);
    await expect(row).toBeVisible({ timeout: 5000 });
  }

  /**
   * 验证表格不包含指定员工姓名
   */
  async assertEmployeeNotExists(name: string) {
    const row = this.el(`.el-table__body tr:has-text("${name}")`);
    await expect(row).not.toBeVisible();
  }

  /**
   * 验证员工总数
   */
  async assertTotalCount(expected: number) {
    const totalEl = this.el('.el-pagination__total');
    if (await totalEl.count() > 0) {
      const text = await totalEl.textContent();
      expect(text).toContain(String(expected));
    }
  }

  /**
   * 验证弹窗存在
   */
  async assertDialogVisible(title?: string) {
    await expect(this.el(this.selectors.dialog)).toBeVisible({ timeout: 5000 });
    if (title) {
      await expect(this.el(this.selectors.dialogTitle)).toContainText(title);
    }
  }

  /**
   * 验证弹窗隐藏
   */
  async assertDialogHidden() {
    await expect(this.el(this.selectors.dialog)).not.toBeVisible();
  }
}
