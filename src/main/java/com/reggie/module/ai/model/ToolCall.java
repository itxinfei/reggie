package com.reggie.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型发起的一次工具调用（function calling）。
 * <p>{@code arguments} 保留模型返回的原始 JSON 字符串，由工具执行方解析；
 * OpenAI 协议中 id 为 tool_call_id，Anthropic 协议中 id 为 tool_use 块 id，
 * 均在适配器层归一化到本结构。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {

    /** 工具调用唯一 ID（结果回填时关联用） */
    private String id;

    /** 工具名（与 {@code AiTool#name()} 对应） */
    private String name;

    /** 入参 JSON 字符串（模型生成，可能为非法 JSON，执行方需容错） */
    private String arguments;
}
