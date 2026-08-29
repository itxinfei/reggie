package com.reggie.common;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

/**
 * CSRF Token 生成器
 * 不依赖 Spring Security，轻量级实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
public class CsrfTokenUtil {

    /**
     * 生成 CSRF Token
     *
     * @return CSRF Token
     */
    public static String generateToken() {
        try {
            // 生成随机字节
            SecureRandom random = new SecureRandom();
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);

            // 添加时间戳
            long timestamp = System.currentTimeMillis();
            byte[] timestampBytes = Long.toString(timestamp).getBytes();

            // 合并
            byte[] combined = new byte[bytes.length + timestampBytes.length];
            System.arraycopy(bytes, 0, combined, 0, bytes.length);
            System.arraycopy(timestampBytes, 0, combined, bytes.length, timestampBytes.length);

            // Base64 编码
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new CustomException("生成 CSRF Token 失败");
        }
    }

    /**
     * 验证 CSRF Token
     *
     * @param token  Token
     * @param expected 预期的 Token
     * @return true=验证通过
     */
    public static boolean validateToken(String token, String expected) {
        if (token == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(token.getBytes(), expected.getBytes());
    }

    /**
     * 从 Base64 Token 中提取时间戳
     *
     * @param token CSRF Token
     * @return 时间戳（毫秒）
     */
    public static long extractTimestamp(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            // Token 格式：32字节随机数 + 时间戳字节
            if (decoded.length > 32) {
                byte[] timestampBytes = new byte[decoded.length - 32];
                System.arraycopy(decoded, 32, timestampBytes, 0, timestampBytes.length);
                return Long.parseLong(new String(timestampBytes, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.warn("提取Token时间戳失败", e);
        }
        return 0;
    }

    /**
     * 检查 Token 是否过期
     *
     * @param token CSRF Token
     * @param maxAge 最大有效期（毫秒）
     * @return true=未过期
     */
    public static boolean isTokenNotExpired(String token, long maxAge) {
        long timestamp = extractTimestamp(token);
        if (timestamp == 0) {
            return false;
        }
        long age = System.currentTimeMillis() - timestamp;
        return age < maxAge;
    }
}
