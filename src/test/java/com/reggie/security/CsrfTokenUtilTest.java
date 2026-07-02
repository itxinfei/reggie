package com.reggie.security;

import com.reggie.common.CsrfTokenUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CSRF Token 工具类测试
 *
 * @author itxinfei
 */
class CsrfTokenUtilTest {

    @Test
    void testGenerateToken() {
        String token = CsrfTokenUtil.generateToken();
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.length() > 20); // Base64 编码后应该有一定长度
    }

    @Test
    void testGenerateTokenUnique() {
        String token1 = CsrfTokenUtil.generateToken();
        String token2 = CsrfTokenUtil.generateToken();
        assertNotEquals(token1, token2, "每次生成的 Token 应该唯一");
    }

    @Test
    void testValidateToken() {
        String token = CsrfTokenUtil.generateToken();
        assertTrue(CsrfTokenUtil.validateToken(token, token), "相同的 Token 应该验证通过");
        assertFalse(CsrfTokenUtil.validateToken(token, "wrong-token"), "错误的 Token 应该验证失败");
        assertFalse(CsrfTokenUtil.validateToken(null, null), "null Token 应该验证失败");
        assertFalse(CsrfTokenUtil.validateToken(token, null), "预期 Token 为 null 应该验证失败");
        assertFalse(CsrfTokenUtil.validateToken(null, token), "实际 Token 为 null 应该验证失败");
    }

    @Test
    void testExtractTimestamp() {
        String token = CsrfTokenUtil.generateToken();
        long timestamp = CsrfTokenUtil.extractTimestamp(token);
        assertTrue(timestamp > 0, "时间戳应该大于 0");
        assertTrue(System.currentTimeMillis() - timestamp < 1000, "时间戳应该在当前时间附近（1秒内）");
    }

    @Test
    void testIsTokenNotExpired() {
        String token = CsrfTokenUtil.generateToken();
        // 新生成的 Token 应该未过期
        assertTrue(CsrfTokenUtil.isTokenNotExpired(token, 3600 * 1000), "新 Token 应该在 1 小时内未过期");

        // 模拟过期的 Token（使用过去的时间戳）
        long pastTimestamp = System.currentTimeMillis() - 7200 * 1000; // 2小时前
        String expiredToken = createTokenWithTimestamp(pastTimestamp);
        assertFalse(CsrfTokenUtil.isTokenNotExpired(expiredToken, 3600 * 1000), "2小时前的 Token 应该在 1 小时规则下过期");
    }

    @Test
    void testExtractTimestampInvalidToken() {
        assertEquals(0, CsrfTokenUtil.extractTimestamp(null), "null Token 应该返回 0");
        assertEquals(0, CsrfTokenUtil.extractTimestamp(""), "空 Token 应该返回 0");
        assertEquals(0, CsrfTokenUtil.extractTimestamp("invalid-base64"), "无效 Base64 应该返回 0");
    }

    @Test
    void testIsTokenNotExpiredInvalidToken() {
        assertFalse(CsrfTokenUtil.isTokenNotExpired(null, 3600 * 1000), "null Token 应该返回 false");
        assertFalse(CsrfTokenUtil.isTokenNotExpired("", 3600 * 1000), "空 Token 应该返回 false");
    }

    /**
     * 创建带有指定时间戳的 Token（用于测试）
     */
    private String createTokenWithTimestamp(long timestamp) {
        try {
            byte[] randomBytes = new byte[32];
            new java.security.SecureRandom().nextBytes(randomBytes);
            byte[] timestampBytes = Long.toString(timestamp).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] combined = new byte[randomBytes.length + timestampBytes.length];
            System.arraycopy(randomBytes, 0, combined, 0, randomBytes.length);
            System.arraycopy(timestampBytes, 0, combined, randomBytes.length, timestampBytes.length);
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("创建测试 Token 失败", e);
        }
    }
}
