package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.model.ModelTurn;
import com.reggie.module.ai.model.ToolCall;
import com.reggie.module.ai.tool.ToolDefinition;
import com.reggie.module.ai.util.AiSecretMaskUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Anthropic Messages API 适配器，支持 Claude 系列模型。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-10
 */
@Slf4j
public class AnthropicAdapter extends BaseModelAdapter {

    public static final String FORMAT_ID = "anthropic";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

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
        return "Anthropic Messages API（Claude 系列）";
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
            List<Map<String, Object>> msgList = new ArrayList<>();
            for (AIMessage msg : messages) {
                if ("system".equals(msg.getRole())) {
                    systemPrompt = (systemPrompt == null ? "" : systemPrompt + "\n") + msg.getContent();
                } else {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("role", "user".equals(msg.getRole()) || "assistant".equals(msg.getRole())
                            ? msg.getRole() : "user");
                    m.put("content", buildAnthropicContent(msg));
                    msgList.add(m);
                }
            }

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                requestBody.put("system", systemPrompt);
            }
            requestBody.put("messages", msgList);

            String jsonBody = getObjectMapper().writeValueAsString(requestBody);
            log.info("AI请求[{} / {}]: url={}, model={}, messages={}, systemPrompt={}, maxTokens={}",
                    config.getProviderCode(), FORMAT_ID, AiSecretMaskUtils.maskUrl(apiUrl), config.getModelName(),
                    msgList.size(), systemPrompt != null, resolvedMaxTokens);

            // 4) 发送请求
            sendRequestBody(conn, jsonBody);

            // 5) 解析响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return parseResponse(conn, config);
            } else {
                String errorBody = readErrorBody(conn);
                // 修改点：errorBody 截断 200 字，防止 token 回显或超长响应体落盘
                log.error("AI请求[{} / {}]失败: url={}, code={}, error={}",
                        config.getProviderCode(), FORMAT_ID, AiSecretMaskUtils.maskUrl(apiUrl), responseCode,
                        truncate(errorBody, 200));
                String userMsg = buildUserFriendlyError(config.getProviderName(),
                        parseAnthropicErrorMessage(errorBody));
                return errorResponse(userMsg, config);
            }
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            // 修改点(2026-09-15)：外网不可达属运行环境问题，降为 WARN 且不打全量堆栈，避免刷屏
            if (AiNetworkFailureUtils.isNetworkFailure(e)) {
                log.warn("AI请求[{} / {}]外部服务不可达（网络环境问题，非应用缺陷）：{}",
                        config.getProviderCode(), FORMAT_ID, e.getMessage());
            } else {
                log.error("AI请求[{} / {}]未预期异常", config.getProviderCode(), FORMAT_ID, e);
            }
            return errorResponse("Anthropic AI服务连接失败（" + config.getProviderName() + "）："
                    + e.getMessage(), config);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 构造 Anthropic content：纯文本返回 String；带图 user 消息返回 content blocks，
     * 图片块在前（image/source.base64）、文本块在后。
     */
    private Object buildAnthropicContent(AIMessage msg) {
        List<String> images = msg.getImageDataUrls();
        if (!"user".equals(msg.getRole()) || images == null || images.isEmpty()) {
            return msg.getContent();
        }
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (String dataUrl : images) {
            String[] parsed = parseDataUrl(dataUrl);
            if (parsed == null) {
                continue;
            }
            Map<String, Object> imageBlock = new LinkedHashMap<>();
            imageBlock.put("type", "image");
            Map<String, String> source = new LinkedHashMap<>();
            source.put("type", "base64");
            source.put("media_type", parsed[0]);
            source.put("data", parsed[1]);
            imageBlock.put("source", source);
            blocks.add(imageBlock);
        }
        if (msg.getContent() != null && !msg.getContent().isEmpty()) {
            Map<String, Object> textBlock = new LinkedHashMap<>();
            textBlock.put("type", "text");
            textBlock.put("text", msg.getContent());
            blocks.add(textBlock);
        }
        // 图片全部解析失败时回退为纯文本，避免发出空 content
        return blocks.isEmpty() ? msg.getContent() : blocks;
    }

    /**
     * 解析 data URL：data:image/jpeg;base64,xxxx → [mime, base64]；非法返回 null。
     */
    private String[] parseDataUrl(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:") || dataUrl.length() < 12) {
            return null;
        }
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            return null;
        }
        String meta = dataUrl.substring(5, comma);
        String mime = "image/jpeg";
        int semi = meta.indexOf(';');
        if (semi > 0) {
            mime = meta.substring(0, semi);
        } else if (!meta.isEmpty()) {
            mime = meta;
        }
        return new String[]{mime, dataUrl.substring(comma + 1)};
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            return errorBody;
        }
    }

    // ==================== P4：工具调用（tool_use / tool_result） ====================

    /**
     * Anthropic Messages API 原生支持 tool_use（2023-06-01 版本）。
     */
    @Override
    public boolean supportsToolCalling() {
        return true;
    }

    /**
     * 工具感知单轮对话。Anthropic 当前适配器无流式实现，工具轮走非流式：
     * 文本一次性经 textSink 推给前端（打字机效果退化为整段呈现），工具调用结构化返回。
     */
    @Override
    @SuppressWarnings("unchecked")
    public ModelTurn chatTurn(List<AIMessage> messages, int maxTokens, double temperature,
                              AiProviderConfig config, List<ToolDefinition> tools,
                              AbortableStreamCallback abort, StreamCallback textSink) throws Exception {
        HttpURLConnection conn = null;
        try {
            String apiUrl = normalizeBaseUrl(config.getBaseUrl()) + "/messages";

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("x-api-key", config.getApiKey());
            headers.put("anthropic-version", ANTHROPIC_VERSION);
            conn = createConnection(apiUrl, config, headers);

            // 中止链路：断开阻塞中的整包响应读取
            final HttpURLConnection streamConn = conn;
            if (abort != null) {
                abort.registerAbortAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            streamConn.disconnect();
                        } catch (Exception e) {
                            log.debug("中止工具轮上游连接失败（可忽略）: {}", e.getMessage());
                        }
                    }
                });
            }

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());
            int resolvedMaxTokens = resolveMaxTokens(maxTokens, config);
            requestBody.put("max_tokens", resolvedMaxTokens);
            double resolvedTemp = resolveTemperature(temperature, config);
            if (resolvedTemp > 0) {
                requestBody.put("temperature", resolvedTemp);
            }

            String systemPrompt = extractSystemPrompt(messages);
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                requestBody.put("system", systemPrompt);
            }

            if (tools != null && !tools.isEmpty()) {
                requestBody.put("tools", buildAnthropicTools(tools));
                // tool_choice 必须是对象形态（{type:"auto"}），与 OpenAI 的字符串 "auto" 不同
                Map<String, String> toolChoice = new LinkedHashMap<>();
                toolChoice.put("type", "auto");
                requestBody.put("tool_choice", toolChoice);
            }
            requestBody.put("messages", buildAnthropicTurnMessages(messages));

            String jsonBody = getObjectMapper().writeValueAsString(requestBody);
            log.info("AI工具轮请求[{} / {}]: model={}, messages={}, tools={}",
                    config.getProviderCode(), FORMAT_ID, config.getModelName(),
                    messages.size(), tools == null ? 0 : tools.size());

            sendRequestBody(conn, jsonBody);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorBody = readErrorBody(conn);
                log.error("AI工具轮请求失败: code={}, error={}", responseCode, truncate(errorBody, 200));
                return ModelTurn.error(buildUserFriendlyError(config.getProviderName(),
                        parseAnthropicErrorMessage(errorBody)));
            }

            String rawBody = readResponseBody(conn);
            if (abort != null && abort.isAborted()) {
                return ModelTurn.builder().finishReason(ModelTurn.FINISH_STOP).build();
            }
            return parseTurnResponse(rawBody, textSink);
        } catch (Exception e) {
            if (abort != null && abort.isAborted()) {
                log.info("AI工具轮被用户中止: provider={}", config.getProviderCode());
                return ModelTurn.builder().finishReason(ModelTurn.FINISH_STOP).build();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 收集 system 消息为顶层 system 字段（多条以换行拼接）。
     */
    private String extractSystemPrompt(List<AIMessage> messages) {
        String systemPrompt = null;
        for (AIMessage msg : messages) {
            if ("system".equals(msg.getRole()) && msg.getContent() != null) {
                systemPrompt = (systemPrompt == null ? "" : systemPrompt + "\n") + msg.getContent();
            }
        }
        return systemPrompt;
    }

    /**
     * 工具轮消息协议转换（与普通对话的差异）：
     * <ul>
     *   <li>assistant 的 toolCalls → assistant content 中的 tool_use 块（文本块在前）；</li>
     *   <li>role=tool 结果 → user content 中的 tool_result 块；连续多条 tool 结果
     *       合并进同一条 user 消息（Anthropic 要求 user/assistant 严格交替）。</li>
     * </ul>
     */
    private List<Map<String, Object>> buildAnthropicTurnMessages(List<AIMessage> messages) {
        List<Map<String, Object>> msgList = new ArrayList<>();
        for (AIMessage msg : messages) {
            String role = msg.getRole();
            if ("system".equals(role)) {
                continue;
            }

            if ("tool".equals(role)) {
                Map<String, Object> toolResultBlock = new LinkedHashMap<>();
                toolResultBlock.put("type", "tool_result");
                toolResultBlock.put("tool_use_id", msg.getToolCallId() == null ? "" : msg.getToolCallId());
                toolResultBlock.put("content", msg.getContent());

                Map<String, Object> lastUser = null;
                if (!msgList.isEmpty()) {
                    Map<String, Object> last = msgList.get(msgList.size() - 1);
                    if ("user".equals(last.get("role")) && last.get("content") instanceof List) {
                        lastUser = last;
                    }
                }
                if (lastUser != null) {
                    ((List<Object>) lastUser.get("content")).add(toolResultBlock);
                } else {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("role", "user");
                    List<Object> blocks = new ArrayList<>();
                    blocks.add(toolResultBlock);
                    m.put("content", blocks);
                    msgList.add(m);
                }
                continue;
            }

            Map<String, Object> m = new LinkedHashMap<>();
            if ("assistant".equals(role) && msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                m.put("role", "assistant");
                List<Map<String, Object>> blocks = new ArrayList<>();
                if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                    Map<String, Object> textBlock = new LinkedHashMap<>();
                    textBlock.put("type", "text");
                    textBlock.put("text", msg.getContent());
                    blocks.add(textBlock);
                }
                for (ToolCall tc : msg.getToolCalls()) {
                    Map<String, Object> useBlock = new LinkedHashMap<>();
                    useBlock.put("type", "tool_use");
                    useBlock.put("id", tc.getId() == null ? "" : tc.getId());
                    useBlock.put("name", tc.getName() == null ? "" : tc.getName());
                    useBlock.put("input", parseToolInput(tc.getArguments()));
                    blocks.add(useBlock);
                }
                m.put("content", blocks);
            } else {
                m.put("role", "user".equals(role) || "assistant".equals(role) ? role : "user");
                m.put("content", buildAnthropicContent(msg));
            }
            msgList.add(m);
        }
        return msgList;
    }

    /**
     * 模型给出的 arguments 是 JSON 字符串，Anthropic tool_use.input 要求是 JSON 对象；坏 JSON 回落空对象。
     */
    private Object parseToolInput(String arguments) {
        if (arguments == null || arguments.trim().isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return getObjectMapper().readValue(arguments, Object.class);
        } catch (Exception e) {
            log.debug("工具入参JSON解析失败，按空对象发送: {}", truncate(arguments, 100));
            return new LinkedHashMap<String, Object>();
        }
    }

    /**
     * Anthropic tools 请求体：{name, description, input_schema}。
     */
    private List<Map<String, Object>> buildAnthropicTools(List<ToolDefinition> tools) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (ToolDefinition tool : tools) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", tool.getName());
            item.put("description", tool.getDescription());
            Map<String, Object> schema = tool.getParameters();
            if (schema == null) {
                schema = new LinkedHashMap<>();
                schema.put("type", "object");
                schema.put("properties", new LinkedHashMap<String, Object>());
            }
            item.put("input_schema", schema);
            payload.add(item);
        }
        return payload;
    }

    /**
     * 解析工具轮响应：text 块累积后一次性推给 textSink，tool_use 块结构化为 ToolCall；
     * stop_reason=tool_use 对应 FINISH_TOOL_CALLS。
     */
    private ModelTurn parseTurnResponse(String rawBody, StreamCallback textSink) throws Exception {
        JsonNode root = getObjectMapper().readTree(rawBody);
        JsonNode contentArray = root.get("content");
        StringBuilder text = new StringBuilder();
        List<ToolCall> calls = new ArrayList<>();
        if (contentArray != null && contentArray.isArray()) {
            for (JsonNode block : contentArray) {
                String type = block.path("type").asText("");
                if ("text".equals(type)) {
                    text.append(block.path("text").asText(""));
                } else if ("tool_use".equals(type)) {
                    JsonNode input = block.get("input");
                    String args = (input == null || input.isNull())
                            ? "{}" : getObjectMapper().writeValueAsString(input);
                    calls.add(ToolCall.builder()
                            .id(block.path("id").asText(""))
                            .name(block.path("name").asText(""))
                            .arguments(args)
                            .build());
                }
            }
        }

        String fullText = text.toString();
        if (!fullText.isEmpty() && textSink != null) {
            textSink.onToken(fullText, false);
        }

        // name 缺失的残片丢弃，避免回填造成上游协议错误
        List<ToolCall> validCalls = new ArrayList<>();
        for (ToolCall call : calls) {
            if (call.getName() != null && !call.getName().isEmpty()) {
                validCalls.add(call);
            }
        }

        boolean toolUse = "tool_use".equals(root.path("stop_reason").asText(""));
        return ModelTurn.builder()
                .content(fullText)
                .toolCalls(validCalls.isEmpty() ? null : validCalls)
                .finishReason(toolUse ? ModelTurn.FINISH_TOOL_CALLS : ModelTurn.FINISH_STOP)
                .build();
    }
}
