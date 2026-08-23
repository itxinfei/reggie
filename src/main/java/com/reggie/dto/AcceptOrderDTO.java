package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 外卖订单接单请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class AcceptOrderDTO {

    @Schema(description = "外卖平台（MEITUAN-美团、ELEME-饿了么）", required = true, example = "MEITUAN")
    @NotBlank(message = "平台不能为空")
    private String platform;

    @Schema(description = "平台订单ID", required = true, example = "123456789")
    @NotBlank(message = "平台订单ID不能为空")
    private String platformOrderId;
}
