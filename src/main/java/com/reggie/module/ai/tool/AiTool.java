package com.reggie.module.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * AI 可调用的经营数据工具（只读）。
 * <p>实现类注册为 Spring Bean 后由 {@link AiToolRegistry} 自动收集；
 * 工具全程只读经营数据，禁止任何写操作。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
public interface AiTool {

    /** 工具名（英文蛇形，发送给模型） */
    String name();

    /** 中文名（状态条展示用） */
    String label();

    /** 能力描述（给模型看，决定调用时机） */
    String description();

    /** 入参 JSON Schema */
    Map<String, Object> parametersSchema();

    /**
     * 执行工具。
     *
     * @param args     模型给出的入参（已解析；非法/缺失参数由实现容错默认值）
     * @param tenantId 租户 ID（多租户隔离，禁止跨租户查询）
     * @return 执行结果（失败也返回 fail 结果，不抛异常打断多轮协议）
     */
    ToolExecResult execute(JsonNode args, Long tenantId);
}
