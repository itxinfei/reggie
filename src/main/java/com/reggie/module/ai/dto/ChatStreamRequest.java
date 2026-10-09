package com.reggie.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * AI 流式对话请求 DTO（POST /api/ai/chat/stream）。
 * <p>POST 化目的：携带 clientMsgId 幂等键、附件、context 等大 payload，
 * 并支持前端 AbortController 主动停止生成。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Data
public class ChatStreamRequest {

    @Schema(description = "用户消息（regenerate=true 时可空，服务端重放最后一条用户消息）", example = "帮我推荐几道辣菜")
    @Size(max = 4000, message = "消息长度不能超过4000字符")
    private String message;

    @Schema(description = "场景：order_assistant/dish_desc/business_analysis/marketing")
    @Size(max = 50, message = "场景参数非法")
    private String scene;

    @Schema(description = "对话ID（为空时自动创建）")
    @Size(max = 64, message = "对话ID长度不能超过64字符")
    private String conversationId;

    @Schema(description = "前端生成的消息幂等键，重复提交只落一条用户消息")
    @Size(max = 64, message = "clientMsgId长度不能超过64字符")
    private String clientMsgId;

    @Schema(description = "附件ID列表（P2 视觉多模态启用）")
    @Size(max = 4, message = "单次最多上传4个附件")
    private List<@Size(max = 64) String> attachments;

    @Schema(description = "附加上下文数据（如购物车/地址摘要）")
    private Map<String, Object> context;

    @Schema(description = "是否重新生成上一条回答")
    private Boolean regenerate;
}
