package com.reggie.module.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.common.BaseContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AiToolExecutor} 单元测试：成功/异常/超时收敛为 {@link ToolExecResult}，
 * worker 内租户 ThreadLocal 传播且执行后清理，结果信封超长截断。
 *
 * @author reggie
 * @since 2026-09-21
 */
class AiToolExecutorTest {

    private AiToolExecutor executor;

    /** Jackson 取值即抛异常的 bean，用于触发 toToolContent 序列化兜底分支 */
    public static class ExplodingBean {
        public String getBoom() {
            throw new IllegalStateException("序列化时炸了");
        }
    }

    /** 测试用工具骨架：只覆写 execute 决定行为 */
    private abstract static class TestTool implements AiTool {
        @Override
        public String name() {
            return "test_tool";
        }

        @Override
        public String label() {
            return "测试工具";
        }

        @Override
        public String description() {
            return "测试用";
        }

        @Override
        public Map<String, Object> parametersSchema() {
            return new HashMap<String, Object>();
        }
    }

    @BeforeEach
    void setUp() {
        executor = new AiToolExecutor();
        BaseContext.remove();
    }

    @AfterEach
    void tearDown() {
        BaseContext.remove();
        executor.shutdown();
    }

    @Test
    void successfulExecutionReturnsResult() {
        AiTool tool = new TestTool() {
            @Override
            public ToolExecResult execute(JsonNode args, Long tenantId) {
                return ToolExecResult.ok("完成", "data-value");
            }
        };
        ToolExecResult result = executor.execute(tool, null, 77L);
        assertTrue(result.isSuccess());
        assertEquals("完成", result.getSummary());
        assertEquals("data-value", result.getData());
    }

    @Test
    void tenantPropagatedToWorkerThread() {
        final Long[] seenTenant = new Long[] {null};
        AiTool tool = new TestTool() {
            @Override
            public ToolExecResult execute(JsonNode args, Long tenantId) {
                seenTenant[0] = BaseContext.getCurrentTenantId();
                return ToolExecResult.ok("ok", null);
            }
        };
        executor.execute(tool, null, 99L);
        // worker 线程内读到的租户必须等于入参
        assertEquals(Long.valueOf(99L), seenTenant[0]);
        // finally 清理只作用于 worker 线程；这里验证主线程未被污染
        assertNull(BaseContext.getCurrentTenantId());
    }

    @Test
    void toolThrowingReturnsFailResult() {
        AiTool tool = new TestTool() {
            @Override
            public ToolExecResult execute(JsonNode args, Long tenantId) {
                throw new IllegalStateException("报表服务挂了");
            }
        };
        ToolExecResult result = executor.execute(tool, null, 1L);
        assertFalse(result.isSuccess());
        assertTrue(result.getData().toString().contains("报表服务挂了"));
    }

    @Test
    void slowToolTimesOutAndCancelled() {
        AiTool tool = new TestTool() {
            @Override
            public ToolExecResult execute(JsonNode args, Long tenantId) {
                try {
                    Thread.sleep(AiToolExecutor.TIMEOUT_SECONDS * 1000L + 3000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return ToolExecResult.ok("不该到达", null);
            }
        };
        long start = System.currentTimeMillis();
        ToolExecResult result = executor.execute(tool, null, 1L);
        long elapsed = System.currentTimeMillis() - start;
        assertFalse(result.isSuccess());
        assertEquals("查询超时", result.getSummary());
        // 超时判定应在 TIMEOUT 附近触发，不能等满 sleep
        assertTrue(elapsed < AiToolExecutor.TIMEOUT_SECONDS * 1000L + 3000L,
                "超时后应立即返回，实际耗时=" + elapsed);
    }

    @Test
    void toToolContentBuildsEnvelope() {
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("startDate", "2026-09-01");
        ToolExecResult result = ToolExecResult.ok("摘要", 123);
        String content = executor.toToolContent("test_tool", meta, result);
        assertTrue(content.contains("\"tool\":\"test_tool\""));
        assertTrue(content.contains("\"success\":true"));
        assertTrue(content.contains("\"startDate\":\"2026-09-01\""));
        assertTrue(content.contains("\"result\":123"));
    }

    @Test
    void toToolContentWithoutMetaSkipsMetaField() {
        String content = executor.toToolContent("t", null, ToolExecResult.ok("s", 1));
        assertFalse(content.contains("\"meta\""));
    }

    @Test
    void toToolContentTruncatesOversizedResult() {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < AiToolExecutor.MAX_RESULT_CHARS; i++) {
            huge.append('x');
        }
        String content = executor.toToolContent("t", null,
                ToolExecResult.ok("s", huge.toString()));
        assertTrue(content.length() < AiToolExecutor.MAX_RESULT_CHARS + 60);
        assertTrue(content.contains("结果过长已截断"));
    }

    @Test
    void toToolContentFallsBackWhenSerializationFails() {
        // getter 主动抛异常的 bean：Jackson 取值时抛 JsonMappingException 走固定兜底信封
        String content = executor.toToolContent("t", null, ToolExecResult.ok("s", new ExplodingBean()));
        assertTrue(content.contains("\"success\":false"));
        assertTrue(content.contains("结果序列化失败"));
    }
}
