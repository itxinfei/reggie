package com.reggie.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * AI经营分析请求DTO
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class BusinessAnalysisRequest {

    @Schema(description = "分析问题", required = true, example = "为什么上月营业额下降了？")
    @NotBlank(message = "问题不能为空")
    @Size(min = 1, max = 2000, message = "问题长度不能超过2000字符")
    private String question;

    @Schema(description = "经营数据JSON", example = "{\"sales\":10000,\"orders\":200}")
    @Size(max = 10000, message = "数据JSON长度不能超过10000字符")
    private String data;
}
