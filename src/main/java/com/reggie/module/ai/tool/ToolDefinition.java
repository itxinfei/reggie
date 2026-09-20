package com.reggie.module.ai.tool;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 暴露给大模型的工具定义（function calling 协议无关形态，由适配器翻译为各家 schema）。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Data
@AllArgsConstructor
public class ToolDefinition {

    /** 工具名（英文蛇形，模型据此发起调用） */
    private String name;

    /** 给模型看的能力描述（决定模型何时、如何调用本工具） */
    private String description;

    /** 入参 JSON Schema（OpenAI parameters / Anthropic input_schema） */
    private Map<String, Object> parameters;
}
