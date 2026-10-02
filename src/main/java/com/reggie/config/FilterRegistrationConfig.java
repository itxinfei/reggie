package com.reggie.config;

import com.reggie.common.TraceIdFilter;
import com.reggie.filter.CsrfFilter;
import com.reggie.filter.LoginCheckFilter;
import com.reggie.filter.SecurityHeaderFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * <p>
 * 过滤器显式注册配置（P0 修复，2026-10-02 代码审查报告 第二节问题 1）。
 * </p>
 * <p>
 * 此前 4 个过滤器经 {@code @WebFilter} + {@code @ServletComponentScan} 注册，
 * 该机制由 Servlet 容器处理，<b>不识别</b> Spring 的 {@code @Order} 与 {@code @Profile}，
 * 导致 CSRF/安全头/登录校验的执行顺序未定义、dev 禁用 CSRF 从未生效。
 * 现改为 {@link FilterRegistrationBean} 显式注册，恢复声明语义：
 * </p>
 * <ul>
 *   <li>执行顺序（setOrder 越小越靠前）：
 *       SecurityHeaderFilter(0) → TraceIdFilter(1) → CsrfFilter(2) → LoginCheckFilter(3)。
 *       数值上保持各类原有 {@code @Order} 的相对次序（原值：SecurityHeader=0、Csrf=1、
 *       LoginCheck=3、TraceId=HIGHEST_PRECEDENCE+1，其中 Csrf/LoginCheck 相对先后不变，
 *       TraceId 按任务要求排在 SecurityHeader 之后、CSRF/登录校验之前，
 *       保证二者告警日志均已带 traceId）。</li>
 *   <li>urlPatterns 与原 {@code @WebFilter} 一致：均为 {@code /*}、asyncSupported=true、
 *       filterName 沿用原名（securityHeaderFilter/traceIdFilter/csrfFilter/loginCheckFilter）。</li>
 *   <li>原 {@code @Profile("!dev")}（CsrfFilter）改由 active profiles 判断 +
 *       {@code setEnabled(false)} 实现，dev 环境真实禁用。</li>
 * </ul>
 * <p>
 * 实例化方式说明：4 个过滤器原本就非 Spring Bean（依赖在运行期经
 * {@code WebApplicationContextUtils} 从 ServletContext 获取，如 RiderRememberTokenService），
 * 故此处直接 {@code new} 实例，行为等价、改动最小；仅 SecurityHeaderFilter 需要
 * Environment（HSTS 的 dev 判断），显式注入以避免依赖 GenericFilterBean 的运行期兜底解析。
 * </p>
 *
 * @author reggie
 * @since 2026-10-02
 */
@Slf4j
@Configuration
public class FilterRegistrationConfig {

    private final Environment environment;

    public FilterRegistrationConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * 安全响应头过滤器：最先执行，确保所有响应（含被后续过滤器拒绝的）都带上安全头。
     *
     * @return 注册 Bean
     */
    @Bean
    public FilterRegistrationBean<SecurityHeaderFilter> securityHeaderFilterRegistration() {
        SecurityHeaderFilter filter = new SecurityHeaderFilter();
        // 原 @WebFilter 实例不由 Spring 管理，getEnvironment() 依赖 GenericFilterBean
        // 运行期兜底解析（可能取不到真实 profiles）；显式注入真实 Environment，
        // 保持 HSTS「非 dev 且 HTTPS 才下发」的语义可靠生效
        filter.setEnvironment(environment);
        FilterRegistrationBean<SecurityHeaderFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("securityHeaderFilter");
        registration.addUrlPatterns("/*");
        registration.setAsyncSupported(true);
        registration.setOrder(0); // 原 @Order(0)
        return registration;
    }

    /**
     * TraceId 过滤器：紧随安全头过滤器执行，为后续 CSRF/登录校验的日志注入 traceId。
     *
     * @return 注册 Bean
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setName("traceIdFilter");
        registration.addUrlPatterns("/*");
        registration.setAsyncSupported(true);
        // 原 @Order(Ordered.HIGHEST_PRECEDENCE + 1)，语义为"尽可能靠前"；
        // 显式顺序定为 SecurityHeader(0) 之后、Csrf(2)/LoginCheck(3) 之前
        registration.setOrder(1);
        return registration;
    }

    /**
     * CSRF 过滤器：在登录校验之前执行（先过 CSRF 再过登录校验）。
     * dev 环境不启用（恢复原 {@code @Profile("!dev")} 语义）。
     *
     * @return 注册 Bean
     */
    @Bean
    public FilterRegistrationBean<CsrfFilter> csrfFilterRegistration() {
        FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>(new CsrfFilter());
        registration.setName("csrfFilter");
        registration.addUrlPatterns("/*");
        registration.setAsyncSupported(true);
        registration.setOrder(2); // 原 @Order(1)，仍保持先于 LoginCheckFilter
        // 原 @Profile("!dev")：开发环境禁用 CSRF（内网测试无需，避免前后端联调 Token 失效问题）。
        // @WebFilter 类不受 Spring 管理导致 @Profile 从未生效，此处用真实机制补齐
        boolean devActive = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (devActive) {
            log.info("开发环境（dev）已禁用 CSRF 防护过滤器（原 @Profile(\"!dev\") 语义）");
        }
        registration.setEnabled(!devActive);
        return registration;
    }

    /**
     * 登录校验过滤器：最后执行，依赖 CsrfFilter 已通过 CSRF 校验。
     *
     * @return 注册 Bean
     */
    @Bean
    public FilterRegistrationBean<LoginCheckFilter> loginCheckFilterRegistration() {
        FilterRegistrationBean<LoginCheckFilter> registration = new FilterRegistrationBean<>(new LoginCheckFilter());
        registration.setName("loginCheckFilter");
        registration.addUrlPatterns("/*");
        registration.setAsyncSupported(true);
        registration.setOrder(3); // 原 @Order(3)：在 CsrfFilter 之后，保证先过 CSRF 再过登录校验
        return registration;
    }
}
