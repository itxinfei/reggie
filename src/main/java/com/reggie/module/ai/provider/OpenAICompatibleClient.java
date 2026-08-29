package com.reggie.module.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * OpenAI兼容API Provider，支持OpenAI、通义千问、DeepSeek等兼容OpenAI API格式的服务。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "reggie.ai.enabled", havingValue = "true")
public class OpenAICompatibleClient implements AIClient {

    /** AI配置属性 */
    @Resource
    private AIConfigProperties aiConfig;

    /** JSON序列化工具 */
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();

    /**
     * 初始化AI Provider
     */
    @PostConstruct
    public void init() {
        log.info("AI Provider初始化完成: provider={}, model={}, baseUrl={}",
                aiConfig.getProvider(), aiConfig.getModel(), aiConfig.getBaseUrl());
    }

    /**
     * 发送聊天请求
     *
     * @param messages   消息列表（system/user/assistant）
     * @param maxTokens  最大返回Token数
     * @param temperature 温度参数
     * @return AI响应
     */
    @Override
    public AIChatResponse chat(List<AIMessage> messages, int maxTokens, double temperature) {
        if (!aiConfig.isEnabled()) {
            return AIChatResponse.builder()
                    .content("AI功能未启用，请在配置中开启 reggie.ai.enabled=true")
                    .model("disabled")
                    .build();
        }

        HttpURLConnection conn = null;
        try {
            String apiUrl = aiConfig.getBaseUrl() + "/chat/completions";
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
            conn.setDoOutput(true);
            // 防御性取值：timeout 为原始类型 int（默认 60），若配置为 0 或负数则回退到 15 秒
            int timeoutSec = aiConfig.getTimeout() > 0 ? aiConfig.getTimeout() : 15;
            conn.setConnectTimeout(timeoutSec * 1000);
            conn.setReadTimeout(timeoutSec * 1000);

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
                JsonNode root;
                try (InputStream is = conn.getInputStream()) {
                    root = OBJECT_MAPPER.readTree(is);
                }
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    // 防御性 null 检查：AI 服务可能返回非标准 JSON（缺 message 或 content 字段），
                    // 直接 .get("message").get("content") 会因中间节点为 null 触发 NPE
                    JsonNode choice0 = choices.get(0);
                    JsonNode message = choice0 != null ? choice0.get("message") : null;
                    JsonNode contentNode = message != null ? message.get("content") : null;
                    String content = contentNode != null ? contentNode.asText() : "";
                    int tokensUsed = root.has("usage")
                            ? root.get("usage").path("total_tokens").asInt(0)
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
                String errorMsg;
                try (InputStream es = conn.getErrorStream()) {
                    if (es != null) {
                        JsonNode errorBody = OBJECT_MAPPER.readTree(es);
                        errorMsg = errorBody.has("error")
                                ? errorBody.get("error").get("message").asText()
                                : "HTTP " + responseCode;
                    } else {
                        errorMsg = "HTTP " + responseCode;
                    }
                } catch (IOException ioEx) {
                    errorMsg = "HTTP " + responseCode;
                    log.warn("AI错误响应读取失败", ioEx);
                }
                // 修改点：errorMsg 截断 200 字，防止 token 回显或超长响应体落盘
                log.error("AI请求失败: code={}, error={}", responseCode,
                        truncateError(errorMsg));

                return AIChatResponse.builder()
                        .content("AI服务暂时不可用，请稍后重试。")
                        .model(aiConfig.getModel())
                        .build();
            }
        } catch (Exception e) {
            log.error("AI请求异常", e);
            return AIChatResponse.builder()
                    .content("AI服务暂时不可用，请稍后重试。")
                    .model(aiConfig.getModel())
                    .build();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 获取提供商名称
     *
     * @return 提供商标识
     */
    @Override
    public String getProviderName() {
        return aiConfig.getProvider();
    }

    /**
     * 获取默认模型名称
     *
     * @return 模型名称
     */
    @Override
    public String getDefaultModel() {
        return aiConfig.getModel();
    }

    // 日志脱敏：错误响应体截断到 200 字，防止 token 回显或超长响应体落盘
    private String truncateError(String errorMsg) {
        if (errorMsg == null) {
            return "";
        }
        return errorMsg.length() > 200 ? errorMsg.substring(0, 200) + "..." : errorMsg;
    }
}


