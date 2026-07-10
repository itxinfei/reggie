package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.ai.mapper.AiProviderConfigMapper;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.service.AiProviderConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI供应商配置服务实现
 * <p>
 * @author reggie
 * @since 2026-07-09
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
        if (target == null || Integer.valueOf(1).equals(target.getIsDeleted())) {
            log.warn("激活失败：供应商不存在或已删除, id={}", id);
            return false;
        }

        if (Boolean.TRUE.equals(target.getIsActive()) && Boolean.TRUE.equals(target.getEnabled())) {
            log.info("供应商已处于激活状态: code={}", target.getProviderCode());
            return true;
        }

        // 取消所有激活状态
        LambdaQueryWrapper<AiProviderConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderConfig::getEnabled, true)
                .eq(AiProviderConfig::getIsDeleted, 0);
        List<AiProviderConfig> all = this.list(wrapper);
        int deactivatedCount = 0;
        for (AiProviderConfig p : all) {
            if (Boolean.TRUE.equals(p.getIsActive())) {
                p.setIsActive(false);
                this.updateById(p);
                deactivatedCount++;
            }
        }
        log.info("已取消 {} 个供应商的激活状态", deactivatedCount);

        // 激活目标
        target.setIsActive(true);
        target.setEnabled(true);
        boolean result = this.updateById(target);
        log.info("供应商已激活: code={}, name={}, id={}",
                target.getProviderCode(), target.getProviderName(), target.getId());
        return result;
    }

    // ==================== 测试连通性 ====================

    @Override
    public String testProvider(Long id) {
        AiProviderConfig config = this.getById(id);
        if (config == null) {
            return "FAIL: 供应商配置不存在";
        }

        String apiKey = config.getApiKey();
        String apiFormat = config.getApiFormat();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            updateTestResult(config, "fail");
            return "FAIL: 未配置API密钥";
        }

        if ("anthropic".equalsIgnoreCase(apiFormat)) {
            return testAnthropicProvider(config);
        } else if ("baidu".equalsIgnoreCase(apiFormat)) {
            return testBaiduProvider(config);
        } else {
            return testOpenAICompatibleProvider(config);
        }
    }

    /**
     * 测试 OpenAI 兼容格式的供应商（含 360 格式）
     */
    private String testOpenAICompatibleProvider(AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            String testUrl = config.getBaseUrl() + "/chat/completions";
            URL url = new URL(testUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout(config.getTimeout() != null ? config.getTimeout() * 1000 : 15000);
            conn.setReadTimeout(config.getTimeout() != null ? config.getTimeout() * 1000 : 15000);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModelName());
            List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", "ping");
            messages.add(msg);
            body.put("messages", messages);
            body.put("max_tokens", 5);
            String requestBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();

            if (code == 200) {
                updateTestResult(config, "success");
                return "SUCCESS: 连接正常 (HTTP " + code + ")";
            } else if (code == 401 || code == 403) {
                updateTestResult(config, "fail");
                return "FAIL: API密钥无效或无权访问 (HTTP " + code + ")";
            } else if (code == 404) {
                updateTestResult(config, "fail");
                return "FAIL: API地址不存在 (HTTP 404)，请检查 baseUrl 配置";
            } else {
                updateTestResult(config, "fail");
                return "FAIL: HTTP " + code + "，请检查模型名称和API地址";
            }
        } catch (java.net.SocketTimeoutException e) {
            updateTestResult(config, "fail");
            return "FAIL: 连接超时，请检查网络和API地址";
        } catch (java.net.ConnectException e) {
            updateTestResult(config, "fail");
            return "FAIL: 无法连接到服务器，请检查 baseUrl 是否正确";
        } catch (Exception e) {
            updateTestResult(config, "fail");
            return "FAIL: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 测试百度文心一言格式的供应商
     */
    private String testBaiduProvider(AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            String testUrl = config.getBaseUrl() + "?access_token=" + config.getApiKey();
            URL url = new URL(testUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(config.getTimeout() != null ? config.getTimeout() * 1000 : 15000);
            conn.setReadTimeout(config.getTimeout() != null ? config.getTimeout() * 1000 : 15000);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModelName());
            body.put("prompt", "ping");
            body.put("max_output_tokens", 5);
            String requestBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                updateTestResult(config, "success");
                return "SUCCESS: 百度API连接正常 (HTTP " + code + ")";
            } else if (code == 401 || code == 403) {
                updateTestResult(config, "fail");
                return "FAIL: access_token 无效 (HTTP " + code + ")";
            } else {
                updateTestResult(config, "fail");
                return "FAIL: HTTP " + code + "，请检查模型和配置";
            }
        } catch (Exception e) {
            updateTestResult(config, "fail");
            return "FAIL: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 测试 Anthropic Messages API 格式的供应商
     * Anthropic API 使用 x-api-key 头 + anthropic-version 头
     */
    private String testAnthropicProvider(AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            String testUrl = config.getBaseUrl() + "/messages";
            URL url = new URL(testUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", config.getApiKey());
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setDoOutput(true);
            conn.setConnectTimeout(config.getTimeout() != null ? config.getTimeout() * 1000 : 15000);
            conn.setReadTimeout(config.getTimeout() != null ? config.getTimeout() * 1000 : 15000);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModelName());
            body.put("max_tokens", 5);
            List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", "ping");
            messages.add(msg);
            body.put("messages", messages);
            String requestBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();

            if (code == 200) {
                updateTestResult(config, "success");
                return "SUCCESS: Anthropic API 连接正常 (HTTP " + code + ")";
            } else if (code == 401 || code == 403) {
                updateTestResult(config, "fail");
                return "FAIL: API密钥无效或无权访问 (HTTP " + code + ")";
            } else if (code == 404) {
                updateTestResult(config, "fail");
                return "FAIL: API地址不存在 (HTTP 404)，请检查 baseUrl 配置";
            } else {
                updateTestResult(config, "fail");
                return "FAIL: HTTP " + code + "，请检查模型名称和API地址";
            }
        } catch (java.net.SocketTimeoutException e) {
            updateTestResult(config, "fail");
            return "FAIL: 连接超时，请检查网络和API地址";
        } catch (java.net.ConnectException e) {
            updateTestResult(config, "fail");
            return "FAIL: 无法连接到服务器，请检查 baseUrl 是否正确";
        } catch (Exception e) {
            updateTestResult(config, "fail");
            return "FAIL: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 更新测试结果
     */
    private void updateTestResult(AiProviderConfig config, String result) {
        try {
            config.setLastTestTime(LocalDateTime.now());
            config.setLastTestResult(result);
            this.updateById(config);
        } catch (Exception e) {
            log.warn("更新测试结果失败: providerCode={}", config.getProviderCode(), e);
        }
    }
}
