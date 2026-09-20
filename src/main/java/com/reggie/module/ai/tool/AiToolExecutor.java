package com.reggie.module.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.ObjectMapperHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 工具执行器：独立线程池执行，单工具 {@value #TIMEOUT_SECONDS}s 超时，
 * worker 内显式设置租户 BaseContext（双保险，即使未来不从 aiExecutor 转入也安全），
 * 回灌模型的 JSON 结果截断到 {@value #MAX_RESULT_CHARS} 字符控制 token。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Slf4j
@Component
public class AiToolExecutor {

    /** 单工具超时秒数 */
    public static final int TIMEOUT_SECONDS = 8;

    /** 回灌模型结果的最大字符数 */
    public static final int MAX_RESULT_CHARS = 4096;

    private final ObjectMapper objectMapper = ObjectMapperHolder.getDefault();

    private final ExecutorService pool = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private final AtomicInteger seq = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "ai-tool-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * 执行工具（永不抛异常，失败/超时统一转 fail 结果）。
     */
    public ToolExecResult execute(AiTool tool, JsonNode args, Long tenantId) {
        Future<ToolExecResult> future = pool.submit(() -> {
            // 报表服务部分方法依赖 ThreadLocal 租户（拦截器 fail-closed），显式设置 + finally 清理
            BaseContext.setCurrentTenantId(tenantId);
            try {
                return tool.execute(args, tenantId);
            } finally {
                BaseContext.remove();
            }
        });
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("AI工具执行超时({}s): name={}, tenantId={}", TIMEOUT_SECONDS, tool.name(), tenantId);
            return ToolExecResult.fail("查询超时", "工具执行超过 " + TIMEOUT_SECONDS + " 秒已终止，请缩小日期范围后重试");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("AI工具执行异常: name={}, tenantId={}, err={}", tool.name(), tenantId, cause.getMessage());
            return ToolExecResult.fail("查询失败", "工具执行失败：" + cause.getMessage());
        }
    }

    /**
     * 把工具结果包装为回灌模型的 JSON 字符串（含工具名与实际生效参数），超长截断。
     */
    public String toToolContent(String toolName, Map<String, Object> meta, ToolExecResult result) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("tool", toolName);
        envelope.put("success", result.isSuccess());
        if (meta != null && !meta.isEmpty()) {
            envelope.put("meta", meta);
        }
        envelope.put("result", result.getData());
        try {
            String json = objectMapper.writeValueAsString(envelope);
            if (json.length() > MAX_RESULT_CHARS) {
                json = json.substring(0, MAX_RESULT_CHARS) + "...(结果过长已截断)";
            }
            return json;
        } catch (Exception e) {
            return "{\"tool\":\"" + toolName + "\",\"success\":false,\"result\":\"结果序列化失败\"}";
        }
    }

    @PreDestroy
    public void shutdown() {
        pool.shutdownNow();
    }
}
