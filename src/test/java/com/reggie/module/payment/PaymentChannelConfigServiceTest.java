package com.reggie.module.payment;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.BaseContext;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.reggie.module.payment.service.PaymentChannelConfigService;
import com.reggie.module.payment.util.PaymentCredentialEncryptor;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付渠道配置服务集成测试（H2）。
 * 覆盖加密落库、掩码、留空不改、查重、租户隔离、启停对 findActive 的影响。
 *
 * @author reggie
 * @since 2026-09-21
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-payment.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PaymentChannelConfigServiceTest {

    private static final String MASK = "***已加密***";

    @Autowired
    private PaymentChannelConfigService configService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("payment_channel_config");
        BaseContext.setCurrentTenantId(999L);
    }

    private PaymentChannelConfig buildWechat() {
        PaymentChannelConfig c = new PaymentChannelConfig();
        c.setConfigName("微信支付-总店");
        c.setChannel("WECHAT");
        c.setWxAppId("wx1234567890abcdef");
        c.setWxMchId("1600000001");
        c.setWxApiV3Key("test-api-v3-key-12345678901234567890123456");
        c.setWxMchCertSerialNo("CERT-SERIAL-001");
        c.setWxMchPrivateKey("-----BEGIN PRIVATE KEY-----\nWECHAT-PRIVATE-KEY-VALUE\n-----END PRIVATE KEY-----");
        c.setPayNotifyUrl("https://example.com/api/payment/notify/WECHAT");
        c.setRefundNotifyUrl("https://example.com/api/payment/refund-notify/WECHAT");
        c.setEnabled(1);
        return c;
    }

    private PaymentChannelConfig buildAlipay() {
        PaymentChannelConfig c = new PaymentChannelConfig();
        c.setConfigName("支付宝-总店");
        c.setChannel("ALIPAY");
        c.setAliAppId("2021000000000001");
        c.setAliPrivateKey("-----BEGIN PRIVATE KEY-----\nALIPAY-PRIVATE-KEY-VALUE\n-----END PRIVATE KEY-----");
        c.setAliPublicKey("-----BEGIN PUBLIC KEY-----\nALIPAY-PUBLIC-KEY-VALUE\n-----END PUBLIC KEY-----");
        c.setPayNotifyUrl("https://example.com/api/payment/notify/ALIPAY");
        c.setEnabled(1);
        return c;
    }

    @Test
    void testAddEncryptsCredentials() {
        PaymentChannelConfig saved = configService.addConfig(buildWechat());
        assertNotNull(saved.getId());
        // 返回体已掩码
        assertEquals(MASK, saved.getWxApiV3Key());

        // 库里是密文：非明文，且能解密还原
        PaymentChannelConfig raw = configService.getEntityById(saved.getId());
        assertNotEquals("test-api-v3-key-12345678901234567890123456", raw.getWxApiV3Key());
        assertEquals("test-api-v3-key-12345678901234567890123456",
                PaymentCredentialEncryptor.decrypt(raw.getWxApiV3Key()));
        assertEquals("wx1234567890abcdef", raw.getWxAppId());
        assertEquals(1L, raw.getTenantId().longValue());
    }

    @Test
    void testPageAndDetailMasked() {
        configService.addConfig(buildWechat());
        configService.addConfig(buildAlipay());

        IPage<PaymentChannelConfig> page = configService.pageMasked(PageUtils.of(1, 10), null, null);
        assertEquals(2, page.getRecords().size());
        for (PaymentChannelConfig c : page.getRecords()) {
            if (c.getWxApiV3Key() != null) {
                assertEquals(MASK, c.getWxApiV3Key());
            }
            if (c.getWxMchPrivateKey() != null) {
                assertEquals(MASK, c.getWxMchPrivateKey());
            }
            if (c.getAliPrivateKey() != null) {
                assertEquals(MASK, c.getAliPrivateKey());
            }
        }

        PaymentChannelConfig first = page.getRecords().get(0);
        PaymentChannelConfig detail = configService.getMaskedById(first.getId());
        // 详情中至少一个敏感字段被掩码
        boolean anyMasked = MASK.equals(detail.getWxApiV3Key())
                || MASK.equals(detail.getWxMchPrivateKey())
                || MASK.equals(detail.getAliPrivateKey());
        assertTrue(anyMasked);
    }

    @Test
    void testUpdateKeepsCipherWhenBlank() {
        PaymentChannelConfig saved = configService.addConfig(buildWechat());
        String oldApiV3Cipher = configService.getEntityById(saved.getId()).getWxApiV3Key();
        String oldPrivateCipher = configService.getEntityById(saved.getId()).getWxMchPrivateKey();

        // 更新只改名，三个密钥字段留空（null）
        PaymentChannelConfig up = new PaymentChannelConfig();
        up.setId(saved.getId());
        up.setChannel("WECHAT");
        up.setConfigName("微信支付-改名");
        up.setEnabled(1);
        assertTrue(configService.updateConfig(up));

        PaymentChannelConfig raw = configService.getEntityById(saved.getId());
        // 密钥密文原样保留
        assertEquals(oldApiV3Cipher, raw.getWxApiV3Key());
        assertEquals(oldPrivateCipher, raw.getWxMchPrivateKey());
        // 普通字段更新生效
        assertEquals("微信支付-改名", raw.getConfigName());
    }

    @Test
    void testUpdateReEncryptsWhenProvided() {
        PaymentChannelConfig saved = configService.addConfig(buildWechat());
        String oldApiV3Cipher = configService.getEntityById(saved.getId()).getWxApiV3Key();

        PaymentChannelConfig up = new PaymentChannelConfig();
        up.setId(saved.getId());
        up.setChannel("WECHAT");
        up.setWxApiV3Key("new-api-v3-key-12345678901234567890123456");
        assertTrue(configService.updateConfig(up));

        PaymentChannelConfig raw = configService.getEntityById(saved.getId());
        assertNotEquals(oldApiV3Cipher, raw.getWxApiV3Key());
        assertEquals("new-api-v3-key-12345678901234567890123456",
                PaymentCredentialEncryptor.decrypt(raw.getWxApiV3Key()));
    }

    @Test
    void testExistsActiveConfig() {
        PaymentChannelConfig saved = configService.addConfig(buildWechat());
        assertTrue(configService.existsActiveConfig("WECHAT", null));
        assertFalse(configService.existsActiveConfig("ALIPAY", null));
        // 更新时排除自身
        assertFalse(configService.existsActiveConfig("WECHAT", saved.getId()));
    }

    @Test
    void testFindActiveByTenantAndEnabled() {
        PaymentChannelConfig saved = configService.addConfig(buildWechat());

        assertNotNull(configService.findActive(1L, "WECHAT"));
        // 不同租户隔离
        assertNull(configService.findActive(2L, "WECHAT"));
        // 停用后查不到
        assertTrue(configService.setEnabled(saved.getId(), 0));
        assertNull(configService.findActive(1L, "WECHAT"));
    }
}
