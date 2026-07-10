package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpURLConnection;
import java.util.*;

/**
 * Anthropic Messages API 适配器
 * <p>
 * 支持 Claude 系列模型：
 * claude-3-opus / claude-3-sonnet / claude-3-haiku / claude-3.5-sonnet 等。
 * </p>
 *
 * <p><b>Anthropic API 特点：</b></p>
 * <ul>
 *   <li>使用 {@code x-api-key} 头而非 {@code Authorization: Bearer}</li>
 *   <li>必须携带 {@code anthropic-version: 2023-06-01} 版本头</li>
 *   <li>{@code max_tokens} 是必填参数</li>
 *   <li>system prompt 放在顶层 {@code system} 字段，而非 messages 数组中</li>
 *   <li>响应 content 是数组 {@code [{type: "text", text: "..."}]}</li>
 * </ul>
 *
 * @author reggie
 * @since 2026-07-10
 */
@Slf4j
public class AnthropicAdapter extends BaseModelAdapter {

    public static final String FORMAT_ID = "anthropic";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Override
    public String getFormatId() {
        return FORMAT_ID;
    }

    @Override
    public String getDisplayName() {
        return "Anthropic Messages API（Claude 系列）";
    }

    @Override
    protected AIChatResponse doChat(List<AIMessage> messages, int maxTokens,
                                     double temperature, AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            // 1) 构建 URL
            String baseUrl = normalizeBaseUrl(config.getBaseUrl());
            String apiUrl = baseUrl + "/messages";

            // 2) 创建连接（Anthropic 专用请求头）
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("x-api-key", config.getApiKey());
            headers.put("anthropic-version", ANTHROPIC_VERSION);
            conn = createConnection(apiUrl, config, headers);

            // 3) 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());

            // max_tokens 是 Anthropic 必填参数
            int resolvedMaxTokens = resolveMaxTokens(maxTokens, config);
            requestBody.put("max_tokens", resolvedMaxTokens);

            double resolvedTemp = resolveTemperature(temperature, config);
            if (resolvedTemp > 0) {
                requestBody.put("temperature", resolvedTemp);
            }

            // 分离 system 消息和对话消息
            String systemPrompt = null;
            List<Map<String, String>> msgList = new ArrayList<>();
            for (AIMessage msg : messages) {
                if ("system".equals(msg.getRole())) {
                    systemPrompt = (systemPrompt == null ? "" : systemPrompt + "\n") + msg.getContent();
                } else {
                    Map<String, String> m = new LinkedHashMap<>();
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

            String jsonBody = getObjectMapper().writeValueAsString(requestBody);
            log.info("AI请求[{} / {}]: url={}, model={}, messages={}, systemPrompt={}, maxTokens={}",
                    config.getProviderCode(), FORMAT_ID, apiUrl, config.getModelName(),
                    msgList.size(), systemPrompt != null, resolvedMaxTokens);

            // 4) 发送请求
            sendRequestBody(conn, jsonBody);

            // 5) 解析响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return parseResponse(conn, config);
            } else {
                String errorBody = readErrorBody(conn);
                log.error("AI请求[{} / {}]失败: url={}, code={}, error={}",
                        config.getProviderCode(), FORMAT_ID, apiUrl, responseCode, errorBody);
                String userMsg = buildUserFriendlyError(config.getProviderName(),
                        parseAnthropicErrorMessage(errorBody));
                return errorResponse(userMsg, config);
            }
        } catch (Exception e) {
            log.error("AI请求[{} / {}]未预期异常", config.getProviderCode(), FORMAT_ID, e);
            return errorResponse("Anthropic AI服务连接失败（" + config.getProviderName() + "）："
                    + e.getMessage(), config);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析 Anthropic 成功响应
     * <pre>{ content: [{type: "text", text: "..."}], usage: {input_tokens, output_tokens} }</pre>
     */
    private AIChatResponse parseResponse(HttpURLConnection conn, AiProviderConfig config) throws Exception {
        String rawBody = readResponseBody(conn);
        JsonNode root = getObjectMapper().readTree(rawBody);

        JsonNode contentArray = root.get("content");
        if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
            StringBuilder contentBuilder = new StringBuilder();
            for (JsonNode block : contentArray) {
                if ("text".equals(block.path("type").asText("")) && block.has("text")) {
                    contentBuilder.append(block.get("text").asText());
                }
            }
            String content = contentBuilder.toString();

            int inputTokens = root.has("usage")
                    ? root.path("usage").path("input_tokens").asInt(0) : 0;
            int outputTokens = root.has("usage")
                    ? root.path("usage").path("output_tokens").asInt(0) : 0;
            int totalTokens = inputTokens + outputTokens;

            log.info("AI响应[{} / {}]: inputTokens={}, outputTokens={}, contentLength={}",
                    config.getProviderCode(), FORMAT_ID, inputTokens, outputTokens, content.length());
            return successResponse(content,
                    root.path("model").asText(config.getModelName()), totalTokens);
        }

        return errorResponse(config.getProviderName() + "返回了空响应，请检查模型是否可用", config);
    }

    /**
     * 解析 Anthropic 错误响应
     * <pre>{ type: "error", error: { type: "...", message: "..." } }</pre>
     */
    private String parseAnthropicErrorMessage(String errorBody) {
        try {
            JsonNode root = getObjectMapper().readTree(errorBody);
            JsonNode error = root.path("error");
            if (error.isObject()) {
                String msg = error.path("message").asText("");
                if (!msg.isEmpty()) {
                    return msg;
                }
            }
            return errorBody;
        } catch (Exception e) {
            return errorBody;
        }
    }
}
