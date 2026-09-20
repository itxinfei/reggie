package com.reggie.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI消息（用于对话历史）
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIMessage {

    /** 角色：user / assistant / system */
    private String role;

    /** 消息内容 */
    private String content;

    /**
     * 附件ID列表（仅 role=user；P2 视觉多模态）。
     * <p>上下文缓存与 DB 重建只持有轻量 ID，不缓存 base64；
     * 发送前由附件服务做 owner 校验并读图生成 {@link #imageDataUrls}。</p>
     */
    private List<Long> attachmentIds;

    /**
     * 图片 data URL 列表（仅 role=user；P2 视觉多模态）。
     * 每项形如 data:image/jpeg;base64,...，由附件服务读取归属图片后生成；
     * 适配器按各家协议分流（OpenAI image_url / Anthropic image.source / Baidu 压平）。
     */
    private List<String> imageDataUrls;

    /**
     * 模型发起的工具调用（仅 role=assistant；P4 经营数据工具）。
     * 下一轮需以 role="tool" 的结果消息逐条回应，否则各家协议都会报错。
     */
    private List<ToolCall> toolCalls;

    /**
     * 工具调用 ID（仅 role=tool），关联对应 assistant 工具调用的 id。
     */
    private String toolCallId;

    /**
     * 工具名（仅 role=tool），OpenAI 协议要求工具结果消息携带 name 字段。
     */
    private String name;
}
