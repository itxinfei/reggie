package com.reggie.common;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码加密工具类
 * 支持 MD5（旧）和 BCrypt（新）两种加密方式
 */
public class PasswordUtils {

    /**
     * BCrypt强度因子
     */
    private static final int BCRYPT_STRENGTH = 10;

    /**
     * 密码类型：MD5
     */
    public static final String PASSWORD_TYPE_MD5 = "MD5";

    /**
     * 密码类型：BCrypt
     */
    public static final String PASSWORD_TYPE_BCRYPT = "BCRYPT";

    /**
     * 使用BCrypt加密密码
     * @param rawPassword 明文密码
     * @return 加密后的密码
     */
    public static String encodePassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_STRENGTH));
    }

    /**
     * BCrypt密码校验
     * @param rawPassword 明文密码
     * @param encodedPassword BCrypt加密密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }

    /**
     * MD5密码校验（兼容旧系统）
     */
    public static boolean matches(String rawPassword, String encodedPassword, String passwordType) {
        if (PASSWORD_TYPE_MD5.equals(passwordType)) {
            String md5Hex = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
            return md5Hex.equals(encodedPassword);
        }
        return matches(rawPassword, encodedPassword);
    }

    /**
     * 升级密码（如果当前是旧版本，自动升级到新版本）
     * @param rawPassword 明文密码
     * @param encodedPassword 当前加密密码
     * @param passwordType 密码类型
     * @return 新加密密码（如果需要升级）
     */
    public static String upgradeIfNeeded(String rawPassword, String encodedPassword, String passwordType) {
        if (PASSWORD_TYPE_BCRYPT.equals(passwordType)) {
            return encodedPassword;
        }
        if (PASSWORD_TYPE_MD5.equals(passwordType)) {
            String md5Hex = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
            if (md5Hex.equals(encodedPassword)) {
                return encodePassword(rawPassword);
            }
            return null;
        }
        return null;
    }
}
