package com.reggie.module.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.module.ai.mapper.AiProviderConfigMapper;
import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 一次性数据迁移：把 ai_provider_config 中历史加密存储的 apiKey 批量转回明文。
 * <p>
 * 背景：2026-09-26 移除「数据库 key 加密 + REGGIE_AI_KEY 主密钥」机制
 * （厂家 key 统一在 MySQL 表中明文管理）。本类在启动时幂等执行：
 * 能用历史默认密钥 GCM 解密成功的值回写明文；明文 / 占位假 key（GCM tag
 * 校验失败）保持不动。
 * </p>
 * <p>注意：仅覆盖「未配 REGGIE_AI_KEY、用内置默认密钥加密」的历史数据；
 * 曾用自定义环境变量密钥加密的环境需另行处理。确认各环境无密文后可整类删除。</p>
 *
 * @author reggie
 * @since 2026-09-26
 */
@Slf4j
@Order(0)
@Component
public class AiKeyPlaintextMigrator implements ApplicationRunner {

    /** 历史内置默认主密钥（与被删除的 AiKeyEncryptor.DEFAULT_KEY_BASE64 相同） */
    private static final String LEGACY_DEFAULT_KEY_BASE64 =
            "UmVnZ2llQUlEZWZhdWx0S2V5MjAyNjA5MTdEZXYzMkI=";

    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    @Resource
    private AiProviderConfigMapper providerConfigMapper;

    @Override
    public void run(ApplicationArguments args) {
        List<AiProviderConfig> all;
        try {
            all = providerConfigMapper.selectList(
                    new LambdaQueryWrapper<AiProviderConfig>()
                            .eq(AiProviderConfig::getIsDeleted, 0));
        } catch (Exception e) {
            // 宽异常兜底：部分非 AI 模块测试环境可能未建 ai_provider_config 表，跳过即可
            log.debug("[AI密钥迁移] 未检测到供应商配置表，已跳过: {}", e.getMessage());
            return;
        }

        int migrated = 0;
        for (AiProviderConfig config : all) {
            String stored = config.getApiKey();
            String plain = tryDecrypt(stored);
            if (plain != null && !plain.equals(stored)) {
                AiProviderConfig update = new AiProviderConfig();
                update.setId(config.getId());
                update.setApiKey(plain);
                providerConfigMapper.updateById(update);
                migrated++;
                log.info("[AI密钥迁移] provider={} 的密文已转回明文", config.getProviderCode());
            }
        }
        log.info("[AI密钥迁移] 完成：共扫描 {} 行，迁移 {} 行", all.size(), migrated);
    }

    /**
     * 尝试用历史默认密钥解密。
     *
     * @param stored 库中存储值
     * @return 明文；输入不是合法密文（明文 / 占位假 key）时返回 null
     */
    private String tryDecrypt(String stored) {
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            if (combined.length < IV_LENGTH + 16) {
                return null;
            }
            byte[] keyBytes = Base64.getDecoder().decode(LEGACY_DEFAULT_KEY_BASE64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGO);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS,
                    Arrays.copyOfRange(combined, 0, IV_LENGTH));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            byte[] plainBytes = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // GCM tag 校验失败 = 非该密钥加密的密文（明文 / 占位 key 走这里），属正常情况
            return null;
        }
    }
}
