package com.reggie.module.ai.util;

/**
 * AI 供应商 API 地址解析工具。
 * <p>
 * 修复(2026-09-18)：原先 chat/stream/test 三处各自裸拼 {@code baseUrl + "/chat/completions"}，
 * 用户填裸域名（如 https://api.iamhc.cn）时请求打到网页入口返回 HTML，
 * 报「返回了非JSON响应」。统一收口到本工具，规则：
 * <ol>
 *   <li>baseUrl 已包含完整端点路径（如 https://host/v1/chat/completions）→ 原样返回；</li>
 *   <li>baseUrl 为裸域名（无路径，如 https://api.iamhc.cn）→ 自动补 /v1 前缀（主流网关标准路径）；</li>
 *   <li>baseUrl 已带版本路径（如 https://api.deepseek.com/v1）→ 直接拼接端点。</li>
 * </ol>
 * </p>
 *
 * @author reggie
 * @since 2026-09-18
 */
public final class AiUrlUtils {

    private AiUrlUtils() {
    }

    /**
     * 解析 API 端点完整地址
     *
     * @param baseUrl 用户配置的基础地址
     * @param endpoint 端点路径（如 /chat/completions、/models）
     * @return 完整请求地址；baseUrl 为空时原样返回
     */
    public static String resolveEndpoint(String baseUrl, String endpoint) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return baseUrl;
        }
        String base = baseUrl.trim().replaceAll("/+$", "");
        if (base.endsWith(endpoint)) {
            return base;
        }
        int schemeIdx = base.indexOf("://");
        int pathIdx = schemeIdx >= 0 ? base.indexOf('/', schemeIdx + 3) : base.indexOf('/');
        if (pathIdx < 0) {
            // 裸域名：自动补 /v1（OpenAI 兼容网关的标准路径前缀）
            return base + "/v1" + endpoint;
        }
        return base + endpoint;
    }
}
