package com.reggie.module.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 支付模块配置属性，从 application.yml 读取 `reggie.payment.*` 配置。
 * </p>
 *
 * <p>
 * 安全约定（与短信/推送的 mock-mode 一致）：
 * <ul>
 *   <li><b>开发环境</b>（默认 mock-mode=true）：回调签名校验跳过，仅打印警告，便于联调演示；</li>
 *   <li><b>生产环境</b>（mock-mode=false）：必须配置支付宝公钥 / 微信 API 密钥做<b>真实签名校验</b>；
 *       密钥缺失时回调一律拒绝（fail-closed），严禁恒真放行。</li>
 * </ul>
 * </p>
 *
 * @author reggie
 * @since 2026-08-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "reggie.payment")
public class PaymentConfigProperties {

    /**
     * 回调签名校验开关：true=跳过签名校验（仅开发/演示环境），false=真实校验（生产必配密钥）。
     * 默认 true，与短信/推送的 mock-mode 语义保持一致。
     */
    // 修改点：mockMode 默认 false（fail-closed 安全原则）；
// 生产环境若未显式配置 mock-mode，回退到 false 即启用真实支付渠道，
// 避免"忘记配置"导致生产环境静默跳过支付。
private boolean mockMode = false;

    /**
     * 支付宝公钥（RSA2，Base64 X.509 格式，不含 PEM 头尾）。
     * 在支付宝开放平台 → 应用 → 开发设置 中获取，用于回调验签。
     */
    private String alipayPublicKey = "";

    /**
     * 微信支付 API 密钥（APIv2 的 key，32 位）。
     * 用于回调签名 MD5/HMAC-SHA256 计算。
     */
    private String wechatApiKey = "";

    /**
     * 微信回调签名类型：MD5（默认）或 HMAC-SHA256。
     * 对应微信支付 APIv2 的 sign_type 参数。
     */
    private String wechatSignType = "MD5";
}
