package com.reggie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @Author pangzhao
 * @Date 2026-5-25 上午 8:49
 * @Description
 **/
@Slf4j
@SpringBootApplication
@ServletComponentScan
@EnableTransactionManagement
@org.springframework.cache.annotation.EnableCaching
@EnableScheduling
@EnableAsync
public class ReggieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class, args);
        log.info("瑞吉外卖--项目启动成功...");
        // 修改点：触发 DevTools 重启以生效 DeliveryOrder 字段映射修复
    }
}
