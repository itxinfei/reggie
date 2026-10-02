package com.reggie.module.platform.config;

import com.reggie.common.LocalDevFallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.crypto.Cipher;
import java.util.Base64;

/**
 * 平台凭据主密钥安全启动检查。
 * <p>
 * 背景：{@code PlatformCredentialEncryptor} 曾于 REGGIE_PLATFORM_KEY 未配置时静默回退到代码内置
 * 兜底密钥，兜底密钥随仓库公开——生产环境未配置环境变量即等于平台凭据明文可解。
 * 现统一口径：dev/test/local 允许回退（显式告警），其余环境启动阶段 fail-fast，
 * REGGIE_PLATFORM_KEY 缺失、非法或当前 JDK 不支持 AES-256 时拒绝启动。
 * </p>
 *
 * @author reggie
 * @since 2026-10-02
 */
@Slf4j
@Component
public class PlatformKeySecurityCheck {

    @Resource
    private org.springframework.core.env.Environment environment;

    /**
     * 启动校验：非 dev/test/local 环境 REGGIE_PLATFORM_KEY 必须为合法 32 字节 Base64 且 JDK 支持 AES-256。
     */
    @PostConstruct
    public void validate() {
        String[] activeProfiles = environment != null ? environment.getActiveProfiles() : new String[0];
        boolean devLike = LocalDevFallback.isDevLikeProfiles(activeProfiles);
        String keyBase64 = System.getenv("REGGIE_PLATFORM_KEY");
        boolean missing = keyBase64 == null || keyBase64.trim().isEmpty();
        if (devLike) {
            if (missing) {
                log.warn("[平台凭据] REGGIE_PLATFORM_KEY 未配置，使用内置 DEV_FALLBACK 兜底密钥"
                        + "（该密钥随仓库公开，仅限 dev/test/local，严禁用于生产！）");
            }
            return;
        }
        if (missing) {
            throw new IllegalStateException("[平台凭据] 未配置 REGGIE_PLATFORM_KEY 环境变量，"
                    + "非 dev/test/local 环境拒绝启动（防止使用内置兜底密钥解密平台凭据）");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(keyBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("[平台凭据] REGGIE_PLATFORM_KEY 不是合法 Base64，拒绝启动", e);
        }
        if (key.length != 32) {
            throw new IllegalStateException("[平台凭据] REGGIE_PLATFORM_KEY 解码后长度=" + key.length
                    + "（期望 32 字节），拒绝启动");
        }
        try {
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            if (maxKeyLen < 256) {
                throw new IllegalStateException("[平台凭据] 当前 JDK 不支持 AES-256（最大密钥长度=" + maxKeyLen
                        + "），请安装 JCE 无限强度策略，拒绝启动");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("[平台凭据] 无法校验 JDK AES 密钥强度，拒绝启动", e);
        }
        log.info("[平台凭据] REGGIE_PLATFORM_KEY 校验通过，加解密使用环境变量密钥");
    }
}
