package com.reggie.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 智能点餐助手请求DTO
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class OrderAssistantRequest {

    @Schema(description = "用户需求描述（携带图片时可为空）", example = "来一份辣的，不要香菜")
    @Size(max = 2000, message = "消息长度不能超过2000字符")
    private String message;

    @Schema(description = "对话ID（可选，用于延续历史对话）")
    @Size(max = 64, message = "对话ID长度不能超过64字符")
    private String conversationId;

    @Schema(description = "附件ID列表（P2 视觉多模态，纯图片消息时 message 可空）")
    @Size(max = 4, message = "单条消息最多上传4张图片")
    private List<String> attachments;
}
