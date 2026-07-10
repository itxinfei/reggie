package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 退款请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class RefundRequestDTO {

    @Schema(description = "支付订单ID", required = true, example = "1")
    @NotNull(message = "支付订单ID不能为空")
    private Long paymentOrderId;

    @Schema(description = "退款金额", required = true, example = "88.50")
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "退款原因", required = true, example = "用户取消订单")
    @NotNull(message = "退款原因不能为空")
    @Size(max = 200, message = "退款原因不能超过200个字符")
    private String reason;
}
