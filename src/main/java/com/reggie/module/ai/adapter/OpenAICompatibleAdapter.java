package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpURLConnection;
import java.util.*;

/**
 * OpenAI 兼容格式适配器
 * <p>
 * 支持所有实现了 OpenAI /v1/chat/completions 接口的模型，包括但不限于：
 * </p>
 * <ul>
 *   <li>OpenAI GPT-3.5 / GPT-4 / GPT-4o</li>
 *   <li>DeepSeek (deepseek-chat, deepseek-reasoner)</li>
 *   <li>通义千问 Qwen (qwen-turbo, qwen-plus, qwen-max)</li>
 *   <li>智谱AI GLM (glm-4, glm-4-flash)</li>
 *   <li>Moonshot Kimi (moonshot-v1-8k, moonshot-v1-32k)</li>
 *   <li>零一万物 Yi (yi-large, yi-medium)</li>
 *   <li>MiniMax (abab6.5s-chat)</li>
 *   <li>百川 Baichuan (Baichuan4)</li>
 *   <li>SKylark / 字节豆包</li>
 *   <li>New API / One API 代理网关 (api.iamhc.cn 等)</li>
 *   <li>vLLM / LocalAI / Ollama 等自部署模型</li>
 * </ul>
 *
 * <p><b>请求格式：</b></p>
 * <pre>
 * POST {baseUrl}/chat/completions
 * Authorization: Bearer {apiKey}
 * { model, messages: [{role, content}], max_tokens, temperature }
 * </pre>
 *
 * <p><b>响应格式：</b></p>
 * <pre>
 * { choices: [{ message: { role, content } }], usage: { total_tokens } }
 * </pre>
 *
 * @author reggie
 * @since 2026-07-10
 */
@Slf4j
public class OpenAICompatibleAdapter extends BaseModelAdapter {

    public static final String FORMAT_ID = "openai";

    @Override
    public String getFormatId() {
        return FORMAT_ID;
    }

    @Override
    public String getDisplayName() {
        return "OpenAI兼容格式（GPT / DeepSeek / Qwen / GLM / Kimi 等）";
    }

    @Override
    protected AIChatResponse doChat(List<AIMessage> messages, int maxTokens,
                                     double temperature, AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            // 1) 构建 URL
            String baseUrl = normalizeBaseUrl(config.getBaseUrl());
            String apiUrl = baseUrl + "/chat/completions";

            // 2) 创建连接
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Bearer " + config.getApiKey());
            conn = createConnection(apiUrl, config, headers);

            // 3) 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());

            List<Map<String, String>> msgList = new ArrayList<>();
            for (AIMessage msg : messages) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                msgList.add(m);
            }
            requestBody.put("messages", msgList);
            requestBody.put("max_tokens", resolveMaxTokens(maxTokens, config));
            requestBody.put("temperature", resolveTemperature(temperature, config));

            String jsonBody = getObjectMapper().writeValueAsString(requestBody);
            log.info("AI请求[{} / {}]: url={}, model={}, messages={}, maxTokens={}, temp={}",
                    config.getProviderCode(), FORMAT_ID, apiUrl, config.getModelName(),
                    msgList.size(), resolveMaxTokens(maxTokens, config));

            // 4) 发送请求
            sendRequestBody(conn, jsonBody);

            // 5) 解析响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return parseResponse(conn, config);
            } else {
                String errorBody = readErrorBody(conn);
                log.error("AI请求[{} / {}]失败: url={}, code={}, error={}, requestBody={}",
                        config.getProviderCode(), FORMAT_ID, apiUrl, responseCode, errorBody,
                        truncate(jsonBody, 500));
                String userMsg = buildUserFriendlyError(config.getProviderName(), errorBody);
                return errorResponse(userMsg, config);
            }
        } catch (Exception e) {
            // 基类 chat() 方法已处理 SocketTimeout 和 ConnectException
            // 这里捕获其他检查型异常
            log.error("AI请求[{} / {}]未预期异常", config.getProviderCode(), FORMAT_ID, e);
            return errorResponse("AI服务连接失败（" + config.getProviderName() + "）：" + e.getMessage(), config);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析 OpenAI 兼容格式的成功响应
     * <pre>{ choices: [{ message: { role, content } }], usage: { total_tokens } }</pre>
     */
    private AIChatResponse parseResponse(HttpURLConnection conn, AiProviderConfig config) {
        try {
            String rawBody = readResponseBody(conn);

            JsonNode root = getObjectMapper().readTree(rawBody);

            // 优先解析 choices[0].message.content（标准 OpenAI 格式）
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null) {
                    String content = messageNode.path("content").asText("");
                    int tokensUsed = root.has("usage")
                            ? root.path("usage").path("total_tokens").asInt(0) : 0;

                    log.info("AI响应[{} / {}]: tokensUsed={}, contentLength={}",
                            config.getProviderCode(), FORMAT_ID, tokensUsed, content.length());
                    return successResponse(content, config.getModelName(), tokensUsed);
                }

                // 兼容部分模型直接返回 choices[0].text（文本补全格式）
                JsonNode textNode = choices.get(0).get("text");
                if (textNode != null) {
                    String content = textNode.asText("");
                    return successResponse(content, config.getModelName(), 0);
                }
            }

            // DeepSeek reasoner / o1 等推理模型：可能返回 choices[0].content（无 message 包装）
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode contentNode = choices.get(0).get("content");
                if (contentNode != null && contentNode.isTextual()) {
                    return successResponse(contentNode.asText(""), config.getModelName(), 0);
                }
            }

            // Ollama 兼容：可能 response 字段
            JsonNode responseNode = root.get("response");
            if (responseNode != null && responseNode.isTextual()) {
                return successResponse(responseNode.asText(""), config.getModelName(), 0);
            }

            log.warn("AI响应[{} / {}]无法解析: body={}", config.getProviderCode(), FORMAT_ID,
                    truncate(rawBody, 300));
            return errorResponse(config.getProviderName() + "返回了无法识别的响应格式", config);

        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.error("AI接口[{} / {}]返回了非JSON响应: bodyPreview={}",
                    config.getProviderCode(), FORMAT_ID, truncate(e.getMessage(), 200));
            return errorResponse("AI接口地址配置错误（" + config.getProviderName()
                    + "）：服务器返回了非 JSON 格式的响应。请检查「" + config.getBaseUrl()
                    + "」是否为正确的 API 基础地址。", config);
        } catch (Exception e) {
            log.error("AI响应[{} / {}]解析异常", config.getProviderCode(), FORMAT_ID, e);
            return errorResponse("AI服务返回异常（" + config.getProviderName() + "）：" + e.getMessage(), config);
        }
    }
}
