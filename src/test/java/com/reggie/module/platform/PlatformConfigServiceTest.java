package com.reggie.module.platform;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.BaseContext;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.util.PlatformCredentialEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 外卖平台接入配置 Service 集成测试
 * <p>覆盖：凭据加解密、新增加密、脱敏、查重、分页、更新保留原密文、启用停用、租户隔离。</p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-platform.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PlatformConfigServiceTest {

    @Autowired
    private PlatformConfigService platformConfigService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testEncryptorRoundTrip() {
        String plain = "s3cr3t-token-2026";
        String encrypted = PlatformCredentialEncryptor.encrypt(plain);
        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted);
        // 相同明文两次加密密文应不同（随机 IV）
        String encrypted2 = PlatformCredentialEncryptor.encrypt(plain);
        assertNotEquals(encrypted, encrypted2);
        // 解密还原
        assertEquals(plain, PlatformCredentialEncryptor.decrypt(encrypted));
        assertEquals(plain, PlatformCredentialEncryptor.decrypt(encrypted2));
    }

    @Test
    void testEncryptorNullSafe() {
        // 空值/空串不加密，原样返回，避免写入无意义密文
        assertNull(PlatformCredentialEncryptor.encrypt(null));
        assertEquals("", PlatformCredentialEncryptor.encrypt(""));
        // 非合法密文（长度不足）原样返回
        assertEquals("x", PlatformCredentialEncryptor.decrypt("x"));
    }

    @Test
    void testAddEncryptsAndMasks() {
        PlatformConfig config = buildConfig("MEITUAN", "MT-001", "my-app-key", "my-secret", "my-token");
        PlatformConfig saved = platformConfigService.addConfig(config);
        assertNotNull(saved.getId());

        // 数据库落库应为密文
        PlatformConfig fromDb = platformConfigService.getById(saved.getId());
        assertNotEquals("my-app-key", fromDb.getAppKey());
        assertNotEquals("my-secret", fromDb.getAppSecret());
        assertNotEquals("my-token", fromDb.getAccessToken());

        // 列表脱敏，不返回明文也不返回密文
        IPage<PlatformConfig> page = platformConfigService.pageMasked(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));
        assertEquals(1, page.getRecords().size());
        PlatformConfig masked = page.getRecords().get(0);
        assertEquals("***已加密***", masked.getAppKey());
        assertEquals("***已加密***", masked.getAppSecret());
        assertEquals("***已加密***", masked.getAccessToken());
        assertEquals(1, masked.getEnabled());
        assertEquals(1, masked.getSyncScope());
    }

    @Test
    void testDuplicateTypeAndShop() {
        platformConfigService.addConfig(buildConfig("ELEME", "EL-001", "k1", "s1", "t1"));
        assertTrue(platformConfigService.existsByTypeAndShop("ELEME", "EL-001", null));
        assertFalse(platformConfigService.existsByTypeAndShop("ELEME", "EL-002", null));
        assertFalse(platformConfigService.existsByTypeAndShop("MEITUAN", "EL-001", null));
    }

    @Test
    void testUpdateKeepsOriginalCipherWhenBlank() {
        PlatformConfig saved = platformConfigService.addConfig(buildConfig("DOUYIN", "DY-001", "k1", "s1", "t1"));
        String originalSecret = platformConfigService.getById(saved.getId()).getAppSecret();

        // 更新时只改 appKey，secret/token 留空 -> 保留原密文
        PlatformConfig update = new PlatformConfig();
        update.setId(saved.getId());
        update.setPlatformType("DOUYIN");
        update.setPlatformName("抖音-分店");
        update.setShopId("DY-001");
        update.setAppKey("new-key");
        update.setAppSecret("");
        update.setAccessToken("");
        update.setSyncScope(3);
        update.setEnabled(1);
        assertTrue(platformConfigService.updateConfig(update));

        PlatformConfig after = platformConfigService.getById(saved.getId());
        assertEquals("new-key", PlatformCredentialEncryptor.decrypt(after.getAppKey()));
        assertEquals(originalSecret, after.getAppSecret());
        assertEquals(3, after.getSyncScope());
    }

    @Test
    void testSetEnabled() {
        PlatformConfig saved = platformConfigService.addConfig(buildConfig("SELF", "SF-001", "k", "s", "t"));
        assertTrue(platformConfigService.setEnabled(saved.getId(), 0));
        assertEquals(0, platformConfigService.getById(saved.getId()).getEnabled());
        assertTrue(platformConfigService.setEnabled(saved.getId(), 1));
        assertEquals(1, platformConfigService.getById(saved.getId()).getEnabled());
    }

    @Test
    void testTenantIsolation() {
        BaseContext.setCurrentTenantId(1L);
        platformConfigService.addConfig(buildConfig("MEITUAN", "A1", "k", "s", "t"));
        // 切到租户2，新建设置 tenant_id=2
        BaseContext.setCurrentTenantId(2L);
        platformConfigService.addConfig(buildConfig("MEITUAN", "A2", "k", "s", "t"));

        // 当前线程 tenant=2，仅应看到 A2
        BaseContext.setCurrentTenantId(2L);
        IPage<PlatformConfig> page = platformConfigService.pageMasked(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));
        assertEquals(1, page.getRecords().size());
        assertEquals("A2", page.getRecords().get(0).getShopId());
    }

    private PlatformConfig buildConfig(String type, String shopId, String key, String secret, String token) {
        PlatformConfig c = new PlatformConfig();
        c.setPlatformType(type);
        c.setPlatformName(type + "-" + shopId);
        c.setShopId(shopId);
        c.setAppKey(key);
        c.setAppSecret(secret);
        c.setAccessToken(token);
        c.setSyncScope(1);
        c.setEnabled(1);
        return c;
    }
}
