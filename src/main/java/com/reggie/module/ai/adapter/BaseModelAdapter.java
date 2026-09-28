package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.ai.failover.AiProviderException;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
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

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    static {
        // 启用 HTTP keep-alive 连接复用（JDK 内置 KeepAliveCache），
        // 避免高并发下每次 AI 调用都新建 TCP/TLS 握手
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "10");
    }

    // ==================== 子类必须实现 ====================

    /**
     * 由子类实现具体的请求构建、发送和响应解析逻辑
     * @throws Exception 允许子类抛出任意异常，由 {@link #chat} 模板方法统一处理
     */
    protected abstract AIChatResponse doChat(java.util.List<com.reggie.module.ai.model.AIMessage> messages,
                                              int maxTokens, double temperature,
                                              AiProviderConfig config) throws Exception;

    // ==================== 模板方法 ====================

    /**
     * 处理 chat。
     * <p>修改点(2026-09-26)：失败不再吞成错误 AIChatResponse，而是分类抛出
     * {@link AiProviderException}，由上层故障转移执行器决定是否切换供应商。</p>
     * @param messages 参数 messages
     * @param maxTokens 参数 maxTokens
     * @param temperature 参数 temperature
     * @param config 参数 config
     * @return 返回结果
     */
    @Override
    public AIChatResponse chat(java.util.List<com.reggie.module.ai.model.AIMessage> messages,
                                int maxTokens, double temperature, AiProviderConfig config) {
        AIChatResponse resp;
        try {
            resp = doChat(messages, maxTokens, temperature, config);
        } catch (AiProviderException e) {
            // 子类已分类的异常直接透传
            throw e;
        } catch (Exception e) {
            // 沿用日志分级口径：网络/超时类 WARN 无堆栈，其余 ERROR 保留堆栈
            if (e instanceof java.net.SocketTimeoutException || AiNetworkFailureUtils.isNetworkFailure(e)) {
                log.warn("AI请求[{}]网络异常：{}", config.getProviderCode(), e.getMessage());
            } else {
                log.error("AI请求[{}]异常", config.getProviderCode(), e);
            }
            throw AiProviderException.local(config, e);
        }
        if (resp == null || resp.getContent() == null || resp.getContent().isEmpty()) {
            log.warn("AI请求[{}]返回空内容", config.getProviderCode());
            throw AiProviderException.empty(config);
        }
        return resp;
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
        // 不设置 Connection: close，交由 JDK KeepAliveCache 复用连接
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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


