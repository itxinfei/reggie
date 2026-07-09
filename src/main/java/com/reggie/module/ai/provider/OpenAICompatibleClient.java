package com.reggie.module.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * OpenAI兼容API Provider
 * 修改点：添加@ConditionalOnProperty，仅当reggie.ai.enabled=true时激活
 * 支持OpenAI / 通义千问 / DeepSeek 等兼容OpenAI API格式的服务
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "reggie.ai.enabled", havingValue = "true")
public class OpenAICompatibleClient implements AIClient {

    @Resource
    private AIConfigProperties aiConfig;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("AI Provider初始化完成: provider={}, model={}, baseUrl={}",
                aiConfig.getProvider(), aiConfig.getModel(), aiConfig.getBaseUrl());
    }

    @Override
    public AIChatResponse chat(List<AIMessage> messages, int maxTokens, double temperature) {
        if (!aiConfig.isEnabled()) {
            return AIChatResponse.builder()
                    .content("AI功能未启用，请在配置中开启 reggie.ai.enabled=true")
                    .model("disabled")
                    .build();
        }

        try {
            String apiUrl = aiConfig.getBaseUrl() + "/chat/completions";
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout(aiConfig.getTimeout() * 1000);
            conn.setReadTimeout(aiConfig.getTimeout() * 1000);

            // 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", aiConfig.getModel());

            List<Map<String, String>> msgList = new ArrayList<>();
            for (AIMessage msg : messages) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                msgList.add(m);
            }
            requestBody.put("messages", msgList);
            requestBody.put("max_tokens", maxTokens > 0 ? maxTokens : aiConfig.getMaxTokens());
            requestBody.put("temperature", temperature >= 0 ? temperature : aiConfig.getTemperature());

            String jsonBody = OBJECT_MAPPER.writeValueAsString(requestBody);
            log.debug("AI请求: model={}, messages={}", aiConfig.getModel(), msgList.size());

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                JsonNode root = OBJECT_MAPPER.readTree(conn.getInputStream());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).get("message").get("content").asText();
                    int tokensUsed = root.has("usage")
                            ? root.get("usage").get("total_tokens").asInt()
                            : 0;

                    log.info("AI响应成功: tokensUsed={}, contentLength={}", tokensUsed, content.length());
                    return AIChatResponse.builder()
                            .content(content)
                            .model(aiConfig.getModel())
                            .tokensUsed(tokensUsed)
                            .build();
                }
                return AIChatResponse.builder()
                        .content("AI返回了空响应")
                        .model(aiConfig.getModel())
                        .build();
            } else {
                JsonNode errorBody = OBJECT_MAPPER.readTree(conn.getErrorStream());
                String errorMsg = errorBody.has("error")
                        ? errorBody.get("error").get("message").asText()
                        : "HTTP " + responseCode;
                log.error("AI请求失败: code={}, error={}", responseCode, errorMsg);

                return AIChatResponse.builder()
                        .content("AI服务暂时不可用，请稍后重试。（" + errorMsg + "）")
                        .model(aiConfig.getModel())
                        .build();
            }
        } catch (Exception e) {
            log.error("AI请求异常", e);
            return AIChatResponse.builder()
                    .content("AI服务连接失败：" + e.getMessage())
                    .model(aiConfig.getModel())
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return aiConfig.getProvider();
    }

    @Override
    public String getDefaultModel() {
        return aiConfig.getModel();
    }
}
