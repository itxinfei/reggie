package com.reggie.module.ai.rag.service.impl;

import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.provider.AiProviderManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link EmbeddingServiceImpl} 单元测试：{@link HttpServer} 假上游验证按 index 归位、
 * 条数/缺项/越界/非 200/坏 JSON 的异常路径，以及能力探测。
 *
 * @author reggie
 * @since 2026-09-21
 */
class EmbeddingServiceImplTest {

    private HttpServer server;
    private String baseUrl;
    private EmbeddingServiceImpl service;
    private AiProviderManager manager;
    private AiProviderConfig config;

    private HttpHandler currentHandler;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                currentHandler.handle(exchange);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";

        config = new AiProviderConfig();
        config.setBaseUrl(baseUrl);
        config.setApiKey("test-key");
        config.setModelName("embedding-model");
        config.setTimeout(10);

        manager = mock(AiProviderManager.class);
        when(manager.getActiveConfig()).thenReturn(config);

        service = new EmbeddingServiceImpl();
        ReflectionTestUtils.setField(service, "aiProviderManager", manager);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void embedPlacesVectorsByIndex() {
        // 上游返回顺序与请求相反，必须按 index 归位
        currentHandler = jsonHandler(200,
                "{\"data\":["
                        + "{\"index\":1,\"embedding\":[0.3,0.4]},"
                        + "{\"index\":0,\"embedding\":[0.1,0.2]}"
                        + "]}");
        List<double[]> result = service.embed(Arrays.asList("第一条", "第二条"));
        assertEquals(2, result.size());
        assertEquals(0.1, result.get(0)[0], 1e-9);
        assertEquals(0.2, result.get(0)[1], 1e-9);
        assertEquals(0.4, result.get(1)[1], 1e-9);
    }

    @Test
    void embedEmptyInputReturnsEmptyListWithoutHttp() {
        final int[] hits = new int[] {0};
        currentHandler = new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                hits[0]++;
            }
        };
        List<double[]> result = service.embed(new java.util.ArrayList<String>());
        assertTrue(result.isEmpty());
        assertEquals(0, hits[0]);
    }

    @Test
    void embedWithoutActiveConfigThrows() {
        when(manager.getActiveConfig()).thenReturn(null);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.embed(Arrays.asList("a")));
        assertTrue(ex.getMessage().contains("无可用的AI供应商配置"));
    }

    @Test
    void embedCountMismatchThrows() {
        currentHandler = jsonHandler(200,
                "{\"data\":[{\"index\":0,\"embedding\":[0.1]}]}");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.embed(Arrays.asList("a", "b")));
        assertTrue(ex.getMessage().contains("条数不符"));
    }

    @Test
    void embedMissingSlotThrows() {
        // 两个条目都声明 index=0，index=1 位置空缺
        currentHandler = jsonHandler(200,
                "{\"data\":["
                        + "{\"index\":0,\"embedding\":[0.1]},"
                        + "{\"index\":0,\"embedding\":[0.2]}"
                        + "]}");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.embed(Arrays.asList("a", "b")));
        assertTrue(ex.getMessage().contains("缺失项"));
    }

    @Test
    void embedIndexOutOfRangeThrows() {
        currentHandler = jsonHandler(200,
                "{\"data\":[{\"index\":5,\"embedding\":[0.1]}]}");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.embed(Arrays.asList("a")));
        assertTrue(ex.getMessage().contains("响应项非法"));
    }

    @Test
    void embedNonArrayEmbeddingThrows() {
        currentHandler = jsonHandler(200,
                "{\"data\":[{\"index\":0,\"embedding\":\"not-array\"}]}");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.embed(Arrays.asList("a")));
        assertTrue(ex.getMessage().contains("响应项非法"));
    }

    @Test
    void embedNon200ThrowsWithUpstreamBody() {
        currentHandler = jsonHandler(500, "server exploded");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.embed(Arrays.asList("a")));
        assertTrue(ex.getMessage().contains("500"));
        assertTrue(ex.getMessage().contains("server exploded"));
    }

    @Test
    void embedMalformedJsonThrows() {
        currentHandler = jsonHandler(200, "this is not json");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.embed(Arrays.asList("a")));
        assertTrue(ex.getMessage().contains("解析失败"));
    }

    @Test
    void isEmbeddingAvailableTrueWhenCapableAndConfigPresent() {
        Map<String, Boolean> caps = new HashMap<String, Boolean>();
        caps.put("embedding", Boolean.TRUE);
        when(manager.getCapabilities()).thenReturn(caps);
        assertTrue(service.isEmbeddingAvailable());
    }

    @Test
    void isEmbeddingAvailableFalseWhenCapabilityMissing() {
        Map<String, Boolean> caps = new HashMap<String, Boolean>();
        caps.put("embedding", Boolean.FALSE);
        when(manager.getCapabilities()).thenReturn(caps);
        assertFalse(service.isEmbeddingAvailable());
    }

    @Test
    void isEmbeddingAvailableFalseWhenManagerThrows() {
        when(manager.getCapabilities()).thenThrow(new RuntimeException("redis down"));
        assertFalse(service.isEmbeddingAvailable());
    }

    @Test
    void getEmbeddingModelReturnsActiveModelOrNull() {
        assertEquals("embedding-model", service.getEmbeddingModel());
        when(manager.getActiveConfig()).thenReturn(null);
        assertNull(service.getEmbeddingModel());
    }

    // ==================== 辅助 ====================

    private HttpHandler jsonHandler(final int code, final String body) {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(code, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            }
        };
    }
}
