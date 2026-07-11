package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.ai.mapper.AiProviderConfigMapper;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.service.AiProviderConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

    // ==================== 修改点：Upsert 逻辑 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProviderConfig saveOrUpdateByCode(AiProviderConfig config) {
        if (config.getProviderCode() == null || config.getProviderCode().trim().isEmpty()) {
            throw new IllegalArgumentException("供应商编码不能为空");
        }

        String providerCode = config.getProviderCode().trim();

        // 查询同 providerCode 的所有记录（含已软删），避免唯一键冲突
        LambdaQueryWrapper<AiProviderConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderConfig::getProviderCode, providerCode);
        AiProviderConfig existing = this.getOne(wrapper);

        if (existing != null) {
            // 如果记录被软删，恢复它并启用
            if (Integer.valueOf(1).equals(existing.getIsDeleted())) {
                existing.setIsDeleted(0);
                existing.setEnabled(true);
            }
            // 复用已有 ID，保留现有激活状态，避免唯一键冲突
            config.setId(existing.getId());
            config.setIsActive(existing.getIsActive() != null ? existing.getIsActive() : false);
            if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
                config.setApiKey(existing.getApiKey());
            }
            this.updateById(config);
            log.info("供应商已更新（upsert）: code={}, id={}, name={}",
                    providerCode, existing.getId(), config.getProviderName());
        } else {
            config.setId(null);
            config.setProviderCode(providerCode);
            config.setIsActive(config.getIsActive() != null ? config.getIsActive() : false);
            config.setIsDeleted(0);
            this.save(config);
            log.info("供应商已新增（upsert）: code={}, name={}",
                    providerCode, config.getProviderName());
        }

        return config;
    }

    // ==================== 修改点：拉取模型列表 ====================

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public List<String> fetchModelList(String baseUrl, String apiKey) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            log.warn("fetchModelList: baseUrl 为空");
            return Collections.emptyList();
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("fetchModelList: apiKey 为空");
            return Collections.emptyList();
        }

        HttpURLConnection conn = null;
        try {
            // 规范化 baseUrl，去除末尾斜杠
            String normalizedUrl = baseUrl.replaceAll("/+$", "");
            // 尝试多个可能的模型列表端点
            String[] candidatePaths = {"/models", "/v1/models"};
            List<String> allModels = new ArrayList<>();

            for (String path : candidatePaths) {
                String modelUrl = normalizedUrl + path;
                try {
                    URL url = new URL(modelUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        List<String> models = parseModelListResponse(conn);
                        allModels.addAll(models);
                        // 取到数据就结束，不继续尝试其他路径
                        if (!models.isEmpty()) break;
                    } else if (code == 401 || code == 403) {
                        // 密钥无效，不必尝试其他端点
                        log.warn("fetchModelList: API密钥无效, url={}, code={}", modelUrl, code);
                        break;
                    }
                    // 其他错误（404等），继续尝试下一个路径
                } catch (java.net.SocketTimeoutException e) {
                    log.warn("fetchModelList: 超时 url={}", modelUrl);
                } catch (java.net.ConnectException e) {
                    log.warn("fetchModelList: 无法连接 url={}", modelUrl);
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                        conn = null;
                    }
                }
            }

            // 去重
            List<String> result = new ArrayList<>();
            for (String m : allModels) {
                if (!result.contains(m)) {
                    result.add(m);
                }
            }
            log.info("fetchModelList: 获取到 {} 个模型, baseUrl={}", result.size(), normalizedUrl);
            return result;
        } catch (Exception e) {
            log.error("fetchModelList: 未预期异常, baseUrl={}", baseUrl, e);
            return Collections.emptyList();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析模型列表响应，支持多种格式：
     * <ul>
     *   <li>OpenAI 标准: { data: [{ id: "gpt-4", ... }] }</li>
     *   <li>Ollama: { models: [{ name: "llama2", ... }] }</li>
     *   <li>New API / One API: 同 OpenAI 格式</li>
     *   <li>部分代理: 直接返回 [{ id: "x" }] 数组</li>
     * </ul>
     */
    private List<String> parseModelListResponse(HttpURLConnection conn) {
        List<String> models = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();
            if (body.trim().isEmpty()) {
                return models;
            }

            JsonNode root = OBJECT_MAPPER.readTree(body);

            // 格式1：{ data: [{ id: "model-name" }] } —— OpenAI / New API / One API
            JsonNode data = root.get("data");
            if (data != null && data.isArray()) {
                for (JsonNode node : data) {
                    String modelId = node.path("id").asText("");
                    if (!modelId.isEmpty()) {
                        models.add(modelId);
                    }
                }
                return models;
            }

            // 格式2：{ models: [{ name: "model-name" }] } —— Ollama
            JsonNode modelsNode = root.get("models");
            if (modelsNode != null && modelsNode.isArray()) {
                for (JsonNode node : modelsNode) {
                    String modelName = node.path("name").asText("");
                    if (!modelName.isEmpty()) {
                        models.add(modelName);
                    }
                }
                return models;
            }

            // 格式3：直接是数组 [{ id: "m1" }, { id: "m2" }]
            if (root.isArray()) {
                for (JsonNode node : root) {
                    String id = node.path("id").asText("");
                    if (!id.isEmpty()) {
                        models.add(id);
                    }
                }
                return models;
            }

            // 格式4：{ object: "list", data: [...] } —— DeepSeek 等
            JsonNode dataNode = root.get("data");
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    String modelId = node.path("id").asText("");
                    if (!modelId.isEmpty()) {
                        models.add(modelId);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("解析模型列表响应失败", e);
        }
        return models;
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
