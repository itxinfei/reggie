package com.reggie.module.payment;

import com.reggie.common.CustomException;
import com.reggie.module.payment.channel.AlipayChannel;
import com.reggie.module.payment.channel.PaymentChannel;
import com.reggie.module.payment.channel.PaymentChannelFactory;
import com.reggie.module.payment.channel.WechatPayChannel;
import com.reggie.module.payment.config.PaymentConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付渠道工厂 mock 模式单测。
 * 验证 mockMode=true 时工厂直接返回存量 mock 渠道（不触数据库/网络）、
 * 异常与可空语义，以及缓存驱逐方法在 mock 模式下安全无副作用。
 *
 * @author reggie
 * @since 2026-09-21
 */
class PaymentFactoryMockTest {

    private PaymentChannelFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = new PaymentChannelFactory();
        setField(factory, "alipayChannel", new AlipayChannel());
        setField(factory, "wechatPayChannel", new WechatPayChannel());
        PaymentConfigProperties properties = new PaymentConfigProperties();
        properties.setMockMode(true);
        setField(factory, "paymentConfigProperties", properties);
    }

    @Test
    void returnsMockChannelsInMockMode() {
        PaymentChannel wechat = factory.getChannel("WECHAT");
        PaymentChannel alipay = factory.getChannel("ALIPAY");
        assertTrue(wechat instanceof WechatPayChannel, "WECHAT 应返回微信 mock 渠道");
        assertTrue(alipay instanceof AlipayChannel, "ALIPAY 应返回支付宝 mock 渠道");
        // 大小写不敏感
        assertTrue(factory.getChannel("wechat") instanceof WechatPayChannel);
    }

    @Test
    void nullAndUnknownChannelThrow() {
        assertThrows(CustomException.class, () -> factory.getChannel(null));
        assertThrows(CustomException.class, () -> factory.getChannel("UNIONPAY"));
    }

    @Test
    void nullableReturnsNullForNullAndUnknown() {
        assertNull(factory.getChannelNullable(null));
        assertNull(factory.getChannelNullable("UNIONPAY"));
        // 已知渠道仍正常返回 mock
        assertTrue(factory.getChannelNullable("alipay") instanceof AlipayChannel);
    }

    @Test
    void evictIsSafeInMockMode() {
        assertDoesNotThrow(() -> factory.evict(1L, "WECHAT"));
        assertDoesNotThrow(() -> factory.evict(null, "WECHAT"));
        assertDoesNotThrow(() -> factory.evictTenant(1L));
        assertDoesNotThrow(() -> factory.evictTenant(null));
        // 驱逐后 mock 模式仍返回同一 mock 渠道
        assertTrue(factory.getChannel("WECHAT") instanceof WechatPayChannel);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
