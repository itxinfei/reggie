package com.reggie.common;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilsTest {

    @Test
    void testEncodePassword() {
        String rawPassword = "123456";
        String encoded = PasswordUtils.encodePassword(rawPassword);
        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        assertTrue(encoded.startsWith("$2a$")); // BCrypt prefix
    }

    @Test
    void testMatchesPassword() {
        String rawPassword = "123456";
        String encoded = PasswordUtils.encodePassword(rawPassword);
        assertTrue(PasswordUtils.matches(rawPassword, encoded));
        assertFalse(PasswordUtils.matches("wrong", encoded));
    }

    @Test
    void testMatchesLegacyMd5() {
        String md5Password = "e10adc3949ba59abbe56e057f20f883e"; // "123456"的MD5
        assertTrue(PasswordUtils.matches("123456", md5Password, PasswordUtils.PASSWORD_TYPE_MD5));
        assertFalse(PasswordUtils.matches("654321", md5Password, PasswordUtils.PASSWORD_TYPE_MD5));
    }

    @Test
    void testUpgradePassword() {
        String md5Password = "e10adc3949ba59abbe56e057f20f883e";
        String newEncoded = PasswordUtils.upgradeIfNeeded("123456", md5Password, PasswordUtils.PASSWORD_TYPE_MD5);
        assertNotNull(newEncoded);
        assertTrue(newEncoded.startsWith("$2a$"));
        assertTrue(PasswordUtils.matches("123456", newEncoded));
    }
}
