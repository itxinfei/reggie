package com.reggie.module.urgency.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 催单请求 DTO
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@Schema(description = "催单请求")
public class UrgencyRequestDTO {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", example = "2001")
    private Long orderId;

    @NotNull(message = "会员ID不能为空")
    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号", example = "ORD20260827001")
    private String orderNo;
}
