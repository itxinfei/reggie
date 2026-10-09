package com.reggie.utils;

/**
 * <p>
 * 收货地址文本拼接工具：省/市/区 + 明细统一口径。
 * 处理两类脏数据：
 * 1）直辖市 provinceName 与 cityName 相同（"北京市"+"北京市"），相邻重复段只保留一个；
 * 2）存量数据 detail 自带行政区划前缀（如"北京市东城区东华门街道…"），再拼省市区会重复，
 *    拼接前从 detail 头部反复剥离省/市/区词（仅当剩余长度大于该词，避免整串被剥空）。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-09-27
 */
public final class AddressTextUtils {

    private AddressTextUtils() {
        throw new AssertionError();
    }

    /**
     * 拼接完整地址：省市区去重 + 剥离 detail 自带的区划前缀
     *
     * @param province 省名
     * @param city     市名
     * @param district 区名
     * @param detail   详细地址（可能含区划前缀的存量数据）
     * @return 无重复的完整地址
     */
    public static String compose(String province, String city, String district, String detail) {
        String[] regions = {province, city, district};
        StringBuilder sb = new StringBuilder();
        String prev = null;
        for (String region : regions) {
            String s = trimToEmpty(region);
            if (s.isEmpty() || s.equals(prev)) {
                continue;
            }
            sb.append(s);
            prev = s;
        }
        String stripped = stripRegionPrefix(detail, regions);
        if (!stripped.isEmpty() && !stripped.equals(prev)) {
            sb.append(stripped);
        }
        return sb.toString();
    }

    /**
     * 从 detail 头部反复剥离省/市/区词，直到不再以其中任一开头
     */
    public static String stripRegionPrefix(String detail, String... regions) {
        String s = trimToEmpty(detail);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String region : regions) {
                String t = trimToEmpty(region);
                if (!t.isEmpty() && s.length() > t.length() && s.startsWith(t)) {
                    s = s.substring(t.length());
                    changed = true;
                    break;
                }
            }
        }
        return s;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
