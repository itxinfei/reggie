package com.reggie.common;

import java.security.SecureRandom;

/**
 * 安全相关常量
 *
 * @author reggie
 * @since 2026-07-09
 */
public final class SecurityConstants {

    /**
     * 私有构造方法，防止实例化
     */
    private SecurityConstants() {
        throw new AssertionError();
    }

    /**
     * 密码最小长度
     */
    public static final int PASSWORD_MIN_LENGTH = 6;

    /**
     * 密码最大长度
     */
    public static final int PASSWORD_MAX_LENGTH = 20;

    /**
     * 手机号正则
     */
    public static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    /**
     * 登录失败最大次数
     */
    public static final int MAX_LOGIN_FAIL_COUNT = 5;

    /**
     * 登录失败锁定时间（分钟）
     */
    public static final int LOGIN_LOCK_DURATION = 15;

    /**
     * 会话超时时间（秒）
     */
    public static final int SESSION_TIMEOUT = 1800; // 30分钟

    /**
     * 密码类型：MD5（旧）
     */
    public static final String PASSWORD_TYPE_MD5 = "MD5";

    /**
     * 密码类型：BCrypt（新）
     */
    public static final String PASSWORD_TYPE_BCRYPT = "BCRYPT";

    /**
     * 默认密码类型（新用户/重置密码使用）
     */
    public static final String DEFAULT_PASSWORD_TYPE = PASSWORD_TYPE_BCRYPT;

    /**
     * 随机密码字符集（不含易混淆字符：0/O/1/l/I）
     */
    private static final String PASSWORD_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";

    /**
     * 默认密码长度
     */
    private static final int DEFAULT_PASSWORD_LENGTH = 8;

    /**
     * 生成随机密码
     * 用于新员工创建或密码重置，避免硬编码弱密码
     *
     * @return 随机生成的密码
     */
    public static String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(DEFAULT_PASSWORD_LENGTH);
        for (int i = 0; i < DEFAULT_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
