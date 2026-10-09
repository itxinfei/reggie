package com.reggie.module.platform.util;

import com.reggie.common.LocalDevFallback;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 外卖平台凭据加解密工具（AES-256-GCM）
 * <p>
 * 平台接入需保存 appKey / appSecret / accessToken 等敏感凭据，明文入库会导致泄露。
 * 复用与 AI 密钥一致的 AES-256-GCM 方案：密钥从环境变量 REGGIE_PLATFORM_KEY（Base64 32 字节）读取。
 * 未配置时仅 dev/test/local（或无 Spring 容器的纯单元测试）允许回退到显式命名的 DEV_FALLBACK 密钥并告警；
 * 生产环境由启动检查 {@code PlatformKeySecurityCheck} 与本类 {@link #getKey()} fail-fast 拒绝使用兜底密钥。
 * </p>
 *
 * <p>存储格式：Base64(IV[12字节] + ciphertext + GCM_tag[16字节])</p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
public final class PlatformCredentialEncryptor {

    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    /** 内置兜底密钥（32 字节），仅限 dev/test/local 使用；生产环境缺失主密钥一律 fail-fast 拒绝 */
    private static final byte[] DEV_FALLBACK_KEY_BYTES = new byte[] {
        (byte) 0x72, (byte) 0x65, (byte) 0x67, (byte) 0x67, (byte) 0x69, (byte) 0x65, (byte) 0x2d, (byte) 0x70,
        (byte) 0x6c, (byte) 0x61, (byte) 0x74, (byte) 0x66, (byte) 0x6f, (byte) 0x72, (byte) 0x6d, (byte) 0x2d,
        (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x61, (byte) 0x75, (byte) 0x6c, (byte) 0x74, (byte) 0x2d,
        (byte) 0x61, (byte) 0x65, (byte) 0x73, (byte) 0x32, (byte) 0x35, (byte) 0x36, (byte) 0x2d, (byte) 0x6b
    };
    /** DEV_FALLBACK 兜底密钥告警只打一次 */
    private static volatile boolean devFallbackWarned = false;

    private PlatformCredentialEncryptor() {
    }

    static byte[] getKey() {
        String keyBase64 = System.getenv("REGGIE_PLATFORM_KEY");
        if (keyBase64 != null && !keyBase64.trim().isEmpty()) {
            byte[] key;
            try {
                key = Base64.getDecoder().decode(keyBase64.trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "[平台凭据] REGGIE_PLATFORM_KEY 不是合法 Base64，拒绝使用平台凭据兜底密钥", e);
            }
            if (key.length != 32) {
                throw new IllegalStateException("[平台凭据] REGGIE_PLATFORM_KEY 解码后长度=" + key.length
                        + "（期望 32 字节），拒绝使用平台凭据兜底密钥");
            }
            return key;
        }
        if (LocalDevFallback.isDevLike()) {
            if (!devFallbackWarned) {
                devFallbackWarned = true;
                log.error("[平台凭据] REGGIE_PLATFORM_KEY 未配置，使用内置 DEV_FALLBACK 兜底密钥"
                        + "（该密钥随仓库公开，仅限 dev/test/local，严禁用于生产！）");
            }
            return DEV_FALLBACK_KEY_BYTES;
        }
        throw new IllegalStateException("[平台凭据] REGGIE_PLATFORM_KEY 环境变量未配置："
                + "非 dev/test/local 环境禁止使用内置兜底密钥加解密平台凭据");
    }

    /**
     * 处理 encrypt。
     * @param plain 参数 plain
     * @return 返回结果
     */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        // 取钥放在 try 外：主密钥缺失/非法的 fail-fast 异常不允许被宽异常兜底吞掉
        SecretKeySpec keySpec = new SecretKeySpec(getKey(), ALGO);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[平台凭据] 加密失败", e);
            return null;
        }
    }

    /**
     * 处理 decrypt。
     * @param encrypted 参数 encrypted
     * @return 返回结果
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return encrypted;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);
            if (combined.length < IV_LENGTH + 16) {
                return encrypted;
            }
        } catch (IllegalArgumentException e) {
            return encrypted;
        }
        // 取钥放在 try 外：主密钥缺失/非法的 fail-fast 异常不允许被宽异常兜底吞掉
        SecretKeySpec keySpec = new SecretKeySpec(getKey(), ALGO);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] enc = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, enc, 0, enc.length);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] decrypted = cipher.doFinal(enc);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[平台凭据] 解密失败: prefix={}, error={}",
                    encrypted.substring(0, Math.min(20, encrypted.length())), e.getMessage());
            return null;
        }
    }
}
