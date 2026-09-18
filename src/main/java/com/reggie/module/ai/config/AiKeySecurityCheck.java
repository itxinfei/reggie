package com.reggie.module.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Base64;

/**
 * AI 密钥安全启动检查。
 * <p>
 * 背景：{@code AiKeyEncryptor} 在 REGGIE_AI_KEY 未配置时回退到代码内置的兜底密钥，
 * 而兜底密钥随仓库公开——生产环境若未配置环境变量，任何人都能用它解密数据库中的
 * 供应商 API Key。因此生产（{@code reggie.ai.require-env-key=true}）必须 fail-fast：
 * REGGIE_AI_KEY 缺失或非法时拒绝启动。
 * </p>
 *
 * @author reggie
 * @since 2026-09-18
 */
@Slf4j
@Component
public class AiKeySecurityCheck {

    @Resource
    private AIConfigProperties aiConfig;

    /**
     * 启动时校验：requireEnvKey 开启时，REGGIE_AI_KEY 必须存在且为合法 32 字节 Base64
     */
    @PostConstruct
    public void validate() {
        if (!aiConfig.isRequireEnvKey()) {
            return;
        }
        String keyBase64 = System.getenv("REGGIE_AI_KEY");
        if (keyBase64 == null || keyBase64.trim().isEmpty()) {
            throw new IllegalStateException("[AI密钥] reggie.ai.require-env-key=true 但未配置 REGGIE_AI_KEY 环境变量，"
                    + "生产环境拒绝启动（防止使用内置兜底密钥解密供应商API Key）");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(keyBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("[AI密钥] REGGIE_AI_KEY 不是合法 Base64，生产环境拒绝启动", e);
        }
        if (key.length != 32) {
            throw new IllegalStateException("[AI密钥] REGGIE_AI_KEY 解码后长度=" + key.length + "（期望 32 字节），生产环境拒绝启动");
        }
        log.info("[AI密钥] REGGIE_AI_KEY 校验通过，加解密使用环境变量密钥");
    }
}
