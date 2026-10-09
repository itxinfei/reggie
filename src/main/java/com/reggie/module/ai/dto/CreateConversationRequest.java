package com.reggie.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 创建对话请求DTO
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class CreateConversationRequest {

    @Schema(description = "对话标题", example = "午餐推荐")
    @Size(max = 100, message = "标题长度不能超过100字符")
    private String title;

    @Schema(description = "场景类型", example = "order_assistant")
    @Size(max = 50, message = "场景类型长度不能超过50字符")
    @Pattern(regexp = "^[a-zA-Z_]+$", message = "场景类型只能包含字母和下划线")
    private String scene;
}
