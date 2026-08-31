package com.reggie.module.delivery.platform;

import cn.hutool.http.HttpUtil;
import com.reggie.module.delivery.config.DeliveryPlatformConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>
 * 外卖平台适配器抽象基类，统一承载以下公共逻辑：
 * </p>
 * <ul>
 *   <li><b>mock 模式</b>：reggie.delivery.mock-mode=true 时跳过真实调用与验签，仅打印告警（开发/演示）；</li>
 *   <li><b>fail-closed</b>：mock-mode=false 但平台未启用/凭证缺失时，平台操作拒绝执行（返回 false），
 *       回调验签拒绝（返回 false），严禁恒真放行；</li>
 *   <li><b>真实 HTTP 调用</b>：凭证齐全时按平台网关地址发起签名 POST 请求（Hutool HttpUtil）。</li>
 * </ul>
 *
 * <p>
 * 子类需实现 {@link #platformKey()} 返回平台标识（MEITUAN/ELEME/DOUYIN），
 * 并在各业务方法中调用 {@link #isMockOrUnconfigured(String)} / {@link #postPlatform(String, Map)}。
 * </p>
 *
 * @author reggie
 * @since 2026-08-15
 */
@Slf4j
public abstract class AbstractDeliveryPlatform implements DeliveryPlatform {

    /** 配送平台配置（凭证、mock 开关） */
    @Autowired
    protected DeliveryPlatformConfigProperties config;

    /**
     * 返回本适配器对应的平台标识（MEITUAN / ELEME / DOUYIN），用于读取 platforms 配置。
     *
     * @return 平台标识
     */
    protected abstract String platformKey();

    /**
     * 判断当前是否处于 mock 模式或平台未配置（fail-closed）。
     *
     * @param action 操作描述（用于日志），如"自动接单"
     * @return true=跳过执行（mock 或未配置）；false=应继续真实执行
     */
    protected boolean isMockOrUnconfigured(String action) {
        if (config.isMockMode()) {
            log.warn("[{}] {} 已跳过（reggie.delivery.mock-mode=true，仅限开发/演示），生产环境需关闭 mock-mode 并配置平台凭证",
                    platformKey(), action);
            return true;
        }
        DeliveryPlatformConfigProperties.PlatformConfig pc = getPlatformConfig();
        if (pc == null) {
            log.error("[{}] {} 被拒绝：平台未启用或凭证缺失（fail-closed）。请配置 reggie.delivery.platforms.{}.*",
                    platformKey(), action, platformKey().toLowerCase());
            return true;
        }
        return false;
    }

    /**
     * 获取本平台配置；平台未启用或凭证缺失时返回 null。
     *
     * @return 平台配置，或 null
     */
    protected DeliveryPlatformConfigProperties.PlatformConfig getPlatformConfig() {
        if (config.getPlatforms() == null) {
            return null;
        }
        DeliveryPlatformConfigProperties.PlatformConfig pc = config.getPlatforms().get(platformKey());
        if (pc == null || !pc.isEnabled()) {
            return null;
        }
        if (isBlank(pc.getBaseUrl()) || isBlank(pc.getAppId()) || isBlank(pc.getAppSecret())) {
            log.warn("[{}] 平台已启用但 baseUrl/appId/appSecret 配置不完整", platformKey());
            return null;
        }
        return pc;
    }

    /**
     * 向平台网关发起签名 POST 请求（表单参数），返回平台响应体。
     * 签名规则（通用约定）：参数按 key 字典序拼接 k=v&k=v...&key=AppSecret，取 MD5 大写。
     * 各平台实际签名规则以对应开放平台文档为准，接入时如有差异仅需覆写此方法。
     *
     * @param action 操作描述（用于日志）
     * @param bizParams 业务参数（不含 appId/appSecret 签名相关公共参数）
     * @return 平台响应体字符串；请求异常返回 null
     */
    protected String postPlatform(String action, Map<String, Object> bizParams) {
        DeliveryPlatformConfigProperties.PlatformConfig pc = getPlatformConfig();
        if (pc == null) {
            log.error("[{}] {} 被拒绝：平台未配置（fail-closed）", platformKey(), action);
            return null;
        }
        try {
            TreeMap<String, Object> params = new TreeMap<>();
            if (bizParams != null) {
                params.putAll(bizParams);
            }
            params.put("appId", pc.getAppId());
            params.put("merchantId", pc.getMerchantId());
            params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            params.put("sign", buildSign(params, pc.getAppSecret()));

            String url = trimTrailingSlash(pc.getBaseUrl()) + "/api/delivery/" + action;
            log.info("[{}] 调用平台接口: {} params={}", platformKey(), url, params.keySet());
            String body = HttpUtil.post(url, params);
            // 响应体可能含手机号/地址/token 等敏感信息，仅记录响应长度和关键标识，脱敏后落日志
            log.info("[{}] 平台响应: length={} chars, action={}", platformKey(),
                    body == null ? 0 : body.length(), action);
            return body;
        } catch (Exception e) {
            log.error("[{}] 调用平台接口异常: action={}, error={}", platformKey(), action, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 通用签名：参数按 key 字典序拼接 {@code k=v&k=v...&key=secret}，MD5 后转大写。
     *
     * @param params 待签名参数
     * @param secret 平台 AppSecret
     * @return 大写 MD5 签名
     */
    private String buildSign(Map<String, Object> params, String secret) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (e.getValue() == null || isBlank(String.valueOf(e.getValue()))) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        sb.append("&key=").append(secret);
        return md5Upper(sb.toString());
    }

    /** 计算字符串的 MD5（大写） */
    protected String md5Upper(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }

    /**
     * 回调签名校验的合法时间窗口（秒）：超过该窗口的请求视为过期，拒绝验签，防重放。
     * 美团/饿了么/抖音开放平台官方均要求回调带 timestamp，5 分钟是通用安全阈值。
     */
    private static final long CALLBACK_TIMESTAMP_WINDOW_SECONDS = 5 * 60L;

    /**
     * 通用回调验签（美团/饿了么/抖音官方签名规范统一实现）。
     *
     * <p>规则：
     * <ol>
     *   <li>排除 sign 自身、值为空/空白 的参数；</li>
     *   <li>剩余参数按 key 字典序升序拼接为 {@code k1=v1&k2=v2...}；</li>
     *   <li>末尾拼接 {@code &key=secret}（secret 为平台回调验签密钥/notifyToken）；</li>
     *   <li>对整串做 MD5 转大写，与回调 sign 大写比对。</li>
     * </ol>
     * 同时校验 timestamp 时效性防重放（缺失或超 5 分钟窗口则拒绝）。</p>
     *
     * @param params       回调参数（含 sign、timestamp）
     * @param secret       平台回调验签密钥（notifyToken）
     * @param platformLabel 平台日志标签（如"美团"）
     * @return true=签名合法且未过期；false=校验失败
     */
    protected boolean verifyCallbackByToken(Map<String, String> params, String secret, String platformLabel) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        if (config.isMockMode()) {
            log.warn("[{}] 回调签名校验已跳过（mock-mode=true，仅限开发/演示），生产环境必须关闭 mock-mode 并配置 notify-token",
                    platformLabel);
            return true;
        }
        if (isBlank(secret)) {
            log.error("[{}] 回调签名校验失败：平台未配置 notify-token（fail-closed）", platformLabel);
            return false;
        }
        String sign = params.get("sign");
        if (sign == null || sign.trim().isEmpty()) {
            log.warn("[{}] 回调缺少 sign 参数", platformLabel);
            return false;
        }
        // timestamp 防重放：缺失或超 5 分钟窗口则拒绝（官方回调均带 timestamp）
        if (!checkCallbackTimestamp(params.get("timestamp"), platformLabel)) {
            return false;
        }
        // 按官方规范：除 sign 外非空参数按 key 字典序拼接
        TreeMap<String, Object> sort = new TreeMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if ("sign".equals(e.getKey())) {
                continue;
            }
            String v = e.getValue();
            if (v != null && !v.trim().isEmpty()) {
                sort.put(e.getKey(), v.trim());
            }
        }
        String computed = md5Upper(buildCallbackSignContent(sort) + "&key=" + secret);
        boolean ok = computed.equals(sign.trim().toUpperCase());
        if (!ok) {
            log.warn("[{}] 回调签名校验失败（签名不匹配）", platformLabel);
        }
        return ok;
    }

    /**
     * 校验回调 timestamp 时效性，防止重放攻击。
     * timestamp 缺失或无法解析为秒级时间戳时拒绝；与当前时间差超过窗口则拒绝。
     */
    private boolean checkCallbackTimestamp(String timestamp, String platformLabel) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            log.warn("[{}] 回调缺少 timestamp 参数，拒绝验签（防重放）", platformLabel);
            return false;
        }
        try {
            long ts = Long.parseLong(timestamp.trim());
            // 兼容秒级与毫秒级时间戳
            if (ts > 1_000_000_000_000L) {
                ts = ts / 1000;
            }
            long now = System.currentTimeMillis() / 1000;
            if (Math.abs(now - ts) > CALLBACK_TIMESTAMP_WINDOW_SECONDS) {
                log.warn("[{}] 回调 timestamp 已过期（差值 {} 秒，窗口 {} 秒），拒绝验签",
                        platformLabel, Math.abs(now - ts), CALLBACK_TIMESTAMP_WINDOW_SECONDS);
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            log.warn("[{}] 回调 timestamp 格式非法: {}，拒绝验签", platformLabel, timestamp);
            return false;
        }
    }

    /** 按官方规范拼接回调签名串：k1=v1&k2=v2...（参数已按 key 字典序排好） */
    private String buildCallbackSignContent(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
