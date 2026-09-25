package com.reggie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 核心基础数据初始化。
 *
 * <p>应用启动时幂等执行 {@code db/seed/core-data-seed.sql}，保证主租户、测试租户和
 * 超级管理员 admin 始终存在；脚本全部 INSERT IGNORE，不覆盖运营数据。</p>
 *
 * <p>初始化失败只告警、不阻断启动（避免本地库异常时整个应用起不来）。</p>
 *
 * @author reggie
 * @since 2026-09-25
 */
@Slf4j
@Component
@Order(10)
public class CoreDataSeeder implements ApplicationRunner {

    @Resource
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            // 必须显式指定 UTF-8：脚本以 UTF-8 保存，而 Windows 中文系统 JVM 默认字符集是 GBK，
            // 不指定会用 GBK 读取，导致中文（如"瑞吉主门店""管理员"）乱码入库。
            EncodedResource sql = new EncodedResource(
                    new ClassPathResource("db/seed/core-data-seed.sql"), "UTF-8");
            ScriptUtils.executeSqlScript(conn, sql);
            log.info("核心基础数据检查完成（租户/admin）");
        } catch (Exception e) {
            log.warn("核心基础数据初始化失败，不影响启动: {}", e.getMessage());
        }
    }
}
