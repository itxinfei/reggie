package com.reggie.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 对话重命名请求 DTO。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Data
public class RenameConversationRequest {

    @Schema(description = "新标题", required = true, example = "周末聚餐推荐")
    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题长度不能超过50字符")
    private String title;
}
