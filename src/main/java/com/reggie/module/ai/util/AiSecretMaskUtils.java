package com.reggie.module.ai.util;

/**
 * AI 供应商请求 URL 脱敏工具。
 * <p>
 * 背景：部分供应商（如百度文心一言旧版 ERNIE 原生 API）要求把密钥放在 URL
 * 查询参数中（{@code ?access_token=*** 若将完整 URL 写入日志或回显给前端，
 * 密钥会随访问日志/代理日志/页面报错信息扩散。
 * </p>
 * <p>
 * 约定：所有对外输出（log / 用户可见错误信息）的 API URL 必须先经过
 * {@link #maskUrl(String)} 脱敏——保留 scheme://host 和路径，仅保留查询参数名、
 * 参数值一律替换为 {@code ***}。
 * </p>
 *
 * @author reggie
 * @since 2026-09-18
 */
public final class AiSecretMaskUtils {

    private AiSecretMaskUtils() {
        // 工具类不允许实例化
    }

    /**
     * 脱敏 URL 中的查询参数值
     *
     * <p>示例：
     * {@code https://aip.baidubce.com/rpc/2.0/ai/complation/chat?access_token=abcd1234}
     * → {@code https://aip.baidubce.com/rpc/2.0/ai/complation/chat?access_token=***}</p>
     *
     * @param url 原始 URL，允许为 null
     * @return 脱敏后的 URL；无查询参数时原样返回
     */
    public static String maskUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        int queryIdx = url.indexOf('?');
        if (queryIdx < 0) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        int paramStart = queryIdx + 1;
        while (paramStart < sb.length()) {
            int amp = sb.indexOf("&", paramStart);
            int eq = sb.indexOf("=", paramStart);
            if (eq < 0 || (amp >= 0 && amp < eq)) {
                // 无值参数（如 ?flag）或非法片段，保持原样
                if (amp < 0) {
                    break;
                }
                paramStart = amp + 1;
                continue;
            }
            int valueEnd = (amp >= 0) ? amp : sb.length();
            if (eq + 1 < valueEnd) {
                sb.replace(eq + 1, valueEnd, "***");
            } else {
                sb.insert(eq + 1, "***");
            }
            if (amp < 0) {
                break;
            }
            paramStart = amp + 1;
        }
        return sb.toString();
    }
}
