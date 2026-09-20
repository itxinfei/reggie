package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.util.AiSecretMaskUtils;
import com.reggie.module.ai.util.AiUrlUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
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

    /**
     * 获取 format id。
     * @return 返回结果
     */
    @Override
    public String getFormatId() {
        return FORMAT_ID;
    }

    /**
     * 获取 display name。
     * @return 返回结果
     */
    @Override
    public String getDisplayName() {
        return "OpenAI兼容格式（GPT / DeepSeek / Qwen / GLM / Kimi 等）";
    }

    /**
     * 处理 do chat。
     * @param messages 参数 messages
     * @param maxTokens 参数 maxTokens
     * @param temperature 参数 temperature
     * @param config 参数 config
     * @return 返回结果
     */
    @Override
    protected AIChatResponse doChat(List<AIMessage> messages, int maxTokens,
                                     double temperature, AiProviderConfig config) {
        HttpURLConnection conn = null;
        try {
            // 1) 构建 URL（修改点(2026-09-18)：统一走 AiUrlUtils，裸域名自动补 /v1）
            String apiUrl = AiUrlUtils.resolveEndpoint(config.getBaseUrl(), "/chat/completions");

            // 2) 创建连接
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Bearer " + config.getApiKey());
            conn = createConnection(apiUrl, config, headers);

            // 3) 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());

            List<Map<String, Object>> msgList = new ArrayList<>();
            for (AIMessage msg : messages) {
                msgList.add(buildMessagePayload(msg));
            }
            requestBody.put("messages", msgList);
            requestBody.put("max_tokens", resolveMaxTokens(maxTokens, config));
            requestBody.put("temperature", resolveTemperature(temperature, config));

            String jsonBody = getObjectMapper().writeValueAsString(requestBody);
            log.info("AI请求[{} / {}]: url={}, model={}, messages={}, maxTokens={}, temp={}",
                    config.getProviderCode(), FORMAT_ID, AiSecretMaskUtils.maskUrl(apiUrl), config.getModelName(),
                    msgList.size(), resolveMaxTokens(maxTokens, config));

            // 4) 发送请求
            sendRequestBody(conn, jsonBody);

            // 5) 解析响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return parseResponse(conn, config);
            } else {
                String errorBody = readErrorBody(conn);
                // 修改点：移除 requestBody（jsonBody）参数——requestBody 含用户 prompt 明文，
// 即使截断 500 字仍会泄露用户输入原文；同时截断 errorBody 到 200 字，
// 防止 token 回显或超长响应体落盘。
                log.error("AI请求[{} / {}]失败: url={}, code={}, error={}",
                        config.getProviderCode(), FORMAT_ID, AiSecretMaskUtils.maskUrl(apiUrl), responseCode,
                        truncate(errorBody, 200));
                String userMsg = buildUserFriendlyError(config.getProviderName(), errorBody);
                return errorResponse(userMsg, config);
            }
        } catch (Exception e) {
            // 基类 chat() 方法已处理 SocketTimeout 和 ConnectException
            // 这里捕获其他检查型异常
            // 修改点(2026-09-15)：外网不可达属运行环境问题，降为 WARN 且不打全量堆栈，避免刷屏
            if (AiNetworkFailureUtils.isNetworkFailure(e)) {
                log.warn("AI请求[{} / {}]外部服务不可达（网络环境问题，非应用缺陷）：{}",
                        config.getProviderCode(), FORMAT_ID, e.getMessage());
            } else {
                log.error("AI请求[{} / {}]未预期异常", config.getProviderCode(), FORMAT_ID, e);
            }
            return errorResponse("AI服务连接失败（" + config.getProviderName() + "）：" + e.getMessage(), config);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 构造单条消息体。纯文本保持 String content（兼容性最好）；
     * 携带图片的 user 消息使用多模态 content 数组：
     * <pre>[{type:"text",text}, {type:"image_url",image_url:{url:"data:image/jpeg;base64,..."}}]</pre>
     */
    private Map<String, Object> buildMessagePayload(AIMessage msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", msg.getRole());
        List<String> images = msg.getImageDataUrls();
        boolean multimodal = "user".equals(msg.getRole()) && images != null && !images.isEmpty();
        if (!multimodal) {
            m.put("content", msg.getContent());
            return m;
        }
        List<Map<String, Object>> parts = new ArrayList<>();
        if (msg.getContent() != null && !msg.getContent().isEmpty()) {
            Map<String, Object> textPart = new LinkedHashMap<>();
            textPart.put("type", "text");
            textPart.put("text", msg.getContent());
            parts.add(textPart);
        }
        for (String dataUrl : images) {
            Map<String, Object> imagePart = new LinkedHashMap<>();
            imagePart.put("type", "image_url");
            Map<String, String> imageUrl = new LinkedHashMap<>();
            imageUrl.put("url", dataUrl);
            imagePart.put("image_url", imageUrl);
            parts.add(imagePart);
        }
        m.put("content", parts);
        return m;
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
                AIChatResponse messageResponse = parseMessageContent(choices.get(0).get("message"), root, config);
                if (messageResponse != null) {
                    return messageResponse;
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

            // 修改点(2026-09-18)：Anthropic 原生格式（网关将请求路由到 Claude 上游且未按 OpenAI 格式转换）
            String anthropicContent = extractAnthropicContent(root);
            if (!anthropicContent.isEmpty()) {
                return successResponse(anthropicContent, config.getModelName(), 0);
            }

            // Ollama 兼容：可能 response 字段
            JsonNode responseNode = root.get("response");
            if (responseNode != null && responseNode.isTextual()) {
                return successResponse(responseNode.asText(""), config.getModelName(), 0);
            }

            // 修改点：rawBody 截断 300→50 字，防止响应体泄露
            log.warn("AI响应[{} / {}]无法解析: bodyPreview={}", config.getProviderCode(), FORMAT_ID,
                    truncate(rawBody, 50));
            return errorResponse(config.getProviderName() + "返回了无法识别的响应格式", config);

        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.error("AI接口[{} / {}]返回了非JSON响应: bodyPreview={}",
                    config.getProviderCode(), FORMAT_ID, truncate(e.getMessage(), 200));
            return errorResponse("AI接口地址配置错误（" + config.getProviderName()
                    + "）：服务器返回了非 JSON 格式的响应。请检查「" + config.getBaseUrl()
                    + "」是否为正确的 API 基础地址。", config);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("AI响应[{} / {}]解析异常", config.getProviderCode(), FORMAT_ID, e);
            return errorResponse("AI服务返回异常（" + config.getProviderName() + "）：" + e.getMessage(), config);
        }
    }

    /**
     * 处理 supports streaming。
     * @return 返回结果
     */
    @Override
    public boolean supportsStreaming() {
        return true;
    }

    /**
     * 处理 chat stream。
     * @param messages 参数 messages
     * @param maxTokens 参数 maxTokens
     * @param temperature 参数 temperature
     * @param config 参数 config
     * @param callback 参数 callback
     * @return 返回结果
     */
    @Override
    public String chatStream(List<AIMessage> messages, int maxTokens, double temperature,
                             AiProviderConfig config, StreamCallback callback) throws Exception {
        HttpURLConnection conn = null;
        StringBuilder fullContent = new StringBuilder();
        try {
            // 修改点(2026-09-18)：统一走 AiUrlUtils，裸域名自动补 /v1
            String apiUrl = AiUrlUtils.resolveEndpoint(config.getBaseUrl(), "/chat/completions");

            // 启用流式输出
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Bearer " + config.getApiKey());
            conn = createConnection(apiUrl, config, headers);

            // 中止链路：用户点「停止生成」时断开上游连接，打断阻塞中的 readLine
            final HttpURLConnection streamConn = conn;
            if (callback instanceof AbortableStreamCallback) {
                ((AbortableStreamCallback) callback).registerAbortAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            streamConn.disconnect();
                        } catch (Exception e) {
                            log.debug("中止上游连接失败（可忽略）: {}", e.getMessage());
                        }
                    }
                });
            }

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());

            List<Map<String, Object>> msgList = new ArrayList<>();
            for (AIMessage msg : messages) {
                msgList.add(buildMessagePayload(msg));
            }
            requestBody.put("messages", msgList);
            requestBody.put("max_tokens", resolveMaxTokens(maxTokens, config));
            requestBody.put("temperature", resolveTemperature(temperature, config));
            requestBody.put("stream", true);

            String jsonBody = getObjectMapper().writeValueAsString(requestBody);
            log.info("AI流式请求[{} / {}]: url={}, model={}, messages={}",
                    config.getProviderCode(), FORMAT_ID, AiSecretMaskUtils.maskUrl(apiUrl), config.getModelName(), msgList.size());

            sendRequestBody(conn, jsonBody);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorBody = readErrorBody(conn);
                log.error("AI流式请求失败: code={}, error={}", responseCode, truncate(errorBody, 200));
                callback.onToken("AI服务请求失败：" + responseCode, true);
                return null;
            }

            // 逐行读取 SSE 流（JDK 1.8 兼容：分开 try-with-resources）
            // 修改点(2026-09-18)：部分网关（如将请求路由到 Claude 上游）不按 SSE 返回，
            // 而是整包 application/json —— 按响应类型分流解析
            String contentType = conn.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("application/json")) {
                String body = readResponseBody(conn);
                extractContentFromJson(body, fullContent, config, callback);
            } else {
                readSseStream(conn, fullContent, callback);
            }

            if (fullContent.length() > 0) {
                log.info("AI流式响应[{} / {}]: totalLength={}",
                        config.getProviderCode(), FORMAT_ID, fullContent.length());
                callback.onToken("", true);
                return fullContent.toString();
            }
            // 修改点(2026-09-18)：流式无内容不直接报「模型返回了空响应」，
            // 返回 null 由 AiProviderManager 降级为非流式重试（非流式解析兼容更多格式）
            log.warn("AI流式响应[{} / {}]为空，将降级为非流式重试: url={}, model={}",
                    config.getProviderCode(), FORMAT_ID, AiSecretMaskUtils.maskUrl(apiUrl), config.getModelName());
            return null;
        } catch (Exception e) {
            // 用户主动停止：disconnect 打断 readLine 会抛 SocketException，安静返回，
            // 不推送错误 token（服务层负责把已生成片段以 stopped 状态落库）
            if (callback instanceof AbortableStreamCallback && ((AbortableStreamCallback) callback).isAborted()) {
                log.info("AI流式被用户中止: provider={}, partialLength={}",
                        config.getProviderCode(), fullContent.length());
                return null;
            }
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            // 修改点(2026-09-15)：外网不可达属运行环境问题，降为 WARN 且不打全量堆栈，避免刷屏
            if (AiNetworkFailureUtils.isNetworkFailure(e)) {
                log.warn("AI流式请求[{}]外部服务不可达（网络环境问题，非应用缺陷）：{}",
                        config.getProviderCode(), e.getMessage());
            } else {
                log.error("AI流式请求[{}]异常", config.getProviderCode(), e);
            }
            callback.onToken("流式输出异常：" + e.getMessage(), true);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析 choices[0].message 内容（优先 content，兼容推理模型的 reasoning_content）。
     *
     * @param messageNode choices[0].message 节点
     * @param root 响应根节点（用于取 usage）
     * @param config 供应商配置
     * @return 解析成功返回响应，message 为空返回 null
     */
    private AIChatResponse parseMessageContent(JsonNode messageNode, JsonNode root, AiProviderConfig config) {
        if (messageNode == null) {
            return null;
        }
        String content = messageNode.path("content").asText("");
        int tokensUsed = root.has("usage") ? root.path("usage").path("total_tokens").asInt(0) : 0;

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

    /**
     * 读取 SSE 响应流并逐行处理（等价抽取，降低嵌套层级）。
     *
     * @param conn 已建立的连接
     * @param fullContent 已累积的完整内容
     * @param callback 流式回调
     * @throws IOException 读取流失败
     */
    private void readSseStream(HttpURLConnection conn, StringBuilder fullContent, StreamCallback callback)
            throws IOException {
        InputStream is = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                // 中止链路：停止生成时尽快退出读取循环
                if (callback instanceof AbortableStreamCallback && ((AbortableStreamCallback) callback).isAborted()) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith(":")) {
                    continue;
                }
                // 修改点(2026-09-18)：兼容 "data:{...}"（冒号后无空格）的网关写法
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }
                handleSseDelta(data, fullContent, callback);
            }
        } finally {
            reader.close();
        }
    }

    /**
     * 从整包 JSON 响应中提取文本内容（网关未按 SSE 流式返回时使用）。
     * <p>兼容 OpenAI 标准格式与 Anthropic 原生格式（网关将请求路由到 Claude 上游时常见）。</p>
     *
     * @param body 响应体 JSON 字符串
     * @param fullContent 已累积的完整内容
     * @param config 供应商配置
     * @param callback 流式回调（内容一次性整包回吐）
     */
    private void extractContentFromJson(String body, StringBuilder fullContent, AiProviderConfig config,
                                        StreamCallback callback) {
        try {
            JsonNode root = getObjectMapper().readTree(body);
            String content = extractTextFromRoot(root);
            if (!content.isEmpty()) {
                fullContent.append(content);
                callback.onToken(content, false);
            }
        } catch (Exception e) {
            // 宽异常兜底：非 JSON 响应体留待降级重试
            log.warn("AI流式响应[{} / {}]整包解析失败: {}", config.getProviderCode(), FORMAT_ID,
                    truncate(e.getMessage(), 100));
        }
    }

    /**
     * 从根节点提取文本（OpenAI choices / Anthropic content 两种形态）。
     */
    private String extractTextFromRoot(JsonNode root) {
        // OpenAI 标准：choices[0].message.content
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null) {
                String content = message.path("content").asText("");
                if (content.isEmpty()) {
                    content = message.path("reasoning_content").asText("");
                }
                if (!content.isEmpty()) {
                    return content;
                }
            }
            JsonNode textNode = choices.get(0).get("text");
            if (textNode != null && textNode.isTextual()) {
                return textNode.asText("");
            }
            JsonNode contentNode = choices.get(0).get("content");
            if (contentNode != null && contentNode.isTextual()) {
                return contentNode.asText("");
            }
        }
        // Anthropic 原生：content: [ { type: "text", text: "..." } ]
        String anthropic = extractAnthropicContent(root);
        if (!anthropic.isEmpty()) {
            return anthropic;
        }
        // Ollama 兼容：response 字段
        JsonNode responseNode = root.get("response");
        if (responseNode != null && responseNode.isTextual()) {
            return responseNode.asText("");
        }
        return "";
    }

    /**
     * 提取 Anthropic 原生格式的文本内容。
     * <pre>{ content: [ { type: "text", text: "..." }, ... ] }</pre>
     */
    private String extractAnthropicContent(JsonNode root) {
        JsonNode contentArr = root.get("content");
        if (contentArr == null || !contentArr.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : contentArr) {
            if ("text".equals(block.path("type").asText(""))) {
                sb.append(block.path("text").asText(""));
            }
        }
        return sb.toString();
    }

    /**
     * 解析单条 SSE data 并回吐 token（等价抽取，降低嵌套层级）。
     *
     * @param data SSE data 内容
     * @param fullContent 已累积的完整内容
     * @param callback 流式回调
     */
    private void handleSseDelta(String data, StringBuilder fullContent, StreamCallback callback) {
        try {
            JsonNode root = getObjectMapper().readTree(data);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta == null) {
                    return;
                }
                String token = delta.path("content").asText("");
                if (token.isEmpty()) {
                    return;
                }
                fullContent.append(token);
                callback.onToken(token, false);
                return;
            }
            // 修改点(2026-09-18)：Anthropic 原生 SSE 事件（网关透传 Claude 上游时常见）：
            // {"type":"content_block_delta","delta":{"type":"text_delta","text":"..."}}
            if ("content_block_delta".equals(root.path("type").asText(""))) {
                JsonNode delta = root.get("delta");
                if (delta == null) {
                    return;
                }
                String token = delta.path("text").asText("");
                if (token.isEmpty()) {
                    // 思考模型（extended thinking）的思考块，跳过不推送
                    return;
                }
                fullContent.append(token);
                callback.onToken(token, false);
            }
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.debug("SSE行解析跳过: {}", truncate(data, 100));
        }
    }
}
