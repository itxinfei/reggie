package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 支付请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class PayRequestDTO {

    @Schema(description = "订单ID", required = true, example = "1")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "支付渠道（WECHAT-微信、ALIPAY-支付宝）", required = true, example = "WECHAT")
    @NotNull(message = "支付渠道不能为空")
    private String channel;

    // 金额不做入参校验：支付金额一律以数据库订单 order.amount 为准（防篡改），见 PaymentController#pay。
    // 历史教训：此处曾误加 @NotNull/@DecimalMin，而前端按防篡改设计不传 amount，
    // 导致请求在进入方法前被 Bean Validation 拦截、C 端无法支付。保留字段仅为兼容历史调用方，禁止再补校验。
    @Schema(description = "支付金额（仅透传、不参与计价，后端以订单金额为准）", example = "88.50")
    private BigDecimal amount;
}
