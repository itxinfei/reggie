package com.reggie.config;

import com.reggie.common.SecurityConstants;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Set;
import javax.servlet.SessionTrackingMode;

/**
 * <p>
 * Session安全配置类，配置Session超时时间和追踪模式。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Configuration
public class SessionTimeoutConfig {

    /**
     * 配置Session超时时间和追踪模式
     * 禁用URL重写，防止Session ID泄露
     *
     * @return ServletContext初始化器
     */
    @Bean
    public ServletContextInitializer sessionConfig() {
        return servletContext -> {
            // Session 超时时间（分钟）
            servletContext.setSessionTimeout(SecurityConstants.SESSION_TIMEOUT / 60);

            // 禁用 URL 重写（防止 Session ID 泄露）
            servletContext.setSessionTrackingModes(
                Collections.singleton(SessionTrackingMode.COOKIE)
            );
        };
    }
}
