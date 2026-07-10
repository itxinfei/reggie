package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.ai.mapper.AiProviderConfigMapper;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.service.AiProviderConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI供应商配置服务实现
 *
 * @author reggie
 * @since 2026-07-10
 */
@Slf4j
@Service
public class AiProviderConfigServiceImpl extends ServiceImpl<AiProviderConfigMapper, AiProviderConfig> implements AiProviderConfigService {

    // ==================== 查询 ====================

    @Override
    public AiProviderConfig getActiveProvider() {
        LambdaQueryWrapper<AiProviderConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderConfig::getEnabled, true)
                .eq(AiProviderConfig::getIsActive, true)
                .eq(AiProviderConfig::getIsDeleted, 0)
                .orderByDesc(AiProviderConfig::getSort)
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public List<AiProviderConfig> listEnabled() {
        LambdaQueryWrapper<AiProviderConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderConfig::getEnabled, true)
                .eq(AiProviderConfig::getIsDeleted, 0)
                .orderByAsc(AiProviderConfig::getSort);
        return this.list(wrapper);
    }

    // ==================== 切换 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean activateProvider(Long id) {
        AiProviderConfig target = this.getById(id);
        if (target == null || target.getIsDeleted() == 1) {
            return false;
        }

        // 取消所有激活状态
        LambdaQueryWrapper<AiProviderConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderConfig::getEnabled, true)
                .eq(AiProviderConfig::getIsDeleted, 0);
        List<AiProviderConfig> all = this.list(wrapper);
        for (AiProviderConfig p : all) {
            if (p.getIsActive()) {
                p.setIsActive(false);
                this.updateById(p);
            }
        }

        // 激活目标
        target.setIsActive(true);
        target.setEnabled(true);
        return this.updateById(target);
    }

    // ==================== 测试连通性 ====================

    @Override
    public String testProvider(Long id) {
        AiProviderConfig config = this.getById(id);
        if (config == null) {
            return "FAIL: 供应商配置不存在";
        }

        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "FAIL: 未配置API密钥";
        }

        try {
            String testUrl = config.getBaseUrl() + "/chat/completions";
            URL url = new URL(testUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(config.getTimeout() != null ? config.getTimeout() * 1000 : 15000);
            conn.setReadTimeout(config.getTimeout() != null ? config.getTimeout() * 1000 : 15000);

            // 构造最小测试请求
            String requestBody = "{\"model\":\"" + config.getModelName() + "\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}],"
                    + "\"max_tokens\":5}";
            conn.getOutputStream().write(requestBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            int code = conn.getResponseCode();

            // 更新测试结果
            config.setLastTestTime(LocalDateTime.now());
            if (code == 200) {
                config.setLastTestResult("success");
                this.updateById(config);
                return "SUCCESS: 连接正常 (HTTP " + code + ")";
            } else if (code == 401) {
                config.setLastTestResult("fail");
                this.updateById(config);
                return "FAIL: API密钥无效 (HTTP 401)";
            } else {
                config.setLastTestResult("fail");
                this.updateById(config);
                return "FAIL: HTTP " + code;
            }
        } catch (Exception e) {
            try {
                config.setLastTestTime(LocalDateTime.now());
                config.setLastTestResult("fail");
                this.updateById(config);
            } catch (Exception ex) { /* ignore */ }
            return "FAIL: " + e.getMessage();
        }
    }
}
