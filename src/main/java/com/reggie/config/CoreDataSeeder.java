package com.reggie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 核心基础数据初始化。
 *
 * <p>应用启动时幂等保证主租户（1）、测试租户（999）和超级管理员 admin 始终存在。
 * 全部 INSERT IGNORE：只保证存在，绝不覆盖运营已修改的数据。</p>
 *
 * <p>历史上种子逻辑放在 db/seed/core-data-seed.sql，该脚本已在 2026-09-28
 * "清理废弃迁移种子脚本"中删除（结构以测试 schema 为准），这里改为代码直接播种，
 * 不再依赖 classpath 外部脚本。</p>
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

    /** admin 默认口令 123456 的 32 位小写 MD5；登录由 PasswordUtils 按 MD5 校验 */
    private static final String DEFAULT_PASSWORD_MD5 = "e10adc3949ba59abbe56e057f20f883e";

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            LocalDateTime now = LocalDateTime.now();
            // 租户：1=主门店（开发联调），999=自动化测试租户
            jdbcTemplate.update("INSERT IGNORE INTO tenant (id, name, contact, phone, status, "
                    + "create_time, update_time, create_user, update_user) "
                    + "VALUES (?, ?, ?, ?, 1, ?, ?, 1, 1)",
                    1L, "瑞吉主门店", "管理员", "13800000000", now, now);
            jdbcTemplate.update("INSERT IGNORE INTO tenant (id, name, contact, phone, status, "
                    + "create_time, update_time, create_user, update_user) "
                    + "VALUES (?, ?, ?, ?, 1, ?, ?, 1, 1)",
                    999L, "自动化测试租户", "测试", "13900000099", now, now);

            // 超级管理员 admin / 123456（MD5；PasswordUtils.matches 同时支持 MD5/BCRYPT）
            jdbcTemplate.update("INSERT IGNORE INTO employee (id, username, name, password, password_type, "
                    + "phone, status, sex, role, tenant_id, create_time, update_time, create_user, update_user) "
                    + "VALUES (1, 'admin', '管理员', ?, 'MD5', '13800000000', 1, '1', 1, 1, ?, ?, 1, 1)",
                    DEFAULT_PASSWORD_MD5, now, now);

            log.info("核心基础数据检查完成（租户/admin）");
        } catch (Exception e) {
            log.warn("核心基础数据初始化失败，不影响启动: {}", e.getMessage());
        }
    }
}
