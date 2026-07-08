package com.reggie.common;

/**
 * 日志脱敏工具类
 */
public class LogMaskUtils {

    /**
     * 手机号脱敏
     * 示例：13812341234 -> 138****1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        // 处理带区号的格式 "0592-1234-5678"
        if (phone.contains("-")) {
            String[] parts = phone.split("-");
            if (parts.length == 3) {
                return String.format("%s-%s-%s", parts[0], maskGeneric(parts[1], 2, 2), maskEnd(parts[2], 4));
            }
        }
        // 普通手机号 "13812341234"
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return maskGeneric(phone, 3, 4);
    }

    /**
     * 用户名脱敏
     * 示例：admin -> a*** 或 张三 -> 张*
     */
    public static String maskUsername(String username) {
        if (username == null || username.isEmpty()) {
            return username;
        }
        if (username.length() <= 2) {
            return maskGeneric(username, 1, 0);
        }
        if (username.length() <= 4) {
            return maskGeneric(username, 1, 1);
        }
        return maskGeneric(username, 1, 2);
    }

    /**
     * 身份证号脱敏
     * 示例：110101199001011234 -> 110***********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 地址脱敏
     * 示例：北京市朝阳区建国路88号 -> 北京***88号
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() < 6) {
            return address;
        }
        return address.substring(0, 3) + "***" + address.substring(address.length() - 3);
    }

    /**
     * 通用脱敏（保留前n后m）
     */
    private static String maskGeneric(String str, int keepPrefix, int keepSuffix) {
        if (str == null || str.length() <= keepPrefix + keepSuffix) {
            return str;
        }
        int maskLength = str.length() - keepPrefix - keepSuffix;
        return str.substring(0, keepPrefix) + mask(maskLength) + str.substring(str.length() - keepSuffix);
    }

    private static String maskEnd(String str, int keepPrefix) {
        return maskGeneric(str, keepPrefix, 0);
    }

    private static String mask(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append('*');
        }
        return sb.toString();
    }
}
