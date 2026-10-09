package com.reggie.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 反馈记录请求DTO
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class RecordFeedbackRequest {

    @Schema(description = "消息ID", required = true, example = "1")
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    @Schema(description = "反馈类型：positive / negative", required = true, example = "positive")
    @Pattern(regexp = "^(positive|negative)$", message = "反馈类型只能是 positive 或 negative")
    private String feedbackType;
}
