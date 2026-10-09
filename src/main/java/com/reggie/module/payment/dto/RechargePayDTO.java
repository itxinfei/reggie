package com.reggie.module.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 会员充值支付请求 DTO
 *
 * @author reggie
 * @since 2026-09-29
 */
@Data
public class RechargePayDTO {

    @Schema(description = "充值单号", required = true, example = "RC20260929120000AB12CD34")
    @NotBlank(message = "充值单号不能为空")
    private String rechargeNo;

    @Schema(description = "支付渠道：WECHAT-微信、ALIPAY-支付宝", required = true, example = "WECHAT")
    @NotBlank(message = "支付渠道不能为空")
    private String channel;

    @Schema(description = "支付方式（NATIVE/H5，不传由渠道默认）", example = "H5")
    private String payType;
}
