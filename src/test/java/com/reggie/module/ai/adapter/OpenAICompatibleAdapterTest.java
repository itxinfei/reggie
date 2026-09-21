package com.reggie.module.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.model.ModelTurn;
import com.reggie.module.ai.model.ToolCall;
import com.reggie.module.ai.tool.ToolDefinition;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OpenAICompatibleAdapter} 单元测试：用 JDK 内置 {@link HttpServer} 模拟
 * OpenAI 兼容上游，覆盖 SSE 分片、vision 请求体、tool_calls 按 index 累积、
 * 整包 JSON 兼容、非 200 错误与用户中止链路。
 *
 * @author reggie
 * @since 2026-09-21
 */
class OpenAICompatibleAdapterTest {

    private final ObjectMapper mapper = ObjectMapperHolder.getDefault();

    private HttpServer server;
    private String baseUrl;
    private OpenAICompatibleAdapter adapter;
    private AiProviderConfig config;

    /** 当前测试的请求处理器（模拟上游行为） */
    private HttpHandler currentHandler;

    /** 收集所有 token 与 isLast 回调 */
    private static class CollectingCallback implements AiModelAdapter.StreamCallback {
        final List<String> tokens = new ArrayList<String>();
        int lastCount = 0;

        @Override
        public void onToken(String token, boolean isLast) {
            tokens.add(token);
            if (isLast) {
                lastCount++;
            }
        }

        String joined() {
            StringBuilder sb = new StringBuilder();
            for (String t : tokens) {
                sb.append(t);
            }
            return sb.toString();
        }
    }

    /** 可在首个 token 后转入中止态的回调 */
    private static class AbortingCallback extends CollectingCallback implements AbortableStreamCallback {
        volatile boolean aborted = false;
        boolean abortAfterFirstToken = true;
        Runnable abortAction;

        @Override
        public void onToken(String token, boolean isLast) {
            super.onToken(token, isLast);
            if (abortAfterFirstToken && !aborted) {
                aborted = true;
            }
        }

        @Override
        public boolean isAborted() {
            return aborted;
        }

        @Override
        public void registerAbortAction(Runnable action) {
            this.abortAction = action;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                currentHandler.handle(exchange);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";

        adapter = new OpenAICompatibleAdapter();
        config = new AiProviderConfig();
        config.setBaseUrl(baseUrl);
        config.setApiKey("test-key");
        config.setModelName("test-model");
        config.setProviderCode("test");
        config.setProviderName("测试供应商");
        config.setTimeout(10);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    // ==================== chat / chatStream ====================

    @Test
    void chatStreamAggregatesSseDeltas() throws Exception {
        currentHandler = sseHandler(Arrays.asList(
                sseDelta("你好"),
                sseDelta("，世界"),
                "[DONE]"));
        CollectingCallback callback = new CollectingCallback();
        String result = adapter.chatStream(singleUser("说点什么"), 0, -1, config, callback);

        assertEquals("你好，世界", result);
        assertEquals("你好，世界", callback.joined());
        assertEquals(1, callback.lastCount);
    }

    @Test
    void chatStreamSkipsMalformedSseFrame() throws Exception {
        currentHandler = sseHandler(Arrays.asList(
                "data: not-a-json",
                sseDelta("正常"),
                "[DONE]"));
        CollectingCallback callback = new CollectingCallback();
        String result = adapter.chatStream(singleUser("hi"), 0, -1, config, callback);
        assertEquals("正常", result);
    }

    @Test
    void chatStreamSendsVisionImageUrlBody() throws Exception {
        final String[] seenBody = new String[] {null};
        currentHandler = new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                seenBody[0] = readRequest(exchange);
                writeSse(exchange, Arrays.asList(sseDelta("看到了"), "[DONE]"));
            }
        };
        AIMessage msg = AIMessage.builder()
                .role("user")
                .content("这是什么")
                .imageDataUrls(Arrays.asList("data:image/jpeg;base64,/9j/AAAA"))
                .build();
        CollectingCallback callback = new CollectingCallback();
        String result = adapter.chatStream(Arrays.asList(msg), 0, -1, config, callback);

        assertEquals("看到了", result);
        JsonNode root = mapper.readTree(seenBody[0]);
        JsonNode content = root.path("messages").get(0).path("content");
        assertTrue(content.isArray(), "vision 请求体 content 必须是多模态数组");
        assertEquals("text", content.get(0).path("type").asText());
        assertEquals("这是什么", content.get(0).path("text").asText());
        assertEquals("image_url", content.get(1).path("type").asText());
        assertEquals("data:image/jpeg;base64,/9j/AAAA",
                content.get(1).path("image_url").path("url").asText());
        assertTrue(root.path("stream").asBoolean());
    }

    @Test
    void chatStreamNon200PushesErrorToken() throws Exception {
        currentHandler = jsonErrorHandler(401, "{\"error\":{\"message\":\"bad key\"}}");
        CollectingCallback callback = new CollectingCallback();
        String result = adapter.chatStream(singleUser("hi"), 0, -1, config, callback);
        assertNull(result);
        assertTrue(callback.tokens.get(callback.tokens.size() - 1).contains("401"));
    }

    @Test
    void chatStreamAcceptsWholeJsonResponse() throws Exception {
        currentHandler = new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"整包回答\"}}]}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            }
        };
        CollectingCallback callback = new CollectingCallback();
        String result = adapter.chatStream(singleUser("hi"), 0, -1, config, callback);
        assertEquals("整包回答", result);
        assertTrue(callback.tokens.contains("整包回答"));
    }

    @Test
    void chatStreamAbortedAfterFirstFrameKeepsPartial() throws Exception {
        currentHandler = sseHandler(Arrays.asList(
                sseDelta("第一帧"),
                sseDelta("第二帧"),
                "[DONE]"));
        AbortingCallback callback = new AbortingCallback();
        String result = adapter.chatStream(singleUser("hi"), 0, -1, config, callback);
        // 读取循环在第二帧前检测到中止并退出，已收片段正常返回供 stopped 落库
        assertEquals("第一帧", result);
    }

    @Test
    void chatReturnsParsedContent() {
        currentHandler = new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"非流式答复\"}}],"
                        + "\"usage\":{\"total_tokens\":42}}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            }
        };
        assertEquals("非流式答复", adapter.chat(singleUser("hi"), 0, -1, config).getContent());
    }

    // ==================== chatTurn（工具调用） ====================

    @Test
    void chatTurnAccumulatesToolCallFragmentsByIndex() throws Exception {
        currentHandler = sseHandler(Arrays.asList(
                "{\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":["
                        + "{\"index\":0,\"id\":\"call_1\",\"type\":\"function\","
                        + "\"function\":{\"name\":\"get_daily_report\",\"arguments\":\"\"}}]}}]}",
                "{\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":["
                        + "{\"index\":0,\"function\":{\"arguments\":\"{\\\"startDate\\\":\"}}]}}]}",
                "{\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":["
                        + "{\"index\":0,\"function\":{\"arguments\":\"\\\"2026-09-01\\\"}\"}}]}}]}",
                "{\"choices\":[{\"index\":0,\"finish_reason\":\"tool_calls\",\"delta\":{}}]}"));
        ModelTurn turn = adapter.chatTurn(singleUser("昨天营业额"), 0, -1, config,
                Arrays.asList(sampleTool()), new NeverAbort(), null);

        assertEquals(ModelTurn.FINISH_TOOL_CALLS, turn.getFinishReason());
        assertEquals(1, turn.getToolCalls().size());
        ToolCall call = turn.getToolCalls().get(0);
        assertEquals("call_1", call.getId());
        assertEquals("get_daily_report", call.getName());
        assertEquals("{\"startDate\":\"2026-09-01\"}", call.getArguments());
        // chatTurn 显式以 StringBuilder 落 content，无文本时为空串
        assertEquals("", turn.getContent());
    }

    @Test
    void chatTurnSortsMultipleIndexesAndFillsDefaults() throws Exception {
        // index 乱序到达，arguments 跨片；index=1 不带 id 应补 call_1
        currentHandler = sseHandler(Arrays.asList(
                "{\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":["
                        + "{\"index\":1,\"type\":\"function\",\"function\":{\"name\":\"tool_b\",\"arguments\":\"{}\"}}]}}]}",
                "{\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":["
                        + "{\"index\":0,\"id\":\"c0\",\"type\":\"function\","
                        + "\"function\":{\"name\":\"tool_a\",\"arguments\":\"\"}}]}}]}",
                "{\"choices\":[{\"index\":0,\"finish_reason\":\"tool_calls\",\"delta\":{}}]}"));
        ModelTurn turn = adapter.chatTurn(singleUser("多工具"), 0, -1, config,
                Arrays.asList(sampleTool()), new NeverAbort(), null);

        assertEquals(2, turn.getToolCalls().size());
        assertEquals("tool_a", turn.getToolCalls().get(0).getName());
        assertEquals("c0", turn.getToolCalls().get(0).getId());
        assertEquals("{}", turn.getToolCalls().get(0).getArguments());
        assertEquals("tool_b", turn.getToolCalls().get(1).getName());
        assertEquals("call_1", turn.getToolCalls().get(1).getId());
    }

    @Test
    void chatTurnTextDeltasFinishStop() throws Exception {
        CollectingCallback textSink = new CollectingCallback();
        currentHandler = sseHandler(Arrays.asList(
                sseDelta("答案"),
                sseDelta("是42"),
                "{\"choices\":[{\"index\":0,\"finish_reason\":\"stop\",\"delta\":{}}]}"));
        ModelTurn turn = adapter.chatTurn(singleUser("终极问题"), 0, -1, config,
                new ArrayList<ToolDefinition>(), new NeverAbort(), textSink);

        assertEquals(ModelTurn.FINISH_STOP, turn.getFinishReason());
        assertEquals("答案是42", turn.getContent());
        assertNull(turn.getToolCalls());
        assertEquals("答案是42", textSink.joined());
    }

    @Test
    void chatTurnWholeJsonToolCalls() throws Exception {
        currentHandler = new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String body = "{\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{"
                        + "\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{"
                        + "\"id\":\"call_9\",\"type\":\"function\","
                        + "\"function\":{\"name\":\"get_daily_report\",\"arguments\":\"{\\\"k\\\":1}\"}"
                        + "}]}}]}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            }
        };
        ModelTurn turn = adapter.chatTurn(singleUser("查数"), 0, -1, config,
                Arrays.asList(sampleTool()), new NeverAbort(), null);
        assertEquals(ModelTurn.FINISH_TOOL_CALLS, turn.getFinishReason());
        assertEquals(1, turn.getToolCalls().size());
        assertEquals("call_9", turn.getToolCalls().get(0).getId());
        assertEquals("{\"k\":1}", turn.getToolCalls().get(0).getArguments());
    }

    @Test
    void chatTurnNon200ReturnsErrorTurn() throws Exception {
        currentHandler = jsonErrorHandler(500, "upstream exploded");
        ModelTurn turn = adapter.chatTurn(singleUser("hi"), 0, -1, config,
                Arrays.asList(sampleTool()), new NeverAbort(), null);
        assertTrue(turn.isError());
        assertTrue(turn.getErrorMessage().contains("upstream exploded"));
    }

    @Test
    void chatTurnAbortedReturnsEmptyStopTurn() throws Exception {
        // 真实中止由服务层外部置位：模拟为第 2 次轮询起返回 true，
        // 则首帧 append「片段」后、第二帧前 break
        final int[] checks = new int[] {0};
        AbortableStreamCallback abort = new AbortableStreamCallback() {
            @Override
            public void onToken(String token, boolean isLast) {
                // 工具轮 token 走 textSink，此回调不收文本
            }

            @Override
            public boolean isAborted() {
                checks[0]++;
                return checks[0] >= 2;
            }

            @Override
            public void registerAbortAction(Runnable action) {
                // 测试中不真断 socket，靠循环轮询退出
            }
        };
        currentHandler = sseHandler(Arrays.asList(
                sseDelta("片段"),
                sseDelta("更多"),
                "[DONE]"));
        ModelTurn turn = adapter.chatTurn(singleUser("hi"), 0, -1, config,
                Arrays.asList(sampleTool()), abort, null);
        assertEquals(ModelTurn.FINISH_STOP, turn.getFinishReason());
        assertNull(turn.getContent());
        assertNull(turn.getToolCalls());
        assertFalse(turn.isError());
    }

    // ==================== 辅助方法 ====================

    private List<AIMessage> singleUser(String content) {
        return Arrays.asList(AIMessage.builder().role("user").content(content).build());
    }

    private ToolDefinition sampleTool() {
        return new ToolDefinition("get_daily_report", "查日报",
                new LinkedHashMap<String, Object>());
    }

    private String sseDelta(String text) {
        return "{\"choices\":[{\"index\":0,\"delta\":{\"content\":\"" + text + "\"}}]}";
    }

    private HttpHandler sseHandler(final List<String> frames) {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                writeSse(exchange, frames);
            }
        };
    }

    private HttpHandler jsonErrorHandler(final int code, final String body) {
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

    private static void writeSse(HttpExchange exchange, List<String> frames) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String frame : frames) {
            // 统一加 SSE 字段前缀；[DONE] 与坏 JSON 帧同样适用
            sb.append("data: ").append(frame).append("\n\n");
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private static String readRequest(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) != -1) {
            bos.write(buf, 0, len);
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    /** 永不中止的回调 */
    private static class NeverAbort implements AbortableStreamCallback {
        @Override
        public void onToken(String token, boolean isLast) {
            // 文本不需要收集时的空实现
        }

        @Override
        public boolean isAborted() {
            return false;
        }

        @Override
        public void registerAbortAction(Runnable action) {
            // 测试中不动作
        }
    }
}
