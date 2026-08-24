package com.reggie.module.ai.util;

import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AI 模块密钥加解密工具（AES-256-GCM）
 * <p>
 * 修复 P0-6：AI 供应商 API Key 原先明文写入数据库，数据库泄露即导致所有 AI 服务密钥暴露。
 * 使用 AES-256-GCM 加密存储，密钥从环境变量 REGGIE_AI_KEY（Base64 编码的 32 字节）读取，
 * 生产环境必须配置。未配置时回退到默认密钥（仅本地开发用）。
 * </p>
 *
 * <p>存储格式：Base64(IV[12字节] + ciphertext + GCM_tag[16字节])</p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
public final class AiKeyEncryptor {

    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    /** 本地开发兜底密钥（生产环境必须通过 REGGIE_AI_KEY 环境变量覆盖） */
    private static final String DEFAULT_KEY_BASE64 = "Y29tLnJlZ2dpZS5haS5wcm92aWRlci5kZWZhdWx0LmtleQ==";

    private AiKeyEncryptor() {
    }

    /**
     * 获取 AES 密钥（32 字节）
     */
    static byte[] getKey() {
        String keyBase64 = System.getenv("REGGIE_AI_KEY");
        if (keyBase64 != null && !keyBase64.trim().isEmpty()) {
            try {
                byte[] key = Base64.getDecoder().decode(keyBase64.trim());
                if (key.length == 32) {
                    return key;
                }
                log.error("[AI密钥] REGGIE_AI_KEY 解码后长度={}，期望 32 字节", key.length);
            } catch (IllegalArgumentException e) {
                log.error("[AI密钥] REGGIE_AI_KEY 不是合法 Base64", e);
            }
        }
        log.warn("[AI密钥] REGGIE_AI_KEY 未配置，使用默认密钥（仅限本地开发）");
        return Base64.getDecoder().decode(DEFAULT_KEY_BASE64);
    }

    /**
     * 加密明文 API Key
     *
     * @param plainApiKey 明文密钥
     * @return 加密后的 Base64 字符串，失败返回 null
     */
    public static String encrypt(String plainApiKey) {
        if (plainApiKey == null || plainApiKey.isEmpty()) {
            return plainApiKey;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(), ALGO);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] encrypted = cipher.doFinal(plainApiKey.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("[AI密钥] 加密失败", e);
            return null;
        }
    }

    /**
     * 解密数据库中存储的加密 API Key
     * <p>兼容迁移前数据：若输入不是合法加密格式，直接当作明文返回。</p>
     *
     * @param encryptedApiKey 加密字符串或明文
     * @return 明文密钥，解密失败返回 null
     */
    public static String decrypt(String encryptedApiKey) {
        if (encryptedApiKey == null || encryptedApiKey.isEmpty()) {
            return encryptedApiKey;
        }
        // 快速判断：加密数据解码后长度 >= IV(12) + tag(16) = 28
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedApiKey);
            if (combined.length < IV_LENGTH + 16) {
                // 太短，当作明文返回（兼容迁移前数据）
                return encryptedApiKey;
            }
        } catch (IllegalArgumentException e) {
            return encryptedApiKey;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(), ALGO);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] combined = Base64.getDecoder().decode(encryptedApiKey);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[AI密钥] 解密失败: prefix={}, error={}",
                    encryptedApiKey.substring(0, Math.min(20, encryptedApiKey.length())), e.getMessage());
            return null;
        }
    }

    /**
     * 原地解密 AiProviderConfig 中的 apiKey 字段
     * <p>供 AiProviderManager 等需要直接使用明文的场景调用</p>
     *
     * @param config 实体对象，apiKey 字段将被替换为明文
     */
    public static void decryptApiKeyInPlace(AiProviderConfig config) {
        if (config != null && config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            String decrypted = decrypt(config.getApiKey());
            config.setApiKey(decrypted);
        }
    }
}