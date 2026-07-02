# CSRF 防护配置指南

## 📖 目录

- [CSRF 简介](#csrf-简介)
- [配置说明](#配置说明)
- [前端集成](#前端集成)
- [API 调用](#api-调用)
- [测试验证](#测试验证)
- [常见问题](#常见问题)

---

## CSRF 简介

### 什么是 CSRF

CSRF（Cross-Site Request Forgery，跨站请求伪造）是一种常见的 Web 攻击手段。攻击者诱导已登录的用户访问恶意网站，在用户不知情的情况下，以用户的身份向目标网站发送请求，从而执行非预期的操作。

### 防护原理

CSRF 防护的核心思想是：**每个请求必须携带一个随机生成的 Token，服务器验证 Token 的有效性**。

1. 服务器生成 CSRF Token
2. Token 存储到 Session、Cookie 和响应头中
3. 前端从 Cookie 读取 Token
4. 请求时携带 Token（请求头或参数）
5. 服务器验证 Token

**本项目的轻量级实现特点**：
- ✅ 不依赖 Spring Security
- ✅ 自动为 GET 请求生成/刷新 Token
- ✅ POST/PUT/DELETE 请求自动验证 Token
- ✅ 测试环境自动禁用，不影响测试
- ✅ Redis 可选的优雅降级

---

## 配置说明

### 后端配置

CSRF 防护已自动配置，无需手动启用。以下是核心组件说明：

#### 1. CsrfTokenUtil - Token 生成工具

**位置：** `src/main/java/com/reggie/common/CsrfTokenUtil.java`

**功能**：
- `generateToken()` - 生成安全的随机 Token（32字节随机数 + 时间戳）
- `validateToken()` - 验证 Token 是否匹配
- `extractTimestamp()` - 提取 Token 时间戳
- `isTokenNotExpired()` - 检查 Token 是否过期（默认1小时）

**Token 格式**：
```
Base64(32字节随机数 + 时间戳毫秒值)
```

#### 2. CsrfFilter - CSRF 过滤器

**位置：** `src/main/java/com/reggie/filter/CsrfFilter.java`

**拦截策略**：
- **GET 请求**：自动生成或刷新 CSRF Token，存入 Session、响应头和 Cookie
- **POST/PUT/DELETE 请求**：验证 CSRF Token
- **排除路径**：无需验证的路径

**排除路径配置**：
```java
private static final String[] EXCLUDED_PATHS = {
    "/actuator/**",      // 监控端点
    "/backend/**",       // 后台管理界面
    "/front/**",         // 前端页面
    "/common/upload",    // 文件上传
    "/common/download",  // 文件下载
    "/csrf/**",          // CSRF 相关接口
    "/user/sendMsg",     // 发送短信验证码
    "/user/login",       // 用户登录
    "/employee/login"    // 员工登录
};
```

**Token 存储**：
- **Session**：`HttpSession.CSRF_TOKEN_SESSION_ATTR`
- **Cookie**：`XSRF-TOKEN`（非 HttpOnly，前端可读取）
- **响应头**：`X-CSRF-TOKEN`

#### 3. FilterConfig - 过滤器注册

**位置：** `src/main/java/com/reggie/config/FilterConfig.java`

**注册配置**：
```java
@Bean
@Profile("!test")  // 非测试环境启用
public FilterRegistrationBean<CsrfFilter> csrfFilterRegistration() {
    FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new CsrfFilter());
    registration.addUrlPatterns("/*");
    registration.setName("csrfFilter");
    registration.setOrder(2); // 顺序：1=LoginCheckFilter, 2=CsrfFilter
    return registration;
}
```

**测试环境处理**：
- 使用 `@Profile("!test")` 注解，测试环境自动禁用 CSRF 过滤器
- 避免 CSRF 干扰单元测试和集成测试

#### 4. SessionTimeoutConfig - Session 超时配置

**位置：** `src/main/java/com/reggie/config/SessionTimeoutConfig.java`

**配置**：
```java
// Session 超时时间（30分钟）
servletContext.setSessionTimeout(SecurityConstants.SESSION_TIMEOUT / 60);

// 禁用 URL 重写，防止 Session ID 泄露
servletContext.setSessionTrackingModes(
    Collections.singleton(SessionTrackingMode.COOKIE)
);
```

---

## 前端集成

### 1. 自动读取 CSRF Token

**方式一：从 Cookie 读取（推荐）**

```javascript
// 获取 Cookie 中的 CSRF Token
function getCsrfToken() {
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}
```

**方式二：从响应头读取**

```javascript
// 在 AJAX 请求拦截器中从响应头获取
axios.interceptors.response.use(response => {
    const csrfToken = response.headers['x-csrf-token'];
    if (csrfToken) {
        // 保存 Token 到 Cookie 或 LocalStorage
        document.cookie = `XSRF-TOKEN=${encodeURIComponent(csrfToken)}; path=/`;
    }
    return response;
});
```

### 2. 发送请求时携带 Token

**方式一：请求头（推荐）**

```javascript
// Axios 拦截器自动添加 CSRF Token
axios.interceptors.request.use(config => {
    const token = getCsrfToken();
    if (token) {
        config.headers['X-CSRF-TOKEN'] = token;
    }
    return config;
});
```

**方式二：请求参数**

```javascript
// 在 POST/PUT/DELETE 请求中添加 _csrf 参数
const params = {
    username: 'admin',
    password: '123456',
    _csrf: getCsrfToken()  // 添加 CSRF Token
};
```

### 3. 完整示例（Vue + Axios）

```javascript
// main.js
import axios from 'axios';

// 请求拦截器
axios.interceptors.request.use(config => {
    // 添加 CSRF Token
    const csrfToken = getCsrfToken();
    if (csrfToken && ['post', 'put', 'delete'].includes(config.method)) {
        config.headers['X-CSRF-TOKEN'] = csrfToken;
    }
    return config;
});

// 响应拦截器：更新 CSRF Token
axios.interceptors.response.use(
    response => {
        const newToken = response.headers['x-csrf-token'];
        if (newToken) {
            document.cookie = `XSRF-TOKEN=${encodeURIComponent(newToken)}; path=/; ${location.protocol === 'https:' ? 'Secure; ' : ''}SameSite=Strict`;
        }
        return response;
    },
    error => {
        if (error.response?.status === 403) {
            console.error('CSRF Token 验证失败');
            // 刷新页面重新获取 Token
            window.location.reload();
        }
        return Promise.reject(error);
    }
);
```

### 4. Angular/React 配置

**Angular**（自动支持）：

Angular 的 `HttpClient` 默认支持 CSRF，会自动读取 `XSRF-TOKEN` Cookie 并添加到 `X-XSRF-TOKEN` 请求头。

**React（Axios）**：

```javascript
// 参考上面的 Vue + Axios 示例
```

---

## API 调用

### 1. 登录接口（排除 CSRF）

**无需携带 CSRF Token**：

```bash
POST /user/login
Content-Type: application/json

{
    "phone": "13800138000",
    "code": "123456"
}
```

### 2. 员工登录接口（排除 CSRF）

**无需携带 CSRF Token**：

```bash
POST /employee/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456"
}
```

### 3. 其他接口（需要 CSRF）

**必须携带 CSRF Token**：

```bash
PUT /employee
Content-Type: application/json
X-CSRF-TOKEN: <从Cookie获取的Token>

{
    "id": 1,
    "name": "修改后管理员",
    "phone": "13600136000"
}
```

### 4. CSRF Token 刷新

**方式一：GET 请求自动刷新**

```javascript
// 任意 GET 请求都会刷新 Token
GET /employee/page?page=1&pageSize=10
// 响应头 X-CSRF-TOKEN 包含新的 Token
```

**方式二：主动刷新**

```javascript
// 在 Filter 中配置的 /csrf/refresh 路径
GET /csrf/refresh
// 返回新的 Token
```

---

## 测试验证

### 1. 单元测试

CSRF 过滤器在测试环境（`@Profile("test")`）下自动禁用，无需特殊处理。

### 2. 集成测试

**验证 CSRF 生效**：

```bash
# 1. 先发送 GET 请求获取 CSRF Token
curl -c cookies.txt http://localhost:8080/employee/page?page=1

# 2. 从 Cookie 中提取 Token
TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $NF}')

# 3. 发送 POST 请求并携带 Token
curl -b cookies.txt -c cookies.txt \
  -H "X-CSRF-TOKEN: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"test","name":"测试"}' \
  http://localhost:8080/employee

# 预期：成功（HTTP 200）
```

**验证 CSRF 拦截**：

```bash
# 不携带 Token 发送 POST 请求
curl -c cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"username":"test","name":"测试"}' \
  http://localhost:8080/employee

# 预期：失败（HTTP 403 Forbidden）
```

### 3. 浏览器测试

1. 打开浏览器开发者工具（F12）
2. 进入 **Application** → **Cookies**
3. 查找 `XSRF-TOKEN` Cookie
4. 发送任意 POST 请求，观察是否携带 `X-CSRF-TOKEN` 请求头

---

## 常见问题

### 1. POST 请求返回 403 Forbidden

**原因**：未携带 CSRF Token 或 Token 无效

**解决**：
- 检查 Cookie 中是否有 `XSRF-TOKEN`
- 检查请求头是否包含 `X-CSRF-TOKEN`
- 确认 Token 未过期（默认1小时）

### 2. 测试时 CSRF 影响测试用例

**原因**：测试环境未正确禁用 CSRF

**解决**：
- 确认测试类标注了 `@ActiveProfiles("test")`
- 检查 `application-test.yml` 中 `spring.profiles.active=test`

### 3. Token 过期问题

**原因**：Token 默认1小时过期

**解决**：
- 在 GET 请求中自动刷新 Token
- 或调用 `/csrf/refresh` 接口主动刷新

### 4. Cookie 未设置 SameSite 属性

**原因**：跨域请求时 Cookie 可能被拒绝

**解决**：
- 前端在读取 Cookie 时添加 `SameSite=Strict` 属性
- 或使用请求头方式传递 Token

### 5. 静态资源路径被误拦截

**原因**：路径匹配规则过于宽泛

**解决**：
- 检查 `EXCLUDED_PATHS` 配置
- 确保使用 Ant 路径模式（如 `/front/**`）

---

## 安全建议

### 1. Token 生命周期

- **默认1小时**：平衡安全和用户体验
- **敏感操作**：每次操作后刷新 Token
- **登出时**：清除 Session 中的 Token

### 2. Cookie 安全配置

```javascript
// 生产环境建议
document.cookie = `XSRF-TOKEN=${token}; path=/; Secure; SameSite=Strict`;
```

- `Secure`：仅通过 HTTPS 传输
- `SameSite=Strict`：防止跨站请求携带 Cookie
- `HttpOnly=false`：允许前端读取（必须在客户端读取）

### 3. 自定义排除路径

如需排除更多路径，修改 `CsrfFilter.EXCLUDED_PATHS`：

```java
private static final String[] EXCLUDED_PATHS = {
    "/actuator/**",
    "/backend/**",
    "/front/**",
    "/common/upload",
    "/common/download",
    "/csrf/**",
    "/user/sendMsg",
    "/user/login",
    "/employee/login",
    "/your/custom/path/**"  // 添加自定义路径
};
```

---

## 技术细节

### Token 生成算法

```java
// 1. 生成 32 字节随机数（256位）
SecureRandom random = new SecureRandom();
byte[] randomBytes = new byte[32];
random.nextBytes(randomBytes);

// 2. 添加时间戳（毫秒）
long timestamp = System.currentTimeMillis();
byte[] timestampBytes = Long.toString(timestamp).getBytes(StandardCharsets.UTF_8);

// 3. 合并
byte[] combined = new byte[randomBytes.length + timestampBytes.length];
System.arraycopy(randomBytes, 0, combined, 0, randomBytes.length);
System.arraycopy(timestampBytes, 0, combined, randomBytes.length, timestampBytes.length);

// 4. Base64 URL 安全编码
String token = Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
```

### 过滤器执行流程

```
Request → CsrfFilter
            ├─ 测试环境？ → 跳过
            ├─ GET 请求？ → 生成/刷新 Token → 继续
            ├─ 排除路径？ → 跳过
            └─ POST/PUT/DELETE → 验证 Token
                                ├─ 验证通过 → 继续
                                └─ 验证失败 → 403 Forbidden
```

### 性能影响

- **内存**：每个 Session 存储一个 Token（约 50 字节）
- **CPU**：Token 生成使用 `SecureRandom`（启动时一次性初始化）
- **网络**：每次 GET 请求响应头添加 Token（约 50 字节）
- **Redis**：未使用，纯内存操作

---

## 参考资源

- [OWASP CSRF 防护](https://owasp.org/www-community/attacks/csrf)
- [Spring Security CSRF 文档](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [RFC 7616: HTTP Origin](https://tools.ietf.org/html/rfc7616)

### 后端配置

#### 1. SecurityConfig.java

**位置：** `src/main/java/com/reggie/config/SecurityConfig.java`

**CSRF 配置：**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository()) // 使用 Cookie 存储 Token
            .ignoringAntMatchers(
                "/actuator/**",    // 监控端点排除
                "/backend/**",     // 静态资源排除
                "/front/**",       // 静态资源排除
                "/common/upload",  // 文件上传排除
                "/common/download" // 文件下载排除
            )
        );

    return http.build();
}
```

**CSRF Token 仓库：**

```java
@Bean
public CsrfTokenRepository csrfTokenRepository() {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookieName("XSRF-TOKEN");
    repository.setHeaderName("X-XSRF-TOKEN");
    repository.setCookiePath("/");
    repository.setSecure(false); // 开发环境
    repository.setCookieMaxAge(3600 * 24); // 1天
    return repository;
}
```

#### 2. CsrfController.java

**位置：** `src/main/java/com/reggie/controller/CsrfController.java`

**接口：** `GET /csrf/token`

**功能：** 提供 CSRF Token 给前端

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "parameterName": "_csrf",
    "token": "abc123def456ghi789",
    "headerName": "X-CSRF-TOKEN"
  }
}
```

#### 3. CsrfTokenResponseFilter.java

**位置：** `src/main/java/com/reggie/filter/CsrfTokenResponseFilter.java`

**功能：** 自动将 CSRF Token 添加到响应头和 Cookie

**工作流程：**

1. 拦截所有请求
2. 从请求属性中获取 CSRF Token
3. 将 Token 添加到响应头 `X-CSRF-TOKEN`
4. 将 Token 设置到 Cookie `XSRF-TOKEN`

---

## 前端集成

### Vue.js 集成方案

#### 1. Axios 拦截器配置

```javascript
// src/utils/request.js
import axios from 'axios';

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 5000
});

// 请求拦截器：添加 CSRF Token
request.interceptors.request.use(
  (config) => {
    // 从 Cookie 获取 CSRF Token
    const csrfToken = getCookie('XSRF-TOKEN');

    if (csrfToken) {
      // 添加到请求头
      config.headers['X-CSRF-TOKEN'] = csrfToken;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：更新 CSRF Token
request.interceptors.response.use(
  (response) => {
    // 从响应头获取新的 CSRF Token
    const newCsrfToken = response.headers['x-csrf-token'];
    if (newCsrfToken) {
      // 更新 Cookie
      document.cookie = `XSRF-TOKEN=${newCsrfToken}; path=/`;
    }

    return response;
  },
  (error) => {
    if (error.response && error.response.status === 403) {
      console.error('CSRF Token 验证失败');
    }
    return Promise.reject(error);
  }
);

// Cookie 读取工具函数
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop().split(';').shift();
  }
  return null;
}

export default request;
```

#### 2. 初始化获取 Token

```javascript
// App.vue 或 main.js
import request from './utils/request';

// 应用启动时获取 CSRF Token
request.get('/csrf/token')
  .then((response) => {
    console.log('CSRF Token 已获取');
  })
  .catch((error) => {
    console.error('获取 CSRF Token 失败:', error);
  });
```

---

## API 调用

### POST/PUT/DELETE 请求

所有**修改请求**（POST、PUT、DELETE）必须携带 CSRF Token。

#### 请求方式 1：请求头（推荐）

```javascript
// 请求头
X-CSRF-TOKEN: abc123def456ghi789

// 或
X-XSRF-TOKEN: abc123def456ghi789
```

**示例：**

```javascript
axios.post('/employee/login', {
  username: 'admin',
  password: '123456'
}, {
  headers: {
    'X-CSRF-TOKEN': getCookie('XSRF-TOKEN')
  }
});
```

#### 请求方式 2：请求参数

```javascript
// 请求体
{
  "username": "admin",
  "password": "123456",
  "_csrf": "abc123def456ghi789"
}
```

### GET 请求

GET 请求**不需要** CSRF Token（安全方法）。

---

## 测试验证

### 1. 测试 CSRF Token 获取

```bash
# 获取 CSRF Token
curl -c cookies.txt http://localhost:8080/csrf/token

# 响应：Set-Cookie: XSRF-TOKEN=abc123...
```

### 2. 测试 CSRF 防护生效

#### 无 Token 请求（应该失败）

```bash
curl -b cookies.txt -X POST http://localhost:8080/employee/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 期望响应：403 Forbidden
```

#### 携带 Token 请求（应该成功）

```bash
# 从 Cookie 读取 Token
TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

# 发送请求
curl -b cookies.txt -X POST http://localhost:8080/employee/login \
  -H "Content-Type: application/json" \
  -H "X-CSRF-TOKEN: $TOKEN" \
  -d '{"username":"admin","password":"123456"}'

# 期望响应：200 OK
```

### 3. 测试排除规则

```bash
# 静态资源（应该成功，不需要 CSRF Token）
curl http://localhost:8080/backend/index.html

# 监控端点（应该成功，不需要 CSRF Token）
curl http://localhost:8080/actuator/health
```

---

## 常见问题

### Q1: 为什么前后端分离需要 CSRF？

虽然前后端分离使用 Token 认证（JWT/Session），但 CSRF 攻击仍然可能生效：

- 浏览器会自动携带 Cookie
- 攻击者可以伪造跨域请求
- CSRF Token 是最有效的防护手段之一

### Q2: 为什么不排除所有接口？

排除过多接口会降低安全性。建议只排除：
- 公开的静态资源
- 文件上传/下载
- 监控端点（已认证）

### Q3: 生产环境如何配置？

```yaml
# application-prod.yml
spring:
  security:
    csrf:
      secure: true  # 仅 HTTPS
      cookie-max-age: 86400  # 1天
```

修改 `SecurityConfig.java`：

```java
repository.setSecure(true); // HTTPS only
```

### Q4: 如何处理跨域问题？

CSRF 和 CORS 是不同的问题，但配置 CSRF 时需要注意：

```java
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList("https://your-domain.com"));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(Arrays.asList("Content-Type", "X-CSRF-TOKEN"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}
```

### Q5: 如何关闭 CSRF 防护？

不推荐关闭，但如果确实需要：

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf().disable(); // 关闭 CSRF
    return http.build();
}
```

---

## 📊 配置总结

| 配置项 | 值 | 说明 |
|--------|-----|------|
| Token 存储 | Cookie | `XSRF-TOKEN` |
| Token 有效期 | 24小时 | 自动续期 |
| 请求头名称 | `X-CSRF-TOKEN` | 也可使用 `_csrf` 参数 |
| 排除路径 | `/actuator/**`, `/backend/**`, `/front/**`, `/common/upload`, `/common/download` | 静态资源和文件上传 |

---

**最后更新：** 2026-07-02
