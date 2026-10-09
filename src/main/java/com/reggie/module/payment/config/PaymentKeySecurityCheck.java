package com.reggie.module.payment.config;

import com.reggie.common.LocalDevFallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.crypto.Cipher;
import java.util.Base64;

/**
 * 支付凭据主密钥安全启动检查。
 * <p>
 * 背景：{@code PaymentCredentialEncryptor} 曾于 REGGIE_PAYMENT_KEY 未配置时静默回退到代码内置兜底密钥，
 * 兜底密钥随仓库公开——生产环境若未配置环境变量，任何人都能用它解密数据库中的支付密钥/私钥。
 * 现默认 fail-fast（{@code reggie.payment.require-env-key} 默认 true）：非 dev/test/local 环境下
 * REGGIE_PAYMENT_KEY 缺失、非法或当前 JDK 不支持 AES-256 时拒绝启动；显式置 false 可关闭（不推荐）。
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
     *   <li>2026-09-30 新增：prod profile 下 mock-mode=true 拒绝启动 —— mock 模式会整体跳过
     *       支付/平台回调验签，生产误开等于无验签支付回调（匿名凭 tradeNo+金额即可置已支付）。</li>
     *   <li>dev/test/local：允许回退 DEV_FALLBACK 兜底密钥，仅显式告警；</li>
     *   <li>其余环境：requireEnvKey（默认 true）开启时，REGGIE_PAYMENT_KEY 必须为合法 32 字节 Base64，
     *       且 JDK 支持 AES-256；显式 false 视为知情豁免（打 WARN）。</li>
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
        if (paymentConfig.isMockMode() && prodProfile && !isExplicitMockExempt()) {
            throw new IllegalStateException("[支付] reggie.payment.mock-mode=true 在 prod 环境禁止启用："
                    + "mock 模式跳过全部回调验签，等于无验签支付回调，生产环境拒绝启动；"
                    + "如确为隔离的容器演示 / 单机自部署（例如 docker compose 零配置一键体验），"
                    + "须显式设置环境变量 ALLOW_MOCK_IN_PROD=true 表示知情豁免");
        }
        if (paymentConfig.isMockMode() && prodProfile) {
            log.warn("[支付] 已显式设置 ALLOW_MOCK_IN_PROD=true：prod 环境下仍启用 mock 支付并跳过全部回调验签，"
                    + "仅限隔离演示 / 单机自部署，严禁用于对公网提供服务的真实生产环境！");
        }
        if (paymentConfig.isMockMode()) {
            log.warn("[支付] mock-mode=true：支付/平台回调验签已跳过，仅限开发/演示环境，严禁用于生产！");
        }
        String keyBase64 = System.getenv("REGGIE_PAYMENT_KEY");
        boolean keyMissing = keyBase64 == null || keyBase64.trim().isEmpty();
        if (LocalDevFallback.isDevLikeProfiles(activeProfiles)) {
            if (keyMissing) {
                log.warn("[支付凭据] REGGIE_PAYMENT_KEY 未配置，使用内置 DEV_FALLBACK 兜底密钥"
                        + "（该密钥随仓库公开，仅限 dev/test/local，严禁用于生产！）");
            }
            return;
        }
        if (!paymentConfig.isRequireEnvKey()) {
            if (keyMissing) {
                log.warn("[支付凭据] 非 dev/test/local 环境显式配置 reggie.payment.require-env-key=false 且未配置 "
                        + "REGGIE_PAYMENT_KEY：启动放行，但支付凭据加解密会在使用时 fail-fast 抛错"
                        + "（内置 DEV_FALLBACK 兜底密钥仅限 dev/test/local）");
            }
            return;
        }
        if (keyMissing) {
            throw new IllegalStateException("[支付凭据] 未配置 REGGIE_PAYMENT_KEY 环境变量（require-env-key 默认 true），"
                    + "生产环境拒绝启动（防止使用内置兜底密钥解密支付凭据）；如确需关闭检查须显式配置 "
                    + "reggie.payment.require-env-key=false");
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

    /**
     * 隔离演示豁免：仅当显式设置环境变量 {@code ALLOW_MOCK_IN_PROD=true} 时，
     * 才允许 prod profile 与 mock-mode 共存（用于 docker compose 零配置一键体验等单机 / 隔离场景）。
     * 默认不豁免，保持 fail-closed。
     */
    private boolean isExplicitMockExempt() {
        String flag = System.getenv("ALLOW_MOCK_IN_PROD");
        return "true".equalsIgnoreCase(flag == null ? "" : flag.trim());
    }
}
