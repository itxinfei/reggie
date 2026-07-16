/**
 * API 请求拦截与验证工具
 *
 * 功能：
 * - 拦截页面中的所有网络请求
 * - 验证请求参数（URL、方法、请求头、请求体）
 * - 验证响应状态码和返回数据
 * - 支持请求等待和断言
 */

import { type Page, type Request, type Response, type APIRequestContext } from '@playwright/test';

// 请求记录
export interface RequestRecord {
  url: string;
  method: string;
  headers: Record<string, string>;
  body?: any;
  timestamp: number;
}

// 响应记录
export interface ResponseRecord {
  url: string;
  status: number;           // HTTP 状态码
  apiCode: number | null;   // API 业务码（如 1=成功）
  statusText: string;
  headers: Record<string, string>;
  body: any;
  requestBody?: any;
  responseTime: number;
}

// 请求匹配条件
export interface RequestMatchOptions {
  urlPattern?: string | RegExp;
  method?: string;
  headers?: Record<string, string>;
}

export class ApiInterceptor {
  private capturedRequests: RequestRecord[] = [];
  private capturedResponses: ResponseRecord[] = [];
  private pendingRequests: Map<string, Request> = new Map();
  private pendingResponses: Map<string, Response> = new Map();
  private page: Page;

  constructor(page: Page) {
    this.page = page;
    this.setupInterceptors();
  }

  /**
   * 设置请求/响应拦截器
   */
  private setupInterceptors() {
    // 拦截请求
    this.page.on('request', (request) => {
      const requestRecord: RequestRecord = {
        url: request.url(),
        method: request.method(),
        headers: request.headers() as Record<string, string>,
        timestamp: Date.now(),
      };

      // 尝试获取请求体
      const postData = request.postData();
      if (postData && request.headers()['content-type']?.includes('application/json')) {
        try { requestRecord.body = JSON.parse(postData); } catch {}
      }

      this.capturedRequests.push(requestRecord);
      this.pendingRequests.set(requestRecord.url, request);
    });

    // 拦截响应
    this.page.on('response', async (response) => {
      const request = response.request();
      const responseRecord: ResponseRecord = {
        url: response.url(),
        status: response.status(),
        apiCode: null,
        statusText: response.statusText(),
        headers: response.headers() as Record<string, string>,
        body: null,
        responseTime: Date.now(),
      };

      // 获取请求体
      const reqRecord = this.capturedRequests.find(r => r.url === response.url());
      responseRecord.requestBody = reqRecord?.body;

      // 获取响应体并提取 API code
      try {
        const contentType = response.headers()['content-type'] || '';
        if (contentType.includes('application/json')) {
          const jsonBody = await response.json();
          responseRecord.body = jsonBody;
          responseRecord.apiCode = typeof jsonBody.code === 'number' ? jsonBody.code : (jsonBody.code === '1' ? 1 : 0);
        } else {
          responseRecord.body = await response.text();
        }
      } catch {
        responseRecord.body = null;
      }

      this.capturedResponses.push(responseRecord);
      this.pendingResponses.set(responseRecord.url, response);
    });
  }

  /**
   * 等待并验证 API 请求
   */
  async waitForApiRequest(options: RequestMatchOptions & { timeout?: number }): Promise<ResponseRecord> {
    const { timeout = 10000 } = options;
    const startTime = Date.now();

    // 先检查是否已存在匹配的请求
    const existing = this.findMatchingResponse(options);
    if (existing) return existing;

    // 等待新请求
    while (Date.now() - startTime < timeout) {
      await this.page.waitForTimeout(200);

      const match = this.findMatchingResponse(options);
      if (match) return match;
    }

    throw new Error(`等待 API 请求超时 (${timeout}ms): ${JSON.stringify(options)}`);
  }

  /**
   * 查找匹配的响应
   */
  private findMatchingResponse(options: RequestMatchOptions): ResponseRecord | null {
    for (let i = this.capturedResponses.length - 1; i >= 0; i--) {
      const resp = this.capturedResponses[i];

      if (options.urlPattern) {
        const pattern = typeof options.urlPattern === 'string'
          ? new RegExp(options.urlPattern.replace(/\*/g, '.*'))
          : options.urlPattern;
        if (!pattern.test(resp.url)) continue;
      }

      if (options.method) {
        const req = this.capturedRequests.find(r => r.url === resp.url);
        if (req?.method !== options.method.toUpperCase()) continue;
      }

      return resp;
    }
    return null;
  }

  /**
   * 获取所有匹配的请求
   */
  getRequests(options: RequestMatchOptions): RequestRecord[] {
    return this.capturedRequests.filter(req => {
      if (options.urlPattern) {
        const pattern = typeof options.urlPattern === 'string'
          ? new RegExp(options.urlPattern.replace(/\*/g, '.*'))
          : options.urlPattern;
        if (!pattern.test(req.url)) return false;
      }
      if (options.method && req.method !== options.method.toUpperCase()) return false;
      return true;
    });
  }

  /**
   * 获取所有匹配的响应
   */
  getResponses(options: RequestMatchOptions): ResponseRecord[] {
    return this.capturedResponses.filter(resp => {
      if (options.urlPattern) {
        const pattern = typeof options.urlPattern === 'string'
          ? new RegExp(options.urlPattern.replace(/\*/g, '.*'))
          : options.urlPattern;
        if (!pattern.test(resp.url)) return false;
      }
      if (options.method) {
        const req = this.capturedRequests.find(r => r.url === resp.url);
        if (req?.method !== options.method.toUpperCase()) return false;
      }
      return true;
    });
  }

  /**
   * 清除捕获的请求/响应记录
   */
  clear() {
    this.capturedRequests = [];
    this.capturedResponses = [];
    this.pendingRequests.clear();
    this.pendingResponses.clear();
  }

  /**
   * 获取请求计数
   */
  getRequestCount(options?: RequestMatchOptions): number {
    return options ? this.getRequests(options).length : this.capturedRequests.length;
  }

  /**
   * 获取响应计数
   */
  getResponseCount(options?: RequestMatchOptions): number {
    return options ? this.getResponses(options).length : this.capturedResponses.length;
  }
}

/**
 * API 断言工具
 */
export class ApiAssertions {
  /**
   * 断言响应状态码
   */
  static assertStatus(response: ResponseRecord, expectedStatus: number) {
    if (response.status !== expectedStatus) {
      throw new Error(
        `状态码断言失败: 期望 ${expectedStatus}, 实际 ${response.status}\n` +
        `URL: ${response.url}\n` +
        `响应体: ${JSON.stringify(response.body, null, 2)}`
      );
    }
  }

  /**
   * 断言 API 业务码为成功（code=1）
   */
  static assertApiSuccess(response: ResponseRecord) {
    const code = response.apiCode ?? response.status;
    if (code !== 1) {
      throw new Error(
        `API 断言失败: 期望 code=1, 实际 code=${code}\n` +
        `URL: ${response.url}\n` +
        `响应体: ${JSON.stringify(response.body, null, 2)}`
      );
    }
  }

  /**
   * 断言响应体包含指定字段
   */
  static assertHasField(response: ResponseRecord, fieldPath: string) {
    const parts = fieldPath.split('.');
    let current: any = response.body;

    for (const part of parts) {
      if (current === null || current === undefined || !(part in current)) {
        throw new Error(
          `字段断言失败: 响应体不包含字段 "${fieldPath}"\n` +
          `URL: ${response.url}\n` +
          `响应体: ${JSON.stringify(response.body, null, 2)}`
        );
      }
      current = current[part];
    }
  }

  /**
   * 断言响应体字段值
   */
  static assertFieldValue(response: ResponseRecord, fieldPath: string, expectedValue: any) {
    const parts = fieldPath.split('.');
    let current: any = response.body;

    for (const part of parts) {
      if (current === null || current === undefined || !(part in current)) {
        throw new Error(
          `字段值断言失败: 字段 "${fieldPath}" 不存在\n` +
          `URL: ${response.url}\n` +
          `响应体: ${JSON.stringify(response.body, null, 2)}`
        );
      }
      current = current[part];
    }

    if (current !== expectedValue) {
      throw new Error(
        `字段值断言失败: "${fieldPath}" 期望 ${JSON.stringify(expectedValue)}, 实际 ${JSON.stringify(current)}\n` +
        `URL: ${response.url}\n` +
        `响应体: ${JSON.stringify(response.body, null, 2)}`
      );
    }
  }

  /**
   * 断言响应体数组长度
   */
  static assertArrayLength(response: ResponseRecord, expectedLength: number, arrayPath = 'data') {
    const parts = arrayPath.split('.');
    let current: any = response.body;

    for (const part of parts) {
      if (current === null || current === undefined || !(part in current)) {
        throw new Error(
          `数组长度断言失败: 路径 "${arrayPath}" 不存在\n` +
          `URL: ${response.url}\n` +
          `响应体: ${JSON.stringify(response.body, null, 2)}`
        );
      }
      current = current[part];
    }

    if (!Array.isArray(current) || current.length !== expectedLength) {
      throw new Error(
        `数组长度断言失败: "${arrayPath}" 期望长度 ${expectedLength}, 实际 ${Array.isArray(current) ? current.length : '非数组'}\n` +
        `URL: ${response.url}\n` +
        `响应体: ${JSON.stringify(response.body, null, 2)}`
      );
    }
  }

  /**
   * 断言响应体包含字符串
   */
  static assertBodyContains(response: ResponseRecord, expected: string) {
    const bodyStr = typeof response.body === 'string' ? response.body : JSON.stringify(response.body);
    if (!bodyStr.includes(expected)) {
      throw new Error(
        `响应体包含断言失败: 期望包含 "${expected}"\n` +
        `URL: ${response.url}\n` +
        `响应体: ${bodyStr}`
      );
    }
  }

  /**
   * 断言请求体包含指定字段
   */
  static assertRequestBodyContains(request: RequestRecord, fieldPath: string) {
    if (!request.body) {
      throw new Error(`请求体断言失败: 请求无请求体\nURL: ${request.url}`);
    }

    const parts = fieldPath.split('.');
    let current: any = request.body;

    for (const part of parts) {
      if (current === null || current === undefined || !(part in current)) {
        throw new Error(
          `请求体字段断言失败: 请求不包含字段 "${fieldPath}"\n` +
          `URL: ${request.url}\n` +
          `请求体: ${JSON.stringify(request.body, null, 2)}`
        );
      }
      current = current[part];
    }
  }
}
