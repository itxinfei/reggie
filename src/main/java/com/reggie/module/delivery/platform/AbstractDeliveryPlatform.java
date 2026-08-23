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
            log.info("[{}] 平台响应: {}", platformKey(), body == null ? "null" : body);
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
