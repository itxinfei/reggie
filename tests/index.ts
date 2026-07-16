/**
 * 统一导出所有测试工具和 Page Object
 */

// Page Objects
export {
  LoginPage,
  EmployeePage,
  DishPage,
  CategoryPage,
  OrderPage,
} from './pages';

// Utils
export {
  ApiInterceptor,
  ApiAssertions,
  type RequestRecord,
  type ResponseRecord,
  type RequestMatchOptions,
} from './utils/api-interceptor';

export {
  getEnvData,
  type TestEnvData,
} from './utils/env-data';

export { default as globalSetup } from './utils/global-setup';
export { default as globalTeardown } from './utils/global-teardown';
