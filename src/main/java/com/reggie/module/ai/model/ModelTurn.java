package com.reggie.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单轮模型调用结果（P4 工具多轮循环用）。
 * <p>与流式 token 回调互补：文本增量经 StreamCallback 实时推送，
 * 本结构承载该轮的完整文本、工具调用请求与结束原因。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelTurn {

    /** 结束原因：自然回答结束 */
    public static final String FINISH_STOP = "stop";
    /** 结束原因：模型请求调用工具 */
    public static final String FINISH_TOOL_CALLS = "tool_calls";

    /** 本轮完整文本（工具调用轮通常为空） */
    private String content;

    /** 模型请求的工具调用列表；为空表示本轮即最终自然语言答复 */
    private List<ToolCall> toolCalls;

    /** 结束原因：stop / tool_calls */
    private String finishReason;

    /** 是否发生错误（配置缺失/上游非 200/熔断等）；为 true 时 {@link #errorMessage} 可读 */
    private boolean error;

    /** 面向用户的错误提示 */
    private String errorMessage;

    /** 错误轮工厂 */
    public static ModelTurn error(String message) {
        return ModelTurn.builder()
                .error(true)
                .errorMessage(message)
                .finishReason(FINISH_STOP)
                .build();
    }
}
