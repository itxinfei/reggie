package com.reggie.module.ai.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.mapper.AiProviderConfigMapper;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AI供应商管理器
 * 从数据库读取当前激活的供应商配置，动态创建对应的AI Client
 * 支持所有 OpenAI 兼容格式的国产大模型
 *
 * @author reggie
 * @since 2026-07-10
 */
@Slf4j
@Component
public class AiProviderManager implements AIClient {

    @Resource
    private AiProviderConfigMapper providerConfigMapper;

    @Resource
    private AIConfigProperties aiConfig;

    @Resource
    private ObjectMapper objectMapper;

    /** 缓存当前激活的供应商配置 */
    private volatile AiProviderConfig activeConfig;

    @PostConstruct
    public void init() {
        reloadConfig();
        log.info("AI供应商管理器初始化完成");
    }

    /**
     * 从数据库重新加载激活的供应商配置
     */
    public synchronized void reloadConfig() {
        AiProviderConfig config = providerConfigMapper.selectOne(
                new LambdaQueryWrapper<AiProviderConfig>()
                        .eq(AiProviderConfig::getEnabled, true)
                        .eq(AiProviderConfig::getIsActive, true)
                        .eq(AiProviderConfig::getIsDeleted, 0)
                        .last("LIMIT 1")
        );

        if (config != null) {
            this.activeConfig = config;
            log.info("AI供应商已切换: provider={}, model={}, format={}",
                    config.getProviderCode(), config.getModelName(), config.getApiFormat());
        } else {
            // 无数据库配置时使用 application.yml 配置
            this.activeConfig = null;
            log.warn("未找到激活的AI供应商配置，将使用 application.yml 默认配置");
        }
    }

    // ==================== AIClient 接口实现 ====================

    @Override
    public AIChatResponse chat(List<com.reggie.module.ai.model.AIMessage> messages, int maxTokens, double temperature) {
        AiProviderConfig config = getActiveConfig();

        // 根据 apiFormat 分发到不同的处理器
        String apiFormat = config.getApiFormat();
        if ("openai_compatible".equalsIgnoreCase(apiFormat) || apiFormat == null) {
            return chatOpenAICompatible(messages, maxTokens, temperature, config);
        } else if ("baidu".equalsIgnoreCase(apiFormat)) {
            return chatBaidu(messages, maxTokens, temperature, config);
        } else if ("360".equalsIgnoreCase(apiFormat)) {
            return chat360(messages, maxTokens, temperature, config);
        } else if ("custom".equalsIgnoreCase(apiFormat)) {
            return chatCustom(messages, maxTokens, temperature, config);
        } else {
            return AIChatResponse.builder()
                    .content("不支持的API格式: " + apiFormat)
                    .model(config.getModelName())
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        AiProviderConfig config = getActiveConfig();
        return config != null ? config.getProviderCode() : aiConfig.getProvider();
    }

    @Override
    public String getDefaultModel() {
        AiProviderConfig config = getActiveConfig();
        return config != null ? config.getModelName() : aiConfig.getModel();
    }

    // ==================== OpenAI 兼容格式（支持 majority 国产模型）====================

    private AIChatResponse chatOpenAICompatible(List<com.reggie.module.ai.model.AIMessage> messages,
                                                  int maxTokens, double temperature, AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            String apiUrl = config.getBaseUrl() + "/chat/completions";
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout((config.getTimeout() != null ? config.getTimeout() : 60) * 1000);
            conn.setReadTimeout((config.getTimeout() != null ? config.getTimeout() : 60) * 1000);

            // 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());

            List<Map<String, String>> msgList = new ArrayList<>();
            for (com.reggie.module.ai.model.AIMessage msg : messages) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                msgList.add(m);
            }
            requestBody.put("messages", msgList);
            requestBody.put("max_tokens", maxTokens > 0 ? maxTokens : (config.getMaxTokens() != null ? config.getMaxTokens() : 2048));
            requestBody.put("temperature", temperature >= 0 ? temperature : (config.getTemperature() != null ? config.getTemperature() : 0.7));

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.debug("AI请求[{}]: model={}, messages={}", config.getProviderCode(), config.getModelName(), msgList.size());

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                JsonNode root = objectMapper.readTree(conn.getInputStream());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).get("message").get("content").asText();
                    int tokensUsed = root.has("usage")
                            ? root.get("usage").get("total_tokens").asInt() : 0;

                    log.info("AI响应[{}]: tokensUsed={}, contentLength={}", config.getProviderCode(), tokensUsed, content.length());
                    return AIChatResponse.builder()
                            .content(content)
                            .model(config.getModelName())
                            .tokensUsed(tokensUsed)
                            .build();
                }
                return AIChatResponse.builder()
                        .content(config.getProviderCode() + "返回了空响应")
                        .model(config.getModelName())
                        .build();
            } else {
                String errorMsg = parseErrorResponse(conn);
                log.error("AI请求[{}]失败: code={}, error={}", config.getProviderCode(), responseCode, errorMsg);
                return AIChatResponse.builder()
                        .content("AI服务暂时不可用（" + config.getProviderName() + "）：" + errorMsg)
                        .model(config.getModelName())
                        .build();
            }
        } catch (Exception e) {
            log.error("AI请求[{}]异常", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("AI服务连接失败（" + config.getProviderName() + "）：" + e.getMessage())
                    .model(config.getModelName())
                    .build();
        }
    }

    // ==================== 百度文心一言格式 ====================

    private AIChatResponse chatBaidu(List<com.reggie.module.ai.model.AIMessage> messages,
                                      int maxTokens, double temperature, AiProviderConfig config) {
        // 百度API需要 access_token，通过 apiKey 字段存储 client_id:client_secret 获取
        // 简化处理：使用 Bearer Token 方式（如果已配置 access_token）
        HttpURLConnection conn = null;
        try {
            String apiUrl = config.getBaseUrl() + "?access_token=" + config.getApiKey();
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout((config.getTimeout() != null ? config.getTimeout() : 60) * 1000);
            conn.setReadTimeout((config.getTimeout() != null ? config.getTimeout() : 60) * 1000);

            // 构建百度格式请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());

            // 将 messages 转换为百度的 prompt 格式
            String prompt = messages.stream()
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");

            requestBody.put("prompt", prompt);
            requestBody.put("temperature", temperature >= 0 ? temperature : 0.7);
            requestBody.put("max_output_tokens", maxTokens > 0 ? maxTokens : 2048);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                JsonNode root = objectMapper.readTree(conn.getInputStream());
                String content = root.path("result").asText("");
                return AIChatResponse.builder()
                        .content(content)
                        .model(config.getModelName())
                        .build();
            } else {
                return AIChatResponse.builder()
                        .content("百度AI服务错误：HTTP " + responseCode)
                        .model(config.getModelName())
                        .build();
            }
        } catch (Exception e) {
            return AIChatResponse.builder()
                    .content("百度AI连接失败：" + e.getMessage())
                    .model(config.getModelName())
                    .build();
        }
    }

    // ==================== 360智脑格式 ====================

    private AIChatResponse chat360(List<com.reggie.module.ai.model.AIMessage> messages,
                                    int maxTokens, double temperature, AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            String apiUrl = config.getBaseUrl();
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout((config.getTimeout() != null ? config.getTimeout() : 60) * 1000);
            conn.setReadTimeout((config.getTimeout() != null ? config.getTimeout() : 60) * 1000);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());

            List<Map<String, String>> msgList = new ArrayList<>();
            for (com.reggie.module.ai.model.AIMessage msg : messages) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                msgList.add(m);
            }
            requestBody.put("messages", msgList);
            requestBody.put("max_tokens", maxTokens > 0 ? maxTokens : 2048);
            requestBody.put("temperature", temperature >= 0 ? temperature : 0.7);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                JsonNode root = objectMapper.readTree(conn.getInputStream());
                String content = root.path("choices").path(0).path("message").path("content").asText("");
                return AIChatResponse.builder()
                        .content(content)
                        .model(config.getModelName())
                        .build();
            } else {
                return AIChatResponse.builder()
                        .content("360智脑服务错误：HTTP " + responseCode)
                        .model(config.getModelName())
                        .build();
            }
        } catch (Exception e) {
            return AIChatResponse.builder()
                    .content("360智脑连接失败：" + e.getMessage())
                    .model(config.getModelName())
                    .build();
        }
    }

    // ==================== 自定义格式 ====================

    private AIChatResponse chatCustom(List<com.reggie.module.ai.model.AIMessage> messages,
                                       int maxTokens, double temperature, AiProviderConfig config) {
        // 使用 requestTemplate 和 responsePath 进行通用适配
        // 暂退回 OpenAI 兼容格式
        return chatOpenAICompatible(messages, maxTokens, temperature, config);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取当前激活的供应商配置（带缓存）
     */
    public AiProviderConfig getActiveConfig() {
        AiProviderConfig cached = activeConfig;
        if (cached != null && cached.getEnabled() != null && !cached.getEnabled()) {
            reloadConfig();
        }
        AiProviderConfig config = activeConfig;
        if (config != null) {
            return config;
        }

        // 无数据库配置，使用 application.yml 兜底
        AiProviderConfig fallback = new AiProviderConfig();
        fallback.setProviderCode(aiConfig.getProvider());
        fallback.setProviderName(aiConfig.getProvider());
        fallback.setBaseUrl(aiConfig.getBaseUrl());
        fallback.setModelName(aiConfig.getModel());
        fallback.setApiKey(aiConfig.getApiKey());
        fallback.setTimeout(aiConfig.getTimeout());
        fallback.setMaxTokens(aiConfig.getMaxTokens());
        fallback.setTemperature(aiConfig.getTemperature());
        fallback.setApiFormat("openai_compatible");
        return fallback;
    }

    /**
     * 解析错误响应体
     */
    private String parseErrorResponse(HttpURLConnection conn) {
        try {
            JsonNode root = objectMapper.readTree(conn.getErrorStream());
            JsonNode error = root.path("error");
            if (error.isObject()) {
                String msg = error.path("message").asText("");
                if (!msg.isEmpty()) return msg;
            }
            return root.toString();
        } catch (Exception e) {
            return "HTTP " + conn.getResponseCode();
        }
    }
}
