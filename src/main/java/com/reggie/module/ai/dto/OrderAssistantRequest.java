package com.reggie.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 智能点餐助手请求DTO
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class OrderAssistantRequest {

    @Schema(description = "用户需求描述", required = true, example = "来一份辣的，不要香菜")
    @NotBlank(message = "消息内容不能为空")
    @Size(min = 1, max = 2000, message = "消息长度不能超过2000字符")
    private String message;

    @Schema(description = "对话ID（可选，用于延续历史对话）")
    @Size(max = 64, message = "对话ID长度不能超过64字符")
    private String conversationId;
}
