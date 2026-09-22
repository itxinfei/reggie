package com.reggie.module.payment.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 支付渠道凭据加解密工具（AES-256-GCM）
 * <p>
 * 支付渠道配置需保存微信 APIv3 密钥、微信商户私钥、支付宝应用私钥等敏感凭据，明文入库会导致泄露。
 * 复用与平台/AI 凭据一致的 AES-256-GCM 方案：密钥从环境变量 REGGIE_PAYMENT_KEY（Base64 32 字节）读取，
 * 未配置时回退默认密钥（仅本地开发）。
 * </p>
 *
 * <p>存储格式：Base64(IV[12字节] + ciphertext + GCM_tag[16字节])</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
public final class PaymentCredentialEncryptor {

    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    /** 内置兜底密钥（32 字节），仅本地开发使用；与平台/AI 模块的兜底密钥不同 */
    private static final String DEFAULT_KEY = "reggie-payment-default-aes256k!!";

    private PaymentCredentialEncryptor() {
    }

    static byte[] getKey() {
        String keyBase64 = System.getenv("REGGIE_PAYMENT_KEY");
        if (keyBase64 != null && !keyBase64.trim().isEmpty()) {
            try {
                byte[] key = Base64.getDecoder().decode(keyBase64.trim());
                if (key.length == 32) {
                    return key;
                }
                log.error("[支付凭据] REGGIE_PAYMENT_KEY 解码后长度={}，期望 32 字节", key.length);
            } catch (IllegalArgumentException e) {
                log.error("[支付凭据] REGGIE_PAYMENT_KEY 不是合法 Base64", e);
            }
        }
        log.warn("[支付凭据] REGGIE_PAYMENT_KEY 未配置，使用默认密钥（仅限本地开发）");
        return DEFAULT_KEY.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 加密明文。
     * @param plain 明文
     * @return 密文；入为空原样返回，异常返回 null
     */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(), ALGO);
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
            log.error("[支付凭据] 加密失败", e);
            return null;
        }
    }

    /**
     * 解密密文。
     * @param encrypted 密文
     * @return 明文；入为空或非本格式密文原样返回，异常返回 null
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
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(), ALGO);
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
            log.error("[支付凭据] 解密失败: prefix={}, error={}",
                    encrypted.substring(0, Math.min(20, encrypted.length())), e.getMessage());
            return null;
        }
    }
}
