package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 支付请求DTO
 */
@Data
public class PayRequestDTO {

    @Schema(description = "订单ID", required = true, example = "1")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "支付渠道（WECHAT-微信、ALIPAY-支付宝）", required = true, example = "WECHAT")
    @NotNull(message = "支付渠道不能为空")
    private String channel;

    @Schema(description = "支付金额", required = true, example = "88.50")
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private BigDecimal amount;
}
