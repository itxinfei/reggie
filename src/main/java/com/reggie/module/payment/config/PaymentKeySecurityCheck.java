package com.reggie.module.payment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.crypto.Cipher;
import java.util.Base64;

/**
 * 支付凭据主密钥安全启动检查。
 * <p>
 * 背景：{@code PaymentCredentialEncryptor} 在 REGGIE_PAYMENT_KEY 未配置时回退到代码内置兜底密钥，
 * 兜底密钥随仓库公开——生产环境若未配置环境变量，任何人都能用它解密数据库中的支付密钥/私钥。
 * 因此生产（{@code reggie.payment.require-env-key=true}）必须 fail-fast：
 * REGGIE_PAYMENT_KEY 缺失、非法或当前 JDK 不支持 AES-256 时拒绝启动。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@Component
public class PaymentKeySecurityCheck {

    @Resource
    private PaymentConfigProperties paymentConfig;

    @Resource
    private org.springframework.core.env.Environment environment;

    /**
     * 启动校验：
     * <ul>
     *   <li>requireEnvKey 开启时，REGGIE_PAYMENT_KEY 必须为合法 32 字节 Base64，且 JDK 支持 AES-256；</li>
     *   <li>2026-09-30 新增：prod profile 下 mock-mode=true 拒绝启动 —— mock 模式会整体跳过
     *       支付/平台回调验签，生产误开等于无验签支付回调（匿名凭 tradeNo+金额即可置已支付）。</li>
     * </ul>
     */
    @PostConstruct
    public void validate() {
        // mock-mode 防呆：prod profile 与 mock-mode 互斥（fail-fast），非 prod 的 mock 环境打显式警告
        String[] activeProfiles = environment != null ? environment.getActiveProfiles() : new String[0];
        boolean prodProfile = false;
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                prodProfile = true;
                break;
            }
        }
        if (paymentConfig.isMockMode() && prodProfile) {
            throw new IllegalStateException("[支付] reggie.payment.mock-mode=true 在 prod 环境禁止启用："
                    + "mock 模式跳过全部回调验签，等于无验签支付回调，生产环境拒绝启动");
        }
        if (paymentConfig.isMockMode()) {
            log.warn("[支付] mock-mode=true：支付/平台回调验签已跳过，仅限开发/演示环境，严禁用于生产！");
        }
        if (!paymentConfig.isRequireEnvKey()) {
            return;
        }
        String keyBase64 = System.getenv("REGGIE_PAYMENT_KEY");
        if (keyBase64 == null || keyBase64.trim().isEmpty()) {
            throw new IllegalStateException("[支付凭据] reggie.payment.require-env-key=true 但未配置 REGGIE_PAYMENT_KEY 环境变量，"
                    + "生产环境拒绝启动（防止使用内置兜底密钥解密支付凭据）");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(keyBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("[支付凭据] REGGIE_PAYMENT_KEY 不是合法 Base64，生产环境拒绝启动", e);
        }
        if (key.length != 32) {
            throw new IllegalStateException("[支付凭据] REGGIE_PAYMENT_KEY 解码后长度=" + key.length
                    + "（期望 32 字节），生产环境拒绝启动");
        }
        try {
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            if (maxKeyLen < 256) {
                throw new IllegalStateException("[支付凭据] 当前 JDK 不支持 AES-256（最大密钥长度=" + maxKeyLen
                        + "），请安装 JCE 无限强度策略或改用 AES-128-GCM，生产环境拒绝启动");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("[支付凭据] 无法校验 JDK AES 密钥强度，生产环境拒绝启动", e);
        }
        log.info("[支付凭据] REGGIE_PAYMENT_KEY 校验通过，加解密使用环境变量密钥");
    }
}
