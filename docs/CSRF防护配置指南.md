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
2. Token 存储到 Cookie 中
3. 前端从 Cookie 读取 Token
4. 请求时携带 Token（请求头或参数）
5. 服务器验证 Token

---

## 配置说明

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
