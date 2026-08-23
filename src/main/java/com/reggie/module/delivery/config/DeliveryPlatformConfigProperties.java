package com.reggie.module.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 外卖平台对接配置属性，从 application.yml 读取 `reggie.delivery.*`。
 * </p>
 *
 * <p>
 * 安全约定（与支付/短信/推送的 mock-mode 一致）：
 * <ul>
 *   <li><b>mock-mode=true</b>（开发/演示，默认）：平台调用与回调验签跳过，仅打印告警；</li>
 *   <li><b>mock-mode=false</b>（生产）：必须配置对应平台的凭证（enabled=true + appId/appSecret/baseUrl），
 *       未配置或未启用时平台操作<b>拒绝执行（fail-closed）</b>，回调验签失败直接拒绝——严禁恒真放行。</li>
 * </ul>
 * </p>
 *
 * @author reggie
 * @since 2026-08-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "reggie.delivery")
public class DeliveryPlatformConfigProperties {

    /** 全局 mock 开关：true=跳过真实平台调用与回调验签（仅限开发/演示） */
    // 修改点：mockMode 默认 false（fail-closed 安全原则）；
// 生产环境若未显式配置 mock-mode，回退到 false 即启用真实配送渠道，
// 避免"忘记配置"导致生产环境静默跳过配送。
private boolean mockMode = false;

    /** 各平台配置，key 为平台标识（MEITUAN / ELEME / DOUYIN） */
    private Map<String, PlatformConfig> platforms = new HashMap<>();

    /**
     * 单个平台的对接凭证。
     */
    @Data
    public static class PlatformConfig {

        /** 是否启用该平台对接（false 时即使 mock-mode=false 也拒绝调用，fail-closed） */
        private boolean enabled = false;

        /** 平台开放平台 AppID（在对应开放平台申请） */
        private String appId = "";

        /** 平台开放平台 AppSecret（用于请求签名） */
        private String appSecret = "";

        /** 商户号（美团/饿了么/抖音商户 ID，按平台要求填写） */
        private String merchantId = "";

        /** 平台开放 API 网关地址，如 https://api-open.meituan.com */
        private String baseUrl = "";

        /** 回调验签 Token（平台回调通知的签名密钥，用于 verifyCallback 校验） */
        private String notifyToken = "";
    }
}
