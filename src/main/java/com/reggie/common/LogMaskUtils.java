package com.reggie.common;

/**
 * 日志脱敏工具类
 *
 * @author reggie
 * @since 2026-07-09
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
     * JSON字符串脱敏：自动识别手机号/身份证号/地址等字段并脱敏
     * 用于操作日志等需要记录完整请求参数的场景
     */
    public static String maskSensitiveInfo(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        String result = json;
        // 脱敏手机号
        result = result.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        // 脱敏身份证号（18位）
        result = result.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2");
        // 脱敏地址（较长字符串的后半部分）
        result = maskLongStrings(result);

        return result;
    }

    /**
     * 脱敏JSON中的长字符串字段
     */
    private static String maskLongStrings(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        StringBuilder currentString = new StringBuilder();

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                if (inString) {
                    // 字符串结束
                    if (currentString.length() > 15) {
                        sb.append("\"").append(currentString.substring(0, 6)).append("***")
                          .append(currentString.substring(currentString.length() - 3)).append("\"");
                    } else {
                        sb.append("\"").append(currentString).append("\"");
                    }
                    currentString.setLength(0);
                    inString = false;
                } else {
                    // 字符串开始
                    inString = true;
                }
            } else if (inString) {
                currentString.append(c);
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 通用脱敏（保留前n后m）
     *
     * @param str 原始字符串
     * @param keepPrefix 保留前缀长度
     * @param keepSuffix 保留后缀长度
     * @return 脱敏后的字符串
     */
    private static String maskGeneric(String str, int keepPrefix, int keepSuffix) {
        if (str == null || str.length() <= keepPrefix + keepSuffix) {
            return str;
        }
        int maskLength = str.length() - keepPrefix - keepSuffix;
        return str.substring(0, keepPrefix) + mask(maskLength) + str.substring(str.length() - keepSuffix);
    }

    /**
     * 脱敏字符串末尾部分
     *
     * @param str 原始字符串
     * @param keepPrefix 保留前缀长度
     * @return 脱敏后的字符串
     */
    private static String maskEnd(String str, int keepPrefix) {
        return maskGeneric(str, keepPrefix, 0);
    }

    /**
     * 生成指定长度的掩码字符串
     *
     * @param length 掩码长度
     * @return 由*组成的字符串
     */
    private static String mask(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append('*');
        }
        return sb.toString();
    }
}
