/**
 * 管理后台 - 基础页面类
 *
 * 管理后台采用 iframe 架构：
 * - 主框架页：/backend/index.html（侧边栏 + iframe#cIframe）
 * - 子页面：通过 iframe 加载（如 page/member/list.html）
 *
 * 所有子页面操作都通过 frame 进行。
 */

import { type Page, type Frame, expect } from '@playwright/test';
import { ApiInterceptor } from '../utils/api-interceptor';

export class BasePage {
  readonly page: Page;
  protected frame: Frame | null = null;
  protected interceptor: ApiInterceptor;

  constructor(page: Page) {
    this.page = page;
    this.interceptor = new ApiInterceptor(page);
  }

  /**
   * 导航到指定菜单项
   * 使用 JavaScript 调用 window.menuHandle 切换 iframe 内容
   */
  async navigateTo(menuName: string) {
    // 确保在管理后台首页
    if (!this.page.url().includes('/backend/index.html')) {
      await this.page.goto('/backend/index.html');
      await this.page.waitForSelector('iframe#cIframe', { timeout: 15000 });
      // 等待 Vue 应用加载完成（menuList/menuHandle 暴露到 window）
      await this.page.waitForFunction(() => {
        return typeof (window as any).menuHandle === 'function';
      }, { timeout: 10000 });
      await this.page.waitForTimeout(1000);
    }

    // 如果已经在目标页面，跳过
    const currentIframeSrc = await this.getIframeSrc();
    const expectedPath = this.getExpectedPath(menuName);
    if (currentIframeSrc && expectedPath && currentIframeSrc.includes(expectedPath)) {
      this.frame = await this.getFrame();
      return;
    }

    // 使用 JavaScript 调用 menuHandle 切换菜单
    await this.page.evaluate((name: string) => {
      const menuList = (window as any).menuList;
      const menuHandle = (window as any).menuHandle;
      if (menuList && menuHandle) {
        for (const item of menuList) {
          if (item.children) {
            const sub = item.children.find((s: any) => s.name === name);
            if (sub) { menuHandle(sub, false); return; }
          } else if (item.name === name) {
            menuHandle(item, false); return;
          }
        }
      }
    }, menuName);

    // 等待 iframe 加载新内容
    await this.page.waitForTimeout(3000);

    // 获取 iframe frame
    this.frame = await this.getFrame();

    // 等待 iframe 内部页面加载完成
    if (this.frame) {
      try {
        await this.frame.waitForSelector('body', { timeout: 10000 });
      } catch {}
      await this.page.waitForTimeout(1000);
    }
  }

  /**
   * 获取 iframe 的 Frame 对象
   */
  protected async getFrame(): Promise<Frame> {
    // 等待 iframe src 变化（导航后 src 会更新）
    await this.page.waitForSelector('iframe#cIframe', { timeout: 10000 });

    // 尝试通过 name 获取
    let frame = this.page.frame('cIframe');
    if (frame) return frame;

    // 通过 locator 获取
    const iframeLocator = this.page.locator('iframe#cIframe');
    frame = await iframeLocator.contentFrame();
    if (frame) return frame;

    // 回退：取第一个子 frame（通常是 #cIframe）
    const frames = this.page.frames();
    return frames.find(f => f.name() === 'cIframe') || frames[1]!;
  }

  /**
   * 获取 iframe 当前 src
   */
  protected async getIframeSrc(): Promise<string | null> {
    try {
      const src = await this.page.getAttribute('iframe#cIframe', 'src');
      return src || null;
    } catch {
      return null;
    }
  }

  /**
   * 子类实现：返回菜单对应的 iframe 路径片段
   */
  protected getExpectedPath(_menuName: string): string {
    return '';
  }

  /**
   * 在 iframe 内查找元素（返回主页面 locator 用于 expect）
   */
  protected el(selector: string) {
    if (!this.frame) throw new Error('frame 未初始化，请先调用 navigateTo()');
    return this.frame.locator(selector);
  }

  /**
   * 在 iframe 内断言元素可见（返回 page 级的 expect）
   */
  protected expectInFrame(selector: string) {
    if (!this.frame) throw new Error('frame 未初始化，请先调用 navigateTo()');
    return expect(this.frame.locator(selector));
  }

  /**
   * 等待 iframe 内容加载
   */
  protected async waitForFrameLoad(timeout = 10000) {
    if (!this.frame) return;
    try {
      await this.frame.waitForSelector('body', { timeout });
    } catch {}
    await this.page.waitForTimeout(500);
  }

  /**
   * 获取拦截器中的 API 响应
   */
  protected getApiResponses(urlPattern: string | RegExp) {
    return this.interceptor.getResponses({ urlPattern });
  }

  /**
   * 清除拦截器记录
   */
  protected clearApi() {
    this.interceptor.clear();
  }
}
