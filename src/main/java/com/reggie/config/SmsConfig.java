package com.reggie.config;

import com.reggie.utils.SMSUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * <p>
 * 短信服务配置类，从application.yml读取阿里云SMS凭证并初始化SMSUtils。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Configuration
public class SmsConfig {

    @Value("${reggie.sms.access-key:}")
    private String accessKey;

    @Value("${reggie.sms.secret-key:}")
    private String secretKey;

    /**
     * 初始化短信服务凭证
     * 从配置文件读取accessKey和secretKey并初始化SMSUtils
     */
    @PostConstruct
    public void initSmsUtils() {
        log.info("正在初始化短信服务凭证...");
        SMSUtils.init(accessKey, secretKey);
    }
}
