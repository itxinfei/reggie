package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * <p>
 * AI模型适配器抽象基类，封装通用的HTTP连接管理、错误处理、参数解析等逻辑。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-10
 */
@Slf4j
public abstract class BaseModelAdapter implements AiModelAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    // ==================== 子类必须实现 ====================

    /**
     * 由子类实现具体的请求构建、发送和响应解析逻辑
     * @throws Exception 允许子类抛出任意异常，由 {@link #chat} 模板方法统一处理
     */
    protected abstract AIChatResponse doChat(java.util.List<com.reggie.module.ai.model.AIMessage> messages,
                                              int maxTokens, double temperature,
                                              AiProviderConfig config) throws Exception;

    // ==================== 模板方法 ====================

    @Override
    public AIChatResponse chat(java.util.List<com.reggie.module.ai.model.AIMessage> messages,
                                int maxTokens, double temperature, AiProviderConfig config) {
        try {
            return doChat(messages, maxTokens, temperature, config);
        } catch (Exception e) {
            // 修改点：由于 doChat() 的实现类内部可能已捕获异常，
            // 这里使用 instanceof 分发以支持子类向外抛出网络异常的场景
            if (e instanceof java.net.SocketTimeoutException) {
                log.error("AI请求[{}]超时", config.getProviderCode(), e);
                return errorResponse("AI服务响应超时（" + config.getProviderName() + "），请稍后重试", config);
            }
            if (e instanceof java.net.ConnectException) {
                log.error("AI请求[{}]连接失败", config.getProviderCode(), e);
                return errorResponse("无法连接到AI服务（" + config.getProviderName() + "），请检查网络和API地址", config);
            }
            log.error("AI请求[{}]异常", config.getProviderCode(), e);
            return errorResponse("AI服务连接失败（" + config.getProviderName() + "）：" + e.getMessage(), config);
        }
    }

    // ==================== HTTP 连接工具 ====================

    /**
     * 创建 HTTP 连接
     *
     * @param urlStr  完整的 API 地址
     * @param config  供应商配置（用于获取 timeout）
     * @param headers 自定义请求头
     */
    protected HttpURLConnection createConnection(String urlStr, AiProviderConfig config,
                                                  Map<String, String> headers) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Connection", "close");
        conn.setDoOutput(true);

        // 自定义请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        int timeout = (config.getTimeout() != null ? config.getTimeout() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);

        return conn;
    }

    /**
     * 发送 JSON 请求体
     */
    protected void sendRequestBody(HttpURLConnection conn, String jsonBody) throws Exception {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    /**
     * 读取响应体为字符串
     */
    protected String readResponseBody(HttpURLConnection conn) throws Exception {
        try (InputStream is = conn.getInputStream()) {
            return readStream(is);
        }
    }

    /**
     * 读取错误响应体为字符串
     */
    protected String readErrorBody(HttpURLConnection conn) {
        try (InputStream es = conn.getErrorStream()) {
            if (es == null) {
                return "HTTP " + getResponseCode(conn);
            }
            return readStream(es);
        } catch (Exception e) {
            return "HTTP " + getResponseCode(conn);
        }
    }

    /**
     * 将 InputStream 读取为字符串，按完整内容读取，最多读取 1MB 防止异常超大响应。
     */
    private String readStream(InputStream stream) throws Exception {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[4096];
        int len;
        int total = 0;
        int maxBytes = 1024 * 1024;
        while ((len = stream.read(buf)) != -1) {
            total += len;
            if (total > maxBytes) {
                sb.append(new String(buf, 0, maxBytes - (total - len), java.nio.charset.StandardCharsets.UTF_8));
                break;
            }
            sb.append(new String(buf, 0, len, java.nio.charset.StandardCharsets.UTF_8));
        }
        return sb.toString().trim();
    }

    /**
     * 安全获取 HTTP 状态码
     */
    protected int getResponseCode(HttpURLConnection conn) {
        try {
            return conn.getResponseCode();
        } catch (Exception e) {
            return -1;
        }
    }

    // ==================== 参数解析 ====================

    /**
     * 解析 maxTokens（优先使用传入值，否则用配置值，兜底 2048）
     */
    protected int resolveMaxTokens(int maxTokens, AiProviderConfig config) {
        return maxTokens > 0 ? maxTokens : (config.getMaxTokens() != null ? config.getMaxTokens() : 2048);
    }

    /**
     * 解析 temperature（优先使用传入值，否则用配置值，兜底 0.7）
     */
    protected double resolveTemperature(double temperature, AiProviderConfig config) {
        return temperature >= 0 ? temperature : (config.getTemperature() != null ? config.getTemperature() : 0.7);
    }

    /**
     * 规范化 baseUrl，去除尾部斜杠
     */
    protected String normalizeBaseUrl(String baseUrl) {
        if (baseUrl != null && baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    // ==================== 响应构建 ====================

    /**
     * 构建成功响应
     */
    protected AIChatResponse successResponse(String content, String model, int tokensUsed) {
        return AIChatResponse.builder()
                .content(content)
                .model(model)
                .tokensUsed(tokensUsed)
                .build();
    }

    /**
     * 构建错误响应
     */
    protected AIChatResponse errorResponse(String message, AiProviderConfig config) {
        return AIChatResponse.builder()
                .content(message)
                .model(config.getModelName())
                .build();
    }

    // ==================== 错误诊断 ====================

    /**
     * 根据 API 返回的错误类型，构建用户友好的错误提示
     * <p>特别针对 Go 反序列化错误（常见于 New API / One API 网关代理）</p>
     */
    protected String buildUserFriendlyError(String providerName, String rawError) {
        if (rawError == null) {
            return "AI服务暂时不可用（" + providerName + "）";
        }
        if (rawError.contains("unmarshal") || rawError.contains("Unmarshal")) {
            if (rawError.contains(".messages") && rawError.contains("string")) {
                return "AI网关响应解析失败（" + providerName + "）\n\n"
                        + "错误原因：上游模型返回的响应中「messages」字段是字符串，"
                        + "但 api.iamhc.cn 网关期望的是对象格式（Go 泛型反序列化失败）。\n\n"
                        + "这是 api.iamhc.cn 渠道/上游模型的配置问题，不是本系统的 Bug。\n\n"
                        + "排查步骤（登录 api.iamhc.cn 管理后台）：\n"
                        + "1. 进入「渠道」→ 找到当前使用的渠道 → 点击编辑\n"
                        + "2. 重点检查「类型」下拉框是否正确：\n"
                        + "   - 如果上游是 OpenAI / DeepSeek / 通义千问等对话模型 → 类型选择对应选项\n"
                        + "   - 如果上游是文本补全模型（非 chat 模型）→ 类型不要选 OpenAI\n"
                        + "3. 确认「模型」名称与上游实际模型名一致（含版本后缀）\n"
                        + "4. 在渠道页面点击「测试」按钮验证连通性\n"
                        + "5. 如果测试失败，尝试更换渠道类型或上游模型\n\n"
                        + "原始错误：" + rawError;
            }
            return "AI网关响应解析失败（" + providerName + "）\n\n"
                    + "原因：上游 AI 模型返回的响应格式与网关（api.iamhc.cn）期望不匹配，"
                    + "JSON 字段类型不一致（Go 反序列化错误）。\n\n"
                    + "这不是本系统的 Bug，请检查以下配置：\n"
                    + "1. 登录 api.iamhc.cn 管理后台\n"
                    + "2. 检查对应渠道（Channel）的「模型」和「类型」配置是否正确\n"
                    + "3. 确认上游模型 API 是否正常运行\n"
                    + "4. 尝试切换渠道类型或更换模型\n\n"
                    + "原始错误：" + rawError;
        }
        return "AI服务暂时不可用（" + providerName + "）：" + rawError;
    }

    /**
     * 截断字符串用于日志输出
     */
    protected String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }

    /**
     * 获取共享的 ObjectMapper 实例
     */
    protected ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
