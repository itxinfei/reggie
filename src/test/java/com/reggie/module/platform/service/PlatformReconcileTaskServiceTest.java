package com.reggie.module.platform.service;

import com.reggie.module.platform.model.PlatformReconcileTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 平台对账服务测试
 *
 * @author reggie
 * @since 2026-08-24
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = "classpath:schema-platform.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PlatformReconcileTaskServiceTest {

    @Autowired
    private PlatformReconcileTaskService reconcileTaskService;

    @Test
    void testGetByDate_NotFound() {
        PlatformReconcileTask task = reconcileTaskService.getByDate("MEITUAN", LocalDate.now());
        assertNull(task);
    }
}
