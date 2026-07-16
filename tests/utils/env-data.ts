/**
 * 测试环境数据读取
 * 从 global-setup 写入的环境变量中读取
 */

export interface TestEnvData {
  sessionCookie: { name: string; value: string; domain: string } | null;
  employeeId: number;
  tenantId: string;
  testTimestamp: string;
}

export function getEnvData(): TestEnvData {
  const raw = process.env['REGGIE_ENV_DATA'];
  if (!raw) {
    throw new Error('测试环境数据未初始化');
  }
  return JSON.parse(raw) as TestEnvData;
}
