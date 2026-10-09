package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 会员充值请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class RechargeDTO {

    @Schema(description = "会员ID", required = true, example = "1")
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "充值金额", required = true, example = "100.00")
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "赠送金额", example = "10.00")
    private BigDecimal giftAmount;

    @Schema(description = "支付方式（WECHAT-微信, ALIPAY-支付宝, CASH-现金）", example = "WECHAT")
    private String paymentMethod;
}
