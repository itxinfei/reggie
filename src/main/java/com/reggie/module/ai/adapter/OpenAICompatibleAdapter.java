package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * OpenAI兼容格式适配器，支持所有实现了OpenAI /v1/chat/completions接口的模型。
 * </p>
 *
 * @author 心飞为你飞
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

                    // 思考模型（如 stepfun step-3.7-flash、DeepSeek-R1）content 可能为空，
                    // 实际文本放在 reasoning_content 字段中
                    if (content.isEmpty()) {
                        String rc = messageNode.path("reasoning_content").asText("");
                        if (!rc.isEmpty()) {
                            content = rc;
                        }
                    }

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

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public String chatStream(List<AIMessage> messages, int maxTokens, double temperature,
                             AiProviderConfig config, StreamCallback callback) throws Exception {
        HttpURLConnection conn = null;
        StringBuilder fullContent = new StringBuilder();
        try {
            String baseUrl = normalizeBaseUrl(config.getBaseUrl());
            String apiUrl = baseUrl + "/chat/completions";

            // 启用流式输出
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Bearer " + config.getApiKey());
            conn = createConnection(apiUrl, config, headers);

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
            requestBody.put("stream", true);

            String jsonBody = getObjectMapper().writeValueAsString(requestBody);
            log.info("AI流式请求[{} / {}]: url={}, model={}, messages={}",
                    config.getProviderCode(), FORMAT_ID, apiUrl, config.getModelName(), msgList.size());

            sendRequestBody(conn, jsonBody);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorBody = readErrorBody(conn);
                log.error("AI流式请求失败: code={}, error={}", responseCode, truncate(errorBody, 500));
                callback.onToken("AI服务请求失败：" + responseCode, true);
                return null;
            }

            // 逐行读取 SSE 流（JDK 1.8 兼容：分开 try-with-resources）
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith(":")) continue;

                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) continue;

                        try {
                            JsonNode root = getObjectMapper().readTree(data);
                            JsonNode choices = root.get("choices");
                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).get("delta");
                                if (delta != null) {
                                    String token = delta.path("content").asText("");
                                    if (!token.isEmpty()) {
                                        fullContent.append(token);
                                        callback.onToken(token, false);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("SSE行解析跳过: {}", truncate(line, 100));
                        }
                    }
                }
            } finally {
                reader.close();
            }

            if (fullContent.length() > 0) {
                log.info("AI流式响应[{} / {}]: totalLength={}",
                        config.getProviderCode(), FORMAT_ID, fullContent.length());
                callback.onToken("", true);
                return fullContent.toString();
            }
            callback.onToken("模型返回了空响应", true);
            return null;
        } catch (Exception e) {
            log.error("AI流式请求[{}]异常", config.getProviderCode(), e);
            callback.onToken("流式输出异常：" + e.getMessage(), true);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
