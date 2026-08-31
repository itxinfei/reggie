package com.reggie.module.printer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 打印代理任务回执
 *
 * @author AI
 * @since 2026-08-30
 */
@Data
@Schema(description = "打印代理任务回执")
public class AgentCallbackDTO {

    @Schema(description = "执行结果：true=成功 false=失败", required = true)
    private Boolean success;

    @Schema(description = "失败原因（成功时可为空）", example = "打印机未连接")
    private String errorMsg;
}
