package com.reggie.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF Token 生成器
 * 不依赖 Spring Security，轻量级实现
 *
 * @author itxinfei
 */
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
            throw new RuntimeException("生成 CSRF Token 失败", e);
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
            if (decoded.length > 8) {
                byte[] timestampBytes = new byte[decoded.length - 8];
                System.arraycopy(decoded, 8, timestampBytes, 0, timestampBytes.length);
                return Long.parseLong(new String(timestampBytes));
            }
        } catch (Exception e) {
            // ignore
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
