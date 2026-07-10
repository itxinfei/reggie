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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AI供应商管理器（核心调度器）
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

    /** 缓存当前激活的供应商配置（volatile 保证可见性） */
    private volatile AiProviderConfig activeConfig;

    /** reInit 锁对象，防止并发重新加载时多次查库 */
    private final Object reloadLock = new Object();

    /** 最近一次重新加载的时间戳，用于避免高频 reload */
    private volatile long lastReloadTime = 0L;

    @PostConstruct
    public void init() {
        reloadConfig();
        log.info("AI供应商管理器初始化完成，当前供应商: {}",
                activeConfig != null ? activeConfig.getProviderCode() : "application.yml 配置");
    }

    /**
     * 从数据库重新加载激活的供应商配置（线程安全）
     */
    public void reloadConfig() {
        // 防抖动：2秒内不重复加载
        long now = System.currentTimeMillis();
        if (now - lastReloadTime < 2000 && activeConfig != null) {
            return;
        }

        synchronized (reloadLock) {
            // 双重检查，防止在等待锁时已被其他线程加载
            if (now - lastReloadTime < 2000 && activeConfig != null) {
                return;
            }

            AiProviderConfig config = providerConfigMapper.selectOne(
                    new LambdaQueryWrapper<AiProviderConfig>()
                            .eq(AiProviderConfig::getEnabled, true)
                            .eq(AiProviderConfig::getIsActive, true)
                            .eq(AiProviderConfig::getIsDeleted, 0)
                            .last("LIMIT 1")
            );

            if (config != null) {
                // 配置校验
                String validationError = validateConfig(config);
                if (validationError != null) {
                    log.error("AI供应商配置校验失败: provider={}, error={}",
                            config.getProviderCode(), validationError);
                    this.activeConfig = null;
                } else {
                    this.activeConfig = config;
                    log.info("AI供应商已切换: provider={}, model={}, format={}",
                            config.getProviderCode(), config.getModelName(), config.getApiFormat());
                }
            } else {
                this.activeConfig = null;
                log.warn("未找到激活的AI供应商配置，将使用 application.yml 默认配置");
            }

            lastReloadTime = System.currentTimeMillis();
        }
    }

    /**
     * 校验供应商配置完整性
     *
     * @param config 待校验的配置
     * @return null 表示通过，否则返回错误描述
     */

    private String validateConfig(AiProviderConfig config) {
        if (config.getProviderCode() == null || config.getProviderCode().trim().isEmpty()) {
            return "供应商编码不能为空";
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().trim().isEmpty()) {
            return "API基础URL不能为空";
        }
        if (config.getModelName() == null || config.getModelName().trim().isEmpty()) {
            return "模型名称不能为空";
        }
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            return "API密钥未配置，请在后台管理页面设置API Key";
        }
        return null;
    }

    // ==================== AIClient 接口实现 ====================

    @Override
    public AIChatResponse chat(List<com.reggie.module.ai.model.AIMessage> messages, int maxTokens, double temperature) {
        AiProviderConfig config = getActiveConfig();

        // 参数校验
        if (messages == null || messages.isEmpty()) {
            return AIChatResponse.builder()
                    .content("消息列表为空，无法发起对话")
                    .model(config != null ? config.getModelName() : aiConfig.getModel())
                    .build();
        }

        // 对配置进行生效前校验，抛出明确的用户可读错误
        if (config == null) {
            return AIChatResponse.builder()
                    .content("AI功能未配置：没有激活的AI供应商。请前往后台管理 → AI供应商配置 中设置API密钥并激活供应商。")
                    .model("none")
                    .build();
        }

        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return AIChatResponse.builder()
                    .content("AI功能未就绪：供应商「" + config.getProviderName() + "」未配置API密钥，请前往后台管理页面设置。")
                    .model(config.getModelName())
                    .build();
        }

        // 根据 apiFormat 分发到不同的处理器
        String apiFormat = config.getApiFormat();
        if ("openai_compatible".equalsIgnoreCase(apiFormat) || apiFormat == null || apiFormat.isEmpty()) {
            return chatOpenAICompatible(messages, maxTokens, temperature, config);
        } else if ("anthropic".equalsIgnoreCase(apiFormat)) {
            return chatAnthropic(messages, maxTokens, temperature, config);
        } else if ("baidu".equalsIgnoreCase(apiFormat)) {
            return chatBaidu(messages, maxTokens, temperature, config);
        } else if ("360".equalsIgnoreCase(apiFormat)) {
            return chat360(messages, maxTokens, temperature, config);
        } else if ("custom".equalsIgnoreCase(apiFormat)) {
            return chatCustom(messages, maxTokens, temperature, config);
        } else {
            log.error("不支持的API格式: provider={}, format={}", config.getProviderCode(), apiFormat);
            return AIChatResponse.builder()
                    .content("不支持的API格式「" + apiFormat + "」，支持的格式: openai_compatible, anthropic, baidu, 360, custom")
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
            int resolvedMaxTokens = maxTokens > 0 ? maxTokens : (config.getMaxTokens() != null ? config.getMaxTokens() : 2048);
            double resolvedTemperature = temperature >= 0 ? temperature : (config.getTemperature() != null ? config.getTemperature() : 0.7);
            requestBody.put("max_tokens", resolvedMaxTokens);
            requestBody.put("temperature", resolvedTemperature);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.debug("AI请求[{}]: model={}, messages={}, maxTokens={}, temp={}",
                    config.getProviderCode(), config.getModelName(), msgList.size(),
                    resolvedMaxTokens, resolvedTemperature);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return parseOpenAIResponseSafe(conn, config);
            } else {
                String errorMsg = parseErrorResponse(conn);
                log.error("AI请求[{}]失败: code={}, error={}", config.getProviderCode(), responseCode, errorMsg);
                return AIChatResponse.builder()
                        .content("AI服务暂时不可用（" + config.getProviderName() + "）：" + errorMsg)
                        .model(config.getModelName())
                        .build();
            }
        } catch (java.net.SocketTimeoutException e) {
            log.error("AI请求[{}]超时", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("AI服务响应超时（" + config.getProviderName() + "），请稍后重试")
                    .model(config.getModelName())
                    .build();
        } catch (java.net.ConnectException e) {
            log.error("AI请求[{}]连接失败", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("无法连接到AI服务（" + config.getProviderName() + "），请检查网络和API地址")
                    .model(config.getModelName())
                    .build();
        } catch (Exception e) {
            log.error("AI请求[{}]异常", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("AI服务连接失败（" + config.getProviderName() + "）：" + e.getMessage())
                    .model(config.getModelName())
                    .build();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private AIChatResponse parseOpenAIResponse(HttpURLConnection conn, AiProviderConfig config) throws Exception {
        try (InputStream is = conn.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode == null) {
                    return AIChatResponse.builder()
                            .content(config.getProviderName() + "返回了异常格式的响应")
                            .model(config.getModelName())
                            .build();
                }
                String content = messageNode.get("content").asText();
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
                    .content(config.getProviderName() + "返回了空响应，请检查模型是否可用")
                    .model(config.getModelName())
                    .build();
        }
    }

    // ==================== Anthropic Messages API 格式 ====================
    // Anthropic API 特点：
    //   1. 使用 x-api-key 头而非 Bearer Token
    //   2. 必须携带 anthropic-version 头（当前 2023-06-01）
    //   3. max_tokens 是必填参数
    //   4. system prompt 放在顶层字段，而非 messages 数组中
    //   5. 响应 content 是数组 [{type: "text", text: "..."}]

    private AIChatResponse chatAnthropic(List<com.reggie.module.ai.model.AIMessage> messages,
                                          int maxTokens, double temperature, AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            String apiUrl = config.getBaseUrl() + "/messages";
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", config.getApiKey());
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setDoOutput(true);
            conn.setConnectTimeout((config.getTimeout() != null ? config.getTimeout() : 60) * 1000);
            conn.setReadTimeout((config.getTimeout() != null ? config.getTimeout() : 60) * 1000);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());

            // max_tokens 是 Anthropic 的必填参数
            int resolvedMaxTokens = maxTokens > 0 ? maxTokens : (config.getMaxTokens() != null ? config.getMaxTokens() : 2048);
            requestBody.put("max_tokens", resolvedMaxTokens);

            // temperature 可选
            double resolvedTemp = temperature >= 0 ? temperature : (config.getTemperature() != null ? config.getTemperature() : 0.7);
            if (resolvedTemp > 0) {
                requestBody.put("temperature", resolvedTemp);
            }

            // 分离 system 消息和对话消息
            String systemPrompt = null;
            List<Map<String, String>> msgList = new ArrayList<>();
            for (com.reggie.module.ai.model.AIMessage msg : messages) {
                if ("system".equals(msg.getRole())) {
                    // Anthropic 将 system prompt 放在顶层 system 字段（可以是字符串或数组）
                    systemPrompt = (systemPrompt == null ? "" : systemPrompt + "\n") + msg.getContent();
                } else {
                    Map<String, String> m = new LinkedHashMap<>();
                    // Anthropic 仅支持 "user" 和 "assistant" 角色
                    m.put("role", "user".equals(msg.getRole()) || "assistant".equals(msg.getRole())
                            ? msg.getRole() : "user");
                    m.put("content", msg.getContent());
                    msgList.add(m);
                }
            }

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                requestBody.put("system", systemPrompt);
            }
            requestBody.put("messages", msgList);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.debug("Anthropic请求[{}]: model={}, messages={}, systemPrompt={}, maxTokens={}",
                    config.getProviderCode(), config.getModelName(), msgList.size(),
                    systemPrompt != null, resolvedMaxTokens);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try {
                    return parseAnthropicResponse(conn, config);
                } catch (com.fasterxml.jackson.core.JsonParseException e) {
                    String htmlPreview = readBodyAsText200(conn);
                    log.error("Anthropic接口[{}]返回了非JSON响应: url={}, bodyPreview={}",
                            config.getProviderCode(), config.getBaseUrl() + "/messages",
                            truncate(htmlPreview, 200));
                    return AIChatResponse.builder()
                            .content("Anthropic接口地址配置错误（" + config.getProviderName() + "）：服务器返回了非 JSON 格式的响应。" +
                                    "请检查「" + config.getBaseUrl() + "」是否为正确的 API 基础地址。")
                            .model(config.getModelName())
                            .build();
                }
            } else {
                String errorMsg = parseAnthropicErrorResponse(conn);
                log.error("Anthropic请求[{}]失败: code={}, error={}", config.getProviderCode(), responseCode, errorMsg);
                return AIChatResponse.builder()
                        .content("Anthropic AI服务暂时不可用（" + config.getProviderName() + "）：" + errorMsg)
                        .model(config.getModelName())
                        .build();
            }
        } catch (java.net.SocketTimeoutException e) {
            log.error("Anthropic请求[{}]超时", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("Anthropic AI服务响应超时（" + config.getProviderName() + "），请稍后重试")
                    .model(config.getModelName())
                    .build();
        } catch (java.net.ConnectException e) {
            log.error("Anthropic请求[{}]连接失败", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("无法连接到Anthropic AI服务（" + config.getProviderName() + "），请检查网络和API地址")
                    .model(config.getModelName())
                    .build();
        } catch (Exception e) {
            log.error("Anthropic请求[{}]异常", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("Anthropic AI服务连接失败（" + config.getProviderName() + "）：" + e.getMessage())
                    .model(config.getModelName())
                    .build();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析 Anthropic API 成功响应
     * <p>
     * Anthropic 响应格式:
     * { content: [{type: "text", text: "..."}], model: "...", usage: {input_tokens, output_tokens} }
     */
    private AIChatResponse parseAnthropicResponse(HttpURLConnection conn, AiProviderConfig config) throws Exception {
        try (InputStream is = conn.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode contentArray = root.get("content");
            if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
                // 拼接所有 text 类型的 content block
                StringBuilder contentBuilder = new StringBuilder();
                for (JsonNode block : contentArray) {
                    if ("text".equals(block.path("type").asText("")) && block.has("text")) {
                        contentBuilder.append(block.get("text").asText());
                    }
                }
                String content = contentBuilder.toString();

                int inputTokens = root.has("usage")
                        ? root.get("usage").path("input_tokens").asInt(0) : 0;
                int outputTokens = root.has("usage")
                        ? root.get("usage").path("output_tokens").asInt(0) : 0;
                int totalTokens = inputTokens + outputTokens;

                log.info("Anthropic响应[{}]: inputTokens={}, outputTokens={}, contentLength={}",
                        config.getProviderCode(), inputTokens, outputTokens, content.length());
                return AIChatResponse.builder()
                        .content(content)
                        .model(root.path("model").asText(config.getModelName()))
                        .tokensUsed(totalTokens)
                        .build();
            }
            return AIChatResponse.builder()
                    .content(config.getProviderName() + "返回了空响应，请检查模型是否可用")
                    .model(config.getModelName())
                    .build();
        }
    }

    /**
     * 解析 Anthropic API 错误响应
     * <p>
     * Anthropic 错误响应格式:
     * { type: "error", error: { type: "...", message: "..." } }
     */
    private String parseAnthropicErrorResponse(HttpURLConnection conn) {
        InputStream errorStream = null;
        try {
            errorStream = conn.getErrorStream();
            if (errorStream != null) {
                JsonNode root = objectMapper.readTree(errorStream);
                JsonNode error = root.path("error");
                if (error.isObject()) {
                    String msg = error.path("message").asText("");
                    if (!msg.isEmpty()) {
                        return msg;
                    }
                }
                return root.toString();
            }
            return "HTTP " + conn.getResponseCode();
        } catch (Exception e) {
            try {
                return "HTTP " + conn.getResponseCode();
            } catch (Exception ex) {
                return "HTTP error (code unknown)";
            }
        } finally {
            if (errorStream != null) {
                try { errorStream.close(); } catch (Exception ignored) { }
            }
        }
    }

    // ==================== 百度文心一言格式 ====================

    private AIChatResponse chatBaidu(List<com.reggie.module.ai.model.AIMessage> messages,
                                      int maxTokens, double temperature, AiProviderConfig config) {
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
            StringBuilder promptBuilder = new StringBuilder();
            for (com.reggie.module.ai.model.AIMessage msg : messages) {
                if ("system".equals(msg.getRole())) {
                    promptBuilder.append("【系统指令】").append(msg.getContent()).append("\n");
                } else if ("user".equals(msg.getRole())) {
                    promptBuilder.append("用户：").append(msg.getContent()).append("\n");
                } else {
                    promptBuilder.append("助手：").append(msg.getContent()).append("\n");
                }
            }
            String prompt = promptBuilder.toString();

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
                try {
                    JsonNode root = objectMapper.readTree(conn.getInputStream());
                    String content = root.path("result").asText("");
                    return AIChatResponse.builder()
                            .content(content)
                            .model(config.getModelName())
                            .build();
                } catch (com.fasterxml.jackson.core.JsonParseException e) {
                    String htmlPreview = readBodyAsText200(conn);
                    log.error("百度AI[{}]返回了非JSON响应: url={}, bodyPreview={}",
                            config.getProviderCode(), config.getBaseUrl(), truncate(htmlPreview, 200));
                    return AIChatResponse.builder()
                            .content("百度AI接口地址配置错误（" + config.getProviderName() + "）：服务器返回了非 JSON 格式的响应。" +
                                    "请检查「" + config.getBaseUrl() + "」是否为正确的 API 地址。")
                            .model(config.getModelName())
                            .build();
                }
            } else {
                String errorMsg = parseErrorResponse(conn);
                log.error("百度AI[{}]失败: code={}, error={}", config.getProviderCode(), responseCode, errorMsg);
                return AIChatResponse.builder()
                        .content("百度AI服务错误（" + config.getProviderName() + "）：HTTP " + responseCode + " - " + errorMsg)
                        .model(config.getModelName())
                        .build();
            }
        } catch (Exception e) {
            log.error("百度AI[{}]异常", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("百度AI连接失败（" + config.getProviderName() + "）：" + e.getMessage())
                    .model(config.getModelName())
                    .build();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
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
                try {
                    JsonNode root = objectMapper.readTree(conn.getInputStream());
                    String content = root.path("choices").path(0).path("message").path("content").asText("");
                    return AIChatResponse.builder()
                            .content(content)
                            .model(config.getModelName())
                            .build();
                } catch (com.fasterxml.jackson.core.JsonParseException e) {
                    String htmlPreview = readBodyAsText200(conn);
                    log.error("360智脑[{}]返回了非JSON响应: url={}, bodyPreview={}",
                            config.getProviderCode(), config.getBaseUrl(), truncate(htmlPreview, 200));
                    return AIChatResponse.builder()
                            .content("360智脑接口地址配置错误（" + config.getProviderName() + "）：服务器返回了非 JSON 格式的响应。" +
                                    "请检查「" + config.getBaseUrl() + "」是否为正确的 API 地址。")
                            .model(config.getModelName())
                            .build();
                }
            } else {
                String errorMsg = parseErrorResponse(conn);
                log.error("360智脑[{}]失败: code={}, error={}", config.getProviderCode(), responseCode, errorMsg);
                return AIChatResponse.builder()
                        .content("360智脑服务错误（" + config.getProviderName() + "）：HTTP " + responseCode + " - " + errorMsg)
                        .model(config.getModelName())
                        .build();
            }
        } catch (Exception e) {
            log.error("360智脑[{}]异常", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("360智脑连接失败（" + config.getProviderName() + "）：" + e.getMessage())
                    .model(config.getModelName())
                    .build();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // ==================== 自定义格式 ====================

    private AIChatResponse chatCustom(List<com.reggie.module.ai.model.AIMessage> messages,
                                       int maxTokens, double temperature, AiProviderConfig config) {
        // 使用 requestTemplate 和 responsePath 进行通用适配
        // 暂退回 OpenAI 兼容格式
        log.warn("自定义格式[{}]暂未完全适配，回退到 OpenAI 兼容模式", config.getProviderCode());
        return chatOpenAICompatible(messages, maxTokens, temperature, config);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取当前激活的供应商配置（带缓存，线程安全）
     */
    public AiProviderConfig getActiveConfig() {
        // 先读取 volatile 本地副本
        AiProviderConfig cached = activeConfig;

        // 如果缓存不为空但标记为禁用，重新加载
        if (cached != null && cached.getEnabled() != null && !cached.getEnabled()) {
            reloadConfig();
            cached = activeConfig;
        }

        if (cached != null) {
            return cached;
        }

        // 无数据库配置，使用 application.yml 兜底
        // 检查 yml 中 apiKey 是否已配置，未配置则返回 null 让调用方处理
        String ymlApiKey = aiConfig.getApiKey();
        if (ymlApiKey == null || ymlApiKey.trim().isEmpty()) {
            log.warn("application.yml 中未配置 reggie.ai.api-key，AI功能将无法使用");
            return null;
        }

        AiProviderConfig fallback = new AiProviderConfig();
        fallback.setProviderCode(aiConfig.getProvider());
        fallback.setProviderName(aiConfig.getProvider());
        fallback.setBaseUrl(aiConfig.getBaseUrl());
        fallback.setModelName(aiConfig.getModel());
        fallback.setApiKey(ymlApiKey);
        fallback.setTimeout(aiConfig.getTimeout());
        fallback.setMaxTokens(aiConfig.getMaxTokens());
        fallback.setTemperature(aiConfig.getTemperature());
        fallback.setApiFormat("openai_compatible");
        fallback.setEnabled(true);
        return fallback;
    }

    /**
     * 解析错误响应体（线程安全，使用 InputStream 而非 ErrorStream）
     */
    private String parseErrorResponse(HttpURLConnection conn) {
        InputStream errorStream = null;
        try {
            errorStream = conn.getErrorStream();
            if (errorStream != null) {
                JsonNode root = objectMapper.readTree(errorStream);
                JsonNode error = root.path("error");
                if (error.isObject()) {
                    String msg = error.path("message").asText("");
                    if (!msg.isEmpty()) {
                        return msg;
                    }
                }
                return root.toString();
            }
            return "HTTP " + conn.getResponseCode();
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            // 非 JSON 响应（如 HTML 网关错误页），读取文本摘要
            String htmlPreview = readErrorBodyAsText(conn);
            log.warn("错误响应非JSON格式 (HTML?)，原始内容: {}", truncate(htmlPreview, 300));
            return "HTTP " + getResponseCodeSafe(conn) + "，服务器返回了非JSON响应（可能是网关错误页）";
        } catch (Exception e) {
            try {
                return "HTTP " + conn.getResponseCode();
            } catch (Exception ex) {
                return "HTTP error (code unknown)";
            }
        } finally {
            if (errorStream != null) {
                try {
                    errorStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 安全获取 HTTP 状态码（不抛异常）
     */
    private int getResponseCodeSafe(HttpURLConnection conn) {
        try {
            return conn.getResponseCode();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 读取错误响应流为纯文本，用于诊断非 JSON 响应（如 HTML 网关错误页）
     */
    private String readErrorBodyAsText(HttpURLConnection conn) {
        InputStream stream = null;
        try {
            stream = conn.getErrorStream();
            if (stream == null) {
                stream = conn.getInputStream();
            }
            if (stream == null) {
                return "(无响应体)";
            }
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 15) {
                sb.append(line).append("\n");
                lineCount++;
            }
            reader.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return "(无法读取响应体: " + e.getMessage() + ")";
        } finally {
            if (stream != null) {
                try { stream.close(); } catch (Exception ignored) { }
            }
        }
    }

    /**
     * 安全解析 JSON 响应，若为非 JSON 内容（如 HTML）则返回友好错误
     */
    private AIChatResponse parseOpenAIResponseSafe(HttpURLConnection conn, AiProviderConfig config) {
        try {
            return parseOpenAIResponse(conn, config);
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            String htmlPreview = readBodyAsText200(conn);
            log.error("AI接口[{}]返回了非JSON响应(HTTP 200): url={}, bodyPreview={}",
                    config.getProviderCode(), config.getBaseUrl() + "/chat/completions",
                    truncate(htmlPreview, 200));
            return AIChatResponse.builder()
                    .content("AI接口地址配置错误（" + config.getProviderName() + "）：服务器返回了非 JSON 格式的响应。" +
                            "请检查「" + config.getBaseUrl() + "」是否为正确的 API 基础地址。" +
                            "如果是代理/网关地址，请确认路径是否正确。")
                    .model(config.getModelName())
                    .build();
        } catch (Exception e) {
            log.error("AI响应[{}]解析异常", config.getProviderCode(), e);
            return AIChatResponse.builder()
                    .content("AI服务返回异常（" + config.getProviderName() + "）：" + e.getMessage())
                    .model(config.getModelName())
                    .build();
        }
    }

    /**
     * 读取 HTTP 200 的成功响应体前 200 字符，用于错误诊断
     */
    private String readBodyAsText200(HttpURLConnection conn) {
        InputStream stream = null;
        try {
            stream = conn.getInputStream();
            if (stream == null) return "(无响应体)";
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 10) {
                sb.append(line).append("\n");
                lineCount++;
            }
            reader.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return "(无法读取响应体)";
        } finally {
            if (stream != null) {
                try { stream.close(); } catch (Exception ignored) { }
            }
        }
    }

    /**
     * 截断字符串用于日志输出
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
