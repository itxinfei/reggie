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
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 工具执行器：独立线程池执行，单工具 {@value #TIMEOUT_SECONDS}s 超时，
 * worker 内显式设置租户 BaseContext（双保险，即使未来不从 aiExecutor 转入也安全），
 * 回灌模型的 JSON 结果截断到 {@value #MAX_RESULT_CHARS} 字符控制 token。
 *
 * <p>线程池弹性可回收（核心线程空闲 60s 释放、上限内按需扩容），并配合
 * {@link AiToolQueryTimeoutInterceptor} 给工具线程的 SQL 设置 JDBC queryTimeout：
 * 慢查询在驱动层被打断、worker 线程得以回收，避免「future.cancel(true) 打不断
 * JDBC socket 读 → 固定 4 线程被永久占满，工具调用降级到重启才恢复」。</p>
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

    /** 常驻核心线程数（空闲 60s 自动回收） */
    private static final int CORE_POOL_SIZE = 2;

    /** 线程池上限：全部占满时快速拒绝返回「系统繁忙」，而非无限堆积 */
    private static final int MAX_POOL_SIZE = 8;

    private final ObjectMapper objectMapper = ObjectMapperHolder.getDefault();

    /** worker 线程标记：{@link AiToolQueryTimeoutInterceptor} 据此给工具 SQL 设置 queryTimeout */
    private static final ThreadLocal<Boolean> TOOL_QUERY_HINT = new ThreadLocal<Boolean>();

    /** 仅限 AI 工具 worker 线程内判断（拦截器调用），其他线程恒 false */
    public static boolean shouldApplyQueryTimeout() {
        return Boolean.TRUE.equals(TOOL_QUERY_HINT.get());
    }

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new ThreadFactory() {
        private final AtomicInteger seq = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "ai-tool-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    public AiToolExecutor() {
        // 核心线程也允许超时回收，闲时归零
        pool.allowCoreThreadTimeOut(true);
    }

    /**
     * 执行工具（永不抛异常，失败/超时/线程池满统一转 fail 结果）。
     */
    public ToolExecResult execute(AiTool tool, JsonNode args, Long tenantId) {
        Future<ToolExecResult> future;
        try {
            future = pool.submit(() -> {
                // 报表服务部分方法依赖 ThreadLocal 租户（拦截器 fail-closed），显式设置 + finally 清理
                BaseContext.setCurrentTenantId(tenantId);
                TOOL_QUERY_HINT.set(Boolean.TRUE);
                try {
                    return tool.execute(args, tenantId);
                } finally {
                    TOOL_QUERY_HINT.remove();
                    BaseContext.remove();
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("AI工具线程池已满({}个worker均在执行): name={}, tenantId={}",
                    MAX_POOL_SIZE, tool.name(), tenantId);
            return ToolExecResult.fail("系统繁忙", "当前查询请求较多，请稍后重试");
        }
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
     * <p>截断为 JSON 安全截断：在边界补齐闭合括号并用 Jackson 验证，保证模型收到
     * 的是合法 JSON（原按字符硬切会把 JSON 切成非法串，模型只能对残文瞎猜）；
     * 无法闭合时回退硬切。</p>
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
                json = safeTruncateJson(json);
            }
            return json;
        } catch (Exception e) {
            return "{\"tool\":\"" + toolName + "\",\"success\":false,\"result\":\"结果序列化失败\"}";
        }
    }

    /**
     * JSON 安全截断：按括号/字符串状态补齐闭合符，Jackson 验证通过则包装为
     * {"truncated":true,"partialResult":...}；无法得到合法 JSON 时回退硬切加提示。
     */
    private String safeTruncateJson(String json) {
        String head = json.substring(0, MAX_RESULT_CHARS);
        String candidate = closeTruncatedJson(head);
        try {
            objectMapper.readTree(candidate);
            return "{\"truncated\":true,\"partialResult\":" + candidate + "}";
        } catch (Exception e) {
            // 括号闭合兜底失败（极端嵌套/转义场景）：退回硬切，保证不为模型制造非法 JSON 之外的坏状态
            return head + "...(结果过长已截断)";
        }
    }

    /**
     * 对截断后的 JSON 前缀补齐闭合符（尽力而为，最终以 Jackson 验证为准）：
     * 跟踪字符串/转义状态与括号栈——
     * <ul>
     *   <li>截断点在字符串中间：若该字符串是对象的悬空「键」（紧跟 { 或 , 之后），
     *       整个丢弃该残缺 token；否则补闭合引号（作为完整值）；</li>
     *   <li>去掉截断点残留的逗号/冒号/空白/半截字面量（tru、fals 等）；</li>
     *   <li>按逆序补齐未闭合括号。</li>
     * </ul>
     * 极端场景（如截断点恰在"键已闭合待取值"处）闭合失败时由调用方回退硬切。
     */
    private String closeTruncatedJson(String head) {
        java.util.ArrayDeque<Character> stack = new java.util.ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;
        int stringStart = -1;
        char prevStructural = 0;
        for (int i = 0; i < head.length(); i++) {
            char c = head.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inString) {
                if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                stringStart = i;
                continue;
            }
            if (c == '{' || c == '[') {
                stack.push(c);
                prevStructural = c;
            } else if (c == '}' || c == ']') {
                if (!stack.isEmpty() && stack.peek() == (c == '}' ? '{' : '[')) {
                    stack.pop();
                }
                prevStructural = c;
            } else if (c == ',' || c == ':') {
                prevStructural = c;
            } else if (!Character.isWhitespace(c)) {
                prevStructural = c;
            }
        }

        StringBuilder sb = new StringBuilder(head);
        if (inString) {
            // 悬空键判定：字符串以 { 或 , 开头（仅隔空白）且当前栈顶是对象 → 残缺键整体丢弃
            boolean danglingKey = !stack.isEmpty() && stack.peek() == '{' && stringStart > 0;
            if (danglingKey) {
                char before = 0;
                int k = stringStart - 1;
                while (k >= 0 && Character.isWhitespace(head.charAt(k))) {
                    k--;
                }
                if (k >= 0) {
                    before = head.charAt(k);
                }
                danglingKey = before == '{' || before == ',';
            }
            if (danglingKey) {
                sb.setLength(stringStart);
            } else {
                sb.append('"');
            }
        }
        // 去掉截断点残留的悬空分隔符/半截字面量（", " / ":" / "tru" 等）
        while (sb.length() > 0) {
            char last = sb.charAt(sb.length() - 1);
            if (last == ',' || last == ':' || Character.isWhitespace(last) || Character.isLetter(last)) {
                sb.setLength(sb.length() - 1);
                continue;
            }
            break;
        }
        while (!stack.isEmpty()) {
            sb.append(stack.pop() == '{' ? '}' : ']');
        }
        return sb.toString();
    }

    @PreDestroy
    public void shutdown() {
        pool.shutdownNow();
    }
}
