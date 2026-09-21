package com.reggie.module.ai.rag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.provider.AiProviderManager;
import com.reggie.module.ai.rag.service.EmbeddingService;
import com.reggie.module.ai.util.AiUrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
 * Embedding 服务实现（P5 RAG）：走激活供应商 OpenAI 兼容 /embeddings。
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    /** embedding HTTP 超时（秒），短请求不沿用 chat 的长超时 */
    private static final int TIMEOUT_SECONDS = 20;

    /** 单批最大条数（调用方按此分批） */
    public static final int BATCH_SIZE = 16;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private AiProviderManager aiProviderManager;

    @Override
    public boolean isEmbeddingAvailable() {
        try {
            return Boolean.TRUE.equals(aiProviderManager.getCapabilities().get("embedding"))
                    && getActiveConfigOrNull() != null;
        } catch (Exception e) {
            log.warn("读取 embedding 能力失败，按不可用处理: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getEmbeddingModel() {
        AiProviderConfig config = getActiveConfigOrNull();
        return config == null ? null : config.getModelName();
    }

    @Override
    public List<double[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }
        AiProviderConfig config = getActiveConfigOrNull();
        if (config == null) {
            throw new IllegalStateException("无可用的AI供应商配置");
        }

        HttpURLConnection conn = null;
        try {
            String apiUrl = AiUrlUtils.resolveEndpoint(config.getBaseUrl(), "/embeddings");

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModelName());
            requestBody.put("input", texts);
            String jsonBody = MAPPER.writeValueAsString(requestBody);

            conn = createConnection(apiUrl, config.getApiKey());
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            String responseText;
            if (responseCode == 200) {
                try (InputStream is = conn.getInputStream()) {
                    responseText = readStream(is);
                }
            } else {
                String errorBody;
                try (InputStream es = conn.getErrorStream()) {
                    errorBody = es == null ? ("HTTP " + responseCode) : readStream(es);
                }
                if (errorBody.length() > 200) {
                    errorBody = errorBody.substring(0, 200);
                }
                log.warn("Embedding请求失败: code={}, error={}", responseCode, errorBody);
                throw new IllegalStateException("Embedding服务返回 " + responseCode + ": " + errorBody);
            }

            return parseEmbeddings(responseText, texts.size());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Embedding调用异常: {}", e.getMessage());
            throw new IllegalStateException("Embedding服务调用失败: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析 {"data":[{"index":0,"embedding":[...]}, ...]}，按 index 归位。
     */
    private List<double[]> parseEmbeddings(String responseText, int expected) {
        try {
            JsonNode root = MAPPER.readTree(responseText);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.size() != expected) {
                throw new IllegalStateException("Embedding响应条数不符: expected=" + expected
                        + ", actual=" + (data.isArray() ? data.size() : 0));
            }
            List<double[]> result = new ArrayList<>(expected);
            for (int i = 0; i < expected; i++) {
                result.add(null);
            }
            for (JsonNode item : data) {
                int index = item.path("index").asInt(0);
                JsonNode embNode = item.path("embedding");
                if (index < 0 || index >= expected || !embNode.isArray()) {
                    throw new IllegalStateException("Embedding响应项非法: index=" + index);
                }
                double[] vector = new double[embNode.size()];
                for (int j = 0; j < embNode.size(); j++) {
                    vector[j] = embNode.get(j).asDouble();
                }
                result.set(index, vector);
            }
            for (double[] vector : result) {
                if (vector == null) {
                    throw new IllegalStateException("Embedding响应存在缺失项");
                }
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Embedding响应解析失败: " + e.getMessage(), e);
        }
    }

    private AiProviderConfig getActiveConfigOrNull() {
        try {
            return aiProviderManager.getActiveConfig();
        } catch (Exception e) {
            log.warn("读取激活供应商失败: {}", e.getMessage());
            return null;
        }
    }

    private HttpURLConnection createConnection(String urlStr, String apiKey) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT_SECONDS * 1000);
        conn.setReadTimeout(TIMEOUT_SECONDS * 1000);
        return conn;
    }

    private String readStream(InputStream stream) throws Exception {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[4096];
        int len;
        while ((len = stream.read(buf)) != -1) {
            sb.append(new String(buf, 0, len, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
