package com.reggie.module.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.ai.adapter.AbortableStreamCallback;
import com.reggie.module.ai.adapter.AiModelAdapter;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.ModelTurn;
import com.reggie.module.ai.model.ToolCall;
import com.reggie.module.ai.provider.AiProviderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * AI 工具调用编排器（P4）：在模型与报表工具之间跑 function-calling 多轮循环。
 * <pre>
 * 模型答复(可流式) ── 无 tool_calls → 结束（最终答复）
 *                 └─ 有 tool_calls → 逐工具执行（推 SSE tool 事件）→ 结果回填 → 下一轮
 * </pre>
 * 最多 {@value #MAX_ROUNDS} 轮（前 2 轮提供工具，最后 1 轮强制无工具，让模型基于结果作答），
 * 累计工具调用不超过 {@value #MAX_TOOL_CALLS} 次，防止异常模型空转烧 token。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Slf4j
@Component
public class AiToolOrchestrator {

    /** 最大模型轮次（含最终无工具总结轮） */
    public static final int MAX_ROUNDS = 3;

    /** 单次对话累计工具调用上限 */
    public static final int MAX_TOOL_CALLS = 5;

    @Resource
    private AiProviderManager providerManager;

    @Resource
    private AiToolRegistry toolRegistry;

    @Resource
    private AiToolExecutor toolExecutor;

    private final ObjectMapper objectMapper = ObjectMapperHolder.getDefault();

    /**
     * 执行多轮工具对话。文本增量经 textSink 实时推送（与普通流式链路同一出口），
     * 工具状态经 eventSink 推 SSE tool 事件。
     *
     * @param messages  对话消息（system/history/user 已由服务层构建；本方法原地追加工具轮消息）
     * @param tenantId  租户 ID
     * @param abort     中止回调
     * @param textSink  文本增量出口（只推非 last token）
     * @param eventSink 工具事件出口
     * @return 最终一轮结果；error=true 表示需向用户展示错误
     */
    public ModelTurn run(List<AIMessage> messages, int maxTokens, double temperature, Long tenantId,
                         AbortableStreamCallback abort,
                         AiModelAdapter.StreamCallback textSink, ToolEventSink eventSink) {
        int totalCalls = 0;
        ModelTurn turn = null;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            if (abort.isAborted()) {
                return abortedTurn();
            }
            // 最后一轮强制收回工具：模型必须基于已取到的数据自然语言作答，杜绝循环空转
            boolean offerTools = round < MAX_ROUNDS - 1 && totalCalls < MAX_TOOL_CALLS;
            List<ToolDefinition> definitions = offerTools
                    ? toolRegistry.definitions() : Collections.<ToolDefinition>emptyList();

            try {
                turn = providerManager.chatTurn(messages, maxTokens, temperature,
                        definitions, abort, textSink);
            } catch (Exception e) {
                if (abort.isAborted()) {
                    return abortedTurn();
                }
                log.warn("AI工具轮模型调用异常: round={}", round, e);
                return ModelTurn.error("AI服务调用失败：" + e.getMessage());
            }

            if (turn == null) {
                return abort.isAborted() ? abortedTurn() : ModelTurn.error("AI服务返回了空响应");
            }
            if (turn.isError() || abort.isAborted()) {
                return abort.isAborted() ? abortedTurn() : turn;
            }

            List<ToolCall> calls = turn.getToolCalls();
            if (calls == null || calls.isEmpty()) {
                // 最终自然语言答复（文本已流式推给前端）
                return turn;
            }

            // 回填：先放携带 tool_calls 的 assistant 消息，再逐条放 role=tool 结果
            messages.add(AIMessage.builder()
                    .role("assistant")
                    .content(blankToNull(turn.getContent()))
                    .toolCalls(calls)
                    .build());

            for (ToolCall call : calls) {
                String label = toolRegistry.labelOf(call.getName());
                if (totalCalls >= MAX_TOOL_CALLS) {
                    messages.add(buildLimitMessage(call));
                    eventSink.onToolEvent(ToolEvent.error(call.getName(), label, "已达调用上限"));
                    continue;
                }
                totalCalls++;
                eventSink.onToolEvent(ToolEvent.running(call.getName(), label));

                ToolExecResult result;
                AiTool tool = toolRegistry.get(call.getName());
                if (tool == null) {
                    result = ToolExecResult.fail("工具不存在", "模型请求了未注册的工具：" + call.getName());
                } else {
                    result = toolExecutor.execute(tool, parseArguments(call.getArguments()), tenantId);
                }
                String content = toolExecutor.toToolContent(call.getName(), null, result);
                messages.add(AIMessage.builder()
                        .role("tool")
                        .name(call.getName())
                        .toolCallId(call.getId() == null ? "" : call.getId())
                        .content(content)
                        .build());
                eventSink.onToolEvent(result.isSuccess()
                        ? ToolEvent.done(call.getName(), label, result.getSummary())
                        : ToolEvent.error(call.getName(), label, result.getSummary()));
            }
        }
        return turn;
    }

    private ModelTurn abortedTurn() {
        return ModelTurn.builder().finishReason(ModelTurn.FINISH_STOP).build();
    }

    private String blankToNull(String text) {
        return (text == null || text.isEmpty()) ? null : text;
    }

    private AIMessage buildLimitMessage(ToolCall call) {
        return AIMessage.builder()
                .role("tool")
                .name(call.getName())
                .toolCallId(call.getId() == null ? "" : call.getId())
                .content("已达到单次对话工具调用次数上限(" + MAX_TOOL_CALLS + ")，请基于已有数据直接回答")
                .build();
    }

    /** 模型给的 arguments 是 JSON 字符串；非法时给空对象，工具按缺省参数执行 */
    private JsonNode parseArguments(String arguments) {
        try {
            if (arguments == null || arguments.trim().isEmpty()) {
                return objectMapper.readTree("{}");
            }
            return objectMapper.readTree(arguments);
        } catch (Exception e) {
            log.debug("工具入参JSON解析失败，按空参数执行: {}", arguments);
            try {
                return objectMapper.readTree("{}");
            } catch (Exception ignore) {
                return null;
            }
        }
    }
}
