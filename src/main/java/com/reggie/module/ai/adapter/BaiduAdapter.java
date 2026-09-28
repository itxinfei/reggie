package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.failover.AiFailureType;
import com.reggie.module.ai.failover.AiProviderException;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.util.AiSecretMaskUtils;
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
        return "百度文心一言（ERNIE Bot 原生 API）";
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
            // 1) 构建 URL（百度旧版 ERNIE 原生 API 要求 access_token 放查询参数，无法改请求头）
            String baseUrl = normalizeBaseUrl(config.getBaseUrl());
            // 安全约束：apiUrl 含明文密钥，严禁写入任何日志/用户可见消息（访问日志由运维侧脱敏）
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
                    // 百度压平协议不支持图片入参：以 [图片] 占位提示模型用户附带了图片（vision 不应在此格式勾选）
                    List<String> images = msg.getImageDataUrls();
                    promptBuilder.append("用户：");
                    if (images != null && !images.isEmpty()) {
                        for (int i = 0; i < images.size(); i++) {
                            promptBuilder.append("[图片]");
                        }
                        promptBuilder.append(" ");
                    }
                    promptBuilder.append(msg.getContent()).append("\n");
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
                // 修改点：errorBody 截断 200 字，防止 token 回显或超长响应体落盘
                log.error("AI请求[{} / {}]失败: code={}, error={}",
                        config.getProviderCode(), FORMAT_ID, responseCode,
                        truncate(errorBody, 200));
                // 修改点(2026-09-26)：抛异常而非吞成错误响应，供故障转移分类/切换
                throw AiProviderException.upstream(config, responseCode, errorBody);
            }
        } catch (AiProviderException e) {
            // 已分类异常直接透传
            throw e;
        } catch (Exception e) {
            // 修改点(2026-09-15)：外网不可达属运行环境问题，降为 WARN 且不打全量堆栈，避免刷屏
            // 异常消息可能携带含 access_token 的完整 URL，日志/回显前脱敏
            String safeMsg = AiSecretMaskUtils.maskUrl(e.getMessage());
            if (AiNetworkFailureUtils.isNetworkFailure(e)) {
                log.warn("AI请求[{} / {}]外部服务不可达（网络环境问题，非应用缺陷）：{}",
                        config.getProviderCode(), FORMAT_ID, safeMsg);
                throw new AiProviderException(AiFailureType.NETWORK, 0,
                        "百度网络异常: " + safeMsg, e);
            }
            log.error("AI请求[{} / {}]未预期异常", config.getProviderCode(), FORMAT_ID, e);
            throw new AiProviderException(AiFailureType.SERVER_ERROR, 0,
                    "百度调用异常: " + safeMsg, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析百度 ERNIE 成功响应。
     * <p>注意：百度常把限流/鉴权/计费错误以 HTTP 200 + error_code/error_msg 返回，
     * 需按错误码分类抛出，供故障转移切换。</p>
     * <pre>{ result: "..." } 或 { error_code, error_msg }</pre>
     */
    private AIChatResponse parseResponse(HttpURLConnection conn, AiProviderConfig config) throws Exception {
        String rawBody = readResponseBody(conn);
        JsonNode root = getObjectMapper().readTree(rawBody);

        String content = root.path("result").asText("");
        if (!content.isEmpty()) {
            return successResponse(content, config.getModelName(), 0);
        }

        // 200 body 内错误：按错误码 + 错误文案分类（拿不准的归 SERVER_ERROR，绝不误判 BAD_REQUEST）
        String errorMsg = root.path("error_msg").asText("");
        if (!errorMsg.isEmpty()) {
            int errorCode = root.path("error_code").asInt(-1);
            throw new AiProviderException(classifyBaidu(errorCode, errorMsg), 200,
                    "百度返回错误（code=" + errorCode + "）: " + errorMsg);
        }

        // 修改点(2026-09-26)：抛空响应异常而非吞成错误响应
        throw AiProviderException.empty(config);
    }

    /**
     * 按百度错误码 / 错误文案分类。
     * <p>常见：17=日总量限流, 18=QPS限流, 19/4=总配额限流；
     * 110/111=access_token 失效；336xxx 多为服务/计费类。</p>
     */
    private AiFailureType classifyBaidu(int errorCode, String errorMsg) {
        if (errorCode == 110 || errorCode == 111) {
            return AiFailureType.AUTH_INVALID;
        }
        if (errorCode == 17 || errorCode == 18) {
            return AiFailureType.RATE_LIMIT;
        }
        String msg = errorMsg == null ? "" : errorMsg.toLowerCase();
        if (msg.contains("额度") || msg.contains("余额") || msg.contains("欠费")
                || msg.contains("quota") || msg.contains("balance") || errorCode == 19 || errorCode == 4) {
            return AiFailureType.QUOTA;
        }
        return AiFailureType.SERVER_ERROR;
    }
}
