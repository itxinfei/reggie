# Task 15: Session 超时配置

**Files:**
- Create: `src/main/java/com/reggie/config/SecurityConfig.java`
- Modify: `src/main/java/com/reggie/filter/LoginCheckFilter.java`

## 任务描述

配置 Session 超时时间（30分钟），禁用 URL 重写，增强会话安全性。

## 具体要求

### 1. 创建 SecurityConfig.java

```java
package com.reggie.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.reggie.common.SecurityConstants;

import javax.servlet.ServletContext;

/**
 * 安全配置
 */
@Configuration
public class SecurityConfig {

    /**
     * 配置Session超时时间
     */
    @Bean
    public ServletContextInitializer sessionConfig() {
        return servletContext -> {
            // Session超时时间（分钟）
            servletContext.setSessionTimeout(SecurityConstants.SESSION_TIMEOUT / 60);
            // 禁用URL重写（防止Session ID泄露）
            servletContext.setSessionTrackingModes(java.util.Set.of(
                javax.servlet.SessionTrackingMode.COOKIE
            ));
        };
    }
}
```

### 2. 修改 LoginCheckFilter.java（可选增强）

在登录成功后，设置最后访问时间戳：

```java
// 在存储Session之前添加
HttpSession session = request.getSession();
session.setAttribute("lastAccessTime", System.currentTimeMillis());
```

**注意：** 此步骤为可选增强，如果时间不足可以跳过。

## 验收标准

- [ ] SecurityConfig.java 创建成功
- [ ] Session 超时时间配置为 30 分钟
- [ ] 禁用 URL 重写（仅使用 Cookie）
- [ ] 编译通过

