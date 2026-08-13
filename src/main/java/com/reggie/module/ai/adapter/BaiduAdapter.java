package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 百度文心一言适配器，支持百度 ERNIE Bot 系列模型。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-10
 */
@Slf4j
public class BaiduAdapter extends BaseModelAdapter {

    public static final String FORMAT_ID = "baidu";

    @Override
    public String getFormatId() {
        return FORMAT_ID;
    }

    @Override
    public String getDisplayName() {
        return "百度文心一言（ERNIE Bot 原生 API）";
    }

    @Override
    protected AIChatResponse doChat(List<AIMessage> messages, int maxTokens,
                                     double temperature, AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            // 1) 构建 URL（百度用 access_token 鉴权）
            String baseUrl = normalizeBaseUrl(config.getBaseUrl());
            String apiUrl = baseUrl + "?access_token=" + config.getApiKey();

            // 2) 创建连接（百度无额外请求头）
            conn = createConnection(apiUrl, config, null);

            // 3) 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();

            // 将 messages 转换为百度的 prompt 文本格式
            StringBuilder promptBuilder = new StringBuilder();
            for (AIMessage msg : messages) {
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
            requestBody.put("temperature", resolveTemperature(temperature, config));
            requestBody.put("max_output_tokens", resolveMaxTokens(maxTokens, config));

            String jsonBody = getObjectMapper().writeValueAsString(requestBody);

            // 4) 发送请求
            sendRequestBody(conn, jsonBody);

            // 5) 解析响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return parseResponse(conn, config);
            } else {
                String errorBody = readErrorBody(conn);
                log.error("AI请求[{} / {}]失败: code={}, error={}",
                        config.getProviderCode(), FORMAT_ID, responseCode, errorBody);
                return errorResponse("百度AI服务错误（" + config.getProviderName()
                        + "）：HTTP " + responseCode + " - " + errorBody, config);
            }
        } catch (Exception e) {
            log.error("AI请求[{} / {}]未预期异常", config.getProviderCode(), FORMAT_ID, e);
            return errorResponse("百度AI连接失败（" + config.getProviderName() + "）：" + e.getMessage(), config);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析百度 ERNIE Bot 成功响应
     * <pre>{ result: "..." }</pre>
     */
    private AIChatResponse parseResponse(HttpURLConnection conn, AiProviderConfig config) throws Exception {
        String rawBody = readResponseBody(conn);
        JsonNode root = getObjectMapper().readTree(rawBody);

        String content = root.path("result").asText("");
        if (!content.isEmpty()) {
            return successResponse(content, config.getModelName(), 0);
        }

        // 新版百度 API 也支持 errorMsg 字段
        String errorMsg = root.path("error_msg").asText("");
        if (!errorMsg.isEmpty()) {
            return errorResponse("百度AI返回错误：" + errorMsg, config);
        }

        return errorResponse(config.getProviderName() + "返回了空响应", config);
    }
}
