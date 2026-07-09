package com.reggie.config;

import com.reggie.utils.SMSUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 短信服务配置
 * 从application.yml读取阿里云SMS凭证并初始化SMSUtils
 *
 * @author Reggie Team
 */
@Slf4j
@Configuration
public class SmsConfig {

    @Value("${reggie.sms.access-key:}")
    private String accessKey;

    @Value("${reggie.sms.secret-key:}")
    private String secretKey;

    @PostConstruct
    public void initSmsUtils() {
        log.info("正在初始化短信服务凭证...");
        SMSUtils.init(accessKey, secretKey);
    }
}
