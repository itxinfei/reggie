package com.reggie.module.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.store.mapper.StoreSyncLogMapper;
import com.reggie.module.store.model.StoreSyncLog;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * store_sync_log 的租户白名单回归测试。
 * <p>该表无 tenant_id 列（跨店同步用 source_tenant_id/target_tenant_id 表达），必须留在
 * {@code MybatisPlusConfig.IGNORE_TABLES} 里；一旦漏配，TenantLineInnerInterceptor 会给
 * 每条 SQL 追加 {@code WHERE tenant_id = ?}，CRUD 直接报 Unknown column 异常。</p>
 *
 * @author reggie
 * @since 2026-09-27
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class StoreSyncLogTenantWhitelistTest {

    @Autowired
    private StoreSyncLogMapper storeSyncLogMapper;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("store_sync_log");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(TestDatabaseCleaner.TEST_TENANT_ID);
    }

    @Test
    void crudSucceedsWithoutTenantColumn() {
        StoreSyncLog log = newLog(StoreSyncLog.SYNC_TYPE_DISH, StoreSyncLog.STATUS_SUCCESS);
        assertEquals(1, storeSyncLogMapper.insert(log));
        assertNotNull(log.getId(), "自增主键应回填");

        List<StoreSyncLog> found = storeSyncLogMapper.selectList(
                new LambdaQueryWrapper<StoreSyncLog>().eq(StoreSyncLog::getSourceTenantId, 999L));
        assertEquals(1, found.size());

        log.setSyncCount(42);
        log.setSyncStatus(StoreSyncLog.STATUS_PARTIAL);
        assertEquals(1, storeSyncLogMapper.updateById(log));
        assertEquals(42, storeSyncLogMapper.selectById(log.getId()).getSyncCount());

        assertTrue(storeSyncLogMapper.deleteById(log.getId()) > 0);
        assertEquals(0, storeSyncLogMapper.selectList(
                new LambdaQueryWrapper<StoreSyncLog>().eq(StoreSyncLog::getSourceTenantId, 999L)).size());
    }

    private StoreSyncLog newLog(int syncType, int syncStatus) {
        StoreSyncLog log = new StoreSyncLog();
        log.setSourceTenantId(TestDatabaseCleaner.TEST_TENANT_ID);
        log.setTargetTenantId(1L);
        log.setSyncType(syncType);
        log.setSyncMode(StoreSyncLog.SYNC_MODE_FULL);
        log.setSyncStatus(syncStatus);
        log.setSyncCount(0);
        log.setFailCount(0);
        log.setOperatorId(1L);
        return log;
    }
}
