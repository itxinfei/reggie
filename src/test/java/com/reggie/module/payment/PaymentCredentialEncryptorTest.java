package com.reggie.module.payment;

import com.reggie.module.payment.util.PaymentCredentialEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 支付凭据加解密工具纯单测（不启动 Spring）。
 * 覆盖 round-trip、随机 IV、空值/非本格式原样、篡改密文失败。
 *
 * @author reggie
 * @since 2026-09-21
 */
class PaymentCredentialEncryptorTest {

    @Test
    @DisplayName("加密后解密应还原明文")
    void roundTrip() {
        String plain = "-----BEGIN PRIVATE KEY-----\nMIIEvwIBADANBgkqhki\n-----END PRIVATE KEY-----";
        String encrypted = PaymentCredentialEncryptor.encrypt(plain);
        assertNotEquals(plain, encrypted);
        assertEquals(plain, PaymentCredentialEncryptor.decrypt(encrypted));
    }

    @Test
    @DisplayName("同一明文两次加密结果不同（随机 IV）")
    void samePlainDifferentCipher() {
        String plain = "48d1f3a6c0e24b8a9f5d7e1c3b6a9024";
        String c1 = PaymentCredentialEncryptor.encrypt(plain);
        String c2 = PaymentCredentialEncryptor.encrypt(plain);
        assertNotEquals(c1, c2);
        assertEquals(plain, PaymentCredentialEncryptor.decrypt(c1));
        assertEquals(plain, PaymentCredentialEncryptor.decrypt(c2));
    }

    @Test
    @DisplayName("null 与空串原样返回")
    void nullAndEmptyAsIs() {
        assertNull(PaymentCredentialEncryptor.encrypt(null));
        assertEquals("", PaymentCredentialEncryptor.encrypt(""));
        assertNull(PaymentCredentialEncryptor.decrypt(null));
        assertEquals("", PaymentCredentialEncryptor.decrypt(""));
    }

    @Test
    @DisplayName("非本格式短串原样返回（兼容历史明文）")
    void plainStringAsIs() {
        // 长度不足 IV(12)+tag(16)=28 字节，解码后判定非本格式，原样返回
        String shortValue = "just-a-plain-value";
        assertEquals(shortValue, PaymentCredentialEncryptor.decrypt(shortValue));
    }

    @Test
    @DisplayName("篡改密文后解密应返回 null")
    void tamperedCipherReturnsNull() {
        String encrypted = PaymentCredentialEncryptor.encrypt("sensitive-api-v3-key-value");
        // 翻转末尾字符破坏 GCM tag
        char last = encrypted.charAt(encrypted.length() - 1);
        char replaced = last == 'A' ? 'B' : 'A';
        String tampered = encrypted.substring(0, encrypted.length() - 1) + replaced;
        assertNull(PaymentCredentialEncryptor.decrypt(tampered));
    }
}
