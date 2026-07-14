package com.reggie.config;

import com.reggie.common.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * <p>
 * Web MVC配置类，配置静态资源映射和消息转换器。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 * 修改点(2026-07-10)：从 extends WebMvcConfigurationSupport 改为 implements WebMvcConfigurer，
 * 避免禁用Spring Boot MVC自动配置（否则springdoc-openapi和WebJars自动配置会失效）。
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 设置静态资源映射
     * 映射前端页面、后端管理页面资源
     * Swagger UI 和 WebJars 资源由 springdoc-openapi 和 Spring Boot 自动配置处理，无需手动添加
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始进行静态资源映射...");
        registry.addResourceHandler("/backend/**").addResourceLocations("classpath:/backend/");
        registry.addResourceHandler("/front/**").addResourceLocations("classpath:/front/");
    }

    /**
     * 修改点：通过 WebMvcConfigurer 方式扩展消息转换器
     * 追加自定义Jackson转换器以支持Long类型序列化为字符串、Java 8时间类型格式化。
     * 注意：不再声明ObjectMapper Bean，避免与RedisConfig的redisObjectMapper冲突。
     *
     * @param converters 消息转换器列表
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(new JacksonObjectMapper());
        converters.add(0, messageConverter);
    }
}
