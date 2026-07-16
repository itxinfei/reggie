/**
 * Allure 报告辅助工具
 *
 * 使用 allure-playwright v3 的 step API
 * 在 Playwright 测试中使用 step 包裹关键操作
 */

import { type Page, type APIRequestContext } from '@playwright/test';

export class AllureReporter {
  /**
   * 附加 API 请求/响应信息到报告
   */
  static attachApiExchange(
    requestUrl: string,
    requestBody: any,
    responseStatus: number,
    responseBody: any,
    responseTime: number
  ) {
    // allure-playwright v3 使用 test.info() 附加附件
    const content = JSON.stringify({
      request: { url: requestUrl, body: requestBody },
      response: { status: responseStatus, body: responseBody, time: `${responseTime}ms` },
    }, null, 2);
    return content; // 返回内容，由调用方附加
  }

  /**
   * 附加请求参数到报告
   */
  static attachRequestParams(url: string, method: string, headers: Record<string, string>, body?: any) {
    return JSON.stringify({ url, method, headers, body }, null, 2);
  }

  /**
   * 附加响应数据到报告
   */
  static attachResponseData(url: string, status: number, body: any, responseTime: number) {
    return JSON.stringify({ url, status, responseTime: `${responseTime}ms`, body }, null, 2);
  }

  /**
   * 附加测试数据快照
   */
  static attachTestData(label: string, data: Record<string, any>) {
    return JSON.stringify(data, null, 2);
  }

  /**
   * 附加 DOM 快照
   */
  static async attachDomSnapshot(page: Page, label = 'DOM Snapshot') {
    return await page.content();
  }

  /**
   * 附加控制台日志
   */
  static attachConsoleLogs(logs: Array<{ type: string; text: string; timestamp: number }>) {
    return logs
      .map(log => `[${new Date(log.timestamp).toISOString()}] [${log.type}] ${log.text}`)
      .join('\n');
  }
}
