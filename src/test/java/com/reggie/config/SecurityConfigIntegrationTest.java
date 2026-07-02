package com.reggie.config;

import com.reggie.common.LogMaskUtils;
import com.reggie.common.PasswordUtils;
import com.reggie.common.CsrfTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全配置集成测试
 * 验证安全相关组件的配置正确性
 *
 * @author itxinfei
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Test
    void testCsrfTokenGenerationAndValidation() {
        // 测试 CSRF Token 生成和验证的核心逻辑
        String token = CsrfTokenUtil.generateToken();
        assertNotNull(token);
        assertTrue(token.length() > 0);

        // 验证刚生成的 Token（未过期）
        assertTrue(CsrfTokenUtil.validateToken(token, token));

        // 验证无效 Token
        assertFalse(CsrfTokenUtil.validateToken(token, "invalid-token"));

        // 验证 null Token
        assertFalse(CsrfTokenUtil.validateToken(null, null));
        assertFalse(CsrfTokenUtil.validateToken(token, null));
        assertFalse(CsrfTokenUtil.validateToken(null, token));
    }

    @Test
    void testCsrfTokenExpiration() {
        // 测试 Token 时间戳提取
        String token = CsrfTokenUtil.generateToken();
        long timestamp = CsrfTokenUtil.extractTimestamp(token);
        assertTrue(timestamp > 0, "时间戳应该大于0");

        // 验证 Token 未过期（使用1小时有效期）
        assertTrue(CsrfTokenUtil.isTokenNotExpired(token, 3600000));

        // 验证 Token 已过期（使用0毫秒有效期）
        assertFalse(CsrfTokenUtil.isTokenNotExpired(token, 0));
    }

    @Test
    void testCsrfTokenEdgeCases() {
        // 测试边界条件
        assertFalse(CsrfTokenUtil.validateToken(null, null));
        assertFalse(CsrfTokenUtil.validateToken("token", null));
        assertFalse(CsrfTokenUtil.validateToken(null, "token"));
        assertFalse(CsrfTokenUtil.validateToken("invalid-base64!!!", "test"));

        // 时间戳提取
        assertEquals(0, CsrfTokenUtil.extractTimestamp(null));
        assertEquals(0, CsrfTokenUtil.extractTimestamp(""));
        assertEquals(0, CsrfTokenUtil.extractTimestamp("invalid"));

        // Token 过期检查
        assertFalse(CsrfTokenUtil.isTokenNotExpired(null, 3600000));
        assertFalse(CsrfTokenUtil.isTokenNotExpired("", 3600000));
    }

    @Test
    void testCsrfTokenUniqueness() {
        // 测试 Token 唯一性
        String token1 = CsrfTokenUtil.generateToken();
        String token2 = CsrfTokenUtil.generateToken();
        assertNotEquals(token1, token2, "Token 生成应该具有唯一性");
    }

    @Test
    void testPasswordUtilsIntegration() {
        // 测试密码加密集成
        String rawPassword = "test123456";
        String encoded = PasswordUtils.encodePassword(rawPassword);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$2a$"));
        assertTrue(encoded.length() >= 60);

        // 验证密码匹配
        assertTrue(PasswordUtils.matches(rawPassword, encoded));

        // 验证错误密码不匹配
        assertFalse(PasswordUtils.matches("wrongpassword", encoded));
    }

    @Test
    void testLogMaskUtilsIntegration() {
        // 测试日志脱敏集成
        String phone = "13800138000";
        String idCard = "110101199001011234";
        String address = "北京市朝阳区建国路88号";

        // 手机号脱敏
        String maskedPhone = LogMaskUtils.maskPhone(phone);
        assertNotEquals(phone, maskedPhone);
        assertTrue(maskedPhone.contains("****"));
        assertFalse(maskedPhone.contains(phone.substring(3, 7)));

        // 身份证脱敏
        String maskedIdCard = LogMaskUtils.maskIdCard(idCard);
        assertNotEquals(idCard, maskedIdCard);
        assertTrue(maskedIdCard.contains("***********"));

        // 地址脱敏
        String maskedAddress = LogMaskUtils.maskAddress(address);
        assertNotEquals(address, maskedAddress);
        assertTrue(maskedAddress.contains("***"));
    }
}
