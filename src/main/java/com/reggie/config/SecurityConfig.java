package com.reggie.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.reggie.common.SecurityConstants;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Set;
import javax.servlet.SessionTrackingMode;

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
            servletContext.setSessionTrackingModes(
                Collections.singleton(SessionTrackingMode.COOKIE)
            );
        };
    }
}
