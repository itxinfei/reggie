/**
 * 全局环境清理
 *
 * 职责：
 *   1. 清理测试数据
 *   2. 生成 Allure 环境信息
 *   3. 关闭 Allure 生命周期
 */

import { FullConfig } from '@playwright/test';
import * as allure from 'allure-playwright';
import fs from 'fs';
import path from 'path';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

async function globalTeardown(config: FullConfig) {
  console.log('\n========================================');
  console.log('  全局环境清理');
  console.log('========================================\n');

  const startTime = Date.now();

  // 读取环境数据
  const envDataStr = process.env['REGGIE_ENV_DATA'];
  let envData: any = null;
  if (envDataStr) {
    try { envData = JSON.parse(envDataStr); } catch {}
  }

  // 使用 Session Cookie 清理数据
  if (envData?.sessionCookie) {
    const cookie = `JSESSIONID=${envData.sessionCookie.value}`;
    const headers = { 'Cookie': cookie, 'Content-Type': 'application/json' };

    // 删除测试菜品
    if (envData.dishId) {
      try {
        const res = await fetch(`${BASE_URL}/dish/${envData.dishId}`, { method: 'DELETE', headers });
        console.log(`  清理菜品: ${res.ok ? '成功' : '跳过'}`);
      } catch {}
    }

    // 删除测试分类
    if (envData.categoryId) {
      try {
        const res = await fetch(`${BASE_URL}/category/${envData.categoryId}`, { method: 'DELETE', headers });
        console.log(`  清理分类: ${res.ok ? '成功' : '跳过'}`);
      } catch {}
    }

    // 取消测试订单
    if (envData.orderId) {
      try {
        await fetch(`${BASE_URL}/order/cancel/${envData.orderId}`, { method: 'POST', headers });
        console.log(`  取消订单: 完成`);
      } catch {}
    }
  }

  // 生成 Allure 环境信息
  const envPropsPath = path.join(__dirname, '..', 'allure-results', 'environment.properties');
  const envLines = [
    'Browser=Chromium',
    'Platform=' + process.platform,
    'Base.URL=' + BASE_URL,
    'API.URL=' + BASE_URL,
    'Test.Time=' + new Date().toLocaleString('zh-CN'),
  ];
  if (envData) {
    envLines.push(`Employee.ID=${envData.employeeId}`);
    envLines.push(`Tenant.ID=${envData.tenantId}`);
  }
  fs.writeFileSync(envPropsPath, envLines.join('\n'));

  // 写入测试摘要
  await generateTestSummary();

  console.log(`\n  清理完成 (${Date.now() - startTime}ms)`);
  console.log('\n========================================');
  console.log('  测试报告命令：');
  console.log('  npx allure serve ./allure-results');
  console.log('  npx playwright show-report');
  console.log('========================================\n');
}

async function generateTestSummary() {
  const resultsPath = path.join(__dirname, '..', 'allure-results');
  const summaryPath = path.join(__dirname, '..', 'reports', 'test-summary.json');

  let total = 0, passed = 0, failed = 0;
  try {
    const files = fs.readdirSync(resultsPath).filter(f => f.startsWith('result-') && f.endsWith('.json'));
    for (const file of files) {
      const result = JSON.parse(fs.readFileSync(path.join(resultsPath, file), 'utf-8'));
      total++;
      if (result.status === 'passed') passed++;
      else if (result.status === 'failed') failed++;
    }
  } catch {}

  fs.writeFileSync(summaryPath, JSON.stringify({
    total, passed, failed,
    passRate: total > 0 ? ((passed / total) * 100).toFixed(2) + '%' : 'N/A',
    executionTime: new Date().toISOString(),
  }, null, 2));
}

export default globalTeardown;
