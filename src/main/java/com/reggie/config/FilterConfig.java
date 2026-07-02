package com.reggie.config;

import com.reggie.filter.CsrfFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Filter 配置
 *
 * @author itxinfei
 */
@Configuration
public class FilterConfig {

    /**
     * CSRF 防护过滤器
     * 在生产环境启用，测试环境禁用
     * 拦截顺序：在 LoginCheckFilter 之后
     */
    @Bean
    @Profile("!test") // 非测试环境启用
    public FilterRegistrationBean<CsrfFilter> csrfFilterRegistration() {
        FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CsrfFilter());
        registration.addUrlPatterns("/*");
        registration.setName("csrfFilter");
        registration.setOrder(2); // 顺序：1=LoginCheckFilter, 2=CsrfFilter
        return registration;
    }
}
